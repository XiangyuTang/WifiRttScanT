package com.example.android.wifirttscan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.example.android.wifirttscan.entity.ApInfo;
import com.example.android.wifirttscan.entity.ApRangingHistoryInfo;
import com.example.android.wifirttscan.utils.ConvertUtils;
import com.example.android.wifirttscan.utils.FMCoordTransformer;
import com.example.android.wifirttscan.utils.LocationAlgorithm;
import com.example.android.wifirttscan.utils.ViewHelper;
import com.example.android.wifirttscan.widget.ImageViewCheckBox;
import com.fengmap.android.FMMapSDK;
import com.fengmap.android.analysis.navi.FMActualNavigation;
import com.fengmap.android.analysis.navi.FMNaviAnalyser;
import com.fengmap.android.analysis.navi.FMNaviOption;
import com.fengmap.android.analysis.navi.FMNaviResult;
import com.fengmap.android.analysis.navi.FMNavigation;
import com.fengmap.android.analysis.navi.FMNavigationInfo;
import com.fengmap.android.analysis.navi.FMPointOption;
import com.fengmap.android.analysis.navi.FMSimulateNavigation;
import com.fengmap.android.analysis.navi.OnFMNavigationListener;
import com.fengmap.android.exception.FMObjectException;
import com.fengmap.android.map.FMMap;
import com.fengmap.android.map.FMMapUpgradeInfo;
import com.fengmap.android.map.FMMapView;
import com.fengmap.android.map.FMPickMapCoordResult;
import com.fengmap.android.map.FMRenderMode;
import com.fengmap.android.map.FMViewMode;
import com.fengmap.android.map.event.OnFMMapClickListener;
import com.fengmap.android.map.event.OnFMMapInitListener;
import com.fengmap.android.map.event.OnFMNodeListener;
import com.fengmap.android.map.geometry.FMGeoCoord;
import com.fengmap.android.map.geometry.FMMapCoord;
import com.fengmap.android.map.layer.FMImageLayer;
import com.fengmap.android.map.layer.FMLineLayer;
import com.fengmap.android.map.layer.FMLocationLayer;
import com.fengmap.android.map.marker.FMImageMarker;
import com.fengmap.android.map.marker.FMLineMarker;
import com.fengmap.android.map.marker.FMLocationMarker;
import com.fengmap.android.map.marker.FMNode;
import com.fengmap.android.map.marker.FMSegment;
import com.fengmap.android.utils.FMLog;
import com.fengmap.android.widget.FM3DControllerButton;
import com.fengmap.android.widget.FMNodeInfoWindow;
import com.fengmap.android.widget.FMSwitchFloorComponent;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

public class APLocationVisualizationActivity extends AppCompatActivity implements
        OnFMNodeListener,
        OnFMMapInitListener,
        OnFMMapClickListener,
        ImageViewCheckBox.OnCheckStateChangedListener,
        OnFMNavigationListener {
    private static final String TAG = "APLocationVisualizationActivity";
    private String DEFAULT_MAP_ID = "1457330643429830658";
    private FMNodeInfoWindow mInfoWindow;
    protected FMMap mFMMap;
    protected FMMapView mapView;

    private int mNumberOfRangeRequests;
    protected WifiManager mWifiManager;
    private WifiRttManager mWifiRttManager;
    private WifiScanReceiver mWifiScanReceiver;
    private List<ScanResult> mFTMCapableAPs;
    private List<ScanResult> old_mFTMCapableAPs;
    private int mMillisecondsDelayBeforeNewRangingRequest = 200;//每200ms进行一组测距定位
    // Triggers additional RangingRequests with delay (mMillisecondsDelayBeforeNewRangingRequest).
    final Handler mRangeRequestDelayHandler = new Handler();
    private Timer timer;//每隔一段时间,就重复执行扫描AP代码
    private Handler mHandler;

    private List<FMMapCoord> walking_routes;//用来保存STA的历史定位轨迹
    private List<FMGeoCoord> walking_geo_routes;//用来保存STA的历史定位点轨迹集合
    private int SNAPSHOT_POINTS_NUM = 10;
    List<double[]> snapshot_location_points; //存储连续SNAPSHOT_POINTS_NUM次定位的快照点集合 用于传入算法得到这一时段的最优定位点

    private Map<String, ApRangingHistoryInfo> map = new HashMap(); // key:AP的MAC地址，value：AP测距历史信息对象

    private FMLocationLayer mLocationLayer;//定位STA图层
    private FMImageLayer mAPLayer;

    private FMLocationMarker mLocationMarker;//定位点标记
    private FMLocationMarker mAMapLocationMarker;//高德定位点标记
    private FMGeoCoord mLocationMarkerRealTimeCoord; //定位点实时位置
    private FMGeoCoord mLocationMarkerLastTimeCoord; //定位点10s前的位置
    private int max_allowed_offset = 10; //定位点最大位移 10m

    private float mLocationMarkerAngle; //定位点方位角，结合高德定位点得到
    private float mLocationMarkerSpeed; //定位点速度

    private FMImageMarker imageStartMarker; //导航起点标记
    private FMImageMarker imageEndMarker; //导航终点标记

    //注册用户在APRangingResultActivity中输入AP经纬度等信息的广播接收器
    private ApInfoReceiver mApInfoReceiver;

    /**导航路径规划系列*/
    protected FMMapCoord mStartCoord;//起点坐标
    protected FMGeoCoord mStartGeoCoord;
    protected int mStartGroupId;//起点楼层
    protected FMImageLayer mStartImageLayer;//起点图层
    protected FMMapCoord mEndCoord;//终点坐标
    protected FMGeoCoord mEndGeoCoord;
    protected int mEndGroupId;//终点楼层id
    protected FMImageLayer mEndImageLayer;//终点图层
    protected FMLineLayer mLineLayer;//导航线图层
    protected FMNaviAnalyser mNaviAnalyser;//导航分析

    //高德地图SDK
    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明定位回调监听器
    public AMapLocationListener mLocationListener;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;

    /**导航过程相关*/
    // 约束过的定位标注
    private FMLocationMarker mHandledMarker;
    // 是否为第一人称
    private boolean mIsFirstView = true;
    // 是否为跟随状态
    private boolean mHasFollowed = true;
    // 总共距离
    private double mTotalDistance;
    // 楼层切换控件
    private FMSwitchFloorComponent mSwitchFloorComponent;
    // 上一次文字描述
    private String mLastDescription;

    // 最大约束距离 m
    private static final float DEFAULT_MAX_DISTANCE = 1.0f;

    /**语音导航合成相关*/
    private SpeechSynthesizer mTts;
    private boolean language = true;
    private Button bv_language;

    // 导航对象
    protected FMNavigation mNavigation;
    // 导航配置
    protected FMNaviOption mNaviOption;

    // 起点标志物配置
//    protected FMPointOption mStartOption = new FMPointOption();
    // 终点标志物配置
//    protected FMPointOption mEndOption = new FMPointOption();

    // 路径是否计算完成
    protected boolean isRouteCalculated;

    // 地图是否加载完成
    protected boolean isMapLoaded;
    
    // 点移距离视图中心点超过最大距离5米，就会触发移动动画
    protected static final double NAVI_MOVE_CENTER_MAX_DISTANCE = 5;

    // 进入导航时地图显示级别
    protected static final int NAVI_ZOOM_LEVEL = 20;

    // 计时器，每隔一段时间更新导航点位置
    private Timer mTimer;

    private boolean isInNavigationProcess = false;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        FMMapSDK.init(this);

        setContentView(R.layout.activity_aplocation_visualization);

        walking_routes = new ArrayList<>();
        walking_geo_routes = new ArrayList<>();
        snapshot_location_points = new ArrayList<>();

        mapView = (FMMapView) findViewById(R.id.mapview);
        mFMMap = mapView.getFMMap(); // 获取地图操作对象
        mFMMap.openMapById(DEFAULT_MAP_ID, true); //打开地图
        mFMMap.setOnFMMapInitListener(this);
//        mFMMap.setBackgroundColor();


        Bundle receive = this.getIntent().getExtras();
        mFTMCapableAPs = receive.getParcelableArrayList("FTMCapableAPs");
        Log.d(TAG, "onCreate: " + mFTMCapableAPs);
        old_mFTMCapableAPs = new ArrayList<>();

        mWifiRttManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);

        mApInfoReceiver = new ApInfoReceiver();
        //注册广播接收器
        registerReceiver(
                mApInfoReceiver, new IntentFilter("broadcast_ap_info"));

//        if(mFTMCapableAPs.size()>0){
//            ranging_request_to_all_FTM_capable_Aps();
//        }
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiScanReceiver = new WifiScanReceiver();
        registerReceiver(
                mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        //高德地图隐私合规校验
        AMapLocationClient.updatePrivacyShow(getApplicationContext(),true,true);
        AMapLocationClient.updatePrivacyAgree(getApplicationContext(),true);

        SpeechUtility.createUtility(getApplicationContext(), SpeechConstant.APPID + "=57673ee1");
        //创建语音合成SpeechSynthesizer对象
        createSynthesizer();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();

    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        //取消注册广播接收器
        unregisterReceiver(mApInfoReceiver);
        unregisterReceiver(mWifiScanReceiver);
        //注销定时事件
        timer.cancel();
        mTimer.cancel();
        //停止高德定位
        mLocationClient.stopLocation();
    }

    @Override
    public void onCheckStateChanged(View view, boolean isChecked) {
        switch (view.getId()) {
            case R.id.btn_3d: {
                setViewMode();
            }
            break;
            case R.id.btn_view: {
                setViewState(isChecked);
            }
            break;
            case R.id.btn_locate: {
                setFollowState(isChecked);
            }
            break;
            default:
                break;
        }
    }

    @Override
    public void onCrossGroupId(int lastGroupId, int currGroupId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFMMap.setFocusByGroupId(currGroupId, null);
                updateLocateGroupView();
            }
        });
    }

    public void updateLocateGroupView() {
        int groupSize = mFMMap.getFMMapInfo().getGroupSize();
        int position = groupSize - mFMMap.getFocusGroupId();
        mSwitchFloorComponent.setSelected(position);
    }

    /**
     * 设置地图2、3D效果
     */
    private void setViewMode() {
        if (mFMMap.getCurrentFMViewMode() == FMViewMode.FMVIEW_MODE_2D) {
            mFMMap.setFMViewMode(FMViewMode.FMVIEW_MODE_3D);
        } else {
            mFMMap.setFMViewMode(FMViewMode.FMVIEW_MODE_2D);
        }
    }

    /**
     * 设置是否为第一人称
     *
     * @param enable true 第一人称
     *               false 第三人称
     */
    private void setViewState(boolean enable) {
        this.mIsFirstView = !enable;
        setFloorControlEnable();
    }

    /**
     * 设置楼层控件是否可用
     */
    private void setFloorControlEnable() {
        if (getFloorControlEnable()) {
            mSwitchFloorComponent.close();
            mSwitchFloorComponent.setEnabled(false);
        } else {
            mSwitchFloorComponent.setEnabled(true);
        }
    }
    /**
     * 楼层控件是否可以使用。
     */
    private boolean getFloorControlEnable() {
        return mHasFollowed || mIsFirstView;
    }
    /**
     * 设置跟随状态
     *
     * @param enable true 跟随
     *               false 不跟随
     */
    private void setFollowState(boolean enable) {
        mHasFollowed = enable;
        setFloorControlEnable();
    }

    /**
     * 开始重新规划行走路线
     *
     * @param currentCoord 当前已经偏移的坐标
     */
    private void scheduleResetRoute(FMGeoCoord currentCoord) {
        startSpeaking("您已偏离路线，正在为您重新规划。");
        mStartCoord = currentCoord.getCoord();
        mStartGroupId = currentCoord.getGroupId();
        createStartImageMarker(currentCoord);
        analyzeNavigation();
        if (isRouteCalculated) {
            startNavigation();
        }
    }

    @Override
    public void onWalking(FMNavigationInfo fmNavigationInfo) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // 定位位置的偏移距离
                double offset = fmNavigationInfo.getOffsetDistance();

                FMLog.le("offset distance", ""+ offset);

                // 被约束过的点
                FMGeoCoord contraintedCoord = fmNavigationInfo.getPosition();
                if (offset > DEFAULT_MAX_DISTANCE) {
                    // 重新规划路径
                    scheduleResetRoute(fmNavigationInfo.getRealPosition());
                    return;
                }

                // 更新定位标志物
                updateHandledMarker(contraintedCoord, fmNavigationInfo.getAngle());

                // 更新路段显示信息
                updateWalkRouteLine(fmNavigationInfo);

                // 更新导航配置
                updateNavigationOption();
            }
        });
    }

    private void updateNavigationOption() {
        mNaviOption.setFollowAngle(mIsFirstView);
        mNaviOption.setFollowPosition(mHasFollowed);
    }

    /**
     * 更新行走距离和文字导航。
     */
    private void updateWalkRouteLine(FMNavigationInfo info) {
        // 剩余时间
        int timeByWalk = ConvertUtils.getTimeByWalk(info.getSurplusDistance());

        // 导航路段描述
        String description = info.getNaviText();

        String viewText = getResources().getString(R.string.label_walk_format, info.getSurplusDistance(),
                timeByWalk, description);

        ViewHelper.setViewText(APLocationVisualizationActivity.this, R.id.txt_info, viewText);

        if (!description.equals(mLastDescription)) {
            mLastDescription = description;
            startSpeaking(mLastDescription);
        }
    }
    /**
     * 更新约束定位点
     *
     * @param coord 坐标
     */
    private void updateHandledMarker(FMGeoCoord coord, float angle) {
        if (mHandledMarker == null) {
            mHandledMarker = ViewHelper.buildLocationMarker(coord.getGroupId(), coord.getCoord(), angle);
            mLocationLayer.addMarker(mHandledMarker);
        } else {
            mHandledMarker.updateAngleAndPosition(coord.getGroupId(), angle, coord.getCoord());
        }
    }

    /**
     * 删除导航约束定位点标记
     */
    private void removeHandledMarker(){
        if (mHandledMarker != null) {
            mLocationLayer.removeMarker(mHandledMarker);
            mHandledMarker = null;
        }
    }

    @Override
    public void onComplete() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String description = "到达目的地，步行导航结束。";

                String info = getResources().getString(R.string.label_walk_format, 0f,
                        0, description);
                ViewHelper.setViewText(APLocationVisualizationActivity.this, R.id.txt_info, info);

                startSpeaking(description);
            }
        });
    }

    /**
     * 主要目的，接收mWifiManager.startScan()的AP扫描结果
     * 重新装载 mFTMCapableAPs
     */
    private class WifiScanReceiver extends BroadcastReceiver {

        // This is checked via mLocationPermissionApproved boolean
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            old_mFTMCapableAPs = new ArrayList<>(mFTMCapableAPs); // 注意这里不能直接赋=，相当于传引用，当mFTMCapableAPs变化，old_mFTMCapableAPs也会变化
            mFTMCapableAPs.clear();


            List<ScanResult> scanResults = mWifiManager.getScanResults();
            for (ScanResult scanResult : scanResults) {
                if (scanResult.is80211mcResponder() && !mFTMCapableAPs.contains(scanResult)) {
                    mFTMCapableAPs.add(scanResult);
                } else {
                    //通过反射验证FTMCapableAP
                    Class cls = scanResult.getClass();
                    try {
                        Field flags = cls.getDeclaredField("flags");
                        flags.set(scanResult, 0x0000000000000002);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    //再进行一次测距request
                    RangingRequest rangingRequest =
                            new RangingRequest.Builder().addAccessPoint(scanResult).build();
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }

                    final boolean[] is_AP_supported_FTM = {false};
                    mWifiRttManager.startRanging(
                            rangingRequest, getApplication().getMainExecutor(), new RangingResultCallback() {
                                @Override
                                public void onRangingFailure(int code) {
                                    Log.e(TAG, "onRangingFailure: " + code);
                                    //修改FLAGS后依然测距失败，那就恢复之前的状态
                                    try {
                                        Field flags = cls.getDeclaredField("flags");
                                        flags.set(scanResult, 0);
                                    } catch (NoSuchFieldException | IllegalAccessException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onRangingResults(@NonNull List<RangingResult> results) {
                                    Log.d(TAG, "onRangingResults: " + results);

                                    for(RangingResult rr : results){
                                        if(rr.getStatus() == RangingResult.STATUS_SUCCESS && !mFTMCapableAPs.contains(scanResult)){
                                            synchronized (this){
                                                mFTMCapableAPs.add(scanResult);
                                                is_AP_supported_FTM[0] = true;
                                            }
                                        }
                                        else{
                                            //修改FLAGS后依然测距失败，那就恢复之前的状态
                                            try {
                                                Field flags = cls.getDeclaredField("flags");
                                                flags.set(scanResult, 0);
                                            } catch (NoSuchFieldException | IllegalAccessException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                    }
                                }
                            });

                    if(is_AP_supported_FTM[0]==true){
                        Log.i(TAG, "onReceive: AP_supports_FTM"+scanResult.toString());
//                        mFTMCapableAPs.add(scanResult);
                    }
                }
            }

        }
    }

    public void scan_to_find_all_FTM_capable_aps(){
        mWifiManager.startScan();
    }

    public void ranging_request_to_all_FTM_capable_aps(){
        // Permission for fine location should already be granted via MainActivity (you can't get
        // to this class unless you already have permission. If they get to this class, then disable
        // fine location permission, we kick them back to main activity.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            finish();
        }
        if(mFTMCapableAPs.size()==0) return;

        mNumberOfRangeRequests++;

        RangingRequest rangingRequest =
                new RangingRequest.Builder().addAccessPoints(mFTMCapableAPs).build();


        mWifiRttManager.startRanging(
                rangingRequest, getApplication().getMainExecutor(), new RangingResultCallback() {
                    private void queueNextRangingRequest() {
                        mRangeRequestDelayHandler.postDelayed(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        ranging_request_to_all_FTM_capable_aps();
                                    }
                                },
                                mMillisecondsDelayBeforeNewRangingRequest);
                    }
                    @Override
                    public void onRangingFailure(int code) {
//                        Log.e(TAG, "onRangingFailure: "+code);
                        if(mFMMap == null) return;//说明用户已经退出了定位界面
                        queueNextRangingRequest();
                    }

                    @Override
                    public void onRangingResults(@NonNull List<RangingResult> results) {
                        Log.d(TAG, "onRangingResults: "+results);
                        if(mFMMap == null) return;//说明用户已经退出了定位界面

                        cal_STA_location(results);
                        queueNextRangingRequest();

                    }

                    /**
                     * 通过定位算法，计算AP的测距结果并添加预测的坐标至全局变量
                     */
                    public boolean cal_STA_location(@NonNull List<RangingResult> rangingResults){

                        // 预处理ap_count，得到本次成功测距结果中的已部署AP的数量，即计算既知道坐标又能返回测距结果的AP的数量
                        int ap_count = 0;
                        for(RangingResult rangingResult : rangingResults){
                            if(rangingResult.getStatus()==RangingResult.STATUS_SUCCESS){
                                if(map.containsKey(rangingResult.getMacAddress().toString())){
                                    Log.d(TAG, "cal_STA_location: ap_count: "+rangingResult.getMacAddress());
                                    ap_count++ ;
                                }
                            }
                        }
                        if(ap_count < 2) {
                            Log.e(TAG, "cal_STA_location: 至少需要2个支持FTM测量的AP才能定位，mFTMCapableAPs.size():"+mFTMCapableAPs.size());
                            for(ScanResult sr: mFTMCapableAPs){// todo mFTMCapableAPs 到后面经常size=1 or 2 有问题
                                Log.e(TAG, "mFTMCapableAPs: "+sr.SSID+" "+sr.BSSID );
                            }
                            return false;
                        }
                        double[][] positions = new double[ap_count][3]; //  部署AP的三维坐标集合
                        double[] distances = new double[ap_count]; //AP的测距信息
                        int i = 0;
                        for(RangingResult rangingResult : rangingResults){
                            if(rangingResult.getStatus()==RangingResult.STATUS_SUCCESS){
                                Log.d(TAG, "cal_STA_location: "+rangingResult);
                                if(map.containsKey(rangingResult.getMacAddress().toString())){
                                    ApRangingHistoryInfo apRangingHistoryInfo = map.get(rangingResult.getMacAddress().toString());
                                    assert apRangingHistoryInfo != null;
                                    apRangingHistoryInfo.add_ranging_record(rangingResult); //记录AP测距结果，存入历史测距数据
                                    positions[i] = map.get(rangingResult.getMacAddress().toString()).getApinfo().getPosition();//拿到AP坐标
                                    distances[i] = rangingResult.getDistanceMm() / 1000f; //本次测距结果 单位m
                                    // 0.762 参考 http://people.csail.mit.edu/bkph/other/WifiRttScanX/FTM_RTT_AP_ratings.txt
                                    i++;
                                }
                            }
                        }
                        //todo 下面两行仅调试用，记得删
//                        positions[i] = map.get("a4:b1:c1:81:47:23").getApinfo().getPosition();//拿到AP坐标
//                        distances[i] = 3 / 1000f; //本次测距结果 单位m
//                        i++;
//                        positions[i] = map.get("12:34:56:78:90:11").getApinfo().getPosition();//拿到AP坐标
//                        distances[i] = 3 / 1000f; //本次测距结果 单位m

                        //调用定位算法进行预测
                        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
                        LeastSquaresOptimizer.Optimum optimum = solver.solve();
                        double[] centroid = optimum.getPoint().toArray();
                        Log.d(TAG, "cal_STA_location: 预测坐标为 "+ Arrays.toString(centroid));

                        // 每SNAPSHOT_POINTS_NUM个坐标代入Mean_shift聚类算法求最密集点，作为最终预测坐标
                        snapshot_location_points.add(centroid);
                        if(snapshot_location_points.size() == SNAPSHOT_POINTS_NUM){
//                            Toast.makeText(getApplicationContext(),"更新定位点状态",Toast.LENGTH_SHORT).show();
                            double[] init_point = new double[centroid.length];
                            for(int x=0;x<init_point.length;x++){
                                double tmp_sum = 0;
                                for(int j=0;j<snapshot_location_points.size();j++){
                                    tmp_sum += snapshot_location_points.get(j)[x];
                                }
                                init_point[x] = tmp_sum / snapshot_location_points.size();
                            }
                            //init_point实际上就是10个点的平均坐标
                            centroid = LocationAlgorithm.mean_shift(snapshot_location_points,init_point);
                            snapshot_location_points.clear();
                            FMGeoCoord coord = null;
                            //如果是首次定位, 或者用户移动速度>0, 才录入定位坐标
                            if(walking_routes.size()==0 || mLocationMarkerSpeed>0){
                                Log.i(TAG, "cal_STA_location: mLocationMarkerSpeed:"+mLocationMarkerSpeed);
                                if(centroid.length==3){
                                    walking_routes.add(new FMMapCoord(centroid[0],centroid[1],centroid[2]));
                                    coord = new FMGeoCoord(mFMMap.getFocusGroupId(),//todo 默认单层定位，待优化
                                            new FMMapCoord(centroid[0], centroid[1],centroid[2]));
                                }
                                else if(centroid.length==2){
                                    walking_routes.add(new FMMapCoord(centroid[0],centroid[1]));
                                    coord = new FMGeoCoord(mFMMap.getFocusGroupId(),//todo 默认单层定位，待优化
                                            new FMMapCoord(centroid[0], centroid[1]));
                                }
                                //todo 是否需要检验新定位点是否漂移？
                                walking_geo_routes.add(coord);
                                mLocationMarkerRealTimeCoord = coord;
                            }


                            //更新定位点在地图上的位置,如果不在导航过程中就更新实时位置
                            if(!isInNavigationProcess){
                                updateLocationMarker();
                            }else{//如果在导航中就暂时移除定位点标记
                                removeLocationMarker();
                            }
                        }

//                        walking_routes.add(new FMMapCoord(centroid[0],centroid[1],centroid[2]));
//                        FMGeoCoord coord = new FMGeoCoord(mFMMap.getFocusGroupId(),//todo 默认单层定位，待优化
//                                new FMMapCoord(centroid[0], centroid[1],centroid[2]));
//                        walking_geo_routes.add(coord);
//                        mLocationMarkerRealTimeCoord = coord;
//                        //更新定位点在地图上的位置,如果不在导航过程中就更新实时位置
//                        if(!isInNavigationProcess){
//                            updateLocationMarker();
//                        }else{//如果在导航中就暂时移除定位点标记
//                            removeLocationMarker();
//                        }
                        return true;
                    }



                });
    }
    public void updateLocationMarker(){
        if (mLocationMarker == null) {
            mLocationMarker = ViewHelper.buildLocationMarker(mFMMap.getFocusGroupId(),walking_routes.get(walking_routes.size()-1),mLocationMarkerAngle);
            mLocationLayer.addMarker(mLocationMarker);
        }
        else{
            //更新定位点位置和方向
            mLocationMarker.updateAngleAndPosition(mLocationMarkerAngle,walking_routes.get(walking_routes.size()-1));
            if(mFMMap != null)
                mFMMap.moveToCenter(walking_routes.get(walking_routes.size()-1),true);
        }
    }

    public void removeLocationMarker(){
        if(mLocationMarker!=null){
            mLocationLayer.removeMarker(mLocationMarker);
            mLocationMarker = null;
        }
    }
    @Override
    public void onBackPressed(){
        if (mFMMap != null) {
            mFMMap.onDestroy();
            mFMMap = null;
        }
        if (mTts != null) {
            mTts.destroy();
        }
        if(mNavigation!=null){
            mNavigation.stop();
            mNavigation.clear();
            mNavigation.release();
        }

        // 清除定位点集合
        walking_routes.clear();
        walking_geo_routes.clear();

        //停止高德定位
        mLocationClient.stopLocation();

        super.onBackPressed();
        this.finish();
    }

    public void updateActiveAPMarker(){
        if(mFMMap == null) return;
        Toast.makeText(getApplicationContext(),"更新AP状态...",Toast.LENGTH_SHORT).show();
        //先清空旧的记录AP的marker地图标记
//        if(old_mFTMCapableAPs!=null && old_mFTMCapableAPs.size()>0){
//            for(ScanResult sr : old_mFTMCapableAPs){
//                if(map.containsKey(sr.BSSID)){
//                    ApInfo ap_info = map.get(sr.BSSID).getApinfo();
//                    removeAPImageMarker(mAPLayer,ap_info);
//                }
//            }
//        }
        //更新，重新绘制AP的ImageMarker
        for(ScanResult sr : mFTMCapableAPs){
            ApInfo ap_info = null;
            if(sr.BSSID.equals("a8:5e:45:4b:05:bc")){ //zsf_5G 左下角,张丰露工位
                ap_info = new ApInfo(sr.SSID, sr.BSSID, 12950036.9823, 4865480.2896, 0.72,2);
            }
            else if(sr.BSSID.equals("a8:5e:45:20:7d:7c")){ //ASUS_78_5G 随机中间放置 自己工位上的地上
                ap_info = new ApInfo(sr.SSID, sr.BSSID, 12950042.4683 ,4865492.3977, 0,2);
            }
            else if(sr.BSSID.equals("24:4b:fe:6c:ba:54")){ //ASUS_50_5G 右下角 王明工位上
                ap_info = new ApInfo(sr.SSID, sr.BSSID, 12950044.6284, 4865480.5960, 0.74,2);
            }
            else if(sr.BSSID.equals("24:4b:fe:6c:b8:24")){ //ASUS_20_5G 左上角 4-204门口桌上
                ap_info = new ApInfo(sr.SSID, sr.BSSID, 12950031.7031, 4865500.0258, 0.71,2);
            }
//            else if(sr.BSSID.equals("24:4b:fe:6c:b8:24")){ //ASUS_20_5G 右上角 李元杰老师工位上 壮烈牺牲 老师担心有辐射
//                ap_info = new ApInfo(sr.SSID, sr.BSSID, 12950046.5318, 4865496.7674, 0.74,2);
//            }
//            else if(sr.BSSID.equals("a4:b1:c1:81:47:23")){ //CPMPULAB_WILD 自己工位上的桌上
//                ap_info = new ApInfo(sr.SSID, sr.BSSID, 12950042.4683 ,4865492.3977, 0.74,2);
//            }
            else if(sr.BSSID.equals("a8:5e:45:74:d4:5c")){ //todo ASUS_58_5G 放在宿舍，测试路由器
                ap_info = new ApInfo(sr.SSID, sr.BSSID, 12950046.5318, 4865496.7674, 0.74,2);
            }

            else continue;
            removeAPImageMarker(mAPLayer,ap_info);
            createAPImageMarker(mAPLayer,ap_info);
        }

//        //todo 添加ImageMarker = AP，调试用
//        ApInfo ap_info = new ApInfo("WILD","a4:b1:c1:81:47:23",12950035.2929, 4865480.2503,0,2);
//        removeAPImageMarker(mAPLayer,ap_info);
//        createAPImageMarker(mAPLayer,ap_info);
//
//        //todo 再添加一个
//        ApInfo ap_info2 = new ApInfo("ASUS_13_5G","12:34:56:78:90:11",12950031.7031, 4865500.0258,0,2);
//        removeAPImageMarker(mAPLayer,ap_info2);
//        createAPImageMarker(mAPLayer,ap_info2);

    }

    @SuppressLint("HandlerLeak")
    @Override
    public void onMapInitSuccess(String s) {
        Log.d(TAG, "onMapInitSuccess: ");
        mFMMap.loadThemeById("1461273078488985602");//加载地图在线主题

        int groupId = mFMMap.getFocusGroupId();

        //定位点图层
        mLocationLayer = mFMMap.getFMLayerProxy().getFMLocationLayer();
        mFMMap.addLayer(mLocationLayer);

        //AP图片图层
        mAPLayer = mFMMap.getFMLayerProxy().getFMImageLayer(groupId);
        mAPLayer.setOnFMNodeListener(this);
//        updateActiveAPMarker();

        //线图层
        mLineLayer = mFMMap.getFMLayerProxy().getFMLineLayer();
        mFMMap.addLayer(mLineLayer);

        //导航分析
        try {
            mNaviAnalyser = FMNaviAnalyser.getFMNaviAnalyserById(DEFAULT_MAP_ID);
        } catch (FileNotFoundException | FMObjectException e) {
            e.printStackTrace();
        }

        timer=new Timer();
        // (2) 使用handler处理接收到的消息
        mHandler = new Handler(){
            @SuppressLint("HandlerLeak")
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == 0){
                    if(mLocationMarkerRealTimeCoord == null){
                        scan_to_find_all_FTM_capable_aps();//扫描当前活跃AP
                        updateActiveAPMarker();//更新活跃AP位置
                        ranging_request_to_all_FTM_capable_aps(); //更新定位STA点
                    }
                    if(mLocationMarkerLastTimeCoord == null && mLocationMarkerRealTimeCoord!=null){ //初始化
                        mLocationMarkerLastTimeCoord = mLocationMarkerRealTimeCoord;
                    }
                    if(mLocationMarkerLastTimeCoord!=null &&
                            mLocationMarkerRealTimeCoord!=null &&
                            offset_bettween(mLocationMarkerLastTimeCoord,mLocationMarkerRealTimeCoord)>max_allowed_offset) {
                        Toast.makeText(getApplicationContext(),"检测到较大位移",Toast.LENGTH_SHORT).show();
                        scan_to_find_all_FTM_capable_aps();//扫描当前活跃AP
                        updateActiveAPMarker();//更新活跃AP位置
                        ranging_request_to_all_FTM_capable_aps(); //更新定位STA点
                    }
                }
            }
        };
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // (1) 使用handler发送消息
                Message message=new Message();
                message.what=0;
                mHandler.sendMessage(message);
            }
        },0,10000);//每隔10秒使用handler发送一下消息,也就是每隔10秒执行一次,一直重复执行

        launchGaoDeLocation();
        isMapLoaded = true;
    }

    private double offset_bettween(FMGeoCoord c1,FMGeoCoord c2){
        double[] x = {c1.getCoord().x,c1.getCoord().y,c1.getCoord().z};
        double[] y = {c2.getCoord().x,c2.getCoord().y,c2.getCoord().z};
        return LocationAlgorithm.euclid_distance(x,y);
    }

    private void initNavi() {
        // 创建真实导航对象
        mNavigation = new FMActualNavigation(mFMMap);

        // 设置导航文字语种，目前支持中文（CH）英文(EN)两种模式，不区分大小写，默认中文
        mNavigation.setNaviLanguage(this, "Ch");
        // 创建模拟导航配置对象
        mNaviOption = new FMNaviOption();

        // 设置跟随模式，默认跟随
        mNaviOption.setFollowPosition(mHasFollowed);

        // 设置跟随角度（第一人视角），默认跟随
        mNaviOption.setFollowAngle(mIsFirstView);

        // 点移距离视图中心点超过最大距离5米，就会触发移动动画；若设为0，则实时居中
        mNaviOption.setNeedMoveToCenterMaxDistance(NAVI_MOVE_CENTER_MAX_DISTANCE);

        // 设置导航开始时的缩放级别，true: 导航结束时恢复开始前的缩放级别，false：保持现状
        mNaviOption.setZoomLevel(NAVI_ZOOM_LEVEL, false);

        // 设置配置
        mNavigation.setNaviOption(mNaviOption);

        // 设置导航监听接口
        mNavigation.setOnNavigationListener(this);

    }
    
    /**
     * 楼层切换控件初始化
     */
    private void initSwitchFloorComponent() {
        mSwitchFloorComponent = new FMSwitchFloorComponent(this);
        //最多显示6个
        mSwitchFloorComponent.setMaxItemCount(6);
        mSwitchFloorComponent.setEnabled(false);
        mSwitchFloorComponent.setOnFMSwitchFloorComponentListener(new FMSwitchFloorComponent.OnFMSwitchFloorComponentListener() {
            @Override
            public boolean onItemSelected(int groupId, String floorName) {
                mFMMap.setFocusByGroupId(groupId, null);
                return true;
            }
        });

        mSwitchFloorComponent.setFloorDataFromFMMapInfo(mFMMap.getFMMapInfo(), mFMMap.getFocusGroupId());

        addSwitchFloorComponent();
    }

    /**
     * 添加楼层切换按钮
     */
    private void addSwitchFloorComponent() {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        FrameLayout viewGroup = (FrameLayout) findViewById(R.id.layout_group_control);
        viewGroup.addView(mSwitchFloorComponent, lp);
    }
    
    public void launchGaoDeLocation(){
        //高德地图SDK测试
        //声明定位回调监听器
        mLocationListener = new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                if(aMapLocation!=null){
                    if(aMapLocation.getErrorCode()==0){
                        Log.d(TAG, "高德 onLocationChanged: "+aMapLocation.toString());
                        FMMapCoord coord = FMCoordTransformer.wgs2WebMercator(aMapLocation.getLongitude(),aMapLocation.getLatitude());
                        Log.d(TAG, "高德定位 FMMapCoord: "+coord);
//                        updateAMapLocationMarker(coord,aMapLocation);
                        if(walking_routes.size()>0) { //这里主要同步方向角
                            //更新定位点在地图上的位置,如果不在导航过程中就更新实时位置
                            if(!isInNavigationProcess){
                                updateLocationMarker();
                            }else{//如果在导航中就暂时移除定位点标记
                                removeLocationMarker();
                            }
                        }
                        Log.d(TAG, "高德定位方向角度: "+aMapLocation.getBearing()+" and "+aMapLocation.getBearingAccuracyDegrees());
                        mLocationMarkerAngle = -aMapLocation.getBearing();
                        Log.d(TAG, "onLocationChanged: getLocationDetail() "+aMapLocation.getLocationDetail());
                        Log.d(TAG, "onLocationChanged: getLocationQualityReport() "+ aMapLocation.getLocationQualityReport());
                        Log.d(TAG, "onLocationChanged: getConScenario() "+aMapLocation.getConScenario());
                        Log.d(TAG, "onLocationChanged: getDescription() "+aMapLocation.getDescription());
                        Log.d(TAG, "onLocationChanged: getLocationType() "+aMapLocation.getLocationType());
                        Log.d(TAG, "onLocationChanged: getSpeed() "+aMapLocation.getSpeed());
                        mLocationMarkerSpeed = aMapLocation.getSpeed();
                        Log.d(TAG, "onLocationChanged: getTrustedLevel() "+aMapLocation.getTrustedLevel());
                        Log.d(TAG, "onLocationChanged: getBuildingId() and getFloor() "+aMapLocation.getBuildingId()+" and "+aMapLocation.getFloor());
                        Log.d(TAG, "onLocationChanged: getPoiName() and getProvider() "+aMapLocation.getPoiName()+" and "+aMapLocation.getProvider());
                        Log.d(TAG, "onLocationChanged: getSatellites() "+aMapLocation.getSatellites());
                        Log.d(TAG, "onLocationChanged: getGpsAccuracyStatus() "+aMapLocation.getGpsAccuracyStatus());
                    }
                }
            }
            public void updateAMapLocationMarker(FMMapCoord coord,AMapLocation aMapLocation){
                if (mAMapLocationMarker == null) {
                    mAMapLocationMarker = ViewHelper.buildLocationMarker(mFMMap.getFocusGroupId(),coord,-aMapLocation.getBearing());
                    mLocationLayer.addMarker(mAMapLocationMarker);
                }
                else{
                    //更新定位点位置和方向
                    mAMapLocationMarker.updateAngleAndPosition(-aMapLocation.getBearing(),coord);
                }
            }

        };

        //初始化定位
        try {
            mLocationClient = new AMapLocationClient(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        //设置定位回调监听
        mLocationClient.setLocationListener(mLocationListener);
        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置定位间隔,单位毫秒,默认为2000ms，最低1000ms。
        mLocationOption.setInterval(1000);
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);
        //设置是否启用缓存策略
        mLocationOption.setLocationCacheEnable(false);
        Log.i(TAG, "launchGaoDeLocation:mLocationOption.isLocationCacheEnable() "+mLocationOption.isLocationCacheEnable());
        //设置是否返回方向角度、速度等传感器信息
        mLocationOption.setSensorEnable(true);
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        //启动定位
        mLocationClient.startLocation();
    }

    @Override
    public void onMapInitFailure(String s, int i) {
        Log.e(TAG, "onMapInitFailure: Error.");
    }

    @Override
    public boolean onUpgrade(FMMapUpgradeInfo fmMapUpgradeInfo) {
        return false;
    }

    /**
     * 显示AP信息框
     */
    private void showInfoWindow(FMImageMarker imageMarker,String ap_info) {
        if (mInfoWindow == null) {
            mInfoWindow = new FMNodeInfoWindow(mapView, R.layout.layout_info_window);
            TextView map_info = (TextView)findViewById(R.id.ap_info);
            map_info.setText(ap_info);
            mInfoWindow.setPosition(mFMMap.getFocusGroupId(), imageMarker.getPosition());

            //关闭信息框
            mInfoWindow.getView().findViewById(R.id.ap_info).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mInfoWindow.close();
                }
            });
        }

        if (mInfoWindow.isOpened()) {
            mInfoWindow.close();
        } else {
            mInfoWindow.setPosition(mFMMap.getFocusGroupId(), imageMarker.getPosition());//设置位置
            TextView map_info = (TextView)findViewById(R.id.ap_info);
            map_info.setText(ap_info);
            Log.d(TAG, "showInfoWindow: "+ap_info);
            mInfoWindow.openOnTarget(imageMarker);
        }
        mFMMap.updateMap();
    }

    @Override
    public boolean onClick(FMNode node) {
        FMImageMarker imageMarker = (FMImageMarker) node;
        Bundle ap_info_bundle = imageMarker.getBundle();
        if(ap_info_bundle != null){
            ApInfo apInfo = (ApInfo) ap_info_bundle.getSerializable("ap_info");
            //String ap_info = "SSID:WILD\nBSSID:a8:52:34:4f:3d:12\nLongitude:12949361.8178\nLatitude:4865287.2138";
            if(apInfo != null)
                showInfoWindow(imageMarker,apInfo.toString());
        }

        return true;
    }

    @Override
    public boolean onLongPress(FMNode node) {
        return false;
    }

    /**
     *
     * @param x,y 屏幕点击位置的地图坐标
     */
    @Override
    public void onMapClick(float x, float y) {
        // 获取屏幕点击位置的地图坐标
        final FMPickMapCoordResult mapCoordResult = mFMMap.pickMapCoord(x, y);
        if (mapCoordResult == null) {
            return;
        }

        // 起点
        if (mStartCoord == null) {
            clear();
            mStartCoord = mapCoordResult.getMapCoord();
            mStartGroupId = mapCoordResult.getGroupId();
            mStartGeoCoord = new FMGeoCoord(mStartGroupId, mStartCoord);
            createStartImageMarker(mStartGeoCoord);
            return;
        }

        // 终点
        if (mEndCoord == null) {
            mEndCoord = mapCoordResult.getMapCoord();
            mEndGroupId = mapCoordResult.getGroupId();
        }
        else{
            clearLineLayer();
            clearEndImageLayer();
            mEndCoord = mapCoordResult.getMapCoord();
            mEndGroupId = mapCoordResult.getGroupId();
        }
        mEndGeoCoord = new FMGeoCoord(mEndGroupId,mEndCoord);
        createEndImageMarker(mEndGeoCoord);

        // 路径规划
        analyzeNavigation();

        // 总长
        mTotalDistance = mNavigation.getSceneRouteLength();

        setSceneRouteLength(mTotalDistance);

        // todo 画完置空,记得取消注释
//        if(walking_routes.size()==0){
//            mStartCoord = null;
//            mEndCoord = null;
//        }
    }


//    private void analyzeNavigation() {
//        int type = mNaviAnalyser.analyzeNavi(mStartGroupId, mStartCoord, mEndGroupId, mEndCoord,
//                FMNaviAnalyser.FMNaviModule.MODULE_SHORTEST);
//        if (type == FMNaviAnalyser.FMRouteCalcuResult.ROUTE_SUCCESS) {
//            addLineMarker();
//            //行走总距离
//            double sceneRouteLength = mNaviAnalyser.getSceneRouteLength();
//            setSceneRouteLength(sceneRouteLength);
//        }
//        else{
//            Toast.makeText(getApplicationContext(),"该终点无法到达，请重新拾取终点",Toast.LENGTH_SHORT).show();
//        }
//    }

    /**
     * 开始分析导航
     */
    private void analyzeNavigation() {

        // 路径规划
        int ret = mNavigation.analyseRoute(FMNaviAnalyser.FMNaviModule.MODULE_SHORTEST);
        if (ret == FMNaviAnalyser.FMRouteCalcuResult.ROUTE_SUCCESS) {
            mNavigation.drawNaviLine();
            isRouteCalculated = true;
        } else {
            Toast.makeText(getApplicationContext(),"该终点无法到达，请重新拾取终点",Toast.LENGTH_SHORT).show();
            FMLog.le("failed",FMNaviAnalyser.FMRouteCalcuResult.getErrorMsg(ret));
            isRouteCalculated = false;
        }
    }
    /**
     * 格式化距离
     *
     * @param sceneRouteLength 行走总距离
     */
    private void setSceneRouteLength(double sceneRouteLength) {
        if(isRouteCalculated){
            int time = ConvertUtils.getTimeByWalk(sceneRouteLength);
            String text = getString(R.string.label_distance_format, sceneRouteLength, time);
//        Toast.makeText(getApplicationContext(),text,Toast.LENGTH_SHORT).show();
            TextView textView = ViewHelper.getView(this, R.id.txt_info);
            textView.setText(text);
        }
    }

    /**
     * 清理所有的线与起终点图层
     */
    protected void clear() {
        clearLineLayer();
        clearStartImageLayer();
        clearEndImageLayer();

        mNavigation.stop();
        mNavigation.clear();
        mNavigation.release();
    }

    /**
     * 清除线图层
     */
    protected void clearLineLayer() {
        if (mLineLayer != null) {
            mLineLayer.removeAll();
        }
    }

    /**
     * 清除起点图层
     */
    protected void clearStartImageLayer() {
        if (mStartImageLayer != null) {
//            mStartImageLayer.removeAll();
            mStartImageLayer.removeMarker(imageStartMarker);
            mFMMap.removeLayer(mStartImageLayer); // 移除图层
            mStartImageLayer = null;

            mNavigation.setStartPoint(null);
//            mNavigation.setStartOption(null);
        }

//        if(mStartOption.getBitmap() != null){
//            mStartOption.setBitmap(null);
//            mNavigation.setStartPoint(null);
//            mNavigation.setStartOption(null);
//        }


    }

    /**
     * 清除终点图层
     */
    protected void clearEndImageLayer() {
        if (mEndImageLayer != null) {
            mEndImageLayer.removeMarker(imageEndMarker);
            mFMMap.removeLayer(mEndImageLayer); // 移除图层
            mEndImageLayer = null;

            mNavigation.setEndPoint(null);
            mNavigation.setEndOption(null);
        }
//        if(mEndOption.getBitmap() != null){
//            mEndOption.setBitmap(null);
//            mNavigation.setEndPoint(null);
//            mNavigation.setEndOption(null);
//        }
    }

    /**
     * 添加线标注
     */
    protected void addLineMarker() {
        ArrayList<FMNaviResult> results = mNaviAnalyser.getNaviResults();
        // 填充导航数据
        ArrayList<FMSegment> segments = new ArrayList<>();
        for (FMNaviResult r : results) {
            int groupId = r.getGroupId();
            FMSegment s = new FMSegment(groupId, r.getPointList());
            segments.add(s);
        }
        //添加LineMarker
        FMLineMarker lineMarker = new FMLineMarker(segments);
        lineMarker.setLineWidth(3f);
        mLineLayer.addMarker(lineMarker);
    }

    /**
     * 创建起点图标
     */
    protected void createStartImageMarker(FMGeoCoord startPt) {
        clearStartImageLayer();
        // 设置起点
//        Bitmap startBmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_marker_start);
//        mStartOption.setBitmap(startBmp);

        // 添加起点图层
        mStartImageLayer = mFMMap.getFMLayerProxy().getFMImageLayer(mStartGroupId);
        // 标注物样式
        imageStartMarker = ViewHelper.buildImageMarker(getResources(), mStartCoord, R.drawable.ic_marker_start);
        mStartImageLayer.addMarker(imageStartMarker);

        mNavigation.setStartPoint(startPt);
//        mNavigation.setStartOption(mStartOption);

    }

    /**
     * 创建终点图层
     */
    protected void createEndImageMarker(FMGeoCoord endPt) {
        clearEndImageLayer();
        // 设置终点
//        Bitmap endBmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_marker_end);
//        mEndOption.setBitmap(endBmp);
        mNavigation.setEndPoint(endPt);
//        mNavigation.setEndOption(mEndOption);

        // 添加起点图层
        mEndImageLayer = mFMMap.getFMLayerProxy().getFMImageLayer(mEndGroupId);
        // 标注物样式
        imageEndMarker = ViewHelper.buildImageMarker(getResources(), mEndCoord, R.drawable.ic_marker_end);
        mEndImageLayer.addMarker(imageEndMarker);
    }

    protected void createAPImageMarker(FMImageLayer mAPLayer ,ApInfo ap_info){
        if(ap_info == null) return;
        FMMapCoord ap_coord = new FMMapCoord(ap_info.getLongitude(), ap_info.getLatitude(), ap_info.getAltitude());
        FMImageMarker imageMarker = ViewHelper.buildImageMarker(getResources(), ap_coord, R.drawable.ic_wifi);
        Bundle ap_info_bundle = new Bundle();
        ap_info_bundle.putSerializable("ap_info", (Serializable) ap_info);
        imageMarker.setBundle(ap_info_bundle);
        mAPLayer.addMarker(imageMarker);

        //把AP信息存入map对象
        ApRangingHistoryInfo apRangingHistoryInfo = new ApRangingHistoryInfo(ap_info,new ArrayList<RangingResult>());
        map.put(ap_info.getBSSID(),apRangingHistoryInfo);

    }

    protected void removeAPImageMarker(FMImageLayer imageLayer,ApInfo ap_info){

        //解决java.util.ConcurrentModificationException异常，参考：https://www.jianshu.com/p/c5b52927a61a
        Iterator<FMImageMarker> markers = imageLayer.getAll().iterator();
        synchronized (imageLayer.getAll()){
            while(markers.hasNext()){
                FMImageMarker marker = markers.next();
                if(marker.getGroupId()+1 == ap_info.getFloor_id() &&
                        marker.getPosition().toString().equals(ap_info.getFMMapCoord().toString())){
                    markers.remove();
                    imageLayer.removeMarker(marker);
                }
            }
        }
    }

    private class ApInfoReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle ap_info_bundle = intent.getBundleExtra("ap_info");
            ApInfo apInfo = (ApInfo) ap_info_bundle.getSerializable("ap_info");
            Log.d(TAG, "onReceive: "+apInfo.toString());
            Toast.makeText(getApplicationContext(),"收到广播:"+apInfo.toString(),Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 开始导航，点击后出现 确认导航 和 取消导航 选项
     * @param view
     */
    public void onClickBeginNavigation(View view){
        Log.d(TAG, "onClickBeginNavigation: ");
        initNavi();//初始化导航配置
        mFMMap.setOnFMMapClickListener(this);
        Toast.makeText(getApplicationContext(),"请顺序选取起终点",Toast.LENGTH_SHORT).show();
        LinearLayout linearLayout = findViewById(R.id.after_click_btn_start_navigation);
        if(linearLayout.getVisibility()==View.INVISIBLE){
            linearLayout.setVisibility(View.VISIBLE);
        }
        //把"开始导航"按钮的状态设为不可见
        TextView btn_start_navigation = findViewById(R.id.btn_start_navigation);
        if(btn_start_navigation.getVisibility()==View.VISIBLE){
            btn_start_navigation.setVisibility(View.INVISIBLE);
        }
        //如果存在WIFI定位结果，将最近一次定位作为"我的位置"起点
        if(walking_routes.size()>0){
            mStartCoord = walking_routes.get(walking_routes.size() - 1);
            mStartGroupId = mFMMap.getFocusGroupId(); //todo 后续跨楼层还需要优化，目前默认单层
            mStartGeoCoord = new FMGeoCoord(mStartGroupId, mStartCoord);
            createStartImageMarker(mStartGeoCoord);
        }
        startSpeaking("请在地图上点击选取目的地");

    }

    /**
     * 确认导航，点击后进入实际导航状态
     * @param view
     */
    public void onClickConfirmNavigation(View view){
        if(isInNavigationProcess) {
            Toast.makeText(getApplicationContext(),"当前正在导航",Toast.LENGTH_SHORT).show();
            return;//正在导航过程中
        }
        if(!isRouteCalculated){
            Toast.makeText(getApplicationContext(),"路径规划失败，请重新设置起终点。",Toast.LENGTH_SHORT).show();
        }
        else{
            Log.d(TAG, "onClickConfirmNavigation: ");
            startSpeaking("步行导航开始");
            //将导航操作控件布局显示为可见
            RelativeLayout relativeLayout = findViewById(R.id.ImageViewCheckBoxesLayout);
            if(relativeLayout.getVisibility()==View.INVISIBLE){
                relativeLayout.setVisibility(View.VISIBLE);
            }

            if (mSwitchFloorComponent == null) {
                initSwitchFloorComponent();
            }

            ViewHelper.setViewCheckedChangeListener(APLocationVisualizationActivity.this, R.id.btn_3d,  this);
            ViewHelper.setViewCheckedChangeListener(APLocationVisualizationActivity.this, R.id.btn_locate, this);
            ViewHelper.setViewCheckedChangeListener(APLocationVisualizationActivity.this, R.id.btn_view, this);

            startNavigation();

        }

    }
    public void startNavigation() {
        //模拟导航对象
//        FMSimulateNavigation simulateNavigation = (FMSimulateNavigation) mNavigation;
//        // 行走速度：米每秒。
//        simulateNavigation.simulate(mLocationMarkerSpeed);

        isInNavigationProcess = true;
        //移除实时定位marker
        if(mLocationMarker!=null){
            mLocationLayer.removeMarker(mLocationMarker);
            mLocationMarker = null;
        }

        // 真实导航对象
        final FMActualNavigation actualNavigation = (FMActualNavigation) mNavigation;
        actualNavigation.start();

        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // 里面会做路径约束
                actualNavigation.locate(mLocationMarkerRealTimeCoord, mLocationMarkerAngle);
            }
        },0,1000);

    }

    /**
     * 取消导航，点击后回到开始导航状态
     * @param view
     */
    public void onClickCancelNavigation(View view){
        Log.d(TAG, "onClickCancelNavigation: ");
        clear();
        if(isInNavigationProcess){
            removeHandledMarker();
            isInNavigationProcess = false;
        }
        mFMMap.setOnFMMapClickListener(null);
        //把自己所在linearLayout的状态设为不可见
        LinearLayout linearLayout = findViewById(R.id.after_click_btn_start_navigation);
        if(linearLayout.getVisibility()==View.VISIBLE){
            linearLayout.setVisibility(View.INVISIBLE);
        }
        //把开始导航按钮的状态设为可见
        TextView btn_start_navigation = findViewById(R.id.btn_start_navigation);
        if(btn_start_navigation.getVisibility()==View.INVISIBLE){
            btn_start_navigation.setVisibility(View.VISIBLE);
        }
        //清空路径提示textview
        TextView textView = ViewHelper.getView(this, R.id.txt_info);
        textView.setText("");

        //将导航操作控件布局显示为不可见
        RelativeLayout relativeLayout = findViewById(R.id.ImageViewCheckBoxesLayout);
        if(relativeLayout.getVisibility()==View.VISIBLE){
            relativeLayout.setVisibility(View.INVISIBLE);
        }

        //恢复3D视图
        mFMMap.setFMViewMode(FMViewMode.FMVIEW_MODE_3D);

        startSpeaking("取消导航");
    }

    /**
     * 初始化监听。
     */
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(TAG, "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                Log.e(TAG, "InitListener onInit error()" );
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
                Toast.makeText(getApplicationContext(),"欢迎使用FIT室内定位系统demo",Toast.LENGTH_SHORT).show();
                startSpeaking("欢迎使用清华FIT室内定位系统！");

            }
        }
    };

    /**
     * 创建语音合成SpeechSynthesizer对象
     */
    private void createSynthesizer() {
        //1.创建 SpeechSynthesizer 对象, 第二个参数： 本地合成时传 InitListener
        mTts = SpeechSynthesizer.createSynthesizer(this, mTtsInitListener);
        //2.合成参数设置，详见《 MSC Reference Manual》 SpeechSynthesizer 类
        //设置发音人（更多在线发音人，用户可参见科大讯飞附录13.2
        mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan"); //设置发音人
        mTts.setParameter(SpeechConstant.SPEED, "65");//设置语速
        mTts.setParameter(SpeechConstant.VOLUME, "80");//设置音量，范围 0~100
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD); //设置云端
    }

    /**
     * 开始语音合成
     *
     * @param inputStr 语音合成文字
     */
    private void startSpeaking(String inputStr) {
        mTts.stopSpeaking();
//        mTts.startSpeaking("说话测试", null);
        mTts.startSpeaking(inputStr, null);
    }


}
