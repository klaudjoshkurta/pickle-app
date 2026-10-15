package com.shkurta.pickle.data.remote.dto

import com.shkurta.pickle.domain.model.InvalidRemoteDataException
import java.time.DateTimeException
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/** PostgREST emits numeric offsets; Android's Instant parser does not accept all of them. */
fun parseDatabaseInstant(value: String): Instant = try {
    OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
} catch (e: DateTimeException) {
    throw InvalidRemoteDataException()
}
