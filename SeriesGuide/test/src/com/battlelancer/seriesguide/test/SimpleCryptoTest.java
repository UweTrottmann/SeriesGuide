
package com.battlelancer.seriesguide.test;

import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.SimpleCrypto;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;

public class SimpleCryptoTest extends AndroidTestCase {

    private static final String CLEARTEXT = "This1s@Passw0rd";

    private static final String KEY_SECURE = "com.battlelancer.seriesguide.secure";

    public SimpleCryptoTest() {
        super();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putString(KEY_SECURE, null).commit();
    }

    @Override
    public void testAndroidTestCaseSetupProperly() {
        super.testAndroidTestCaseSetupProperly();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        assertNull("Secret is not null", prefs.getString(KEY_SECURE, null));
    }

    public void testClearTextEncryption() {
        String encrypted = null;
        try {
            encrypted = SimpleCrypto.encrypt(CLEARTEXT, getContext());
        } catch (Exception e) {
            fail(e.getMessage());
        }

        String decrypted = null;
        try {
            decrypted = SimpleCrypto.decrypt(encrypted, getContext());
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(CLEARTEXT, decrypted);
    }

    public void testHashEncryption() {
        String hash = ShareUtils.toSHA1(CLEARTEXT.getBytes());

        String encrypted = null;
        try {
            encrypted = SimpleCrypto.encrypt(hash, getContext());
        } catch (Exception e) {
            fail(e.getMessage());
        }

        String decrypted = null;
        try {
            decrypted = SimpleCrypto.decrypt(encrypted, getContext());
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(hash, decrypted);
    }
}
