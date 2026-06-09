package com.cintory.reno.data.repository

import com.cintory.reno.data.local.ExchangeRateDao
import com.cintory.reno.data.model.ExchangeRate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Cintory on 2026/5/26 15:00
 * Email：Cintory@gmail.com
 */
@Singleton
class ExchangeRateRepository @Inject constructor(
  private val dao: ExchangeRateDao
) {
  private val baseUrl = "https://www.boc.cn/sourcedb/whpj/"
  private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

  fun getLatestRates(): Flow<List<ExchangeRate>> = dao.getLatestRates()

  fun getAllRates(): Flow<List<ExchangeRate>> = dao.getAllRates()

  fun getHistoryRates(name: String): Flow<List<ExchangeRate>> = dao.getHistoryRates(name)

  suspend fun getLastUpdateTime(): String? = dao.getLastUpdateTime()

  suspend fun hasCachedData(): Boolean = dao.getCount() > 0

  suspend fun getChangePercentages(): Map<String, Double> = withContext(Dispatchers.IO) {
    val previousRates = dao.getPreviousDayRates()
    if (previousRates.isEmpty()) return@withContext emptyMap()

    val latestRates = dao.getLatestRatesOnce()
    val prevMap = previousRates.associateBy { it.name }

    latestRates.mapNotNull { current ->
      val prev = prevMap[current.name] ?: return@mapNotNull null
      val currentPrice = getPrice(current) ?: return@mapNotNull null
      val prevPrice = getPrice(prev) ?: return@mapNotNull null
      if (prevPrice == 0.0) return@mapNotNull null
      current.name to (currentPrice - prevPrice) / prevPrice * 100
    }.toMap()
  }

  private fun getPrice(rate: ExchangeRate): Double? {
    return rate.conversionPrice.toDoubleOrNull() ?: rate.fSellPrice.toDoubleOrNull()
  }

  suspend fun refreshRates() = withContext(Dispatchers.IO) {
    Timber.d("refreshRates: fetching first page")
    val firstPageDoc = fetchDocument("${baseUrl}index.html")
    val totalPages = getTotalPages(firstPageDoc)
    Timber.d("refreshRates: total pages = %d", totalPages)

    val allRates = mutableListOf<ExchangeRate>()
    allRates.addAll(parsePage(firstPageDoc))

    for (page in 1 until totalPages) {
      delay(500)
      val url = "${baseUrl}index_$page.html"
      Timber.d("refreshRates: fetching page %d", page + 1)
      val doc = fetchDocument(url)
      allRates.addAll(parsePage(doc))
    }

    Timber.d("refreshRates: inserting %d rates into DB", allRates.size)
    dao.insertAll(allRates)
    cleanOldData()
    Timber.d("refreshRates: done")
  }

  private suspend fun cleanOldData() {
    val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -365)
    val cutoff = sdf.format(calendar.time)
    dao.deleteOlderThan(cutoff)
  }

  private fun fetchDocument(url: String): Document {
    return Jsoup.connect(url)
      .userAgent(userAgent)
      .timeout(15000)
      .get()
  }

  private fun getTotalPages(doc: Document): Int {
    val scriptContent = doc.select("script").html()
    val regex = "var m_nPageCount = (\\d+);".toRegex()
    val match = regex.find(scriptContent)
    if (match != null) {
      return match.groupValues[1].toInt()
    }

    val pageText = doc.select(".turn_page").text()
    val textRegex = "共(\\d+)页".toRegex()
    return textRegex.find(pageText)?.groupValues?.get(1)?.toInt() ?: 1
  }

  private fun parsePage(doc: Document): List<ExchangeRate> {
    val table = doc.select("table:contains(货币名称)").last() ?: return emptyList()
    return table.select("tr").drop(1).mapNotNull { row ->
      val cols = row.select("td")
      if (cols.size >= 7) {
        ExchangeRate(
          name = cols[0].text().trim(),
          fBuyPrice = cols[1].text().trim(),
          mBuyPrice = cols[2].text().trim(),
          fSellPrice = cols[3].text().trim(),
          mSellPrice = cols[4].text().trim(),
          conversionPrice = cols[5].text().trim(),
          publishTime = "${cols[6].text().trim()} ${cols.getOrNull(7)?.text()?.trim() ?: ""}"
        )
      } else null
    }
  }
}
