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

/**
 * This class listens to the pedometer sensor
 */
public class StepIstDetector extends CordovaPlugin implements SensorEventListener {

    public static float STEP_COUNTER_VALE =0.0f;

    private String TAG = "recording api";

    private SensorManager sensorManager;
    private Sensor stepSensor;

    /// ///////////////////////////////////////////////////////////////////////
    ///

    public static long PREVIOUS_STEP_TIME = 0;
    public static float STEP_COUNTER_LAST1 = 0.0f;
    public static float STEP_COUNTER_LAST2 = 0.0f;
    public static float STEP_COUNTER_LAST3 = 0.0f;
    public static float STEP_COUNTER_LAST4 = 0.0f;
    //public static float STEP_COUNTER_VALE = 0.0f;
    //private String TAG = "recording api";
    public CallbackContext callbackContext;
    private CordovaInterface cordova;
    private float counterSensorResetSteps = 0.0f;
    private long counterSensorResetTime = 0;
    private long lastTimeStampTrigger;
    private float previousSteps = 0.0f;
    private long previousTime = 0;
    //private SensorManager sensorManager;
    private float startsteps;
    private long starttimestamp;
    //private Sensor stepSensor;
    private CordovaWebView webView;


    ///
    /// ////////////////////////////////////////////////////////////////////////



    StepIstDetector() {
        super();
    }

    StepIstDetector(CordovaInterface cordova, CordovaWebView webView) {
        super();
        this.webView = webView;
        this.cordova = cordova;
        this.sensorManager = (SensorManager) cordova.getActivity().getSystemService(Context.SENSOR_SERVICE);
        this.stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        if (this.stepSensor == null) {
            Log.e(TAG, "Step sensor not available on this device.");
        } else {
            Log.i(TAG, "Step sensor initialized successfully.");
        }

        
        List<Sensor> listSD = this.sensorManager.getSensorList(Sensor.TYPE_STEP_DETECTOR);
        if ((listSD != null) && (listSD.size() > 0)) {
            this.stepSensor = listSD.get(0);
            this.sensorManager.registerListener(this, this.stepSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

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
        if (event.sensor.getType() != Sensor.TYPE_STEP_DETECTOR) {
            return;
        }

        float steps = event.values[0];
        STEP_COUNTER_VALE += steps;
        win(getStepsJSON(steps));

    }

    /**
     * Called when the view navigates.
     */
    @Override
    public void onReset() {
        //if (this.status == StepIstListener.RUNNING) {
        //    this.stop();
        //}
    }

    private void win(JSONObject message) {
        PluginResult result;
        if (message != null) {
            result = new PluginResult(PluginResult.Status.OK, message);
        } else {
            result = new PluginResult(PluginResult.Status.OK);
        }
        result.setKeepCallback(true);
        this.callbackContext.sendPluginResult(result);
    }

    private JSONObject getStepsJSON(float steps) {
        JSONObject r = new JSONObject();
        this.counterSensorResetSteps += 1.0f;
        long currentTimeMillis = System.currentTimeMillis();
        long timeDiff = currentTimeMillis - PREVIOUS_STEP_TIME;
        float perMinute = (60000.0f * 1.0f) / ((float) timeDiff);
        float f = 220.0f;
        if (perMinute <= 220.0f) {
            f = perMinute;
        }
        float perMinute2 = f;
        this.lastTimeStampTrigger = currentTimeMillis;
        float perLast3Minutes = ((STEP_COUNTER_LAST1 + STEP_COUNTER_LAST2) + perMinute2) / 3.0f;
        float perLast5Minutes = ((((STEP_COUNTER_LAST1 + STEP_COUNTER_LAST2) + STEP_COUNTER_LAST3) + STEP_COUNTER_LAST4) + perMinute2) / 5.0f;
        STEP_COUNTER_LAST4 = STEP_COUNTER_LAST3;
        STEP_COUNTER_LAST3 = STEP_COUNTER_LAST2;
        STEP_COUNTER_LAST2 = STEP_COUNTER_LAST1;
        STEP_COUNTER_LAST1 = perMinute2;
        this.previousSteps = steps;
        this.previousTime = currentTimeMillis;
        try {
            r.put("numberOfSteps", (double) STEP_COUNTER_VALE);
            r.put("perMinute", Math.round(perMinute2));
            r.put("perLast3Minutes", Math.round(perLast3Minutes));
            r.put("perLast5Minutes", Math.round(perLast5Minutes));
            r.put("stepDiff", (double) 1.0f);
            r.put("timeDiff", timeDiff);
            r.put("steps", (double) steps);
            r.put("sensor", "stepSensor");
            r.put("counterSensorResetSteps", (double) this.counterSensorResetSteps);
            r.put("startDate", PREVIOUS_STEP_TIME);
            r.put("endDate", currentTimeMillis);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PREVIOUS_STEP_TIME = currentTimeMillis;
        return r;
    }

    public void setStartsteps(float startsteps2) {
        this.startsteps = startsteps2;
    }

    public void setStarttimestamp(long starttimestamp2) {
        this.starttimestamp = starttimestamp2;
    }

    public void setCounterSensorResetSteps(float counterSensorResetSteps2) {
        this.counterSensorResetSteps = 0.0f;
        STEP_COUNTER_VALE = counterSensorResetSteps2;
        this.counterSensorResetTime = System.currentTimeMillis();
    }


}
