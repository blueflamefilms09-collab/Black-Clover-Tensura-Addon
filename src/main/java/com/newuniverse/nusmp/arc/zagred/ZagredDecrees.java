package com.newuniverse.nusmp.arc.zagred;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.book.KotodamaWords;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.GrimoireDaemonEntity;
import com.newuniverse.nusmp.entity.ZagredAttacks;
import com.newuniverse.nusmp.entity.ZagredFx;
import com.newuniverse.nusmp.entity.zogratis.DanteMoves;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.quests.NusmpEvents;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntConsumer;

/**
 * Why: every Word Zagred speaks has a TELEGRAPH (what the floor and the screen show during the wind-up) and a DECREE (what lands when the wind-up ends), and
 * the two must use the same numbers or "leave the ring" would be a lie. This class holds both halves of every move, one pair of functions each, so the
 * entity class stays about the fight's state and rules and this one about what each Word does in the world. It is the remade boss's own use of his Words:
 * it does NOT call the players' Kotodama spells (the magic stays untouched); it borrows only what is public and neutral (the Tensura effects through
 * {@code EnergyBridge}, the control law {@code BalanceLaw}, the glyph effects {@code ZagredFx}, the letters' telegraph {@code ZagredAttacks.telegraph}).
 *
 * <p>The rules every decree follows: damage only through the boss's own law ({@code ZogratisBoss.strike}: the power tuning, the victim's EP, the balance
 * law's cap); a hard control only when {@code BalanceLaw.beginControl} allows it (one binding per twelve seconds) and then for the control law's short
 * time; no block is ever placed or broken (the Dungeon Guard holds every block of the Mega-Dungeon); everything that waits goes through a guarded
 * delayed task, so a mistake in one decree is logged once and never takes the server tick down.
 */
final class ZagredDecrees {
    private ZagredDecrees() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    static final int VIOLET = 0x8A4CFF, RED = 0xF2194D, PALE = 0xD8C4FF, INDIGO = 0x3A2A9A, WHITE = 0xFFFFFF;

    // ================================================================ small helpers
    /** A delayed task that logs a failure once and stops instead of throwing into the server tick. */
    static Runnable safe(String what, Runnable task) {
        boolean[] dead = {false};
        return () -> {
            if (dead[0]) return;
            try {
                task.run();
            } catch (Throwable t) {
                dead[0] = true;
                LOG.warn("[nusmp] Zagred's {} stopped: {}", what, t.toString());
            }
        };
    }

    /** The same for a zone step. */
    static IntConsumer safe(String what, IntConsumer step) {
        boolean[] dead = {false};
        return age -> {
            if (dead[0]) return;
            try {
                step.accept(age);
            } catch (Throwable t) {
                dead[0] = true;
                LOG.warn("[nusmp] Zagred's {} stopped: {}", what, t.toString());
            }
        };
    }

    static Vec3 flat(Vec3 v) { return new Vec3(v.x, 0, v.z); }

    static double flatDist(Entity e, Vec3 c) {
        double dx = e.getX() - c.x, dz = e.getZ() - c.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** The unit direction from the boss to a target on the floor plane, or the way he faces. */
    static Vec3 aim(ZagredBoss b, Entity target) {
        Vec3 d = flat(target.position().subtract(b.position()));
        if (d.lengthSqr() < 1e-4) d = flat(b.getViewVector(1f));
        return d.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : d.normalize();
    }

    /** Blocks between a foe's feet and the floor under them (4 when there is none within reach): a jump makes it positive. */
    static double floorHeight(ServerLevel sl, Entity e) {
        Vec3 from = new Vec3(e.getX(), e.getY() + 0.1, e.getZ());
        BlockHitResult hit = sl.clip(new ClipContext(from, from.add(0, -4.2, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e));
        return hit.getType() == HitResult.Type.MISS ? 4.0 : Math.max(0, e.getY() - hit.getLocation().y);
    }

    private static void dustRing(ServerLevel sl, Vec3 c, double r, DustParticleOptions d, int points) {
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2 / points;
            sl.sendParticles(d, c.x + Math.cos(a) * r, c.y + 0.15, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    private static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), size);
    }

    private static void shove(LivingEntity e, Vec3 from, double strength) {
        Vec3 away = flat(e.position().subtract(from));
        if (away.lengthSqr() < 1e-4) return;
        e.knockback(strength, -away.x, -away.z);
        e.hurtMarked = true;
    }

    /** A hard bind: Tensura paralysis and silence for {@code ticks}, with the old velocity lock so the legs hold too. The caller has the control law's permission. */
    static void bind(ServerLevel sl, LivingEntity t, int ticks) {
        EnergyBridge.effect(t, "paralysis", ticks, 1);
        EnergyBridge.effect(t, "silence", ticks * 2, 1);
        if (t instanceof Mob m) {
            m.getNavigation().stop();
            m.setTarget(null);
        }
        Vec3 at = t.position();
        SpellRuntime.zone(sl, ticks, 1, safe("bind", age -> {
            if (!t.isAlive()) return;
            if (t.position().distanceToSqr(at) > 0.04) t.teleportTo(at.x, Math.min(t.getY(), at.y + 0.1), at.z);
            t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
            t.hurtMarked = true;
        }));
    }

    /** A skill jam: Tensura silence and the mod's own skill lock for the control law's time. */
    static void jam(LivingEntity t, int ticks) {
        Nullification.jamAll(t, Math.max(1, ticks / 20), 0.5f);
        EnergyBridge.effect(t, "silence", ticks, 1);
    }

    /** Throws a foe to the ground: flight off, levitation off, a hard pull down, and no fall damage for the next moments. */
    static void pullDown(ServerLevel sl, LivingEntity t) {
        t.removeEffect(MobEffects.LEVITATION);
        t.removeEffect(MobEffects.SLOW_FALLING);
        if (t instanceof ServerPlayer sp && sp.getAbilities().flying && !sp.isCreative()) {
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
        }
        if (t instanceof Player pl) pl.stopFallFlying();
        boolean air = !t.onGround();
        t.setDeltaMovement(t.getDeltaMovement().x * 0.2, -2.5, t.getDeltaMovement().z * 0.2);
        t.hurtMarked = true;
        EnergyBridge.effect(t, "burden", BalanceLaw.controlTicks(t, 20), 1);
        if (air) SpellRuntime.zone(sl, 16, 1, safe("no fall damage", age -> { if (t.isAlive()) t.fallDistance = 0; }));
    }

    // ================================================================ the dispatcher
    static void tell(ZagredBoss b, ServerLevel sl, String id, LivingEntity target, int left, int total, boolean first) {
        switch (id) {
            case ZagredTable.CLAW -> clawTell(b, sl, target, left, total, first);
            case ZagredTable.HALT -> haltTell(b, sl, left, total, first);
            case ZagredTable.FALL -> fallTell(b, sl, left, total, first);
            case ZagredTable.TRIDENT -> tridentTell(b, sl, target, left, total, first);
            case ZagredTable.REDACT -> redactTell(b, sl, target, left, total, first);
            case ZagredTable.ARTS -> artsTell(b, sl, left);
            case ZagredTable.SWORDS -> swordsTell(b, sl, target, left, total, first);
            case ZagredTable.SEAL -> sealTell(b, sl, target, left, total, first);
            case ZagredTable.SHATTER -> shatterTell(b, sl, left, total, first);
            case ZagredTable.BANISH -> banishTell(b, sl, left, total, first);
            case ZagredTable.PAGES -> pagesTell(b, sl, left);
            case ZagredTable.REVERSE -> reverseTell(b, sl, left, total, first);
            case ZagredTable.VERDICT -> verdictTell(b, sl, left, total, first);
            case ZagredTable.OVERWRITE -> overwriteTell(b, sl, left, total, first);
            default -> { }
        }
    }

    static void run(ZagredBoss b, ServerLevel sl, String id, LivingEntity target) {
        switch (id) {
            case ZagredTable.CLAW -> claw(b, sl, target);
            case ZagredTable.HALT -> halt(b, sl);
            case ZagredTable.FALL -> fall(b, sl);
            case ZagredTable.TRIDENT -> trident(b, sl);
            case ZagredTable.REDACT -> redact(b, sl);
            case ZagredTable.ARTS -> arts(b, sl, target);
            case ZagredTable.SWORDS -> swords(b, sl);
            case ZagredTable.SEAL -> seal(b, sl);
            case ZagredTable.SHATTER -> shatter(b, sl);
            case ZagredTable.BANISH -> banish(b, sl);
            case ZagredTable.PAGES -> callPages(b, sl);
            case ZagredTable.REVERSE -> reverse(b, sl);
            case ZagredTable.VERDICT -> verdict(b, sl);
            case ZagredTable.OVERWRITE -> overwriteRelease(b, sl);
            default -> { }
        }
    }

    // ================================================================ Inked Claw
    private static void clawTell(ZagredBoss b, ServerLevel sl, LivingEntity target, int left, int total, boolean first) {
        Vec3 pos = b.position();
        if (first) {
            Vec3 dir = aim(b, target);
            boolean devil = b.gatesOpen();
            double reach = ZagredMath.clawReach(devil);
            ZagredFx.inkLane(sl, pos, pos.add(dir.scale(reach)), devil ? 2.8f : 2.2f, total + 4);
            b.plan.to = null;
            if (devil && b.distanceTo(target) > 4.5) {                       // the devil leaps: the landing is drawn where he will stand
                Vec3 land = ZogratisBoss.groundOrNull(sl, target.position().subtract(dir.scale(2.4)));
                b.plan.to = land;
                if (land != null) ZagredFx.ring(sl, land, (float) (3.2 / 0.7), total + 2, ZagredWord.OVERWRITE.argb());
            }
        }
        Tells.gather(sl, pos.add(0, b.getBbHeight() * 0.6, 0), 0.7, VIOLET, 4);
    }

    private static void claw(ZagredBoss b, ServerLevel sl, LivingEntity target) {
        boolean devil = b.gatesOpen();
        if (devil && b.plan.to != null && b.distanceTo(target) > 4.5) b.blinkTo(b.plan.to);
        double reach = ZagredMath.clawReach(devil);
        float base = devil ? ZagredMath.DEVIL_CLAW : ZagredMath.CLAW;
        b.swing(InteractionHand.MAIN_HAND);
        Vec3 facing = aim(b, target);
        Vec3 from = b.position().add(0, b.getBbHeight() * 0.65, 0);
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, from.add(facing.scale(reach * 0.6)), from.add(facing.scale(reach)), devil ? RED | 0xFF000000 : VIOLET | 0xFF000000, 12, devil ? 1.5f : 1.0f);
        sl.playSound(null, b.blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 1.4f, 0.7f);
        for (LivingEntity e : b.foes(reach + 2)) {
            Vec3 to = flat(e.position().subtract(b.position()));
            double d = to.length();
            if (d > reach + e.getBbWidth() / 2) continue;
            if (d > 1.2 && to.normalize().dot(facing) < (devil ? -0.2 : 0.1)) continue;
            if (b.strike(e, base, false)) shove(e, b.position(), 0.8);
        }
    }

    // ================================================================ Halt: the band
    private static void haltTell(ZagredBoss b, ServerLevel sl, int left, int total, boolean first) {
        Vec3 pos = b.position();
        double outer = ZagredMath.haltOuter(b.stage()), core = ZagredMath.haltCore();
        if (first) {
            ZagredFx.ring(sl, pos, (float) (outer / 0.7), total + 2, ZagredWord.HALT.argb());        // the glyph ring closes to 70% of its size: it starts bigger so it ends on the real edge
            ZagredFx.ring(sl, pos, (float) (core / 0.7), total + 2, 0xFFFFFFFF);
        }
        if (left % 6 == 0) {
            Tells.ring(sl, pos, outer, ZagredWord.HALT.rgb(), 1.5f, 56);
            Tells.ring(sl, pos, core, WHITE, 1.2f, 24);
        }
    }

    private static void halt(ZagredBoss b, ServerLevel sl) {
        Vec3 c = b.position();
        double outer = ZagredMath.haltOuter(b.stage()), core = ZagredMath.haltCore();
        List<LivingEntity> hit = new ArrayList<>();
        for (LivingEntity e : b.foes(outer + 2)) {
            if (!ZagredGeometry.inBand(flatDist(e, c), core, outer)) continue;
            if (e instanceof Player p && DanteMoves.antiMagicHeld(p)) {
                p.displayClientMessage(ZagredScreen.text("message.nusmp.zagred.held", "You hold the line against the word.", 0xB8FFC8, true), true);
                continue;
            }
            hit.add(e);
        }
        boolean bound = !hit.isEmpty() && BalanceLaw.beginControl(b);                  // the control law: one binding Word per twelve seconds, whatever is ready
        for (LivingEntity e : hit) {
            b.strike(e, ZagredMath.HALT, true);
            EnergyBridge.spirit(e, 2);
            if (bound) bind(sl, e, BalanceLaw.controlTicks(e, ZagredMath.haltBindTicks()));
        }
        VfxSpawn.sendFollowing(sl, VfxShape.KOTO_AURA, b, c, ZagredWord.HALT.argb(), 30, (float) (outer / 6));
        sl.playSound(null, b.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.5f, 0.6f);
    }

    // ================================================================ Fall: the pull and the wave
    private static void fallTell(ZagredBoss b, ServerLevel sl, int left, int total, boolean first) {
        if (first) ZagredFx.corrupt(sl, b.position(), (float) ZagredMath.fallPullReach(b.stage()), total + 4, ZagredWord.FALL.argb());
        if (left % 6 == 0) Tells.gather(sl, b.position().add(0, 0.4, 0), 1.6, INDIGO, 8);
    }

    private static void fall(ZagredBoss b, ServerLevel sl) {
        Vec3 o = b.position();
        for (LivingEntity t : b.foes(ZagredMath.fallPullReach(b.stage()))) {
            if (!t.onGround() && floorHeight(sl, t) > 1.6) pullDown(sl, t);                 // only real fliers: the answer to the wave is a jump, so a jump must not be weighed down
        }
        sl.playSound(null, b.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.2f, 0.5f);
        Set<UUID> hit = new HashSet<>();
        int rings = ZagredMath.fallRings(b.stage());
        for (int k = 0; k < rings; k++) {
            int kk = k;
            b.later(18 + 2 * k, safe("fall wave", () -> waveRing(b, sl, o, kk, hit)));
        }
    }

    private static void waveRing(ZagredBoss b, ServerLevel sl, Vec3 o, int k, Set<UUID> hit) {
        double r = ZagredMath.fallRingRadius(k);
        dustRing(sl, o, r, dust(0x8A6CFF, 1.7f), (int) Math.max(12, r * 3.5));      // bright enough to read on the dark floors of the dungeon: this ring is the one thing to jump
        for (LivingEntity e : b.foesAround(o, r + 2.5)) {
            if (!ZagredGeometry.onRing(flatDist(e, o), r, ZagredMath.fallBand())) continue;
            if (floorHeight(sl, e) > ZagredMath.fallHeight()) continue;             // jumped: the wave passes under
            if (!hit.add(e.getUUID())) continue;
            if (b.strike(e, ZagredMath.FALL, true)) shove(e, o, 0.8);
        }
        if (k == 0) sl.playSound(null, BlockPos.containing(o), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.4f, 0.4f);
    }

    // ================================================================ Trident: the lane
    private static void tridentTell(ZagredBoss b, ServerLevel sl, LivingEntity target, int left, int total, boolean first) {
        Vec3 pos = b.position();
        if (first) {
            Vec3 dir = aim(b, target);
            b.plan.from = pos.add(0, b.getBbHeight() * 0.7, 0);
            b.plan.to = b.plan.from.add(dir.scale(ZagredMath.tridentLength()));
            ZagredFx.inkLane(sl, pos, new Vec3(b.plan.to.x, pos.y, b.plan.to.z), (float) ZagredMath.tridentHalfWidth(), total + 4);
        }
        if (b.plan.from != null && b.plan.to != null && left % 6 == 0) Tells.line(sl, b.plan.from, b.plan.to, ZagredWord.TRIDENT.rgb());
    }

    private static void trident(ZagredBoss b, ServerLevel sl) {
        Vec3 from = b.plan.from, to = b.plan.to;
        if (from == null || to == null) return;
        VfxSpawn.send(sl, VfxShape.KOTO_TRIDENT, from, to, ZagredWord.TRIDENT.argb(), 14, 1.2f);
        ZagredFx.strike(sl, from, to, ZagredFx.PALE, 1.3f, 16);
        sl.playSound(null, b.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 1.5f, 0.5f);
        b.later(6, safe("trident", () -> {
            for (LivingEntity e : b.foesAround(from.add(to).scale(0.5), ZagredMath.tridentLength() / 2 + 3)) {
                if (Math.abs(e.getY() - from.y) > 3.5) continue;
                if (ZagredGeometry.distanceToSegment(e.getX(), e.getZ(), from.x, from.z, to.x, to.z) > ZagredMath.tridentHalfWidth() + e.getBbWidth() / 2) continue;
                if (b.strike(e, ZagredMath.TRIDENT, false)) {                          // a plain blow: a raised shield stops it
                    EnergyBridge.spirit(e, 2);
                    shove(e, from, 0.5);
                }
            }
        }));
    }

    // ================================================================ Redact: the sentence
    private static void redactTell(ZagredBoss b, ServerLevel sl, LivingEntity target, int left, int total, boolean first) {
        if (!first) return;
        Vec3 pos = b.position();
        Vec3 dir = aim(b, target);
        int n = ZagredMath.redactLetters(b.stage());
        int sentences = ZagredMath.sentences(b.gatesOpen());
        double bend = b.getRandom().nextBoolean() ? 1 : -1;
        b.plan.sentences.clear();
        for (int s = 0; s < sentences; s++) {
            double[] d = ZagredGeometry.rotate(dir.x, dir.z, s * 0.6);
            double[][] pts = ZagredGeometry.curve(pos.x, pos.z, d[0], d[1], n, s == 0 ? bend : -bend);
            List<Vec3> list = new ArrayList<>();
            for (double[] p : pts) list.add(ZogratisBoss.ground(sl, new Vec3(p[0], pos.y, p[1])));
            b.plan.sentences.add(list);
            ZagredAttacks.telegraph(b, list);
            Vec3 mid = list.get(list.size() / 2);
            ZagredFx.inkLane(sl, pos, mid, 1.8f, total + ZagredMath.letterGap() * n + 12);
        }
    }

    private static void redact(ZagredBoss b, ServerLevel sl) {
        int s = 0;
        for (List<Vec3> list : new ArrayList<>(b.plan.sentences)) {
            boolean[] cut = {false};
            int delay = s * 16;
            for (int k = 0; k < list.size(); k++) {
                int kk = k;
                b.later(delay + 2 + k * ZagredMath.letterGap(), safe("redact letter", () -> letter(b, sl, list, kk, cut)));
            }
            s++;
        }
    }

    private static void letter(ZagredBoss b, ServerLevel sl, List<Vec3> pts, int k, boolean[] cut) {
        if (cut[0]) return;
        Vec3 p = pts.get(k);
        boolean last = k == pts.size() - 1;
        for (Player pl : sl.getEntitiesOfClass(Player.class, new AABB(p, p).inflate(2.6))) {      // anti-magic swung on a letter cuts it and every letter after it
            if (pl.swinging && pl.swingTime <= 3 && DanteMoves.antiMagicHeld(pl)) {
                cut[0] = true;
                VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, p.add(0, 1, 0), p, 0xFFFFFFFF, 14, 1.6f);
                sl.playSound(null, BlockPos.containing(p), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5f, 1.4f);
                if (pl instanceof ServerPlayer sp) NusmpEvents.fire(sp, "zagred.cut", ZagredWord.REDACT.id());
                return;
            }
        }
        double r = ZagredMath.letterRadius(last);
        for (LivingEntity e : b.foesAround(p, r + 1)) {
            if (flatDist(e, p) > r || Math.abs(e.getY() - p.y) > 3) continue;
            b.strike(e, last ? ZagredMath.LETTER * ZagredMath.LAST_LETTER_FACTOR : ZagredMath.LETTER, true);
            EnergyBridge.spirit(e, 1);
        }
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, p.add(0, 0.8, 0), p, ZagredWord.REDACT.argb(), 16, (float) (r / 2.2));
        ZagredFx.strike(sl, p.add(0, 4.5, 0), p, last ? ZagredFx.RED : 0xFF000000 | VIOLET, (float) (r / 1.6), 12);
        sl.playSound(null, BlockPos.containing(p), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.2f, 0.5f);
        if (last) {                                                                                // the redacted patch: buffs struck out, mending stopped
            SpellRuntime.zone(sl, 80, 10, safe("redacted patch", age -> {
                for (LivingEntity e : b.foesAround(p, 4)) {
                    e.removeEffect(MobEffects.REGENERATION);
                    for (MobEffectInstance eff : new ArrayList<>(e.getActiveEffects()))
                        if (eff.getEffect().value().isBeneficial()) { e.removeEffect(eff.getEffect()); break; }
                }
            }));
        }
    }

    // ================================================================ Borrowed Art
    private static void artsTell(ZagredBoss b, ServerLevel sl, int left) {
        if (left % 6 == 0) Tells.gather(sl, b.position().add(0, b.getBbHeight() * 0.7, 0), 0.9, INDIGO, 6);
    }

    private static void arts(ZagredBoss b, ServerLevel sl, LivingEntity target) {
        if (!b.castTensura(target)) b.strike(target, ZagredMath.TRIDENT * 0.6f, true);               // no spell learned: a plain bolt of ruin instead
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, b.getEyePosition(), target.getBoundingBox().getCenter(), VIOLET | 0xFF000000, 20, 1.2f);
    }

    // ================================================================ Swords: the marked field
    private static void swordsTell(ZagredBoss b, ServerLevel sl, LivingEntity target, int left, int total, boolean first) {
        int slots = b.tuning().castSlots();
        if (first) {
            double[][] pts = ZagredGeometry.spots(target.getX(), target.getZ(), ZagredMath.swordCount(slots), ZagredMath.swordSpread(slots), 2.0, b.getRandom().nextLong());
            b.plan.marks.clear();
            for (double[] p : pts) b.plan.marks.add(ZogratisBoss.ground(sl, new Vec3(p[0], target.getY(), p[1])));
            ZagredFx.ring(sl, target.position(), (float) (ZagredMath.swordSpread(slots) / 0.7), total + 2, ZagredWord.SWORDS.argb());
        }
        if (left % 6 == 0) for (Vec3 m : b.plan.marks) Tells.ring(sl, m, ZagredMath.swordImpact(), ZagredWord.SWORDS.rgb(), 1.0f, 10);
    }

    private static void swords(ZagredBoss b, ServerLevel sl) {
        List<Vec3> marks = new ArrayList<>(b.plan.marks);
        for (int k = 0; k < marks.size(); k++) {
            Vec3 ground = marks.get(k);
            Vec3 sky = ground.add(b.getRandom().nextGaussian() * 1.5, 14, b.getRandom().nextGaussian() * 1.5);
            b.later(2 * k, safe("sword fall", () -> VfxSpawn.send(sl, VfxShape.KOTO_SWORDS, sky, ground, VIOLET | 0xFF000000, 14, 1f)));
            b.later(2 * k + 8, safe("sword hit", () -> {
                for (LivingEntity e : b.foesAround(ground, ZagredMath.swordImpact() + 1)) {
                    if (flatDist(e, ground) > ZagredMath.swordImpact() || Math.abs(e.getY() - ground.y) > 3) continue;
                    if (b.strike(e, ZagredMath.SWORD, false)) EnergyBridge.spirit(e, 1);                 // plain blows: a raised shield stops them
                }
                sl.playSound(null, BlockPos.containing(ground), SoundEvents.TRIDENT_HIT_GROUND, SoundSource.HOSTILE, 0.8f, 0.6f);
            }));
        }
    }

    // ================================================================ Seal: the chain
    private static void sealTell(ZagredBoss b, ServerLevel sl, LivingEntity target, int left, int total, boolean first) {
        if (first) {
            LivingEntity victim = strongestCaster(b, target);
            b.plan.victim = victim.getUUID();
            b.plan.from = b.getEyePosition();
            b.plan.to = victim.getBoundingBox().getCenter();
            Vec3 pos = b.position();
            ZagredFx.inkLane(sl, pos, new Vec3(b.plan.to.x, pos.y, b.plan.to.z), (float) ZagredMath.sealHalfWidth(), total + 4);
        }
        if (b.plan.from != null && b.plan.to != null && left % 3 == 0) Tells.line(sl, b.plan.from, b.plan.to, ZagredWord.SEAL.rgb());
    }

    /** The player with the most EP within reach (the caster the chain is for), else the target. */
    private static LivingEntity strongestCaster(ZagredBoss b, LivingEntity fallback) {
        LivingEntity best = fallback;
        double top = best instanceof Player ? Nullification.maxEP(best) : -1;
        for (LivingEntity e : b.foes(32)) {
            if (!(e instanceof Player)) continue;
            double ep = Nullification.maxEP(e);
            if (ep > top) { top = ep; best = e; }
        }
        return best;
    }

    private static void seal(ZagredBoss b, ServerLevel sl) {
        Vec3 from = b.plan.from, to = b.plan.to;
        if (from == null || to == null) return;
        Vec3 dir = to.subtract(from);
        if (dir.lengthSqr() < 1e-4) return;
        Vec3 end = from.add(dir.normalize().scale(dir.length() + 2));
        BlockHitResult wall = sl.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, b));
        if (wall.getType() != HitResult.Type.MISS) end = wall.getLocation();                      // a wall breaks the chain
        LivingEntity first = null;
        double nearest = Double.MAX_VALUE;
        for (LivingEntity e : b.foes(45)) {                                                       // the first body on the line takes it, a summon included
            Optional<Vec3> at = e.getBoundingBox().inflate(ZagredMath.sealHalfWidth()).clip(from, end);
            if (at.isEmpty()) continue;
            double d = at.get().distanceToSqr(from);
            if (d < nearest) { nearest = d; first = e; }
        }
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, from, end, ZagredWord.SEAL.argb(), 16, 0.9f);
        sl.playSound(null, b.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 1.5f, 0.5f);
        if (first == null) {
            VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, end, end.add(0, 0.5, 0), 0xFFFFFFFF, 14, 1.2f);   // the chain finds no one
            return;
        }
        if (!(first instanceof Player p)) {
            VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, first.getBoundingBox().getCenter(), first.position(), 0xFFFFFFFF, 14, 1.2f);   // a summon takes the chain and it breaks on it
            return;
        }
        if (DanteMoves.antiMagicHeld(p)) {
            p.displayClientMessage(ZagredScreen.text("message.nusmp.zagred.held", "You hold the line against the word.", 0xB8FFC8, true), true);
            VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, p.getBoundingBox().getCenter(), p.position(), 0xFFFFFFFF, 14, 1.2f);
            return;
        }
        b.strike(p, ZagredMath.SEAL, true);
        if (BalanceLaw.beginControl(b)) jam(p, BalanceLaw.controlTicks(p, 80));
    }

    // ================================================================ Shatter and Reverse: the defensive windows
    private static void shatterTell(ZagredBoss b, ServerLevel sl, int left, int total, boolean first) {
        if (first) ZagredFx.ring(sl, b.position(), (float) (ZagredMath.shatterRadius() / 0.7), total + 2, ZagredWord.SHATTER.argb());
        if (left % 6 == 0) Tells.ring(sl, b.position(), ZagredMath.shatterRadius(), ZagredWord.SHATTER.rgb(), 1.3f, 40);
    }

    private static void shatter(ZagredBoss b, ServerLevel sl) {
        b.shatterUntil = b.now() + ZagredMath.shatterTicks();
        VfxSpawn.sendFollowing(sl, VfxShape.KOTO_AURA, b, b.position(), ZagredWord.SHATTER.argb(), ZagredMath.shatterTicks(), 2.6f);
        sl.playSound(null, b.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5f, 0.5f);
    }

    private static void reverseTell(ZagredBoss b, ServerLevel sl, int left, int total, boolean first) {
        if (first) ZagredFx.ring(sl, b.position(), 4f, total + 2, ZagredWord.REVERSE.argb());
        if (left % 6 == 0) Tells.gather(sl, b.position().add(0, 1, 0), 1.1, PALE, 6);
    }

    private static void reverse(ZagredBoss b, ServerLevel sl) {
        b.reverseUntil = b.now() + ZagredMath.reverseTicks();
        VfxSpawn.sendFollowing(sl, VfxShape.KOTO_AURA, b, b.position(), ZagredWord.REVERSE.argb(), ZagredMath.reverseTicks(), 1.6f);
        ZagredFx.corrupt(sl, b.position(), 5f, ZagredMath.reverseTicks(), ZagredFx.PALE);
        sl.playSound(null, b.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.5f, 0.5f);
    }

    // ================================================================ Banish: clear the crowd
    private static void banishTell(ZagredBoss b, ServerLevel sl, int left, int total, boolean first) {
        double r = ZagredMath.banishRadius();
        if (first) ZagredFx.ring(sl, b.position(), (float) (r / 0.7), total + 2, ZagredWord.BANISH.argb());
        if (left % 6 == 0) Tells.ring(sl, b.position(), r, ZagredWord.BANISH.rgb(), 1.4f, 40);
    }

    private static void banish(ZagredBoss b, ServerLevel sl) {
        Vec3 c = b.position();
        double r = ZagredMath.banishRadius();
        for (LivingEntity e : b.foes(r + 1)) {
            if (flatDist(e, c) > r) continue;
            if (e instanceof Player p && DanteMoves.antiMagicHeld(p)) continue;
            b.strike(e, ZagredMath.BANISH, true);                                                  // summons are shoved too, never sent away
            shove(e, c, 1.7);
            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.25, 0));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
        }
        ZagredFx.corrupt(sl, c, (float) r, 24, ZagredWord.BANISH.argb());
        sl.playSound(null, b.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.5f, 0.5f);
    }

    // ================================================================ Call the Pages: the daemons
    private static void pagesTell(ZagredBoss b, ServerLevel sl, int left) {
        if (left % 6 == 0) {
            Tells.ring(sl, b.position(), 5, VIOLET);
            Tells.gather(sl, b.position().add(0, 1.2, 0), 1.0, PALE, 6);
        }
    }

    /** A spot on the ring round the arena's middle, on the floor, {@code up} blocks above it. */
    static Vec3 ringPos(ZagredBoss b, ServerLevel sl, double r, double up) {
        Vec3 c = b.arenaCenter();
        double a = b.getRandom().nextDouble() * Math.PI * 2;
        return ZogratisBoss.ground(sl, c.add(Math.cos(a) * r, 0, Math.sin(a) * r)).add(0, up, 0);
    }

    private static void callPages(ZagredBoss b, ServerLevel sl) {
        b.upkeepDaemons(sl);
        int stage = b.stage(), players = Math.max(1, b.fighterCount());
        int room = ZagredMath.lesserCap(stage, players) - b.lesserAlive;
        int made = 0;
        for (int i = 0; i < Math.min(ZagredMath.pagesPerCall(stage), room); i++) {
            Vec3 at = ringPos(b, sl, 8 + b.getRandom().nextDouble() * 5, 2.5);
            ZagredFx.ring(sl, at.subtract(0, 2.5, 0), 2f, 16, ZagredFx.INDIGO);
            if (GrimoireDaemonEntity.spawn(sl, b, GrimoireDaemonEntity.LESSER, at) != null) made++;
        }
        b.upkeepDaemons(sl);
        sl.playSound(null, b.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.HOSTILE, 1.5f, 0.5f);
        if (made > 0) b.announce("Pages tear loose from Zagred's book.");
    }

    /** Brings the greater and arch daemons the act allows and that are not already standing (the pages come with the move). */
    static void anchors(ZagredBoss b, ServerLevel sl) {
        b.upkeepDaemons(sl);
        int stage = b.stage(), players = Math.max(1, b.fighterCount());
        for (int i = b.greaterAlive; i < ZagredMath.greaterCap(stage, players); i++) {
            Vec3 at = ringPos(b, sl, 11, 0.8);
            ZagredFx.ring(sl, at.subtract(0, 0.8, 0), 3f, 20, ZagredFx.RED);
            GrimoireDaemonEntity.spawn(sl, b, GrimoireDaemonEntity.GREATER, at);
        }
        for (int i = b.archAlive; i < ZagredMath.archCap(stage); i++) {
            GrimoireDaemonEntity.spawn(sl, b, GrimoireDaemonEntity.ARCH, ringPos(b, sl, 8, 2.2));
        }
        b.upkeepDaemons(sl);
    }

    // ================================================================ Verdict: a word of your own
    private static void verdictTell(ZagredBoss b, ServerLevel sl, int left, int total, boolean first) {
        if (first) ZagredFx.corrupt(sl, b.position(), 14f, total + 4, ZagredWord.VERDICT.argb());
        if (left % 6 == 0) Tells.gather(sl, b.position().add(0, 1.4, 0), 1.2, RED, 6);
    }

    private static void verdict(ZagredBoss b, ServerLevel sl) {
        List<ServerPlayer> players = new ArrayList<>();
        for (LivingEntity e : b.foes(40)) if (e instanceof ServerPlayer sp) players.add(sp);
        int n = ZagredVerdict.targets(players.size(), b.stage());
        if (n == 0) return;
        java.util.Collections.shuffle(players, new java.util.Random(b.getRandom().nextLong()));
        LivingEntity current = b.getTarget();
        if (current instanceof ServerPlayer cp && players.remove(cp)) players.add(0, cp);       // the one he looks at is always named first
        List<ZagredWord> words = ZagredVerdict.assign(n, b.getRandom().nextLong());
        long now = b.now();
        int window = ZagredVerdict.windowTicks(b.stage());
        b.marks.clear();
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < n; i++) {
            ServerPlayer p = players.get(i);
            ZagredWord w = words.get(i);
            b.marks.add(new ZagredMark(p.getUUID(), w, now + window));
            ZagredScreen.personal(p, w, ZagredAnswers.told(w), window);
            ZagredFx.ring(sl, p.position(), 2.4f, window, w.argb());
            names.append(i == 0 ? "" : ", ").append(p.getGameProfile().getName()).append(" (").append(w.title()).append(")");
        }
        b.verdictUntil = now + window;
        ZagredScreen.bar(sl, b.position(), Component.literal("VERDICT: " + names).withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD));
        sl.playSound(null, b.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 2f, 0.5f);
    }

    /** Every tick while a verdict is open: watch the marked players; at the deadline judge them. */
    static void tickVerdict(ZagredBoss b, ServerLevel sl, long now) {
        if (b.marks.isEmpty()) return;
        b.marks.removeIf(m -> !(sl.getPlayerByUUID(m.id) instanceof ServerPlayer p) || !p.isAlive() || p.isSpectator());
        for (ZagredMark m : b.marks) if (sl.getPlayerByUUID(m.id) instanceof ServerPlayer p) m.observe(p);
        if (now >= b.verdictUntil) resolveVerdict(b, sl);
        else if (now % 10 == 0) for (ZagredMark m : b.marks) if (sl.getPlayerByUUID(m.id) instanceof ServerPlayer p) Tells.ring(sl, p.position(), 1.4, m.word.rgb(), 1.2f, 18);
    }

    private static void resolveVerdict(ZagredBoss b, ServerLevel sl) {
        List<ZagredMark> failed = new ArrayList<>();
        int answered = 0;
        for (ZagredMark m : new ArrayList<>(b.marks)) {
            if (!(sl.getPlayerByUUID(m.id) instanceof ServerPlayer p)) continue;
            if (ZagredAnswers.spared(m.word, m.snapshot(p, b))) {
                answered++;
                p.displayClientMessage(ZagredScreen.text("message.nusmp.zagred.answered", "Answered!", 0xB8FFC8, true), true);
                sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.HOSTILE, 0.6f, 1.6f);
                NusmpEvents.fire(p, "zagred.verdict", m.word.id());
            } else {
                failed.add(m);
            }
        }
        b.marks.clear();
        boolean wantsControl = false;
        for (ZagredMark m : failed) wantsControl |= m.word.hardControl();
        boolean control = wantsControl && BalanceLaw.beginControl(b);
        for (ZagredMark m : failed) if (sl.getPlayerByUUID(m.id) instanceof ServerPlayer p) verdictHit(b, sl, p, m.word, control);
        if (failed.isEmpty() && answered > 0) b.say(ZagredLines.ANSWERED[b.getRandom().nextInt(ZagredLines.ANSWERED.length)]);
    }

    private static void verdictHit(ZagredBoss b, ServerLevel sl, ServerPlayer p, ZagredWord w, boolean control) {
        Vec3 at = p.position();
        switch (w) {
            case HALT -> {
                b.strike(p, ZagredMath.VERDICT_HALT, true);
                if (control) bind(sl, p, BalanceLaw.controlTicks(p, 20));
            }
            case FALL -> {
                pullDown(sl, p);
                b.strike(p, ZagredMath.VERDICT_FALL, true);
                ZagredFx.corrupt(sl, at, 5f, 24, w.argb());
            }
            case SEAL -> {
                b.strike(p, ZagredMath.VERDICT_SEAL, true);
                if (control) jam(p, BalanceLaw.controlTicks(p, 60));
            }
            case SWORDS -> {
                VfxSpawn.send(sl, VfxShape.KOTO_SWORDS, at.add(0, 14, 0), at, w.argb(), 14, 1f);
                b.strike(p, ZagredMath.VERDICT_SWORDS, false);                                       // a plain blow: shield first, next time
            }
            case COWER -> {
                b.strike(p, ZagredMath.VERDICT_COWER, true);
                if (control) {
                    EnergyBridge.effect(p, "fear", BalanceLaw.controlTicks(p, 40), 1);
                    p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
                }
            }
            default -> b.strike(p, ZagredMath.VERDICT_HALT, true);
        }
        ZagredFx.strike(sl, b.getEyePosition(), p.getBoundingBox().getCenter(), w.argb(), 1.4f, 14);
        p.displayClientMessage(Component.literal(w.title() + " lands.").withStyle(net.minecraft.ChatFormatting.DARK_RED), true);
    }

    // ================================================================ Overwrite: the rule and the stones
    private static void overwriteTell(ZagredBoss b, ServerLevel sl, int left, int total, boolean first) {
        if (first) {
            ZagredRule rule = ZagredRule.next(new java.util.Random(b.getRandom().nextLong()), b.lastRule);
            b.pendingRule = rule;
            b.lastRule = rule;
            b.discardStones(sl);
            b.stoneKills = 0;
            int n = ZagredMath.stones();
            double off = b.getRandom().nextDouble() * Math.PI * 2;
            for (int i = 0; i < n; i++) {
                double a = off + i * Math.PI * 2 / n;
                Vec3 at = b.position().add(Math.cos(a) * 6, 1.4, Math.sin(a) * 6);
                GrimoireDaemonEntity s = GrimoireDaemonEntity.spawn(sl, b, GrimoireDaemonEntity.STONE, at);
                if (s != null) b.stones.add(s.getUUID());
            }
            ZagredFx.corrupt(sl, b.position(), 16f, total + 20, ZagredFx.RED);
            ZagredFx.stones(sl, b.stones, rule.ordinal());
            ZagredScreen.title(sl, b.position(), ZagredScreen.text(rule.textKey(), rule.text().toUpperCase(java.util.Locale.ROOT), RED, true),
                    ZagredScreen.text("message.nusmp.zagred.overwrite.sub", "Break three of the five stones before the sentence is written", 0xE8E0F8, false), 4, total, 8);
        }
        if (left % 6 == 0) {
            ZagredFx.ring(sl, b.position(), 6f, 12, ZagredFx.RED);
            Tells.ring(sl, b.position(), 6, RED, 1.4f, 36);
        }
        if (left % 15 == 0) ZagredFx.stones(sl, b.stones, b.pendingRule == null ? 0 : b.pendingRule.ordinal());
    }

    /** The wind-up ended with the stones standing: the rule is written over the arena. */
    private static void overwriteRelease(ZagredBoss b, ServerLevel sl) {
        if (b.pendingRule == null || b.stoneKills >= ZagredMath.stonesNeeded()) return;
        b.rule = b.pendingRule;
        b.ruleUntil = b.now() + ZagredMath.ruleTicks();
        b.tracks.clear();
        ZagredScreen.title(sl, b.position(), ZagredScreen.text(b.rule.textKey(), b.rule.text().toUpperCase(java.util.Locale.ROOT), RED, true),
                ZagredScreen.text(b.rule.hintKey(), b.rule.hint(), 0xE8E0F8, false), 2, 60, 10);
        sl.playSound(null, b.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 2f, 0.5f);
        VfxSpawn.sendFollowing(sl, VfxShape.KOTO_AURA, b, b.position(), RED | 0xFF000000, 60, 2.4f);
        ZagredFx.corrupt(sl, b.position(), 18f, 80, ZagredFx.RED);
    }

    /** Every five ticks while a rule holds: who is breaking it. (The countdown is part of the boss's one status line, so two lines never fight for the action bar.) */
    static void tickRule(ZagredBoss b, ServerLevel sl, long now) {
        if (b.rule == null) return;
        if (now >= b.ruleUntil) {
            b.say("The rule fades.");
            b.endRule(sl);
            return;
        }
        if (now % 5 != 0) return;
        for (LivingEntity f : b.foes(48)) {
            if (!(f instanceof ServerPlayer p)) continue;
            ZagredBoss.Track t = b.tracks.computeIfAbsent(p.getUUID(), k -> new ZagredBoss.Track());
            Vec3 pos = p.position();
            boolean still = t.last != null && Math.hypot(pos.x - t.last.x, pos.z - t.last.z) < 0.05 && p.onGround();
            t.still = still ? t.still + 5 : 0;
            t.air = p.onGround() || p.isInWater() || p.onClimbable() ? 0 : t.air + 5;
            boolean flying = p.getAbilities().flying || p.isFallFlying() || p.hasEffect(MobEffects.LEVITATION);
            boolean bad = b.rule.breaks(flying, t.air, t.still);
            if (b.rule == ZagredRule.NO_HEALING) {
                if (t.health >= 0 && p.getHealth() > t.health + 0.25f) {                          // healing is undone as it happens
                    p.setHealth(t.health);
                    VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, p.getBoundingBox().getCenter(), b.getBoundingBox().getCenter(), ZagredFx.RED, 12, 0.6f);
                }
                t.health = p.getHealth();
            }
            t.bad = bad ? t.bad + 5 : 0;
            if (t.bad >= ZagredMath.ruleGraceTicks()) {
                t.bad = 0;
                punish(b, sl, p);
            }
            t.last = pos;
        }
    }

    /** One second of rule-breaking: a bruise and a cut of the spirit. */
    static void punish(ZagredBoss b, ServerLevel sl, LivingEntity who) {
        b.strike(who, ZagredMath.RULE_BREAK, true);
        EnergyBridge.spirit(who, 2);
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, who.getBoundingBox().getCenter(), b.getBoundingBox().getCenter(), KotodamaWords.ABYSS, 12, 0.6f);
    }

    /** The element of a hit (only the three the rules can forbid are told apart). */
    static String elementOf(net.minecraft.world.damagesource.DamageSource s) {
        String path = s.typeHolder().unwrapKey().map(k -> k.location().getPath()).orElse("");
        if (path.contains("lightning")) return "lightning";
        if (path.contains("fire") || path.contains("lava") || s.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) return "fire";
        if (path.contains("ice") || path.contains("freez") || s.is(net.minecraft.tags.DamageTypeTags.IS_FREEZING)) return "ice";
        return "";
    }
}
