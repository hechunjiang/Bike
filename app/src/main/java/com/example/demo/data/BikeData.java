package com.example.demo.data;

import org.litepal.crud.LitePalSupport;

public class BikeData extends LitePalSupport {
    private int id;
    private String time;
    private String distance;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }
}
