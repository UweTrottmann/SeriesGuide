
package com.battlelancer.seriesguide.beta.test;

import static org.fest.assertions.api.Assertions.assertThat;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;

import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.SimpleCrypto;
import com.battlelancer.seriesguide.util.Utils;

import java.io.File;

public class SimpleCryptoTest extends AndroidTestCase {

    private static final String CLEARTEXT = "This1s@Passw0rd";
    private static final String DATACORE = "datacore";

    public SimpleCryptoTest() {
        super();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Delete seed
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putString(SeriesGuidePreferences.KEY_SECURE, null).commit();

        // Delete keystore file
        getContext().deleteFile(DATACORE);
    }

    @Override
    public void testAndroidTestCaseSetupProperly() {
        super.testAndroidTestCaseSetupProperly();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        assertThat(prefs.getString(SeriesGuidePreferences.KEY_SECURE, null)).isNull();

        File filesDir = getContext().getFilesDir();
        File[] listFiles = filesDir.listFiles();
        for (File file : listFiles) {
            if (DATACORE.equals(file.getName())) {
                fail("Keystore file still exists.");
            }
        }
    }

    public void testClearTextEncryption() {
        String encrypted = SimpleCrypto.encrypt(CLEARTEXT, getContext());

        String decrypted = SimpleCrypto.decrypt(encrypted, getContext());

        assertThat(decrypted).isEqualTo(CLEARTEXT);
    }

    public void testHashEncryption() {
        String hash = Utils.toSHA1(CLEARTEXT.getBytes());

        String encrypted = SimpleCrypto.encrypt(hash, getContext());

        String decrypted = SimpleCrypto.decrypt(encrypted, getContext());

        assertThat(decrypted).isEqualTo(hash);
    }
}
