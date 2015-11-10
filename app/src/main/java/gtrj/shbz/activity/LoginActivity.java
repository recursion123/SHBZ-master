package gtrj.shbz.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.BootstrapEditText;
import com.gc.materialdesign.views.ProgressBarCircularIndeterminate;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import gtrj.shbz.R;
import gtrj.shbz.util.ContextString;
import gtrj.shbz.util.OkHttpUtil;

import static gtrj.shbz.R.drawable.bg;

/**
 * 登录activity
 * create by zhang77555
 */
public class LoginActivity extends Activity {
    public LinearLayout loginActivity;
    private BootstrapEditText username;
    private BootstrapEditText password;
    private BootstrapButton loginBtn;
    private ProgressBarCircularIndeterminate loading;
    private Activity loginContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_login);

        loginContext = this;

        loginActivity = (LinearLayout) findViewById(R.id.login);
        loginActivity.setBackgroundResource(bg);

        loading = (ProgressBarCircularIndeterminate) findViewById(R.id.loading);
        loading.setVisibility(View.INVISIBLE);
        username = (BootstrapEditText) findViewById(R.id.username);
        password = (BootstrapEditText) findViewById(R.id.password);


        loginBtn = (BootstrapButton) findViewById(R.id.loginBtn);
        loginBtn.setOnClickListener(v -> {
            editable(false);
            new Thread(() -> {
                boolean loginSuccess = isLoginSuccess();
                if (loginSuccess) {
                    Intent intent = new Intent();
                    intent.setClass(loginContext, MainActivity.class);
                    startActivity(intent);
                    loginContext.finish();
                } else {
                    Message msg = msgHandler.obtainMessage();
                    msg.arg1 = 1;
                    msgHandler.sendMessage(msg);
                }
            }).start();
        });
        String isSessionOutOfTime = getIntent().getStringExtra("isSessionOutOfTime");
        if (isSessionOutOfTime != null && "1".equals(isSessionOutOfTime)) {
            Toast.makeText(this, "登陆超时，请重新登录", Toast.LENGTH_LONG).show();
        }
    }

    private final Handler msgHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case 1:
                    Toast.makeText(getApplicationContext(), "登录失败，账户或密码错误", Toast.LENGTH_SHORT).show();
                    editable(true);
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 发送登录请求
     * @return true表示登陆成功，false表示失败
     */
    private boolean isLoginSuccess() {
        Map<String, String> map = new HashMap<>();
        map.put("userName", username.getText().toString());
        map.put("userPwd", password.getText().toString());
        try {
            //String result = HttpClientUtil.getData(ContextString.LOGIN, map);
            //改用okhttp
            String result = OkHttpUtil.Post(ContextString.LOGIN, map);
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>() {
            }.getType();
            Map<String, String> resultMap = gson.fromJson(result, type);
            if (resultMap != null && resultMap.get("IsLogin") != null && resultMap.get("IsLogin").equals("0")) {
                SharedPreferences preferences = getSharedPreferences("SHBZ", 0);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("userName", username.getText().toString());
                editor.apply();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 控制界面上的按钮和输入框是否可用
     * @param flag
     */
    private void editable(boolean flag) {
        username.setEnabled(flag);
        password.setEnabled(flag);
        loginBtn.setEnabled(flag);
        if (flag) {
            loading.setVisibility(View.INVISIBLE);
        } else {
            loading.setVisibility(View.VISIBLE);
        }
    }

}
