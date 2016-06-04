package de.belu.appstarter.tools;

import android.content.Context;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class KodiUpdater extends Updater
{
    public static List<String> UPDATE_POLICY = new ArrayList<String>()
    {
        {
            add("Stable");
            add("Beta and RC");
            add("Nightly Builds");
        }
    };

    /** Update URL where updated versions are found */
    private String mUpdateUrl = "http://mirrors.kodi.tv/releases/android/arm/";

    /** Update URL for nightly versions */
    private String mUpdateUrlNightly = "http://mirrors.kodi.tv/nightlies/android/arm/";

    /** Current context */
    private Context mContext;

    /** Instance of settings */
    private SettingsProvider mSettings;

    /** Constructor to get Context */
    public KodiUpdater(Context context)
    {
        mContext = context;
        mSettings = SettingsProvider.getInstance(context);
    }

    @Override
    public String getAppName()
    {
        return "Kodi";
    }

    @Override
    public String getPackageName(Context context)
    {
        return "org.xbmc.kodi";
    }

    @Override
    public Boolean isVersionNewer(String oldVersion, String newVersion)
    {
//        // Kodi versiong String can have additional RC-Version which is lower
//        // than equal version String without RC-Version
//        Boolean retVal = false;
//
//        // Start do-while-false to be able to break out any time
//        do
//        {
//            // First prepare Strings
//            String workOldVersion = oldVersion.replace("_", "-").toUpperCase();
//            String workNewVersion = newVersion.replace("_", "-").toUpperCase();
//
//            // If Strings are now equal, version is not newer
//            if(workOldVersion.equals(workNewVersion))
//            {
//                retVal = false;
//                break;
//            }
//
//            // Split version Strings
//            String[] splitOldVersion = workOldVersion.split("-");
//            String[] splitNewVersion = workNewVersion.split("-");
//
//            // We should have at least one version string
//            if(splitOldVersion.length < 1 || splitNewVersion.length < 1)
//            {
//                retVal = false;
//                break;
//            }
//
//            // If the first part (the normal version string) is identical
//            // we have to perform further tests
//            if(splitOldVersion[0].equals(splitNewVersion[0]))
//            {
//                // If only the oldversion string has the rc-part,
//                // the version is newer as no rc-part is newer than any rc-part
//                if(splitOldVersion.length == 2 && splitNewVersion.length == 1)
//                {
//                    retVal = true;
//                    break;
//                }
//
//                // If both have the rc-part, we have to compare the rc part
//                if(splitOldVersion.length == 2 && splitNewVersion.length == 2)
//                {
//                    try
//                    {
//                        String oldRc = splitOldVersion[1];
//                        String newRc = splitNewVersion[1];
//
//                        if(oldRc.startsWith("RC") && newRc.startsWith("RC"))
//                        {
//                            oldRc = oldRc.substring(2, oldRc.length());
//                            newRc = newRc.substring(2, newRc.length());
//                            Integer oldInt = Integer.valueOf(oldRc);
//                            Integer newInt = Integer.valueOf(newRc);
//                            if(newInt > oldInt)
//                            {
//                                retVal = true;
//                                break;
//                            }
//                        }
//                        else if(oldRc.startsWith("BETA") && newRc.startsWith("RC"))
//                        {
//                            retVal = true;
//                        }
//                    }
//                    catch(Exception ignore){}
//                    break;
//                }
//            }
//
//            // Else we use the standard check for the main-version part
//            retVal = isVersionNewerStandardCheck(splitOldVersion[0], splitNewVersion[0]);
//
//        }
//        while(false);

        // Too complex, we simply check if the versions are equal or not
        Boolean retVal = false;
        String workOldVersion = oldVersion.replace("_", "-").toUpperCase();
        String workNewVersion = newVersion.replace("_", "-").toUpperCase();
        if(!workOldVersion.equals(workNewVersion))
        {
            retVal = true;
        }

        return retVal;
    }

    @Override
    protected void updateLatestVersionAndApkDownloadUrl() throws Exception
    {
        String updatePolicy = mSettings.getKodiUpdatePolicy();
        String updateUrl = mUpdateUrl;
        Log.d("KODI-UPDATER", "Policy: " + updatePolicy);

        if(updatePolicy.equals(UPDATE_POLICY.get(2)))
        {
            // Change link to nightlies
            updateUrl = mUpdateUrlNightly;
        }

        Document doc = Jsoup.connect(updateUrl).get();
        Elements files = doc.select("#list tbody tr");
        String latestApk = "";
        for(int i = 0; i < files.size(); i++)
        {
            String foundApk = files.get(i).select("td").first().text();
            Log.d("KODI-UPDATER", "Found: " + foundApk);
            if(foundApk.toLowerCase().endsWith(".apk"))
            {
                if(updatePolicy.equals(UPDATE_POLICY.get(1)) || updatePolicy.equals(UPDATE_POLICY.get(2)))
                {
                    // We use the first file with .apk ending
                    latestApk = foundApk;
                    break;
                }
                else
                {
                    // We have to check if this is no RC, ALPHA, OR BETA
                    String replacedApkName = foundApk.replace("_", "-").toUpperCase();
                    String[] splitApkName = replacedApkName.split("-");
                    if(splitApkName.length < 4 || (!splitApkName[3].startsWith("RC") && !splitApkName[3].startsWith("BETA") && !splitApkName[3].startsWith("ALPHA")))
                    {
                        latestApk = foundApk;
                        break;
                    }
                }
            }
        }
        if(latestApk.equals(""))
        {
            throw new Exception("No apk found on: " + mUpdateUrl);
        }

        mApkDownloadUrl =  updateUrl + latestApk;
        mLatestVersion = getVersion(latestApk);
    }

    /**
     * Get version from file name
     * @param apkName file name to be parsedf
     * @return Version string
     */
    private static String getVersion(String apkName)
    {
        String retVal = null;

        try
        {
            if(apkName != null && !apkName.equals(""))
            {
//                apkName = apkName
//                            .replace("kodi-", "")
//                            .replace("-Isengard", "")
//                            .replace("-Jarvis", "")
//                            .replace("_rc", "-RC")
//                            .replace("_alpha", "-ALPHA")
//                            .replace("-armeabi-v7a.apk", "");
//                retVal = "v" + apkName;

                // We try to split up the kodi apk name into parts
                // and check the correct parts
                String replacedApkName = apkName.replace("_", "-").toUpperCase();
                String[] splitApkName = replacedApkName.split("-");
                if(splitApkName.length >= 2)
                {
                    retVal = "v" + splitApkName[1];
                }
                if(splitApkName.length >= 4 && splitApkName[3].startsWith("RC") || splitApkName[3].startsWith("BETA") || splitApkName[3].startsWith("ALPHA"))
                {
                    retVal += "-" + splitApkName[3];
                }
            }
        }
        catch(Exception ignore) { }

        return retVal;
    }
}
