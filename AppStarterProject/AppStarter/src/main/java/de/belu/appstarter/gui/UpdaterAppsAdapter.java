package de.belu.appstarter.gui;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.belu.appstarter.R;
import de.belu.appstarter.tools.AppStarterUpdater;
import de.belu.appstarter.tools.KodiUpdater;
import de.belu.appstarter.tools.SPMCUpdater;
import de.belu.appstarter.tools.SettingsProvider;
import de.belu.appstarter.tools.Updater;

/**
 * Adapter that lists all installed apps
 */
public class UpdaterAppsAdapter extends BaseAdapter
{
    Activity mActivity;
    private List<Updater> mUpdaterList;

    /**
     * Create new UpdaterAppsadapter
     */
    public UpdaterAppsAdapter(Activity activity)
    {
        // Set context
        mActivity = activity;

        // Set list of updaters
        mUpdaterList = new ArrayList<>();
        mUpdaterList.add(new AppStarterUpdater());
        mUpdaterList.add(new KodiUpdater(activity));
        mUpdaterList.add(new SPMCUpdater());
    }

    /**
     * @return Count of installed apps
     */
    public int getCount()
    {
        return mUpdaterList.size();
    }

    /**
     * @param position Position of item to be returned
     * @return Item on position
     */
    public Object getItem(int position)
    {
        return mUpdaterList.get(position);
    }

    /**
     * Currently not used..
     */
    public long getItemId(int position)
    {
        return position;
    }

    /**
     * @return View of the given position
     */
    public View getView(int position, View convertView, ViewGroup parent)
    {
        // Get act updater
        final Updater actUpdater = mUpdaterList.get(position);

        // Inflate layout
        View rootView;

        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rootView = new View(mActivity);
            rootView = inflater.inflate(R.layout.appupdateritemlayout, parent, false);

        }
        else
        {
            rootView = (View) convertView;
        }

        // Set title
        TextView textViewTitle = (TextView) rootView.findViewById(R.id.title);
        textViewTitle.setText(actUpdater.getAppName());

        // Set current version
        TextView textViewCurrentVersion = (TextView) rootView.findViewById(R.id.currentVersion);
        textViewCurrentVersion.setText(actUpdater.getCurrentVersion(mActivity));

        // Set latest version
        final TextView textViewLatestVersion = (TextView) rootView.findViewById(R.id.latestVersion);
        String latestVersion = actUpdater.getLatestVersion();
        if(latestVersion == null)
        {
            latestVersion = mActivity.getResources().getString(R.string.update_hitcheckfor);
        }
        textViewLatestVersion.setText(latestVersion);

        // Create an UpdaterDialogHandler
        final UpdaterDialogHandler updaterDialogHandler = new UpdaterDialogHandler(mActivity, actUpdater);
        actUpdater.DialogHandler = updaterDialogHandler;
        updaterDialogHandler.setCheckForUpdateFinishedListener(new Updater.OnCheckForUpdateFinishedListener()
        {
            @Override
            public void onCheckForUpdateFinished(String message)
            {
                if (actUpdater.getLatestVersion() != null)
                {
                    if (actUpdater.isVersionNewer(actUpdater.getCurrentVersion(mActivity), actUpdater.getLatestVersion()))
                    {
                        textViewLatestVersion.setText(actUpdater.getLatestVersion() + " - " + mActivity.getResources().getString(R.string.update_foundnew));

                        if (actUpdater instanceof AppStarterUpdater)
                        {
                            AppActivity.LATEST_APP_VERSION = actUpdater.getLatestVersion();
                        }
                    } else
                    {
                        textViewLatestVersion.setText(actUpdater.getLatestVersion() + " - " + mActivity.getResources().getString(R.string.update_foundnotnew));
                    }
                } else
                {
                    textViewLatestVersion.setText(message);
                }
            }
        });

        // Set the button onclicks
        Button checkUpdateButton = (Button) rootView.findViewById(R.id.buttonCheckForUpdate);
        checkUpdateButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                updaterDialogHandler.checkForUpdate();
            }
        });

        Button updateButton = (Button) rootView.findViewById(R.id.buttonUpdate);
        updateButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                updaterDialogHandler.performUpdate();

                if(actUpdater instanceof AppStarterUpdater)
                {
                    SettingsProvider settings = SettingsProvider.getInstance(mActivity);
                    settings.setHaveUpdateSeen(false);
                }
            }
        });

        if(rootView.getResources().getString(R.string.not_installed).equals(textViewCurrentVersion.getText()))
        {
            // App is not installed, change update to install text
            updateButton.setText(rootView.getResources().getString(R.string.install));
        }

        return rootView;
    }
}
