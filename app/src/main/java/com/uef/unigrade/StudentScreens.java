package com.uef.unigrade;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.uef.unigrade.Models.*;

/** Các màn hình của sinh viên (layout: res/layout/screen_student_*.xml). */
public class StudentScreens {

    private static StudentDashboardActivity me(ShellActivity a) { return (StudentDashboardActivity) a; }

    // =====================================================================
    // Trang chủ
    // =====================================================================
    public static class Home extends Screen {
        public Home(ShellActivity a) { super(a); }

        @Override public View build() {
            View r = inf.inflate(R.layout.screen_student_home, null);
            Student s = me(act).student;
            Stats all = db.stats(s.id, null), term = db.stats(s.id, DatabaseHelper.CURRENT_SEMESTER);

            Ui.setAvatar(r.findViewById(R.id.home_avatar), s.name);
            text(r, R.id.home_cohort, s.cohort + " · " + s.className);
            text(r, R.id.home_name, s.name);
            text(r, R.id.home_code, s.code);
            text(r, R.id.home_major, s.major);

            text(r, R.id.tv_gpa, Ui.f2(all.gpa4));
            text(r, R.id.tv_gpa_unit, str(R.string.home_gpa_unit));
            text(r, R.id.tv_gpa_sub, str(R.string.home_gpa_scale, Ui.f1(all.gpa10), DatabaseHelper.rating(all.gpa10)));
            text(r, R.id.tv_credits, String.valueOf(all.credits));
            text(r, R.id.tv_credits_unit, str(R.string.home_credits_unit, DatabaseHelper.PROGRAM_CREDITS));
            ((ProgressBar) r.findViewById(R.id.pb_credits)).setProgress(Math.min(100, all.credits * 100 / DatabaseHelper.PROGRAM_CREDITS));
            text(r, R.id.tv_term_gpa, Ui.f2(term.gpa4));
            text(r, R.id.tv_term_label, DatabaseHelper.CURRENT_SEMESTER);
            text(r, R.id.tv_passed, String.valueOf(all.passed));
            text(r, R.id.tv_pass_rate, str(R.string.home_pass_rate, all.total == 0 ? 0 : all.passed * 100 / all.total));

            r.findViewById(R.id.tv_see_all).setOnClickListener(v -> act.showTab(1));
            LinearLayout list = r.findViewById(R.id.recent_list);
            List<Grade> grades = db.gradesOfStudent(s.id);
            r.findViewById(R.id.tv_recent_empty).setVisibility(grades.isEmpty() ? View.VISIBLE : View.GONE);
            for (int i = 0; i < Math.min(2, grades.size()); i++) {
                Grade g = grades.get(i);
                View it = inf.inflate(R.layout.item_recent_grade, list, false);
                text(it, R.id.rg_code, g.subjectCode);
                text(it, R.id.rg_credits, str(R.string.home_credits_tc, g.credits));
                text(it, R.id.rg_name, g.subjectName);
                text(it, R.id.rg_sem, g.semester);
                text(it, R.id.rg_total, Ui.f1(g.total));
                TextView letter = it.findViewById(R.id.rg_letter);
                letter.setText(str(R.string.home_letter, g.letter));
                letter.setTextColor(Ui.letterColors(c, g.letter)[0]);
                list.addView(it);
            }
            return r;
        }
    }

    // =====================================================================
    // Bảng điểm
    // =====================================================================
    public static class Grades extends Screen {
        private List<Grade> all;
        private List<Grade> shown = new ArrayList<>();
        private String query = "", semester = null;
        private int sortMode = 0; // 0 mới nhất, 1 điểm cao nhất, 2 tên môn
        private View root;

        public Grades(ShellActivity a) { super(a); }

        @Override public View build() {
            root = inf.inflate(R.layout.screen_student_grades, null);
            all = db.gradesOfStudent(me(act).student.id);
            ((EditText) root.findViewById(R.id.et_search)).addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
                public void onTextChanged(CharSequence s, int a, int b, int d) {}
                public void afterTextChanged(Editable e) { query = e.toString(); refresh(); }
            });
            root.findViewById(R.id.btn_pdf).setOnClickListener(v -> exportPdf());
            root.findViewById(R.id.tv_sort).setOnClickListener(v -> { sortMode = (sortMode + 1) % 3; refresh(); });
            refresh();
            return root;
        }

        private void refresh() {
            LinearLayout chips = root.findViewById(R.id.chips);
            chips.removeAllViews();
            Ui.chip(inf, chips, str(R.string.grades_chip_all), semester == null, v -> { semester = null; refresh(); });
            for (String sem : db.semesters()) Ui.chip(inf, chips, sem, sem.equals(semester), v -> { semester = sem; refresh(); });

            shown = new ArrayList<>();
            String q = query.trim().toLowerCase();
            for (Grade g : all) {
                if (semester != null && !semester.equals(g.semester)) continue;
                if (!q.isEmpty() && !g.subjectName.toLowerCase().contains(q) && !g.subjectCode.toLowerCase().contains(q)) continue;
                shown.add(g);
            }
            if (sortMode == 1) Collections.sort(shown, (x, y) -> Double.compare(y.total, x.total));
            else if (sortMode == 2) Collections.sort(shown, (x, y) -> x.subjectName.compareTo(y.subjectName));
            text(root, R.id.tv_count, str(R.string.grades_count, shown.size()));
            text(root, R.id.tv_sort, "↕ " + c.getResources().getStringArray(R.array.grade_sorts)[sortMode]);

            int credits = 0, passed = 0;
            double pts = 0;
            for (Grade g : shown) {
                credits += g.credits;
                pts += DatabaseHelper.point4(g.letter) * g.credits;
                if (g.total >= 4.0) passed += g.credits;
            }
            text(root, R.id.tv_sum_credits, str(R.string.grades_sum_credits, credits, passed));
            text(root, R.id.tv_sum_gpa, Ui.f2(credits == 0 ? 0 : pts / credits));

            LinearLayout list = root.findViewById(R.id.list);
            list.removeAllViews();
            root.findViewById(R.id.tv_empty).setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
            for (Grade g : shown) {
                View it = inf.inflate(R.layout.item_grade, list, false);
                text(it, R.id.g_code, g.subjectCode);
                text(it, R.id.g_credits, str(R.string.grade_credits, g.credits));
                boolean pass = g.total >= 4.0;
                TextView st = it.findViewById(R.id.g_status);
                st.setText(pass ? R.string.grade_pass : R.string.grade_fail);
                st.setTextColor(c.getColor(pass ? R.color.green : R.color.red));
                text(it, R.id.g_name, g.subjectName);
                text(it, R.id.g_att, Ui.f1(g.att));
                text(it, R.id.g_ass, Ui.f1(g.ass));
                text(it, R.id.g_mid, Ui.f1(g.mid));
                text(it, R.id.g_fin, Ui.f1(g.fin));
                text(it, R.id.g_sem, g.semester);
                text(it, R.id.g_total, Ui.f1(g.total));
                Ui.bindLetter(it.findViewById(R.id.g_letter), g.letter);
                list.addView(it);
            }
        }

        /** Xuất bảng điểm đang hiển thị ra PDF trong thư mục riêng của ứng dụng. */
        private void exportPdf() {
            if (shown.isEmpty()) { Ui.toast(c, R.string.pdf_empty); return; }
            try {
                Student s = me(act).student;
                PdfDocument doc = new PdfDocument();
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                int pageNo = 1, y = 0;
                PdfDocument.Page page = null;
                android.graphics.Canvas cv = null;
                for (int i = -1; i < shown.size(); i++) {
                    if (page == null || y > 790) {
                        if (page != null) doc.finishPage(page);
                        page = doc.startPage(new PdfDocument.PageInfo.Builder(595, 842, pageNo++).create());
                        cv = page.getCanvas();
                        p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(16); p.setColor(Color.BLACK);
                        cv.drawText(str(R.string.pdf_heading, s.name, s.code), 40, 50, p);
                        p.setTextSize(10);
                        cv.drawText(str(R.string.pdf_col_code), 40, 80, p); cv.drawText(str(R.string.pdf_col_name), 110, 80, p);
                        cv.drawText(str(R.string.pdf_col_credits), 330, 80, p); cv.drawText(str(R.string.pdf_col_sem), 360, 80, p);
                        cv.drawText(str(R.string.pdf_col_score), 460, 80, p); cv.drawText(str(R.string.pdf_col_letter), 520, 80, p);
                        y = 100;
                    }
                    if (i < 0) continue;
                    Grade g = shown.get(i);
                    p.setTypeface(Typeface.DEFAULT); p.setTextSize(10);
                    cv.drawText(g.subjectCode, 40, y, p);
                    cv.drawText(g.subjectName.length() > 36 ? g.subjectName.substring(0, 35) + "…" : g.subjectName, 110, y, p);
                    cv.drawText(String.valueOf(g.credits), 330, y, p); cv.drawText(g.semester, 360, y, p);
                    cv.drawText(Ui.f1(g.total), 460, y, p); cv.drawText(g.letter, 520, y, p);
                    y += 20;
                }
                if (page != null) doc.finishPage(page);
                File out = new File(c.getExternalFilesDir(null), "BangDiem_" + s.code + ".pdf");
                FileOutputStream fos = new FileOutputStream(out);
                doc.writeTo(fos);
                fos.close();
                doc.close();
                new AlertDialog.Builder(c).setTitle(R.string.pdf_title).setMessage(str(R.string.pdf_saved, out.getAbsolutePath())).setPositiveButton(R.string.btn_close, null).show();
            } catch (Exception e) { Ui.toast(c, R.string.pdf_error); }
        }
    }

    // =====================================================================
    // Học phần
    // =====================================================================
    public static class Courses extends Screen {
        private boolean showCompleted = false;
        private List<Course> all;
        private View root;

        public Courses(ShellActivity a) { super(a); }

        @Override public View build() {
            root = inf.inflate(R.layout.screen_student_courses, null);
            all = db.coursesOfStudent(me(act).student.id);
            refresh();
            return root;
        }

        private void refresh() {
            int ongoing = 0, done = 0;
            for (Course k : all) if (k.completed) done++; else ongoing++;
            Ui.segment(root.findViewById(R.id.seg_tabs), str(R.string.seg_ongoing, ongoing), str(R.string.seg_done, done), showCompleted ? 1 : 0, i -> { showCompleted = i == 1; refresh(); });
            LinearLayout list = root.findViewById(R.id.list);
            list.removeAllViews();
            int n = 0;
            for (Course k : all) {
                if (k.completed != showCompleted) continue;
                list.addView(card(k, list));
                n++;
            }
            root.findViewById(R.id.tv_empty).setVisibility(n == 0 ? View.VISIBLE : View.GONE);
        }

        private View card(Course k, LinearLayout parent) {
            View it = inf.inflate(R.layout.item_course, parent, false);
            text(it, R.id.c_code, k.subjectCode);
            text(it, R.id.c_class, str(R.string.course_class, k.classCode));
            text(it, R.id.c_credits, str(R.string.grade_credits, k.credits));
            text(it, R.id.c_name, k.subjectName);
            text(it, R.id.c_schedule, k.schedule);
            text(it, R.id.c_room, k.room);
            Ui.setAvatar(it.findViewById(R.id.c_avatar), k.lecturer);
            text(it, R.id.c_lecturer, k.lecturer);
            text(it, R.id.c_email, k.lecturerEmail);
            it.findViewById(R.id.c_mail).setOnClickListener(v -> {
                try { c.startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + k.lecturerEmail))); }
                catch (Exception e) { Ui.toast(c, R.string.mail_none); }
            });
            int pct = k.totalSessions == 0 ? 0 : k.attended * 100 / k.totalSessions;
            text(it, R.id.c_attend, str(R.string.course_attend, k.attended, k.totalSessions, pct));
            ProgressBar pb = it.findViewById(R.id.c_progress);
            pb.setProgress(pct);
            pb.setProgressTintList(ColorStateList.valueOf(c.getColor(pct >= 80 ? R.color.primary : R.color.amber)));
            it.findViewById(R.id.c_syllabus).setOnClickListener(v -> new AlertDialog.Builder(c).setTitle(k.subjectName + " (" + k.subjectCode + ")")
                    .setMessage(str(R.string.syllabus_msg, k.description, k.credits, k.lecturer, k.semester)).setPositiveButton(R.string.btn_close, null).show());
            return it;
        }
    }

    // =====================================================================
    // Thông báo
    // =====================================================================
    public static class Notices extends Screen {
        private boolean unreadOnly = false;
        private List<Notice> all;
        private View root;

        public Notices(ShellActivity a) { super(a); }

        @Override public View build() {
            root = inf.inflate(R.layout.screen_student_notices, null);
            root.findViewById(R.id.btn_read_all).setOnClickListener(v -> { db.markAllNoticesRead(me(act).student.id); reload(); });
            reload();
            return root;
        }

        private void reload() { all = db.notices(me(act).student.id); refresh(); }

        private void refresh() {
            int unread = 0;
            for (Notice n : all) if (!n.read) unread++;
            Ui.segment(root.findViewById(R.id.seg_tabs), str(R.string.seg_all, all.size()), str(R.string.seg_unread, unread), unreadOnly ? 1 : 0, i -> { unreadOnly = i == 1; refresh(); });
            LinearLayout list = root.findViewById(R.id.list);
            list.removeAllViews();
            int shown = 0;
            for (Notice n : all) {
                if (unreadOnly && n.read) continue;
                list.addView(card(n, list));
                shown++;
            }
            root.findViewById(R.id.tv_empty).setVisibility(shown == 0 ? View.VISIBLE : View.GONE);
        }

        private View card(Notice n, LinearLayout parent) {
            int icon, fg, bg;
            switch (n.type) {
                case "grade": icon = R.drawable.ic_star; fg = R.color.primary; bg = R.color.primary_soft; break;
                case "exam": icon = R.drawable.ic_event; fg = R.color.amber; bg = R.color.amber_soft; break;
                default: icon = R.drawable.ic_info; fg = R.color.purple; bg = R.color.purple_soft;
            }
            View it = inf.inflate(R.layout.item_notice, parent, false);
            it.setBackgroundResource(n.read ? R.drawable.bg_notice_read : R.drawable.bg_notice_unread);
            ImageView ic = it.findViewById(R.id.n_icon);
            ic.setImageResource(icon);
            ic.setImageTintList(ColorStateList.valueOf(c.getColor(fg)));
            Ui.tint(ic, c.getColor(bg));
            text(it, R.id.n_title, n.title);
            text(it, R.id.n_body, n.body);
            text(it, R.id.n_time, Ui.relativeTime(c, n.createdAt));
            it.setOnClickListener(v -> {
                db.markNoticeRead(n.id);
                n.read = true;
                new AlertDialog.Builder(c).setTitle(n.title).setMessage(n.body + "\n\n" + Ui.fullTime(n.createdAt))
                        .setPositiveButton(R.string.btn_close, (d, w) -> refresh()).setOnCancelListener(d -> refresh()).show();
            });
            return it;
        }
    }

    // =====================================================================
    // Tài khoản
    // =====================================================================
    public static class Account extends Screen {
        public Account(ShellActivity a) { super(a); }

        @Override public View build() {
            StudentDashboardActivity sa = me(act);
            Student s = sa.student;
            View r = inf.inflate(R.layout.screen_student_account, null);
            Ui.setAvatar(r.findViewById(R.id.acc_avatar), s.name);
            text(r, R.id.acc_name, s.name);
            text(r, R.id.acc_code, s.code);
            text(r, R.id.acc_class, s.className);
            text(r, R.id.acc_major, s.major);
            text(r, R.id.acc_faculty, s.faculty);
            text(r, R.id.acc_cohort, s.cohort);
            text(r, R.id.acc_dob, s.dob);
            text(r, R.id.acc_gender, s.gender);
            text(r, R.id.acc_email, s.email);
            text(r, R.id.acc_phone, s.phone);
            text(r, R.id.acc_address, s.address);

            Ui.action(inf, r.findViewById(R.id.acc_actions), R.drawable.ic_lock, str(R.string.acc_password_title), str(R.string.acc_password_sub), false,
                    v -> PasswordDialog.show(act, sa.loginCode));
            if (sa.demo) Ui.action(inf, r.findViewById(R.id.acc_demo_actions), R.drawable.ic_shield, str(R.string.acc_admin_title), str(R.string.acc_admin_sub), false,
                    v -> sa.openAdminDemo());

            SharedPreferences p = c.getSharedPreferences("unigrade", Context.MODE_PRIVATE);
            CheckBox cb = r.findViewById(R.id.cb_notify);
            cb.setChecked(p.getBoolean("notify_" + s.code, true));
            cb.setOnCheckedChangeListener((b, on) -> p.edit().putBoolean("notify_" + s.code, on).apply());
            r.findViewById(R.id.btn_logout).setOnClickListener(v -> sa.logout());
            return r;
        }
    }
}
