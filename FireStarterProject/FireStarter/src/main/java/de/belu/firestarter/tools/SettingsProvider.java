package de.belu.firestarter.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import de.belu.firestarter.R;

/**
 * Settings Provider to store all Settins of the app
 */
public class SettingsProvider
{
    /** Map of possible languages */
    public static final Map<String, String> LANG = new LinkedHashMap<String, String>()
    {{
        // Key is the static field name of Locale (e.g. Locale.GERMAN or Locale.ENGLISH)
        // Value is the displayed value for the settings
        put("", "Auto");
        put("ENGLISH", "English");
        put("GERMAN", "Deutsch");
        put("ru", "Русский");
        put("uk", "Українська");
    }};

    /** Private static singleton object */
    private static SettingsProvider _instance;

    /** Semaphore to limit readValues and writeValues access */
    private static Semaphore mSemaphore = new Semaphore(1);

    /** Synchronized singleton getter (Thread Safe) */
    public static synchronized SettingsProvider getInstance (Context context)
    {
        if(SettingsProvider._instance == null)
        {
            SettingsProvider._instance = new SettingsProvider(context);
        }
        return SettingsProvider._instance;
    }

    /** Stored context */
    Context mContext;

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

    /** Language string */
    String mLanguage = "";

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

    /** Size of the app icons */
    Integer mAppIconSize = 0;

    /** Time to wait for second click in milliseconds */
    Integer mDoubleClickInterval = 500;

    /** Time to watch for jumpbacks */
    Integer mJumpbackWatchdogTime = 5000;

    /** Indicates if user have seen update but do not want to update */
    Boolean mHaveUpdateSeen = false;

    /** Automatically select first icon when switching to app-drawer */
    Boolean mAutoSelectFirstIcon = true;

    /** FLAG_ACTIVITY_CLEAR_TASK is used for single click actions */
    Boolean mClearPreviousInstancesForSingleClick = false;

    /** FLAG_ACTIVITY_CLEAR_TASK is used for double click actions */
    Boolean mClearPreviousInstancesForDoubleClick = false;

    /** Create a the instace of SettingsProvider */
    private SettingsProvider(Context context)
    {
        mContext = context;
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

    public void setClearPreviousInstancesForDoubleClick(Boolean value)
    {
        mClearPreviousInstancesForDoubleClick = value;
        storeValues();
    }
    public Boolean getClearPreviousInstancesForDoubleClick()
    {
        readValues();
        return mClearPreviousInstancesForDoubleClick;
    }

    public void setClearPreviousInstancesForSingleClick(Boolean value)
    {
        mClearPreviousInstancesForSingleClick = value;
        storeValues();
    }
    public Boolean getClearPreviousInstancesForSingleClick()
    {
        readValues();
        return mClearPreviousInstancesForSingleClick;
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

    public void setHaveUpdateSeen(Boolean value)
    {
        mHaveUpdateSeen = value;
        storeValues();
    }
    public Boolean getHaveUpdateSeen()
    {
        readValues();
        return mHaveUpdateSeen;
    }

    public void setAutoSelectFirstIcon(Boolean value)
    {
        mAutoSelectFirstIcon = value;
        storeValues();
    }
    public Boolean getAutoSelectFirstIcon()
    {
        readValues();
        return mAutoSelectFirstIcon;
    }

    public void setLanguage(String language)
    {
        mLanguage = language;
        storeValues();
    }
    public String getLanguage()
    {
        readValues();
        return mLanguage;
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

    public Boolean setAppIconSize(Object appIconSize, Boolean simulate)
    {
        Boolean retVal = numberCheck(appIconSize, 0, 200);
        if(!simulate && retVal)
        {
            setAppIconSize(Integer.valueOf(appIconSize.toString()));
        }
        return retVal;
    }
    public void setAppIconSize(Integer appIconSize)
    {
        mAppIconSize = appIconSize;
        storeValues();
    }
    public Integer getAppIconSize()
    {
        readValues();
        return mAppIconSize;
    }

    public Boolean setDoubleClickInterval(Object doubleClickInterval, Boolean simulate)
    {
        Boolean retVal = numberCheck(doubleClickInterval, 100, 1000);
        if(!simulate && retVal)
        {
            setDoubleClickInterval(Integer.valueOf(doubleClickInterval.toString()));
        }
        return retVal;
    }
    public void setDoubleClickInterval(Integer doubleClickInterval)
    {
        mDoubleClickInterval = doubleClickInterval;
        storeValues();
    }
    public Integer getDoubleClickInterval()
    {
        readValues();
        return mDoubleClickInterval;
    }

    public Boolean setJumpbackWatchdogTime(Object jumpbackWatchdogTime, Boolean simulate)
    {
        Boolean retVal = numberCheck(jumpbackWatchdogTime, 0, 10000);
        if(!simulate && retVal)
        {
            setJumpbackWatchdogTime(Integer.valueOf(jumpbackWatchdogTime.toString()));
        }
        return retVal;
    }
    public void setJumpbackWatchdogTime(Integer jumpbackWatchdogTime)
    {
        mJumpbackWatchdogTime = jumpbackWatchdogTime;
        storeValues();
    }
    public Integer getJumpbackWatchdogTime()
    {
        readValues();
        return mJumpbackWatchdogTime;
    }

    private boolean numberCheck(Object newValue, Integer min, Integer max)
    {
        Boolean retVal = false;

        try
        {
            if(!newValue.toString().equals("")  &&  newValue.toString().matches("\\d*") )
            {
                Integer value = Integer.valueOf(newValue.toString());
                if(value >= min && value <= max)
                {
                    retVal = true;
                }
            }
        }
        catch(Exception ignore){}

        if(!retVal)
        {
            Toast.makeText(mContext, newValue + " " + mContext.getResources().getString(R.string.is_invalid_number), Toast.LENGTH_SHORT).show();
        }

        return retVal;
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
        try
        {
            // ATTENTION: NEVER CALL ONE OF THE GETTERS OR SETTERS IN HERE!
            // Aquire semaphore
            mSemaphore.acquire();

            // Load only once (hold in singleton)
            if (mIsLoaded && !forceRead)
            {
                mSemaphore.release();
                return;
            }

            // PackageList
            List<String> packageList = new ArrayList<String>();
            Integer size = mPreferences.getInt(PACKAGEORDER + "size", 0);
            for (Integer i = 0; i < size; i++)
            {
                String actKey = PACKAGEORDER + i.toString();
                packageList.add(mPreferences.getString(actKey, null));
            }
            mPackageOrder = packageList;

            // BackgroundObserverEnabled
            mBackgroundObserverEnabled = mPreferences.getBoolean("prefBackgroundObservationEnabled", mBackgroundObserverEnabled);

            // Have update seen
            mHaveUpdateSeen = mPreferences.getBoolean("prefHaveUpdateSeen", mHaveUpdateSeen);

            // Auto select first icon
            mAutoSelectFirstIcon = mPreferences.getBoolean("prefAutoSelectFirstIcon", mAutoSelectFirstIcon);

            // ClearPreviousInstancesForDoubleClick
            mClearPreviousInstancesForDoubleClick = mPreferences.getBoolean("prefClearPreviousInstancesForDoubleClick", mClearPreviousInstancesForDoubleClick);

            // ClearPreviousInstancesForSingleClick
            mClearPreviousInstancesForSingleClick = mPreferences.getBoolean("prefClearPreviousInstancesForSingleClick", mClearPreviousInstancesForSingleClick);

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

            // lang
            mLanguage = mPreferences.getString("prefLanguage", mLanguage);

            // App icon size
            String pref = mPreferences.getString("prefAppIconSize", mAppIconSize.toString());
            if(setAppIconSize(pref, true))
            {
                mAppIconSize = Integer.valueOf(pref);
            }

            // Double click interval
            pref = mPreferences.getString("prefClickInterval", mDoubleClickInterval.toString());
            if(setDoubleClickInterval(pref, true))
            {
                mDoubleClickInterval = Integer.valueOf(pref);
            }

            // Jumpback watchdog time
            pref = mPreferences.getString("prefJumpbackWatchdogTime", mJumpbackWatchdogTime.toString());
            if(setJumpbackWatchdogTime(pref, true))
            {
                mJumpbackWatchdogTime = Integer.valueOf(pref);
            }

            // Set is loaded flag
            mIsLoaded = true;
        }
        catch(Exception e)
        {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String errorReason = errors.toString();
            Log.d(SettingsProvider.class.getName(), "Exception while reading settings: \n" + errorReason);
        }

        mSemaphore.release();
    }

    /**
     * Store values to settings
     */
    public void storeValues()
    {
        try
        {
            // ATTENTION: NEVER CALL ONE OF THE GETTERS OR SETTERS IN HERE!
            // Aquire semaphore
            mSemaphore.acquire();

            // Get editor, clear editor and save new values
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.clear();


            // PackageList
            editor.putInt(PACKAGEORDER + "size", mPackageOrder.size());
            for (Integer i = 0; i < mPackageOrder.size(); i++)
            {
                String actKey = PACKAGEORDER + i.toString();
                editor.remove(actKey);
                editor.putString(actKey, mPackageOrder.get(i));
            }

            // BackgroundObserverEnabled
            editor.putBoolean("prefBackgroundObservationEnabled", mBackgroundObserverEnabled);

            // Update seen
            editor.putBoolean("prefHaveUpdateSeen", mHaveUpdateSeen);

            // Auto select first icon
            editor.putBoolean("prefAutoSelectFirstIcon", mAutoSelectFirstIcon);

            // ClearPreviousInstancesForDoubleClick
            editor.putBoolean("prefClearPreviousInstancesForDoubleClick", mClearPreviousInstancesForDoubleClick);

            // ClearPreviousInstancesForSingleClick
            editor.putBoolean("prefClearPreviousInstancesForSingleClick", mClearPreviousInstancesForSingleClick);

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

            // Lang
            editor.putString("prefLanguage", mLanguage);

            // App icon size
            editor.putString("prefAppIconSize", mAppIconSize.toString());

            // Double click interval
            editor.putString("prefClickInterval", mDoubleClickInterval.toString());

            // JumpbackWatchdogTime
            editor.putString("prefJumpbackWatchdogTime", mJumpbackWatchdogTime.toString());


            editor.commit();
        }
        catch(Exception e)
        {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String errorReason = errors.toString();
            Log.d(SettingsProvider.class.getName(), "Exception while reading settings: \n" + errorReason);
        }

        mSemaphore.release();
    }
}
