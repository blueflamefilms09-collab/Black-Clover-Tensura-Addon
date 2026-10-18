package com.newuniverse.nusmp.client.heroweapon.carriage;

/**
 * Why: the Vassal Carriage you can ride is drawn from the SAME mesh file as the small carriage in the hand, about three times larger, and the two do not share a
 * coordinate frame: the weapon mesh follows the mod's item convention (the long axis is y with the nose at +y, the roof is on the -x side, the width is z), the
 * entity's frame is Minecraft's (z forward, y up, x to the left of the way it faces). This class is the one place that maps one to the other, with plain doubles
 * so a test can pin it: the nose points forward, the roof points up, the floor (the lowest point of the wheels) rests on the ground, the model is centred on
 * its own width and length, and the scale makes it a chosen length. It has no game classes. The mapping is a proper rotation (no mirror), so the winding of the
 * quads and the direction of their normals survive it.
 */
public final class MeshFrame {
    private MeshFrame() {}

    /** The bounding box of a mesh in mesh space. */
    public record Box(double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        public double length() { return maxY - minY; }

        public boolean valid() { return maxY - minY > 1.0e-6 && maxX - minX > 1.0e-6 && maxZ - minZ > 1.0e-6; }
    }

    /** The scale that makes the mesh's length (its y extent) {@code targetLength} blocks; 1 when the box is empty. */
    public static double scaleFor(Box box, double targetLength) { return box.length() > 1.0e-6 ? targetLength / box.length() : 1.0; }

    /**
     * Maps a mesh point to the entity's frame: x (left) = -(z - centre z), y (up) = (max x - x), z (forward) = (y - centre y), all times {@code scale}. So the
     * nose (+y) is forward, the roof (the smallest x) is the highest point and the floor (the largest x) is at y = 0.
     */
    public static double[] point(double mx, double my, double mz, Box box, double scale) {
        double cy = (box.minY() + box.maxY()) * 0.5, cz = (box.minZ() + box.maxZ()) * 0.5;
        return new double[]{-(mz - cz) * scale, (box.maxX() - mx) * scale, (my - cy) * scale};
    }

    /** Maps a mesh direction (a normal) to the entity's frame (the same rotation, no scale or offset). */
    public static double[] normal(double nx, double ny, double nz) { return new double[]{-nz, -nx, ny}; }
}
