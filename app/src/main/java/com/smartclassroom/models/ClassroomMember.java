package com.smartclassroom.models;

import java.io.Serializable;

public class ClassroomMember implements Serializable {
    private String memberId;
    private String classroomId;
    private String studentUid;
    private String studentName;
    private String studentIdNumber;
    private String rollNumber;
    private String section;
    private boolean classRepresentative;
    private long joinedAt;

    public ClassroomMember() {
        // Required for Firestore
    }

    public ClassroomMember(String memberId, String classroomId, String studentUid,
                           String studentName, String studentIdNumber, String rollNumber,
                           String section, boolean classRepresentative, long joinedAt) {
        this.memberId = memberId;
        this.classroomId = classroomId;
        this.studentUid = studentUid;
        this.studentName = studentName;
        this.studentIdNumber = studentIdNumber;
        this.rollNumber = rollNumber;
        this.section = section;
        this.classRepresentative = classRepresentative;
        this.joinedAt = joinedAt;
    }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getClassroomId() { return classroomId; }
    public void setClassroomId(String classroomId) { this.classroomId = classroomId; }

    public String getStudentUid() { return studentUid; }
    public void setStudentUid(String studentUid) { this.studentUid = studentUid; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentIdNumber() { return studentIdNumber; }
    public void setStudentIdNumber(String studentIdNumber) { this.studentIdNumber = studentIdNumber; }

    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public boolean isClassRepresentative() { return classRepresentative; }
    public void setClassRepresentative(boolean classRepresentative) { this.classRepresentative = classRepresentative; }

    public long getJoinedAt() { return joinedAt; }
    public void setJoinedAt(long joinedAt) { this.joinedAt = joinedAt; }
}
