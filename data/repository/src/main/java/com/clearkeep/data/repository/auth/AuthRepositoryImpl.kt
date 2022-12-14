package com.clearkeep.data.repository.auth

import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.data.local.preference.AppStorage
import com.clearkeep.data.remote.service.AuthService
import com.clearkeep.data.repository.utils.parseError
import com.clearkeep.domain.model.Profile
import com.clearkeep.domain.model.Server
import com.clearkeep.domain.model.response.AuthChallengeRes
import com.clearkeep.domain.model.response.AuthRes
import com.clearkeep.domain.model.response.SocialLoginRes
import com.clearkeep.domain.repository.AuthRepository
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val userManager: AppStorage,
    private val authService: AuthService
) : AuthRepository {
    override suspend fun register(
        domain: String,
        email: String,
        verificatorHex: String,
        saltHex: String,
        displayName: String,
        iv: String,
        transitionID: Int,
        identityKeyPublic: ByteArray,
        preKeyId: Int,
        preKey: ByteArray,
        signedPreKeyId: Int,
        signedPreKey: ByteArray,
        identityKeyEncrypted: String?,
        signedPreKeySignature: ByteArray
    ): Resource<Any> = withContext(Dispatchers.IO) {
        try {
            val response = authService.registerSrp(
                domain,
                email,
                verificatorHex,
                saltHex,
                displayName,
                iv,
                transitionID,
                identityKeyPublic,
                preKeyId,
                preKey,
                signedPreKeyId,
                signedPreKey,
                identityKeyEncrypted,
                signedPreKey
            )

            if (response.error.isNullOrBlank()) {
                return@withContext Resource.success(response)
            } else {
                printlnCK("register failed: ${response.error}")
                return@withContext Resource.error(response.error, null)
            }
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = when (parsedError.code) {
                1002 -> "This email address is already being used"
                else -> parsedError.message
            }
            return@withContext Resource.error(message, null, parsedError.code)
        } catch (e: Exception) {
            printlnCK("register error: $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    override suspend fun loginByGoogle(
        token: String,
        domain: String
    ): Resource<SocialLoginRes> = withContext(Dispatchers.IO) {
        try {
            val response = authService.loginByGoogle(token, domain)

            return@withContext Resource.success(response.toEntity())
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = when (parsedError.code) {
                1001 -> "Login information is not correct. Please try again"
                else -> parsedError.message
            }
            return@withContext Resource.error(message, null)
        } catch (e: Exception) {
            printlnCK("login by google error: $e")
            return@withContext Resource.error(e.toString(), null)
        }

    }

    override suspend fun loginByFacebook(
        token: String,
        domain: String
    ): Resource<SocialLoginRes> = withContext(Dispatchers.IO) {
        try {
            val response = authService.loginByFacebook(token, domain)
            return@withContext Resource.success(response.toEntity())
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = when (parsedError.code) {
                1001 -> "Login information is not correct. Please try again"
                else -> parsedError.message
            }
            return@withContext Resource.error(message, null)
        } catch (e: Exception) {
            printlnCK("login by facebook error: $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    override suspend fun loginByMicrosoft(
        accessToken: String,
        domain: String
    ): Resource<SocialLoginRes> = withContext(Dispatchers.IO) {
        try {
            val response = authService.loginByMicrosoft(accessToken, domain)
            return@withContext Resource.success(response.toEntity())
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = when (parsedError.code) {
                1001 -> "Login information is not correct. Please try again"
                else -> parsedError.message
            }
            printlnCK("login by microsoft error statusRuntime $e $message")
            return@withContext Resource.error(message, null)
        } catch (e: Exception) {
            printlnCK("login by microsoft error $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    override suspend fun registerSocialPin(
        transitionID: Int,
        identityKeyPublic: ByteArray,
        preKey: ByteArray,
        preKeyId: Int,
        signedPreKeyId: Int,
        signedPreKey: ByteArray,
        identityKeyEncrypted: String?,
        signedPreKeySignature: ByteArray,
        userName: String,
        saltHex: String,
        verificatorHex: String,
        iv: String,
        domain: String
    ): Resource<AuthRes> = withContext(Dispatchers.IO) {
        try {
            val response = authService.registerPincode(
                transitionID,
                identityKeyPublic,
                preKey,
                preKeyId,
                signedPreKeyId,
                signedPreKey,
                identityKeyEncrypted,
                signedPreKeySignature,
                userName,
                saltHex,
                verificatorHex,
                iv,
                domain
            )
            if (response.error.isEmpty()) {
                return@withContext Resource.success(response.toEntity())
            }
            return@withContext Resource.error(response.error, null)
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = when (parsedError.code) {
                else -> parsedError.message
            }
            printlnCK("registerSocialPin error statusRuntime $e $message")
            return@withContext Resource.error(message, null)
        } catch (e: Exception) {
            printlnCK("registerSocialPin error $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    override suspend fun verifySocialPin(
        userName: String,
        aHex: String,
        mHex: String,
        domain: String
    ): Resource<AuthRes> =
        withContext(Dispatchers.IO) {
            try {
                val response = authService.verifyPinCode(userName, aHex, mHex, domain)
                if (response.error.isEmpty()) {
                    return@withContext Resource.success(response.toEntity())
                }
                return@withContext Resource.error(response.error, null)
            } catch (e: StatusRuntimeException) {
                val parsedError = parseError(e)
                val message = when (parsedError.code) {
                    else -> parsedError.message
                }
                printlnCK("verifySocialPin error statusRuntime $e $message")
                return@withContext Resource.error(message, null)
            } catch (e: Exception) {
                printlnCK("verifySocialPin error $e")
                return@withContext Resource.error(e.toString(), null)
            }
        }

    override suspend fun resetSocialPin(
        transitionID: Int,
        publicKey: ByteArray,
        preKey: ByteArray,
        preKeyId: Int,
        signedPreKey: ByteArray,
        signedPreKeyId: Int,
        identityKeyEncrypted: String?,
        signedPreKeySignature: ByteArray,
        userName: String,
        resetPincodeToken: String,
        verficatorHex: String,
        saltHex: String,
        iv: String,
        domain: String
    ): Resource<AuthRes> = withContext(Dispatchers.IO) {
        try {
            val response = authService.resetPinCode(
                transitionID,
                publicKey,
                preKey,
                preKeyId,
                signedPreKey,
                signedPreKeyId,
                identityKeyEncrypted,
                signedPreKeySignature,
                userName,
                resetPincodeToken,
                verficatorHex,
                saltHex,
                iv,
                domain,
            )

            if (response.error.isEmpty()) {
                return@withContext Resource.success(response.toEntity())
            }
            return@withContext Resource.error(response.error, null)
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = when (parsedError.code) {
                else -> parsedError.message
            }
            printlnCK("resetSocialPin error statusRuntime $e $message")
            return@withContext Resource.error(message, null)
        } catch (e: Exception) {
            printlnCK("resetSocialPin error $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    override suspend fun resetPassword(
        transitionID: Int,
        publicKey: ByteArray,
        preKey: ByteArray,
        preKeyId: Int,
        signedPreKeyId: Int,
        signedPreKey: ByteArray,
        identityKeyEncrypted: String?,
        signedPreKeySignature: ByteArray,
        preAccessToken: String,
        email: String,
        verificatorHex: String,
        saltHex: String,
        iv: String,
        domain: String
    ): Resource<AuthRes> = withContext(Dispatchers.IO) {
        try {
            val response = authService.forgotPasswordUpdate(
                transitionID,
                publicKey,
                preKey,
                preKeyId,
                signedPreKeyId,
                signedPreKey,
                identityKeyEncrypted,
                signedPreKeySignature,
                preAccessToken,
                email,
                verificatorHex,
                saltHex,
                iv,
                domain
            )
            if (response.error.isEmpty()) {
                return@withContext Resource.success(response.toEntity())
            }
            return@withContext Resource.error(response.error, null)
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = when (parsedError.code) {
                else -> parsedError.message
            }
            printlnCK("resetSocialPin error statusRuntime $e $message")
            return@withContext Resource.error(message, null)
        } catch (e: Exception) {
            printlnCK("resetSocialPin error $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    override suspend fun recoverPassword(
        email: String,
        domain: String
    ): Resource<String> = withContext(Dispatchers.IO) {
        printlnCK("recoverPassword: $email")
        try {
            val response = authService.forgotPassword(email, domain)

            if (response.error?.isEmpty() == true) {
                return@withContext Resource.success(response.error)
            } else {
                return@withContext Resource.error(response.error, null)
            }
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            val message = parsedError.message
            return@withContext Resource.error(message, null, parsedError.code)
        } catch (e: Exception) {
            printlnCK("recoverPassword error: $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    override suspend fun logoutFromAPI(server: Server): Resource<String> =
        withContext(Dispatchers.IO) {
            printlnCK("logoutFromAPI")
            try {
                val deviceId = userManager.getUniqueDeviceID()
                val response = authService.logout(deviceId, server)

                if (response.error?.isEmpty() == true) {
                    printlnCK("logoutFromAPI successed")
                    return@withContext Resource.success(response.error)
                } else {
                    printlnCK("logoutFromAPI failed: ${response.error}")
                    return@withContext Resource.error(response.error, null)
                }
            } catch (e: Exception) {
                printlnCK("logoutFromAPI error: $e")
                return@withContext Resource.error(e.toString(), null)
            }
        }

    override suspend fun validateOtp(
        domain: String,
        otp: String,
        otpHash: String,
        userId: String,
        hashKey: String
    ): Resource<AuthRes> = withContext(Dispatchers.IO) {
        try {
            val response = authService.validateOtp(otp, otpHash, userId, domain)
            return@withContext Resource.success(response.toEntity())
        } catch (exception: StatusRuntimeException) {
            val parsedError = parseError(exception)
            val message = when (parsedError.code) {
                1071 -> "Authentication failed. Please retry."
                1068, 1072 -> "Verification code has expired. Please request a new code and retry."
                else -> parsedError.message
            }
            return@withContext Resource.error(message, null)
        } catch (exception: Exception) {
            printlnCK("mfaValidateOtp: $exception")
            return@withContext Resource.error(exception.toString(), null)
        }
    }

    override suspend fun mfaResendOtp(
        domain: String,
        otpHash: String,
        userId: String
    ): Resource<Pair<Int, String>> = withContext(Dispatchers.IO) {
        try {
            val response = authService.resendOtp(otpHash, userId, domain)
            printlnCK("mfaResendOtp oldOtpHash $otpHash newOtpHash? ${response.preAccessToken} success? ${response.success}")
            return@withContext if (response.preAccessToken.isNotBlank()) Resource.success(0 to response.preAccessToken) else Resource.error(
                "",
                0 to ""
            )
        } catch (exception: StatusRuntimeException) {
            val parsedError = parseError(exception)
            val message = when (parsedError.code) {
                1069 -> "Your account has been locked out due to too many attempts. Please try again later!"
                else -> parsedError.message
            }
            return@withContext Resource.error("", parsedError.code to message)
        } catch (exception: Exception) {
            printlnCK("mfaResendOtp: $exception")
            return@withContext Resource.error("", 0 to exception.toString())
        }
    }

    override suspend fun getProfile(
        domain: String,
        accessToken: String,
        hashKey: String
    ): Profile? =
        withContext(Dispatchers.IO) {
            try {
                val response = authService.getProfile(domain, accessToken, hashKey)
                printlnCK("getProfileWithGrpc: $response")
                return@withContext Profile(
                    userId = response.id,
                    userName = response.displayName,
                    email = response.email,
                    phoneNumber = response.phoneNumber,
                    avatar = response.avatar,
                    updatedAt = Calendar.getInstance().timeInMillis
                )
            } catch (e: Exception) {
                printlnCK("getProfileWithGrpc: $e")
                return@withContext null
            }
        }

    override suspend fun sendLoginChallenge(
        username: String,
        aHex: String,
        domain: String
    ): Resource<AuthChallengeRes> = withContext(Dispatchers.IO) {
        try {
            val response = authService.loginChallenge(username, aHex, domain)
            return@withContext Resource.success(response.toEntity())
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            printlnCK("login error: ${e.message}")
            val errorMessage = when (parsedError.code) {
                1001, 1079 -> "Please check your details and try again"
                1026 -> "Your account has not been activated. Please check the email for the activation link."
                1069 -> "Your account has been locked out due to too many attempts. Please try again later!"
                else -> {
                    parsedError.message
                }
            }
            return@withContext Resource.error(errorMessage,null,errorCode = parsedError.code,e)
        } catch (e: Exception) {
            printlnCK("login error: $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    override suspend fun sendLoginSocialChallenge(
        userName: String,
        aHex: String,
        domain: String
    ): Resource<AuthChallengeRes> = withContext(Dispatchers.IO) {
        try {
            val response = authService.loginSocialChallenge(userName, aHex, domain)
            return@withContext Resource.success(response.toEntity())
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            printlnCK("login error: ${e.message}")
            val errorMessage = when (parsedError.code) {
                1001, 1079 -> "Please check your details and try again"
                1026 -> "Your account has not been activated. Please check the email for the activation link."
                1069 -> "Your account has been locked out due to too many attempts. Please try again later!"
                else -> {
                    parsedError.message
                }
            }
            return@withContext Resource.error(errorMessage, null)
        } catch (e: Exception) {
            printlnCK("login error: $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    override suspend fun loginAuthenticate(
        userName: String,
        aHex: String,
        mHex: String,
        domain: String
    ): Resource<AuthRes> = withContext(Dispatchers.IO) {
        try {
            val response = authService.loginAuthenticate(userName, aHex, mHex, domain)
            return@withContext Resource.success(response.toEntity())
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)
            printlnCK("login error: ${e.message}")
            val errorMessage = when (parsedError.code) {
                1001, 1079 -> "Please check your details and try again"
                1026 -> "Your account has not been activated. Please check the email for the activation link."
                1069 -> "Your account has been locked out due to too many attempts. Please try again later!"
                else -> {
                    parsedError.message
                }
            }
            return@withContext Resource.error(errorMessage, null, errorCode = parsedError.code)
        } catch (e: Exception) {
            printlnCK("login error: $e")
            return@withContext Resource.error(e.toString(), null, errorCode = -1)
        }
    }
}