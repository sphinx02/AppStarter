package de.belu.appstarter.gui;

import android.app.Activity;
import android.app.ProgressDialog;

import de.belu.appstarter.R;
import de.belu.appstarter.tools.AppStarterUpdater;
import de.belu.appstarter.tools.Updater;

/**
 * Handles the update-dialogs in case of an update
 */
public class UpdaterDialogHandler
{
    /** Updater of the dialog handler */
    private Updater mUpdater;

    /** Context of the current dialog handler */
    private Activity mActivity;

    /** Check for update progress */
    ProgressDialog mCheckForUpdateProgress = null;

    /** Update progress */
    ProgressDialog mUpdateProgress = null;

    /** Listener for checkforupdate finished event */
    Updater.OnCheckForUpdateFinishedListener mCheckForUpdateFinishedListener = null;

    public UpdaterDialogHandler(Activity activity, Updater updater)
    {
        mActivity = activity;
        mUpdater = updater;

        mUpdater.setOnCheckForUpdateFinishedListener(mOnCheckForUpdateFinishedListener);
        mUpdater.setOnUpdateProgressListener(mOnUpdateProgressListener);
    }

    public void setCheckForUpdateFinishedListener(Updater.OnCheckForUpdateFinishedListener listener)
    {
        mCheckForUpdateFinishedListener = listener;
    }

    public void checkForUpdate()
    {
        mCheckForUpdateProgress = ProgressDialog.show(mActivity, mActivity.getResources().getString(R.string.update_checkfortitle), mActivity.getResources().getString(R.string.update_checkfordesc), true);
        mUpdater.checkForUpdate();
    }

    public void performUpdate()
    {
        mUpdateProgress = new ProgressDialog(mActivity);
        mUpdateProgress.setMessage(mActivity.getResources().getString(R.string.update_checkformessage));
        mUpdateProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mUpdateProgress.setCancelable(false);
        mUpdateProgress.setProgress(0);
        mUpdateProgress.show();

        mUpdater.update(mActivity);
    }

    /** Handle check for update */
    Updater.OnCheckForUpdateFinishedListener mOnCheckForUpdateFinishedListener = new AppStarterUpdater.OnCheckForUpdateFinishedListener()
    {
        @Override
        public void onCheckForUpdateFinished(final String message)
        {
            mActivity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {

                    if (mCheckForUpdateProgress != null)
                    {
                        mCheckForUpdateProgress.dismiss();
                        mCheckForUpdateProgress = null;
                    }
                    mCheckForUpdateFinishedListener.onCheckForUpdateFinished(message);
                }
            });
        }
    };

    /** Handle update progress */
    Updater.OnUpdateProgressListener mOnUpdateProgressListener = new AppStarterUpdater.OnUpdateProgressListener()
    {
        @Override
        public void onUpdateProgress(final Boolean isError,final Integer percent, final String message)
        {
            mActivity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if (isError)
                    {
                        mUpdateProgress.setProgress(100);
                        mUpdateProgress.setMessage(message);
                        mUpdateProgress.setCancelable(true);
                    } else
                    {
                        mUpdateProgress.setProgress(percent);
                        mUpdateProgress.setMessage(message);
                        if (percent >= 100)
                        {
                            mUpdateProgress.setCancelable(true);
                            mUpdateProgress.dismiss();
                        }
                    }
                }
            });
        }
    };

}
