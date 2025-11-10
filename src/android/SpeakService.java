package org.apache.cordova.stepist;

import static org.apache.cordova.stepist.StepIstListener.STEPIST_SOUND_RERMINDER_FREQUENCY;
import static org.apache.cordova.stepist.StepIstListener.STEPIST_SOUND_RERMINDER_LAST_TIMESTAMP;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

import android.util.Log;

public class SpeakService extends Service {

    private TextToSpeech tts;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String message = intent.getStringExtra("message");
        if((System.currentTimeMillis()-STEPIST_SOUND_RERMINDER_LAST_TIMESTAMP)>= ((long) STEPIST_SOUND_RERMINDER_FREQUENCY *60*1000)){
            tts = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(new Locale("tr", "TR"));
                    tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "speakId");
                }
            });
            STEPIST_SOUND_RERMINDER_LAST_TIMESTAMP = System.currentTimeMillis();
        }

        // Servisi kısa süre sonra durdur
        new Handler(Looper.getMainLooper()).postDelayed(this::stopSelf, 5000);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
