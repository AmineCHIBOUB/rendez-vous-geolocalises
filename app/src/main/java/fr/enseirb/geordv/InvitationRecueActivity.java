package fr.enseirb.geordv;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activité affichée lorsqu'une invitation GeoRDV est reçue.
 */
public class InvitationRecueActivity extends Activity {

    private String numeroExpediteur;
    private String coordonnees;
    private TextView textExpediteur;
    private TextView textCoordonnees;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitation_recue);

        textExpediteur = (TextView) findViewById(R.id.textExpediteur);
        textCoordonnees = (TextView) findViewById(R.id.textCoordonnees);

        numeroExpediteur = getIntent().getStringExtra("expediteur");
        coordonnees = getIntent().getStringExtra("coordonnees");

        if (numeroExpediteur == null) {
            numeroExpediteur = "inconnu";
        }
        if (coordonnees == null) {
            coordonnees = "non disponibles";
        }

        textExpediteur.setText("Invitation de : " + numeroExpediteur);
        textCoordonnees.setText("Coordonnées GPS : " + coordonnees);
    }


    /**
     * Version v2 : affichage du rendez-vous sur une carte,
     * en utilisant un Intent ACTION_VIEW avec une URI geo:.
     * C'est la solution la plus proche du cours.
     */
    public void afficherCarte(View vue) {
        if (coordonnees == null) {
            Toast.makeText(this, "Coordonnées indisponibles", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] morceaux = coordonnees.split(",");
        if (morceaux.length != 2) {
            Toast.makeText(this, "Coordonnées invalides", Toast.LENGTH_SHORT).show();
            return;
        }

        String latitude = morceaux[0].trim();
        String longitude = morceaux[1].trim();
        Uri position = Uri.parse("geo:" + latitude + "," + longitude);

        Intent intentCarte = new Intent(Intent.ACTION_VIEW);
        intentCarte.setData(position);

        try {
            startActivity(intentCarte);
        } catch (Exception e) {
            Toast.makeText(this, "Aucune application de carte disponible", Toast.LENGTH_SHORT).show();
        }
    }

    public void accepterInvitation(View vue) {
        envoyerReponse("RDV_REPONSE:ACCEPTE", "Rendez-vous accepté !");
    }

    public void refuserInvitation(View vue) {
        envoyerReponse("RDV_REPONSE:REFUSE", "Rendez-vous refusé.");
    }

    private void envoyerReponse(String message, String confirmation) {
        if (!permissionSmsAccordee()) {
            Toast.makeText(this, "Permission SMS refusée", Toast.LENGTH_SHORT).show();
            return;
        }

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(numeroExpediteur, null, message, null, null);
        Toast.makeText(this, confirmation, Toast.LENGTH_SHORT).show();
        finish();
    }

    private boolean permissionSmsAccordee() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }
}
