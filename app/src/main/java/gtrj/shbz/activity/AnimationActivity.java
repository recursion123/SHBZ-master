package gtrj.shbz.activity;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;

import gtrj.shbz.R;

/**
 * ¿ª³¡¶¯»­
 * create by zhang77555
 */
public class AnimationActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_animation);

        Handler mHandler = new Handler();
        mHandler.postDelayed(this::goLoginActivity, 3000);
    }

    public void goLoginActivity() {
        Intent intent = new Intent();
        intent.setClass(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
