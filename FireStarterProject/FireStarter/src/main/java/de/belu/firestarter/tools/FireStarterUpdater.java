package de.belu.firestarter.tools;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Update class to download updates..
 */
public class FireStarterUpdater extends Updater
{
    public FireStarterUpdater()
    {
        super();
        DOWNLOADFOLDER  = "FireStarterUpdates";
        UPDATEURL = "https://api.github.com/repos/sphinx02/FireStarter/releases";
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
