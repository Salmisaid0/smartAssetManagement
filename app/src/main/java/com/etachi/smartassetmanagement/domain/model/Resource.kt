package com.etachi.smartassetmanagement.domain.model

/**
 * A sealed class to represent the state of a network or database operation.
 *
 * WHY "Resource" instead of "Result"?
 * Because Kotlin has a built-in class called "kotlin.Result". If we name ours
 * "Result", it will cause a naming collision and crash your app.
 */
sealed class Resource<out T> {

    /**
     * Represents a successful operation containing data.
     */
    data class Success<T>(val data: T) : Resource<T>()

    /**
     * Represents a failed operation containing the error and a user-friendly message.
     */
    data class Error(val exception: Throwable, val message: String? = null) : Resource<Nothing>()

    /**
     * Represents an ongoing operation (e.g., showing a loading spinner).
     */
    object Loading : Resource<Nothing>()

    // --- Convenience properties ---
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    /**
     * Returns the data if Success, otherwise returns null.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
}

// --- Extension functions for cleaner syntax ---

/**
 * Executes an action if the Resource is Success.
 * Usage: resource.onSuccess { data -> doSomething(data) }
 */
inline fun <T> Resource<T>.onSuccess(action: (T) -> Unit): Resource<T> {
    if (this is Resource.Success) action(data)
    return this
}

/**
 * Executes an action if the Resource is Error.
 * Usage: resource.onError { throwable, msg -> showError(msg) }
 */
inline fun <T> Resource<T>.onError(action: (Throwable, String?) -> Unit): Resource<T> {
    if (this is Resource.Error) action(exception, message)
    return this
}