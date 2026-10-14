package com.shkurta.pickle.ui.auth

import android.net.Uri
import java.util.UUID

object DiceBear {
    private const val BASE_URL = "https://api.dicebear.com/10.x/critters/svg"

    fun avatarUrl(seed: String): String = "$BASE_URL?seed=${Uri.encode(seed)}"

    fun randomSeeds(count: Int = 9): List<String> =
        List(count) { UUID.randomUUID().toString().take(8) }
}
