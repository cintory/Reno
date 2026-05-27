package com.cintory.reno.ext

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController

/**
 * Created by Cintory on 2025/8/1 17:09
 * Email：Cintory@gmail.com
 */
fun NavController.popBackStackIfLifecycleIsResumed(lifecycleOwner: LifecycleOwner? = null) {
  if (lifecycleOwner?.lifecycle?.currentState === Lifecycle.State.RESUMED) {
    popBackStack()
  }
}