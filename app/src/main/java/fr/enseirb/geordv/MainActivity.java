package fr.enseirb.geordv;

import android.Manifest;
import android.app.Activity;
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
import android.telephony.SmsManager;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activité principale de l'application GeoRDV.
 *
 * Version simple, proche du cours :
 * - saisie manuelle d'un ou plusieurs numéros
 * - choix d'un contact du répertoire
 * - récupération des coordonnées GPS actuelles
 * - affichage sur une carte via Intent ACTION_VIEW + URI geo:
 * - version v3 : possibilité de fixer un autre lieu de rendez-vous
 *   en saisissant une latitude et une longitude.
 */
public class MainActivity extends Activity {

    private static final int CODE_CHOIX_CONTACT = 1;
    private static final int CODE_PERMISSIONS = 2;

    private EditText editNumero;
    private EditText editLatitude;
    private EditText editLongitude;
    private TextView textStatut;

    private LocationManager locationManager;
    private Location dernierePosition;
    private LocationListener ecouteurGPS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editNumero = (EditText) findViewById(R.id.editNumero);
        editLatitude = (EditText) findViewById(R.id.editLatitude);
        editLongitude = (EditText) findViewById(R.id.editLongitude);
        textStatut = (TextView) findViewById(R.id.textStatut);

        verifierPermissions();
        initialiserGPS();
    }

    private void verifierPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        String[] permissions = new String[] {
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        boolean manqueUnePermission = false;

        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                manqueUnePermission = true;
                break;
            }
        }

        if (manqueUnePermission) {
            requestPermissions(permissions, CODE_PERMISSIONS);
        }
    }

    private void initialiserGPS() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        ecouteurGPS = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                dernierePosition = location;
                textStatut.setText("GPS actuel : " + location.getLatitude() + " , " + location.getLongitude());
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
                textStatut.setText("GPS désactivé - activez-le dans les paramètres");
            }
        };

        if (!permissionPositionAccordee()) {
            textStatut.setText("Permission GPS refusée");
            return;
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, ecouteurGPS);
            textStatut.setText("Recherche du signal GPS...");
        } catch (SecurityException e) {
            textStatut.setText("Permission GPS refusée");
        }
    }

    public void choisirContact(View vue) {
        if (!permissionContactsAccordee()) {
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
            Cursor curseur = getContentResolver().query(
                    uriContact,
                    new String[] {ContactsContract.CommonDataKinds.Phone.NUMBER},
                    null,
                    null,
                    null
            );

            if (curseur != null) {
                if (curseur.moveToFirst()) {
                    String numero = curseur.getString(0);
                    ajouterNumeroAuChamp(numero);
                }
                curseur.close();
            }
        }
    }

    /**
     * V1 + V3 :
     * - si latitude/longitude sont saisies, elles définissent le lieu du rendez-vous
     * - sinon, on utilise la position GPS actuelle de l'initiateur
     */
    public void envoyerInvitation(View vue) {
        if (!permissionSmsAccordee()) {
            Toast.makeText(this, "Permission SMS refusée", Toast.LENGTH_SHORT).show();
            verifierPermissions();
            return;
        }

        String saisie = editNumero.getText().toString().trim();

        if (saisie.length() == 0) {
            Toast.makeText(this, "Veuillez saisir au moins un numéro", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] numeros = saisie.split(";");
        int nbDestinataires = 0;

        for (int i = 0; i < numeros.length; i++) {
            String numeroNettoye = nettoyerNumero(numeros[i]);
            if (numeroNettoye.length() == 0) {
                continue;
            }
            if (!numeroValide(numeroNettoye)) {
                Toast.makeText(this, "Numéro invalide : " + numeros[i].trim(), Toast.LENGTH_LONG).show();
                return;
            }
            numeros[i] = numeroNettoye;
            nbDestinataires++;
        }

        if (nbDestinataires == 0) {
            Toast.makeText(this, "Veuillez saisir au moins un numéro valide", Toast.LENGTH_SHORT).show();
            return;
        }

        String coordonnees = recupererCoordonneesDuRendezVous();
        if (coordonnees == null) {
            return;
        }

        String messageSms = "RDV_INVITE:" + coordonnees;
        SmsManager smsManager = SmsManager.getDefault();
        int envoyes = 0;

        for (String numero : numeros) {
            if (numero.length() == 0) {
                continue;
            }
            smsManager.sendTextMessage(numero, null, messageSms, null, null);
            envoyes++;
        }

        Toast.makeText(this, "Invitation envoyée à " + envoyes + " destinataire(s)", Toast.LENGTH_SHORT).show();
        textStatut.setText("Invitation envoyée\n" + messageSms);
    }

    /**
     * V2 : affiche le lieu du rendez-vous sur une carte.
     * Si un lieu manuel est saisi, on l'affiche.
     * Sinon, on affiche la position GPS actuelle.
     */
    public void afficherPositionSurCarte(View vue) {
        String coordonnees = recupererCoordonneesSansEnvoi();
        if (coordonnees == null) {
            return;
        }

        String[] morceaux = coordonnees.split(",");
        if (morceaux.length != 2) {
            Toast.makeText(this, "Coordonnées invalides", Toast.LENGTH_SHORT).show();
            return;
        }

        String latitude = morceaux[0].trim();
        String longitude = morceaux[1].trim();
        Uri position = Uri.parse("geo:" + latitude + "," + longitude);

        Intent intentCarte = new Intent(Intent.ACTION_VIEW);
        intentCarte.setData(position);

        try {
            startActivity(intentCarte);
        } catch (Exception e) {
            Toast.makeText(this, "Aucune application de carte disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private String recupererCoordonneesDuRendezVous() {
        String latitudeSaisie = editLatitude.getText().toString().trim();
        String longitudeSaisie = editLongitude.getText().toString().trim();

        if (latitudeSaisie.length() > 0 || longitudeSaisie.length() > 0) {
            if (latitudeSaisie.length() == 0 || longitudeSaisie.length() == 0) {
                Toast.makeText(this, "Saisissez la latitude ET la longitude", Toast.LENGTH_SHORT).show();
                return null;
            }

            if (!coordonneesValides(latitudeSaisie, longitudeSaisie)) {
                Toast.makeText(this, "Coordonnées manuelles invalides", Toast.LENGTH_SHORT).show();
                return null;
            }

            textStatut.setText("Lieu du rendez-vous choisi manuellement :\n" + latitudeSaisie + " , " + longitudeSaisie);
            return latitudeSaisie + "," + longitudeSaisie;
        }

        return recupererCoordonneesGPS();
    }

    private String recupererCoordonneesSansEnvoi() {
        String latitudeSaisie = editLatitude.getText().toString().trim();
        String longitudeSaisie = editLongitude.getText().toString().trim();

        if (latitudeSaisie.length() > 0 || longitudeSaisie.length() > 0) {
            if (latitudeSaisie.length() == 0 || longitudeSaisie.length() == 0) {
                Toast.makeText(this, "Saisissez la latitude ET la longitude", Toast.LENGTH_SHORT).show();
                return null;
            }

            if (!coordonneesValides(latitudeSaisie, longitudeSaisie)) {
                Toast.makeText(this, "Coordonnées manuelles invalides", Toast.LENGTH_SHORT).show();
                return null;
            }

            return latitudeSaisie + "," + longitudeSaisie;
        }

        return recupererCoordonneesGPS();
    }

    private String recupererCoordonneesGPS() {
        if (!permissionPositionAccordee()) {
            Toast.makeText(this, "Permission GPS refusée", Toast.LENGTH_SHORT).show();
            verifierPermissions();
            return null;
        }

        if (dernierePosition == null) {
            try {
                dernierePosition = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (dernierePosition == null) {
                    dernierePosition = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            } catch (SecurityException e) {
                Toast.makeText(this, "Permission GPS refusée", Toast.LENGTH_SHORT).show();
                return null;
            }
        }

        if (dernierePosition == null) {
            Toast.makeText(this, "Position GPS non disponible. Patientez...", Toast.LENGTH_LONG).show();
            return null;
        }

        String latitude = String.valueOf(dernierePosition.getLatitude());
        String longitude = String.valueOf(dernierePosition.getLongitude());
        return latitude + "," + longitude;
    }

    private boolean coordonneesValides(String latitudeTexte, String longitudeTexte) {
        try {
            double latitude = Double.parseDouble(latitudeTexte);
            double longitude = Double.parseDouble(longitudeTexte);
            return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void ajouterNumeroAuChamp(String numero) {
        String numeroNettoye = nettoyerNumero(numero);
        String contenuActuel = editNumero.getText().toString().trim();

        if (contenuActuel.length() == 0) {
            editNumero.setText(numeroNettoye);
        } else {
            editNumero.setText(contenuActuel + ";" + numeroNettoye);
        }
    }

    private String nettoyerNumero(String numero) {
        return numero.replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");
    }

    private boolean numeroValide(String numero) {
        int nbChiffres = 0;

        for (int i = 0; i < numero.length(); i++) {
            if (Character.isDigit(numero.charAt(i))) {
                nbChiffres++;
            }
        }

        return nbChiffres >= 4;
    }

    private boolean permissionSmsAccordee() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean permissionContactsAccordee() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean permissionPositionAccordee() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (locationManager != null && ecouteurGPS != null) {
            try {
                locationManager.removeUpdates(ecouteurGPS);
            } catch (SecurityException e) {
            }
        }
    }
}
