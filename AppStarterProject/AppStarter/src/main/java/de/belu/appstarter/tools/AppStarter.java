package de.belu.appstarter.tools;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import de.belu.appstarter.gui.InstalledAppsAdapter;

/**
 * Provides methods to start apps
 */
public class AppStarter
{
    /** Name of the launcher package */
    private static String mLauncherPackageName = null;

    /** Thread to start and watch if correct action is happening */
    private static Thread mStartAndWatchThread = null;

    private static Object mSyncObj = new Object();

    public static void stopWatchThread()
    {
        synchronized (mSyncObj)
        {
            Log.d(AppStarter.class.getName(), "Stop watch thread");
            try
            {
                if(mStartAndWatchThread != null && mStartAndWatchThread.isAlive())
                {
                    mStartAndWatchThread.interrupt();
                    mStartAndWatchThread.join();
                    mStartAndWatchThread = null;
                    Log.d(AppStarter.class.getName(), "Watchthread stopped");
                }
                else
                {
                    Log.d(AppStarter.class.getName(), "Watchthread was not alive, nothing to be done.");
                }
            }
            catch(Exception e)
            {
                Log.d(AppStarter.class.getName(), "Exception while stopping watchthread: \n" + e.getMessage());
            }
        }
    }

    /**
     * Starting an app by its package-name
     * @param context Context in that the app shall be started.
     * @param packageName Name of the apps package
     * @param isClickAction Indicates if this method is initiated by a click-action
     */
    public static void startAppByPackageName(final Context context, String packageName, Boolean isClickAction, Boolean isStartupAction, Boolean isClearPreviousInstancesForced)
    {
        try
        {
            // If currently a watchdog thread is running, stop it first
            stopWatchThread();

            synchronized (mSyncObj)
            {
                if (packageName != null && !packageName.equals(""))
                {
                    // Prepare the intent
                    final Intent launchIntent = InstalledAppsAdapter.getLaunchableIntentByPackageName(context, packageName);
                    if (isStartupAction || isClearPreviousInstancesForced)
                    {
                        // Start in new Task if startup -> perhaps prevents weird colors of kodi
                        // that some users reported..
                        Log.d(AppStarter.class.getName(), "Using FLAG_ACTIVITY_CLEAR_TASK: isStartupAction=" + isStartupAction.toString() + ", isClearPreviousInstancesForced=" + isClearPreviousInstancesForced.toString());
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    }

                    // Launch the intent
                    Log.d(AppStarter.class.getName(), "Starting launcher activity of package: " + packageName);
                    context.startActivity(launchIntent);
                }
            }
        }
        catch(Exception e)
        {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String errorReason = errors.toString();
            Log.d(AppStarter.class.getName(), "Failed to launch activity: \n" + errorReason);
        }
    }

    /**
     * Start settings view by packagename
     * @param context
     * @param packageName
     */
    public static void startSettingsViewByPackageName(Context context, String packageName)
    {
        try
        {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", packageName, null));

            context.startActivity(intent);
        }
        catch(Exception e)
        {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String errorReason = errors.toString();
            Log.d(AppStarter.class.getName(), "Failed to launch settings-activity: \n" + errorReason);
        }
    }

    /**
     * Uses package manager to find the package name of the device launcher. Usually this package
     * is "com.android.launcher" but can be different at times. This is a generic solution which
     * works on all platforms.`
     */
    public static synchronized String getLauncherPackageName(Context context)
    {
        // We only need to find the launcher package once as it should not change
        if(mLauncherPackageName == null)
        {
            // Create launcher Intent
            final Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);

            // Use PackageManager to get the launcher package name
            PackageManager pm = context.getPackageManager();
            ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            mLauncherPackageName = resolveInfo.activityInfo.packageName;
        }

        return mLauncherPackageName;
    }
}
