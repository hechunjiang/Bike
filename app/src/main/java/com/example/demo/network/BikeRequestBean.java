package com.example.demo.network;

public class BikeRequestBean {
    private String bicycleId;
    private String speed;
    private String power;
    private String duration;
    private String mileage;
    private String emission;
    private String calories;

    public String getBicycleId() {
        return bicycleId;
    }

    public void setBicycleId(String bicycleId) {
        this.bicycleId = bicycleId;
    }

    public String getSpeed() {
        return speed;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public String getPower() {
        return power;
    }

    public void setPower(String power) {
        this.power = power;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getMileage() {
        return mileage;
    }

    public void setMileage(String mileage) {
        this.mileage = mileage;
    }

    public String getEmission() {
        return emission;
    }

    public void setEmission(String emission) {
        this.emission = emission;
    }

    public String getCalories() {
        return calories;
    }

    public void setCalories(String calories) {
        this.calories = calories;
    }
}
