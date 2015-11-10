package gtrj.shbz.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import gtrj.shbz.R;

import static gtrj.shbz.R.color.text;
import static gtrj.shbz.R.color.white;
import static gtrj.shbz.R.drawable.bg_border;
import static gtrj.shbz.R.drawable.bg_border_select;
import static gtrj.shbz.R.styleable.zTextView;

/**
 * Created by zhang77555 on 2015/7/31.
 * 自定义文本按钮
 */
public class zTextView extends FrameLayout{
    private RelativeLayout zView;
    private TextView zText;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public zTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.z_text_view, this);
        zView=(RelativeLayout)findViewById(R.id.z_view);
        zText=(TextView)findViewById(R.id.z_text);
        @SuppressLint("Recycle") TypedArray text = context.obtainStyledAttributes(attrs,zTextView);
        setzText(text.getString(R.styleable.zTextView_text));
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void click(Boolean isSelected){
        if(isSelected){
            zView.setBackground(this.getResources().getDrawable(bg_border_select));
            zText.setTextColor(this.getResources().getColor(white));
        }else{
            zView.setBackground(this.getResources().getDrawable(bg_border));
            zText.setTextColor(this.getResources().getColor(text));
        }
    }

    public void setzText(String zText) {
        this.zText.setText(zText);
    }
}
