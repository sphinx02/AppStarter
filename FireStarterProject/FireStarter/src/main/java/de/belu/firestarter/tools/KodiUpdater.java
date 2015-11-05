package de.belu.firestarter.tools;

import android.content.Context;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class KodiUpdater extends Updater
{
    /** Update URL where updated versions are found */
    private String mUpdateUrl = "http://mirrors.kodi.tv/releases/android/arm/";

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
        // Kodi versiong String can have additional RC-Version which is lower
        // than equal version String without RC-Version
        Boolean retVal = false;

        // Start do-while-false to be able to break out any time
        do
        {
            // First prepare Strings
            String workOldVersion = oldVersion.replace("_", "-").toUpperCase();
            String workNewVersion = newVersion.replace("_", "-").toUpperCase();

            // If Strings are now equal, version is not newer
            if(workOldVersion.equals(workNewVersion))
            {
                retVal = false;
                break;
            }

            // Split version Strings
            String[] splitOldVersion = workOldVersion.split("-");
            String[] splitNewVersion = workNewVersion.split("-");

            // We should have at least one version string
            if(splitOldVersion.length < 1 || splitNewVersion.length < 1)
            {
                retVal = false;
                break;
            }

            // If the first part (the normal version string) is identical
            // we have to perform further tests
            if(splitOldVersion[0].equals(splitNewVersion[0]))
            {
                // If only the oldversion string has the rc-part,
                // the version is newer as no rc-part is newer than any rc-part
                if(splitOldVersion.length == 2 && splitNewVersion.length == 1)
                {
                    retVal = true;
                    break;
                }

                // If both have the rc-part, we have to compare the rc part
                if(splitOldVersion.length == 2 && splitNewVersion.length == 2)
                {
                    try
                    {
                        String oldRc = splitOldVersion[1];
                        String newRc = splitNewVersion[1];

                        if(oldRc.startsWith("RC") && newRc.startsWith("RC"))
                        {
                            oldRc = oldRc.substring(2, oldRc.length());
                            newRc = newRc.substring(2, newRc.length());
                            Integer oldInt = Integer.valueOf(oldRc);
                            Integer newInt = Integer.valueOf(newRc);
                            if(newInt > oldInt)
                            {
                                retVal = true;
                                break;
                            }
                        }
                    }
                    catch(Exception ignore){}
                    break;
                }
            }

            // Else we use the standard check for the main-version part
            retVal = isVersionNewerStandardCheck(splitOldVersion[0], splitNewVersion[0]);

        }
        while(false);

        return retVal;
    }

    @Override
    protected void updateLatestVersionAndApkDownloadUrl() throws Exception
    {
        Document doc = Jsoup.connect(mUpdateUrl).get();
        Elements files = doc.select("#list tbody tr");
        String latestApk = files.get(2).select("td").first().text();

        mApkDownloadUrl =  mUpdateUrl + latestApk;
        mLatestVersion = getVersion(latestApk);
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
                if(splitApkName.length >= 4 && splitApkName[3].startsWith("RC"))
                {
                    retVal += "-" + splitApkName[3];
                }
            }
        }
        catch(Exception ignore) { }

        return retVal;
    }
}
