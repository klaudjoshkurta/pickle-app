package com.shkurta.pickle.data

import com.shkurta.pickle.data.auth.AccountCache
import com.shkurta.pickle.data.auth.SessionGuard
import com.shkurta.pickle.data.remote.SupabasePollRepository
import com.shkurta.pickle.data.remote.SupabaseNotificationRepository
import com.shkurta.pickle.domain.model.*
import com.shkurta.pickle.domain.repository.AuthSession
import com.shkurta.pickle.testing.*
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteStabilityTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val auth = FakeAuthRepository()
    private val notifications = FakeNotificationRepository()
    private val guard = SessionGuard(auth)
    private val headers = headersOf(HttpHeaders.ContentType, "application/json")
    private val pollJson = """[{"id":"poll","question":"Dinner?","creator_id":"creator",
        "allow_suggestions":true,"require_approval":true,"show_results_before_voting":false,
        "allow_vote_change":true,"status":"OPEN","created_at":"2026-10-15T00:00:00.123456+00:00",
        "poll_options":[{"id":"a","poll_id":"poll","text":"Pizza","status":"APPROVED","created_at":"2026-10-15T00:00:00.123456+00:00"},
        {"id":"b","poll_id":"poll","text":"Pasta","status":"APPROVED","created_at":"2026-10-15T00:00:00.123456+00:00"}]}]"""

    @Test fun offlineDemoDoesNotSendAnyNetworkRequestsAndWritesRequireSignIn() = runTest {
        auth.currentUserId = null; auth.session.value = AuthSession(true)
        var calls = 0
        val client = createSupabaseClient("http://localhost", "test-key") {
            install(Postgrest)
            httpEngine = MockEngine { calls++; error("Demo must not use network") }
        }
        try {
            val repo = SupabasePollRepository(client.postgrest, auth, notifications, guard)
            val notices = SupabaseNotificationRepository(client.postgrest, auth, guard)
            repo.refresh(); notices.refresh(); notices.markRead("sample"); notices.markAllRead()
            assertTrue(repo.polls.value.isNotEmpty())
            assertTrue(notices.notifications.value.isNotEmpty())
            assertTrue(runCatching { repo.vote("poll", "a") }.exceptionOrNull() is SignInRequiredException)
            assertEquals(0, calls)
        } finally { client.close() }
    }

    @Test fun creationUsesTheTransactionalRpcAndSavedFailuresCarryItsId() = runTest {
        val rpcBodies = mutableListOf<JsonObject>()
        var failRead = true
        val client = createSupabaseClient("http://localhost", "test-key") {
            install(Postgrest)
            httpEngine = MockEngine { request ->
                when {
                    request.url.encodedPath.endsWith("/rpc/create_poll") -> {
                        rpcBodies += Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
                        respond("\"poll\"", headers = headers)
                    }
                    request.url.encodedPath.endsWith("/polls") -> if (failRead) respond("{}", HttpStatusCode.ServiceUnavailable, headers)
                        else respond(pollJson, headers = headers)
                    request.url.encodedPath.endsWith("/votes") -> respond("[]", headers = headers)
                    else -> error("Unexpected endpoint: ${request.url.encodedPath}")
                }
            }
        }
        try {
            val repo = SupabasePollRepository(client.postgrest, auth, notifications, guard)
            val first = runCatching { repo.createPoll("Dinner?", listOf("Pizza", "Pasta"), PollSettings(), null, null, "creator", "request") }
            assertEquals("poll", (first.exceptionOrNull() as SavedRefreshException).savedId)
            failRead = false
            val poll = repo.createPoll("Dinner?", listOf("Pizza", "Pasta"), PollSettings(), null, null, "creator", "request")
            assertEquals("poll", poll!!.id)
            assertEquals(2, rpcBodies.size)
            assertEquals(rpcBodies[0]["p_request_id"], rpcBodies[1]["p_request_id"])
            assertEquals(JsonNull, rpcBodies[0]["p_deadline_seconds"])
            assertEquals(2, rpcBodies[0]["p_options"]!!.jsonArray.size)
        } finally { client.close() }
    }

    @Test fun invalidRemoteStatusPreservesTheLastGoodSnapshot() = runTest {
        var bad = false
        val client = createSupabaseClient("http://localhost", "test-key") {
            install(Postgrest)
            httpEngine = MockEngine { request ->
                respond(if (request.url.encodedPath.endsWith("/votes")) "[]"
                    else if (bad) pollJson.replace("\"OPEN\"", "\"UNKNOWN\"") else pollJson, headers = headers)
            }
        }
        try {
            val repo = SupabasePollRepository(client.postgrest, auth, notifications, guard)
            repo.refresh(); val before = repo.polls.value
            bad = true
            assertTrue(runCatching { repo.refresh() }.exceptionOrNull() is InvalidRemoteDataException)
            assertEquals(before, repo.polls.value)
        } finally { client.close() }
    }

    @Test fun accountChangeClearsCachesAndRejectsLateResponses() = runTest {
        val cache = AccountCache(auth, emptyList<String>(), listOf("demo"))
        val snapshot = auth.session.value
        cache.publish(snapshot, listOf("private"))
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val request = backgroundScope.async {
            guard.authenticated {
                started.complete(Unit)
                withContext(NonCancellable) { finish.await() }
                cache.publish(snapshot, listOf("late private data"))
            }
        }
        runCurrent(); started.await()
        auth.session.value = AuthSession(true, "another-account"); runCurrent()
        assertTrue(cache.state.value.isEmpty())
        finish.complete(Unit); runCurrent()
        assertTrue(request.isCancelled)
        assertTrue(cache.state.value.isEmpty())
        auth.session.value = AuthSession(true); runCurrent()
        assertEquals(listOf("demo"), cache.state.value)
    }

    @Test fun requestsWaitForSessionInitialization() = runTest {
        auth.session.value = AuthSession()
        val request = backgroundScope.async { guard.ready() }
        runCurrent(); assertFalse(request.isCompleted)
        auth.session.value = AuthSession(true, "restored-user"); runCurrent()
        assertEquals("restored-user", request.await().userId)
    }
}
