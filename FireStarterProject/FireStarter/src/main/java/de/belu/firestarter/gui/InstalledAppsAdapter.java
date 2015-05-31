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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.belu.firestarter.R;
import de.belu.firestarter.tools.AppInfo;
import de.belu.firestarter.tools.SettingsProvider;
import de.belu.firestarter.tools.Tools;

/**
 * Adapter that lists all installed apps
 */
public class InstalledAppsAdapter extends BaseAdapter
{
    /** Context we are currently running in */
    private Context mContext;

    /** List of installed apps */
    private List<AppInfo> mInstalledApps;

    /** Default launcher package */
    private String mDefaultLauncherPackage;

    /** Settings */
    private SettingsProvider mSettings;

    /** Include this app */
    private Boolean mIncludeOwnApp;

    /** Show hidden apps */
    private Boolean mShowHiddenApps;

    /**
     * Create new InstalledAppsAdapter
     * @param c Current context
     */
    public InstalledAppsAdapter(Context c)
    {
        this(c, false, false);
    }

    /**
     * Create new InstalledAppsAdapter
     * @param c Current context
     * @param includeOwnApp Include this app (de.belu.firestarter)
     */
    public InstalledAppsAdapter(Context c, Boolean includeOwnApp, Boolean showHiddenApps)
    {
        mContext = c;
        mIncludeOwnApp = includeOwnApp;
        mShowHiddenApps = showHiddenApps;

        // Get default-launcher package
        mDefaultLauncherPackage = Tools.getLauncherPackageName(mContext);

        // Get instance of settingsprovider
        mSettings = SettingsProvider.getInstance(mContext);

        // Now load all installed apps
        loadInstalledApps();
    }

    /**
     * @return Count of installed apps
     */
    public int getCount()
    {
        return mInstalledApps.size();
    }

    /**
     * @param position Position of item to be returned
     * @return Item on position
     */
    public Object getItem(int position)
    {
        return mInstalledApps.get(position);
    }

    /**
     * Currently not used..
     */
    public long getItemId(int position)
    {
        return position;
    }

    /**
     * @return List of found apps
     */
    public List<AppInfo> getAppList()
    {
        return mInstalledApps;
    }

    /**
     * Store current package-order in settings
     */
    public void storeNewPackageOrder()
    {
        List<String> packageList = new ArrayList<String>();
        for(AppInfo actApp : mInstalledApps)
        {
            packageList.add(actApp.packageName);
        }
        mSettings.setPackageOrder(packageList);
    }

    /**
     * Move certain item to certain position
     * @param app Item to move
     * @param position Position to move to
     */
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

    /**
     * @return View of the given position
     */
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

    /**
     * Load all installed apps and order them correctly
     */
    public void loadInstalledApps()
    {
        // Get hashset of hidden apps
        Set<String> hiddenApps;
        if(mShowHiddenApps)
        {
            // Create empty hidden-list
            hiddenApps = new HashSet<String>();
        }
        else
        {
            hiddenApps = mSettings.getHiddenApps();
        }

        // Get list of installed apps
        PackageManager pm = mContext.getPackageManager();
        List<ApplicationInfo> installedApplications = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        // Put them into a map with packagename as keyword for faster handling
        Boolean includeSysApps = mSettings.getShowSystemApps();
        String ownPackageName = mContext.getApplicationContext().getPackageName();
        Map<String, ApplicationInfo> appMap = new LinkedHashMap<String, ApplicationInfo>();
        for(ApplicationInfo installedApplication : installedApplications)
        {
            if(!hiddenApps.contains(installedApplication.packageName))
            {
                // Check for system app
                Boolean isSystemApp = ((installedApplication.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 1 ||
                                       (installedApplication.flags & ApplicationInfo.FLAG_SYSTEM) == 1);

                // If amazon-home, mark not as system app
                if(isSystemApp && installedApplication.packageName.equals(mDefaultLauncherPackage))
                {
                    isSystemApp = false;
                }

                if(includeSysApps || !isSystemApp)
                {
                    if((mIncludeOwnApp || !installedApplication.packageName.equals(ownPackageName)))
                    {
                        appMap.put(installedApplication.packageName, installedApplication);
                    }
                }
            }
        }

        // Get order of last run
        List<String> packageOrder = mSettings.getPackageOrder();

        // Create new list of apps
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
            addAppToCurrentList(installedApplication);        }
    }

    /**
     * @param app App to be added to current apps-list
     */
    private void addAppToCurrentList(ApplicationInfo app)
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
