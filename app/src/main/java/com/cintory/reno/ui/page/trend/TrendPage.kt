package com.cintory.reno.ui.page.trend

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.cintory.reno.data.model.ExchangeRate
import com.cintory.reno.viewmodel.ExchangeRateViewModel

/**
 * Created by Cintory on 2026/5/28 10:00
 * Email：Cintory@gmail.com
 */

enum class TimeRange(val label: String, val days: Int) {
  WEEK("7天", 7),
  MONTH("1月", 30),
  THREE_MONTHS("3月", 90),
  YEAR("1年", 365),
}

data class DailyRate(
  val date: String,
  val price: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendPage(
  currencyName: String,
  navController: NavHostController,
  viewModel: ExchangeRateViewModel = hiltViewModel(),
) {
  val historyRates by viewModel.getHistoryRates(currencyName).collectAsState(initial = emptyList())

  var selectedRange by rememberSaveable { mutableStateOf(TimeRange.MONTH) }

  val allDailyRates = remember(historyRates) {
    historyRates
      .mapNotNull { rate ->
        val price = rate.conversionPrice.toDoubleOrNull()
          ?: rate.fSellPrice.toDoubleOrNull()
          ?: return@mapNotNull null
        val date = rate.publishTime.split(" ").firstOrNull() ?: return@mapNotNull null
        date to price
      }
      .groupBy { it.first }
      .map { (date, entries) -> DailyRate(date, entries.last().second) }
      .sortedBy { it.date }
  }

  val dailyRates = remember(allDailyRates, selectedRange) {
    allDailyRates.takeLast(selectedRange.days)
  }

  Scaffold(topBar = {
    TopAppBar(
      title = { Text(currencyName, style = MaterialTheme.typography.titleLarge) },
      navigationIcon = {
        IconButton(onClick = { navController.popBackStack() }) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
      }
    )
  }) { paddingValues ->
    if (dailyRates.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator()
      }
    } else {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "中行折算价走势",
            style = MaterialTheme.typography.titleMedium
          )
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TimeRange.entries.forEach { range ->
              FilterChip(
                selected = selectedRange == range,
                onClick = { selectedRange = range },
                label = { Text(range.label, style = MaterialTheme.typography.labelSmall) }
              )
            }
          }
        }

        TrendChart(
          dailyRates = dailyRates,
          modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        val latest = historyRates.lastOrNull()
        if (latest != null) {
          LatestRateDetail(latest)
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun TrendChart(
  dailyRates: List<DailyRate>,
  modifier: Modifier = Modifier,
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val outlineColor = MaterialTheme.colorScheme.outlineVariant
  val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant
  val surfaceColor = MaterialTheme.colorScheme.inverseSurface
  val onSurfaceInverseColor = MaterialTheme.colorScheme.inverseOnSurface
  val highColor = MaterialTheme.colorScheme.error
  val lowColor = Color(0xFF4CAF50)
  val density = LocalDensity.current
  val labelSizePx = with(density) { 10.sp.toPx() }
  val tooltipTextSizePx = with(density) { 11.sp.toPx() }

  var selectedIndex by remember(dailyRates) { mutableStateOf(-1) }

  val prices = remember(dailyRates) { dailyRates.map { it.price } }
  val minPrice = remember(prices) { prices.min() }
  val maxPrice = remember(prices) { prices.max() }
  val priceRange = remember(minPrice, maxPrice) { if (maxPrice - minPrice < 0.01) 1.0 else maxPrice - minPrice }
  val yPadding = remember(priceRange) { priceRange * 0.1 }
  val yMin = remember(minPrice, yPadding) { minPrice - yPadding }
  val yMax = remember(maxPrice, yPadding) { maxPrice + yPadding }
  val yRange = remember(yMin, yMax) { yMax - yMin }

  val paddingLeft = remember(yMin, yMax, yRange, labelSizePx) {
    val paint = android.graphics.Paint().apply {
      textSize = labelSizePx
      isAntiAlias = true
    }
    val maxLabelWidth = (0..4).maxOf { i ->
      val value = yMax - (i.toFloat() / 4) * yRange
      paint.measureText(String.format("%.2f", value))
    }
    maxLabelWidth + 20f
  }
  val paddingRight = 12f

  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
  ) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 16.dp)
        .pointerInput(dailyRates, paddingLeft) {
          detectDragGestures(
            onDragStart = { offset ->
              selectedIndex = findNearestIndex(offset.x, dailyRates.size, paddingLeft, size.width - paddingLeft - paddingRight)
            },
            onDrag = { change, _ ->
              change.consume()
              selectedIndex = findNearestIndex(change.position.x, dailyRates.size, paddingLeft, size.width - paddingLeft - paddingRight)
            },
            onDragEnd = { selectedIndex = -1 },
            onDragCancel = { selectedIndex = -1 }
          )
        }
    ) {
      if (dailyRates.size < 2) {
        return@Canvas
      }

      val paddingBottom = 48f
      val paddingTop = 12f

      val chartWidth = size.width - paddingLeft - paddingRight
      val chartHeight = size.height - paddingTop - paddingBottom

      drawGridAndLabels(
        dailyRates = dailyRates,
        yMin = yMin,
        yMax = yMax,
        yRange = yRange,
        paddingLeft = paddingLeft,
        paddingTop = paddingTop,
        paddingRight = paddingRight,
        chartWidth = chartWidth,
        chartHeight = chartHeight,
        gridColor = outlineColor,
        labelColor = onSurfaceColor,
        labelSizePx = labelSizePx
      )

      val path = Path()
      val points = dailyRates.mapIndexed { index, rate ->
        val x = paddingLeft + (index.toFloat() / (dailyRates.size - 1)) * chartWidth
        val y = paddingTop + ((yMax - rate.price) / yRange).toFloat() * chartHeight
        Offset(x, y)
      }

      points.forEachIndexed { index, point ->
        if (index == 0) path.moveTo(point.x, point.y)
        else path.lineTo(point.x, point.y)
      }

      drawPath(path, primaryColor, style = Stroke(width = 3f))

      if (dailyRates.size <= 60) {
        points.forEach { point ->
          drawCircle(Color.White, radius = 6f, center = point)
          drawCircle(primaryColor, radius = 4.5f, center = point)
        }
      }

      val maxIndex = prices.indices.maxByOrNull { prices[it] } ?: 0
      val minIndex = prices.indices.minByOrNull { prices[it] } ?: 0

      drawHighLowMarker(
        point = points[maxIndex],
        label = String.format("%.2f", prices[maxIndex]),
        color = highColor,
        above = true,
        paddingLeft = paddingLeft,
        chartWidth = chartWidth,
        labelSizePx = labelSizePx
      )

      if (maxIndex != minIndex) {
        drawHighLowMarker(
          point = points[minIndex],
          label = String.format("%.2f", prices[minIndex]),
          color = lowColor,
          above = false,
          paddingLeft = paddingLeft,
          chartWidth = chartWidth,
          labelSizePx = labelSizePx
        )
      }

      if (selectedIndex in points.indices) {
        val point = points[selectedIndex]
        val rate = dailyRates[selectedIndex]

        drawLine(
          onSurfaceColor.copy(alpha = 0.5f),
          start = Offset(point.x, paddingTop),
          end = Offset(point.x, paddingTop + chartHeight),
          strokeWidth = 1.5f
        )

        drawCircle(Color.White, radius = 7f, center = point)
        drawCircle(primaryColor, radius = 5f, center = point)

        val tooltipText = "${rate.date}  ${String.format("%.2f", rate.price)}"
        val paint = android.graphics.Paint().apply {
          textSize = tooltipTextSizePx
          isAntiAlias = true
          color = onSurfaceInverseColor.hashCode()
          typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val textWidth = paint.measureText(tooltipText)
        val tooltipW = textWidth + 24f
        val tooltipH = tooltipTextSizePx + 20f

        var tooltipX = point.x - tooltipW / 2
        if (tooltipX < paddingLeft) tooltipX = paddingLeft
        if (tooltipX + tooltipW > paddingLeft + chartWidth) tooltipX = paddingLeft + chartWidth - tooltipW
        val tooltipY = point.y - tooltipH - 12f

        val bgPaint = android.graphics.Paint().apply {
          color = surfaceColor.hashCode()
          isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawRoundRect(
          tooltipX, tooltipY, tooltipX + tooltipW, tooltipY + tooltipH,
          12f, 12f, bgPaint
        )

        drawContext.canvas.nativeCanvas.drawText(
          tooltipText,
          tooltipX + 12f,
          tooltipY + tooltipTextSizePx + 6f,
          paint
        )
      }
    }
  }
}

private fun DrawScope.drawHighLowMarker(
  point: Offset,
  label: String,
  color: Color,
  above: Boolean,
  paddingLeft: Float,
  chartWidth: Float,
  labelSizePx: Float,
) {
  drawCircle(Color.White, radius = 7f, center = point)
  drawCircle(color, radius = 5f, center = point)

  val paint = android.graphics.Paint().apply {
    textSize = labelSizePx
    isAntiAlias = true
    this.color = color.hashCode()
    typeface = android.graphics.Typeface.DEFAULT_BOLD
    textAlign = android.graphics.Paint.Align.CENTER
  }
  val textY = if (above) point.y - 12f else point.y + labelSizePx + 10f
  var textX = point.x
  val textWidth = paint.measureText(label) / 2
  if (textX - textWidth < paddingLeft) textX = paddingLeft + textWidth
  if (textX + textWidth > paddingLeft + chartWidth) textX = paddingLeft + chartWidth - textWidth

  drawContext.canvas.nativeCanvas.drawText(label, textX, textY, paint)
}

private fun findNearestIndex(touchX: Float, count: Int, paddingLeft: Float, chartWidth: Float): Int {
  if (count < 2) return -1
  val ratio = ((touchX - paddingLeft) / chartWidth).coerceIn(0f, 1f)
  return (ratio * (count - 1)).toInt().coerceIn(0, count - 1)
}

private fun DrawScope.drawGridAndLabels(
  dailyRates: List<DailyRate>,
  yMin: Double,
  yMax: Double,
  yRange: Double,
  paddingLeft: Float,
  paddingTop: Float,
  paddingRight: Float,
  chartWidth: Float,
  chartHeight: Float,
  gridColor: Color,
  labelColor: Color,
  labelSizePx: Float,
) {
  val gridLines = 4
  val paint = android.graphics.Paint().apply {
    color = labelColor.hashCode()
    textSize = labelSizePx
    isAntiAlias = true
    textAlign = android.graphics.Paint.Align.RIGHT
  }

  for (i in 0..gridLines) {
    val ratio = i.toFloat() / gridLines
    val y = paddingTop + ratio * chartHeight
    val value = yMax - ratio * yRange

    drawLine(
      gridColor,
      start = Offset(paddingLeft, y),
      end = Offset(size.width - paddingRight, y),
      strokeWidth = 1f
    )

    drawContext.canvas.nativeCanvas.drawText(
      String.format("%.2f", value),
      paddingLeft - 10f,
      y + labelSizePx / 3,
      paint
    )
  }

  val maxXLabels = 5
  val step = if (dailyRates.size <= maxXLabels) 1
  else dailyRates.size / (maxXLabels - 1)

  paint.textAlign = android.graphics.Paint.Align.CENTER
  val xLabelY = paddingTop + chartHeight + labelSizePx + 16f
  for (i in dailyRates.indices step step) {
    val x = paddingLeft + (i.toFloat() / (dailyRates.size - 1)) * chartWidth
    val label = dailyRates[i].date.substring(5)

    drawContext.canvas.nativeCanvas.drawText(
      label,
      x,
      xLabelY,
      paint
    )
  }

  if (step > 1 && (dailyRates.size - 1) % step != 0) {
    val x = paddingLeft + chartWidth
    val label = dailyRates.last().date.substring(5)
    drawContext.canvas.nativeCanvas.drawText(label, x, xLabelY, paint)
  }
}

@Composable
private fun LatestRateDetail(rate: ExchangeRate) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "最新汇率",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(8.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(8.dp))
      DetailRow("现汇买入", rate.fBuyPrice)
      DetailRow("现钞买入", rate.mBuyPrice)
      DetailRow("现汇卖出", rate.fSellPrice)
      DetailRow("现钞卖出", rate.mSellPrice)
      DetailRow("中行折算价", rate.conversionPrice)
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = rate.publishTime,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun DetailRow(label: String, value: String) {
  if (value.isEmpty()) return
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(1f)
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium
    )
  }
}
