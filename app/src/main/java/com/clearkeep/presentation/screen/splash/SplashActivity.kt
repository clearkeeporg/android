package com.clearkeep.presentation.screen.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import com.clearkeep.presentation.components.CKTheme
import com.clearkeep.presentation.screen.auth.login.LoginActivity
import com.clearkeep.presentation.screen.chat.home.MainActivity
import com.clearkeep.data.local.preference.UserPreferencesStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    private val splashViewModel: SplashViewModel by viewModels()

    @Inject
    lateinit var storage: UserPreferencesStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OurView()
        }

        nextNavigation()
    }

    @Composable
    fun OurView() {
        CKTheme {
            SplashScreen()
        }
    }

    private fun nextNavigation() {
        GlobalScope.launch(context = Dispatchers.Main) {
            delay(1000)
            if (splashViewModel.isUserRegistered()) {
                splashViewModel.setupEnvironment()
                navigateToHomeActivity()
            } else navigateToLoginActivity()
        }
    }

    private fun navigateToHomeActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToLoginActivity() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}