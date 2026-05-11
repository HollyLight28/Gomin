package uz.unnarsx.cherrygram.camera;

import android.graphics.SurfaceTexture;
import android.util.Range;

public class VideoMessagesHelper {
    public CameraXController cameraXController = new CameraXController(null, null, null);
    public static Range<Integer> getCameraXFpsRange() { return new Range<>(30, 30); }
    public float getSliderW() { return 0f; }
    public float getSliderH() { return 0f; }
    public float getSliderBM() { return 0f; }
    public void setZoom(float zoom) {}
    public void showExposureControls(Object view, boolean show) {}
    public void switchCameraX(Object view) {}
    public void checkFlash(Object view) {}
    public boolean createFlashConfigurator(Object view) { return false; }
    public void destroyCameraX(Object view) {}
    public void updateCameraXFlash(Object view) {}
    public void createCameraX(Object view, SurfaceTexture[] surface) {}
}
