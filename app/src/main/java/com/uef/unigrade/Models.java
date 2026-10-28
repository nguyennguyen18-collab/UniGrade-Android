package com.uef.unigrade;

/** Các lớp dữ liệu đơn giản dùng chung giữa DatabaseHelper và giao diện. */
public class Models {
    public static class Student {
        public int id;
        public String code = "", name = "", email = "", className = "", faculty = "", major = "",
                cohort = "", dob = "", gender = "Nam", phone = "", address = "";
        public double gpa4;
    }

    public static class Subject {
        public int id, credits = 3, wAtt = 10, wAss = 20, wMid = 20, wFin = 50;
        public String code = "", name = "", description = "", faculty = "", suggestedTerm = "";
    }

    public static class Grade {
        public int id, studentId, subjectId, credits;
        public String studentCode, studentName, subjectCode, subjectName, semester, letter, updatedAt;
        public double att, ass, mid, fin, total;
    }

    public static class History {
        public String studentCode, studentName, subjectCode, subjectName, semester, reason, changedBy, changedAt;
        public Double oldTotal;
        public double newTotal;
    }

    public static class Notice {
        public int id;
        public String title, body, type, createdAt;
        public boolean read;
    }

    public static class Course {
        public String classCode, subjectCode, subjectName, description, schedule, room, lecturer, lecturerEmail, semester;
        public int credits, attended, totalSessions;
        public boolean completed;
    }

    public static class Staff {
        public String code = "", name = "", position = "", unit = "", email = "", phone = "", office = "";
    }

    public static class Overview {
        public int students, subjects, classes, grades;
        public double completedPct;
        public int[] bands = new int[4]; // >=8.5, 7.0-8.4, 5.0-6.9, <5.0
    }

    /** {gpa4, gpa10, tín chỉ đạt, số môn đạt, tổng số môn} */
    public static class Stats {
        public double gpa4, gpa10;
        public int credits, passed, total;
    }
}
