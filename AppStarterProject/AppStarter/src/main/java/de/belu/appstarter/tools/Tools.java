package de.belu.appstarter.tools;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import de.belu.appstarter.R;

/**
 * Tools class to provide some additional infos
 */
public class Tools
{
    // Fallback-value if timeout-value is not found
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    // Path of the backup-file
    private static final String EXPORT_FILE_PATH_NAME = new File(Environment.getExternalStorageDirectory(), "AppStarterBackup.zip").getAbsolutePath();

    /**
     * Restarts the current application
     *
     * @param c
     */
    public static void doRestart(Context c)
    {
        try
        {
            //check if the context is given
            if (c != null)
            {
                //fetch the packagemanager so we can get the default launch activity
                // (you can replace this intent with any other activity if you want
                PackageManager pm = c.getPackageManager();
                //check if we got the PackageManager
                if (pm != null)
                {
                    //create the intent with the default start activity for your application
                    Intent mStartActivity = pm.getLaunchIntentForPackage(c.getPackageName());
                    if (mStartActivity != null)
                    {
                        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                        //create a pending intent so the application is restarted after System.exit(0) was called.
                        // We use an AlarmManager to call this intent in 100ms
                        int mPendingIntentId = 223344;
                        PendingIntent mPendingIntent = PendingIntent.getActivity(c, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);

                        //kill the application
                        System.exit(0);
                    } else
                    {
                        Log.e("AppRestarter", "Was not able to restart application, mStartActivity null");
                    }
                } else
                {
                    Log.e("AppRestarter", "Was not able to restart application, PM null");
                }
            } else
            {
                Log.e("AppRestarter", "Was not able to restart application, Context null");
            }
        }
        catch (Exception ex)
        {
            Log.e("AppRestarter", "Was not able to restart application");
        }
    }

    public static long getSleepModeTimeout(Context c)
    {
        long currentTimeout = Settings.System.getLong(c.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE);
        Log.d("SLEEPMODE", "CurrentSleepModeTime: " + currentTimeout);
        return currentTimeout;
    }

    public static void setSleepModeTimeout(Context c, long valueInMs)
    {
        Settings.System.putInt(c.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, (int) valueInMs);
    }

    public static String formatInterval(final long ms)
    {
        long millis = ms % 1000;
        long x = ms / 1000;
        long seconds = x % 60;
        x /= 60;
        long minutes = x % 60;
        x /= 60;
        long hours = x % 24;
        x /= 24;
        long days = x;

        return String.format("%dd %02dh %02dm %02ds", days, hours, minutes, seconds, millis);
    }

    /**
     * @param c context
     * @return active ip address
     */
    public static String getActiveIpAddress(Context c, String defValue)
    {
        try
        {
            String retVal = "";

            try
            {
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); )
                {
                    NetworkInterface intf = en.nextElement();
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); )
                    {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress())
                        {
                            if (!retVal.equals(""))
                            {
                                retVal += ", ";
                            }
                            // retVal += Formatter.formatIpAddress(inetAddress.hashCode());
                            retVal += inetAddress.getHostAddress().toString();
                        }
                    }
                }
            }
            catch (SocketException ex)
            {
            }

            if (retVal == null || retVal.equals(""))
            {
                retVal = defValue;
            }

            ConnectivityManager connectivityManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

            String subType = activeNetworkInfo.getSubtypeName();
            if (subType != null && !subType.equals(""))
            {
                subType = "-" + subType;
            }

            retVal = activeNetworkInfo.getTypeName() + subType + ": " + retVal;
            return retVal;
        }
        catch (Exception e)
        {
            return defValue;
        }
    }

    /**
     * Retrieves the wifi name
     *
     * @param defValue the value to be returned if info could
     *                 not be resolved
     */
    public static String getWifiSsid(Context context, String defValue)
    {
        try
        {
            WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            return wifiInfo.getSSID();
        }
        catch (Exception ex)
        {
            return defValue;
        }
    }

    /**
     * @return Details about the device
     */
    public static String getDeviceDetails()
    {
        String versionName = getProp("ro.build.version.name");
        if(!versionName.equals(""))
        {
            versionName = "- " + versionName;
        }

        String retVal = String.format("%s - Android %s %s\n\t-> %s", Build.MODEL, Build.VERSION.RELEASE, versionName, Build.DISPLAY);
        return retVal;
    }

    /**
     * Makes a system call to "getprop" and returns the property
     * @param propertyName name of the property like ro.build.version.name
     * @return property content or empty string if not found
     */
    public static String getProp(String propertyName)
    {
        String result = "";
        if(propertyName != null && !propertyName.equals(""))
        {
            Process getProcess = null;
            try
            {
                getProcess = Runtime.getRuntime().exec("getprop " + propertyName);
                BufferedReader reader = new BufferedReader(new InputStreamReader(getProcess.getInputStream()));
                result = reader.readLine().trim();
                getProcess.destroy();
            }
            catch (java.io.IOException e)
            {
            }
        }
        return result;
    }

    /**
     * Retrieves the net.hostname system property
     *
     * @param defValue the value to be returned if the hostname could
     *                 not be resolved
     */
    public static String getHostName(String defValue)
    {
        try
        {
            Method getString = Build.class.getDeclaredMethod("getString", String.class);
            getString.setAccessible(true);
            return getString.invoke(null, "net.hostname").toString();
        }
        catch (Exception ex)
        {
            return defValue;
        }
    }

    /**
     * @param c
     * @param dip Value in DIP
     * @return Value in Pixel
     */
    public static int getPixelFromDip(Context c, int dip)
    {
        int retVal = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, c.getResources().getDisplayMetrics());
        return retVal;
    }

    /**
     * @param context
     * @return Current version of AppStarter
     */
    public static String getCurrentAppVersion(Context context)
    {
        return getCurrentAppVersion(context, context.getApplicationInfo().packageName);
    }

    /**
     * @param context
     * @return Current version of an app
     */
    public static String getCurrentAppVersion(Context context, String app)
    {
        String retVal = context.getResources().getString(R.string.not_installed);

        try
        {
            retVal = "v" + context.getPackageManager().getPackageInfo(app, 0).versionName;
        }
        catch (PackageManager.NameNotFoundException e)
        {
        }

        return retVal;
    }

    /**
     * Delete a directory recursively
     *
     * @param f Directory
     * @throws IOException
     */
    public static void deleteDirectoryRecursively(Context context, File f, Boolean onlyContent) throws IOException
    {
        if (f.isDirectory())
        {
            for (File c : f.listFiles())
            {
                deleteDirectoryRecursively(context, c, false);
            }
        }

        if (!onlyContent)
        {
            if (!f.delete())
            {
                throw new IOException("Failed to delete file: " + f);
            }
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + f.getAbsolutePath())));
        }
    }

    public static String settingsExport(Context c)
    {
        String retVal = "Settings export failed..";

        try
        {
            ZipDirectory.zipDirectory(c.getApplicationInfo().dataDir, EXPORT_FILE_PATH_NAME);
            retVal = "Settings exported to: " + EXPORT_FILE_PATH_NAME;
        }
        catch(Exception e)
        {
            retVal += " " + e.getMessage();
        }

        return retVal;
    }

    public static String settingsImport(Context c)
    {
        String retVal = null;

        try
        {
            File dataDir = new File(c.getApplicationInfo().dataDir);
            ZipDirectory.unZipDirectory(new File(EXPORT_FILE_PATH_NAME), dataDir);
        }
        catch(Exception e)
        {
            retVal = "Settings import failed: " + e.getMessage();
        }

        return retVal;
    }

    /**
     * Resize a bitmap to fit the new dimensions (Fit-Center)
     *
     * @param source
     * @param fitWidth
     * @param fitHeight
     * @return
     */
    public static Bitmap resizeBitmapToFit(Bitmap source, Integer fitWidth, Integer fitHeight)
    {
        return ThumbnailUtils.extractThumbnail(source, fitWidth, fitHeight);
//        // Set new width and height to fit center
//        int targetW = fitWidth;
//        int targetH = fitHeight;
//        Log.d("PHOTO", "Target-Size : " + targetW + "x" + targetH);
//
//        // Get size of current bitmap
//        int photoW = source.getWidth();
//        int photoH = source.getHeight();
//        Log.d("PHOTO", "Source-Size : " + photoW + "x" + photoH);
//
//        // Create and return correct bitmap
//        Matrix m = new Matrix();
//        m.setRectToRect(new RectF(0, 0, photoW, photoH), new RectF(0, 0, targetW, targetH), Matrix.ScaleToFit.CENTER);
//
//        Bitmap retVal = Bitmap.createBitmap(source, 0, 0, photoW, photoH, m, true);
//        Log.d("PHOTO", "Cropped-Size: " + retVal.getWidth() + "x" + retVal.getHeight());
//        return retVal;
    }
}
