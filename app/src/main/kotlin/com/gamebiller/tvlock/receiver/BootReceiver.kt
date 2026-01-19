package com.gamebiller.tvlock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gamebiller.tvlock.ui.MainActivity
import timber.log.Timber

/**
 * Broadcast receiver that launches the app on device boot
 * Ensures TV is locked immediately after power restore
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Timber.d("Device booted, launching GameBiller Lock app")
            
            // Launch MainActivity
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            
            context.startActivity(launchIntent)
        }
    }
}
