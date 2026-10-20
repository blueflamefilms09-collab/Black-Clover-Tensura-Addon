package com.newuniverse.nusmp.relics.wave;


import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicEngine;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.relics.wave.Wave14Rules.HighGround;
import com.newuniverse.nusmp.relics.wave.Wave14Rules.Mark;
import com.newuniverse.nusmp.relics.wave.Wave14Rules.Pool;
import com.newuniverse.nusmp.relics.wave.Wave14Rules.Spell;
import com.newuniverse.nusmp.relics.wave.Wave14Rules.ZoneKind;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntUnaryOperator;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Wave 14, the footwear: the Ruby Boots of Planestriding (three charges, a place to click home to), the Boots of the Indomitable (cursed; nothing
 * moves you), Mudwalkers, the Boots of the High Ground, the Greaves of the Wind Walker (eight wind spells on five charges), Waterwalkers, the
 * Sandals of Sneaking and the Dragon Fire Boots (a short flight and a fireball). Numbers live in {@link Wave14Rules}.
 */
final class Wave14Feet {
    private Wave14Feet() {}

    private static final String K_MARKS = "marks", K_PLACE = "place", K_INDOM = "indom_day", K_HIGH = "high_until", K_HIGH_Y = "high_y", K_HIGH_STRIKE = "nusmp_w14_hg",
            K_FLY = "fly_until", K_FLY_GRANTED = "fly_granted", K_WARD = "ward_until";
    private static final String P_RUBY = "ruby", P_WIND = "wind";
    private static final String HOME_NOTE = "home";

    private static IntUnaryOperator rng(ServerPlayer p) { return bound -> p.getRandom().nextInt(bound); }

    // ================================================================ Ruby Boots of Planestriding
    /** Where the boots can take you: place 0 is home (your bed or the world's spawn), then the places you learned. */
    private static List<Mark> marks(CompoundTag d) {
        List<Mark> out = new ArrayList<>();
        ListTag l = d.getList(K_MARKS, Tag.TAG_COMPOUND);
        for (int i = 0; i < l.size(); i++) {
            CompoundTag t = l.getCompound(i);
            out.add(new Mark(t.getString("dim"), t.getInt("x"), t.getInt("y"), t.getInt("z")));
        }
        return out;
    }

    private static void storeMarks(CompoundTag d, List<Mark> marks) {
        ListTag l = new ListTag();
        for (Mark m : marks) {
            CompoundTag t = new CompoundTag();
            t.putString("dim", m.dim());
            t.putInt("x", m.x());
            t.putInt("y", m.y());
            t.putInt("z", m.z());
            l.add(t);
        }
        d.put(K_MARKS, l);
    }

    private static Mark home(ServerPlayer p) {
        BlockPos bed = p.getRespawnPosition();
        if (bed != null) return new Mark(p.getRespawnDimension().location().toString(), bed.getX(), bed.getY() + 1, bed.getZ());
        ServerLevel over = p.getServer().overworld();
        BlockPos s = over.getSharedSpawnPos();
        int y = over.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, s.getX(), s.getZ());
        return new Mark(Level.OVERWORLD.location().toString(), s.getX(), y, s.getZ());
    }

    private static Mark place(ServerPlayer p, CompoundTag d, int index) {
        List<Mark> m = marks(d);
        return index <= 0 || index > m.size() ? home(p) : m.get(index - 1);
    }

    private static ServerLevel levelOf(ServerPlayer p, String dim) {
        var rl = Wave14Util.rl(dim);
        return rl == null ? null : p.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, rl));
    }

    /** Planestriding: spend a charge, click your heels three times (three seconds), and step to the next place you know: home first, then the places you learned. */
    static void planestride(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave14Util.data(p);
        long today = Wave14Util.today(p.serverLevel());
        Pool pool = Wave14Util.pool(d, P_RUBY, Wave14Rules.RUBY_CHARGES, today, () -> Wave14Rules.regainD3(rng(p)));
        int next = Wave14Rules.nextPlace(d.getInt(K_PLACE), marks(d).size());
        Mark to = place(p, d, next);
        ServerLevel dest = levelOf(p, to.dim());
        if (!pool.canSpend(1)) {
            Wave14Util.store(d, P_RUBY, pool);
            Wave14Util.tell(p, "The Ruby Boots have no charges left (1d3 return at dawn).", ChatFormatting.GRAY);
            return;
        }
        if (dest == null || Labyrinth.in(dest) || Labyrinth.in(p.level())) {
            Wave14Util.store(d, P_RUBY, pool);
            Wave14Util.tell(p, "The plane resists: you cannot stride to or from the Dungeon.", ChatFormatting.RED);
            return;
        }
        Wave14Util.store(d, P_RUBY, pool.spend(1));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.8f);
        Wave14Util.say(p, "\"There's no place like " + (next == 0 ? HOME_NOTE : "place " + next) + "...\" Click your heels: the boots take three seconds.");
        FxRelic.ring(p.serverLevel(), p.position(), 1.4, 0xE0245E);
        Wave14Zones.later(p, 20, Wave14Zones.KIND_CLICK, 0);
        Wave14Zones.later(p, 40, Wave14Zones.KIND_CLICK, 0);
        Wave14Zones.later(p, Wave14Rules.PLANE_RITUAL, Wave14Zones.KIND_PLANE, next);
    }

    /** One of the three heel clicks. */
    static void heelClick(ServerPlayer p) {
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0f, 1.0f + p.getRandom().nextFloat() * 0.4f);
        com.newuniverse.nusmp.particle.fx.Fx.spray(p.serverLevel(), FxThemes.RELIC_WAVE_14, Wave14Util.align(0xE0245E), p.getX(), p.getY() + 0.1, p.getZ(), 10, 0.3, 0.05, 0.3, 0.0);
    }

    /** The third click: step to place {@code index}. If there is nowhere safe to stand the charge is given back. */
    static void finishPlane(ServerPlayer p, int index) {
        if (!RelicFlags.has(p, Wave14Rules.F_RUBY)) return;
        CompoundTag d = Wave14Util.data(p);
        Mark to = place(p, d, index);
        ServerLevel dest = levelOf(p, to.dim());
        Vec3 spot = dest == null || Labyrinth.in(dest) || Labyrinth.in(p.level()) ? null : standingSpot(dest, p, to.x() + 0.5, to.y(), to.z() + 0.5);
        if (spot == null) {
            long today = Wave14Util.today(p.serverLevel());
            Pool pool = Wave14Util.pool(d, P_RUBY, Wave14Rules.RUBY_CHARGES, today, () -> 0);
            Wave14Util.store(d, P_RUBY, new Pool(Math.min(Wave14Rules.RUBY_CHARGES, pool.have() + 1), pool.day()));
            Wave14Util.tell(p, "There is nowhere to stand there: the charge is returned.", ChatFormatting.RED);
            return;
        }
        FxRelic.burst(p.serverLevel(), p.position(), 0xE0245E);
        p.teleportTo(dest, spot.x, spot.y, spot.z, p.getYRot(), p.getXRot());
        p.fallDistance = 0;
        d.putInt(K_PLACE, index);
        FxRelic.burst(dest, spot, 0xE0245E);
        dest.playSound(null, BlockPos.containing(spot), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.2f);
        Wave14Util.tell(p, index == 0 ? "Home." : "You stride to place " + index + ".", ChatFormatting.RED);
    }

    /** A spot with room to stand at or just above the given one (up to four blocks), or null. */
    private static Vec3 standingSpot(ServerLevel level, ServerPlayer p, double x, double y, double z) {
        level.getChunkAt(BlockPos.containing(x, y, z));                                // a place far from anyone is in an unloaded chunk, which the collision test would read as empty air
        for (int dy = 0; dy <= 4; dy++) {
            AABB box = p.getBoundingBox().move(x - p.getX(), y + dy - p.getY(), z - p.getZ());
            if (level.noCollision(box) && !level.containsAnyLiquid(box)) return new Vec3(x, y + dy, z);
        }
        return null;
    }

    /** Learn this Place: the boots remember where you stand (four places at most; the oldest is forgotten). */
    static void learnPlace(ActCtx c) {
        ServerPlayer p = c.player();
        if (Labyrinth.in(p.level())) {
            Wave14Util.tell(p, "The Dungeon will not be remembered.", ChatFormatting.RED);
            return;
        }
        CompoundTag d = Wave14Util.data(p);
        BlockPos at = p.blockPosition();
        List<Mark> next = Wave14Rules.addMark(marks(d), new Mark(p.level().dimension().location().toString(), at.getX(), at.getY(), at.getZ()));
        storeMarks(d, next);
        FxRelic.ring(p.serverLevel(), p.position(), 1.2, 0xE0245E);
        Wave14Util.tell(p, "You learn this place (" + next.size() + " of " + Wave14Rules.MAX_MARKS + ").", ChatFormatting.RED);
    }

    // ================================================================ Boots of the Indomitable
    /** Every two or four ticks for a wearer in water: the boots drag them down. */
    static void sink(ServerPlayer p) {
        if (!p.isInWater() || p.isCreative() || p.isSpectator() || p.getAbilities().flying) return;
        Vec3 dm = p.getDeltaMovement();
        if (dm.y > -0.1) {
            p.setDeltaMovement(dm.x * 0.95, dm.y * 0.5 - 0.04, dm.z * 0.95);
            p.hurtMarked = true;
        }
    }

    /** A fall of six blocks or more, once a day, does the wearer no harm; the damage lands on a creature below instead (half of them step aside). */
    static void indomitableFall(ServerPlayer p, LivingFallEvent e) {
        CompoundTag d = Wave14Util.data(p);
        long today = Wave14Util.today(p.serverLevel());
        if (!Wave14Rules.indomitableFall(e.getDistance(), Wave14Rules.dailyReady(d.getLong(K_INDOM), today))) return;
        d.putLong(K_INDOM, Wave14Rules.dailyMark(today));
        float dmg = Wave14Rules.fallDamage(e.getDistance());
        e.setDamageMultiplier(0f);
        List<LivingEntity> under = Wave14Util.near(p.serverLevel(), p.position(), 3.0, x -> x != p && Wave14Util.target(p, x));
        com.newuniverse.nusmp.particle.fx.Fx.spray(p.serverLevel(), FxThemes.RELIC_WAVE_14, Wave14Util.align(0x8A8E96), p.getX(), p.getY() + 0.1, p.getZ(), 24, 0.8, 0.1, 0.8, 0.02);
        if (under.isEmpty()) {
            Wave14Util.tell(p, "The boots take the fall: you are unhurt.", ChatFormatting.GRAY);
            return;
        }
        LivingEntity t = under.get(0);
        boolean saved = Wave14Rules.fallSaved(rng(p));
        if (!saved) {
            Fx.damage(p, t, dmg, "minecraft:fall");
            Wave14Util.give(t, 40, 4, "minecraft:slowness");
        }
        Wave14Util.tell(p, "The boots take the fall: " + (saved ? t.getName().getString() + " leaps clear." : t.getName().getString() + " takes " + (int) dmg + " and is knocked flat."), ChatFormatting.GRAY);
    }

    // ================================================================ Mudwalkers
    /** Mud Slide: slide about six blocks the way you look; the ground you cross is coated in slippery mud for a minute (slows foes, trips them now and then). */
    static void mudSlide(ActCtx c) {
        ServerPlayer p = c.player();
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 dir = flat.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 from = p.position();
        Fx.dash(p, 6.0);
        for (int i = 0; i <= 4; i++) {
            Vec3 at = from.add(dir.scale(1.5 * i));
            Wave14Zones.addZone(p, ZoneKind.GREASE, at, 0, 0, 1.8, Wave14Rules.GREASE_TICKS);
        }
        com.newuniverse.nusmp.particle.fx.Fx.spray(p.serverLevel(), FxThemes.RELIC_WAVE_14, Wave14Util.align(0x7A5232), from.x, from.y + 0.2, from.z, 16, 0.5, 0.1, 0.5, 0.02);
    }

    // ================================================================ Boots of the High Ground
    static boolean highActive(ServerPlayer p) {
        CompoundTag d = Wave14Util.peek(p);
        return d != null && d.getLong(K_HIGH) > p.level().getGameTime();
    }

    /** "It's over, I have the high ground!": for a minute you stand taller (a hop, a longer jump), strike harder, and answer anyone who comes close. */
    static void highGround(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave14Util.data(p);
        d.putLong(K_HIGH, p.level().getGameTime() + Wave14Rules.HIGH_TICKS);
        d.putDouble(K_HIGH_Y, p.getY());
        Vec3 dm = p.getDeltaMovement();
        p.setDeltaMovement(dm.x, 0.55, dm.z);
        p.hurtMarked = true;
        p.fallDistance = 0;
        com.newuniverse.nusmp.particle.fx.Fx.spray(p.serverLevel(), FxThemes.RELIC_WAVE_14, Wave14Util.align(0x8A6A44), p.getX(), p.getY() + 0.1, p.getZ(), 30, 0.8, 0.1, 0.8, 0.03);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7f, 0.6f);
        Wave14Util.say(p, "\"It's over, I have the high ground!\" For one minute your melee blows hit 30% harder and anyone who comes within three blocks is struck. Step down and it ends.");
    }

    /** Four times a second at most for a wearer while it lasts: ends it when the minute is up or they step off; strikes anyone who comes close. */
    static void highPass(ServerPlayer p, boolean react) {
        CompoundTag d = Wave14Util.peek(p);
        if (d == null) return;
        long until = d.getLong(K_HIGH);
        if (until == 0L) return;
        long now = p.level().getGameTime();
        if (new HighGround(until, d.getDouble(K_HIGH_Y)).over(now, p.getY()) || !RelicFlags.has(p, Wave14Rules.F_HIGH)) {
            d.remove(K_HIGH);
            Wave14Util.tell(p, "You step off the high ground.", ChatFormatting.GRAY);
            return;
        }
        if (!react) return;
        int struck = 0;
        for (LivingEntity f : Fx.foesAround(p, Wave14Rules.HIGH_REACH)) {
            if (struck >= 3) break;
            if (!Wave14Rules.reactionReady(f.getPersistentData().getLong(K_HIGH_STRIKE), now)) continue;
            f.getPersistentData().putLong(K_HIGH_STRIKE, now);
            Fx.damage(p, f, (float) (p.getAttributeValue(Attributes.ATTACK_DAMAGE) * Wave14Rules.HIGH_REACTION_FACTOR), "minecraft:player_attack");
            p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            com.newuniverse.nusmp.particle.fx.Fx.spray(p.serverLevel(), FxThemes.RELIC_WAVE_14, FxTone.LIGHT, f.getX(), f.getY() + f.getBbHeight() * 0.5, f.getZ(), 1, 0, 0, 0, 0);
            struck++;
        }
    }

    // ================================================================ Greaves of the Wind Walker
    private static boolean spend(ServerPlayer p, Spell s) {
        if (s.cost <= 0) return true;
        CompoundTag d = Wave14Util.data(p);
        long today = Wave14Util.today(p.serverLevel());
        Pool pool = Wave14Util.pool(d, P_WIND, Wave14Rules.WIND_CHARGES, today, () -> Wave14Rules.regainWind(rng(p)));
        if (!pool.canSpend(s.cost)) {
            Wave14Util.store(d, P_WIND, pool);
            Wave14Util.tell(p, "The greave needs " + s.cost + " charges; it has " + pool.have() + " (1d4+3 return at dawn).", ChatFormatting.GRAY);
            return false;
        }
        Wave14Util.store(d, P_WIND, pool.spend(s.cost));
        Wave14Util.tell(p, (s.cost > 0 ? (pool.have() - s.cost) + " charges left." : ""), ChatFormatting.AQUA);
        return true;
    }

    private static void breeze(ServerPlayer p, Vec3 at) {
        com.newuniverse.nusmp.particle.fx.Fx.spray(p.serverLevel(), FxThemes.RELIC_WAVE_14, FxTone.LIGHT, at.x, at.y, at.z, 14, 0.5, 0.3, 0.5, 0.05);
    }

    /** Gust (a cantrip, no charge): a puff of wind in front of you shoves creatures within six blocks back. */
    static void gust(ActCtx c) {
        push(c.player(), 6.0, 0.9, 0.25);
        c.player().serverLevel().playSound(null, c.player().blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0f, 1.6f);
    }

    /** Gust of Wind (2 charges): a long line of strong wind, 14 blocks; creatures in it are thrown back. */
    static void gustOfWind(ActCtx c) {
        if (!spend(c.player(), Spell.GUST_OF_WIND)) return;
        push(c.player(), 14.0, 1.6, 0.35);
        c.player().serverLevel().playSound(null, c.player().blockPosition(), SoundEvents.BREEZE_IDLE_AIR, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    private static void push(ServerPlayer p, double range, double force, double lift) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 dir = flat.lengthSqr() < 1.0e-4 ? look : flat.normalize();
        for (LivingEntity e : Wave14Util.near(p.serverLevel(), p.position(), range, x -> x != p && !Wave14Util.friend(p, x))) {
            Vec3 d = e.getBoundingBox().getCenter().subtract(eye);
            if (!Wave14Rules.inCone(d.x, d.y, d.z, look.x, look.y, look.z, 0.8, range)) continue;
            if (e instanceof net.minecraft.world.entity.player.Player o && (o.isCreative() || o.isSpectator())) continue;
            e.push(dir.x * force, lift, dir.z * force);
            e.hurtMarked = true;
        }
        for (int i = 1; i <= Math.min(14, (int) range); i += 2) breeze(p, eye.add(look.scale(i)));
    }

    /** Feather Fall (1 charge): you and your friends within eight blocks float down gently for half a minute. */
    static void featherFallSpell(ActCtx c) {
        if (!spend(c.player(), Spell.FEATHER_FALL)) return;
        for (LivingEntity e : Wave14Util.friendsAround(c.player(), 8.0)) Wave14Util.give(e, Wave14Rules.FEATHER_TICKS, 0, "minecraft:slow_falling");
        breeze(c.player(), c.player().position().add(0, 1, 0));
    }

    /** Dust Devil (2 charges): a whirling pillar of dust drifts away in front of you for eight seconds, scouring and blinding what it touches. */
    static void dustDevil(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spend(p, Spell.DUST_DEVIL)) return;
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 dir = flat.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Wave14Zones.addZone(p, ZoneKind.DEVIL, p.position().add(dir.scale(2.5)), dir.x * 2.5, dir.z * 2.5, 2.5, Wave14Rules.DEVIL_TICKS);
        breeze(p, p.position().add(dir.scale(2.5)));
    }

    /** Warding Wind (2 charges): for ten minutes a wind about you turns away arrows and bolts (their damage falls by 60%) and Tensura's wind protection covers you. */
    static void wardingWind(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spend(p, Spell.WARDING_WIND)) return;
        Wave14Util.data(p).putLong(K_WARD, p.level().getGameTime() + Wave14Rules.WARD_TICKS);
        Wave14Util.giveQuiet(p, Wave14Rules.WARD_TICKS, 0, "tensura:wind_protection");
        breeze(p, p.position().add(0, 1, 0));
        Wave14Util.say(p, "Warding Wind: for ten minutes arrows and bolts lose 60% of their bite.");
    }

    /** Is the wind about the wearer turning missiles? */
    static boolean warded(ServerPlayer p) {
        CompoundTag d = Wave14Util.peek(p);
        return d != null && d.getLong(K_WARD) > p.level().getGameTime();
    }

    /** Wind Wall (3 charges): a wall of wind, ten blocks wide, stands four blocks in front of you for ten seconds; arrows and bolts that reach it are blown apart. */
    static void windWall(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spend(p, Spell.WIND_WALL)) return;
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 dir = flat.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 at = p.position().add(dir.scale(4.0));
        Wave14Zones.addZone(p, ZoneKind.WALL, at, dir.x, dir.z, 5.0, Wave14Rules.WALL_TICKS);
        breeze(p, at.add(0, 1, 0));
    }

    /** Control Winds (3 charges): the wind lifts you for a minute (Tensura's air flight, or a slow fall and a high jump if it is not there). */
    static void controlWinds(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spend(p, Spell.CONTROL_WINDS)) return;
        int lent = Fx.lendSkills(p, Wave14Rules.CONTROL_TICKS, "tensura:air_flight");
        if (lent == 0) {
            Wave14Util.give(p, Wave14Rules.CONTROL_TICKS, 0, "minecraft:slow_falling");
            Wave14Util.give(p, Wave14Rules.CONTROL_TICKS, 3, "minecraft:jump_boost");
        }
        breeze(p, p.position().add(0, 1, 0));
        Wave14Util.say(p, "Control Winds: the wind carries you for a minute.");
    }

    /** Wind Walk (5 charges, a little existence): you and your friends within six blocks become wind for two minutes: flight, great speed, half seen. */
    static void windWalk(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spend(p, Spell.WIND_WALK)) return;
        int ticks = Wave14Rules.WIND_WALK_TICKS;
        for (LivingEntity e : Wave14Util.friendsAround(p, 6.0)) {
            Wave14Util.give(e, ticks, 2, "minecraft:speed");
            Wave14Util.give(e, ticks / 4, 0, "minecraft:invisibility");
            Wave14Util.giveQuiet(e, ticks, 0, "tensura:wind_protection");
            if (e instanceof ServerPlayer sp && Fx.lendSkills(sp, ticks, "tensura:air_flight", "tensura:flight") == 0) Wave14Util.give(sp, ticks, 0, "minecraft:slow_falling");
            breeze(p, e.position().add(0, 1, 0));
        }
        Wave14Util.say(p, "Wind Walk: you become wind for two minutes.");
    }

    // ================================================================ Waterwalkers
    /** Every other tick while Liquid to Solid is on: the water under and around the wearer's feet freezes to a thin ice that melts again behind them (like frost walking). */
    static void walkOnWater(ServerPlayer p) {
        if (p.isShiftKeyDown() || p.isSpectator() || !RelicEngine.isOn(p, "waterwalkers", "liquid_to_solid")) return;
        ServerLevel sl = p.serverLevel();
        BlockPos base = p.blockPosition().below();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx * dx + dz * dz > 5) continue;
                BlockPos pos = base.offset(dx, 0, dz);
                BlockState st = sl.getBlockState(pos);
                if (!st.is(Blocks.WATER) || !st.getFluidState().isSource() || !sl.isEmptyBlock(pos.above()) || !sl.mayInteract(p, pos)) continue;
                sl.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
                sl.scheduleTick(pos, Blocks.FROSTED_ICE, Mth.nextInt(p.getRandom(), 60, 120));
            }
        }
    }

    // ================================================================ Sandals of Sneaking
    /** The players who wear the sandals (kept by the once-a-second pass; the sound and vibration handlers ask it, so they cost nothing for everyone else). */
    static final Set<UUID> SNEAKERS = ConcurrentHashMap.newKeySet();

    /** Once a second for everyone: keeps the list of wearers; creatures far off that were following a sneaking wearer lose the trail. */
    static void sneakPass(ServerPlayer p) {
        boolean worn = RelicFlags.has(p, Wave14Rules.F_SNEAK);
        if (worn) SNEAKERS.add(p.getUUID());
        else SNEAKERS.remove(p.getUUID());
        if (worn && p.isShiftKeyDown()) {                                              // tracks erased
            for (LivingEntity e : Wave14Util.near(p.serverLevel(), p.position(), 24.0, x -> x instanceof Mob m && m.getTarget() == p && x.distanceToSqr(p) > 64.0)) ((Mob) e).setTarget(null);
        }
    }

    static void sneakForget(ServerPlayer p) { SNEAKERS.remove(p.getUUID()); }

    // ================================================================ Dragon Fire Boots
    /** The command word: jets of fire lift you for half a minute (flight). The jets cut out at the end with a gentle fall; casting the fireball in the air may cost you control. */
    static void jets(ActCtx c) {
        ServerPlayer p = c.player();
        if (p.isCreative() || p.isSpectator()) return;
        CompoundTag d = Wave14Util.data(p);
        d.putLong(K_FLY, p.level().getGameTime() + Wave14Rules.FLIGHT_TICKS);
        flightOn(p, d);
        Wave14Util.say(p, "Jets of fire lift you: half a minute of flight.");
    }

    /** Flight is lent only to a player who has none of their own: the jets never touch the flight of creative or spectator mode, or another source's. */
    private static void flightOn(ServerPlayer p, CompoundTag d) {
        Abilities ab = p.getAbilities();
        if (ab.mayfly) return;
        ab.mayfly = true;
        d.putBoolean(K_FLY_GRANTED, true);
        ab.setFlyingSpeed(0.08f);
        ab.flying = true;
        p.onUpdateAbilities();
        p.fallDistance = 0;
    }

    /**
     * Takes the lent flight back. A player who is meanwhile in creative or spectator mode keeps the flight of that mode (only the mark is dropped), so a
     * game-mode change, a logout or a death can never leave anyone flying without the jets or stuck unable to fly.
     */
    private static void flightOff(ServerPlayer p, CompoundTag d, boolean gently) {
        if (d.getBoolean(K_FLY_GRANTED)) {
            d.remove(K_FLY_GRANTED);
            if (!p.isCreative() && !p.isSpectator()) {
                Abilities ab = p.getAbilities();
                ab.mayfly = false;
                ab.flying = false;
                ab.setFlyingSpeed(0.05f);
                p.onUpdateAbilities();
                if (gently) Wave14Util.give(p, 100, 0, "minecraft:slow_falling");
            }
        }
        d.remove(K_FLY);
    }

    /** Is the wearer flying on the jets? */
    static boolean flying(ServerPlayer p) {
        CompoundTag d = Wave14Util.peek(p);
        return d != null && d.getLong(K_FLY) > p.level().getGameTime();
    }

    /** Every five ticks for a wearer while they fly, and once a second for everyone: flames at the feet; the end of the flight, or its cleanup if the boots came off. */
    static void flightPass(ServerPlayer p) {
        CompoundTag d = Wave14Util.peek(p);
        if (d == null) return;
        long until = d.getLong(K_FLY);
        boolean granted = d.getBoolean(K_FLY_GRANTED);
        if (until == 0L && !granted) return;
        long now = p.level().getGameTime();
        if (now >= until || !RelicFlags.has(p, Wave14Rules.F_FIRE) || p.isSpectator()) {
            flightOff(p, d, true);
            Wave14Util.tell(p, "The jets cut out.", ChatFormatting.GRAY);
            return;
        }
        if (until - now <= Wave14Rules.FLIGHT_LAST_WARN && (until - now) % 20 < 5) Wave14Util.tell(p, "The jets sputter: " + (until - now) / 20 + " s.", ChatFormatting.GOLD);
        com.newuniverse.nusmp.particle.fx.Fx.spray(p.serverLevel(), FxThemes.RELIC_WAVE_14, FxTone.LIGHT, p.getX(), p.getY() + 0.1, p.getZ(), 4, 0.15, 0.05, 0.15, 0.02);
    }

    /** Called when the player leaves the world or dies: the flight is closed. */
    static void flightForget(ServerPlayer p) {
        CompoundTag d = Wave14Util.peek(p);
        if (d != null) flightOff(p, d, false);
    }

    /** Fireball (once a day): a burst of fire where you look (up to 40 blocks); while you fly there is a four in ten chance the kick sends you crashing. */
    static void fireball(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave14Util.data(p);
        ServerLevel sl = p.serverLevel();
        Wave14Util.Aim a = Wave14Util.aim(p, Wave14Rules.FIREBALL_RANGE);
        Vec3 at = a.hit() != null ? a.hit().getBoundingBox().getCenter() : a.end();
        Wave14Util.trail(sl, p.position().add(0, 0.1, 0), at, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_14, FxTone.LIGHT));
        for (LivingEntity e : Wave14Util.near(sl, at, Wave14Rules.FIREBALL_RADIUS, x -> Wave14Util.target(p, x))) {
            Fx.damage(p, e, Wave14Rules.FIREBALL_DAMAGE, "minecraft:fireball");
            if (!e.fireImmune()) e.setRemainingFireTicks(100);
        }
        com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_14, FxTone.LIGHT, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_14, FxTone.LIGHT, at.x, at.y + 0.3, at.z, 50, 1.6, 0.8, 1.6, 0.06);
        sl.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 1.1f);
        if (flying(p) && Wave14Rules.crashes(rng(p))) {
            flightOff(p, d, true);                                                      // the crash hurts (never kills); the long fall after it does not
            p.setDeltaMovement(p.getDeltaMovement().x * 0.3, -1.0, p.getDeltaMovement().z * 0.3);
            p.hurtMarked = true;
            float hurt = Wave14Rules.crashDamage(p.getHealth());
            if (hurt > 0f) p.hurt(p.damageSources().fall(), hurt);
            Wave14Util.say(p, "The kick throws you off course: you crash into the ground!");
        }
    }
}
