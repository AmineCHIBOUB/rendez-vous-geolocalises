package fr.enseirb.geordv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.enseirb.geordv.data.RdvDatabase;
import fr.enseirb.geordv.model.RendezVous;
import fr.enseirb.geordv.util.Contacts;
import fr.enseirb.geordv.util.Format;

/**
 * Historique de tous les rendez-vous envoyés et reçus.
 */
public class HistoriqueActivity extends Activity {

    private List<RendezVous> rendezVous;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historique);

        ListView liste = findViewById(R.id.listeRdv);
        liste.setEmptyView(findViewById(R.id.textVide));
        liste.setOnItemClickListener((parent, vue, position, id) -> ouvrir(rendezVous.get(position)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // rechargé à chaque retour sur l'écran pour afficher les nouvelles réponses
        RdvDatabase db = RdvDatabase.get(this);
        rendezVous = db.lister();

        List<Map<String, String>> lignes = new ArrayList<>();
        int envoyes = 0;
        for (RendezVous rdv : rendezVous) {
            Map<String, String> ligne = new HashMap<>();
            String sens;
            if (rdv.estEnvoye()) {
                envoyes++;
                sens = "📤 " + resumeReponses(db.participants(rdv.getId()));
            } else {
                sens = "📥 De " + Contacts.nom(this, rdv.getContact()) + " · " + Format.statut(rdv.getStatut());
            }
            if (rdv.estAnnule()) {
                sens = "🚫 Annulé";
            }
            ligne.put("titre", Format.titre(rdv) + (rdv.aUneDate() ? " — " + Format.date(rdv.getDateHeure()) : ""));
            ligne.put("detail", sens);
            lignes.add(ligne);
        }

        ((ListView) findViewById(R.id.listeRdv)).setAdapter(new SimpleAdapter(this, lignes,
                android.R.layout.simple_list_item_2,
                new String[] {"titre", "detail"},
                new int[] {android.R.id.text1, android.R.id.text2}));

        ((TextView) findViewById(R.id.textResume)).setText(
                envoyes + " envoyé(s) · " + (rendezVous.size() - envoyes) + " reçu(s)");
    }

    /** Ex : "2 acceptés · 1 en attente". */
    static String resumeReponses(List<RdvDatabase.Participant> participants) {
        int acceptes = 0;
        int refuses = 0;
        for (RdvDatabase.Participant p : participants) {
            if (RendezVous.STATUT_ACCEPTE.equals(p.statut)) {
                acceptes++;
            } else if (RendezVous.STATUT_REFUSE.equals(p.statut)) {
                refuses++;
            }
        }
        int attente = participants.size() - acceptes - refuses;
        return acceptes + " accepté(s) · " + refuses + " refusé(s) · " + attente + " en attente";
    }

    private void ouvrir(RendezVous rdv) {
        Intent intent;
        if (!rdv.estEnvoye() && RendezVous.STATUT_EN_ATTENTE.equals(rdv.getStatut())) {
            // invitation reçue sans réponse : on ouvre l'écran pour répondre
            intent = new Intent(this, InvitationRecueActivity.class);
            intent.putExtra(InvitationRecueActivity.EXTRA_ID_RDV, rdv.getId());
        } else {
            intent = new Intent(this, DetailRdvActivity.class);
            intent.putExtra(DetailRdvActivity.EXTRA_ID_RDV, rdv.getId());
        }
        startActivity(intent);
    }
}
