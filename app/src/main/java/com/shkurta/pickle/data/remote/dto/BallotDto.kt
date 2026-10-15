package com.shkurta.pickle.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BallotDto(
    @SerialName("poll_id") val pollId: String,
    @SerialName("voter_id") val voterId: String,
    @SerialName("option_ids") val optionIds: List<String>,
    @SerialName("updated_at") val updatedAt: String,
)
