package com.cintory.reno

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

/**
 * Created by Cintory on 2025/7/31 18:23
 * Email：Cintory@gmail.com
 */
@HiltAndroidApp
class RenoApp : Application() {
  companion object {
    @SuppressLint("StaticFieldLeak")
    lateinit var CONTEXT: Context
  }

  override fun attachBaseContext(base: Context?) {
    CONTEXT = this
    super.attachBaseContext(base)
  }
}