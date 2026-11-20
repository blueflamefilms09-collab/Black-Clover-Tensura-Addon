package com.newuniverse.nusmp.client.upgrade;

import com.newuniverse.nusmp.client.relics.item.HolderTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.List;
import java.util.UUID;

/**
 * Why: Hellblaze's glow takes a purple tone on every hit (spec field {@code hit_tone}, {@link HellblazeTone}). The renderer is not told about hits, so this watches them on the client with what the game already syncs:
 * the damage event of the server marks a hurt entity ({@code hurtTime}) and remembers its damage source ({@code getLastDamageSource}); a hit of a wielder is a victim whose source is that wielder. No packet, no server code.
 * <p>
 * Bounded: only players holding Hellblaze within {@link #RANGE} blocks, at most {@link #SLOTS} of them (a slot per player UUID, so the effect is capped per player at one tone that a new hit only restarts), and a victim scan
 * (one small box query) only while that player swung in the last {@link #SWING_WINDOW} ticks. Slots are freed when the player stops holding it, and cleared on logout. The render thread only reads: {@link #tone}.
 */
public final class HellblazeHits {
    private HellblazeHits() {}

    public static final String ID = "hellblaze_the_demon_sin";
    static final int SLOTS = 8, SWING_WINDOW = 12;
    static final double RANGE = 48.0, REACH = 5.0;

    private static final UUID[] who = new UUID[SLOTS];
    private static final long[] hitAt = new long[SLOTS], swungAt = new long[SLOTS];
    private static final boolean[] live = new boolean[SLOTS];
    private static final boolean[] seen = new boolean[SLOTS];

    public static boolean holds(Player p) {
        return ID.equals(BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).getPath());
    }

    private static int slot(UUID id, boolean create) {
        int free = -1;
        for (int i = 0; i < SLOTS; i++) {
            if (live[i] && who[i].equals(id)) return i;
            if (!live[i] && free < 0) free = i;
        }
        if (!create || free < 0) return -1;
        who[free] = id;
        live[free] = true;
        hitAt[free] = Long.MIN_VALUE / 2;
        swungAt[free] = Long.MIN_VALUE / 2;
        return free;
    }

    public static void clear() {
        for (int i = 0; i < SLOTS; i++) {
            live[i] = false;
            who[i] = null;
        }
    }

    public static void onClientTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            clear();
            return;
        }
        if (mc.isPaused() || !UpgradeConfig.hellblazeKatana()) return;
        long now = level.getGameTime();
        for (int i = 0; i < SLOTS; i++) seen[i] = false;
        List<AbstractClientPlayer> players = level.players();
        for (int n = 0; n < players.size(); n++) {
            AbstractClientPlayer p = players.get(n);
            if (p.isSpectator() || !holds(p) || p.distanceToSqr(mc.player) > RANGE * RANGE) continue;
            int s = slot(p.getUUID(), true);
            if (s < 0) continue;                                                           // more wielders than slots: the rest simply show no tone
            seen[s] = true;
            if (p.swinging) swungAt[s] = now;
            if (now - swungAt[s] > SWING_WINDOW) continue;
            AABB box = p.getBoundingBox().inflate(REACH);
            List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, box);
            for (int k = 0; k < near.size(); k++) {
                LivingEntity v = near.get(k);
                if (v == p || v.hurtTime <= 0 || v.hurtTime < v.hurtDuration - 1) continue;      // damaged on this tick (the entity tick has already counted it down once)
                DamageSource src = v.getLastDamageSource();
                if (src != null && src.getEntity() == p) hitAt[s] = now;
            }
        }
        for (int i = 0; i < SLOTS; i++) {
            if (live[i] && !seen[i]) {
                live[i] = false;
                who[i] = null;
            }
        }
    }

    /** The purple tone (0..1) of the Hellblaze being drawn in this display context: the holder is the local player in first person, else the entity being rendered ({@link HolderTracker}); 0 for anything else. */
    public static float tone(ItemDisplayContext ctx, float time, int ticks) {
        if (ticks <= 0 || ctx == ItemDisplayContext.GUI) return 0f;
        LivingEntity holder = HolderTracker.holderFor(ctx);
        if (holder == null) return 0f;
        int s = slot(holder.getUUID(), false);
        return s < 0 ? 0f : HellblazeTone.tone(time - hitAt[s], ticks);
    }
}
