package gtrj.shbz.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gc.materialdesign.views.ProgressBarCircularIndeterminate;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import gtrj.gallerylibrary.GalleryWidget.FilePagerAdapter;
import gtrj.gallerylibrary.GalleryWidget.GalleryViewPager;
import gtrj.shbz.R;
import gtrj.shbz.util.ContextString;
import gtrj.shbz.util.HttpClientUtil;

public class ValidateResultActivity extends BaseActivity implements View.OnClickListener {
    private String procedureNumber;
    private String path;
    private String id;

    private GalleryViewPager pictureGroup;
    private LinearLayout buttonGroup;
    private TextView title;
    private ProgressBarCircularIndeterminate loading;

    private String matchRatio;
    private Integer imageNum;

    private Context context;

    private List<String> paths;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_validate_result);

        procedureNumber = getIntent().getStringExtra("procedureNumber");
        path = getIntent().getStringExtra("path");
        id = getIntent().getStringExtra("id");

        context = this;

        loading = (ProgressBarCircularIndeterminate) findViewById(R.id.loading);
        title = (TextView) findViewById(R.id.title);
        Button noPass = (Button) findViewById(R.id.no_pass);
        noPass.setOnClickListener(this);
        Button nextStep = (Button) findViewById(R.id.next_step);
        nextStep.setOnClickListener(this);
        Button retake = (Button) findViewById(R.id.result_retake);
        retake.setOnClickListener(this);
        buttonGroup = (LinearLayout) findViewById(R.id.button_group);
        buttonGroup.setVisibility(View.INVISIBLE);
        pictureGroup = (GalleryViewPager) findViewById(R.id.picture_group);
        pictureGroup.setVisibility(View.INVISIBLE);
        Message msg = msgHandler.obtainMessage();
        msg.arg1 = 1;
        msgHandler.sendMessage(msg);
    }

    private final Handler msgHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case 1:
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            boolean flag = false;
                            for (int i = 0; i < 10; i++) {
                                Map<String, String> map = getValidateResult(procedureNumber);
                                if (map != null) {
                                    matchRatio = map.get("matchRatio");
                                    imageNum = Integer.parseInt(map.get("pictureNum"));
                                }
                                if (matchRatio != null && imageNum != null) {
                                    Message msg = msgHandler.obtainMessage();
                                    msg.arg1 = 2;
                                    msgHandler.sendMessage(msg);
                                    break;
                                }
                                try {
                                    Thread.sleep(3000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if (i == 9) {
                                    flag = true;
                                }
                            }
                            if (flag) {
                                Message msg = msgHandler.obtainMessage();
                                msg.arg1 = 4;
                                msgHandler.sendMessage(msg);
                            }
                        }
                    });
                    thread.start();
                    break;
                case 2:
                    paths = new ArrayList<>();
                    title.setText("正在获取图片....");
                    Thread thread1 = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (Float.valueOf(matchRatio) * 100 < 90) {
                                if (imageNum < 3) {
                                    for (int i = 0; i <= imageNum; i++) {
                                        paths.add(getPicture(procedureNumber, String.valueOf(i)));
                                    }
                                } else {
                                    for (int i = 1; i <= imageNum; i++) {
                                        paths.add(getPicture(procedureNumber, String.valueOf(i)));
                                    }
                                }
                            } else {
                                paths.add(getPicture(procedureNumber, String.valueOf(imageNum)));
                            }
                            Message msg = msgHandler.obtainMessage();
                            msg.arg1 = 3;
                            msgHandler.sendMessage(msg);
                        }
                    });
                    thread1.start();


                    break;
                case 3:
                    int mr = (int) (Float.valueOf(matchRatio) * 100);
                    title.setText("当前上传照片比对率为 " + mr + "%\n\n    以下是往年比对结果");
                    loading.setVisibility(View.GONE);
                    buttonGroup.setVisibility(View.VISIBLE);
                    List<String> items = new ArrayList<>();
                    items.clear();
                    items.addAll(paths);
                    FilePagerAdapter pagerAdapter = new FilePagerAdapter(context, items);
                    pictureGroup.setVisibility(View.VISIBLE);
                    pictureGroup.setOffscreenPageLimit(3);
                    pictureGroup.setAdapter(pagerAdapter);
                    break;
                case 4:
                    Toast.makeText(context, "比对超时，请重试", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private String getPicture(String procedureNumber, String order) {
        Map<String, String> map = new HashMap<>();
        map.put("procedureNumber", procedureNumber);
        map.put("order", order);
        String filePath = context.getFilesDir() + File.separator + UUID.randomUUID() + ".jpg";
        try {
            InputStream result = HttpClientUtil.getImage(ContextString.GET_PICTURE, map);
            if (result != null) {
                File temFile = new File(filePath);
                if (!temFile.exists()) {
                    temFile.getParentFile().mkdirs();
                }
                FileOutputStream fileOut = new FileOutputStream(temFile);
                byte[] buffer = new byte[1024 * 8];
                for (int j; (j = result.read(buffer)) != -1; ) {
                    fileOut.write(buffer, 0, j);
                }
                fileOut.flush();
                result.close();
                fileOut.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filePath;

    }

    private Map<String, String> getValidateResult(String procedureNumber) {
        Map<String, String> map = new HashMap<>();
        map.put("procedureNumber", procedureNumber);
        try {
            String result = HttpClientUtil.getData(ContextString.IS_FINISH, map);
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>() {
            }.getType();
            Map<String, String> resultMap = gson.fromJson(result, type);
            if (resultMap != null && resultMap.get("matchRatio") != null) {
                return resultMap;
            }
        } catch (HttpClientUtil.SessionOutOfTimeException e) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("isSessionOutOfTime", "1");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            this.finish();
        }
        return null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.no_pass:
                this.finish();
                break;
            case R.id.result_retake:
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(new File(path)));
                startActivityForResult(intent, 1);
                break;
            case R.id.next_step:
                if (Float.valueOf(matchRatio) * 100 < 90) {
                    new AlertDialog.Builder(this).setTitle("提示")
                            .setIconAttribute(android.R.attr.alertDialogIcon)
                            .setMessage("匹配率小于90%，确定要进入下一步开始视频拍摄吗？")
                            .setPositiveButton("确认", (dialog, which) -> {
                                Intent intent1 = new Intent(context, CameraRecordActivity.class);
                                intent1.putExtra("procedureNumber", procedureNumber);
                                startActivity(intent1);
                            })
                            .setNegativeButton("取消", null)
                            .create().show();
                } else {
                    Intent intent1 = new Intent(context, CameraRecordActivity.class);
                    intent1.putExtra("procedureNumber", procedureNumber);
                    startActivity(intent1);
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            Intent intent = new Intent(this, UploadPictureActivity.class);
            intent.putExtra("path", path);
            intent.putExtra("Id", id);
            startActivity(intent);
            this.finish();
        }
    }
}
