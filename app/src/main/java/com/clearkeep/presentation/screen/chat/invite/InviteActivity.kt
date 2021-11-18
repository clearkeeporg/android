package com.clearkeep.presentation.screen.chat.invite

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.clearkeep.presentation.components.CKSimpleTheme

class InviteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CKSimpleTheme {
                InviteScreen {
                    finish()
                }
            }
        }
    }
}