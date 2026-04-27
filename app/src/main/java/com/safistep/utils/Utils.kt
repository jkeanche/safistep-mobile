package com.safistep.utils

import com.safistep.data.remote.dto.ApiErrorResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Response
import java.io.IOException

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null, val errors: Map<String, List<String>>? = null) : ApiResult<Nothing>()
    object NetworkError : ApiResult<Nothing>()
}

suspend fun <T> safeApiCall(block: suspend () -> Response<T>): ApiResult<T> {
    return try {
        android.util.Log.d("ApiCall", "Making API call...")
        val response = block()
        android.util.Log.d("ApiCall", "Response: ${response.code()} ${response.message()}")
        
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                android.util.Log.d("ApiCall", "Success: $body")
                ApiResult.Success(body)
            } else {
                android.util.Log.e("ApiCall", "Empty response body")
                ApiResult.Error("Empty response", response.code())
            }
        } else {
            val errorBody = response.errorBody()?.string()
            android.util.Log.e("ApiCall", "Error response: $errorBody")
            val parsed = runCatching {
                Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                    .adapter(ApiErrorResponse::class.java).fromJson(errorBody ?: "")
            }.getOrNull()

            ApiResult.Error(
                message = parsed?.message ?: "Request failed (${response.code()})",
                code    = response.code(),
                errors  = parsed?.errors
            )
        }
    } catch (e: IOException) {
        android.util.Log.e("ApiCall", "Network error: ${e.message}")
        ApiResult.NetworkError
    } catch (e: Exception) {
        android.util.Log.e("ApiCall", "Exception: ${e.message}", e)
        ApiResult.Error(e.message ?: "Unknown error")
    }
}

// ── Phone normalization ───────────────────────────────────────

fun normalizePhone(raw: String): String {
    val cleaned = raw.trim().replace(Regex("\\s+"), "").removePrefix("+")
    return when {
        cleaned.startsWith("254") -> cleaned
        cleaned.startsWith("0")   -> "254" + cleaned.removePrefix("0")
        else                      -> "254$cleaned"
    }
}

fun formatPhoneDisplay(phone: String): String {
    // 254712345678 → 0712 345 678
    val local = "0" + phone.removePrefix("254")
    return if (local.length == 10)
        "${local.substring(0, 4)} ${local.substring(4, 7)} ${local.substring(7)}"
    else phone
}

fun formatCurrency(amount: Double?): String {
    if (amount == null) return "—"
    return "KSh ${String.format("%,.0f", amount)}"
}

fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000        -> "Just now"
        diff < 3_600_000     -> "${diff / 60_000}m ago"
        diff < 86_400_000    -> "${diff / 3_600_000}h ago"
        diff < 604_800_000   -> "${diff / 86_400_000}d ago"
        else                  -> java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())
                                     .format(java.util.Date(timestamp))
    }
}

// Extract paybill number from STK text
fun extractPaybill(text: String): String? {
    val regex = Regex("\\b(\\d{5,7})\\b")
    return regex.find(text)?.groupValues?.get(1)
}

// Extract amount from STK text (e.g. "Ksh500" or "KES 1,200")
fun extractAmount(text: String): Double? {
    val regex = Regex("(?:ksh|kes|ksh\\.)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", RegexOption.IGNORE_CASE)
    return regex.find(text)?.groupValues?.get(1)
        ?.replace(",", "")
        ?.toDoubleOrNull()
}
