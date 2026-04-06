package com.gtr3.byheart.domain.model

data class UserProfile(
    val email: String,
    val displayName: String?,
    val dateOfBirth: String?,   // "YYYY-MM-DD" or null
    val sex: String?
)
