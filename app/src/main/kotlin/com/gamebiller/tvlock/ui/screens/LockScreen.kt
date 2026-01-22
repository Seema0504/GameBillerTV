package com.gamebiller.tvlock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebiller.tvlock.BuildConfig
import com.gamebiller.tvlock.domain.model.LockState
import com.gamebiller.tvlock.domain.model.toDisplayText
import android.app.Activity
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation

/**
 * Full-screen lock overlay
 * Displayed when station is not running
 */
@Composable
fun LockScreen(
    lockState: LockState,
    onUnpair: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Admin Reset State
    var clickCount by remember { mutableIntStateOf(0) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    
    // Reset click count after 3 seconds of inactivity
    LaunchedEffect(clickCount) {
        if (clickCount > 0) {
            kotlinx.coroutines.delay(3000)
            clickCount = 0
        }
    }
    
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Enter Admin PIN") },
            text = {
                Column {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { 
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                pinInput = it
                                pinError = false
                            }
                        },
                        label = { Text("PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = pinError
                    )
                    if (pinError) {
                        Text(
                            text = "Incorrect PIN",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    if (pinInput == "1100") {
                        showPinDialog = false
                        showConfirmDialog = true
                    } else {
                        pinError = true
                    }
                }) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Admin Reset") },
            text = { Text("Unpair this device? This will require re-pairing.") },
            confirmButton = {
                TextButton(onClick = { 
                    showConfirmDialog = false
                    onUnpair()
                }) {
                    Text("Unpair")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
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
            // Lock Icon (Hidden Admin Reset)

            androidx.compose.material3.IconButton(
                onClick = {
                    clickCount++
                    if (clickCount >= 10) {
                        showPinDialog = true
                        clickCount = 0
                        pinInput = ""
                        pinError = false
                    }
                },
                modifier = Modifier.size(160.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Admin Reset",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(120.dp)
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
                    // Shop name removed as per request
                    
                    if (lockState.stationName != null) {
                        // Spacer(modifier = Modifier.height(8.dp)) // Removed spacer since shop name is gone
                        Text(
                            text = "Station: ${lockState.stationName}",
                            fontSize = 28.sp,
                            color = Color(0xFF808080),
                            textAlign = TextAlign.Center
                        )
                    }
                    // Redundant "Session Stopped" text removed as per request
                }
                
                is LockState.GracePeriod -> {
                    // Shop name removed as per request

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
                    if (pin == "1100") {
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
