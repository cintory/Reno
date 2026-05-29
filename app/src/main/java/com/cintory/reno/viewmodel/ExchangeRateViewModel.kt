package com.cintory.reno.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cintory.reno.data.model.ExchangeRate
import com.cintory.reno.data.repository.ExchangeRateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import timber.log.Timber
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by Cintory on 2026/1/29 15:56
 * Email：Cintory@gmail.com
 */

sealed class RateUiState {
  data object Loading : RateUiState()
  data object Refreshing : RateUiState()
  data class Success(val rates: List<ExchangeRate>, val lastUpdateTime: String?) : RateUiState()
  data class Error(val message: String) : RateUiState()
}

@HiltViewModel
class ExchangeRateViewModel @Inject constructor(
  private val repository: ExchangeRateRepository,
  @ApplicationContext context: Context
) : ViewModel() {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)

  private val _uiState = MutableStateFlow<RateUiState>(RateUiState.Loading)
  val uiState: StateFlow<RateUiState> = _uiState

  private var refreshJob: Job? = null

  val latestRates: StateFlow<List<ExchangeRate>> = repository.getLatestRates()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun getHistoryRates(name: String): Flow<List<ExchangeRate>> = repository.getHistoryRates(name)

  fun getSavedFromCurrency(): String = prefs.getString(KEY_FROM, CNY) ?: CNY
  fun getSavedToCurrency(): String = prefs.getString(KEY_TO, "") ?: ""

  fun saveFromCurrency(currency: String) {
    prefs.edit().putString(KEY_FROM, currency).apply()
  }

  fun saveToCurrency(currency: String) {
    prefs.edit().putString(KEY_TO, currency).apply()
  }

  init {
    observeRates()
    initialLoad()
    startPeriodicRefresh()
  }

  private fun observeRates() {
    viewModelScope.launch {
      latestRates.collect { rates ->
        if (rates.isNotEmpty()) {
          val lastUpdate = repository.getLastUpdateTime()
          _uiState.value = RateUiState.Success(rates, lastUpdate)
        }
      }
    }
  }

  private fun initialLoad() {
    Timber.d("initialLoad started")
    refreshJob = viewModelScope.launch {
      try {
        if (!repository.hasCachedData()) {
          _uiState.value = RateUiState.Loading
        }
        repository.refreshRates()
        val lastUpdate = repository.getLastUpdateTime()
        _uiState.value = RateUiState.Success(latestRates.value, lastUpdate)
        Timber.d("initialLoad completed")
      } catch (e: CancellationException) {
        Timber.d("initialLoad cancelled")
        throw e
      } catch (e: Exception) {
        Timber.e(e, "initialLoad failed")
        if (_uiState.value is RateUiState.Loading) {
          _uiState.value = RateUiState.Error(e.message ?: "未知错误")
        }
      }
    }
  }

  fun refreshRates() {
    if (refreshJob?.isActive == true) {
      Timber.d("refresh already in progress, skipping")
      return
    }
    refreshJob = viewModelScope.launch {
      val current = _uiState.value
      Timber.d("refreshRates started, current state: %s", current::class.simpleName)
      if (current is RateUiState.Success) {
        _uiState.value = RateUiState.Refreshing
      } else {
        _uiState.value = RateUiState.Loading
      }
      try {
        repository.refreshRates()
        val lastUpdate = repository.getLastUpdateTime()
        _uiState.value = RateUiState.Success(latestRates.value, lastUpdate)
        Timber.d("refreshRates completed")
      } catch (e: CancellationException) {
        Timber.d("refreshRates cancelled")
        throw e
      } catch (e: Exception) {
        Timber.e(e, "refreshRates failed")
        if (current is RateUiState.Success) {
          _uiState.value = current
        } else {
          _uiState.value = RateUiState.Error(e.message ?: "刷新失败")
        }
      }
    }
  }

  private fun startPeriodicRefresh() {
    viewModelScope.launch {
      while (true) {
        delay(30 * 60 * 1000L)
        Timber.d("periodic refresh triggered")
        try {
          repository.refreshRates()
          Timber.d("periodic refresh completed")
        } catch (_: Exception) {
          Timber.w("periodic refresh failed")
        }
      }
    }
  }

  companion object {
    const val CNY = "人民币 (CNY)"
    private const val KEY_FROM = "selected_from_currency"
    private const val KEY_TO = "selected_to_currency"

    val COMMON_CURRENCIES = listOf(
      "美元", "欧元", "英镑", "日元", "港币",
      "澳大利亚元", "加拿大元", "瑞士法郎", "新加坡元", "新西兰元", "韩国元", "新台币"
    )

    fun sortedCurrencyOptions(rates: List<ExchangeRate>): List<String> {
      val names = rates.map { it.name }
      val common = COMMON_CURRENCIES.filter { it in names }
      val rest = names.filter { it !in COMMON_CURRENCIES }
      return listOf(CNY) + common + rest
    }

    fun sortedRates(rates: List<ExchangeRate>): List<ExchangeRate> {
      val commonSet = COMMON_CURRENCIES.toSet()
      val common = COMMON_CURRENCIES.mapNotNull { name -> rates.find { it.name == name } }
      val rest = rates.filter { it.name !in commonSet }
      return common + rest
    }

    fun getEffectiveRate(rate: ExchangeRate): Double? {
      val conv = rate.conversionPrice.toDoubleOrNull()
      if (conv != null && conv > 0) return conv
      val sell = rate.fSellPrice.toDoubleOrNull()
      if (sell != null && sell > 0) return sell
      val mSell = rate.mSellPrice.toDoubleOrNull()
      if (mSell != null && mSell > 0) return mSell
      return null
    }

    fun convert(
      amount: Double,
      fromCurrency: String,
      toCurrency: String,
      rates: List<ExchangeRate>
    ): Double? {
      if (fromCurrency == toCurrency) return amount

      val fromIsCny = fromCurrency == CNY
      val toIsCny = toCurrency == CNY

      if (fromIsCny) {
        val toRate = rates.find { it.name == toCurrency } ?: return null
        val effectiveRate = getEffectiveRate(toRate) ?: return null
        return amount / effectiveRate * 100
      }

      if (toIsCny) {
        val fromRate = rates.find { it.name == fromCurrency } ?: return null
        val effectiveRate = getEffectiveRate(fromRate) ?: return null
        return amount * effectiveRate / 100
      }

      val fromRate = rates.find { it.name == fromCurrency } ?: return null
      val toRate = rates.find { it.name == toCurrency } ?: return null
      val fromEffective = getEffectiveRate(fromRate) ?: return null
      val toEffective = getEffectiveRate(toRate) ?: return null
      val cnyAmount = amount * fromEffective / 100
      return cnyAmount / toEffective * 100
    }
  }
}
