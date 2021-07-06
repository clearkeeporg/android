package com.clearkeep.screen.chat.profile

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.clearkeep.components.CKSimpleTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.clearkeep.screen.chat.change_pass_word.ChangePasswordActivity


@AndroidEntryPoint
class ProfileActivity : AppCompatActivity(), LifecycleObserver {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val profileViewModel: ProfileViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        setContent {
            CKSimpleTheme {
                MainComposable()
            }
        }
    }

    @Composable
    private fun MainComposable() {
        ProfileScreen(
            profileViewModel = profileViewModel,
            onCloseView = {
                finish()
            },
            onChangePassword = {
                navigateToChangePassword()
            },
            onCopyToClipBoard = {
                copyProfileLinkToClipBoard("profile link", profileViewModel.getProfileLink())
            }
        )
    }

    private fun navigateToChangePassword() {
        val intent = Intent(this, ChangePasswordActivity::class.java)
        startActivity(intent)
    }

    private fun copyProfileLinkToClipBoard(label: String, text: String) {
        val clipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(applicationContext, "You copied", Toast.LENGTH_SHORT).show()
    }
}