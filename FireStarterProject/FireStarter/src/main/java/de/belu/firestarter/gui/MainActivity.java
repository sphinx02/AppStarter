package de.belu.firestarter.gui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import de.belu.firestarter.R;
import de.belu.firestarter.observer.ForeGroundService;
import de.belu.firestarter.tools.SettingsProvider;


public class MainActivity extends Activity
{
    private final static Integer PADDINGNORMAL = 5;
    private final static Integer PADDINGSELECTED = 30;

    ListView mListView;
    Integer mPaddingNormal;
    Integer mPaddingSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
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

        // Calculate padding values
        float paddingNormalfloat = PADDINGNORMAL * getResources().getDisplayMetrics().density;
        mPaddingNormal = Math.round(paddingNormalfloat);

        float paddingSelectedfloat = PADDINGSELECTED * getResources().getDisplayMetrics().density;
        mPaddingSelected = Math.round(paddingSelectedfloat);

        // Get listview and set adapter
        mListView = (ListView)findViewById(R.id.listView);

        // Handle item selected changes
        mListView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                // Get instance of selected item and set as current fragment
                try
                {
                    Fragment fragment = (Fragment)Class.forName(LeftBarItemsListAdapter.ITEMS.get(position).className).getConstructor().newInstance();

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
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });
        mListView.setAdapter(new LeftBarItemsListAdapter(this));
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

    @Override
    public void onBackPressed()
    {
        // Prevent default..
    }
}
