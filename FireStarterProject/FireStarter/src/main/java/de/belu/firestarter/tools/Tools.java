package de.belu.firestarter.tools;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Tools class to provide some additional features
 */
public class Tools
{
    /** Name of FiredTV Launcher Package */
    public static final String FIREDTVPACKAGE = "com.altusapps.firedtvlauncher";

    /** Name of Ikono-TV Package */
    public static final String IKONOTVPACKAGE = "org.ikonotv.smarttv";

    /** Indicates if the amazon home screen is expicitly opened by the user */
    public static Boolean IsAmazonHomeScreenWanted = false;

    /** Path of the Ikono-Icons */
    private static final String IKONOICONPATH = ".imagecache/com.amazon.venezia/" + IKONOTVPACKAGE + "/";

    /**
     * Start FiredTV Launcher
     * @param context Context in that the Launcher shall be started.
     */
    public static void startFiredTv(Context context)
    {
        startAppByPackageName(context, FIREDTVPACKAGE);
    }

    /**
     * Starting an app by its package-name
     * @param context Context in that the app shall be started.
     * @param packageName Name of the apps package
     */
    public static void startAppByPackageName(Context context, String packageName)
    {
        Log.d(Tools.class.getName(), "Starting Launcher Activity: " + packageName);
        Intent i = context.getPackageManager().getLaunchIntentForPackage(packageName);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(i);
    }

    /**
     * Tries to replace the icon of ikono-tv app
     */
    public static void replaceIkonoIcon(Context context)
    {
        File sdCard = Environment.getExternalStorageDirectory();
        File ikonoPrePrePath = new File(sdCard, IKONOICONPATH);
        Log.d(Tools.class.getName(), "Path: " + ikonoPrePrePath.getAbsolutePath());
        if(ikonoPrePrePath.exists() && ikonoPrePrePath.isDirectory())
        {
            Log.d(Tools.class.getName(), "Path: " + ikonoPrePrePath.getAbsolutePath() + " is directory");

            // Now get subfolders:
            File[] directories = ikonoPrePrePath.listFiles(new FilenameFilter()
            {
                @Override
                public boolean accept(File current, String name)
                {
                    return new File(current, name).isDirectory();
                }
            });
            if(directories.length > 0)
            {
                // Sort by date (newest one gets last one)
                Arrays.sort(directories, new Comparator<File>()
                {
                    public int compare(File f1, File f2)
                    {
                        return Long.valueOf(f1.lastModified()).compareTo(Long.valueOf(f2.lastModified()));
                    }
                });

                String dirs = directories[0].getAbsolutePath();
                for(int i = 1; i < directories.length; i++)
                {
                    dirs += "\n" + directories[i].getAbsolutePath();
                }
                Log.d(Tools.class.getName(), "Found Directories: \n" + dirs);

                // Go on with newest directory
                File[] iconFiles = directories[directories.length-1].listFiles();
                if(iconFiles.length > 0)
                {
                    String icons = iconFiles[0].getAbsolutePath();
                    for(int i = 1; i < iconFiles.length; i++)
                    {
                        icons += "\n" + iconFiles[i].getAbsolutePath();
                    }
                    Log.d(Tools.class.getName(), "Found Icons: \n" + icons);

                    try
                    {
                        // Get link to icon replacement
                        AssetManager assetManager = context.getAssets();
                        Bitmap replacementBitmapOrig = BitmapFactory.decodeStream(assetManager.open("replacementicon.png"));

                        // Try to replace every icon file:
                        for(File icon : iconFiles)
                        {
                            Log.d(Tools.class.getName(), "Replacing: " + icon.getAbsolutePath());
                            Bitmap replacementBitmap = resizeBitmap(replacementBitmapOrig, icon.getAbsolutePath());
                            FileOutputStream fos = new FileOutputStream(icon.getAbsolutePath());
                            replacementBitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                            fos.close();
                        }
                    }
                    catch (Exception e)
                    {
                        StringWriter errors = new StringWriter();
                        e.printStackTrace(new PrintWriter(errors));
                        String errorReason = errors.toString();
                        Log.d(Tools.class.getName(), "Error in Icon-Replacement: \n" + errorReason);
                    }
                }
            }
        }
    }

    /**
     * Creates an resized Bitmap from source with the dimensions of targetPath
     * @param source Source Bitmap
     * @param targetPath Path of the Target-Image-File (used to get dimensions)
     * @return Resized Bitmap
     */
    private static Bitmap resizeBitmap(Bitmap source, String targetPath)
    {
        // Get widht and height from targetpath
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(targetPath, bmOptions);
        int targetW = bmOptions.outWidth;
        int targetH = bmOptions.outHeight;

        // Now generate correct pic
        int photoW = source.getWidth();
        int photoH = source.getHeight();

        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, photoW, photoH), new RectF(0, 0, targetW, targetH), Matrix.ScaleToFit.CENTER);

        return Bitmap.createBitmap(source, 0, 0, photoW, photoH, m, true);
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
}
