package de.belu.firestarter.gui;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;

import de.belu.firestarter.R;
import de.belu.firestarter.observer.BackgroundHomeButtonObserverThread;
import de.belu.firestarter.tools.Tools;

/**
 * Launcher main (shows the user apps)
 */
public class InfosActivity extends Fragment
{
    /** Mandatory for fragment initation */
    public InfosActivity(){ }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.infoactivity, container, false);

        TextView ipAddres = (TextView) rootView.findViewById(R.id.ipAddress);
        ipAddres.setText(Tools.getActiveIpAddress(getActivity(), getActivity().getResources().getString(R.string.notfound)));

        TextView hostname = (TextView) rootView.findViewById(R.id.hostname);
        hostname.setText(Tools.getHostName(getActivity().getResources().getString(R.string.notfound)));

        TextView wifissid = (TextView) rootView.findViewById(R.id.wifissid);
        wifissid.setText(Tools.getWifiSsid(this.getActivity(), getActivity().getResources().getString(R.string.notfound)));

        Button buttonReboot = (Button) rootView.findViewById(R.id.buttonReboot);
        buttonReboot.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(BackgroundHomeButtonObserverThread.CONNECTDEVICE.equals(BackgroundHomeButtonObserverThread.CONNECTEDDEVICE))
                {
                    Toast.makeText(InfosActivity.this.getActivity(), "Reboot only works if Background-Observer is up and running..", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    try
                    {
                        Log.d(InfosActivity.class.getName(), "Send reboot command..");
                        Process p = Runtime.getRuntime().exec(new String[]{"adb", "-s", BackgroundHomeButtonObserverThread.CONNECTEDDEVICE, "reboot"});
                        p.waitFor();
                    }
                    catch (Exception e)
                    {
                        StringWriter errors = new StringWriter();
                        e.printStackTrace(new PrintWriter(errors));
                        String errorReason = errors.toString();
                        Log.d(InfosActivity.class.getName(), "Failed to send reboot command: \n" + errorReason);

                        Toast.makeText(InfosActivity.this.getActivity(), "Failed to send reboot command..", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }
}