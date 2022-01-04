package com.example.android.wifirttscan.entity;

import android.net.wifi.rtt.RangingResult;

import java.util.List;

/**
 * 封装AP基本信息和测距的历史数据
 */
public class ApRangingHistoryInfo {
    private ApInfo apinfo;
    private List<RangingResult> rangingResults;

    public ApRangingHistoryInfo(ApInfo apinfo, List<RangingResult> rangingResult) {
        this.apinfo = apinfo;
        this.rangingResults = rangingResult;
    }

    public ApInfo getApinfo() {
        return apinfo;
    }

    public void setApinfo(ApInfo apinfo) {
        this.apinfo = apinfo;
    }

    public List<RangingResult> getRangingResults() {
        return rangingResults;
    }

    public void setRangingResults(List<RangingResult> rangingResult) {
        this.rangingResults = rangingResult;
    }

    /**
     * 添加一条新的测距记录
     * @param rangingResult 测距记录 {@link RangingResult}
     */
    public void add_ranging_record(RangingResult rangingResult){
        rangingResults.add(rangingResult);
    }

    /**
     * 获得最新的一次测距记录（也是最后一条测距记录）
     * @return 测距记录 {@link RangingResult}
     */
    public RangingResult get_last_ranging_record(){
        if(rangingResults==null) return null;
        else return rangingResults.get(rangingResults.size()-1);
    }

}
