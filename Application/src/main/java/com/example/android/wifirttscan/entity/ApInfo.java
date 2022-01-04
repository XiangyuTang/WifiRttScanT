package com.example.android.wifirttscan.entity;

import com.fengmap.android.map.geometry.FMMapCoord;

import java.io.Serializable;

public class ApInfo implements Serializable { //实现Serializable接口是为了作为bundle对象传递，否则报错
    private String SSID;
    private String BSSID; //or MAC
    private double longitude;
    private double latitude;
    private double altitude;
    private int floor_id; //楼层id

    public FMMapCoord getFMMapCoord(){
        return new FMMapCoord(this.getLongitude(),this.getLatitude(),this.getFloor_id());
    }
    public int getFloor_id() {
        return floor_id;
    }

    public void setFloor_id(int floor_id) {
        this.floor_id = floor_id;
    }

    public ApInfo() {
    }

    public ApInfo(String SSID, String BSSID, double longitude, double latitude, double altitude) {
        this.SSID = SSID;
        this.BSSID = BSSID;
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
    }
    public ApInfo(String SSID, String BSSID, double longitude, double latitude, double altitude, int floor_id) {
        this.SSID = SSID;
        this.BSSID = BSSID;
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
        this.floor_id = floor_id;
    }

    public String getSSID() {
        return SSID;
    }

    public void setSSID(String SSID) {
        this.SSID = SSID;
    }

    public String getBSSID() {
        return BSSID;
    }

    public void setBSSID(String BSSID) {
        this.BSSID = BSSID;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public void setPosition(double longitude, double latitude, double altitude){
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
    }

    public double[] getPosition() {
        return new double [] {this.longitude, this.latitude, this.altitude};
    }


    @Override
    public String toString() {
        return "SSID='" + SSID + '\'' +'\n' +
                "BSSID='" + BSSID + '\'' +'\n' +
                "Longitude=" + longitude + '°' + '\n' +
                "Latitude=" + latitude + '°' + '\n' +
                "Altitude=" + altitude + 'm'+ '\n' +
                "Floor_id=" +'第'+ floor_id +'层';

    }
}
