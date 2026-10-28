package com.uef.unigrade;

import android.content.Intent;
import android.os.Bundle;

import com.uef.unigrade.Models.Student;

/** Giao diện sinh viên: Trang chủ / Bảng điểm / Học phần / Thông báo / Tài khoản. */
public class StudentDashboardActivity extends ShellActivity {
    public Student student;
    public String loginCode;
    public boolean demo;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        int studentId = getIntent().getIntExtra("studentId", -1);
        loginCode = getIntent().getStringExtra("loginCode");
        demo = getIntent().getBooleanExtra("demo", false);
        student = db.getStudent(studentId);
        if (student == null) { Ui.toast(this, R.string.toast_no_student); logout(); return; }
        buildShell(0);
    }

    @Override protected String[] tabLabels() { return getResources().getStringArray(R.array.student_tab_labels); }
    @Override protected int[] tabIcons() { return new int[]{R.drawable.ic_dashboard, R.drawable.ic_school, R.drawable.ic_book, R.drawable.ic_bell, R.drawable.ic_person}; }
    @Override protected String[] tabTitles() { return getResources().getStringArray(R.array.student_tab_titles); }
    @Override protected String roleLabel() { return getString(R.string.role_student); }
    @Override protected String userName() { return student.name; }
    @Override protected void onAvatarClick() { showTab(4); }

    @Override protected Screen createTab(int i) {
        switch (i) {
            case 1: return new StudentScreens.Grades(this);
            case 2: return new StudentScreens.Courses(this);
            case 3: return new StudentScreens.Notices(this);
            case 4: return new StudentScreens.Account(this);
            default: return new StudentScreens.Home(this);
        }
    }

    /** Chỉ dùng ở chế độ demo: chuyển nhanh sang giao diện quản trị. */
    public void openAdminDemo() {
        Intent i = new Intent(this, AdminDashboardActivity.class);
        i.putExtra("loginCode", "GV9901");
        i.putExtra("demo", true);
        startActivity(i);
    }
}
