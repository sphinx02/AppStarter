package de.belu.firestarter.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Settings Provider to store all Settins of the app
 */
public class SettingsProvider
{
    /** Private static singleton object */
    private static SettingsProvider _instance;

    /** Synchronized singleton getter (Thread Safe) */
    public static synchronized SettingsProvider getInstance (Context context)
    {
        if(SettingsProvider._instance == null)
        {
            SettingsProvider._instance = new SettingsProvider(context);
        }
        return SettingsProvider._instance;
    }

    /** Instance of app-shared preferences */
    SharedPreferences mPreferences;

    /** Indicator for package-order */
    final static String PACKAGEORDER = "packageorder_";

    /** Indicates if settings are already loaded */
    Boolean mIsLoaded = false;

    /** Package-Order List */
    List<String> mPackageOrder = new ArrayList<String>();

    /** Background observer enabled */
    Boolean mBackgroundObserverEnabled = true;

    /** Startup package name */
    String mStartupPackage;

    /** Single click app */
    String mSingleClickApp;

    /** Double click app */
    String mDoubleClickApp;

    /** List of hidden apps */
    Set<String> mHiddenAppsList = new HashSet<String>();

    /** Show system apps */
    Boolean mShowSystemApps = false;

    /** Create a the instace of SettingsProvider */
    private SettingsProvider(Context context)
    {
        mStartupPackage = context.getApplicationInfo().packageName;
        mSingleClickApp = context.getApplicationInfo().packageName;
        mDoubleClickApp = ""; // No action
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Set order of packages for app-drawer
     * @param packageOrder List of packagenames in correct order
     */
    public void setPackageOrder(List<String> packageOrder)
    {
        mPackageOrder = packageOrder;
        storeValues();
    }

    /**
     * @return List of packagenames in last saved order
     */
    public List<String> getPackageOrder()
    {
        readValues();
        return mPackageOrder;
    }


    public void setBackgroundObserverEnabled(Boolean value)
    {
        mBackgroundObserverEnabled = value;
        storeValues();
    }
    public Boolean getBackgroundObserverEnabled()
    {
        readValues();
        return mBackgroundObserverEnabled;
    }

    public void setStartupPackage(String startupPackage)
    {
        mStartupPackage = startupPackage;
        storeValues();
    }
    public String getStartupPackage()
    {
        readValues();
        return mStartupPackage;
    }

    public void setSingleClickApp(String singleClickApp)
    {
        mSingleClickApp = singleClickApp;
        storeValues();
    }
    public String getSingleClickApp()
    {
        readValues();
        return mSingleClickApp;
    }

    public void setDoubleClickApp(String doubleClickApp)
    {
        mDoubleClickApp = doubleClickApp;
        storeValues();
    }
    public String getDoubleClickApp()
    {
        readValues();
        return mDoubleClickApp;
    }

    public void setHiddenApps(Set<String> hiddenApps)
    {
        mHiddenAppsList = hiddenApps;
        storeValues();
    }
    public Set<String> getHiddenApps()
    {
        readValues();
        return mHiddenAppsList;
    }

    public void setShowSystemApps(Boolean value)
    {
        mShowSystemApps = value;
        storeValues();
    }
    public Boolean getShowSystemApps()
    {
        readValues();
        return mShowSystemApps;
    }

    /**
     * Read values from settings
     */
    public void readValues()
    {
        readValues(false);
    }

    /**
     * @param forceRead Force reading values from preferences
     */
    public void readValues(Boolean forceRead)
    {
        // Load only once (hold in singleton)
        if(mIsLoaded && !forceRead)
        {
            return;
        }

        // PackageList
        List<String> packageList = new ArrayList<String>();
        Integer size = mPreferences.getInt(PACKAGEORDER + "size", 0);
        for(Integer i=0; i<size; i++)
        {
            String actKey = PACKAGEORDER + i.toString();
            packageList.add(mPreferences.getString(actKey, null));
        }
        mPackageOrder = packageList;

        // BackgroundObserverEnabled
        mBackgroundObserverEnabled = mPreferences.getBoolean("prefBackgroundObservationEnabled", mBackgroundObserverEnabled);

        // Startup-package
        mStartupPackage = mPreferences.getString("prefStartupPackage", mStartupPackage);

        // Single click package
        mSingleClickApp = mPreferences.getString("prefHomeSingleClickPackage", mSingleClickApp);

        // Double click package
        mDoubleClickApp = mPreferences.getString("prefHomeDoubleClickPackage", mDoubleClickApp);

        // HiddenApps-List
        mHiddenAppsList = mPreferences.getStringSet("prefHiddenApps", mHiddenAppsList);

        // Show sys apps
        mShowSystemApps = mPreferences.getBoolean("prefShowSysApps", mShowSystemApps);

        // Set is loaded flag
        mIsLoaded = true;
    }

    /**
     * Store values to settings
     */
    public void storeValues()
    {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.clear();


        // PackageList
        editor.putInt(PACKAGEORDER + "size", mPackageOrder.size());
        for(Integer i = 0; i < mPackageOrder.size(); i++)
        {
            String actKey = PACKAGEORDER + i.toString();
            editor.remove(actKey);
            editor.putString(actKey, mPackageOrder.get(i));
        }

        // BackgroundObserverEnabled
        editor.putBoolean("prefBackgroundObservationEnabled", mBackgroundObserverEnabled);

        // Startup package
        editor.putString("prefStartupPackage", mStartupPackage);

        // Single click
        editor.putString("prefHomeSingleClickPackage", mSingleClickApp);

        // Double click
        editor.putString("prefHomeDoubleClickPackage", mDoubleClickApp);

        // Hidden apps list
        editor.putStringSet("prefHiddenApps", mHiddenAppsList);

        // Show sys apps
        editor.putBoolean("prefShowSysApps", mShowSystemApps);


        editor.commit();
    }
}
