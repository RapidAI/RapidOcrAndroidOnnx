#include <opencv2/imgproc.hpp>
#include <opencv2/imgcodecs.hpp>
#include "OcrUtils.h"
#include "clipper.hpp"

double getCurrentTime() {
    return (static_cast<double>(cv::getTickCount())) / cv::getTickFrequency() * 1000;//单位毫秒
}

ScaleParam getScaleParam(cv::Mat &src, const float scale) {
    int srcWidth = src.cols;
    int srcHeight = src.rows;
    int dstWidth = int((float) srcWidth * scale);
    int dstHeight = int((float) srcHeight * scale);
    if (dstWidth % 32 != 0) {
        dstWidth = (dstWidth / 32 - 1) * 32;
        dstWidth = (std::max)(dstWidth, 32);
    }
    if (dstHeight % 32 != 0) {
        dstHeight = (dstHeight / 32 - 1) * 32;
        dstHeight = (std::max)(dstHeight, 32);
    }
    float scaleWidth = (float) dstWidth / (float) srcWidth;
    float scaleHeight = (float) dstHeight / (float) srcHeight;
    return {srcWidth, srcHeight, dstWidth, dstHeight, scaleWidth, scaleHeight};
}

ScaleParam getScaleParam(cv::Mat &src, const int targetSize) {
    int srcWidth, srcHeight, dstWidth, dstHeight;
    srcWidth = dstWidth = src.cols;
    srcHeight = dstHeight = src.rows;

    float scale = 1.f;
    if (dstWidth > dstHeight) {
        scale = (float) targetSize / (float) dstWidth;
        dstWidth = targetSize;
        dstHeight = int((float) dstHeight * scale);
    } else {
        scale = (float) targetSize / (float) dstHeight;
        dstHeight = targetSize;
        dstWidth = int((float) dstWidth * scale);
    }
    if (dstWidth % 32 != 0) {
        dstWidth = (dstWidth / 32 - 1) * 32;
        dstWidth = (std::max)(dstWidth, 32);
    }
    if (dstHeight % 32 != 0) {
        dstHeight = (dstHeight / 32 - 1) * 32;
        dstHeight = (std::max)(dstHeight, 32);
    }
    float scaleWidth = (float) dstWidth / (float) srcWidth;
    float scaleHeight = (float) dstHeight / (float) srcHeight;
    return {srcWidth, srcHeight, dstWidth, dstHeight, scaleWidth, scaleHeight};
}

cv::RotatedRect getPartRect(std::vector<cv::Point> &box, float scaleWidth, float scaleHeight) {
    cv::RotatedRect rect = cv::minAreaRect(box);
    int minSize = rect.size.width > rect.size.height ? rect.size.height : rect.size.width;
    if (rect.size.width > rect.size.height) {
        rect.size.width = rect.size.width + (float) minSize * scaleWidth;
        rect.size.height = rect.size.height + (float) minSize * scaleHeight;
    } else {
        rect.size.width = rect.size.width + (float) minSize * scaleHeight;
        rect.size.height = rect.size.height + (float) minSize * scaleWidth;
    }
    return rect;
}

int getThickness(cv::Mat &boxImg) {
    int minSize = boxImg.cols > boxImg.rows ? boxImg.rows : boxImg.cols;
    int thickness = minSize / 1000 + 2;
    return thickness;
}

std::vector<cv::Point2f> getBox(const cv::RotatedRect &rect) {
    cv::Point2f vertices[4];
    rect.points(vertices);
    //std::vector<cv::Point2f> ret(4);
    std::vector<cv::Point2f> ret2(vertices, vertices + sizeof(vertices) / sizeof(vertices[0]));
    //memcpy(vertices, &ret[0], ret.size() * sizeof(ret[0]));
    return ret2;
}

void drawTextBox(cv::Mat &boxImg, cv::RotatedRect &rect, int thickness) {
    cv::Point2f vertices[4];
    rect.points(vertices);
    for (int i = 0; i < 4; i++)
        cv::line(boxImg, vertices[i], vertices[(i + 1) % 4], cv::Scalar(0, 0, 255), thickness);
    //cv::polylines(srcmat, textpoint, true, cv::Scalar(0, 255, 0), 2);
}

void drawTextBox(cv::Mat &boxImg, const std::vector<cv::Point> &box, int thickness) {
    auto color = cv::Scalar(0, 0, 255);// B(0) G(0) R(255)
    cv::line(boxImg, box[0], box[1], color, thickness);
    cv::line(boxImg, box[1], box[2], color, thickness);
    cv::line(boxImg, box[2], box[3], color, thickness);
    cv::line(boxImg, box[3], box[0], color, thickness);
}

void drawTextBoxes(cv::Mat &boxImg, std::vector<TextBox> &textBoxes, int thickness) {
    for (int i = 0; i < textBoxes.size(); ++i) {
        drawTextBox(boxImg, textBoxes[i].boxPoint, thickness);
    }
}

cv::Mat matRotateClockWise180(cv::Mat src) {
    flip(src, src, 0);
    flip(src, src, 1);
    return src;
}

cv::Mat matRotateClockWise90(cv::Mat src) {
    transpose(src, src);
    flip(src, src, 1);
    return src;
}

cv::Mat GetRotateCropImage(const cv::Mat &src, std::vector<cv::Point> box) {
    cv::Mat image;
    src.copyTo(image);
    std::vector<cv::Point> points = box;

    int collectX[4] = {box[0].x, box[1].x, box[2].x, box[3].x};
    int collectY[4] = {box[0].y, box[1].y, box[2].y, box[3].y};
    int left = int(*std::min_element(collectX, collectX + 4));
    int right = int(*std::max_element(collectX, collectX + 4));
    int top = int(*std::min_element(collectY, collectY + 4));
    int bottom = int(*std::max_element(collectY, collectY + 4));

    cv::Mat imgCrop;
    image(cv::Rect(left, top, right - left, bottom - top)).copyTo(imgCrop);

    for (int i = 0; i < points.size(); i++) {
        points[i].x -= left;
        points[i].y -= top;
    }

    int imgCropWidth = int(sqrt(pow(points[0].x - points[1].x, 2) +
                                pow(points[0].y - points[1].y, 2)));
    int imgCropHeight = int(sqrt(pow(points[0].x - points[3].x, 2) +
                                 pow(points[0].y - points[3].y, 2)));

    cv::Point2f ptsDst[4];
    ptsDst[0] = cv::Point2f(0., 0.);
    ptsDst[1] = cv::Point2f(imgCropWidth, 0.);
    ptsDst[2] = cv::Point2f(imgCropWidth, imgCropHeight);
    ptsDst[3] = cv::Point2f(0.f, imgCropHeight);

    cv::Point2f ptsSrc[4];
    ptsSrc[0] = cv::Point2f(points[0].x, points[0].y);
    ptsSrc[1] = cv::Point2f(points[1].x, points[1].y);
    ptsSrc[2] = cv::Point2f(points[2].x, points[2].y);
    ptsSrc[3] = cv::Point2f(points[3].x, points[3].y);

    cv::Mat M = cv::getPerspectiveTransform(ptsSrc, ptsDst);

    cv::Mat partImg;
    cv::warpPerspective(imgCrop, partImg, M,
                        cv::Size(imgCropWidth, imgCropHeight),
                        cv::BORDER_REPLICATE);

    if (float(partImg.rows) >= float(partImg.cols) * 1.5) {
        cv::Mat srcCopy = cv::Mat(partImg.rows, partImg.cols, partImg.depth());
        cv::transpose(partImg, srcCopy);
        cv::flip(srcCopy, srcCopy, 0);
        return srcCopy;
    } else {
        return partImg;
    }
}

cv::Mat adjustTargetImg(cv::Mat &src, int dstWidth, int dstHeight) {
    cv::Mat srcResize;
    float scale = (float) dstHeight / (float) src.rows;
    int angleWidth = int((float) src.cols * scale);
    cv::resize(src, srcResize, cv::Size(angleWidth, dstHeight));
    cv::Mat srcFit = cv::Mat(dstHeight, dstWidth, CV_8UC3, cv::Scalar(255, 255, 255));
    if (angleWidth < dstWidth) {
        cv::Rect rect(0, 0, srcResize.cols, srcResize.rows);
        srcResize.copyTo(srcFit(rect));
    } else {
        cv::Rect rect(0, 0, dstWidth, dstHeight);
        srcResize(rect).copyTo(srcFit);
    }
    return srcFit;
}

bool cvPointCompare(cv::Point a, cv::Point b) {
    return a.x < b.x;
}

std::vector<cv::Point2f> getMinBoxes(const cv::RotatedRect &boxRect, float &maxSideLen) {
    maxSideLen = std::max(boxRect.size.width, boxRect.size.height);
    std::vector<cv::Point2f> boxPoint = getBox(boxRect);
    std::sort(boxPoint.begin(), boxPoint.end(), cvPointCompare);
    int index1, index2, index3, index4;
    if (boxPoint[1].y > boxPoint[0].y) {
        index1 = 0;
        index4 = 1;
    } else {
        index1 = 1;
        index4 = 0;
    }
    if (boxPoint[3].y > boxPoint[2].y) {
        index2 = 2;
        index3 = 3;
    } else {
        index2 = 3;
        index3 = 2;
    }
    std::vector<cv::Point2f> minBox(4);
    minBox[0] = boxPoint[index1];
    minBox[1] = boxPoint[index2];
    minBox[2] = boxPoint[index3];
    minBox[3] = boxPoint[index4];
    return minBox;
}

float boxScoreFast(const std::vector<cv::Point2f> &boxes, const cv::Mat &pred) {
    int width = pred.cols;
    int height = pred.rows;

    float arrayX[4] = {boxes[0].x, boxes[1].x, boxes[2].x, boxes[3].x};
    float arrayY[4] = {boxes[0].y, boxes[1].y, boxes[2].y, boxes[3].y};

    int minX = clamp(int(std::floor(*(std::min_element(arrayX, arrayX + 4)))), 0, width - 1);
    int maxX = clamp(int(std::ceil(*(std::max_element(arrayX, arrayX + 4)))), 0, width - 1);
    int minY = clamp(int(std::floor(*(std::min_element(arrayY, arrayY + 4)))), 0, height - 1);
    int maxY = clamp(int(std::ceil(*(std::max_element(arrayY, arrayY + 4)))), 0, height - 1);

    cv::Mat mask = cv::Mat::zeros(maxY - minY + 1, maxX - minX + 1, CV_8UC1);

    cv::Point box[4];
    box[0] = cv::Point(int(boxes[0].x) - minX, int(boxes[0].y) - minY);
    box[1] = cv::Point(int(boxes[1].x) - minX, int(boxes[1].y) - minY);
    box[2] = cv::Point(int(boxes[2].x) - minX, int(boxes[2].y) - minY);
    box[3] = cv::Point(int(boxes[3].x) - minX, int(boxes[3].y) - minY);
    const cv::Point *pts[1] = {box};
    int npts[] = {4};
    cv::fillPoly(mask, pts, npts, 1, cv::Scalar(1));

    cv::Mat croppedImg;
    pred(cv::Rect(minX, minY, maxX - minX + 1, maxY - minY + 1))
            .copyTo(croppedImg);

    auto score = cv::mean(croppedImg, mask)[0];
    return score;
}

float getContourArea(const std::vector<cv::Point2f> &box, float unClipRatio) {
    int size = box.size();
    float area = 0.0f;
    float dist = 0.0f;
    for (int i = 0; i < size; i++) {
        area += box[i].x * box[(i + 1) % size].y -
                box[i].y * box[(i + 1) % size].x;
        dist += sqrtf((box[i].x - box[(i + 1) % size].x) *
                      (box[i].x - box[(i + 1) % size].x) +
                      (box[i].y - box[(i + 1) % size].y) *
                      (box[i].y - box[(i + 1) % size].y));
    }
    area = fabs(float(area / 2.0));

    return area * unClipRatio / dist;
}

cv::RotatedRect unClip(std::vector<cv::Point2f> box, float unClipRatio) {
    float distance = getContourArea(box, unClipRatio);

    ClipperLib::ClipperOffset offset;
    ClipperLib::Path p;
    p << ClipperLib::IntPoint(int(box[0].x), int(box[0].y))
      << ClipperLib::IntPoint(int(box[1].x), int(box[1].y))
      << ClipperLib::IntPoint(int(box[2].x), int(box[2].y))
      << ClipperLib::IntPoint(int(box[3].x), int(box[3].y));
    offset.AddPath(p, ClipperLib::jtRound, ClipperLib::etClosedPolygon);

    ClipperLib::Paths soln;
    offset.Execute(soln, distance);
    std::vector<cv::Point2f> points;

    for (int j = 0; j < soln.size(); j++) {
        for (int i = 0; i < soln[soln.size() - 1].size(); i++) {
            points.emplace_back(soln[j][i].X, soln[j][i].Y);
        }
    }
    cv::RotatedRect res;
    if (points.empty()) {
        res = cv::RotatedRect(cv::Point2f(0, 0), cv::Size2f(1, 1), 0);
    } else {
        res = cv::minAreaRect(points);
    }
    return res;
}

std::vector<float> substractMeanNormalize(cv::Mat &src, const float *meanVals, const float *normVals) {
    auto inputTensorSize = src.cols * src.rows * src.channels();
    std::vector<float> inputTensorValues(inputTensorSize);
    size_t numChannels = src.channels();
    size_t imageSize = src.cols * src.rows;

    for (size_t pid = 0; pid < imageSize; pid++) {
        for (size_t ch = 0; ch < numChannels; ++ch) {
            float data = (float) (src.data[pid * numChannels + ch] * normVals[ch] - meanVals[ch] * normVals[ch]);
            inputTensorValues[ch * imageSize + pid] = data;
        }
    }
    return inputTensorValues;
}

std::vector<int> getAngleIndexes(std::vector<Angle> &angles) {
    std::vector<int> angleIndexes;
    angleIndexes.reserve(angles.size());
    for (int i = 0; i < angles.size(); ++i) {
        angleIndexes.push_back(angles[i].index);
    }
    return angleIndexes;
}

std::vector<char *> getInputNames(Ort::Session *session) {
    Ort::AllocatorWithDefaultOptions allocator;
    size_t numInputNodes = session->GetInputCount();
    std::vector<char *> inputNodeNames(numInputNodes);
    std::vector<int64_t> inputNodeDims;

    //printf("Number of inputs = %zu\n", numInputNodes);

    for (int i = 0; i < numInputNodes; i++) {
        // print input node names
        char *inputName = session->GetInputName(i, allocator);
        printf("InputName[%d]=%s\n", i, inputName);
        inputNodeNames[i] = inputName;

        // print input node types
        //Ort::TypeInfo typeInfo = session->GetInputTypeInfo(i);
        //auto tensorInfo = typeInfo.GetTensorTypeAndShapeInfo();

        //ONNXTensorElementDataType type = tensorInfo.GetElementType();
        //printf("Input[%d] type=%d\n", i, type);

        // print input shapes/dims
        //inputNodeDims = tensorInfo.GetShape();
        //printf("Input[%d] num_dims=%zu\n", i, inputNodeDims.size());
        /*for (int j = 0; j < inputNodeDims.size(); j++)
            printf("Input[%d] dim%d=%jd\n", i, j, inputNodeDims[j]);*/
    }
    return inputNodeNames;
}

std::vector<char *> getOutputNames(Ort::Session *session) {
    Ort::AllocatorWithDefaultOptions allocator;
    size_t numOutputNodes = session->GetOutputCount();
    std::vector<char *> outputNodeNames(numOutputNodes);
    //std::vector<int64_t> outputNodeDims;

    //printf("Number of outputs = %zu\n", numOutputNodes);

    for (int i = 0; i < numOutputNodes; i++) {
        // print input node names
        char *outputName = session->GetOutputName(i, allocator);
        printf("OutputName[%d]=%s\n", i, outputName);
        outputNodeNames[i] = outputName;

        // print input node types
        //Ort::TypeInfo type_info = session->GetOutputTypeInfo(i);
        //auto tensorInfo = type_info.GetTensorTypeAndShapeInfo();

        //ONNXTensorElementDataType type = tensorInfo.GetElementType();
        //printf("Output %d : type=%d\n", i, type);

        // print input shapes/dims
        //outputNodeDims = tensorInfo.GetShape();
        //printf("Output %d : num_dims=%zu\n", i, outputNodeDims.size());
        /*for (int j = 0; j < outputNodeDims.size(); j++)
            printf("Output %d : dim %d=%jd\n", i, j, outputNodeDims[j]);*/
    }
    return outputNodeNames;
}

void *getModelDataFromAssets(AAssetManager *mgr, const char *modelName, int &size) {
    if (mgr == NULL) {
        LOGE(" %s", "AAssetManager==NULL");
        return NULL;
    }
    AAsset *asset = AAssetManager_open(mgr, modelName, AASSET_MODE_UNKNOWN);
    if (asset == NULL) {
        LOGE(" %s", "asset==NULL");
        return NULL;
    }
    off_t bufferSize = AAsset_getLength(asset);
    void *modelData = malloc(bufferSize + 1);
    size = AAsset_read(asset, modelData, bufferSize);
    AAsset_close(asset);
    LOGI("model=%s, numBytesRead=%d", modelName, size);
    return modelData;
}

std::string jstringTostring(JNIEnv *env, jstring input) {
    char *str = NULL;
    jclass clsstring = env->FindClass("java/lang/String");
    jstring strencode = env->NewStringUTF("utf-8");
    jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
    jbyteArray barr = (jbyteArray) env->CallObjectMethod(input, mid, strencode);
    jsize alen = env->GetArrayLength(barr);
    jbyte *ba = env->GetByteArrayElements(barr, JNI_FALSE);
    if (alen > 0) {
        str = (char *) malloc(alen + 1);
        memcpy(str, ba, alen);
        str[alen] = 0;
    }
    env->ReleaseByteArrayElements(barr, ba, 0);
    std::string ret = str;
    return ret;
}