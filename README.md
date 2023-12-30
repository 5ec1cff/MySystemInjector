# 自用 MIUI 系统 hook

支持版本： ~~12.5.7~~ -> 13.0.3

作用域：system （系统服务）、android（系统）

## 使用方法

```sh
mkdir -p /data/system/fuckmiui
touch /data/system/fuckmiui/${feature}
```

feature 为你想要开启的功能（详见下方标题）

如果创建了名为 `disable` 的文件，则会关闭所有功能，便于系统无法启动的时候排查问题。

## 功能

画删除线的是停止维护的功能，不保证可用

### nowakepath

禁用应用间启动 activity 的警告

### ~~installer~~

防止 google installer 被自动卸载

如果你想换用 google installer ，需要开启该功能同时卸载 miui installer ，因为系统不允许同时存在两个 installer （会崩）。

### nomiuiintent

防止 MIUI 锁定 intent （例如调用应用安装器）

### ~~protect_mc~~

防止杀进程（不知道什么情况下会触发的自动清理）

启用后还需要需要添加文件 `protect_mc_${packageName}` 指定你想要阻止被杀进程的包名。例如 `protect_mc_com.tencent.mobileqq` 。

这个功能的历史：作者曾经有一段时间被 MIUI 自动杀 QQ 困扰。按理来说 QQ 应该在各大国产系统的白名单中，也许是云控系统抽风了。

由于 QQ 冷启动实在太慢，因此写了这个功能保活。~~没想到用户也有主动帮 QQ 保活的一天。~~

### fonts

强制启用 FontManagerService 的更新功能，并禁用基于 fs-verity 的验证。

启用后可通过以下命令升级字体：

```sh
# /path/to/dummy 是空文件
# 所有路径需要系统服务有读权限，例如 /data/local/tmp
cmd font update /path/to/font.ttf /path/to/dummy
```

这个功能和 MIUI 无关，理论上适用于任何 Android 12 系统。

### xspace

1. 在系统服务：允许 shell 指定用户直接启动 activity 而无需弹出选择双开的提示（ `am start --user` ）
2. 在系统：ResolverActivity 直接显示双开 app 的打开方式，无需二次点击。
3. 修复该版本中点击 xmsf 推送的通知会显示选择双开的 bug 。
4. 禁止 ResolverActivity 添加 `SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS` flags ，允许悬浮窗在其上显示。  
