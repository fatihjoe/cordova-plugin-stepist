package org.apache.cordova.stepist;

import static org.apache.cordova.stepist.TrackingService.STEPIST_CHANNEL_ID;
import static org.apache.cordova.stepist.StepIstListener.STEPIST_SOUND_ON;
import static org.apache.cordova.stepist.StepIstListener.STEPIST_SOUND_RERMINDER_FREQUENCY;

import android.Manifest;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.widget.RemoteViews;

//import androidx.core.app.ActivityCompat;
//import androidx.core.app.NotificationCompat;
//import androidx.core.app.NotificationManagerCompat;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ist.hmg.nutrigpt.R;


public class NotificationActionReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationActionReceiverForStepIst";

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if ("org.apache.cordova.stepist.BUTTON1_CLICKED".equals(action)) {
            // Handle the button click here

            Log.d(TAG, "onReceive: BUTTON1_CLICKED");

            speak(context, "Takip başladı" );

            return;
        }

        if ("org.apache.cordova.stepist.BUTTON2_CLICKED".equals(action)) {
            // Handle the button click here
            Log.d(TAG, "onReceive: BUTTON2_CLICKED");

            speak( context, "Takip durdu");

            return;
        }

        if ("org.apache.cordova.stepist.BUTTON3_CLICKED".equals(action)) {
            // Handle the button click here

            Log.d(TAG, "onReceive: BUTTON3_CLICKED");

            return;
        }

        if ("org.apache.cordova.stepist.BUTTON4_CLICKED".equals(action)) {
            // Handle the button click here

            Log.d(TAG, "onReceive: BUTTON4_CLICKED");

            return;
        }

        if ("org.apache.cordova.stepist.smallViewSoundIcon".equals(action)) {
            // Handle the button click here
            Log.d(TAG, "onReceive: org.apache.cordova.stepist.smallViewSoundIcon");
            //updateNotification(context);
            if (STEPIST_SOUND_ON == 0) {
                STEPIST_SOUND_ON = 1;

                speak( context, "Sesli uyarı açık");

            } else {
                STEPIST_SOUND_ON = 0;

                speak( context, "Sesli uyarı kapalı");

            }

            return;
        }

        Log.d(TAG, "onReceive: XXX");
    }

    private void updateNotification(Context context) {
        /*
        RemoteViews updatedViews = new RemoteViews(context.getPackageName(), R.layout.notification_layout);
        updatedViews.setTextViewText(R.id.your_button_id, "Clicked!");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "your_channel_id")
                .setSmallIcon(R.drawable.ic_notification)
                .setContent(updatedViews)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(1001, builder.build());

*/
/*
        RemoteViews remoteViewSmall = new RemoteViews(context.getPackageName(), R.layout.notificationscreen);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_fitness_status);

        remoteViewSmall.setImageViewResource(R.id.smallViewSoundIcon, R.drawable.sound_onx48);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, STEPIST_CHANNEL_ID)
                .setSmallIcon(R.drawable.icon)
                .setCustomContentView(remoteViewSmall)
                .setCustomBigContentView(remoteViews)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setOnlyAlertOnce(true);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        List<StatusBarNotification> notifications = NotificationManagerCompat.from(context).getActiveNotifications();

        StatusBarNotification statusBarNotification = notifications.getFirst();
        Notification notification = statusBarNotification.getNotification();
*/
        //notification.

        //NotificationManagerCompat.from(context).notify(1001, builder.build());

        //NotificationManagerCompat.from(context).notify(1001, notifications.get);

    }

private  void speak(Context context, String message ){



    Intent serviceIntent = new Intent(context, SpeakService.class);
    serviceIntent.putExtra("message", message);
    context.startService(serviceIntent);

}




}



