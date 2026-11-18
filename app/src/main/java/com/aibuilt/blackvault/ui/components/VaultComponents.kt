=== ABOUT: Shared building blocks — the top bar, the terminal input boxes, warning strips and section frames ===
package com.aibuilt.blackvault.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aibuilt.blackvault.ui.theme.VaultAmber
import com.aibuilt.blackvault.ui.theme.VaultBlack
import com.aibuilt.blackvault.ui.theme.VaultCard
import com.aibuilt.blackvault.ui.theme.VaultGreen
import com.aibuilt.blackvault.ui.theme.VaultGrey
import com.aibuilt.blackvault.ui.theme.VaultRed
import com.aibuilt.blackvault.ui.theme.VaultText
import com.aibuilt.blackvault.ui.theme.VaultTextDim

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = VaultGreen
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = VaultGreen
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = VaultBlack,
            titleContentColor = VaultGreen,
            actionIconContentColor = VaultGreen,
            navigationIconContentColor = VaultGreen
        )
    )
}

@Composable
fun TerminalField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    hint: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    transformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next
) {
    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = {
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
            },
            singleLine = true,
            isError = error != null,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            visualTransformation = transformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = imeAction),
            trailingIcon = trailing,
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VaultGreen,
                unfocusedBorderColor = VaultGrey,
                focusedLabelColor = VaultGreen,
                unfocusedLabelColor = VaultTextDim,
                cursorColor = VaultGreen,
                focusedTextColor = VaultText,
                unfocusedTextColor = VaultText,
                errorBorderColor = VaultRed,
                errorLabelColor = VaultRed,
                errorTextColor = VaultText
            ),
            modifier = Modifier.fillMaxWidth()
        )
        val support = error ?: hint
        if (support != null) {
            Text(
                text = if (error != null) "! $error" else support,
                style = MaterialTheme.typography.labelSmall,
                color = if (error != null) VaultRed else VaultTextDim,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun NoteBanner(
    text: String,
    color: Color = VaultAmber,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.6f)), RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f, fill = false)
        )
        trailing?.invoke()
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    accent: Color = VaultGreen,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(VaultCard, RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, VaultGrey), RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Text(
            "> $title",
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontFamily = FontFamily.Monospace
        )
        Column(Modifier.padding(top = 10.dp)) { content() }
    }
}

@Composable
fun MonoLabel(text: String, color: Color = VaultTextDim, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontFamily = FontFamily.Monospace,
        modifier = modifier
    )
}
