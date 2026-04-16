package fr.enseirb.geordv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

/**
 * Récepteur des SMS de l'application.
 *
 * Deux formats sont reconnus :
 * - RDV_INVITE:latitude,longitude
 * - RDV_REPONSE:ACCEPTE ou RDV_REPONSE:REFUSE
 */
public class SmsReceiver extends BroadcastReceiver {

    private static final String PREFIXE_INVITE = "RDV_INVITE:";
    private static final String PREFIXE_REPONSE = "RDV_REPONSE:";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();

        if (bundle == null) {
            return;
        }

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null || pdus.length == 0) {
            return;
        }

        String expediteur = null;
        StringBuilder texteComplet = new StringBuilder();

        for (Object pdu : pdus) {
            SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);

            if (expediteur == null) {
                expediteur = sms.getOriginatingAddress();
            }

            String partie = sms.getMessageBody();
            if (partie != null) {
                texteComplet.append(partie);
            }
        }

        String texte = texteComplet.toString();

        if (expediteur == null || texte.length() == 0) {
            return;
        }

        if (texte.startsWith(PREFIXE_INVITE)) {
            String coordonnees = texte.substring(PREFIXE_INVITE.length());

            Intent intentInvitation = new Intent(context, InvitationRecueActivity.class);
            intentInvitation.putExtra("expediteur", expediteur);
            intentInvitation.putExtra("coordonnees", coordonnees);
            intentInvitation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intentInvitation);
        } else if (texte.startsWith(PREFIXE_REPONSE)) {
            String reponse = texte.substring(PREFIXE_REPONSE.length());

            Intent intentReponse = new Intent(context, ReponseActivity.class);
            intentReponse.putExtra("expediteur", expediteur);
            intentReponse.putExtra("reponse", reponse);
            intentReponse.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intentReponse);
        }
    }
}
