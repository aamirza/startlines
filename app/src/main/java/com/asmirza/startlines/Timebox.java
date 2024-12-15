package com.asmirza.startlines;

public class Timebox {
    private long startTime;
    private long endTime;
    private boolean isScheduleCompliant;

    public Timebox(long startTime, long endTime, boolean isScheduleCompliant) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.isScheduleCompliant = isScheduleCompliant;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public boolean isScheduleCompliant() {
        return isScheduleCompliant;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setScheduleCompliant(boolean isScheduleCompliant) {
        this.isScheduleCompliant = isScheduleCompliant;
    }

    public int getDuration() {
        return (int) (endTime - startTime) / (1000 * 60);  // convert milliseconds to minutes
    }

    public boolean isComplete() {
        return endTime > startTime;
    }
}
