package de.belu.appstarter.gui;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.util.List;
import java.util.Map;

import de.belu.appstarter.R;
import de.belu.appstarter.tools.AppInfo;
import de.belu.appstarter.tools.KodiUpdater;
import de.belu.appstarter.tools.SettingsProvider;
import de.belu.appstarter.tools.Tools;

/**
 * Preferences activity
 */
public class PreferenceActivity extends PreferenceFragment
{
    SettingsProvider mSettings = SettingsProvider.getInstance(this.getActivity());

    SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceListener = new SharedPreferences.OnSharedPreferenceChangeListener()
    {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        {
            Thread readValuesThread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    mSettings.readValues(true);
                }
            });
            readValuesThread.start();
        }
    };

    public PreferenceActivity()
    {

    }

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
//    {
//        View rootView = super.onCreateView(inflater, container, savedInstanceState);
//
//        // Set padding to settings view:
//        Integer pad = Math.round(getActivity().getResources().getDimension(R.dimen.settingspadding));
//        Integer padBottom = Math.round(getActivity().getResources().getDimension(R.dimen.settingspadding_bottom));
//        if (rootView != null)
//        {
//            rootView.setPadding(pad,pad,pad,padBottom);
//        }
//
//        return rootView;
//    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferencesactivity);

        InstalledAppsAdapter actAppsAdapter = new InstalledAppsAdapter(getActivity(), true, false);
        List<AppInfo> actApps = actAppsAdapter.getAppList();

        CharSequence[] entries = new CharSequence[actApps.size() + 1];
        CharSequence[] entryValues = new CharSequence[actApps.size() + 1];

        entries[0] = " - No Action - ";
        entryValues[0] = "";

        for (Integer i = 1; i < actApps.size() + 1; i++)
        {
            AppInfo actApp = actApps.get(i - 1);
            entries[i] = actApp.getDisplayName();
            entryValues[i] = actApp.packageName;
        }

        CharSequence[] langEntries = new CharSequence[SettingsProvider.LANG.size()];
        CharSequence[] langValues = new CharSequence[SettingsProvider.LANG.size()];
        Integer counter = 0;
        for (Map.Entry<String, String> entry : SettingsProvider.LANG.entrySet())
        {
            langEntries[counter] = entry.getValue();
            langValues[counter] = entry.getKey();
            counter++;
        }
        ListPreference languagePreference = (ListPreference) findPreference("prefLanguage");
        languagePreference.setEntries(langEntries);
        languagePreference.setEntryValues(langValues);
        languagePreference.setDefaultValue(mSettings.getLanguage());
        languagePreference.setValue(mSettings.getLanguage());
        languagePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                // Check if value has really changed:
                if (!mSettings.getLanguage().equals(newValue.toString()))
                {
                    // Force reload settings
                    mSettings.setLanguage(newValue.toString());

                    // Restart whole app
                    Tools.doRestart(PreferenceActivity.this.getActivity());
                }
                return true;
            }
        });

        ListPreference prefKodiUpdatePolicy = (ListPreference) findPreference("prefKodiUpdatePolicy");
        prefKodiUpdatePolicy.setEntries(KodiUpdater.UPDATE_POLICY.toArray(new CharSequence[KodiUpdater.UPDATE_POLICY.size()]));
        prefKodiUpdatePolicy.setEntryValues(KodiUpdater.UPDATE_POLICY.toArray(new CharSequence[KodiUpdater.UPDATE_POLICY.size()]));
        prefKodiUpdatePolicy.setDefaultValue(mSettings.getKodiUpdatePolicy());
        prefKodiUpdatePolicy.setValue(mSettings.getKodiUpdatePolicy());

        InstalledAppsAdapter actHiddenAppsAdapter = new InstalledAppsAdapter(getActivity(), true, true);
        List<AppInfo> actHiddenApps = actHiddenAppsAdapter.getAppList();
        CharSequence[] hiddenEntries = new CharSequence[actHiddenApps.size()];
        CharSequence[] hiddenEntryValues = new CharSequence[actHiddenApps.size()];
        for (Integer i = 0; i < actHiddenApps.size(); i++)
        {
            AppInfo actApp = actHiddenApps.get(i);
            hiddenEntries[i] = actApp.getDisplayName();
            hiddenEntryValues[i] = actApp.packageName;
        }

        MultiSelectListPreference hiddenAppsList = (MultiSelectListPreference) findPreference("prefHiddenApps");
        hiddenAppsList.setEntries(hiddenEntries);
        hiddenAppsList.setEntryValues(hiddenEntryValues);
        hiddenAppsList.setDefaultValue(mSettings.getHiddenApps());

        EditTextPreference appIconSize = (EditTextPreference) findPreference("prefAppIconSize");
        appIconSize.setDefaultValue(mSettings.getAppIconSize().toString());
        appIconSize.setText(mSettings.getAppIconSize().toString());
        appIconSize.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                return mSettings.setAppIconSize(newValue, true);
            }
        });

        Preference prefWallpaper = (Preference) findPreference("prefWallpaper");
        prefWallpaper.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                WallpaperSelectDialog wallpaperSelector = new WallpaperSelectDialog((MainActivity) PreferenceActivity.this.getActivity());
                wallpaperSelector.show();
                return false;
            }
        });

        Preference prefExportCurrentSettings = (Preference) findPreference("prefExportCurrentSettings");
        prefExportCurrentSettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                Toast.makeText(getActivity(), Tools.settingsExport(getActivity()), Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        Preference prefImportCurrentSettings = (Preference) findPreference("prefImportCurrentSettings");
        prefImportCurrentSettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                String retVal = Tools.settingsImport(getActivity());
                if (retVal == null)
                {
                    Toast.makeText(getActivity(), "Settings imported successful, restart..", Toast.LENGTH_SHORT).show();
                    Thread restarter = new Thread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                Thread.sleep(2000);
                            }
                            catch (Exception ignore)
                            {
                            }

                            Tools.doRestart(getActivity());
                        }
                    });
                    restarter.start();
                } else
                {
                    Toast.makeText(getActivity(), retVal, Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });

    }


    @Override
    public void onPause()
    {
        super.onPause();

        PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(mSharedPreferenceListener);

        // Force read-settings
        mSettings.readValues(true);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(mSharedPreferenceListener);
    }
}
