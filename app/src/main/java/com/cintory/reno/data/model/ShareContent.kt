package com.cintory.reno.data.model

import android.net.Uri

/**
 * Created by Cintory on 2025/7/31 11:05
 * Email：Cintory@gmail.com
 */
data class ShareContent(
  val text: String = "",
  val images: List<Uri> = ArrayList()
) {
}