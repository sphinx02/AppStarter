package de.belu.appstarter.gui;

import android.os.Bundle;
import android.os.SystemClock;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

import de.belu.appstarter.R;
import de.belu.appstarter.tools.Tools;

/**
 * Info / System view
 */
public class InfosPrefActivity extends PreferenceFragment
{
    static final long SLEEPTIME_MINIMUM = 1;
    static final long SLEEPTIME_MAXIMUM = 120;

    public InfosPrefActivity()
    {

    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.infoprefactivity);

        Preference prefDeviceDetails = (Preference) findPreference("prefVirtualDeviceDetails");
        prefDeviceDetails.setSummary(Tools.getDeviceDetails());

        Preference prefHostname = (Preference) findPreference("prefVirtualHostname");
        prefHostname.setSummary(Tools.getHostName(getActivity().getResources().getString(R.string.notfound)));

        Preference prefWifiName = (Preference) findPreference("prefVirtualWifiName");
        prefWifiName.setSummary(Tools.getWifiSsid(this.getActivity(), getActivity().getResources().getString(R.string.notfound)));

        Preference prefIpAddress = (Preference) findPreference("prefVirtualIpAddress");
        prefIpAddress.setSummary(Tools.getActiveIpAddress(getActivity(), getActivity().getResources().getString(R.string.notfound)));

        Preference prefDeviceUpTime = (Preference) findPreference("prefVirtualDeviceUpTime");
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.MEDIUM, getResources().getConfiguration().locale);
        String upTime = String.format(getActivity().getResources().getString(R.string.uptimedesc), Tools.formatInterval(SystemClock.elapsedRealtime()), dateFormat.format(new Date(System.currentTimeMillis() - SystemClock.elapsedRealtime())));
        prefDeviceUpTime.setSummary(upTime);

        final EditTextPreference prefSetSleepTimeout = (EditTextPreference) findPreference("prefVirtualSleepTime");
        prefSetSleepTimeout.setText(((Long)(Tools.getSleepModeTimeout(getActivity()) / 1000 / 60)).toString());
        prefSetSleepTimeout.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                long newTimeInMinutes = 0;
                try
                {
                    newTimeInMinutes = Long.valueOf(newValue.toString());
                }
                catch (Exception ignore)
                {
                }

                do
                {
                    if (newTimeInMinutes < SLEEPTIME_MINIMUM || newTimeInMinutes > SLEEPTIME_MAXIMUM)
                    {
                        Toast.makeText(InfosPrefActivity.this.getActivity(), String.format(getActivity().getResources().getString(R.string.sleeptime_limit), SLEEPTIME_MINIMUM, SLEEPTIME_MAXIMUM), Toast.LENGTH_SHORT).show();
                        break;
                    }

                    long newTimeInMs = newTimeInMinutes * 60 * 1000;
                    Tools.setSleepModeTimeout(getActivity(), newTimeInMs);
                    prefSetSleepTimeout.setText(((Long)(Tools.getSleepModeTimeout(getActivity()) / 1000 / 60)).toString());
                }
                while (false);

                return false;
            }
        });

        Preference prefGoToSleep = (Preference) findPreference("prefVirtualGoToSleep");
        prefGoToSleep.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                Toast.makeText(InfosPrefActivity.this.getActivity(), getActivity().getResources().getString(R.string.gotosleep_summary), Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        Preference prefReboot = (Preference) findPreference("prefVirtualRestart");
        prefReboot.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                Toast.makeText(InfosPrefActivity.this.getActivity(), getActivity().getResources().getString(R.string.system_restart_summary_removed), Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }
}
