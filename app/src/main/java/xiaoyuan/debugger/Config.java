package xiaoyuan.debugger;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import de.robv.android.xposed.XSharedPreferences;

/**
 * 配置模型 + 持久化。
 * - 控制面板在小猿进程内（InAppOverlay 叠加到 Activity decorView）读写小猿自身的
 *   SharedPreferences；
 * - 模块进程 MainActivity 仅说明页，不写配置。
 * 写入用 commit()（同步落盘）。
 */
public final class Config {

    public static final String MODULE_PACKAGE = "xiaoyuan.debugger";
    public static final String PREFS_NAME = "xy_config";

    public static final String KEY_ALL_CORRECT = "all_correct";
    public static final String KEY_AUTO_ANSWER = "auto_answer";
    public static final String KEY_AUTO_DELAY = "auto_delay";
    public static final String KEY_OVERLAY_MARGIN_TOP = "overlay_margin_top";
    public static final String KEY_OVERLAY_MARGIN_RIGHT = "overlay_margin_right";
    public static final String KEY_OVERLAY_EXPANDED = "overlay_expanded";

    // 默认：全对模式沿用模块既有行为（开）；自动答题恢复旧版默认（开），面板可随时关；抓取类默认不启用
    public static final boolean DEF_ALL_CORRECT = true;
    public static final boolean DEF_AUTO_ANSWER = true;
    public static final int DEF_AUTO_DELAY = 500;
    public static final int MIN_AUTO_DELAY = 20;
    public static final int MAX_AUTO_DELAY = 800;
    public static final int DEF_OVERLAY_MARGIN_TOP = 60;
    public static final int DEF_OVERLAY_MARGIN_RIGHT = 24;
    public static final boolean DEF_OVERLAY_EXPANDED = true;

    public boolean allCorrect = DEF_ALL_CORRECT;
    public boolean autoAnswer = DEF_AUTO_ANSWER;
    public int autoDelay = DEF_AUTO_DELAY;
    // 悬浮窗 UI 状态（非答题配置，不参与 provider / sameAs 比较）
    public int overlayMarginTop = DEF_OVERLAY_MARGIN_TOP;
    public int overlayMarginRight = DEF_OVERLAY_MARGIN_RIGHT;
    public boolean overlayExpanded = DEF_OVERLAY_EXPANDED;

    public static Config from(SharedPreferences sp) {
        Config c = new Config();
        if (sp == null) return c;
        c.allCorrect = sp.getBoolean(KEY_ALL_CORRECT, DEF_ALL_CORRECT);
        c.autoAnswer = sp.getBoolean(KEY_AUTO_ANSWER, DEF_AUTO_ANSWER);
        c.autoDelay = clampDelay(sp.getInt(KEY_AUTO_DELAY, DEF_AUTO_DELAY));
        c.overlayMarginTop = sp.getInt(KEY_OVERLAY_MARGIN_TOP, DEF_OVERLAY_MARGIN_TOP);
        c.overlayMarginRight = sp.getInt(KEY_OVERLAY_MARGIN_RIGHT, DEF_OVERLAY_MARGIN_RIGHT);
        c.overlayExpanded = sp.getBoolean(KEY_OVERLAY_EXPANDED, DEF_OVERLAY_EXPANDED);
        return c;
    }

    /** 模块进程内读取自身 SharedPreferences */
    public static Config fromContext(Context ctx) {
        return from(ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE));
    }

    /** 目标进程内经 XSharedPreferences 读取模块配置（仅作初始值；实时值走 provider） */
    public static Config fromXPrefs() {
        try {
            XSharedPreferences sp = new XSharedPreferences(MODULE_PACKAGE, PREFS_NAME);
            sp.reload();
            return from(sp);
        } catch (Throwable t) {
            return new Config();
        }
    }

    /** 模块进程内持久化全量配置 */
    public static void save(Context ctx, Config c) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ALL_CORRECT, c.allCorrect)
                .putBoolean(KEY_AUTO_ANSWER, c.autoAnswer)
                .putInt(KEY_AUTO_DELAY, c.autoDelay)
                .putInt(KEY_OVERLAY_MARGIN_TOP, c.overlayMarginTop)
                .putInt(KEY_OVERLAY_MARGIN_RIGHT, c.overlayMarginRight)
                .putBoolean(KEY_OVERLAY_EXPANDED, c.overlayExpanded)
                .commit();
    }

    // ---- provider 协议：JSON ----
    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("type", "state");
            o.put(KEY_ALL_CORRECT, allCorrect);
            o.put(KEY_AUTO_ANSWER, autoAnswer);
            o.put(KEY_AUTO_DELAY, autoDelay);
        } catch (Throwable ignore) {}
        return o;
    }

    public static Config fromJson(JSONObject o) {
        Config c = new Config();
        c.allCorrect = o.optBoolean(KEY_ALL_CORRECT, DEF_ALL_CORRECT);
        c.autoAnswer = o.optBoolean(KEY_AUTO_ANSWER, DEF_AUTO_ANSWER);
        c.autoDelay = clampDelay(o.optInt(KEY_AUTO_DELAY, DEF_AUTO_DELAY));
        return c;
    }

    public static int clampDelay(int ms) {
        if (ms < MIN_AUTO_DELAY) return MIN_AUTO_DELAY;
        if (ms > MAX_AUTO_DELAY) return MAX_AUTO_DELAY;
        return ms;
    }

    public boolean sameAs(Config o) {
        return o != null
                && allCorrect == o.allCorrect
                && autoAnswer == o.autoAnswer
                && autoDelay == o.autoDelay;
    }
}
