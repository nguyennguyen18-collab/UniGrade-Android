package com.uef.unigrade;

import android.content.Intent;
import android.os.Bundle;

import com.uef.unigrade.Models.Staff;

/** Giao diện Ban Quản Trị: Tổng quan / Sinh viên / Môn học / Nhập điểm / Lịch sử. */
public class AdminDashboardActivity extends ShellActivity {
    public Staff staff;
    public boolean demo;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        String code = getIntent().getStringExtra("loginCode");
        demo = getIntent().getBooleanExtra("demo", false);
        staff = db.getStaff(code == null ? "GV9901" : code);
        buildShell(0);
    }

    @Override protected String[] tabLabels() { return getResources().getStringArray(R.array.admin_tab_labels); }
    @Override protected int[] tabIcons() { return new int[]{R.drawable.ic_dashboard, R.drawable.ic_people, R.drawable.ic_book, R.drawable.ic_doc, R.drawable.ic_history}; }
    @Override protected String[] tabTitles() { return getResources().getStringArray(R.array.admin_tab_titles); }
    @Override protected String roleLabel() { return getString(R.string.role_staff); }
    @Override protected String userName() { return staff.name; }
    @Override protected void onAvatarClick() { showExtra(getString(R.string.bar_title_profile), new AdminScreens.Profile(this)); }

    @Override protected Screen createTab(int i) {
        switch (i) {
            case 1: return new AdminScreens.Students(this);
            case 2: return new AdminScreens.Subjects(this);
            case 3: return new AdminScreens.GradeEntry(this);
            case 4: return new AdminScreens.HistoryLog(this);
            default: return new AdminScreens.Overview(this);
        }
    }

    /** Dùng ở chế độ demo: xem giao diện sinh viên mẫu (SV2024001). */
    public void openStudentDemo() {
        Intent i = new Intent(this, StudentDashboardActivity.class);
        i.putExtra("studentId", db.studentIdForCode("SV2024001"));
        i.putExtra("loginCode", "SV2024001");
        i.putExtra("demo", true);
        startActivity(i);
    }
}
