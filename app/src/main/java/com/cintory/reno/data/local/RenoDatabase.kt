package com.cintory.reno.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cintory.reno.data.model.ExchangeRate

/**
 * Created by Cintory on 2026/5/26 15:00
 * Email：Cintory@gmail.com
 */
@Database(entities = [ExchangeRate::class], version = 2, exportSchema = false)
abstract class RenoDatabase : RoomDatabase() {
  abstract fun exchangeRateDao(): ExchangeRateDao
}
