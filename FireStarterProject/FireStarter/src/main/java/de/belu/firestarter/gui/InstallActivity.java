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
public class InstallActivity extends Fragment
{
    /** TextView of latest Kodi version */
    TextView mLatestVersionKodi;

    /** Kodi FireStarterUpdater service */
    KodiUpdater mKodiUpdater;

    /** Check for update progress */
    ProgressDialog mCheckForUpdateProgress = null;

    /** Update progress */
    ProgressDialog mUpdateProgress = null;

    /** Current Kodi version */
    String mCurrentAppVersionKodi;

    /** Update button to trigger updates */
    Button mUpdateKodiButton;

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
    KodiUpdater.OnCheckForUpdateFinishedListener mOnCheckForKodiUpdateFinishedListener = new KodiUpdater.OnCheckForUpdateFinishedListener()
    {
        @Override
        public void onCheckForUpdateFinished(final String message)
        {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mKodiUpdater.LATEST_VERSION != null) {
                        if (KodiUpdater.isVersionNewer(mCurrentAppVersionKodi, mKodiUpdater.LATEST_VERSION)) {
                            mLatestVersionKodi.setText(mKodiUpdater.LATEST_VERSION + " - " + getActivity().getResources().getString(R.string.update_foundnew));
                        } else {
                            mLatestVersionKodi.setText(mKodiUpdater.LATEST_VERSION + " - " + getActivity().getResources().getString(R.string.update_foundnotnew));
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
    public InstallActivity(){ }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.installactivity, container, false);

        // Set current version
        TextView kodiversion = (TextView) rootView.findViewById(R.id.currentVersionKodi);
        mCurrentAppVersionKodi = Tools.getCurrentAppVersion(getActivity(), "org.xbmc.kodi");
        kodiversion.setText(mCurrentAppVersionKodi);

        // Set Kodi button events
        Button checkUpdateKodiButton = (Button) rootView.findViewById(R.id.buttonCheckForUpdateKodi);
        checkUpdateKodiButton.setOnClickListener(mCheckKodiUpdateClickListener);

        mUpdateKodiButton = (Button) rootView.findViewById(R.id.buttonUpdateKodi);
        mUpdateKodiButton.setOnClickListener(mUpdateKodiClickListener);

        if (mCurrentAppVersionKodi == null || mCurrentAppVersionKodi.equals("unknown"))
        {
            mUpdateKodiButton.setText(getResources().getString(R.string.install));
        }

        // Set Kodi updater
        mKodiUpdater = new KodiUpdater();
        mKodiUpdater.setOnCheckForUpdateFinishedListener(mOnCheckForKodiUpdateFinishedListener);
        mKodiUpdater.setOnUpdateProgressListener(mOnUpdateKodiProgressListener);

        // Set latest Kodi version
        mLatestVersionKodi = (TextView) rootView.findViewById(R.id.latestVersionKodi);
        if(mKodiUpdater.LATEST_VERSION != null)
        {
            mLatestVersionKodi.setText(mKodiUpdater.LATEST_VERSION);
        }

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