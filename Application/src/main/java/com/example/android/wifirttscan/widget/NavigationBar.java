package com.example.android.wifirttscan.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.android.wifirttscan.R;

/**
 * 自定义标题栏视图组件
 *
 * @author hezutao@fengmap.com
 * @version 2.0.0
 */
public class NavigationBar extends RelativeLayout {

    Context mContext;
    TextView mTitleTV;
    ImageView mLeftIV;
    /**
     * 回退默认点击事件
     */
    private OnClickListener mDefaultClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            ((Activity) mContext).onBackPressed();
        }
    };

    public NavigationBar(Context context) {
        super(context);
        initView(context, null);
    }

    public NavigationBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public NavigationBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context, attrs);
    }

    /**
     * 初始化资源
     */
    private void initView(Context context, AttributeSet attrs) {
        this.mContext = context;
        LayoutInflater.from(context).inflate(R.layout.widget_navigationbar, this,
            true);
        mLeftIV = (ImageView) findViewById(R.id.img_left);
        mTitleTV = (TextView) findViewById(R.id.txt_title);

        TypedArray typeArray = context.obtainStyledAttributes(attrs,
            R.styleable.NavigationBar);
        if (typeArray == null) {
            return;
        }
        // 对包含的ImageView和TextView赋值
        int count = typeArray.getIndexCount();
        for (int i = 0; i < count; i++) {
            int index = typeArray.getIndex(i);
            switch (index) {
                case R.styleable.NavigationBar_title:
                    mTitleTV.setText(typeArray.getText(index));
                    break;
                default:
                    break;
            }
        }
        typeArray.recycle();

        mLeftIV.setOnClickListener(mDefaultClickListener);
        this.setBackgroundResource(R.color.colorPrimaryDark);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    /*---------------------------------以下方法为公开接口方法---------------------------------------*/

    /**
     * 设置左侧按钮点击事件
     *
     * @param listener 点击监听事件
     */
    public void setLeftButtonListener(OnClickListener listener) {
        mLeftIV.setOnClickListener(listener);
    }

    /**
     * 设置标题栏文本
     *
     * @param titleResId 文本资源ID
     */
    public void setTitle(int titleResId) {
        String title = mContext.getString(titleResId);
        this.setTitle(title);
    }

    /**
     * 设置标题栏文本
     *
     * @param title 文本字符串
     */
    public void setTitle(String title) {
        mTitleTV.setText(title);
    }

}
