package com.clearkeep.domain.usecase.auth

import com.clearkeep.domain.repository.AuthRepository
import javax.inject.Inject

class LoginMfaResendOtpUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        domain: String,
        otpHash: String,
        userId: String
    ) = authRepository.mfaResendOtp(domain, otpHash, userId)
}