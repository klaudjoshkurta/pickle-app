package com.shkurta.pickle.data

import com.shkurta.pickle.data.auth.SessionGuard
import com.shkurta.pickle.data.remote.SupabaseTemplateRepository
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
class RemoteTemplatesTest {
    @get:Rule val dispatcher=MainDispatcherRule()
    private val auth=FakeAuthRepository()
    private val headers=headersOf(HttpHeaders.ContentType,"application/json")
    private val fixture="""[{"id":"template","source_poll_id":"poll","name":"Film","icon":"film","question":"Film?","option_texts":["A","B"],"allow_suggestions":true,"require_approval":true,"show_results_before_voting":false,"allow_vote_change":true,"voting_mode":"SINGLE","rounds_count":1,"schedule_weekday":5,"schedule_time":"20:00","timezone":"Europe/Athens"}]"""

    @Test fun offlineTemplatesNeverUseNetwork()=runTest {
        auth.currentUserId=null;auth.session.value=AuthSession(true)
        val client=createSupabaseClient("http://localhost","test") {install(Postgrest);httpEngine=MockEngine {error("Unexpected network")}}
        try {
            val repo=SupabaseTemplateRepository(client.postgrest,auth,SessionGuard(auth))
            repo.refresh();assertTrue(repo.templates.value.isEmpty())
            assertTrue(runCatching {repo.save("poll","Film","film",null)}.exceptionOrNull() is SignInRequiredException)
        } finally {client.close()}
    }
    @Test fun committedSaveCarriesIdAndMalformedRecordsPreserveLastGoodSnapshot()=runTest {
        var fail=false;var malformed=false;var saves=0
        val client=createSupabaseClient("http://localhost","test") {install(Postgrest);httpEngine=MockEngine {request->
            when {
                request.url.encodedPath.endsWith("/rpc/save_poll_template")-> {
                    saves++
                    val body=Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
                    assertFalse(body.containsKey("owner_id"));assertEquals(JsonNull,body["p_weekday"])
                    respond("\"template\"",headers=headers)
                }
                fail->respond("{}",HttpStatusCode.ServiceUnavailable,headers)
                else->respond(if(malformed) fixture.replace("Europe/Athens","Invalid/Zone") else fixture,headers=headers)
            }
        }}
        try {
            val repo=SupabaseTemplateRepository(client.postgrest,auth,SessionGuard(auth))
            repo.refresh();val before=repo.templates.value
            malformed=true;assertTrue(runCatching {repo.refresh()}.exceptionOrNull() is InvalidRemoteDataException)
            assertEquals(before,repo.templates.value)
            fail=true
            assertEquals("template",(runCatching {repo.save("poll","Film","film",null)}.exceptionOrNull() as SavedRefreshException).savedId)
            fail=false;malformed=false;repo.refresh();assertEquals(1,saves)
        } finally {client.close()}
    }
    @Test fun switchingAccountsCancelsAnInFlightTemplateRefreshAndClearsData()=runTest {
        val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>();var slow=false
        val client=createSupabaseClient("http://localhost","test") {install(Postgrest);httpEngine=MockEngine {
            if(slow){entered.complete(Unit);withContext(NonCancellable){release.await()}}
            respond(fixture,headers=headers)
        }}
        try {
            val repo=SupabaseTemplateRepository(client.postgrest,auth,SessionGuard(auth));repo.refresh()
            slow=true;val request=backgroundScope.async {repo.refresh()};runCurrent();entered.await()
            auth.session.value=AuthSession(true,"other");runCurrent();assertTrue(repo.templates.value.isEmpty())
            release.complete(Unit);runCurrent();assertTrue(request.isCancelled);assertTrue(repo.templates.value.isEmpty())
        } finally {client.close()}
    }
}
