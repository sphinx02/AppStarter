package de.belu.firestarter.gui;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import de.belu.firestarter.R;
import de.belu.firestarter.tools.AppInfo;
import de.belu.firestarter.tools.Tools;

/**
 * Launcher main (shows the user apps)
 */
public class AppActivity extends Fragment
{
    /**
     * Default launcher package
     */
    private String mDefaultLauncherPackage;

    // Moving app-info (if null, no app is moving
    private final AppInfo[] mMovingApp = {null};

    /**
     * Holds the gridview resource
     */
    private GridView mGridView;

    /**
     * Indicated if app has been in pause but was not destroyed
     */
    private Boolean mHasBeenInOnPauseButNotInDestroy = false;

    /** Mandatory for fragment initation */
    public AppActivity(){ }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.appactivity, container, false);

        // Reset pause flag
        mHasBeenInOnPauseButNotInDestroy = false;

        // Get default-launcher package
        mDefaultLauncherPackage = Tools.getLauncherPackageName(getActivity());

        mGridView = (GridView) rootView.findViewById(R.id.gridview);
        final GridView gridview = mGridView;
        gridview.setAdapter(new InstalledAppsAdapter(getActivity()));

//        // Focus first item
//        gridview.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
//        {
//            @Override
//            public void onGlobalLayout()
//            {
//                try
//                {
//                    gridview.requestFocusFromTouch();
//                    gridview.setSelection(0);
//                    gridview.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//                }
//                catch (Exception e)
//                {
//                    StringWriter errors = new StringWriter();
//                    e.printStackTrace(new PrintWriter(errors));
//                    String errorReason = errors.toString();
//                    Log.d(MainActivity.class.getName(), "Failed to focus first app: \n" + errorReason);
//                }
//            }
//        });

        // Set click listener
        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id)
            {
                if (mMovingApp[0] != null)
                {
                    // Stop moving
                    mMovingApp[0] = null;
                    gridview.setDrawSelectorOnTop(false);
                    gridview.invalidate();

                    // Save current order
                    InstalledAppsAdapter actAdapter = (InstalledAppsAdapter) parent.getAdapter();
                    actAdapter.storeNewPackageOrder();
                } else
                {
                    // Get packagename of the app to be started
                    String packageName = ((AppInfo) parent.getAdapter().getItem(position)).packageName;

                    // Now start app
                    Tools.startAppByPackageName(getActivity(), packageName);
                }
            }
        });

        // Set long click listener
        gridview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
            {
                // Get packagename of the app to be started
                mMovingApp[0] = (AppInfo) parent.getAdapter().getItem(position);
                gridview.setDrawSelectorOnTop(true);
                gridview.invalidate();

                Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.MoveAppAndClickToDrop), Toast.LENGTH_SHORT).show();

                return true;
            }
        });

        // Set select listener
        gridview.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if (mMovingApp[0] != null)
                {
                    InstalledAppsAdapter actAdapter = (InstalledAppsAdapter) parent.getAdapter();
                    actAdapter.moveAppToPosition(mMovingApp[0], position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                // Do nothing here
            }
        });

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
        mHasBeenInOnPauseButNotInDestroy = true;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (mHasBeenInOnPauseButNotInDestroy)
        {
            // Reload app order
            Log.d(MainActivity.class.getName(), "Reloading Order.");
            InstalledAppsAdapter actAdapter = (InstalledAppsAdapter) mGridView.getAdapter();
            actAdapter.loadInstalledApps();
            actAdapter.notifyDataSetChanged();
        }
    }

    //@Override
    public void onBackPressed()
    {
        if(mMovingApp[0] != null)
        {
            // Stop moving without saving the order
            mMovingApp[0] = null;
            mGridView.setDrawSelectorOnTop(false);
            mGridView.invalidate();

            // Restore old order
            InstalledAppsAdapter actAdapter = (InstalledAppsAdapter)mGridView.getAdapter();
            actAdapter.loadInstalledApps();
            actAdapter.notifyDataSetChanged();
        }
    }
}