package com.shkurta.pickle.domain.repository

import com.shkurta.pickle.domain.model.OptionStatus
import com.shkurta.pickle.domain.model.Poll
import com.shkurta.pickle.domain.model.PollSettings
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

interface PollRepository {
    val polls: StateFlow<List<Poll>>
    val myVotes: StateFlow<Map<String, String>>

    suspend fun createPoll(
        question: String,
        optionTexts: List<String>,
        settings: PollSettings,
        closesAt: Instant?,
        expectedVoters: Int?,
        creatorId: String,
    ): Poll?

    suspend fun refresh()
    suspend fun vote(pollId: String, optionId: String)
    suspend fun suggestOption(pollId: String, text: String, userId: String): OptionStatus?
    suspend fun moderate(pollId: String, optionId: String, approve: Boolean)
    suspend fun close(pollId: String)
}
