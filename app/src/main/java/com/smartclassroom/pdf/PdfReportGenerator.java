package com.smartclassroom.pdf;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.smartclassroom.models.Assessment;
import com.smartclassroom.models.AttendanceRecord;
import com.smartclassroom.models.Classroom;
import com.smartclassroom.models.ClassroomMember;
import com.smartclassroom.models.StudentMark;
import com.smartclassroom.utils.DateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

public class PdfReportGenerator {

    private static final int PAGE_WIDTH = 595; // A4 standard width (points)
    private static final int PAGE_HEIGHT = 842; // A4 standard height (points)
    private static final int MARGIN = 36;

    private final Context context;

    public PdfReportGenerator(Context context) {
        this.context = context;
    }

    public File generateCompleteAttendanceReport(Classroom classroom, String section,
                                                 List<AttendanceRecord> records,
                                                 List<ClassroomMember> students) throws Exception {
        String fileName = "Attendance_Report_" + classroom.getCourseCode() + "_" + section.replace(" ", "_") + "_" + System.currentTimeMillis() + ".pdf";
        File file = getOutputFile(fileName);

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int y = drawHeader(canvas, classroom, section, "COMPLETE ATTENDANCE REPORT");

        // Table Header
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#2563EB"));
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 25, paint);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(10f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawText("Roll", MARGIN + 8, y + 17, textPaint);
        canvas.drawText("Student Name", MARGIN + 60, y + 17, textPaint);
        canvas.drawText("Present", MARGIN + 260, y + 17, textPaint);
        canvas.drawText("Absent", MARGIN + 325, y + 17, textPaint);
        canvas.drawText("Late", MARGIN + 385, y + 17, textPaint);
        canvas.drawText("Percentage", MARGIN + 440, y + 17, textPaint);

        y += 25;
        Paint rowPaint = new Paint();
        rowPaint.setTextSize(9f);
        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#E2E8F0"));
        linePaint.setStrokeWidth(1f);

        boolean alt = false;
        Paint bgPaint = new Paint();

        for (ClassroomMember student : students) {
            int pres = 0;
            int abs = 0;
            int late = 0;
            for (AttendanceRecord r : records) {
                if (student.getStudentUid().equals(r.getStudentUid())) {
                    if ("PRESENT".equalsIgnoreCase(r.getStatus())) pres++;
                    else if ("LATE".equalsIgnoreCase(r.getStatus())) late++;
                    else abs++;
                }
            }
            int total = pres + abs + late;
            double pct = total > 0 ? ((double) pres / total) * 100.0 : 0.0;

            if (alt) {
                bgPaint.setColor(Color.parseColor("#F8FAFC"));
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 22, bgPaint);
            }
            alt = !alt;

            rowPaint.setColor(Color.parseColor("#0F172A"));
            canvas.drawText(student.getRollNumber() != null ? student.getRollNumber() : "-", MARGIN + 8, y + 15, rowPaint);
            canvas.drawText(student.getStudentName(), MARGIN + 60, y + 15, rowPaint);
            canvas.drawText(String.valueOf(pres), MARGIN + 260, y + 15, rowPaint);
            canvas.drawText(String.valueOf(abs), MARGIN + 325, y + 15, rowPaint);
            canvas.drawText(String.valueOf(late), MARGIN + 385, y + 15, rowPaint);

            if (pct >= 75.0) {
                rowPaint.setColor(Color.parseColor("#16A34A"));
            } else {
                rowPaint.setColor(Color.parseColor("#DC2626"));
            }
            canvas.drawText(String.format(Locale.getDefault(), "%.1f%%", pct), MARGIN + 440, y + 15, rowPaint);

            canvas.drawLine(MARGIN, y + 22, PAGE_WIDTH - MARGIN, y + 22, linePaint);
            y += 22;

            if (y > PAGE_HEIGHT - 60) break;
        }

        drawFooter(canvas);
        document.finishPage(page);
        saveDocumentToFile(document, file);
        return file;
    }

    public File generateIndividualAttendanceReport(Classroom classroom, ClassroomMember student,
                                                   List<AttendanceRecord> records) throws Exception {
        String fileName = "Attendance_" + student.getRollNumber() + "_" + System.currentTimeMillis() + ".pdf";
        File file = getOutputFile(fileName);

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int y = drawHeader(canvas, classroom, student.getSection(), "INDIVIDUAL ATTENDANCE REPORT");

        Paint infoPaint = new Paint();
        infoPaint.setTextSize(11f);
        infoPaint.setColor(Color.parseColor("#0F172A"));
        infoPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Student: " + student.getStudentName() + " (Roll: " + student.getRollNumber() + ")", MARGIN, y, infoPaint);
        y += 25;

        // Table Header
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#2563EB"));
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 25, paint);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(10f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawText("Class #", MARGIN + 10, y + 17, textPaint);
        canvas.drawText("Date", MARGIN + 100, y + 17, textPaint);
        canvas.drawText("Status", MARGIN + 260, y + 17, textPaint);

        y += 25;
        Paint rowPaint = new Paint();
        rowPaint.setTextSize(10f);
        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#E2E8F0"));

        int presentCount = 0;
        for (AttendanceRecord r : records) {
            rowPaint.setColor(Color.parseColor("#0F172A"));
            canvas.drawText("Class " + r.getClassNumber(), MARGIN + 10, y + 16, rowPaint);
            canvas.drawText(r.getDate(), MARGIN + 100, y + 16, rowPaint);

            if ("PRESENT".equalsIgnoreCase(r.getStatus())) {
                rowPaint.setColor(Color.parseColor("#16A34A"));
                presentCount++;
            } else if ("LATE".equalsIgnoreCase(r.getStatus())) {
                rowPaint.setColor(Color.parseColor("#D97706"));
            } else {
                rowPaint.setColor(Color.parseColor("#DC2626"));
            }
            canvas.drawText(r.getStatus(), MARGIN + 260, y + 16, rowPaint);
            canvas.drawLine(MARGIN, y + 22, PAGE_WIDTH - MARGIN, y + 22, linePaint);
            y += 22;
        }

        y += 20;
        double pct = records.size() > 0 ? ((double) presentCount / records.size()) * 100.0 : 0.0;
        infoPaint.setTextSize(12f);
        infoPaint.setColor(Color.parseColor("#2563EB"));
        canvas.drawText(String.format(Locale.getDefault(), "Total Classes: %d | Present: %d | Percentage: %.1f%%",
                records.size(), presentCount, pct), MARGIN, y, infoPaint);

        drawFooter(canvas);
        document.finishPage(page);
        saveDocumentToFile(document, file);
        return file;
    }

    public File generateCompleteMarksReport(Classroom classroom, String section,
                                            List<Assessment> assessments,
                                            List<ClassroomMember> students,
                                            List<StudentMark> allMarks) throws Exception {
        String fileName = "Marks_Report_" + classroom.getCourseCode() + "_" + section.replace(" ", "_") + "_" + System.currentTimeMillis() + ".pdf";
        File file = getOutputFile(fileName);

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int y = drawHeader(canvas, classroom, section, "COMPLETE MARKS REPORT");

        // Table Header
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#2563EB"));
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 25, paint);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(10f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawText("Roll", MARGIN + 8, y + 17, textPaint);
        canvas.drawText("Student Name", MARGIN + 55, y + 17, textPaint);
        canvas.drawText("Assessments", MARGIN + 230, y + 17, textPaint);
        canvas.drawText("Total Obtained", MARGIN + 330, y + 17, textPaint);
        canvas.drawText("Max Marks", MARGIN + 415, y + 17, textPaint);
        canvas.drawText("%", MARGIN + 485, y + 17, textPaint);

        y += 25;
        Paint rowPaint = new Paint();
        rowPaint.setTextSize(9f);
        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#E2E8F0"));
        linePaint.setStrokeWidth(1f);

        for (ClassroomMember s : students) {
            double totalObt = 0.0;
            double totalMax = 0.0;
            int count = 0;

            for (StudentMark m : allMarks) {
                if (s.getStudentUid().equals(m.getStudentUid())) {
                    totalObt += m.getObtainedMarks();
                    totalMax += m.getMaxMarks();
                    count++;
                }
            }

            double pct = totalMax > 0 ? (totalObt / totalMax) * 100.0 : 0.0;

            rowPaint.setColor(Color.parseColor("#0F172A"));
            canvas.drawText(s.getRollNumber() != null ? s.getRollNumber() : "-", MARGIN + 8, y + 15, rowPaint);
            canvas.drawText(s.getStudentName(), MARGIN + 55, y + 15, rowPaint);
            canvas.drawText(count + " evaluated", MARGIN + 230, y + 15, rowPaint);
            canvas.drawText(String.format(Locale.getDefault(), "%.1f", totalObt), MARGIN + 330, y + 15, rowPaint);
            canvas.drawText(String.format(Locale.getDefault(), "%.1f", totalMax), MARGIN + 415, y + 15, rowPaint);
            canvas.drawText(String.format(Locale.getDefault(), "%.1f%%", pct), MARGIN + 485, y + 15, rowPaint);

            canvas.drawLine(MARGIN, y + 22, PAGE_WIDTH - MARGIN, y + 22, linePaint);
            y += 22;
            if (y > PAGE_HEIGHT - 60) break;
        }

        drawFooter(canvas);
        document.finishPage(page);
        saveDocumentToFile(document, file);
        return file;
    }

    public File generateIndividualMarksReport(Classroom classroom, ClassroomMember student,
                                              List<Assessment> assessments,
                                              List<StudentMark> marks) throws Exception {
        String fileName = "Marks_" + student.getRollNumber() + "_" + System.currentTimeMillis() + ".pdf";
        File file = getOutputFile(fileName);

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int y = drawHeader(canvas, classroom, student.getSection(), "INDIVIDUAL STUDENT MARKS REPORT");

        Paint infoPaint = new Paint();
        infoPaint.setTextSize(11f);
        infoPaint.setColor(Color.parseColor("#0F172A"));
        infoPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Student: " + student.getStudentName() + " (Roll: " + student.getRollNumber() + ", ID: " + student.getStudentIdNumber() + ")", MARGIN, y, infoPaint);
        y += 25;

        // Table Header
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#2563EB"));
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 25, paint);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(10f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawText("Assessment Title", MARGIN + 10, y + 17, textPaint);
        canvas.drawText("Category", MARGIN + 180, y + 17, textPaint);
        canvas.drawText("Obtained", MARGIN + 300, y + 17, textPaint);
        canvas.drawText("Max Marks", MARGIN + 380, y + 17, textPaint);
        canvas.drawText("Percentage", MARGIN + 460, y + 17, textPaint);

        y += 25;
        Paint rowPaint = new Paint();
        rowPaint.setTextSize(9f);
        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#E2E8F0"));

        double totalObt = 0;
        double totalMax = 0;

        for (StudentMark m : marks) {
            String title = "Assessment";
            String cat = "Test";
            for (Assessment a : assessments) {
                if (a.getAssessmentId().equals(m.getAssessmentId())) {
                    title = a.getTitle();
                    cat = a.getCategory();
                    break;
                }
            }

            double pct = m.getMaxMarks() > 0 ? (m.getObtainedMarks() / m.getMaxMarks()) * 100.0 : 0.0;
            totalObt += m.getObtainedMarks();
            totalMax += m.getMaxMarks();

            rowPaint.setColor(Color.parseColor("#0F172A"));
            canvas.drawText(title, MARGIN + 10, y + 15, rowPaint);
            canvas.drawText(cat, MARGIN + 180, y + 15, rowPaint);
            canvas.drawText(String.format(Locale.getDefault(), "%.1f", m.getObtainedMarks()), MARGIN + 300, y + 15, rowPaint);
            canvas.drawText(String.format(Locale.getDefault(), "%.1f", m.getMaxMarks()), MARGIN + 380, y + 15, rowPaint);
            canvas.drawText(String.format(Locale.getDefault(), "%.1f%%", pct), MARGIN + 460, y + 15, rowPaint);

            canvas.drawLine(MARGIN, y + 22, PAGE_WIDTH - MARGIN, y + 22, linePaint);
            y += 22;
        }

        y += 25;
        double grandPct = totalMax > 0 ? (totalObt / totalMax) * 100.0 : 0.0;
        infoPaint.setTextSize(12f);
        infoPaint.setColor(Color.parseColor("#2563EB"));
        canvas.drawText(String.format(Locale.getDefault(), "Grand Total: %.1f / %.1f (%.1f%%)", totalObt, totalMax, grandPct), MARGIN, y, infoPaint);

        drawFooter(canvas);
        document.finishPage(page);
        saveDocumentToFile(document, file);
        return file;
    }

    public File generateSectionStudentListReport(Classroom classroom, String section,
                                                 List<ClassroomMember> students) throws Exception {
        String fileName = "Student_List_" + classroom.getCourseCode() + "_" + section.replace(" ", "_") + "_" + System.currentTimeMillis() + ".pdf";
        File file = getOutputFile(fileName);

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int y = drawHeader(canvas, classroom, section, "SECTION-WISE STUDENT LIST");

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#2563EB"));
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 25, paint);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(10f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawText("Roll", MARGIN + 10, y + 17, textPaint);
        canvas.drawText("Student Name", MARGIN + 70, y + 17, textPaint);
        canvas.drawText("Student ID", MARGIN + 260, y + 17, textPaint);
        canvas.drawText("Section", MARGIN + 370, y + 17, textPaint);
        canvas.drawText("Role", MARGIN + 450, y + 17, textPaint);

        y += 25;
        Paint rowPaint = new Paint();
        rowPaint.setTextSize(9f);
        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#E2E8F0"));

        for (ClassroomMember s : students) {
            rowPaint.setColor(Color.parseColor("#0F172A"));
            canvas.drawText(s.getRollNumber() != null ? s.getRollNumber() : "-", MARGIN + 10, y + 15, rowPaint);
            canvas.drawText(s.getStudentName(), MARGIN + 70, y + 15, rowPaint);
            canvas.drawText(s.getStudentIdNumber() != null ? s.getStudentIdNumber() : "-", MARGIN + 260, y + 15, rowPaint);
            canvas.drawText(s.getSection() != null ? s.getSection() : section, MARGIN + 370, y + 15, rowPaint);

            if (s.isClassRepresentative()) {
                rowPaint.setColor(Color.parseColor("#2563EB"));
                canvas.drawText("Class Rep (CR)", MARGIN + 450, y + 15, rowPaint);
            } else {
                rowPaint.setColor(Color.parseColor("#64748B"));
                canvas.drawText("Student", MARGIN + 450, y + 15, rowPaint);
            }

            canvas.drawLine(MARGIN, y + 22, PAGE_WIDTH - MARGIN, y + 22, linePaint);
            y += 22;
            if (y > PAGE_HEIGHT - 60) break;
        }

        drawFooter(canvas);
        document.finishPage(page);
        saveDocumentToFile(document, file);
        return file;
    }

    public File generatePerformanceSummaryReport(Classroom classroom, String section,
                                                 List<ClassroomMember> students,
                                                 List<AttendanceRecord> attendanceRecords,
                                                 List<StudentMark> marks) throws Exception {
        String fileName = "Performance_" + classroom.getCourseCode() + "_" + section.replace(" ", "_") + "_" + System.currentTimeMillis() + ".pdf";
        File file = getOutputFile(fileName);

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int y = drawHeader(canvas, classroom, section, "CLASSROOM PERFORMANCE REPORT");

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#2563EB"));
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 25, paint);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(10f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawText("Roll", MARGIN + 8, y + 17, textPaint);
        canvas.drawText("Student Name", MARGIN + 60, y + 17, textPaint);
        canvas.drawText("Attendance %", MARGIN + 230, y + 17, textPaint);
        canvas.drawText("Total Marks", MARGIN + 330, y + 17, textPaint);
        canvas.drawText("Marks %", MARGIN + 415, y + 17, textPaint);
        canvas.drawText("Status", MARGIN + 480, y + 17, textPaint);

        y += 25;
        Paint rowPaint = new Paint();
        rowPaint.setTextSize(9f);
        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#E2E8F0"));

        for (ClassroomMember s : students) {
            int pres = 0;
            int totalClasses = 0;
            for (AttendanceRecord ar : attendanceRecords) {
                if (s.getStudentUid().equals(ar.getStudentUid())) {
                    totalClasses++;
                    if ("PRESENT".equalsIgnoreCase(ar.getStatus())) pres++;
                }
            }
            double attPct = totalClasses > 0 ? ((double) pres / totalClasses) * 100.0 : 0.0;

            double obt = 0;
            double max = 0;
            for (StudentMark sm : marks) {
                if (s.getStudentUid().equals(sm.getStudentUid())) {
                    obt += sm.getObtainedMarks();
                    max += sm.getMaxMarks();
                }
            }
            double marksPct = max > 0 ? (obt / max) * 100.0 : 0.0;

            rowPaint.setColor(Color.parseColor("#0F172A"));
            canvas.drawText(s.getRollNumber() != null ? s.getRollNumber() : "-", MARGIN + 8, y + 15, rowPaint);
            canvas.drawText(s.getStudentName(), MARGIN + 60, y + 15, rowPaint);
            canvas.drawText(String.format(Locale.getDefault(), "%.1f%%", attPct), MARGIN + 230, y + 15, rowPaint);
            canvas.drawText(String.format(Locale.getDefault(), "%.1f", obt), MARGIN + 330, y + 15, rowPaint);
            canvas.drawText(String.format(Locale.getDefault(), "%.1f%%", marksPct), MARGIN + 415, y + 15, rowPaint);

            if (attPct >= 75.0 && marksPct >= 50.0) {
                rowPaint.setColor(Color.parseColor("#16A34A"));
                canvas.drawText("Good", MARGIN + 480, y + 15, rowPaint);
            } else {
                rowPaint.setColor(Color.parseColor("#DC2626"));
                canvas.drawText("Warning", MARGIN + 480, y + 15, rowPaint);
            }

            canvas.drawLine(MARGIN, y + 22, PAGE_WIDTH - MARGIN, y + 22, linePaint);
            y += 22;
            if (y > PAGE_HEIGHT - 60) break;
        }

        drawFooter(canvas);
        document.finishPage(page);
        saveDocumentToFile(document, file);
        return file;
    }

    private int drawHeader(Canvas canvas, Classroom classroom, String section, String reportTitle) {
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.parseColor("#0F172A"));
        titlePaint.setTextSize(16f);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawText("SMART CLASSROOM ACADEMIC REPORT", MARGIN, 45, titlePaint);

        Paint subPaint = new Paint();
        subPaint.setColor(Color.parseColor("#2563EB"));
        subPaint.setTextSize(12f);
        subPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(reportTitle, MARGIN, 65, subPaint);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.parseColor("#334155"));
        textPaint.setTextSize(9.5f);

        canvas.drawText("Course: " + classroom.getCourseName() + " (" + classroom.getCourseCode() + ")", MARGIN, 85, textPaint);
        canvas.drawText("Credit: " + classroom.getCourseCredit() + " | Department: " + classroom.getDepartment(), MARGIN, 100, textPaint);
        canvas.drawText("Section: " + (section != null ? section : classroom.getSection()) + " | Semester: " + classroom.getSemester(), MARGIN, 115, textPaint);
        canvas.drawText("Teacher: " + classroom.getTeacherName() + " | Date: " + DateUtils.getCurrentDate(), MARGIN, 130, textPaint);

        Paint divPaint = new Paint();
        divPaint.setColor(Color.parseColor("#CBD5E1"));
        divPaint.setStrokeWidth(1.5f);
        canvas.drawLine(MARGIN, 140, PAGE_WIDTH - MARGIN, 140, divPaint);

        return 155;
    }

    private void drawFooter(Canvas canvas) {
        Paint footerPaint = new Paint();
        footerPaint.setColor(Color.parseColor("#94A3B8"));
        footerPaint.setTextSize(8f);
        canvas.drawText("Generated by Smart Classroom System • " + DateUtils.getCurrentDate(), MARGIN, PAGE_HEIGHT - 25, footerPaint);
    }

    private File getOutputFile(String fileName) {
        File dir = new File(context.getExternalFilesDir(null), "reports");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, fileName);
    }

    private void saveDocumentToFile(PdfDocument document, File file) throws Exception {
        FileOutputStream fos = new FileOutputStream(file);
        document.writeTo(fos);
        document.close();
        fos.flush();
        fos.close();
    }

    public void openPdf(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(intent, "Open PDF Report"));
        } catch (Exception e) {
            Toast.makeText(context, "No PDF viewer found on device", Toast.LENGTH_SHORT).show();
        }
    }

    public void sharePdf(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(intent, "Share PDF Report"));
        } catch (Exception e) {
            Toast.makeText(context, "Cannot share PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void downloadToPublicStorage(File sourceFile) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, sourceFile.getName());
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SmartClassroom");

                Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream os = context.getContentResolver().openOutputStream(uri);
                         InputStream is = Files.newInputStream(sourceFile.toPath())) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    }
                    Toast.makeText(context, "Downloaded to Downloads/SmartClassroom", Toast.LENGTH_LONG).show();
                }
            } else {
                File publicDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "SmartClassroom");
                if (!publicDir.exists()) publicDir.mkdirs();
                File dest = new File(publicDir, sourceFile.getName());
                try (OutputStream os = new FileOutputStream(dest);
                     InputStream is = Files.newInputStream(sourceFile.toPath())) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }
                Toast.makeText(context, "Saved to: " + dest.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Failed to download: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
