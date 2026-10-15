package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;

import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicEngine;
import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Wave 15, worn things that are not bracers: the Boots of Burglery (a stride through walls), and the nine cloaks, capes and mantles. The ability code
 * of each card is one static method here; the once-a-second upkeep is {@link #clocks} (always, so a relic taken off is cleaned up) and
 * {@link #pass} (only for a wearer).
 */
final class Wave15Wear {
    private Wave15Wear() {}

    private static final String ARMOR = "minecraft:generic.armor";

    // ================================================================ Boots of Burglery
    /**
     * Misty Step. Plain: blink up to nine blocks along the look, to a safe place. Sneaking: slip through a wall in front of you (it must start within
     * three blocks and be at most five thick) to the first place to stand behind it, as the thief's modification does.
     */
    static void mistyStep(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        Vec3 from = p.position();
        boolean ok = c.sneaking() ? slipThroughWall(p) : Fx.teleportLook(p, 9.0);
        if (!ok) {
            Wave15Util.tell(p, c.sneaking() ? "No room behind that wall." : "No room to step into.", ChatFormatting.GRAY);
            return;
        }
        Fx.burst(lv, from, 0xCFE8FF);
        Fx.burst(lv, p.position(), 0xCFE8FF);
        lv.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 1.5f);
    }

    private static boolean slipThroughWall(ServerPlayer p) {
        ServerLevel lv = p.serverLevel();
        if (LivingDungeon.in(lv)) return false;                                      // the Dungeon's walls and sealed rooms are not for slipping through
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        int n = Wave15Rules.SLIP_STEPS + 1;
        boolean[] blocked = new boolean[n], stand = new boolean[n];
        Vec3[] spots = new Vec3[n];
        for (int i = 1; i < n; i++) {
            Vec3 at = eye.add(look.scale(0.5 * i));
            BlockPos bp = BlockPos.containing(at);
            blocked[i] = !lv.getBlockState(bp).getCollisionShape(lv, bp).isEmpty();
            if (!blocked[i] && i > 1) {
                spots[i] = standAt(lv, p, at.x, at.y - p.getEyeHeight(), at.z);
                stand[i] = spots[i] != null;
            }
        }
        int idx = Wave15Rules.slipIndex(blocked, stand);
        if (idx < 0) return false;
        Vec3 s = spots[idx];
        p.teleportTo(s.x, s.y, s.z);
        p.fallDistance = 0;
        return true;
    }

    /** A place to stand near this point: room for the player and ground within three blocks below (no lava, no fire). */
    private static Vec3 standAt(ServerLevel lv, ServerPlayer p, double x, double y, double z) {
        for (int dy : new int[]{0, 1, -1, 2, -2}) {
            double yy = y + dy;
            if (!lv.noCollision(p, p.getBoundingBox().move(x - p.getX(), yy - p.getY(), z - p.getZ()))) continue;
            BlockPos feet = BlockPos.containing(x, yy, z);
            for (int i = 1; i <= 3; i++) {
                BlockState below = lv.getBlockState(feet.below(i));
                if (below.is(Blocks.LAVA) || below.is(Blocks.FIRE) || below.is(Blocks.MAGMA_BLOCK)) return null;
                if (!below.getCollisionShape(lv, feet.below(i)).isEmpty()) return new Vec3(x, yy, z);
            }
        }
        return null;
    }

    /** Stealth +3: while sneaking in the boots, Tensura's presence concealment is kept on you. */
    static void bootsPass(ServerPlayer p) {
        if (p.isShiftKeyDown()) Wave15Util.giveQuiet(p, 60, 0, "tensura:presence_concealment");
    }

    // ================================================================ Handy Towel-Cape
    static void wrap(ActCtx c) {
        boolean on = c.isOn("wrap");
        Wave15Util.tell(c.player(), on ? "You wrap the towel round you: warm, and both hands full." : "You shrug the towel off.", ChatFormatting.GOLD);
    }

    static boolean wrapped(ServerPlayer p) { return RelicEngine.isOn(p, Wave15Rules.ID_TOWEL, "wrap"); }

    /** Wrapped: hands full (a little slower), frost cannot touch you, Tensura cold resistance lent. */
    static void towelPass(ServerPlayer p) {
        if (!wrapped(p)) return;
        Wave15Util.giveQuiet(p, 60, 0, "minecraft:slowness");
        Fx.lendSkill(p, "tensura:cold_resistance", 80);
        if (p.getTicksFrozen() > 0) p.setTicksFrozen(0);
    }

    /** Rest beneath the stars: holding still under the open sky at night heals twice as much as indoors. */
    static void starRest(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        boolean sky = lv.canSeeSky(p.blockPosition()) && lv.isNight();
        Fx.heal(p, sky ? 12f : 6f);
        Wave15Util.giveQuiet(p, 100, 0, "minecraft:regeneration");
        Wave15Util.remove(p, "minecraft:weakness", "minecraft:mining_fatigue");
        RukhFx.spray(lv, RukhFx.Alignment.LIGHT, p.getX(), p.getY() + 1.0, p.getZ(), sky ? 24 : 8, 0.6, 0.8, 0.6, 0.02);
        Wave15Util.tell(p, sky ? "The towel and the stars: twice the rest." : "A rest on the towel. (Under an open night sky it would heal twice as much.)", ChatFormatting.GOLD);
    }

    /** "Great practical value": dry in an instant; fire out, frost and chill gone. */
    static void dryOff(ActCtx c) {
        ServerPlayer p = c.player();
        p.clearFire();
        p.setTicksFrozen(0);
        Wave15Util.remove(p, "tensura:chill", "tensura:frost");
        RukhFx.spray(c.player().serverLevel(), RukhFx.Alignment.LIGHT, p.getX(), p.getY() + 1.0, p.getZ(), 10, 0.4, 0.6, 0.4, 0.02);
        Wave15Util.tell(p, "Dry. \"42!\"", ChatFormatting.GOLD);
    }

    // ================================================================ Cape of Bees
    static void swarm(ActCtx c) {
        ServerPlayer p = c.player();
        Wave15Util.Aim a = Wave15Util.aim(p, 16.0);
        Vec3 at = a.hit() != null ? a.hit().position().add(0, a.hit().getBbHeight() * 0.6, 0) : a.end();
        Wave15Field.release(p, a.hit(), at);
        RukhFx.spray(p.serverLevel(), Wave15Util.align(0xF2C230), p.getX(), p.getY() + 1.2, p.getZ(), 24, 0.5, 0.4, 0.5, 0.1);
        Wave15Util.trail(p.serverLevel(), p.getEyePosition().add(0, -0.4, 0), at, Wave15Util.dust(0x1E1C1A, 1.0f));
        Wave15Util.tell(p, "The cape flaps open: bees!", ChatFormatting.YELLOW);
    }

    // ================================================================ Cloak of the First Snow
    /** Gone in a Flurry: speed, a sting of frost on whatever stands next to you, and a window in which a melee blow each second is turned aside. */
    static void flurry(ActCtx c) {
        ServerPlayer p = c.player();
        long now = p.level().getGameTime();
        CompoundTag d = Wave15Util.data(p);
        d.putLong("evade_until", now + Wave15Rules.FLURRY_TICKS);
        d.putString("evade_kind", "flurry");
        d.putLong("evade_last", 0L);
        Wave15Util.giveQuiet(p, Wave15Rules.FLURRY_TICKS, 0, "minecraft:speed");
        for (LivingEntity t : Fx.foesAround(p, 3.5)) Wave15Util.give(t, 60, 0, "tensura:chill", "minecraft:slowness");
        ServerLevel lv = p.serverLevel();
        RukhFx.spray(lv, RukhFx.Alignment.LIGHT, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.7, 0.8, 0.7, 0.05);
        Wave15Util.tell(p, "Gone in a flurry: nothing catches you for a while.", ChatFormatting.AQUA);
    }

    // ================================================================ Autumn Cloak
    static boolean inWoods(ServerPlayer p) {
        var biome = p.level().getBiome(p.blockPosition());
        return biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA);
    }

    /** On command, the leaves swirl: for a minute a quarter of the blows against you (melee or shot) miss. */
    static void leafSwirl(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave15Util.data(p);
        d.putLong("evade_until", p.level().getGameTime() + Wave15Rules.BLUR_TICKS);
        d.putString("evade_kind", "blur");
        RukhFx.spray(p.serverLevel(), Wave15Util.align(0xE08A2A), p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.7, 0.9, 0.7, 0.05);
        Wave15Util.tell(p, "The leaves swirl round you, blurring you for a minute.", ChatFormatting.GOLD);
    }

    /** Leaves keep drifting off a blurred wearer; snowflakes off one in a flurry. */
    static void evadeFlavour(ServerPlayer p) {
        CompoundTag d = Wave15Util.peek(p);
        if (d == null || p.level().getGameTime() >= d.getLong("evade_until")) return;
        ServerLevel lv = p.serverLevel();
        if ("flurry".equals(d.getString("evade_kind"))) RukhFx.spray(lv, RukhFx.Alignment.LIGHT, p.getX(), p.getY() + 1.0, p.getZ(), 6, 0.5, 0.7, 0.5, 0.03);
        else RukhFx.spray(lv, Wave15Util.align(0xC8601E), p.getX(), p.getY() + 1.0, p.getZ(), 5, 0.5, 0.7, 0.5, 0.03);
    }

    // ================================================================ Darkened Cloak of Elvenkind
    static boolean hoodUp(ServerPlayer p) { return RelicEngine.isOn(p, Wave15Rules.ID_ELVEN, "hood"); }

    static void hood(ActCtx c) {
        ServerPlayer p = c.player();
        if (c.isOn("hood")) {
            Wave15Util.tell(p, "Hood up: you fade into the cloak's colours.", ChatFormatting.DARK_GRAY);
            return;
        }
        CompoundTag d = Wave15Util.data(p);                                          // hood down: the spells it holds end early
        Wave15Util.remove(p, "tensura:presence_concealment");
        if (d.getBoolean("elven_inv")) {
            d.remove("elven_inv");
            Wave15Util.remove(p, "minecraft:invisibility");
        }
        d.remove("elven_trace_until");
        Wave15Util.tell(p, "Hood down: the cloak's spells end.", ChatFormatting.GRAY);
    }

    private static Wave15Rules.Charges charges(ServerPlayer p) {
        CompoundTag d = Wave15Util.data(p);
        Wave15Rules.Charges c = new Wave15Rules.Charges(Wave15Rules.ELVEN_MAX, d.contains("elven_left") ? d.getInt("elven_left") : Wave15Rules.ELVEN_MAX, d.getLong("elven_day"));
        return c.at(Wave15Rules.dayOf(p.level().getDayTime()), Wave15Rules.dawnGain(p.getRandom().nextInt(4)));
    }

    private static void save(ServerPlayer p, Wave15Rules.Charges c) {
        CompoundTag d = Wave15Util.data(p);
        d.putInt("elven_left", c.left());
        d.putLong("elven_day", c.day());
    }

    /** The hood must be up and the pool must hold the charges; false (with a line) otherwise. */
    private static boolean spendCharges(ServerPlayer p, int cost) {
        if (!hoodUp(p)) {
            Wave15Util.tell(p, "Pull the hood up first.", ChatFormatting.GRAY);
            return false;
        }
        Wave15Rules.Charges c = charges(p);
        if (!c.canSpend(cost)) {
            save(p, c);
            Wave15Util.tell(p, "Not enough charges (" + c.left() + " left; 1d4+1 come back each dawn).", ChatFormatting.RED);
            return false;
        }
        c = c.spend(cost);
        save(p, c);
        Wave15Util.tell(p, "The cloak spends " + cost + " (" + c.left() + " of " + Wave15Rules.ELVEN_MAX + " left).", ChatFormatting.DARK_AQUA);
        return true;
    }

    /** Pass Without a Trace (1 charge): a minute of stealth for you and friends within ten blocks; foes beyond three blocks cannot pick you out. */
    static void passWithoutTrace(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spendCharges(p, Wave15Rules.TRACE_COST)) return;
        Wave15Util.data(p).putLong("elven_trace_until", p.level().getGameTime() + 1200);
        for (LivingEntity f : Wave15Util.near(p.serverLevel(), p.position(), 10, e -> Wave15Util.friend(p, e))) Wave15Util.give(f, 1200, 0, "tensura:presence_concealment");
        Wave15Util.giveQuiet(p, 1200, 0, "tensura:presence_concealment");
        RukhFx.spray(p.serverLevel(), RukhFx.Alignment.DARK, p.getX(), p.getY() + 0.3, p.getZ(), 20, 0.8, 0.2, 0.8, 0.01);
    }

    /** Darkness (2 charges): a cloud of dark round you; foes within eight blocks cannot see, you can. */
    static void darkness(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spendCharges(p, Wave15Rules.DARKNESS_COST)) return;
        ServerLevel lv = p.serverLevel();
        for (LivingEntity t : Fx.foesAround(p, 8.0)) Wave15Util.give(t, 120, 0, "minecraft:blindness");
        Wave15Util.giveQuiet(p, 600, 0, "minecraft:night_vision");
        Fx.ring(lv, p.position(), 8.0, 0x20243A);
        RukhFx.spray(lv, RukhFx.Alignment.DARK, p.getX(), p.getY() + 1.0, p.getZ(), 40, 3.0, 0.8, 3.0, 0.01);
    }

    /** Greater Invisibility (3 charges): half a minute unseen; lowering the hood ends it. */
    static void greaterInvisibility(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spendCharges(p, Wave15Rules.INVISIBILITY_COST)) return;
        Wave15Util.data(p).putBoolean("elven_inv", true);
        Wave15Util.giveQuiet(p, 600, 0, "minecraft:invisibility");
        RukhFx.spray(p.serverLevel(), RukhFx.Alignment.DARK, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
    }

    /** Hood up keeps the Tensura stealth effect on you; the cloak's wearer sneaks better. */
    static void elvenPass(ServerPlayer p) {
        if (hoodUp(p)) Wave15Util.giveQuiet(p, 60, 0, "tensura:presence_concealment");
    }

    /**
     * Is this creature allowed to pick the wearer as a target? With the hood up a creature further than eight blocks away cannot (three while Pass
     * Without a Trace lasts); one the wearer has just hurt always can.
     */
    static boolean elvenHides(Mob mob, ServerPlayer p) {
        if (!RelicFlags.has(p, Wave15Rules.F_ELVEN) || !hoodUp(p) || mob.getLastHurtByMob() == p) return false;
        CompoundTag d = Wave15Util.peek(p);
        boolean trace = d != null && p.level().getGameTime() < d.getLong("elven_trace_until");
        double reach = trace ? 3.0 : 8.0;
        return mob.distanceToSqr(p) > reach * reach;
    }

    // ================================================================ Cloak of Levitation
    static void levitate(ActCtx c) {
        Wave15Util.tell(c.player(), c.isOn("levitate") ? "The cloak lifts you: you hover." : "The cloak sets you down.", ChatFormatting.GOLD);
    }

    static boolean levitating(ServerPlayer p) { return RelicEngine.isOn(p, Wave15Rules.ID_LEVITATE, "levitate"); }

    /** Smother: the cloak wraps a creature within five blocks, holding it fast and blind for a few seconds. */
    static void smother(ActCtx c) {
        ServerPlayer p = c.player();
        Wave15Util.Aim a = Wave15Util.aim(p, 5.0);
        if (a.hit() == null) {
            Wave15Util.tell(p, "Nothing within reach to smother.", ChatFormatting.GRAY);
            return;
        }
        smotherTarget(p, a.hit(), true);
    }

    static void smotherTarget(ServerPlayer p, LivingEntity t, boolean hurt) {
        if (Wave15Util.saves(p, t)) {
            Wave15Util.tell(p, "It slips out of the cloak's grip.", ChatFormatting.GRAY);
            return;
        }
        if (hurt) Fx.damageHeavy(p, t, 5f, "minecraft:magic");
        Wave15Util.give(t, 80, 5, "minecraft:slowness");
        Wave15Util.give(t, 80, 0, "minecraft:weakness");
        Wave15Util.give(t, 40, 0, "minecraft:blindness");
        RukhFx.spray(p.serverLevel(), Wave15Util.align(0xB02A2A), t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 24, 0.4, 0.5, 0.4, 0.02);
    }

    /** Hover: flight while the toggle is on (and the cloak is worn); taken away again when it is not. Creative and spectator players are left alone. */
    static void levitationClock(ServerPlayer p) {
        CompoundTag peek = Wave15Util.peek(p);
        boolean marked = peek != null && peek.getBoolean("lev_flying");
        if (p.isCreative() || p.isSpectator()) return;
        boolean want = RelicFlags.has(p, Wave15Rules.F_LEVITATE) && levitating(p);
        var ab = p.getAbilities();
        if (want && !ab.mayfly) {
            ab.mayfly = true;
            Wave15Util.data(p).putBoolean("lev_flying", true);
            p.onUpdateAbilities();
        } else if (!want && marked) {
            ab.mayfly = false;
            ab.flying = false;
            Wave15Util.data(p).remove("lev_flying");
            p.onUpdateAbilities();
        }
    }

    /** The sentient cloak's reaction: struck below two fifths of your health, it smothers the attacker (once per half minute). */
    static void levitationReaction(ServerPlayer p, LivingEntity attacker, float after) {
        if (attacker == null || after > p.getMaxHealth() * 0.4f) return;
        long now = p.level().getGameTime();
        CompoundTag d = Wave15Util.data(p);
        if (now < d.getLong("lev_react")) return;
        d.putLong("lev_react", now + 600);
        if (!Wave15Util.target(p, attacker)) return;
        smotherTarget(p, attacker, false);
        Wave15Util.tell(p, "The cloak moves on its own and smothers your attacker.", ChatFormatting.GOLD);
    }

    // ================================================================ Cape of Projection
    /** Smoulder: soft creatures within eight blocks lose interest in you for five seconds. */
    static void smoulder(ActCtx c) {
        ServerPlayer p = c.player();
        long until = p.level().getGameTime() + 100;
        int n = 0;
        for (LivingEntity t : Wave15Util.near(p.serverLevel(), p.position(), 8.0, e -> Wave15Util.softMob(p, e) && !Wave15Util.saves(p, e))) {
            if (!(t instanceof Mob m)) continue;
            if (n++ >= 12) break;
            m.setTarget(null);
            m.getPersistentData().putUUID("w15_calm_who", p.getUUID());
            m.getPersistentData().putLong("w15_calm_until", until);
            RukhFx.spray(p.serverLevel(), RukhFx.Alignment.LIGHT, m.getX(), m.getY() + m.getBbHeight() + 0.2, m.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
        }
        RukhFx.spray(p.serverLevel(), RukhFx.Alignment.LIGHT, p.getX(), p.getY() + 1.7, p.getZ(), 10, 0.3, 0.2, 0.3, 0.01);
        Wave15Util.tell(p, n > 0 ? "A smouldering look: " + n + " lose interest." : "A smouldering look... nobody was impressed.", ChatFormatting.GOLD);
    }

    /** Has the wearer's stare calmed this creature towards this target? */
    static boolean calmed(Mob m, LivingEntity target) {
        CompoundTag d = m.getPersistentData();
        return d.hasUUID("w15_calm_who") && d.getUUID("w15_calm_who").equals(target.getUUID()) && m.level().getGameTime() < d.getLong("w15_calm_until");
    }

    // ================================================================ Mantle of Selflessness
    /** Healing Wave: heals every friend within eight blocks, never the wearer; on a natural 20 (one in twenty) the wave comes twice. */
    static void healingWave(ActCtx c) {
        ServerPlayer p = c.player();
        int waves = p.getRandom().nextInt(20) == 19 ? 2 : 1;
        int healed = 0;
        for (int w = 0; w < waves; w++) healed = Math.max(healed, wave(p, 6f + 1 + p.getRandom().nextInt(4)));
        Fx.ring(p.serverLevel(), p.position(), 6.0, 0xFFF2B0);
        Wave15Util.tell(p, healed == 0 ? "No one near needs you." : (waves == 2 ? "A natural 20: the mantle heals twice! " : "") + "The mantle heals " + healed + " (not you).", ChatFormatting.WHITE);
    }

    private static int wave(ServerPlayer p, float amount) {
        int n = 0;
        for (LivingEntity f : Wave15Util.near(p.serverLevel(), p.position(), 8.0, e -> e != p && Wave15Util.friend(p, e))) {
            if (n >= 12) break;
            if (f instanceof ServerPlayer sp) Fx.heal(sp, amount);
            else f.heal(amount);
            RukhFx.spray(p.serverLevel(), RukhFx.Alignment.LIGHT, f.getX(), f.getY() + f.getBbHeight() + 0.2, f.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
            n++;
        }
        return n;
    }

    /** Even unasked, the mantle eases friends who are badly hurt: a little, every ten seconds. */
    static void mantlePass(ServerPlayer p) {
        if ((p.tickCount / 20) % 10 != 0) return;
        int n = 0;
        for (LivingEntity f : Wave15Util.near(p.serverLevel(), p.position(), 6.0, e -> e != p && e instanceof Player && Wave15Util.friend(p, e) && e.getHealth() < e.getMaxHealth() * 0.5f)) {
            if (n++ >= 6) break;
            if (f instanceof ServerPlayer sp) Fx.heal(sp, 2f);
        }
    }

    // ================================================================ upkeep
    /** Runs once a second for every player, even one wearing none of this wave's relics: takes back what a relic that came off had given. */
    static void clocks(ServerPlayer p) {
        CompoundTag d = Wave15Util.peek(p);
        boolean autumn = RelicFlags.has(p, Wave15Rules.F_AUTUMN) && inWoods(p);
        if (autumn) Wave15Util.ensureMod(p, ARMOR, "autumn_ac", 1.0, Operation.ADD_VALUE);
        else Wave15Util.dropMod(p, ARMOR, "autumn_ac");
        boolean fore = d != null && p.level().getGameTime() < d.getLong("foresight_until") && RelicFlags.has(p, Wave15Rules.F_CREST);
        if (fore) Wave15Util.ensureMod(p, ARMOR, "foresight_ac", 2.0, Operation.ADD_VALUE);
        else Wave15Util.dropMod(p, ARMOR, "foresight_ac");
        if ((d != null && d.getBoolean("lev_flying")) || RelicFlags.has(p, Wave15Rules.F_LEVITATE)) levitationClock(p);
    }

    /** Runs once a second for a wearer of any relic of this wave. */
    static void pass(ServerPlayer p) {
        if (RelicFlags.has(p, Wave15Rules.F_BOOTS)) bootsPass(p);
        if (RelicFlags.has(p, Wave15Rules.F_TOWEL)) towelPass(p);
        if (RelicFlags.has(p, Wave15Rules.F_ELVEN)) elvenPass(p);
        if (RelicFlags.has(p, Wave15Rules.F_MANTLE)) mantlePass(p);
        if (RelicFlags.has(p, Wave15Rules.F_SNOW) || RelicFlags.has(p, Wave15Rules.F_AUTUMN)) evadeFlavour(p);
    }
}
