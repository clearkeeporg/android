package com.clearkeep.features.auth.presentation.forgot

import androidx.lifecycle.*
import com.clearkeep.domain.usecase.auth.RecoverPasswordUseCase
import com.clearkeep.common.utilities.network.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ForgotViewModel @Inject constructor(
    private val recoverPasswordUseCase: RecoverPasswordUseCase,
) : ViewModel() {
    private val _isLoading = MutableLiveData<Boolean>()

    private lateinit var _domain: String
    val domain: String
        get() = _domain

    val isLoading: LiveData<Boolean>
        get() = _isLoading

    fun setDomain(domain: String) {
        _domain = domain
    }

    suspend fun recoverPassword(email: String): Resource<String> {
        _isLoading.value = true
        val res = recoverPasswordUseCase(email, domain)
        _isLoading.value = false
        return res
    }
}