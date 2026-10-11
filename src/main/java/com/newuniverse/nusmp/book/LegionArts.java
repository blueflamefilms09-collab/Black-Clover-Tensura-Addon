package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.GrimoireSummon;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Legion Magic: the spells of LegionBook that need their own logic. The caster keeps an army of magic soldiers (a short list of
 * positions with a lifetime, shown by the Legion VFX); the pages raise soldiers, send them against enemies, and Blink Castling
 * swaps the caster with one of them.
 */
public final class LegionArts {
    private LegionArts() {}

    private static final int MAX_ARMY = 12;
    private static final double TAU = Math.PI * 2;
    /** Soldier kinds: pawn, rook, knight, bishop, queen, king. */
    private static final int PAWN = 0, ROOK = 1, KNIGHT = 2, BISHOP = 3, EMPRESS = 4, KING = 5;

    /** One magic soldier: where it stands, until when, and which piece it is. */
    static final class Soldier {
        final ResourceKey<Level> dim;
        Vec3 pos;
        final long until;
        final int kind;
        final MagicPropEntity model;
        Soldier(ResourceKey<Level> dim, Vec3 pos, long until, int kind, MagicPropEntity model) {
            this.dim = dim; this.pos = pos; this.until = until; this.kind = kind; this.model = model;
        }
    }

    private static final Map<UUID, List<Soldier>> ARMY = new HashMap<>();
    private static final Map<UUID, MagicPropEntity> CHESSBOARDS = new HashMap<>();

    /** Toggle the optional chessboard aura; its page was appended to preserve every existing mode id. */
    public static void toggleChessboard(ServerPlayer player) {
        var skill = io.github.manasmods.manascore.skill.api.SkillAPI.getSkillsFrom(player)
                .getSkill(com.newuniverse.nusmp.skill.NUSkills.BOOK_LEGION.getId());
        if (skill.isEmpty()) return;
        boolean enabled = !skill.get().isToggled();
        skill.get().setToggled(enabled);
        if (enabled) skill.get().onToggleOn(player); else skill.get().onToggleOff(player);
    }

    /** Keep the modeled board behind the caster only while the toggle is enabled and the grimoire is summoned. */
    static void updateChessboard(io.github.manasmods.manascore.skill.api.ManasSkillInstance instance, ServerPlayer player) {
        if (!instance.isToggled()
                || !GrimoireSummon.isFloating(player, MagicType.LEGION)) {
            clearChessboard(player);
            return;
        }
        UUID id = player.getUUID();
        MagicPropEntity board = CHESSBOARDS.get(id);
        float yaw = player.yBodyRot * 0.017453292f;
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 at = GrimoireSummon.handPoint(player).subtract(right.scale(1.05)).add(0, 0.15, 0);
        if (board == null || board.isRemoved() || board.level() != player.level()) {
            if (board != null && !board.isRemoved()) board.discard();
            board = MagicProps.spawn(player.serverLevel(), PropKind.LEGION_2, at, player.getYRot(), 0.78f, 80, 0, player);
            if (board == null) return;
            CHESSBOARDS.put(id, board);
        } else {
            board.setPos(at);
            board.setYRot(player.getYRot());
        }
    }

    /** Drops the army of a player who left (called from LegionProps). */
    public static void forget(ServerPlayer p) {
        clearChessboard(p);
        List<Soldier> removed = ARMY.remove(p.getUUID());
        if (removed != null) for (Soldier soldier : removed) if (soldier.model != null) soldier.model.expire();
    }

    public static void clearChessboard(ServerPlayer p) {
        MagicPropEntity board = CHESSBOARDS.remove(p.getUUID());
        if (board != null && !board.isRemoved()) board.expire();
    }

    /** The live soldiers of a caster (expired ones are dropped here). */
    private static List<Soldier> army(ServerPlayer p) {
        List<Soldier> l = ARMY.computeIfAbsent(p.getUUID(), u -> new ArrayList<>());
        long now = p.level().getGameTime();
        l.removeIf(s -> now >= s.until);
        return l;
    }

    private static Soldier raise(ServerPlayer p, Vec3 at, int kind, int ticks) {
        List<Soldier> l = army(p);
        MagicPropEntity model = MagicProps.spawn(p.serverLevel(), PropKind.LEGION_1, at, p.getYRot(),
                1.12f * EnergyBridge.scale(p), ticks, kind, p);
        Soldier s = new Soldier(p.level().dimension(), at, p.level().getGameTime() + ticks, kind, model);
        l.add(s);
        while (l.size() > MAX_ARMY) {
            Soldier removed = l.remove(0);
            if (removed.model != null) removed.model.expire();
        }
        return s;
    }

    private static void move(Soldier soldier, Vec3 at) {
        soldier.pos = at;
        if (soldier.model != null && !soldier.model.isRemoved()) soldier.model.setPos(at);
    }

    // ------------------------------------------------------------------ small helpers
    private static float k(ManasSkillInstance i, ServerPlayer p) {
        return GrimoireBook.size(i, p) * EnergyBridge.scale(p);
    }

    private static Vec3 flat(Vec3 v) {
        Vec3 f = v.multiply(1, 0, 1);
        return f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
    }

    private static float cap(LivingEntity t, float raw) { return BalanceLaw.isBoss(t) ? raw * 0.5f : raw; }

    /** Slowness, shortened on bosses. */
    private static void slow(LivingEntity t, int ticks, int amp) {
        if (BalanceLaw.isBoss(t)) { ticks /= 3; amp = Math.min(amp, 1); }
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amp));
    }

    private static void weaken(LivingEntity t, int ticks) {
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, BalanceLaw.isBoss(t) ? ticks / 3 : ticks, 0));
    }

    private static void shove(LivingEntity t, Vec3 dir, double power, double lift) {
        t.knockback(power, -dir.x, -dir.z);
        if (lift > 0 && !BalanceLaw.isBoss(t)) t.setDeltaMovement(t.getDeltaMovement().add(0, lift, 0));
        t.hurtMarked = true;
    }

    private static Vec3 ring(Vec3 c, double r, double ang) { return c.add(Math.cos(ang) * r, 0, Math.sin(ang) * r); }

    /** Keep an occasional queen signature; individual soldier idle flashes obscured the board. */
    private static void presence(GrimoireBook b, ServerPlayer p, List<Soldier> group, int ticks) {
        SpellRuntime.zone(p.serverLevel(), ticks, 60, age -> {
            if (age % 60 != 0) return;
            List<Soldier> live = army(p);
            for (Soldier s : group) if (s.kind == EMPRESS && live.contains(s))
                b.vfx(p, VfxShape.LEGION_FX3, s.pos, s.pos.add(0, 1, 0), 18, 0.55f);
        });
    }

    /** Hits everything near any live soldier of the group, once each per call (at most 16 targets). */
    private static int sweep(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, List<Soldier> group, double r, float raw,
                             Consumer<LivingEntity> rider) {
        List<Soldier> live = army(p);
        Set<LivingEntity> seen = new LinkedHashSet<>();
        for (Soldier s : group) {
            if (!live.contains(s)) continue;
            seen.addAll(GrimoireBook.around(p, s.pos, r));
            if (seen.size() >= 16) break;
        }
        int n = 0;
        for (LivingEntity t : seen) {
            b.hurt(i, p, t, mode, cap(t, raw));
            if (rider != null) rider.accept(t);
            if (++n >= 16) break;
        }
        return n;
    }

    // ------------------------------------------------------------------ Pawn March
    /** Five pawns abreast march eight blocks forward; whoever they meet is hit once, shoved back and slowed. */
    static boolean pawnMarch(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float sz = k(i, p);
        Vec3 dir = flat(p.getViewVector(1f)), side = new Vec3(-dir.z, 0, dir.x);
        Vec3 base = p.position().add(dir.scale(1.5));
        b.castCircle(p, 0.8f);
        for (int n = 0; n < 5; n++) {
            Vec3 a = base.add(side.scale((n - 2) * 1.1)).add(0, 0.6, 0);
            b.vfx(p, VfxShape.LEGION_FX1, a, a.add(dir.scale(9)), 18, 0.5f);
        }
        List<Soldier> lead = new ArrayList<>();
        lead.add(raise(p, base.add(dir.scale(9)), PAWN, 200));
        presence(b, p, lead, 200);
        Set<UUID> hit = new HashSet<>();
        SpellRuntime.zone(p.serverLevel(), 20, 2, age -> {
            double d = Math.min(9, age * 0.45);
            for (int n = 0; n < 5; n++) {
                Vec3 c = base.add(side.scale((n - 2) * 1.1)).add(dir.scale(d));
                for (LivingEntity t : GrimoireBook.around(p, c, 1.4 * sz)) {
                    if (!hit.add(t.getUUID())) continue;
                    b.hurt(i, p, t, mode, cap(t, 5f));
                    slow(t, 50, 1);
                    shove(t, dir, 0.5, 0);
                    b.vfx(p, VfxShape.LEGION_FX3, t.getBoundingBox().getCenter(), t.getBoundingBox().getCenter(), 14, 0.5f);
                }
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Muster Soldiers
    /** Raises a ring of soldiers around you for 30 s; each one cuts down whoever steps within three blocks of it. */
    static boolean muster(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int count = 4 + EnergyBridge.modifier(p) / 2;
        int[] kinds = {PAWN, ROOK, KNIGHT, PAWN, BISHOP, PAWN};
        b.castCircle(p, 1f);
        List<Soldier> group = new ArrayList<>();
        Vec3 c = p.position();
        for (int n = 0; n < count; n++) {
            Soldier s = raise(p, ring(c, 3, n * TAU / count), kinds[n % kinds.length], 600);
            group.add(s);
            b.vfx(p, VfxShape.LEGION_FX2, s.pos, s.pos.add(0, 1, 0), 30, 3f);
        }
        presence(b, p, group, 600);
        SpellRuntime.zone(p.serverLevel(), 600, 10, age -> {
            if (!p.isAlive()) return;
            sweep(b, i, p, mode, group, 3.0, 2.5f, t -> slow(t, 30, 0));
        });
        return true;
    }

    // ------------------------------------------------------------------ Knight Gambit
    /** A knight moves in an L (two blocks one way, three the other) and crashes into the target. */
    static boolean knightGambit(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null) { GrimoireBook.fail(p, "No target for the knight."); return false; }
        Vec3 look = flat(p.getViewVector(1f)), side = new Vec3(-look.z, 0, look.x);
        Vec3 tc = t.position();
        Vec3 start = p.position().add(side.scale(3)).add(0, 0.6, 0);
        Vec3 corner = new Vec3(tc.x + side.x * 3, p.getY() + 0.6, tc.z + side.z * 3);
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.LEGION_FX1, start, corner, 8, 0.9f);
        b.vfx(p, VfxShape.LEGION_FX1, corner, tc.add(0, 0.8, 0), 8, 0.9f);
        List<Soldier> kn = new ArrayList<>();
        kn.add(raise(p, tc.add(side.scale(1.5)), KNIGHT, 300));
        presence(b, p, kn, 300);
        Vec3 push = tc.subtract(p.position()).multiply(1, 0, 1);
        Vec3 dir = push.lengthSqr() < 1e-4 ? look : push.normalize();
        SpellRuntime.later(p.serverLevel(), 8, () -> {
            if (!t.isAlive() || !p.isAlive()) return;
            b.hurt(i, p, t, mode, cap(t, 11f));
            shove(t, dir, 1.2, 0.4);
            weaken(t, 80);
            EnergyBridge.effect(t, "fragility", 80, 0);
            b.vfx(p, VfxShape.LEGION_FX3, t.getBoundingBox().getCenter(), t.getBoundingBox().getCenter(), 22, 1f);
        });
        return true;
    }

    // ------------------------------------------------------------------ Soldier Snare
    /** Three soldiers grab one enemy and hold it: a hard control that shares the control cooldown. */
    static boolean snare(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null) { GrimoireBook.fail(p, "No one to hold."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 80);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9));
        weaken(t, ticks + 40);
        b.hurt(i, p, t, mode, cap(t, 4f));
        b.castCircle(p, 0.8f);
        List<Soldier> grip = new ArrayList<>();
        Vec3 tc = t.position();
        for (int n = 0; n < 3; n++) {
            Soldier s = raise(p, ring(tc, 1.4, n * TAU / 3), PAWN, ticks + 40);
            grip.add(s);
            b.vfx(p, VfxShape.LEGION_FX1, s.pos.add(0, 0.8, 0), t.getBoundingBox().getCenter(), 10, 0.5f);
        }
        b.vfx(p, VfxShape.LEGION_FX2, tc, tc.add(0, 1, 0), ticks, 1.6f);
        presence(b, p, grip, ticks + 40);
        return true;
    }

    // ------------------------------------------------------------------ Commander's Banner
    /** A banner is raised: for 30 s your melee blows deal +3, and you and the allies within 8 blocks gain Speed and Resistance. */
    static boolean banner(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        var tag = i.getOrCreateTag();
        tag.putLong("LegionRallyUntil", p.level().getGameTime() + 600);
        tag.putFloat("LegionRallyBonus", 3f);
        for (ServerPlayer ally : p.serverLevel().getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(8))) {
            if (ally != p && !ally.isAlliedTo(p)) continue;
            ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0));
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 0));
        }
        b.castCircle(p, 1.2f);
        List<Soldier> bearer = new ArrayList<>();
        bearer.add(raise(p, p.position().add(flat(p.getViewVector(1f)).scale(1.5)), PAWN, 600));
        presence(b, p, bearer, 600);
        b.vfx(p, VfxShape.LEGION_FX2, p.position(), p.position().add(0, 1, 0), 60, 8f);
        b.vfx(p, VfxShape.LEGION_FX3, p.position(), p.position().add(0, 1, 0), 28, 1f);
        return true;
    }

    // ------------------------------------------------------------------ Rook Barricade
    /** Rooks pile up into a stone wall five wide and three high for 10 s; you and your allies stand firm behind it. */
    static boolean barricade(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel sl = p.serverLevel();
        Vec3 dir = flat(p.getViewVector(1f)), side = new Vec3(-dir.z, 0, dir.x);
        Vec3 base = p.position().add(dir.scale(2.5));
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
        int placed = 0;
        for (int w = -2; w <= 2; w++) for (int h = 0; h < 3; h++) {
            BlockPos pos = BlockPos.containing(base.add(side.scale(w)).add(0, h, 0));
            if (!sl.isLoaded(pos)) continue;
            if (SpellRuntime.tempBlock(sl, pos, stone, 200)) placed++;
        }
        if (placed == 0) { GrimoireBook.fail(p, "No room for a barricade."); return false; }
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
        for (ServerPlayer ally : sl.getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(6))) {
            if (ally != p && ally.isAlliedTo(p)) ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0));
        }
        b.castCircle(p, 1f);
        List<Soldier> rooks = new ArrayList<>();
        for (int e = -1; e <= 1; e += 2) {
            Soldier s = raise(p, base.add(side.scale(3 * e)), ROOK, 300);
            rooks.add(s);
            b.vfx(p, VfxShape.LEGION_FX3, s.pos, s.pos.add(0, 1, 0), 24, 0.6f);
        }
        presence(b, p, rooks, 300);
        b.vfx(p, VfxShape.LEGION_FX2, base, base.add(0, 1, 0), 40, 3f);
        return true;
    }

    // ------------------------------------------------------------------ Blink Castling
    /** Swaps places with the soldier nearest your crosshair; the soldier takes your old spot and the hunters lose you. */
    static boolean blinkCastling(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        List<Soldier> mine = new ArrayList<>(army(p));
        mine.removeIf(s -> s.dim != p.level().dimension());
        if (mine.isEmpty()) { GrimoireBook.fail(p, "You have no soldier to swap with."); return false; }
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        Soldier best = null;
        double bestScore = -9;
        for (Soldier s : mine) {
            Vec3 d = s.pos.add(0, 1, 0).subtract(eye);
            double len = d.length();
            if (len < 1.5 || len > 48) continue;
            double score = d.scale(1 / len).dot(look) - len / 200.0;
            if (score > bestScore) { bestScore = score; best = s; }
        }
        if (best == null) { GrimoireBook.fail(p, "Your soldiers are too near or too far."); return false; }
        Vec3 from = p.position(), dest = best.pos;
        if (!p.level().noCollision(p, p.getBoundingBox().move(dest.subtract(from)))) {
            dest = dest.add(0, 1, 0);
            if (!p.level().noCollision(p, p.getBoundingBox().move(dest.subtract(from)))) { GrimoireBook.fail(p, "No room to stand there."); return false; }
        }
        move(best, from);
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.LEGION_FX1, from.add(0, 1, 0), dest.add(0, 1, 0), 12, 1f);
        b.vfx(p, VfxShape.LEGION_FX3, from, from.add(0, 1, 0), 20, 0.6f);
        p.teleportTo(dest.x, dest.y, dest.z);
        p.fallDistance = 0;
        b.vfx(p, VfxShape.LEGION_FX3, dest, dest.add(0, 1, 0), 20, 0.6f);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 2));
        for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16))) if (m.getTarget() == p) m.setTarget(null);
        return true;
    }

    // ------------------------------------------------------------------ Gehenna Game
    /** A round chessboard opens at the aim point (7 blocks) for 8 s; eight pieces circle it, striking and holding everyone on it. */
    static boolean gehenna(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float sz = k(i, p);
        Vec3 c = GrimoireBook.aim(p, 24);
        double r = 7 * sz;
        int ticks = 160;
        int[] kinds = {ROOK, KNIGHT, BISHOP, EMPRESS, KING, BISHOP, KNIGHT, ROOK};
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.LEGION_FX2, c.add(0, 0.05, 0), c.add(0, 1, 0), ticks, (float) r);
        MagicProps.spawn(p.serverLevel(), PropKind.LEGION_2, c, 0f, (float) r, ticks, 0, p);
        List<Soldier> pieces = new ArrayList<>();
        for (int n = 0; n < 8; n++) pieces.add(raise(p, ring(c, r * 0.75, n * Math.PI / 4), kinds[n], ticks + 20));
        presence(b, p, pieces, ticks);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            if (!p.isAlive()) return;
            double ang = age * 0.06;
            for (int n = 0; n < 8; n++) move(pieces.get(n), ring(c, r * 0.75, n * Math.PI / 4 + ang));
            sweep(b, i, p, mode, pieces, 2.2, 3f, t -> slow(t, 30, 1));
            for (LivingEntity t : GrimoireBook.around(p, c, r)) slow(t, 25, 0);
            if (age % 40 == 0) b.vfx(p, VfxShape.LEGION_FX3, c, c.add(0, 1, 0), 22, (float) (r / 6));
        });
        return true;
    }

    // ------------------------------------------------------------------ Endless Domination
    /** Eight soldiers orbit you for 15 s, cutting down everything within three blocks of them; every 3 s they lunge at the four nearest foes. */
    static boolean domination(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float sz = k(i, p);
        double r = 4 * sz;
        int ticks = 300;
        int[] kinds = {PAWN, ROOK, KNIGHT, BISHOP, EMPRESS, KING, BISHOP, ROOK};
        List<Soldier> host = new ArrayList<>();
        for (int n = 0; n < 8; n++) host.add(raise(p, ring(p.position(), r, n * Math.PI / 4), kinds[n], ticks + 20));
        b.castCircle(p, 1.4f);
        b.vfx(p, VfxShape.LEGION_FX2, p.position(), p.position().add(0, 1, 0), 40, (float) (r + 3));
        b.vfx(p, VfxShape.LEGION_FX3, p.position(), p.position().add(0, 1, 0), 30, 1.3f);
        presence(b, p, host, ticks);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            if (!p.isAlive()) return;
            Vec3 c = p.position();
            double ang = age * 0.1;
            for (int n = 0; n < 8; n++) move(host.get(n), ring(c, r, n * Math.PI / 4 + ang));
            sweep(b, i, p, mode, host, 3.0, 4.5f, t -> slow(t, 30, 1));
            if (age % 60 != 0) return;
            List<LivingEntity> foes = new ArrayList<>(GrimoireBook.around(p, c, 20));
            foes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(c)));
            for (int n = 0; n < Math.min(4, foes.size()); n++) {
                LivingEntity t = foes.get(n);
                b.vfx(p, VfxShape.LEGION_FX1, host.get(n * 2).pos.add(0, 1, 0), t.getBoundingBox().getCenter(), 12, 0.7f);
                b.hurt(i, p, t, mode, cap(t, 7f));
                EnergyBridge.effect(t, "fragility", 60, 0);
                weaken(t, 60);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ End Empress
    /** Every soldier you have merges into one giant: after 1.5 s it slams down (radius 8, 16 + 3 per merged soldier), then stomps for 8 s. */
    static boolean endEmpress(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float sz = k(i, p);
        Vec3 c = GrimoireBook.aim(p, 28);
        List<Soldier> live = new ArrayList<>(army(p));
        int merged = Math.min(8, live.size());
        for (int n = 0; n < merged; n++) b.vfx(p, VfxShape.LEGION_FX1, live.get(n).pos.add(0, 1, 0), c.add(0, 2, 0), 30, 0.7f);
        army(p).removeAll(live);
        for (Soldier soldier : live) if (soldier.model != null) soldier.model.expire();
        b.castCircle(p, 1.6f);
        b.vfx(p, VfxShape.LEGION_FX2, c.add(0, 0.05, 0), c.add(0, 1, 0), 30, 4f);
        ServerLevel sl = p.serverLevel();
        SpellRuntime.later(sl, 30, () -> {
            if (!p.isAlive()) return;
            float raw = 16f + 3f * merged;
            double r = 8 * sz;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                b.hurt(i, p, t, mode, cap(t, raw));
                shove(t, t.position().subtract(c).multiply(1, 0, 1).add(0.001, 0, 0).normalize(), 0.3, 0.8);
                slow(t, 100, 2);
                EnergyBridge.effect(t, "fragility", 100, 0);
            }
            b.vfx(p, VfxShape.LEGION_FX3, c, c.add(0, 1, 0), 40, 2.2f);
            b.impact(p, c, 1.4f);
            List<Soldier> giant = new ArrayList<>();
            giant.add(raise(p, c, EMPRESS, 190));
            presence(b, p, giant, 190);
            SpellRuntime.zone(sl, 160, 20, age -> {
                if (!p.isAlive()) return;
                double sr = 6 * sz;
                for (LivingEntity t : GrimoireBook.around(p, c, sr)) {
                    b.hurt(i, p, t, mode, cap(t, 6f));
                    slow(t, 30, 1);
                }
                b.vfx(p, VfxShape.LEGION_FX2, c, c.add(0, 1, 0), 20, (float) sr);
            });
        });
        return true;
    }
}
