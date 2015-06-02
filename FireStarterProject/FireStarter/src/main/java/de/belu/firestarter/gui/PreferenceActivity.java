package de.belu.firestarter.gui;

import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import java.util.List;

import de.belu.firestarter.R;
import de.belu.firestarter.observer.ForeGroundService;
import de.belu.firestarter.tools.AppInfo;
import de.belu.firestarter.tools.SettingsProvider;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferencesactivity);

        InstalledAppsAdapter actAppsAdapter = new InstalledAppsAdapter(getActivity(), true, false);
        List<AppInfo> actApps = actAppsAdapter.getAppList();

        CharSequence[] entries = new CharSequence[actApps.size()+1];
        CharSequence[] entryValues = new CharSequence[actApps.size()+1];

        entries[0] = " - No Action - ";
        entryValues[0] = "";

        for(Integer i = 1; i < actApps.size()+1; i++)
        {
            AppInfo actApp = actApps.get(i-1);
            entries[i] = actApp.getDisplayName();
            entryValues[i] = actApp.packageName;
        }

        ListPreference startUpPackage = (ListPreference) findPreference("prefStartupPackage");
        startUpPackage.setEntries(entries);
        startUpPackage.setEntryValues(entryValues);
        startUpPackage.setDefaultValue(mSettings.getStartupPackage());

        ListPreference singleClick = (ListPreference) findPreference("prefHomeSingleClickPackage");
        singleClick.setEntries(entries);
        singleClick.setEntryValues(entryValues);
        singleClick.setDefaultValue(mSettings.getSingleClickApp());

        ListPreference doubleClick = (ListPreference) findPreference("prefHomeDoubleClickPackage");
        doubleClick.setEntries(entries);
        doubleClick.setEntryValues(entryValues);
        doubleClick.setDefaultValue(mSettings.getDoubleClickApp());

        InstalledAppsAdapter actHiddenAppsAdapter = new InstalledAppsAdapter(getActivity(), true, true);
        List<AppInfo> actHiddenApps = actHiddenAppsAdapter.getAppList();
        CharSequence[] hiddenEntries = new CharSequence[actHiddenApps.size()];
        CharSequence[] hiddenEntryValues = new CharSequence[actHiddenApps.size()];
        for(Integer i = 0; i < actHiddenApps.size(); i++)
        {
            AppInfo actApp = actHiddenApps.get(i);
            hiddenEntries[i] = actApp.getDisplayName();
            hiddenEntryValues[i] = actApp.packageName;
        }

        MultiSelectListPreference hiddenAppsList = (MultiSelectListPreference) findPreference("prefHiddenApps");
        hiddenAppsList.setEntries(hiddenEntries);
        hiddenAppsList.setEntryValues(hiddenEntryValues);
        hiddenAppsList.setDefaultValue(mSettings.getHiddenApps());

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
    }



    @Override
    public void onPause()
    {
        super.onPause();

        // Force read-settings
        mSettings.readValues(true);

        // Check if background observer is active
        if(mSettings.getBackgroundObserverEnabled())
        {
            // Start foreground service
            Intent startIntent = new Intent(this.getActivity(), ForeGroundService.class);
            startIntent.setAction(ForeGroundService.FOREGROUNDSERVICE_START);
            this.getActivity().startService(startIntent);
        }
        else
        {
            // Stop foreground service
            Intent startIntent = new Intent(this.getActivity(), ForeGroundService.class);
            startIntent.setAction(ForeGroundService.FOREGROUNDSERVICE_STOP);
            this.getActivity().startService(startIntent);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }
}
