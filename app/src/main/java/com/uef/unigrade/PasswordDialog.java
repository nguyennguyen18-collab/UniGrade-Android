package com.uef.unigrade;

import android.app.AlertDialog;
import android.view.View;
import android.widget.EditText;

/** Hộp thoại đổi mật khẩu (res/layout/dialog_password.xml) dùng chung cho sinh viên và cán bộ. */
public class PasswordDialog {
    public static void show(ShellActivity a, String loginCode) {
        View form = a.getLayoutInflater().inflate(R.layout.dialog_password, null);
        EditText oldP = form.findViewById(R.id.et_old), newP = form.findViewById(R.id.et_new), cfm = form.findViewById(R.id.et_confirm);
        new AlertDialog.Builder(a).setTitle(R.string.pw_title).setView(form).setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_save, (d, w) -> {
                    String n = newP.getText().toString();
                    if (n.length() < 6) { Ui.toast(a, R.string.pw_short); return; }
                    if (!n.equals(cfm.getText().toString())) { Ui.toast(a, R.string.pw_mismatch); return; }
                    Ui.toast(a, a.db.changePassword(loginCode, oldP.getText().toString(), n) ? R.string.pw_done : R.string.pw_wrong);
                }).show();
    }
}
