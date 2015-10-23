package de.belu.firestarter.tests;

import android.test.InstrumentationTestCase;

import de.belu.firestarter.tools.UpdaterKodi;

/**
 * Created by attila.szasz on 23-Oct-15.
 */
public class KodiTests extends InstrumentationTestCase {
    public void testCheckForUpdate() throws Exception {
        UpdaterKodi mUpdaterKodi = new UpdaterKodi();
        mUpdaterKodi.checkForUpdate(true);
        assertNotNull("Latest version should not be null", mUpdaterKodi.LATEST_VERSION);
    }
}
