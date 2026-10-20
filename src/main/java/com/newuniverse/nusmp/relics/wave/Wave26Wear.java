package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: most of the wave's relics have something they do while merely worn or carried, once a second (a beard that grows at dawn, a curse
 * that strikes at dawn, a headache, the jellyfish's cleansing, the lens's memory, the masks' chatter and their sleeplessness). The handlers
 * keep that work in one place, staggered by entity id so no tick does everything, bounded, and leaving at once for anyone who wears none of
 * them. Per-player values live in the player's persistent data; the few per-session maps are cleared on logout and when the server stops.
 */
final class Wave26Wear {
    private Wave26Wear() {}

    /** Per player: which of the curse-bearing relics they carry and are attuned to (a bit mask of {@link Wave26Util#C_FRANK} and friends). */
    private static final Map<UUID, Integer> CARRY = new HashMap<>();
    /** The lens's memory, per wearer (bounded by {@link Wave26Rules#LEDGER_CAP}). */
    private static final Map<UUID, Wave26Rules.Ledger> LEDGERS = new HashMap<>();

    static boolean carries(ServerPlayer p, int bit) { return (CARRY.getOrDefault(p.getUUID(), 0) & bit) != 0; }

    static Wave26Rules.Ledger ledger(ServerPlayer p) { return LEDGERS.computeIfAbsent(p.getUUID(), k -> new Wave26Rules.Ledger()); }

    static void forget(ServerPlayer p) {
        CARRY.remove(p.getUUID());
        LEDGERS.remove(p.getUUID());
    }

    static void clearAll() {
        CARRY.clear();
        LEDGERS.clear();
    }

    // ================================================================ once a second, per player (the caller staggers it)
    static void pass(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        if ((p.tickCount / 20) % 2 == 0) {                                                  // every two seconds: what curse-bearers does the pack hold?
            int mask = Wave26Util.carried(p);
            if (mask == 0) CARRY.remove(p.getUUID()); else CARRY.put(p.getUUID(), mask);
        }
        Wave26Frank.pass(p);
        int mask = CARRY.getOrDefault(p.getUUID(), 0);
        if ((mask & Wave26Util.C_HELM) != 0) voices(p, sl);
        if (!RelicFlags.has(p, Wave26Rules.F_ANY)) {
            Wave26Util.dropMod(p, "minecraft:generic.knockback_resistance", "beard");
            return;
        }
        boolean goggles = RelicFlags.has(p, Wave26Rules.F_GOGGLES);
        if (goggles) beard(p, sl); else Wave26Util.dropMod(p, "minecraft:generic.knockback_resistance", "beard");
        if (RelicFlags.has(p, Wave26Rules.F_HELM)) menace(p, sl);
        if (RelicFlags.has(p, Wave26Rules.F_BUTT)) headache(p);
        if (RelicFlags.has(p, Wave26Rules.F_LENS) && (p.tickCount / 20) % 2 == 1) sample(p, sl, now);
        boolean diff = RelicFlags.has(p, Wave26Rules.F_DIFF), indiff = RelicFlags.has(p, Wave26Rules.F_INDIFF);
        if (diff) difference(p, now);
        if (indiff) indifference(p, now);
        if (diff || indiff) chatter(p, diff, mask);
        if (RelicFlags.has(p, Wave26Rules.F_WITCH)) {
            Wave26Util.remove(p, "tensura:hypnosis", "tensura:infection");                 // the mask's immunities: hypnosis and disease
        }
    }

    /** The Jellyfish Cap's cleansing, ten times a second is too often: every half second. */
    static void jelly(ServerPlayer p) {
        Wave26Util.remove(p, "minecraft:poison", "tensura:fatal_poison");
    }

    // ================================================================ Goggles of Dwarvenkin
    /** At every dawn there is a one in five chance the wearer grows a magical beard, which lasts the day and steadies the wearer against blows. */
    private static void beard(ServerPlayer p, ServerLevel sl) {
        CompoundTag d = Wave26Util.data(p);
        long today = Wave26Util.today(sl);
        if (!d.contains("beard_day") || d.getLong("beard_day") != today) {
            d.putLong("beard_day", today);
            boolean grows = Wave26Rules.growsBeard(Wave26Rules.die(Wave26Util.rng(p), 100));
            d.putBoolean("beard", grows);
            if (grows) {
                Wave26Util.say(p, "Your chin itches. A magnificent beard has grown overnight. (The miners will want to compare.)", ChatFormatting.GOLD);
                com.newuniverse.nusmp.particle.fx.Fx.burst(sl, FxThemes.RELIC_WAVE_26, p, FxTone.LIGHT, 8);
            }
        }
        if (d.getBoolean("beard")) Wave26Util.setMod(p, "minecraft:generic.knockback_resistance", "beard", Wave26Rules.BEARD_KNOCKBACK, Operation.ADD_VALUE);
        else Wave26Util.dropMod(p, "minecraft:generic.knockback_resistance", "beard");
    }

    // ================================================================ Helm of Whispers
    /** The helm's menace: a hostile creature of no great stature that comes within five blocks and has chosen the wearer may lose its nerve. */
    private static void menace(ServerPlayer p, ServerLevel sl) {
        int n = 0;
        for (LivingEntity e : Wave26Util.near(sl, p.position(), 5.0, x -> x instanceof Mob m && m instanceof Enemy && m.getTarget() == p && x.getMaxHealth() < 60f)) {
            if (n++ >= 4) break;
            if (p.getRandom().nextInt(100) < 15) Wave26Util.give(e, 60, 0, "tensura:fear", "minecraft:weakness");
        }
    }

    /** The curse of Voices: at each dawn, a Wisdom save (DC 16, with the helm's own help); a failure is a spell of madness, helm worn or not. */
    private static void voices(ServerPlayer p, ServerLevel sl) {
        CompoundTag d = Wave26Util.data(p);
        long today = Wave26Util.today(sl);
        if (!d.contains("voices_day")) {
            d.putLong("voices_day", today);
            return;
        }
        if (d.getLong("voices_day") == today) return;
        d.putLong("voices_day", today);
        if (Wave26Rules.madnessStrikes(Wave26Rules.die(Wave26Util.rng(p), 20))) {
            Wave26Util.give(p, Wave26Rules.MADNESS_TICKS, 0, "tensura:insanity", "minecraft:nausea");
            Wave26Util.give(p, Wave26Rules.MADNESS_TICKS / 3, 0, "minecraft:darkness");
            Wave26Util.say(p, "The whispers rise to a roar. You fail the day's save: madness takes you for a while.", ChatFormatting.DARK_GREEN);
        } else {
            Wave26Util.tell(p, "The whispers are only whispers today.", ChatFormatting.DARK_GREEN);
        }
    }

    // ================================================================ Helmet of Headbutting
    private static void headache(ServerPlayer p) {
        CompoundTag d = Wave26Util.data(p);
        int ache = d.getInt("ache") + 20;
        if (ache >= Wave26Rules.HEADACHE_EVERY) {
            ache = 0;
            Wave26Util.give(p, Wave26Rules.HEADACHE_TICKS, 0, "minecraft:nausea");
            Wave26Util.tell(p, "Your head is splitting. You are, frankly, in a mood.", ChatFormatting.RED);
        }
        d.putInt("ache", ache);
    }

    // ================================================================ Lens of Straight Creepin'
    /** Every two seconds the lens notes up to twelve creatures within 24 blocks (once each per spot). */
    private static void sample(ServerPlayer p, ServerLevel sl, long now) {
        Wave26Rules.Ledger l = ledger(p);
        int n = 0;
        for (LivingEntity e : Wave26Util.near(sl, p.position(), 24.0, x -> x != p && !x.isSpectator())) {
            if (n++ >= 12) break;
            int kind = e instanceof Enemy ? 0 : e instanceof Player || e instanceof net.minecraft.world.entity.npc.Villager ? 2 : 1;
            l.add(e.getX(), e.getY(), e.getZ(), now, kind);
        }
    }

    // ================================================================ the masks
    private static final String[] DIFF_LINES = {
            "\"Ooh, are we going somewhere? Please say we're going somewhere!\"", "\"Bored. Bored bored bored. Do something!\"",
            "\"Have you seen my brother? Grey. Very grey. Quiet. You'd have noticed.\"", "\"Who made me, do you think? And why? ...Anyway, ADVENTURE!\"",
            "\"I could cast Suggest on that! Not that. That. No, THAT.\""};
    private static final String[] INDIFF_LINES = {
            "\"...\"", "\"Must we.\"", "\"Leave it be.\"", "\"My sister is loud, somewhere. Let us not go there.\"", "\"Nothing is happening. Good.\""};

    /** Difference never sleeps (and the phantoms stop counting), and she gets restless when the wearer stands about. */
    private static void difference(ServerPlayer p, long now) {
        if (p.isSleeping()) {
            p.stopSleeping();
            Wave26Util.tell(p, "\"No sleeping! Sleeping is the BORING part of the day!\"", ChatFormatting.AQUA);
        }
        if ((p.tickCount / 20) % 10 == 0) p.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        Wave26Util.remove(p, "tensura:sleep", "tensura:drowsiness");                      // it cannot be forced to sleep by any means
        CompoundTag d = Wave26Util.data(p);
        double ax = d.getDouble("idle_x"), az = d.getDouble("idle_z");
        if ((p.getX() - ax) * (p.getX() - ax) + (p.getZ() - az) * (p.getZ() - az) > 16.0) {
            d.putDouble("idle_x", p.getX());
            d.putDouble("idle_z", p.getZ());
            d.putLong("idle_since", now);
        } else if (Wave26Rules.bored(now - d.getLong("idle_since")) && d.getLong("idle_since") != 0L) {
            d.putLong("idle_since", now);
            Wave26Util.give(p, 80, 0, "minecraft:nausea");
            Wave26Util.say(p, "\"We have stood HERE for an HOUR! My eyes are crossing! GO SOMEWHERE!\"", ChatFormatting.AQUA);
        }
    }

    /** Indifference: fright and charm slip off (each tick of a mind effect has an even chance to be shrugged off), and the neutral creatures he is rude to do not get angry. */
    private static void indifference(ServerPlayer p, long now) {
        if (p.getRandom().nextBoolean()) Wave26Util.remove(p, "tensura:fear", "tensura:confusion", "tensura:mind_control", "tensura:hypnosis");
    }

    private static void chatter(ServerPlayer p, boolean diff, int mask) {
        CompoundTag d = Wave26Util.data(p);
        int t = d.getInt("chat") + 20;
        if (t < Wave26Rules.CHATTER_EVERY) {
            d.putInt("chat", t);
            return;
        }
        d.putInt("chat", 0);
        boolean sibling = diff ? (mask & Wave26Util.C_INDIFF) != 0 : (mask & Wave26Util.C_DIFF) != 0;
        String[] lines = diff ? DIFF_LINES : INDIFF_LINES;
        String line = sibling ? (diff ? "\"I can FEEL him! My brother is in your pack! Put him on! No? ...Fine.\"" : "\"She is near. ...Unfortunately.\"") : lines[p.getRandom().nextInt(lines.length)];
        Wave26Util.say(p, line, diff ? ChatFormatting.AQUA : ChatFormatting.GRAY);
    }

    // ================================================================ Mask of Indifference: Dull News
    /** Neutral creatures (wolves, bees, iron golems, piglins) the wearer strikes do not hold the blow against them: their anger is wiped. */
    static void dullNews(LivingEntity victim) {
        if (victim instanceof net.minecraft.world.entity.NeutralMob nm) {
            nm.setRemainingPersistentAngerTime(0);
            nm.setPersistentAngerTarget(null);
            if (victim instanceof Mob m) m.setTarget(null);
        }
    }

    /** The Jellyfish Wrath: when a creature within nine blocks strikes the wearer, the cap expels poisonous cells: 1d6 poison damage (half on a save), and a few seconds of poison. */
    static void wrath(ServerPlayer wearer, LivingEntity attacker) {
        if (attacker == wearer || !Wave26Util.target(wearer, attacker) || attacker.distanceTo(wearer) > Wave26Rules.WRATH_RANGE) return;
        CompoundTag d = Wave26Util.data(wearer);
        long now = wearer.level().getGameTime();
        if (now < d.getLong("wrath_at")) return;
        d.putLong("wrath_at", now + Wave26Rules.WRATH_COOLDOWN);
        boolean saved = Wave26Util.epSaves(wearer, attacker);
        float dmg = Wave26Rules.wrathDamage(Wave26Rules.die(Wave26Util.rng(wearer), 6), saved);
        Fx.damage(wearer, attacker, dmg, "minecraft:magic");
        if (!saved) Wave26Util.give(attacker, Wave26Rules.WRATH_POISON_TICKS, 0, "minecraft:poison");
        com.newuniverse.nusmp.particle.fx.Fx.burst((ServerLevel) wearer.level(), FxThemes.RELIC_WAVE_26, attacker, FxTone.LIGHT, 8);
        Wave26Util.tell(wearer, "Jellyfish Wrath: poisonous cells lash out at " + attacker.getName().getString() + ".", ChatFormatting.AQUA);
    }
}
