package five.ec1cff.mysysteminjector.xposed;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.view.Window;

import java.io.File;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

@SuppressWarnings("unchecked")
public class HookEntry implements IXposedHookLoadPackage {
    private static final String TAG = "MySystemInjector";
    private static final String WORKDIR = "/data/system/fuckmiui";
    boolean loaded = false;
    public static boolean isFeatureEnabled(String featureName) {
        return new File(new File(WORKDIR), featureName).exists();
    }

    public static void log(String msg) {
        XposedBridge.log("[" + TAG + "] " + msg);
    }

    public static void log(String msg, Throwable t) {
        XposedBridge.log("[" + TAG + "] " + msg);
        XposedBridge.log(t);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        boolean inSystemServer = lpparam.packageName.equals("android") && lpparam.processName.equals("android");
        boolean inSystem = lpparam.packageName.equals("system");
        XposedBridge.log("handleLoadPackage inSystem=" + inSystem + " package=" + lpparam.packageName + " process=" + lpparam.processName);
        if (isFeatureEnabled("share") && !inSystem) {
            hookShareInUI(lpparam);
        }
        if (!inSystemServer && !inSystem) {
            return;
        }
        if (loaded) return;
        loaded = true;

        if (isFeatureEnabled("disable")) {
            log("disabled, exit");
            return;
        }

        if (inSystem) {
            if (isFeatureEnabled("xspace")) {
                hookXSpaceInUI(lpparam);
            }
        }

        if (!inSystemServer) return;

        try {
            if (isFeatureEnabled("nowakepath")) {
                log("hook for nowakepath");
                // miui-framework.jar
                XposedBridge.hookAllMethods(
                        XposedHelpers.findClass("miui.security.SecurityManager", lpparam.classLoader),
                        "getCheckStartActivityIntent",
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                param.setResult(null);
                            }
                        }
                );
                log("hook done");
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        try {
            if (isFeatureEnabled("installer")) {
                log("hook for installer");
                XposedBridge.hookAllMethods(
                        XposedHelpers.findClass("com.android.server.pm.PackageManagerServiceInjector", lpparam.classLoader),
                        "checkPackageInstallerStatus",
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                XposedBridge.log(TAG + "tried to protect installer");
                                Object curPkgSettings = param.args[1];
                                Object mPackages = XposedHelpers.getObjectField(curPkgSettings, "mPackages");
                                Object googleInstaller = XposedHelpers.callMethod(mPackages, "get", "com.google.android.packageinstaller");
                                Object miuiInstaller = XposedHelpers.callMethod(mPackages, "get", "com.miui.packageinstaller");
                                if (googleInstaller == null || miuiInstaller == null) {
                                    log("failed to find PackageSetting, cancel");
                                    return;
                                }
                                log("google=" + googleInstaller);
                                log("miui=" + miuiInstaller);
                                XposedHelpers.callMethod(googleInstaller, "setInstalled", true, 0);
                                XposedHelpers.callMethod(miuiInstaller, "setInstalled", false, 0);
                                try {
                                    Object installer = XposedHelpers.callMethod(param.args[0], "getRequiredInstallerLPr");
                                    log("replace installer:" + installer);
                                } catch (RuntimeException e) {
                                    // ?
                                    log("failed to replace installer, call original method fallback...", e);
                                    return;
                                } catch (Throwable t) {
                                    log("something wrong", t);
                                }
                                param.setResult(null);
                            }
                        }
                );
                log("hook done");
            }
        } catch (Throwable t) {
            log("hook installer", t);
        }

        try {
            if (isFeatureEnabled("nomiuiintent")) {
                log("hook for nomiuiintent");
                // for hyperos 1.0.2.0

                XposedHelpers.findAndHookMethod("com.android.server.pm.PackageManagerServiceImpl", lpparam.classLoader,
                        "hookChooseBestActivity",
                        Intent.class, String.class, long.class, List.class, int.class, ResolveInfo.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                param.setResult(param.args[5]); // defaultValue
                            }
                        }
                );
                /*
                // for 13.0.3
                // miui-services.jar
                XposedHelpers.findAndHookMethod("com.android.server.pm.PackageManagerServiceImpl", lpparam.classLoader,
                        "hookChooseBestActivity",
                    Intent.class, String.class, int.class, List.class, int.class, ResolveInfo.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(param.args[5]); // defaultValue
                        }
                    }
                );*/
                // for 12.5.7
                // services.jar
                /*
                XposedBridge.hookAllMethods(
                        XposedHelpers.findClass("com.android.server.pm.PackageManagerServiceInjector", lpparam.classLoader),
                        "checkMiuiIntent",
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                param.setResult(
                                        XposedHelpers.getObjectField(param.args[0], "mResolveInfo")
                                );
                            }
                        }
                );*/
                log("nomiuiintent hook done");
            }
        } catch (Throwable t) {
            log("nomiuiintent error", t);
        }

        try {
            if (isFeatureEnabled("protect_mc")) {
                log("hook for ProcessMemoryCleaner");
                XposedHelpers.findAndHookMethod(
                        XposedHelpers.findClass("com.android.server.am.ProcessMemoryCleaner", lpparam.classLoader),
                        "checkBackgroundApp",
                        String.class, int.class,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                var packageName = (String) param.args[0];
                                if (isFeatureEnabled("protect_mc_" + packageName)) {
                                    // XposedBridge.log("protect " + packageName + " from PMC");
                                    param.setResult(null);
                                }
                            }
                        }
                );
            }
        } catch (Throwable t) {
            log("protect_mc error", t);
        }

        try {
            if (isFeatureEnabled("fonts")) {
                log("hook for fonts");
                ThreadLocal<Boolean> isCreating = new ThreadLocal<>();
                Class<?> FMS = XposedHelpers.findClass("com.android.server.graphics.fonts.FontManagerService", lpparam.classLoader);
                Class<?> FUI = XposedHelpers.findClass("com.android.server.graphics.fonts.FontManagerService$FsverityUtilImpl", lpparam.classLoader);
                XposedBridge.hookAllConstructors(
                        FMS,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                isCreating.set(true);
                            }

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                isCreating.set(false);
                            }
                        }
                );
                XposedBridge.hookAllMethods(
                        XposedHelpers.findClass("com.android.internal.security.VerityUtils", lpparam.classLoader),
                        "isFsVeritySupported",
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (isCreating.get()) {
                                    param.setResult(true);
                                    isCreating.set(false);
                                }
                            }
                        }
                );
                XposedBridge.hookAllMethods(
                        FUI,
                        "hasFsverity",
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                param.setResult(true);
                            }
                        }
                );
                XposedBridge.hookAllMethods(
                        FUI,
                        "setUpFsverity",
                        new XC_MethodReplacement() {
                            @Override
                            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                return null;
                            }
                        }
                );
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        try {
            if (isFeatureEnabled("clipboard")) {
                XposedBridge.hookAllMethods(
                        XposedHelpers.findClass("com.android.server.clipboard.ClipboardService", lpparam.classLoader),
                        "clipboardAccessAllowed",
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                var pkg = (String) param.args[1];
                                if (pkg == null) return;
                                if ("com.fooview.android.fooview".equals(pkg) || "com.termux".equals(pkg) || "com.termux.api".equals(pkg))
                                    param.setResult(true);
                            }
                        }
                );
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        hookXSpace(lpparam);
    }

    private void hookShareInUI(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            /*
            var impl = new ResolverActivityStubImpl() {
                @Override
                public boolean useAospShareSheet() {
                    XposedBridge.log("useAospShareSheet");
                    return true;
                }
            };
            XposedHelpers.callStaticMethod(
                XposedHelpers.findClass("com.miui.base.MiuiStubRegistry", lpparam.classLoader),
                "registerSingleton", impl
            );
            XposedHelpers.setStaticObjectField(ResolverActivityStub.class, "sInstance", impl);
            */
            XposedBridge.hookAllMethods(
                    XposedHelpers.findClass("com.android.internal.app.ResolverActivityStubImpl", lpparam.classLoader),
                    "useAospShareSheet",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log("useAospShareSheet");
                            param.setResult(true);
                        }
                    }
            );
            XposedBridge.log("hook installed");
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    @SuppressWarnings("unchecked")
    private void hookXSpace(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!isFeatureEnabled("xspace")) return;
        log("[MySystemInjector] hook for xspace");
        try {
                Class<?> classXSpaceManager = XposedHelpers.findClass("com.miui.server.xspace.XSpaceManagerServiceImpl", lpparam.classLoader);
                List<String> list = (List<String>) XposedHelpers.getStaticObjectField(classXSpaceManager, "sCrossUserCallingPackagesWhiteList");
                if (list != null) {
                    list.add("com.android.shell");
                    list.add("com.xiaomi.xmsf");
                    log("add required packages to whitelist at init");
                } else {
                    log("whitelist is null");
                }
            List<String> publicActions = (List<String>) XposedHelpers.getStaticObjectField(classXSpaceManager, "sPublicActionList");
            if (publicActions != null) {
                publicActions.clear();
                log("clear publicActionList");
            } else {
                log("publicActionList is null");
            }
        } catch (Throwable t) {
            log("hook xspace shell failed", t);
        }
    }

    private void hookXSpaceInUI(XC_LoadPackage.LoadPackageParam lpparam) {
        log("hookForXspace in system");
        XposedBridge.hookAllMethods(
                XposedHelpers.findClass("com.android.internal.app.ResolverListController", lpparam.classLoader),
                "getResolversForIntent",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        List<Object> result = (List<Object>) param.getResult();
                        List<Intent> intents = (List<Intent>) param.args[2];
                        if (intents != null && !intents.isEmpty()) {
                            String action = intents.get(0).getAction();
                            if ("miui.intent.action.MIUI_CHOOSER".equals(action)
                                    || Intent.ACTION_CHOOSER.equals(action)) {
                                return;
                            }
                        }
                        if (result == null) return;
                        List<Object>/*com.android.internal.app.ResolverActivity$ResolvedComponentInfo*/ listForXSpace = (List<Object>) XposedHelpers.callMethod(param.thisObject, "getResolversForIntentAsUser",
                                param.args[0], param.args[1], param.args[2],
                                XposedHelpers.callStaticMethod(android.os.UserHandle.class, "of", 999
                                )
                        );
                        for (Object infoXs : listForXSpace) {
                            List<ResolveInfo> rInfosXs = (List<ResolveInfo>) XposedHelpers.getObjectField(infoXs, "mResolveInfos");
                            if (rInfosXs.isEmpty()) continue;
                            ResolveInfo rInfoXs = rInfosXs.get(0);
                            rInfosXs.set(0, new MyResolveInfo(rInfoXs));
                            for (Object orig : result) {
                                List<ResolveInfo> infos = (List<ResolveInfo>) XposedHelpers.getObjectField(orig, "mResolveInfos");
                                if (infos.isEmpty()) continue;
                                ResolveInfo oldInfo = infos.get(0);
                                if (Objects.equals(oldInfo.activityInfo.packageName, rInfoXs.activityInfo.packageName)
                                        && Objects.equals(oldInfo.activityInfo.name, rInfoXs.activityInfo.name)) {
                                    infos.set(0, new MyResolveInfo(oldInfo));
                                }
                            }
                        }
                        log("add resolveInfo " + result.size() + " + " + listForXSpace.size());
                        result.addAll(listForXSpace);
                    }
                }
        );

        XposedBridge.hookAllMethods(
                XposedHelpers.findClass("com.android.internal.app.ResolverListAdapter", lpparam.classLoader),
                "shouldAddResolveInfo",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Object o = param.args[0];
                        ResolveInfo resolve = (ResolveInfo) XposedHelpers.getObjectField(o, "mResolveInfo");
                        log("check resolveinfo " + resolve);
                        if (resolve.activityInfo.applicationInfo.uid / 100000 == 999) {
                            log("allow resolveinfo " + resolve);
                            param.setResult(true);
                        }
                    }
                }
        );

        XposedBridge.hookAllMethods(
                XposedHelpers.findClass("com.android.internal.app.ResolverListAdapter$TargetPresentationGetter", lpparam.classLoader),
                "getIconBitmap",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        log("get icon");
                        ApplicationInfo ai = (ApplicationInfo) XposedHelpers.getObjectField(param.thisObject, "mAi");
                        if (ai.uid / 100000 == 999) {
                            param.args[0] = XposedHelpers.callStaticMethod(UserHandle.class, "of", 999);
                        }
                    }
                }
        );

        ThreadLocal<MyResolveInfo> currentResolveInfo = new ThreadLocal<>();

        Class<?> classMIUIResolverActivity = XposedHelpers.findClass("com.android.internal.app.MiuiResolverActivity", lpparam.classLoader);

        XposedBridge.hookAllMethods(
                classMIUIResolverActivity,
                "safelyStartActivity", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        ResolveInfo ri = (ResolveInfo) XposedHelpers.getObjectField(param.args[0], "mResolveInfo");
                        log("startActivity ri=" + ri + " class=" + ri.getClass());
                        if (ri instanceof MyResolveInfo) {
                            currentResolveInfo.set((MyResolveInfo) ri);
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        currentResolveInfo.remove();
                    }
                });

        XposedBridge.hookAllMethods(
                classMIUIResolverActivity,
                "startAsCaller",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        log("starting");
                        MyResolveInfo current = currentResolveInfo.get();
                        if (current != null) {
                            Intent intent = (Intent) param.args[0];
                            // see com.miui.server.xspace.XSpaceManagerServiceImpl#checkXSpaceControl
                            intent.putExtra("android.intent.extra.xspace_userid_selected", true);
                            int userId = current.activityInfo.applicationInfo.uid / 100000;
                            log("set to " + userId);
                            param.args[1] = userId;
                        }
                    }
                }
        );

        XposedBridge.hookAllMethods(
                XposedHelpers.findClass("com.android.internal.app.ResolverActivityStubImpl$LoadIconTask", lpparam.classLoader),
                "doInBackground",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ResolveInfo info = (ResolveInfo) XposedHelpers.getObjectField(param.thisObject, "mResolveInfo");
                        if (info.activityInfo.applicationInfo.uid / 100000 != 999) return;
                        log("fixup icon");
                        Drawable result = (Drawable) param.getResult();
                        Context context = (Context) XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "this$0"), "mContext");
                        Drawable realResult = context.getPackageManager().getUserBadgedIcon(result, (UserHandle) XposedHelpers.callStaticMethod(UserHandle.class, "of", 999));
                        param.setResult(realResult);
                    }
                }
        );

        try {
            Helper.deoptimizeMethod(classMIUIResolverActivity, "onStart");
        } catch (Throwable t) {
            log("failed to deoptimize", t);
        }

        XposedBridge.hookAllMethods(Window.class, "addSystemFlags",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        log("clear hide non system overlay windows flags");
                        param.args[0] = (int) param.args[0] & ~524288 /*SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS*/;
                    }
                });

        XposedBridge.hookAllMethods(classMIUIResolverActivity, "showTargetDetails",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        ResolveInfo ri = (ResolveInfo) param.args[0];
                        Activity activity = (Activity) param.thisObject;
                        if (ri.activityInfo.applicationInfo.uid / 100000 == 999) {
                            Intent intent = new Intent().setAction("miui.intent.action.APP_MANAGER_APPLICATION_DETAIL")
                                    .putExtra("package_name", ri.activityInfo.packageName)
                                    .putExtra("miui.intent.extra.USER_ID", 999);
                            activity.startActivity(intent);
                            XposedHelpers.callMethod(activity, "dismiss");
                            param.setResult(null);
                        }
                    }
                });
    }

    private static class MyResolveInfo extends ResolveInfo {
        MyResolveInfo(ResolveInfo info) {
            super(info);
        }
    }
}
