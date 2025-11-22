package com.example.cse476assignment2.model;

import com.google.gson.annotations.SerializedName;

public class LocationDto {
    @SerializedName("location_id")
    public int locationId;

    @SerializedName("name")
    public String name;

    @SerializedName("latitude")
    public Double latitude;

    @SerializedName("longitude")
    public Double longitude;
}

