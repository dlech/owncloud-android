/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.syncadapter;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.UnknownHostException;

import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.authenticator.AccountAuthenticator;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;

public class ContactSyncAdapter extends AbstractOwnCloudSyncAdapter {
    private String mAddrBookUri;

    public ContactSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mAddrBookUri = null;
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        
        
        setAccount(account);
        setContentProvider(provider);
        try {
            initClientForCurrentAccount();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        Cursor cursor = getLocalContacts(false);
        if (cursor.moveToFirst()) {
            do {
                String lookup = cursor.getString(cursor
                        .getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                String a = getAddressBookUri();
                String uri = a + lookup + ".vcf";
                try {                    
                    PutMethod put = new PutMethod(uri);
                    put.addRequestHeader("If-None-Match", "*");
                    String vCard = getContactVcard(lookup);
                    Log.d("syncadapter", vCard);
                    put.setRequestEntity(new StringRequestEntity(vCard,
                            "text/vcard", "utf-8"));
                    getClient().executeMethod(put);
                    Log.d("syncadapter", put.getResponseBodyAsString());
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            } while (cursor.moveToNext());
        }
    }

    private String getAddressBookUri() {
        if (mAddrBookUri != null)
            return mAddrBookUri;

        AccountManager am = getAccountManager();
        String uri = am.getUserData(getAccount(),
                AccountAuthenticator.KEY_OC_BASE_URL);
        uri += AccountUtils.CARDDAV_PATH_4_0 + "/addressbooks/"
                + getAccount().name.substring(0,
                        getAccount().name.lastIndexOf('@')) + "/contacts/";
        mAddrBookUri = uri;
        return uri;
    }

    private String getContactVcard(String lookupKey) throws IOException {
        Uri uri = Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
        //AssetFileDescriptor afd = getContext().getContentResolver()
        //        .openAssetFileDescriptor( uri, "r");        
        //FileDescriptor fd = afd.getFileDescriptor();
        //FileInputStream fis = new FileInputStream(fd);
        //byte[] b = new byte[(int) afd.getDeclaredLength()];
        //fis.read(b);
        InputStream instream = getContext().getContentResolver().openInputStream(uri);
        byte[] b = new byte[(int) instream.available()];
        instream.read(b, 0, 1);
        instream.close();
        return new String(b);
    }

    private Cursor getLocalContacts(boolean include_hidden_contacts) {
        return getContext().getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[] { ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.LOOKUP_KEY },
                ContactsContract.Contacts.IN_VISIBLE_GROUP + " = ?",
                new String[] { (include_hidden_contacts ? "0" : "1") },
                ContactsContract.Contacts._ID + " DESC");
    }

}
