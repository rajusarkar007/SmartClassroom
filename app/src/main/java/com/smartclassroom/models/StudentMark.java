package com.smartclassroom.models;

import java.io.Serializable;

public class StudentMark implements Serializable {
    private String markId;
    private String assessmentId;
    private String classroomId;
    private String studentUid;
    private String studentRoll;
    private String studentName;
    private String section;
    private double obtainedMarks;
    private double maxMarks;
    private String remarks;
    private long updatedAt;

    public StudentMark() {
        // Required for Firestore
    }

    public StudentMark(String markId, String assessmentId, String classroomId,
                       String studentUid, String studentRoll, String studentName,
                       String section, double obtainedMarks, double maxMarks,
                       String remarks, long updatedAt) {
        this.markId = markId;
        this.assessmentId = assessmentId;
        this.classroomId = classroomId;
        this.studentUid = studentUid;
        this.studentRoll = studentRoll;
        this.studentName = studentName;
        this.section = section;
        this.obtainedMarks = obtainedMarks;
        this.maxMarks = maxMarks;
        this.remarks = remarks;
        this.updatedAt = updatedAt;
    }

    public String getMarkId() { return markId; }
    public void setMarkId(String markId) { this.markId = markId; }

    public String getAssessmentId() { return assessmentId; }
    public void setAssessmentId(String assessmentId) { this.assessmentId = assessmentId; }

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

    public double getObtainedMarks() { return obtainedMarks; }
    public void setObtainedMarks(double obtainedMarks) { this.obtainedMarks = obtainedMarks; }

    public double getMaxMarks() { return maxMarks; }
    public void setMaxMarks(double maxMarks) { this.maxMarks = maxMarks; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
