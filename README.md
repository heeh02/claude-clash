# Claude Clash

面向 Claude 的定制 Clash（Android），基于 [ClashMetaForAndroid](https://github.com/MetaCubeX/ClashMetaForAndroid) 和 mihomo。

**独立开源项目，与 Anthropic 无隶属关系，也不代表 Anthropic 官方认可。**

## 当前状态

项目处于公开测试阶段：应用层功能与 release 构建可用，仍需在目标设备上验证具体节点、系统版本和网络环境。构建成功不等于所有 Android 设备上的 VPN 行为均已验证。

不包含任何实际代理节点、节点 IP、UUID、私钥、个人配置、个人目录、SSH 凭据或发布密钥。

## 功能

- 导入本地文件或 HTTPS URL 形式的 Clash YAML 配置。
- 不内置、不锁定任何国家或节点；Claude 的分流规则由导入的配置决定。
- 规则模式：按导入配置中的代理组、规则和 DIRECT 分流。
- 全局模式：所有连接必须进入代理，过滤 DIRECT 及其他不可转发项；没有可用代理时拒绝启动，而不是静默直连。
- 全应用 `VpnService`，接管 IPv4、IPv6 和 DNS，不提供应用绕过或 `allowBypass`。
- 禁止 Direct 模式。
- 原始配置保持不变，运行时生成 `.privacy/config.yaml`，并共享代理资源目录。
- 关闭外部控制 Activity、配置备份和二维码入口，合并时移除 CAMERA 权限。
- 提供系统 VPN 设置入口，便于启用“始终开启 VPN”和“阻止未连接 VPN 的网络”。

## 权限说明

- `INTERNET`、`ACCESS_NETWORK_STATE`、`CHANGE_NETWORK_STATE`：代理与 VPN 网络管理。
- `FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_SPECIAL_USE`：维持代理服务。
- `RECEIVE_BOOT_COMPLETED`：按系统允许的方式恢复服务。
- `POST_NOTIFICATIONS`：显示服务状态。
- `QUERY_ALL_PACKAGES`：仅用于 Clash 进程规则的本地连接归属，不用于配置应用绕过。

不申请通讯录、精确位置或 CAMERA 权限。安装应用列表权限只用于进程规则匹配，不进行用户画像。

## 使用

1. 构建并安装应用。
2. 在“配置”中选择文件或 URL，导入自己的 Clash 配置。仓库不提供可用节点。
3. 首次启动时批准 Android 的 VPN 连接请求。
4. 需要防断网直连时，在系统 VPN 设置中启用“始终开启 VPN”和“阻止未连接 VPN 的网络”。

规则模式保留配置中的 DIRECT 语义；全局模式则要求所有流量进入代理。全局组中的 DIRECT 选项不会作为有效出口。

## 构建

要求：OpenJDK 21、Android SDK、Python 3 和 curl。

可使用本机已有的 `local.properties` 指定 SDK 与自定义包名；该文件及签名配置不会纳入版本控制：

```properties
sdk.dir=/path/to/android-sdk
custom.application.id=app.claudeclash
remove.suffix=true
```

构建 release：

```bash
./gradlew app:assembleMetaRelease
```

输出目录为 `app/build/outputs/apk/meta/release/`，按 ABI 分包。文件名采用 `claude-clash-<version>-meta-<abi>-release.apk` 的形式。

未提供 `signing.properties` 时，release 构建使用本机 Android Debug 签名，仅用于测试。正式发布需配置自己的发布签名；`signing.properties`、`release.keystore` 和本机 `debug.keystore` 均不得提交或公开。

CI 只执行构建，不发布 APK，也不需要或保存任何签名密钥。

## 原生依赖

- 上游：ClashMetaForAndroid `v2.11.37`。
- `libbridge.so` 与 `libclash.so` 来自同版本官方发布包，`scripts/fetch-native-core.sh` 校验发布包 SHA-256 后按 ABI 提取。
- YAML 使用 `it.krzeminski:snakeyaml-engine-kmp:3.0.3`，避免依赖 Android 不提供的 `java.beans`。
- 地理数据库由 Gradle 下载；已有文件不会在每个构建中重复下载。

## 已验证范围

2026-11-17 在 Android 15（API 35）arm64 模拟器上完成：

- 通过系统文件选择器导入配置；原配置不被改写，运行时的 GLOBAL 组移除 DIRECT。
- 使用本地标记出口验证规则模式的 DIRECT 分流和全局模式的强制代理，两者返回不同标记。
- 使用实际 VLESS/REALITY 节点完成 DNS 和 HTTPS 出口检查；测试配置与结果不进入仓库。
- 启用系统“始终开启 VPN”和“阻止未连接 VPN 的网络”后，停止服务会使浏览器返回 `ERR_NETWORK_ACCESS_DENIED`；完成两轮停止与恢复测试。
- 修复了重启时全局检查可能先于配置加载完成的启动竞态。
- release 构建、安装、CRC、zip 对齐、v1/v2 签名验证及权限检查。

尚未在实体手机上验证蜂窝网络切换、WebRTC、开机恢复和各厂商后台策略。在线订阅、proxy-providers 和特殊协议仍需按实际配置复测。本应用是公开测试版本，不保证所有设备与网络环境下均能达到相同结果。

## 隐私边界

本应用是网络层代理工具，不会代替用户管理以下信息：

- Claude 账户、手机号、付款资料、登录历史和设备权限；
- SIM 国家、运营商、系统语言、Google Play 地区和设备型号；
- 浏览器、应用或 Claude Code 主动收集或回传的数据；
- 用户导入的节点提供商、Claude 及其他网络服务自身的日志和隐私行为。

详见 [PRIVACY_POLICY.md](PRIVACY_POLICY.md)。

## 验证与发布纪律

- `./gradlew app:assembleMetaRelease` 必须通过。
- 安装包需通过 CRC、zip 对齐及签名验证。
- 节点、规则、GLOBAL、DNS、IPv4/IPv6、WebRTC、Wi-Fi/蜂窝切换及 always-on/lockdown 应在目标设备上实测。
- `app:lintMetaDebug` 仍有上游 `FilesActivity.kt` 的 `MissingSuperCall`，不宣称 Lint 全通过。
- 发布时不得包含真实节点、凭据、个人配置、私有目录、调试密钥或带个人信息的截图和日志。

## 许可证

本项目保留上游 GPLv3、NOTICE 和第三方许可。上游 CMFA 的内核来自 `android-real` 分支；其内核贡献分别进入 `Alpha` 与 `android-open`。

本项目不是 MetaCubeX 官方发行版。修改内容在源码中公开，第三方名称和商标归各自所有者。
