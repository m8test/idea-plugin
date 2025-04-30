<!-- Plugin description -->

# [M8Test](https://dev-docs.m8test.com) IntelliJ 插件

此插件需要配合 [M8Test 调试插件](https://github.com/m8test/debugger) 使用.

+ Tips: 如果有任何问题都可以加入我们的官方qq交流群:[749248182](https://qm.qq.com/q/d0g29SBUGY)
  或者 [qq频道](https://pd.qq.com/g/m8testofficial),
  官方视频教程可以到 [哔哩哔哩](https://space.bilibili.com/1588813179) 查看

## 配置设备信息

+ 设备IP: 调试设备(手机/模拟器/云机)的ip地址
+ Adb端口: 调试设备Adb调试端口
+ 调试端口: [M8Test 调试插件](https://github.com/m8test/debugger) 启动的端口
+ 启用 Adb 端口转发: 对于无法访问设备ip(模拟器)的可以开启此选项然后在操作面板中执行端口转发, 就可以将电脑本地端口和设备调试端口映射。

![settings.png](https://raw.githubusercontent.com/m8test/idea-plugin/refs/heads/main/images/settings.png)

## M8Test项目开发步骤

![operations.png](https://raw.githubusercontent.com/m8test/idea-plugin/refs/heads/main/images/operations.png)

1. 点击`连接设备`按钮连接调试设备
2. 点击`执行端口转发`按钮进行adb端口转发(可选)
3. 点击`启动scrcpy`将调试设备投屏到电脑(可选)
4. 点击`连接 websocket 日志`按钮将调试设备的日志同步到电脑端
5. 在电脑端修改代码后点击`推送并运行项目`按钮即可将本地修改内容同步到调试设备然后运行项目

<!-- Plugin description end -->

