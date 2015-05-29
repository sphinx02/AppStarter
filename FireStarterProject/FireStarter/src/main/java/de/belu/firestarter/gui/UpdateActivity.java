package de.belu.firestarter.gui;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import de.belu.firestarter.R;
import de.belu.firestarter.tools.SettingsProvider;
import de.belu.firestarter.tools.Tools;
import de.belu.firestarter.tools.Updater;

/**
 * Launcher main (shows the user apps)
 */
public class UpdateActivity extends Fragment
{
    /** TextView of latest version */
    TextView mLatestVersion;

    /** Updater service */
    Updater mUpdater;

    /** Check for update progress */
    ProgressDialog mCheckForUpdateProgress = null;

    /** Update progress */
    ProgressDialog mUpdateProgress = null;

    /** Current app version */
    String mCurrentAppVersion;

    /** Update button to trigger updates */
    Button mUpdateButton;

    /** Indicates if update shall be triggered directly */
    private Boolean mTriggerUpdate = false;

    /** Instance of settings provider */
    SettingsProvider mSettings = SettingsProvider.getInstance(getActivity());

    /** Handle check update click */
    View.OnClickListener mCheckUpdateClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            mCheckForUpdateProgress = ProgressDialog.show(getActivity(), "Checking for updates", "Wait till checked Github for latest release.", true);
            mUpdater.checkForUpdate();
        }
    };

    /** Handle update click */
    View.OnClickListener mUpdateClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            mUpdateProgress = new ProgressDialog(getActivity());
            mUpdateProgress.setMessage("Check for update.");
            mUpdateProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mUpdateProgress.setCancelable(false);
            mUpdateProgress.setProgress(0);
            mUpdateProgress.show();

            mUpdater.updateFireStarter(getActivity(), mCurrentAppVersion);

            // Set back have seen setting
            mSettings.setHaveUpdateSeen(false);
        }
    };

    /** Handle check for update */
    Updater.OnCheckForUpdateFinishedListener mOnCheckForUpdateFinishedListener = new Updater.OnCheckForUpdateFinishedListener()
    {
        @Override
        public void onCheckForUpdateFinished(final String message)
        {
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if(mUpdater.LATEST_VERSION != null)
                    {
                        if(Updater.isVersionNewer(mCurrentAppVersion, Updater.LATEST_VERSION))
                        {
                            mLatestVersion.setText(Updater.LATEST_VERSION + " - Found newer Version - Update Now!");
                        }
                        else
                        {
                            mLatestVersion.setText(Updater.LATEST_VERSION + " - You have already the newest version!");
                        }
                    }
                    else
                    {
                        mLatestVersion.setText(message);
                    }
                    if(mCheckForUpdateProgress != null)
                    {
                        mCheckForUpdateProgress.dismiss();
                        mCheckForUpdateProgress = null;
                    }
                }
            });
        }
    };

    /** Handle update progress */
    Updater.OnUpdateProgressListener mOnUpdateProgressListener = new Updater.OnUpdateProgressListener()
    {
        @Override
        public void onUpdateProgress(final Boolean isError,final Integer percent, final String message)
        {
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if (isError)
                    {
                        mUpdateProgress.setProgress(100);
                        mUpdateProgress.setMessage(message);
                        mUpdateProgress.setCancelable(true);
                    }
                    else
                    {
                        mUpdateProgress.setProgress(percent);
                        mUpdateProgress.setMessage(message);
                        if(percent >= 100)
                        {
                            mUpdateProgress.setCancelable(true);
                        }
                    }
                }
            });
        }
    };

    /** Mandatory for fragment initation */
    public UpdateActivity(){ }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.updateactivity, container, false);

        // Set current version
        TextView firestarterversion = (TextView) rootView.findViewById(R.id.currentVersion);
        mCurrentAppVersion = Tools.getCurrentAppVersion(getActivity());
        firestarterversion.setText(mCurrentAppVersion);

        // Set latest version
        mLatestVersion = (TextView) rootView.findViewById(R.id.latestVersion);
        if(Updater.LATEST_VERSION != null)
        {
            mLatestVersion.setText(Updater.LATEST_VERSION);
        }

        // Set button events
        Button checkUpdateButton = (Button) rootView.findViewById(R.id.buttonCheckForUpdate);
        checkUpdateButton.setOnClickListener(mCheckUpdateClickListener);

        mUpdateButton = (Button) rootView.findViewById(R.id.buttonUpdate);
        mUpdateButton.setOnClickListener(mUpdateClickListener);

        // Set updater
        mUpdater = new Updater();
        mUpdater.setOnCheckForUpdateFinishedListener(mOnCheckForUpdateFinishedListener);
        mUpdater.setOnUpdateProgressListener(mOnUpdateProgressListener);

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
        if(mTriggerUpdate)
        {
            mTriggerUpdate = false;
            if(mUpdateButton != null)
            {
                mUpdateButton.requestFocusFromTouch();
                mUpdateButton.setSelected(true);
                mUpdateButton.callOnClick();
            }
        }
        super.onResume();
    }
}