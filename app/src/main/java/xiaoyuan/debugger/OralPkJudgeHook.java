package xiaoyuan.debugger;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * LSPosed 模块：小猿AI 口算PK 判题「全对 + 自动答题」。
 * 目标包: com.fenbi.android.leo  （H5 在该进程的 WebView 内运行）
 *
 * 实现: 向判题 JS 注入控制脚本（覆写 Array.prototype.includes 实现全对；
 * 定时在答题 canvas 上模拟 MouseEvent 笔画触发原生手写识别实现自动答题），
 * 通过 shouldInterceptRequest 拼接到判题 JS 前端 / evaluateJavascript 追加注入。
 *
 * 实时配置：模块应用/悬浮窗写入 SharedPreferences(xy_config)，本类每 800ms 用
 * XSharedPreferences reload 轮询，变化时经 evaluateJavascript 实时推送：
 *   window.__setAllCorrect(bool)  —— 全对模式开关
 *   window.__setAutoAnswer(bool)  —— 自动答题开关
 *   window.__setAutoDelay(ms)     —— 自动答题间隔
 */
public class OralPkJudgeHook implements IXposedHookLoadPackage {

    private static final String TAG = "OralPkJudge";
    private static final String TARGET = "com.fenbi.android.leo";

    // 判断 URL 是否与口算PK 相关 (leo-game-pk 或 leo-web-oral-pk)
    private static final String[] PK_KEYWORDS = {"leo-game-pk", "leo-web-oral-pk", "math/pk", "pk/"};

    private static final int MAX_BODY = 8000;   // body 截断长度

    private static volatile Config sConfig = new Config();
    private static Handler sMainHandler = null;
    private static InAppOverlay sInAppOverlay = null;

    // 存活的答题 WebView，用于实时推送配置变化（按对象 identity 去重，防同一实例反复注入）
    private static final Set<WeakReference<Object>> LIVE_WEBVIEWS =
            Collections.synchronizedSet(new HashSet<>());
    // X5 与系统两套 ValueCallback：注入/推送时按 WebView 实际类型选对的接口，
    // 否则系统 WebView 上 callMethod 反射 NoSuchMethod，注入/推送静默失败
    private static Class<?> sValueCallback = null;       // com.tencent.smtt.sdk.ValueCallback
    private static Class<?> sSysValueCallback = null;    // android.webkit.ValueCallback

    private static ClassLoader sClassLoader;

    // 注入的自动答题控制脚本:
    // 1) window.__setAllCorrect(bool)：覆写/还原 Array.prototype.includes ——
    //    判题用 answers.includes(识别结果) 判定, 对"答案数组"特征的数组 includes 恒真 → 判对
    // 2) window.__setAutoAnswer(bool)/__setAutoDelay(ms)：定时在答题 canvas 上模拟
    //    MouseEvent 画随机轨迹触发原生手写识别:
    //    mousedown 在 canvas 上、mousemove/mouseup 在 window 上(用 buttons 判断)
    //    随机化防检测: 随机起点/方向/长度, 贝塞尔弯曲+抖动, move 渐进模拟手速,
    //    间隔 = autoDelay*(0.85~1.15) 抖动
    // 注入后由 hook 立刻调用三个 setter 应用当前配置。
    private static final String AUTO_ANSWER_JS =
        "(function(){" +
        "if(window.__oralControlsInstalled)return;window.__oralControlsInstalled=1;" +
        "try{document.title='[XYOK]'+document.title;}catch(e){}" +
        "var O=Array.prototype.includes;" +
        "var allOn=false;" +
        "window.__setAllCorrect=function(on){" +
        "  on=!!on;" +
        "  if(on===allOn)return;allOn=on;" +
        "  if(on){" +
        "    Array.prototype.includes=function(v){" +
        "      // answer arrays may contain fractions like \\frac{1}{2}, letters, etc." +
        "      if(this&&this.length>0&&this.length<=8&&this.every(function(x){" +
        "        return typeof x==='string'||typeof x==='number';" +
        "      }))return true;" +
        "      return O.apply(this,arguments);" +
        "    };" +
        "  }else{Array.prototype.includes=O;}" +
        "};" +
        "function pickCanvas(){" +
        "  var cs=document.querySelectorAll('canvas');" +
        "  for(var i=cs.length-1;i>=0;i--){" +
        "    var rr=cs[i].getBoundingClientRect();" +
        "    if(rr.width>200)return {c:cs[i],r:rr};" +
        "  }" +
        "  return null;" +
        "}" +
        "function buildPoints(r){" +
        "  var pad=0.08;" +
        "  var x0=r.left+r.width*(pad+Math.random()*(1-2*pad));" +
        "  var y0=r.top+r.height*(pad+Math.random()*(1-2*pad));" +
        "  var ang=Math.random()*2*Math.PI;" +
        "  var len=r.width*(0.3+Math.random()*0.35);" +
        "  var x1=x0+Math.cos(ang)*len;" +
        "  var y1=y0+Math.sin(ang)*len*0.8;" +
        "  x1=Math.max(r.left+r.width*0.05,Math.min(r.left+r.width*0.95,x1));" +
        "  y1=Math.max(r.top+r.height*0.05,Math.min(r.top+r.height*0.95,y1));" +
        "  var cx=(x0+x1)/2+(Math.random()-0.5)*r.width*0.25;" +
        "  var cy=(y0+y1)/2+(Math.random()-0.5)*r.height*0.25;" +
        "  var steps=18+Math.floor(Math.random()*26);" +
        "  var pts=[];" +
        "  for(var i=0;i<=steps;i++){" +
        "    var t=i/steps,mt=1-t;" +
        "    var nx=mt*mt*x0+2*mt*t*cx+t*t*x1+(Math.random()-0.5)*5;" +
        "    var ny=mt*mt*y0+2*mt*t*cy+t*t*y1+(Math.random()-0.5)*5;" +
        "    pts.push([nx,ny]);" +
        "  }" +
        "  return pts;" +
        "}" +
        "var autoOn=false,autoDelay=1000,timer=null;" +
        "window.__setAutoDelay=function(ms){" +
        "  var n=parseInt(ms,10);" +
        "  if(isNaN(n))n=500;" +
        "  autoDelay=Math.max(20,Math.min(800,n));" +
        "};" +
        "function schedule(){" +
        "  if(!autoOn)return;" +
        "  // stroke 80pct of delay, rest for recognize/transition" +
        "  timer=setTimeout(stroke,Math.max(1,autoDelay*0.2)*(0.8+Math.random()*0.4));" +
        "}" +
        "function stroke(){" +
        "  try{" +
        "    var pick=pickCanvas();" +
        "    if(!pick){schedule();return;}" +
        "    var c=pick.c,r=pick.r;" +
        "    var pts=buildPoints(r);" +
        "    // total stroke time = 80% of delay, split evenly with jitter" +
        "    var stepMs=Math.max(1,autoDelay*0.8/pts.length);" +
        "    c.dispatchEvent(new MouseEvent('mousedown',{clientX:pts[0][0],clientY:pts[0][1],button:0,buttons:1,bubbles:true,cancelable:true}));" +
        "    var k=1;" +
        "    (function step(){" +
        "      if(k>=pts.length){" +
        "        window.dispatchEvent(new MouseEvent('mouseup',{clientX:pts[pts.length-1][0],clientY:pts[pts.length-1][1],button:0,buttons:0,bubbles:true,cancelable:true}));" +
        "        schedule();" +
        "        return;" +
        "      }" +
        "      var p=pts[k++];" +
        "      window.dispatchEvent(new MouseEvent('mousemove',{clientX:p[0],clientY:p[1],button:0,buttons:1,bubbles:true,cancelable:true}));" +
        "      setTimeout(step,stepMs*(0.8+Math.random()*0.4));" +
        "    })();" +
        "  }catch(e){schedule();}" +
        "}" +
        "window.__setAutoAnswer=function(on){" +
        "  on=!!on;" +
        "  if(on===autoOn)return;autoOn=on;" +
        "  if(timer){clearTimeout(timer);timer=null;}" +
        "  if(on){timer=setTimeout(stroke,300+Math.random()*500);}" +
        "};" +
        "})();";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpp) throws Throwable {
        if (!lpp.packageName.equals(TARGET)) return;

        // 必须先保存目标进程的 classLoader，后续 findClass 都依赖它
        sClassLoader = lpp.classLoader;

        // 无条件初始化标记（不受日志开关门控）：用于确认模块是否真的加载、是否多进程
        String procName = "";
        try {
            Class<?> at = XposedHelpers.findClass("android.app.ActivityThread", sClassLoader);
            Object thread = XposedHelpers.callStaticMethod(at, "currentActivityThread");
            if (thread != null) procName = String.valueOf(XposedHelpers.callMethod(thread, "getProcessName"));
        } catch (Throwable ignore) {}
        XposedBridge.log("[OralPkJudge] [init] hook loaded pid=" + Process.myPid() + " proc=" + procName);

        XposedBridge.log("[OralPkJudge] [init] enter handleLoadPackage");

        try {
            // 配置存目标进程自己的 prefs（同进程读写，无需跨进程 IPC）
            Context app = getAppContext();
            if (app != null) {
                sConfig = Config.fromContext(app);
                // 清理历史残留：重构前 prefs 可能存了 auto_answer=false，
                // 覆盖为默认开启（全对+自动答题），用户可经面板再关
                if (!sConfig.allCorrect || !sConfig.autoAnswer) {
                    Config force = Config.fromContext(app);
                    force.allCorrect = true;
                    force.autoAnswer = true;
                    Config.save(app, force);
                    sConfig = Config.fromContext(app);
                    XposedBridge.log("[OralPkJudge] [init] config force-enabled allCorrect+autoAnswer");
                }
            }
            log("====== OralPkJudge hooked into " + lpp.packageName + " ======");
            log("config: allCorrect=" + sConfig.allCorrect
                    + " autoAnswer=" + sConfig.autoAnswer
                    + " autoDelay=" + sConfig.autoDelay);
            hookShouldInterceptRequest();
            hookAutoAnswer();
            hookInAppOverlay();
            log("All hooks registered.");
            XposedBridge.log("[OralPkJudge] [init] hooks ready pid=" + Process.myPid());
        } catch (Throwable t) {
            log("init error: " + t);
        }
    }

    private static Context getAppContext() {
        try {
            Class<?> at = XposedHelpers.findClass("android.app.ActivityThread", sClassLoader);
            Object app = XposedHelpers.callStaticMethod(at, "currentApplication");
            if (app instanceof Context) return (Context) app;
        } catch (Throwable ignore) {}
        return null;
    }

    /**
     * 控制面板作为 View 叠加到小猿当前 Activity 的 decorView 上（无需悬浮窗权限）。
     * hook Instrumentation.callActivityOnResume：每次 Activity resume 必经，
     * 不依赖 Activity 是否覆写 onResume；Activity 销毁后 decorView 失效，
     * 下次 resume 重新叠加。context 在回调里懒获取（进程早期 Application 可能尚未创建）。
     */
    private void hookInAppOverlay() {
        XposedBridge.log("[OralPkJudge] [init] hookInAppOverlay enter");
        try {
            // 用 Instrumentation.callActivityOnResume：所有 Activity resume 必经此入口，
            // 比 hook Activity.onResume 稳（不依赖 Activity 是否覆写 onResume）。
            Class<?> inst = XposedHelpers.findClass("android.app.Instrumentation", sClassLoader);
            XposedHelpers.findAndHookMethod(inst, "callActivityOnResume",
                    android.app.Activity.class, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    try {
                        Object activityObj = p.args[0];
                        if (!(activityObj instanceof android.app.Activity)) return;
                        final android.app.Activity activity = (android.app.Activity) activityObj;
                        XposedBridge.log("[OralPkJudge] [init] callActivityOnResume fired activity="
                                + activity.getClass().getName() + " pid=" + Process.myPid());
                        final Context app = getAppContext();
                        if (app == null) {
                            XposedBridge.log("[OralPkJudge] [init] app null on resume, skip");
                            return;
                        }
                        if (sInAppOverlay == null) {
                            // 纯代码构建面板，不依赖模块布局资源
                            sInAppOverlay = new InAppOverlay(app);
                            // 面板改动配置后实时把新值推给存活 WebView
                            sInAppOverlay.setOnConfigChanged(OralPkJudgeHook::applyConfigToWebViews);
                        }
                        // callActivityOnResume 的 afterHook 已运行在主线程，直接同步叠加
                        try {
                            sInAppOverlay.ensureAttached(activity);
                        } catch (Throwable t) {
                            XposedBridge.log("[OralPkJudge] overlay attach err: " + t);
                        }
                    } catch (Throwable t) {
                        XposedBridge.log("[OralPkJudge] onResume hook err: " + t);
                    }
                }
            });
            // Activity 销毁时把面板从旧 decorView 摘掉（避免残留/下次 attach 混乱）
            XposedHelpers.findAndHookMethod(inst, "callActivityOnDestroy",
                    android.app.Activity.class, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    try {
                        Object activityObj = p.args[0];
                        if (sInAppOverlay != null && activityObj == sInAppOverlay.currentActivity()) {
                            sInAppOverlay.detach();
                        }
                    } catch (Throwable ignore) {}
                }
            });
            XposedBridge.log("[OralPkJudge] [init] in-app overlay hook OK");
        } catch (Throwable t) {
            XposedBridge.log("[OralPkJudge] hookInAppOverlay err: " + t);
        }
    }

    // ---------------------------------------------------------------
    // 实时控制: 控制悬浮窗在小猿进程内直接读写 prefs(同进程),
    // 无需跨进程通道; 配置经 WebView 注入的 setter 实时应用
    // ---------------------------------------------------------------
    private static void applyConfigToWebViews() {
        XposedBridge.log("[OralPkJudge] [init] config changed -> push to "
                + LIVE_WEBVIEWS.size() + " webviews");
        postToMain(() -> applyToWebViews(currentConfig()));
    }

    /** 读目标进程当前配置（悬浮窗改动直接写目标进程 prefs，注入/推送都用最新值） */
    private static Config currentConfig() {
        try {
            Context app = getAppContext();
            if (app != null) return Config.fromContext(app);
        } catch (Throwable ignore) {}
        return sConfig;
    }

    private static void postToMain(Runnable r) {
        try {
            if (sMainHandler == null) sMainHandler = new Handler(Looper.getMainLooper());
            sMainHandler.post(r);
        } catch (Throwable t) {
            r.run();
        }
    }

    private static void applyToWebViews(Config c) {
        XposedBridge.log("[OralPkJudge] [init] applyToWebViews live=" + LIVE_WEBVIEWS.size()
                + " allCorrect=" + c.allCorrect + " auto=" + c.autoAnswer + " delay=" + c.autoDelay);
        synchronized (LIVE_WEBVIEWS) {
            Iterator<WeakReference<Object>> it = LIVE_WEBVIEWS.iterator();
            while (it.hasNext()) {
                Object wv = it.next().get();
                if (wv == null) { it.remove(); continue; }
                try {
                    // 每次 resume 都对存活 WebView 做完整注入（幂等）：不仅设 setter，
                    // 确保控制脚本本身也已在页面上。注入时机不对时这里重试。
                    // 容器/无效实例返回 false → 移除，避免反复遍历刷屏
                    if (!injectViaLoadUrl(wv, c)) it.remove();
                } catch (Throwable t) {
                    it.remove();
                }
            }
        }
    }

    /** 构建完整注入 JS（控制脚本 + 应用配置），末尾返回标记便于 [eval] 确认执行 */
    private static String buildInjectJs(Config c) {
        return AUTO_ANSWER_JS
                + "window.__setAllCorrect(" + c.allCorrect + ");"
                + "window.__setAutoAnswer(" + c.autoAnswer + ");"
                + "window.__setAutoDelay(" + c.autoDelay + ");"
                + "console.log('[XY] applied ac=" + c.allCorrect + " auto=" + c.autoAnswer + " d=" + c.autoDelay + "');"
                + "'[XY:injected]';";
    }

    /**
     * 拼到判题 JS 文件开头的控制脚本（最大安全）：
     * - Array.isArray 保护：覆写 includes 时不破坏非数组对象的调用
     * - 不在模块加载阶段启动笔画（模块未就绪时 canvas 不存在，定时器也会干扰初始化）
     * - 全对(覆写 includes)立即生效；自动答题用延迟 setInterval 轮询 canvas 出现后自持运行
     * - 所有逻辑包在 try/catch，任何异常不中断模块自身执行
     */
    private static String buildJsFilePrefix(Config c) {
        // 绝对安全：includes 覆写立即生效(全对)；笔画延迟 3s 后才启动 setInterval，
        // 不干扰模块初始化。任何异常被 try/catch 吞掉，不中断模块自身。
        return "(function(){try{" +
        "if(window.__oralControlsInstalled)return;window.__oralControlsInstalled=1;" +
        "var _O=Array.prototype.includes;" +
        "Array.prototype.includes=function(v){" +
        "  if(Array.isArray(this)&&this.length>0&&this.length<=8&&this.every(function(x){" +
        "    return typeof x==='string'||typeof x==='number';" +
        "  }))return true;" +
        "  return _O.apply(this,arguments);" +
        "};" +
        "var _autoOn=" + c.autoAnswer + ",_delay=" + c.autoDelay + ",_last=0;" +
        "window.__setAllCorrect=function(){};" +
        "window.__setAutoAnswer=function(o){_autoOn=!!o;};" +
        "window.__setAutoDelay=function(ms){var n=parseInt(ms,10);if(!isNaN(n)&&n>0)_delay=Math.max(20,Math.min(800,n));};" +
        // 随机化笔画：随机起点/方向/长度，贝塞尔弯曲+抖动，渐进移动模拟手速
        "function _pts(r){var p=0.08,x0=r.left+r.width*(p+Math.random()*(1-2*p)),y0=r.top+r.height*(p+Math.random()*(1-2*p)),a=Math.random()*6.283,l=r.width*(0.3+Math.random()*0.35);" +
        "var x1=x0+Math.cos(a)*l,y1=y0+Math.sin(a)*l*0.8;" +
        "x1=Math.max(r.left+r.width*0.05,Math.min(r.left+r.width*0.95,x1));y1=Math.max(r.top+r.height*0.05,Math.min(r.top+r.height*0.95,y1));" +
        "var cx=(x0+x1)/2+(Math.random()-0.5)*r.width*0.25,cy=(y0+y1)/2+(Math.random()-0.5)*r.height*0.25,n=16+Math.floor(Math.random()*24),q=[];" +
        "for(var i=0;i<=n;i++){var t=i/n,m=1-t;q.push([m*m*x0+2*m*t*cx+t*t*x1+(Math.random()-0.5)*5,m*m*y0+2*m*t*cy+t*t*y1+(Math.random()-0.5)*5]);}return q;}" +
        "function _stroke(){try{var cs=document.querySelectorAll('canvas');for(var i=cs.length-1;i>=0;i--){var r=cs[i].getBoundingClientRect();if(r.width>200){var c=cs[i],q=_pts(r);" +
        "c.dispatchEvent(new MouseEvent('mousedown',{clientX:q[0][0],clientY:q[0][1],button:0,buttons:1,bubbles:true,cancelable:true}));" +
        "var k=1;(function st(){if(k>=q.length){window.dispatchEvent(new MouseEvent('mouseup',{clientX:q[q.length-1][0],clientY:q[q.length-1][1],button:0,buttons:0,bubbles:true,cancelable:true}));return;}" +
        "var pt=q[k++];window.dispatchEvent(new MouseEvent('mousemove',{clientX:pt[0],clientY:pt[1],button:0,buttons:1,bubbles:true,cancelable:true}));" +
        "setTimeout(st,5+Math.random()*22);})();return;}}}catch(e){}}" +
        // 笔画间隔 = _delay 附近随机抖动(0.85~1.15)，且 15% 概率跳过本次(防固定频率)
        "setTimeout(function(){setInterval(function(){if(_autoOn&&Date.now()-_last>=_delay*(0.85+Math.random()*0.3)&&Math.random()>0.15){_last=Date.now();_stroke();}},Math.max(60,_delay*0.5));},3000);" +
        "}catch(e){}})();";
    }

    /** 只对真正的 WebView 内核实例做注入（X5 WebView / 系统 WebView），跳过 yuanfudao 容器包装 */
    private static boolean isWebViewKernel(Object wv) {
        if (wv == null) return false;
        String n = wv.getClass().getName();
        return n.equals("com.tencent.smtt.sdk.WebView")
                || n.startsWith("android.webkit.")
                || n.startsWith("com.tencent.smtt.sdk.WebView");
    }

    /** 最可靠的注入：loadUrl("javascript:...") 不依赖 ValueCallback，X5/系统 WebView 均支持。返回 false 表示实例无效/容器，调用方可移除 */
    private static boolean injectViaLoadUrl(Object wv, Config c) {
        if (!isWebViewKernel(wv)) {
            XposedBridge.log("[OralPkJudge] [init] loadUrl-inject SKIP container " + wv.getClass().getName());
            return false;
        }
        try {
            String js = buildInjectJs(c);
            XposedHelpers.callMethod(wv, "loadUrl", "javascript:" + js);
            XposedBridge.log("[OralPkJudge] [init] loadUrl-injected " + wv.getClass().getName());
            return true;
        } catch (Throwable t) {
            XposedBridge.log("[OralPkJudge] [init] loadUrl-inject err on "
                    + wv.getClass().getName() + ": " + t);
            return false;
        }
    }

    private static void pushState(Object wv, Config c) throws Throwable {
        injectViaLoadUrl(wv, c);
    }

    private static void injectAndApply(Object wv, Config c) {
        try {
            if (!isWebViewKernel(wv)) {
                // 容器/包装类（如 BaseWebApp）的 loadUrl/evaluateJavascript 不执行 JS，
                // 真内核由 evaluateJavascript hook（X5/系统）捕获后注入，这里只记录。
                LIVE_WEBVIEWS.add(new WeakReference<>(wv));
                XposedBridge.log("[OralPkJudge] [init] inject SKIP container " + wv.getClass().getName());
                return;
            }
            // evaluateJavascript 必须在主线程调用。且不能延迟——WebView 可能被销毁
            // （cr_AwContents: destroyed WebView）。立即注入，WebView 存活时才有效。
            postToMain(() -> {
                try {
                    String js = buildInjectJs(c);
                    // 主力用 loadUrl("javascript:...")：不依赖 ValueCallback 类型
                    try {
                        XposedHelpers.callMethod(wv, "loadUrl", "javascript:" + js);
                        XposedBridge.log("[OralPkJudge] [init] injected into " + wv.getClass().getName()
                                + " allCorrect=" + c.allCorrect + " auto=" + c.autoAnswer + " delay=" + c.autoDelay);
                    } catch (Throwable t2) {
                        XposedBridge.log("[OralPkJudge] [init] loadUrl inject err: " + t2);
                    }
                    // 兜底：evaluateJavascript 有时可执行（保留，但不再作为唯一通道）
                    try {
                        XposedHelpers.callMethod(wv, "evaluateJavascript", js, makeValueCallback(wv));
                    } catch (Throwable te) {
                        XposedBridge.log("[OralPkJudge] [init] eval inject err: " + te);
                    }
                } catch (Throwable t) {
                    XposedBridge.log("[OralPkJudge] [init] inject EXC: " + t);
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("[OralPkJudge] [init] inject EXC: " + t);
        }
    }

    /** 按 WebView 实际类型选正确的 ValueCallback 接口（系统 WebView 用 android.webkit，否则 X5） */
    private static Class<?> callbackClassFor(Object wv) {
        if (wv != null && wv.getClass().getName().startsWith("android.webkit.")
                && sSysValueCallback != null) return sSysValueCallback;
        return sValueCallback;
    }

    /** evaluateJavascript 结果回调：把 JS 执行结果/异常打出来（无条件，便于排障） */
    private static Object makeValueCallback(Object wv) {
        Class<?> cb = callbackClassFor(wv);
        if (cb == null) return null;
        return Proxy.newProxyInstance(
                sClassLoader, new Class[]{cb}, (proxy, method, args) -> {
                    if ("onReceiveValue".equals(method.getName())
                            && args != null && args.length > 0 && args[0] != null) {
                        XposedBridge.log("[OralPkJudge] [eval] " + args[0]);
                    }
                    return null;
                });
    }

    // ---------------------------------------------------------------
    // shouldInterceptRequest: 判题 JS 注入（控制脚本拼到 JS 内容前面）
    // ---------------------------------------------------------------
    private void hookShouldInterceptRequest() {
        try {
            Class<?> wvc = XposedHelpers.findClass("com.tencent.smtt.sdk.WebViewClient", sClassLoader);

            // 新版: shouldInterceptRequest(WebView, WebResourceRequest)
            XposedHelpers.findAndHookMethod(wvc, "shouldInterceptRequest",
                    XposedHelpers.findClass("com.tencent.smtt.sdk.WebView", sClassLoader),
                    XposedHelpers.findClass("com.tencent.smtt.export.external.interfaces.WebResourceRequest", sClassLoader),
                    new XC_MethodHook() {
                        @Override protected void beforeHookedMethod(MethodHookParam p) {
                            try {
                                Object req = p.args[1];
                                String url = String.valueOf(XposedHelpers.callMethod(req, "getUrl"));
                                if (!isPkRelated(url)) return;
                                Map<String,String> hdrs = null;
                                try {
                                    hdrs = (Map) XposedHelpers.callMethod(req, "getRequestHeaders");
                                } catch (Throwable ignore) {}
                                // 判题 JS 直接注入：构造 WebResourceResponse（控制脚本 + 原始 JS）
                                if (isJudgingJs(url)) {
                                    try {
                                        String js = syncGetJs(url, hdrs);
                                        if (js != null && !js.contains("__oralControlsInstalled")) {
                                            String prefix = buildJsFilePrefix(currentConfig());
                                            String newBody = prefix + "\n" + js;
                                            InputStream data = new ByteArrayInputStream(newBody.getBytes("UTF-8"));
                                            Object resp = makeWebResourceResponse("application/javascript", "utf-8", data);
                                            p.setResult(resp);
                                        }
                                    } catch (Throwable t) {
                                        XposedBridge.log("[OralPkJudge] sir-inject err: " + t);
                                    }
                                }
                            } catch (Throwable t) { log("sir err: " + t); }
                        }
                    });
            log("shouldInterceptRequest hook OK");
        } catch (Throwable t) {
            log("shouldInterceptRequest hook FAILED: " + t);
        }
    }

    // ---------------------------------------------------------------
    // 自动答题: 答题页加载完成后注入控制脚本并应用当前配置
    // ---------------------------------------------------------------
    private void hookAutoAnswer() {
        try {
            Class<?> wvc = XposedHelpers.findClass("com.tencent.smtt.sdk.WebViewClient", sClassLoader);
            Class<?> wv = XposedHelpers.findClass("com.tencent.smtt.sdk.WebView", sClassLoader);
            sValueCallback = XposedHelpers.findClass("com.tencent.smtt.sdk.ValueCallback", sClassLoader);
            try {
                sSysValueCallback = XposedHelpers.findClass("android.webkit.ValueCallback", sClassLoader);
            } catch (Throwable ignore) {
                sSysValueCallback = null;
            }

            XposedHelpers.findAndHookMethod(wvc, "onPageFinished", wv, String.class, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    try {
                        Object wvObj = p.args[0];
                        String url = String.valueOf(p.args[1]);
                        XposedBridge.log("[OralPkJudge] [init] onPageFinished fired -> " + cut(url));
                        // 对所有页面注入（脚本幂等）：SPA 内部路由不触发 onPageFinished，
                        // 若只在含 pk/oral 关键词的 URL 注入，入口 URL 不匹配时会整场静默失效
                        injectAndApply(wvObj, currentConfig());
                        LIVE_WEBVIEWS.add(new WeakReference<>(wvObj));
                        log("auto-answer JS injected -> " + cut(url));
                    } catch (Throwable t) { log("auto-answer inject err: " + t); }
                }
            });
            // WebView 销毁时从存活集合移除：失效 WebView 上的 evaluateJavascript 报错是
            // 异步的（cr_AwContents 线程），catch 抓不到，不清理会每次推送都刷屏
            XposedBridge.hookAllMethods(wv, "destroy", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    Object wvObj = p.thisObject;
                    if (wvObj == null) return;
                    synchronized (LIVE_WEBVIEWS) {
                        Iterator<WeakReference<Object>> it = LIVE_WEBVIEWS.iterator();
                        while (it.hasNext()) if (it.next().get() == wvObj) it.remove();
                    }
                }
            });

            // 主力：hook 系统 WebView.evaluateJavascript —— X5 在此环境回退到系统 chromium
            // WebView（cr_AwContents 堆栈证实）。当小猿在这个 WebView 上执行判题相关 JS
            // 时，把控制脚本前缀拼进去一起执行 → 必中真正被使用的 WebView。
            hookEvaluateJavascriptInjection();

            // console 日志桥：注入脚本 console.log 的内容输出到 logcat，确认 JS 执行
            hookConsoleMessage();

            log("auto-answer hook OK");
        } catch (Throwable t) {
            log("auto-answer hook FAILED: " + t);
        }
    }

    /** 捕获 WebView console 日志（确认注入脚本是否执行） */
    private void hookConsoleMessage() {
        try {
            // 系统 WebView WebChromeClient.onConsoleMessage
            Class<?> wcc = XposedHelpers.findClass("android.webkit.WebChromeClient", sClassLoader);
            try {
                XposedHelpers.findAndHookMethod(wcc, "onConsoleMessage",
                        XposedHelpers.findClass("android.webkit.ConsoleMessage", sClassLoader),
                        new XC_MethodHook() {
                            @Override protected void beforeHookedMethod(MethodHookParam p) {
                                try {
                                    Object msg = p.args[0];
                                    String m = String.valueOf(XposedHelpers.callMethod(msg, "message"));
                                    if (m != null && m.contains("[XY]")) {
                                        XposedBridge.log("[OralPkJudge] [console] " + m);
                                    }
                                } catch (Throwable ignore) {}
                            }
                        });
            } catch (Throwable t) {
                // 老签名 onConsoleMessage(String,int,String)
                try {
                    XposedHelpers.findAndHookMethod(wcc, "onConsoleMessage",
                            String.class, int.class, String.class,
                            new XC_MethodHook() {
                                @Override protected void beforeHookedMethod(MethodHookParam p) {
                                    try {
                                        String m = String.valueOf(p.args[0]);
                                        if (m.contains("[XY]")) {
                                            XposedBridge.log("[OralPkJudge] [console] " + m);
                                        }
                                    } catch (Throwable ignore) {}
                                }
                            });
                } catch (Throwable ignore) {}
            }
            // X5 WebChromeClient.onConsoleMessage(String,int,String)
            try {
                Class<?> x5wcc = XposedHelpers.findClass("com.tencent.smtt.sdk.WebChromeClient", sClassLoader);
                XposedHelpers.findAndHookMethod(x5wcc, "onConsoleMessage",
                        String.class, int.class, String.class,
                        new XC_MethodHook() {
                            @Override protected void beforeHookedMethod(MethodHookParam p) {
                                try {
                                    String m = String.valueOf(p.args[0]);
                                    if (m.contains("[XY]")) {
                                        XposedBridge.log("[OralPkJudge] [console] " + m);
                                    }
                                } catch (Throwable ignore) {}
                            }
                        });
            } catch (Throwable ignore) {}
            log("console hook OK");
        } catch (Throwable t) {
            log("console hook FAILED: " + t);
        }
    }

    /** 防递归：我们自己调用 evaluateJavascript 时不再触发追加注入 */
    private static volatile boolean sInjecting = false;
    /** 已注入控制脚本的系统 WebView 实例（identity 去重，防刷屏） */
    private static final Set<Object> INJECTED_WEBVIEWS =
            Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /** 在真实活跃的 WebView 内核上注入控制脚本（必须主线程） */
    private static void injectIntoRealWebView(final Object wv) {
        if (!isWebViewKernel(wv)) {
            XposedBridge.log("[OralPkJudge] [init] injectIntoRealWebView SKIP container " + wv.getClass().getName());
            return;
        }
        postToMain(() -> {
            if (sInjecting) return;
            sInjecting = true;
            try {
                Config c = currentConfig();
                // 主力用 loadUrl：不依赖 ValueCallback，之前 evaluateJavascript 全程未执行
                String js = buildInjectJs(c);
                try {
                    XposedHelpers.callMethod(wv, "loadUrl", "javascript:" + js);
                    XposedBridge.log("[OralPkJudge] [init] real-webview loadUrl-injected "
                            + wv.getClass().getName());
                } catch (Throwable t2) {
                    XposedBridge.log("[OralPkJudge] [init] real-webview loadUrl err: " + t2);
                }
                // 兜底 evaluateJavascript（可执行时补充 [eval] 标记确认）
                try {
                    XposedHelpers.callMethod(wv, "evaluateJavascript", js, makeValueCallback(wv));
                } catch (Throwable te) {
                    XposedBridge.log("[OralPkJudge] [init] real-webview eval err: " + te);
                }
            } catch (Throwable t) {
                XposedBridge.log("[OralPkJudge] [init] real-webview inject EXC: " + t);
            } finally {
                sInjecting = false;
            }
        });
    }

    private void hookEvaluateJavascriptInjection() {
        try {
            Class<?> sysWv = XposedHelpers.findClass("android.webkit.WebView", sClassLoader);
            // 系统 WebView.evaluateJavascript(String, ValueCallback)
            try {
                XposedHelpers.findAndHookMethod(sysWv, "evaluateJavascript",
                        String.class,
                        XposedHelpers.findClass("android.webkit.ValueCallback", sClassLoader),
                        new XC_MethodHook() {
                            @Override protected void beforeHookedMethod(MethodHookParam p) {
                                try {
                                    if (sInjecting) return;
                                    Object jsObj = p.args[0];
                                    if (!(jsObj instanceof String)) return;
                                    String js = (String) jsObj;
                                    Object wvObj = p.thisObject;
                                    if (wvObj == null) return;
                                    // 小猿自己的 JS（performance 埋点等）——这个 WebView 的
                                    // evaluateJavascript 有效。把控制脚本拼接进去同一次执行，必中。
                                    // 不做实例去重（AUTO_ANSWER_JS 有幂等 guard，重复执行无害），
                                    // 先确认追加与执行链路。
                                    if (js.contains("__setAllCorrect") || js.contains("__oralControlsInstalled")) return;
                                    LIVE_WEBVIEWS.add(new WeakReference<>(wvObj));
                                    Config c = currentConfig();
                                    String ctrl = AUTO_ANSWER_JS
                                            + "window.__setAllCorrect(" + c.allCorrect + ");"
                                            + "window.__setAutoAnswer(" + c.autoAnswer + ");"
                                            + "window.__setAutoDelay(" + c.autoDelay + ");";
                                    p.args[0] = ctrl + js;
                                    XposedBridge.log("[OralPkJudge] [init] APPENDED ctrl to sysEvalJS on "
                                            + wvObj.getClass().getName() + " jsLen=" + js.length());
                                    // 探针：主动调一次 evaluateJavascript，确认该内核实例能执行 JS
                                    // （返回值 [XY:probe] 会通过 [eval] 打印）。系统 WebView 子类用系统回调。
                                    try {
                                        if (sSysValueCallback != null) {
                                            Object cb = Proxy.newProxyInstance(sClassLoader,
                                                    new Class[]{ sSysValueCallback }, (proxy, method, args) -> {
                                                        if ("onReceiveValue".equals(method.getName())
                                                                && args != null && args.length > 0 && args[0] != null) {
                                                            XposedBridge.log("[OralPkJudge] [eval] " + args[0]);
                                                        }
                                                        return null;
                                                    });
                                            XposedHelpers.callMethod(wvObj, "evaluateJavascript", "'[XY:probe]'", cb);
                                        }
                                    } catch (Throwable t2) {
                                        XposedBridge.log("[OralPkJudge] [init] probe err: " + t2);
                                    }
                                } catch (Throwable t) {
                                    XposedBridge.log("[OralPkJudge] [init] evalHook err: " + t);
                                }
                            }
                        });
                log("sys WebView evaluateJavascript hook OK");
            } catch (Throwable t) {
                XposedBridge.log("[OralPkJudge] [init] sys eval hook FAILED: " + t);
            }

            // X5 WebView.evaluateJavascript 兜底（若 X5 真内核加载而非回退系统）
            try {
                Class<?> x5Wv = XposedHelpers.findClass("com.tencent.smtt.sdk.WebView", sClassLoader);
                XposedHelpers.findAndHookMethod(x5Wv, "evaluateJavascript",
                        String.class,
                        XposedHelpers.findClass("com.tencent.smtt.sdk.ValueCallback", sClassLoader),
                        new XC_MethodHook() {
                            @Override protected void beforeHookedMethod(MethodHookParam p) {
                                try {
                                    if (sInjecting) return;
                                    Object jsObj = p.args[0];
                                    if (!(jsObj instanceof String)) return;
                                    String js = (String) jsObj;
                                    if (js.contains("__setAllCorrect") || js.contains("__oralControlsInstalled")) return;
                                    sInjecting = true;
                                    try {
                                        Config c = currentConfig();
                                        String ctrl = AUTO_ANSWER_JS
                                                + "window.__setAllCorrect(" + c.allCorrect + ");"
                                                + "window.__setAutoAnswer(" + c.autoAnswer + ");"
                                                + "window.__setAutoDelay(" + c.autoDelay + ");"
                                                + "window.__xyApplied=true;";
                                        p.args[0] = ctrl + js;
                                        Object x5wv = p.thisObject;
                                        LIVE_WEBVIEWS.add(new WeakReference<>(x5wv));
                                        // 主动对 X5 真内核做完整注入（loadUrl），不依赖小猿后续调用
                                        if (!INJECTED_WEBVIEWS.contains(x5wv)) {
                                            INJECTED_WEBVIEWS.add(x5wv);
                                            injectViaLoadUrl(x5wv, c);
                                        }
                                        XposedBridge.log("[OralPkJudge] [init] prepended ctrl (x5) jsLen=" + js.length());
                                    } finally {
                                        sInjecting = false;
                                    }
                                } catch (Throwable t) {
                                    XposedBridge.log("[OralPkJudge] [init] x5 evalHook err: " + t);
                                }
                            }
                        });
                log("x5 WebView evaluateJavascript hook OK");
            } catch (Throwable t) {
                XposedBridge.log("[OralPkJudge] [init] x5 eval hook FAILED: " + t);
            }

            // 系统 WebViewClient.onPageFinished：判题渲染在系统 WebView（X5 回退），
            // p.args[0] 是真正渲染的系统 WebView，对它 loadUrl 注入必中
            try {
                Class<?> sysWvc = XposedHelpers.findClass("android.webkit.WebViewClient", sClassLoader);
                Class<?> sysWv2 = XposedHelpers.findClass("android.webkit.WebView", sClassLoader);
                XposedHelpers.findAndHookMethod(sysWvc, "onPageFinished", sysWv2, String.class, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam p) {
                        try {
                            Object wvObj = p.args[0];
                            String url = String.valueOf(p.args[1]);
                            if (!isPkRelated(url)) return;
                            XposedBridge.log("[OralPkJudge] [init] SYS onPageFinished -> " + cut(url)
                                    + " wv=" + wvObj.getClass().getName());
                            if (INJECTED_WEBVIEWS.contains(wvObj)) return;
                            INJECTED_WEBVIEWS.add(wvObj);
                            LIVE_WEBVIEWS.add(new WeakReference<>(wvObj));
                            injectIntoRealWebView(wvObj);
                        } catch (Throwable t) {
                            XposedBridge.log("[OralPkJudge] [init] sys onPageFinished err: " + t);
                        }
                    }
                });
                log("sys WebViewClient onPageFinished hook OK");
            } catch (Throwable t) {
                XposedBridge.log("[OralPkJudge] [init] sys WebViewClient hook FAILED: " + t);
            }
        } catch (Throwable t) {
            XposedBridge.log("[OralPkJudge] [init] hookEvalInjection err: " + t);
        }
    }

    // ---------------------------------------------------------------
    // 工具
    // ---------------------------------------------------------------
    private static boolean isPkRelated(String s) {
        if (s == null) return false;
        String low = s.toLowerCase();
        for (String k : PK_KEYWORDS) if (low.contains(k)) return true;
        return low.contains("oral-pk") || low.contains("exercise.html") || low.contains("pk");
    }

    private static String cut(String s) {
        if (s == null) return "";
        return s.length() > MAX_BODY ? s.substring(0, MAX_BODY) + " ...[truncated] len=" + s.length() : s;
    }

    // ---------------------------------------------------------------
    // JS 篡改注入: 判题页面所有 JS 走 shouldInterceptRequest 加载。
    // 命中判题 JS 时，把控制脚本拼到 JS 内容前面 → 控制脚本和判题逻辑
    // 必然在同一 JS 上下文执行（不受 WebView 实例/容器影响）。
    // ---------------------------------------------------------------
    private static boolean isJudgingJs(String url) {
        if (url == null) return false;
        String u = url;
        int q = u.indexOf('?');
        if (q > 0) u = u.substring(0, q);
        if (!u.endsWith(".js")) return false;
        // 只注入判题核心 JS，避免破坏其它模块导致卡加载
        if (!u.contains("leo-web-oral-pk/assets/")) return false;
        String file = u.substring(u.lastIndexOf('/') + 1);
        return file.startsWith("index-legacy.")
                || file.startsWith("Oral-legacy.")
                || file.startsWith("exercise-legacy.");
    }

    private static String readAll(InputStream in) throws Throwable {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        return new String(bos.toByteArray(), "UTF-8");
    }

    /** 同步获取判题 JS 内容（网络下载）。供 shouldInterceptRequest 注入用 */
    private static String syncGetJs(String url, Map<String,String> headers) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(15000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0 Mobile Safari/537.36");
            conn.setRequestProperty("Accept", "*/*");
            if (headers != null) for (Map.Entry<String,String> e : headers.entrySet()) {
                if (e.getKey() != null && e.getValue() != null)
                    conn.setRequestProperty(e.getKey(), e.getValue());
            }
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) return null;
            InputStream in = conn.getInputStream();
            String s = readAll(in);
            in.close();
            return s;
        } catch (Throwable t) {
            XposedBridge.log("[OralPkJudge] syncGetJs err: " + t);
            return null;
        }
    }

    /** 构造 X5 WebResourceResponse：优先反射具体实现类，WebResourceResponse 不是接口无法用 Proxy */
    private static Object makeWebResourceResponse(String mime, String encoding, InputStream data) {
        // 候选实现类（X5 各版本）
        String[] candidates = {
                "com.tencent.smtt.export.external.WebResourceResponseImpl",
                "com.tencent.smtt.sdk.WebResourceResponse",
                "com.tencent.smtt.export.external.interfaces.WebResourceResponse"
        };
        for (String cn : candidates) {
            try {
                Class<?> cls = XposedHelpers.findClass(cn, sClassLoader);
                if (!cls.isInterface()) {
                    // 优先带 headers 的构造（Content-Length 等，WebView 解析需要）
                    try {
                        Map<String,String> hdrs = new HashMap<>();
                        hdrs.put("Content-Type", mime);
                        hdrs.put("Content-Length", String.valueOf(data.available()));
                        hdrs.put("Access-Control-Allow-Origin", "*");
                        hdrs.put("Cache-Control", "no-cache");
                        return XposedHelpers.newInstance(cls, mime, encoding, 200, "OK", hdrs, data);
                    } catch (Throwable t) {
                        return XposedHelpers.newInstance(cls, mime, encoding, data);
                    }
                }
            } catch (Throwable ignore) {}
        }
        // 最后兜底：如果全是接口/找不到，用动态代理（仅当目标真是接口时）
        try {
            Class<?> iface = XposedHelpers.findClass(
                    "com.tencent.smtt.export.external.interfaces.WebResourceResponse", sClassLoader);
            if (iface.isInterface()) {
                return Proxy.newProxyInstance(sClassLoader, new Class[]{ iface }, (proxy, method, args) -> {
                    String n = method.getName();
                    switch (n) {
                        case "getData": return data;
                        case "getMimeType": return mime;
                        case "getEncoding": return encoding;
                        case "getStatusCode": return 200;
                        case "getReasonPhrase": return "OK";
                        case "getResponseHeaders": return java.util.Collections.EMPTY_MAP;
                        case "getHeaders": return java.util.Collections.EMPTY_MAP;
                        case "toString": return "X5WebResourceResponse(proxy)";
                    }
                    return null;
                });
            }
        } catch (Throwable ignore) {}
        throw new IllegalArgumentException("no WebResourceResponse impl class found");
    }

    private static void log(String s) {
        Log.i(TAG, s);
        XposedBridge.log("[OralPkJudge] " + s);
    }
}
