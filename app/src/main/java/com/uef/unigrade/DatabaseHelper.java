package com.uef.unigrade;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import com.uef.unigrade.Models.*;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "UniGrade.db";
    private static final int DB_VERSION = 2;
    public static final String CURRENT_SEMESTER = "HK1 2025-2026";
    public static final int PROGRAM_CREDITS = 135;
    public static final String DEFAULT_PASSWORD = "123456";

    private final Context appContext;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        appContext = context.getApplicationContext();
    }

    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }

    // =====================================================================
    // Schema + dữ liệu mẫu
    // =====================================================================
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE UserAccount(id INTEGER PRIMARY KEY AUTOINCREMENT, loginCode TEXT UNIQUE NOT NULL, passwordHash TEXT NOT NULL, role TEXT NOT NULL, studentId INTEGER)");
        db.execSQL("CREATE TABLE Staff(id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT UNIQUE NOT NULL, fullName TEXT NOT NULL, position TEXT, unit TEXT, email TEXT, phone TEXT, office TEXT)");
        db.execSQL("CREATE TABLE Student(id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT UNIQUE NOT NULL, fullName TEXT NOT NULL, email TEXT, className TEXT, faculty TEXT, major TEXT, cohort TEXT, dob TEXT, gender TEXT, phone TEXT, address TEXT)");
        db.execSQL("CREATE TABLE Subject(id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT UNIQUE NOT NULL, name TEXT NOT NULL, credits INTEGER NOT NULL DEFAULT 3, description TEXT, faculty TEXT, wAtt INTEGER NOT NULL DEFAULT 10, wAss INTEGER NOT NULL DEFAULT 20, wMid INTEGER NOT NULL DEFAULT 20, wFin INTEGER NOT NULL DEFAULT 50, suggestedTerm TEXT)");
        db.execSQL("CREATE TABLE Grade(id INTEGER PRIMARY KEY AUTOINCREMENT, studentId INTEGER NOT NULL, subjectId INTEGER NOT NULL, semester TEXT NOT NULL, attendance REAL NOT NULL, assignment REAL NOT NULL, midterm REAL NOT NULL, final REAL NOT NULL, total REAL NOT NULL, letter TEXT NOT NULL, updatedAt TEXT, UNIQUE(studentId, subjectId, semester), FOREIGN KEY(studentId) REFERENCES Student(id) ON DELETE CASCADE, FOREIGN KEY(subjectId) REFERENCES Subject(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE GradeHistory(id INTEGER PRIMARY KEY AUTOINCREMENT, studentCode TEXT, studentName TEXT, subjectCode TEXT, subjectName TEXT, semester TEXT, oldTotal REAL, newTotal REAL, reason TEXT, changedBy TEXT, changedAt TEXT)");
        db.execSQL("CREATE TABLE CourseClass(id INTEGER PRIMARY KEY AUTOINCREMENT, classCode TEXT UNIQUE NOT NULL, subjectId INTEGER NOT NULL, semester TEXT NOT NULL, schedule TEXT, room TEXT, lecturer TEXT, lecturerEmail TEXT, totalSessions INTEGER DEFAULT 15, FOREIGN KEY(subjectId) REFERENCES Subject(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE Enrollment(id INTEGER PRIMARY KEY AUTOINCREMENT, studentId INTEGER NOT NULL, classId INTEGER NOT NULL, attended INTEGER DEFAULT 0, UNIQUE(studentId, classId), FOREIGN KEY(studentId) REFERENCES Student(id) ON DELETE CASCADE, FOREIGN KEY(classId) REFERENCES CourseClass(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE Notification(id INTEGER PRIMARY KEY AUTOINCREMENT, studentId INTEGER NOT NULL, title TEXT, body TEXT, type TEXT, createdAt TEXT, isRead INTEGER DEFAULT 0, FOREIGN KEY(studentId) REFERENCES Student(id) ON DELETE CASCADE)");
        seed(db);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { dropAll(db); onCreate(db); }

    private void dropAll(SQLiteDatabase db) {
        for (String t : new String[]{"Notification", "Enrollment", "CourseClass", "GradeHistory", "Grade", "Subject", "Student", "Staff", "UserAccount"})
            db.execSQL("DROP TABLE IF EXISTS " + t);
    }

    /** Xóa toàn bộ dữ liệu và nạp lại dữ liệu mẫu. */
    public void resetData() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try { dropAll(db); onCreate(db); db.setTransactionSuccessful(); } finally { db.endTransaction(); }
    }

    private static final String CNTT = "Khoa Công nghệ Thông tin", TOAN = "Khoa Toán - Thống kê", DIEN = "Khoa Điện - Điện tử";

    private void seed(SQLiteDatabase db) {
        // Cán bộ
        ContentValues st = new ContentValues();
        st.put("code", "GV9901"); st.put("fullName", "ThS. Trần Thị Mai"); st.put("position", "Phó Trưởng phòng Khảo thí");
        st.put("unit", "Phòng Quản lý Đào tạo & Khảo thí"); st.put("email", "mai.tran@uni.edu.vn"); st.put("phone", "028 3865 2411");
        st.put("office", "Khu A - Tòa Nhà Trung Tâm, ĐH Quốc Gia");
        db.insert("Staff", null, st);
        addAccount(db, "GV9901", "admin", 0);

        // Sinh viên (id 1..5)
        student(db, "SV2024001", "Nguyễn Văn An", "an.nguyen@uni.edu.vn", "24KTPM01", CNTT, "Kỹ thuật Phần mềm", "15/08/2006", "Nam", "0912 345 678", "268 Lý Thường Kiệt, Phường 14, Quận 10, TP. Hồ Chí Minh");
        student(db, "SV2024002", "Trần Thị Bích Ngọc", "ngoc.tran@uni.edu.vn", "24KHMT02", CNTT, "Khoa học Máy tính", "02/03/2006", "Nữ", "0903 111 222", "15 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh");
        student(db, "SV2024003", "Lê Hoàng Long", "long.le@uni.edu.vn", "24HTTT01", CNTT, "Hệ thống Thông tin", "21/11/2005", "Nam", "0987 654 321", "90 Cách Mạng Tháng 8, Quận 3, TP. Hồ Chí Minh");
        student(db, "SV2024004", "Phạm Minh Đức", "duc.pham@uni.edu.vn", "24KTPM02", CNTT, "Kỹ thuật Phần mềm", "09/07/2006", "Nam", "0938 222 333", "45 Phan Văn Trị, Quận Gò Vấp, TP. Hồ Chí Minh");
        student(db, "SV2024005", "Võ Thanh Hà", "ha.vo@uni.edu.vn", "24DTVT01", DIEN, "Điện tử Viễn thông", "30/01/2006", "Nữ", "0977 888 999", "12 Lê Văn Việt, TP. Thủ Đức, TP. Hồ Chí Minh");
        for (int i = 1; i <= 5; i++) addAccount(db, "SV202400" + i, "student", i);

        // Môn học (id 1..8)
        subject(db, "IT2001", "Lập trình Hướng đối tượng", 3, "Nguyên lý kế thừa, đa hình, đóng gói và trừu tượng hóa trên C++ và Java.", CNTT, "HK3");
        subject(db, "IT2002", "Cấu trúc Dữ liệu & Giải thuật", 4, "Danh sách liên kết, cây nhị phân, đồ thị và các thuật toán tìm kiếm, sắp xếp tối ưu.", CNTT, "HK3");
        subject(db, "IT2003", "Cơ sở Dữ liệu", 3, "Mô hình quan hệ, SQL, chuẩn hóa dữ liệu và quản trị giao dịch.", CNTT, "HK4");
        subject(db, "IT2004", "Lập trình Thiết bị Di động", 3, "Phát triển ứng dụng Android với Java, SQLite và thiết kế giao diện người dùng.", CNTT, "HK5");
        subject(db, "IT3001", "Công nghệ Phần mềm", 3, "Quy trình phát triển phần mềm, phân tích thiết kế hệ thống và kiểm thử.", CNTT, "HK5");
        subject(db, "IT2005", "Mạng Máy tính", 3, "Mô hình OSI, TCP/IP, định tuyến và bảo mật mạng cơ bản.", CNTT, "HK4");
        subject(db, "MA1001", "Giải tích 1", 3, "Giới hạn, đạo hàm, tích phân và chuỗi số.", TOAN, "HK1");
        subject(db, "EE1001", "Mạch Điện Tử", 3, "Linh kiện bán dẫn, mạch khuếch đại và mạch số cơ bản.", DIEN, "HK2");

        // Điểm
        grade(db, 1, 1, CURRENT_SEMESTER, 10, 9, 8.5, 9);
        grade(db, 1, 2, CURRENT_SEMESTER, 9.5, 8.5, 8, 8.5);
        grade(db, 1, 3, CURRENT_SEMESTER, 9, 8, 8.5, 9);
        grade(db, 1, 7, "HK2 2024-2025", 9, 8, 8, 8.5);
        grade(db, 2, 1, CURRENT_SEMESTER, 10, 10, 9.5, 9.5);
        grade(db, 2, 3, CURRENT_SEMESTER, 10, 9, 9, 9);
        grade(db, 2, 7, "HK2 2024-2025", 9, 9, 9, 9);
        grade(db, 3, 1, CURRENT_SEMESTER, 8, 7, 6.5, 7);
        grade(db, 3, 3, CURRENT_SEMESTER, 8, 7, 7, 7.5);
        grade(db, 4, 1, CURRENT_SEMESTER, 9, 8, 7.5, 8);
        grade(db, 5, 8, CURRENT_SEMESTER, 8, 7, 7, 7);

        // Lịch sử điểm
        String by = "ThS. Trần Thị Mai (Phòng Khảo thí)";
        history(db, 1, 1, 8.5, 9.0, "Chấm phúc khảo bài thi cuối kỳ: Bổ sung điểm câu hỏi thiết kế mẫu Singleton (cộng 1.0đ cuối kỳ).", by, "2026-09-25 14:30:00");
        history(db, 3, 1, 6.2, 7.0, "Cập nhật bổ sung điểm bài tập lớn phần nhóm đã nộp bù có xác nhận của khoa.", by, "2026-09-24 09:15:00");
        history(db, 4, 1, 7.5, 8.0, "Điều chỉnh điểm giữa kỳ do nhập sai khi chuyển bảng điểm.", by, "2026-09-22 16:45:00");

        // Lớp học phần (id 1..6) và đăng ký
        courseClass(db, "IT2001.01", 1, CURRENT_SEMESTER, "Thứ 2 (Tiết 1-3, 07:00 - 09:30)", "Phòng A2.301 - Giảng đường A", "TS. Nguyễn Thanh Sơn", "son.nguyen@uni.edu.vn");
        courseClass(db, "IT2002.03", 2, CURRENT_SEMESTER, "Thứ 4 (Tiết 4-6, 09:45 - 12:15)", "Phòng B1.105 - Lab Máy tính", "ThS. Lê Hoàng Tuấn", "tuan.le@uni.edu.vn");
        courseClass(db, "IT2003.02", 3, CURRENT_SEMESTER, "Thứ 3 (Tiết 1-3, 07:00 - 09:30)", "Phòng A1.204 - Giảng đường A", "ThS. Phạm Quốc Bảo", "bao.pham@uni.edu.vn");
        courseClass(db, "IT2004.01", 4, CURRENT_SEMESTER, "Thứ 5 (Tiết 7-9, 13:00 - 15:30)", "Phòng B2.201 - Lab Di động", "TS. Vũ Minh Khoa", "khoa.vu@uni.edu.vn");
        courseClass(db, "EE1001.01", 8, CURRENT_SEMESTER, "Thứ 6 (Tiết 4-6, 09:45 - 12:15)", "Phòng C3.101 - Giảng đường C", "ThS. Đỗ Thị Lan", "lan.do@uni.edu.vn");
        courseClass(db, "MA1001.02", 7, "HK2 2024-2025", "Thứ 2 (Tiết 4-6, 09:45 - 12:15)", "Phòng A1.101 - Giảng đường A", "TS. Hoàng Gia Bảo", "bao.hoang@uni.edu.vn");
        enroll(db, 1, 1, 14); enroll(db, 1, 2, 13); enroll(db, 1, 3, 12); enroll(db, 1, 4, 15); enroll(db, 1, 6, 15);
        enroll(db, 2, 1, 15); enroll(db, 2, 3, 14); enroll(db, 2, 6, 15);
        enroll(db, 3, 1, 11); enroll(db, 3, 3, 13);
        enroll(db, 4, 1, 14); enroll(db, 4, 2, 12);
        enroll(db, 5, 5, 13);

        // Thông báo cho sinh viên An (đã đọc)
        notice(db, 1, "Điểm phúc khảo đã cập nhật", "Điểm học phần Lập trình Hướng đối tượng (IT2001) đã được cập nhật từ 8.5 lên 9.0 (A+).", "grade", Ui.isoDaysAgo(0, 14, 32), 1);
        notice(db, 1, "Công bố điểm thi HK1 2025-2026", "Phòng Đào tạo đã công bố bảng điểm chính thức các học phần nhóm ngành CNTT.", "announce", Ui.isoDaysAgo(1, 8, 15), 1);
        notice(db, 1, "Đăng ký xét học bổng khuyến khích", "Mở cổng đăng ký học bổng khuyến khích học tập kỳ 1 cho sinh viên có GPA từ 3.2 trở lên.", "announce", "2026-09-23 10:00:00", 1);
        notice(db, 1, "Lịch thi học phần cuối kỳ", "Học phần Lập trình Thiết bị Di động (IT2004) dự kiến thi vào ngày 15/12/2026 tại phòng C3.401.", "exam", "2026-09-20 09:00:00", 1);
    }

    private void student(SQLiteDatabase db, String code, String name, String email, String cls, String faculty, String major, String dob, String gender, String phone, String address) {
        ContentValues v = new ContentValues();
        v.put("code", code); v.put("fullName", name); v.put("email", email); v.put("className", cls); v.put("faculty", faculty);
        v.put("major", major); v.put("cohort", "Khóa 2024 (K24)"); v.put("dob", dob); v.put("gender", gender); v.put("phone", phone); v.put("address", address);
        db.insert("Student", null, v);
    }

    private void subject(SQLiteDatabase db, String code, String name, int credits, String desc, String faculty, String term) {
        ContentValues v = new ContentValues();
        v.put("code", code); v.put("name", name); v.put("credits", credits); v.put("description", desc); v.put("faculty", faculty); v.put("suggestedTerm", term);
        db.insert("Subject", null, v);
    }

    private void addAccount(SQLiteDatabase db, String code, String role, int studentId) {
        ContentValues v = new ContentValues();
        v.put("loginCode", code); v.put("passwordHash", sha256(DEFAULT_PASSWORD)); v.put("role", role);
        if (studentId > 0) v.put("studentId", studentId);
        db.insert("UserAccount", null, v);
    }

    private void grade(SQLiteDatabase db, int sid, int sub, String sem, double att, double ass, double mid, double fin) {
        double total = calcTotal(att, ass, mid, fin, 10, 20, 20, 50);
        ContentValues v = new ContentValues();
        v.put("studentId", sid); v.put("subjectId", sub); v.put("semester", sem); v.put("attendance", att); v.put("assignment", ass);
        v.put("midterm", mid); v.put("final", fin); v.put("total", total); v.put("letter", letter(total));
        v.put("updatedAt", sem.equals(CURRENT_SEMESTER) ? Ui.isoDaysAgo(sub + sid, 9, 0) : "2025-06-20 10:00:00");
        db.insert("Grade", null, v);
    }

    private void history(SQLiteDatabase db, int sid, int sub, double oldT, double newT, String reason, String by, String at) {
        ContentValues v = new ContentValues();
        v.put("studentCode", "SV202400" + sid);
        v.put("studentName", new String[]{"", "Nguyễn Văn An", "Trần Thị Bích Ngọc", "Lê Hoàng Long", "Phạm Minh Đức"}[sid]);
        v.put("subjectCode", "IT2001"); v.put("subjectName", "Lập trình Hướng đối tượng"); v.put("semester", CURRENT_SEMESTER);
        v.put("oldTotal", oldT); v.put("newTotal", newT); v.put("reason", reason); v.put("changedBy", by); v.put("changedAt", at);
        db.insert("GradeHistory", null, v);
    }

    private void courseClass(SQLiteDatabase db, String code, int subjectId, String sem, String schedule, String room, String lecturer, String email) {
        ContentValues v = new ContentValues();
        v.put("classCode", code); v.put("subjectId", subjectId); v.put("semester", sem); v.put("schedule", schedule); v.put("room", room);
        v.put("lecturer", lecturer); v.put("lecturerEmail", email); v.put("totalSessions", 15);
        db.insert("CourseClass", null, v);
    }

    private void enroll(SQLiteDatabase db, int sid, int classId, int attended) {
        ContentValues v = new ContentValues();
        v.put("studentId", sid); v.put("classId", classId); v.put("attended", attended);
        db.insert("Enrollment", null, v);
    }

    private void notice(SQLiteDatabase db, int sid, String title, String body, String type, String at, int read) {
        ContentValues v = new ContentValues();
        v.put("studentId", sid); v.put("title", title); v.put("body", body); v.put("type", type); v.put("createdAt", at); v.put("isRead", read);
        db.insert("Notification", null, v);
    }

    // =====================================================================
    // Quy đổi điểm
    // =====================================================================
    private static String sha256(String s) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception e) { return s; }
    }

    public static double calcTotal(double att, double ass, double mid, double fin, int wA, int wB, int wC, int wD) {
        double t = (att * wA + ass * wB + mid * wC + fin * wD) / 100.0;
        return Math.round(t * 100) / 100.0;
    }

    public static String letter(double s) {
        if (s >= 9.0) return "A+"; if (s >= 8.5) return "A"; if (s >= 8.0) return "B+"; if (s >= 7.0) return "B";
        if (s >= 6.5) return "C+"; if (s >= 5.5) return "C"; if (s >= 5.0) return "D+"; if (s >= 4.0) return "D";
        return "F";
    }

    public static double point4(String letter) {
        switch (letter) {
            case "A+": case "A": return 4.0; case "B+": return 3.5; case "B": return 3.0; case "C+": return 2.5;
            case "C": return 2.0; case "D+": return 1.5; case "D": return 1.0; default: return 0.0;
        }
    }

    public static String rating(double score10) {
        if (score10 >= 8.5) return "Xuất sắc"; if (score10 >= 7.0) return "Khá"; if (score10 >= 5.0) return "Trung bình"; return "Yếu";
    }

    // =====================================================================
    // Đăng nhập / tài khoản
    // =====================================================================
    public boolean login(String code, String password, String role) {
        Cursor c = getReadableDatabase().rawQuery("SELECT passwordHash FROM UserAccount WHERE loginCode=? AND role=?", new String[]{code.trim(), role});
        boolean ok = c.moveToFirst() && sha256(password).equals(c.getString(0));
        c.close();
        return ok;
    }

    public int studentIdForCode(String code) {
        Cursor c = getReadableDatabase().rawQuery("SELECT id FROM Student WHERE code=?", new String[]{code.trim()});
        int id = c.moveToFirst() ? c.getInt(0) : -1;
        c.close();
        return id;
    }

    public boolean changePassword(String loginCode, String oldPass, String newPass) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT passwordHash FROM UserAccount WHERE loginCode=?", new String[]{loginCode});
        boolean ok = c.moveToFirst() && sha256(oldPass).equals(c.getString(0));
        c.close();
        if (!ok) return false;
        ContentValues v = new ContentValues();
        v.put("passwordHash", sha256(newPass));
        return db.update("UserAccount", v, "loginCode=?", new String[]{loginCode}) > 0;
    }

    public Staff getStaff(String code) {
        Staff s = new Staff();
        Cursor c = getReadableDatabase().rawQuery("SELECT code,fullName,position,unit,email,phone,office FROM Staff WHERE code=?", new String[]{code});
        if (c.moveToFirst()) {
            s.code = c.getString(0); s.name = c.getString(1); s.position = nz(c.getString(2)); s.unit = nz(c.getString(3));
            s.email = nz(c.getString(4)); s.phone = nz(c.getString(5)); s.office = nz(c.getString(6));
        }
        c.close();
        return s;
    }

    private static String nz(String s) { return s == null ? "" : s; }

    // =====================================================================
    // Sinh viên
    // =====================================================================
    private static final String STUDENT_COLS = "id,code,fullName,email,className,faculty,major,cohort,dob,gender,phone,address";

    private Student readStudent(Cursor c) {
        Student s = new Student();
        s.id = c.getInt(0); s.code = nz(c.getString(1)); s.name = nz(c.getString(2)); s.email = nz(c.getString(3)); s.className = nz(c.getString(4));
        s.faculty = nz(c.getString(5)); s.major = nz(c.getString(6)); s.cohort = nz(c.getString(7)); s.dob = nz(c.getString(8));
        s.gender = nz(c.getString(9)); s.phone = nz(c.getString(10)); s.address = nz(c.getString(11));
        return s;
    }

    public Student getStudent(int id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT " + STUDENT_COLS + " FROM Student WHERE id=?", new String[]{String.valueOf(id)});
        Student s = c.moveToFirst() ? readStudent(c) : null;
        c.close();
        if (s != null) s.gpa4 = stats(s.id, null).gpa4;
        return s;
    }

    public List<Student> listStudents(String query, String faculty) {
        String q = "%" + (query == null ? "" : query.trim()) + "%";
        String sql = "SELECT " + STUDENT_COLS + " FROM Student WHERE (code LIKE ? OR fullName LIKE ? OR className LIKE ?)";
        ArrayList<String> args = new ArrayList<>();
        args.add(q); args.add(q); args.add(q);
        if (faculty != null) { sql += " AND faculty=?"; args.add(faculty); }
        Cursor c = getReadableDatabase().rawQuery(sql + " ORDER BY code", args.toArray(new String[0]));
        List<Student> out = new ArrayList<>();
        while (c.moveToNext()) out.add(readStudent(c));
        c.close();
        for (Student s : out) s.gpa4 = stats(s.id, null).gpa4;
        return out;
    }

    public List<String> faculties(String table) {
        Cursor c = getReadableDatabase().rawQuery("SELECT DISTINCT faculty FROM " + table + " WHERE faculty IS NOT NULL AND faculty<>'' ORDER BY faculty", null);
        List<String> out = new ArrayList<>();
        while (c.moveToNext()) out.add(c.getString(0));
        c.close();
        return out;
    }

    /** Trả về null nếu thành công, ngược lại là thông báo lỗi. */
    public String saveStudent(Student s) {
        if (s.code.trim().isEmpty() || s.name.trim().isEmpty()) return "MSSV và họ tên là bắt buộc";
        ContentValues v = new ContentValues();
        v.put("code", s.code.trim()); v.put("fullName", s.name.trim()); v.put("email", s.email.trim()); v.put("className", s.className.trim());
        v.put("faculty", s.faculty.trim()); v.put("major", s.major.trim()); v.put("cohort", s.cohort.trim()); v.put("dob", s.dob.trim());
        v.put("gender", s.gender); v.put("phone", s.phone.trim()); v.put("address", s.address.trim());
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (s.id > 0) {
                db.update("Student", v, "id=?", new String[]{String.valueOf(s.id)});
                ContentValues a = new ContentValues();
                a.put("loginCode", s.code.trim());
                db.update("UserAccount", a, "studentId=?", new String[]{String.valueOf(s.id)});
            } else {
                long id = db.insertOrThrow("Student", null, v);
                addAccount(db, s.code.trim(), "student", (int) id); // mật khẩu mặc định 123456
            }
            db.setTransactionSuccessful();
            return null;
        } catch (Exception e) {
            return "MSSV đã tồn tại hoặc dữ liệu không hợp lệ";
        } finally { db.endTransaction(); }
    }

    public boolean deleteStudent(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("UserAccount", "studentId=?", new String[]{String.valueOf(id)});
            db.delete("Student", "id=?", new String[]{String.valueOf(id)}); // Grade/Enrollment/Notification xóa theo khóa ngoại
            db.setTransactionSuccessful();
            return true;
        } finally { db.endTransaction(); }
    }

    // =====================================================================
    // Môn học
    // =====================================================================
    private static final String SUBJECT_COLS = "id,code,name,credits,description,faculty,wAtt,wAss,wMid,wFin,suggestedTerm";

    private Subject readSubject(Cursor c) {
        Subject s = new Subject();
        s.id = c.getInt(0); s.code = c.getString(1); s.name = c.getString(2); s.credits = c.getInt(3); s.description = nz(c.getString(4));
        s.faculty = nz(c.getString(5)); s.wAtt = c.getInt(6); s.wAss = c.getInt(7); s.wMid = c.getInt(8); s.wFin = c.getInt(9); s.suggestedTerm = nz(c.getString(10));
        return s;
    }

    public List<Subject> listSubjects(String query, String faculty) {
        String q = "%" + (query == null ? "" : query.trim()) + "%";
        String sql = "SELECT " + SUBJECT_COLS + " FROM Subject WHERE (code LIKE ? OR name LIKE ?)";
        ArrayList<String> args = new ArrayList<>();
        args.add(q); args.add(q);
        if (faculty != null) { sql += " AND faculty=?"; args.add(faculty); }
        Cursor c = getReadableDatabase().rawQuery(sql + " ORDER BY code", args.toArray(new String[0]));
        List<Subject> out = new ArrayList<>();
        while (c.moveToNext()) out.add(readSubject(c));
        c.close();
        return out;
    }

    public Subject getSubject(int id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT " + SUBJECT_COLS + " FROM Subject WHERE id=?", new String[]{String.valueOf(id)});
        Subject s = c.moveToFirst() ? readSubject(c) : null;
        c.close();
        return s;
    }

    public String saveSubject(Subject s) {
        if (s.code.trim().isEmpty() || s.name.trim().isEmpty() || s.credits < 1) return "Vui lòng nhập mã môn, tên môn và số tín chỉ hợp lệ";
        if (s.wAtt + s.wAss + s.wMid + s.wFin != 100) return "Tổng tỷ lệ điểm phải bằng 100%";
        ContentValues v = new ContentValues();
        v.put("code", s.code.trim()); v.put("name", s.name.trim()); v.put("credits", s.credits); v.put("description", s.description.trim());
        v.put("faculty", s.faculty.trim()); v.put("wAtt", s.wAtt); v.put("wAss", s.wAss); v.put("wMid", s.wMid); v.put("wFin", s.wFin);
        v.put("suggestedTerm", s.suggestedTerm.trim());
        try {
            SQLiteDatabase db = getWritableDatabase();
            if (s.id > 0) db.update("Subject", v, "id=?", new String[]{String.valueOf(s.id)});
            else db.insertOrThrow("Subject", null, v);
            return null;
        } catch (Exception e) { return "Mã môn học đã tồn tại hoặc dữ liệu không hợp lệ"; }
    }

    public boolean deleteSubject(int id) {
        return getWritableDatabase().delete("Subject", "id=?", new String[]{String.valueOf(id)}) > 0;
    }

    // =====================================================================
    // Điểm
    // =====================================================================
    private static final String GRADE_SQL = "SELECT g.id,g.studentId,g.subjectId,s.code,s.fullName,u.code,u.name,u.credits,g.semester,g.attendance,g.assignment,g.midterm,g.final,g.total,g.letter,g.updatedAt "
            + "FROM Grade g JOIN Student s ON s.id=g.studentId JOIN Subject u ON u.id=g.subjectId ";

    private Grade readGrade(Cursor c) {
        Grade g = new Grade();
        g.id = c.getInt(0); g.studentId = c.getInt(1); g.subjectId = c.getInt(2); g.studentCode = c.getString(3); g.studentName = c.getString(4);
        g.subjectCode = c.getString(5); g.subjectName = c.getString(6); g.credits = c.getInt(7); g.semester = c.getString(8);
        g.att = c.getDouble(9); g.ass = c.getDouble(10); g.mid = c.getDouble(11); g.fin = c.getDouble(12); g.total = c.getDouble(13);
        g.letter = c.getString(14); g.updatedAt = nz(c.getString(15));
        return g;
    }

    /** Điểm của một sinh viên, mới nhất trước. */
    public List<Grade> gradesOfStudent(int studentId) {
        Cursor c = getReadableDatabase().rawQuery(GRADE_SQL + "WHERE g.studentId=? ORDER BY g.updatedAt DESC, g.id DESC", new String[]{String.valueOf(studentId)});
        List<Grade> out = new ArrayList<>();
        while (c.moveToNext()) out.add(readGrade(c));
        c.close();
        return out;
    }

    public Grade findGrade(int studentId, int subjectId, String semester) {
        Cursor c = getReadableDatabase().rawQuery(GRADE_SQL + "WHERE g.studentId=? AND g.subjectId=? AND g.semester=?",
                new String[]{String.valueOf(studentId), String.valueOf(subjectId), semester});
        Grade g = c.moveToFirst() ? readGrade(c) : null;
        c.close();
        return g;
    }

    public List<String> semesters() {
        Cursor c = getReadableDatabase().rawQuery("SELECT semester FROM Grade UNION SELECT semester FROM CourseClass UNION SELECT ? ORDER BY 1 DESC", new String[]{CURRENT_SEMESTER});
        List<String> out = new ArrayList<>();
        while (c.moveToNext()) out.add(c.getString(0));
        c.close();
        out.remove(CURRENT_SEMESTER);
        out.add(0, CURRENT_SEMESTER); // học kỳ hiện tại luôn đứng đầu
        return out;
    }

    /** semester = null: tính trên toàn bộ học kỳ. */
    public Stats stats(int studentId, String semester) {
        List<Grade> list = gradesOfStudent(studentId);
        Stats s = new Stats();
        double pts = 0, sc = 0; int cr = 0;
        for (Grade g : list) {
            if (semester != null && !semester.equals(g.semester)) continue;
            s.total++;
            cr += g.credits;
            pts += point4(g.letter) * g.credits;
            sc += g.total * g.credits;
            if (g.total >= 4.0) { s.passed++; s.credits += g.credits; }
        }
        if (cr > 0) { s.gpa4 = pts / cr; s.gpa10 = sc / cr; }
        return s;
    }

    /** Trả về null nếu thành công, ngược lại là thông báo lỗi. */
    public String saveGrade(int studentId, int subjectId, String semester, double att, double ass, double mid, double fin, String reason, String changedBy) {
        if (att < 0 || att > 10 || ass < 0 || ass > 10 || mid < 0 || mid > 10 || fin < 0 || fin > 10) return "Điểm phải nằm trong khoảng 0–10";
        Subject sub = getSubject(subjectId);
        Student stu = getStudent(studentId);
        if (sub == null || stu == null) return "Không tìm thấy sinh viên hoặc môn học";
        Grade old = findGrade(studentId, subjectId, semester);
        if (old != null && (reason == null || reason.trim().isEmpty())) return "Vui lòng nhập lý do khi chỉnh sửa điểm đã có";
        double total = calcTotal(att, ass, mid, fin, sub.wAtt, sub.wAss, sub.wMid, sub.wFin);
        String letter = letter(total), now = Ui.nowIso();
        ContentValues v = new ContentValues();
        v.put("studentId", studentId); v.put("subjectId", subjectId); v.put("semester", semester); v.put("attendance", att); v.put("assignment", ass);
        v.put("midterm", mid); v.put("final", fin); v.put("total", total); v.put("letter", letter); v.put("updatedAt", now);
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (old != null) db.update("Grade", v, "id=?", new String[]{String.valueOf(old.id)});
            else db.insertOrThrow("Grade", null, v);
            ContentValues h = new ContentValues();
            h.put("studentCode", stu.code); h.put("studentName", stu.name); h.put("subjectCode", sub.code); h.put("subjectName", sub.name); h.put("semester", semester);
            if (old != null) h.put("oldTotal", old.total);
            h.put("newTotal", total); h.put("reason", old == null ? "Nhập điểm lần đầu" : reason.trim()); h.put("changedBy", changedBy); h.put("changedAt", now);
            db.insert("GradeHistory", null, h);
            // Thông báo cho sinh viên nếu họ bật nhận thông báo điểm
            SharedPreferences p = appContext.getSharedPreferences("unigrade", Context.MODE_PRIVATE);
            if (p.getBoolean("notify_" + stu.code, true)) {
                String body = old == null ? "Điểm học phần " + sub.name + " (" + sub.code + ") đã được công bố: " + Ui.f1(total) + " (" + letter + ")."
                        : "Điểm học phần " + sub.name + " (" + sub.code + ") đã được cập nhật từ " + Ui.f1(old.total) + " lên " + Ui.f1(total) + " (" + letter + ").";
                notice(db, studentId, old == null ? "Điểm mới đã được công bố" : "Điểm học phần đã cập nhật", body, "grade", now, 0);
            }
            db.setTransactionSuccessful();
            return null;
        } catch (Exception e) {
            return "Không lưu được dữ liệu điểm";
        } finally { db.endTransaction(); }
    }

    public List<History> listHistory(String query) {
        String q = "%" + (query == null ? "" : query.trim()) + "%";
        Cursor c = getReadableDatabase().rawQuery("SELECT studentCode,studentName,subjectCode,subjectName,semester,oldTotal,newTotal,reason,changedBy,changedAt FROM GradeHistory "
                + "WHERE studentCode LIKE ? OR studentName LIKE ? OR subjectName LIKE ? OR subjectCode LIKE ? ORDER BY changedAt DESC, id DESC", new String[]{q, q, q, q});
        List<History> out = new ArrayList<>();
        while (c.moveToNext()) {
            History h = new History();
            h.studentCode = c.getString(0); h.studentName = c.getString(1); h.subjectCode = c.getString(2); h.subjectName = c.getString(3);
            h.semester = nz(c.getString(4)); h.oldTotal = c.isNull(5) ? null : c.getDouble(5); h.newTotal = c.getDouble(6); h.reason = nz(c.getString(7));
            h.changedBy = nz(c.getString(8)); h.changedAt = nz(c.getString(9));
            out.add(h);
        }
        c.close();
        return out;
    }

    // =====================================================================
    // Học phần & thông báo
    // =====================================================================
    public List<Course> coursesOfStudent(int studentId) {
        Cursor c = getReadableDatabase().rawQuery("SELECT cc.classCode,u.code,u.name,u.description,cc.schedule,cc.room,cc.lecturer,cc.lecturerEmail,cc.semester,u.credits,e.attended,cc.totalSessions "
                + "FROM Enrollment e JOIN CourseClass cc ON cc.id=e.classId JOIN Subject u ON u.id=cc.subjectId WHERE e.studentId=? ORDER BY cc.semester DESC, cc.classCode", new String[]{String.valueOf(studentId)});
        List<Course> out = new ArrayList<>();
        while (c.moveToNext()) {
            Course k = new Course();
            k.classCode = c.getString(0); k.subjectCode = c.getString(1); k.subjectName = c.getString(2); k.description = nz(c.getString(3));
            k.schedule = nz(c.getString(4)); k.room = nz(c.getString(5)); k.lecturer = nz(c.getString(6)); k.lecturerEmail = nz(c.getString(7));
            k.semester = c.getString(8); k.credits = c.getInt(9); k.attended = c.getInt(10); k.totalSessions = c.getInt(11);
            k.completed = !CURRENT_SEMESTER.equals(k.semester);
            out.add(k);
        }
        c.close();
        return out;
    }

    public List<Notice> notices(int studentId) {
        Cursor c = getReadableDatabase().rawQuery("SELECT id,title,body,type,createdAt,isRead FROM Notification WHERE studentId=? ORDER BY createdAt DESC, id DESC", new String[]{String.valueOf(studentId)});
        List<Notice> out = new ArrayList<>();
        while (c.moveToNext()) {
            Notice n = new Notice();
            n.id = c.getInt(0); n.title = c.getString(1); n.body = c.getString(2); n.type = c.getString(3); n.createdAt = c.getString(4); n.read = c.getInt(5) == 1;
            out.add(n);
        }
        c.close();
        return out;
    }

    public void markNoticeRead(int id) {
        ContentValues v = new ContentValues();
        v.put("isRead", 1);
        getWritableDatabase().update("Notification", v, "id=?", new String[]{String.valueOf(id)});
    }

    public void markAllNoticesRead(int studentId) {
        ContentValues v = new ContentValues();
        v.put("isRead", 1);
        getWritableDatabase().update("Notification", v, "studentId=?", new String[]{String.valueOf(studentId)});
    }

    // =====================================================================
    // Tổng quan quản trị
    // =====================================================================
    private int count(String sql, String... args) {
        Cursor c = getReadableDatabase().rawQuery(sql, args);
        int n = c.moveToFirst() ? c.getInt(0) : 0;
        c.close();
        return n;
    }

    public Overview overview() {
        Overview o = new Overview();
        o.students = count("SELECT COUNT(*) FROM Student");
        o.subjects = count("SELECT COUNT(*) FROM Subject");
        o.classes = count("SELECT COUNT(*) FROM CourseClass WHERE semester=?", CURRENT_SEMESTER);
        o.grades = count("SELECT COUNT(*) FROM Grade");
        int enrollments = count("SELECT COUNT(*) FROM Enrollment");
        int graded = count("SELECT COUNT(*) FROM Enrollment e JOIN CourseClass cc ON cc.id=e.classId WHERE EXISTS (SELECT 1 FROM Grade g WHERE g.studentId=e.studentId AND g.subjectId=cc.subjectId AND g.semester=cc.semester)");
        o.completedPct = enrollments == 0 ? 0 : Math.min(100.0, graded * 100.0 / enrollments);
        o.bands[0] = count("SELECT COUNT(*) FROM Grade WHERE total>=8.5");
        o.bands[1] = count("SELECT COUNT(*) FROM Grade WHERE total>=7.0 AND total<8.5");
        o.bands[2] = count("SELECT COUNT(*) FROM Grade WHERE total>=5.0 AND total<7.0");
        o.bands[3] = count("SELECT COUNT(*) FROM Grade WHERE total<5.0");
        return o;
    }
}
