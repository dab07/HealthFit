package com.example.androidfit.UI.sleep;

import android.os.Parcel;
import android.os.Parcelable;

public abstract class SleepData implements Parcelable {

    private long startTimeStamp;
    private long endTimeStamp;
    private float efficiency;
    private int index;
    private int len;

    public SleepData(long st, long et, float e, int i, int l) {
        startTimeStamp = st;
        endTimeStamp = et;
        efficiency = e;
        index = i;
        len = l;
    }

    public SleepData(Parcel source) {
        startTimeStamp = source.readLong();
        endTimeStamp = source.readLong();
        efficiency = source.readFloat();
        index = source.readInt();
        len = source.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SleepData> CREATOR = new Creator<SleepData>() {
        public SleepData createFromParcel(Parcel source) {
            return new SleepData(source) {
                @Override
                public void writeToParcel(Parcel dest, int flags) {

                }
            };
        }

        @Override
        public SleepData[] newArray(int size) {
            return new SleepData[size];
        }
    };

    public long getStartTimeStamp() {
        return startTimeStamp;
    }

    public long getEndTimeStamp() {
        return endTimeStamp;
    }

    public float getEfficiency() {
        return efficiency;
    }

    public int getIndex() {
        return index;
    }

    public int getLen() {
        return len;
    }
}
