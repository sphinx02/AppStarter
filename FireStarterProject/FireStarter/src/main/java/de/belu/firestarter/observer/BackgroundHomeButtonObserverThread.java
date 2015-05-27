package de.belu.firestarter.observer;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;


/**
 * Runs in the Background and observes the home button clicks
 */
public class BackgroundHomeButtonObserverThread extends Thread
{
    /** Time to wait for second click in milliseconds */
    private final static Integer WAITFORSECONDCLICK = 270;
    
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
                int read;
                char[] buffer = new char[4096];
                final Boolean[] isConnected = new Boolean[]{false};

                // ADB initialization sometings freezes, so do it in another thread..
                Thread initAdbThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        // Start ADB
                        try
                        {
                            Process initProcess = Runtime.getRuntime().exec(new String[]{"adb", "devices"});
                            initProcess.waitFor();
                            Log.d(BackgroundHomeButtonObserverThread.class.getName(), "INITTHREAD: Successfull init of adb.");
                        }
                        catch (IOException e)
                        {
                            Log.d(BackgroundHomeButtonObserverThread.class.getName(), "INITTHREAD: Error in adb init thread (IOExcpetion).");
                        }
                        catch (InterruptedException e)
                        {
                            Log.d(BackgroundHomeButtonObserverThread.class.getName(), "INITTHREAD: Error in adb init thread (InterruptedException).");
                        }
                    }
                });
                initAdbThread.start();
                initAdbThread.join(1000);
                if(initAdbThread.isAlive())
                {
                    Log.d(BackgroundHomeButtonObserverThread.class.getName(), "INITTHREAD: Was still running, interrupt thread.");
                    initAdbThread.interrupt();
                    initAdbThread = null;
                }

                // Create anonymous thread to check if the logcat-process have started correctly
                Thread checkerThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            Thread.sleep(5000);
                            Log.d(BackgroundHomeButtonObserverThread.class.getName(), "Checking if HomeButtonObserver came up.");
                            if(!isConnected[0] && mErrorMessage == null)
                            {
                                mErrorMessage = "Starting HomeButtonObserver needs too long..";
                                fireServiceErrorEvent(mErrorMessage);
                                Log.d(BackgroundHomeButtonObserverThread.class.getName(), mErrorMessage);
                            }
                            else
                            {
                                Log.d(BackgroundHomeButtonObserverThread.class.getName(), "Seems as HomeButtonObserver came up.");
                            }
                        }
                        catch (InterruptedException ignore){ }
                    }
                });
                checkerThread.start();

                // Clear logcat
                mProcess = Runtime.getRuntime().exec(new String[]{"adb", "logcat", "-c"});

                // Get output reader
                BufferedReader reader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
                try
                {
                    // Reads stdout of process
                    Boolean wasSuccessfull = true;
                    while ((read = reader.read(buffer)) > 0)
                    {
                        String message = String.valueOf(buffer, 0, read);
                        Log.d(BackgroundHomeButtonObserverThread.class.getName(), "Adb received: " + message);
                        if(message.contains("- waiting for device -"))
                        {
                            wasSuccessfull = false;
                            mProcess.destroy();
                        }
                    }
                    isConnected[0] = wasSuccessfull;
                }
                finally
                {
                    reader.close();
                }
                mProcess.waitFor();

                if(!isConnected[0])
                {
                    throw new Exception("Unable to connect adb..");
                }

                // Ok lets go on, adb is connected :)
                Log.d(BackgroundHomeButtonObserverThread.class.getName(), "Adb is connected, start logcat..");
                mErrorMessage = null;
                
                // Start logcat with proper filters
                mProcess = Runtime.getRuntime().exec(new String[]{"adb", "logcat", "ActivityManager:I", "*:S"});
                
                // Get output reader
                reader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
                try
                {
                    // Reads stdout of process
                    while ((read = reader.read(buffer)) > 0)
                    {
                        String message = String.valueOf(buffer, 0, read);
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
            }
            
            // Wait some time to try again
            mErrorMessage = "Something went wrong restarting HomeButtonObserver in a few seconds.";
            fireServiceErrorEvent(mErrorMessage);

            if(mRun)
            {
                Log.d(BackgroundHomeButtonObserverThread.class.getName(), "Restart observer in a few seconds..");
                try
                {
                    Thread.sleep(4000);
                }
                catch (InterruptedException ignore)
                {
                }
            }
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
