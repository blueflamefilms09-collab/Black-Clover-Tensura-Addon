package net.minecraft.client;

/** Preview stub of Options: only the camera perspective. */
public class Options {
    private CameraType cameraType = CameraType.THIRD_PERSON_BACK;

    public CameraType getCameraType() { return cameraType; }
    public void setCameraType(CameraType cameraType) { this.cameraType = cameraType; }
}
