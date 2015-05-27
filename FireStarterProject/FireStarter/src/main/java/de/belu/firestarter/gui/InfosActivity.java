package de.belu.firestarter.gui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.belu.firestarter.R;
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