package de.belu.firestarter.gui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;

import de.belu.firestarter.R;
import de.belu.firestarter.observer.BackgroundHomeButtonObserverThread;
import de.belu.firestarter.tools.Tools;

/**
 * Info / System view
 */
public class InfosPrefActivity extends PreferenceFragment
{
    static final long SLEEPTIME_MINIMUM = 1;
    static final long SLEEPTIME_MAXIMUM = 30;

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

        EditTextPreference prefSetSleepTimeout = (EditTextPreference) findPreference("prefVirtualSleepTime");
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
                catch(Exception ignore){}

                do
                {
                    if(newTimeInMinutes < SLEEPTIME_MINIMUM || newTimeInMinutes > SLEEPTIME_MAXIMUM)
                    {
                        Toast.makeText(InfosPrefActivity.this.getActivity(), String.format(getActivity().getResources().getString(R.string.sleeptime_limit), SLEEPTIME_MINIMUM, SLEEPTIME_MAXIMUM), Toast.LENGTH_SHORT).show();
                        break;
                    }

                    long newTimeInMs = newTimeInMinutes * 60 * 1000;
                    Tools.setSleepModeTimeout(getActivity(), newTimeInMs);
                }
                while(false);

                return false;
            }
        });

        Preference prefGoToSleep = (Preference) findPreference("prefVirtualGoToSleep");
        prefGoToSleep.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                if(BackgroundHomeButtonObserverThread.CONNECTDEVICE.equals(BackgroundHomeButtonObserverThread.CONNECTEDDEVICE))
                {
                    Toast.makeText(InfosPrefActivity.this.getActivity(), "Sleep only works if Background-Observer is up and running..", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Tools.setSleepModeDirectActive(InfosPrefActivity.this.getActivity());
                    final ProgressDialog actDialog = ProgressDialog.show(getActivity(), getActivity().getResources().getString(R.string.gotosleep_waitforsleep_title), getActivity().getResources().getString(R.string.gotosleep_waitforsleep), true);
                    actDialog.setOnDismissListener(new DialogInterface.OnDismissListener()
                    {
                        @Override
                        public void onDismiss(DialogInterface dialog)
                        {
                            // If user dismiss dialog before the Thread
                            // is dismissing the dialog, stop go to sleep..
                            Tools.setSleepModeDirectNotActive(getActivity());
                            Toast.makeText(InfosPrefActivity.this.getActivity(), R.string.gotosleep_cancel, Toast.LENGTH_SHORT).show();
                        }
                    });
                    actDialog.setCancelable(true);

                    Thread closerThread = new Thread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                Thread.sleep(10000);
                            }
                            catch (Exception ignore) { }
                            if(actDialog != null && actDialog.isShowing())
                            {
                                actDialog.setOnDismissListener(null);
                                actDialog.dismiss();
                            }
                        }
                    });
                    closerThread.start();
                }
                return false;
            }
        });

        Preference prefReboot = (Preference) findPreference("prefVirtualRestart");
        prefReboot.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                if(BackgroundHomeButtonObserverThread.CONNECTDEVICE.equals(BackgroundHomeButtonObserverThread.CONNECTEDDEVICE))
                {
                    Toast.makeText(InfosPrefActivity.this.getActivity(), "Reboot only works if Background-Observer is up and running..", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    try
                    {
                        Log.d(InfosPrefActivity.class.getName(), "Send reboot command..");
                        Process p = Runtime.getRuntime().exec(new String[]{"adb", "-s", BackgroundHomeButtonObserverThread.CONNECTEDDEVICE, "reboot"});
                        p.waitFor();
                    }
                    catch (Exception e)
                    {
                        StringWriter errors = new StringWriter();
                        e.printStackTrace(new PrintWriter(errors));
                        String errorReason = errors.toString();
                        Log.d(InfosPrefActivity.class.getName(), "Failed to send reboot command: \n" + errorReason);

                        Toast.makeText(InfosPrefActivity.this.getActivity(), "Failed to send reboot command..", Toast.LENGTH_SHORT).show();
                    }
                }
                return false;
            }
        });
    }
}
