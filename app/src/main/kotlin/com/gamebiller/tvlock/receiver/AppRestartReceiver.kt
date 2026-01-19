package com.gamebiller.tvlock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gamebiller.tvlock.ui.MainActivity
import timber.log.Timber

/**
 * Broadcast receiver that restarts the app if it's killed
 * Provides additional layer of protection against app closure
 */
class AppRestartReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("App restart triggered")
        
        // Restart MainActivity
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        context.startActivity(launchIntent)
    }
}
