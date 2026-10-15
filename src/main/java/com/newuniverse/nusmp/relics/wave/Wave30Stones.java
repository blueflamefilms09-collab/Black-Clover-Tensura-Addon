package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.relics.UseRules;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Piglin;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Why: the Stones are six separate unique relics but one design; the Gauntlet's acts reuse the Stones' bodies, so they live together to keep the two
 * routes (a Stone alone, a Stone through the Gauntlet) one mechanism.
 * <p>
 * Wave 30: the six Infinity Stones and the Gauntlet that holds them. Each Stone has its own acts when worn; the Gauntlet (worn) carries all of them:
 * every Stone found anywhere on the bearer's person is "set in" the Gauntlet and answers through it, stronger by {@link Wave30Rules#gauntletBoost}.
 * With all six, once a game day each, the Gauntlet can make a Greater Wish (a menu of five bounded wishes) and the Snap (half the current health of
 * every hostile mob in forty-eight blocks, a tenth of a boss's maximum, never a player, never a block).
 */
final class Wave30Stones {
    private Wave30Stones() {}

    // ================================================================ what the bearer has
    static Set<String> carried(ServerPlayer p) { return Wave30Kit.carriedIds(p); }

    static boolean gauntlet(ServerPlayer p) { return RelicFlags.has(p, Wave30Tune.F_GAUNTLET); }

    /** The power factor of the Stones' acts for this bearer (1.0 without the Gauntlet). */
    static double boost(ServerPlayer p) { return Wave30Rules.gauntletBoost(gauntlet(p), Wave30Rules.stoneCount(carried(p))); }

    private static int scale(int ticks, double boost) { return (int) Math.min(ticks * boost, 20 * 60 * 30); }

    /** A Stone's act through the Gauntlet: the Stone must be on the bearer. */
    static void through(ActCtx c, int stone) {
        ServerPlayer p = c.player();
        String id = Wave30Tune.STONES[stone];
        if (!carried(p).contains(id)) {
            Wave30Kit.say(p, "The " + Wave30Rules.stoneNames().get(stone) + " Stone is not set in the Gauntlet: carry it.");
            return;
        }
        switch (stone) {
            case 0 -> space(c);
            case 1 -> mindSense(c);
            case 2 -> power(c);
            case 3 -> foresight(c);
            case 4 -> polymorph(c);
            default -> soulLedger(c);
        }
    }

    // ================================================================ Space
    /** The Tesseract: you and up to nine willing companions (players who sneak, your pets) step to the marked place on the same plane. */
    static void space(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        double boost = boost(p);
        BlockPos dest = destination(p, level);
        if (dest == null) {
            Wave30Kit.say(p, "The Space Stone has no mark in this plane and you have no bed here: sneak and use the Stone to set one.");
            return;
        }
        List<Entity> party = new ArrayList<>();
        party.add(p);
        double r = Wave30Tune.SPACE_GATHER * boost;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), e -> e != p && e.isAlive() && e.distanceToSqr(p) <= r * r)) {
            boolean willing = (e instanceof ServerPlayer o && !o.isSpectator() && o.isShiftKeyDown()) || (e instanceof TamableAnimal t && t.isOwnedBy(p));
            if (willing) party.add(e);
        }
        party.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        double x = dest.getX() + 0.5, y = dest.getY(), z = dest.getZ() + 0.5;
        Vec3 from = p.position();
        int moved = 0;
        for (Entity e : party) {
            if (moved >= Wave30Tune.SPACE_MAX_PEOPLE) break;
            if (e instanceof ServerPlayer sp) {
                sp.teleportTo(x, y, z);
                sp.fallDistance = 0;
            } else {
                e.moveTo(x, y, z, e.getYRot(), e.getXRot());
                e.fallDistance = 0;
            }
            moved++;
        }
        RukhFx.burst(level, from.add(0, 1, 0), RukhFx.Alignment.LIGHT, 12, 0.8);
        RukhFx.burst(level, new Vec3(x, y + 1, z), RukhFx.Alignment.LIGHT, 12, 0.8);
        Wave30Kit.say(p, "The Tesseract opens: " + moved + (moved == 1 ? " person steps" : " people step") + " through to " + dest.getX() + ", " + dest.getY() + ", " + dest.getZ() + ".");
    }

    /** The destination on this plane: the mark if it is in this dimension, else the bed or the world spawn; null if the spot is not safe. */
    private static BlockPos destination(ServerPlayer p, ServerLevel level) {
        String dim = level.dimension().location().toString();
        BlockPos at = null;
        if (dim.equals(Wave30Data.getString(p, Wave30Data.MARK_DIM))) {
            at = new BlockPos(Wave30Data.getInt(p, Wave30Data.MARK_X), Wave30Data.getInt(p, Wave30Data.MARK_Y), Wave30Data.getInt(p, Wave30Data.MARK_Z));
        } else if (p.getRespawnPosition() != null && level.dimension().equals(p.getRespawnDimension())) {
            at = p.getRespawnPosition();
        } else if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            at = level.getSharedSpawnPos();
        }
        if (at == null) return null;
        for (int dy = 0; dy <= 3; dy++) {
            BlockPos q = at.above(dy);
            AABB box = p.getBoundingBox().move(q.getX() + 0.5 - p.getX(), q.getY() - p.getY(), q.getZ() + 0.5 - p.getZ());
            if (level.noCollision(p, box)) return q;
        }
        return null;
    }

    /** Sneak and use: the Stone marks the place you stand as the Tesseract's destination on this plane. */
    static void spaceMark(ActCtx c) {
        ServerPlayer p = c.player();
        BlockPos b = p.blockPosition();
        Wave30Data.putString(p, Wave30Data.MARK_DIM, p.serverLevel().dimension().location().toString());
        Wave30Data.putInt(p, Wave30Data.MARK_X, b.getX());
        Wave30Data.putInt(p, Wave30Data.MARK_Y, b.getY());
        Wave30Data.putInt(p, Wave30Data.MARK_Z, b.getZ());
        RukhFx.gather(p.serverLevel(), p, RukhFx.Alignment.LIGHT, 12);
        Wave30Kit.say(p, "The Tesseract remembers this place: " + b.getX() + ", " + b.getY() + ", " + b.getZ() + ".");
    }

    // ================================================================ Mind
    private static boolean sentient(LivingEntity e) {
        return e instanceof Player || e instanceof Npc || e instanceof Raider || e instanceof Piglin || e instanceof Witch;
    }

    /** The surface thoughts and places of every sentient creature within 200 blocks (the nearest twelve are named; those within 96 glow). */
    static void mindSense(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        double r = Wave30Tune.MIND_RADIUS * boost(p);
        AABB box = p.getBoundingBox().inflate(r, Math.min(r, 160), r);
        List<LivingEntity> all = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive() && sentient(e) && !(e instanceof Player o && (o.isCreative() || o.isSpectator()))
                && e.distanceToSqr(p) <= r * r);
        all.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        if (all.isEmpty()) {
            Wave30Kit.say(p, "The Mind Stone hears no thought within " + (int) r + " blocks.");
            return;
        }
        Wave30Kit.say(p, "The Mind Stone hears " + all.size() + " mind" + (all.size() == 1 ? "" : "s") + " within " + (int) r + " blocks:");
        int n = 0;
        for (LivingEntity e : all) {
            if (n++ >= Wave30Tune.MIND_LIST) break;
            Wave30Kit.say(p, "  " + e.getName().getString() + " at " + e.getBlockX() + ", " + e.getBlockY() + ", " + e.getBlockZ() + ": " + thought(e));
            if (e.distanceToSqr(p) <= 96 * 96) Fx.addEffect(e, "minecraft:glowing", 100, 0);
        }
        RukhFx.gather(level, p, RukhFx.Alignment.LIGHT, 12);
    }

    private static String thought(LivingEntity e) {
        if (e instanceof Mob m && m.getTarget() != null) return "bent on " + m.getTarget().getName().getString();
        if (e instanceof Player o) return "holding " + (o.getMainHandItem().isEmpty() ? "nothing" : o.getMainHandItem().getHoverName().getString());
        if (e instanceof Villager v) return "a " + v.getVillagerData().getProfession().toString().replace("minecraft:", "") + ", at its work";
        return "calm";
    }

    /** Reads one mind in sight: what it is, what it holds and wants. A player may resist (DC 20, a natural 20 holds). */
    static void mindRead(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = Wave30Kit.target(p, Wave30Tune.MIND_READ_REACH);
        if (t == null) {
            Wave30Kit.say(p, "Look at a creature to read its mind.");
            return;
        }
        if (t instanceof ServerPlayer o && Wave30Rules.saves(p.getRandom().nextInt(20) + 1, 0, 20)) {
            Wave30Kit.say(p, o.getName().getString() + "'s mind slips out of the Stone's grasp.");
            Wave30Kit.say(o, "Something brushes your thoughts and you push it away.");
            return;
        }
        StringBuilder sb = new StringBuilder(t.getName().getString()).append(": health ").append((int) t.getHealth()).append("/").append((int) t.getMaxHealth());
        if (t instanceof Mob m && m.getTarget() != null) sb.append(", wants to attack ").append(m.getTarget().getName().getString());
        ItemStack held = t.getMainHandItem();
        sb.append(", holds ").append(held.isEmpty() ? "nothing" : held.getHoverName().getString());
        if (t instanceof Villager v) sb.append(", a ").append(v.getVillagerData().getProfession().toString().replace("minecraft:", "")).append(" of level ").append(v.getVillagerData().getLevel());
        if (t instanceof Player o) sb.append(", food ").append(o.getFoodData().getFoodLevel()).append("/20, at ").append(o.getBlockX()).append(", ").append(o.getBlockY()).append(", ").append(o.getBlockZ());
        sb.append(", carrying ").append(t.getActiveEffects().size()).append(" lingering effects");
        Wave30Kit.say(p, sb.toString() + ".");
        RukhFx.stream(p.serverLevel(), p.getEyePosition(), t.position().add(0, t.getBbHeight() * 0.7, 0), RukhFx.Alignment.LIGHT, 6);
    }

    // ================================================================ Power
    /** The Power Stone's burst: a shock of raw power round the bearer; the Stone's real gift (double pools) is its passive. */
    static void power(ActCtx c) {
        ServerPlayer p = c.player();
        double boost = boost(p);
        int n = 0;
        for (LivingEntity t : Fx.foesAround(p, Wave30Tune.POWER_RADIUS * boost)) {
            Wave30Kit.hit(p, t, (float) (Wave30Tune.POWER_DAMAGE * boost), "minecraft:explosion", true);
            Vec3 away = t.position().subtract(p.position()).multiply(1, 0, 1);
            Vec3 dir = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
            t.setDeltaMovement(dir.x * 0.9, 0.35, dir.z * 0.9);
            t.hurtMarked = true;
            n++;
        }
        Fx.ring(p.serverLevel(), p.position(), Wave30Tune.POWER_RADIUS * boost, 0xB070FF);
        RukhFx.burst(p.serverLevel(), p.position().add(0, 1, 0), RukhFx.Alignment.LIGHT, 12, 1.0);
        Wave30Kit.say(p, "Raw power bursts from you" + (n == 0 ? ", and strikes only air." : " and throws back " + n + (n == 1 ? " foe." : " foes.")));
    }

    // ================================================================ Time
    /** Foresight, as a ritual (hold): for twelve thousand ticks the first blow from an attacker in each twenty seconds is turned aside, with luck and the future in sight. */
    static void foresight(ActCtx c) {
        ServerPlayer p = c.player();
        int ticks = scale(Wave30Tune.FORESIGHT_TICKS, boost(p));
        Wave30Data.putLong(p, Wave30Data.FORESIGHT_UNTIL, p.level().getGameTime() + ticks);
        Fx.addEffect(p, "minecraft:luck", ticks, 1);
        Fx.addEffect(p, Wave30Tune.EF_FUTURE_VISION, Math.min(ticks, 2400), 0);
        Fx.lendSkill(p, Wave30Tune.SK_DANGER_SENSE, ticks);
        RukhFx.gather(p.serverLevel(), p, RukhFx.Alignment.LIGHT, 12);
        Wave30Kit.say(p, "Foresight opens: for " + UseRules.span(ticks) + " you cannot be surprised (the first blow from an attacker in each 20 seconds is turned aside).");
    }

    // ================================================================ Reality
    /** Polymorph (as a shrinking and weakening, not a swap): a mob in sight is made small and feeble for half a minute. A boss, a player or an ally is not touched. */
    static void polymorph(ActCtx c) {
        ServerPlayer p = c.player();
        double boost = boost(p);
        LivingEntity t = Wave30Kit.target(p, Wave30Tune.REALITY_REACH * boost);
        if (t == null || !Wave30Kit.controllable(p, t)) {
            Wave30Kit.say(p, "Polymorph needs a mob (not a boss) in your sight.");
            return;
        }
        int ticks = scale(Wave30Tune.POLY_TICKS, boost);
        Wave30Events.shrink((Mob) t, t.level().getGameTime() + ticks);
        Fx.addEffect(t, "minecraft:weakness", ticks, 2);
        Fx.addEffect(t, "minecraft:slowness", ticks, 0);
        Fx.addEffect(t, "minecraft:glowing", ticks, 0);
        RukhFx.burst(p.serverLevel(), t, RukhFx.Alignment.LIGHT, 12);
        Wave30Kit.say(p, t.getName().getString() + " shrinks into a small, feeble form for " + UseRules.span(ticks) + ".");
    }

    /** Seeming: for three minutes you wear another seeming: invisible and hard to sense. */
    static void seeming(ActCtx c) {
        ServerPlayer p = c.player();
        int ticks = scale(Wave30Tune.SEEMING_TICKS, boost(p));
        Fx.addEffect(p, "minecraft:invisibility", ticks, 0);
        Fx.addEffect(p, Wave30Tune.EF_PRESENCE_CONCEAL, ticks, 0);
        RukhFx.burst(p.serverLevel(), p, RukhFx.Alignment.LIGHT, 12);
        Wave30Kit.say(p, "Reality gives you another seeming for " + UseRules.span(ticks) + ": you are unseen.");
    }

    /** Greater Image and Mirage Arcane: foes within fourteen blocks see things that are not there: they lose their target and stumble for ten seconds. */
    static void mirage(ActCtx c) {
        ServerPlayer p = c.player();
        double boost = boost(p);
        int ticks = scale(Wave30Tune.MIRAGE_TICKS, boost);
        long until = p.level().getGameTime() + ticks;
        int n = 0;
        for (LivingEntity t : Fx.foesAround(p, Wave30Tune.MIRAGE_RADIUS * boost)) {
            if (t instanceof Mob m && !Wave30Kit.isBoss(m)) {
                Wave30Events.confuse(m, p, until);
                Fx.addEffect(t, Wave30Tune.EF_CONFUSION, ticks, 0);
                Fx.addEffect(t, "minecraft:slowness", ticks, 0);
                n++;
            }
        }
        Fx.ring(p.serverLevel(), p.position(), Wave30Tune.MIRAGE_RADIUS * boost, 0xFF6A6A);
        Wave30Kit.say(p, n == 0 ? "A mirage shimmers, but no mind is near to be fooled." : "A mirage shimmers: " + n + (n == 1 ? " foe loses" : " foes lose") + " you and wander.");
    }

    // ================================================================ Soul
    /** The souls stored in the Stone: those of anyone who died within 64 blocks of you. One of them can bring you back once a day. */
    static void soulLedger(ActCtx c) {
        ServerPlayer p = c.player();
        int souls = Wave30Data.getInt(p, Wave30Data.SOULS);
        String names = Wave30Data.getString(p, Wave30Data.SOUL_NAMES);
        long today = UseRules.day(p.level().getGameTime());
        boolean ready = Wave30Rules.soulReviveOk(souls, Wave30Data.getLong(p, Wave30Data.SOUL_DAY), today + 1);
        Wave30Kit.say(p, "The Soul Stone holds " + souls + (souls == 1 ? " soul" : " souls") + (names.isEmpty() ? "" : " (" + names + ")")
                + ". True Resurrection is " + (ready ? "ready: it will bring you back from a killing blow." : (souls == 0 ? "silent: it needs a soul." : "spent until dawn.")));
    }

    /** Does this player carry a Soul Stone that works (worn, or set in a worn Gauntlet)? */
    static boolean soulBearer(ServerPlayer p) {
        if (RelicFlags.has(p, Wave30Tune.F_SOUL)) return true;
        return gauntlet(p) && carried(p).contains(Wave30Tune.ID_SOUL);
    }

    // ================================================================ the Gauntlet's wishes
    private static List<net.minecraft.world.entity.LivingEntity> beneficiaries(ServerPlayer p, double r) {
        List<LivingEntity> out = new ArrayList<>();
        ServerLevel level = p.serverLevel();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), e -> e.isAlive() && !e.isSpectator() && e.distanceToSqr(p) <= r * r)) {
            if (e == p || e instanceof ServerPlayer || (e instanceof TamableAnimal t && t.isOwnedBy(p))) out.add(e);
            if (out.size() >= 40) break;
        }
        return out;
    }

    private static boolean ready(ActCtx c) {
        ServerPlayer p = c.player();
        if (!Wave30Rules.snapReady(gauntlet(p), Wave30Rules.stoneCount(carried(p)))) {
            Wave30Kit.say(p, "Only the Gauntlet worn with all six Stones set in it can do this.");
            return false;
        }
        return true;
    }

    static void greatWishPick(ActCtx c) {
        ServerPlayer p = c.player();
        int next = Wave30Rules.nextIndex(Wave30Data.getInt(p, Wave30Data.GREAT_PICK), Wave30Rules.GreatWish.values().length);
        Wave30Data.putInt(p, Wave30Data.GREAT_PICK, next);
        Wave30Rules.GreatWish w = Wave30Rules.GreatWish.values()[next];
        Wave30Kit.say(p, "Greater Wish chosen: " + w.title + " (" + w.text + ").");
    }

    /** The Greater Wish: once a game day, with all six Stones, the chosen bounded wish. */
    static void greatWish(ActCtx c) {
        ServerPlayer p = c.player();
        if (!ready(c)) return;
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        long today = UseRules.day(now);
        if (Wave30Data.getLong(p, Wave30Data.GREAT_DAY) == today + 1) {
            Wave30Kit.say(p, "The Greater Wish has been spoken today; it returns at dawn.");
            return;
        }
        Wave30Rules.GreatWish[] all = Wave30Rules.GreatWish.values();
        Wave30Rules.GreatWish w = all[Math.floorMod(Wave30Data.getInt(p, Wave30Data.GREAT_PICK), all.length)];
        List<LivingEntity> friends = beneficiaries(p, Wave30Tune.WISH_RADIUS);
        switch (w) {
            case TREASURE -> {
                Wave30Kit.give(p, new ItemStack(Items.DIAMOND, Wave30Tune.TREASURE_DIAMONDS));
                Wave30Kit.give(p, new ItemStack(Items.EMERALD, Wave30Tune.TREASURE_EMERALDS));
            }
            case RESTORATION -> {
                for (LivingEntity e : friends) {
                    Wave30Kit.cleanse(e);
                    e.setHealth(e.getMaxHealth());
                    if (e instanceof Player o) o.getFoodData().setFoodLevel(20);
                }
            }
            case RESISTANCE -> {
                for (LivingEntity e : friends) {
                    Fx.addEffect(e, "minecraft:resistance", Wave30Tune.GREAT_WISH_BLESS_TICKS, 1);
                    Fx.addEffect(e, "minecraft:fire_resistance", Wave30Tune.GREAT_WISH_BLESS_TICKS, 0);
                    Fx.addEffect(e, "minecraft:water_breathing", Wave30Tune.GREAT_WISH_BLESS_TICKS, 0);
                }
            }
            case IMMUNITY -> {
                for (LivingEntity e : friends) {
                    Wave30Kit.cleanse(e);
                    if (e instanceof Player o) Wave30Data.putLong(o, Wave30Data.WARD_UNTIL, now + Wave30Tune.WARD_TICKS);
                }
            }
            case REROLL -> {
                float back = Wave30Rules.undoAmount(Float.intBitsToFloat(Wave30Data.getInt(p, Wave30Data.LAST_HIT)), now - Wave30Data.getLong(p, Wave30Data.LAST_HIT_AT), p.getHealth(), p.getMaxHealth());
                if (back > 0f) p.setHealth(p.getHealth() + back);
                Wave30Kit.say(p, back > 0f ? "Reality rerolls: the blow you took is undone (" + (int) back + " health returns)." : "There is no recent blow to undo.");
            }
        }
        Wave30Data.putLong(p, Wave30Data.GREAT_DAY, today + 1);
        Fx.burst(level, p.position(), 0xFFFFFF);
        Wave30Kit.shout(p, "THE GREATER WISH: " + w.title + ".", ChatFormatting.GOLD);
    }

    /** The Snap: once a game day with all six Stones. Every hostile mob within 48 blocks loses half its current health (a boss a tenth of its maximum), never a player, never a block. */
    static void snap(ActCtx c) {
        ServerPlayer p = c.player();
        if (!ready(c)) return;
        ServerLevel level = p.serverLevel();
        long today = UseRules.day(level.getGameTime());
        if (Wave30Data.getLong(p, Wave30Data.SNAP_DAY) == today + 1) {
            Wave30Kit.say(p, "The universe has been touched enough for one day; the Snap returns at dawn.");
            return;
        }
        double r = Wave30Tune.SNAP_RADIUS;
        List<Mob> foes = level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(r), m -> m.isAlive() && !m.isAlliedTo(p) && (m instanceof Enemy || m.getTarget() == p)
                && !m.getPersistentData().hasUUID(Wave30Kit.ALLY_OWNER) && m.distanceToSqr(p) <= r * r);
        foes.sort(Comparator.comparingDouble(m -> m.distanceToSqr(p)));
        int n = 0;
        for (Mob m : foes) {
            if (n >= 40) break;
            float amount = Wave30Rules.snap(m.getMaxHealth(), m.getHealth(), Wave30Kit.isBoss(m));
            if (amount > 0f) Wave30Kit.hurtShare(p, m, amount);
            Fx.addEffect(m, "minecraft:weakness", 200, 1);
            Fx.addEffect(m, "minecraft:slowness", 200, 1);
            RukhFx.burst(level, m, RukhFx.Alignment.DARK, 6);
            n++;
        }
        Wave30Data.putLong(p, Wave30Data.SNAP_DAY, today + 1);
        Fx.addEffect(p, "minecraft:weakness", Wave30Tune.SNAP_TOLL_TICKS, 1);
        Fx.addEffect(p, "minecraft:mining_fatigue", Wave30Tune.SNAP_TOLL_TICKS, 1);
        Fx.addEffect(p, "minecraft:hunger", Wave30Tune.SNAP_TOLL_TICKS, 1);
        Fx.ring(level, p.position(), 10.0, 0xFFD27A);
        Wave30Kit.shout(p, "THE SNAP: dust takes " + n + (n == 1 ? " foe" : " foes") + " by half. The Gauntlet's toll falls on you for thirty seconds.", ChatFormatting.GOLD);
    }

    // ================================================================ the Gauntlet's gifts (every two seconds from the player tick)
    /** Stones set in a worn Gauntlet lend their skills; a carried Power Stone lifts the pools as if worn. */
    static void gauntletTick(ServerPlayer p) {
        Set<String> have = carried(p);
        if (have.contains(Wave30Tune.ID_SPACE)) Fx.lendSkill(p, Wave30Tune.SK_SPATIAL_MOTION, 100);
        if (have.contains(Wave30Tune.ID_MIND)) Fx.lendSkills(p, 100, Wave30Tune.SK_THOUGHT_COMM, Wave30Tune.SK_ANALYTICAL);
        if (have.contains(Wave30Tune.ID_TIME)) Fx.lendSkill(p, Wave30Tune.SK_DANGER_SENSE, 100);
        if (have.contains(Wave30Tune.ID_SOUL)) Fx.lendSkill(p, Wave30Tune.SK_EYE_OF_TRUTH, 100);
        boolean power = have.contains(Wave30Tune.ID_POWER) && !RelicFlags.has(p, Wave30Tune.F_POWER);
        setPower(p, power);
    }

    /** The Power Stone's doubling: +50% magicules and aura, double regeneration (the pools are Tensura's; no new meter). */
    static void setPower(ServerPlayer p, boolean on) {
        Wave30Kit.setMod(p, Wave30Tune.AT_MAX_MAGICULE, "pow_mag", 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, on);
        Wave30Kit.setMod(p, Wave30Tune.AT_MAX_AURA, "pow_aura", 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, on);
        Wave30Kit.setMod(p, Wave30Tune.AT_MAGICULE_REGEN, "pow_regen", 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, on);
    }
}
