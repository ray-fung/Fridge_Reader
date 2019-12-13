package com.example.fridgetag;

import java.io.Serializable;
import java.util.ArrayList;

/*
 * This class represents a Fridge Tag Object. The information inside this
 * object is obtained by parsing the .txt file from a fridgeTag.
 * Each object should hold exactly 30 days (or less) worth of the
 * information in the tag.
 */
public class FTObject implements Serializable {

    private static final long serialVersionUID = 6529685098267757690L;

    public ArrayList<FTDate> lastMonth;

    public FTObject() {
        lastMonth = new ArrayList<>();
    }

    public void add(String date, float avgTemp, boolean lowerAlarm,
                    double totalTimeOutRange, String alarmTriggerTime,
                    boolean upperAlarm, float maxTemp, float minTemp) {
        FTDate temp = new FTDate(date, avgTemp, lowerAlarm, totalTimeOutRange,
                alarmTriggerTime, upperAlarm, maxTemp, minTemp);
        lastMonth.add(temp);
    }

    public int size() {
        return lastMonth.size();
    }

    /*Date, average temp, Status (lower alarm?), min temp,
    status (upper alarm?), max temp*/

    public class FTDate implements Serializable {

        private static final long serialVersionUID = 6529685098267757691L;

        private String date;
        public float avgTemp;
        public boolean lowerAlarm; // true means it did not go off
        public double totalTimeOutRange;
        public String alarmTriggerTime;
        public boolean upperAlarm;
        private float maxTemp;
        private float minTemp;

        private FTDate (String date, float avgTemp, boolean lowerAlarm,
                        double totalTimeOutRange, String alarmTriggerTime,
                        boolean upperAlarm, float maxTemp, float minTemp) {
            this.date = date;
            this.avgTemp = avgTemp;
            this.lowerAlarm = lowerAlarm;
            this.totalTimeOutRange = totalTimeOutRange;
            this.alarmTriggerTime = alarmTriggerTime;
            this.upperAlarm = upperAlarm;
            this.maxTemp = maxTemp;
            this.minTemp = minTemp;
        }
    }
}
