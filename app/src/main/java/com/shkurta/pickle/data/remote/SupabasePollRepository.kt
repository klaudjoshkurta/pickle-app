package com.shkurta.pickle.data.remote

import com.shkurta.pickle.data.auth.AccountCache
import com.shkurta.pickle.data.auth.SessionGuard
import com.shkurta.pickle.data.remote.dto.*
import com.shkurta.pickle.data.sample.SamplePolls
import com.shkurta.pickle.domain.repository.*
import com.shkurta.pickle.domain.model.*
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabasePollRepository @Inject constructor(
    private val postgrest: Postgrest,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val guard: SessionGuard,
) : PollRepository {
    private val pollCache = AccountCache(authRepository, emptyList<Poll>(), SamplePolls.all)
    private val voteCache = AccountCache(authRepository, emptyMap<String, Set<String>>())
    override val polls = pollCache.state
    override val myVotes = voteCache.state
    private val refreshMutex = Mutex()

    override suspend fun refresh() {
        if (guard.ready().userId == null) return // The demo never calls the database.
        guard.authenticated { refreshPolls() }
    }

    private suspend fun refreshPolls() = refreshMutex.withLock {
        val session = authRepository.session.value
        val userId = session.userId ?: throw SignInRequiredException()
        var result = fetchPolls()
        val expired = result.filter { it.status == "OPEN" && it.closedAt?.let(::parseDatabaseInstant)?.isBefore(Instant.now()) == true }
        for (poll in expired) postgrest.rpc("expire_poll", buildJsonObject { put("p_poll_id", poll.id) })
        if (expired.isNotEmpty()) result = fetchPolls()
        // Parse everything before publishing, so bad rows preserve the last good snapshot.
        val mapped = try { result.map { it.toDomain() } } catch (e: RuntimeException) {
            throw InvalidRemoteDataException()
        }
        val votes = postgrest.from("votes").select { filter { eq("voter_id", userId) } }.decodeList<VoteDto>()
        val ballots = postgrest.from("poll_ballots").select { filter { eq("voter_id", userId) } }.decodeList<BallotDto>()
        pollCache.publish(session, mapped)
        voteCache.publish(session, votes.associate { it.pollId to setOf(it.optionId) } + ballots.associate { it.pollId to it.optionIds.toSet() })
    }

    private suspend fun fetchPolls() = postgrest.from("polls")
        .select(columns = Columns.raw("*, poll_options(*)")) { order("created_at", Order.DESCENDING) }
        .decodeList<PollDto>()

    override suspend fun createPoll(
        question: String, optionTexts: List<String>, settings: PollSettings,
        closesAt: Instant?, expectedVoters: Int?, creatorId: String, requestId: String, templateId: String?,
    ): Poll? = guard.authenticated { userId ->
        if (creatorId != userId) throw SignInRequiredException()
        if (!isValidPollInput(question, optionTexts)) return@authenticated null
        val id = postgrest.rpc("create_poll_v2", buildJsonObject {
            put("p_question", question.trim())
            put("p_options", JsonArray(distinctOptionTexts(optionTexts).map(::JsonPrimitive)))
            put("p_allow_suggestions", settings.allowSuggestions)
            put("p_require_approval", settings.requireApproval)
            put("p_show_results_before_voting", settings.showResultsBeforeVoting)
            put("p_allow_vote_change", settings.allowVoteChange)
            put("p_closes_at", closesAt?.toString()?.let(::JsonPrimitive) ?: JsonNull)
            put("p_voting_mode", settings.votingMode.name)
            put("p_template_id", templateId?.let(::JsonPrimitive) ?: JsonNull)
            put("p_expected_voters", expectedVoters?.let(::JsonPrimitive) ?: JsonNull)
            put("p_request_id", requestId)
        }).decodeAs<String>()
        followUp(id) {
            refreshPolls()
            notificationRepository.refresh() // create_poll already inserted the notification atomically.
        }
        polls.value.firstOrNull { it.id == id } ?: throw SavedRefreshException(id)
    }

    override suspend fun vote(pollId: String, optionIds: Set<String>) = guard.authenticated {
        postgrest.rpc("cast_ballot", buildJsonObject { put("p_poll_id", pollId); put("p_option_ids", JsonArray(optionIds.map(::JsonPrimitive))) })
        followUp { refreshPolls() }
    }

    override suspend fun suggestOption(pollId: String, text: String, userId: String): OptionStatus? = guard.authenticated { currentUser ->
        if (userId != currentUser) throw SignInRequiredException()
        val poll = polls.value.firstOrNull { it.id == pollId } ?: return@authenticated null
        if (!poll.isOpen() || !poll.settings.allowSuggestions || text.isBlank()) return@authenticated null
        val trimmed = text.trim()
        if (poll.options.any { it.status != OptionStatus.REJECTED && it.text.equals(trimmed, true) }) return@authenticated null
        val isCreator = poll.isCreator(currentUser)
        val status = if (poll.settings.requireApproval && !isCreator) OptionStatus.PENDING else OptionStatus.APPROVED
        postgrest.from("poll_options").insert(CreateOptionDto(pollId, trimmed, status.name, currentUser.takeUnless { isCreator }))
        followUp {
            refreshPolls()
            if (status == OptionStatus.PENDING) notificationRepository.add(NotificationType.SUGGESTION_RECEIVED, poll, trimmed)
        }
        status
    }

    override suspend fun moderate(pollId: String, optionId: String, approve: Boolean) = guard.authenticated {
        val poll = polls.value.firstOrNull { it.id == pollId }
        val text = poll?.pendingSuggestions?.firstOrNull { it.id == optionId }?.text
        postgrest.rpc("moderate_option", buildJsonObject {
            put("p_poll_id", pollId); put("p_option_id", optionId); put("p_approve", approve)
        })
        followUp {
            refreshPolls()
            if (poll != null) notificationRepository.add(
                if (approve) NotificationType.SUGGESTION_APPROVED else NotificationType.SUGGESTION_REJECTED, poll, text,
            )
        }
    }

    override suspend fun close(pollId: String) = guard.authenticated {
        postgrest.rpc("close_poll", buildJsonObject { put("p_poll_id", pollId) })
        followUp {
            refreshPolls()
            polls.value.firstOrNull { it.id == pollId }?.let { poll ->
                notificationRepository.add(NotificationType.POLL_CLOSED, poll, when (val outcome = poll.toResults().outcome) {
                    Outcome.NoVotes -> "No votes"
                    is Outcome.Winner -> "Winner: ${outcome.option.text}"
                    is Outcome.Tie -> "Tie: ${outcome.options.joinToString(" · ") { it.text }}"
                })
            }
        }
    }

    private suspend fun followUp(savedId: String? = null, block: suspend () -> Unit) {
        try { block() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { throw SavedRefreshException(savedId) }
    }
}
