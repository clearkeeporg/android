package com.clearkeep.features.auth.presentation.login

import android.content.Context
import androidx.lifecycle.*
import com.clearkeep.common.utilities.*
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.domain.model.response.LoginResponse
import com.clearkeep.domain.model.response.AuthRes
import com.clearkeep.domain.model.response.SocialLoginRes
import com.clearkeep.domain.repository.SignalKeyRepository
import com.clearkeep.domain.usecase.auth.*
import com.clearkeep.domain.usecase.workspace.GetWorkspaceInfoUseCase
import com.clearkeep.features.auth.R
import com.facebook.AccessToken
import com.facebook.login.LoginManager
import javax.inject.Inject
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import com.facebook.GraphRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val loginByGoogleUseCase: LoginByGoogleUseCase,
    private val loginByMicrosoftUseCase: LoginByMicrosoftUseCase,
    private val loginByFacebookUseCase: LoginByFacebookUseCase,
    private val validateOtpUseCase: ValidateOtpUseCase,
    private val mfaResendOtpUseCase: LoginMfaResendOtpUseCase,
    private val getWorkspaceInfoUseCase: GetWorkspaceInfoUseCase,
    private val signalKeyRepository: SignalKeyRepository
) : ViewModel() {
    lateinit var googleSignIn: GoogleSignInOptions
    lateinit var googleSignInClient: GoogleSignInClient
    var mSingleAccountApp: ISingleAccountPublicClientApplication? = null
    val loginFacebookManager: LoginManager = LoginManager.getInstance()

    private val _email = MutableLiveData<String>()
    val email: LiveData<String> get() = _email

    var isCustomServer: Boolean = false
    var customDomain: String = ""

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val _emailError = MutableLiveData<String>()
    val emailError: LiveData<String>
        get() = _emailError

    val passError: LiveData<String>
        get() = _passError
    private val _passError = MutableLiveData<String>()

    private val _isAccountLocked = MutableLiveData<Boolean>()
    val isAccountLocked: LiveData<Boolean> get() = _isAccountLocked

    private val _securityPhrase = MutableLiveData<String>()
    private val _confirmSecurityPhrase = MutableLiveData<String>()

    private val _isSecurityPhraseValid = MutableLiveData<Boolean>()
    val isSecurityPhraseValid: LiveData<Boolean> get() = _isSecurityPhraseValid

    private val _isConfirmSecurityPhraseValid = MutableLiveData<Boolean>()
    val isConfirmSecurityPhraseValid: LiveData<Boolean> get() = _isConfirmSecurityPhraseValid

    val loginErrorMess = MutableLiveData<LoginActivity.ErrorMessage>()
    val verifyPassphraseResponse = MutableLiveData<Resource<AuthRes>>()
    val registerSocialPinResponse = MutableLiveData<Resource<AuthRes>>()
    val verifyOtpResponse = MutableLiveData<Resource<String>>()
    val serverUrlValidateResponse = MutableLiveData<Resource<String>>()

    private var otpHash: String = ""
    private var userId: String = ""
    private var hashKey: String = ""

    private var checkValidServerJob: Job? = null

    private var isResetPincode = false
    private var resetPincodeToken: String = ""

    init {
        viewModelScope.launch {
            signalKeyRepository.getIdentityKey("","")
        }
    }
    fun setOtpLoginInfo(otpHash: String, userId: String, hashKey: String) {
        this.otpHash = otpHash
        this.userId = userId
        this.hashKey = hashKey
    }

    fun setResetPincodeState(isResetPincode: Boolean) {
        this.isResetPincode = isResetPincode
    }

    fun setSecurityPhrase(phrase: String) {
        _securityPhrase.value = phrase
        _isSecurityPhraseValid.value = isSecurityPhraseValid(phrase)
    }

    fun setConfirmSecurityPhrase(phrase: String) {
        _confirmSecurityPhrase.value = phrase
        _isConfirmSecurityPhraseValid.value = isConfirmSecurityPhraseValid()
    }

    fun initGoogleSingIn(context: Context) {
        googleSignIn = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestProfile()
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, googleSignIn)
        googleSignInClient.signOut()
    }

    fun initMicrosoftSignIn(
        context: Context,
        onSuccess: (() -> Unit),
        onError: ((String?) -> Unit)
    ) {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context, R.raw.auth_config_single_account, object :
                IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication?) {
                    mSingleAccountApp = application
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            mSingleAccountApp?.signOut()
                        } catch (e: Exception) {
                            printlnCK(e.message.toString())
                        }
                        onSuccess.invoke()
                    }
                }

                override fun onError(exception: MsalException?) {
                    onError.invoke(exception?.message)
                }
            })
    }

    suspend fun loginByGoogle(token: String): Resource<SocialLoginRes> {
        _isLoading.value = true
        val result = loginByGoogleUseCase(token, getDomain()).also {
            if (it.status == com.clearkeep.common.utilities.network.Status.SUCCESS) {
                resetPincodeToken = it.data?.resetPincodeToken ?: ""
                userId = it.data?.username ?: ""
            }
        }
        _isLoading.value = false
        return result
    }

    suspend fun loginByFacebook(token: String): Resource<SocialLoginRes> {
        _isLoading.value = true
        val result = loginByFacebookUseCase(token, getDomain()).also {
            if (it.status == com.clearkeep.common.utilities.network.Status.SUCCESS) {
                resetPincodeToken = it.data?.resetPincodeToken ?: ""
                userId = it.data?.username ?: ""
            }
        }
        _isLoading.value = false
        return result
    }

    fun getFacebookProfile(accessToken: AccessToken, getName: (String) -> Unit) {
        val request = GraphRequest.newGraphPathRequest(
            accessToken, "me"
        ) {
            try {
                val name = it.jsonObject?.get("name").toString()
                getName.invoke(name)
            } catch (e: Exception) {
                getName.invoke("")
            }
        }
        request.executeAsync()
    }

    suspend fun loginByMicrosoft(accessToken: String): Resource<SocialLoginRes> {
        _isLoading.value = true
        val result = loginByMicrosoftUseCase(accessToken, getDomain()).also {
            if (it.status == com.clearkeep.common.utilities.network.Status.SUCCESS) {
                resetPincodeToken = it.data?.resetPincodeToken ?: ""
                userId = it.data?.username ?: ""
            }
        }
        _isLoading.value = false
        return result
    }

    suspend fun login(context: Context, email: String, password: String): Resource<LoginResponse>? {
        _emailError.value = ""
        _passError.value = ""
        _isLoading.value = true
        val result = if (email.isBlank()) {
            _emailError.value = context.getString(R.string.email_empty)
            loginErrorMess.postValue(
                LoginActivity.ErrorMessage(
                    context.getString(R.string.email_empty),
                    context.getString(R.string.pls_check_again)
                )
            )
            null
        } else if (!email.trim().isValidEmail()) {
            _emailError.value = context.getString(R.string.email_invalid)
            loginErrorMess.postValue(
                LoginActivity.ErrorMessage(
                    context.getString(R.string.email_invalid),
                    context.getString(R.string.pls_check_again)
                )
            )
            null
        } else if (password.isBlank()) {
            _passError.value = context.getString(R.string.password_empty)
            loginErrorMess.postValue(
                LoginActivity.ErrorMessage(
                    context.getString(R.string.password_empty),
                    context.getString(R.string.pls_check_again)
                )
            )
            null
        } else {
            loginUseCase.byEmail(email, password, getDomain())
        }
        _isLoading.value = false
        return result
    }

    fun clearSecurityPhraseInput() {
        _securityPhrase.value = ""
        _confirmSecurityPhrase.value = ""
        _isSecurityPhraseValid.value = null
    }

    fun resetSecurityPhraseErrors() {
        verifyPassphraseResponse.value = null
        registerSocialPinResponse.value = null
    }

    fun onSubmitNewPin() {
        viewModelScope.launch {
            _isLoading.value = true
            val pin = _confirmSecurityPhrase.value?.trim() ?: ""
            registerSocialPinResponse.value = if (isResetPincode) {
                loginUseCase.resetSocialPin(getDomain(), pin, userId, resetPincodeToken)
            } else {
                loginUseCase.registerSocialPin(getDomain(), pin, userId)
            }
            _isLoading.value = false
        }
    }

    fun verifySocialPin() {
        viewModelScope.launch {
            _isLoading.value = true
            val pin = _securityPhrase.value?.trim() ?: ""
            verifyPassphraseResponse.value = loginUseCase.verifySocialPin(getDomain(), pin, userId)
            _isLoading.value = false
        }
    }

    fun validateOtp(otp: String) {
        if (otp.isBlank() || otp.length != 4) {
            verifyOtpResponse.value =
                Resource.error("The code you???ve entered is incorrect. Please try again", null)
            return
        }

        viewModelScope.launch {
            val response = validateOtpUseCase(getDomain(), otp, otpHash, userId, hashKey)
            if (response.status == com.clearkeep.common.utilities.network.Status.ERROR) {
                verifyOtpResponse.value = Resource.error(
                    response.message ?: "The code you???ve entered is incorrect. Please try again",
                    null
                )
            } else {
                verifyOtpResponse.value = Resource.success(null)
            }
        }
    }

    fun requestResendOtp() {
        viewModelScope.launch {
            val response = mfaResendOtpUseCase(getDomain(), otpHash, userId)
            val errorCode = response.data?.first

            if (response.status == com.clearkeep.common.utilities.network.Status.ERROR) {
                verifyOtpResponse.value = Resource.error(
                    response.data?.second
                        ?: "The code you???ve entered is incorrect. Please try again", null
                )

                if (errorCode == 1069) {
                    _isAccountLocked.value = true
                }
            } else {
                otpHash = response.data?.second ?: ""
            }
        }
    }

    fun resetAccountLock() {
        _isAccountLocked.value = false
    }

    fun checkValidServerUrl(url: String) {
        _isLoading.value = true
        checkValidServerJob?.cancel()
        checkValidServerJob = viewModelScope.launch {
            if (!isValidServerUrl(url)) {
                serverUrlValidateResponse.value =
                    Resource.error("Wrong server URL. Please try again", null, 0)
                _isLoading.value = false
                return@launch
            }

            val workspaceInfoResponse = getWorkspaceInfoUseCase(domain = url)
            _isLoading.value = false
            if (workspaceInfoResponse.status == com.clearkeep.common.utilities.network.Status.ERROR) {
                serverUrlValidateResponse.value = workspaceInfoResponse
                return@launch
            }

            serverUrlValidateResponse.value = Resource.success(url)
        }
    }

    fun clearLoading() {
        _isLoading.value = false
    }

    fun cancelCheckValidServer() {
        checkValidServerJob?.cancel()
    }

    fun getDomain(): String {
        return if (isCustomServer) customDomain else "$BASE_URL:$PORT"
    }

    private fun isSecurityPhraseValid(phrase: String): Boolean {
        val words = phrase.split("\\s+".toRegex())
        return words.size >= 3 && words.sumBy { it.length } >= 15
    }

    private fun isConfirmSecurityPhraseValid(): Boolean {
        return isSecurityPhraseValid(
            (_securityPhrase.value ?: "").trim()
        ) && _confirmSecurityPhrase.value?.trim() == _securityPhrase.value?.trim()
    }

    fun setEmail(email: String) {
        _email.value = email
    }

    companion object {
        val SCOPES_MICROSOFT = arrayOf("Files.Read", "User.Read")
    }
}