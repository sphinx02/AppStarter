package de.belu.appstarter.gui;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;

import de.belu.appstarter.R;
import de.belu.appstarter.tools.AppInfo;
import de.belu.appstarter.tools.AppStarter;
import de.belu.appstarter.tools.AppStarterUpdater;
import de.belu.appstarter.tools.SettingsProvider;
import de.belu.appstarter.tools.Tools;

import static de.belu.appstarter.gui.AppSettingsOverlayDialog.*;

/**
 * Launcher main (shows the user apps)
 */
public class AppActivity extends CustomFragment
{
    /**
     * Default launcher package
     */
    private String mDefaultLauncherPackage;

    // Moving app-info (if null, no app is moving
    private final AppInfo[] mMovingApp = {null};

    /**
     * Instance of settingsprovider
     */
    SettingsProvider mSettings = SettingsProvider.getInstance(this.getActivity());

    /** Latest AppStarter version found */
    public static String LATEST_APP_VERSION = null;

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
        mDefaultLauncherPackage = AppStarter.getLauncherPackageName(getActivity());

        // Check for custom app-icon size
        mGridView = (GridView) rootView.findViewById(R.id.gridview);
        Integer appIconSize = mSettings.getAppIconSize();
        if(appIconSize > 0)
        {
            // Set size of items
            mGridView.setColumnWidth(Tools.getPixelFromDip(getActivity(), appIconSize));
        }
        mGridView.setAdapter(new InstalledAppsAdapter(getActivity()));

        // Focus first item
        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                try
                {
                    // Remove listener
                    mGridView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // Check if first icon have to be selected
                    if(mSettings.getAutoSelectFirstIcon() && mGridView.getChildCount() > 0)
                    {
                        mGridView.requestFocusFromTouch();
                        mGridView.setSelection(0);
                    }

                    // Check for new update
                    AppStarterUpdater appStarterUpdater = new AppStarterUpdater();
                    if(!mSettings.getHaveUpdateSeen() && appStarterUpdater.isVersionNewer(Tools.getCurrentAppVersion(getActivity()), LATEST_APP_VERSION))
                    {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                        builder.setTitle("AppStarter " + LATEST_APP_VERSION);
                        builder.setMessage("There is a new version of AppStarter, do you want to update?");

                        builder.setPositiveButton("YES", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();

                                // Trigger update
                                if(getActivity() instanceof MainActivity)
                                {
                                    ((MainActivity) getActivity()).triggerUpdate();
                                }
                            }

                        });

                        builder.setNegativeButton("NO", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                // Set have seen
                                mSettings.setHaveUpdateSeen(true);
                                dialog.dismiss();
                            }
                        });

                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                }
                catch (Exception e)
                {
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));
                    String errorReason = errors.toString();
                    Log.d(MainActivity.class.getName(), "Failed to focus first app: \n" + errorReason);
                }
            }
        });

        // Set click listener
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id)
            {
                if (mMovingApp[0] != null)
                {
                    // Stop moving
                    mMovingApp[0] = null;
                    mGridView.setDrawSelectorOnTop(false);
                    mGridView.invalidate();

                    // Save current order
                    InstalledAppsAdapter actAdapter = (InstalledAppsAdapter) parent.getAdapter();
                    actAdapter.storeNewPackageOrder();
                } else
                {
                    // Get packagename of the app to be started
                    String packageName = ((AppInfo) parent.getAdapter().getItem(position)).packageName;

                    // Now start app
                    AppStarter.startAppByPackageName(getActivity(), packageName, false, false, false);
                }
            }
        });

        // Set long click listener
        mGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
            {
                // Get packagename of the app to be started
                mMovingApp[0] = (AppInfo) parent.getAdapter().getItem(position);
                mGridView.setDrawSelectorOnTop(true);
                mGridView.invalidate();

                Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.MoveAppAndClickToDrop), Toast.LENGTH_SHORT).show();

                return true;
            }
        });

        // Set select listener
        mGridView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
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

    @Override
    public boolean onBackPressed()
    {
        boolean retVal = false;

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

            retVal = true;
        }

        return retVal;
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e)
    {
        if(mMovingApp[0] == null && keycode == KeyEvent.KEYCODE_MENU)
        {
            showAppSettingsDialogForCurrentApp();
            return true;
        }

        return false;
    }

    private void showAppSettingsDialogForCurrentApp()
    {
        try
        {
            // Check selected app
            if(mGridView.hasFocus())
            {
                final AppInfo appInfo = (AppInfo) ((InstalledAppsAdapter) mGridView.getAdapter()).getItem(mGridView.getSelectedItemPosition());
                final AppSettingsOverlayDialog appSettingsDialog = newInstance(appInfo);

                appSettingsDialog.setOnActionClickedHandler(new OnActionClickedHandler()
                {
                    @Override
                    public void onActionClicked(ActionEnum action)
                    {
                        Log.d("AppSettingsAction", "Clicked action: " + action.toString());

                        // Close dialog and evaluate click event:
                        appSettingsDialog.dismiss();
                        switch (action)
                        {
                            case SORT:
                                // Get packagename of the app to be started
                                mMovingApp[0] = appInfo;
                                mGridView.setDrawSelectorOnTop(true);
                                mGridView.invalidate();

                                Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.MoveAppAndClickToDropWhenStartedByMenu), Toast.LENGTH_SHORT).show();
                                break;
                            case SETTINGS:
                                AppStarter.startSettingsViewByPackageName(AppActivity.this.getActivity(), appInfo.packageName);
                                break;
                            default:
                                // Do nothing by default
                        }
                    }
                });

                FragmentManager fm = getActivity().getFragmentManager();
                appSettingsDialog.show(fm, "");
            }
        }
        catch (Exception e)
        {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String errorReason = errors.toString();
            Log.d(MainActivity.class.getName(), "Failed to load app settings: \n" + errorReason);
        }
    }
}