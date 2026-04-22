package com.videonatics.smartcapture.core.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

suspend fun <T> safeApiCall(
    call: suspend () -> HttpResponse,
    map: suspend (HttpResponse) -> T
): Result<T> {
    return try {
        val response = call()
        if (response.status.isSuccess()) {
            Result.success(map(response))
        } else {
            Result.failure(Exception("API call failed with status: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * Extension for simple POST requests
 */
suspend inline fun <reified B, reified R> HttpClient.safePost(
    endpoint: String,
    body: B,
    crossinline map: suspend (HttpResponse) -> R
): Result<R> = safeApiCall(
    call = {
        post(endpoint) {
            setBody(body)
        }
    },
    map = { map(it) }
)

/**
 * Extension for simple GET requests
 */
suspend inline fun <reified R> HttpClient.safeGet(
    endpoint: String,
    crossinline block: HttpRequestBuilder.() -> Unit = {},
    crossinline map: suspend (HttpResponse) -> R
): Result<R> = safeApiCall(
    call = {
        get(endpoint, block)
    },
    map = { map(it) }
)
