package fr.enseirb.geordv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;

import java.util.UUID;

import fr.enseirb.geordv.data.RdvDatabase;
import fr.enseirb.geordv.model.MessageRdv;
import fr.enseirb.geordv.model.RendezVous;
import fr.enseirb.geordv.util.Contacts;
import fr.enseirb.geordv.util.Format;
import fr.enseirb.geordv.util.Notifications;
import fr.enseirb.geordv.util.Rappels;

/**
 * Reçoit les SMS GeoRDV (invitation, réponse, annulation),
 * les enregistre et prévient l'utilisateur par une notification.
 */
public class SmsReceiver extends BroadcastReceiver {

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

        // un SMS long arrive en plusieurs morceaux : on les recompose
        String format = bundle.getString("format");
        String expediteur = null;
        StringBuilder texteComplet = new StringBuilder();
        for (Object pdu : pdus) {
            SmsMessage sms = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    ? SmsMessage.createFromPdu((byte[]) pdu, format)
                    : SmsMessage.createFromPdu((byte[]) pdu);
            if (expediteur == null) {
                expediteur = sms.getOriginatingAddress();
            }
            if (sms.getMessageBody() != null) {
                texteComplet.append(sms.getMessageBody());
            }
        }

        String texte = texteComplet.toString();
        if (expediteur == null || texte.isEmpty()) {
            return;
        }

        if (MessageRdv.estInvitation(texte)) {
            recevoirInvitation(context, expediteur, texte);
        } else if (MessageRdv.estReponse(texte)) {
            recevoirReponse(context, expediteur, texte);
        } else if (MessageRdv.estAnnulation(texte)) {
            recevoirAnnulation(context, expediteur, texte);
        }
    }

    private void recevoirInvitation(Context context, String expediteur, String texte) {
        RendezVous rdv = MessageRdv.lireInvitation(texte);
        if (rdv == null) {
            return;
        }
        // une invitation de l'ancien format n'a pas d'identifiant : on en crée un
        if (rdv.getId().isEmpty()) {
            rdv = new RendezVous(UUID.randomUUID().toString().substring(0, 8), rdv.getTitre(),
                    rdv.getLatitude(), rdv.getLongitude(), rdv.getDateHeure());
        }
        rdv.setSens(RendezVous.SENS_RECU);
        rdv.setContact(expediteur);
        RdvDatabase.get(context).enregistrer(rdv);

        Intent ouverture = new Intent(context, InvitationRecueActivity.class);
        ouverture.putExtra(InvitationRecueActivity.EXTRA_ID_RDV, rdv.getId());

        String nom = Contacts.nom(context, expediteur);
        String detail = Format.titre(rdv) + (rdv.aUneDate() ? "\n" + Format.date(rdv.getDateHeure()) : "");
        Notifications.afficher(context, rdv.getId().hashCode(), "📍 Invitation de " + nom, detail, ouverture);
    }

    private void recevoirReponse(Context context, String expediteur, String texte) {
        MessageRdv.Reponse reponse = MessageRdv.lireReponse(texte);
        if (reponse == null) {
            return;
        }
        String idRdv = RdvDatabase.get(context).enregistrerReponse(
                reponse.idRdv, expediteur, reponse.statut, reponse.message);

        Intent ouverture;
        if (idRdv != null) {
            ouverture = new Intent(context, DetailRdvActivity.class);
            ouverture.putExtra(DetailRdvActivity.EXTRA_ID_RDV, idRdv);
        } else {
            ouverture = new Intent(context, HistoriqueActivity.class);
        }

        String nom = Contacts.nom(context, expediteur);
        String titre = nom + (reponse.estAcceptee() ? " a accepté" : " a refusé") + " votre rendez-vous";
        String detail = reponse.message.isEmpty() ? "Touchez pour voir les réponses" : "« " + reponse.message + " »";
        Notifications.afficher(context, (expediteur + reponse.idRdv).hashCode(), titre, detail, ouverture);
    }

    private void recevoirAnnulation(Context context, String expediteur, String texte) {
        String idRdv = MessageRdv.lireAnnulation(texte);
        RdvDatabase db = RdvDatabase.get(context);
        RendezVous rdv = idRdv == null ? null : db.trouver(idRdv);
        if (rdv == null) {
            return;
        }
        db.changerStatut(idRdv, RendezVous.STATUT_ANNULE);
        Rappels.annuler(context, idRdv);

        Intent ouverture = new Intent(context, DetailRdvActivity.class);
        ouverture.putExtra(DetailRdvActivity.EXTRA_ID_RDV, idRdv);
        Notifications.afficher(context, idRdv.hashCode(), "🚫 Rendez-vous annulé",
                Contacts.nom(context, expediteur) + " a annulé « " + Format.titre(rdv) + " »", ouverture);
    }
}
