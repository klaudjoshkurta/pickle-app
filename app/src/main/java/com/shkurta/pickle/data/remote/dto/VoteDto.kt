package com.shkurta.pickle.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoteDto(
    val id: String,
    @SerialName("poll_id") val pollId: String,
    @SerialName("voter_id") val voterId: String,
    @SerialName("option_id") val optionId: String,
    @SerialName("created_at") val createdAt: String,
)
