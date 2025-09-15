package org.apache.cordova.stepist;

import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.app.Service;
import android.content.Intent;
import android.hardware.SensorEvent;
import android.os.IBinder;


public class StepService extends Service implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private int lastStepCount = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }


        /*

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
---------------------------------------------------------------------
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        long interval = 5 * 60 * 1000;
        long startTime = System.currentTimeMillis();

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, startTime, interval, pendingIntent);
--------------------------------------------------------------

####################################################################################3

----------------------------------------------------------------------------------------------
AndroidManifest.xml


<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<application
    ... >

    <service android:name=".StepService" />

    <receiver android:name=".AlarmReceiver" />

    <receiver android:name=".BootReceiver">
        <intent-filter>
            <action android:name="android.intent.action.BOOT_COMPLETED" />
        </intent-filter>
    </receiver>

</application>

-------------------------------------------------------------------------------------------------------------

    }
}



        *
        *
        * */



    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int currentSteps = (int) event.values[0];
            if (currentSteps != lastStepCount) {
                lastStepCount = currentSteps;
                StepDatabaseHelper dbHelper = new StepDatabaseHelper(this);
                dbHelper.insertStep(currentSteps);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        sensorManager.unregisterListener(this);
        super.onDestroy();
    }
}
