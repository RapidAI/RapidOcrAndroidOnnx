package com.benjaminwan.ocr.java;

import static java.lang.Math.max;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.benjaminwan.ocr.java.databinding.ActivityMainBinding;
import com.benjaminwan.ocr.java.utils.BitmapUtils;
import com.benjaminwan.ocr.java.utils.ToastUtils;
import com.benjaminwan.ocrlibrary.OcrEngine;
import com.benjaminwan.ocrlibrary.OcrResult;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.orhanobut.logger.Logger;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener,
        EasyPermissions.PermissionCallbacks {

    private final int REQUEST_SELECT_IMAGE = 666;
    private final int REQUEST_PERMISSION = 6666;

    private ActivityMainBinding binding = null;

    private OcrEngine ocrEngine = null;

    private RequestOptions glideOptions = new RequestOptions().skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE);

    private Bitmap selectedImg = null;
    private OcrResult ocrResult = null;

    private Disposable detectJob = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ocrEngine = new OcrEngine(this.getApplicationContext());
        ocrEngine.setDoAngle(true);//相册识别时，默认启用文字方向检测
        initViews();
    }

    private void initViews() {
        binding.selectBtn.setOnClickListener(this);
        binding.detectBtn.setOnClickListener(this);
        binding.stopBtn.setOnClickListener(this);
        binding.stopBtn.setEnabled(false);
        binding.doAngleSw.setChecked(ocrEngine.getDoAngle());
        binding.mostAngleSw.setChecked(ocrEngine.getMostAngle());
        updatePadding(ocrEngine.getPadding());
        updateBoxScoreThresh((int) (ocrEngine.getBoxScoreThresh() * 100));
        updateBoxThresh((int) (ocrEngine.getBoxThresh() * 100));
        updateUnClipRatio((int) (ocrEngine.getUnClipRatio() * 10));
        binding.paddingSeekBar.setOnSeekBarChangeListener(this);
        binding.boxScoreThreshSeekBar.setOnSeekBarChangeListener(this);
        binding.boxThreshSeekBar.setOnSeekBarChangeListener(this);
        binding.maxSideLenSeekBar.setOnSeekBarChangeListener(this);
        binding.scaleUnClipRatioSeekBar.setOnSeekBarChangeListener(this);
        binding.doAngleSw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                ocrEngine.setDoAngle(isChecked);
                binding.mostAngleSw.setEnabled(isChecked);
            }
        });
        binding.mostAngleSw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                ocrEngine.setMostAngle(isChecked);
            }
        });
    }

    private void updateMaxSideLen(int progress) {
        float ratio = (float) progress / 100.f;
        if (selectedImg != null) {
            int maxSize = max(selectedImg.getWidth(), selectedImg.getHeight());
            int maxSizeLen = (int) (ratio * maxSize);
            String msg = "MaxSideLen:" + maxSizeLen + "(" + ratio * 100 + "%)";
            binding.maxSideLenTv.setText(msg);
        } else {
            String msg = "MaxSideLen:0(" + ratio * 100 + "%)";
            binding.maxSideLenTv.setText(msg);
        }
    }

    private void updatePadding(int progress) {
        binding.paddingTv.setText(String.format("Padding:%d", progress));
        ocrEngine.setPadding(progress);
    }

    private void updateBoxScoreThresh(int progress) {
        float thresh = (float) progress / 100.f;
        binding.boxScoreThreshTv.setText(String.format("框置信度门限:%f", thresh));
        ocrEngine.setBoxScoreThresh(thresh);
    }

    private void updateBoxThresh(int progress) {
        float thresh = (float) progress / 100.f;
        binding.boxThreshTv.setText(String.format("BoxThresh:%f", thresh));
        ocrEngine.setBoxThresh(thresh);
    }

    private void updateUnClipRatio(int progress) {
        float scale = (float) progress / 10.f;
        binding.unClipRatioTv.setText(String.format("框大小倍率:%f", scale));
        ocrEngine.setUnClipRatio(scale);
    }

    public void openSelector() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.selectBtn:
                EasyPermissions.requestPermissions(
                        new PermissionRequest.Builder(this, REQUEST_PERMISSION, Manifest.permission.READ_EXTERNAL_STORAGE)
                                .setRationale("请允许相关权限")
                                .setPositiveButtonText("确定")
                                .setNegativeButtonText("取消")
                                .build());
                break;
            case R.id.detectBtn:
                if (selectedImg == null) {
                    ToastUtils.showToast(MainActivity.this, "请先选择一张图片", Toast.LENGTH_SHORT);
                    return;
                }
                float ratio = (float) binding.maxSideLenSeekBar.getProgress() / 100.f;
                int maxSize = max(selectedImg.getWidth(), selectedImg.getHeight());
                int maxSideLen = (int) (ratio * maxSize);
                detectJob = detect(selectedImg, maxSideLen);
                break;
            case R.id.stopBtn:
                if (detectJob != null) {
                    detectJob.dispose();
                    detectJob = null;
                }
                clearLoading();
                ocrResult = null;
                break;
            default:
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.maxSideLenSeekBar:
                updateMaxSideLen(progress);
                break;
            case R.id.paddingSeekBar:
                updatePadding(progress);
                break;
            case R.id.boxScoreThreshSeekBar:
                updateBoxScoreThresh(progress);
                break;
            case R.id.boxThreshSeekBar:
                updateBoxThresh(progress);
                break;
            case R.id.scaleUnClipRatioSeekBar:
                updateUnClipRatio(progress);
                break;
            default:
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && data.getData() != null && resultCode == Activity.RESULT_OK && requestCode == REQUEST_SELECT_IMAGE) {
            Uri imgUri = data.getData();
            Glide.with(this).load(imgUri).apply(glideOptions).into(binding.imageView);
            try {
                selectedImg = BitmapUtils.decodeUri(MainActivity.this, imgUri);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            updateMaxSideLen(binding.maxSideLenSeekBar.getProgress());
            clearLastResult();
        }
    }

    private void showLoading() {
        Glide.with(this).load(R.drawable.loading_anim).into(binding.imageView);
    }

    private void clearLoading() {
        Glide.with(this).clear(binding.imageView);
    }

    private void clearLastResult() {
        binding.timeTV.setText("");
        ocrResult = null;
    }

    private Disposable detect(Bitmap img, int reSize) {
        return Single.fromCallable(new Callable<OcrResult>() {
                    @Override
                    public OcrResult call() throws Exception {
                        Bitmap boxImg = Bitmap.createBitmap(img.getWidth(), img.getHeight(), Bitmap.Config.ARGB_8888);
                        return ocrEngine.detect(img, boxImg, reSize);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Throwable {
                        showLoading();
                        binding.detectBtn.setEnabled(false);
                        binding.stopBtn.setEnabled(true);
                    }
                })
                .doOnDispose(new Action() {
                    @Override
                    public void run() throws Throwable {
                        binding.detectBtn.setEnabled(true);
                        binding.stopBtn.setEnabled(false);
                        clearLastResult();
                    }
                })
                .doFinally(new Action() {
                    @Override
                    public void run() throws Throwable {
                        binding.detectBtn.setEnabled(true);
                        binding.stopBtn.setEnabled(false);
                    }
                })
                .subscribe(new Consumer<OcrResult>() {
                    @Override
                    public void accept(OcrResult ocrResult) throws Throwable {
                        String time = String.format("识别时间:%d ms", (int) ocrResult.getDetectTime());
                        binding.timeTV.setText(time);
                        Glide.with(MainActivity.this)
                                .load(ocrResult.getBoxImg())
                                .apply(glideOptions)
                                .into(binding.imageView);
                        Logger.i(ocrResult.toString());
                        ToastUtils.showToast(MainActivity.this, ocrResult.getStrRes(), Toast.LENGTH_SHORT);
                    }
                });

    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        openSelector();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        ToastUtils.showToast(this, "权限申请被拒绝!", Toast.LENGTH_SHORT);
    }
}