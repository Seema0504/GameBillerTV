package com.gamebiller.tvlock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.gamebiller.tvlock.BuildConfig
import com.gamebiller.tvlock.ui.viewmodel.PairingState
import timber.log.Timber
import android.app.Activity

/**
 * Device pairing screen
 * Shown on first launch to pair device with a station
 */
@Composable
fun PairingScreen(
    pairingState: PairingState,
    onPairClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Timber.d("PairingScreen composable called, state: $pairingState")
    var stationCode by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)), // Dark background for TV
        contentAlignment = Alignment.Center
    ) {
        // Debug-only Back button (top-right corner)
        if (BuildConfig.DEBUG) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
            ) {
                Button(
                    onClick = {
                        (context as? Activity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF666666)
                    )
                ) {
                    Text("Exit (Debug)", color = Color.White)
                }
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(48.dp)
        ) {
            // Title
            Text(
                text = "Device Pairing",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Instruction
            Text(
                text = "Enter the station code provided by staff",
                fontSize = 24.sp,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Station Code Input
            OutlinedTextField(
                value = stationCode,
                onValueChange = { stationCode = it.uppercase() },
                label = { Text("Station Code") },
                placeholder = { Text("Enter code") },
                singleLine = true,
                enabled = pairingState !is PairingState.Loading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF2196F3),
                    unfocusedBorderColor = Color(0xFF808080),
                    focusedLabelColor = Color(0xFF2196F3),
                    unfocusedLabelColor = Color(0xFF808080)
                ),
                modifier = Modifier
                    .width(400.dp)
                    .height(80.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Pair Button
            Button(
                onClick = { onPairClick(stationCode) },
                enabled = stationCode.isNotBlank() && pairingState !is PairingState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3),
                    disabledContainerColor = Color(0xFF404040)
                ),
                modifier = Modifier
                    .width(400.dp)
                    .height(64.dp)
            ) {
                if (pairingState is PairingState.Loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "Pair Device",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Error Message
            if (pairingState is PairingState.Error) {
                Text(
                    text = pairingState.message,
                    fontSize = 20.sp,
                    color = Color(0xFFF44336),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Success Message
            if (pairingState is PairingState.Success) {
                Text(
                    text = "✓ Device paired successfully!",
                    fontSize = 20.sp,
                    color = Color(0xFF4CAF50),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
