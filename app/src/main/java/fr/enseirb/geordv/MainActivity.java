package fr.enseirb.geordv;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import fr.enseirb.geordv.data.RdvDatabase;
import fr.enseirb.geordv.model.MessageRdv;
import fr.enseirb.geordv.model.Numeros;
import fr.enseirb.geordv.model.RendezVous;
import fr.enseirb.geordv.util.Cartes;
import fr.enseirb.geordv.util.Rappels;
import fr.enseirb.geordv.util.Sms;

/**
 * Écran de création d'un rendez-vous :
 * - titre, date et heure
 * - un ou plusieurs invités (saisie ou répertoire)
 * - lieu : position GPS actuelle ou coordonnées saisies
 * L'invitation est envoyée par SMS et enregistrée dans l'historique.
 */
public class MainActivity extends Activity {

    private static final int CODE_CHOIX_CONTACT = 1;
    private static final int CODE_PERMISSIONS = 2;

    private EditText editTitre;
    private EditText editNumero;
    private EditText editLatitude;
    private EditText editLongitude;
    private Button boutonDate;
    private Button boutonHeure;
    private TextView textStatut;

    private LocationManager locationManager;
    private Location dernierePosition;
    private LocationListener ecouteurGPS;

    /** Date et heure choisies, ou null si non précisées. */
    private Calendar dateChoisie;
    private boolean heureChoisie;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTitre = findViewById(R.id.editTitre);
        editNumero = findViewById(R.id.editNumero);
        editLatitude = findViewById(R.id.editLatitude);
        editLongitude = findViewById(R.id.editLongitude);
        boutonDate = findViewById(R.id.boutonDate);
        boutonHeure = findViewById(R.id.boutonHeure);
        textStatut = findViewById(R.id.textStatut);

        verifierPermissions();
        initialiserGPS();
    }

    // === PERMISSIONS ===

    private void verifierPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.SEND_SMS);
        permissions.add(Manifest.permission.RECEIVE_SMS);
        permissions.add(Manifest.permission.READ_CONTACTS);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        List<String> manquantes = new ArrayList<>();
        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                manquantes.add(permission);
            }
        }

        if (!manquantes.isEmpty()) {
            requestPermissions(manquantes.toArray(new String[0]), CODE_PERMISSIONS);
        }
    }

    private boolean permissionAccordee(String permission) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean permissionPositionAccordee() {
        return permissionAccordee(Manifest.permission.ACCESS_FINE_LOCATION)
                || permissionAccordee(Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODE_PERMISSIONS) {
            if (permissionPositionAccordee()) {
                initialiserGPS();
            } else {
                textStatut.setText("Certaines permissions ont été refusées");
            }
        }
    }

    // === GPS ===

    private void initialiserGPS() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        ecouteurGPS = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                dernierePosition = location;
                textStatut.setText(String.format(Locale.FRANCE, "📍 Position actuelle : %.5f , %.5f",
                        location.getLatitude(), location.getLongitude()));
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
                textStatut.setText("GPS activé");
            }

            @Override
            public void onProviderDisabled(String provider) {
                textStatut.setText("GPS désactivé : activez-le dans les paramètres");
            }
        };

        if (!permissionPositionAccordee()) {
            textStatut.setText("Permission GPS refusée");
            return;
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, ecouteurGPS);
            // le réseau donne une position approximative plus rapidement, en intérieur notamment
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, ecouteurGPS);
            }
            textStatut.setText("Recherche de votre position…");
        } catch (SecurityException e) {
            textStatut.setText("Permission GPS refusée");
        }
    }

    // === DATE ET HEURE ===

    public void choisirDate(View vue) {
        final Calendar base = dateChoisie != null ? dateChoisie : Calendar.getInstance();
        DatePickerDialog dialogue = new DatePickerDialog(this, (picker, annee, mois, jour) -> {
            if (dateChoisie == null) {
                dateChoisie = Calendar.getInstance();
            }
            dateChoisie.set(annee, mois, jour);
            if (!heureChoisie) {
                dateChoisie.set(Calendar.HOUR_OF_DAY, 12);
                dateChoisie.set(Calendar.MINUTE, 0);
            }
            mettreAJourBoutonsDate();
        }, base.get(Calendar.YEAR), base.get(Calendar.MONTH), base.get(Calendar.DAY_OF_MONTH));
        dialogue.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialogue.show();
    }

    public void choisirHeure(View vue) {
        final Calendar base = dateChoisie != null ? dateChoisie : Calendar.getInstance();
        new TimePickerDialog(this, (picker, heure, minute) -> {
            if (dateChoisie == null) {
                dateChoisie = Calendar.getInstance();
            }
            dateChoisie.set(Calendar.HOUR_OF_DAY, heure);
            dateChoisie.set(Calendar.MINUTE, minute);
            dateChoisie.set(Calendar.SECOND, 0);
            heureChoisie = true;
            mettreAJourBoutonsDate();
        }, base.get(Calendar.HOUR_OF_DAY), base.get(Calendar.MINUTE), true).show();
    }

    private void mettreAJourBoutonsDate() {
        boutonDate.setText("📅 " + new SimpleDateFormat("EEE d MMM", Locale.FRANCE).format(dateChoisie.getTime()));
        if (heureChoisie) {
            boutonHeure.setText("🕒 " + new SimpleDateFormat("HH:mm", Locale.FRANCE).format(dateChoisie.getTime()));
        }
    }

    // === CONTACTS ===

    public void choisirContact(View vue) {
        if (!permissionAccordee(Manifest.permission.READ_CONTACTS)) {
            Toast.makeText(this, "Permission contacts refusée", Toast.LENGTH_SHORT).show();
            verifierPermissions();
            return;
        }
        Intent intentContact = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intentContact, CODE_CHOIX_CONTACT);
    }

    @Override
    protected void onActivityResult(int codeRequete, int codeResultat, Intent data) {
        super.onActivityResult(codeRequete, codeResultat, data);

        if (codeRequete == CODE_CHOIX_CONTACT && codeResultat == RESULT_OK && data != null) {
            Uri uriContact = data.getData();
            if (uriContact == null) {
                return;
            }
            Cursor curseur = getContentResolver().query(uriContact,
                    new String[] {ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null);
            if (curseur != null) {
                if (curseur.moveToFirst()) {
                    ajouterNumeroAuChamp(curseur.getString(0));
                }
                curseur.close();
            }
        }
    }

    private void ajouterNumeroAuChamp(String numero) {
        String numeroNettoye = Numeros.nettoyer(numero);
        String contenuActuel = editNumero.getText().toString().trim();
        editNumero.setText(contenuActuel.isEmpty() ? numeroNettoye : contenuActuel + ";" + numeroNettoye);
    }

    // === ENVOI ===

    public void envoyerInvitation(View vue) {
        if (!permissionAccordee(Manifest.permission.SEND_SMS)) {
            Toast.makeText(this, "Permission SMS refusée", Toast.LENGTH_SHORT).show();
            verifierPermissions();
            return;
        }

        List<String> destinataires = lireDestinataires();
        if (destinataires == null) {
            return;
        }

        double[] lieu = lireLieu();
        if (lieu == null) {
            return;
        }

        if (dateChoisie != null && dateChoisie.getTimeInMillis() < System.currentTimeMillis()) {
            Toast.makeText(this, "La date du rendez-vous est déjà passée", Toast.LENGTH_SHORT).show();
            return;
        }

        RendezVous rdv = new RendezVous(
                UUID.randomUUID().toString().substring(0, 8),
                editTitre.getText().toString().trim(),
                lieu[0], lieu[1],
                dateChoisie != null ? dateChoisie.getTimeInMillis() : 0);
        rdv.setSens(RendezVous.SENS_ENVOYE);

        RdvDatabase db = RdvDatabase.get(this);
        db.enregistrer(rdv);

        String message = MessageRdv.invitation(rdv);
        for (String numero : destinataires) {
            Sms.envoyer(numero, message);
            db.ajouterParticipant(rdv.getId(), numero);
        }
        Rappels.programmer(this, rdv);

        Toast.makeText(this, "Invitation envoyée à " + destinataires.size() + " personne(s)", Toast.LENGTH_SHORT).show();
        reinitialiserFormulaire();

        // on ouvre le suivi des réponses du rendez-vous
        Intent detail = new Intent(this, DetailRdvActivity.class);
        detail.putExtra(DetailRdvActivity.EXTRA_ID_RDV, rdv.getId());
        startActivity(detail);
    }

    /** Numéros saisis, nettoyés et validés, ou null en cas d'erreur. */
    private List<String> lireDestinataires() {
        String saisie = editNumero.getText().toString().trim();
        List<String> destinataires = new ArrayList<>();
        for (String morceau : saisie.split(";")) {
            String numero = Numeros.nettoyer(morceau);
            if (numero.isEmpty()) {
                continue;
            }
            if (!Numeros.estValide(numero)) {
                Toast.makeText(this, "Numéro invalide : " + morceau.trim(), Toast.LENGTH_LONG).show();
                return null;
            }
            if (!destinataires.contains(numero)) {
                destinataires.add(numero);
            }
        }
        if (destinataires.isEmpty()) {
            Toast.makeText(this, "Ajoutez au moins un numéro", Toast.LENGTH_SHORT).show();
            return null;
        }
        return destinataires;
    }

    /** Lieu saisi, sinon position actuelle. Null si indisponible. */
    private double[] lireLieu() {
        String latitudeSaisie = editLatitude.getText().toString().trim();
        String longitudeSaisie = editLongitude.getText().toString().trim();

        if (!latitudeSaisie.isEmpty() || !longitudeSaisie.isEmpty()) {
            if (latitudeSaisie.isEmpty() || longitudeSaisie.isEmpty()) {
                Toast.makeText(this, "Saisissez la latitude ET la longitude", Toast.LENGTH_SHORT).show();
                return null;
            }
            try {
                double latitude = Double.parseDouble(latitudeSaisie);
                double longitude = Double.parseDouble(longitudeSaisie);
                if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                    throw new NumberFormatException();
                }
                return new double[] {latitude, longitude};
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Coordonnées invalides", Toast.LENGTH_SHORT).show();
                return null;
            }
        }

        Location position = positionActuelle();
        if (position == null) {
            Toast.makeText(this, "Position non disponible, patientez quelques secondes", Toast.LENGTH_LONG).show();
            return null;
        }
        return new double[] {position.getLatitude(), position.getLongitude()};
    }

    private Location positionActuelle() {
        if (!permissionPositionAccordee()) {
            verifierPermissions();
            return null;
        }
        if (dernierePosition == null && locationManager != null) {
            try {
                dernierePosition = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (dernierePosition == null) {
                    dernierePosition = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            } catch (SecurityException e) {
                return null;
            }
        }
        return dernierePosition;
    }

    private void reinitialiserFormulaire() {
        editTitre.setText("");
        editNumero.setText("");
        editLatitude.setText("");
        editLongitude.setText("");
        dateChoisie = null;
        heureChoisie = false;
        boutonDate.setText("📅 Date");
        boutonHeure.setText("🕒 Heure");
    }

    // === CARTE ET HISTORIQUE ===

    public void afficherPositionSurCarte(View vue) {
        double[] lieu = lireLieu();
        if (lieu == null) {
            return;
        }
        // le titre vide est remplacé par "Rendez-vous" sur la carte
        Cartes.afficher(this, new RendezVous("", editTitre.getText().toString().trim(), lieu[0], lieu[1], 0));
    }

    public void ouvrirHistorique(View vue) {
        startActivity(new Intent(this, HistoriqueActivity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null && ecouteurGPS != null) {
            try {
                locationManager.removeUpdates(ecouteurGPS);
            } catch (SecurityException e) {
                // rien à faire
            }
        }
    }
}
