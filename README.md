# RapidOcrAndroidOnnx

[![Issue](https://img.shields.io/github/issues/RapidAI/RapidOcrAndroidOnnx.svg)](https://github.com/RapidAI/RapidOcrAndroidOnnx/issues)
[![Star](https://img.shields.io/github/stars/RapidAI/RapidOcrAndroidOnnx.svg)](https://github.com/RapidAI/RapidOcrAndroidOnnx)

<details open>
    <summary>目录</summary>

- [RapidOcrAndroidOnnx](#RapidOcrAndroidOnnx)
    - [联系方式](#联系方式)
    - [项目完整源码](#项目完整源码)
    - [APK下载](#APK下载)
    - [简介](#简介)
    - [总体说明](#总体说明)
    - [更新说明](#更新说明)
    - [编译说明](#编译说明)
    - [使用说明](#使用说明)
    - [项目结构](#项目结构)
    - [常见问题](#常见问题)
        - [输入参数说明](#输入参数说明)
    - [关于作者](#关于作者)
    - [版权声明](#版权声明)
    - [示例图](#示例图)
        - [IMEI识别](#IMEI识别)
        - [身份证识别](#身份证识别)
        - [车牌识别](#车牌识别)

</details>

## 联系方式

[QQ群](https://rapidai.github.io/RapidOCRDocs/main/communicate/#qq)

## 项目完整源码

* 整合好源码和依赖库的完整工程项目，可到Q群共享内下载或Release下载，以Project开头的压缩包文件为源码工程，例：Project_RapidOcrAndroidOnnx-版本号.7z
* 如果想自己折腾，则请继续阅读本说明

## APK下载

* 编译好的demo apk，可以在release中下载，或者Q群共享内下载，文件名例：RapidOcrAndroidOnnx-版本号-release.apk

## 简介

RapidOcr onnxruntime推理 for Android

onnxruntime框架[https://github.com/microsoft/onnxruntime](https://github.com/microsoft/onnxruntime)

新版：https://github.com/RapidAI/RapidOcrAndroidOnnxCompose

## 总体说明

1. 封装为独立的Library，可以编译为aar，作为模块来调用；
2. Native层以C++编写；
3. Demo App以Kotlin-JVM编写；
4. Android版与其它版本不同，包含了几个应用场景，包括相册识别、摄像头识别、手机IMEI号识别、摄像头身份证识别这几个功能页面；
5. opencv 3.4.15，使用了[opencv-mobile](https://github.com/nihui/opencv-mobile)的代码，编译脚本在script文件夹里；

## 更新说明
#### 2021-09-30 update v1.1.0

* opencv 3.4.15(特别说明：4.5.3也可以支持，如果换成cv4，minSdkVersion必须改为24)
* det模型更换为效果更好的ch_PP-OCRv2_det_infer
* 升级kotlin: 1.5.31
* 升级gradle: 7.0.2
* 升级各dependencies
* 使用kotlin coroutines代替rxjava

#### 2021-10-28 update v1.1.1

* 编辑build.gradle，把版本定义放到root build.gradle
* 编辑github workflow config，上传aar文件到release

#### 2022-03-03 update v1.1.2

* 升级gradle
* 升级各依赖库

#### 2022-10-21 update v1.2.0

* onnxruntime 1.12.1
* 升级各依赖库

#### 2023-02-16 update v1.3.0

* onnxruntime 1.14.0
* 升级各依赖库
* 相册识别和相机识别增加停止按钮
* 添加java demo

### [编译说明](./BUILD.md)

### [使用说明](./INSTRUCTIONS.md)

## 项目结构

```
RapidOcrAndroidOnnx
    ├── app               # demo app
    ├── capture           # 截图
    ├── common-aar        # app引用的aar库
    ├── keystore          # app签名密钥文件
    ├── OcrLibrary        # Ocr引擎库，包含Jni和C++代码
    └── scripts           # 编译脚本
```

## 常见问题

### 输入参数说明

请参考[Cpp项目说明](https://github.com/RapidAI/RapidOcrOnnx)

## 关于作者

* Android demo编写：[benjaminwan](https://github.com/benjaminwan)
* 模型来自：[PaddleOCR](https://github.com/PaddlePaddle/PaddleOCR)

## 版权声明

- OCR模型版权归[PaddleOCR](https://github.com/PaddlePaddle/PaddleOCR)所有；
- 其它工程代码版权归本仓库所有者所有；

## 示例图

#### IMEI识别

![avatar](capture/detect_IMEI.gif)

#### 身份证识别

![avatar](capture/detect_id_card.gif)

#### 车牌识别

![avatar](capture/detect_plate.gif)

