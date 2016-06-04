package de.belu.appstarter.observer;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import de.belu.appstarter.tools.AppStarter;
import de.belu.appstarter.tools.SettingsProvider;


/**
 * Runs in the Background and observes the home button clicks
 */
public class BackgroundHomeButtonObserverThreadNonADB extends Thread
{
    /** Home-button-clicked-listener */
    private OnHomeButtonClickedListener mHomeButtonClickedListener = null;

    /** ServiceError listener */
    private OnServiceErrorListener mOnServiceErrorListener = null;

    /** Instance of settings */
    private SettingsProvider mSettings;

    /** Instance of the BroadcastReceiver */
    private BroadcastHelperReceiver mBroadcastReceiver = null;

    /** Indicates if the Thread is stopped */
    private Boolean mRun = true;

    /** Instance of Activity Manager */
    private ActivityManager mActivityManager = null;

    /** Default launcher package */
    private String mDefaultLauncherPackage;

    /** Last top package */
    private String mLastTopPackage = "";

    /** Thread to wait for second click */
    private Thread mWaitForSecondClickThread = null;

    /** Indicator for second click */
    private Boolean mSecondClickInTime = false;

    /** Listener for close system dialog events */
    private BroadcastHelperReceiver.OnReceivedCloseSystemDialog mReceivedCloseSystemDialogListener = new BroadcastHelperReceiver.OnReceivedCloseSystemDialog()
    {
        @Override
        public void onReceivedCloseSystemDialog(String reason)
        {
            if(reason.equals("homekey"))
            {
                clickActionDetected();
            }
        }
    };

    /**
     * Create new BackgroundObserverThread
     */
    public BackgroundHomeButtonObserverThreadNonADB(Context context, BroadcastHelperReceiver broadcastHelperReceiver)
    {
        // Get settings instance
        mSettings = SettingsProvider.getInstance(context);

        // Set our priority to minimal
        this.setPriority(Thread.MIN_PRIORITY);

        // Get instance of ActivityManager
        mActivityManager = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);

        // Get default-launcher package
        mDefaultLauncherPackage = AppStarter.getLauncherPackageName(context);

        // Set listener
        mBroadcastReceiver = broadcastHelperReceiver;
        mBroadcastReceiver.setOnReceivedCloseSystemDialog(mReceivedCloseSystemDialogListener);
    }

    public void stopThread()
    {
        mRun = false;
        try
        {
            Thread.sleep(500);
        }
        catch (InterruptedException ignore){}
        mBroadcastReceiver.setOnReceivedCloseSystemDialog(null);
    }
    
    /**
     * @param listener OnHomeButtonClickedLister to be added
     */
    public void setOnHomeButtonClickedListener(OnHomeButtonClickedListener listener)
    {
        mHomeButtonClickedListener = listener;
    }

    /**
     * @param listener OnServiceErrorListener to be added
     */
    public void setOnServiceErrorListener(OnServiceErrorListener listener)
    {
        mOnServiceErrorListener = listener;
    }

    /** Override run-method which is initiated on Thread-Start */
    @Override
    public void run()
    {
//        // Start endless-loop to observer the running TopActivity
//        while(mRun)
//        {
//            try
//            {
//                // Check top active package
//                List<ActivityManager.RunningTaskInfo> taskInfo = mActivityManager.getRunningTasks(1);
//                ComponentName componentInfo = taskInfo.get(0).topActivity;
//                String packageName = componentInfo.getPackageName();
//
//                Log.d(BackgroundHomeButtonObserverThreadNonADB.class.getName(), "TopPackage: " + packageName + " - " + componentInfo.getClassName());
//
//                // Check top package
//                if(packageName.equals(mDefaultLauncherPackage) && !packageName.equals(mLastTopPackage))
//                {
//                    // We had a change from any package to the launcher package
//                    clickActionDetected();
//                }
//
//                // Set last package name
//                mLastTopPackage = packageName;
//
//                // Sleep till next check
//                Thread.sleep(500);
//            }
//            catch (InterruptedException e)
//            {
//                StringWriter errors = new StringWriter();
//                e.printStackTrace(new PrintWriter(errors));
//                String errorReason = errors.toString();
//                Log.d(BackgroundHomeButtonObserverThreadNonADB.class.getName(), "Sleep interrupted: \n" + errorReason);
//            }
//        }
    }

    private void clickActionDetected()
    {
        Log.d("TEST", "click action detected");

        if(mWaitForSecondClickThread != null && mWaitForSecondClickThread.isAlive())
        {
            // Signal second click
            mSecondClickInTime = true;
        }
        else
        {
            // For each first home-button click disable immediately the jumpback mechanism
            AppStarter.stopWatchThread();

            // Create new thread to check for double click
            mWaitForSecondClickThread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Thread.sleep(mSettings.getDoubleClickInterval());
                        if(mSecondClickInTime)
                        {
                            // Fire double click event
                            fireHomeButtonDoubleClickedEvent();
                        }
                        else
                        {
                            // Fire single click event
                            fireHomeButtonClickedEvent();
                        }
                        mSecondClickInTime = false;
                    }
                    catch (InterruptedException ignore){ }
                }
            });
            mWaitForSecondClickThread.start();
            mSecondClickInTime = false;
        }
    }
    
    /**
     * Fire home button clicked event to all registered listeners
     */
    private void fireHomeButtonClickedEvent()
    {
        Thread fireThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if(mHomeButtonClickedListener != null)
                {
                    mHomeButtonClickedListener.onHomeButtonClicked();
                }
            }
        });
        fireThread.start();
    }
    
    /**
     * Fire home button double clicked event to all registered listeners
     */
    private void fireHomeButtonDoubleClickedEvent()
    {
        Thread fireThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if(mHomeButtonClickedListener != null)
                {
                    mHomeButtonClickedListener.onHomeButtonDoubleClicked();
                }
            }
        });
        fireThread.start();
    }

    /**
     * @param message Fire service error message
     */
    private void fireServiceErrorEvent(final String message)
    {
        Thread fireThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if(mOnServiceErrorListener != null)
                {
                    mOnServiceErrorListener.onServiceError(message);
                }
            }
        });
        fireThread.start();
    }
}
