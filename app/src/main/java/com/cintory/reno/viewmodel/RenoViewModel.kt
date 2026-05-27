package com.cintory.reno.viewmodel

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.cintory.reno.data.model.Plan
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Created by Cintory on 2025/7/31 16:49
 * Email：Cintory@gmail.com
 */
@HiltViewModel
class RenoViewModel @Inject constructor(
  @ApplicationContext private val appContext: Context
) :
  ViewModel() {
  var planList = mutableStateListOf<Plan>()
    private set

  init {

  }
}

val LocalRenoViewModel =
  compositionLocalOf<RenoViewModel> { error("Reno view model not found!") }