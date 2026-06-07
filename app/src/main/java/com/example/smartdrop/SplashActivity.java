package com.example.smartdrop;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SplashActivity extends AppCompatActivity {

    ImageView gota, logo, fondoCompleto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        gota = findViewById(R.id.gota);
        logo = findViewById(R.id.logo);
        fondoCompleto = findViewById(R.id.fondoCompleto);

        Animation bajar = AnimationUtils.loadAnimation(this, R.anim.drop_down);
        Animation crecer = AnimationUtils.loadAnimation(this, R.anim.grow);
        Animation aparecer = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        // 1. Baja la gota
        gota.startAnimation(bajar);

        bajar.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationEnd(Animation animation) {

                // 2. La gota crece
                gota.startAnimation(crecer);

                crecer.setAnimationListener(new Animation.AnimationListener() {

                    @Override
                    public void onAnimationEnd(Animation animation) {

                        // 3. Mostrar logo
                        gota.setVisibility(View.INVISIBLE);

                        fondoCompleto.setAlpha(1f);
                        fondoCompleto.startAnimation(aparecer);

                        logo.setAlpha(1f);
                        logo.startAnimation(aparecer);

                        // 4. Ir al login
                        new Handler().postDelayed(() -> {

                            Intent intent = new Intent(
                                    SplashActivity.this,
                                    MainActivity.class);

                            startActivity(intent);
                            finish();

                        },1500);
                    }

                    @Override public void onAnimationStart(Animation animation){}
                    @Override public void onAnimationRepeat(Animation animation){}
                });
            }

            @Override public void onAnimationStart(Animation animation){}
            @Override public void onAnimationRepeat(Animation animation){}
        });
    }

}