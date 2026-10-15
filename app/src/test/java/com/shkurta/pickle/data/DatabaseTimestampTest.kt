package com.shkurta.pickle.data

import com.shkurta.pickle.data.remote.dto.parseDatabaseInstant
import com.shkurta.pickle.domain.model.InvalidRemoteDataException
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class DatabaseTimestampTest {
    @Test fun postgresUtcOffsetAndMicrosecondsArePreserved() {
        assertEquals(Instant.parse("2026-10-14T10:51:16.426011Z"), parseDatabaseInstant("2026-10-14T10:51:16.426011+00:00"))
    }
    @Test fun nonUtcOffsetIsConvertedToUtc() {
        assertEquals(Instant.parse("2026-10-14T10:51:16.426011Z"), parseDatabaseInstant("2026-10-14T13:51:16.426011+03:00"))
    }
    @Test fun utcDesignatorRemainsSupported() {
        assertEquals(Instant.parse("2026-10-14T10:51:16Z"), parseDatabaseInstant("2026-10-14T10:51:16Z"))
    }
    @Test fun invalidTimestampBecomesRecoverableDataError() {
        assertTrue(runCatching { parseDatabaseInstant("not a date") }.exceptionOrNull() is InvalidRemoteDataException)
    }
}
