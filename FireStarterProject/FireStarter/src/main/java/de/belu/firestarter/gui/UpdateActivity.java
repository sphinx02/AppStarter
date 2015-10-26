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
import de.belu.firestarter.tools.FireStarterUpdater;
import de.belu.firestarter.tools.SettingsProvider;
import de.belu.firestarter.tools.Tools;

/**
 * Launcher main (shows the user apps)
 */
public class UpdateActivity extends Fragment
{
    /** TextView of latest version */
    TextView mLatestVersion;

    /** FireStarterUpdater service */
    FireStarterUpdater mFireStarterUpdater;

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
            mCheckForUpdateProgress = ProgressDialog.show(getActivity(), getActivity().getResources().getString(R.string.update_checkfortitle), getActivity().getResources().getString(R.string.update_checkfordesc), true);
            mFireStarterUpdater.checkForUpdate();
        }
    };

    /** Handle update click */
    View.OnClickListener mUpdateClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            mUpdateProgress = new ProgressDialog(getActivity());
            mUpdateProgress.setMessage(getActivity().getResources().getString(R.string.update_checkformessage));
            mUpdateProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mUpdateProgress.setCancelable(false);
            mUpdateProgress.setProgress(0);
            mUpdateProgress.show();

            mFireStarterUpdater.update(getActivity(), mCurrentAppVersion);

            // Set back have seen setting
            mSettings.setHaveUpdateSeen(false);
        }
    };

    /** Handle check for update */
    FireStarterUpdater.OnCheckForUpdateFinishedListener mOnCheckForUpdateFinishedListener = new FireStarterUpdater.OnCheckForUpdateFinishedListener()
    {
        @Override
        public void onCheckForUpdateFinished(final String message)
        {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mFireStarterUpdater.LATEST_VERSION != null) {
                        if (FireStarterUpdater.isVersionNewer(mCurrentAppVersion, mFireStarterUpdater.LATEST_VERSION)) {
                            mLatestVersion.setText(mFireStarterUpdater.LATEST_VERSION + " - " + getActivity().getResources().getString(R.string.update_foundnew));
                            AppActivity.LATEST_APP_VERSION = mFireStarterUpdater.LATEST_VERSION;
                        } else {
                            mLatestVersion.setText(mFireStarterUpdater.LATEST_VERSION + " - " + getActivity().getResources().getString(R.string.update_foundnotnew));
                        }
                    } else {
                        mLatestVersion.setText(message);
                    }
                    if (mCheckForUpdateProgress != null) {
                        mCheckForUpdateProgress.dismiss();
                        mCheckForUpdateProgress = null;
                    }
                }
            });
        }
    };

    /** Handle update progress */
    FireStarterUpdater.OnUpdateProgressListener mOnUpdateProgressListener = new FireStarterUpdater.OnUpdateProgressListener()
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

        // Set button events
        Button checkUpdateButton = (Button) rootView.findViewById(R.id.buttonCheckForUpdate);
        checkUpdateButton.setOnClickListener(mCheckUpdateClickListener);

        mUpdateButton = (Button) rootView.findViewById(R.id.buttonUpdate);
        mUpdateButton.setOnClickListener(mUpdateClickListener);

        // Set updater
        mFireStarterUpdater = new FireStarterUpdater();
        mFireStarterUpdater.setOnCheckForUpdateFinishedListener(mOnCheckForUpdateFinishedListener);
        mFireStarterUpdater.setOnUpdateProgressListener(mOnUpdateProgressListener);

        // Set latest version
        mLatestVersion = (TextView) rootView.findViewById(R.id.latestVersion);
        if(mFireStarterUpdater.LATEST_VERSION != null)
        {
            mLatestVersion.setText(mFireStarterUpdater.LATEST_VERSION);
        }

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