package de.belu.firestarter.observer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import de.belu.firestarter.R;
import de.belu.firestarter.gui.MainActivity;
import de.belu.firestarter.tools.AppStarter;
import de.belu.firestarter.tools.SettingsProvider;
import de.belu.firestarter.tools.Updater;


/**
 * Service to run tasks in background:
 * Here the service is set to ForeGroundService, which is the only
 * Service that is not killed by the Android OS.
 */
public class ForeGroundService extends Service
{
    /** Start foreground action */
    public static final String FOREGROUNDSERVICE_START = "com.belu.luki.uispeedapptester.action.startforeground";

    /** Stop forground action */
    public static final String FOREGROUNDSERVICE_STOP = "com.belu.luki.uispeedapptester.action.stopforeground";

    /** ID of the foreground service */
    public static final int FOREGROUNDSERVICE_ID = 101;

    /** Request code of the notification */
    public static final int REQUEST_CODE = 1;

    /** Indicates if the service is currently running as foreground service */
    private Boolean mIsForeGroundRunning = false;

    /** Simple binder to interact with the service */
    private final IBinder mBinder = new TestRunnerLocalBinder();

    /** Handler to call things on uiThread */
    private Handler mHandler;

    /** Access to settings */
    private SettingsProvider mSettings;

    /** BackgroundObserver to observe home-button with ADB */
    private BackgroundHomeButtonObserverThreadADB mBackgroundHomeButtonObserverThreadADB = null;

    /** BackgroundObserver to observe home-button NOT with ADB */
    private BackgroundHomeButtonObserverThreadNonADB mBackgroundHomeButtonObserverThreadNonADB = null;

    /** Broadcast Helper receiver */
    BroadcastHelperReceiver mBroadcastHelperReceiver = new BroadcastHelperReceiver();

    /** Timer to check for updates */
    private Timer mTimer;

    // Handler for ScreenOnEvents
    BroadcastReceiver mScreenOnReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // Possibly used again in future
        }
    };

    /** Handler for Home-Button events */
    OnHomeButtonClickedListener mHomeButtonClickedListener = new OnHomeButtonClickedListener()
    {
        @Override
        public void onHomeButtonClicked()
        {
            Log("Received single home button click.");

            // Start single click package
            String startPackage = mSettings.getSingleClickApp();
            Log("Single-click start package is: " + startPackage);
            AppStarter.startAppByPackageName(ForeGroundService.this, startPackage, true, false, mSettings.getClearPreviousInstancesForSingleClick());
        }

        @Override
        public void onHomeButtonDoubleClicked()
        {
            Log("Received double home button click.");

            // Start single click package
            String startPackage = mSettings.getDoubleClickApp();
            Log("Double-click start package is: " + startPackage);
            AppStarter.startAppByPackageName(ForeGroundService.this, startPackage, true, false, mSettings.getClearPreviousInstancesForDoubleClick());
        }
    };

    /** Handler for error-events */
    OnServiceErrorListener mOnServiceErrorListener = new OnServiceErrorListener()
    {
        @Override
        public void onServiceError(final String message)
        {
            ForeGroundService.this.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Toast.makeText(ForeGroundService.this, "FireStarter: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    /**
     * @return Binder object
     */
    @Override
    public IBinder onBind(Intent arg0)
    {
        Log("onBind");
        return mBinder;
    }

    /**
     * Handle start commands (start-stop foreground)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log("onStartCommand: Received start id " + startId + ": " + intent);

        if (FOREGROUNDSERVICE_START.equals(intent.getAction()))
        {
            foreGroundServiceStart();
        }
        else if (FOREGROUNDSERVICE_STOP.equals(intent.getAction()))
        {
            foregroundServiceStop();
        }

        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        Log("onCreate");
        mHandler = new Handler();
        mSettings = SettingsProvider.getInstance(this);
    }

    @Override
    public void onDestroy()
    {
        Log("onDestroy");
        super.onDestroy();
    }

    /** Start the foreground service */
    private void foreGroundServiceStart()
    {
        if(mIsForeGroundRunning)
        {
            Log("Foreground Service already running.");
            return;
        }

        Log("Start Foreground Service");
        startForeground(FOREGROUNDSERVICE_ID, getCompatNotification());

        // Now start the Thread
        startBackgroundActions();

        // Set the variable
        mIsForeGroundRunning = true;
    }

    /** Stop the foreground service */
    private void foregroundServiceStop()
    {
        if(!mIsForeGroundRunning)
        {
            Log("No Foreground Service running to stop.");
            return;
        }

        // First try to stop the runner thread
        stopBackgroundActions();

        // Now stop the service
        Log("Stop Foreground Service");
        stopForeground(true);
        mIsForeGroundRunning = false;
    }

    /** Start the actions that take place in background */
    private void startBackgroundActions()
    {
        stopBackgroundActions();

        // Schedule task every hour
        Log("Start update task.");
        Integer everyXminute = 60;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
        Calendar calendar = Calendar.getInstance();
        Log("Current time: " + sdf.format(calendar.getTime()));
        calendar.add(Calendar.MINUTE, 2);
        Log("Start: " + sdf.format(calendar.getTime()) + " | Every: " + everyXminute + " minutes");

        mTimer = new Timer();
        TimerTask timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                try
                {
                    Log("Start scheduled update-check:");
                    Updater updater = new Updater();
                    updater.checkForUpdate(true);
                }
                catch(Exception e)
                {
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));
                    String errorReason = errors.toString();
                    Log("ERROR in timertask: \n" + errorReason);
                }
            }
        };
        mTimer.schedule(timerTask, calendar.getTime(), 1000*60*everyXminute);

        if(mSettings.getBackgroundObservationViaAdb())
        {
            Log("Start background thread for ADB observation.");
            mBackgroundHomeButtonObserverThreadADB = new BackgroundHomeButtonObserverThreadADB(this);
            mBackgroundHomeButtonObserverThreadADB.setOnHomeButtonClickedListener(mHomeButtonClickedListener);
            mBackgroundHomeButtonObserverThreadADB.setOnServiceErrorListener(mOnServiceErrorListener);
            mBackgroundHomeButtonObserverThreadADB.start();
        }
        else
        {
            Log("Start background thread for NON-ADB observation.");

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BroadcastHelperReceiver.ACTION_CLOSE_SYSTEM_DIALOGS);
            registerReceiver(mBroadcastHelperReceiver, intentFilter);

            mBackgroundHomeButtonObserverThreadNonADB = new BackgroundHomeButtonObserverThreadNonADB(this, mBroadcastHelperReceiver);
            mBackgroundHomeButtonObserverThreadNonADB.setOnHomeButtonClickedListener(mHomeButtonClickedListener);
            mBackgroundHomeButtonObserverThreadNonADB.setOnServiceErrorListener(mOnServiceErrorListener);
            mBackgroundHomeButtonObserverThreadNonADB.start();
        }

        // Register screen on receiver
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        registerReceiver(mScreenOnReceiver, filter);
    }

    /** Stops the actions in the background */
    private void stopBackgroundActions()
    {
        // Disable NON-ADB observation
        try
        {
            unregisterReceiver(mBroadcastHelperReceiver);
        }
        catch(Exception ignore) {}
        if(mBackgroundHomeButtonObserverThreadNonADB != null && mBackgroundHomeButtonObserverThreadNonADB.isAlive())
        {
            Log("Shut down background thread for NON-ADB observation.");
            mBackgroundHomeButtonObserverThreadNonADB.stopThread();
            mBackgroundHomeButtonObserverThreadNonADB.interrupt();
            try
            {
                mBackgroundHomeButtonObserverThreadNonADB.join();
            }
            catch (Exception e)
            {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                String errorReason = errors.toString();
                Log("Failed to stop thread: \n" + errorReason);
            }
        }
        else
        {
            Log("No background thread for NON-ADB observation running..");
        }

        // Disable ADB observation
        if(mBackgroundHomeButtonObserverThreadADB != null && mBackgroundHomeButtonObserverThreadADB.isAlive())
        {
            Log("Shut down background thread for ADB observation.");
            mBackgroundHomeButtonObserverThreadADB.stopThread();
            try
            {
                mBackgroundHomeButtonObserverThreadADB.join(2000);
            }
            catch(Exception e)
            {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                String errorReason = errors.toString();
                Log("Failed to stop thread: \n" + errorReason);
            }

            if(mBackgroundHomeButtonObserverThreadADB != null && mBackgroundHomeButtonObserverThreadADB.isAlive())
            {
                Log("Force shut down background thread.");
                mBackgroundHomeButtonObserverThreadADB.interrupt();
                try
                {
                    mBackgroundHomeButtonObserverThreadADB.join();
                }
                catch (Exception e)
                {
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));
                    String errorReason = errors.toString();
                    Log("Failed to stop thread: \n" + errorReason);
                }
            }
        }
        else
        {
            Log("No background thread for ADB observation running..");
        }
        mBackgroundHomeButtonObserverThreadADB = null;

        // Then check for running timers
        if(mTimer != null)
        {
            try
            {
                Log("Stop timed update checks..");
                mTimer.cancel();
                mTimer = null;
            }
            catch(Exception e)
            {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                String errorReason = errors.toString();
                Log("Failed to stop timed update checks: \n" + errorReason);
            }
        }
        else
        {
            Log("No timed update check is active at the moment.");
        }

        // Stop screen on receiver
        try
        {
            unregisterReceiver(mScreenOnReceiver);
        }
        catch(Exception ignore){}
    }

    /**
     * @return Indicates if the foreground service is running
     */
    public Boolean getIsForeGroundRunning()
    {
        return mIsForeGroundRunning;
    }

    /**
     * @return Notification object which is shown while foreground service is running
     */
    private Notification getCompatNotification()
    {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("ForeGroundService Started.")
                .setTicker("Service is running..")
                .setWhen(System.currentTimeMillis());

        Intent startIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, REQUEST_CODE, startIntent, 0);
        builder.setContentIntent(contentIntent);
        Notification notification = builder.build();

        return notification;
    }

    /** Runs runnables on the UI-Thread */
    private void runOnUiThread(Runnable runnable)
    {
        mHandler.post(runnable);
    }

    /**
     * Log messages to logcat
     * @param message message to log
     */
    private void Log(String message)
    {
        Log.d(ForeGroundService.class.getName(), message);
    }

    /**
     * Simple binder class to get current TestRunnerService
     */
    public class TestRunnerLocalBinder extends Binder
    {
        public ForeGroundService getService()
        {
            return ForeGroundService.this;
        }
    }
}