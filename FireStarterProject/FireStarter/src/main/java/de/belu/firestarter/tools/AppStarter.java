package de.belu.firestarter.tools;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import de.belu.firestarter.gui.InstalledAppsAdapter;

/**
 * Provides methods to start apps
 */
public class AppStarter
{
    /** Name of the launcher package */
    private static String mLauncherPackageName = null;

    /** Thread to start and watch if correct action is happening */
    private static Thread mStartAndWatchThread = null;

    /**
     * Starting an app by its package-name
     * @param context Context in that the app shall be started.
     * @param packageName Name of the apps package
     * @param isClickAction Indicates if this method is initiated by a click-action
     */
    public static synchronized void startAppByPackageName(final Context context, String packageName, Boolean isClickAction)
    {
        try
        {
            // If currently a watchdog thread is running, stop it first
            if(mStartAndWatchThread != null && mStartAndWatchThread.isAlive())
            {
                mStartAndWatchThread.interrupt();
                mStartAndWatchThread.join();
                mStartAndWatchThread = null;
            }

            if(packageName != null && !packageName.equals(""))
            {
                // Prepare the intent
                final Intent launchIntent = InstalledAppsAdapter.getLaunchableIntentByPackageName(context, packageName);

                // Launch the intent
                Log.d(Tools.class.getName(), "Starting launcher activity of package: " + packageName);
                context.startActivity(launchIntent);

                // In case of an click-action start the watchdog which prevents the default home-button
                // action that is to start the amazon home launcher
                if (isClickAction)
                {
                    // Check jumpback interval
                    SettingsProvider settings = SettingsProvider.getInstance(context);
                    final Integer watchdogTime = settings.getJumpbackWatchdogTime();

                    if (watchdogTime > 0)
                    {
                        // Get the name of the launcher package
                        final String launcherPackageName = getLauncherPackageName(context);

                        // Make sure this is not the launcherpackage which have been started
                        if (!packageName.equals(launcherPackageName))
                        {
                            // Now prepare and start thread
                            mStartAndWatchThread = new Thread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    try
                                    {
                                        Log.d(AppStarter.class.getName(), "JumpbackWatchdog:: Start jumpback protection");

                                        // First get needed information
                                        ActivityManager activityManager = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);

                                        // Start the observation
                                        Long startTime = System.currentTimeMillis();
                                        while (true)
                                        {
                                            // Check topmost package
                                            List<ActivityManager.RunningTaskInfo> taskInfo = activityManager.getRunningTasks(1);
                                            ComponentName componentInfo = taskInfo.get(0).topActivity;
                                            String topActivityPackageName = componentInfo.getPackageName();

                                            // If top package is launcher start intent again
                                            if (topActivityPackageName.equals(launcherPackageName))
                                            {
                                                Log.d(AppStarter.class.getName(), "JumpbackWatchdog:: Amazon home was topmost, start intent again");
                                                context.startActivity(launchIntent);
                                            }

                                            // Sleep 200ms
                                            if ((System.currentTimeMillis() - startTime) > watchdogTime)
                                            {
                                                Log.d(AppStarter.class.getName(), "JumpbackWatchdog:: Stop jumpback protection");
                                                break;
                                            }
                                            Thread.sleep(300);
                                            if ((System.currentTimeMillis() - startTime) > watchdogTime)
                                            {
                                                Log.d(AppStarter.class.getName(), "JumpbackWatchdog:: Stop jumpback protection");
                                                break;
                                            }
                                        }
                                    }
                                    catch (Exception e)
                                    {
                                        StringWriter errors = new StringWriter();
                                        e.printStackTrace(new PrintWriter(errors));
                                        String errorReason = errors.toString();
                                        Log.d(AppStarter.class.getName(), "JumpbackWatchdog:: Exception: \n" + errorReason);
                                    }
                                }
                            });
                            mStartAndWatchThread.setPriority(Thread.MIN_PRIORITY);
                            mStartAndWatchThread.start();
                        }
                    }
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
