package com.smartclassroom.models;

import java.io.Serializable;

public class JoinRequest implements Serializable {
    private String requestId;
    private String classroomId;
    private String classroomCode;
    private String classroomName;
    private String teacherId;
    private String studentUid;
    private String studentName;
    private String studentIdNumber;
    private String requestedSection;
    private String status;
    private long requestedAt;

    public JoinRequest() {
        // Required for Firestore
    }

    public JoinRequest(String requestId, String classroomId, String classroomCode,
                       String classroomName, String teacherId, String studentUid,
                       String studentName, String studentIdNumber, String requestedSection,
                       String status, long requestedAt) {
        this.requestId = requestId;
        this.classroomId = classroomId;
        this.classroomCode = classroomCode;
        this.classroomName = classroomName;
        this.teacherId = teacherId;
        this.studentUid = studentUid;
        this.studentName = studentName;
        this.studentIdNumber = studentIdNumber;
        this.requestedSection = requestedSection;
        this.status = status;
        this.requestedAt = requestedAt;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getClassroomId() { return classroomId; }
    public void setClassroomId(String classroomId) { this.classroomId = classroomId; }

    public String getClassroomCode() { return classroomCode; }
    public void setClassroomCode(String classroomCode) { this.classroomCode = classroomCode; }

    public String getClassroomName() { return classroomName; }
    public void setClassroomName(String classroomName) { this.classroomName = classroomName; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getStudentUid() { return studentUid; }
    public void setStudentUid(String studentUid) { this.studentUid = studentUid; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentIdNumber() { return studentIdNumber; }
    public void setStudentIdNumber(String studentIdNumber) { this.studentIdNumber = studentIdNumber; }

    public String getRequestedSection() { return requestedSection; }
    public void setRequestedSection(String requestedSection) { this.requestedSection = requestedSection; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getRequestedAt() { return requestedAt; }
    public void setRequestedAt(long requestedAt) { this.requestedAt = requestedAt; }
}
