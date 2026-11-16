package com.newuniverse.nusmp.client.playermodel;

import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxTone;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: the reference model has a faint sparkle round it that makes the character look alive; the redesigned Black Clover looks get the same (docs/bc_playermodels.md) from the mod's own particle
 * facade, so the theme, the colour and the quality levels are the mod's, not a new system. CLIENT only (called from the renderer), nothing is sent over the network.
 *
 * <p>Bounded (rule 11): at most ONE particle every {@link #PERIOD} ticks per boss (5 a second), only while the boss is drawn at full detail (within the detail distance, {@link PlayerLod#FULL}),
 * never for a dead or invisible one, and the facade applies its own client budget and quality level on top. The renderer is called several times per tick, so a tiny array remembers the last tick a
 * boss pulsed (keyed by entity id, 64 slots; two bosses that share a slot would only pulse a little less often). No per-frame allocation: the theme keys are resolved once.</p>
 */
public final class BcShimmer {
    private BcShimmer() {}

    /** Ticks between two pulses of one boss (a power of two). */
    public static final int PERIOD = 4;
    private static final int[] LAST = new int[64];
    private static final Map<String, ThemeKey> KEYS = new HashMap<>();

    private static ThemeKey key(String lookId) {
        ThemeKey k = KEYS.get(lookId);
        if (k == null) {
            String id = BcLooks.theme(lookId);
            if (id == null) return null;
            k = ThemeKey.of(id);
            KEYS.put(lookId, k);
        }
        return k;
    }

    /** Called once per frame by the renderer after the body is drawn; does something once per {@link #PERIOD} ticks. */
    public static void tick(LivingEntity e, PlayerLook look, int lod) {
        if (lod != PlayerLod.FULL || look == null || !BcLooks.isBc(look.id())) return;
        int t = e.tickCount;
        if ((t & (PERIOD - 1)) != 0) return;
        int slot = e.getId() & 63;
        if (LAST[slot] == t + 1) return;
        LAST[slot] = t + 1;
        if (e.isDeadOrDying() || e.isInvisible()) return;
        Level level = e.level();
        if (!level.isClientSide) return;
        ThemeKey k = key(look.id());
        if (k == null) return;
        RandomSource r = e.getRandom();
        double w = e.getBbWidth() * 0.9, h = e.getBbHeight();
        Fx.send(level, k, FxTone.LIGHT, e.getX() + (r.nextDouble() - 0.5) * w, e.getY() + r.nextDouble() * h, e.getZ() + (r.nextDouble() - 0.5) * w, 1, 0.12, 0.01);
    }
}
