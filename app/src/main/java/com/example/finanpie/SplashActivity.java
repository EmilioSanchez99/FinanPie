package com.example.finanpie;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;
import android.media.MediaPlayer;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int DURACION_SPLASH = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        VideoView videoView = findViewById(R.id.videoView);
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.publi);
        videoView.setVideoURI(videoUri);

        videoView.setOnCompletionListener(mp -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        });

        videoView.start();

        // ❗ Por si quieres forzar cambio tras X segundos
        /*
        new Handler().postDelayed(() -> {
            if (videoView.isPlaying()) videoView.stopPlayback();
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }, DURACION_SPLASH);
        */
    }
}
