package com.cintory.reno.ext

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Created by Cintory on 2025/8/12 14:43
 * Email：Cintory@gmail.com
 */

fun Long.timeStamp2Date(
  pattern: String = "yyyy-MM-dd hh:mm:ss a zzz",
  local: Locale = Locale.getDefault()
): String {
  val sdf = SimpleDateFormat(pattern, local)
  return sdf.format(this)
}

fun String.date2TimeStamp(
  pattern: String = "yyyy-MM-dd hh:mm:ss a zzz",
  local: Locale = Locale.getDefault()
): Long {
  if (isNullOrEmpty()) return 0
  val date = SimpleDateFormat(pattern, local).parse(this)
  return date.time
}

fun Long.getTimeDiffFormatted(
  end: Long,
  format: String = "{D}天{H}小时{M}分{S}秒"
): String {
  val diffMillis = kotlin.math.abs(end - this)

  val days = diffMillis / (1000 * 60 * 60 * 24)
  val hours = (diffMillis / (1000 * 60 * 60)) % 24
  val minutes = (diffMillis / (1000 * 60)) % 60
  val seconds = (diffMillis / 1000) % 60

  return format
    .replace("{D}", days.toString())
    .replace("{H}", hours.toString())
    .replace("{M}", minutes.toString())
    .replace("{S}", seconds.toString())
}