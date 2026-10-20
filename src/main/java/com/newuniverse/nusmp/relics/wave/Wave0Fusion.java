package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.item.MagicWeaponItem;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * The Boreal Blades' Fusion: with the Permafrost Sabre in one hand and the Cyclone Rapier in the other, both attuned, the wielder interlocks
 * them and cuts downward: a whirlwind of frost forms in front of them.
 * <ul>
 *   <li>It is a 2.2 block sphere that lasts 12 seconds; using the Fusion again while it stands moves it up to 9 blocks toward where you aim
 *       (the "bonus action"), and using it while sneaking dismisses it. One whirlwind per wielder.</li>
 *   <li>A creature inside it saves against Strength (DC 16; with disadvantage if it stood within 1.6 blocks of the wielder when it was cut).
 *       On a failure it takes 2d6 cold and 2d6 force damage and, if there is room (two medium creatures or one large one), is held in it.</li>
 *   <li>A held creature takes the same damage at the end of every second and saves again to break free (then it cannot be caught again for
 *       two seconds).</li>
 *   <li>Twice per Minecraft day, because a long rest is a day; the two uses are counted on the player.</li>
 * </ul>
 * Damage goes through the core's rules (PvP caps), the work per step is bounded, and nothing here changes a block.
 */
final class Wave0Fusion {
    private Wave0Fusion() {}

    static final int DC = 16, PER_DAY = 2, SAVE_EVERY = 20;
    private static final int ICE = 0xFFAEEBFF;

    private static final class Whirl {
        final ServerLevel level;
        final long end;
        double x, y, z, tx, tz;
        final Map<UUID, Long> held = new HashMap<>(), immune = new HashMap<>();

        Whirl(ServerLevel level, Vec3 at, long end) {
            this.level = level;
            this.end = end;
            x = tx = at.x;
            y = at.y;
            z = tz = at.z;
        }
    }

    private static final Map<UUID, Whirl> WHIRLS = new HashMap<>();

    static boolean standing(ServerPlayer p) {
        Whirl w = WHIRLS.get(p.getUUID());
        return w != null && p.level().getGameTime() < w.end;
    }

    static void dismiss(UUID owner) { WHIRLS.remove(owner); }

    static void clear() { WHIRLS.clear(); }

    /** Both Boreal Blades are in the hands (either way round) and both attuned. */
    static boolean paired(ServerPlayer p) {
        if (!RelicFlags.has(p, Wave0.F_SABRE) || !RelicFlags.has(p, Wave0.F_RAPIER)) return false;
        return isKind(p.getMainHandItem(), MagicWeaponItem.Kind.PERMAFROST_SABRE) && isKind(p.getOffhandItem(), MagicWeaponItem.Kind.CYCLONE_RAPIER)
                || isKind(p.getMainHandItem(), MagicWeaponItem.Kind.CYCLONE_RAPIER) && isKind(p.getOffhandItem(), MagicWeaponItem.Kind.PERMAFROST_SABRE);
    }

    private static boolean isKind(ItemStack s, MagicWeaponItem.Kind k) { return s.getItem() instanceof MagicWeaponItem w && w.kind == k; }

    /** The act: cut a whirlwind, or (while it stands) move it, or (sneaking) dismiss it. */
    static void fuse(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        Whirl w = WHIRLS.get(p.getUUID());
        if (w != null && sl.getGameTime() < w.end && w.level == sl) {
            if (p.isShiftKeyDown()) {
                WHIRLS.remove(p.getUUID());
                Wave0Util.note(p, "You let the whirlwind die.", ChatFormatting.AQUA);
                return;
            }
            Vec3 aim = GrimoireBook.aim(p, 24);
            double[] g = Wave0Rules.clampMove(w.x, w.z, aim.x, aim.z);
            w.tx = g[0];
            w.tz = g[1];
            w.y = aim.y;
            Wave0Util.note(p, "The whirlwind drifts where you point.", ChatFormatting.AQUA);
            return;
        }
        if (!paired(p)) {
            GrimoireBook.fail(p, "The Fusion needs the Permafrost Sabre and the Cyclone Rapier, one in each hand, both attuned.");
            return;
        }
        if (!Wave0Util.takeDaily(p, "fusion", PER_DAY)) {
            GrimoireBook.fail(p, "The blades have done this twice since dawn: they must rest until the next day.");
            return;
        }
        Vec3 look = p.getViewVector(1f).multiply(1, 0, 1);
        look = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
        Vec3 at = p.position().add(look.scale(3.0));
        Whirl nw = new Whirl(sl, at, sl.getGameTime() + Wave0Rules.WHIRL_TICKS);
        WHIRLS.put(p.getUUID(), nw);
        sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4f, 0.6f);
        sl.playSound(null, p.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 1.7f);
        Wave0Util.send(sl, VfxShape.WIND_RING, p.position().add(0, 1, 0), at.add(0, 1, 0), ICE, 14, 1.6f);
        Fx.lendSkills(p, Wave0Rules.WHIRL_TICKS, "tensura:wind_manipulation", "tensura:ice_breath");   // wind and frost, merged into one whirlwind
        Wave0Util.note(p, "Boreal Fusion! (use it again to move it, sneak to dismiss it)", ChatFormatting.AQUA);
        final Vec3 cut = p.position();
        SpellRuntime.zone(sl, Wave0Rules.WHIRL_TICKS, Wave0Rules.WHIRL_STEP, age -> {
            try {
                step(p.getUUID(), nw, age, cut);
            } catch (RuntimeException failed) {
                WHIRLS.remove(p.getUUID());
            }
        });
    }

    private static void step(UUID owner, Whirl w, int age, Vec3 cut) {
        if (WHIRLS.get(owner) != w) return;
        ServerLevel sl = w.level;
        if (sl.getGameTime() >= w.end) { WHIRLS.remove(owner); return; }
        ServerPlayer p = sl.getServer().getPlayerList().getPlayer(owner);
        if (p == null || !p.isAlive() || p.level() != sl) { WHIRLS.remove(owner); return; }
        long now = sl.getGameTime();

        double[] s = Wave0Rules.stepToward(w.x, w.z, w.tx, w.tz, Wave0Rules.WHIRL_SPEED * Wave0Rules.WHIRL_STEP);
        w.x = s[0];
        w.z = s[1];
        Vec3 c = new Vec3(w.x, w.y, w.z);
        if (age % 8 == 0) {
            Wave0Util.send(sl, VfxShape.WIND_TORNADO, c, c, ICE, 14, 2.4f);
            com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_0, FxTone.LIGHT, c.x, c.y + 1.0, c.z, 12, Wave0Rules.WHIRL_RADIUS * 0.5, 0.9, Wave0Rules.WHIRL_RADIUS * 0.5, 0.05);
        }
        w.held.keySet().removeIf(id -> sl.getEntity(id) == null);
        w.immune.values().removeIf(until -> until <= now);

        double r = Wave0Rules.WHIRL_RADIUS;
        AABB box = new AABB(c.x - r, c.y - 1.0, c.z - r, c.x + r, c.y + 2.5, c.z + r);
        List<LivingEntity> inside = new ArrayList<>(sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> Wave0Util.fair(p, e) && Math.hypot(e.getX() - c.x, e.getZ() - c.z) <= r + e.getBbWidth() * 0.5));
        if (inside.size() > 12) inside.subList(12, inside.size()).clear();

        int used = 0;
        for (LivingEntity e : inside) if (w.held.containsKey(e.getUUID())) used += Wave0Rules.sizeWeight(e.getBbWidth(), e.getBbHeight());
        boolean second = age % SAVE_EVERY == 0;
        for (LivingEntity e : inside) {
            UUID id = e.getUUID();
            if (w.held.containsKey(id)) {                                                  // held fast: pulled to the middle, and cut every second
                Vec3 pull = new Vec3(c.x - e.getX(), 0, c.z - e.getZ()).scale(0.3);
                e.setDeltaMovement(pull.x, Math.min(0.0, e.getDeltaMovement().y), pull.z);
                e.hurtMarked = true;
                if (second) {
                    cut(p, e);
                    if (Wave0Util.saves(e, DC, 0)) {
                        w.held.remove(id);
                        w.immune.put(id, now + 40);
                        Wave0Util.send(sl, VfxShape.KOTO_SHATTER, e.getBoundingBox().getCenter(), e.position(), ICE, 10, 0.8f);
                    }
                }
            } else if (second && !w.immune.containsKey(id)) {                              // caught: a Strength save
                boolean close = e.position().distanceToSqr(cut) <= 1.6 * 1.6;
                if (Wave0Util.saves(e, DC, close ? -1 : 0)) {
                    w.immune.put(id, now + 20);
                    continue;
                }
                cut(p, e);
                int weight = Wave0Rules.sizeWeight(e.getBbWidth(), e.getBbHeight());
                if (e.isAlive() && Wave0Rules.fits(used, weight)) {
                    used += weight;
                    w.held.put(id, now);
                }
            }
        }
    }

    /** The whirlwind's bite: 2d6 cold and 2d6 force. */
    private static void cut(ServerPlayer p, LivingEntity e) {
        if (e instanceof net.minecraft.world.entity.player.Player) {                                 // one capped blow on a player (the PvP cap is per hit)
            Wave0Util.extra(p, e, Wave0Util.roll(p, 2, 6) + Wave0Util.roll(p, 2, 6), "minecraft:freeze");
        } else {
            Wave0Util.extra(p, e, Wave0Util.roll(p, 2, 6), "minecraft:freeze");
            Wave0Util.extra(p, e, Wave0Util.roll(p, 2, 6), "minecraft:magic");
        }
        e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 100, e.getTicksFrozen() + 80));
        Fx.addEffect(e, "tensura:frost", 40, 0);
        Wave0Util.send(p.serverLevel(), VfxShape.WATER_BURST, e.getBoundingBox().getCenter(), e.position(), ICE, 10, 0.6f);
    }
}
