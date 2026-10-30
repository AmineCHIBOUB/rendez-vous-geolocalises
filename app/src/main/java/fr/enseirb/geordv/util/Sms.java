package fr.enseirb.geordv.util;

import android.telephony.SmsManager;

import java.util.ArrayList;

/**
 * Envoi de SMS. Un message trop long (titre de rendez-vous par exemple)
 * est découpé automatiquement et recomposé par le téléphone qui le reçoit.
 */
public final class Sms {

    private Sms() {
    }

    public static void envoyer(String numero, String message) {
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> parties = smsManager.divideMessage(message);
        if (parties.size() > 1) {
            smsManager.sendMultipartTextMessage(numero, null, parties, null, null);
        } else {
            smsManager.sendTextMessage(numero, null, message, null, null);
        }
    }
}
