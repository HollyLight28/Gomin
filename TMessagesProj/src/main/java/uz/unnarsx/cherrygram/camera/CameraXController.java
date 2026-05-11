package uz.unnarsx.cherrygram.camera;

import android.content.Context;
import androidx.lifecycle.LifecycleOwner;
import org.telegram.messenger.camera.Size;

public class CameraXController {
    public static final int CAMERA_NONE = 0;
    public static final int CAMERA_HDR = 1;
    public static final int CAMERA_NIGHT = 2;
    public static final int CAMERA_AUTO = 3;
    public static final int CAMERA_WIDE = 4;
    public static final int CAMERA_ASPECT_RATIO_SELECTOR = 5;

    public static class CameraLifecycle implements LifecycleOwner {
        public void stop() {}
        @Override public androidx.lifecycle.Lifecycle getLifecycle() { return null; }
    }
    public static boolean hasGoodCamera(Context ctx) { return false; }
    public static boolean isFlashAvailable() { return false; }
    
    public CameraXController(Object lifecycle, Object factory, Object provider) {}

    public boolean isExposureCompensationSupported() { return false; }
    public void setExposureCompensation(float ev) {}
    public boolean isInitiated() { return false; }
    public void switchCamera() {}
    public void setTargetOrientation(int orient) {}
    public void setWorldCaptureOrientation(int orient) {}
    public boolean isFrontface() { return false; }
    public void bindUseCases() {}
    public void closeCamera() {}
    public void setCameraEffect(int effect) {}
    public int getCameraEffect() { return 0; }
    public void setFrontFace(boolean front) {}
    public void initCamera(Context ctx, boolean front, Runnable callback) {}
    public boolean hasFrontFaceCamera() { return false; }
    public int setNextFlashMode() { return 0; }
    public int getCurrentFlashMode() { return 0; }
    public void setZoom(float zoom) {}
    public float resetZoom() { return 1f; }
    public boolean isAvailableHdrMode() { return false; }
    public boolean isAvailableWideMode() { return false; }
    public boolean isAvailableNightMode() { return false; }
    public boolean isAvailableAutoMode() { return false; }
    public void focusToPoint(float x, float y) {}
    public Size getPreviewSize() { return new Size(0, 0); }
    public void recordVideo(Object file, boolean mirror, Object callback) {}
    public void stopVideoRecording(boolean abandon) {}
    public void takePicture(Object file, Object callback) {}
    public int getDisplayOrientation() { return 0; }

    public @interface EffectFacing {}
}
