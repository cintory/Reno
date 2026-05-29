package com.cintory.reno

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cintory.reno.worker.RefreshRatesWorker
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Created by Cintory on 2025/7/31 18:23
 * Email：Cintory@gmail.com
 */
@HiltAndroidApp
class RenoApp : Application(), Configuration.Provider {
  companion object {
    @SuppressLint("StaticFieldLeak")
    lateinit var CONTEXT: Context
  }

  @Inject
  lateinit var workerFactory: HiltWorkerFactory

  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder()
      .setWorkerFactory(workerFactory)
      .build()

  override fun attachBaseContext(base: Context?) {
    CONTEXT = this
    super.attachBaseContext(base)
  }

  override fun onCreate() {
    super.onCreate()
    if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
      Timber.plant(Timber.DebugTree())
    }
    scheduleDailyRefresh()
  }

  private fun scheduleDailyRefresh() {
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

    val request = PeriodicWorkRequestBuilder<RefreshRatesWorker>(24, TimeUnit.HOURS)
      .setConstraints(constraints)
      .build()

    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
      RefreshRatesWorker.WORK_NAME,
      ExistingPeriodicWorkPolicy.KEEP,
      request
    )
  }
}
