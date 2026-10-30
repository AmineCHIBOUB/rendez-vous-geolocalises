package fr.enseirb.geordv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import fr.enseirb.geordv.data.RdvDatabase;
import fr.enseirb.geordv.model.RendezVous;
import fr.enseirb.geordv.util.Format;
import fr.enseirb.geordv.util.Notifications;
import fr.enseirb.geordv.util.Rappels;

/**
 * Déclenché par l'alarme 30 minutes avant un rendez-vous.
 */
public class RappelReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String idRdv = intent.getStringExtra(Rappels.EXTRA_ID_RDV);
        if (idRdv == null) {
            return;
        }
        RendezVous rdv = RdvDatabase.get(context).trouver(idRdv);
        if (rdv == null || rdv.estAnnule() || RendezVous.STATUT_REFUSE.equals(rdv.getStatut())) {
            return;
        }

        Intent ouverture = new Intent(context, DetailRdvActivity.class);
        ouverture.putExtra(DetailRdvActivity.EXTRA_ID_RDV, idRdv);
        Notifications.afficher(context, ("rappel" + idRdv).hashCode(),
                "⏰ " + Format.titre(rdv) + " dans 30 minutes",
                Format.date(rdv.getDateHeure()) + "\nTouchez pour lancer l'itinéraire", ouverture);
    }
}
