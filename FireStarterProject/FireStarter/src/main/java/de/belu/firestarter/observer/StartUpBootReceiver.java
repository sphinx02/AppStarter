package de.belu.firestarter.observer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.belu.firestarter.tools.Tools;

/**
 * Receiver for Boot-Complete Broadcast
 */
public class StartUpBootReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))
        {
            Log.d(StartUpBootReceiver.class.getName(), "Received BOOT_COMPLETED intent.");

            // Start foreground service
            Intent startIntent = new Intent(context, ForeGroundService.class);
            startIntent.setAction(ForeGroundService.FOREGROUNDSERVICE_START);
            context.startService(startIntent);

            // Start firedTV launcher
            Tools.startFiredTv(context);
        }
    }
}
