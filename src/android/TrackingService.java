package org.apache.cordova.stepist;

import android.Manifest;
import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.Build;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import ist.hmg.nutrigpt.R;

public class TrackingService extends Service implements SensorEventListener {

    private LocationManager locationManager;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private int stepCount = 0;
    private int distanceTaken = 0;
    private int distanceRemaining = 0;
    private float speed = 0.0f;
    private int calories = 0;
    private int time_elapsed = 0;
    private int time_remaining = 0;
    private int floors_ascended = 0;
    private int floors_descended = 0;

    @Override
    public void onCreate() {
        super.onCreate();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34
            Context context = getApplicationContext();
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // İzin zaten verilmiş
                try {
                    startForeground(1, createNotification("Konum alınıyor...", 0));
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // Daha düşük API'lerde bu izne gerek yok
            try {
                startForeground(1, createNotification("Konum alınıyor...", 0));
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private Notification createNotification(String locationText, int steps) {

        Context context = getApplicationContext();
        // Context context = cordova.getActivity().getApplicationContext();
        // RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
        // R.layout.notificationscreen);
        // remoteViews.setTextViewText(R.id.text_location, locationText);
        // remoteViews.setTextViewText(R.id.text_steps, "Adımlar: " + steps);

        String _steps = context.getString(R.string.stepist_foreground_notification_info_steps);
        String _speed = context.getString(R.string.stepist_foreground_notification_info_speed);
        String _elapsed = context.getString(R.string.stepist_foreground_notification_info_time_elapsed);
        String _remaining = context.getString(R.string.stepist_foreground_notification_info_time_remaining);
        String _distance = context.getString(R.string.stepist_foreground_notification_info_distance_taken);
        String _remaining_distance = context
                .getString(R.string.stepist_foreground_notification_info_distance_remaining);
        String _calories = context.getString(R.string.stepist_foreground_notification_info_calories);

        RemoteViews remoteViewSmall = new RemoteViews(context.getPackageName(), R.layout.notificationscreen);

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_fitness_status);
        // remoteViews.setTextViewText(R.id.text_location, locationText);
        remoteViews.setTextViewText(R.id.text_steps, String.format(_steps, steps));
        // remoteViews.setTextViewText(R.id.text_steps, "Adımlar: " + steps);

        remoteViews.setTextViewText(R.id.text_speed, String.format(_speed, speed));
        remoteViews.setTextViewText(R.id.text_elapsed, String.format(_elapsed, time_elapsed));
        remoteViews.setTextViewText(R.id.text_remaining_time, String.format(_remaining, time_remaining));
        remoteViews.setTextViewText(R.id.text_distance, String.format(_distance, distanceTaken));
        remoteViews.setTextViewText(R.id.text_remaining_distance,
                String.format(_remaining_distance, distanceRemaining));
        remoteViews.setTextViewText(R.id.text_calories, String.format(_calories, calories));

        NotificationChannel channel = new NotificationChannel("track_channel", "Tracking",
                NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(context, "track_channel")
                .setSmallIcon(R.drawable.icon)
                .setCustomContentView(remoteViewSmall)
                .setCustomBigContentView(remoteViews)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();

        // return new NotificationCompat.Builder(this, "track_channel")
        // .setContentTitle("SSS Plus Takip Aktif")
        // .setContentText(locationText + " | Adımlar: " + steps)
        // .setSmallIcon(context.getApplicationInfo().icon)
        // .setOngoing(true)
        // .build();

        return notification;
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            updateNotification(location, stepCount);
        }
    };

    private void updateNotification(Location location, int steps) {
        String locText = "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude();
        Notification notification = createNotification(locText, steps);
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            // ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            // public void onRequestPermissionsResult(int requestCode, String[] permissions,
            // int[] grantResults)
            // to handle the case where the user grants the permission. See the
            // documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        NotificationManagerCompat.from(this).notify(1, notification);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            stepCount = (int) event.values[0];
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                // ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                // public void onRequestPermissionsResult(int requestCode, String[] permissions,
                // int[] grantResults)
                // to handle the case where the user grants the permission. See the
                // documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            updateNotification(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER), stepCount);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void setStepCount(int stepCount) {
        this.stepCount = stepCount;

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            // ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            // public void onRequestPermissionsResult(int requestCode, String[] permissions,
            // int[] grantResults)
            // to handle the case where the user grants the permission. See the
            // documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        updateNotification(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER), stepCount);

    }

}