package de.belu.appstarter.tests;

import android.test.InstrumentationTestCase;

import de.belu.appstarter.tools.KodiUpdater;

/**
 * Created by attila.szasz on 23-Oct-15.
 */
public class KodiTests extends InstrumentationTestCase {
    public void testCheckForUpdate() throws Exception {
        KodiUpdater mKodiUpdater = new KodiUpdater(this.getInstrumentation().getContext());
        mKodiUpdater.checkForUpdate(true);
        assertNotNull("Latest version should not be null", mKodiUpdater.getLatestVersion());
    }
}
