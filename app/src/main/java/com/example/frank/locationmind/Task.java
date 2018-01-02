package com.example.frank.locationmind;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by frank on 17/12/25.
 */

public class Task implements Parcelable, Serializable {
    private String TaskDesciption;
    private String LocationDescription;
    private Date BeginingAt;
    private Date UntilTime;

    public String getTaskDesciption() {
        return TaskDesciption;
    }

    public void setTaskDesciption(String taskDesciption) {
        TaskDesciption = taskDesciption;
    }

    public String getLocationDescription() {
        return LocationDescription;
    }

    public void setLocationDescription(String locationDescription) {
        LocationDescription = locationDescription;
    }

    public Date getBeginingAt() {
        return BeginingAt;
    }

    public void setBeginingAt(Date beginingAt) {
        BeginingAt = beginingAt;
    }

    public Date getUntilTime() {
        return UntilTime;
    }

    public void setUntilTime(Date untilTime) {
        UntilTime = untilTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Task task = (Task) o;

        if (!TaskDesciption.equals(task.TaskDesciption)) return false;
        if (!LocationDescription.equals(task.LocationDescription)) return false;
        if (!BeginingAt.equals(task.BeginingAt)) return false;
        return UntilTime.equals(task.UntilTime);
    }

    @Override
    public int hashCode() {
        int result = TaskDesciption.hashCode();
        result = 31 * result + LocationDescription.hashCode();
        result = 31 * result + BeginingAt.hashCode();
        result = 31 * result + UntilTime.hashCode();
        return result;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.TaskDesciption);
        dest.writeString(this.LocationDescription);
        dest.writeLong(this.BeginingAt != null ? this.BeginingAt.getTime() : -1);
        dest.writeLong(this.UntilTime != null ? this.UntilTime.getTime() : -1);
    }

    public Task() {
    }

    protected Task(Parcel in) {
        this.TaskDesciption = in.readString();
        this.LocationDescription = in.readString();
        long tmpBeginingAt = in.readLong();
        this.BeginingAt = tmpBeginingAt == -1 ? null : new Date(tmpBeginingAt);
        long tmpUntilTime = in.readLong();
        this.UntilTime = tmpUntilTime == -1 ? null : new Date(tmpUntilTime);
    }

    public static final Creator<Task> CREATOR = new Creator<Task>() {
        @Override
        public Task createFromParcel(Parcel source) {
            return new Task(source);
        }

        @Override
        public Task[] newArray(int size) {
            return new Task[size];
        }
    };
}
