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
import de.belu.firestarter.tools.KodiUpdater;

/**
 * Launcher main (shows the user apps)
 */
public class UpdateActivity extends Fragment
{
    /** TextView of latest version */
    TextView mLatestVersion;

    /** TextView of latest Kodi version */
    TextView mLatestVersionKodi;

    /** FireStarterUpdater service */
    FireStarterUpdater mFireStarterUpdater;

    /** Kodi FireStarterUpdater service */
    KodiUpdater mKodiUpdater;

    /** Check for update progress */
    ProgressDialog mCheckForUpdateProgress = null;

    /** Update progress */
    ProgressDialog mUpdateProgress = null;

    /** Current app version */
    String mCurrentAppVersion;

    /** Current Kodi version */
    String mCurrentAppVersionKodi;

    /** Update button to trigger updates */
    Button mUpdateButton;

    /** Update button to trigger updates */
    Button mUpdateKodiButton;

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

    /** Handle check Kodi update click */
    View.OnClickListener mCheckKodiUpdateClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            mCheckForUpdateProgress = ProgressDialog.show(getActivity(), getActivity().getResources().getString(R.string.update_checkfortitle), getActivity().getResources().getString(R.string.update_checkfordesc), true);
            mKodiUpdater.checkForUpdate();
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

    /** Handle Kodi update click */
    View.OnClickListener mUpdateKodiClickListener = new View.OnClickListener()
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

            mKodiUpdater.update(getActivity(), mCurrentAppVersionKodi);
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
                        if (FireStarterUpdater.isVersionNewer(mCurrentAppVersion, FireStarterUpdater.LATEST_VERSION)) {
                            mLatestVersion.setText(FireStarterUpdater.LATEST_VERSION + " - " + getActivity().getResources().getString(R.string.update_foundnew));
                        } else {
                            mLatestVersion.setText(FireStarterUpdater.LATEST_VERSION + " - " + getActivity().getResources().getString(R.string.update_foundnotnew));
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

    /** Handle check for update */
    KodiUpdater.OnCheckForUpdateFinishedListener mOnCheckForKodiUpdateFinishedListener = new KodiUpdater.OnCheckForUpdateFinishedListener()
    {
        @Override
        public void onCheckForUpdateFinished(final String message)
        {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mKodiUpdater.LATEST_VERSION != null) {
                        if (KodiUpdater.isVersionNewer(mCurrentAppVersionKodi, KodiUpdater.LATEST_VERSION)) {
                            mLatestVersionKodi.setText(KodiUpdater.LATEST_VERSION + " - " + getActivity().getResources().getString(R.string.update_foundnew));
                        } else {
                            mLatestVersionKodi.setText(KodiUpdater.LATEST_VERSION + " - " + getActivity().getResources().getString(R.string.update_foundnotnew));
                        }
                    } else {
                        mLatestVersionKodi.setText(message);
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

    /** Handle Kodi update progress */
    KodiUpdater.OnUpdateProgressListener mOnUpdateKodiProgressListener = new KodiUpdater.OnUpdateProgressListener()
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

        // Set current version
        TextView kodiversion = (TextView) rootView.findViewById(R.id.currentVersionKodi);
        mCurrentAppVersionKodi = Tools.getCurrentAppVersion(getActivity(), "org.xbmc.kodi");
        kodiversion.setText(mCurrentAppVersionKodi);
        if (mCurrentAppVersionKodi == null)
        {
            mUpdateKodiButton.setText(getResources().getString(R.string.install));
        }

        // Set latest version
        mLatestVersion = (TextView) rootView.findViewById(R.id.latestVersion);
        if(FireStarterUpdater.LATEST_VERSION != null)
        {
            mLatestVersion.setText(FireStarterUpdater.LATEST_VERSION);
        }

        // Set latest Kodi version
        mLatestVersionKodi = (TextView) rootView.findViewById(R.id.latestVersionKodi);
        if(KodiUpdater.LATEST_VERSION != null)
        {
            mLatestVersionKodi.setText(KodiUpdater.LATEST_VERSION);
        }

        // Set button events
        Button checkUpdateButton = (Button) rootView.findViewById(R.id.buttonCheckForUpdate);
        checkUpdateButton.setOnClickListener(mCheckUpdateClickListener);

        // Set Kodi button events
        Button checkUpdateKodiButton = (Button) rootView.findViewById(R.id.buttonCheckForUpdateKodi);
        checkUpdateKodiButton.setOnClickListener(mCheckKodiUpdateClickListener);

        mUpdateButton = (Button) rootView.findViewById(R.id.buttonUpdate);
        mUpdateButton.setOnClickListener(mUpdateClickListener);

        mUpdateKodiButton = (Button) rootView.findViewById(R.id.buttonUpdateKodi);
        mUpdateKodiButton.setOnClickListener(mUpdateKodiClickListener);

        // Set updater
        mFireStarterUpdater = new FireStarterUpdater();
        mFireStarterUpdater.setOnCheckForUpdateFinishedListener(mOnCheckForUpdateFinishedListener);
        mFireStarterUpdater.setOnUpdateProgressListener(mOnUpdateProgressListener);

        // Set Kodi updater
        mKodiUpdater = new KodiUpdater();
        mKodiUpdater.setOnCheckForUpdateFinishedListener(mOnCheckForKodiUpdateFinishedListener);
        mKodiUpdater.setOnUpdateProgressListener(mOnUpdateKodiProgressListener);

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