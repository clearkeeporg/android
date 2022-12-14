package com.clearkeep.features.chat.presentation.notificationsetting

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.clearkeep.common.presentation.components.CKSimpleTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationSettingActivity : AppCompatActivity() {
    private val notificationSettingsViewModel: NotificationSettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CKSimpleTheme {
                NotificationSettingScreen(notificationSettingsViewModel) {
                    finish()
                }
            }
        }
    }
}