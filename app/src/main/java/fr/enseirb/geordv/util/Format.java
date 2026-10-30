package fr.enseirb.geordv.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import fr.enseirb.geordv.model.RendezVous;

/**
 * Textes affichés à l'écran.
 */
public final class Format {

    private Format() {
    }

    public static String date(long millis) {
        if (millis <= 0) {
            return "Date non précisée";
        }
        return new SimpleDateFormat("EEEE d MMMM 'à' HH:mm", Locale.FRANCE).format(new Date(millis));
    }

    public static String titre(RendezVous rdv) {
        return rdv.getTitre() == null || rdv.getTitre().isEmpty() ? "Rendez-vous" : rdv.getTitre();
    }

    public static String statut(String statut) {
        if (RendezVous.STATUT_ACCEPTE.equals(statut)) {
            return "✅ Accepté";
        }
        if (RendezVous.STATUT_REFUSE.equals(statut)) {
            return "❌ Refusé";
        }
        if (RendezVous.STATUT_ANNULE.equals(statut)) {
            return "🚫 Annulé";
        }
        return "⏳ En attente";
    }
}
