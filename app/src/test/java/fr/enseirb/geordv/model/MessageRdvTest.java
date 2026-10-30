package fr.enseirb.geordv.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class MessageRdvTest {

    private RendezVous exemple() {
        return new RendezVous("ab12cd34", "Café", 44.8378, -0.5792, 1_800_000_000_000L);
    }

    // === INVITATIONS ===

    @Test
    public void uneInvitationEcritePuisRelueEstIdentique() {
        RendezVous lu = MessageRdv.lireInvitation(MessageRdv.invitation(exemple()));

        assertNotNull(lu);
        assertEquals("ab12cd34", lu.getId());
        assertEquals("Café", lu.getTitre());
        assertEquals(44.8378, lu.getLatitude(), 1e-9);
        assertEquals(-0.5792, lu.getLongitude(), 1e-9);
        assertEquals(1_800_000_000_000L, lu.getDateHeure());
    }

    @Test
    public void leSeparateurEstRetireDuTitre() {
        RendezVous rdv = new RendezVous("id", "Réunion | projet", 1, 2, 0);
        RendezVous lu = MessageRdv.lireInvitation(MessageRdv.invitation(rdv));

        assertEquals("Réunion / projet", lu.getTitre());
    }

    @Test
    public void uneInvitationSansTitreEstLue() {
        RendezVous rdv = new RendezVous("id", "", 1, 2, 0);
        RendezVous lu = MessageRdv.lireInvitation(MessageRdv.invitation(rdv));

        assertNotNull(lu);
        assertEquals("", lu.getTitre());
        assertFalse(lu.aUneDate());
    }

    @Test
    public void uneInvitationDeLAncienneVersionEstToujoursComprise() {
        RendezVous lu = MessageRdv.lireInvitation("RDV_INVITE:44.8378,-0.5792");

        assertNotNull(lu);
        assertEquals(MessageRdv.ID_INCONNU, lu.getId());
        assertEquals(44.8378, lu.getLatitude(), 1e-9);
    }

    @Test
    public void desCoordonneesInvalidesSontRejetees() {
        assertNull(MessageRdv.lireInvitation("RDV_INVITE:abc,def"));
        assertNull(MessageRdv.lireInvitation("RDV_INVITE:95,10"));
        assertNull(MessageRdv.lireInvitation("RDV_INVITE:2|id|10,200|0|titre"));
        assertNull(MessageRdv.lireInvitation("Bonjour"));
    }

    // === RÉPONSES ===

    @Test
    public void uneReponseAvecMessageEstLue() {
        String sms = MessageRdv.reponse("ab12cd34", RendezVous.STATUT_ACCEPTE, "J'arrive dans 10 min");
        MessageRdv.Reponse reponse = MessageRdv.lireReponse(sms);

        assertEquals("ab12cd34", reponse.idRdv);
        assertTrue(reponse.estAcceptee());
        assertEquals("J'arrive dans 10 min", reponse.message);
    }

    @Test
    public void uneReponseSansMessageEstLue() {
        MessageRdv.Reponse reponse = MessageRdv.lireReponse(
                MessageRdv.reponse("id", RendezVous.STATUT_REFUSE, ""));

        assertEquals(RendezVous.STATUT_REFUSE, reponse.statut);
        assertEquals("", reponse.message);
    }

    @Test
    public void uneReponseDeLAncienneVersionEstComprise() {
        MessageRdv.Reponse reponse = MessageRdv.lireReponse("RDV_REPONSE:ACCEPTE");

        assertEquals(MessageRdv.ID_INCONNU, reponse.idRdv);
        assertTrue(reponse.estAcceptee());
    }

    // === ANNULATION ===

    @Test
    public void uneAnnulationEstLue() {
        assertEquals("ab12cd34", MessageRdv.lireAnnulation(MessageRdv.annulation("ab12cd34")));
        assertNull(MessageRdv.lireAnnulation("RDV_INVITE:1,2"));
    }

    @Test
    public void lesTypesDeMessagesSontReconnus() {
        assertTrue(MessageRdv.estInvitation(MessageRdv.invitation(exemple())));
        assertTrue(MessageRdv.estReponse(MessageRdv.reponse("id", "ACCEPTE", "")));
        assertTrue(MessageRdv.estAnnulation(MessageRdv.annulation("id")));
        assertFalse(MessageRdv.estInvitation(null));
    }
}
