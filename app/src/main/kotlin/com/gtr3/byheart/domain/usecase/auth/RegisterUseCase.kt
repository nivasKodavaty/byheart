package com.gtr3.byheart.domain.usecase.auth

import com.gtr3.byheart.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String) =
        repository.register(email, password)
}
