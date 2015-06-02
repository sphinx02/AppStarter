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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

/**
 * Update class to download updates..
 */
public class Updater
{
    /** Latest version found on github */
    public static String LATEST_VERSION = null;

    /** Update url on github */
    private final static String UPDATEURL = "https://api.github.com/repos/sphinx02/FireStarter/releases";

    /** Update dir on external storage */
    private final static String DOWNLOADFOLDER = "FireStarterUpdates";

    /** Indicates if process is busy */
    private static Boolean mIsBusy = false;

    /** Url of APK **/
    private String mApkURL = null;

    /** Update semaphore */
    private Semaphore mUpdateSemaphore = null;

    /** Indicates if the download was succesful */
    private Boolean mDownloadSuccessful = false;

    /** Error reason */
    private String mDownloadErrorReason = null;

    /** Queue value of running download */
    private Long mQueueValue;

    /** Download manager */
    private DownloadManager mDownloadManager;

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

    /** Check if version is higher */
    public static Boolean isVersionNewer(String oldVersion, String newVersion)
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

                    // Else versions habe been equal --> no newer
                    // --> check next stage or finish
                }
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
    private static List<Integer> getVersionList(String versionString)
    {
        List<Integer> retVal = new ArrayList<Integer>();

        try
        {
            if(versionString != null && !versionString.equals(""))
            {
                versionString = versionString.replaceAll("[^\\d.]", "");
                String[] parts = versionString.split("\\.");
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

    /** Check for update and the update FireStarter */
    public void updateFireStarter(final Context context, final String oldVersion)
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
                        throw new Exception("Updater is already working..");
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
                    Log.d(Updater.class.getName(), "Download to file://" + downloadFile.getAbsolutePath());
                    localRequest.setDestinationUri(Uri.parse("file://" + downloadFile.getAbsolutePath()));

                    context.registerReceiver(new BroadcastReceiver()
                    {
                        public void onReceive(Context context, Intent intent)
                        {
                            String action = intent.getAction();
                            Log.d(Updater.class.getName(), "Received intent: " + action);
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
                            Log.d(Updater.class.getName(), "Release semaphore..");
                            mUpdateSemaphore.release();
                        }
                    }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

                    Log.d(Updater.class.getName(), "Aquire semaphore");
                    mUpdateSemaphore = new Semaphore(1);
                    mUpdateSemaphore.acquire();

                    // Here the download is performed
                    Log.d(Updater.class.getName(), "Start download");
                    mDownloadManager = (DownloadManager)context.getSystemService(context.DOWNLOAD_SERVICE);
                    mQueueValue = mDownloadManager.enqueue(localRequest);

                    Log.d(Updater.class.getName(), "Aquire semaphore again");
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

                    Log.d(Updater.class.getName(), "Download finished");
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
                    Log.d(Updater.class.getName(), "UpdateError: " + e.getMessage());
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
                    if(mIsBusy)
                    {
                        throw new JSONException("Updater is already working..");
                    }
                    mIsBusy = true;

                    // Build the url
                    URL url = new URL(UPDATEURL);

                    // Read from the URL
                    Scanner scan = new Scanner(url.openStream());
                    StringBuilder str = new StringBuilder();
                    while (scan.hasNext())
                    {
                        str.append(scan.nextLine());
                    }
                    scan.close();

                    // build a JSON object
                    JSONArray obj = new JSONArray(str.toString());
                    if(obj.length() <= 0)
                    {
                        throw new JSONException("No content found");
                    }

                    JSONObject latestRelease = obj.getJSONObject(0);
                    LATEST_VERSION = null;
                    String tagName = latestRelease.getString("tag_name");
                    if(tagName == null || tagName.equals(""))
                    {
                        throw new JSONException("Latest release tag name is empty");
                    }
                    LATEST_VERSION = tagName;

                    // Search for apk-download url
                    mApkURL = null;
                    JSONArray assets = latestRelease.getJSONArray("assets");
                    for(Integer i = 0; i < assets.length(); i++)
                    {
                        JSONObject currentAsset = assets.getJSONObject(i);
                        String downloadUrl = currentAsset.getString("browser_download_url");
                        if(downloadUrl.startsWith("https://github.com/sphinx02/FireStarter/releases") && downloadUrl.endsWith(".apk"))
                        {
                            mApkURL = downloadUrl;
                            break;
                        }
                    }
                    if(mApkURL == null)
                    {
                        throw new JSONException("No .apk download URL found");
                    }

                    fireOnCheckForUpdateFinished("Update finished.");
                    Log.d(Updater.class.getName(), "Update finished successful, found version: " + LATEST_VERSION);
                }
                catch (IOException e)
                {
                    Log.d(Updater.class.getName(), "IOError: " + e.getMessage());
                    fireOnCheckForUpdateFinished("Update-Check-Error with connection: " + e.getMessage());
                }
                catch (JSONException e)
                {
                    Log.d(Updater.class.getName(), "ParseError: " + e.getMessage());
                    fireOnCheckForUpdateFinished("Update-Check-Error with parsing: " + e.getMessage());
                }
                catch (Exception e)
                {
                    Log.d(Updater.class.getName(), "GeneralError: " + e.getMessage());
                    fireOnCheckForUpdateFinished("Update-Check-Error with parsing: " + e.getMessage());
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

    /**
     * Fire update progress
     * @param percent Percentage
     * @param message Message
     */
    private void fireOnUpdateProgressListener(final Boolean isError, final Integer percent, final String message)
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
    private void fireOnCheckForUpdateFinished(final String message)
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
