package de.belu.firestarter.gui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.belu.firestarter.R;

/**
 * Created by luki on 27.05.15.
 */
public class LeftBarItemsListAdapter extends BaseAdapter
{
    /**
     * A dummy item representing a piece of description.
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

    /**
     * An array of sample (dummy) items.
     */
    public static List<FragmentListItem> ITEMS = new ArrayList<FragmentListItem>();

    static
    {
        // Add 3 sample items.
        addItem(new FragmentListItem("All Apps", AppActivity.class.getName()));
        addItem(new FragmentListItem("Infos", InfosActivity.class.getName()));
        addItem(new FragmentListItem("Settings", PreferenceActivity.class.getName()));
    }

    private static void addItem(FragmentListItem item)
    {
        ITEMS.add(item);
    }

    private Context mContext;

    /** Create new adapter */
    public LeftBarItemsListAdapter(Context context)
    {
        mContext = context;
    }

    /**
     * @return Count of installed apps
     */
    public int getCount()
    {
        return ITEMS.size();
    }

    /**
     * @param position Position of item to be returned
     * @return Item on position
     */
    public Object getItem(int position)
    {
        return ITEMS.get(position);
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
            itemView = inflater.inflate(R.layout.fragmentlistitemlayout, parent, false);

        } else
        {
            itemView = (View) convertView;
        }

        // set value into textview
        TextView textView = (TextView) itemView.findViewById(R.id.textLabel);
        textView.setText(ITEMS.get(position).description);

        return itemView;
    }
}
