package de.belu.appstarter.gui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import de.belu.appstarter.R;
import de.belu.appstarter.tools.SettingsProvider;


public class MainActivity extends Activity
{
    private LinearLayout mMainLayout;
    private LinearLayout mSemiTransparentLayout;
    private ListView mListView;
    private Fragment mLastSetFragment;
    private SettingsProvider mSettings;
    private LinearLayout mLeftBar;

    private TextView mTextViewClock;
    private TextView mTextViewDate;

    private Timer mTimer = null;

    private boolean mOnResumeDirectlyAfterOnCreate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // Get settings provider
        mSettings = SettingsProvider.getInstance(this);

        // Check language
        Log.d(MainActivity.class.getName(), "Set locale in onCreate");
        setLocale();

        // Set flag indicating we are in oncreate
        mOnResumeDirectlyAfterOnCreate = true;

        // Now go on
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainactivity);

        // Get base linear layout
        mMainLayout = (LinearLayout)findViewById(R.id.linearLayoutMain);

        // Check if background image have to be set
        WallpaperSelectDialog selectDialog = new WallpaperSelectDialog(this);
        selectDialog.setWallpaper(false);

        // Get linear layout for semi-transparent background
        mSemiTransparentLayout = (LinearLayout) findViewById(R.id.linearLayoutSemiTransparentBackground);

        // Get clock and date
        mTextViewClock = (TextView)findViewById(R.id.textViewClock);
        mTextViewDate = (TextView)findViewById(R.id.textViewDate);

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

        // Check if left bar shall be hided
        mLeftBar = (LinearLayout) findViewById(R.id.leftbar);
    }

    @Override
    public void onResume()
    {
        // If not onResume directly after onCreate reset locale
        if(!mOnResumeDirectlyAfterOnCreate)
        {
            Log.d(MainActivity.class.getName(), "Set locale again in onResume");
            setLocale();
        }

        // Set date and time and start timer
        setDateAndTime();
        startTimer();

        super.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        // Stop update timer
        stopTimer();

        // Set back onResume directly after onCreate
        mOnResumeDirectlyAfterOnCreate = false;
    }

    private void setActiveFragment(Fragment fragment)
    {
        try
        {
            // Check fragment type
            if(fragment instanceof AppActivity)
            {
                // In app overview disable semi-transparent background
                mSemiTransparentLayout.setBackgroundResource(android.R.color.transparent);

                // Check if animation have to be changed
                if(mSettings.getHideLeftBarInAppOverview())
                {
                    mListView.setOnFocusChangeListener(new View.OnFocusChangeListener()
                            //mLeftBar.setOnFocusChangeListener(new View.OnFocusChangeListener()
                    {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus)
                        {
                            Log.d(MainActivity.class.getName(), "LeftBar focus changed to: hasFocus = " + hasFocus);

                            if (mLastSetFragment instanceof AppActivity)
                            {
                                Integer newWidth = 0;
                                if (hasFocus)
                                {
                                    newWidth = Math.round(getResources().getDimension(R.dimen.leftbarwidth));
                                }
                                Log.d(MainActivity.class.getName(), "LeftBar new width = " + newWidth);

                                ResizeWidthAnimation anim = new ResizeWidthAnimation(mLeftBar, newWidth);
                                anim.setDuration(300);
                                mLeftBar.startAnimation(anim);
                            }
                        }
                    });
                }
                else
                {
                    mListView.setOnFocusChangeListener(null);
                }
            }
            else
            {
                // In all other fragments enable semi-transparent background if setting is set
                if(mSettings.getShowBackgroundForAppNames())
                {
                    mSemiTransparentLayout.setBackgroundResource(R.color.semitransparentbackground_medium);
                }
            }

            // Set fragment as lastSetFragment
            mLastSetFragment = fragment;

            // Initiate fragment change
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
            Log.d(MainActivity.class.getName(), "Set Active Fragment: Exception: \n" + errorReason);
        }
    }

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

            // Create fragment
            Fragment fragment = (Fragment)Class.forName(((LeftBarItemsListAdapter)parent.getAdapter()).getItem(position).className).getConstructor().newInstance();

            // Set fragment
            setActiveFragment(fragment);
        }
        catch (Exception e)
        {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String errorReason = errors.toString();
            Log.d(MainActivity.class.getName(), "HandleLeftBarItemSelection: Exception: \n" + errorReason);
        }
    }

    private void setLocale()
    {
        // Check language
        String lang = mSettings.getLanguage();
        if(lang != null && !lang.equals("") && SettingsProvider.LANG.containsKey(lang))
        {
            try
            {
                Locale newLocale;

                // If lang is <= 3 chars, it is a language code
                if(lang.length() <= 3)
                {
                    newLocale = new Locale(lang);
                }
                else
                {
                    newLocale = (Locale) Locale.class.getField(lang).get(Locale.getDefault());
                }

                Locale.setDefault(newLocale);

                Configuration config = new Configuration(getResources().getConfiguration());
                config.locale = newLocale;
                getResources().updateConfiguration(config, getResources().getDisplayMetrics());
            }
            catch (Exception e)
            {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                String errorReason = errors.toString();
                Log.d(MainActivity.class.getName(), "Failed to load custom language setting: \n" + errorReason);
            }
        }
    }

    /**
     * Start the timer to update clock
     */
    private void startTimer()
    {
        mTimer = new Timer ();
        TimerTask timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                setDateAndTime();
            }
        };

        // Schedule task every full minute
        Integer everyXminute = 1;
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Integer toBeAdded = everyXminute - (calendar.get(Calendar.MINUTE) % everyXminute);
        if(toBeAdded == 0)
        {
            toBeAdded = everyXminute;
        }
        calendar.add(Calendar.MINUTE, toBeAdded);

        mTimer.schedule(timerTask, calendar.getTime(), 1000*60*everyXminute);
        Log.d(MainActivity.class.getName(), "Update Time started");
    }

    /**
     * Stops the timer to update clock
     */
    private void stopTimer()
    {
        if(mTimer != null)
        {
            mTimer.cancel();
            mTimer = null;
        }
        Log.d(MainActivity.class.getName(), "Update Time stopped");
    }

    /**
     * Sets the current time and date
     */
    private void setDateAndTime()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                // Get current date time
                Date actDateTime = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
                Log.d(MainActivity.class.getName(), "Update Time to " + sdf.format(actDateTime));

                // Set clock
//                DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, getResources().getConfiguration().locale.getDefault());
//                if(android.text.format.DateFormat.is24HourFormat(MainActivity.this))
//                {
//                }
//                mTextViewClock.setText(timeFormat.format(actDateTime));
                mTextViewClock.setText(DateUtils.formatDateTime(MainActivity.this, actDateTime.getTime(), DateUtils.FORMAT_SHOW_TIME));

                // Set date
                DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL, getResources().getConfiguration().locale);
                mTextViewDate.setText(dateFormat.format(actDateTime));
            }
        });
    }

    /**
     * @param drawable Set this drawable as background image
     */
    public void setBackgroundImage(Drawable drawable)
    {
        mMainLayout.setBackground(drawable);
    }

    public Integer getBackgroundWidth()
    {
        return mMainLayout.getWidth();
    }

    public Integer getBackgroundHeight()
    {
        return mMainLayout.getHeight();
    }

    /** Trigger update */
    public void triggerUpdate()
    {
        try
        {
            UpdaterActivity fragment = (UpdaterActivity)Class.forName(UpdaterActivity.class.getName()).getConstructor().newInstance();
            fragment.triggerUpdateOnStartup();
            setActiveFragment(fragment);
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
        boolean isHandled = false;

        // Check if there is a receiver fragment
        if(mLastSetFragment != null && mLastSetFragment instanceof CustomFragment)
        {
            CustomFragment actFragment = (CustomFragment)mLastSetFragment;
            isHandled = ((CustomFragment) mLastSetFragment).onBackPressed();
        }

        // Check if back-pressed is already handled
        if(!isHandled)
        {
            // If the fragment does not handle the back-button and AppStarter is not
            // on the main-app-view, open main-app-view
            if(!(mLastSetFragment instanceof AppActivity))
            {
                mListView.setSelection(0);
            }
        }

        // Prevent default by not calling super class..
    }
}
