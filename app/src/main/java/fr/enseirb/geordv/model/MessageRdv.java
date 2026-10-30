package fr.enseirb.geordv.model;

/**
 * Protocole des SMS échangés entre deux applications GeoRDV.
 *
 * Version 2 (actuelle) :
 *   RDV_INVITE:2|id|latitude,longitude|dateHeureMillis|titre
 *   RDV_REPONSE:id|ACCEPTE|message facultatif
 *   RDV_ANNULE:id
 *
 * Version 1 (toujours comprise, pour les anciennes installations) :
 *   RDV_INVITE:latitude,longitude
 *   RDV_REPONSE:ACCEPTE
 *
 * Classe Java pure pour être testée sans téléphone.
 */
public final class MessageRdv {

    public static final String PREFIXE_INVITE = "RDV_INVITE:";
    public static final String PREFIXE_REPONSE = "RDV_REPONSE:";
    public static final String PREFIXE_ANNULE = "RDV_ANNULE:";

    private static final String VERSION = "2";
    private static final String SEPARATEUR = "|";
    private static final String SEPARATEUR_REGEX = "\\|";
    /** Identifiant utilisé pour les messages version 1, qui n'en ont pas. */
    public static final String ID_INCONNU = "";

    private MessageRdv() {
    }

    // === CONSTRUCTION DES MESSAGES ===

    public static String invitation(RendezVous rdv) {
        return PREFIXE_INVITE + VERSION
                + SEPARATEUR + rdv.getId()
                + SEPARATEUR + rdv.getCoordonnees()
                + SEPARATEUR + rdv.getDateHeure()
                + SEPARATEUR + nettoyer(rdv.getTitre());
    }

    public static String reponse(String idRdv, String statut, String message) {
        String texte = PREFIXE_REPONSE + idRdv + SEPARATEUR + statut;
        String commentaire = nettoyer(message);
        if (!commentaire.isEmpty()) {
            texte += SEPARATEUR + commentaire;
        }
        return texte;
    }

    public static String annulation(String idRdv) {
        return PREFIXE_ANNULE + idRdv;
    }

    /** Le séparateur ne doit pas apparaître dans un texte libre. */
    static String nettoyer(String texte) {
        if (texte == null) {
            return "";
        }
        return texte.replace(SEPARATEUR, "/").replace("\n", " ").trim();
    }

    // === LECTURE DES MESSAGES ===

    public static boolean estInvitation(String sms) {
        return sms != null && sms.startsWith(PREFIXE_INVITE);
    }

    public static boolean estReponse(String sms) {
        return sms != null && sms.startsWith(PREFIXE_REPONSE);
    }

    public static boolean estAnnulation(String sms) {
        return sms != null && sms.startsWith(PREFIXE_ANNULE);
    }

    /** Lit une invitation (v1 ou v2). Retourne null si le message est invalide. */
    public static RendezVous lireInvitation(String sms) {
        if (!estInvitation(sms)) {
            return null;
        }
        String contenu = sms.substring(PREFIXE_INVITE.length());
        String[] parties = contenu.split(SEPARATEUR_REGEX, 5);
        try {
            if (parties.length >= 4 && VERSION.equals(parties[0])) {
                double[] position = lirePosition(parties[2]);
                if (position == null) {
                    return null;
                }
                String titre = parties.length == 5 ? parties[4] : "";
                return new RendezVous(parties[1], titre, position[0], position[1], Long.parseLong(parties[3]));
            }
            // version 1 : uniquement les coordonnées
            double[] position = lirePosition(contenu);
            if (position == null) {
                return null;
            }
            return new RendezVous(ID_INCONNU, "", position[0], position[1], 0);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Réponse lue dans un SMS : identifiant du rendez-vous, statut et message éventuel. */
    public static final class Reponse {
        public final String idRdv;
        public final String statut;
        public final String message;

        Reponse(String idRdv, String statut, String message) {
            this.idRdv = idRdv;
            this.statut = statut;
            this.message = message;
        }

        public boolean estAcceptee() {
            return RendezVous.STATUT_ACCEPTE.equals(statut);
        }
    }

    public static Reponse lireReponse(String sms) {
        if (!estReponse(sms)) {
            return null;
        }
        String[] parties = sms.substring(PREFIXE_REPONSE.length()).split(SEPARATEUR_REGEX, 3);
        if (parties.length == 1) {
            // version 1 : "RDV_REPONSE:ACCEPTE"
            return new Reponse(ID_INCONNU, parties[0].trim(), "");
        }
        String message = parties.length == 3 ? parties[2] : "";
        return new Reponse(parties[0].trim(), parties[1].trim(), message);
    }

    public static String lireAnnulation(String sms) {
        if (!estAnnulation(sms)) {
            return null;
        }
        return sms.substring(PREFIXE_ANNULE.length()).trim();
    }

    /** "lat,lng" -> {lat, lng}, ou null si invalide ou hors limites. */
    static double[] lirePosition(String texte) {
        String[] morceaux = texte.split(",");
        if (morceaux.length != 2) {
            return null;
        }
        try {
            double latitude = Double.parseDouble(morceaux[0].trim());
            double longitude = Double.parseDouble(morceaux[1].trim());
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                return null;
            }
            return new double[] {latitude, longitude};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
