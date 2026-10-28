package com.uef.unigrade;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Khung chung (res/layout/activity_shell.xml): thanh trên, nội dung, thanh điều hướng dưới. */
public abstract class ShellActivity extends Activity {
    public DatabaseHelper db;
    protected FrameLayout content;
    private LinearLayout nav;
    private TextView titleView;
    private int currentTab = 0;
    private boolean showingExtra = false;

    protected abstract String[] tabLabels();
    protected abstract int[] tabIcons();
    protected abstract String[] tabTitles();
    protected abstract Screen createTab(int index);
    protected abstract String roleLabel();
    protected abstract String userName();
    protected abstract void onAvatarClick();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        db = new DatabaseHelper(this);
    }

    /** Gọi sau khi lớp con đã nạp xong dữ liệu của mình. */
    protected void buildShell(int startTab) {
        setContentView(R.layout.activity_shell);
        Ui.applyInsets(findViewById(R.id.shell_root));
        content = findViewById(R.id.shell_content);
        nav = findViewById(R.id.shell_nav);
        titleView = findViewById(R.id.bar_title);

        View avatar = findViewById(R.id.bar_avatar);
        Ui.setAvatar(avatar, userName());
        avatar.setOnClickListener(v -> onAvatarClick());

        String sub = roleLabel() + " · " + userName();
        SpannableStringBuilder sp = new SpannableStringBuilder(sub);
        sp.setSpan(new ForegroundColorSpan(getColor(R.color.primary)), roleLabel().length() + 3, sub.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView) findViewById(R.id.bar_subtitle)).setText(sp);
        showTab(startTab);
    }

    private void buildNav(int selected) {
        nav.removeAllViews();
        String[] labels = tabLabels();
        int[] icons = tabIcons();
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            boolean sel = i == selected;
            int color = getColor(sel ? R.color.primary : R.color.nav_inactive);
            View item = getLayoutInflater().inflate(R.layout.item_nav, nav, false);
            ImageView ic = item.findViewById(R.id.nav_icon);
            ic.setImageResource(icons[i]);
            ic.setImageTintList(ColorStateList.valueOf(color));
            TextView t = item.findViewById(R.id.nav_label);
            t.setText(labels[i]);
            t.setTextColor(color);
            t.setTypeface(null, sel ? Typeface.BOLD : Typeface.NORMAL);
            Ui.tint(item.findViewById(R.id.nav_dot), sel ? color : Color.TRANSPARENT);
            item.setOnClickListener(v -> showTab(idx));
            nav.addView(item);
        }
    }

    public void showTab(int index) {
        currentTab = index;
        showingExtra = false;
        titleView.setText(tabTitles()[index]);
        buildNav(index);
        setContent(createTab(index).build());
    }

    /** Màn hình phụ không thuộc tab nào (ví dụ hồ sơ cán bộ). */
    public void showExtra(String title, Screen screen) {
        showingExtra = true;
        titleView.setText(title);
        buildNav(-1);
        setContent(screen.build());
    }

    private void setContent(View v) {
        content.removeAllViews();
        content.addView(v, new FrameLayout.LayoutParams(-1, -1));
    }

    @Override public void onBackPressed() {
        if (showingExtra) showTab(currentTab); else super.onBackPressed();
    }

    public void logout() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }
}
