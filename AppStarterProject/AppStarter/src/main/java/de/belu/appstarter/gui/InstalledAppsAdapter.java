package de.belu.appstarter.gui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.belu.appstarter.R;
import de.belu.appstarter.tools.AppInfo;
import de.belu.appstarter.tools.AppStarter;
import de.belu.appstarter.tools.SettingsProvider;
import de.belu.appstarter.tools.Tools;

/**
 * Adapter that lists all installed apps
 */
public class InstalledAppsAdapter extends BaseAdapter
{
    /** Virtual package for settings app */
    public static final String VIRTUAL_SETTINGS_PACKAGE = "de.belu.appstarter.virtual.settings";

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
     * @param includeOwnApp Include this app (de.belu.appstarter)
     */
    public InstalledAppsAdapter(Context c, Boolean includeOwnApp, Boolean showHiddenApps)
    {
        mContext = c;
        mIncludeOwnApp = includeOwnApp;
        mShowHiddenApps = showHiddenApps;

        // Get default-launcher package
        mDefaultLauncherPackage = AppStarter.getLauncherPackageName(mContext);

        // Get instance of settingsprovider
        mSettings = SettingsProvider.getInstance(mContext);

        // Now load all installed apps
        loadInstalledApps();
    }

    /**
     * Create a launchable intent by package-name
     * @param context Current context
     * @param packageName Package name of app
     * @return Launchable intent
     */
    @SuppressLint("NewApi")
    public static Intent getLaunchableIntentByPackageName(Context context, String packageName)
    {
        // Create intent
        Intent launchIntent = null;

        // Check if package is one of the virtual packages
        if(packageName.equals(VIRTUAL_SETTINGS_PACKAGE))
        {
            launchIntent = new Intent(android.provider.Settings.ACTION_SETTINGS);

            // Start settings as new activity
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        else
        {
            try
            {
                launchIntent = context.getPackageManager().getLeanbackLaunchIntentForPackage(packageName);
            }
            catch(Throwable ignore) { }

            if(launchIntent == null)
            {
                launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            }
        }

        // Return the launchable intent
        return launchIntent;
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

            // get layout depending on setting
            if(mSettings.getShowBackgroundForAppNames())
            {
                gridView = inflater.inflate(R.layout.appdrawergriditemlayout_withbackground, parent, false);
            }
            else
            {
                gridView = inflater.inflate(R.layout.appdrawergriditemlayout, parent, false);
            }

        } else
        {
            gridView = (View) convertView;
        }

        Integer appIconSize = mSettings.getAppIconSize();
        if(appIconSize > 0)
        {
            // Set size of items
            appIconSize = Tools.getPixelFromDip(parent.getContext(), appIconSize);
            LinearLayout linearLayout = (LinearLayout) gridView.findViewById(R.id.linearLayout);
            ViewGroup.LayoutParams params = linearLayout.getLayoutParams();
            params.width = appIconSize;
            params.height = appIconSize;
            linearLayout.setLayoutParams(params);
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

        // Add virtual packages if not hided
        if(!hiddenApps.contains(VIRTUAL_SETTINGS_PACKAGE))
        {
            ApplicationInfo applicationInfo = new ApplicationInfo();
            applicationInfo.packageName = VIRTUAL_SETTINGS_PACKAGE;
            appMap.put(VIRTUAL_SETTINGS_PACKAGE, new AppInfo(mContext, applicationInfo)
            {
                @Override
                public String getDisplayName()
                {
                    return mContext.getResources().getString(R.string.AmazonSettings);
                }

                @Override
                public Drawable getDisplayIcon()
                {
                    Drawable retVal = null;

                    try
                    {
                        retVal = new BitmapDrawable(mContext.getResources(), BitmapFactory.decodeStream(mContext.getAssets().open("firetv-settings-icon.png")));
                    }
                    catch (Exception ignore){ }

                    return retVal;
                }
            });
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

        // Default amazon settings to the second position
        // (if user has moved it, it is not anymore in list here)
        if(appMap.containsKey(VIRTUAL_SETTINGS_PACKAGE))
        {
            addAppToCurrentList(appMap.get(VIRTUAL_SETTINGS_PACKAGE));
            appMap.remove(VIRTUAL_SETTINGS_PACKAGE);
        }

        // Now handle all other apps
        for(ApplicationInfo installedApplication : appMap.values())
        {
            addAppToCurrentList(installedApplication);
        }
    }

    /**
     * @param app App to be added to current apps-list
     */
    private void addAppToCurrentList(ApplicationInfo app)
    {
        if(app.packageName.equals(mDefaultLauncherPackage))
        {
            AppInfo amazonLauncher = new AppInfo(mContext, app)
            {
                @Override
                public String getDisplayName()
                {
                    return mContext.getResources().getString(R.string.AmazonHome);
                }

                @Override
                public Drawable getDisplayIcon()
                {
                    Drawable retVal = null;

                    try
                    {
                        retVal = new BitmapDrawable(mContext.getResources(), BitmapFactory.decodeStream(mContext.getAssets().open("firetv-home-icon.png")));
                    }
                    catch (Exception ignore){ }

                    return retVal;
                }
            };

            mInstalledApps.add(amazonLauncher);
        }
        else
        {
            if(app instanceof AppInfo)
            {
                mInstalledApps.add((AppInfo)app);
            }
            else
            {
                mInstalledApps.add(new AppInfo(mContext, app));
            }
        }
    }
}
