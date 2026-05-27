package com.cintory.reno.viewmodel

import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Created by Cintory on 2025/7/30 18:01
 * Email：Cintory@gmail.com
 */
@HiltViewModel
class UserStateViewModel @Inject constructor() : ViewModel() {
}

val LocalUserState =
  compositionLocalOf<UserStateViewModel> { error("Local user state not found.") }