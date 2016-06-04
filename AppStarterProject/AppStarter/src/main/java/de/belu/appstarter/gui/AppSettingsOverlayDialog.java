package de.belu.appstarter.gui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.belu.appstarter.R;
import de.belu.appstarter.tools.AppInfo;

/**
 * App Settings Overlay for additional settings per app
 */
public class AppSettingsOverlayDialog extends DialogFragment
{
    public enum ActionEnum { NOTHING, SORT, SETTINGS }

    public static AppSettingsOverlayDialog newInstance(AppInfo appInfo)
    {
        // Create dialog and set custom style
        AppSettingsOverlayDialog dialog = new AppSettingsOverlayDialog();
        dialog.setStyle(DialogFragment.STYLE_NO_FRAME, R.style.TransparentDialog);

        // Add needed stuff
        dialog.setAppInfo(appInfo);

        return dialog;
    }

    /** AppInfo of the current App */
    private AppInfo mAppInfo;

    /** Action handler */
    private OnActionClickedHandler onActionClickedHandler;

    /**
     * Default constructor required for DialogFragment
     */
    public AppSettingsOverlayDialog()
    {
    }

    /**
     * Set the action click listener
     * @param listener
     */
    public void setOnActionClickedHandler(OnActionClickedHandler listener)
    {
        onActionClickedHandler = listener;
    }

    /**
     * Sets the current AppInfo
     * @param appInfo
     */
    public void setAppInfo(AppInfo appInfo)
    {
        mAppInfo = appInfo;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Create view
        View view = inflater.inflate(R.layout.appsettingsoverlaydialog, container);

        // Set text, icons and click-handler
        TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(mAppInfo.getDisplayName());

        ImageView appIcon = (ImageView) view.findViewById(R.id.appIcon);
        appIcon.setImageDrawable(mAppInfo.getDisplayIcon());

        LinearLayout appSort = (LinearLayout) view.findViewById(R.id.appSort);
        appSort.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                fireActionClicked(ActionEnum.SORT);
            }
        });

        LinearLayout appSettings = (LinearLayout) view.findViewById(R.id.appSettings);
        appSettings.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                fireActionClicked(ActionEnum.SETTINGS);
            }
        });

        return view;
    }

    private void fireActionClicked(ActionEnum action)
    {
        if(onActionClickedHandler != null)
        {
            onActionClickedHandler.onActionClicked(action);
        }
    }

    /**
     * Interface for a service error
     */
    public interface OnActionClickedHandler
    {
        public void onActionClicked(ActionEnum action);
    }
}