package de.belu.firestarter.tools;

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
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import de.belu.firestarter.observer.ForeGroundService;

/**
 * Tools class to provide some additional infos
 */
public class Tools
{
    /**
     * Restarts the current application
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

                        // Stop foreground service
                        Intent startIntent = new Intent(c, ForeGroundService.class);
                        startIntent.setAction(ForeGroundService.FOREGROUNDSERVICE_STOP);
                        c.startService(startIntent);

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
        catch(Exception e)
        {
            return defValue;
        }
    }

    /**
     * Retrieves the wifi name
     * @param defValue the value to be returned if info could
     * not be resolved
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
     * Retrieves the net.hostname system property
     * @param defValue the value to be returned if the hostname could
     * not be resolved
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

    public static String getCurrentAppVersion(Context context)
    {
        String retVal = "unknown";

        try
        {
            retVal = "v" + context.getPackageManager().getPackageInfo(context.getApplicationInfo().packageName, 0).versionName;
        }
        catch (PackageManager.NameNotFoundException e)
        {
        }

        return retVal;
    }

    /**
     * Delete a directory recursively
     * @param f Directory
     * @throws IOException
     */
    public static void deleteDirectoryRecursively(Context context, File f, Boolean onlyContent) throws IOException
    {
        if(f.isDirectory())
        {
            for(File c : f.listFiles())
            {
                deleteDirectoryRecursively(context, c, false);
            }
        }

        if(!onlyContent)
        {
            if (!f.delete())
            {
                throw new IOException("Failed to delete file: " + f);
            }
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + f.getAbsolutePath())));
        }
    }

    /**
     * Resize a bitmap to fit the new dimensions (Fit-Center)
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
