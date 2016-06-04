package de.belu.appstarter.tools;

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

import de.belu.appstarter.R;

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
        put("GERMAN", "Deutsch");
        put("ENGLISH", "English");
        //put("hu", "Magyar");
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

    /** Language string */
    String mLanguage = "";

    /** Update policy of Kodi */
    String mKodiUpdatePolicy = KodiUpdater.UPDATE_POLICY.get(0);

    /** List of hidden apps */
    Set<String> mHiddenAppsList = new HashSet<String>();

    /** Show system apps */
    Boolean mShowSystemApps = false;

    /** Size of the app icons */
    Integer mAppIconSize = 0;

    /** Indicates if user have seen update but do not want to update */
    Boolean mHaveUpdateSeen = false;

    /** Automatically select first icon when switching to app-drawer */
    Boolean mAutoSelectFirstIcon = true;

    /** Indicates if the LeftBar is hided when on the app overview */
    Boolean mHideLeftBarInAppOverview = false;

    /** Indicates if app names in the app drawer are having a background */
    Boolean mShowBackgroundForAppNames = false;

    /** Create a the instace of SettingsProvider */
    private SettingsProvider(Context context)
    {
        mContext = context;
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

    public void setHideLeftBarInAppOverview(Boolean value)
    {
        mHideLeftBarInAppOverview = value;
        storeValues();
    }
    public Boolean getHideLeftBarInAppOverview()
    {
        readValues();
        return mHideLeftBarInAppOverview;
    }

    public void setShowBackgroundForAppNames(Boolean value)
    {
        mShowBackgroundForAppNames = value;
        storeValues();
    }
    public Boolean getShowBackgroundForAppNames()
    {
        readValues();
        return mShowBackgroundForAppNames;
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

    public void setKodiUpdatePolicy(String policy)
    {
        mKodiUpdatePolicy = policy;
        storeValues();
    }
    public String getKodiUpdatePolicy()
    {
        readValues();
        return mKodiUpdatePolicy;
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

            // HideLeftBarInAppOverview
            mHideLeftBarInAppOverview = mPreferences.getBoolean("prefHideLeftBarInAppOverview", mHideLeftBarInAppOverview);

            // ShowBackgroundForAppNames
            mShowBackgroundForAppNames = mPreferences.getBoolean("prefShowBackgroundForAppNames", mShowBackgroundForAppNames);

            // Have update seen
            mHaveUpdateSeen = mPreferences.getBoolean("prefHaveUpdateSeen", mHaveUpdateSeen);

            // Auto select first icon
            mAutoSelectFirstIcon = mPreferences.getBoolean("prefAutoSelectFirstIcon", mAutoSelectFirstIcon);

            // HiddenApps-List
            mHiddenAppsList = mPreferences.getStringSet("prefHiddenApps", mHiddenAppsList);

            // Show sys apps
            mShowSystemApps = mPreferences.getBoolean("prefShowSysApps", mShowSystemApps);

            // lang
            mLanguage = mPreferences.getString("prefLanguage", mLanguage);

            // Kodi update policy
            mKodiUpdatePolicy = mPreferences.getString("prefKodiUpdatePolicy", mKodiUpdatePolicy);

            // App icon size
            String pref = mPreferences.getString("prefAppIconSize", mAppIconSize.toString());
            if(setAppIconSize(pref, true))
            {
                mAppIconSize = Integer.valueOf(pref);
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
            editor.putBoolean("prefHideLeftBarInAppOverview", mHideLeftBarInAppOverview);

            // ShowBackgroundForAppNames
            editor.putBoolean("prefShowBackgroundForAppNames", mShowBackgroundForAppNames);

            // Update seen
            editor.putBoolean("prefHaveUpdateSeen", mHaveUpdateSeen);

            // Auto select first icon
            editor.putBoolean("prefAutoSelectFirstIcon", mAutoSelectFirstIcon);

            // Hidden apps list
            editor.putStringSet("prefHiddenApps", mHiddenAppsList);

            // Show sys apps
            editor.putBoolean("prefShowSysApps", mShowSystemApps);

            // Kodi update policy
            editor.putString("prefKodiUpdatePolicy", mKodiUpdatePolicy);

            // Lang
            editor.putString("prefLanguage", mLanguage);

            // App icon size
            editor.putString("prefAppIconSize", mAppIconSize.toString());


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
