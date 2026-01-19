package com.gamebiller.tvlock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebiller.tvlock.domain.model.LockState
import com.gamebiller.tvlock.domain.model.toDisplayText

/**
 * Full-screen lock overlay
 * Displayed when station is not running
 */
@Composable
fun LockScreen(
    lockState: LockState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Lock Icon (Hidden Backdoor: Click 5 times to exit)
            var clickCount by remember { mutableIntStateOf(0) }
            val context = androidx.compose.ui.platform.LocalContext.current
            var showExitDialog by remember { mutableStateOf(false) }

            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = Color(0xFFB0B0B0),
                modifier = Modifier
                    .size(120.dp)
                    .clickable {
                        clickCount++
                        if (clickCount >= 5) {
                            showExitDialog = true
                            clickCount = 0
                        }
                    }
            )

            if (showExitDialog) {
                ExitDialog(
                    onDismiss = { showExitDialog = false },
                    onExit = {
                        // Open Android Settings
                        val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                        
                        // Or minimize app
                        // (context as? android.app.Activity)?.moveTaskToBack(true) 
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Main message
            Text(
                text = when (lockState) {
                    is LockState.Locked -> "🔒 SESSION NOT ACTIVE"
                    is LockState.GracePeriod -> "⏳ CHECKING CONNECTION..."
                    else -> "🔒 SESSION NOT ACTIVE"
                },
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Shop and Station info
            when (lockState) {
                is LockState.Locked -> {
                    if (lockState.shopName != null) {
                        Text(
                            text = lockState.shopName,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFB0B0B0),
                            textAlign = TextAlign.Center
                        )
                    }
                    if (lockState.stationName != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Station: ${lockState.stationName}",
                            fontSize = 28.sp,
                            color = Color(0xFF808080),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = lockState.reason.toDisplayText(),
                        fontSize = 24.sp,
                        color = Color(0xFF606060),
                        textAlign = TextAlign.Center
                    )
                }
                
                is LockState.GracePeriod -> {
                    Text(
                        text = lockState.shopName,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFB0B0B0),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Station: ${lockState.stationName}",
                        fontSize = 28.sp,
                        color = Color(0xFF808080),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Network unavailable (${lockState.secondsRemaining}s)",
                        fontSize = 24.sp,
                        color = Color(0xFFF44336),
                        textAlign = TextAlign.Center
                    )
                }
                
                else -> {}
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun ExitDialog(
    onDismiss: () -> Unit,
    onExit: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Emergency Exit") },
        text = {
            Column {
                Text("Enter Admin PIN to exit:")
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = pin,
                    onValueChange = { 
                        if (it.length <= 4) pin = it 
                        isError = false
                    },
                    singleLine = true,
                    isError = isError,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                if (isError) {
                    Text("Invalid PIN", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    if (pin == "5555") {
                        onExit()
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Exit App")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
