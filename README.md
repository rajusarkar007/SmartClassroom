# Smart Classroom – Student & Teacher Management System

Smart Classroom is an enterprise-grade Android application developed in **Java** and **XML** for Android Studio Dolphin (2022) targeting Android 7.0 (API 24) to Android 12L/13 (API 32/33). It features Firebase Authentication, Cloud Firestore real-time database, offline persistence, and the native Android `PdfDocument` engine for multi-page academic PDF report generation.

---

## 📱 Project Specifications

- **IDE:** Android Studio Dolphin (2022.3.1+)
- **Language:** Java (JDK 11 compatibility)
- **UI Architecture:** XML Layouts with ViewBinding
- **Minimum SDK:** Android 7.0 (API 24)
- **Target / Compile SDK:** API 32
- **Backend Services:** Firebase Authentication, Cloud Firestore, Firebase Storage
- **Security:** Strict Firebase Security Rules (`firestore.rules`)
- **PDF Generation:** Android Native `android.graphics.pdf.PdfDocument` API with `FileProvider`

---

## 🏗️ Architecture & Package Structure

```
com.smartclassroom
│
├── activities
│   ├── SplashActivity.java                // 6-month session validation & role redirection
│   ├── LoginActivity.java                 // Email/password, forgot password, remember me
│   ├── StudentSignupActivity.java         // Student registration
│   ├── TeacherSignupActivity.java         // Teacher registration
│   ├── TeacherMainActivity.java           // Teacher dashboard & pending join requests
│   ├── StudentMainActivity.java           // Student dashboard & enrolled courses
│   ├── CreateClassroomActivity.java       // Classroom creation & unique code generation
│   ├── ClassroomDetailActivity.java       // Classroom hub & quick module navigation
│   ├── StudentManagementActivity.java     // Student roster, manual add, search, CR toggle
│   ├── AttendanceActivity.java            // Fast single-click attendance marking (P/A/L)
│   ├── AttendanceHistoryActivity.java     // Past sessions & auto-percentage calculation
│   ├── MarksManagementActivity.java       // CTs, spot tests, grade entry, student view
│   ├── ScheduleActivity.java              // Timetable, room numbers, cancellations (CR/Teacher)
│   ├── NoticeActivity.java                // Notice board & categorization (CR/Teacher)
│   ├── ReportsActivity.java               // PDF generation, preview, share & download
│   ├── PersonalNotesActivity.java         // Private student study notes
│   └── ProfileActivity.java               // Account details & manual logout
│
├── adapters
│   ├── ClassroomAdapter.java
│   ├── StudentAdapter.java
│   ├── AttendanceAdapter.java
│   ├── AttendanceSessionAdapter.java
│   ├── AssessmentAdapter.java
│   ├── StudentMarksAdapter.java
│   ├── ScheduleAdapter.java
│   ├── NoticeAdapter.java
│   ├── NoteAdapter.java
│   └── JoinRequestAdapter.java
│
├── models
│   ├── User.java                          // Teacher or Student profile
│   ├── Classroom.java                     // Course, credits, semester, section info
│   ├── ClassroomMember.java               // Enrolled student, section, CR status
│   ├── JoinRequest.java                   // Student join request
│   ├── AttendanceSession.java             // Class #, date, topic, summary counts
│   ├── AttendanceRecord.java              // Individual status (PRESENT/ABSENT/LATE)
│   ├── Assessment.java                    // CT, spot test, max marks, date
│   ├── StudentMark.java                   // Obtained mark per student
│   ├── ClassSchedule.java                 // Class timing, room, cancellation status
│   ├── Notice.java                        // Categorized announcement
│   └── PersonalNote.java                  // Strictly private notes
│
├── repositories
│   ├── AuthRepository.java
│   ├── ClassroomRepository.java
│   ├── AttendanceRepository.java
│   ├── MarksRepository.java
│   ├── ScheduleRepository.java
│   ├── NoticeRepository.java
│   └── NotesRepository.java
│
├── utils
│   ├── Constants.java
│   ├── CodeGenerator.java                 // 6-character unique code generator
│   ├── DateUtils.java
│   └── SessionManager.java                // 6-month session cache & validations
│
└── pdf
    └── PdfReportGenerator.java            // A4 Canvas-drawn table reports
```

---

## 🔐 Firebase Security Architecture (`firestore.rules`)

The system enforces backend-level permissions through Firestore Security Rules:
1. **Users:** Role cannot be modified after registration.
2. **Attendance & Marks:** Only teachers can record or edit attendance and marks. Students have read-only access strictly to their own records.
3. **Classroom Representatives (CR):** Teachers can assign CR permissions. CRs are restricted to creating/updating class schedules and posting announcements. They are blocked from modifying marks, attendance, or student rosters.
4. **Personal Notes:** Encrypted to individual students (`studentUid == request.auth.uid`). No teacher or other student can read or write another student's notes.

---

## 📊 Core Features Implemented

### 1. Authentication & 6-Month Session Management
- Login with Email & Password.
- Password reset via Firebase Auth email.
- 180-day (`180L * 24 * 60 * 60 * 1000`) session validation stored in Firestore and checked on app launch.
- Automatic routing to `TeacherMainActivity` or `StudentMainActivity`.

### 2. Classroom Management
- Teachers create classrooms with course title, code, credits, department, semester, section, capacity, planned classes.
- System generates a 6-character unique code (e.g., `SC8K2M`).
- Students enter code to review course details and submit a join request.
- Teacher receives live notification banner and approves/rejects requests with roll number assignment.

### 3. Attendance Tracking
- Dedicated "Start Class" workflow.
- Single-click toggle between **Present (P)**, **Absent (A)**, and **Late (L)**.
- Duplicate submission prevention for the same class number and section.
- Automatic calculation of attendance percentage:
  $$\text{Attendance Percentage} = \left(\frac{\text{Total Present Classes}}{\text{Total Conducted Classes}}\right) \times 100$$

### 4. Marks & Assessments
- Support for multiple Class Tests (CTs), Attendance Marks, Spot Tests, and Class Performance.
- Max marks configuration and input validation ($0 \le \text{Obtained} \le \text{Max}$).
- Real-time Firestore sync: when a teacher updates marks, the student's dashboard reflects the new total and percentage immediately.

### 5. Schedule & Notices (with CR Permissions)
- Teachers can designate any enrolled student as Classroom Representative (CR).
- CRs can publish extra-class announcements, class cancellations, and room updates.
- General and exam notices categorized with status badges.

### 6. Private Personal Notes
- Notes categorized by subject/classroom tag.
- Strictly private to the student's UID.

### 7. Professional PDF Report Generation
Uses Android's native `PdfDocument` API to draw crisp A4 documents with institution headers, course metadata, and formatted data tables:
1. Complete Attendance Report
2. Individual Student Attendance Report
3. Complete Marks Report
4. Individual Student Marks Report
5. Section-wise Student List
6. Complete Classroom Performance Report

Supports:
- Direct PDF preview via `FileProvider`.
- System Share Intent (Email, WhatsApp, Drive).
- Download to device public storage (`Downloads/SmartClassroom`).

---

## 🚀 How to Run in Android Studio Dolphin (2022)

1. Open Android Studio Dolphin (2022.3.1).
2. Select **Open** and choose the directory:
   `C:\Users\HP\.gemini\antigravity\scratch\SmartClassroom`
3. Replace `app/google-services.json` with your Firebase project's configuration file (Enable Email/Password in Firebase Authentication and create a Cloud Firestore database in test/production mode).
4. Deploy the rules from `firestore.rules` to your Firebase Console under Firestore -> Rules.
5. Click **Sync Project with Gradle Files**.
6. Select an emulator or connected device (API 24+) and click **Run 'app'**.
