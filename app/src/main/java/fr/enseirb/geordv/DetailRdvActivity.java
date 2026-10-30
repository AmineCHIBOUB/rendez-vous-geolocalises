package fr.enseirb.geordv;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import fr.enseirb.geordv.data.RdvDatabase;
import fr.enseirb.geordv.model.MessageRdv;
import fr.enseirb.geordv.model.RendezVous;
import fr.enseirb.geordv.util.Cartes;
import fr.enseirb.geordv.util.Contacts;
import fr.enseirb.geordv.util.Format;
import fr.enseirb.geordv.util.Rappels;
import fr.enseirb.geordv.util.Sms;

/**
 * Détail d'un rendez-vous : suivi des réponses des invités,
 * carte, itinéraire, partage, annulation et suppression.
 */
public class DetailRdvActivity extends Activity {

    public static final String EXTRA_ID_RDV = "id_rdv";

    private RendezVous rdv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_rdv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // rechargé à chaque affichage pour voir les nouvelles réponses
        String idRdv = getIntent().getStringExtra(EXTRA_ID_RDV);
        rdv = idRdv == null ? null : RdvDatabase.get(this).trouver(idRdv);
        if (rdv == null) {
            Toast.makeText(this, "Rendez-vous introuvable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        afficher();
    }

    private void afficher() {
        ((TextView) findViewById(R.id.textSens)).setText(rdv.estEnvoye()
                ? "Rendez-vous que vous organisez"
                : "Invitation de " + Contacts.nom(this, rdv.getContact()));
        ((TextView) findViewById(R.id.textTitre)).setText(Format.titre(rdv));
        ((TextView) findViewById(R.id.textDate)).setText("📅 " + Format.date(rdv.getDateHeure()));

        TextView textStatut = findViewById(R.id.textStatut);
        if (rdv.estAnnule()) {
            textStatut.setText(Format.statut(RendezVous.STATUT_ANNULE));
        } else if (!rdv.estEnvoye()) {
            textStatut.setText("Votre réponse : " + Format.statut(rdv.getStatut()));
        } else {
            textStatut.setText("");
        }

        int visibiliteParticipants = rdv.estEnvoye() ? View.VISIBLE : View.GONE;
        findViewById(R.id.titreParticipants).setVisibility(visibiliteParticipants);
        TextView textParticipants = findViewById(R.id.textParticipants);
        textParticipants.setVisibility(visibiliteParticipants);
        if (rdv.estEnvoye()) {
            textParticipants.setText(texteParticipants(RdvDatabase.get(this).participants(rdv.getId())));
        }

        // seul l'organisateur peut annuler, et une seule fois
        findViewById(R.id.boutonAnnuler).setVisibility(
                rdv.estEnvoye() && !rdv.estAnnule() ? View.VISIBLE : View.GONE);
    }

    private String texteParticipants(List<RdvDatabase.Participant> participants) {
        StringBuilder sb = new StringBuilder(HistoriqueActivity.resumeReponses(participants)).append("\n\n");
        for (RdvDatabase.Participant p : participants) {
            sb.append(Format.statut(p.statut)).append("  ").append(Contacts.nom(this, p.numero));
            if (p.message != null && !p.message.isEmpty()) {
                sb.append("\n      « ").append(p.message).append(" »");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    public void afficherCarte(View vue) {
        Cartes.afficher(this, rdv);
    }

    public void lancerItineraire(View vue) {
        Cartes.itineraire(this, rdv);
    }

    public void partager(View vue) {
        Cartes.partager(this, rdv);
    }

    /** Prévient tous les invités par SMS et annule le rappel. */
    public void annulerRendezVous(View vue) {
        new AlertDialog.Builder(this)
                .setTitle("Annuler le rendez-vous ?")
                .setMessage("Tous les invités seront prévenus par SMS.")
                .setPositiveButton("Annuler le rendez-vous", (dialogue, choix) -> {
                    RdvDatabase db = RdvDatabase.get(this);
                    String message = MessageRdv.annulation(rdv.getId());
                    for (RdvDatabase.Participant p : db.participants(rdv.getId())) {
                        Sms.envoyer(p.numero, message);
                    }
                    db.changerStatut(rdv.getId(), RendezVous.STATUT_ANNULE);
                    rdv.setStatut(RendezVous.STATUT_ANNULE);
                    Rappels.annuler(this, rdv.getId());
                    Toast.makeText(this, "Rendez-vous annulé, invités prévenus", Toast.LENGTH_SHORT).show();
                    afficher();
                })
                .setNegativeButton("Retour", null)
                .show();
    }

    public void supprimer(View vue) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer de l'historique ?")
                .setMessage("Aucun SMS ne sera envoyé.")
                .setPositiveButton("Supprimer", (dialogue, choix) -> {
                    RdvDatabase.get(this).supprimer(rdv.getId());
                    Rappels.annuler(this, rdv.getId());
                    finish();
                })
                .setNegativeButton("Retour", null)
                .show();
    }
}
