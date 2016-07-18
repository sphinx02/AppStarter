package de.belu.appstarter.gui;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ListView;

import java.io.PrintWriter;
import java.io.StringWriter;

import de.belu.appstarter.R;
import de.belu.appstarter.tools.AppStarterUpdater;
import de.belu.appstarter.tools.SettingsProvider;
import de.belu.appstarter.tools.Updater;

/**
 * Shows the updates for all available installable apps including AppStarter itself
 */
public class UpdaterActivity extends Fragment
{
    /** Instance of settings provider */
    private SettingsProvider mSettings = SettingsProvider.getInstance(getActivity());

    /** ListView to show all updateable apps */
    private ListView mListView;

    /** Indicates if update shall be triggered directly */
    private Boolean mTriggerUpdate = false;

    /** Updater apps adapter */
    UpdaterAppsAdapter mUpdaterAppsAdapter;

    /** Mandatory for fragment initation */
    public UpdaterActivity(){ }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.updateractivity, container, false);

        // Get ListView
        mListView = (ListView) rootView.findViewById(R.id.listView);

        // Set updateable apps adapter
        mUpdaterAppsAdapter = new UpdaterAppsAdapter(this.getActivity());
        mListView.setAdapter(mUpdaterAppsAdapter);

        // Make buttons useable
        mListView.setItemsCanFocus(true);
        mListView.setFocusable(false);
        mListView.setFocusableInTouchMode(false);
        mListView.setClickable(false);

        // Trigger update mechanism when first view is ready..
        mListView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                try
                {
                    // Remove listener
                    mListView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    if(mTriggerUpdate)
                    {
                        mTriggerUpdate = false;

                        Updater actAppStarterUpdater = (Updater) mUpdaterAppsAdapter.getItem(0);
                        if(actAppStarterUpdater != null && actAppStarterUpdater instanceof AppStarterUpdater)
                        {
                            actAppStarterUpdater.DialogHandler.performUpdate();
                        }
                    }
                }
                catch (Exception e)
                {
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));
                    String errorReason = errors.toString();
                    Log.d(MainActivity.class.getName(), "Failed to trigger update directly: \n" + errorReason);
                }
            }
        });

        return rootView;
    }

    /** Trigger update programmtically */
    public void triggerUpdateOnStartup()
    {
        mTriggerUpdate = true;
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
        mUpdaterAppsAdapter.notifyDataSetChanged();
    }
}
