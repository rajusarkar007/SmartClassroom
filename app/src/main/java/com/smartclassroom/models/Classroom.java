package com.smartclassroom.models;

import java.io.Serializable;

public class Classroom implements Serializable {
    private String classroomId;
    private String classroomCode;
    private String classroomName;
    private String courseName;
    private String courseCode;
    private double courseCredit;
    private String department;
    private String semester;
    private String section;
    private int totalStudents;
    private int totalPlannedClasses;
    private String description;
    private String teacherId;
    private String teacherName;
    private long createdAt;

    public Classroom() {
        // Required for Firestore
    }

    public Classroom(String classroomId, String classroomCode, String classroomName,
                     String courseName, String courseCode, double courseCredit,
                     String department, String semester, String section,
                     int totalStudents, int totalPlannedClasses, String description,
                     String teacherId, String teacherName, long createdAt) {
        this.classroomId = classroomId;
        this.classroomCode = classroomCode;
        this.classroomName = classroomName;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.courseCredit = courseCredit;
        this.department = department;
        this.semester = semester;
        this.section = section;
        this.totalStudents = totalStudents;
        this.totalPlannedClasses = totalPlannedClasses;
        this.description = description;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.createdAt = createdAt;
    }

    public String getClassroomId() { return classroomId; }
    public void setClassroomId(String classroomId) { this.classroomId = classroomId; }

    public String getClassroomCode() { return classroomCode; }
    public void setClassroomCode(String classroomCode) { this.classroomCode = classroomCode; }

    public String getClassroomName() { return classroomName; }
    public void setClassroomName(String classroomName) { this.classroomName = classroomName; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public double getCourseCredit() { return courseCredit; }
    public void setCourseCredit(double courseCredit) { this.courseCredit = courseCredit; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public int getTotalStudents() { return totalStudents; }
    public void setTotalStudents(int totalStudents) { this.totalStudents = totalStudents; }

    public int getTotalPlannedClasses() { return totalPlannedClasses; }
    public void setTotalPlannedClasses(int totalPlannedClasses) { this.totalPlannedClasses = totalPlannedClasses; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
