package com.shkurta.pickle.ui.components

import android.content.Context
import android.content.Intent
import com.shkurta.pickle.domain.model.Poll

const val WEB_BASE_URL = "https://pickle-web-iota.vercel.app"

fun pollLink(pollId: String): String = "$WEB_BASE_URL/p/$pollId"

fun sharePoll(context: Context, poll: Poll) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "${poll.question}\n${pollLink(poll.id)}")
    }
    context.startActivity(Intent.createChooser(intent, null))
}
