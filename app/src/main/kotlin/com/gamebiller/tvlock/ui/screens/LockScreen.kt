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
import androidx.compose.runtime.Composable
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
            // Lock Icon
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = Color(0xFFB0B0B0),
                modifier = Modifier.size(120.dp)
            )
            
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
