package fr.enseirb.geordv.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;

/**
 * Affiche le nom d'un contact à la place de son numéro quand il est connu.
 */
public final class Contacts {

    private Contacts() {
    }

    public static String nom(Context context, String numero) {
        if (numero == null || numero.isEmpty()) {
            return "inconnu";
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return numero;
        }

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(numero));
        Cursor curseur = null;
        try {
            curseur = context.getContentResolver().query(uri,
                    new String[] {ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (curseur != null && curseur.moveToFirst()) {
                return curseur.getString(0);
            }
        } catch (Exception e) {
            // en cas d'erreur, on garde le numéro
        } finally {
            if (curseur != null) {
                curseur.close();
            }
        }
        return numero;
    }
}
