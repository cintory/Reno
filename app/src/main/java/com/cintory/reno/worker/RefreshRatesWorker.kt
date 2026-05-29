package com.cintory.reno.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cintory.reno.data.repository.ExchangeRateRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Created by Cintory on 2026/5/28 11:00
 * Email：Cintory@gmail.com
 */
@HiltWorker
class RefreshRatesWorker @AssistedInject constructor(
  @Assisted appContext: Context,
  @Assisted workerParams: WorkerParameters,
  private val repository: ExchangeRateRepository,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    Timber.d("worker started")
    return try {
      repository.refreshRates()
      Timber.d("worker completed")
      Result.success()
    } catch (e: Exception) {
      Timber.e(e, "worker failed, will retry")
      Result.retry()
    }
  }

  companion object {
    const val WORK_NAME = "refresh_exchange_rates"
  }
}
