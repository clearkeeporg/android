package com.clearkeep.screen.chat.otp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.backgroundGradientEnd
import com.clearkeep.components.backgroundGradientStart
import com.clearkeep.components.base.*
import com.clearkeep.components.grayscaleOffWhite
import com.clearkeep.utilities.network.Status
import com.clearkeep.utilities.sdp
import com.clearkeep.utilities.toNonScalableTextSize

@Composable
fun OtpVerifyPasswordScreen(
    otpViewModel: OtpViewModel,
    onBackPress: () -> Unit,
    onClickNext: () -> Unit
) {
    val verifyPasswordResponse = otpViewModel.verifyPasswordResponse.observeAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        //todo disable dark mode
                        if (isSystemInDarkTheme()) backgroundGradientStart else backgroundGradientStart,
                        if (isSystemInDarkTheme()) backgroundGradientEnd else backgroundGradientEnd
                    )
                )
            )
    ) {
        val currentPassWord = remember { mutableStateOf("") }

        Spacer(Modifier.height(58.sdp()))
        CKTopAppBarSample(modifier = Modifier.padding(end = 8.sdp()),
            title = stringResource(R.string.otp_verify_password_title), onBackPressed = {
                onBackPress.invoke()
            })
        Spacer(Modifier.height(30.sdp()))
        Column(Modifier.padding(horizontal = 16.sdp())) {
            Text(
                text = stringResource(R.string.otp_verify_password_description),
                style = MaterialTheme.typography.caption,
                color = grayscaleOffWhite,
                fontSize = 16.sdp().toNonScalableTextSize()
            )
            Spacer(Modifier.height(16.sdp()))
            CKTextInputField(
                placeholder = stringResource(R.string.current_password),
                textValue = currentPassWord,
                keyboardType = KeyboardType.Password,
                singleLine = true,
                leadingIcon = {
                    Image(
                        painterResource(R.drawable.ic_icon_lock),
                        contentDescription = null
                    )
                },
            )
            Spacer(Modifier.height(24.sdp()))
            CKButton(
                stringResource(R.string.btn_next),
                onClick = {
                    otpViewModel.verifyPassword(currentPassWord.value)
                },
                modifier = Modifier.fillMaxWidth(),
                buttonType = ButtonType.White,
                enabled = currentPassWord.value.isNotBlank()
            )
        }
        when (verifyPasswordResponse.value?.status) {
            Status.ERROR -> {
                CKAlertDialog(
                    title = verifyPasswordResponse.value!!.data?.first
                        ?: stringResource(R.string.error),
                    text = verifyPasswordResponse.value!!.data?.second
                        ?: stringResource(R.string.otp_verify_password_incorrect),
                    onDismissButtonClick = {
                        otpViewModel.verifyPasswordResponse.value = null
                    },
                    dismissTitle = stringResource(R.string.close)
                )
            }
            Status.SUCCESS -> {
                onClickNext()
            }
            else -> {

            }
        }
    }
}