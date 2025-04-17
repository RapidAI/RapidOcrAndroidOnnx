# 编译说明

1. AndroidStudio 2022.1.1或以上；
2. NDK下载，在SDK Tools中下载，版本选最新版；
3. cmake 3.22.1，在SDK Tools中下载；
4. 整合好的范例工程自带了模型，在models文件夹中。模型下载：[link](https://github.com/RapidAI/RapidOcrAndroidOnnx/releases)

    ```text
    Project_RapidOcrAndroidOnnx-1.3.0/OcrLibrary/src/main/assets
        ├── ch_ppocr_mobile_v2.0_cls_infer.onnx
        ├── ch_PP-OCRv2_det_infer.onnx det二选一，推荐
        ├── ch_ppocr_server_v2.0_det_infer.onnx det二选一
        ├── ch_ppocr_mobile_v2.0_rec_infer.onnx rec二选一，推荐
        ├── ch_ppocr_server_v2.0_rec_infer.onnx rec二选一
        └── ppocr_keys_v1.txt
    ```

    - det模型用于分割文字块，有两种模型(server和mobile)，其中server体积大且较慢但效果好，mobile体积小且较快但效果差点。
   - cls模型用于检测文字方向，只有一种模型
   - rec模型用于文字识别，有两种模型(server和mobile)，其中server体积大且较慢但效果好，mobile体积小且较快但效果差点。

5. 下载opencv-mobile-3.4.15-android.7z，[下载地址](https://gitee.com/benjaminwan/ocr-lite-android-ncnn/attach_files/843219/download/opencv-mobile-3.4.15-android.7z)。解压后目录结构为

    ```text
    RapidOcrAndroidOnnx/OcrLibrary/src/sdk
        └── native
            ├── jni
            └── staticlibs
    ```

6. onnxruntime-1.14.0-android-shared.7z，[下载地址](https://github.com/RapidAI/OnnxruntimeBuilder/releases/tag/1.14.0)。 解压后目录结构为

    ```text
    RapidOcrAndroidOnnx/OcrLibrary/src/main/onnxruntime-shared
    ├── OnnxRuntimeWrapper.cmake
    ├── arm64-v8a
    ├── armeabi-v7a
    ├── include
    ├── x86
    └── x86_64
    ```

### 编译Release包

- mac/linux使用命令编译```./gradlew assembleRelease```
- win使用命令编译```gradlew.bat assembleRelease```
- 输出apk文件在app/build/outputs/apk

### 重新编译

删除项目根目录下的如下缓存文件夹

```text
.idea
build
app/build
OcrLibrary/.cxx
OcrLibrary/build
```
