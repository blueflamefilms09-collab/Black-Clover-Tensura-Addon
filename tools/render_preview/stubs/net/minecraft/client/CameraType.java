package net.minecraft.client;

/** Preview stub of CameraType (the F5 perspective). */
public enum CameraType {
    FIRST_PERSON(true, false),
    THIRD_PERSON_BACK(false, false),
    THIRD_PERSON_FRONT(false, true);

    private final boolean firstPerson, mirrored;

    CameraType(boolean firstPerson, boolean mirrored) {
        this.firstPerson = firstPerson;
        this.mirrored = mirrored;
    }

    public boolean isFirstPerson() { return firstPerson; }
    public boolean isMirrored() { return mirrored; }
    public CameraType cycle() { CameraType[] v = values(); return v[(ordinal() + 1) % v.length]; }
}
