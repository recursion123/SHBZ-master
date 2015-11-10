package gtrj.shbz.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.gc.materialdesign.views.ProgressBarCircularIndeterminate;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import gtrj.shbz.R;
import gtrj.shbz.util.ContextString;
import gtrj.shbz.util.HttpClientUtil;
import gtrj.shbz.util.OkHttpUtil;

public class UploadPictureActivity extends BaseActivity {
    private ImageView imageView;
    private Button upload;
    private Button retake;
    private ProgressBarCircularIndeterminate loading;

    private String path;
    private String Id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_picture);
        path = getIntent().getStringExtra("path");
        Id = getIntent().getStringExtra("Id");

        loading = (ProgressBarCircularIndeterminate) findViewById(R.id.loading);

        Bitmap bitmap = getBitmapFromFile(path, 358, 441);
        imageView = (ImageView) findViewById(R.id.photo);
        imageView.setImageBitmap(bitmap);

        upload = (Button) findViewById(R.id.upload);
        upload.setOnClickListener(v -> {
            loading.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            upload.setVisibility(View.GONE);
            retake.setVisibility(View.GONE);
            Thread thread = new Thread(this::upload);
            thread.start();
        });
        retake = (Button) findViewById(R.id.retake);
        retake.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT,
                    Uri.fromFile(new File(path)));
            startActivityForResult(intent, 1);
        });
        loading.setVisibility(View.GONE);
    }

    private void upload() {
        Map<String, String> map = new HashMap<>();
        map.put("type", "0");
        SharedPreferences preferences = getSharedPreferences("SHBZ", 0);
        String userName = preferences.getString("userName", "");
        map.put("userId", userName);
        map.put("id", Id);
        map.put("gps", "");
        try {
            //String result = HttpClientUtil.multipartRequest(ContextString.UPLOAD_PICTURE, map, path, "pictrueData", "image/jpg");
            //改用okhttp
            String result = OkHttpUtil.uploadFile(ContextString.UPLOAD_PICTURE, map, path, "pictrueData", "image/jpg");
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>() {
            }.getType();
            Map<String, String> resultMap = gson.fromJson(result, type);
            if (resultMap != null && resultMap.get("procedureNumber") != null) {
                Intent intent = new Intent(this, ValidateResultActivity.class);
                intent.putExtra("procedureNumber", resultMap.get("procedureNumber"));
                intent.putExtra("path", path);
                intent.putExtra("id", Id);
                startActivity(intent);
                this.finish();
            }
        } catch (OkHttpUtil.SessionOutOfTimeException e) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("isSessionOutOfTime", "1");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            this.finish();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            Bitmap bitmap = getBitmapFromFile(path, 358, 441);
            imageView = (ImageView) findViewById(R.id.photo);
            imageView.setImageBitmap(bitmap);
        }
    }

    public Bitmap getBitmapFromFile(String path, int width, int height) {

        BitmapFactory.Options opts = null;
        if (width > 0 && height > 0) {
            opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, opts);
            // 计算图片缩放比例
            final int minSideLength = Math.min(width, height);
            opts.inSampleSize = computeSampleSize(opts, minSideLength,
                    width * height);
            opts.inJustDecodeBounds = false;
        }
        Bitmap bitmap = BitmapFactory.decodeFile(path, opts);
        try {
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(path);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return bitmap;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }

        return null;
    }

    public static int computeSampleSize(BitmapFactory.Options options,
                                        int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength,
                maxNumOfPixels);
        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }
        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options,
                                                int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;

        int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math
                .sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == -1) ? 128 : (int) Math.min(Math
                .floor(w / minSideLength), Math.floor(h / minSideLength));

        if (upperBound < lowerBound) {
            // return the larger one when there is no overlapping zone.
            return lowerBound;
        }

        if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
            return 1;
        } else if (minSideLength == -1) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }
}
