package com.clearkeep.features.chat.presentation.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.*
import com.clearkeep.common.utilities.files.*
import com.clearkeep.domain.usecase.preferences.GetUserPreferenceUseCase
import com.clearkeep.domain.usecase.profile.GetMfaSettingsUseCase
import com.clearkeep.domain.usecase.profile.UpdateMfaSettingsUseCase
import com.clearkeep.domain.usecase.profile.UpdateProfileUseCase
import com.clearkeep.domain.usecase.profile.UploadAvatarUseCase
import com.clearkeep.domain.usecase.server.GetDefaultServerProfileAsStateUseCase
import com.clearkeep.common.utilities.network.Status
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.User
import com.clearkeep.domain.repository.Environment
import com.clearkeep.domain.usecase.auth.LogoutUseCase
import com.clearkeep.domain.usecase.people.DeleteAccountUseCase
import com.clearkeep.domain.usecase.server.GetIsLogoutUseCase
import com.clearkeep.features.chat.presentation.utils.getLinkFromPeople
import com.clearkeep.features.shared.presentation.BaseViewModel
import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlin.Exception

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val environment: Environment,
    private val uploadAvatarUseCase: UploadAvatarUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val getMfaSettingsUseCase: GetMfaSettingsUseCase,
    private val updateMfaSettingsUseCase: UpdateMfaSettingsUseCase,
    private val getUserPreferenceUseCase: GetUserPreferenceUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    getDefaultServerProfileAsStateUseCase: GetDefaultServerProfileAsStateUseCase,
    logoutUseCase: LogoutUseCase,
    ) : BaseViewModel(logoutUseCase) {
    private val phoneUtil = PhoneNumberUtil.getInstance()

    val profile: LiveData<com.clearkeep.domain.model.Profile?> = getDefaultServerProfileAsStateUseCase().map {
        _username.postValue(it.userName)
        _email.postValue(it.email)

        try {
            val numberProto: PhoneNumber = phoneUtil.parse(it.phoneNumber, "")
            val countryCode = numberProto.countryCode
            val phoneNum = it.phoneNumber?.replace("+$countryCode", "")
            _countryCode.postValue("+$countryCode")
            _phoneNumber.postValue(phoneNum)
        } catch (e: Exception) {
            _phoneNumber.postValue(it.phoneNumber)
            _countryCode.postValue("")
        }
        it
    }

    private lateinit var _userPreference: LiveData<com.clearkeep.domain.model.UserPreference>
    val userPreference: LiveData<com.clearkeep.domain.model.UserPreference>
        get() = _userPreference

    private var _currentPhotoUri: Uri? = null
    private var isAvatarChanged = false

    private val _imageUriSelected = MutableLiveData<String>()
    val imageUriSelected: LiveData<String>
        get() = _imageUriSelected

    val uploadAvatarResponse = MutableLiveData<com.clearkeep.common.utilities.network.Resource<String>>()

    val updateMfaSettingResponse = MutableLiveData<com.clearkeep.common.utilities.network.Resource<Pair<String, String>>>()

    private val _username = MutableLiveData<String>()
    val username: LiveData<String>
        get() = _username

    private val _usernameError = MutableLiveData<Boolean>()
    val usernameError: LiveData<Boolean>
        get() = _usernameError

    private val _email = MutableLiveData<String>()
    val email: LiveData<String>
        get() = _email

    private val _phoneNumber = MutableLiveData<String>()
    val phoneNumber: LiveData<String>
        get() = _phoneNumber

    private val _phoneNumberError = MutableLiveData<Boolean>()
    val phoneNumberError: LiveData<Boolean>
        get() = _phoneNumberError

    private val _countryCode = MutableLiveData<String>()
    val countryCode: LiveData<String>
        get() = _countryCode

    val unsavedChangeDialogVisible = MutableLiveData<Boolean>()
    val deleteUserDialogVisible = MutableLiveData<Boolean>()
    val deleteUserSuccess : MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    val isLoading = MutableLiveData(false)
    fun getMfaDetail() {
        val server = environment.getServer()
        viewModelScope.launch {
            getMfaSettingsUseCase(
                Owner(
                    server.serverDomain,
                    server.profile.userId
                )
            )
        }
        _userPreference = getUserPreferenceUseCase.asState(
            server.serverDomain,
            server.profile.userId
        )
    }

    fun getProfileLink(): String {
        val server = environment.getServer()
        return getLinkFromPeople(
            User(
                userId = server.profile.userId,
                userName = server.profile.userName ?: "",
                domain = server.serverDomain
            )
        )
    }

    fun setEmail(email: String) {
        _email.value = email
    }

    fun setPhoneNumber(phoneNumber: String) {
        _phoneNumber.value = phoneNumber.filter { it.isDigit() }
        if (phoneNumber.isEmpty()) {
            _phoneNumberError.value = false
        } else {
            _phoneNumberError.value = !isValidPhoneNumber(countryCode.value ?: "", phoneNumber)
        }
    }

    fun setCountryCode(countryCode: String) {
        _countryCode.value =
            if (countryCode.isBlank()) "" else "+${countryCode.filter { it.isDigit() }}"
        if (!phoneNumber.value.isNullOrBlank()) {
            _phoneNumberError.value = !isValidPhoneNumber(countryCode, phoneNumber.value ?: "")
        }
    }

    fun setUsername(username: String) {
        _username.value = if (username.length > 30) {
            username.substring(0 until USERNAME_MAX_LENGTH)
        } else {
            username
        }
        _usernameError.value = !isValidUsername(username)
    }

    fun setSelectedImage(uris: String) {
        _imageUriSelected.value = uris
        isAvatarChanged = true
    }

    fun setTakePhoto() {
        _imageUriSelected.value = _currentPhotoUri.toString()
        _currentPhotoUri = null
        isAvatarChanged = true
    }

    fun updateProfileDetail(
        context: Context,
        displayName: String,
        phoneNumber: String,
        countryCode: String
    ) {
        isAvatarChanged = false
        val avatarToUpload = imageUriSelected.value
        val server = environment.getServer()
        val isPhoneNumberChanged = (countryCode + phoneNumber) != profile.value?.phoneNumber
        val shouldUpdateMfaSetting = isPhoneNumberChanged && userPreference.value?.mfa == true

        var hasError = false
        if (!isValidUsername(displayName)) {
            hasError = true
        }

        if (!isValidPhoneNumber(countryCode, phoneNumber)) {
            hasError = true
        }

        if (hasError) return

        val updatedPhoneNumber = if (phoneNumber.isEmpty()) "" else "${countryCode}${phoneNumber}"
        if (!avatarToUpload.isNullOrEmpty()) {
            //Update avatar case
            if (!isValidFileSizes(context, Uri.parse(avatarToUpload))) {
                uploadAvatarResponse.value =
                    com.clearkeep.common.utilities.network.Resource.error("Your Profile Picture cannot be larger than 5MB", null)
                undoAvatarChange()
                return
            }

            GlobalScope.launch {
                val avatarUrl = uploadAvatarImage(avatarToUpload, context)
                if (profile.value != null) {
                    updateProfileUseCase(
                        Owner(
                            server.serverDomain,
                            server.profile.userId
                        ),
                        profile.value!!.copy(
                            userName = displayName.trim(),
                            phoneNumber = updatedPhoneNumber,
                            avatar = avatarUrl,
                            updatedAt = Calendar.getInstance().timeInMillis
                        )
                    )
                    if (shouldUpdateMfaSetting) {
                        val response = updateMfaSettingsUseCase(getOwner(), false)
                        updateMfaSettingResponse.value = response
                    }
                }
            }
        } else {
            //Update normal data only
            viewModelScope.launch {
                if (profile.value != null) {
                    updateProfileUseCase(
                        Owner(
                            server.serverDomain,
                            server.profile.userId
                        ),
                        profile.value!!.copy(
                            userName = displayName.trim(),
                            phoneNumber = updatedPhoneNumber
                        )
                    )
                    if (shouldUpdateMfaSetting) {
                        val response = updateMfaSettingsUseCase(getOwner(), false)
                        if (response.status == Status.ERROR) {
                            updateMfaSettingResponse.value = response
                        }
                    }
                }
            }
        }
    }

    fun hasUnsavedChanges(): Boolean {
        val usernameChanged = _username.value != profile.value?.userName
        val emailChanged = _email.value != profile.value?.email
        val phoneNumberChanged =
            if (phoneNumber.value.isNullOrEmpty()) {
                phoneNumber.value != profile.value?.phoneNumber
            } else
                "${countryCode.value}${phoneNumber.value}" != profile.value?.phoneNumber

        val hasUnsavedChange =
            usernameChanged || emailChanged || phoneNumberChanged || isAvatarChanged
        unsavedChangeDialogVisible.value = hasUnsavedChange
        return hasUnsavedChange
    }

    private fun undoAvatarChange() {
        _imageUriSelected.value = null
        isAvatarChanged = false
    }

    fun undoProfileChanges() {
        val oldProfile = profile.value
        try {
            val numberProto: PhoneNumber = phoneUtil.parse(oldProfile?.phoneNumber, "")
            val countryCode = numberProto.countryCode
            val phoneNum = oldProfile?.phoneNumber?.replace("+$countryCode", "")
            _phoneNumber.value = phoneNum
            _countryCode.value = "+$countryCode"
        } catch (e: Exception) {
            _phoneNumber.value = ""
            _countryCode.value = ""
        }
        _email.value = oldProfile?.email
        _username.value = oldProfile?.userName
        _imageUriSelected.value = null
        isAvatarChanged = false
    }

    fun getMfaErrorMessage(): Pair<String, String> {
        if (userPreference.value?.isSocialAccount == true) {
            return "2FA is not supported" to "We are so sorry. This function is not supported for your account."
        }
        if (profile.value?.phoneNumber.isNullOrEmpty()) {
            return "Type in your phone number" to "You must input your phone number in order to enable this feature."
        }
        return "" to ""
    }

    fun updateMfaSettings(enabled: Boolean) {
        viewModelScope.launch {
            val response = updateMfaSettingsUseCase(getOwner(), enabled)
            if (enabled || response.status == Status.ERROR) {
                //Don't send message to UI when disable success
                updateMfaSettingResponse.value = response
            }
        }
    }

    private suspend fun uploadAvatarImage(avatarToUpload: String, context: Context): String {
        return uploadAvatarUseCase(
            avatarToUpload,
            context,
        )
    }

    fun getPhotoUri(context: Context): Uri {
        if (_currentPhotoUri == null) {
            _currentPhotoUri = generatePhotoUri(context)
        }
        return _currentPhotoUri!!
    }

    private fun getOwner(): Owner {
        val server = environment.getServer()
        return Owner(server.serverDomain, server.profile.userId)
    }

    private fun isValidFileSizes(
        context: Context,
        uri: Uri,
    ): Boolean {
        return uri.getFileSize(context, false) <= AVATAR_MAX_SIZE
    }

    private fun isValidUsername(username: String?): Boolean = !username.isNullOrBlank()

    private fun isValidPhoneNumber(countryCode: String, phoneNumber: String): Boolean {
        if (countryCode.isBlank() && phoneNumber.isNotBlank()) {
            return false
        }
        if (countryCode.isNotBlank() && phoneNumber.isNotBlank()) {
            try {
                val numberProto = phoneUtil.parse("${countryCode}${phoneNumber}", null)
                if (countryCode == "+84" && phoneNumber.startsWith("3") && phoneNumber.length == 9) {
                    //Manual override for Viettel numbers
                    return true
                }
                if (!phoneUtil.isValidNumber(numberProto)) {
                    return false
                }
            } catch (e: Exception) {
                printlnCK("updateProfileDetail error $e")
                printlnCK(e.toString())
                return false
            }
        }
        return true
    }

    fun deleteUser(){
         viewModelScope.launch{
             deleteUserDialogVisible.value = false
             isLoading.value = true
             val result = deleteAccountUseCase.invoke()
             if (result){
                 isLoading.value = false
                 deleteUserSuccess.value = true
             }

         }
     }

    companion object {
        private const val AVATAR_MAX_SIZE = 4_000_000 //4MB
        private const val USERNAME_MAX_LENGTH = 30
    }
}
