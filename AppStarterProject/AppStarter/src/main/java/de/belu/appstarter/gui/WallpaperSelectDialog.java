package de.belu.appstarter.gui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import de.belu.appstarter.R;
import de.belu.appstarter.tools.Tools;

/**
 * Created by luki on 28.06.15.
 */
public class WallpaperSelectDialog
{
    /** Current MainActivity */
    MainActivity mMainActivity;

    /** Current Wallpaper Path */
    File mWallpaperPath;

    public WallpaperSelectDialog(MainActivity mainActivity)
    {
        mMainActivity = mainActivity;
        mWallpaperPath = new File(mMainActivity.getApplicationInfo().dataDir, "wallpaper.png");
    }

    public void setWallpaper(Boolean forceError)
    {
        try
        {
            if(!forceError)
            {
                if(!mWallpaperPath.exists())
                {
                    return;
                }
            }
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeFile(mWallpaperPath.getAbsolutePath(), bmOptions);

            BitmapDrawable drawable = new BitmapDrawable(mMainActivity.getResources(), bitmap);
            mMainActivity.setBackgroundImage(drawable);
        }
        catch(Exception e)
        {
            Toast.makeText(mMainActivity, "Could not set wallpaper", Toast.LENGTH_SHORT).show();
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String errorReason = errors.toString();
            Log.d(WallpaperSelectDialog.class.getName(), "Failed to set background: \n" + errorReason);
        }
    }

    public void show()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);

        builder.setTitle("JPG/PNG - 1920x1080px");

        List<String> items = new ArrayList<String>();
        if(mWallpaperPath.exists())
        {
            items.add(mMainActivity.getResources().getString(R.string.remove_wallpaper));
        }
        items.add(mMainActivity.getResources().getString(R.string.select_wallpaper));

        final String[] itemList = items.toArray(new String[items.size()]);

        builder.setItems(itemList, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
                dialog.dismiss();

                String itemChosen = itemList[which];
                if (itemChosen == mMainActivity.getResources().getString(R.string.remove_wallpaper))
                {
                    deleteWallpaper();
                }
                else if(itemChosen == mMainActivity.getResources().getString(R.string.select_wallpaper))
                {
                    showFileSelectorDialog();
                }
            }
        });

        Dialog dialog = builder.show();
        dialog.show();
    }

    private void deleteWallpaper()
    {
        if(mWallpaperPath.exists())
        {
            mWallpaperPath.delete();
            Tools.doRestart(mMainActivity);
        }
    }

    private void showFileSelectorDialog()
    {
        File mPath = Environment.getExternalStorageDirectory();
        FileDialog fileDialog = new FileDialog(mMainActivity, mPath);
        fileDialog.setFileEndsWith(new String[]{".jpg", ".jpeg", ".png"});
        fileDialog.addFileListener(new FileDialog.FileSelectedListener()
        {
            public void fileSelected(File file)
            {
                WallpaperSelectDialog.this.handleFileSelected(file);
            }
        });
        //fileDialog.addDirectoryListener(new FileDialog.DirectorySelectedListener() {
        //  public void directorySelected(File directory) {
        //      Log.d(getClass().getName(), "selected dir " + directory.toString());
        //  }
        //});
        //fileDialog.setSelectDirectoryOption(false);
        fileDialog.showDialog();
    }

    private void handleFileSelected(File file)
    {
        // Now we try to resize and set this file
        try
        {
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), bmOptions);

            // Resize image
            bitmap = Tools.resizeBitmapToFit(bitmap, mMainActivity.getBackgroundWidth(), mMainActivity.getBackgroundHeight());

            File dir = mWallpaperPath.getParentFile();
            if(!dir.exists())
            {
                dir.mkdirs();
            }
            if(mWallpaperPath.exists())
            {
                mWallpaperPath.delete();
            }

            FileOutputStream fOut = new FileOutputStream(mWallpaperPath);

            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fOut);
            fOut.flush();
            fOut.close();

            // Now try to load this one
            setWallpaper(true);
        }
        catch(Exception e)
        {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String errorReason = errors.toString();
            Log.d(MainActivity.class.getName(), "Failed to set background: \n" + errorReason);
        }
    }
}
