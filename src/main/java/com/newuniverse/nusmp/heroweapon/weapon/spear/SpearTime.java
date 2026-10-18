package com.newuniverse.nusmp.heroweapon.weapon.spear;

import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Moves;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Tasks;
import com.newuniverse.nusmp.heroweapon.weapon.spear.RewindBuffer.Snap;
import com.newuniverse.nusmp.quests.NusmpEvents;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the unique property of the Legendary Spear in the series is Time Reversal (the spear's own power sends its hero back in time when a Legendary Hero
 * dies), and the owner asked for it as a real, readable, bounded mechanic. Here it is: while the weapon is summoned and the form holds the Memory of the
 * Hours the wielder's past is written down once a second (where, how healthy, in which world, burning or not, which harmful effects), and the ultimate
 * Time Reversal returns the WIELDER to the oldest safe moment of the last eight seconds. It never rewinds the world or anyone else: only the wielder's place,
 * a quarter of their health at most, and the harmful effects they gained since. The snapshots are in memory only (a rewind across a relog would be a bug,
 * not a feature), per player, and dropped at logout, at respawn, when the weapon is put away and when the server stops.
 */
final class SpearTime {
    private SpearTime() {}

    private static final Map<UUID, RewindBuffer> BUFFERS = new HashMap<>();
    private static boolean registered;

    /** Starts the cleaning of the memory: at logout, at respawn and when the server stops. Once. */
    static synchronized void registerEvents() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() != null) BUFFERS.remove(e.getEntity().getUUID());
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> {          // a new body has no past: a death must not be rewound
            if (e.getEntity() != null) BUFFERS.remove(e.getEntity().getUUID());
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> BUFFERS.clear());
    }

    /** Forgets one wielder's past (the weapon was put away, or the bond ended). */
    static void forget(UUID id) { BUFFERS.remove(id); }

    /** Test seam and self-test: the wielder's buffer, created when asked. */
    static RewindBuffer buffer(UUID id) { return BUFFERS.computeIfAbsent(id, k -> new RewindBuffer(SpearNumbers.REWIND_CAPACITY)); }

    // ================================================================ the memory
    /** Every few ticks while the weapon is summoned: write the wielder's present into the buffer when a second has passed. */
    static void remember(HeroCtx c) {
        ServerPlayer p = c.player;
        if (!p.isAlive() || p.isSpectator() || p.isPassenger()) return;
        long now = c.level.getGameTime();
        RewindBuffer buf = buffer(p.getUUID());
        if (!buf.due(now, SpearNumbers.REWIND_STEP)) return;
        Set<String> harmful = new HashSet<>();
        for (MobEffectInstance e : p.getActiveEffects()) if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) harmful.add(e.getEffect().getRegisteredName());
        buf.push(new Snap(now, p.getX(), p.getY(), p.getZ(), p.getHealth(), c.level.dimension().location().toString(), p.isOnFire(), harmful));
    }

    // ================================================================ Time Reversal
    static boolean rewind(HeroCtx c) {
        ServerPlayer p = c.player;
        RewindBuffer buf = BUFFERS.get(p.getUUID());
        if (buf == null || buf.size() == 0) {
            c.say("The minute hand has no past to turn back to yet.");
            return false;
        }
        if (p.isPassenger()) {
            c.say("You cannot turn time back while riding.");
            return false;
        }
        long now = c.level.getGameTime();
        RewindPlanner.Plan plan = RewindPlanner.choose(buf, now, SpearNumbers.REWIND_WINDOW, SpearNumbers.REWIND_MIN_AGE,
                c.level.dimension().location().toString(), Moves.probe(c.level, p), SpearNumbers.REWIND_RADIUS);
        if (plan == null) {
            c.say(RewindPlanner.hasPast(buf, now, SpearNumbers.REWIND_WINDOW, SpearNumbers.REWIND_MIN_AGE)
                    ? "There is no safe footing to return to in the last " + (SpearNumbers.REWIND_WINDOW / 20) + " seconds."
                    : "The minute hand has not run long enough yet: keep the spear in hand a few seconds more.");
            return false;
        }
        Snap then = plan.snap();
        Vec3 from = p.position();
        Vec3 to = plan.exact() ? Moves.feet(c.level, then.x(), plan.spot().y(), then.z()) : Moves.feet(c.level, plan.spot().x() + 0.5, plan.spot().y(), plan.spot().z() + 0.5);
        c.fx(VfxShape.TIME_REWIND, from.add(0.0, 0.1, 0.0), from.add(0.0, 1.0, 0.0), SpearFx.light(c), 40, 1.8f);
        if (!Moves.teleport(p, to)) {
            c.say("Time would not let go of you.");
            return false;
        }
        c.fx(VfxShape.TIME_REWIND, to.add(0.0, 0.1, 0.0), to.add(0.0, 1.0, 0.0), SpearFx.light(c), 40, 1.8f);
        SpearFx.flare(c, to.add(0.0, 1.0, 0.0), 1.4f);

        float back = RewindPlanner.healBack(p.getHealth(), then.health(), p.getMaxHealth(), SpearNumbers.REWIND_HEAL_CAP);
        if (back > 0f) c.heal(p, back);
        for (MobEffectInstance e : new ArrayList<>(p.getActiveEffects())) {
            if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL && !then.harmful().contains(e.getEffect().getRegisteredName())) p.removeEffect(e.getEffect());
        }
        if (!then.onFire() && p.isOnFire()) p.setRemainingFireTicks(0);
        if (p.getTicksFrozen() > 0) p.setTicksFrozen(0);
        buf.clear();
        SpearFx.sound(c, to, SoundEvents.BEACON_DEACTIVATE, 1.2f, 1.8f);
        SpearFx.sound(c, to, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, 0.6f);
        Tasks.later(c, 6, () -> SpearFx.sound(c, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 0.7f));
        c.say("Time turns back.");
        NusmpEvents.fire(p, "hero.rewind");
        return true;
    }
}
