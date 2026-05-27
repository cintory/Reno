package com.cintory.reno.di

import android.content.Context
import androidx.room.Room
import com.cintory.reno.data.local.ExchangeRateDao
import com.cintory.reno.data.local.RenoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Created by Cintory on 2026/5/26 15:00
 * Email：Cintory@gmail.com
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

  @Provides
  @Singleton
  fun provideDatabase(@ApplicationContext context: Context): RenoDatabase {
    return Room.databaseBuilder(
      context,
      RenoDatabase::class.java,
      "reno_database"
    ).build()
  }

  @Provides
  fun provideExchangeRateDao(database: RenoDatabase): ExchangeRateDao {
    return database.exchangeRateDao()
  }
}
