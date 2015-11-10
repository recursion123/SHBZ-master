package gtrj.shbz.activity;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gc.materialdesign.views.ProgressBarCircularIndeterminate;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import gtrj.shbz.R;
import gtrj.shbz.util.ContextString;
import gtrj.shbz.util.OkHttpUtil;

@SuppressWarnings("deprecation")
public class CameraRecordActivity extends Activity implements SurfaceHolder.Callback, OnClickListener, SensorEventListener {
    private Button start;// ��ʼ¼�ư�ť
    private Button retake;
    private Button upload;
    private MediaRecorder mediarecorder;// ¼����Ƶ����
    private SurfaceView surfaceview;// ��ʾ��Ƶ�Ŀؼ�
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private TextView time;
    private ProgressBarCircularIndeterminate loading;

    private Context context;

    private String path;
    private int i;
    private String procedureNumber;

    int windowHeight;
    int windowWidth;

    // ���μ���ʱ����
    private static final int UPTATE_INTERVAL_TIME = 200;

    // �ϴμ��ʱ��
    private long lastUpdateTime;

    private boolean isVisible_start = true;//����start��ť����ʾ״̬
    private boolean canVisible_start = true;//�ж�start��ť�ܷ���ʾ

    private SoundPool soundPool;

    private int maxTime = 10;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);// ȥ��������  
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);// ����ȫ��
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);//����
        // ѡ��֧�ְ�͸��ģʽ,����surfaceview��activity��ʹ�á�  
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        setContentView(R.layout.activity_camera_record);

        context = this;

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        windowHeight = size.y;
        windowWidth = size.x;

        path = Environment.getExternalStorageDirectory().getPath() + File.separator + "SHBZ" + File.separator + "video.mp4";
        procedureNumber = getIntent().getStringExtra("procedureNumber");

        time = (TextView) findViewById(R.id.time);
        start = (Button) findViewById(R.id.start);
        start.setOnClickListener(this);
        retake = (Button) findViewById(R.id.retake_video);
        retake.setOnClickListener(this);
        upload = (Button) findViewById(R.id.upload_video);
        upload.setOnClickListener(this);
        loading = (ProgressBarCircularIndeterminate) findViewById(R.id.loading);
        loading.setVisibility(View.INVISIBLE);

        surfaceview = (SurfaceView) this.findViewById(R.id.surfaceview);
        SurfaceHolder holder = surfaceview.getHolder();// ȡ��holder
        holder.setKeepScreenOn(true);
        holder.addCallback(this); // holder����ص��ӿ�
        // holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, sensor,
                SensorManager.SENSOR_DELAY_NORMAL);


        int[][] sounds = new int[][]{{R.raw.sound, R.raw.sound_end}, {R.raw.sound2, R.raw.sound2_end}, {R.raw.sound3, R.raw.sound3_end}, {R.raw.sound4},
                {R.raw.sound5}, {R.raw.sound6}, {R.raw.sound7}, {R.raw.sound8, R.raw.sound8_end}, {R.raw.sound9, R.raw.sound9_end},};


        soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
        int soundNum = 3;
        for (int i = 0; i < soundNum; i++) {
            int action = new Random().nextInt(100) % sounds.length;
            if (sounds[action].length > 1 && i < 2) {
                soundPool.load(this, sounds[action][0], i + 1);
                soundPool.load(this, sounds[action][1], i + 2);
                i++;
            } else {
                soundPool.load(this, sounds[action][0], i + 1);
            }
        }

    }

    private void playSound() {
        msgHandler.postDelayed(() -> soundPool.play(1, 1, 1, 0, 0, 1), 0);
        msgHandler.postDelayed(() -> soundPool.play(2, 1, 1, 0, 0, 1), 4000);
        msgHandler.postDelayed(() -> soundPool.play(3, 1, 1, 0, 0, 1), 7000);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start:
                playSound();
                mediarecorder = new MediaRecorder();// ����mediarecorder����
                camera.release();
                camera = null;
                // mediarecorder.setCamera(camera);
                // ����¼����ƵԴΪCamera(���)
                mediarecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                mediarecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);

                // ����¼����ɺ���Ƶ�ķ�װ��ʽTHREE_GPPΪ3gp.MPEG_4Ϊmp4
                mediarecorder
                        .setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

                // ����¼�Ƶ���Ƶ����h263 h264
                mediarecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                mediarecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                // ������Ƶ¼�Ƶķֱ��ʡ�����������ñ���͸�ʽ�ĺ��棬���򱨴�
                mediarecorder.setVideoSize(176, 144);
                mediarecorder.setPreviewDisplay(surfaceHolder.getSurface());
                // ������Ƶ�ļ������·��
                mediarecorder.setOutputFile(path);

                // ׼��¼��
                try {
                    mediarecorder.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    // ��ʼ¼��
                    mediarecorder.start();
                    canVisible_start = false;
                    start.setVisibility(View.GONE);
                    Thread thread = new Thread(() -> {
                        try {
                            for (i = maxTime; i >= 1; i--) {
                                Message msg = msgHandler.obtainMessage();
                                msg.arg1 = 1;
                                msgHandler.sendMessage(msg);
                                Thread.sleep(1000);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (mediarecorder != null) {
                            // ֹͣ¼��
                            mediarecorder.stop();
                            // �ͷ���Դ
                            mediarecorder.release();
                            mediarecorder = null;
                            Message msg = msgHandler.obtainMessage();
                            msg.arg1 = 2;
                            msgHandler.sendMessage(msg);
                        }
                    });
                    thread.start();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.retake_video:
                playSound();
                mediarecorder = new MediaRecorder();// ����mediarecorder����
                //mediarecorder.setCamera(camera);
                // ����¼����ƵԴΪCamera(���)
                mediarecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                mediarecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
                // ����¼����ɺ���Ƶ�ķ�װ��ʽTHREE_GPPΪ3gp.MPEG_4Ϊmp4
                mediarecorder
                        .setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                // ����¼�Ƶ���Ƶ����h263 h264
                mediarecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                mediarecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                // ������Ƶ¼�Ƶķֱ��ʡ�����������ñ���͸�ʽ�ĺ��棬���򱨴�
                mediarecorder.setVideoSize(176, 144);
                mediarecorder.setPreviewDisplay(surfaceHolder.getSurface());
                // ������Ƶ�ļ������·��
                mediarecorder.setOutputFile(path);
                // ׼��¼��
                try {
                    mediarecorder.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    // ��ʼ¼��
                    mediarecorder.start();
                    retake.setVisibility(View.INVISIBLE);
                    upload.setVisibility(View.INVISIBLE);
                    Thread thread = new Thread(() -> {
                        try {
                            for (i = maxTime; i >= 1; i--) {
                                Message msg = msgHandler.obtainMessage();
                                msg.arg1 = 1;
                                msgHandler.sendMessage(msg);
                                Thread.sleep(1000);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (mediarecorder != null) {
                            // ֹͣ¼��
                            mediarecorder.stop();
                            // �ͷ���Դ
                            mediarecorder.release();
                            mediarecorder = null;
                            Message msg = msgHandler.obtainMessage();
                            msg.arg1 = 2;
                            msgHandler.sendMessage(msg);
                        }
                    });
                    thread.start();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.upload_video:
                loading.setVisibility(View.VISIBLE);
                retake.setClickable(false);
                upload.setClickable(false);
                new Thread(() -> {
                    Boolean success = uploadVideo();
                    if (success) {
                        Message msg = msgHandler.obtainMessage();
                        msg.arg1 = 3;
                        msgHandler.sendMessage(msg);
                        Intent intent = new Intent(context, PensionValidateListActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra("finish", "1");
                        startActivity(intent);
                        finish();
                    } else {
                        Message msg = msgHandler.obtainMessage();
                        msg.arg1 = 4;
                        msgHandler.sendMessage(msg);
                    }
                }).start();
                break;
        }

    }


    private Boolean uploadVideo() {
        try {
            Map<String, String> map = new HashMap<>();
            map.put("procedureNumber", procedureNumber);
            String result = OkHttpUtil.uploadFile(ContextString.UPLOAD_VIDEO, map, path, "videoData", "video/mp4");
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>() {
            }.getType();
            Map<String, String> resultMap = gson.fromJson(result, type);
            if (resultMap != null && resultMap.get("isSave") != null && resultMap.get("isSave").equals("0")) {
                return true;
            }
        } catch (OkHttpUtil.SessionOutOfTimeException e) {
            e.printStackTrace();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("isSessionOutOfTime", "1");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            this.finish();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private final Handler msgHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case 1:
                    time.setText(String.valueOf(i));
                    break;
                case 2:
                    retake.setVisibility(View.VISIBLE);
                    upload.setVisibility(View.VISIBLE);
                    time.setText("�������");
                    break;
                case 3:
                    Toast.makeText(context, "��֤���", Toast.LENGTH_LONG).show();
                    break;
                case 4:
                    Toast.makeText(context, "�ϴ�ʧ��", Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        // ��holder�����holderΪ��ʼ��oncreat����ȡ�õ�holder����������surfaceHolder
        surfaceHolder = holder;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // ��holder�����holderΪ��ʼ��oncreat����ȡ�õ�holder����������surfaceHolder
        surfaceHolder = holder;
        try {
            if (camera == null) {
                camera = Camera.open();
                Camera.Parameters parameters = camera.getParameters();
                camera.setParameters(parameters);
            }
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // surfaceDestroyed��ʱ��ͬʱ��������Ϊnull
        surfaceview = null;
        surfaceHolder = null;
        mediarecorder = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long currentUpdateTime = System.currentTimeMillis();
        // ���μ���ʱ����
        long timeInterval = currentUpdateTime - lastUpdateTime;
        // �ж��Ƿ�ﵽ�˼��ʱ����
        if (timeInterval < UPTATE_INTERVAL_TIME) {
            return;
        }
        // ���ڵ�ʱ����lastʱ��
        lastUpdateTime = currentUpdateTime;

        // ���x,y,z����
        float x = event.values[0];
        if (x > 9) {
            if (canVisible_start && !isVisible_start) {
                start.setVisibility(View.VISIBLE);
                isVisible_start = true;
            }
        } else {
            if (isVisible_start && canVisible_start) {
                start.setVisibility(View.GONE);
                isVisible_start = false;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}