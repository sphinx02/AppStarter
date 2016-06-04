package de.belu.appstarter.gui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.belu.appstarter.R;

/**
 * Info Overlay for additional information
 */
public class InfoOverlayDialog extends DialogFragment
{
    /**
     * Create a new Info-Overlay dialog
     * @param title Title of the info dialog
     * @param summary Text of the info dialog
     * @return the dialog
     */
    public static InfoOverlayDialog newInstance(String title, String summary)
    {
        // Create dialog and set custom style
        InfoOverlayDialog dialog = new InfoOverlayDialog();
        dialog.setStyle(DialogFragment.STYLE_NO_FRAME, R.style.TransparentDialog);

        // Set title and summary
        dialog.setTitleAndSummary(title, summary);

        return dialog;
    }

    /** Title of the info dialog */
    private String mTitle;

    /** Text of the info dialog */
    private String mSummary;

    /**
     * Default constructor required for DialogFragment
     */
    public InfoOverlayDialog()
    {
    }

    public void setTitleAndSummary(String title, String summary)
    {
        // Set title and summary
        mTitle = title;
        mSummary = summary;
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
        View view = inflater.inflate(R.layout.infooverlaydialog, container);

        // Set text, icons and click-handler
        TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(mTitle);

        TextView summary = (TextView) view.findViewById(R.id.summary);
        summary.setText(mSummary);

        return view;
    }
}