package de.belu.firestarter.gui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import de.belu.firestarter.R;
import de.belu.firestarter.observer.ForeGroundService;
import de.belu.firestarter.tools.SettingsProvider;


public class MainActivity extends Activity
{
    private ListView mListView;
    private Fragment mLastSetFragment;
    private SettingsProvider mSettings;

    /**
     * Handles selection or click of the left-bar items..
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    private void handleLeftBarItemSelection(AdapterView<?> parent, View view, int position, long id)
    {
        // Get instance of selected item and set as current fragment
        try
        {
            Log.d(MainActivity.class.getName(), "HandleLeftBarItemSelection: selected position " + position);
            Fragment fragment = (Fragment)Class.forName(((LeftBarItemsListAdapter)parent.getAdapter()).getItem(position).className).getConstructor().newInstance();
            mLastSetFragment = fragment;

            FragmentManager fm = getFragmentManager();
            FragmentTransaction fragmentTransaction = fm.beginTransaction();
            fragmentTransaction.replace(R.id.item_detail_container, fragment);
            fragmentTransaction.commit();
        }
        catch (Exception e)
        {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String errorReason = errors.toString();
            Log.d(MainActivity.class.getName(), "HandleLeftBarItemSelection: Exception: \n" + errorReason);
        }
    }

    private void setLocale(Locale locale)
    {
        Configuration config = new Configuration(getResources().getConfiguration());
        config.locale = locale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Get settings provider
        mSettings = SettingsProvider.getInstance(this);

        // Check language
        String lang = mSettings.getLanguage();
        if(lang != null && !lang.equals("") && SettingsProvider.LANG.containsKey(lang))
        {
            try
            {
                setLocale((Locale) Locale.class.getField(lang).get(Locale.getDefault()));
            }
            catch (Exception e)
            {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                String errorReason = errors.toString();
                Log.d(MainActivity.class.getName(), "Failed to load custom language setting: \n" + errorReason);
            }
        }

        // Now go on
        setContentView(R.layout.mainactivity);

        // Get settings provider
        SettingsProvider settingsProvider = SettingsProvider.getInstance(this);
        if(settingsProvider.getBackgroundObserverEnabled())
        {
            // Start foreground service
            Intent startIntent = new Intent(this, ForeGroundService.class);
            startIntent.setAction(ForeGroundService.FOREGROUNDSERVICE_START);
            startService(startIntent);
        }

        // Get ListView
        mListView = (ListView)findViewById(R.id.listView);

        // Handle item click listener
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Log.d(MainActivity.class.getName(), "OnItemClickListener: clicked position " + position);
                handleLeftBarItemSelection(parent, view, position, id);
            }
        });

        // Handle item selected changes
        mListView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                Log.d(MainActivity.class.getName(), "OnItemSelectedListener: selected position " + position);
                handleLeftBarItemSelection(parent, view, position, id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });

        // Set adapter
        LeftBarItemsListAdapter actAdapter = new LeftBarItemsListAdapter(this);
        mListView.setAdapter(actAdapter);

        // Focus first item
        mListView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                try
                {
                    // Remove listener
                    mListView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // Check if first icon have to be selected
                    mListView.requestFocusFromTouch();
                    mListView.setSelection(0);
                }
                catch (Exception e)
                {
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));
                    String errorReason = errors.toString();
                    Log.d(MainActivity.class.getName(), "Failed to focus first left bar list item: \n" + errorReason);
                }
            }
        });
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

    /** Trigger update */
    public void triggerUpdate()
    {
        try
        {
            UpdateActivity fragment = (UpdateActivity)Class.forName(UpdateActivity.class.getName()).getConstructor().newInstance();
            fragment.triggerUpdateOnStartup();
            mLastSetFragment = fragment;

            FragmentManager fm = getFragmentManager();
            FragmentTransaction fragmentTransaction = fm.beginTransaction();
            fragmentTransaction.replace(R.id.item_detail_container, fragment);
            fragmentTransaction.commit();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e)
    {
        // Check if there is a receiver fragment
        if(mLastSetFragment != null && mLastSetFragment instanceof CustomFragment)
        {
            CustomFragment actFragment = (CustomFragment)mLastSetFragment;
            Boolean retVal = ((CustomFragment) mLastSetFragment).onKeyDown(keycode, e);
            if(retVal)
            {
                return true;
            }
        }

        return super.onKeyDown(keycode, e);
    }

    @Override
    public void onBackPressed()
    {
        // Check if there is a receiver fragment
        if(mLastSetFragment != null && mLastSetFragment instanceof CustomFragment)
        {
            CustomFragment actFragment = (CustomFragment)mLastSetFragment;
            ((CustomFragment) mLastSetFragment).onBackPressed();
        }

        // Prevent default by not calling super class..
    }
}
