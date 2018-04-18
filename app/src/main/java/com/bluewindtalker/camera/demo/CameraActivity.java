package com.bluewindtalker.camera.demo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

/**
 * @author bluewindtalker
 * @description 相机demo
 * @date 2018/4/14-下午12:08
 */

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = this.getClass().getSimpleName();
    /**
     * 预览控件
     */
    private SurfaceView displaySfv;

    /**
     * 拍照按钮
     */
    private Button takePicBtn;

    /**
     * 照片控件容器
     */
    private FrameLayout picFl;

    /**
     * 照片容器
     */
    private ImageView picIV;

    /**
     * 提示开灯的tv
     */
    private TextView lightTV;

    private Camera camera;

    private boolean isRequestPermission = false;

    /**
     * 获得传感器管理者
     */
    private SensorManager sensorManager;

    /**
     * 当前打开摄像头类型，当前写的是后置摄像头
     */
    private int currentCameraType = Camera.CameraInfo.CAMERA_FACING_BACK;

    //需要申请的权限
    private String[] permissions = {Manifest.permission.CAMERA};
    private static final int CAMERA_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        displaySfv = findViewById(R.id.sf_display_demo_camera);
        takePicBtn = findViewById(R.id.btn_take_picture_demo_camera);
        picFl = findViewById(R.id.fl_picture_demo_camera);
        picIV = findViewById(R.id.iv_picture_demo_camera);
        lightTV = findViewById(R.id.tv_light_demo_camera);
        takePicBtn.setOnClickListener(this);
        picFl.setOnClickListener(this);

        sensorManager = LightSensorUtil.getSenosrManager(this);
    }

    private SensorEventListener lightSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                //光线强度
                float lux = event.values[0];
                Log.e(TAG, "光线传感器得到的光线强度-->" + lux);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        LightSensorUtil.registerLightSensor(sensorManager, lightSensorListener);

        if (!isRequestPermission) {
            checkAndInitCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LightSensorUtil.unregisterLightSensor(sensorManager, lightSensorListener);

        Log.e(TAG, "onPause");
        releaseCamera();

    }

    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_take_picture_demo_camera) {
            takePicture();
            takePicBtn.setVisibility(View.GONE);
        } else if (v.getId() == R.id.fl_picture_demo_camera) {
            takePicBtn.setVisibility(View.VISIBLE);
            picFl.setVisibility(View.GONE);
            picIV.setImageBitmap(null);
            checkAndInitCamera();
        }
    }

    private void checkAndInitCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 检查该权限是否已经获取
            int i = ContextCompat.checkSelfPermission(this, permissions[0]);
            // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
            if (i != PackageManager.PERMISSION_GRANTED) {
                // 如果没有授予该权限，就去提示用户请求
                isRequestPermission = true;
                ActivityCompat.requestPermissions(this, permissions, CAMERA_PERMISSION_CODE);
            } else {
                initCamera();
            }
        } else {
            initCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.e(TAG, "onRequestPermissionsResult");
        if (requestCode == CAMERA_PERMISSION_CODE) {
            boolean isGranted = false;
            if (grantResults != null && grantResults.length > 0) {
                isGranted = true;
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        isGranted = false;
                        break;
                    }
                }
            }
            if (isGranted) {
                initCamera();
            } else {
                Toast.makeText(this, "请去设置授权", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 初始化照片
     */
    private void initCamera() {
        if (camera != null) {
            camera.startPreview();
            setPreviewLight();
        }
        Log.e(TAG, "initCamera");
        //1. Obtain an instance of Camera from open(int).
        //这里可以根据前后摄像头设置
        camera = openCamera(currentCameraType);
        if (camera == null) {
            return;
        }
        //2. Get existing (default) settings with getParameters().
        //获得存在的默认配置属性
        Camera.Parameters parameters = camera.getParameters();

        //3. If necessary, modify the returned Camera.Parameters object and call setParameters(Camera.Parameters).
        //可以根据需要修改属性，这些属性包括是否自动持续对焦、拍摄的gps信息、图片视频格式及大小、预览的fps、
        // 白平衡和自动曝光补偿、自动对焦区域、闪光灯状态等。
        //具体可以参阅https://developer.android.com/reference/android/hardware/Camera.Parameters.html
        if (parameters.getSupportedFocusModes().contains(Camera.Parameters
                .FOCUS_MODE_CONTINUOUS_PICTURE)) {
            //自动持续对焦
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        //在设置图片和预览的大小时要注意当前摄像头支持的大小，不同手机支持的大小不同，如果你的SurfaceView不是全屏，有可能被拉伸。
        // parameters.getSupportedPreviewSizes(),parameters.getSupportedPictureSizes()
        List<Camera.Size> picSizes = parameters.getSupportedPictureSizes();
        Resources resources = this.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        float density = dm.density;
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        Camera.Size picSize = getPictureSize(picSizes, width, height);
        parameters.setPictureSize(picSize.width, picSize.height);
        camera.setParameters(parameters);
        //4. Call setDisplayOrientation(int) to ensure correct orientation of preview.
        //你可能会遇到画面方向和手机的方向不一致的问题，竖向手机的时候，但是画面是横的，这是由于摄像头默认捕获的画面横向的
        // 通过调用setDisplayOrientation来设置PreviewDisplay的方向，可以解决这个问题。
        setCameraDisplayOrientation(this, currentCameraType, camera);

        //5. Important: Pass a fully initialized SurfaceHolder to setPreviewDisplay(SurfaceHolder).
        // Without a surface, the camera will be unable to start the preview.
        //camera必须绑定一个surfaceview才可以正常显示。
        try {
            camera.setPreviewDisplay(displaySfv.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //6. Important: Call startPreview() to start updating the preview surface.
        // Preview must be started before you can take a picture.
        //在调用拍照之前必须调用startPreview()方法,但是在此时有可能surface还未创建成功。
        // 所以加上SurfaceHolder.Callback()，在回调再次初始化下。
        camera.startPreview();
        setPreviewLight();
        //7. When you want, call
        // takePicture(Camera.ShutterCallback, Camera.PictureCallback, Camera.PictureCallback, Camera.PictureCallback)
        // to capture a photo. Wait for the callbacks to provide the actual image data.
        //当如果想要拍照的时候，调用takePicture方法，这个下面我们会讲到。

        //8. After taking a picture, preview display will have stopped. To take more photos, call startPreview() again first.
        //在拍照结束后相机预览将会关闭，如果要再次拍照需要再次调用startPreview（)

        //9. Call stopPreview() to stop updating the preview surface.
        //通过调用stopPreview方法可以结束预览
        //10. Important: Call release() to release the camera for use by other applications.
        // Applications should release the camera immediately in onPause()(and re-open() it in onResume()).
        //建议在onResume调用open的方法，在onPause的时候执行release方法

        SurfaceHolder holder = displaySfv.getHolder();
        if (holder != null) {
            holder.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    Log.e(TAG, "surfaceCreated" + holder);
                    checkAndInitCamera();

                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    Log.e(TAG, "surfaceChanged" + holder);
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    Log.e(TAG, "surfaceDestroyed" + holder);
                }
            });
        }
    }

    /**
     * 获得最合是的宽高size
     */
    private Camera.Size getPictureSize(List<Camera.Size> picSizes, int width, int height) {
        Camera.Size betterSize = null;
        int diff = Integer.MAX_VALUE;
        if (picSizes != null && picSizes.size() > 0) {
            for (Camera.Size size : picSizes) {
                int newDiff = Math.abs(size.width - width) + Math.abs(size.height - height);
                if (newDiff == 0) {
                    return size;
                }
                if (newDiff < diff) {
                    betterSize = size;
                    diff = newDiff;
                }
            }
        }
        return betterSize;
    }

    private Camera openCamera(int type) {
        int cameraTypeIndex = -1;
        int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int cameraIndex = 0; cameraIndex < cameraCount; cameraIndex++) {
            Camera.getCameraInfo(cameraIndex, info);
            if (info.facing == type) {
                cameraTypeIndex = cameraIndex;
                break;
            }
        }
        if (cameraTypeIndex != -1) {
            return Camera.open(cameraTypeIndex);
        }
        return null;
    }

    /**
     * 拍摄照片
     */
    private void takePicture() {
        picIV.setImageBitmap(null);
        if (camera == null) {
            return;
        }
        //如果不加第一个回调，手机会没有拍照音效，第二个回调是返回raw格式图片，
        // 了解过相机的人可能知道这是原图的意思，这个我们不处理，我们处理第三个回调，jpg格式的数据
        // 拍摄照片
        camera.takePicture(new Camera.ShutterCallback() {
            @Override
            public void onShutter() {
            }
        }, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                // 将拍照数据data数组转化为Bitmap，这里应该放到线程执行了，这里为了简单处理直接放UI线程了
                Bitmap imageBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                //一般手机需要旋转90度来适应方向，如果setCameraDisplayOrientation得到的结果不是90度，一般还需要再次旋转180
                picIV.setImageBitmap(rotate(imageBitmap, 90));
                picFl.setVisibility(View.VISIBLE);
            }
        });
    }

    public Bitmap rotate(Bitmap bitmap, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
    }

    //设置相机的方向
    public int setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                degrees = 0;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;   // compensate the mirror
        } else {
            // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
        return degrees;
    }


    //上次记录的时间戳
    long lastRecordTime = System.currentTimeMillis();

    //上次记录的索引
    int darkIndex = 0;
    //一个历史记录的数组，255是代表亮度最大值
    long[] darkList = new long[]{255, 255, 255, 255};
    //扫描间隔
    int waitScanTime = 300;

    //亮度低的阀值
    int darkValue = 60;
    private void setPreviewLight() {
        //不需要的时候直接清空
//        if(noNeed){
//            camera.setPreviewCallback(null);
//            return;
//        }
        camera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastRecordTime < waitScanTime) {
                    return;
                }
                lastRecordTime = currentTime;

                int width = camera.getParameters().getPreviewSize().width;
                int height = camera.getParameters().getPreviewSize().height;
                //像素点的总亮度
                long pixelLightCount = 0L;
                //像素点的总数
                long pixeCount = width * height;
                //采集步长，因为没有必要每个像素点都采集，可以跨一段采集一个，减少计算负担，必须大于1。
                int step = 10;
                //data.length - allCount * 1.5f的目的是判断图像格式是不是YUV420格式，只有是这种格式才相等
                //因为int整形与float浮点直接比较会出问题，所以这么比
                if (Math.abs(data.length - pixeCount * 1.5f) < 0.00001f) {
                    for (int i = 0; i < pixeCount; i += step) {
                        //如果直接加是不行的，因为data[i]记录的是色值并不是数值，byte的范围是+127到—128，
                        // 而亮度FFFFFF是11111111是-127，所以这里需要先转为无符号unsigned long参考Byte.toUnsignedLong()
                        pixelLightCount += ((long) data[i]) & 0xffL;
                    }
                    //平均亮度
                    long cameraLight = pixelLightCount / (pixeCount / step);
                    //更新历史记录
                    int lightSize = darkList.length;
                    darkList[darkIndex = darkIndex % lightSize] = cameraLight;
                    darkIndex++;
                    boolean isDarkEnv = true;
                    //判断在时间范围waitScanTime * lightSize内是不是亮度过暗
                    for (int i = 0; i < lightSize; i++) {
                        if (darkList[i] > darkValue) {
                            isDarkEnv = false;
                        }
                    }
                    Log.e(TAG, "摄像头环境亮度为 ： " + cameraLight);
                    if (!isFinishing()) {
                        //亮度过暗就提醒
                        if (isDarkEnv) {
                            lightTV.setVisibility(View.VISIBLE);
                        } else {
                            lightTV.setVisibility(View.GONE);
                        }
                    }
                }
            }
        });
    }
}
