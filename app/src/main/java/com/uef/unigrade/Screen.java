package com.uef.unigrade;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/** Một màn hình nội dung (inflate từ res/layout) nằm trong ShellActivity. */
public abstract class Screen {
    protected final ShellActivity act;
    protected final DatabaseHelper db;
    protected final Context c;
    protected final LayoutInflater inf;

    protected Screen(ShellActivity act) {
        this.act = act;
        this.db = act.db;
        this.c = act;
        this.inf = act.getLayoutInflater();
    }

    public abstract View build();

    protected void text(View root, int id, CharSequence s) { ((TextView) root.findViewById(id)).setText(s); }

    protected String str(int res, Object... args) { return c.getString(res, args); }
}
