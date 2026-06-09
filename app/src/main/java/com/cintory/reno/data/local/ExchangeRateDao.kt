package com.cintory.reno.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cintory.reno.data.model.ExchangeRate
import kotlinx.coroutines.flow.Flow

/**
 * Created by Cintory on 2026/5/26 15:00
 * Email：Cintory@gmail.com
 */
@Dao
interface ExchangeRateDao {

  @Query(
    """
    SELECT * FROM exchange_rate e1
    WHERE e1.publishTime = (
      SELECT MAX(e2.publishTime) FROM exchange_rate e2 WHERE e2.name = e1.name
    )
    GROUP BY e1.name
    ORDER BY e1.name
    """
  )
  fun getLatestRates(): Flow<List<ExchangeRate>>

  @Query(
    """
    SELECT * FROM exchange_rate e1
    WHERE e1.publishTime = (
      SELECT MAX(e2.publishTime) FROM exchange_rate e2 WHERE e2.name = e1.name
    )
    GROUP BY e1.name
    ORDER BY e1.name
    """
  )
  suspend fun getLatestRatesOnce(): List<ExchangeRate>

  @Query("SELECT * FROM exchange_rate ORDER BY publishTime DESC")
  fun getAllRates(): Flow<List<ExchangeRate>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(rates: List<ExchangeRate>)

  @Query("DELETE FROM exchange_rate")
  suspend fun clearAll()

  @Query("SELECT MAX(publishTime) FROM exchange_rate")
  suspend fun getLastUpdateTime(): String?

  @Query("SELECT COUNT(*) FROM exchange_rate")
  suspend fun getCount(): Int

  @Query("SELECT * FROM exchange_rate WHERE name = :name ORDER BY publishTime ASC")
  fun getHistoryRates(name: String): Flow<List<ExchangeRate>>

  @Query(
    """
    SELECT * FROM exchange_rate
    WHERE substr(publishTime, 1, 10) = (
      SELECT MAX(substr(publishTime, 1, 10)) FROM exchange_rate
      WHERE substr(publishTime, 1, 10) < (
        SELECT MAX(substr(publishTime, 1, 10)) FROM exchange_rate
      )
    )
    GROUP BY name
    ORDER BY name
    """
  )
  suspend fun getPreviousDayRates(): List<ExchangeRate>

  @Query("DELETE FROM exchange_rate WHERE publishTime < :cutoffTime")
  suspend fun deleteOlderThan(cutoffTime: String)
}
