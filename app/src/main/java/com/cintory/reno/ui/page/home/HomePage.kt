package com.cintory.reno.ui.page.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.cintory.reno.data.model.ExchangeRate
import com.cintory.reno.viewmodel.ExchangeRateViewModel
import com.cintory.reno.viewmodel.RateUiState
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Created by Cintory on 2026/1/29 14:15
 * Email：Cintory@gmail.com
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
  navController: NavHostController,
  viewModel: ExchangeRateViewModel = hiltViewModel(),
  lazyListState: LazyListState = rememberLazyListState(),
) {
  val uiState by viewModel.uiState.collectAsState()

  var amountText by remember { mutableStateOf("100") }
  var fromCurrency by remember { mutableStateOf(viewModel.getSavedFromCurrency()) }
  var toCurrency by remember { mutableStateOf(viewModel.getSavedToCurrency()) }

  Scaffold(topBar = {
    TopAppBar(
      title = {
        Column {
          Text(
            "汇率计算",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
          )
          val state = uiState
          if (state is RateUiState.Success && state.lastUpdateTime != null) {
            Text(
              text = "更新: ${state.lastUpdateTime}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      },
      actions = {
        IconButton(onClick = { viewModel.refreshRates() }) {
          Icon(
            Icons.Filled.Refresh,
            contentDescription = "刷新",
            tint = MaterialTheme.colorScheme.primary
          )
        }
      },
    )
  }) { paddingValues ->
    when (val state = uiState) {
      is RateUiState.Loading -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("正在获取汇率数据...", color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }

      is RateUiState.Error -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              "加载失败",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              state.message,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = { viewModel.refreshRates() }) {
              Text("重试")
            }
          }
        }
      }

      is RateUiState.Refreshing, is RateUiState.Success -> {
        val rates = when (state) {
          is RateUiState.Success -> state.rates
          else -> viewModel.latestRates.collectAsState().value
        }
        if (toCurrency.isEmpty() && rates.isNotEmpty()) {
          toCurrency = rates.find { it.name == "美元" }?.name ?: rates.first().name
          viewModel.saveToCurrency(toCurrency)
        }

        val currencyOptions = remember(rates) {
          ExchangeRateViewModel.sortedCurrencyOptions(rates)
        }

        val sortedRates = remember(rates) {
          ExchangeRateViewModel.sortedRates(rates)
        }

        val convertedResult by remember(amountText, fromCurrency, toCurrency, rates) {
          derivedStateOf {
            val amount = amountText.toDoubleOrNull() ?: return@derivedStateOf null
            ExchangeRateViewModel.convert(amount, fromCurrency, toCurrency, rates)
          }
        }

        val currentRate by remember(fromCurrency, toCurrency, rates) {
          derivedStateOf {
            if (fromCurrency == toCurrency) return@derivedStateOf "1"
            val fromIsCny = fromCurrency == ExchangeRateViewModel.CNY
            val toIsCny = toCurrency == ExchangeRateViewModel.CNY
            when {
              fromIsCny -> {
                val r = rates.find { it.name == toCurrency }
                val eff = r?.let { ExchangeRateViewModel.getEffectiveRate(it) }
                if (eff != null) formatRate(100.0 / eff * 100) else null
              }
              toIsCny -> {
                val r = rates.find { it.name == fromCurrency }
                val eff = r?.let { ExchangeRateViewModel.getEffectiveRate(it) }
                if (eff != null) formatRate(eff / 100.0) else null
              }
              else -> {
                val fromRate = rates.find { it.name == fromCurrency }
                val toRate = rates.find { it.name == toCurrency }
                val fromEff = fromRate?.let { ExchangeRateViewModel.getEffectiveRate(it) }
                val toEff = toRate?.let { ExchangeRateViewModel.getEffectiveRate(it) }
                if (fromEff != null && toEff != null) formatRate(fromEff / toEff) else null
              }
            }
          }
        }

        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
          if (state is RateUiState.Refreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
          }
          LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
          ) {
            item(contentType = "converter") {
              ConverterSection(
              amountText = amountText,
              onAmountChange = { amountText = it },
              fromCurrency = fromCurrency,
              toCurrency = toCurrency,
              currencyOptions = currencyOptions,
              onFromCurrencyChange = {
                fromCurrency = it
                viewModel.saveFromCurrency(it)
              },
              onToCurrencyChange = {
                toCurrency = it
                viewModel.saveToCurrency(it)
              },
              onSwap = {
                val temp = fromCurrency
                fromCurrency = toCurrency
                toCurrency = temp
                viewModel.saveFromCurrency(fromCurrency)
                viewModel.saveToCurrency(toCurrency)
              },
              convertedResult = convertedResult,
              currentRate = currentRate
            )
          }

          item(contentType = "header") {
            Text(
              "全部汇率",
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
          }

          items(
            items = sortedRates,
            key = { it.name },
            contentType = { "rate_card" }
          ) { rate ->
            RateCard(rate)
          }

          item {
            Spacer(modifier = Modifier.height(16.dp))
          }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConverterSection(
  amountText: String,
  onAmountChange: (String) -> Unit,
  fromCurrency: String,
  toCurrency: String,
  currencyOptions: List<String>,
  onFromCurrencyChange: (String) -> Unit,
  onToCurrencyChange: (String) -> Unit,
  onSwap: () -> Unit,
  convertedResult: Double?,
  currentRate: String?,
) {
  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    OutlinedTextField(
      value = amountText,
      onValueChange = { input ->
        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
          onAmountChange(input)
        }
      },
      label = { Text("金额") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
    )

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      CurrencyDropdown(
        label = "源货币",
        selected = fromCurrency,
        options = currencyOptions,
        onSelected = onFromCurrencyChange,
        modifier = Modifier.weight(1f)
      )

      FilledIconButton(
        onClick = onSwap,
        modifier = Modifier
          .padding(horizontal = 8.dp)
          .size(48.dp)
      ) {
        Icon(Icons.Filled.SwapVert, contentDescription = "交换货币")
      }

      CurrencyDropdown(
        label = "目标货币",
        selected = toCurrency,
        options = currencyOptions,
        onSelected = onToCurrencyChange,
        modifier = Modifier.weight(1f)
      )
    }

    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer
      )
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        if (convertedResult != null && amountText.isNotEmpty()) {
          val fromLabel = currencyShortName(fromCurrency)
          val toLabel = currencyShortName(toCurrency)
          Text(
            text = "$amountText $fromLabel = ${formatResult(convertedResult)} $toLabel",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
          if (currentRate != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "1 $fromLabel = $currentRate $toLabel",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
          }
        } else {
          Text(
            text = if (amountText.isEmpty()) "请输入金额" else "无法计算",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(
  label: String,
  selected: String,
  options: List<String>,
  onSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded },
    modifier = modifier
  ) {
    TextField(
      value = currencyShortName(selected),
      onValueChange = {},
      readOnly = true,
      label = { Text(label) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      modifier = Modifier
        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        .fillMaxWidth(),
      singleLine = true
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { currency ->
        DropdownMenuItem(
          text = { Text(currency) },
          onClick = {
            onSelected(currency)
            expanded = false
          }
        )
      }
    }
  }
}

@Composable
fun RateCard(rate: ExchangeRate) {
  var expanded by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .animateContentSize()
      .clickable { expanded = !expanded }
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = rate.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (rate.conversionPrice.isNotEmpty()) {
            Text(
              text = rate.conversionPrice,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.primary
            )
          }
          Spacer(modifier = Modifier.width(4.dp))
          Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      if (expanded) {
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        RateRow("现汇买入", rate.fBuyPrice)
        RateRow("现钞买入", rate.mBuyPrice)
        RateRow("现汇卖出", rate.fSellPrice)
        RateRow("现钞卖出", rate.mSellPrice)
        RateRow("中行折算价", rate.conversionPrice)

        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = rate.publishTime,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun RateRow(label: String, value: String) {
  if (value.isEmpty()) return
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium
    )
  }
}

private fun currencyShortName(name: String): String {
  return when (name) {
    ExchangeRateViewModel.CNY -> "CNY"
    "美元" -> "USD"
    "欧元" -> "EUR"
    "英镑" -> "GBP"
    "日元" -> "JPY"
    "港币" -> "HKD"
    "澳大利亚元" -> "AUD"
    "加拿大元" -> "CAD"
    "新加坡元" -> "SGD"
    "瑞士法郎" -> "CHF"
    "新西兰元" -> "NZD"
    "韩国元" -> "KRW"
    "泰国铢" -> "THB"
    "瑞典克朗" -> "SEK"
    "丹麦克朗" -> "DKK"
    "挪威克朗" -> "NOK"
    "林吉特" -> "MYR"
    "卢布" -> "RUB"
    "南非兰特" -> "ZAR"
    "澳门元" -> "MOP"
    "新台币" -> "TWD"
    else -> name
  }
}

private fun formatResult(value: Double): String {
  return BigDecimal(value).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}

private fun formatRate(value: Double): String {
  return BigDecimal(value).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}
