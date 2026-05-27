package com.cintory.reno.ui.page.common

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cintory.reno.ui.page.home.HomePage
import com.cintory.reno.ui.theme.RenoTheme

/**
 * Created by Cintory on 2025/7/30 17:48
 * Email：Cintory@gmail.com
 */
@Composable
fun Navigation() {
  val navController = rememberNavController()

  CompositionLocalProvider(LocalRootNavController provides navController) {
    RenoTheme {
      NavHost(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        navController = navController,
        startDestination = RouteName.HOME,
        enterTransition = {
          slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Up,
            initialOffset = { it / 4 }) + fadeIn()
        },
        exitTransition = {
          slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Down,
            targetOffset = { it / 4 }) + fadeOut()
        },
      ) {
        composable(RouteName.HOME) {
          HomePage(navController = navController)
        }
      }
    }
  }
}

val LocalRootNavController =
  compositionLocalOf<NavHostController> { error("nav host controller not found") }
