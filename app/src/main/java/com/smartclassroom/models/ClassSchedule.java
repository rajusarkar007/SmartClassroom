package com.smartclassroom.models;

import java.io.Serializable;

public class ClassSchedule implements Serializable {
    private String scheduleId;
    private String classroomId;
    private String section;
    private String title;
    private String roomNumber;
    private String date;
    private String startTime;
    private String endTime;
    private boolean cancelled;
    private boolean extraClass;
    private String createdByUid;
    private String createdByName;
    private long createdAt;

    public ClassSchedule() {
        // Required for Firestore
    }

    public ClassSchedule(String scheduleId, String classroomId, String section,
                         String title, String roomNumber, String date,
                         String startTime, String endTime, boolean cancelled,
                         boolean extraClass, String createdByUid, String createdByName,
                         long createdAt) {
        this.scheduleId = scheduleId;
        this.classroomId = classroomId;
        this.section = section;
        this.title = title;
        this.roomNumber = roomNumber;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.cancelled = cancelled;
        this.extraClass = extraClass;
        this.createdByUid = createdByUid;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
    }

    public String getScheduleId() { return scheduleId; }
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }

    public String getClassroomId() { return classroomId; }
    public void setClassroomId(String classroomId) { this.classroomId = classroomId; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    public boolean isExtraClass() { return extraClass; }
    public void setExtraClass(boolean extraClass) { this.extraClass = extraClass; }

    public String getCreatedByUid() { return createdByUid; }
    public void setCreatedByUid(String createdByUid) { this.createdByUid = createdByUid; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
