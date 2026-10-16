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
    override val session = MutableStateFlow(com.shkurta.pickle.domain.repository.AuthSession(true, "creator"))
    override val isAuthenticated = MutableStateFlow(true)
    var failure: Exception? = null
    var gate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
    var result: Result<Unit> = Result.success(Unit)
    var signInCalls = 0
    var signUpCalls = 0
    var signOutCalls = 0
    var lastEmail: String? = null
    var lastName: String? = null
    var lastAvatar: String? = null
    var lastLanguage: String? = null

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        signInCalls++
        gate?.await()
        failure?.let { throw it }
        lastEmail = email
        return result
    }
    override suspend fun signUp(email: String, password: String, displayName: String, avatarUrl: String, language: String): Result<Unit> {
        signUpCalls++
        gate?.await()
        failure?.let { throw it }
        lastEmail = email
        lastName = displayName
        lastAvatar = avatarUrl
        lastLanguage = language
        return result
    }
    override suspend fun signOut() {
        signOutCalls++
        failure?.let { throw it }
        currentUserId = null
        session.value = com.shkurta.pickle.domain.repository.AuthSession(true)
        isAuthenticated.value = false
    }
}

class FakePollRepository : PollRepository {
    override val polls = MutableStateFlow(listOf(testPoll()))
    override val myVotes = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    var failure: Exception? = null
    var refreshFailure: Exception? = null
    var gate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
    val requestIds = mutableListOf<String>()
    private suspend fun before() { gate?.await(); failure?.let { throw it } }
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
        requestId: String,
        templateId: String?,
    ): Poll {
        createCalls++
        requestIds += requestId
        before()
        lastCreator = creatorId
        lastDeadline = closesAt
        lastExpectedVoters = expectedVoters
        return testPoll("created", creatorId).copy(question = question, settings = settings)
    }
    override suspend fun refresh() { refreshCalls++; refreshFailure?.let { throw it } }
    override suspend fun vote(pollId: String, optionIds: Set<String>) {
        before()
        myVotes.value = myVotes.value + (pollId to optionIds)
    }
    override suspend fun suggestOption(pollId: String, text: String, userId: String): OptionStatus {
        before()
        lastSuggestionUser = userId
        return OptionStatus.PENDING
    }
    override suspend fun moderate(pollId: String, optionId: String, approve: Boolean) {
        before()
        lastModeration = Triple(pollId, optionId, approve)
    }
    override suspend fun close(pollId: String) { before(); lastClosedPoll = pollId }
}

class FakeNotificationRepository : NotificationRepository {
    var failure: Exception? = null
    var refreshFailure: Exception? = null
    override val notifications = MutableStateFlow(listOf(
        PollNotification("one", NotificationType.POLL_CREATED, "poll", "Dinner?", createdAt = Instant.EPOCH),
        PollNotification("two", NotificationType.POLL_CLOSED, "other", "Lunch?", createdAt = Instant.EPOCH),
    ))
    var refreshCalls = 0
    override suspend fun refresh() { refreshCalls++; refreshFailure?.let { throw it } }
    override suspend fun add(type: NotificationType, poll: Poll, detail: String?) {}
    override suspend fun markRead(id: String) {
        failure?.let { throw it }
        notifications.value = notifications.value.map { if (it.id == id) it.copy(isRead = true) else it }
    }
    override suspend fun markAllRead() {
        failure?.let { throw it }
        notifications.value = notifications.value.map { it.copy(isRead = true) }
    }
}

class FakeTemplateRepository : com.shkurta.pickle.domain.repository.TemplateRepository {
    override val templates=MutableStateFlow<List<PollTemplate>>(emptyList())
    var failure: Exception?=null
    var gate:kotlinx.coroutines.CompletableDeferred<Unit>?=null
    var refreshCalls=0
    var saveCalls=0
    override suspend fun refresh() { refreshCalls++;gate?.await();failure?.let {throw it} }
    override suspend fun save(pollId:String,name:String,icon:String,schedule:WeeklySchedule?):String {
        saveCalls++;gate?.await();failure?.let {throw it};return "template"
    }
}
