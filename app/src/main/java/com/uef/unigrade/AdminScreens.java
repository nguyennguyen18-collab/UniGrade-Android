package com.uef.unigrade;

import android.app.AlertDialog;
import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import com.uef.unigrade.Models.*;

/** Các màn hình của Ban Quản Trị (layout: res/layout/screen_admin_*.xml, dialog_*.xml, item_*.xml). */
public class AdminScreens {

    private static AdminDashboardActivity me(ShellActivity a) { return (AdminDashboardActivity) a; }

    private static TextWatcher onChange(Runnable r) {
        return new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
            public void onTextChanged(CharSequence s, int a, int b, int d) {}
            public void afterTextChanged(Editable e) { r.run(); }
        };
    }

    private static String s(View root, int id) { return ((EditText) root.findViewById(id)).getText().toString(); }
    private static void set(View root, int id, String v) { ((EditText) root.findViewById(id)).setText(v); }

    // =====================================================================
    // Tổng quan
    // =====================================================================
    public static class Overview extends Screen {
        public Overview(ShellActivity a) { super(a); }

        @Override public View build() {
            View r = inf.inflate(R.layout.screen_admin_overview, null);
            Staff st = me(act).staff;
            Models.Overview o = db.overview();
            text(r, R.id.ov_name, st.name);
            text(r, R.id.ov_position, str(R.string.ov_position, st.position));
            Ui.setAvatar(r.findViewById(R.id.ov_avatar), st.name);
            text(r, R.id.ov_semester, str(R.string.ov_semester, DatabaseHelper.CURRENT_SEMESTER));
            r.findViewById(R.id.btn_enter_grades).setOnClickListener(v -> act.showTab(3));

            text(r, R.id.ov_students, String.valueOf(o.students));
            text(r, R.id.ov_students_sub, str(R.string.ov_students_sub));
            text(r, R.id.ov_subjects, String.valueOf(o.subjects));
            text(r, R.id.ov_subjects_sub, str(R.string.ov_subjects_sub));
            text(r, R.id.ov_classes, String.valueOf(o.classes));
            text(r, R.id.ov_classes_sub, DatabaseHelper.CURRENT_SEMESTER);
            text(r, R.id.ov_grades, String.valueOf(o.grades));
            text(r, R.id.ov_grades_sub, str(R.string.ov_grades_sub, Ui.f1(o.completedPct)));

            int[] bandIds = {R.id.band_0, R.id.band_1, R.id.band_2, R.id.band_3};
            int[] pctIds = {R.id.pct_0, R.id.pct_1, R.id.pct_2, R.id.pct_3};
            int total = Math.max(1, o.bands[0] + o.bands[1] + o.bands[2] + o.bands[3]);
            for (int i = 0; i < 4; i++) {
                View band = r.findViewById(bandIds[i]);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) band.getLayoutParams();
                lp.weight = o.bands[i];
                band.setLayoutParams(lp);
                text(r, pctIds[i], Math.round(o.bands[i] * 100f / total) + "%");
            }
            return r;
        }
    }

    // =====================================================================
    // Quản lý sinh viên
    // =====================================================================
    public static class Students extends Screen {
        private String query = "", faculty = null;
        private View root;

        public Students(ShellActivity a) { super(a); }

        @Override public View build() {
            root = inf.inflate(R.layout.screen_admin_students, null);
            root.findViewById(R.id.btn_add).setOnClickListener(v -> edit(new Student()));
            ((EditText) root.findViewById(R.id.et_search)).addTextChangedListener(onChange(() -> { query = s(root, R.id.et_search); refresh(); }));
            refresh();
            return root;
        }

        private void refresh() {
            LinearLayout chips = root.findViewById(R.id.chips);
            chips.removeAllViews();
            Ui.chip(inf, chips, str(R.string.chip_all_faculty), faculty == null, v -> { faculty = null; refresh(); });
            for (String f : db.faculties("Student")) Ui.chip(inf, chips, f, f.equals(faculty), v -> { faculty = f; refresh(); });
            List<Student> students = db.listStudents(query, faculty);
            text(root, R.id.tv_total, str(R.string.students_total, students.size()));
            LinearLayout list = root.findViewById(R.id.list);
            list.removeAllViews();
            TextView empty = root.findViewById(R.id.tv_empty);
            empty.setText(R.string.students_empty);
            empty.setVisibility(students.isEmpty() ? View.VISIBLE : View.GONE);
            for (Student st : students) list.addView(card(st, list));
        }

        private View card(Student st, LinearLayout parent) {
            View it = inf.inflate(R.layout.item_student, parent, false);
            Ui.setAvatar(it.findViewById(R.id.s_avatar), st.name);
            text(it, R.id.s_name, st.name);
            text(it, R.id.s_code, st.code);
            text(it, R.id.s_class, " · " + st.className);
            text(it, R.id.s_gpa, Ui.f2(st.gpa4));
            text(it, R.id.s_major, st.major);
            it.findViewById(R.id.s_edit).setOnClickListener(v -> edit(st));
            it.findViewById(R.id.s_delete).setOnClickListener(v -> Ui.confirm(c, str(R.string.student_delete_title), str(R.string.student_delete_msg, st.name), () -> {
                db.deleteStudent(st.id);
                refresh();
                Ui.toast(c, R.string.student_deleted);
            }));
            it.setOnClickListener(v -> detail(st));
            return it;
        }

        private void detail(Student st) {
            Stats stats = db.stats(st.id, null);
            StringBuilder sb = new StringBuilder(str(R.string.student_detail, st.code, st.className, st.faculty, st.major, st.email, Ui.f2(stats.gpa4), stats.credits));
            List<Grade> gs = db.gradesOfStudent(st.id);
            if (gs.isEmpty()) sb.append(str(R.string.student_no_grades));
            for (Grade g : gs) sb.append(g.subjectCode).append(" · ").append(g.subjectName).append("\n   ").append(g.semester).append(": ").append(Ui.f1(g.total)).append(" (").append(g.letter).append(")\n");
            new AlertDialog.Builder(c).setTitle(st.name).setMessage(sb.toString()).setPositiveButton(R.string.btn_close, null).show();
        }

        private void edit(Student st) {
            View f = inf.inflate(R.layout.dialog_student, null);
            set(f, R.id.f_code, st.code); set(f, R.id.f_name, st.name); set(f, R.id.f_email, st.email); set(f, R.id.f_class, st.className);
            set(f, R.id.f_faculty, st.faculty); set(f, R.id.f_major, st.major); set(f, R.id.f_cohort, st.cohort); set(f, R.id.f_dob, st.dob);
            set(f, R.id.f_phone, st.phone); set(f, R.id.f_address, st.address);
            String[] genders = c.getResources().getStringArray(R.array.genders);
            Ui.Select gender = new Ui.Select(f.findViewById(R.id.sel_gender), str(R.string.gender_title));
            gender.setOptions(genders, Arrays.asList(genders).indexOf(st.gender));
            new AlertDialog.Builder(c).setTitle(st.id > 0 ? R.string.student_edit : R.string.student_add).setView(f).setNegativeButton(R.string.btn_cancel, null)
                    .setPositiveButton(R.string.btn_save, (d, w) -> {
                        Student n = new Student();
                        n.id = st.id; n.code = s(f, R.id.f_code); n.name = s(f, R.id.f_name); n.email = s(f, R.id.f_email); n.className = s(f, R.id.f_class);
                        n.faculty = s(f, R.id.f_faculty); n.major = s(f, R.id.f_major); n.cohort = s(f, R.id.f_cohort); n.dob = s(f, R.id.f_dob);
                        n.gender = genders[gender.getIndex()]; n.phone = s(f, R.id.f_phone); n.address = s(f, R.id.f_address);
                        String err = db.saveStudent(n);
                        if (err != null) Ui.toast(c, err);
                        else {
                            Ui.toast(c, st.id > 0 ? str(R.string.student_saved) : str(R.string.student_added, DatabaseHelper.DEFAULT_PASSWORD));
                            refresh();
                        }
                    }).show();
        }
    }

    // =====================================================================
    // Quản lý môn học
    // =====================================================================
    public static class Subjects extends Screen {
        private String query = "", faculty = null;
        private View root;

        public Subjects(ShellActivity a) { super(a); }

        @Override public View build() {
            root = inf.inflate(R.layout.screen_admin_subjects, null);
            root.findViewById(R.id.btn_add).setOnClickListener(v -> edit(new Subject()));
            ((EditText) root.findViewById(R.id.et_search)).addTextChangedListener(onChange(() -> { query = s(root, R.id.et_search); refresh(); }));
            refresh();
            return root;
        }

        private void refresh() {
            LinearLayout chips = root.findViewById(R.id.chips);
            chips.removeAllViews();
            Ui.chip(inf, chips, str(R.string.chip_all_faculty), faculty == null, v -> { faculty = null; refresh(); });
            for (String f : db.faculties("Subject")) Ui.chip(inf, chips, f, f.equals(faculty), v -> { faculty = f; refresh(); });
            List<Subject> subs = db.listSubjects(query, faculty);
            text(root, R.id.tv_total, str(R.string.subjects_total, subs.size()));
            LinearLayout list = root.findViewById(R.id.list);
            list.removeAllViews();
            TextView empty = root.findViewById(R.id.tv_empty);
            empty.setText(R.string.subjects_empty);
            empty.setVisibility(subs.isEmpty() ? View.VISIBLE : View.GONE);
            for (Subject sj : subs) list.addView(card(sj, list));
        }

        private View card(Subject sj, LinearLayout parent) {
            View it = inf.inflate(R.layout.item_subject, parent, false);
            text(it, R.id.sj_code, sj.code);
            text(it, R.id.sj_credits, str(R.string.grade_credits, sj.credits));
            text(it, R.id.sj_name, sj.name);
            text(it, R.id.sj_desc, sj.description);
            text(it, R.id.sj_ratio, str(R.string.subject_ratio, sj.wAtt, sj.wAss, sj.wMid, sj.wFin));
            text(it, R.id.sj_faculty, sj.faculty);
            text(it, R.id.sj_term, sj.suggestedTerm.isEmpty() ? "" : str(R.string.subject_term, sj.suggestedTerm));
            it.findViewById(R.id.sj_edit).setOnClickListener(v -> edit(sj));
            it.findViewById(R.id.sj_delete).setOnClickListener(v -> Ui.confirm(c, str(R.string.subject_delete_title), str(R.string.subject_delete_msg), () -> {
                db.deleteSubject(sj.id);
                refresh();
                Ui.toast(c, R.string.subject_deleted);
            }));
            return it;
        }

        private void edit(Subject sj) {
            View f = inf.inflate(R.layout.dialog_subject, null);
            set(f, R.id.f_code, sj.code); set(f, R.id.f_name, sj.name); set(f, R.id.f_credits, String.valueOf(sj.credits)); set(f, R.id.f_desc, sj.description);
            set(f, R.id.f_faculty, sj.faculty); set(f, R.id.f_term, sj.suggestedTerm);
            set(f, R.id.f_wa, String.valueOf(sj.wAtt)); set(f, R.id.f_wb, String.valueOf(sj.wAss)); set(f, R.id.f_wc, String.valueOf(sj.wMid)); set(f, R.id.f_wd, String.valueOf(sj.wFin));
            new AlertDialog.Builder(c).setTitle(sj.id > 0 ? R.string.subject_edit : R.string.subject_add).setView(f).setNegativeButton(R.string.btn_cancel, null)
                    .setPositiveButton(R.string.btn_save, (d, w) -> {
                        Subject n = new Subject();
                        n.id = sj.id; n.code = s(f, R.id.f_code); n.name = s(f, R.id.f_name); n.description = s(f, R.id.f_desc);
                        n.faculty = s(f, R.id.f_faculty); n.suggestedTerm = s(f, R.id.f_term);
                        try {
                            n.credits = Integer.parseInt(s(f, R.id.f_credits).trim());
                            n.wAtt = Integer.parseInt(s(f, R.id.f_wa).trim()); n.wAss = Integer.parseInt(s(f, R.id.f_wb).trim());
                            n.wMid = Integer.parseInt(s(f, R.id.f_wc).trim()); n.wFin = Integer.parseInt(s(f, R.id.f_wd).trim());
                        } catch (Exception e) { Ui.toast(c, R.string.subject_bad_number); return; }
                        String err = db.saveSubject(n);
                        if (err != null) Ui.toast(c, err); else { Ui.toast(c, R.string.subject_saved); refresh(); }
                    }).show();
        }
    }

    // =====================================================================
    // Nhập điểm
    // =====================================================================
    public static class GradeEntry extends Screen {
        private List<Student> students;
        private List<Subject> subjects;
        private List<String> semesters;
        private int studentIdx = 0;
        private Ui.Select semSel, subSel;
        private View root;
        private EditText[] inputs = new EditText[4];

        public GradeEntry(ShellActivity a) { super(a); }

        @Override public View build() {
            root = inf.inflate(R.layout.screen_admin_grade_entry, null);
            students = db.listStudents("", null);
            subjects = db.listSubjects("", null);
            semesters = db.semesters();
            if (students.isEmpty() || subjects.isEmpty()) {
                root.findViewById(R.id.ge_content).setVisibility(View.GONE);
                root.findViewById(R.id.ge_empty).setVisibility(View.VISIBLE);
                return root;
            }
            semSel = new Ui.Select(root.findViewById(R.id.sel_semester), str(R.string.ge_title_semester));
            semSel.setOptions(semesters.toArray(new String[0]), 0);
            subSel = new Ui.Select(root.findViewById(R.id.sel_subject), str(R.string.ge_title_subject));
            String[] names = new String[subjects.size()];
            for (int i = 0; i < names.length; i++) names[i] = subjects.get(i).code + " - " + subjects.get(i).name + " (" + subjects.get(i).credits + " TC)";
            subSel.setOptions(names, 0);
            semSel.setOnChange(this::loadValues);
            subSel.setOnChange(this::loadValues);
            text(root, R.id.ge_pick_label, str(R.string.ge_pick, students.size()));

            inputs = new EditText[]{root.findViewById(R.id.ge_att), root.findViewById(R.id.ge_ass), root.findViewById(R.id.ge_mid), root.findViewById(R.id.ge_fin)};
            for (EditText e : inputs) e.addTextChangedListener(onChange(this::updatePreview));
            root.findViewById(R.id.btn_save).setOnClickListener(v -> save());
            renderChips();
            loadValues();
            return root;
        }

        private Subject subject() { return subjects.get(subSel.getIndex()); }
        private String semester() { return semesters.get(semSel.getIndex()); }

        private void renderChips() {
            LinearLayout row = root.findViewById(R.id.chip_row);
            row.removeAllViews();
            for (int i = 0; i < students.size(); i++) {
                final int idx = i;
                Student st = students.get(i);
                View chip = inf.inflate(R.layout.item_student_chip, row, false);
                chip.setSelected(i == studentIdx);
                Ui.setAvatar(chip.findViewById(R.id.pc_avatar), st.name);
                text(chip, R.id.pc_name, st.name);
                text(chip, R.id.pc_code, st.code);
                chip.setOnClickListener(v -> { studentIdx = idx; renderChips(); loadValues(); });
                row.addView(chip);
            }
        }

        /** Nạp điểm hiện có (nếu có) của sinh viên/môn/học kỳ đang chọn. */
        private void loadValues() {
            Student st = students.get(studentIdx);
            Subject sub = subject();
            text(root, R.id.ge_name, st.name);
            text(root, R.id.ge_code, sub.code);
            text(root, R.id.ge_subject, sub.name);
            int[] w = {sub.wAtt, sub.wAss, sub.wMid, sub.wFin};
            int[] wIds = {R.id.ge_w0, R.id.ge_w1, R.id.ge_w2, R.id.ge_w3};
            for (int i = 0; i < 4; i++) text(root, wIds[i], w[i] + "%");
            Grade g = db.findGrade(st.id, sub.id, semester());
            double[] v = g == null ? null : new double[]{g.att, g.ass, g.mid, g.fin};
            for (int i = 0; i < 4; i++) inputs[i].setText(v == null ? "" : fmt(v[i]));
            set(root, R.id.et_reason, "");
            text(root, R.id.ge_status, str(g == null ? R.string.ge_status_new : R.string.ge_status_old));
            updatePreview();
        }

        private static String fmt(double v) { return v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v); }

        private Double parse(EditText e) {
            try { return Double.parseDouble(e.getText().toString().trim().replace(',', '.')); } catch (Exception ex) { return null; }
        }

        private void updatePreview() {
            Double[] v = new Double[4];
            for (int i = 0; i < 4; i++) {
                v[i] = parse(inputs[i]);
                if (v[i] == null) { text(root, R.id.ge_preview, str(R.string.ge_preview_empty)); return; }
            }
            Subject sub = subject();
            double t = DatabaseHelper.calcTotal(v[0], v[1], v[2], v[3], sub.wAtt, sub.wAss, sub.wMid, sub.wFin);
            String l = DatabaseHelper.letter(t);
            text(root, R.id.ge_preview, str(R.string.ge_preview, Ui.f2(t), Ui.f1(DatabaseHelper.point4(l)), l));
        }

        private void save() {
            Double[] v = new Double[4];
            for (int i = 0; i < 4; i++) {
                v[i] = parse(inputs[i]);
                if (v[i] == null) { Ui.toast(c, R.string.ge_invalid); return; }
            }
            Staff staff = me(act).staff;
            String err = db.saveGrade(students.get(studentIdx).id, subject().id, semester(), v[0], v[1], v[2], v[3], s(root, R.id.et_reason), staff.name + " (" + staff.position + ")");
            if (err != null) { Ui.toast(c, err); return; }
            Ui.toast(c, R.string.ge_saved);
            loadValues();
        }
    }

    // =====================================================================
    // Lịch sử thay đổi điểm
    // =====================================================================
    public static class HistoryLog extends Screen {
        private String query = "";
        private View root;

        public HistoryLog(ShellActivity a) { super(a); }

        @Override public View build() {
            root = inf.inflate(R.layout.screen_admin_history, null);
            ((EditText) root.findViewById(R.id.et_search)).addTextChangedListener(onChange(() -> { query = s(root, R.id.et_search); refresh(); }));
            refresh();
            return root;
        }

        private void refresh() {
            List<History> hs = db.listHistory(query);
            text(root, R.id.tv_total, str(R.string.history_total, hs.size()));
            LinearLayout list = root.findViewById(R.id.list);
            list.removeAllViews();
            root.findViewById(R.id.tv_empty).setVisibility(hs.isEmpty() ? View.VISIBLE : View.GONE);
            for (History h : hs) {
                View it = inf.inflate(R.layout.item_history, list, false);
                text(it, R.id.h_name, h.studentName);
                text(it, R.id.h_code, h.studentCode);
                text(it, R.id.h_sem, h.semester);
                text(it, R.id.h_subject, h.subjectName + " (" + h.subjectCode + ")");
                TextView old = it.findViewById(R.id.h_old);
                old.setText(h.oldTotal == null ? "—" : Ui.f1(h.oldTotal));
                if (h.oldTotal != null) old.setPaintFlags(old.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                text(it, R.id.h_old_letter, h.oldTotal == null ? "" : " (" + DatabaseHelper.letter(h.oldTotal) + ")");
                text(it, R.id.h_new, Ui.f1(h.newTotal));
                Ui.bindLetter(it.findViewById(R.id.h_new_letter), DatabaseHelper.letter(h.newTotal));
                text(it, R.id.h_reason, str(R.string.history_reason, h.reason));
                text(it, R.id.h_by, h.changedBy);
                text(it, R.id.h_time, Ui.fullTime(h.changedAt));
                list.addView(it);
            }
        }
    }

    // =====================================================================
    // Hồ sơ cán bộ
    // =====================================================================
    public static class Profile extends Screen {
        public Profile(ShellActivity a) { super(a); }

        @Override public View build() {
            AdminDashboardActivity aa = me(act);
            Staff st = aa.staff;
            View r = inf.inflate(R.layout.screen_admin_profile, null);
            Ui.setAvatar(r.findViewById(R.id.ap_avatar), st.name);
            text(r, R.id.ap_name, st.name);
            text(r, R.id.ap_code, st.code);
            text(r, R.id.ap_position_short, str(R.string.ap_position_short, st.position));
            text(r, R.id.ap_unit, st.unit);
            text(r, R.id.ap_position, st.position);
            text(r, R.id.ap_email, st.email);
            text(r, R.id.ap_phone, st.phone);
            text(r, R.id.ap_office, st.office);

            LinearLayout actions = r.findViewById(R.id.ap_actions);
            Ui.action(inf, actions, R.drawable.ic_lock, str(R.string.ap_pw_title), str(R.string.ap_pw_sub), false, v -> PasswordDialog.show(act, st.code));
            if (aa.demo) Ui.action(inf, actions, R.drawable.ic_person, str(R.string.ap_student_title), str(R.string.ap_student_sub), true, v -> aa.openStudentDemo());
            Ui.action(inf, actions, R.drawable.ic_history, str(R.string.ap_reset_title), str(R.string.ap_reset_sub), false,
                    v -> Ui.confirm(c, str(R.string.reset_confirm_title), str(R.string.reset_confirm_msg), () -> {
                        db.resetData();
                        Ui.toast(c, R.string.reset_done);
                        act.showTab(0);
                    }));
            r.findViewById(R.id.btn_logout).setOnClickListener(v -> act.logout());
            return r;
        }
    }
}
