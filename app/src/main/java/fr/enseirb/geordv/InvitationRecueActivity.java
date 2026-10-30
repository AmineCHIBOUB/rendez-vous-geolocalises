package fr.enseirb.geordv;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import fr.enseirb.geordv.data.RdvDatabase;
import fr.enseirb.geordv.model.MessageRdv;
import fr.enseirb.geordv.model.RendezVous;
import fr.enseirb.geordv.model.Trajet;
import fr.enseirb.geordv.util.Cartes;
import fr.enseirb.geordv.util.Contacts;
import fr.enseirb.geordv.util.Format;
import fr.enseirb.geordv.util.Rappels;
import fr.enseirb.geordv.util.Sms;

/**
 * Invitation reçue : détails, distance, itinéraire et réponse.
 */
public class InvitationRecueActivity extends Activity {

    public static final String EXTRA_ID_RDV = "id_rdv";

    private RendezVous rdv;
    private EditText editMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitation_recue);
        editMessage = findViewById(R.id.editMessage);

        String idRdv = getIntent().getStringExtra(EXTRA_ID_RDV);
        rdv = idRdv == null ? null : RdvDatabase.get(this).trouver(idRdv);
        if (rdv == null) {
            Toast.makeText(this, "Invitation introuvable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        afficher();
    }

    private void afficher() {
        ((TextView) findViewById(R.id.textExpediteur)).setText("Invitation de " + Contacts.nom(this, rdv.getContact()));
        ((TextView) findViewById(R.id.textTitre)).setText(Format.titre(rdv));
        ((TextView) findViewById(R.id.textDate)).setText("📅 " + Format.date(rdv.getDateHeure()));
        ((TextView) findViewById(R.id.textCoordonnees)).setText("📍 " + rdv.getCoordonnees());
        ((TextView) findViewById(R.id.textDistance)).setText(texteDistance());

        // si on a déjà répondu, on affiche la réponse à la place des boutons
        boolean dejaRepondu = !RendezVous.STATUT_EN_ATTENTE.equals(rdv.getStatut());
        TextView textStatut = findViewById(R.id.textStatut);
        textStatut.setVisibility(dejaRepondu ? View.VISIBLE : View.GONE);
        textStatut.setText(Format.statut(rdv.getStatut()));
        findViewById(R.id.blocReponse).setVisibility(dejaRepondu ? View.GONE : View.VISIBLE);
    }

    /** Distance et temps de marche depuis la dernière position connue. */
    private String texteDistance() {
        Location position = dernierePositionConnue();
        if (position == null) {
            return "🚶 Distance inconnue (position non disponible)";
        }
        double distance = Trajet.distanceMetres(position.getLatitude(), position.getLongitude(),
                rdv.getLatitude(), rdv.getLongitude());
        return "🚶 À " + Trajet.formaterDistance(distance) + ", environ "
                + Trajet.minutesAPied(distance) + " min à pied";
    }

    private Location dernierePositionConnue() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null) {
            return null;
        }
        try {
            Location gps = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            return gps != null ? gps : manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (SecurityException e) {
            return null;
        }
    }

    public void afficherCarte(View vue) {
        Cartes.afficher(this, rdv);
    }

    public void lancerItineraire(View vue) {
        Cartes.itineraire(this, rdv);
    }

    public void accepterInvitation(View vue) {
        repondre(RendezVous.STATUT_ACCEPTE, "Rendez-vous accepté !");
    }

    public void refuserInvitation(View vue) {
        repondre(RendezVous.STATUT_REFUSE, "Rendez-vous refusé.");
    }

    private void repondre(String statut, String confirmation) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission SMS refusée", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = editMessage.getText().toString().trim();
        Sms.envoyer(rdv.getContact(), MessageRdv.reponse(rdv.getId(), statut, message));

        RdvDatabase.get(this).changerStatut(rdv.getId(), statut);
        rdv.setStatut(statut);
        if (RendezVous.STATUT_ACCEPTE.equals(statut)) {
            Rappels.programmer(this, rdv);
        }

        Toast.makeText(this, confirmation, Toast.LENGTH_SHORT).show();
        afficher();
    }
}
