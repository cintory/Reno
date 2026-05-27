package com.cintory.reno.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Created by Cintory on 2025/8/1 15:25
 * Email：Cintory@gmail.com
 */
@HiltViewModel
class CreatePlanViewModel @Inject constructor(
  @ApplicationContext
  application: Context,
) : AndroidViewModel(application as Application) {
  private val context = application

  suspend fun createPlan() = withContext(viewModelScope.coroutineContext) {

  }
}