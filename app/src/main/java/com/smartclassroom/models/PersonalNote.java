package com.smartclassroom.models;

import java.io.Serializable;

public class PersonalNote implements Serializable {
    private String noteId;
    private String studentUid;
    private String classroomId;
    private String classroomName;
    private String title;
    private String content;
    private long updatedAt;
    private long createdAt;

    public PersonalNote() {
        // Required for Firestore
    }

    public PersonalNote(String noteId, String studentUid, String classroomId,
                        String classroomName, String title, String content,
                        long updatedAt, long createdAt) {
        this.noteId = noteId;
        this.studentUid = studentUid;
        this.classroomId = classroomId;
        this.classroomName = classroomName;
        this.title = title;
        this.content = content;
        this.updatedAt = updatedAt;
        this.createdAt = createdAt;
    }

    public String getNoteId() { return noteId; }
    public void setNoteId(String noteId) { this.noteId = noteId; }

    public String getStudentUid() { return studentUid; }
    public void setStudentUid(String studentUid) { this.studentUid = studentUid; }

    public String getClassroomId() { return classroomId; }
    public void setClassroomId(String classroomId) { this.classroomId = classroomId; }

    public String getClassroomName() { return classroomName; }
    public void setClassroomName(String classroomName) { this.classroomName = classroomName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
