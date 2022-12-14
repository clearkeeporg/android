package com.clearkeep.features.chat.presentation.changepassword

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.clearkeep.common.presentation.components.CKSimpleTheme
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder

@AndroidEntryPoint
class ChangePasswordActivity : AppCompatActivity(), LifecycleObserver {
    private val changePasswordViewModel: ChangePasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        val data: Uri? = intent?.data
        data?.let {
            val decodedURL: String = URLDecoder.decode(data.toString(), "UTF-8")
            changePasswordViewModel.processDeepLinkUri(Uri.parse(decodedURL))
        } ?: kotlin.run {
            changePasswordViewModel.processDeepLinkUri(null)
        }

        setContent {
            CKSimpleTheme {
                MainComposable()
            }
        }
    }

        @Composable
    private fun MainComposable() {
        ChangePasswordScreen(
            changePasswordViewModel,
            onBackPress = {
                finish()
            }
        )
    }
}