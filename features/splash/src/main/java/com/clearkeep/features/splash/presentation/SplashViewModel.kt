package com.clearkeep.features.splash.presentation

import androidx.lifecycle.ViewModel
import com.clearkeep.domain.usecase.server.GetDefaultServerUseCase
import com.clearkeep.domain.usecase.server.SetActiveServerUseCase
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.domain.repository.Environment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val setActiveServerUseCase: SetActiveServerUseCase,
    private val getDefaultServerUseCase: GetDefaultServerUseCase,
    private val environment: Environment
) : ViewModel() {

    suspend fun isUserRegistered(): Boolean {
        return getDefaultServerUseCase() != null
    }

    suspend fun setupEnvironment() {
        val selectedServer = getDefaultServerUseCase()
        if (selectedServer == null) {
            printlnCK("default server must be not NULL")
            throw IllegalArgumentException("default server must be not NULL")
        }
        environment.setUpDomain(selectedServer)
        setActiveServerUseCase(selectedServer)
    }
}
