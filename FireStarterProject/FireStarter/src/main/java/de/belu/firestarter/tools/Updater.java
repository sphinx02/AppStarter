package de.belu.firestarter.tools;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.concurrent.Semaphore;

/**
 * Base Updater functionality
 */
public class Updater {
    /** Latest version found */
    public static String LATEST_VERSION = null;

    /** Update dir on external storage */
    protected String DOWNLOADFOLDER;

    /** Update url */
    protected String UPDATEURL;

    /** Indicates if process is busy */
    protected static Boolean mIsBusy = false;

    /** Url of APK **/
    protected String mApkURL = null;

    /** Indicates if the download was succesful */
    private Boolean mDownloadSuccessful = false;

    /** Error reason */
    private String mDownloadErrorReason = null;

    /** Queue value of running download */
    private Long mQueueValue;

    /** Download manager */
    private DownloadManager mDownloadManager;

    /** Update semaphore */
    private Semaphore mUpdateSemaphore = null;

    /** Check for update listener */
    private OnCheckForUpdateFinishedListener mOnCheckForUpdateFinishedListener;

    /** Update progress listener */
    private OnUpdateProgressListener mOnUpdateProgressListener;

    /** Set the check for update listener */
    public void setOnCheckForUpdateFinishedListener(OnCheckForUpdateFinishedListener listener)
    {
        mOnCheckForUpdateFinishedListener = listener;
    }

    /** Set progress update listener */
    public void setOnUpdateProgressListener(OnUpdateProgressListener listener)
    {
        mOnUpdateProgressListener = listener;
    }

    public static Boolean isVersionNewer(String oldVersion, String newVersion)
    {
        return false;
    }

    public void update(final Context context, final String oldVersion)
    {
        Thread updateThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    if (mIsBusy)
                    {
                        throw new Exception("FireStarterUpdater is already working..");
                    }

                    // Check for update synchron
                    checkForUpdate(true);
                    mIsBusy = true;

                    // Check if update-check was successful and version is newer
                    if (LATEST_VERSION == null || !isVersionNewer(oldVersion, LATEST_VERSION))
                    {
                        throw new Exception("No newer version found..");
                    }
                    if(mApkURL == null)
                    {
                        throw new Exception("Download URL of new version not found..");
                    }
                    fireOnUpdateProgressListener(false, 10, "Newer version found, start download..");

                    // Create download-dir and start download
                    File downloadDir = new File(Environment.getExternalStorageDirectory(), DOWNLOADFOLDER);

                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + downloadDir.getAbsolutePath())));

                    if(downloadDir.exists() && !downloadDir.isDirectory())
                    {
                        if(!downloadDir.delete())
                        {
                            throw new Exception("Can not delete file: " + downloadDir.getAbsolutePath());
                        }
                    }
                    if(!downloadDir.exists() && !downloadDir.mkdir())
                    {
                        throw new Exception("Can not create download folder: " + downloadDir.getAbsolutePath());
                    }
                    else
                    {
                        Tools.deleteDirectoryRecursively(context, downloadDir, true);
                    }

                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + downloadDir.getAbsolutePath())));

                    File downloadFile = new File(downloadDir, "FireStarter-" + LATEST_VERSION + ".apk");

                    mDownloadSuccessful = false;
                    mDownloadErrorReason = null;
                    DownloadManager.Request localRequest = new DownloadManager.Request(Uri.parse(mApkURL));
                    localRequest.setDescription("Downloading FireStarter " + LATEST_VERSION);
                    localRequest.setTitle("FireStarter Update");
                    localRequest.allowScanningByMediaScanner();
                    localRequest.setNotificationVisibility(1);
                    Log.d(FireStarterUpdater.class.getName(), "Download to file://" + downloadFile.getAbsolutePath());
                    localRequest.setDestinationUri(Uri.parse("file://" + downloadFile.getAbsolutePath()));

                    context.registerReceiver(new BroadcastReceiver()
                    {
                        public void onReceive(Context context, Intent intent)
                        {
                            String action = intent.getAction();
                            Log.d(FireStarterUpdater.class.getName(), "Received intent: " + action);
                            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action))
                            {
                                DownloadManager.Query query = new DownloadManager.Query();
                                query.setFilterById(mQueueValue);
                                Cursor c = mDownloadManager.query(query);
                                if (c.moveToFirst())
                                {
                                    int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                                    if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex))
                                    {
                                        mDownloadSuccessful = true;
                                    }
                                    else
                                    {
                                        // Try to get error reason
                                        switch(c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON)))
                                        {
                                            case DownloadManager.ERROR_CANNOT_RESUME:
                                                mDownloadErrorReason = "ERROR_CANNOT_RESUME";
                                                break;
                                            case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                                                mDownloadErrorReason = "ERROR_DEVICE_NOT_FOUND";
                                                break;
                                            case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                                                mDownloadErrorReason = "ERROR_FILE_ALREADY_EXISTS";
                                                break;
                                            case DownloadManager.ERROR_FILE_ERROR:
                                                mDownloadErrorReason = "ERROR_FILE_ERROR";
                                                break;
                                            case DownloadManager.ERROR_HTTP_DATA_ERROR:
                                                mDownloadErrorReason = "ERROR_HTTP_DATA_ERROR";
                                                break;
                                            case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                                                mDownloadErrorReason = "ERROR_INSUFFICIENT_SPACE";
                                                break;
                                            case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                                                mDownloadErrorReason = "ERROR_TOO_MANY_REDIRECTS";
                                                break;
                                            case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                                                mDownloadErrorReason = "ERROR_UNHANDLED_HTTP_CODE";
                                                break;
                                            default:
                                                mDownloadErrorReason = "ERROR_UNKNOWN";
                                                break;
                                        }
                                    }
                                }
                                c.close();
                            }

                            // Unregister receiver
                            context.unregisterReceiver(this);

                            // Release semaphore in any case..
                            Log.d(FireStarterUpdater.class.getName(), "Release semaphore..");
                            mUpdateSemaphore.release();
                        }
                    }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

                    Log.d(FireStarterUpdater.class.getName(), "Aquire semaphore");
                    mUpdateSemaphore = new Semaphore(1);
                    mUpdateSemaphore.acquire();

                    // Here the download is performed
                    Log.d(FireStarterUpdater.class.getName(), "Start download");
                    mDownloadManager = (DownloadManager)context.getSystemService(context.DOWNLOAD_SERVICE);
                    mQueueValue = mDownloadManager.enqueue(localRequest);

                    Log.d(FireStarterUpdater.class.getName(), "Aquire semaphore again");
                    int lastPercentage = 0;
                    while(!mUpdateSemaphore.tryAcquire())
                    {
                        DownloadManager.Query q = new DownloadManager.Query();
                        q.setFilterById(mQueueValue);
                        Cursor cursor = mDownloadManager.query(q);
                        int percentage = 0;
                        if(cursor.moveToFirst())
                        {
                            int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                            percentage = (int)Math.round((((double)bytes_downloaded / (double)bytes_total) * 100.0) * 8.0/10.0);
                            if(percentage < 0) percentage = 0;
                            if(percentage > 100) percentage = 100;
                        }
                        cursor.close();

                        if(percentage > lastPercentage)
                        {
                            lastPercentage = percentage;
                            fireOnUpdateProgressListener(false, 10 + percentage, "Download in progress..");
                        }

                        Thread.sleep(500);
                    }
                    mUpdateSemaphore.release();
                    mUpdateSemaphore = null;

                    Log.d(FireStarterUpdater.class.getName(), "Download finished");
                    if(!mDownloadSuccessful)
                    {
                        String reason = "";
                        if(mDownloadErrorReason != null)
                        {
                            reason = " Reason: " + mDownloadErrorReason;
                        }
                        throw new Exception("Download failed.." + reason);
                    }

                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + downloadFile.getAbsolutePath())));
                    fireOnUpdateProgressListener(false, 80, "Download finished, start installation..");

                    Intent installIntent = new Intent(Intent.ACTION_VIEW);
                    installIntent.setDataAndType(Uri.parse("file://" + downloadFile.getAbsolutePath()), "application/vnd.android.package-archive");
                    installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(installIntent);

                    fireOnUpdateProgressListener(false, 100, "Successfully initiated update..");
                }
                catch(Exception e)
                {
                    Log.d(FireStarterUpdater.class.getName(), "UpdateError: " + e.getMessage());
                    fireOnUpdateProgressListener(true, 100, e.getMessage());
                }
                finally
                {
                    mIsBusy = false;
                }
            }
        });
        updateThread.start();
    }

    /** Check for update */
    public void checkForUpdate()
    {
        checkForUpdate(false);
    }

    public void checkForUpdate(Boolean synchron)
    {
        //override
    }

    /**
     * Fire update progress
     * @param percent Percentage
     * @param message Message
     */
    protected void fireOnUpdateProgressListener(final Boolean isError, final Integer percent, final String message)
    {
        Thread fireThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if(mOnUpdateProgressListener != null)
                {
                    mOnUpdateProgressListener.onUpdateProgress(isError, percent, message);
                }
            }
        });
        fireThread.start();
    }

    /**
     * Fire check for update finished message
     * @param message Message to fire
     */
    protected void fireOnCheckForUpdateFinished(final String message)
    {
        Thread fireThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if(mOnCheckForUpdateFinishedListener != null)
                {
                    mOnCheckForUpdateFinishedListener.onCheckForUpdateFinished(message);
                }
            }
        });
        fireThread.start();
    }

    /**
     * Interface for progress messages of update check
     */
    public interface OnCheckForUpdateFinishedListener
    {
        public void onCheckForUpdateFinished(String message);
    }

    /**
     * Interface for progress messages of performing an update
     */
    public interface OnUpdateProgressListener
    {
        public void onUpdateProgress(Boolean isError, Integer percent, String message);
    }

}
