package com.example.frank.locationmind;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.EnumSet;

/**
 * Created by frank on 17/12/12.
 */

public class Reminder implements Parcelable, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Double WRONG_LAT = 360.0;
    private static final Double WRONG_LNG =360.0;

    //指明是否处于工作状态
    public boolean working;

    //经纬度数据
    public double lat;
    public double lng;

    //提醒半径
    public double diameter;

    //地点描述
    @Nullable
    public String placeDescription;
    //提醒描述
    @Nullable
    public String taskDescription;
    //地图视图文件
    @Nullable
    public String thumbernailFile;

    //提醒服务类型
    @Nullable
    public EnumSet<Reminder.LocationState> ReminderType;

    //提示提醒服务的类型，可以使用或运算
    public  enum  LocationState implements Serializable{
        GEO_IN_REMINDIE, GEO_OUT_REMINDIE, GEO_STAY_REMINDIE
    }


    public Reminder() {
    }

    public Reminder(double latD, double lngD, @Nullable String taskDs) {
        lat = latD;
        lng = lngD;
        diameter =500F;
        taskDescription = taskDs;
        working = true;
        ReminderType = EnumSet.of(LocationState.GEO_IN_REMINDIE);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Reminder reminder = (Reminder) o;

        if (working != reminder.working) return false;
        if (Double.compare(reminder.lat, lat) != 0) return false;
        if (Double.compare(reminder.lng, lng) != 0) return false;
        if (Double.compare(reminder.diameter, diameter) != 0) return false;
        if (placeDescription != null ? !placeDescription.equals(reminder.placeDescription) : reminder.placeDescription != null)
            return false;
        if (!taskDescription.equals(reminder.taskDescription)) return false;
        if (thumbernailFile != null ? !thumbernailFile.equals(reminder.thumbernailFile) : reminder.thumbernailFile != null)
            return false;
        return ReminderType != null ? ReminderType.equals(reminder.ReminderType) : reminder.ReminderType == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (working ? 1 : 0);
        temp = Double.doubleToLongBits(lat);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lng);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(diameter);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (placeDescription != null ? placeDescription.hashCode() : 0);
        result = 31 * result + taskDescription.hashCode();
        result = 31 * result + (thumbernailFile != null ? thumbernailFile.hashCode() : 0);
        result = 31 * result + (ReminderType != null ? ReminderType.hashCode() : 0);
        return result;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public boolean isWorking() {
        return working;
    }

    public void setWorking(boolean working) {
        this.working = working;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getDiameter() {
        return diameter;
    }

    public void setDiameter(double diameter) {
        this.diameter = diameter;
    }

    @Nullable
    public String getPlaceDescription() {
        return placeDescription;
    }

    public void setPlaceDescription(@Nullable String placeDescription) {
        this.placeDescription = placeDescription;
    }

    @Nullable
    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(@Nullable String taskDescription) {
        this.taskDescription = taskDescription;
    }

    @Nullable
    public String getThumbernailFile() {
        return thumbernailFile;
    }

    public void setThumbernailFile(@Nullable String thumbernailFile) {
        this.thumbernailFile = thumbernailFile;
    }

    public EnumSet<LocationState> getReminderType() {
        if (null == ReminderType)
            ReminderType = EnumSet.noneOf(LocationState.class);
        return ReminderType;
    }

    public void setReminderType(EnumSet<LocationState> reminderType) {
        ReminderType = reminderType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.working ? (byte) 1 : (byte) 0);
        dest.writeDouble(this.lat);
        dest.writeDouble(this.lng);
        dest.writeDouble(this.diameter);
        dest.writeString(this.placeDescription);
        dest.writeString(this.taskDescription);
        dest.writeString(this.thumbernailFile);
        dest.writeSerializable(this.ReminderType);
    }

    protected Reminder(Parcel in) {
        this.working = in.readByte() != 0;
        this.lat = in.readDouble();
        this.lng = in.readDouble();
        this.diameter = in.readDouble();
        this.placeDescription = in.readString();
        this.taskDescription = in.readString();
        this.thumbernailFile = in.readString();
        this.ReminderType = (EnumSet<LocationState>) in.readSerializable();
    }

    public static final Creator<Reminder> CREATOR = new Creator<Reminder>() {
        @Override
        public Reminder createFromParcel(Parcel source) {
            return new Reminder(source);
        }

        @Override
        public Reminder[] newArray(int size) {
            return new Reminder[size];
        }
    };

    @Override
    public String toString() {
        return "Reminder{" +
                "working=" + working +
                ", lat=" + lat +
                ", lng=" + lng +
                ", diameter=" + diameter +
                ", placeDescription='" + placeDescription + '\'' +
                ", taskDescription='" + taskDescription + '\'' +
                ", thumbernailFile='" + thumbernailFile + '\'' +
                ", ReminderType=" + ReminderType +
                '}';
    }

    public boolean isQualifiedReminder(){
        if (null==taskDescription||taskDescription.length()<1) return false;
        if (lat<(-90)||lat>90) return  false;
        if (lng<(-180)||lng>180) return false;
        if (ReminderType.size()<1) return false;
        return true;
    }
}
