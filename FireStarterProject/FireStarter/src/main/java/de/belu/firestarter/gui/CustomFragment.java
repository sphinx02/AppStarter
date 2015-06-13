package de.belu.firestarter.gui;

import android.app.Fragment;
import android.view.KeyEvent;

/**
 * Fragement that has additional features
 */
public class CustomFragment extends Fragment
{
    /**
     * Custom on backpressed method
     */
    public void onBackPressed()
    {

    }

    public boolean onKeyDown(int keycode, KeyEvent e)
    {
        return false;
    }
}
