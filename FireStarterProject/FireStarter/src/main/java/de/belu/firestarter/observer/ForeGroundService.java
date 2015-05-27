package de.belu.firestarter.observer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;

import de.belu.firestarter.R;
import de.belu.firestarter.gui.MainActivity;
import de.belu.firestarter.tools.SettingsProvider;
import de.belu.firestarter.tools.Tools;


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

    /** BackgroundObserver to observe home-button */
    private BackgroundHomeButtonObserverThread mBackgroundHomeButtonObserverThread = null;

    /** Handler for Home-Button events */
    BackgroundHomeButtonObserverThread.OnHomeButtonClickedListener mHomeButtonClickedListener = new BackgroundHomeButtonObserverThread.OnHomeButtonClickedListener()
    {
        @Override
        public void onHomeButtonClicked()
        {
            Log("Received single home button click.");

            // Start single click package
            String startPackage = mSettings.getSingleClickApp();
            Log("Single-click start package is: " + startPackage);
            if(startPackage != null && !startPackage.equals(""))
            {
                Tools.startAppByPackageName(ForeGroundService.this, startPackage);
            }
        }

        @Override
        public void onHomeButtonDoubleClicked()
        {
            Log("Received double home button click.");

            // Start single click package
            String startPackage = mSettings.getDoubleClickApp();
            Log("Double-click start package is: " + startPackage);
            if(startPackage != null && !startPackage.equals(""))
            {
                Tools.startAppByPackageName(ForeGroundService.this, startPackage);
            }
        }
    };

    /** Handler for error-events */
    BackgroundHomeButtonObserverThread.OnServiceErrorListener mOnServiceErrorListener = new BackgroundHomeButtonObserverThread.OnServiceErrorListener()
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
        runnerThreadStart();

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
        runnerThreadStop();

        // Now stop the service
        Log("Stop Foreground Service");
        stopForeground(true);
        mIsForeGroundRunning = false;
    }

    /** Start the test runner */
    private void runnerThreadStart()
    {
        runnerThreadStop();

        Log("Start background thread.");
        mBackgroundHomeButtonObserverThread = new BackgroundHomeButtonObserverThread();
        mBackgroundHomeButtonObserverThread.setOnHomeButtonClickedListener(mHomeButtonClickedListener);
        mBackgroundHomeButtonObserverThread.setOnServiceErrorListener(mOnServiceErrorListener);
        mBackgroundHomeButtonObserverThread.start();
    }

    /** Stops the test runner */
    private void runnerThreadStop()
    {
        if(mBackgroundHomeButtonObserverThread != null && mBackgroundHomeButtonObserverThread.isAlive())
        {
            Log("Shut down background thread.");
            mBackgroundHomeButtonObserverThread.stopThread();
            try
            {
                mBackgroundHomeButtonObserverThread.join(2000);
            }
            catch(Exception e)
            {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                String errorReason = errors.toString();
                Log("Failed to stop thread: \n" + errorReason);
            }

            if(mBackgroundHomeButtonObserverThread != null && mBackgroundHomeButtonObserverThread.isAlive())
            {
                Log("Force shut down background thread.");
                mBackgroundHomeButtonObserverThread.interrupt();
                try
                {
                    mBackgroundHomeButtonObserverThread.join();
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
            Log("No background thread running..");
        }
        mBackgroundHomeButtonObserverThread = null;
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