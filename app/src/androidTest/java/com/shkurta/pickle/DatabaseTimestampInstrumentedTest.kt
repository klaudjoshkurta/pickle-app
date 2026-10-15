package com.shkurta.pickle

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shkurta.pickle.data.remote.dto.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Exercise the Android/desugared date implementation, which differs from the desktop JVM. */
@RunWith(AndroidJUnit4::class)
class DatabaseTimestampInstrumentedTest {
    @Test fun numericOffsetsAndMicrosecondsParseOnAndroid() {
        val utc = parseDatabaseInstant("2026-10-14T10:51:16.426011+00:00")
        assertEquals(1791975076L, utc.epochSecond)
        assertEquals(426011000, utc.nano)
        assertEquals(utc, parseDatabaseInstant("2026-10-14T13:51:16.426011+03:00"))
        assertEquals(utc, parseDatabaseInstant("2026-10-14T10:51:16.426011Z"))
    }

    @Test fun liveResponseShapesDecodeIntoDomainModelsOnAndroid() {
        val stamp = "2026-10-14T10:51:16.426011+00:00"
        val option = PollOptionDto("option", "poll", "Pizza", "APPROVED", createdAt = stamp)
        val poll = PollDto("poll", "Dinner?", "creator", true, true, false, true, "OPEN", stamp, stamp,
            pollOptions = listOf(option))
        val notification = NotificationDto("notice", "user", "POLL_CREATED", "poll", "Dinner?", createdAt = stamp)
        assertEquals(parseDatabaseInstant(stamp), poll.toDomain().createdAt)
        assertEquals(poll.toDomain().createdAt, option.toDomain().createdAt)
        assertEquals(poll.toDomain().createdAt, notification.toDomain().createdAt)
        assertEquals(parseDatabaseInstant(stamp), poll.toDomain().closedAt)
    }
}
