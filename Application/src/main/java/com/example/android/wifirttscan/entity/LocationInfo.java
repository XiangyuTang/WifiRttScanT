package com.example.android.wifirttscan.entity;

import java.sql.Timestamp;

public class LocationInfo {
    private String mac_wlan0;
    private String mac_eth0;
    private String ipv4_addr;
    private String ipv6_addr;
    private Timestamp date;
    private int building_id;
    private int floor_id;
    private double longitude;
    private double latitude;
    private double altitude;

    public LocationInfo() {
    }

    public LocationInfo(String mac_wlan0, String mac_eth0, String ipv4_addr, String ipv6_addr, Timestamp date, int building_id, int floor_id, double longitude, double latitude, double altitude) {
        this.mac_wlan0 = mac_wlan0;
        this.mac_eth0 = mac_eth0;
        this.ipv4_addr = ipv4_addr;
        this.ipv6_addr = ipv6_addr;
        this.date = date;
        this.building_id = building_id;
        this.floor_id = floor_id;
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    private String remarks;//备注

    public String getMac_wlan0() {
        return mac_wlan0;
    }

    public void setMac_wlan0(String mac_wlan0) {
        this.mac_wlan0 = mac_wlan0;
    }

    public String getMac_eth0() {
        return mac_eth0;
    }

    public void setMac_eth0(String mac_eth0) {
        this.mac_eth0 = mac_eth0;
    }

    public String getIpv4_addr() {
        return ipv4_addr;
    }

    public void setIpv4_addr(String ipv4_addr) {
        this.ipv4_addr = ipv4_addr;
    }

    public String getIpv6_addr() {
        return ipv6_addr;
    }

    public void setIpv6_addr(String ipv6_addr) {
        this.ipv6_addr = ipv6_addr;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public int getBuilding_id() {
        return building_id;
    }

    public void setBuilding_id(int building_id) {
        this.building_id = building_id;
    }

    public int getFloor_id() {
        return floor_id;
    }

    public void setFloor_id(int floor_id) {
        this.floor_id = floor_id;
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
}
