package com.cintory.reno

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import com.cintory.reno.ui.page.common.Navigation
import com.cintory.reno.viewmodel.LocalRenoViewModel
import com.cintory.reno.viewmodel.LocalUserState
import com.cintory.reno.viewmodel.RenoViewModel
import com.cintory.reno.viewmodel.UserStateViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Created by Cintory on 2025/7/31 14:47
 * Email：Cintory@gmail.com
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  private val userStateViewModel: UserStateViewModel by viewModels()
  private val finleyViewModel: RenoViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      CompositionLocalProvider(
        LocalUserState provides userStateViewModel,
        LocalRenoViewModel provides finleyViewModel
      ) {
        Navigation()
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
  }
}