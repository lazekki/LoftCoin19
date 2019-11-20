package com.loftschool.ozaharenko.loftcoin19.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.loftschool.ozaharenko.loftcoin19.R;
import com.loftschool.ozaharenko.loftcoin19.ui.welcome.WelcomeActivity;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler().postDelayed(()-> {
            startActivity(new Intent(this, WelcomeActivity.class));
            Log.d(TAG, "onCreate: ");
            finish();
        }, 1000);

    }
}
