package com.shkurta.pickle.data

import com.shkurta.pickle.data.sample.SamplePolls
import com.shkurta.pickle.domain.model.NotificationType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRepositoryTest {

    private val repository = InMemoryNotificationRepository()

    @Test
    fun add_prependsUnreadNotificationForPoll() = runBlocking {
        val sizeBefore = repository.notifications.value.size
        repository.add(NotificationType.POLL_CLOSED, SamplePolls.pizzaNight, "Winner: Pizza")

        val added = repository.notifications.value.first()
        assertEquals(sizeBefore + 1, repository.notifications.value.size)
        assertEquals(NotificationType.POLL_CLOSED, added.type)
        assertEquals("poll_1", added.pollId)
        assertEquals(SamplePolls.pizzaNight.question, added.pollQuestion)
        assertEquals("Winner: Pizza", added.detail)
        assertFalse(added.isRead)
    }

    @Test
    fun markRead_onlyAffectsThatNotification() = runBlocking {
        repository.markRead("notif_2")

        val byId = repository.notifications.value.associateBy { it.id }
        assertTrue(byId.getValue("notif_2").isRead)
        assertFalse(byId.getValue("notif_3").isRead)
    }

    @Test
    fun markAllRead_leavesNothingUnread() = runBlocking {
        repository.markAllRead()

        assertTrue(repository.notifications.value.all { it.isRead })
    }
}
