package fr.enseirb.geordv.model;

/**
 * Distance à vol d'oiseau et temps de trajet estimé (formule de haversine).
 */
public final class Trajet {

    private static final double RAYON_TERRE_METRES = 6_371_000;
    /** Vitesse de marche moyenne : 5 km/h. */
    private static final double VITESSE_MARCHE_M_PAR_MIN = 5000.0 / 60;

    private Trajet() {
    }

    public static double distanceMetres(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return RAYON_TERRE_METRES * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static int minutesAPied(double distanceMetres) {
        return (int) Math.ceil(distanceMetres / VITESSE_MARCHE_M_PAR_MIN);
    }

    /** "850 m" ou "12,4 km". */
    public static String formaterDistance(double distanceMetres) {
        if (distanceMetres < 1000) {
            return Math.round(distanceMetres) + " m";
        }
        return String.format(java.util.Locale.FRANCE, "%.1f km", distanceMetres / 1000);
    }
}
