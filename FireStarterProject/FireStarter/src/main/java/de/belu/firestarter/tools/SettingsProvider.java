package de.belu.firestarter.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings Provider to store all Settins of the app
 */
public class SettingsProvider
{
    /** Instance of app-shared preferences */
    SharedPreferences mPreferences;

    /** Indicator for package-order */
    final static String PACKAGEORDER = "packageorder_";

    /** Package-Order List */
    List<String> mPackageOrder;

    public SettingsProvider(Context context)
    {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setPackageOrder(List<String> packageOrder)
    {
        mPackageOrder = packageOrder;
        storeValues();
    }

    public List<String> getPackageOrder()
    {
        readValues();
        return mPackageOrder;
    }

    private void readValues()
    {
        // PackageList
        List<String> packageList = new ArrayList<String>();
        Integer size = mPreferences.getInt(PACKAGEORDER + "size", 0);
        for(Integer i=0; i<size; i++)
        {
            String actKey = PACKAGEORDER + i.toString();
            packageList.add(mPreferences.getString(actKey, null));
        }
        mPackageOrder = packageList;
    }

    private void storeValues()
    {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.clear();

        // PackageList
        editor.putInt(PACKAGEORDER + "size", mPackageOrder.size());
        for(Integer i = 0; i < mPackageOrder.size(); i++)
        {
            String actKey = PACKAGEORDER + i.toString();
            editor.remove(actKey);
            editor.putString(actKey, mPackageOrder.get(i));
        }


        editor.commit();
    }
}
