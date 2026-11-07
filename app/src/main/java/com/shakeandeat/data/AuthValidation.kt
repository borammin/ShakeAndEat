package com.shakeandeat.data

/** Pure input checks for the login screen. Each returns an error message, or null when valid. */
object AuthValidation {
    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_NAME_LENGTH = 50

    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    /** Firebase treats emails case-insensitively; we store and compare them lowercase. */
    fun normalizeEmail(raw: String): String = raw.trim().lowercase()

    fun emailError(raw: String): String? =
        if (EMAIL.matches(normalizeEmail(raw))) null else "Enter a valid email address."

    fun passwordError(password: String): String? =
        if (password.length >= MIN_PASSWORD_LENGTH) null
        else "Password must be at least $MIN_PASSWORD_LENGTH characters."

    fun nameError(raw: String): String? {
        val name = raw.trim()
        return when {
            name.isEmpty() -> "Enter your name."
            name.length > MAX_NAME_LENGTH -> "Name must be $MAX_NAME_LENGTH characters or fewer."
            else -> null
        }
    }
}
