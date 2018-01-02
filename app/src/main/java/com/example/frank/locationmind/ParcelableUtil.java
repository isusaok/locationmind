package com.example.frank.locationmind;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * Created by frank on 17/12/21.
 * thanks to
 * https://stackoverflow.com/questions/18000093/how-to-marshall-and-unmarshall-a-parcelable-to-a-byte-array-with-help-of-parcel/18000094#18000094
 */

public class ParcelableUtil {
    public static byte[] marshall(Parcelable parceable) {
        Parcel parcel = Parcel.obtain();
        parceable.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes;
    }

    public static Parcel unmarshall(byte[] bytes) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0); // This is extremely important!
        return parcel;
    }

    public static <T> T unmarshall(byte[] bytes, Parcelable.Creator<T> creator) {
        Parcel parcel = unmarshall(bytes);
        T result = creator.createFromParcel(parcel);
        parcel.recycle();
        return result;
    }


    public static String parcelableToString(Parcelable parcelable){
        return new String(ParcelableUtil.marshall(parcelable));
    }

    public static Parcel stringToParcel(String string){
            byte[] bytes = string.getBytes();
            Parcel rd = ParcelableUtil.unmarshall(bytes);
            return rd;
    }

    public static <T> T stringToT(String string, Parcelable.Creator<T> creator){
        byte[] bytes = string.getBytes();
        T result = ParcelableUtil.unmarshall(bytes,creator);//creator,eg Reminder.CREATOR
        return result;
    }

    @Nullable
    public static ArrayList<String> parcelableListToStringList(ArrayList <? extends Parcelable> arrayList){
        if (null!=arrayList){
            ArrayList<String> stringArrayList = new ArrayList<String>();
            for(Parcelable pl:arrayList){
                stringArrayList.add(parcelableToString(pl));
            }
            return stringArrayList;
        }
        return null;
    }

    public static <T> ArrayList<T> stringListToParcelableList(ArrayList<String> arrayList,Parcelable.Creator<T> creator){
        if (null!=arrayList){
            ArrayList<T> parcelableList =new ArrayList<T>();
            for(String st:arrayList){
                T t = stringToT(st, creator);
                parcelableList.add(t);
            }
            return  parcelableList;
        }
        return null;
    }

}
