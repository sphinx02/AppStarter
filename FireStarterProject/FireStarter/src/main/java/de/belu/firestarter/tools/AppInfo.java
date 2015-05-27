package de.belu.firestarter.tools;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

/**
 * Container to hold app informations
 */
public class AppInfo extends ApplicationInfo
{
    /** The current context */
    Context mContext;

    /** The displayed name of the app */
    String mDisplayName;

    /** The displayed icon of the app */
    Drawable mDisplayIcon;

    /**
     * @param app App to be hold
     */
    public AppInfo(Context context, ApplicationInfo app)
    {
        super(app);
        mContext = context;
        mDisplayName = this.loadLabel(mContext.getPackageManager()).toString();
        mDisplayIcon = this.loadIcon(mContext.getPackageManager());
    }

    public void setDisplayName(String displayName) { mDisplayName = displayName; }
    public String getDisplayName()
    {
        String retVal = mDisplayName;
        if(retVal == null || retVal.equals(""))
        {
            retVal = packageName;
        }
        return retVal;
    }


    public void setDisplayIcon(Drawable icon) { mDisplayIcon = icon; }
    public Drawable getDisplayIcon() { return mDisplayIcon; }
}
