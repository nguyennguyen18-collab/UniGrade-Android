package com.uef.unigrade;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.function.IntConsumer;

/**
 * Tiện ích dùng chung. Giao diện nằm trong res/layout, res/drawable, res/values;
 * lớp này chỉ giữ các hàm gán dữ liệu/màu động và định dạng.
 */
public class Ui {
    private static final int[] AVATAR_COLORS = {0xFF2563EB, 0xFF7C3AED, 0xFF0891B2, 0xFFDB2777, 0xFF059669, 0xFFEA580C};

    public static int dp(Context c, int v) { return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f); }

    public static void applyInsets(View v) {
        v.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });
    }

    /** Đổi màu nền (drawable dạng shape trắng) của một view. */
    public static void tint(View v, int color) { v.setBackgroundTintList(ColorStateList.valueOf(color)); }

    // ---------- avatar ----------
    public static String initials(String name) {
        ArrayList<String> real = new ArrayList<>();
        for (String p : name.trim().split("\\s+")) if (!p.isEmpty() && !p.endsWith(".")) real.add(p);
        if (real.isEmpty()) return "?";
        String s = real.get(0).substring(0, 1);
        if (real.size() > 1) s += real.get(real.size() - 1).substring(0, 1);
        return s.toUpperCase(Locale.ROOT);
    }

    public static void setAvatar(View avatar, String name) {
        TextView t = (TextView) avatar;
        t.setText(initials(name));
        tint(t, AVATAR_COLORS[Math.abs(name.hashCode()) % AVATAR_COLORS.length]);
    }

    // ---------- điểm số ----------
    public static String f1(double v) { return String.format(Locale.US, "%.1f", v); }
    public static String f2(double v) { return String.format(Locale.US, "%.2f", v); }

    /** {màu chữ, màu nền} theo điểm chữ. */
    public static int[] letterColors(Context c, String letter) {
        switch (letter) {
            case "A+": case "A": return new int[]{c.getColor(R.color.green), c.getColor(R.color.green_soft)};
            case "B+": case "B": return new int[]{c.getColor(R.color.primary), c.getColor(R.color.primary_soft)};
            case "C+": case "C": return new int[]{c.getColor(R.color.amber), c.getColor(R.color.amber_soft)};
            case "D+": case "D": return new int[]{c.getColor(R.color.orange), c.getColor(R.color.orange_soft)};
            default: return new int[]{c.getColor(R.color.red), c.getColor(R.color.red_soft)};
        }
    }

    /** Gán chữ "A+ (4.0)" và màu cho một chip điểm chữ (style Chip). */
    public static void bindLetter(TextView t, String letter) {
        int[] col = letterColors(t.getContext(), letter);
        t.setText(letter + " (" + f1(DatabaseHelper.point4(letter)) + ")");
        t.setTextColor(col[0]);
        tint(t, col[1]);
    }

    // ---------- thành phần dựng từ layout ----------
    /** Thanh 2 tab (view_segment2). */
    public static void segment(View root, String l0, String l1, int selected, IntConsumer onPick) {
        TextView a = root.findViewById(R.id.seg_0), b = root.findViewById(R.id.seg_1);
        a.setText(l0);
        b.setText(l1);
        a.setSelected(selected == 0);
        b.setSelected(selected == 1);
        a.setOnClickListener(v -> onPick.accept(0));
        b.setOnClickListener(v -> onPick.accept(1));
    }

    /** Thêm một chip lọc (item_chip) vào hàng chip. */
    public static void chip(LayoutInflater inf, ViewGroup parent, String text, boolean selected, View.OnClickListener l) {
        TextView t = (TextView) inf.inflate(R.layout.item_chip, parent, false);
        t.setText(text);
        t.setSelected(selected);
        t.setOnClickListener(l);
        parent.addView(t);
    }

    /** Thêm một dòng hành động (item_action: icon, tiêu đề, mô tả, mũi tên). */
    public static void action(LayoutInflater inf, ViewGroup parent, int icon, String title, String sub, boolean accent, View.OnClickListener l) {
        View row = inf.inflate(R.layout.item_action, parent, false);
        ImageView ic = row.findViewById(R.id.a_icon);
        ic.setImageResource(icon);
        if (accent) {
            ic.setImageTintList(ColorStateList.valueOf(row.getContext().getColor(R.color.primary)));
            tint(ic, row.getContext().getColor(R.color.primary_soft));
        }
        ((TextView) row.findViewById(R.id.a_title)).setText(title);
        ((TextView) row.findViewById(R.id.a_sub)).setText(sub);
        row.setOnClickListener(l);
        parent.addView(row);
    }

    /** Ô chọn từ danh sách (view_select) mở hộp thoại khi chạm. */
    public static class Select {
        private final TextView label;
        private String[] options = new String[0];
        private int index = 0;
        private Runnable onChange;

        public Select(View root, String title) {
            label = root.findViewById(R.id.select_label);
            root.setOnClickListener(v -> new AlertDialog.Builder(root.getContext()).setTitle(title)
                    .setItems(options, (d, which) -> { index = which; refresh(); if (onChange != null) onChange.run(); }).show());
        }

        public void setOptions(String[] opts, int selected) { options = opts; index = Math.max(0, Math.min(selected, opts.length - 1)); refresh(); }
        public int getIndex() { return index; }
        public void setOnChange(Runnable r) { onChange = r; }
        private void refresh() { label.setText(options.length == 0 ? "—" : options[index]); }
    }

    // ---------- hộp thoại ----------
    public static void toast(Context c, String s) { Toast.makeText(c, s, Toast.LENGTH_SHORT).show(); }
    public static void toast(Context c, int res) { Toast.makeText(c, res, Toast.LENGTH_SHORT).show(); }

    public static void confirm(Context c, String title, String message, Runnable yes) {
        new AlertDialog.Builder(c).setTitle(title).setMessage(message).setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_confirm, (d, w) -> yes.run()).show();
    }

    // ---------- thời gian ----------
    private static final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    public static String nowIso() { return ISO.format(new Date()); }

    public static String isoDaysAgo(int daysAgo, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        return ISO.format(cal.getTime());
    }

    public static String fullTime(String iso) {
        try { return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(ISO.parse(iso)); } catch (Exception e) { return iso == null ? "" : iso; }
    }

    public static String relativeTime(Context c, String iso) {
        try {
            Date d = ISO.parse(iso);
            Calendar a = Calendar.getInstance(), b = Calendar.getInstance();
            b.setTime(d);
            String hm = new SimpleDateFormat("HH:mm", Locale.US).format(d);
            if (a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)) return c.getString(R.string.time_today, hm);
            a.add(Calendar.DAY_OF_YEAR, -1);
            if (a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)) return c.getString(R.string.time_yesterday, hm);
            return new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(d);
        } catch (Exception e) { return iso == null ? "" : iso; }
    }
}
