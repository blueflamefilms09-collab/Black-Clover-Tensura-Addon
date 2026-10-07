package net.minecraft.client.model.geom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Preview stub of Minecraft 1.21.1's ModelPart, with the game's maths: the box UV layout of ModelPart.Cube (vertex order, uvs, normals,
 * mirror, inflate), translateAndRotate (pivot in 1/16 block, rotation Z then Y then X as {@code rotationZYX(zRot, yRot, xRot)}, scale)
 * and render (push, translate and rotate, cubes, children, pop). The part is in vanilla model space: pixels, y down, front = -z.
 */
public final class ModelPart {
    public static final float DEFAULT_SCALE = 1.0F;
    public float x, y, z, xRot, yRot, zRot;
    public float xScale = 1.0F, yScale = 1.0F, zScale = 1.0F;
    public boolean visible = true;
    public boolean skipDraw;
    private final List<Cube> cubes;
    private final Map<String, ModelPart> children;
    private PartPose initialPose = PartPose.ZERO;

    public ModelPart(List<Cube> cubes, Map<String, ModelPart> children) {
        this.cubes = cubes;
        this.children = children;
    }

    public PartPose storePose() { return PartPose.offsetAndRotation(x, y, z, xRot, yRot, zRot); }

    public PartPose getInitialPose() { return initialPose; }

    public void setInitialPose(PartPose pose) { this.initialPose = pose; }

    public void resetPose() { loadPose(initialPose); }

    public void loadPose(PartPose pose) {
        x = pose.x();
        y = pose.y();
        z = pose.z();
        xRot = pose.xRot();
        yRot = pose.yRot();
        zRot = pose.zRot();
        xScale = 1.0F;
        yScale = 1.0F;
        zScale = 1.0F;
    }

    public void copyFrom(ModelPart part) {
        xScale = part.xScale;
        yScale = part.yScale;
        zScale = part.zScale;
        xRot = part.xRot;
        yRot = part.yRot;
        zRot = part.zRot;
        x = part.x;
        y = part.y;
        z = part.z;
    }

    public boolean hasChild(String name) { return children.containsKey(name); }

    public ModelPart getChild(String name) {
        ModelPart part = children.get(name);
        if (part == null) throw new NoSuchElementException("Can't find part " + name);
        return part;
    }

    public void setPos(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setRotation(float xRot, float yRot, float zRot) {
        this.xRot = xRot;
        this.yRot = yRot;
        this.zRot = zRot;
    }

    public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) { render(poseStack, buffer, packedLight, packedOverlay, -1); }

    public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        if (visible && (!cubes.isEmpty() || !children.isEmpty())) {
            poseStack.pushPose();
            translateAndRotate(poseStack);
            if (!skipDraw) compile(poseStack.last(), buffer, packedLight, packedOverlay, color);
            for (ModelPart child : children.values()) child.render(poseStack, buffer, packedLight, packedOverlay, color);
            poseStack.popPose();
        }
    }

    public void visit(PoseStack poseStack, Visitor visitor) { visit(poseStack, visitor, ""); }

    private void visit(PoseStack poseStack, Visitor visitor, String path) {
        if (!cubes.isEmpty() || !children.isEmpty()) {
            poseStack.pushPose();
            translateAndRotate(poseStack);
            PoseStack.Pose pose = poseStack.last();
            for (int i = 0; i < cubes.size(); i++) visitor.visit(pose, path, i, cubes.get(i));
            String prefix = path + "/";
            children.forEach((name, part) -> part.visit(poseStack, visitor, prefix + name));
            poseStack.popPose();
        }
    }

    public void translateAndRotate(PoseStack poseStack) {
        poseStack.translate(x / 16.0F, y / 16.0F, z / 16.0F);
        if (xRot != 0.0F || yRot != 0.0F || zRot != 0.0F) poseStack.mulPose(new Quaternionf().rotationZYX(zRot, yRot, xRot));
        if (xScale != 1.0F || yScale != 1.0F || zScale != 1.0F) poseStack.scale(xScale, yScale, zScale);
    }

    private void compile(PoseStack.Pose pose, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        for (Cube cube : cubes) cube.compile(pose, buffer, packedLight, packedOverlay, color);
    }

    public Cube getRandomCube(RandomSource random) { return cubes.get(random.nextInt(cubes.size())); }

    public boolean isEmpty() { return cubes.isEmpty(); }

    public Stream<ModelPart> getAllParts() { return Stream.concat(Stream.of(this), children.values().stream().flatMap(ModelPart::getAllParts)); }

    @FunctionalInterface
    public interface Visitor {
        void visit(PoseStack.Pose pose, String path, int index, Cube cube);
    }

    /** Thrown like the game's when a part name is missing. */
    private static final class NoSuchElementException extends RuntimeException {
        NoSuchElementException(String m) { super(m); }
    }

    public static class Cube {
        private final Polygon[] polygons;
        public final float minX, minY, minZ, maxX, maxY, maxZ;

        public Cube(int texCoordU, int texCoordV, float originX, float originY, float originZ, float dimensionX, float dimensionY, float dimensionZ,
                    float growX, float growY, float growZ, boolean mirror, float texWidthScale, float texHeightScale, Set<Direction> visibleFaces) {
            this.minX = originX;
            this.minY = originY;
            this.minZ = originZ;
            this.maxX = originX + dimensionX;
            this.maxY = originY + dimensionY;
            this.maxZ = originZ + dimensionZ;
            this.polygons = new Polygon[visibleFaces.size()];
            float f = originX + dimensionX;
            float f1 = originY + dimensionY;
            float f2 = originZ + dimensionZ;
            originX -= growX;
            originY -= growY;
            originZ -= growZ;
            f += growX;
            f1 += growY;
            f2 += growZ;
            if (mirror) {
                float f3 = f;
                f = originX;
                originX = f3;
            }
            Vertex vertex7 = new Vertex(originX, originY, originZ, 0.0F, 0.0F);
            Vertex vertex = new Vertex(f, originY, originZ, 0.0F, 8.0F);
            Vertex vertex1 = new Vertex(f, f1, originZ, 8.0F, 8.0F);
            Vertex vertex2 = new Vertex(originX, f1, originZ, 8.0F, 0.0F);
            Vertex vertex3 = new Vertex(originX, originY, f2, 0.0F, 0.0F);
            Vertex vertex4 = new Vertex(f, originY, f2, 0.0F, 8.0F);
            Vertex vertex5 = new Vertex(f, f1, f2, 8.0F, 8.0F);
            Vertex vertex6 = new Vertex(originX, f1, f2, 8.0F, 0.0F);
            float f4 = (float) texCoordU;
            float f5 = (float) texCoordU + dimensionZ;
            float f6 = (float) texCoordU + dimensionZ + dimensionX;
            float f7 = (float) texCoordU + dimensionZ + dimensionX + dimensionX;
            float f8 = (float) texCoordU + dimensionZ + dimensionX + dimensionZ;
            float f9 = (float) texCoordU + dimensionZ + dimensionX + dimensionZ + dimensionX;
            float f10 = (float) texCoordV;
            float f11 = (float) texCoordV + dimensionZ;
            float f12 = (float) texCoordV + dimensionZ + dimensionY;
            int i = 0;
            if (visibleFaces.contains(Direction.DOWN))
                polygons[i++] = new Polygon(new Vertex[]{vertex4, vertex3, vertex7, vertex}, f5, f10, f6, f11, texWidthScale, texHeightScale, mirror, Direction.DOWN);
            if (visibleFaces.contains(Direction.UP))
                polygons[i++] = new Polygon(new Vertex[]{vertex1, vertex2, vertex6, vertex5}, f6, f11, f7, f10, texWidthScale, texHeightScale, mirror, Direction.UP);
            if (visibleFaces.contains(Direction.WEST))
                polygons[i++] = new Polygon(new Vertex[]{vertex7, vertex3, vertex6, vertex2}, f4, f11, f5, f12, texWidthScale, texHeightScale, mirror, Direction.WEST);
            if (visibleFaces.contains(Direction.NORTH))
                polygons[i++] = new Polygon(new Vertex[]{vertex, vertex7, vertex2, vertex1}, f5, f11, f6, f12, texWidthScale, texHeightScale, mirror, Direction.NORTH);
            if (visibleFaces.contains(Direction.EAST))
                polygons[i++] = new Polygon(new Vertex[]{vertex4, vertex, vertex1, vertex5}, f6, f11, f8, f12, texWidthScale, texHeightScale, mirror, Direction.EAST);
            if (visibleFaces.contains(Direction.SOUTH))
                polygons[i] = new Polygon(new Vertex[]{vertex3, vertex4, vertex5, vertex6}, f8, f11, f9, f12, texWidthScale, texHeightScale, mirror, Direction.SOUTH);
        }

        public void compile(PoseStack.Pose pose, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
            Matrix4f matrix4f = pose.pose();
            Vector3f vector3f = new Vector3f();
            for (Polygon polygon : polygons) {
                Vector3f normal = pose.transformNormal(polygon.normal, vector3f);
                float f = normal.x(), f1 = normal.y(), f2 = normal.z();
                for (Vertex vertex : polygon.vertices) {
                    float f3 = vertex.pos.x() / 16.0F, f4 = vertex.pos.y() / 16.0F, f5 = vertex.pos.z() / 16.0F;
                    Vector3f p = matrix4f.transformPosition(f3, f4, f5, new Vector3f());
                    buffer.addVertex(p.x(), p.y(), p.z(), color, vertex.u, vertex.v, packedOverlay, packedLight, f, f1, f2);
                }
            }
        }
    }

    static class Polygon {
        public final Vertex[] vertices;
        public final Vector3f normal;

        public Polygon(Vertex[] vertices, float u1, float v1, float u2, float v2, float texWidth, float texHeight, boolean mirror, Direction direction) {
            this.vertices = vertices;
            vertices[0] = vertices[0].remap(u2 / texWidth, v1 / texHeight);
            vertices[1] = vertices[1].remap(u1 / texWidth, v1 / texHeight);
            vertices[2] = vertices[2].remap(u1 / texWidth, v2 / texHeight);
            vertices[3] = vertices[3].remap(u2 / texWidth, v2 / texHeight);
            if (mirror) {
                int n = vertices.length;
                for (int j = 0; j < n / 2; j++) {
                    Vertex v = vertices[j];
                    vertices[j] = vertices[n - 1 - j];
                    vertices[n - 1 - j] = v;
                }
            }
            this.normal = direction.step();
            if (mirror) this.normal.mul(-1.0F, 1.0F, 1.0F);
        }
    }

    static class Vertex {
        public final Vector3f pos;
        public final float u, v;

        public Vertex(float x, float y, float z, float u, float v) { this(new Vector3f(x, y, z), u, v); }

        public Vertex remap(float u, float v) { return new Vertex(pos, u, v); }

        public Vertex(Vector3f pos, float u, float v) {
            this.pos = pos;
            this.u = u;
            this.v = v;
        }
    }
}
