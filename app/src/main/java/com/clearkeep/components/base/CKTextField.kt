package com.clearkeep.components.base

import androidx.compose.ui.Modifier
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun CKTextField(
        label: String,
        placeholder: String,
        textValue: MutableState<String>,
        modifier: Modifier = Modifier.fillMaxWidth(),
        keyboardType: KeyboardType = KeyboardType.Text,
        passwordVisibility: Boolean = false
) {
    OutlinedTextField(
            modifier = modifier,
            value = textValue.value,
            onValueChange = { textValue.value = it },
            label = {
                Text(label, style = MaterialTheme.typography.body2.copy(
                        color = Color.Gray
                ))
            },
            placeholder = { Text(placeholder, style = MaterialTheme.typography.body2.copy(
                    color = Color.Gray))
            },
            activeColor = Color.Red,
            inactiveColor = Color.Blue,
            textStyle = MaterialTheme.typography.body2.copy(
                    color = Color.Blue
            ),
            keyboardType = keyboardType,
            visualTransformation = if (!passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
    )
}