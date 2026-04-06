// File: domain/model/Result.kt
package com.etachi.smartassetmanagement.domain.model

sealed class Result<out T> {

    data class Success<T>(val data: T) : Result<T>()

    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()

    data class Loading(val message: String? = null) : Result<Nothing>()

    // Convenience functions
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
        is Loading -> throw IllegalStateException("Result is still loading")
    }

    fun errorOrNull(): Throwable? = when (this) {
        is Error -> exception
        else -> null
    }
}

// Extension functions for easier chaining
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (Throwable) -> Unit): Result<T> {
    if (this is Result.Error) action(exception)
    return this
}

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> Result.Error(exception, message)
    is Result.Loading -> Result.Loading(message)
}