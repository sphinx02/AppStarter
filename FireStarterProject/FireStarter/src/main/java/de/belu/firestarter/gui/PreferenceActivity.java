package de.belu.firestarter.gui;

import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import java.util.List;
import java.util.Map;

import de.belu.firestarter.R;
import de.belu.firestarter.observer.ForeGroundService;
import de.belu.firestarter.tools.AppInfo;
import de.belu.firestarter.tools.SettingsProvider;
import de.belu.firestarter.tools.Tools;

/**
 * Preferences activity
 */
public class PreferenceActivity extends PreferenceFragment
{
    SettingsProvider mSettings = SettingsProvider.getInstance(this.getActivity());

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

        ListPreference startUpPackage = (ListPreference) findPreference("prefStartupPackage");
        startUpPackage.setEntries(entries);
        startUpPackage.setEntryValues(entryValues);
        startUpPackage.setDefaultValue(mSettings.getStartupPackage());
        startUpPackage.setValue(mSettings.getStartupPackage());

        ListPreference singleClick = (ListPreference) findPreference("prefHomeSingleClickPackage");
        singleClick.setEntries(entries);
        singleClick.setEntryValues(entryValues);
        singleClick.setDefaultValue(mSettings.getSingleClickApp());
        singleClick.setValue(mSettings.getSingleClickApp());

        ListPreference doubleClick = (ListPreference) findPreference("prefHomeDoubleClickPackage");
        doubleClick.setEntries(entries);
        doubleClick.setEntryValues(entryValues);
        doubleClick.setDefaultValue(mSettings.getDoubleClickApp());
        doubleClick.setValue(mSettings.getDoubleClickApp());

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

        EditTextPreference doubleClickInterval = (EditTextPreference) findPreference("prefClickInterval");
        doubleClickInterval.setDefaultValue(mSettings.getDoubleClickInterval().toString());
        doubleClickInterval.setText(mSettings.getDoubleClickInterval().toString());
        doubleClickInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                return mSettings.setDoubleClickInterval(newValue, true);
            }
        });

        EditTextPreference jumpbackWatchdogTime = (EditTextPreference) findPreference("prefJumpbackWatchdogTime");
        jumpbackWatchdogTime.setDefaultValue(mSettings.getJumpbackWatchdogTime().toString());
        jumpbackWatchdogTime.setText(mSettings.getJumpbackWatchdogTime().toString());
        jumpbackWatchdogTime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                return mSettings.setJumpbackWatchdogTime(newValue, true);
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
                if(retVal == null)
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
                            catch (Exception ignore){ }

                            Tools.doRestart(getActivity());
                        }
                    });
                    restarter.start();
                }
                else
                {
                    Toast.makeText(getActivity(), retVal, Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });

        Preference prefBackgroundObservationEnabled = (Preference) findPreference("prefBackgroundObservationEnabled");
        prefBackgroundObservationEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                mSettings.setBackgroundObserverEnabled((Boolean)newValue);

                if(mSettings.getBackgroundObserverEnabled())
                {
                    // Start foreground service
                    Intent startIntent = new Intent(PreferenceActivity.this.getActivity(), ForeGroundService.class);
                    startIntent.setAction(ForeGroundService.FOREGROUNDSERVICE_START);
                    PreferenceActivity.this.getActivity().startService(startIntent);
                }
                else
                {
                    // Stop foreground service
                    Intent stopIntent = new Intent(PreferenceActivity.this.getActivity(), ForeGroundService.class);
                    stopIntent.setAction(ForeGroundService.FOREGROUNDSERVICE_STOP);
                    PreferenceActivity.this.getActivity().startService(stopIntent);
                }

                return true;
            }
        });

        Preference prefBackgroundObservationViaAdb = (Preference) findPreference("prefBackgroundObservationViaAdb");
        prefBackgroundObservationViaAdb.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                mSettings.setBackgroundObservationViaAdb((Boolean) newValue);

                // Stop foreground service
                Intent stopIntent = new Intent(PreferenceActivity.this.getActivity(), ForeGroundService.class);
                stopIntent.setAction(ForeGroundService.FOREGROUNDSERVICE_STOP);
                PreferenceActivity.this.getActivity().startService(stopIntent);

                if(mSettings.getBackgroundObserverEnabled())
                {
                    // Start foreground service
                    Intent startIntent = new Intent(PreferenceActivity.this.getActivity(), ForeGroundService.class);
                    startIntent.setAction(ForeGroundService.FOREGROUNDSERVICE_START);
                    PreferenceActivity.this.getActivity().startService(startIntent);
                }

                return true;
            }
        });
    }


    @Override
    public void onPause()
    {
        super.onPause();

        // Force read-settings
        mSettings.readValues(true);
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }
}
