package com.clearkeep.screen.chat.otp

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.R
import com.clearkeep.components.backgroundGradientEnd
import com.clearkeep.components.backgroundGradientStart
import com.clearkeep.components.base.ButtonType
import com.clearkeep.components.base.CKButton
import com.clearkeep.components.base.CKTopAppBarSample
import com.clearkeep.components.grayscaleOffWhite

@Composable
fun EnterOtpScreen(onBackPress: () -> Unit, onClickSave: () -> Unit) {
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

        Spacer(Modifier.height(58.dp))
        CKTopAppBarSample(modifier = Modifier.padding(start = 6.dp),
            title = "Enter your OTP", onBackPressed = {
                onBackPress.invoke()
            })
        Spacer(Modifier.height(30.dp))
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Please input a code that has been sent to your phone",
                style = MaterialTheme.typography.caption,
                color = grayscaleOffWhite,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(16.dp))
            OtpInput()
            Spacer(Modifier.height(32.dp))
            Text("Don't get the code?", Modifier.align(Alignment.CenterHorizontally), Color.White, 16.sp, fontWeight = FontWeight.W400)
            Text("Resend code", Modifier.align(Alignment.CenterHorizontally), Color.White, 16.sp)
            Spacer(Modifier.height(24.dp))
            CKButton(
                stringResource(R.string.save),
                onClick = {

                },
                modifier = Modifier.fillMaxWidth(),
                buttonType = ButtonType.White
            )
        }
    }
}

@Composable
fun OtpInput() {
    val input = remember { mutableStateListOf("", "", "", "") }
    val focusRequesters = remember { mutableStateListOf(FocusRequester(), FocusRequester(), FocusRequester(), FocusRequester()) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OtpInputSquare(input[0], focusRequesters[0]) {
            if (it.length <= 1) {
                input[0] = it
                if (it.isNotEmpty()) {
                    focusRequesters[1].requestFocus()
                }
            } else if (isValidOtp(it)) {
                //Handle pasted OTP
                it.forEachIndexed { index: Int, c : Char ->
                    input[index] = it[index].toString()
                }
            }
        }
        OtpInputSquare(input[1], focusRequesters[1]) {
            if (it.length <= 1) {
                input[1] = it
                if (it.isNotEmpty()) {
                    focusRequesters[2].requestFocus()
                } else {
                    focusRequesters[0].requestFocus()
                }
            }
        }
        OtpInputSquare(input[2], focusRequesters[2]) {
            if (it.length <= 1) {
                input[2] = it
                if (it.isNotEmpty()) {
                    focusRequesters[3].requestFocus()
                } else {
                    focusRequesters[1].requestFocus()
                }
            }
        }
        OtpInputSquare(input[3], focusRequesters[3]) {
            if (it.length <= 1) {
                input[3] = it
                if (it.isEmpty()) {
                    focusRequesters[2].requestFocus()
                }
            }
        }
    }
}

@Composable
fun OtpInputSquare(value: String, focusRequester: FocusRequester, onValueChange: (String) -> Unit) {
    Box(
        Modifier
            .size(56.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
    ) {
        Box(Modifier.size(28.dp).align(Alignment.Center)) {
            BasicTextField(
                value,
                onValueChange = onValueChange,
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().focusRequester(focusRequester),
                singleLine = true,
                textStyle = TextStyle(fontSize = 20.sp),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
        }
    }
}

private fun isValidOtp(otp: String) : Boolean {
    if (!(otp.isNotBlank() && otp.length <= 4))
        return false
    otp.forEach {
        if (!it.isDigit()) {
            return false
        }
    }
    return true
}