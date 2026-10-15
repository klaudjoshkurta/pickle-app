package com.shkurta.pickle.data.remote

import com.shkurta.pickle.data.auth.AccountCache
import com.shkurta.pickle.data.auth.SessionGuard
import com.shkurta.pickle.data.remote.dto.*
import com.shkurta.pickle.data.sample.SampleNotifications
import com.shkurta.pickle.domain.model.*
import com.shkurta.pickle.domain.repository.*
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseNotificationRepository @Inject constructor(
    private val postgrest: Postgrest,
    private val authRepository: AuthRepository,
    private val guard: SessionGuard,
) : NotificationRepository {
    private val cache = AccountCache(authRepository, emptyList<PollNotification>(), SampleNotifications.all)
    override val notifications = cache.state

    override suspend fun refresh() {
        if (guard.ready().userId == null) return
        guard.authenticated { user ->
            val session = authRepository.session.value
            val result = postgrest.from("notifications").select {
                filter { eq("user_id", user) }
                order("created_at", Order.DESCENDING)
            }.decodeList<NotificationDto>()
            val mapped = try { result.map { it.toDomain() } }
                catch (e: RuntimeException) { throw InvalidRemoteDataException() }
            cache.publish(session, mapped)
        }
    }

    override suspend fun add(type: NotificationType, poll: Poll, detail: String?) = guard.authenticated { user ->
        postgrest.from("notifications").insert(CreateNotificationDto(user, type.name, poll.id, poll.question, detail))
        refreshAfterWrite()
    }

    override suspend fun markRead(id: String) {
        if (guard.ready().userId == null) return // Sample notifications are read-only.
        guard.authenticated { user ->
            postgrest.from("notifications").update({ set("is_read", true) }) {
                filter { eq("id", id); eq("user_id", user) }
            }
            refreshAfterWrite()
        }
    }

    override suspend fun markAllRead() {
        if (guard.ready().userId == null) return
        guard.authenticated { user ->
            postgrest.from("notifications").update({ set("is_read", true) }) {
                filter { eq("user_id", user); eq("is_read", false) }
            }
            refreshAfterWrite()
        }
    }

    private suspend fun refreshAfterWrite() {
        try { refresh() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { throw SavedRefreshException() }
    }
}
