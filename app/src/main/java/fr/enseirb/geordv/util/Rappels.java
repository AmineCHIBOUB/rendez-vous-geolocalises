package fr.enseirb.geordv.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import fr.enseirb.geordv.RappelReceiver;
import fr.enseirb.geordv.model.RendezVous;

/**
 * Programme une notification de rappel 30 minutes avant un rendez-vous.
 * Le rappel est approximatif (quelques minutes) pour ne pas demander
 * la permission des alarmes exactes.
 */
public final class Rappels {

    public static final long AVANCE_MILLIS = 30 * 60 * 1000L;
    public static final String EXTRA_ID_RDV = "id_rdv";

    private Rappels() {
    }

    public static void programmer(Context context, RendezVous rdv) {
        if (!rdv.aUneDate()) {
            return;
        }
        long declenchement = rdv.getDateHeure() - AVANCE_MILLIS;
        if (declenchement <= System.currentTimeMillis()) {
            return;
        }
        AlarmManager alarmes = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmes == null) {
            return;
        }
        PendingIntent pi = pendingIntent(context, rdv.getId());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmes.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, declenchement, pi);
        } else {
            alarmes.set(AlarmManager.RTC_WAKEUP, declenchement, pi);
        }
    }

    public static void annuler(Context context, String idRdv) {
        AlarmManager alarmes = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmes != null) {
            alarmes.cancel(pendingIntent(context, idRdv));
        }
    }

    private static PendingIntent pendingIntent(Context context, String idRdv) {
        Intent intent = new Intent(context, RappelReceiver.class);
        intent.putExtra(EXTRA_ID_RDV, idRdv);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, idRdv.hashCode(), intent, flags);
    }
}
