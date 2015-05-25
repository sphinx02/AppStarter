package de.belu.firestarter.gui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;

import de.belu.firestarter.R;
import de.belu.firestarter.observer.ForeGroundService;
import de.belu.firestarter.tools.AppInfo;
import de.belu.firestarter.tools.Tools;


public class MainActivity extends Activity
{
    /** Default launcher package */
    private String mDefaultLauncherPackage;

    // Moving app-info (if null, no app is moving
    private final AppInfo[] mMovingApp = {null};

    /** Holds the gridview resource */
    private GridView mGridView;

    /** Indicated if app has been in pause but was not destroyed */
    private Boolean mHasBeenInOnPauseButNotInDestroy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainactivity);

        // Reset amazon-home wanted flag:
        Tools.IsAmazonHomeScreenWanted = false;

        // Reset pause flag
        mHasBeenInOnPauseButNotInDestroy = false;

        // Get default-launcher package
        mDefaultLauncherPackage = Tools.getLauncherPackageName(this);

        // Start foreground service
        Intent startIntent = new Intent(this, ForeGroundService.class);
        startIntent.setAction(ForeGroundService.FOREGROUNDSERVICE_START);
        startService(startIntent);

        mGridView = (GridView) findViewById(R.id.gridview);
        final GridView gridview = mGridView;
        gridview.setAdapter(new InstalledAppsAdapter(this));

        // Focus first item
        gridview.getViewTreeObserver().addOnGlobalLayoutListener( new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                try
                {
                    gridview.requestFocusFromTouch();
                    gridview.setSelection(0);
                    gridview.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                catch(Exception e)
                {
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));
                    String errorReason = errors.toString();
                    Log.d(MainActivity.class.getName(), "Failed to focus first app: \n" + errorReason);
                }
            }
        });

        // Set click listener
        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id)
            {
                if(mMovingApp[0] != null)
                {
                    // Stop moving
                    mMovingApp[0] = null;
                    gridview.setDrawSelectorOnTop(false);
                    gridview.invalidate();

                    // Save current order
                    InstalledAppsAdapter actAdapter = (InstalledAppsAdapter)parent.getAdapter();
                    actAdapter.storeNewPackageOrder();
                }
                else
                {
                    // Get packagename of the app to be started
                    String packageName = ((AppInfo) parent.getAdapter().getItem(position)).packageName;

                    // Check if this is the default-launcher, then we have to disable background-observer
                    if (packageName.equals(mDefaultLauncherPackage))
                    {
                        Tools.IsAmazonHomeScreenWanted = true;
                    }

                    // Now start app
                    Tools.startAppByPackageName(MainActivity.this, packageName);
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
                mMovingApp[0] = (AppInfo)parent.getAdapter().getItem(position);
                gridview.setDrawSelectorOnTop(true);
                gridview.invalidate();

                Toast.makeText(MainActivity.this, MainActivity.this.getResources().getString(R.string.MoveAppAndClickToDrop), Toast.LENGTH_SHORT).show();

                return true;
            }
        });

        // Set select listener
        gridview.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if(mMovingApp[0] != null)
                {
                    InstalledAppsAdapter actAdapter = (InstalledAppsAdapter)parent.getAdapter();
                    actAdapter.moveAppToPosition(mMovingApp[0], position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                // Do nothing here
            }
        });
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
        if(mHasBeenInOnPauseButNotInDestroy)
        {
            // Reload app order
            Log.d(MainActivity.class.getName(), "Reloading Order.");
            InstalledAppsAdapter actAdapter = (InstalledAppsAdapter)mGridView.getAdapter();
            actAdapter.loadInstalledApps();
            actAdapter.notifyDataSetChanged();
        }
    }

    @Override
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
