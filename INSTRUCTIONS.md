# 使用说明

1. 从Release下载OcrLibrary-版本-release.aar，放到项目的app/libs路径

1. 编辑app/build.gradle

```groovy
dependencies {
    implementation fileTree(dir: "libs", include: ["*.jar", "*.aar"])
}
```

2. 实例化OcrEngine

```
OcrEngine ocrEngine = new OcrEngine(this.getApplicationContext());
```

3. 调用detect方法

```
OcrResult result = ocrEngine.detect(img, boxImg, reSize);
```