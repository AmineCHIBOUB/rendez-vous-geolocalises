package fr.enseirb.geordv.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Affichage des notifications.
 * Depuis Android 10, une application ne peut plus ouvrir un écran depuis
 * l'arrière-plan : on affiche donc une notification qui ouvre l'écran au clic.
 */
public final class Notifications {

    private static final String CANAL = "rendezvous";

    private Notifications() {
    }

    public static void afficher(Context context, int id, String titre, String texte, Intent ouverture) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(CANAL, "Rendez-vous",
                    NotificationManager.IMPORTANCE_HIGH);
            canal.setDescription("Invitations, réponses et rappels de rendez-vous");
            manager.createNotificationChannel(canal);
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        ouverture.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, id, ouverture, flags);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CANAL)
                : new Notification.Builder(context);

        builder.setSmallIcon(android.R.drawable.ic_dialog_map)
                .setContentTitle(titre)
                .setContentText(texte)
                .setStyle(new Notification.BigTextStyle().bigText(texte))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setPriority(Notification.PRIORITY_HIGH);
        }

        manager.notify(id, builder.build());
    }
}
