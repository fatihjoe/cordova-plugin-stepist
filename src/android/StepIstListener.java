package org.apache.cordova.stepist;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.os.Handler;
import android.util.Log;

import androidx.core.app.ActivityCompat;

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

    private  String TAG = "recording api";

    public static int STOPPED = 0;
    public static int STARTING = 1;
    public static int RUNNING = 2;
    public static int ERROR_FAILED_TO_START = 3;
    public static int ERROR_NO_SENSOR_FOUND = 4;
    public static int RECORDING_API = 5;

    private int status;     // status of listener
    private float startsteps; //first value, to be substracted
    private long starttimestamp; //time stamp of when the measurement starts

    private SensorManager sensorManager; // Sensor manager
    private Sensor mSensor;             // Pedometer sensor returned by sensor manager

    private CallbackContext callbackContext; // Keeps track of the JS callback context.

    private Handler mainHandler=null;

    private LocalRecordingClient localRecordingClient = null;

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
        this.sensorManager = (SensorManager) cordova.getActivity().getSystemService(Context.SENSOR_SERVICE);
    }

    /**
     * Executes the request.
     *
     * @param action the action to execute.
     * @param args the exec() arguments.
     * @param callbackContext the callback context used when calling back into JavaScript.
     * @return whether the action was valid.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;

        if (action.equals("recordingAPI")) {

            LocalRecordingClient localRecordingClient  =  FitnessLocal.getLocalRecordingClient(cordova.getActivity());

            int permission = ActivityCompat.checkSelfPermission(cordova.getActivity(), Manifest.permission.ACTIVITY_RECOGNITION);

            if (permission == PackageManager.PERMISSION_GRANTED) {
                localRecordingClient.subscribe(LocalDataType.TYPE_STEP_COUNT_DELTA);
                /*
               .addOnSuccessListener {
                    Log.i(TAG, "Successfully subscribed!")
                }
                .addOnFailureListener { e ->
                        Log.w(TAG, "There was a problem subscribing.", e)
                }
                */

                this.RecApi();
                return true;
            }
            return false;
        }

        if (action.equals("isStepCountingAvailable")) {
            List<Sensor> list = this.sensorManager.getSensorList(Sensor.TYPE_STEP_COUNTER);
            if ((list != null) && (list.size() > 0)) {
                this.win(true);
                return true;
            } else {
                this.setStatus(StepIstListener.ERROR_NO_SENSOR_FOUND);
                this.win(false);
                return true;
            }
        } else if (action.equals("isDistanceAvailable")) {
            //distance is never available in Android
            this.win(false);
            return true;
        } else if (action.equals("isFloorCountingAvailable")) {
            //floor counting is never available in Android
            this.win(false);
            return true;
        }
        else if (action.equals("startStepIstUpdates")) {
            if (this.status != StepIstListener.RUNNING) {
                // If not running, then this is an async call, so don't worry about waiting
                // We drop the callback onto our stack, call start, and let start and the sensor callback fire off the callback down the road
                this.start();
            }
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT, "");
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return true;
        }
        else if (action.equals("stopStepIstUpdates")) {
            if (this.status == StepIstListener.RUNNING) {
                this.stop();
            }
            this.win(null);
            return true;
        }
         else {
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
            boolean isSupported = this.sensorManager.registerListener(this, this.mSensor, SensorManager.SENSOR_DELAY_FASTEST);
            if (isSupported) {
                this.setStatus(StepIstListener.STARTING);
            } else {
                this.setStatus(StepIstListener.ERROR_FAILED_TO_START);
                this.fail(StepIstListener.ERROR_FAILED_TO_START, "Device sensor returned an error.");
                return;
            };
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
      //nothing to do here
      return;
    }

    /**
     * Sensor listener event.
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

        if(this.startsteps == 0)
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
        if(message != null)
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
                        TimeUnit.SECONDS
                )
                .build();

        localRecordingClient = FitnessLocal.getLocalRecordingClient(cordova.getActivity());

        localRecordingClient.readData(readRequest)
                .addOnSuccessListener(response -> {

                    LocalDataSet localDataSet=  response.getDataSet(LocalDataType.TYPE_STEP_COUNT_DELTA);
                    
                    
                    for (LocalDataPoint dataPoint : localDataSet.getDataPoints()) {
                        dumpDataPoint(dataPoint);
                    }



/*
                    for (Bucket bucket : response.getBuckets()) {
                        for (DataSet dataSet : bucket.getDataSets()) {
                            dumpDataSet(dataSet);
                        }
                    }
                    */

                })
                .addOnFailureListener(e ->
                        Log.w(TAG, "There was an error reading data", e)
                );



        JSONObject message = getStepsJSON(steps);
        // Success return object

        PluginResult result;
        if(message != null)
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
    void dumpDataSet(LocalDataSet dataSet) {
        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(TAG, "Data point:");
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + dp.getStartTime(TimeUnit.HOURS));
            Log.i(TAG, "\tEnd: " + dp.getEndTime(TimeUnit.HOURS));
            for (Field field : dp.getDataType().getFields()) {
                Log.i(TAG, "\tLocalField: " + field.getName() + " LocalValue: " + dp.getValue(field));
            }
        }
    }
*/
    private void setStatus(int status) {
        this.status = status;
    }

    private JSONObject getStepsJSON(float steps) {
        JSONObject r = new JSONObject();
        // pedometerData.startDate; -> ms since 1970
        // pedometerData.endDate; -> ms since 1970
        // pedometerData.numberOfSteps;
        // pedometerData.distance;
        // pedometerData.floorsAscended;
        // pedometerData.floorsDescended;
        try {
            r.put("startDate", this.starttimestamp);
            r.put("endDate", System.currentTimeMillis());
            r.put("numberOfSteps", steps);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return r;
    }
}
