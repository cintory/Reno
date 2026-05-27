package com.cintory.reno.data.model

import androidx.room.Entity

/**
 * Created by Cintory on 2026/2/5 11:32
 * Email：Cintory@gmail.com
 */
@Entity(tableName = "exchange_rate", primaryKeys = ["name", "publishTime"])
data class ExchangeRate(
  val name: String,
  val fBuyPrice: String,
  val mBuyPrice: String,
  val fSellPrice: String,
  val mSellPrice: String,
  val conversionPrice: String,
  val publishTime: String
)
