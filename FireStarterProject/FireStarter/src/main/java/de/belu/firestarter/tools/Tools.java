package de.belu.firestarter.tools;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Tools class to provide some additional infos
 */
public class Tools
{

    /**
     * @param c context
     * @return active ip address
     */
    public static String getActiveIpAddress(Context c, String defValue)
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
                        if(!retVal.equals(""))
                        {
                            retVal += ", ";
                        }
                        // retVal += Formatter.formatIpAddress(inetAddress.hashCode());
                        retVal += inetAddress.getHostAddress().toString();
                    }
                }
            }
        }
        catch (SocketException ex){ }

        if(retVal == null || retVal.equals(""))
        {
            retVal = defValue;
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        String subType = activeNetworkInfo.getSubtypeName();
        if(subType != null && !subType.equals(""))
        {
            subType = "-" + subType;
        }

        retVal = activeNetworkInfo.getTypeName() + subType + ": " + retVal;
        return retVal;
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
}
