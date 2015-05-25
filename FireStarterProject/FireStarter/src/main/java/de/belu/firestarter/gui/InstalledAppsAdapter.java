package de.belu.firestarter.gui;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.belu.firestarter.R;
import de.belu.firestarter.tools.AppInfo;
import de.belu.firestarter.tools.SettingsProvider;
import de.belu.firestarter.tools.Tools;

/**
 * Adapter that lists all installed apps
 */
public class InstalledAppsAdapter extends BaseAdapter
{
    private Context mContext;

    private List<AppInfo> mInstalledApps;

    /** Default launcher package */
    private String mDefaultLauncherPackage;

    /** Settings */
    private SettingsProvider mSettings;

    public InstalledAppsAdapter(Context c)
    {
        mContext = c;

        // Get default-launcher package
        mDefaultLauncherPackage = Tools.getLauncherPackageName(mContext);

        mSettings = new SettingsProvider(mContext);

        loadInstalledApps();
    }

    public int getCount()
    {
        return mInstalledApps.size();
    }

    public Object getItem(int position)
    {
        return mInstalledApps.get(position);
    }

    public long getItemId(int position)
    {
        return position;
    }

    public void storeNewPackageOrder()
    {
        List<String> packageList = new ArrayList<String>();
        for(AppInfo actApp : mInstalledApps)
        {
            packageList.add(actApp.packageName);
        }
        mSettings.setPackageOrder(packageList);
    }

    public void moveAppToPosition(AppInfo app, int position)
    {
        if(mInstalledApps.contains(app))
        {
            try
            {
                mInstalledApps.remove(app);
                mInstalledApps.add(position, app);
                notifyDataSetChanged();
            }
            catch(Exception e)
            {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                String errorReason = errors.toString();
                Log.d(InstalledAppsAdapter.class.getName(), "Error while moving app: \n" + errorReason);
            }
        }
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent)
    {
        // Get act app
        AppInfo actApp = mInstalledApps.get(position);

        // Inflate layout
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View gridView;

        if (convertView == null)
        {
            gridView = new View(mContext);

            // get layout from mobile.xml
            gridView = inflater.inflate(R.layout.griditemlayout, parent, false);

        } else
        {
            gridView = (View) convertView;
        }

        // set value into textview
        TextView textView = (TextView) gridView.findViewById(R.id.textLabel);
        textView.setText(actApp.getDisplayName());

        // set image based on selected text
        ImageView imageView = (ImageView) gridView.findViewById(R.id.imageLabel);
        imageView.setImageDrawable(actApp.getDisplayIcon());

        return gridView;
    }

    public void loadInstalledApps()
    {
        // Get list of installed apps
        PackageManager pm = mContext.getPackageManager();
        List<ApplicationInfo> installedApplications = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        // Put them into a map with packagename as keyword for faster handling
        Map<String, ApplicationInfo> appMap = new LinkedHashMap<String, ApplicationInfo>();
        for(ApplicationInfo installedApplication : installedApplications)
        {
            appMap.put(installedApplication.packageName, installedApplication);
        }

        // Get order of last run
        List<String> packageOrder = mSettings.getPackageOrder();

        // Get own package name to sort out
        String ownPackageName = mContext.getApplicationContext().getPackageName();

        mInstalledApps = new ArrayList<AppInfo>();

        // First handle all apps of the last run
        for(String packageName : packageOrder)
        {
            if(appMap.containsKey(packageName))
            {
                addAppToCurrentList(appMap.get(packageName));
                appMap.remove(packageName);
            }
        }

        // Default the amazon launcher to the first position
        // (if user has moved it, it is not anymore in list here)
        if(appMap.containsKey(mDefaultLauncherPackage))
        {
            addAppToCurrentList(appMap.get(mDefaultLauncherPackage));
            appMap.remove(mDefaultLauncherPackage);
        }

        // Now handle all other apps
        for(ApplicationInfo installedApplication : appMap.values())
        {
            // Sort out system apps & app itself
            if((installedApplication.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 1 &&
                    (installedApplication.flags & ApplicationInfo.FLAG_SYSTEM) != 1 &&
                    !installedApplication.packageName.equals(ownPackageName) &&
                    !installedApplication.packageName.equals(Tools.IKONOTVPACKAGE))
            {
                addAppToCurrentList(installedApplication);
            }
        }
    }

    public void addAppToCurrentList(ApplicationInfo app)
    {
        if(app.packageName.equals(mDefaultLauncherPackage))
        {
            AppInfo amazonLauncher = new AppInfo(mContext, app);
            amazonLauncher.setDisplayName(mContext.getResources().getString(R.string.AmazonHome));
            try
            {
                amazonLauncher.setDisplayIcon(new BitmapDrawable(mContext.getResources(), BitmapFactory.decodeStream(mContext.getAssets().open("amazonhome.png"))));
            }
            catch (Exception ignore){ }

            mInstalledApps.add(amazonLauncher);
        }
        else
        {
            mInstalledApps.add(new AppInfo(mContext, app));
        }
    }
}
