package com.example.frank.locationmind

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.EnumSet

/**
 * Created by frank on 17/12/12.
 */

class Reminder() : Parcelable, Serializable {
    //指明是否处于工作状态
    var isWorking: Boolean = false

    //经纬度数据
    var lat: Double = 0.toDouble()

    var lng: Double = 0.toDouble()

    //提醒半径
    var diameter: Double = 0.toDouble()

    //地点描述
    var placeDescription: String? = null

    //提醒描述
    var taskDescription: String? = null

    //地图视图文件
    var thumbernailFile: String? = null

    //提醒服务类型
    var ReminderType: EnumSet<Reminder.LocationState>? = null

    //if (thumbernailFile==null||thumbernailFile.trim().length()>0) return false;
    val isQualifiedReminder: Boolean
        get() {
            if (null == taskDescription || taskDescription!!.length < 1) return false
            if (lat < -90 || lat > 90) return false
            if (lng < -180 || lng > 180) return false
            return if (ReminderType == null || ReminderType!!.size < 1) false else true
        }

    constructor(parcel: Parcel) : this() {
        isWorking = parcel.readByte() != 0.toByte()
        lat = parcel.readDouble()
        lng = parcel.readDouble()
        diameter = parcel.readDouble()
        placeDescription = parcel.readString()
        taskDescription = parcel.readString()
        thumbernailFile = parcel.readString()
        ReminderType = (parcel.readSerializable() as? EnumSet<LocationState>)
    }



    //提示提醒服务的类型，可以使用或运算
    enum class LocationState : Serializable {
        GEO_IN_REMINDIE, GEO_OUT_REMINDIE, GEO_STAY_REMINDIE
    }




    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val reminder = o as Reminder?

        if (isWorking != reminder!!.isWorking) return false
        if (java.lang.Double.compare(reminder.lat, lat) != 0) return false
        if (java.lang.Double.compare(reminder.lng, lng) != 0) return false
        if (java.lang.Double.compare(reminder.diameter, diameter) != 0) return false
        if (if (placeDescription != null) placeDescription != reminder.placeDescription else reminder.placeDescription != null)
            return false
        if (taskDescription != reminder.taskDescription) return false
        if (if (thumbernailFile != null) thumbernailFile != reminder.thumbernailFile else reminder.thumbernailFile != null)
            return false
        return if (ReminderType != null) ReminderType == reminder.ReminderType else reminder.ReminderType == null
    }

    override fun hashCode(): Int {
        var result: Int
        var temp: Long
        result = if (isWorking) 1 else 0
        temp = java.lang.Double.doubleToLongBits(lat)
        result = 31 * result + (temp xor temp.ushr(32)).toInt()
        temp = java.lang.Double.doubleToLongBits(lng)
        result = 31 * result + (temp xor temp.ushr(32)).toInt()
        temp = java.lang.Double.doubleToLongBits(diameter)
        result = 31 * result + (temp xor temp.ushr(32)).toInt()
        result = 31 * result + if (placeDescription != null) placeDescription!!.hashCode() else 0
        result = 31 * result + taskDescription!!.hashCode()
        result = 31 * result + if (thumbernailFile != null) thumbernailFile!!.hashCode() else 0
        result = 31 * result + if (ReminderType != null) ReminderType!!.hashCode() else 0
        return result
    }

    fun hashCodeExThumber(): Int {
        var result: Int
        var temp: Long
        result = if (isWorking) 1 else 0
        temp = java.lang.Double.doubleToLongBits(lat)
        result = 31 * result + (temp xor temp.ushr(32)).toInt()
        temp = java.lang.Double.doubleToLongBits(lng)
        result = 31 * result + (temp xor temp.ushr(32)).toInt()
        temp = java.lang.Double.doubleToLongBits(diameter)
        result = 31 * result + (temp xor temp.ushr(32)).toInt()
        result = 31 * result + if (placeDescription != null) placeDescription!!.hashCode() else 0
        result = 31 * result + taskDescription!!.hashCode()
        result = 31 * result + if (ReminderType != null) ReminderType!!.hashCode() else 0
        return result
    }

    override fun toString(): String {
        return "Reminder{" +
                "working=" + isWorking +
                ", lat=" + lat +
                ", lng=" + lng +
                ", diameter=" + diameter +
                ", placeDescription='" + placeDescription + '\'' +
                ", taskDescription='" + taskDescription + '\'' +
                ", thumbernailFile='" + thumbernailFile + '\'' +
                ", ReminderType=" + ReminderType +
                '}'
    }

    fun thumberFilaName(): Int {
        var result: Int
        var temp: Long
        temp = java.lang.Double.doubleToLongBits(lat)
        result = (temp xor temp.ushr(32)).toInt()
        temp = java.lang.Double.doubleToLongBits(lng)
        result = 31 * result + (temp xor temp.ushr(32)).toInt()
        result = 31 * result + placeDescription!!.hashCode()
        return result
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (isWorking) 1 else 0)
        parcel.writeDouble(lat)
        parcel.writeDouble(lng)
        parcel.writeDouble(diameter)
        parcel.writeString(placeDescription)
        parcel.writeString(taskDescription)
        parcel.writeString(thumbernailFile)
        parcel.writeSerializable(this.ReminderType)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Reminder> {
        override fun createFromParcel(parcel: Parcel): Reminder {
            return Reminder(parcel)
        }

        override fun newArray(size: Int): Array<Reminder?> {
            return arrayOfNulls(size)
        }
    }


}