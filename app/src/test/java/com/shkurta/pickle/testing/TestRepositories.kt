package com.shkurta.pickle.testing

import com.shkurta.pickle.domain.model.*
import com.shkurta.pickle.domain.repository.AuthRepository
import com.shkurta.pickle.domain.repository.NotificationRepository
import com.shkurta.pickle.domain.repository.PollRepository
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

fun testPoll(id: String = "poll", creatorId: String = "creator", status: PollStatus = PollStatus.OPEN): Poll =
    Poll(
        id = id, question = "Dinner?", creatorId = creatorId,
        options = listOf(
            PollOption("a", "Pizza", OptionStatus.APPROVED, createdAt = Instant.EPOCH),
            PollOption("b", "Pasta", OptionStatus.APPROVED, createdAt = Instant.EPOCH),
        ),
        settings = PollSettings(allowSuggestions = true), status = status, createdAt = Instant.EPOCH,
    )

class FakeAuthRepository : AuthRepository {
    override var currentUserId: String? = "creator"
    override var displayName: String? = "Alex"
    override var avatarUrl: String? = "avatar"
    override val isAuthenticated = MutableStateFlow(true)
    var result: Result<Unit> = Result.success(Unit)
    var signInCalls = 0
    var signUpCalls = 0
    var signOutCalls = 0
    var lastEmail: String? = null
    var lastName: String? = null
    var lastAvatar: String? = null

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        signInCalls++
        lastEmail = email
        return result
    }
    override suspend fun signUp(email: String, password: String, displayName: String, avatarUrl: String): Result<Unit> {
        signUpCalls++
        lastEmail = email
        lastName = displayName
        lastAvatar = avatarUrl
        return result
    }
    override suspend fun signOut() {
        signOutCalls++
        currentUserId = null
        isAuthenticated.value = false
    }
}

class FakePollRepository : PollRepository {
    override val polls = MutableStateFlow(listOf(testPoll()))
    override val myVotes = MutableStateFlow<Map<String, String>>(emptyMap())
    var createCalls = 0
    var refreshCalls = 0
    var lastCreator: String? = null
    var lastSuggestionUser: String? = null
    var lastDeadline: Instant? = null
    var lastExpectedVoters: Int? = null
    var lastModeration: Triple<String, String, Boolean>? = null
    var lastClosedPoll: String? = null

    override suspend fun createPoll(
        question: String, optionTexts: List<String>, settings: PollSettings,
        closesAt: Instant?, expectedVoters: Int?, creatorId: String,
    ): Poll {
        createCalls++
        lastCreator = creatorId
        lastDeadline = closesAt
        lastExpectedVoters = expectedVoters
        return testPoll("created", creatorId).copy(question = question, settings = settings)
    }
    override suspend fun refresh() { refreshCalls++ }
    override suspend fun vote(pollId: String, optionId: String) {
        myVotes.value = myVotes.value + (pollId to optionId)
    }
    override suspend fun suggestOption(pollId: String, text: String, userId: String): OptionStatus {
        lastSuggestionUser = userId
        return OptionStatus.PENDING
    }
    override suspend fun moderate(pollId: String, optionId: String, approve: Boolean) {
        lastModeration = Triple(pollId, optionId, approve)
    }
    override suspend fun close(pollId: String) { lastClosedPoll = pollId }
}

class FakeNotificationRepository : NotificationRepository {
    override val notifications = MutableStateFlow(listOf(
        PollNotification("one", NotificationType.POLL_CREATED, "poll", "Dinner?", createdAt = Instant.EPOCH),
        PollNotification("two", NotificationType.POLL_CLOSED, "other", "Lunch?", createdAt = Instant.EPOCH),
    ))
    var refreshCalls = 0
    override suspend fun refresh() { refreshCalls++ }
    override suspend fun add(type: NotificationType, poll: Poll, detail: String?) {}
    override suspend fun markRead(id: String) {
        notifications.value = notifications.value.map { if (it.id == id) it.copy(isRead = true) else it }
    }
    override suspend fun markAllRead() {
        notifications.value = notifications.value.map { it.copy(isRead = true) }
    }
}
