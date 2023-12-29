package five.ec1cff.mysysteminjector.xposed;

import android.content.Intent;
import android.content.pm.ResolveInfo;

import java.io.File;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

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
        if (!lpparam.packageName.equals("android") || !lpparam.processName.equals("android")) {
            return;
        }
        if (loaded) return;
        loaded = true;

        if (isFeatureEnabled("disable")) {
            log("disabled, exit");
            return;
        }

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
                );
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

        hookXSpace(lpparam);
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
                    XposedBridge.log("[MySystemInjector] add shell to whitelist at init");
                } else {
                    XposedBridge.log("[MySystemInjector] whitelist is null");
                }
        } catch (Throwable t) {
            log("hook xspace shell failed", t);
        }
    }
}
