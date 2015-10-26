package de.belu.firestarter.tools;

import android.util.Log;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.io.IOException;

public class KodiUpdater extends Updater
{
    /** Name of APK **/
    private String mApkName = null;

    public KodiUpdater()
    {
        super();
        DOWNLOADFOLDER = "FireStarterInstalls";
        UPDATEURL = "http://mirrors.kodi.tv/releases/android/arm/";
    }

    /** Check if version is higher */
    public static Boolean isVersionNewer(String oldVersion, String newVersion)
    {
        if (oldVersion == null || oldVersion.equals("unknown")) return true;

        // for now, just check if they are different and treat it like it's newer
        return !oldVersion.equals(newVersion);
    }

    /**
     * Get version from file name
     * @param apkName file name to be parsed
     * @return Version string
     */
    private static String getVersion(String apkName)
    {
        String retVal = null;

        try
        {
            if(apkName != null && !apkName.equals(""))
            {
                apkName = apkName
                            .replace("kodi-", "")
                            .replace("-Isengard", "")
                            .replace("-Jarvis", "")
                            .replace("_rc", "-RC")
                            .replace("_alpha", "-ALPHA")
                            .replace("-armeabi-v7a.apk", "");
                retVal = "v" + apkName;
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
                    if(mIsBusy)
                    {
                        throw new JSONException("FireStarterUpdater is already working..");
                    }
                    mIsBusy = true;

                    Document doc = Jsoup.connect(UPDATEURL).get();
                    Elements files = doc.select("#list tbody tr");
                    String latestApk = files.get(2).select("td").first().text();

                    mApkName = latestApk;

                    LATEST_VERSION = getVersion(mApkName);

                    mApkURL = UPDATEURL + latestApk;

                    fireOnCheckForUpdateFinished("Update finished.");
                    Log.d(FireStarterUpdater.class.getName(), "Update finished successful, found version: " + LATEST_VERSION);
                }
                catch (IOException e)
                {
                    Log.d(FireStarterUpdater.class.getName(), "IOError: " + e.getMessage());
                    fireOnCheckForUpdateFinished("Update-Check-Error with connection: " + e.getMessage());
                }
                catch (JSONException e)
                {
                    Log.d(FireStarterUpdater.class.getName(), "ParseError: " + e.getMessage());
                    fireOnCheckForUpdateFinished("Update-Check-Error with parsing: " + e.getMessage());
                }
                catch (Exception e)
                {
                    Log.d(FireStarterUpdater.class.getName(), "GeneralError: " + e.getMessage());
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
}
