package com.uef.unigrade;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

/** Màn hình đăng nhập (res/layout/activity_login.xml). */
public class MainActivity extends Activity {
    private DatabaseHelper db;
    private SharedPreferences prefs;
    private boolean adminRole = false;
    private EditText code, password;
    private CheckBox remember;
    private TextView codeLabel;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        db = new DatabaseHelper(this);
        prefs = getSharedPreferences("unigrade", MODE_PRIVATE);
        adminRole = "admin".equals(prefs.getString("role", "student"));

        setContentView(R.layout.activity_login);
        Ui.applyInsets(findViewById(R.id.login_root));
        codeLabel = findViewById(R.id.tv_code_label);
        code = findViewById(R.id.et_code);
        password = findViewById(R.id.et_password);
        remember = findViewById(R.id.cb_remember);
        password.setText(DatabaseHelper.DEFAULT_PASSWORD);
        remember.setChecked(prefs.getBoolean("remember", true));

        findViewById(R.id.tv_forgot).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.forgot_title).setMessage(R.string.forgot_msg).setPositiveButton(R.string.btn_understood, null).show());
        findViewById(R.id.btn_login).setOnClickListener(v -> doLogin(code.getText().toString(), password.getText().toString(), false, adminRole));
        findViewById(R.id.btn_demo_student).setOnClickListener(v -> doLogin("SV2024001", DatabaseHelper.DEFAULT_PASSWORD, true, false));
        findViewById(R.id.btn_demo_admin).setOnClickListener(v -> doLogin("GV9901", DatabaseHelper.DEFAULT_PASSWORD, true, true));
        applyRole();
    }

    private void applyRole() {
        Ui.segment(findViewById(R.id.seg_role), getString(R.string.seg_student), getString(R.string.seg_admin), adminRole ? 1 : 0, i -> {
            adminRole = i == 1;
            applyRole();
        });
        codeLabel.setText(adminRole ? R.string.label_code_admin : R.string.label_code_student);
        String saved = prefs.getString("code_" + (adminRole ? "admin" : "student"), null);
        code.setText(saved != null && remember.isChecked() ? saved : (adminRole ? "GV9901" : "SV2024001"));
    }

    private void doLogin(String loginCode, String pass, boolean demo, boolean admin) {
        loginCode = loginCode.trim();
        if (loginCode.isEmpty() || pass.isEmpty()) { Ui.toast(this, R.string.toast_fill_all); return; }
        String role = admin ? "admin" : "student";
        if (!db.login(loginCode, pass, role)) { Ui.toast(this, R.string.toast_bad_login); return; }
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("remember", remember.isChecked()).putString("role", role);
        if (remember.isChecked()) e.putString("code_" + role, loginCode); else e.remove("code_" + role);
        e.apply();
        Intent i;
        if (admin) i = new Intent(this, AdminDashboardActivity.class);
        else { i = new Intent(this, StudentDashboardActivity.class); i.putExtra("studentId", db.studentIdForCode(loginCode)); }
        i.putExtra("loginCode", loginCode);
        i.putExtra("demo", demo);
        startActivity(i);
    }
}
