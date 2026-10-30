package fr.enseirb.geordv.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import fr.enseirb.geordv.model.RendezVous;

/**
 * Ouverture du lieu dans une application de carte, itinéraire et partage.
 */
public final class Cartes {

    private Cartes() {
    }

    /** Affiche le lieu avec un repère nommé. */
    public static void afficher(Activity activite, RendezVous rdv) {
        String libelle = Uri.encode(Format.titre(rdv));
        Uri uri = Uri.parse("geo:" + rdv.getCoordonnees() + "?q=" + rdv.getCoordonnees() + "(" + libelle + ")");
        lancer(activite, new Intent(Intent.ACTION_VIEW, uri), "Aucune application de carte disponible");
    }

    /** Lance la navigation à pied vers le lieu (Google Maps), sinon affiche la carte. */
    public static void itineraire(Activity activite, RendezVous rdv) {
        Intent navigation = new Intent(Intent.ACTION_VIEW,
                Uri.parse("google.navigation:q=" + rdv.getCoordonnees() + "&mode=w"));
        if (navigation.resolveActivity(activite.getPackageManager()) != null) {
            activite.startActivity(navigation);
        } else {
            afficher(activite, rdv);
        }
    }

    /** Partage le rendez-vous par n'importe quelle application (WhatsApp, e-mail…). */
    public static void partager(Activity activite, RendezVous rdv) {
        String texte = Format.titre(rdv)
                + (rdv.aUneDate() ? "\n" + Format.date(rdv.getDateHeure()) : "")
                + "\n" + rdv.getLienCarte();
        Intent partage = new Intent(Intent.ACTION_SEND);
        partage.setType("text/plain");
        partage.putExtra(Intent.EXTRA_TEXT, texte);
        activite.startActivity(Intent.createChooser(partage, "Partager le rendez-vous"));
    }

    private static void lancer(Activity activite, Intent intent, String erreur) {
        try {
            activite.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(activite, erreur, Toast.LENGTH_SHORT).show();
        }
    }
}
