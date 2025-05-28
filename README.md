<!-- Plugin description -->

# [M8Test](https://dev-docs.m8test.com) IntelliJ 插件

> Tips: 推荐使用M8Test官方提供的idea集成开发环境, 因为这是通过测试的, 其他环境可能会有未知的问题.

## 插件功能

1. 一键安装开发环境
2. 本地离线文档
3. 设备日志同步
4. 设备投屏
5. UI布局分析
6. 图色助手
7. 新建脚本项目

## 功能说明

在Tools菜单下找到M8Test可以看到所有M8Test支持的动作。

### 安装环境

安装包含了M8Test开发中常用的工具, 例如M8Test安装包、 离线文档、常用组件和语言插件、 python, node, jruby等语言环境,
安装好后可以不用再从网络下载, 就算没有魔法网络也可以正常开发脚本项目, 该压缩包会解压到m8test目录(`~/.m8test/`)

### 本地离线文档

插件集成了以下文档, 本地存储路径为 `~/.gradle/docs/`:

1. M8Test sdk文档: 可以查看所有M8Test app支持的java api
2. M8Test 开发文档: 开发M8Test脚本项目时的手册
3. [无障碍 sdk 文档](https://github.com/YumiMiyamoto/accessibility-release): 无障碍组件(YumiMiyamotoAccessibility)
   中支持的java api
4. [文字识别 sdk 文档](https://github.com/YumiMiyamoto/ocr-release): 文字识别组件(YumiMiyamotoOcr)中支持的java api
5. [图色 sdk 文档](https://github.com/YumiMiyamoto/opencv-release): 图色组件(YumiMiyamotoOpencv)中支持的java api
6. [adb 自动化 sdk 文档](https://github.com/YumiMiyamoto/scrcpy-release): adb自动化组件(YumiMiyamotoScrcpy)中支持的java
   api

### 连接日志服务

将设备端的日志输出同步到M8Test日志面板中

### 启动投屏

将设备投屏到电脑，可对设备进行控制。

### UI布局分析

分析已通过adb连接的设备的UI布局, 可结合无障碍组件使用。

### 图色助手

对设备进行截屏，之后对截图内容进行精准的裁剪操作，提取所需部分，并且能够方便地进行取色处理，获取准确的颜色信息 。

### 新建M8Test项目

`New` > `Project` > `M8Test` > `Groovy/Java/Javascript/Kotlin/Lua/Php/Python/Ruby` > `Next` > `输入项目名和目录` >
`Create`

<!-- Plugin description end -->