package fr.enseirb.geordv.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class NumerosEtTrajetTest {

    // === NUMÉROS ===

    @Test
    public void unNumeroEstNettoye() {
        assertEquals("0601020304", Numeros.nettoyer("06 01-02.03 04"));
        assertEquals("+33601020304", Numeros.nettoyer("(+33) 6 01 02 03 04"));
    }

    @Test
    public void lesFormatsNationalEtInternationalSontLeMemeNumero() {
        assertTrue(Numeros.memeNumero("0601020304", "+33601020304"));
        assertTrue(Numeros.memeNumero("06 01 02 03 04", "+33 6 01 02 03 04"));
    }

    @Test
    public void deuxNumerosDifferentsNeSontPasConfondus() {
        assertFalse(Numeros.memeNumero("0601020304", "0601020305"));
        assertFalse(Numeros.memeNumero("", "0601020304"));
        assertFalse(Numeros.memeNumero(null, "0601020304"));
    }

    @Test
    public void unNumeroTropCourtEstInvalide() {
        assertFalse(Numeros.estValide("12"));
        assertTrue(Numeros.estValide("0601020304"));
    }

    // === TRAJET ===

    @Test
    public void laDistanceEntreDeuxPointsIdentiquesEstNulle() {
        assertEquals(0, Trajet.distanceMetres(44.84, -0.58, 44.84, -0.58), 1e-6);
    }

    @Test
    public void laDistanceBordeauxParisEstCorrecte() {
        // Bordeaux -> Paris : environ 500 km à vol d'oiseau
        double distance = Trajet.distanceMetres(44.8378, -0.5792, 48.8566, 2.3522);
        assertEquals(500_000, distance, 10_000);
    }

    @Test
    public void leTempsAPiedEstArrondiALaMinuteSuperieure() {
        assertEquals(12, Trajet.minutesAPied(1000)); // 1 km à 5 km/h = 12 min
        assertEquals(1, Trajet.minutesAPied(10));
    }

    @Test
    public void laDistanceEstFormateeEnMetresOuKilometres() {
        assertEquals("850 m", Trajet.formaterDistance(850));
        assertEquals("12,4 km", Trajet.formaterDistance(12_400));
    }
}
