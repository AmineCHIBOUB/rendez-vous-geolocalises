package fr.enseirb.geordv.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import fr.enseirb.geordv.model.Numeros;
import fr.enseirb.geordv.model.RendezVous;

/**
 * Stockage local des rendez-vous (SQLite, intégré à Android).
 *
 * Table rendezvous   : un rendez-vous envoyé ou reçu
 * Table participants : pour un rendez-vous envoyé, la réponse de chaque invité
 */
public class RdvDatabase extends SQLiteOpenHelper {

    private static final String NOM_BASE = "georedv.db";
    private static final int VERSION = 1;

    private static RdvDatabase instance;

    /** Réponse d'un invité à un rendez-vous envoyé. */
    public static class Participant {
        public final String numero;
        public final String statut;
        public final String message;

        Participant(String numero, String statut, String message) {
            this.numero = numero;
            this.statut = statut;
            this.message = message;
        }
    }

    public static synchronized RdvDatabase get(Context context) {
        if (instance == null) {
            instance = new RdvDatabase(context.getApplicationContext());
        }
        return instance;
    }

    private RdvDatabase(Context context) {
        super(context, NOM_BASE, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE rendezvous ("
                + "id TEXT PRIMARY KEY, titre TEXT, latitude REAL, longitude REAL, "
                + "date_heure INTEGER, sens TEXT, contact TEXT, statut TEXT, cree_le INTEGER)");
        db.execSQL("CREATE TABLE participants ("
                + "rdv_id TEXT, numero TEXT, statut TEXT, message TEXT, "
                + "PRIMARY KEY (rdv_id, numero))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int ancienneVersion, int nouvelleVersion) {
        db.execSQL("DROP TABLE IF EXISTS participants");
        db.execSQL("DROP TABLE IF EXISTS rendezvous");
        onCreate(db);
    }

    // === RENDEZ-VOUS ===

    public void enregistrer(RendezVous rdv) {
        ContentValues valeurs = new ContentValues();
        valeurs.put("id", rdv.getId());
        valeurs.put("titre", rdv.getTitre());
        valeurs.put("latitude", rdv.getLatitude());
        valeurs.put("longitude", rdv.getLongitude());
        valeurs.put("date_heure", rdv.getDateHeure());
        valeurs.put("sens", rdv.getSens());
        valeurs.put("contact", rdv.getContact());
        valeurs.put("statut", rdv.getStatut());
        valeurs.put("cree_le", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("rendezvous", null, valeurs, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void changerStatut(String idRdv, String statut) {
        ContentValues valeurs = new ContentValues();
        valeurs.put("statut", statut);
        getWritableDatabase().update("rendezvous", valeurs, "id = ?", new String[] {idRdv});
    }

    public RendezVous trouver(String idRdv) {
        Cursor c = getReadableDatabase().query("rendezvous", null, "id = ?", new String[] {idRdv}, null, null, null);
        try {
            return c.moveToFirst() ? lire(c) : null;
        } finally {
            c.close();
        }
    }

    /** Tous les rendez-vous, les plus récents en premier. */
    public List<RendezVous> lister() {
        List<RendezVous> liste = new ArrayList<>();
        Cursor c = getReadableDatabase().query("rendezvous", null, null, null, null, null, "cree_le DESC");
        try {
            while (c.moveToNext()) {
                liste.add(lire(c));
            }
        } finally {
            c.close();
        }
        return liste;
    }

    public void supprimer(String idRdv) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("participants", "rdv_id = ?", new String[] {idRdv});
        db.delete("rendezvous", "id = ?", new String[] {idRdv});
    }

    private RendezVous lire(Cursor c) {
        RendezVous rdv = new RendezVous(
                c.getString(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("titre")),
                c.getDouble(c.getColumnIndexOrThrow("latitude")),
                c.getDouble(c.getColumnIndexOrThrow("longitude")),
                c.getLong(c.getColumnIndexOrThrow("date_heure")));
        rdv.setSens(c.getString(c.getColumnIndexOrThrow("sens")));
        rdv.setContact(c.getString(c.getColumnIndexOrThrow("contact")));
        rdv.setStatut(c.getString(c.getColumnIndexOrThrow("statut")));
        return rdv;
    }

    // === PARTICIPANTS ===

    public void ajouterParticipant(String idRdv, String numero) {
        ContentValues valeurs = new ContentValues();
        valeurs.put("rdv_id", idRdv);
        valeurs.put("numero", numero);
        valeurs.put("statut", RendezVous.STATUT_EN_ATTENTE);
        valeurs.put("message", "");
        getWritableDatabase().insertWithOnConflict("participants", null, valeurs, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public List<Participant> participants(String idRdv) {
        List<Participant> liste = new ArrayList<>();
        Cursor c = getReadableDatabase().query("participants", null, "rdv_id = ?", new String[] {idRdv},
                null, null, "numero");
        try {
            while (c.moveToNext()) {
                liste.add(new Participant(
                        c.getString(c.getColumnIndexOrThrow("numero")),
                        c.getString(c.getColumnIndexOrThrow("statut")),
                        c.getString(c.getColumnIndexOrThrow("message"))));
            }
        } finally {
            c.close();
        }
        return liste;
    }

    /**
     * Enregistre la réponse d'un invité.
     * Si l'identifiant est inconnu (ancien format), on prend le dernier
     * rendez-vous envoyé à ce numéro.
     * Retourne l'identifiant du rendez-vous mis à jour, ou null.
     */
    public String enregistrerReponse(String idRdv, String numeroExpediteur, String statut, String message) {
        String idCible = (idRdv == null || idRdv.isEmpty()) ? dernierRdvEnvoyeA(numeroExpediteur) : idRdv;
        if (idCible == null) {
            return null;
        }
        for (Participant p : participants(idCible)) {
            if (Numeros.memeNumero(p.numero, numeroExpediteur)) {
                ContentValues valeurs = new ContentValues();
                valeurs.put("statut", statut);
                valeurs.put("message", message == null ? "" : message);
                getWritableDatabase().update("participants", valeurs, "rdv_id = ? AND numero = ?",
                        new String[] {idCible, p.numero});
                return idCible;
            }
        }
        return null;
    }

    private String dernierRdvEnvoyeA(String numero) {
        for (RendezVous rdv : lister()) {
            if (!rdv.estEnvoye()) {
                continue;
            }
            for (Participant p : participants(rdv.getId())) {
                if (Numeros.memeNumero(p.numero, numero)) {
                    return rdv.getId();
                }
            }
        }
        return null;
    }
}
