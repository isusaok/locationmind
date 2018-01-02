package com.example.frank.locationmind;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Created by frank on 17/12/12.
 */

public class Reminder implements Parcelable, Serializable {

    private static final long serialVersionUID = 1L;

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
    public Reminder.LocationState remindieType;

    //定制序列化
    /*
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeBoolean(working);
        oos.writeDouble(lat);
        oos.writeDouble(lng);
        oos.writeDouble(diameter);
        oos.writeChars(placeDescription);
        oos.writeChars(taskDescription);
        oos.writeChars(thumbernailFile);
        oos.writeInt(remindieType.ordinal());
    }

    private void readObject(ObjectInputStream ois) throws IOException,
            ClassNotFoundException {
        ois.defaultReadObject();
        working = ois.readBoolean();
        lat = ois.readDouble();
        lng = ois.readDouble();
        diameter = ois.readDouble();
        placeDescription = (String)ois.readObject();
        taskDescription = (String)ois.readObject();
        thumbernailFile = (String)ois.readObject();
        remindieType = Reminder.LocationState.values()[ois.readInt()];
    }
    */

    public Reminder() {
    }

    public Reminder(double latD, double lngD, @Nullable String taskDs) {
        lat = latD;
        lng = lngD;
        diameter =500F;
        taskDescription = taskDs;
        working = true;
        remindieType = Reminder.LocationState.GEO_IN_REMINDIE;

    }


    //提示提醒服务的类型，可以使用或运算
    public  enum  LocationState {
        GEO_IN_REMINDIE, GEO_OUT_REMINDIE, GEO_STAY_REMINDIE
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

    public LocationState getRemindieType() {
        return remindieType;
    }

    public void setRemindieType(LocationState remindieType) {
        this.remindieType = remindieType;
    }

    @Override
    public String toString() {
        return "Reminder{" +
                "isWorking=" + working +
                ", lat=" + lat +
                ", lng=" + lng +
                ", diameter=" + diameter +
                ", placeDescription='" + placeDescription + '\'' +
                ", taskDescription='" + taskDescription + '\'' +
                ", thumbernailFile='" + thumbernailFile + '\'' +
                ", remindieType=" + remindieType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Reminder reminder = (Reminder) o;

        if (Double.compare(reminder.lat, lat) != 0) return false;
        if (Double.compare(reminder.lng, lng) != 0) return false;
        if (Double.compare(reminder.diameter, diameter) != 0) return false;
        return remindieType == reminder.remindieType;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(lat);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lng);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(diameter);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + remindieType.hashCode();
        return result;
    }

    //Parcelable interface
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
        dest.writeInt(this.remindieType == null ? -1 : this.remindieType.ordinal());
    }

    protected Reminder(Parcel in) {
        this.working = in.readByte() != 0;
        this.lat = in.readDouble();
        this.lng = in.readDouble();
        this.diameter = in.readDouble();
        this.placeDescription = in.readString();
        this.taskDescription = in.readString();
        this.thumbernailFile = in.readString();
        int tmpRemindieType = in.readInt();
        this.remindieType = tmpRemindieType == -1 ? null : LocationState.values()[tmpRemindieType];
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
}
