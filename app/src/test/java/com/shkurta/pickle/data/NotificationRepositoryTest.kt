package com.shkurta.pickle.data

import com.shkurta.pickle.data.sample.SampleNotifications
import com.shkurta.pickle.data.sample.SamplePolls
import com.shkurta.pickle.domain.model.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRepositoryTest {

    private val repository = NotificationRepository(SampleNotifications.all)

    @Test
    fun add_prependsUnreadNotificationForPoll() {
        repository.add(NotificationType.POLL_CLOSED, SamplePolls.pizzaNight, "Winner: Pizza")

        val added = repository.notifications.value.first()
        assertEquals(SampleNotifications.all.size + 1, repository.notifications.value.size)
        assertEquals(NotificationType.POLL_CLOSED, added.type)
        assertEquals("poll_1", added.pollId)
        assertEquals(SamplePolls.pizzaNight.question, added.pollQuestion)
        assertEquals("Winner: Pizza", added.detail)
        assertFalse(added.isRead)
    }

    @Test
    fun markRead_onlyAffectsThatNotification() {
        repository.markRead("notif_2")

        val byId = repository.notifications.value.associateBy { it.id }
        assertTrue(byId.getValue("notif_2").isRead)
        assertFalse(byId.getValue("notif_3").isRead)
    }

    @Test
    fun markAllRead_leavesNothingUnread() {
        repository.markAllRead()

        assertTrue(repository.notifications.value.all { it.isRead })
    }
}
