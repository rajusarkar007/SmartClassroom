package com.smartclassroom.models;

import java.io.Serializable;

public class AttendanceRecord implements Serializable {
    private String recordId;
    private String sessionId;
    private String classroomId;
    private String studentUid;
    private String studentRoll;
    private String studentName;
    private String section;
    private String status; // PRESENT, ABSENT, LATE
    private String date;
    private int classNumber;
    private long timestamp;

    public AttendanceRecord() {
        // Required for Firestore
    }

    public AttendanceRecord(String recordId, String sessionId, String classroomId,
                            String studentUid, String studentRoll, String studentName,
                            String section, String status, String date, int classNumber, long timestamp) {
        this.recordId = recordId;
        this.sessionId = sessionId;
        this.classroomId = classroomId;
        this.studentUid = studentUid;
        this.studentRoll = studentRoll;
        this.studentName = studentName;
        this.section = section;
        this.status = status;
        this.date = date;
        this.classNumber = classNumber;
        this.timestamp = timestamp;
    }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getClassroomId() { return classroomId; }
    public void setClassroomId(String classroomId) { this.classroomId = classroomId; }

    public String getStudentUid() { return studentUid; }
    public void setStudentUid(String studentUid) { this.studentUid = studentUid; }

    public String getStudentRoll() { return studentRoll; }
    public void setStudentRoll(String studentRoll) { this.studentRoll = studentRoll; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getClassNumber() { return classNumber; }
    public void setClassNumber(int classNumber) { this.classNumber = classNumber; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
