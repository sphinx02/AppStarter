package de.belu.firestarter.observer;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.SequenceInputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;


/**
 * Runs in the Background and observes the home button clicks
 */
public class BackgroundHomeButtonObserverThread extends Thread
{
    /** Time to wait for second click in milliseconds */
    private final static Integer WAITFORSECONDCLICK = 270;

    /** Name / IP of the device to be connected */
    private final static String CONNECTDEVICE = "localhost";
    
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

    /** Process object */
    Process mProcess = null;
    
    /**
     * Create new BackgroundObserverThread
     */
    public BackgroundHomeButtonObserverThread()
    {
        // Set our priority to minimal
        this.setPriority(Thread.MIN_PRIORITY);
    }

    /** Try to stop thread */
    public void stopThread()
    {
        mRun = false;
        Thread stopThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    if (mProcess != null)
                    {
                        mProcess.destroy();
                    }
                }
                catch(Exception ignore) {}
            }
        });
        stopThread.start();
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
        while(mRun)
        {
            try
            {
                mErrorMessage = null;
                Log.d(BackgroundHomeButtonObserverThread.class.getName(), "Starting HomeButtonObserver.");

                // Init some variables
                mIsConnected = false;

                // Connect an instance on localhost
                Thread connectAdbThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            Log.d(BackgroundHomeButtonObserverThread.class.getName(), "CONNECTHREAD: try connect to " + CONNECTDEVICE);

                            // Run process
                            mProcess = Runtime.getRuntime().exec(new String[]{"adb", "connect", CONNECTDEVICE});

                            // Get output reader
                            int read;
                            char[] buffer = new char[4096];
                            BufferedReader reader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
                            try
                            {
                                // Reads stdout of process
                                while ((read = reader.read(buffer)) > 0)
                                {
                                    String message = String.valueOf(buffer, 0, read);
                                    Log.d(BackgroundHomeButtonObserverThread.class.getName(), "CONNECTHREAD: adb received: " + message);
                                    if(message.contains("connected") && !message.contains("unable"))
                                    {
                                        mIsConnected = true;
                                    }
                                }
                            }
                            finally
                            {
                                reader.close();
                            }
                            mProcess.waitFor();
                        }
                        catch(Exception e)
                        {
                            StringWriter errors = new StringWriter();
                            e.printStackTrace(new PrintWriter(errors));
                            String errorReason = errors.toString();
                            Log.d(BackgroundHomeButtonObserverThread.class.getName(), "CONNECTHREAD: Exception: \n" + errorReason);
                        }
                    }
                });
                connectAdbThread.start();
                connectAdbThread.join(5000);

                // Check if still alive after timeout
                if(connectAdbThread.isAlive())
                {
                    // Try to kill process
                    killCurrentProcess("CONNECTHREAD");
                }
                connectAdbThread = null;

                if(!mIsConnected)
                {
                    throw new Exception("Error while connecting to adb.");
                }
                Log.d(BackgroundHomeButtonObserverThread.class.getName(), "Adb is connected, check devices..");

                // Reset adb device name
                mAdbDevice = null;

                // Check device name
                Thread getAdbDeviceThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            Log.d(BackgroundHomeButtonObserverThread.class.getName(), "GETADBDEVICETHREAD: try get adb device");

                            // Run process
                            mProcess = Runtime.getRuntime().exec(new String[]{"adb", "devices"});

                            // Get output reader
                            int read;
                            char[] buffer = new char[4096];
                            BufferedReader reader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
                            try
                            {
                                // Reads stdout of process
                                while ((read = reader.read(buffer)) > 0)
                                {
                                    String message = String.valueOf(buffer, 0, read);
                                    Log.d(BackgroundHomeButtonObserverThread.class.getName(), "GETADBDEVICETHREAD: adb received: " + message);
                                    if(message.contains(CONNECTDEVICE))
                                    {
                                        // Split message by any whitespace:
                                        String[] lines = message.split("\\s+");
                                        for(String line : lines)
                                        {
                                            if(line.contains(CONNECTDEVICE))
                                            {
                                                mAdbDevice = line;
                                                break;
                                            }
                                        }

                                        if(mAdbDevice != null)
                                        {
                                            Log.d(BackgroundHomeButtonObserverThread.class.getName(), "GETADBDEVICETHREAD: adb device found: " + mAdbDevice);
                                        }
                                    }
                                }
                            }
                            finally
                            {
                                reader.close();
                            }
                            mProcess.waitFor();
                        }
                        catch(Exception e)
                        {
                            StringWriter errors = new StringWriter();
                            e.printStackTrace(new PrintWriter(errors));
                            String errorReason = errors.toString();
                            Log.d(BackgroundHomeButtonObserverThread.class.getName(), "GETADBDEVICETHREAD: Exception: \n" + errorReason);
                        }
                    }
                });
                getAdbDeviceThread.start();
                getAdbDeviceThread.join(3000);

                // Check if still alive after timeout
                if(getAdbDeviceThread.isAlive())
                {
                    // Try to kill process
                    killCurrentProcess("GETADBDEVICETHREAD");
                }
                getAdbDeviceThread = null;

                if(mAdbDevice == null)
                {
                    throw new Exception("Error finding the correct adb device.");
                }
                Log.d(BackgroundHomeButtonObserverThread.class.getName(), "Adb device found, empty logcat..");

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
                            Log.d(BackgroundHomeButtonObserverThread.class.getName(), "EMPTYLOGCATTHREAD: try clear logcat");

                            // Run process
                            mProcess = Runtime.getRuntime().exec(new String[]{"adb", "-s", mAdbDevice, "logcat", "-c"});

                            // Get output reader
                            int read;
                            char[] buffer = new char[4096];

                            // Combining error and normal output
                            BufferedReader reader = new BufferedReader(new InputStreamReader(new SequenceInputStream(mProcess.getInputStream(), mProcess.getErrorStream())));
                            try
                            {
                                // Reads stdout of process
                                while ((read = reader.read(buffer)) > 0)
                                {
                                    String message = String.valueOf(buffer, 0, read);
                                    Log.d(BackgroundHomeButtonObserverThread.class.getName(), "EMPTYLOGCATTHREAD: adb received: " + message);
                                    if(message.contains("- waiting for device -"))
                                    {
                                        Log.d(BackgroundHomeButtonObserverThread.class.getName(), "EMPTYLOGCATTHREAD: device name must be wrong.. ");
                                        mIsLogcatErrorMessageFound = true;
                                    }
                                }
                            }
                            finally
                            {
                                reader.close();
                            }
                            mProcess.waitFor();
                        }
                        catch(Exception e)
                        {
                            StringWriter errors = new StringWriter();
                            e.printStackTrace(new PrintWriter(errors));
                            String errorReason = errors.toString();
                            Log.d(BackgroundHomeButtonObserverThread.class.getName(), "EMPTYLOGCATTHREAD: Exception: \n" + errorReason);
                        }
                    }
                });
                emptyLogCatThread.start();
                emptyLogCatThread.join(5000);

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
                Log.d(BackgroundHomeButtonObserverThread.class.getName(), "Adb logcat cleared, now start observation..");

                // Start logcat with proper filters
                mProcess = Runtime.getRuntime().exec(new String[]{"adb", "-s", mAdbDevice, "logcat", "ActivityManager:I", "*:S"});

                // Get output reader// Get output reader
                int read;
                char[] buffer = new char[4096];

                // Combining error and normal output
                BufferedReader reader = new BufferedReader(new InputStreamReader(new SequenceInputStream(mProcess.getInputStream(), mProcess.getErrorStream())));
                try
                {
                    // Reads stdout of process
                    while ((read = reader.read(buffer)) > 0)
                    {
                        String message = String.valueOf(buffer, 0, read);
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
                                // Create new thread to check for double click
                                mWaitForSecondClickThread = new Thread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        try
                                        {
                                            Thread.sleep(WAITFORSECONDCLICK);
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
                    reader.close();
                }
                mProcess.waitFor();

                Log.d(BackgroundHomeButtonObserverThread.class.getName(), "Lost connection to adb..");
                mErrorMessage = "Lost connection to adb..";
                fireServiceErrorEvent(mErrorMessage);
            }
            catch (Exception e)
            {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                String errorReason = errors.toString();
                Log.d(BackgroundHomeButtonObserverThread.class.getName(), "Error in BackgroundObserver: \n" + errorReason);

                mErrorMessage = "Exception: " + e.getMessage();
                fireServiceErrorEvent(mErrorMessage);
            }

            // Check if we shall run again:
            if(mRun)
            {
                Log.d(BackgroundHomeButtonObserverThread.class.getName(), "Restart observer in a few seconds..");
                try
                {
                    Thread.sleep(8000);
                }
                catch (InterruptedException ignore)
                {
                }

                // Wait some time to try again
                mErrorMessage = "Something went wrong, restarting HomeButtonObserver now...";
                fireServiceErrorEvent(mErrorMessage);
            }
        }
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
            Log.d(BackgroundHomeButtonObserverThread.class.getName(), component + ": Try to kill running process");
            Integer exitCode = killUnixProcess(mProcess);
            if(exitCode != 0)
            {
                throw new Exception("Received exit-code was not zero but " + exitCode.toString());
            }
            Log.d(BackgroundHomeButtonObserverThread.class.getName(), component + ": Killed running process successful");
        }
        catch(Exception e)
        {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String errorReason = errors.toString();
            Log.d(BackgroundHomeButtonObserverThread.class.getName(), component + ": Exception while killing process: \n" + errorReason);
        }
    }

    /**
     * @param process Process of interest
     * @return Process PID
     * @throws Exception if process is no unix-process
     */
    private static int getUnixPID(Process process) throws Exception
    {
        Log.d(BackgroundHomeButtonObserverThread.class.getName(), "GetUnixPID: process name: " + process.getClass().getName());
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
        Log.d(BackgroundHomeButtonObserverThread.class.getName(), "KillUnixProcess: pid: " + pid.toString());
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

    /**
     * Interface for a service error
     */
    public interface OnServiceErrorListener
    {
        public void onServiceError(String message);
    }

    /**
     * Interface for the home-click listener
     */
    public interface OnHomeButtonClickedListener 
    {
        public void onHomeButtonClicked();
        public void onHomeButtonDoubleClicked();
    }
}
