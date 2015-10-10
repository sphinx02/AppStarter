package de.belu.firestarter.observer;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.SequenceInputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;

import de.belu.firestarter.tools.AppStarter;
import de.belu.firestarter.tools.SettingsProvider;


/**
 * Runs in the Background and observes the home button clicks
 */
public class BackgroundHomeButtonObserverThreadADB extends Thread
{
    public static final String HANDLE_TOO_MUCH_FAILS = "HANDLE_TO_MUCH_FAILS";

    /** Name / IP of the device to be connected via TCP */
    private final String CONNECTDEVICETCP = "localhost";

    /** Name / IP of the device to be connected via EMULATOR */
    private final String CONNECTDEVICEEMU = "emulator";
    
    /** Home-button-clicked-listener */
    private OnHomeButtonClickedListener mHomeButtonClickedListener = null;

    /** ServiceError listener */
    private OnServiceErrorListener mOnServiceErrorListener = null;
    
    /** Thread to wait for second click */
    private Thread mWaitForSecondClickThread = null;
    
    /** Indicator for second click */
    private Boolean mSecondClickInTime = false;

    /** Holds an error message in case something is wrong */
    private String mErrorMessage = "Service is not yet up and running..";

    /** Boolean indicates if background-service shall run */
    private Boolean mRun = true;

    /** Boolean indicating if the adb client is successfully connected */
    private Boolean mIsConnected = false;

    /** Indicates if logcat has been cleared successfull */
    private Boolean mIsLogcatCleared = false;

    /** Indicates that the logcat-clear thread have found an error message */
    private Boolean mIsLogcatErrorMessageFound = false;

    /** String indicating the name of the current adb-device */
    private String mAdbDevice = null;

    /** Counts failed ADB connection tries */
    private Integer mFailCounter = 0;

    /** Process object */
    private Process mProcess = null;

    /** Instance of settings */
    private SettingsProvider mSettings;

    /** Indicates if the observation is currently running */
    private Boolean mIsObservationRunning = false;

    /** Store the read bytes count */
    private int mReadBytes;

     /** Store the read data */
    private char[] mReadBuffer = new char[4096];

     /** Used reader */
    private BufferedReader mReader = null;

    /**
     * Create new BackgroundObserverThread
     */
    public BackgroundHomeButtonObserverThreadADB(Context context)
    {
        // Get settings instance
        mSettings = SettingsProvider.getInstance(context);

        // Set our priority to minimal
        this.setPriority(Thread.MIN_PRIORITY);
    }

    /** Try to stop thread */
    public void stopThread()
    {
        mRun = false;
        if(!mIsObservationRunning)
        {
            Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "stopThread(): wait till current process finished.");
            try
            {
                this.join(3000);
            }
            catch (InterruptedException e)
            {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                String errorReason = errors.toString();
                Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "stopThread(): Exception: \n" + errorReason);
            }
        }
        else
        {
            Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "stopThread(): killing process.");
            killCurrentProcess("stopThread()");
        }

        Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "stopThread(): stop other threads.");
        if(mWaitForSecondClickThread != null && mWaitForSecondClickThread.isAlive())
        {
            mWaitForSecondClickThread.interrupt();
            try
            {
                mWaitForSecondClickThread.join();
            }
            catch (InterruptedException e)
            {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                String errorReason = errors.toString();
                Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "stopThread(): Exception: \n" + errorReason);
            }
            mWaitForSecondClickThread = null;
        }
        Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "stopThread(): finished.");
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
        // Start endless-loop to observer the running TopActivity
        while(true)
        {
            try
            {
                mErrorMessage = null;
                mIsObservationRunning = false;
                Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "Starting HomeButtonObserver.");

//                // Kill running adb server
//                Thread killAdbServerThread = new Thread(new Runnable()
//                {
//                    @Override
//                    public void run()
//                    {
//                        try
//                        {
//                            Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "KILLADBSERVERTHREAD: try to kill running adb-server");
//
//                            // Run process
//                            mProcess = Runtime.getRuntime().exec(new String[]{"adb", "kill-server"});
//                            mProcess.waitFor();
//                        }
//                        catch(Exception e)
//                        {
//                            StringWriter errors = new StringWriter();
//                            e.printStackTrace(new PrintWriter(errors));
//                            String errorReason = errors.toString();
//                            Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "KILLADBSERVERTHREAD: Exception: \n" + errorReason);
//                        }
//                    }
//                });
//                killAdbServerThread.start();
//                killAdbServerThread.join(3000);
//
//                if(!mRun)
//                {
//                    break;
//                }
//
//                // Check if still alive after timeout
//                if(killAdbServerThread.isAlive())
//                {
//                    // Try to kill process
//                    killCurrentProcess("KILLADBSERVERTHREAD");
//                }
//                killAdbServerThread = null;

                // Init some variables
                mIsConnected = false;

                // Reset adb device name
                mAdbDevice = null;

                // Check if emulator is running
                Thread getAdbEmuDeviceThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "GETADBEMUDEVICETHREAD: try get adb device");

                            // Run process
                            mProcess = Runtime.getRuntime().exec(new String[]{"adb", "devices"});

                            // Get output reader
                            mReader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
                            try
                            {
                                // Reads stdout of process
                                while ((mReadBytes = mReader.read(mReadBuffer)) > 0)
                                {
                                    String message = String.valueOf(mReadBuffer, 0, mReadBytes);
                                    Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "GETADBEMUDEVICETHREAD: adb received: " + message);
                                    if(message.contains(CONNECTDEVICEEMU))
                                    {
                                        // Split message by any whitespace:
                                        String[] lines = message.split("\\s+");
                                        for(String line : lines)
                                        {
                                            if(line.contains(CONNECTDEVICEEMU))
                                            {
                                                mAdbDevice = line;
                                                break;
                                            }
                                        }

                                        if(mAdbDevice != null)
                                        {
                                            Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "GETADBEMUDEVICETHREAD: adb device found: " + mAdbDevice);
                                            mIsConnected = true;
                                        }
                                    }
                                }
                            }
                            finally
                            {
                                mReader.close();
                            }
                            mProcess.waitFor();
                        }
                        catch(Exception e)
                        {
                            StringWriter errors = new StringWriter();
                            e.printStackTrace(new PrintWriter(errors));
                            String errorReason = errors.toString();
                            Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "GETADBEMUDEVICETHREAD: Exception: \n" + errorReason);
                        }
                    }
                });
                getAdbEmuDeviceThread.start();
                getAdbEmuDeviceThread.join(5000);

                if(!mRun)
                {
                    break;
                }

                // Check if still alive after timeout
                if(getAdbEmuDeviceThread.isAlive())
                {
                    // Try to kill process
                    killCurrentProcess("GETADBEMUDEVICETHREAD");
                }
                getAdbEmuDeviceThread = null;

                if(!mRun)
                {
                    break;
                }

                // Only if no emulator have been found, try to connect localhost:
                if(!mIsConnected)
                {
                    // Reset adb device name
                    mAdbDevice = null;

                    // Connect an instance on localhost
                    Thread connectAdbThread = new Thread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "CONNECTHREAD: try connect to " + CONNECTDEVICETCP);

                                // Run process
                                mProcess = Runtime.getRuntime().exec(new String[]{"adb", "connect", CONNECTDEVICETCP});

                                // Get output reader
                                mReader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
                                try
                                {
                                    // Reads stdout of process
                                    while ((mReadBytes = mReader.read(mReadBuffer)) > 0)
                                    {
                                        String message = String.valueOf(mReadBuffer, 0, mReadBytes);
                                        Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "CONNECTHREAD: adb received: " + message);
                                        if (message.contains("connected") && !message.contains("unable"))
                                        {
                                            mIsConnected = true;
                                        }
                                    }
                                }
                                finally
                                {
                                    mReader.close();
                                }
                                mProcess.waitFor();
                            }
                            catch (Exception e)
                            {
                                StringWriter errors = new StringWriter();
                                e.printStackTrace(new PrintWriter(errors));
                                String errorReason = errors.toString();
                                Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "CONNECTHREAD: Exception: \n" + errorReason);
                            }
                        }
                    });
                    connectAdbThread.start();
                    connectAdbThread.join(5000);

                    if(!mRun)
                    {
                        break;
                    }

                    // Check if still alive after timeout
                    if (connectAdbThread.isAlive())
                    {
                        // Try to kill process
                        killCurrentProcess("CONNECTHREAD");
                    }
                    connectAdbThread = null;

                    if (!mIsConnected)
                    {
                        throw new Exception("Error while connecting to adb.");
                    }
                    Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "Adb is connected, check devices..");

                    // Check device name
                    Thread getAdbDeviceThread = new Thread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "GETADBDEVICETHREAD: try get adb device");

                                // Run process
                                mProcess = Runtime.getRuntime().exec(new String[]{"adb", "devices"});

                                // Get output reader
                                mReader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
                                try
                                {
                                    // Reads stdout of process
                                    while ((mReadBytes = mReader.read(mReadBuffer)) > 0)
                                    {
                                        String message = String.valueOf(mReadBuffer, 0, mReadBytes);
                                        Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "GETADBDEVICETHREAD: adb received: " + message);
                                        if (message.contains(CONNECTDEVICETCP))
                                        {
                                            // Split message by any whitespace:
                                            String[] lines = message.split("\\s+");
                                            for (String line : lines)
                                            {
                                                if (line.contains(CONNECTDEVICETCP))
                                                {
                                                    mAdbDevice = line;
                                                    break;
                                                }
                                            }

                                            if (mAdbDevice != null)
                                            {
                                                Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "GETADBDEVICETHREAD: adb device found: " + mAdbDevice);
                                            }
                                        }
                                    }
                                }
                                finally
                                {
                                    mReader.close();
                                }
                                mProcess.waitFor();
                            }
                            catch (Exception e)
                            {
                                StringWriter errors = new StringWriter();
                                e.printStackTrace(new PrintWriter(errors));
                                String errorReason = errors.toString();
                                Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "GETADBDEVICETHREAD: Exception: \n" + errorReason);
                            }
                        }
                    });
                    getAdbDeviceThread.start();
                    getAdbDeviceThread.join(3000);

                    if(!mRun)
                    {
                        break;
                    }

                    // Check if still alive after timeout
                    if (getAdbDeviceThread.isAlive())
                    {
                        // Try to kill process
                        killCurrentProcess("GETADBDEVICETHREAD");
                    }
                    getAdbDeviceThread = null;
                }

                if(mAdbDevice == null)
                {
                    throw new Exception("Error finding the correct adb device.");
                }
                Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "Adb device found, empty logcat..");

                // Reset empty logcat flag
                mIsLogcatCleared = false;
                mIsLogcatErrorMessageFound = false;

                // Empty logcat
                Thread emptyLogCatThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "EMPTYLOGCATTHREAD: try clear logcat");

                            // Run process
                            mProcess = Runtime.getRuntime().exec(new String[]{"adb", "-s", mAdbDevice, "logcat", "-c"});

                            // Get output reader
                            mReader = new BufferedReader(new InputStreamReader(new SequenceInputStream(mProcess.getInputStream(), mProcess.getErrorStream())));
                            try
                            {
                                // Reads stdout of process
                                while ((mReadBytes = mReader.read(mReadBuffer)) > 0)
                                {
                                    String message = String.valueOf(mReadBuffer, 0, mReadBytes);
                                    Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "EMPTYLOGCATTHREAD: adb received: " + message);
                                    if(message.contains("- waiting for device -"))
                                    {
                                        Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "EMPTYLOGCATTHREAD: device name must be wrong.. ");
                                        mIsLogcatErrorMessageFound = true;
                                    }
                                }
                            }
                            finally
                            {
                                mReader.close();
                            }
                            mProcess.waitFor();
                        }
                        catch(Exception e)
                        {
                            StringWriter errors = new StringWriter();
                            e.printStackTrace(new PrintWriter(errors));
                            String errorReason = errors.toString();
                            Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "EMPTYLOGCATTHREAD: Exception: \n" + errorReason);
                        }
                    }
                });
                emptyLogCatThread.start();
                emptyLogCatThread.join(5000);

                if(!mRun)
                {
                    break;
                }

                // Check if still alive after timeout
                if(emptyLogCatThread.isAlive())
                {
                    // Try to kill process
                    killCurrentProcess("EMPTYLOGCATTHREAD");
                }
                else
                {
                    // If thread is no more alive and no error message have been found, clearing logcat was successfull
                    if(!mIsLogcatErrorMessageFound)
                    {
                        mIsLogcatCleared = true;
                    }
                }
                emptyLogCatThread = null;

                if(!mIsLogcatCleared)
                {
                    throw new Exception("Clearing logcat failed.");
                }
                Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "Adb logcat cleared, now start observation..");

                // Start logcat with proper filters
                mProcess = Runtime.getRuntime().exec(new String[]{"adb", "-s", mAdbDevice, "logcat", "ActivityManager:I", "*:S"});

                // Seems as everything is fine, so reset fail-counter
                mIsObservationRunning = true;
                mFailCounter = 0;

                // Get output reader// Get output reader
                mReader = new BufferedReader(new InputStreamReader(new SequenceInputStream(mProcess.getInputStream(), mProcess.getErrorStream())));
                try
                {
                    // Reads stdout of process
                    while ((mReadBytes = mReader.read(mReadBuffer)) > 0)
                    {
                        String message = String.valueOf(mReadBuffer, 0, mReadBytes);
                        //Log.d(BackgroundHomeButtonObserverThread.class.getName(), "OBSERVATION: adb received: " + message);
                        if(message.startsWith("I/ActivityManager") && message.contains("act=android.intent.action.MAIN cat=[android.intent.category.HOME]"))
                        {
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
                    }
                }
                finally
                {
                    mReader.close();
                }
                mProcess.waitFor();

                Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "Lost connection to adb..");
                mErrorMessage = "Lost connection to adb..";
                fireServiceErrorEvent(mErrorMessage);
            }
            catch (Exception e)
            {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                String errorReason = errors.toString();
                Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "Error in BackgroundObserver: \n" + errorReason);

                mErrorMessage = "Exception: " + e.getMessage();
                fireServiceErrorEvent(mErrorMessage);
            }

            mIsObservationRunning = false;

            // Check if we shall run again:
            if(mRun)
            {
                // FailCounter Handling
                mFailCounter++;
                if(mFailCounter >= 3)
                {
                    Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "Too much fails, call fail handling..");
                    fireServiceErrorEvent(HANDLE_TOO_MUCH_FAILS);
                    mFailCounter = 0;
                }
                else
                {
                    Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "Restart observer in a few seconds..");
                }

                try
                {
                    Thread.sleep(1500);
                }
                catch (InterruptedException ignore)
                {
                }

                if (mRun)
                {
                    // Wait some time to try again
                    mErrorMessage = "Something went wrong, restarting HomeButtonObserver now...";
                    fireServiceErrorEvent(mErrorMessage);
                }
            }
            else
            {
                break;
            }
        }
        Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "Observer have been stopped..");
    }

    /**
     * Trys to kill current running process
     * @param component Name of the initiating component
     */
    private void killCurrentProcess(String component)
    {
        // Try to kill process
        try
        {
            Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), component + ": Try to kill running process");
            Integer exitCode = killUnixProcess(mProcess);
            if(exitCode != 0)
            {
                throw new Exception("Received exit-code was not zero but " + exitCode.toString());
            }

            try
            {
                Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "killProcess: call adb --help");

                // Run process
                mProcess = Runtime.getRuntime().exec(new String[]{"adb", "--help"});

                // Get output reader
                mReader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
                try
                {
                    // Reads stdout of process
                    while ((mReadBytes = mReader.read(mReadBuffer)) > 0)
                    {
                        String message = String.valueOf(mReadBuffer, 0, mReadBytes);
                        Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "killProcess: adb received: " + message);
                    }
                }
                finally
                {
                    mReader.close();
                }
                mProcess.waitFor();

                Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "killProcess: call adb --help finished..");
            }
            catch(Exception e)
            {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                String errorReason = errors.toString();
                Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "killProcess: Exception: \n" + errorReason);
            }

            Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), component + ": Killed running process successful");
        }
        catch(Exception e)
        {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String errorReason = errors.toString();
            Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), component + ": Exception while killing process: \n" + errorReason);
        }
    }

    /**
     * @param process Process of interest
     * @return Process PID
     * @throws Exception if process is no unix-process
     */
    private static int getUnixPID(Process process) throws Exception
    {
        Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "GetUnixPID: process name: " + process.getClass().getName());
        if (process.getClass().getName().startsWith("java.lang."))
        {
            Class cl = process.getClass();
            Field field = cl.getDeclaredField("pid");
            field.setAccessible(true);
            Object pidObject = field.get(process);
            return (Integer) pidObject;
        }
        else
        {
            throw new IllegalArgumentException("Needs to be a UNIXProcess");
        }
    }

    /**
     * Trys to kill given process
     * @param process Process to be killed
     * @return  ExitValue of the kill-process
     * @throws Exception if process is no unix-process
     */
    private static int killUnixProcess(Process process) throws Exception
    {
        Integer pid = getUnixPID(process);
        Log.d(BackgroundHomeButtonObserverThreadADB.class.getName(), "KillUnixProcess: pid: " + pid.toString());
        return Runtime.getRuntime().exec("kill " + pid).waitFor();
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
