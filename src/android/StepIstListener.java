package org.apache.cordova.stepist;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

//import javax.swing.SpringLayout.Constraints;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.cordova.stepist.StepIstDetector;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;

import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.fitness.FitnessLocal;
import com.google.android.gms.fitness.LocalRecordingClient;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.LocalDataPoint;
import com.google.android.gms.fitness.data.LocalDataSet;
import com.google.android.gms.fitness.data.LocalDataType;
import com.google.android.gms.fitness.data.LocalField;
import com.google.android.gms.fitness.request.LocalDataReadRequest;

/**
 * This class listens to the pedometer sensor
 */
public class StepIstListener extends CordovaPlugin implements SensorEventListener {

    private static final int REQUEST_FOREGROUND_LOCATION_PERMISSION = 1001;

    private String TAG = "recording api";

    public static int STEPIST_SOUND_ON = 0;
    public static long STEPIST_SOUND_RERMINDER_FREQUENCY = 1;
    public static long STEPIST_SOUND_RERMINDER_LAST_TIMESTAMP = 0;
    public static long STEPIST_MAX_SPEED_LIMIT_FOR_WARNING = 6;
    public static long STEPIST_MIN_SPEED_LIMIT_FOR_WARNING = 3;

    public static int STOPPED = 0;
    public static int STARTING = 1;
    public static int RUNNING = 2;
    public static int ERROR_FAILED_TO_START = 3;
    public static int ERROR_NO_SENSOR_FOUND = 4;
    public static int RECORDING_API = 5;

    private int status; // status of listener
    private float startsteps; // first value, to be substracted
    private long starttimestamp; // time stamp of when the measurement starts

    private SensorManager sensorManager; // Sensor manager
    private Sensor mSensor; // Pedometer sensor returned by sensor manager

    private CallbackContext callbackContext; // Keeps track of the JS callback context.

    private Handler mainHandler = null;

    private LocalRecordingClient localRecordingClient = null;

    /// ////////////////////////////////////////////////////////////////////////////
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private float previousSteps = 0.0f;
    private long previousTime = 0;
    private Sensor stepDetectorSensor;
    private StepIstDetector stepIstDetector;
    ///////////////////////////////////////////////////////////////////////////////////////

    /**
     * Constructor
     */
    public StepIstListener() {
        this.starttimestamp = 0;
        this.startsteps = 0;
        this.setStatus(StepIstListener.STOPPED);
    }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova the context of the main Activity.
     * @param webView the associated CordovaWebView.
     */
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        AppCompatActivity activity = cordova.getActivity();

        this.sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        this.stepIstDetector = new StepIstDetector(cordova, webView);
        this.stepIstDetector.setStarttimestamp(this.starttimestamp);
        this.stepIstDetector.setStartsteps(this.startsteps);

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        Intent intent = new Intent(activity, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(activity, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        long interval = 5 * 1000;// 5 * 60 * 1000;
        long startTime = System.currentTimeMillis();

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, startTime, interval, pendingIntent);

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        PeriodicWorkRequest stepWorkRequest = new PeriodicWorkRequest.Builder(StepCounterWorker.class, 5,
                TimeUnit.MINUTES)
                .setConstraints(new Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build())
                .build();

        WorkManager.getInstance(activity.getApplicationContext()).enqueueUniquePeriodicWork(
                "StepCounterWork",
                ExistingPeriodicWorkPolicy.KEEP,
                stepWorkRequest);

    }

    /**
     * Executes the request.
     *
     * @param action          the action to execute.
     * @param args            the exec() arguments.
     * @param callbackContext the callback context used when calling back into
     *                        JavaScript.
     * @return whether the action was valid.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
        this.stepIstDetector.callbackContext = callbackContext;

        if (action.equals("audible_warning")) {
            setAudibleWarning(args);
            return true;
        }
        if (action.equals("audible_reminder_frequency")) {
            setAudibleReminderFrequency(args);
            return true;
        }
        if (action.equals("max_speed_limit_for_warning")) {
            setMaxSpeedLimitForWarning(args);
            return true;
        }
        if (action.equals("min_speed_limit_for_warning")) {
            setMinSpeedLimitForWarning(args);
            return true;
        }

        switch (action) {
            case "updateSetting":
                updateSetting(args, callbackContext);
                return true;
            case "getSetting":
                getSetting(args, callbackContext);
                return true;
        }

        if (action.equals("recordingAPI")) {
            String ar = Manifest.permission.ACTIVITY_RECOGNITION;
            this.localRecordingClient = FitnessLocal.getLocalRecordingClient(cordova.getActivity());
            if (cordova.getContext().checkSelfPermission(ar) == PackageManager.PERMISSION_GRANTED) {
                Log.i(this.TAG, "izin verilmiş!");
                RecApi();
                return true;
            }
            cordova.getActivity().requestPermissions(new String[]{ar}, 0);
            Log.i(this.TAG, "izin yok!");
            if (cordova.getContext().checkSelfPermission(ar) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            Log.i(this.TAG, "izin verilmiş!");
            RecApi();
            return true;
        }

        if (action.equals("startNotificationTracking")) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34

                Context context = cordova.getContext();

                if (ContextCompat.checkSelfPermission(context,
                        Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(cordova.getActivity(),
                            new String[]{Manifest.permission.FOREGROUND_SERVICE_LOCATION},
                            REQUEST_FOREGROUND_LOCATION_PERMISSION);
                } else {
                    // İzin zaten verilmiş
                    try {

                        Intent serviceIntent = new Intent(cordova.getActivity(), TrackingService.class);
                        ContextCompat.startForegroundService(cordova.getActivity(), serviceIntent);

                        return true;
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            }

            Intent serviceIntent = new Intent(cordova.getActivity(), TrackingService.class);
            ContextCompat.startForegroundService(cordova.getActivity(), serviceIntent);

            return true;
        }

        if (action.equals("isStepCountingAvailable")) {
            List<Sensor> list = this.sensorManager.getSensorList(Sensor.TYPE_STEP_COUNTER);
            if ((list != null) && (!list.isEmpty())) {
                this.win(true);
                return true;
            } else {
                this.setStatus(StepIstListener.ERROR_NO_SENSOR_FOUND);
                this.win(false);
                return true;
            }
        } else if (action.equals("isDistanceAvailable")) {
            // distance is never available in Android
            this.win(false);
            return true;
        } else if (action.equals("isFloorCountingAvailable")) {
            // floor counting is never available in Android
            this.win(false);
            return true;
        } else if (action.equals("startStepIstUpdates")) {
            if (this.status != StepIstListener.RUNNING) {
                // If not running, then this is an async call, so don't worry about waiting
                // We drop the callback onto our stack, call start, and let start and the sensor
                // callback fire off the callback down the road
                this.start();
            }
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT, "");
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return true;
        } else if (action.equals("stopStepIstUpdates")) {
            if (this.status == StepIstListener.RUNNING) {
                this.stop();
            }
            this.win(null);
            return true;
        } else {
            // Unsupported action
            return false;
        }
    }

    /**
     * Called by the Broker when listener is to be shut down.
     * Stop listener.
     */
    public void onDestroy() {
        this.stop();
    }

    /**
     * Start listening for pedometers sensor.
     */
    private void start() {
        // If already starting or running, then return
        if ((this.status == StepIstListener.RUNNING) || (this.status == StepIstListener.STARTING)) {
            return;
        }

        starttimestamp = System.currentTimeMillis();
        this.startsteps = 0;
        this.setStatus(StepIstListener.STARTING);

        // Get pedometer from sensor manager
        List<Sensor> list = this.sensorManager.getSensorList(Sensor.TYPE_STEP_COUNTER);

        // If found, then register as listener
        if ((list != null) && (list.size() > 0)) {
            this.mSensor = list.get(0);
            boolean isSupported = this.sensorManager.registerListener(this, this.mSensor,
                    SensorManager.SENSOR_DELAY_FASTEST);
            if (isSupported) {
                this.setStatus(StepIstListener.STARTING);
            } else {
                this.setStatus(StepIstListener.ERROR_FAILED_TO_START);
                this.fail(StepIstListener.ERROR_FAILED_TO_START, "Device sensor returned an error.");
                return;
            }
            ;
        } else {
            this.setStatus(StepIstListener.ERROR_FAILED_TO_START);
            this.fail(StepIstListener.ERROR_FAILED_TO_START, "No sensors found to register step counter listening to.");
            return;
        }
    }

    /**
     * Stop listening to sensor.
     */
    private void stop() {
        if (this.status != StepIstListener.STOPPED) {
            this.sensorManager.unregisterListener(this);
        }
        this.setStatus(StepIstListener.STOPPED);
    }

    /**
     * Called when the accuracy of the sensor has changed.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // nothing to do here
        return;
    }

    /**
     * Sensor listener event.
     *
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Only look at step counter events
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) {
            return;
        }

        // If not running, then just return
        if (this.status == StepIstListener.STOPPED) {
            return;
        }
        this.setStatus(StepIstListener.RUNNING);

        float steps = event.values[0];

        if (this.startsteps == 0)
            this.startsteps = steps;

        steps = steps - this.startsteps;

        this.win(this.getStepsJSON(steps));
    }

    /**
     * Called when the view navigates.
     */
    @Override
    public void onReset() {
        if (this.status == StepIstListener.RUNNING) {
            this.stop();
        }
    }

    // Sends an error back to JS
    private void fail(int code, String message) {
        // Error object
        JSONObject errorObj = new JSONObject();
        try {
            errorObj.put("code", code);
            errorObj.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PluginResult err = new PluginResult(PluginResult.Status.ERROR, errorObj);
        err.setKeepCallback(true);
        callbackContext.sendPluginResult(err);
    }

    private void win(JSONObject message) {
        // Success return object
        PluginResult result;
        if (message != null)
            result = new PluginResult(PluginResult.Status.OK, message);
        else
            result = new PluginResult(PluginResult.Status.OK);

        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
    }

    private void win(boolean success) {
        // Success return object
        PluginResult result;
        result = new PluginResult(PluginResult.Status.OK, success);

        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
    }

    private void RecApi() {
        float steps = 0;

        ZonedDateTime endTime = LocalDateTime.now().atZone(ZoneId.systemDefault());
        ZonedDateTime startTime = endTime.minusDays(10);

        LocalDataReadRequest readRequest = new LocalDataReadRequest.Builder()
                .aggregate(LocalDataType.TYPE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(
                        startTime.toEpochSecond(),
                        endTime.toEpochSecond(),
                        TimeUnit.SECONDS)
                .build();

        localRecordingClient = FitnessLocal.getLocalRecordingClient(cordova.getActivity());

        localRecordingClient.readData(readRequest)
                .addOnSuccessListener(response -> {

                    LocalDataSet localDataSet = response.getDataSet(LocalDataType.TYPE_STEP_COUNT_DELTA);

                    for (LocalDataPoint dataPoint : localDataSet.getDataPoints()) {
                        dumpDataPoint(dataPoint);
                    }

                    /*
                     * for (Bucket bucket : response.getBuckets()) {
                     * for (DataSet dataSet : bucket.getDataSets()) {
                     * dumpDataSet(dataSet);
                     * }
                     * }
                     */

                })
                .addOnFailureListener(e -> Log.w(TAG, "There was an error reading data", e));

        JSONObject message = getStepsJSON(steps);
        // Success return object

        PluginResult result;
        if (message != null)
            result = new PluginResult(PluginResult.Status.OK, message);
        else
            result = new PluginResult(PluginResult.Status.OK);

        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
    }

    void dumpDataPoint(LocalDataPoint dp) {

        Log.i(TAG, "Data point:");
        Log.i(TAG, "\tType: " + dp.getDataType().getName());
        Log.i(TAG, "\tStart: " + dp.getStartTime(TimeUnit.HOURS));
        Log.i(TAG, "\tEnd: " + dp.getEndTime(TimeUnit.HOURS));
        for (LocalField field : dp.getDataType().getFields()) {
            Log.i(TAG, "\tLocalField: " + field.getName() + " LocalValue: " + dp.getValue(field));
        }

    }

    /*
     * void dumpDataSet(LocalDataSet dataSet) {
     * Log.i(TAG, "Data returned for Data type: " +
     * dataSet.getDataType().getName());
     * for (DataPoint dp : dataSet.getDataPoints()) {
     * Log.i(TAG, "Data point:");
     * Log.i(TAG, "\tType: " + dp.getDataType().getName());
     * Log.i(TAG, "\tStart: " + dp.getStartTime(TimeUnit.HOURS));
     * Log.i(TAG, "\tEnd: " + dp.getEndTime(TimeUnit.HOURS));
     * for (Field field : dp.getDataType().getFields()) {
     * Log.i(TAG, "\tLocalField: " + field.getName() + " LocalValue: " +
     * dp.getValue(field));
     * }
     * }
     * }
     */
    private void setStatus(int status) {
        this.status = status;
    }

    private void setAudibleWarning(final JSONArray args) {
        try {
            if (!args.getJSONObject(0).has("audible_warning")) {
                callbackContext.error("Missing argument audible_warning");
                return;
            }

            int audible_warning = args.getJSONObject(0).getInt("audible_warning");

            STEPIST_SOUND_ON = audible_warning;

            Log.d(TAG, "STEPIST_SOUND_ON : " + STEPIST_SOUND_ON);

        } catch (JSONException ex) {
            Log.e(TAG, "Could not parse query object or write response object", ex);
            callbackContext.error("Could not parse query object or write response object");
        }
    }

    private void setAudibleReminderFrequency(final JSONArray args) {
        try {
            if (!args.getJSONObject(0).has("audible_reminder_frequency")) {
                callbackContext.error("Missing argument audible_reminder_frequency");
                return;
            }

            long audible_reminder_frequency = args.getJSONObject(0).getLong("audible_reminder_frequency");

            STEPIST_SOUND_RERMINDER_FREQUENCY = audible_reminder_frequency;

            Log.d(TAG, "STEPIST_SOUND_ON : " + STEPIST_SOUND_RERMINDER_FREQUENCY);

        } catch (JSONException ex) {
            Log.e(TAG, "Could not parse query object or write response object", ex);
            callbackContext.error("Could not parse query object or write response object");
        }
    }

    private void setMaxSpeedLimitForWarning(final JSONArray args) {
        try {
            if (!args.getJSONObject(0).has("max_speed_limit_for_warning")) {
                callbackContext.error("Missing argument max_speed_limit_for_warning");
                return;
            }

            long max_speed_limit_for_warning = args.getJSONObject(0).getLong("max_speed_limit_for_warning");

            STEPIST_MAX_SPEED_LIMIT_FOR_WARNING = max_speed_limit_for_warning;

            Log.d(TAG, "STEPIST_SOUND_ON : " + STEPIST_SOUND_RERMINDER_FREQUENCY);

        } catch (JSONException ex) {
            Log.e(TAG, "Could not parse query object or write response object", ex);
            callbackContext.error("Could not parse query object or write response object");
        }
    }

    private void setMinSpeedLimitForWarning(final JSONArray args) {
        try {
            if (!args.getJSONObject(0).has("min_speed_limit_for_warning")) {
                callbackContext.error("Missing argument min_speed_limit_for_warning");
                return;
            }

            long min_speed_limit_for_warning = args.getJSONObject(0).getLong("min_speed_limit_for_warning");

            STEPIST_MIN_SPEED_LIMIT_FOR_WARNING = min_speed_limit_for_warning;

            Log.d(TAG, "STEPIST_SOUND_ON : " + STEPIST_SOUND_RERMINDER_FREQUENCY);

        } catch (JSONException ex) {
            Log.e(TAG, "Could not parse query object or write response object", ex);
            callbackContext.error("Could not parse query object or write response object");
        }
    }

    private void updateSetting(final JSONArray args, CallbackContext callbackContext) {
        SettingsDatabaseHelper dbHelper = new SettingsDatabaseHelper(cordova.getContext());
        try {
            String key = args.getString(0);
            String value = args.getString(1);

            dbHelper.updateField(key, value);
            callbackContext.success("Updated " + key);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void getSetting(final JSONArray args, CallbackContext callbackContext) {
        SettingsDatabaseHelper dbHelper = new SettingsDatabaseHelper(cordova.getContext());
        try {
            String key = args.getString(0);
            String value = dbHelper.getField(key);
            callbackContext.success(value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


    /*
     * private JSONObject getStepsJSON(float steps) {
     * JSONObject r = new JSONObject();
     * // pedometerData.startDate; -> ms since 1970
     * // pedometerData.endDate; -> ms since 1970
     * // pedometerData.numberOfSteps;
     * // pedometerData.distance;
     * // pedometerData.floorsAscended;
     * // pedometerData.floorsDescended;
     * try {
     * r.put("startDate", this.starttimestamp);
     * r.put("endDate", System.currentTimeMillis());
     * r.put("numberOfSteps", steps);
     * } catch (JSONException e) {
     * e.printStackTrace();
     * }
     * return r;
     * }
     */
    private JSONObject getStepsJSON(float steps) {
        JSONObject r = new JSONObject();
        long currentTimeMillis = System.currentTimeMillis();
        long timeDiff = currentTimeMillis - this.previousTime;
        float stepDiff = steps - this.previousSteps;
        float perMinute = 0.0f;
        if (timeDiff > 0) {
            perMinute = (60000.0f * stepDiff) / ((float) timeDiff);
        }
        this.previousSteps = steps;
        this.previousTime = currentTimeMillis;
        this.stepIstDetector.setCounterSensorResetSteps(steps);
        try {
            r.put("startDate", this.starttimestamp);
            r.put("endDate", currentTimeMillis);
            r.put("numberOfSteps", (double) steps);
            r.put("stepDiff", (double) stepDiff);
            r.put("timeDiff", timeDiff);
            r.put("perMinute", (double) perMinute);
            r.put("sensor", "stepCounterSensor");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return r;
    }
}
