package com.shkurta.pickle.ui.auth

import java.net.URLEncoder
import java.util.UUID

object DiceBear {
    private const val BASE_URL = "https://api.dicebear.com/9.x/notionists/svg"

    // Avatar circle fills from the design: mustard, teal-soft, peach, lavender, sky, cream, sand, blush
    private const val BACKGROUNDS = "f3c24a,cde7e0,f6c9b5,d9c7f0,bfe0f5,fbe3c0,e6d5c3,fbdde3"

    fun avatarUrl(seed: String): String =
        "$BASE_URL?seed=${encodeSeed(seed)}&backgroundColor=$BACKGROUNDS&backgroundType=solid"

    // Keep the same query escaping as Uri.encode without depending on Android.
    private fun encodeSeed(seed: String): String =
        URLEncoder.encode(seed, "UTF-8")
            .replace("+", "%20")
            .replace("%21", "!")
            .replace("%7E", "~")
            .replace("%27", "'")
            .replace("%28", "(")
            .replace("%29", ")")

    fun randomSeeds(count: Int = 10): List<String> =
        List(count) { UUID.randomUUID().toString().take(8) }
}
