package com.newuniverse.nusmp.client.relics.djinn;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.playermodel.GsLooks;
import com.newuniverse.nusmp.client.relics.djinn.DjinnClientState.Snap;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Why: the boss-safe adapter of the player's Djinn Equip (docs/boss_redesign_goku_sinbad.md). A boss has no Magoi, no vessel and no network packet of the player's Djinn system, but the owner wants Sinbad to
 * play the very animation a player plays: so this layer builds a {@link Snap} for the boss from ONE synced number (the djinn he wears, written by the server) and hands it to {@link DjinnArmorLayer#draw},
 * the player layer's own drawing. The player's timeline is reproduced client side from the moment the number changes: the Lesser Summon's bust (12 ticks), the Half Equip (the main arm's pauldron,
 * vambrace, gauntlet, crest and the djinn's weapon forge in, 14 ticks), then the Full Equip (helm, horns, chest, belt, mantle, greaves, wings, tail, halo and the piece behind the back assemble with their
 * delays, with the body-art pass and the fresnel aura), and when the number falls back to 0 the set shatters out. An Extreme Magic moment (the boss's ultimate) lifts the phase to 4 (a brighter aura and a
 * pulse). The equip burst of the player ({@link DjinnClient#burst}) plays on every change.
 *
 * <p>Client only; read-only on the entity (it asks two functions the body gave it). Cost: nothing but {@link DjinnArmorLayer}'s own vertex writes (the player's budgets: nothing beyond 48 blocks, no small
 * details beyond 24), one small map entry per boss that has been seen (cleared past 64 entries), a {@link Snap} record only when the phase changes (never per frame).
 */
public class DjinnBossLayer<T extends LivingEntity> extends RenderLayer<T, PlayerModel<T>> {
    /** Ticks of the Lesser Summon and of the Half Equip before the Full Equip starts (the player's phases, played in order). */
    static final int LESSER_TICKS = 12, HALF_TICKS = 14, SHATTER_TICKS = 24;

    /** One boss's playing state. */
    static final class St {
        int idx;                // the djinn number being shown (0 = none)
        long t0;                // the game tick the current sequence began
        int phase = -1;         // the phase the cached snap was built for
        boolean extreme;
        Snap snap;
        long gone = Long.MIN_VALUE;   // the tick a shatter began
        int prevIdx;
    }

    private static final Map<Integer, St> STATE = new HashMap<>();
    private static final Djinn[] DJINN = new Djinn[GsLooks.DJINN_NAME.size()];

    private final ToIntFunction<T> equipIdx;
    private final Predicate<T> extreme;

    public DjinnBossLayer(RenderLayerParent<T, PlayerModel<T>> parent, ToIntFunction<T> equipIdx, Predicate<T> extreme) {
        super(parent);
        this.equipIdx = equipIdx;
        this.extreme = extreme;
    }

    private static Djinn djinn(int idx) {
        if (idx <= 0 || idx >= DJINN.length) return null;
        Djinn d = DJINN[idx];
        if (d == null) {
            try {
                d = DJINN[idx] = Djinn.valueOf(GsLooks.DJINN_NAME.get(idx));
            } catch (IllegalArgumentException missing) {
                return null;
            }
        }
        return d;
    }

    /** Looks at the synced number once per game tick: a change starts the sequence of the new djinn (or the shatter of the old one). Returns the state of this boss. */
    static St observe(int entityId, int idx, long gt) {
        St s = STATE.get(entityId);
        if (s == null) {
            if (STATE.size() > 64) STATE.clear();
            s = new St();
            s.idx = 0;
            s.t0 = gt;
            STATE.put(entityId, s);
        }
        if (idx != s.idx) {
            if (idx == 0) s.gone = gt;
            else s.gone = Long.MIN_VALUE;
            s.prevIdx = s.idx;
            s.idx = idx;
            s.t0 = gt;
            s.phase = -1;
            Minecraft mc = Minecraft.getInstance();
            Djinn d = djinn(idx != 0 ? idx : s.prevIdx);
            if (mc.level != null && d != null) DjinnClient.burst(entityId, d, idx != 0, false);
        }
        return s;
    }

    /**
     * The djinn number the BODY should wear now: the new djinn's body only from the Half Equip on (the Lesser Summon still shows the old body), the old body until the shatter is under way. A pure
     * function of the timeline, so the body (look) and the armour (layer) agree.
     */
    public static int bodyIdx(int entityId, int idx, long gt) {
        St s = observe(entityId, idx, gt);
        if (s.idx != 0 && gt - s.t0 < LESSER_TICKS) return s.prevIdx != 0 && s.prevIdx != s.idx ? s.prevIdx : 0;
        return s.idx;
    }

    /** Drops what was remembered (a world change). */
    public static void clear() { STATE.clear(); }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float limbSwing, float limbSwingAmount, float partial, float age, float netHeadYaw, float headPitch) {
        if (e.isInvisible() || e.isSpectator()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long gt = mc.level.getGameTime();
        St s = observe(e.getId(), equipIdx.applyAsInt(e), gt);
        Djinn d = djinn(s.idx != 0 ? s.idx : s.prevIdx);
        if (d == null) return;
        long el = gt - s.t0;
        int phase, prev;
        long since;
        if (s.idx == 0) {                                                       // shattering out (or long gone)
            if (el > SHATTER_TICKS) return;
            phase = 0;
            prev = 3;
            since = s.t0;
        } else if (el < LESSER_TICKS) {
            phase = 1;
            prev = 0;
            since = s.t0;
        } else if (el < LESSER_TICKS + HALF_TICKS) {
            phase = 2;
            prev = 1;
            since = s.t0 + LESSER_TICKS;
        } else {
            boolean ex = extreme.test(e);
            phase = ex ? 4 : 3;
            prev = ex && s.phase == 3 ? 3 : 2;
            since = s.t0 + LESSER_TICKS + HALF_TICKS;
            if (ex != s.extreme) {
                s.extreme = ex;
                s.snap = null;
                if (ex) since = gt;
            }
        }
        if (s.snap == null || s.phase != phase) {
            s.phase = phase;
            long pulse = phase == 4 ? gt : -1;
            s.snap = new Snap(phase, prev, d, gt, since, phase == 1 ? LESSER_TICKS : 0, 0, 0, 0, 0, 0, pulse, 0);
        }
        DjinnArmorLayer.draw(pose, buffers, light, e, getParentModel(), s.snap, partial);
    }
}
