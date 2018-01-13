package com.example.frank.locationmind

import android.content.Context
import android.os.Parcel
import android.os.Parcelable

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.ArrayList

/**
 * Created by frank on 17/12/21.
 * thanks to
 * https://stackoverflow.com/questions/18000093/how-to-marshall-and-unmarshall-a-parcelable-to-a-byte-array-with-help-of-parcel/18000094#18000094
 */

object ParcelableUtil {
    fun marshall(parceable: Parcelable): ByteArray {
        val parcel = Parcel.obtain()
        parceable.writeToParcel(parcel, 0)
        val bytes = parcel.marshall()
        parcel.recycle()
        return bytes
    }

    fun unmarshall(bytes: ByteArray): Parcel {
        val parcel = Parcel.obtain()
        parcel.unmarshall(bytes, 0, bytes.size)
        parcel.setDataPosition(0) // This is extremely important!
        return parcel
    }

    fun <T> unmarshall(bytes: ByteArray, creator: Parcelable.Creator<T>): T {
        val parcel = unmarshall(bytes)
        val result = creator.createFromParcel(parcel)
        parcel.recycle()
        return result
    }


    fun parcelableToString(parcelable: Parcelable): String {
        return String(ParcelableUtil.marshall(parcelable))
    }

    fun stringToParcel(string: String): Parcel {
        val bytes = string.toByteArray()
        return unmarshall(bytes)
    }

    fun <T> stringToT(string: String, creator: Parcelable.Creator<T>): T {
        val bytes = string.toByteArray()
        return unmarshall(bytes, creator)
    }

    fun parcelableListToStringList(arrayList: ArrayList<out Parcelable>?): ArrayList<String>? {
        if (null != arrayList) {
            val stringArrayList = ArrayList<String>()
            for (pl in arrayList) {
                stringArrayList.add(parcelableToString(pl))
            }
            return stringArrayList
        }
        return null
    }

    fun <T> stringListToParcelableList(arrayList: ArrayList<String>?, creator: Parcelable.Creator<T>): ArrayList<T>? {
        if (null != arrayList) {
            val parcelableList = ArrayList<T>()
            for (st in arrayList) {
                val t = stringToT(st, creator)
                parcelableList.add(t)
            }
            return parcelableList
        }
        return null
    }

}
