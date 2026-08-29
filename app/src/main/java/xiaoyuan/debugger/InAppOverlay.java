package xiaoyuan.debugger;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/**
 * 小猿进程内的控制面板——纯代码构建，叠加到当前 Activity 的 decorView 上，
 * 无需悬浮窗权限、不依赖模块布局资源（小猿进程无法 inflate 模块 APK 布局）。
 * 配置直接读写小猿自身 SharedPreferences（同进程）。
 */
public class InAppOverlay {
    private static final String TAG = "OralPkJudge";
    private static final int DELAY_STEP = 1;

    private final Context app;
    private View root;
    private View body;
    private Switch swAllCorrect;
    private CheckBox cbAuto;
    private SeekBar seekDelay;
    private TextView tvDelay;
    private Runnable onConfigChanged = null;
    private Activity attachedActivity = null;

    // 悬浮窗位置/展开状态：跨 Activity 切换与进程重启保留（持久化到目标进程 prefs）
    private static final int DEF_MARGIN_TOP = 60;
    private static final int DEF_MARGIN_RIGHT = 24;
    private int marginTop = DEF_MARGIN_TOP;
    private int marginRight = DEF_MARGIN_RIGHT;
    private boolean expanded = true;

    public InAppOverlay(Context ctx) {
        this.app = ctx.getApplicationContext();
        loadState();
    }

    private void loadState() {
        try {
            Config c = Config.fromContext(app);
            marginTop = c.overlayMarginTop;
            marginRight = c.overlayMarginRight;
            expanded = c.overlayExpanded;
        } catch (Throwable ignore) {}
    }

    private void persistState() {
        try {
            Config c = Config.fromContext(app);
            c.overlayMarginTop = marginTop;
            c.overlayMarginRight = marginRight;
            c.overlayExpanded = expanded;
            Config.save(app, c);
        } catch (Throwable ignore) {}
    }

    public void setOnConfigChanged(Runnable r) {
        this.onConfigChanged = r;
    }

    /** 当前面板所依附的 Activity（hook 在 onDestroy 用它判断是否清理） */
    public Activity currentActivity() {
        return attachedActivity;
    }

    private void notifyChanged() {
        if (onConfigChanged != null) {
            try { onConfigChanged.run(); } catch (Throwable ignore) {}
        }
    }

    /** 叠加到当前 Activity；若已叠在同一 Activity 则忽略 */
    public void ensureAttached(Activity activity) {
        if (activity == null) {
            Log.e(TAG, "ensureAttached: null activity");
            return;
        }
        if (attachedActivity == activity) return;
        try {
            ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
            if (decor == null) {
                Log.i(TAG, "[init] decor null, retry");
                new Handler(Looper.getMainLooper()).postDelayed(() -> ensureAttached(activity), 100);
                return;
            }
            if (attachedActivity != null) detach();

            root = buildPanel();

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.END);
            lp.topMargin = marginTop;
            lp.rightMargin = marginRight;
            root.setLayoutParams(lp);

            decor.addView(root, lp);
            attachedActivity = activity;

            setupUi();
            setupDrag();
            refreshFromPrefs();
            if (body != null) body.setVisibility(expanded ? View.VISIBLE : View.GONE);
            Log.i(TAG, "[init] in-app overlay attached to " + activity.getClass().getName());
        } catch (Throwable t) {
            Log.e(TAG, "in-app overlay attach err: " + t);
        }
    }

    /** 纯代码构建面板（不依赖模块布局资源） */
    private View buildPanel() {
        LinearLayout panel = new LinearLayout(app);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(8), dp(2), dp(8), dp(10));
        panel.setMinimumWidth(dp(220));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6161A29);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), 0x33FFFFFF);
        panel.setBackground(bg);
        panel.setElevation(dp(10));

        // 头部：拖拽 + 折叠
        LinearLayout header = new LinearLayout(app);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(6), dp(4), dp(6));
        header.setTag("overlay_header");

        TextView title = new TextView(app);
        title.setText("XY Debugger");
        title.setTextColor(Color.WHITE);
        title.setTextSize(14);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(dp(6), dp(4), dp(6), dp(4));
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView minBtn = new TextView(app);
        minBtn.setText("—");
        minBtn.setTextColor(Color.WHITE);
        minBtn.setTextSize(18);
        minBtn.setGravity(Gravity.CENTER);
        minBtn.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
        minBtn.setTag("overlay_min");

        header.addView(title);
        header.addView(minBtn);
        header.setOnTouchListener(dragListener());

        // 主体
        LinearLayout bodyLay = new LinearLayout(app);
        bodyLay.setOrientation(LinearLayout.VERTICAL);
        bodyLay.setTag("overlay_body");

        swAllCorrect = new Switch(app);
        bodyLay.addView(row(app, "全对模式", swAllCorrect));

        cbAuto = new CheckBox(app);
        bodyLay.addView(row(app, "自动答题", cbAuto));

        // 延迟行
        LinearLayout delayRow = new LinearLayout(app);
        delayRow.setOrientation(LinearLayout.HORIZONTAL);
        delayRow.setGravity(Gravity.CENTER_VERTICAL);
        delayRow.setPadding(dp(6), dp(4), dp(6), dp(4));

        TextView delayLabel = new TextView(app);
        delayLabel.setText("延迟");
        delayLabel.setTextColor(Color.WHITE);
        delayLabel.setTextSize(14);

        tvDelay = new TextView(app);
        tvDelay.setTextColor(0xFF9CD0FF);
        tvDelay.setTextSize(15);
        tvDelay.setText("500ms");
        tvDelay.setPadding(dp(8), 0, dp(4), 0);

        seekDelay = new SeekBar(app);
        seekDelay.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        delayRow.addView(delayLabel);
        delayRow.addView(tvDelay);
        delayRow.addView(seekDelay);
        bodyLay.addView(delayRow);

        // 预设快捷按钮
        LinearLayout presetRow = new LinearLayout(app);
        presetRow.setOrientation(LinearLayout.HORIZONTAL);
        presetRow.setGravity(Gravity.CENTER_VERTICAL);
        presetRow.setPadding(dp(6), dp(4), dp(6), dp(2));
        int[] presets = {20, 100, 200, 400, 800};
        for (final int ms : presets) {
            TextView chip = new TextView(app);
            chip.setText(ms >= 1000 ? (ms / 1000) + "s" : (ms / 1000.0) + "s");
            chip.setTextColor(Color.WHITE);
            chip.setTextSize(12);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(10), dp(8), dp(10), dp(8));
            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setColor(0x26FFFFFF);
            chipBg.setCornerRadius(dp(9));
            chipBg.setStroke(dp(1), 0x55FFFFFF);
            chip.setBackground(chipBg);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
            clp.rightMargin = dp(5);
            chip.setLayoutParams(clp);
            chip.setOnClickListener(v -> {
                Config c = Config.fromContext(app);
                c.autoDelay = ms;
                Config.save(app, c);
                refreshFromPrefs();
                notifyChanged();
            });
            presetRow.addView(chip);
        }
        bodyLay.addView(presetRow);

        panel.addView(header);
        panel.addView(bodyLay);

        root = panel;
        body = bodyLay;
        return panel;
    }

    private View row(Context ctx, String label, View control) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(6), dp(3), dp(6), dp(3));

        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(14);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        row.addView(tv);
        row.addView(control);
        return row;
    }

    private void setupUi() {
        swAllCorrect.setOnCheckedChangeListener((v, on) -> {
            Config c = Config.fromContext(app);
            c.allCorrect = on;
            Config.save(app, c);
            notifyChanged();
        });
        cbAuto.setOnCheckedChangeListener((v, on) -> {
            Config c = Config.fromContext(app);
            c.autoAnswer = on;
            Config.save(app, c);
            notifyChanged();
        });
        seekDelay.setMax((Config.MAX_AUTO_DELAY - Config.MIN_AUTO_DELAY) / DELAY_STEP);
        seekDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                int ms = Config.MIN_AUTO_DELAY + progress * DELAY_STEP;
                if (tvDelay != null) tvDelay.setText(formatDelay(ms));
                if (fromUser) {
                    Config c = Config.fromContext(app);
                    c.autoDelay = ms;
                    Config.save(app, c);
                    notifyChanged();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    public void refreshFromPrefs() {
        if (root == null) return;
        try {
            Config c = Config.fromContext(app);
            if (swAllCorrect != null) swAllCorrect.setChecked(c.allCorrect);
            if (cbAuto != null) cbAuto.setChecked(c.autoAnswer);
            if (tvDelay != null) tvDelay.setText(formatDelay(c.autoDelay));
            if (seekDelay != null) seekDelay.setProgress((c.autoDelay - Config.MIN_AUTO_DELAY) / DELAY_STEP);
        } catch (Throwable ignore) {}
    }

    private View.OnTouchListener dragListener() {
        return new View.OnTouchListener() {
            private int downRawX, downRawY;
            private int baseTop, baseRight;
            private long downTime;
            private final int slop = dp(10);

            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downRawX = (int) e.getRawX();
                        downRawY = (int) e.getRawY();
                        FrameLayout.LayoutParams lp0 = (FrameLayout.LayoutParams) root.getLayoutParams();
                        baseTop = lp0.topMargin;
                        baseRight = lp0.rightMargin;
                        downTime = System.currentTimeMillis();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        // 基准(baseTop/baseRight)在 DOWN 固定，每次只按"基准 + 当前手指增量"计算，
                        // 不能把 marginTop 累加进去——否则每个 MOVE 都叠加一次完整位移，窗口会跑得比手指快
                        int newTop = Math.max(0, baseTop + (int) e.getRawY() - downRawY);
                        int newRight = Math.max(0, baseRight - (int) e.getRawX() + downRawX);
                        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) root.getLayoutParams();
                        lp.topMargin = newTop;
                        lp.rightMargin = newRight;
                        root.setLayoutParams(lp);
                        return true;
                    case MotionEvent.ACTION_UP:
                        boolean moved = Math.abs((int) e.getRawX() - downRawX) > slop
                                || Math.abs((int) e.getRawY() - downRawY) > slop;
                        long dur = System.currentTimeMillis() - downTime;
                        if (!moved && dur < 300 && body != null) {
                            setBodyExpanded(!expanded);
                        } else {
                            FrameLayout.LayoutParams lpU = (FrameLayout.LayoutParams) root.getLayoutParams();
                            marginTop = lpU.topMargin;
                            marginRight = lpU.rightMargin;
                            persistState();
                        }
                        return true;
                }
                return false;
            }
        };
    }

    private void setupDrag() {
        final View header = ((ViewGroup) root).getChildAt(0);
        final View minBtn = header.findViewWithTag("overlay_min");
        minBtn.setOnClickListener(v -> setBodyExpanded(!expanded));
    }

    /** 折叠/展开主体，并持久化展开状态 */
    private void setBodyExpanded(boolean exp) {
        expanded = exp;
        if (body != null) body.setVisibility(exp ? View.VISIBLE : View.GONE);
        persistState();
    }

    /** 从 decorView 移除面板 */
    public void detach() {
        try {
            if (root != null) {
                ViewGroup parent = (ViewGroup) root.getParent();
                if (parent != null) parent.removeView(root);
            }
        } catch (Throwable ignore) {}
        root = null;
        attachedActivity = null;
    }

    private int dp(float v) {
        return (int) (app.getResources().getDisplayMetrics().density * v + 0.5f);
    }

    private static String formatDelay(int ms) {
        if (ms >= 1000) {
            if (ms % 1000 == 0) return (ms / 1000) + "s";
            return String.format(java.util.Locale.US, "%.1fs", ms / 1000.0);
        }
        return ms + "ms";
    }
}
