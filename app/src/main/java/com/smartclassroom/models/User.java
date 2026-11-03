package com.smartclassroom.models;

import java.io.Serializable;

public class User implements Serializable {
    private String uid;
    private String fullName;
    private String email;
    private String role; // TEACHER or STUDENT
    private String identifier; // Teacher ID or Student ID
    private String institution;
    private String department;
    private long sessionExpiryTimestamp;
    private long createdAt;

    public User() {
        // Required for Firestore
    }

    public User(String uid, String fullName, String email, String role, String identifier,
                String institution, String department, long sessionExpiryTimestamp, long createdAt) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.identifier = identifier;
        this.institution = institution;
        this.department = department;
        this.sessionExpiryTimestamp = sessionExpiryTimestamp;
        this.createdAt = createdAt;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public long getSessionExpiryTimestamp() { return sessionExpiryTimestamp; }
    public void setSessionExpiryTimestamp(long sessionExpiryTimestamp) { this.sessionExpiryTimestamp = sessionExpiryTimestamp; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
