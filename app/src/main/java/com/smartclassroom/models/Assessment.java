package com.smartclassroom.models;

import java.io.Serializable;

public class Assessment implements Serializable {
    private String assessmentId;
    private String classroomId;
    private String section;
    private String title;
    private String category; // CT, Attendance, Spot Test, Class Performance
    private double maxMarks;
    private String date;
    private String remarks;
    private long createdAt;

    public Assessment() {
        // Required for Firestore
    }

    public Assessment(String assessmentId, String classroomId, String section,
                      String title, String category, double maxMarks,
                      String date, String remarks, long createdAt) {
        this.assessmentId = assessmentId;
        this.classroomId = classroomId;
        this.section = section;
        this.title = title;
        this.category = category;
        this.maxMarks = maxMarks;
        this.date = date;
        this.remarks = remarks;
        this.createdAt = createdAt;
    }

    public String getAssessmentId() { return assessmentId; }
    public void setAssessmentId(String assessmentId) { this.assessmentId = assessmentId; }

    public String getClassroomId() { return classroomId; }
    public void setClassroomId(String classroomId) { this.classroomId = classroomId; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getMaxMarks() { return maxMarks; }
    public void setMaxMarks(double maxMarks) { this.maxMarks = maxMarks; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
