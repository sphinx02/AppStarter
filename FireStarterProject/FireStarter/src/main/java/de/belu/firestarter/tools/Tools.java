package de.belu.firestarter.tools;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Tools class to provide some additional features
 */
public class Tools
{
    /**
     * Starting an app by its package-name
     * @param context Context in that the app shall be started.
     * @param packageName Name of the apps package
     */
    public static void startAppByPackageName(Context context, String packageName)
    {
        try
        {
            Log.d(Tools.class.getName(), "Starting launcher activity of package: " + packageName);
            Intent i = context.getPackageManager().getLaunchIntentForPackage(packageName);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(i);
        }
        catch(Exception e)
        {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String errorReason = errors.toString();
            Log.d(Tools.class.getName(), "Failed to launch activity: \n" + errorReason);
        }
    }

    /**
     * Uses package manager to find the package name of the device launcher. Usually this package
     * is "com.android.launcher" but can be different at times. This is a generic solution which
     * works on all platforms.`
     */
    public static String getLauncherPackageName(Context context)
    {
        // Create launcher Intent
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);

        // Use PackageManager to get the launcher package name
        PackageManager pm = context.getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo.activityInfo.packageName;
    }

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
}
