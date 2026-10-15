package com.shkurta.pickle.domain.model

import kotlinx.coroutines.CancellationException

/** A write committed, but its follow-up read failed. Retrying must only refresh. */
class SavedRefreshException(val savedId: String? = null) : Exception()
class SignInRequiredException : Exception()
class InvalidRemoteDataException : Exception()

inline fun <reified T : Enum<T>> enumOrDefault(value: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: default

suspend fun <T> cancellationSafeResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
