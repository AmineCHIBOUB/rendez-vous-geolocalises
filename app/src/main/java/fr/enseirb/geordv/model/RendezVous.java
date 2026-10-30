package fr.enseirb.geordv.model;

/**
 * Un rendez-vous géolocalisé, envoyé ou reçu.
 * Classe Java pure (sans dépendance Android) pour pouvoir être testée.
 */
public class RendezVous {

    public static final String SENS_ENVOYE = "ENVOYE";
    public static final String SENS_RECU = "RECU";

    public static final String STATUT_EN_ATTENTE = "EN_ATTENTE";
    public static final String STATUT_ACCEPTE = "ACCEPTE";
    public static final String STATUT_REFUSE = "REFUSE";
    public static final String STATUT_ANNULE = "ANNULE";

    private String id;
    private String titre;
    private double latitude;
    private double longitude;
    /** Date et heure du rendez-vous en millisecondes, 0 si non précisée. */
    private long dateHeure;
    private String sens;
    /** Pour un rendez-vous reçu : numéro de l'organisateur. */
    private String contact;
    /** Pour un rendez-vous reçu : notre réponse. Pour un envoyé : EN_ATTENTE ou ANNULE. */
    private String statut;

    public RendezVous(String id, String titre, double latitude, double longitude, long dateHeure) {
        this.id = id;
        this.titre = titre;
        this.latitude = latitude;
        this.longitude = longitude;
        this.dateHeure = dateHeure;
        this.statut = STATUT_EN_ATTENTE;
    }

    public boolean estEnvoye() {
        return SENS_ENVOYE.equals(sens);
    }

    public boolean estAnnule() {
        return STATUT_ANNULE.equals(statut);
    }

    public boolean aUneDate() {
        return dateHeure > 0;
    }

    /** Lien Google Maps lisible par n'importe quelle application. */
    public String getLienCarte() {
        return "https://maps.google.com/?q=" + latitude + "," + longitude;
    }

    public String getCoordonnees() {
        return latitude + "," + longitude;
    }

    public String getId() { return id; }
    public String getTitre() { return titre; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public long getDateHeure() { return dateHeure; }
    public String getSens() { return sens; }
    public String getContact() { return contact; }
    public String getStatut() { return statut; }

    public void setSens(String sens) { this.sens = sens; }
    public void setContact(String contact) { this.contact = contact; }
    public void setStatut(String statut) { this.statut = statut; }
}
