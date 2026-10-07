import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.aura.PlayerAuraClient;
import com.newuniverse.nusmp.client.aura.PlayerAuraLayer;
import com.newuniverse.nusmp.client.prop.MagicPropRenderer;
import com.newuniverse.nusmp.client.prop.PropPainters;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Headless render preview, Java half: runs the REAL render code of the mod (MagicPropRenderer + a prop painter, PlayerAuraLayer + an aura
 * painter, the geo model drawer) against the preview stubs and records every quad it emits, in the order the game would draw them,
 * with the render type of each batch. preview.py rasterises what is recorded.
 *
 * usage: java Preview job.json meta.json bin-dir
 *
 * What the recorder reproduces from the game's BufferBuilder / BufferSource (1.21.1):
 *  - asking for a different render type ENDS the previous batch (it is drawn right then), the same type again continues the batch;
 *  - writing to a consumer whose batch has ended throws ("Not building!"), a vertex that misses an element of the format throws
 *    ("Missing elements in vertex: ..."), an element set twice keeps the FIRST value, normals are clamped to -1..1 and quantised to 1/127,
 *    colour channels are masked to a byte;
 *  - the pose stack must be balanced when the painter returns.
 */
public final class Preview {
    private Preview() {}

    // ================================================================================================ json
    /** Minimal JSON reader: objects -> LinkedHashMap, arrays -> ArrayList, numbers -> Double, strings, booleans, null. */
    static final class Json {
        private final String s;
        private int i;

        private Json(String s) { this.s = s; }

        static Object parse(String s) {
            Json j = new Json(s);
            Object o = j.value();
            j.ws();
            if (j.i != s.length()) throw new IllegalArgumentException("trailing json at " + j.i);
            return o;
        }

        private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

        private Object value() {
            ws();
            char c = s.charAt(i);
            if (c == '{') {
                i++;
                Map<String, Object> m = new LinkedHashMap<>();
                ws();
                if (s.charAt(i) == '}') { i++; return m; }
                while (true) {
                    ws();
                    String k = string();
                    ws();
                    if (s.charAt(i++) != ':') throw new IllegalArgumentException("expected : at " + i);
                    m.put(k, value());
                    ws();
                    char d = s.charAt(i++);
                    if (d == '}') return m;
                    if (d != ',') throw new IllegalArgumentException("expected , at " + i);
                }
            }
            if (c == '[') {
                i++;
                List<Object> l = new ArrayList<>();
                ws();
                if (s.charAt(i) == ']') { i++; return l; }
                while (true) {
                    l.add(value());
                    ws();
                    char d = s.charAt(i++);
                    if (d == ']') return l;
                    if (d != ',') throw new IllegalArgumentException("expected , at " + i);
                }
            }
            if (c == '"') return string();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            int st = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            return Double.parseDouble(s.substring(st, i));
        }

        private String string() {
            if (s.charAt(i++) != '"') throw new IllegalArgumentException("expected string at " + i);
            StringBuilder b = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') return b.toString();
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n' -> b.append('\n');
                        case 't' -> b.append('\t');
                        case 'r' -> b.append('\r');
                        case 'b' -> b.append('\b');
                        case 'f' -> b.append('\f');
                        case 'u' -> { b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                        default -> b.append(e);
                    }
                } else b.append(c);
            }
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Object o) { return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<>(); }

    @SuppressWarnings("unchecked")
    static List<Object> list(Object o) { return o instanceof List ? (List<Object>) o : new ArrayList<>(); }

    static double num(Object o, double def) {
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof String s) { try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return def; } }
        if (o instanceof Boolean b) return b ? 1 : 0;
        return def;
    }

    static double num(Map<String, Object> m, String k, double def) { return num(m.get(k), def); }

    /** A scene value that is either one number or one number per frame. */
    static double perFrame(Map<String, Object> m, String k, int frame, double def) {
        Object o = m.get(k);
        if (o instanceof List<?> l) return l.isEmpty() ? def : num(l.get(Math.min(frame, l.size() - 1)), def);
        return num(o, def);
    }

    static boolean bool(Map<String, Object> m, String k, boolean def) {
        Object o = m.get(k);
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.doubleValue() != 0;
        if (o instanceof String s) return s.equalsIgnoreCase("true") || s.equals("1");
        return def;
    }

    static String str(Map<String, Object> m, String k, String def) { Object o = m.get(k); return o == null ? def : String.valueOf(o); }

    static double[] vec(Object o, double dx, double dy, double dz) {
        double[] r = {dx, dy, dz};
        if (o instanceof List<?> l) for (int i = 0; i < 3 && i < l.size(); i++) r[i] = num(l.get(i), r[i]);
        return r;
    }

    static String esc(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> { if (c < 32) b.append(String.format("\\u%04x", (int) c)); else b.append(c); }
            }
        }
        return b.append('"').toString();
    }

    // ================================================================================================ recorder (BufferSource + BufferBuilder semantics)
    static final int POS = 1, COLOR = 2, UV0 = 4, UV1 = 8, UV2 = 16, NORMAL = 32;
    static final String[] ELEMENT_NAMES = {"Position", "Color", "UV0", "UV1", "UV2", "Normal"};
    /** floats per recorded vertex: x y z u v r g b a block sky nx ny nz */
    static final int VF = 14;

    /** A mistake the game would crash on (or silently mangle); carries a stable code for the report. */
    static final class VertexError extends IllegalStateException {
        final String code;

        VertexError(String code, String msg) {
            super(msg);
            this.code = code;
        }
    }

    static final class Issue {
        final String code, level, msg;

        Issue(String code, String level, String msg) {
            this.code = code;
            this.level = level;
            this.msg = msg;
        }
    }

    static final class Run {
        final RenderType type;
        final boolean base;
        final Consumer consumer;
        float[] data = new float[VF * 256];
        int vertices;
        boolean ended;

        Run(RenderType type, boolean base, Frame frame) {
            this.type = type;
            this.base = base;
            this.consumer = new Consumer(this, frame);
        }

        void push(float[] v) {
            if ((vertices + 1) * VF > data.length) data = java.util.Arrays.copyOf(data, data.length * 2);
            System.arraycopy(v, 0, data, vertices * VF, VF);
            vertices++;
        }

        /** The batch is drawn: the last vertex must be complete, and nobody may write to this consumer any more. */
        void end() {
            try { consumer.flushVertex(); } finally { ended = true; }
        }
    }

    static final class Frame {
        final List<Run> runs = new ArrayList<>();
        final List<Issue> issues = new ArrayList<>();
        String error;
        int nan, badNormal, secondSets;
        String badNormalExample;
    }

    /** The vertex consumer of one batch: the game's BufferBuilder for the ENTITY vertex format (or POSITION_COLOR for lightning). */
    static final class Consumer implements VertexConsumer {
        final Run run;
        final Frame frame;
        final int required;
        final float[] cur = new float[VF];
        int mask;
        boolean open;

        Consumer(Run run, Frame frame) {
            this.run = run;
            this.frame = frame;
            this.required = run.type.format == RenderType.Format.POSITION_COLOR ? (POS | COLOR) : (POS | COLOR | UV0 | UV1 | UV2 | NORMAL);
        }

        private void ensureBuilding() {
            if (run.ended)
                throw new VertexError("STALE_CONSUMER", "Not building! A VertexConsumer was used after its batch ended: asking buffers.getBuffer(...) for a different "
                        + "render type draws and closes the previous one, so write all of a render type's quads before requesting the next buffer (this consumer was for " + run.type + ")");
        }

        void flushVertex() {
            if (!open) return;
            if ((required & ~mask) != 0) {
                StringBuilder names = new StringBuilder();
                for (int b = 0; b < 6; b++) if (((required & ~mask) >> b & 1) != 0) names.append(names.length() > 0 ? ", " : "").append(ELEMENT_NAMES[b]);
                open = false;
                throw new VertexError("MISSING_ELEMENTS", "Missing elements in vertex: " + names + " (every vertex of " + run.type + " needs position, colour, uv, overlay, light and normal)");
            }
            open = false;
            run.push(cur);
        }

        /** True when the element may be written: a vertex is open and the element is in the format and not yet set (the game ignores the rest). */
        private boolean begin(int element) {
            ensureBuilding();
            if (!open || (required & element) == 0) return false;
            if ((mask & element) != 0) { frame.secondSets++; return false; }
            mask |= element;
            return true;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            ensureBuilding();
            flushVertex();
            open = true;
            mask = POS;
            java.util.Arrays.fill(cur, 0f);
            cur[0] = x;
            cur[1] = y;
            cur[2] = z;
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) frame.nan++;
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            if (begin(COLOR)) { cur[5] = r & 255; cur[6] = g & 255; cur[7] = b & 255; cur[8] = a & 255; }
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            if (begin(UV0)) { cur[3] = u; cur[4] = v; }
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            begin(UV1);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            if (begin(UV2)) { cur[9] = (u & 0xFFFF) >> 4; cur[10] = (v & 0xFFFF) >> 4; }
            return this;
        }

        @Override
        public VertexConsumer setNormal(float nx, float ny, float nz) {
            if (begin(NORMAL)) {
                double len = Math.sqrt((double) nx * nx + (double) ny * ny + (double) nz * nz);
                if (!(Math.abs(len - 1.0) < 0.12)) {
                    frame.badNormal++;
                    if (frame.badNormalExample == null) frame.badNormalExample = String.format(Locale.ROOT, "(%.2f, %.2f, %.2f) |n|=%.2f in %s", nx, ny, nz, len, run.type);
                }
                cur[11] = quantise(nx);
                cur[12] = quantise(ny);
                cur[13] = quantise(nz);
            }
            return this;
        }

        /** BufferBuilder stores a normal component as a signed byte: clamp to -1..1, scale by 127, truncate. */
        private static float quantise(float v) {
            if (Float.isNaN(v)) return 0f;
            float c = Math.max(-1f, Math.min(1f, v));
            return ((int) (c * 127.0f)) / 127.0f;
        }
    }

    /** The MultiBufferSource the painters draw into: batches in call order. */
    static final class Rec implements MultiBufferSource {
        final Frame frame = new Frame();
        Run cur;
        boolean baseMode;

        @Override
        public VertexConsumer getBuffer(RenderType rt) {
            if (cur != null && cur.type.equals(rt)) return cur.consumer;
            endCurrent();
            cur = new Run(rt, baseMode, frame);
            frame.runs.add(cur);
            return cur.consumer;
        }

        void endCurrent() {
            if (cur != null) {
                Run r = cur;
                cur = null;
                r.end();
            }
        }

        void finish() { endCurrent(); }
    }

    // ================================================================================================ the reference player (LivingEntityRenderer + PlayerRenderer, mirrored)
    static final class PlayerRig {
        final AbstractClientPlayer player;
        final PlayerModel<AbstractClientPlayer> model;
        final ResourceLocation skin;
        final RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent;

        PlayerRig(AbstractClientPlayer player, boolean slim, ResourceLocation skin) {
            this.player = player;
            this.skin = skin;
            this.model = new PlayerModel<>(PlayerModel.createRoot(slim), slim);
            final PlayerModel<AbstractClientPlayer> m = model;
            this.parent = new RenderLayerParent<>() {
                @Override public PlayerModel<AbstractClientPlayer> getModel() { return m; }
                @Override public ResourceLocation getTextureLocation(AbstractClientPlayer e) { return skin; }
            };
        }

        /**
         * What vanilla does for a player, in vanilla's order: the entity's render offset (crouching: down 0.125), the body yaw
         * (180 - yBodyRot), scale(-1, -1, 1), PlayerRenderer.scale (0.9375), translate(0, -1.501, 0); then the model is posed and drawn
         * with its own render type, then every layer gets the same pose.
         */
        void render(Rec rec, PoseStack pose, float limbSwing, float limbSwingAmount, float partial, float age, float headYaw, float headPitch,
                    float bodyYaw, boolean crouch, int light, PlayerAuraLayer layer) {
            player.previewSet(crouch, false, false);
            pose.pushPose();
            pose.translate(0.0, crouch ? -0.125 : 0.0, 0.0);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
            pose.scale(-1.0F, -1.0F, 1.0F);
            pose.scale(0.9375F, 0.9375F, 0.9375F);
            pose.translate(0.0F, -1.501F, 0.0F);
            model.young = false;
            model.riding = false;
            model.attackTime = 0f;
            model.crouching = crouch;
            model.prepareMobModel(player, limbSwing, limbSwingAmount, partial);
            model.setupAnim(player, limbSwing, limbSwingAmount, age, headYaw, headPitch);
            rec.baseMode = true;
            VertexConsumer vc = rec.getBuffer(model.renderType(skin));
            model.renderToBuffer(pose, vc, light, OverlayTexture.NO_OVERLAY, -1);
            rec.endCurrent();
            rec.baseMode = false;
            if (layer != null) layer.render(pose, rec, light, player, limbSwing, limbSwingAmount, partial, age, headYaw, headPitch);
            pose.popPose();
        }
    }

    // ================================================================================================ output
    static final class Out {
        final StringBuilder meta = new StringBuilder("{\"scenes\":[");
        boolean firstScene = true;
    }

    static final class SceneOut {
        final DataOutputStream bin;
        long quads;
        final List<String> pool = new ArrayList<>();

        SceneOut(DataOutputStream bin) { this.bin = bin; }

        /** Writes one recorded frame (all its runs) and returns its pool index. */
        int add(Frame f, double age, boolean figure) throws IOException {
            StringBuilder b = new StringBuilder();
            b.append("{\"age\":").append(age).append(",\"runs\":[");
            boolean first = true;
            for (Run r : f.runs) {
                int n = r.vertices / 4;
                if (r.vertices % 4 != 0 && !r.base)
                    f.issues.add(new Issue("PARTIAL_QUAD", "WARNING", "a batch of " + r.type + " ended with " + (r.vertices % 4) + " stray vertices (vertex counts must be a multiple of 4: quads)"));
                if (!first) b.append(',');
                first = false;
                b.append("{\"rt\":").append(rtJson(r.type)).append(",\"off\":").append(quads).append(",\"n\":").append(n).append(",\"base\":").append(r.base).append('}');
                for (int q = 0; q < n * 4; q++) for (int k = 0; k < VF; k++) bin.writeFloat(r.data[q * VF + k]);
                quads += n;
            }
            b.append("],\"issues\":[");
            first = true;
            if (f.nan > 0) f.issues.add(new Issue("NAN", "WARNING", f.nan + " vertices have a NaN or infinite coordinate"));
            if (f.badNormal > 0)
                f.issues.add(new Issue("NORMAL_LEN", "WARNING", f.badNormal + " vertices have a normal that is not unit length, e.g. " + f.badNormalExample
                        + " (the game clamps each component to -1..1 and never renormalises, so the lighting is wrong)"));
            for (Issue is : f.issues) {
                if (!first) b.append(',');
                first = false;
                b.append("{\"code\":").append(esc(is.code)).append(",\"level\":").append(esc(is.level)).append(",\"msg\":").append(esc(is.msg)).append('}');
            }
            b.append("],\"err\":").append(f.error == null ? "null" : esc(f.error)).append(",\"fig\":").append(figure).append('}');
            pool.add(b.toString());
            return pool.size() - 1;
        }
    }

    static String rtJson(RenderType t) {
        return "{\"name\":" + esc(t.name) + ",\"tex\":" + (t.texture == null ? "null" : esc(t.texture.toString())) + ",\"blend\":" + esc(t.blend.name().toLowerCase(Locale.ROOT))
                + ",\"cull\":" + t.cull + ",\"dw\":" + t.depthWrite + ",\"lit\":" + t.lit + ",\"sort\":" + t.sort + ",\"deq\":" + t.depthEqual
                + ",\"disc\":" + esc(t.discard) + ",\"vo\":" + t.viewOffset + ",\"uo\":" + t.uOff + ",\"vv\":" + t.vOff + ",\"fmt\":" + esc(t.format.name().toLowerCase(Locale.ROOT)) + "}";
    }

    // ================================================================================================ scene plumbing
    static String trace(Throwable t) {
        StringWriter w = new StringWriter();
        t.printStackTrace(new PrintWriter(w));
        StringBuilder b = new StringBuilder();
        int lines = 0;
        for (String l : w.toString().split("\n")) {
            String s = l.stripTrailing();
            boolean mine = s.contains("com.newuniverse") || s.contains("examples.") || !s.startsWith("\tat ");
            if (!mine && !s.contains("net.minecraft") && !s.contains("com.mojang")) continue;
            if (s.contains("Preview$") || s.contains("Preview.")) continue;
            b.append(s).append('\n');
            if (++lines >= 14) break;
        }
        return b.toString().trim();
    }

    static Issue issueOf(Throwable t) {
        Throwable c = t instanceof InvocationTargetException && t.getCause() != null ? t.getCause() : t;
        String code = c instanceof VertexError ve ? ve.code : "EXCEPTION";
        return new Issue(code, "ERROR", c.getClass().getSimpleName() + ": " + c.getMessage() + "\n" + trace(c));
    }

    /** Calls the public static register() of a class (what the generated registries do). */
    static Issue register(String cls) {
        try {
            Class<?> c = Class.forName(cls);
            Method m = c.getMethod("register");
            m.invoke(null);
            return null;
        } catch (ClassNotFoundException e) {
            return new Issue("NO_CLASS", "ERROR", "class " + cls + " is not compiled (the file does not exist or has another name)");
        } catch (NoSuchMethodException e) {
            return new Issue("NO_CLASS", "ERROR", "class " + cls + " has no public static register()");
        } catch (Throwable e) {
            Issue i = issueOf(e);
            return new Issue("REGISTER_FAILED", "ERROR", cls + ".register() threw: " + i.msg);
        }
    }

    static void setCamera(double[] eye, double[] target) {
        Minecraft.getInstance().gameRenderer.getMainCamera().previewSet(eye[0], eye[1], eye[2], target[0], target[1], target[2]);
    }

    static int[] splitAges(List<Object> a) { return null; }

    // ================================================================================================ scenes
    interface FrameFn { Frame record(double age, int view) throws Exception; }

    /** Records all ages x views, sharing frames between views unless a painter looked at the camera. */
    static void recordAll(SceneOut so, StringBuilder meta, double[] ages, List<double[][]> views, FrameFn fn, int[][] framesOut, boolean[] cameraUsedOut) throws Exception {
        for (int a = 0; a < ages.length; a++) {
            int before = Camera.previewUses;
            Frame f0 = fn.record(ages[a], 0);
            int p0 = so.add(f0, ages[a], false);
            boolean used = Camera.previewUses != before;
            if (used) cameraUsedOut[0] = true;
            for (int v = 0; v < views.size(); v++) {
                if (v == 0 || !used) { framesOut[a][v] = p0; continue; }
                framesOut[a][v] = so.add(fn.record(ages[a], v), ages[a], false);
            }
        }
    }

    static void finishScene(Out out, String name, String kind, SceneOut so, List<Issue> issues, boolean registered, boolean cameraUsed, int[][] frames, int figure,
                            double[] ages, String extra) {
        StringBuilder m = out.meta;
        if (!out.firstScene) m.append(',');
        out.firstScene = false;
        m.append("{\"name\":").append(esc(name)).append(",\"kind\":").append(esc(kind)).append(",\"registered\":").append(registered)
                .append(",\"cameraUsed\":").append(cameraUsed).append(",\"quads\":").append(so == null ? 0 : so.quads);
        m.append(",\"ages\":[");
        for (int i = 0; i < ages.length; i++) m.append(i > 0 ? "," : "").append(ages[i]);
        m.append("],\"issues\":[");
        boolean first = true;
        for (Issue is : issues) {
            if (!first) m.append(',');
            first = false;
            m.append("{\"code\":").append(esc(is.code)).append(",\"level\":").append(esc(is.level)).append(",\"msg\":").append(esc(is.msg)).append('}');
        }
        m.append("],\"frames\":[");
        if (frames != null) for (int a = 0; a < frames.length; a++) {
            m.append(a > 0 ? "," : "").append('[');
            for (int v = 0; v < frames[a].length; v++) m.append(v > 0 ? "," : "").append(frames[a][v]);
            m.append(']');
        }
        m.append("],\"figure\":").append(figure).append(",\"pool\":[");
        if (so != null) for (int i = 0; i < so.pool.size(); i++) m.append(i > 0 ? "," : "").append(so.pool.get(i));
        m.append(']');
        if (extra != null) m.append(',').append(extra);
        m.append('}');
    }

    static List<double[][]> readViews(Map<String, Object> sc) {
        List<double[][]> views = new ArrayList<>();
        for (Object o : list(sc.get("views"))) {
            Map<String, Object> v = map(o);
            views.add(new double[][]{vec(v.get("eye"), 0, 2, 6), vec(v.get("target"), 0, 1, 0)});
        }
        if (views.isEmpty()) views.add(new double[][]{{0, 2, 6}, {0, 1, 0}});
        return views;
    }

    static double[] readAges(Map<String, Object> sc) {
        List<Object> l = list(sc.get("ages"));
        double[] a = new double[Math.max(1, l.size())];
        for (int i = 0; i < l.size(); i++) a[i] = num(l.get(i), 0);
        return a;
    }

    static ResourceLocation skinOf(Map<String, Object> sc) {
        String s = str(sc, "skin", "steve").toLowerCase(Locale.ROOT);
        if (s.contains(":")) return ResourceLocation.parse(str(sc, "skin", "steve"));
        boolean slim = s.equals("slim") || s.equals("alex");
        return ResourceLocation.fromNamespaceAndPath("nusmp", "textures/preview/skin_" + (slim ? "alex" : "steve") + ".png");
    }

    static boolean slimOf(Map<String, Object> sc) {
        String s = str(sc, "skin", "steve").toLowerCase(Locale.ROOT);
        return bool(sc, "slim", s.equals("slim") || s.equals("alex") || s.endsWith("alex.png"));
    }

    static void world(ClientLevel level, AbstractClientPlayer local, boolean first) {
        Minecraft mc = Minecraft.getInstance();
        mc.level = level;
        mc.player = (LocalPlayer) local;
        mc.options.setCameraType(first ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_BACK);
    }

    // ---------------------------------------------------------------- props
    static void runProp(Map<String, Object> sc, Out out, String binDir) throws Exception {
        String name = str(sc, "name", "scene");
        List<Issue> issues = new ArrayList<>();
        double[] ages = readAges(sc);
        List<double[][]> views = readViews(sc);
        PropPainters.previewClear();
        PropKind kind = PropKind.valueOf(str(sc, "propKind", "BRONZE_1"));
        String attr = str(sc, "attr", null);
        if (attr != null) { Issue i = register("com.newuniverse.nusmp.client.prop." + attr + "PropPainter"); if (i != null) issues.add(i); }
        for (Object ex : list(sc.get("examples"))) { Issue i = register("examples." + ex); if (i != null) issues.add(i); }
        boolean registered = PropPainters.previewHas(kind);
        if (!registered) issues.add(new Issue("NO_PAINTER", "NOTE", "no painter is registered for " + kind + " (the attribute's PropPainter.register() registered nothing, or it is still the stub): nothing is drawn"));

        float scale = (float) num(sc, "scale", 1.0), yaw = (float) num(sc, "yaw", 0.0);
        int maxLife = (int) num(sc, "maxLife", 100), param = (int) num(sc, "param", 0), seed = (int) num(sc, "seed", 12345);
        int light = (int) num(sc, "light", 0xF00000);
        double[] pos = vec(sc.get("pos"), 0, 0, 0);
        double[] size = vec(sc.get("size"), scale, scale, 0);
        float health = (float) num(sc, "health", 0);

        ClientLevel level = new ClientLevel();
        AbstractClientPlayer viewer = new LocalPlayer(new EntityType<>(), level);
        viewer.setId(2);
        world(level, viewer, false);
        MagicPropEntity e = new MagicPropEntity(new EntityType<>(), level);
        e.setId(7);
        e.setPos(pos[0], pos[1], pos[2]);
        e.setYRot(yaw);
        e.yRotO = yaw;
        e.previewSetSize((float) size[0], (float) size[1]);
        e.previewSet(kind, scale, maxLife, param, seed, health);
        if (sc.get("target") != null) {
            double[] t = vec(sc.get("target"), 0, 0, 3);
            Entity target = new Entity(new EntityType<>(), level);
            target.setId(8);
            target.setPos(t[0], t[1], t[2]);
            level.previewAdd(target);
            e.setTarget(target);
        }
        level.previewAdd(e);
        MagicPropRenderer renderer = new MagicPropRenderer(new EntityRendererProvider.Context());

        String bin = binDir + "/" + name + ".bin";
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(bin), 1 << 16))) {
            SceneOut so = new SceneOut(dos);
            int[][] frames = new int[ages.length][views.size()];
            boolean[] camUsed = {false};
            final int[] ownerWarned = {0};
            FrameFn fn = (age, view) -> {
                double[][] cam = views.get(view);
                setCamera(cam[0], cam[1]);
                viewer.setPos(cam[0][0], cam[0][1] - viewer.getEyeHeight(), cam[0][2]);
                int tick = (int) Math.floor(age);
                float partial = (float) (age - tick);
                e.tickCount = tick;
                e.previewSetLife(sc.containsKey("life") ? (int) perFrame(sc, "life", frameIndex(ages, age), tick) : tick);
                level.previewSetGameTime(tick);
                Minecraft.getInstance().previewSetPartialTick(partial);
                Rec rec = new Rec();
                PoseStack pose = new PoseStack();
                pose.translate(pos[0], pos[1], pos[2]);
                int calls0 = e.previewOwnerCalls;
                try {
                    renderer.render(e, yaw, partial, pose, rec, light);
                    rec.finish();
                    if (pose.previewDepth() != 1)
                        rec.frame.issues.add(new Issue("UNBALANCED_POSE", "ERROR", "the painter left the pose stack unbalanced (" + (pose.previewDepth() - 1)
                                + " more pushPose than popPose): the game throws 'Pose stack not empty' at the end of the entity pass"));
                } catch (Throwable t) {
                    Issue is = issueOf(t);
                    rec.frame.error = is.code + ": " + is.msg;
                    rec.frame.issues.add(is);
                    try { rec.finish(); } catch (Throwable ignore) { /* the partial geometry is kept */ }
                }
                if (e.previewOwnerCalls != calls0 && ownerWarned[0]++ == 0)
                    rec.frame.issues.add(new Issue("OWNER_NULL", "WARNING", "the painter called e.owner(): it is ALWAYS null on the client (the owner is only known to the server), use the synced fields (param, seed, target) instead"));
                return rec.frame;
            };
            recordAll(so, out.meta, ages, views, fn, frames, camUsed);

            // the stand-in figure (a player model): recorded once at the origin, placed by the rasteriser
            int figure = -1;
            if (bool(sc, "figure", true)) {
                PlayerRig rig = new PlayerRig(new AbstractClientPlayer(new EntityType<>(), level), false, ResourceLocation.fromNamespaceAndPath("nusmp", "textures/preview/skin_steve.png"));
                Rec rec = new Rec();
                PoseStack pose = new PoseStack();
                try {
                    rig.render(rec, pose, 0, 0, 0f, 100f, 0, 0, 0, false, light, null);
                    rec.finish();
                } catch (Throwable t) { issues.add(issueOf(t)); }
                figure = so.add(rec.frame, 0, true);
            }
            finishScene(out, name, "prop", so, issues, registered, camUsed[0], frames, figure, ages, null);
        }
    }

    static int frameIndex(double[] ages, double age) {
        for (int i = 0; i < ages.length; i++) if (ages[i] == age) return i;
        return 0;
    }

    // ---------------------------------------------------------------- auras
    static void runAura(Map<String, Object> sc, Out out, String binDir) throws Exception {
        String name = str(sc, "name", "scene");
        List<Issue> issues = new ArrayList<>();
        double[] ages = readAges(sc);
        List<double[][]> views = readViews(sc);
        PlayerAuraClient.previewClear();
        Aura aura = Aura.valueOf(str(sc, "aura", "BRONZE"));
        String attr = str(sc, "attr", null);
        if (attr != null) { Issue i = register("com.newuniverse.nusmp.client.aura." + attr + "Aura"); if (i != null) issues.add(i); }
        for (Object ex : list(sc.get("examples"))) { Issue i = register("examples." + ex); if (i != null) issues.add(i); }
        boolean registered = PlayerAuraClient.previewHasPainter(aura);
        if (!registered) issues.add(new Issue("NO_PAINTER", "NOTE", "no painter is registered for Aura." + aura + " (the attribute's Aura.register() registered nothing, or it is still the stub): only the base player is drawn"));

        int style = (int) num(sc, "style", 0), total = (int) num(sc, "total", 120);
        int light = (int) num(sc, "light", 0xF00000);
        boolean crouch = bool(sc, "crouch", false), local = bool(sc, "local", false), firstPerson = bool(sc, "firstPerson", false);
        float bodyYaw = (float) num(sc, "bodyYaw", 0), clock = (float) num(sc, "clock", 100);

        ClientLevel level = new ClientLevel();
        LocalPlayer self = new LocalPlayer(new EntityType<>(), level);
        self.setId(1);
        LocalPlayer other = new LocalPlayer(new EntityType<>(), level);
        other.setId(2);
        AbstractClientPlayer p = local ? self : new AbstractClientPlayer(new EntityType<>(), level);
        p.setId(1);
        world(level, local ? self : other, firstPerson);
        level.previewAdd(p);
        PlayerRig rig = new PlayerRig(p, slimOf(sc), skinOf(sc));
        PlayerAuraLayer layer = new PlayerAuraLayer(rig.parent);
        PlayerAuraClient.previewSet(p.getId(), aura, 0L, total, style);
        for (Object oa : list(sc.get("alsoAuras"))) {
            try { PlayerAuraClient.previewSet(p.getId(), Aura.valueOf(String.valueOf(oa)), 0L, total, 0); } catch (IllegalArgumentException ex) { issues.add(new Issue("BAD_SCENE", "ERROR", "alsoAuras: unknown aura " + oa)); }
        }

        String bin = binDir + "/" + name + ".bin";
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(bin), 1 << 16))) {
            SceneOut so = new SceneOut(dos);
            int[][] frames = new int[ages.length][views.size()];
            boolean[] camUsed = {false};
            FrameFn fn = (age, view) -> {
                double[][] cam = views.get(view);
                setCamera(cam[0], cam[1]);
                int fi = frameIndex(ages, age);
                int tick = (int) Math.floor(age);
                float partial = (float) (age - tick);
                level.previewSetGameTime(tick);
                Minecraft.getInstance().previewSetPartialTick(partial);
                float limbSwing = (float) perFrame(sc, "limbSwing", fi, 0), limbAmount = (float) perFrame(sc, "limbSwingAmount", fi, 0);
                float headYaw = (float) perFrame(sc, "headYaw", fi, 0), headPitch = (float) perFrame(sc, "headPitch", fi, 0);
                Rec rec = new Rec();
                PoseStack pose = new PoseStack();
                try {
                    rig.render(rec, pose, limbSwing, limbAmount, partial, clock + (float) age, headYaw, headPitch, bodyYaw, crouch, light, layer);
                    rec.finish();
                    if (pose.previewDepth() != 1)
                        rec.frame.issues.add(new Issue("UNBALANCED_POSE", "ERROR", "the painter left the pose stack unbalanced (" + (pose.previewDepth() - 1)
                                + " more pushPose than popPose): the game throws 'Pose stack not empty' at the end of the entity pass"));
                } catch (Throwable t) {
                    Issue is = issueOf(t);
                    rec.frame.error = is.code + ": " + is.msg;
                    rec.frame.issues.add(is);
                    try { rec.finish(); } catch (Throwable ignore) { /* partial geometry kept */ }
                }
                return rec.frame;
            };
            recordAll(so, out.meta, ages, views, fn, frames, camUsed);
            finishScene(out, name, "aura", so, issues, registered, camUsed[0], frames, -1, ages, null);
        }
    }

    // ================================================================================================ main
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("usage: Preview job.json meta.json bin-dir");
            System.exit(2);
        }
        Map<String, Object> job = map(Json.parse(new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8)));
        Out out = new Out();
        for (Object o : list(job.get("scenes"))) {
            Map<String, Object> sc = map(o);
            String name = str(sc, "name", "scene");
            try {
                switch (str(sc, "kind", "")) {
                    case "prop" -> runProp(sc, out, args[2]);
                    case "aura" -> runAura(sc, out, args[2]);
                    default -> throw new IllegalArgumentException("unknown scene kind " + sc.get("kind"));
                }
            } catch (Throwable t) {
                List<Issue> issues = new ArrayList<>();
                issues.add(issueOf(t));
                finishScene(out, name, str(sc, "kind", "?"), null, issues, false, false, null, -1, readAges(sc), null);
            }
        }
        out.meta.append("]}");
        Files.write(Paths.get(args[1]), out.meta.toString().getBytes(StandardCharsets.UTF_8));
    }
}
