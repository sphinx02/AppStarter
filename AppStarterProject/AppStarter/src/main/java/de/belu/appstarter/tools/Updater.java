package de.belu.appstarter.tools;

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import de.belu.appstarter.gui.UpdaterDialogHandler;

/**
 * Base Updater functionality
 */
public abstract class Updater
{
    /** Holds the current updater dialog handler */
    public UpdaterDialogHandler DialogHandler = null;

    /** Latest version of AppStarter */
    protected String mLatestVersion = null;

    /** Download URL of the latest APK */
    protected String mApkDownloadUrl = null;

    /** Indicates if process is busy */
    private Boolean mIsBusy = false;

    /** Update dir on external storage */
    private String mDownloadFolder = "AppStarterUpdates";

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

    /** Returns the name of the App */
    public abstract String getAppName();

    /** Returns the name of the App */
    public abstract String getPackageName(Context context);

    public abstract Boolean isVersionNewer(String oldVersion, String newVersion);

    /** Update the values of the latest version and of the APK download URL for the latest version */
    protected abstract void updateLatestVersionAndApkDownloadUrl() throws Exception;

    /** Return the current version */
    public String getCurrentVersion(Context context)
    {
        return Tools.getCurrentAppVersion(context, getPackageName(context));
    }

    /** Return the latest version */
    public String getLatestVersion()
    {
        return mLatestVersion;
    }

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

    /**
     * Compares standard version strings like "2.1", "v2.3.1.0" or "version 1.3.2"
     * @param oldVersion Old version String
     * @param newVersion New version String
     * @return true if newVersion String is newer than oldVersion String
     */
    public Boolean isVersionNewerStandardCheck(String oldVersion, String newVersion)
    {
        Boolean retVal = false;
        try
        {
            List<Integer> oldVerList = getVersionList(oldVersion);
            List<Integer> newVerList = getVersionList(newVersion);
            if(oldVerList.size() > 0 && newVerList.size() > 0)
            {
                for(Integer i = 0; i < newVerList.size(); i++)
                {
                    // If oldversion has no additional step and all
                    // steps before have been equal, newVersion is newer
                    if(i >= oldVerList.size())
                    {
                        retVal = true;
                        break;
                    }

                    // If newVersions current step is higher than oldversions stage,
                    // newVersion is newer
                    if(newVerList.get(i) > oldVerList.get(i))
                    {
                        retVal = true;
                        break;
                    }

                    // If oldVersions current step is higher than newVersions stage,
                    // oldVersion is newer
                    if(oldVerList.get(i) > newVerList.get(i))
                    {
                        break;
                    }

                    // Else versions have been equal --> no newer
                    // --> check next stage or finish
                }
            }
            else if(oldVerList.size() == 0 && newVerList.size() > 0)
            {
                // This happens if old version is not installed / not found
                // which should mean that the latest version is anyway newer
                retVal = true;
            }
        }
        catch(Exception ignore){}

        return retVal;
    }

    /**
     * Separate version string in major, minor, ..
     * Most significant value first
     * @param versionString Version string to be parsed
     * @return List of Integers
     */
    private List<Integer> getVersionList(String versionString)
    {
        List<Integer> retVal = new ArrayList<Integer>();

        try
        {
            if(versionString != null && !versionString.equals(""))
            {
                // Delete everything that is no digit and no dot (like e.g. "v" or "version")
                versionString = versionString.replaceAll("[^\\d.]", "");

                // Split the remaining part by the dots
                String[] parts = versionString.split("\\.");

                // Now create the version list
                if(parts != null && parts.length > 0)
                {
                    for(String part : parts)
                    {
                        retVal.add(Integer.valueOf(part));
                    }
                }
            }
        }
        catch(Exception ignore) { }

        return retVal;
    }

    /** Check github for update */
    public void checkForUpdate(Boolean synchron)
    {
        Thread checkForUpdateThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    if (mIsBusy)
                    {
                        throw new Exception("Updater is already working..");
                    }
                    mIsBusy = true;

                    // Reset variables
                    mApkDownloadUrl = null;
                    mLatestVersion = null;

                    // Call the update mechanism of the actual updater
                    updateLatestVersionAndApkDownloadUrl();

                    // Check if latest version is not null
                    if(mLatestVersion == null)
                    {
                        throw new Exception("Latest version not found.");
                    }

                    // Check if download url is not null
                    if(mApkDownloadUrl == null)
                    {
                        throw new Exception("No .apk download URL found.");
                    }

                    // If everything was fine show success-message:
                    fireOnCheckForUpdateFinished("Check for update finished successful, found version: " + getLatestVersion());
                    Log.d(AppStarterUpdater.class.getName(), "Check for update finished successful, found version: " + getLatestVersion());
                }
                catch (Exception e)
                {
                    Log.d(AppStarterUpdater.class.getName(), "Update-Check-Error: " + e.getMessage());
                    fireOnCheckForUpdateFinished("Update-Check-Error: " + e.getMessage());
                }
                finally
                {
                    mIsBusy = false;
                }
            }
        });
        checkForUpdateThread.start();
        if(synchron)
        {
            try
            {
                checkForUpdateThread.join();
            }
            catch (InterruptedException ignore) {}
        }
    }

    public void update(final Context context)
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
                        throw new Exception("AppStarterUpdater is already working..");
                    }

                    // Check for update synchron
                    checkForUpdate(true);
                    mIsBusy = true;

                    // Check if update-check was successful and version is newer
                    String oldVersion = getCurrentVersion(context);
                    String latestVersion = getLatestVersion();
                    if (latestVersion == null || !isVersionNewer(oldVersion, latestVersion))
                    {
                        throw new Exception("No newer version found..");
                    }
                    String apkUrl = mApkDownloadUrl;
                    if(apkUrl == null)
                    {
                        throw new Exception("Download URL of new version not found..");
                    }
                    Log.d(AppStarterUpdater.class.getName(), "Download from URL: " + apkUrl);
                    fireOnUpdateProgressListener(false, 10, "Newer version found, start download..");

                    // Create download-dir and start download
                    File downloadDir = new File(Environment.getExternalStorageDirectory(), mDownloadFolder);

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

                    File downloadFile = new File(downloadDir, getAppName() + "-" + latestVersion + ".apk");

                    mDownloadSuccessful = false;
                    mDownloadErrorReason = null;
                    DownloadManager.Request localRequest = new DownloadManager.Request(Uri.parse(apkUrl));
                    localRequest.setDescription("Downloading " + getAppName() + " " + latestVersion);
                    localRequest.setTitle(getAppName() + " Update");
                    localRequest.allowScanningByMediaScanner();
                    localRequest.setNotificationVisibility(1);
                    Log.d(AppStarterUpdater.class.getName(), "Download to file://" + downloadFile.getAbsolutePath());
                    localRequest.setDestinationUri(Uri.parse("file://" + downloadFile.getAbsolutePath()));

                    context.registerReceiver(new BroadcastReceiver()
                    {
                        public void onReceive(Context context, Intent intent)
                        {
                            String action = intent.getAction();
                            Log.d(AppStarterUpdater.class.getName(), "Received intent: " + action);
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
                            Log.d(AppStarterUpdater.class.getName(), "Release semaphore..");
                            mUpdateSemaphore.release();
                        }
                    }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

                    Log.d(AppStarterUpdater.class.getName(), "Aquire semaphore");
                    mUpdateSemaphore = new Semaphore(1);
                    mUpdateSemaphore.acquire();

                    // Here the download is performed
                    Log.d(AppStarterUpdater.class.getName(), "Start download");
                    mDownloadManager = (DownloadManager)context.getSystemService(context.DOWNLOAD_SERVICE);
                    mQueueValue = mDownloadManager.enqueue(localRequest);

                    Log.d(AppStarterUpdater.class.getName(), "Aquire semaphore again");
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

                    Log.d(AppStarterUpdater.class.getName(), "Download finished");
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
                    Log.d(AppStarterUpdater.class.getName(), "UpdateError: " + e.getMessage());
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
