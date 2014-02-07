/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.SecretKeyEntry;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import timber.log.Timber;

/**
 * Symmetrically encrypts and decrypts strings using a seeded key stored in a
 * key store on internal storage. Be aware that anyone gaining access to this
 * app's storage may be able to access the key store. This is just supposed to
 * add furhter layers of work before being able to access encrypted data.
 */
public class SimpleCrypto {

    private static final String KEY_ALIAS = "coreentry";

    private static final String DATACORE = "datacore";

    /**
     * Decrypts the given string and returns the clear text, or {@code null} if
     * decryption was unsuccessful.
     */
    public static String decrypt(String encrypted, Context context) {
        try {
            SecretKey key = getKey(context);
            byte[] enc = toByte(encrypted);
            byte[] result = decrypt(key, enc);
            return new String(result);
        } catch (GeneralSecurityException | IOException e) {
            Timber.e(e, "Decrypting failed");
        }
        return null;
    }

    private static SecretKey getKey(Context context) throws IOException,
            GeneralSecurityException {
        // ensure seed/password
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                .getApplicationContext());
        String seed = prefs.getString(SeriesGuidePreferences.KEY_SECURE, null);
        if (seed == null) {
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            byte[] seedBytes = sr.generateSeed(16);
            seed = toHex(seedBytes);
            prefs.edit().putString(SeriesGuidePreferences.KEY_SECURE, seed).commit();
        }

        final char[] keystorePassword = seed.toCharArray();

        // ensure key store
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream fis = null;
        try {
            // load existing key store
            fis = context.openFileInput(DATACORE);
            keystore.load(fis, keystorePassword);
        } catch (IOException e) {
            // create new key store
            keystore.load(null, keystorePassword);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignored) {
                }
            }
        }

        // ensure key
        if (keystore.containsAlias(KEY_ALIAS)) {
            // retrieve existing key
            KeyStore.SecretKeyEntry entry = (SecretKeyEntry) keystore.getEntry(KEY_ALIAS,
                    new KeyStore.PasswordProtection(null));
            SecretKey key = entry.getSecretKey();
            return key;
        } else {
            // create new key
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            sr.setSeed(toByte(seed));
            keyGen.init(128, sr); // 192 and 256 bits may not be available
            SecretKey key = keyGen.generateKey();

            // store key
            KeyStore.SecretKeyEntry entry = new KeyStore.SecretKeyEntry(key);
            keystore.setEntry(KEY_ALIAS, entry, new KeyStore.PasswordProtection(null));

            // write out key store
            FileOutputStream fos = null;
            try {
                fos = context.openFileOutput(DATACORE, Context.MODE_PRIVATE);
                keystore.store(fos, keystorePassword);
            } finally {
                if (fos != null) {
                    fos.close();
                }
            }

            return key;
        }
    }

    private static byte[] decrypt(SecretKey key, byte[] encrypted) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decrypted = cipher.doFinal(encrypted);
        return decrypted;
    }

    public static byte[] toByte(String hexString) {
        int len = hexString.length() / 2;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++)
            result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2), 16).byteValue();
        return result;
    }

    public static String toHex(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

}
