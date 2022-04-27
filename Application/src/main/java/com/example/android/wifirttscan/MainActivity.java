/*
 * Copyright (C) 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.wifirttscan;

import static android.os.SystemClock.sleep;
import static com.example.android.wifirttscan.AccessPointRangingResultsActivity.SCAN_RESULT_EXTRA;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;

import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.wifirttscan.MyAdapter.ScanResultClickListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;


/**
 * Displays list of Access Points enabled with WifiRTT (to check distance). Requests location
 * permissions if they are not approved via secondary splash screen explaining why they are needed.
 */
public class MainActivity extends AppCompatActivity implements ScanResultClickListener {

    private static final String TAG = "MainActivity";

    private boolean mLocationPermissionApproved = false;

    List<ScanResult> mAccessPointsSupporting80211mc;//仅通过广告宣传的
    private static List<ScanResult> mAPsFTMCapable80211mc;//通过广告宣传+非广播但支持FTM测量的AP
    private int num_of_advertised_aps; // 即仅通过广告宣传支持FTM的AP数量

    private WifiManager mWifiManager;
    private WifiScanReceiver mWifiScanReceiver;
    private WifiRttManager mWifiRttManager; //add by txy

    private TextView mOutputTextView;
    private RecyclerView mRecyclerView;

    private MyAdapter mAdapter;

    //add by txy , 菜单项
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    //add by txy , 菜单项
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.about) {
            String msg = "Copyright © 2021 Tsinghua University."+"\n"
                    +"All rights reserved.️" + "\n"
                    +"Please Contact: Utter"+"\n"
                    +"Email: 1661690249@qq.com";
            Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
            toast.setText(msg);//解决Toast提示信息携带应用程序名称的现象
            toast.show();
            return false;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOutputTextView = findViewById(R.id.access_point_summary_text_view);
        mRecyclerView = findViewById(R.id.recycler_view);

        // Improve performance if you know that changes in content do not change the layout size
        // of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);

        mAccessPointsSupporting80211mc = new ArrayList<>();
        mAPsFTMCapable80211mc = new ArrayList<>();

        mAdapter = new MyAdapter(mAccessPointsSupporting80211mc, this);
//        mAdapter = new MyAdapter(mAPsFTMCapable80211mc, this);
        mRecyclerView.setAdapter(mAdapter);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiScanReceiver = new WifiScanReceiver();
        mWifiRttManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);//add by txy

        //added by txy,check whether the device support FTM or not.
        PackageManager pm = getPackageManager();
        boolean isDeviceSupportFTM = pm.hasSystemFeature(PackageManager.FEATURE_WIFI_RTT);
        if (!isDeviceSupportFTM) {
            Toast.makeText(getApplicationContext(), ":( Sorry, the current device does not support FTM. ", Toast.LENGTH_LONG).show();
            Log.e(TAG, "onCreate: Sorry, the current device does not support FTM. :(");
            finish();
        } else {
            String msg = ":) The current device supports FTM. Have fun ！";
            Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
            toast.setText(msg);//解决Toast提示信息携带应用程序名称的现象
            toast.show();
            logToUi("Copyright © 2021 THU. All rights reserved.️ ");
        }




    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();

        mLocationPermissionApproved =
                ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;

        registerReceiver(
                mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        unregisterReceiver(mWifiScanReceiver);
    }

    private void logToUi(final String message) {
        if (!message.isEmpty()) {
            Log.d(TAG, message);
            mOutputTextView.setText(message);
        }
    }

    @Override
    public void onScanResultItemClick(ScanResult scanResult) {
        Log.d(TAG, "onScanResultItemClick(): ssid: " + scanResult.SSID);

        Intent intent = new Intent(this, AccessPointRangingResultsActivity.class);
        intent.putExtra(SCAN_RESULT_EXTRA, scanResult);
        startActivity(intent);
    }

    public void onClickFindDistancesToAccessPoints(View view) {
        if (mLocationPermissionApproved) {
            logToUi(getString(R.string.retrieving_access_points));
            mWifiManager.startScan();
        } else {
            // On 23+ (M+) devices, fine location permission not granted. Request permission.
            Intent startIntent = new Intent(this, LocationPermissionRequestActivity.class);
            startActivity(startIntent);
        }
    }

    //add by txy
    public void onClickApLocationVisualization(View view) {
        if (mLocationPermissionApproved) {
            Log.d(TAG, "onClickApLocationVisualization");
            //跳转到别的activity
//            //显式启动1，class跳转
//            Intent intent = new Intent(this,APLocationVisualizationActivity.class);
//            this.startActivity(intent);
//
//            //隐式启动
//            Intent intent1 = new Intent();
//            intent1.setAction("action.nextActivity");//通过设置AndroidManifest.xml文件里面的名称找到对应的activity
//            startActivity(intent1);

            //隐式启动写法2：
            Intent intent2 = new Intent("action.apLocVisualActivity");

            //将支持FTM的AP列表传入APLocVisualActivity
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList("FTMCapableAPs", (ArrayList<? extends Parcelable>) mAPsFTMCapable80211mc);

            intent2.putExtras(bundle);

            startActivity(intent2);


        } else {
            // On 23+ (M+) devices, fine location permission not granted. Request permission.
            Intent startIntent = new Intent(this, LocationPermissionRequestActivity.class);
            startActivity(startIntent);
        }

    }


    static class SortbyRSSI implements Comparator {
        public int compare(ScanResult a, ScanResult b) {
            return b.level - a.level;
        }

        @Override
        public int compare(Object arg1, Object arg2) {
            return this.compare(((ScanResult) arg1), ((ScanResult) arg2));
        }
    }



    private class WifiScanReceiver extends BroadcastReceiver {

        private List<ScanResult> find80211mcSupportedAccessPoints(
                @NonNull List<ScanResult> originalList) {
            List<ScanResult> newList = new ArrayList<>();

            for (ScanResult scanResult : originalList) {

                if (scanResult.is80211mcResponder()) {
                    newList.add(scanResult);
                }
// comment by txy
//                if (newList.size() >= RangingRequest.getMaxPeers()) {
//                    break;
//                }
            }
            return newList;
        }

        //add by txy
        public void ftm_rtt_check(Context context,List<ScanResult> scanResults) {
//            List<ScanResult> scanResults = mWifiManager.getScanResults();

            scanResults.sort(new SortbyRSSI()); // 基于RSSI从高到低排序
            num_of_advertised_aps = 0;

//            RangingRequest.Builder builder = new RangingRequest.Builder();
//            builder.addAccessPoints(scanResults);
//            RangingRequest req = builder.build();
//            Executor executor = getApplication().getMainExecutor();
//            RangingResultCallback rangingResultCallback = new RangingResultCallback() {
//
//                @Override
//                public void onRangingFailure(int code) {
//                    Log.d("onRangingFailure", "Fail in ranging:" + Integer.toString(code));
//                    try {
//                        //把flags还原
//                        Field flags = cls.getDeclaredField("flags");
//                        flags.setAccessible(true);
//                        flags.set(result, 0);
//                    } catch (NoSuchFieldException | IllegalAccessException e) {
//                        e.printStackTrace();
//                    }
//                    Log.d(TAG, "onRangingResults: callback failure finished.");
//                }
//
//                @Override
//                public void onRangingResults(List<RangingResult> results) {
//                    Log.d("onRangingResults", "Success in ranging:" + results);
//                    // 处理数据
//                    for(RangingResult result: results){
//                        RangingResult rangingResult = results.get(0);
//                        Class cls = result.getClass();
////                            Log.d("onRangingResults", "rangingResult.getStatus():"+rangingResult.getStatus());
//                        if (rangingResult.getStatus() != RangingResult.STATUS_SUCCESS) {
//                            try {
//                                //把flags还原
//                                Field flags = cls.getDeclaredField("flags");
//                                flags.set(result, 0);
//                            } catch (NoSuchFieldException | IllegalAccessException e) {
//                                e.printStackTrace();
//                            }
//                        } else if (rangingResult.getStatus() == RangingResult.STATUS_SUCCESS) {
////                                WifiScanReceiver.this.ap_nums += 1;
//                            mAPsFTMCapable80211mc.add(result);
//                        }
//                    }
//
//                    Log.d("onRangingResults", "Callback success finished. mAPsFTMCapable80211mc：" + mAPsFTMCapable80211mc.size());
//                }
//            };
//            if (ActivityCompat.checkSelfPermission(context, permission.ACCESS_FINE_LOCATION)
//                    != PackageManager.PERMISSION_GRANTED) {
//                finish();
//            }
//            mWifiRttManager.startRanging(req, executor, rangingResultCallback);

            for (final ScanResult result : scanResults) {
                Log.d(TAG, "ScanResult:" + result.toString());
                //Count the number of aps advertised to support 80211MC
                num_of_advertised_aps += result.is80211mcResponder() ? 1 : 0;
                //todo 第一次flag可以全都设置 后面每次就需要判断之前是否已经重置过flag了
                //use Java reflection to modified the "flags" in the ScanResult,
                //In order to find the APs that respond but do not advertize
                final Class cls = result.getClass();
                try {
                    Field flags = cls.getDeclaredField("flags");
                    flags.set(result, 0x0000000000000002);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }

                //这里需要对每一个result进行一次ranging request 查看返回对结果对Status是否=0，如果=0，说明才是RTT capable的AP


                // 构建测距请求
                //https://iot-book.github.io/17_WiFi%E6%84%9F%E7%9F%A5/S3_%E6%A1%88%E4%BE%8B%EF%BC%9AWiFI%20ToF%E6%B5%8B%E8%B7%9D/
                RangingRequest.Builder builder = new RangingRequest.Builder();
                builder.addAccessPoint(result);
                RangingRequest req = builder.build();
                Executor executor = getApplication().getMainExecutor();

                RangingResultCallback rangingResultCallback = new RangingResultCallback() {

                    @Override
                    public void onRangingFailure(int code) {
                        Log.d("onRangingFailure", "Fail in ranging:" + Integer.toString(code));
                        try {
                            //把flags还原
                            Field flags = cls.getDeclaredField("flags");
                            flags.setAccessible(true);
                            flags.set(result, 0);
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "onRangingResults: callback failure finished.");
                    }

                    @Override
                    public void onRangingResults(List<RangingResult> results) {
                        Log.d("onRangingResults:", ""+results);
                        // 处理数据
                        if (results.size() == 1) {
                            RangingResult rangingResult = results.get(0);
//                            Log.d("onRangingResults", "rangingResult.getStatus():"+rangingResult.getStatus());
                            if (rangingResult.getStatus() != RangingResult.STATUS_SUCCESS) {
                                try {
                                    //把flags还原
                                    Field flags = cls.getDeclaredField("flags");
                                    flags.set(result, 0);
                                } catch (NoSuchFieldException | IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                            } else if (rangingResult.getStatus() == RangingResult.STATUS_SUCCESS) {
                                if(!mAPsFTMCapable80211mc.contains(result)){ //如果不包括再添加
                                    mAPsFTMCapable80211mc.add(result);
                                }
                            }
                        }
//                        Log.d("onRangingResults", "Callback success finished. mAPsFTMCapable80211mc：" + mAPsFTMCapable80211mc.size());
                        //刷新屏幕
                        if (scanResults.size() != 0) {
                            mAdapter.swapData(mAPsFTMCapable80211mc);
                            if (mLocationPermissionApproved) {
                                logToUi(
                                        scanResults.size()
                                                + " APs, "
                                                + mAPsFTMCapable80211mc.size()
                                                + " RTT capable."
                                                + num_of_advertised_aps
                                                + " advertise RTT.");
                            } else {
                                // TODO (jewalker): Add Snackbar regarding permissions
                                Log.d(TAG, "Permissions not allowed.");
                            }
                        }
                    }
                };
                if (ActivityCompat.checkSelfPermission(context, permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    finish();
                }
                mWifiRttManager.startRanging(req, executor, rangingResultCallback);
            }
            mAPsFTMCapable80211mc.clear();
        }

        // This is checked via mLocationPermissionApproved boolean
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> scanResults = mWifiManager.getScanResults();
            ftm_rtt_check(context,scanResults);
//            int num_of_advertised_aps = 0;
//            //add by txy
//            for (final ScanResult result : scanResults){
//                Log.d(TAG, "onReceive: ScanResult:"+result.toString());
//                //Count the number of aps advertised to support 80211MC
//                num_of_advertised_aps += result.is80211mcResponder()?1:0;
//                //todo 第一次flag可以全都设置 后面每次就需要判断之前是否已经重置过flag了
//                //use Java reflection to modified the "flags" in the ScanResult,
//                //In order to find the APs that respond but do not advertize
//                final Class cls = result.getClass();
//                try {
//                    Field flags = cls.getDeclaredField("flags");
//                    flags.set(result,0x0000000000000002);
//                } catch (NoSuchFieldException | IllegalAccessException e) {
//                    e.printStackTrace();
//                }
//
//                //这里需要对每一个result进行一次ranging request 查看返回对结果对Status是否=0，如果=0，说明才是RTT capable的AP
//
//                // 构建测距请求
//                //https://iot-book.github.io/17_WiFi%E6%84%9F%E7%9F%A5/S3_%E6%A1%88%E4%BE%8B%EF%BC%9AWiFI%20ToF%E6%B5%8B%E8%B7%9D/
//                RangingRequest.Builder builder = new RangingRequest.Builder();
//                builder.addAccessPoint(result);
//                RangingRequest req = builder.build();
//                Executor executor = getApplication().getMainExecutor();
//
//                RangingResultCallback rangingResultCallback = new RangingResultCallback(){
//
//                    @Override
//                    public void onRangingFailure(int code) {
//                        Log.d("onRangingFailure", "Fail in ranging:" + Integer.toString(code));
//                        try {
//                            //把flags还原
//                            Field flags = cls.getDeclaredField("flags");
//                            flags.setAccessible(true);
//                            flags.set(result,0);
//                        } catch (NoSuchFieldException | IllegalAccessException e) {
//                            e.printStackTrace();
//                        }
//                        Log.d(TAG, "onRangingResults: callback failure finished.");
//                    }
//                    @Override
//                    public void onRangingResults(List<RangingResult> results) {
//                        Log.d("onRangingResults", "Success in ranging:"+results);
//                        // 处理数据
//                        if(results.size()==1){
//                            RangingResult rangingResult = results.get(0);
////                            Log.d("onRangingResults", "rangingResult.getStatus():"+rangingResult.getStatus());
//                            if(rangingResult.getStatus()!=RangingResult.STATUS_SUCCESS){
//                                try {
//                                    //把flags还原
//                                    Field flags = cls.getDeclaredField("flags");
//                                    flags.set(result,0);
//                                } catch (NoSuchFieldException | IllegalAccessException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                            else if(rangingResult.getStatus()==RangingResult.STATUS_SUCCESS){
////                                WifiScanReceiver.this.ap_nums += 1;
//                                mAPsFTMCapable80211mc.add(result);
//                            }
//                        }
//                        Log.d("onRangingResults", "Callback success finished. num_of_rtt_capable_aps："+mAPsFTMCapable80211mc.size());
//                    }
//                };
//                mWifiRttManager.startRanging(req, executor, rangingResultCallback);
//
//            }


//            if (scanResults.size() != 0) {
//                if (mLocationPermissionApproved) {
////                    mAccessPointsSupporting80211mc = find80211mcSupportedAccessPoints(scanResults);
////                    mAdapter.swapData(mAccessPointsSupporting80211mc);
//
//                    mAdapter.swapData(mAPsFTMCapable80211mc);
//                    logToUi(
//                            scanResults.size()
//                                    + " APs, "
//                                    + mAPsFTMCapable80211mc.size()
//                                    + " RTT capable."
//                                    + num_of_advertised_aps
//                                    + " advertise RTT.");
//                    mAPsFTMCapable80211mc.clear();
//                } else {
//                    // TODO (jewalker): Add Snackbar regarding permissions
//                    Log.d(TAG, "Permissions not allowed.");
//                }
//            }
        }
    }
}
