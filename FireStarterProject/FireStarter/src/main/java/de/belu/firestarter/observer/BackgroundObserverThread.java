package de.belu.firestarter.observer;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import de.belu.firestarter.tools.Tools;

/**
 * Runs in the Background and observes the active TopActivty
 */
public class BackgroundObserverThread extends Thread
{
    /** Current context thread is running in */
    private Context mContext = null;

    /** Instance of Activity Manager */
    private ActivityManager mActivityManager = null;

    /** Default launcher package */
    private String mDefaultLauncherPackage;

    /**
     * Create new BackgroundObserverThread
     * @param context Context thread is running in
     */
    public BackgroundObserverThread(Context context)
    {
        // Get current context
        mContext = context;

        // Get instance of ActivityManager
        mActivityManager = (ActivityManager) mContext.getSystemService(mContext.ACTIVITY_SERVICE);

        // Get default-launcher package
        mDefaultLauncherPackage = Tools.getLauncherPackageName(mContext);

        // Set our priority to minimal
        this.setPriority(Thread.MIN_PRIORITY);
    }

    /** Override run-method which is initiated on Thread-Start */
    @Override
    public void run()
    {
        // Start endless-loop to observer the running TopActivity
        while(true)
        {
            try
            {
                // Check top active package
                List<ActivityManager.RunningTaskInfo> taskInfo = mActivityManager.getRunningTasks(1);
                ComponentName componentInfo = taskInfo.get(0).topActivity;
                String packageName = componentInfo.getPackageName();

                // Check top package
                if(packageName.equals(Tools.IKONOTVPACKAGE))
                {
                    // If ikono-tv force start our main activity
                    Log.d(BackgroundObserverThread.class.getName(), "Found Package: " + packageName);

                    // Always force start to our main-activty
                    Tools.startAppByPackageName(mContext, mContext.getApplicationContext().getPackageName());

                    // Sleep some more to prevent double-start..
                    Thread.sleep(1000);
                }
                else if(packageName.equals(mDefaultLauncherPackage) && !Tools.IsAmazonHomeScreenWanted)
                {
                    // If default-launcher start only if this is wanted by the user..
                    Tools.startAppByPackageName(mContext, mContext.getApplicationContext().getPackageName());

                    // Sleep some more to prevent double-start..
                    Thread.sleep(1000);
                }

                // Sleep till next check
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                String errorReason = errors.toString();
                Log.d(BackgroundObserverThread.class.getName(), "Sleep interrupted: \n" + errorReason);
                break;
            }
        }
    }
}
