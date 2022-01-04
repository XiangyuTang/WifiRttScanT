package com.example.android.wifirttscan.utils;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.example.android.wifirttscan.widget.ImageViewCheckBox;
import com.fengmap.android.map.geometry.FMMapCoord;
import com.fengmap.android.map.marker.FMImageMarker;
import com.fengmap.android.map.marker.FMLocationMarker;

/**
 * 控件使用帮助类
 *
 * @author hezutao@fengmap.com
 * @version 2.0.0
 */
public class ViewHelper {

    /**
     * 获取控件
     *
     * @param activity Activity
     * @param id       控件id
     * @param <T>      控件继承于View
     * @return id对应的控件
     */
    public static <T extends View> T getView(Activity activity, int id) {
        return (T) activity.findViewById(id);
    }

    /**
     * 获取控件
     *
     * @param view 视图
     * @param id   控件id
     * @param <T>  继承于View
     * @return id对应的控件
     */
    public static <T extends View> T getView(View view, int id) {
        return (T) view.findViewById(id);
    }

    /**
     * 设置控件的点击事件
     *
     * @param activity Activity
     * @param id       控件id
     * @param listener 点击监听事件
     */
    public static void setViewClickListener(Activity activity, int id, View.OnClickListener listener) {
        View view = getView(activity, id);
        view.setOnClickListener(listener);
    }

    /**
     * 设置控件是否可用
     *
     * @param activity Activity
     * @param id       控件id
     * @param enabled  是否可用
     */
    public static void setViewEnable(Activity activity, int id, boolean enabled) {
        View view = getView(activity, id);
        view.setEnabled(enabled);
    }

    /**
     * 设置控件文字
     *  @param activity Activity
     * @param id       控件id
     * @param text     文字
     */
    public static void setViewText(Activity activity, int id, String text) {
        TextView view = getView(activity, id);
        view.setText(text);
    }

    /**
     * 设置CheckBox的状态改变事件
     *
     * @param activity Activity
     * @param id       控件id
     * @param listener CheckBox选中状态改变事件
     */
    public static void setViewCheckedChangeListener(Activity activity, int id,
                                                    ImageViewCheckBox.OnCheckStateChangedListener listener) {
        ImageViewCheckBox view = getView(activity, id);
        view.setOnCheckedChangeListener(listener);
    }

    /**
     * 设置控件的点击事件
     *
     * @param activity   Activity
     * @param id         控件id
     * @param visibility 控件显示状态
     */
    public static void setViewVisibility(Activity activity, int id, int visibility) {
        View view = getView(activity, id);
        view.setVisibility(visibility);
    }

    /**
     * 添加图片标注
     *
     * @param resources 资源
     * @param mapCoord  坐标
     * @param resId     资源id
     * @return 图片标注
     */
    public static FMImageMarker buildImageMarker(Resources resources, FMMapCoord mapCoord, int resId) {
        Bitmap bitmap = BitmapFactory.decodeResource(resources, resId);
        FMImageMarker imageMarker = new FMImageMarker(mapCoord, bitmap);
        //设置图片宽高
        imageMarker.setMarkerWidth(90);
        imageMarker.setMarkerHeight(90);

        imageMarker.setFMImageMarkerOffsetMode(FMImageMarker.FMImageMarkerOffsetMode.FMNODE_CUSTOM_HEIGHT);
        imageMarker.setCustomOffsetHeight(2.0f);
        return imageMarker;
    }

    /**
     * 创建定位点标注
     *
     * @param groupId  楼层id
     * @param mapCoord 坐标点
     * @param angle    方向
     * @return 图片标注
     */
    public static FMLocationMarker buildLocationMarker(int groupId, FMMapCoord mapCoord, float angle) {
        FMLocationMarker locationMarker = new FMLocationMarker(groupId, mapCoord);
        //设置定位点图片
//        locationMarker.setActiveImageFromAssets("ic_marker_people_red.png");
        locationMarker.setActiveImageFromAssets("active.png");
        //设置定位图片宽高
        locationMarker.setMarkerWidth(90);
        locationMarker.setMarkerHeight(90);
        //设置定位点标注角度
        locationMarker.setAngle(angle);
        return locationMarker;
    }
}
