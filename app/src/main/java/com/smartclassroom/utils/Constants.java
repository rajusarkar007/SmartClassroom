package com.smartclassroom.utils;

public final class Constants {
    private Constants() {}

    // Roles
    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_STUDENT = "STUDENT";

    // Firestore Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_CLASSROOMS = "classrooms";
    public static final String COLLECTION_MEMBERS = "classroom_members";
    public static final String COLLECTION_JOIN_REQUESTS = "join_requests";
    public static final String COLLECTION_ATTENDANCE_SESSIONS = "attendance_sessions";
    public static final String COLLECTION_ATTENDANCE_RECORDS = "attendance_records";
    public static final String COLLECTION_ASSESSMENTS = "assessments";
    public static final String COLLECTION_STUDENT_MARKS = "student_marks";
    public static final String COLLECTION_CLASS_SCHEDULES = "class_schedules";
    public static final String COLLECTION_NOTICES = "notices";
    public static final String COLLECTION_PERSONAL_NOTES = "personal_notes";

    // Attendance Status
    public static final String STATUS_PRESENT = "PRESENT";
    public static final String STATUS_ABSENT = "ABSENT";
    public static final String STATUS_LATE = "LATE";

    // Assessment Categories
    public static final String CATEGORY_CT = "CT";
    public static final String CATEGORY_ATTENDANCE = "Attendance";
    public static final String CATEGORY_SPOT_TEST = "Spot Test";
    public static final String CATEGORY_CLASS_PERFORMANCE = "Class Performance";

    // Join Request Status
    public static final String REQ_PENDING = "PENDING";
    public static final String REQ_APPROVED = "APPROVED";
    public static final String REQ_REJECTED = "REJECTED";

    // Notice Categories
    public static final String NOTICE_REGULAR = "Regular Class";
    public static final String NOTICE_CANCELLATION = "Class Cancellation";
    public static final String NOTICE_EXTRA = "Extra Class";
    public static final String NOTICE_EXAM = "Examination";
    public static final String NOTICE_GENERAL = "General Announcement";

    // Session duration (6 months = 180 days)
    public static final long SIX_MONTHS_MILLIS = 180L * 24 * 60 * 60 * 1000L;

    // Intent extras
    public static final String EXTRA_CLASSROOM_ID = "extra_classroom_id";
    public static final String EXTRA_CLASSROOM_NAME = "extra_classroom_name";
    public static final String EXTRA_CLASSROOM_CODE = "extra_classroom_code";
    public static final String EXTRA_COURSE_CODE = "extra_course_code";
    public static final String EXTRA_SECTION = "extra_section";
    public static final String EXTRA_IS_TEACHER = "extra_is_teacher";
    public static final String EXTRA_IS_CR = "extra_is_cr";
}
