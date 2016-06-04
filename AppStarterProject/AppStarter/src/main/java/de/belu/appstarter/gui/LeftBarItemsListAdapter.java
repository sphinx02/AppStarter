package de.belu.appstarter.gui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.belu.appstarter.R;

/**
 * Adpater for items in the left selection bar
 */
public class LeftBarItemsListAdapter extends BaseAdapter
{
    /**
     * A left bar fragment item
     */
    public static class FragmentListItem
    {
        public String description;
        public String className;


        public FragmentListItem(String description, String className)
        {
            this.description = description;
            this.className = className;
        }

        @Override
        public String toString()
        {
            return description;
        }
    }

    /** Left bar item list */
    private static List<FragmentListItem> mItems = null;

    /** Current context */
    private Context mContext;

    /** Create new adapter */
    public LeftBarItemsListAdapter(Context context)
    {
        mContext = context;
        if(mItems == null)
        {
            mItems = new ArrayList<FragmentListItem>();
            mItems.add(new FragmentListItem(mContext.getResources().getString(R.string.leftbar_allapps), AppActivity.class.getName()));
            mItems.add(new FragmentListItem(mContext.getResources().getString(R.string.leftbar_infos), InfosPrefActivity.class.getName()));
            mItems.add(new FragmentListItem(mContext.getResources().getString(R.string.leftbar_updates), UpdaterActivity.class.getName()));
            mItems.add(new FragmentListItem(mContext.getResources().getString(R.string.leftbar_settings), PreferenceActivity.class.getName()));
        }
    }

    /**
     * @return Count of installed apps
     */
    public int getCount()
    {
        return mItems.size();
    }

    /**
     * @param position Position of item to be returned
     * @return Item on position
     */
    public FragmentListItem getItem(int position)
    {
        return mItems.get(position);
    }

    /**
     * Currently not used..
     */
    public long getItemId(int position)
    {
        return position;
    }

    /**
     * @return View of the given position
     */
    public View getView(int position, View convertView, ViewGroup parent)
    {
        // Inflate layout
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemView;

        if (convertView == null)
        {
            // get layout from mobile.xml
            itemView = inflater.inflate(R.layout.leftbarlistitemlayout, parent, false);

        } else
        {
            itemView = (View) convertView;
        }

        // set value into textview
        TextView textView = (TextView) itemView.findViewById(R.id.textLabel);
        textView.setText(mItems.get(position).description);

        return itemView;
    }
}
