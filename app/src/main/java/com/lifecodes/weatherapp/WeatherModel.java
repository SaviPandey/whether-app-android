package com.lifecodes.weatherapp;

import android.graphics.drawable.Drawable;

public class WeatherModel {

    private Double temp;
    private Drawable icon;
    private Long time;

    public WeatherModel(Double temp, Drawable icon, Long time) {
        this.temp = temp;
        this.icon = icon;
        this.time = time;
    }

    public Double getTemp() {
        return temp;
    }

    public Drawable getIcon() {
        return icon;
    }

    public Long getTime() {
        return time;
    }
}
