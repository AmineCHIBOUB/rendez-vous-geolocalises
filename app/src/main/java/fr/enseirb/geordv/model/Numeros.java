package fr.enseirb.geordv.model;

/**
 * Outils sur les numéros de téléphone.
 * Un même numéro peut arriver sous plusieurs formes (06 12…, +33 6 12…) :
 * on compare donc les 9 derniers chiffres.
 */
public final class Numeros {

    private static final int CHIFFRES_COMPARES = 9;

    private Numeros() {
    }

    public static String nettoyer(String numero) {
        if (numero == null) {
            return "";
        }
        return numero.replace(" ", "").replace("-", "").replace(".", "")
                .replace("(", "").replace(")", "").trim();
    }

    public static boolean estValide(String numero) {
        return chiffres(numero).length() >= 4;
    }

    public static boolean memeNumero(String a, String b) {
        String chiffresA = chiffres(a);
        String chiffresB = chiffres(b);
        if (chiffresA.isEmpty() || chiffresB.isEmpty()) {
            return false;
        }
        return fin(chiffresA).equals(fin(chiffresB));
    }

    private static String chiffres(String numero) {
        StringBuilder sb = new StringBuilder();
        if (numero != null) {
            for (char c : numero.toCharArray()) {
                if (Character.isDigit(c)) {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    private static String fin(String chiffres) {
        return chiffres.length() <= CHIFFRES_COMPARES
                ? chiffres
                : chiffres.substring(chiffres.length() - CHIFFRES_COMPARES);
    }
}
