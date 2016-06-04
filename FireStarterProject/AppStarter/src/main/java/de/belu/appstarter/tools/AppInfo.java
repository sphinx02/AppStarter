package de.belu.appstarter.tools;

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

    /**
     * @param app App to be hold
     */
    public AppInfo(Context context, ApplicationInfo app)
    {
        super(app);
        mContext = context;
    }

    /**
     * @return Name to be displayed
     */
    public String getDisplayName()
    {
        String retVal = this.loadLabel(mContext.getPackageManager()).toString();
        if(retVal == null || retVal.equals(""))
        {
            retVal = packageName;
        }
        return retVal;
    }

    /**
     * @return Icon to be displayed
     */
    public Drawable getDisplayIcon()
    {
        return this.loadIcon(mContext.getPackageManager());
    }
}
