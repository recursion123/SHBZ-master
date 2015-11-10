package gtrj.shbz.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.RelativeLayout;

import com.gc.materialdesign.views.LayoutRipple;
import com.nineoldandroids.view.ViewHelper;

import gtrj.shbz.R;

/**
 * 主菜单activity
 * create by zhang77555
 */
public class MainActivity extends Activity {

    public static int backgroundColor = Color.parseColor("#00796b");
    public static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        RelativeLayout relativeLayout=(RelativeLayout)findViewById(R.id.top_view);
        relativeLayout.setBackgroundColor(backgroundColor);
        View view=findViewById(R.id.line);
        view.setBackgroundColor(backgroundColor);

        LayoutRipple layoutRipple = (LayoutRipple) findViewById(R.id.pension_validate);

        setOriginRiple(layoutRipple);

        layoutRipple.setOnClickListener(arg0 -> {
            Intent intent = new Intent(context, PensionValidateListActivity.class);
            startActivity(intent);
        });
        layoutRipple = (LayoutRipple) findViewById(R.id.medical_validate);


        setOriginRiple(layoutRipple);

        layoutRipple.setOnClickListener(arg0 -> {

        });
        layoutRipple = (LayoutRipple) findViewById(R.id.policy_release);


        setOriginRiple(layoutRipple);

        layoutRipple.setOnClickListener(arg0 -> {

        });
        layoutRipple = (LayoutRipple) findViewById(R.id.latest_policy);


        setOriginRiple(layoutRipple);

        layoutRipple.setOnClickListener(arg0 -> {

        });
    }

    private void setOriginRiple(final LayoutRipple layoutRipple) {

        layoutRipple.post(() -> {
            View v = layoutRipple.getChildAt(0);
            layoutRipple.setxRippleOrigin(ViewHelper.getX(v) + v.getWidth() / 2);
            layoutRipple.setyRippleOrigin(ViewHelper.getY(v) + v.getHeight() / 2);
            layoutRipple.setRippleColor(backgroundColor);
            layoutRipple.setRippleSpeed(40);
        });

    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 监控返回键
            new AlertDialog.Builder(this).setTitle("提示")
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage("确定要退出吗?")
                    .setPositiveButton("确认", (dialog, which) -> {
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addCategory(Intent.CATEGORY_HOME);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("取消", null)
                    .create().show();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }
}
