package com.shkurta.pickle.domain.model

const val MIN_POLL_OPTIONS = 2

/** Trimmed, non-blank options with case-insensitive duplicates removed. */
fun distinctOptionTexts(texts: List<String>): List<String> =
    texts.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }

fun isValidPollInput(question: String, optionTexts: List<String>): Boolean =
    question.isNotBlank() && distinctOptionTexts(optionTexts).size >= MIN_POLL_OPTIONS
