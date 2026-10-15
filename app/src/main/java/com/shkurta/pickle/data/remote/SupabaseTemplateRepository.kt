package com.shkurta.pickle.data.remote

import com.shkurta.pickle.data.auth.AccountCache
import com.shkurta.pickle.data.auth.SessionGuard
import com.shkurta.pickle.domain.model.*
import com.shkurta.pickle.domain.repository.*
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class TemplateDto(
    val id: String,
    @SerialName("source_poll_id") val sourcePollId: String,
    val name: String, val icon: String, val question: String,
    @SerialName("option_texts") val options: List<String>,
    @SerialName("allow_suggestions") val suggestions: Boolean,
    @SerialName("require_approval") val approval: Boolean,
    @SerialName("show_results_before_voting") val showResults: Boolean,
    @SerialName("allow_vote_change") val change: Boolean,
    @SerialName("voting_mode") val mode: String,
    @SerialName("rounds_count") val rounds: Int,
    @SerialName("schedule_weekday") val weekday: Int? = null,
    @SerialName("schedule_time") val time: String? = null,
    val timezone: String,
) {
    fun toDomain(): PollTemplate {
        require(name.isNotBlank() && rounds>0 && icon in PollStarter.entries.map {it.icon})
        require(options.size>=2 && distinctOptionTexts(options).size==options.size)
        java.time.ZoneId.of(timezone)
        require((weekday==null)==(time==null))
        if(weekday!=null) {java.time.DayOfWeek.of(weekday);java.time.LocalTime.parse(time)}
        return PollTemplate(id, sourcePollId, name, icon, question, options,
        PollSettings(VotingMode.valueOf(mode), suggestions, approval, showResults, change), rounds,
        if (weekday != null && time != null) WeeklySchedule(weekday, time, timezone) else null)
    }
}

@Singleton
class SupabaseTemplateRepository @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: AuthRepository,
    private val guard: SessionGuard,
) : TemplateRepository {
    private val cache = AccountCache(auth, emptyList<PollTemplate>())
    override val templates = cache.state
    override suspend fun refresh() {
        if (guard.ready().userId == null) return
        guard.authenticated { user ->
            val snapshot = auth.session.value
            val result = postgrest.from("poll_templates").select {
                filter { eq("owner_id", user) }; order("updated_at", Order.DESCENDING)
            }.decodeList<TemplateDto>()
            val mapped = try { result.map { it.toDomain() } } catch (e: RuntimeException) { throw InvalidRemoteDataException() }
            cache.publish(snapshot, mapped)
        }
    }

    override suspend fun save(pollId: String, name: String, icon: String, schedule: WeeklySchedule?): String = guard.authenticated {
        val id = postgrest.rpc("save_poll_template", buildJsonObject {
            put("p_poll_id", pollId); put("p_name", name.trim()); put("p_icon", icon)
            put("p_weekday", schedule?.weekday?.let(::JsonPrimitive) ?: JsonNull)
            put("p_local_time", schedule?.time?.let(::JsonPrimitive) ?: JsonNull)
            put("p_timezone", schedule?.timezone ?: ZoneId.systemDefault().id)
        }).decodeAs<String>()
        try { refresh() } catch (e: CancellationException) { throw e } catch (e: Exception) { throw SavedRefreshException(id) }
        id
    }
}
