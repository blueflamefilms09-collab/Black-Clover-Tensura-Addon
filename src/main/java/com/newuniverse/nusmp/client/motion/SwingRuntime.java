package com.newuniverse.nusmp.client.motion;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.heroweapon.HeroClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The client half of the weapon swing motion (docs/vassal_forms_v2.md): it keeps the bone state of the few weapons being drawn, advances it with {@link SwingMath} once per drawn frame,
 * builds the bone matrices into pre-allocated arrays and draws a mesh's skinned tail with them. Render thread only; nothing here touches the server or the network.
 * <p>
 * Bounds (rule 11): the state lives in two maps (right hand, left hand) keyed by the holder's UUID, at most {@link #MAX_HOLDERS} entries each (a new holder past the cap is drawn rigid),
 * every entry is dropped {@link #KEEP_TICKS} ticks after its last frame (a tick listener, every 20th tick) and all of them on logout; a weapon farther than {@link SwingMath#LOD_DISTANCE}
 * blocks, an item on the ground / in a frame / in a menu (no holder) and a mesh without a valid rig are drawn rigid at no cost. A frame allocates nothing: the matrices, the vectors and the
 * state arrays are pre-allocated (a holder's arrays once, when it first appears). The client setting {@code models.weaponMotion} (default on) off draws every mesh rigid.
 */
public final class SwingRuntime {
    private SwingRuntime() {}

    public static final int MAX_HOLDERS = 48;
    public static final int KEEP_TICKS = 40;

    /** One holder's bones. */
    static final class State {
        final float[] th = new float[SwingMath.MAX_BONES], thv = new float[SwingMath.MAX_BONES];
        SwingRig rig;
        float lastTime = Float.NaN;
        long touched;
        final float phase;

        State(UUID id) {
            phase = (id.hashCode() & 1023) * (6.2831855f / 1024f);
        }
    }

    private static final Map<UUID, State> RIGHT = new HashMap<>(), LEFT = new HashMap<>();
    private static final Matrix4f[] MAT = new Matrix4f[SwingMath.MAX_BONES];
    private static final Matrix3f[] NRM = new Matrix3f[SwingMath.MAX_BONES];
    private static final Vector3f P0 = new Vector3f(), P1 = new Vector3f(), N0 = new Vector3f(), N1 = new Vector3f();

    static {
        for (int i = 0; i < SwingMath.MAX_BONES; i++) {
            MAT[i] = new Matrix4f();
            NRM[i] = new Matrix3f();
        }
    }

    /** Registered from {@code WeaponRenderer.init}: the cleanup tick and the logout clear. */
    public static void init() {
        NeoForge.EVENT_BUS.addListener(SwingRuntime::onTick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> clear());
    }

    public static void clear() {
        RIGHT.clear();
        LEFT.clear();
    }

    static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            clear();
            return;
        }
        long now = mc.level.getGameTime();
        if (now % 20L != 0L) return;
        prune(RIGHT, now);
        prune(LEFT, now);
    }

    private static void prune(Map<UUID, State> m, long now) {
        if (m.isEmpty()) return;
        for (Iterator<State> it = m.values().iterator(); it.hasNext(); ) {
            if (now - it.next().touched > KEEP_TICKS) it.remove();
        }
    }

    public static boolean enabled() {
        return HeroClientConfig.weaponMotion();
    }

    /** Number of holders being tracked (the tests and the debug read it). */
    public static int tracked() {
        return RIGHT.size() + LEFT.size();
    }

    /**
     * Advances the holder's bones to this frame and fills the bone matrices from {@code base} (the mesh-to-view pose of the draw). Returns false when the weapon is to be drawn rigid.
     *
     * @param timeTicks game time in ticks including the partial tick (the clock the renderer already has)
     */
    public static boolean prepare(SwingRig rig, ItemDisplayContext ctx, LivingEntity holder, float timeTicks, float partial, Matrix4f base, Matrix3f baseN) {
        if (rig == null || holder == null || !enabled()) return false;
        float distSq = base.m30() * base.m30() + base.m31() * base.m31() + base.m32() * base.m32();
        if (!SwingMath.near(distSq)) return false;
        boolean left = ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        Map<UUID, State> map = left ? LEFT : RIGHT;
        UUID id = holder.getUUID();
        State st = map.get(id);
        if (st == null) {
            if (map.size() >= MAX_HOLDERS) return false;
            st = new State(id);
            map.put(id, st);
        }
        Minecraft mc = Minecraft.getInstance();
        st.touched = mc.level == null ? 0L : mc.level.getGameTime();
        if (st.rig != rig) {                                                       // another weapon in this hand: the chain starts at rest
            st.rig = rig;
            java.util.Arrays.fill(st.th, 0f);
            java.util.Arrays.fill(st.thv, 0f);
            st.lastTime = Float.NaN;
        }
        float dt = timeTicks - st.lastTime;
        if (Float.isNaN(st.lastTime) || dt < 0f || dt / 20f > SwingMath.RESET_GAP) {
            java.util.Arrays.fill(st.th, 0f);
            java.util.Arrays.fill(st.thv, 0f);
            st.lastTime = timeTicks;
            dt = 0f;
        }
        if (dt > 0f) {                                                             // dt is in ticks: the second draw of the same frame (the other pass, the other hand) has dt = 0 and reuses the state
            float u = holder.getAttackAnim(partial);
            float rate = Math.max(0f, holder.attackAnim - holder.oAttackAnim) * 20f;
            SwingMath.step(st.th, st.thv, rig.n, rig.w, rig.zeta, rig.gain, rig.maxRad, SwingMath.armAccel(u, rate), dt / 20f);
            st.lastTime = timeTicks;
        }
        float move = Math.min(1f, holder.walkAnimation.speed());
        float t = timeTicks / 20f;
        for (int i = 0; i < rig.n; i++) {
            Matrix4f m = MAT[i];
            Matrix3f nm = NRM[i];
            if (i == 0) {
                m.set(base);
                nm.set(baseN);
            } else {
                m.set(MAT[i - 1]);
                nm.set(NRM[i - 1]);
            }
            float a = st.th[i] + SwingMath.IDLE_FRACTION * rig.maxRad[i] * SwingMath.idle(t, i, st.phase, move);
            float psi = SwingMath.FLUTTER * st.thv[i] / rig.w[i];
            m.translate(rig.px[i], rig.py[i], rig.pz[i]).rotate(a, rig.ax[i], rig.ay[i], rig.az[i]).rotate(psi, rig.bx[i], rig.by[i], rig.bz[i]).translate(-rig.px[i], -rig.py[i], -rig.pz[i]);
            nm.rotate(a, rig.ax[i], rig.ay[i], rig.az[i]).rotate(psi, rig.bx[i], rig.by[i], rig.bz[i]);
        }
        return true;
    }

    /** Draws a layer of a rigged mesh: the rigid head with {@code base}, the skinned tail with the bone matrices {@link #prepare} just filled. */
    public static void emit(SwingRig rig, int layer, List<float[]> verts, VertexConsumer vc, Matrix4f base, Matrix3f baseN, int argb, int light, int overlay) {
        short[] codes = rig.skin[layer];
        int from = codes == null ? verts.size() : Math.min(rig.skinFrom[layer], verts.size());
        com.newuniverse.nusmp.client.WeaponRenderer.emitRange(base, baseN, vc, verts, 0, from, argb, light, overlay);
        if (codes == null) return;
        int a = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        int n = Math.min(codes.length, verts.size() - from);
        for (int k = 0; k < n; k++) {
            float[] v = verts.get(from + k);
            short code = codes[k];
            int bi = SwingRig.bone(code);
            float wt = SwingRig.weight(code);
            MAT[bi].transformPosition(v[0], v[1], v[2], P0);
            NRM[bi].transform(v[5], v[6], v[7], N0);
            if (wt > 0f) {
                MAT[bi + 1].transformPosition(v[0], v[1], v[2], P1);
                NRM[bi + 1].transform(v[5], v[6], v[7], N1);
                P0.lerp(P1, wt);
                N0.lerp(N1, wt);
            }
            N0.normalize();
            vc.addVertex(P0.x, P0.y, P0.z).setColor(r, g, b, a).setUv(v[3] / 128f, v[4] / 128f).setOverlay(overlay).setLight(light).setNormal(N0.x, N0.y, N0.z);
        }
    }
}
