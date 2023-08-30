package five.ec1cff.mysysteminjector.xposed;

import android.os.Process;

import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
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

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("current package=" + lpparam.packageName + " current pid=" + Process.myPid()
                + " current process=" + lpparam.processName + " classLoader=" + lpparam.classLoader);
        if (!lpparam.packageName.equals("android") || !lpparam.processName.equals("android")) {
            return;
        }
        if (loaded) return;
        loaded = true;

        if (isFeatureEnabled("disable")) {
            XposedBridge.log("disabled, exit");
            return;
        }

        try {
            if (isFeatureEnabled("nowakepath")) {
                XposedBridge.log("hook for nowakepath");
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
                XposedBridge.log("hook done");
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        try {
            if (isFeatureEnabled("installer")) {
                XposedBridge.log("hook for installer");
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
                                    XposedBridge.log("failed to find PackageSetting, cancel");
                                    return;
                                }
                                XposedBridge.log("google=" + googleInstaller);
                                XposedBridge.log("miui=" + miuiInstaller);
                                XposedHelpers.callMethod(googleInstaller, "setInstalled", true, 0);
                                XposedHelpers.callMethod(miuiInstaller, "setInstalled", false, 0);
                                try {
                                    Object installer = XposedHelpers.callMethod(param.args[0], "getRequiredInstallerLPr");
                                    XposedBridge.log("replace installer:" + installer);
                                } catch (RuntimeException e) {
                                    XposedBridge.log("failed to replace installer, call original method fallback...");
                                    XposedBridge.log(e);
                                    return;
                                } catch (Throwable t) {
                                    XposedBridge.log(t);
                                }
                                param.setResult(null);
                            }
                        }
                );
                XposedBridge.log("hook done");
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        try {
            if (isFeatureEnabled("nomiuiintent")) {
                XposedBridge.log("hook for installer");
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
                );
                XposedBridge.log("hook done");
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        try {
            if (isFeatureEnabled("protect_mc")) {
                XposedBridge.log("hook for ProcessMemoryCleaner");
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
            XposedBridge.log(t);
        }
    }
}
