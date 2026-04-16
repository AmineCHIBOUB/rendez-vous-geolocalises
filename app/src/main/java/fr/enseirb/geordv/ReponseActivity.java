package fr.enseirb.geordv;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/**
 * Activité affichée quand l'utilisateur reçoit une réponse à son invitation.
 */
public class ReponseActivity extends Activity {

    private TextView textReponse;
    private TextView textExpediteur;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reponse);

        textExpediteur = (TextView) findViewById(R.id.textExpediteurReponse);
        textReponse = (TextView) findViewById(R.id.textReponse);

        String expediteur = getIntent().getStringExtra("expediteur");
        String reponse = getIntent().getStringExtra("reponse");

        if (expediteur == null) {
            expediteur = "inconnu";
        }
        if (reponse == null) {
            reponse = "inconnue";
        }

        textExpediteur.setText("Réponse de : " + expediteur);

        if ("ACCEPTE".equals(reponse)) {
            textReponse.setText(expediteur + " a ACCEPTÉ votre rendez-vous !");
        } else if ("REFUSE".equals(reponse)) {
            textReponse.setText(expediteur + " a REFUSÉ votre rendez-vous.");
        } else {
            textReponse.setText("Réponse reçue : " + reponse);
        }
    }

    public void retourAccueil(View vue) {
        finish();
    }
}
