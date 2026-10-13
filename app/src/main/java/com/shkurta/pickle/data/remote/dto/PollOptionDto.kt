package com.shkurta.pickle.data.remote.dto

import com.shkurta.pickle.domain.model.OptionStatus
import com.shkurta.pickle.domain.model.PollOption
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class PollOptionDto(
    val id: String,
    @SerialName("poll_id") val pollId: String,
    val text: String,
    val status: String,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("suggested_by") val suggestedBy: String? = null,
    @SerialName("created_at") val createdAt: String,
) {
    fun toDomain(): PollOption = PollOption(
        id = id,
        text = text,
        status = OptionStatus.valueOf(status),
        voteCount = voteCount,
        suggestedBy = suggestedBy,
        createdAt = Instant.parse(createdAt),
    )
}

@Serializable
data class CreateOptionDto(
    @SerialName("poll_id") val pollId: String,
    val text: String,
    val status: String = "APPROVED",
    @SerialName("suggested_by") val suggestedBy: String? = null,
)
