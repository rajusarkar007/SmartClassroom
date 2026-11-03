package com.smartclassroom.models;

import java.io.Serializable;

public class Notice implements Serializable {
    private String noticeId;
    private String classroomId;
    private String title;
    private String content;
    private String category;
    private String authorUid;
    private String authorName;
    private String authorRole;
    private long createdAt;

    public Notice() {
        // Required for Firestore
    }

    public Notice(String noticeId, String classroomId, String title, String content,
                  String category, String authorUid, String authorName, String authorRole,
                  long createdAt) {
        this.noticeId = noticeId;
        this.classroomId = classroomId;
        this.title = title;
        this.content = content;
        this.category = category;
        this.authorUid = authorUid;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.createdAt = createdAt;
    }

    public String getNoticeId() { return noticeId; }
    public void setNoticeId(String noticeId) { this.noticeId = noticeId; }

    public String getClassroomId() { return classroomId; }
    public void setClassroomId(String classroomId) { this.classroomId = classroomId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAuthorUid() { return authorUid; }
    public void setAuthorUid(String authorUid) { this.authorUid = authorUid; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
