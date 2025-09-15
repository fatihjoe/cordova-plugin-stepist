package org.apache.cordova.stepist;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class StepCounterWorker extends Worker implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private float stepCount = -1f;
    private CountDownLatch latch = new CountDownLatch(1);
    private Context _context;

    public StepCounterWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        _context = context;



        /*
///main activity için kod
        PeriodicWorkRequest stepWorkRequest =
                new PeriodicWorkRequest.Builder(StepCounterWorker.class, 10, TimeUnit.MINUTES)
                        .setConstraints(new Constraints.Builder()
                                .setRequiresBatteryNotLow(true)
                                .build())
                        .build();

        WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(
                "StepCounterWork",
                ExistingPeriodicWorkPolicy.KEEP,
                stepWorkRequest
        );



        *
        * */

    }

    @NonNull
    @Override
    public Result doWork() {
        sensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepSensor == null) return ListenableWorker.Result.failure();

        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);

        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return ListenableWorker.Result.retry();
        }

        sensorManager.unregisterListener(this);

        if (stepCount >= 0) {
            saveToDatabase(stepCount);
            return Result.success();
        }

        return Result.retry();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        stepCount = event.values[0];
        latch.countDown();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void saveToDatabase(float steps) {
        StepDbHelper dbHelper = new StepDbHelper(getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("timestamp", System.currentTimeMillis());
        values.put("steps", steps);

        db.insert("step_data", null, values);
        db.close();
    }
}


