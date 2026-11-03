package com.smartclassroom.models;

import java.io.Serializable;

public class AttendanceSession implements Serializable {
    private String sessionId;
    private String classroomId;
    private String section;
    private int classNumber;
    private String date;
    private String topic;
    private String teacherId;
    private int totalPresent;
    private int totalAbsent;
    private int totalLate;
    private long timestamp;

    public AttendanceSession() {
        // Required for Firestore
    }

    public AttendanceSession(String sessionId, String classroomId, String section,
                             int classNumber, String date, String topic, String teacherId,
                             int totalPresent, int totalAbsent, int totalLate, long timestamp) {
        this.sessionId = sessionId;
        this.classroomId = classroomId;
        this.section = section;
        this.classNumber = classNumber;
        this.date = date;
        this.topic = topic;
        this.teacherId = teacherId;
        this.totalPresent = totalPresent;
        this.totalAbsent = totalAbsent;
        this.totalLate = totalLate;
        this.timestamp = timestamp;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getClassroomId() { return classroomId; }
    public void setClassroomId(String classroomId) { this.classroomId = classroomId; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public int getClassNumber() { return classNumber; }
    public void setClassNumber(int classNumber) { this.classNumber = classNumber; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public int getTotalPresent() { return totalPresent; }
    public void setTotalPresent(int totalPresent) { this.totalPresent = totalPresent; }

    public int getTotalAbsent() { return totalAbsent; }
    public void setTotalAbsent(int totalAbsent) { this.totalAbsent = totalAbsent; }

    public int getTotalLate() { return totalLate; }
    public void setTotalLate(int totalLate) { this.totalLate = totalLate; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
