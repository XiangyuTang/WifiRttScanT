<div align="center">
<img src="./Application/docs/imgs/logo.png" width = "130" height = "" />
</div>

# WifiRttScanT官方文档

![]( https://img.shields.io/badge/JAVA-1.8+-brightgreen.svg)
![]( https://img.shields.io/badge/AndroidStudio-bumblebee|2021.1.1Patch2-brightgreen.svg)

![]( https://img.shields.io/badge/build-passing-brightgreen.svg)
![]( https://img.shields.io/badge/deployment-passing-brightgreen.svg)


## 目录

* [WifiRttScanT官方文档]()
   * [目录](#目录)
   * [项目简介](#项目简介)
      * [项目定位](#项目定位)
      * [项目特点](#项目特点)
      * [软件功能](#软件功能)
   * [效果图](#效果图)
   * [安装说明](#安装说明)
      * [环境依赖](#环境依赖)
   * [使用说明](#使用说明)
      * [简要说明](#简要说明)
      * [具体说明](#具体说明)
   * [主要目录结构说明](#主要目录结构说明)
   * [关于作者](#关于作者)
   * [鸣谢](#鸣谢)
   * [版权信息](#版权信息)
   * [更新日志](#更新日志)

---


## 项目简介

本项目是基于[802.11mc协议的精确时间测量（Fine Time Measurement，FTM）](https://developer.android.google.cn/guide/topics/connectivity/wifi-rtt?hl=zh-cn)
原理开发的基于精确位置服务的室内定位导航一体化软件，该APP站在巨人的肩膀上[（即Google公司开源项目WifiRttScan）](https://github.com/googlearchive/android-WifiRttScan) 进行增量开发，
并且得益于MIT [Berthold K.P. Horn](https://people.csail.mit.edu/bkph/) 教授的思路及其对应的研究成果[WiFiRttScanX](http://people.csail.mit.edu/bkph/WifiRttScanX) ，
实现了支持802.11mc协议FTM功能的AP "凡扫描，皆响应"（指厂商在制造AP时可能不会显式宣传支持FTM）。


### 项目定位
项目服务于室内定位、室内导航领域，
瞄准了当今市场室内定位领域设备部署成本高、定位精度差的痛点，
目标是基于802.11mc的WI-FI精确时间测量原理，利用市场上现有部署的支持FTM响应功能的AP，实现米级精度的室内定位。

### 项目特点
1. 室内实时定位
2. 引入科大讯飞SDK，支持语音导航播报
3. 结合高德地图SDK，支持室内外地图定位无缝衔接

### 软件功能
1. 扫描到所有支持FTM功能的AP
2. 室内定位
3. 室内导航

## 效果图
1. AP扫描结果
   
    <img src="./Application/docs/imgs/扫描结果snapshot.png" width="300px" />

2. 室内地图展示
   
   [查看演示视频](./Application/docs/videos/室内地图Demo.mp4)

   <img src="./Application/docs/imgs/室内地图snapshot.png" width="300px" />


3. 实时定位过程

   [查看演示视频](./Application/docs/videos/定位效果演示.mp4)
   
    <img src="./Application/docs/imgs/定位过程snapshot.png" width="300px" />

    <video id="video" controls="" preload="none" poster="定位过程">
       <source id="mp4" src="./Application/docs/videos/定位效果演示.mp4" type="video/mp4">
    </video>

4. 导航路径规划

    <img src="./Application/docs/imgs/路径规划snapshot.png" width="300px" />
   
5. 导航过程

    <img src="./Application/docs/imgs/导航过程snapshot.png" width="300px" />
   
## 安装说明


### 环境依赖

第三方相关JAR包依赖：(在Application/libs/目录下已下载完毕)

高德地图：[AMap_Location_V5.6.1_20211115.jar](https://amappc.cn-hangzhou.oss-pub.aliyun-inc.com/lbs/static/zip/AMap_Android_Location_SDK_All.zip)

定位算法：[commons-math3-3.6.1.jar](https://repo1.maven.org/maven2/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar) 、[trilateration-1.0.2.jar](https://github-releases.githubusercontent.com/24968636/9c6dc64c-34ac-11e7-903a-cac48fb33a35?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIWNJYAX4CSVEH53A%2F20211116%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20211116T044150Z&X-Amz-Expires=300&X-Amz-Signature=ee7e4757a609dd37b101ba8d2430d5f6216218657b332b999fe830444bc5d522&X-Amz-SignedHeaders=host&actor_id=47786354&key_id=0&repo_id=24968636&response-content-disposition=attachment%3B%20filename%3Dtrilateration-1.0.2.jar&response-content-type=application%2Foctet-stream) 

蜂鸟云地图：[fengmap.jar](https://source.fengmap.com/sdk/android/v2.5.10/FengMap_SDK_for_Android_v2.5.10.zip) 

科大讯飞语音：[Msc.jar](https://sdk-download.xfyun.cn/57673ee1/1637649687417-3ec35bc2-33a5-4f9b-a84f-549b3b4bc3fb/Android_tts_online1142_57673ee1.zip)

## 使用说明
### 简要说明

安卓开发者直接clone导入项目即可。

### 具体说明
略

## 主要目录结构说明
```
├── Application
│    ├── docs
│    │    ├── README.md
│    │    └── imgs
│    ├── libs //相关的jar包依赖
│    │    ├── AMap_Location_V5.6.1_20211115.jar
│    │    ├── Msc.jar
│    │    ├── commons-math3-3.6.1.jar
│    │    ├── fengmap.jar
│    │    ├── mysql-connector-java-5.0.7.jar
│    │    ├── trilateration-1.0.2.jar
│    └── src
│        └── main
│            ├── AndroidManifest.xml
│            ├── assets
│            └── java
│                └── com
│                    └── example
│                        └── android
│                            └── wifirttscan
│                                ├── APLocationVisualizationActivity.java
│                                ├── AccessPointRangingResultsActivity.java
│                                ├── LocationPermissionRequestActivity.java
│                                ├── MainActivity.java
│                                ├── MyAdapter.java
│                                ├── entity
│                                │    ├── ApInfo.java
│                                │    ├── ApRangingHistoryInfo.java
│                                │    └── LocationInfo.java
│                                ├── utils
│                                │    ├── ConvertUtils.java
│                                │    ├── DBOpenHelper.java
│                                │    ├── DeviceInfoHelper.java
│                                │    ├── FMCoordTransformer.java
│                                │    ├── FileUtils.java
│                                │    ├── LocationAlgorithm.java
│                                │    ├── ViewHelper.java
│                                │    └── ZipUtils.java
│                                └── widget
│                                    ├── ImageViewCheckBox.java
│                                    └── NavigationBar.java
├── README.md   
├── build.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── local.properties
├── settings.gradle
└── tree.txt
```


## 关于作者
作者姓名：唐翔宇

联系方式：1661690249@qq.com

## 鸣谢
该项目参考了Google的WiFiRttScan app开源代码

灵感来源于Google WifiRttScan和MIT WifiRttScanX。

感谢实验室老师和同学的支持和帮助。


## 版权信息
该项目签署了MIT 授权许可，详情请参阅 LICENSE.md

## 更新日志
### V1.0.9 版本，2022-04-27
1. 优化：项目架构，添加Mysql数据库服务器（本地测试通过）
2. 新功能：可通过数据库下发AP位置信息，可上传用户定位信息，设备IP，MAC地址至后台数据库

### V1.0.8 版本，2022-03-07
1. 导航功能初步完成
2. 问题：方位角回显不准确

### V1.0.7 版本，2022-02-25
1. 优化：优化定位点Marker方位角的更新状态，随着高德地图SDK每秒更新一次
2. 新功能：导航过程中路径偏移重新规划
3. 问题：发现高德定位SDK在一段时间后，方位角就不会更新了，像用的之前的缓存
4. 问题：定位经过一段时间后，会重复性报错：至少需要X个支持FTM测量的AP才能定位，mFTMCapableAPs.size():X，此时页面上定位点不再有任何响应，怀疑跟mFTMCapableAPs有关系，异步onRangingResult更改全局变量

### V1.0.6 版本，2022-02-05
1. 优化：减少周期性扫描周边AP状态次数，优化为当位置点与10s前的位置点发生较大位移时再进行扫描
2. 优化：新增[mean_shift](https://blog.csdn.net/github_36923418/article/details/84640908) 聚类算法，将最近10次定位点的最密集处作为结果，减少漂移概率

### V1.0.5 版本，2022-01-19
1. 修复：取消导航之后不能删掉约束后定位导航点和确认导航后多了一个定位点的问题

### V1.0.4 版本，2021-12-28
1. 导航功能架构设计初步完成（壳子）
2. 修复：上一版本的内存泄漏问题，问题定位在导航定位SDK并没有接入真实导航中
3. 问题：确认导航之后多了一个路径约束后的定位导航点，地图中变成了两个定位点，需要在导航过程中把真实定位点隐藏
4. 问题：取消导航之后不能删掉约束后定位导航点

### V1.0.3 版本，2021-11-28
1. 新功能：结合蜂鸟云室内导航实例demo<FMDemoNavigationAdvance.map.FMNavigationApplication>完善 确认导航 功能
2. 新功能：接入科大讯飞语音合成SDK，可进行文字转语音播报
3. 问题：导航过程出现Waiting for a blocking GC Alloc内存泄漏问题
4. 修复：在APLocationVisualizationActivity.removeAPImageMarker()删除AP图标方法导致的java.util.ConcurrentModificationException异常

### V1.0.2 版本，2021-11-26
1. 新功能：基于高德地图定位SDK，实现了定位点方位角随手机朝向进行自动更新
2. 新功能：实现定位可视化中FTM capable AP的自动定时刷新，取到最新鲜的WI-FI状态
   
### V1.0.1 版本，2021-11-24
1. 新功能：实现了根据初步定位结果设置导航起点，也可以随意点击地图任意一点设置终点，并计算总路径和预计时间
2. 新功能：加入了前端开始导航按钮功能，点击起终点后可进一步确认导航/取消导航返回

### V1.0.0 版本，2021-11-23
1. 新功能：实现基本定位可视化效果，定位算法使用[Levenberg-Marquardt算法](https://blog.csdn.net/liu14lang/article/details/53991897)
2. 问题：偶尔的定位漂移比较明显，当STA在AP分布中间位置的时候定位比较准
