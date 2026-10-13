package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.relics.wave.Wave3Rules.Godking;
import com.newuniverse.nusmp.relics.wave.Wave3Rules.Motes;
import io.github.manasmods.tensura.effect.FearEffect;
import io.github.manasmods.tensura.effect.HypnosisEffect;
import io.github.manasmods.tensura.effect.InsanityEffect;
import io.github.manasmods.tensura.effect.debuff.ConfusionEffect;
import io.github.manasmods.tensura.effect.debuff.DrowsinessEffect;
import io.github.manasmods.tensura.effect.debuff.LustEmbracementEffect;
import io.github.manasmods.tensura.effect.debuff.MindControlEffect;
import io.github.manasmods.tensura.effect.debuff.SleepEffect;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Wave 3, the head relics: the Arcanist's Circlet (three motes), Pride's Crown (an aura and a reflected fright), the Dreamwalker Cowl (three
 * dream charges), the Captain's Tricorn (eight charges of orders) and the Godking Crown (ten minutes of flight, then the reckoning).
 * Numbers live in {@link Wave3Rules}; per-player values are kept in {@link Wave3Util#data}.
 */
final class Wave3Head {
    private Wave3Head() {}

    private static final String K_MOTES = "motes", K_MOTES_AT = "motes_at", K_HUBRIS = "hubris", K_DREAM = "dream", K_TRI = "tri", K_TRI_DAY = "tri_day",
            K_GK = "gk_until", K_GK_WARN = "gk_warn", K_GK_FLY = "gk_flew";
    private static final double MOTE_RANGE = 36.0;
    private static final int[] MOTE_COLOURS = {0x6FF7E6, 0xFF7FD9, 0xA98BFF};
    private static final int MINT = 0x5CF0B4;

    // ================================================================ Arcanist's Circlet
    /** Magic Missile: one mote flies at the creature you look at (within 36 blocks) and never misses; when the third is gone they gather for a minute. */
    static void missile(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        CompoundTag d = Wave3Util.data(p);
        Motes m = new Motes(d.getInt(K_MOTES), d.getLong(K_MOTES_AT)).at(now);
        if (!m.canFire(now)) {
            Wave3Util.tell(p, "The motes are gathering again (" + m.secondsToRefill(now) + " s).", ChatFormatting.GRAY);
            return;
        }
        Wave3Util.Aim a = Wave3Util.aim(p, MOTE_RANGE);
        if (a.hit() == null) {
            Wave3Util.tell(p, "No creature in sight within 36 blocks: the mote stays.", ChatFormatting.GRAY);
            return;
        }
        Motes after = m.fire(now);
        d.putInt(K_MOTES, after.left());
        d.putLong(K_MOTES_AT, after.refillAt());
        int colour = MOTE_COLOURS[Math.floorMod(after.left(), MOTE_COLOURS.length)];
        Vec3 to = a.hit().getBoundingBox().getCenter();
        Wave3Util.trail(sl, p.getEyePosition().add(0, 0.35, 0), to, Wave3Util.dust(colour, 1.1f));
        sl.sendParticles(Wave3Util.dust(colour, 1.6f), to.x, to.y, to.z, 10, 0.25, 0.25, 0.25, 0.04);
        Fx.damage(p, a.hit(), Wave3Rules.MOTE_DAMAGE, "minecraft:magic");
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f + 0.2f * after.left());
        Wave3Util.tell(p, "Magic Missile: " + after.left() + " of " + Wave3Rules.MOTES + " motes left", ChatFormatting.LIGHT_PURPLE);
    }

    /** The motes that are left circle the wearer's head (called every few ticks for a wearer; particles only). */
    static void orbit(ServerPlayer p, long now) {
        if (p.isInvisible() || p.isSpectator()) return;
        CompoundTag d = Wave3Util.peek(p);
        int left = d == null ? Wave3Rules.MOTES : new Motes(d.getInt(K_MOTES), d.getLong(K_MOTES_AT)).at(now).left();
        if (left <= 0) return;
        ServerLevel sl = p.serverLevel();
        double y = p.getEyeY() + 0.45;
        for (int i = 0; i < left; i++) {
            double a = now * 0.14 + i * (Math.PI * 2.0 / Wave3Rules.MOTES);
            sl.sendParticles(Wave3Util.dust(MOTE_COLOURS[i % MOTE_COLOURS.length], 0.9f), p.getX() + Math.cos(a) * 0.6, y + Math.sin(now * 0.09 + i) * 0.06,
                    p.getZ() + Math.sin(a) * 0.6, 1, 0, 0, 0, 0);
        }
    }

    // ================================================================ Pride's Crown
    /** Aura of Superiority (intimidation as Tensura existence): hostile creatures within six blocks whose existence does not exceed the wearer's (not players, not bosses) slow down. Every two seconds. */
    static void prideAura(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        for (LivingEntity e : Wave3Util.near(sl, p.position(), 6.0, x -> x instanceof Enemy && x instanceof Mob && x.getMaxHealth() < 150f && Wave3Util.target(p, x)
                && Wave3Util.outranks(p, x)))
            Wave3Util.give(e, 60, 0, "minecraft:slowness");
        sl.sendParticles(ParticleTypes.FLAME, p.getX(), p.getEyeY() + 0.55, p.getZ(), 3, 0.25, 0.1, 0.25, 0.01);
    }

    /** Is this a spell or effect that would charm, frighten or befuddle its victim? */
    static boolean isMindEffect(MobEffect eff) {
        return eff instanceof FearEffect || eff instanceof HypnosisEffect || eff instanceof InsanityEffect || eff instanceof ConfusionEffect
                || eff instanceof LustEmbracementEffect || eff instanceof MindControlEffect;
    }

    /** Magical sleep (the Tensura sleep and drowsiness). */
    static boolean isSleepEffect(MobEffect eff) { return eff instanceof SleepEffect || eff instanceof DrowsinessEffect; }

    /**
     * Hubris's Fall: once a day the crown refuses a charm or fright aimed at its wearer and turns it back on whoever sent it (or, if that cannot be
     * told, the nearest foe within twelve blocks): five seconds of fear, half of that against a boss.
     */
    static void hubris(ServerPlayer p, MobEffectEvent.Applicable e) {
        ServerLevel sl = p.serverLevel();
        long today = Wave3Util.today(sl);
        CompoundTag d = Wave3Util.data(p);
        if (!Wave3Rules.dailyReady(d.getLong(K_HUBRIS), today)) return;
        d.putLong(K_HUBRIS, Wave3Rules.dailyMark(today));
        e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        LivingEntity src = e.getEffectSource() instanceof LivingEntity le && le != p && le.isAlive() ? le : null;
        if (src == null) {
            List<LivingEntity> foes = Fx.foesAround(p, 12.0);
            if (!foes.isEmpty()) src = foes.get(0);
        }
        Wave3Util.say(p, "Pride's Crown: your will does not bend. Hubris's Fall" + (src == null ? " finds no one to turn on." : " turns it back on " + src.getName().getString() + "."));
        sl.sendParticles(ParticleTypes.FLAME, p.getX(), p.getEyeY() + 0.5, p.getZ(), 24, 0.4, 0.4, 0.4, 0.05);
        if (src == null) return;
        int ticks = src.getMaxHealth() >= 150f ? Wave3Rules.FEAR_TICKS / 2 : Wave3Rules.FEAR_TICKS;
        Wave3Util.give(src, ticks, 0, "tensura:fear", "minecraft:weakness");
        Wave3Util.trail(sl, p.getEyePosition(), src.getBoundingBox().getCenter(), ParticleTypes.FLAME);
    }

    // ================================================================ Dreamwalker Cowl
    static int dreamHave(ServerPlayer p) {
        CompoundTag d = Wave3Util.data(p);
        return Wave3Rules.normalize(d.contains(K_DREAM, Tag.TAG_INT) ? d.getInt(K_DREAM) : -1, Wave3Rules.DREAM_MAX);
    }

    private static boolean spendDream(ServerPlayer p, int cost) {
        int have = dreamHave(p);
        if (!Wave3Rules.canSpend(have, cost)) {
            Wave3Util.tell(p, "The cowl has " + have + " of " + Wave3Rules.DREAM_MAX + " dream charges (" + cost + " needed): sleep through a night to refill them.", ChatFormatting.GRAY);
            return false;
        }
        int left = Wave3Rules.spend(have, cost);
        Wave3Util.data(p).putInt(K_DREAM, left);
        Wave3Util.tell(p, "Dream charges: " + left + " of " + Wave3Rules.DREAM_MAX, ChatFormatting.LIGHT_PURPLE);
        return true;
    }

    /** Waking from a night's sleep with the cowl on: every charge comes back. */
    static void dreamRecharge(ServerPlayer p) {
        if (!RelicFlags.has(p, Wave3Rules.F_DREAMER)) return;
        Wave3Util.data(p).putInt(K_DREAM, Wave3Rules.DREAM_MAX);
        Wave3Util.tell(p, "You wake rested: the cowl's dream charges are full again.", ChatFormatting.LIGHT_PURPLE);
    }

    /** Sleep (1 charge): up to five hostile creatures within eight blocks, the weakest first, fall asleep for eight seconds. */
    static void sleepSpell(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        List<LivingEntity> pool = new ArrayList<>(Wave3Util.near(sl, p.position(), 8.0, x -> x instanceof Enemy && Wave3Util.bends(p, x)));
        if (pool.isEmpty()) {
            Wave3Util.tell(p, "Sleep: nothing hostile within 8 blocks; no charge was spent.", ChatFormatting.GRAY);
            return;
        }
        if (!spendDream(p, 1)) return;
        pool.sort(Comparator.comparingDouble(LivingEntity::getHealth));
        int n = 0;
        for (LivingEntity e : pool) {
            if (n++ >= 5) break;
            Wave3Util.give(e, 160, 0, "tensura:sleep", "minecraft:slowness");
            Wave3Util.give(e, 160, 6, "minecraft:slowness");
            Wave3Util.give(e, 160, 1, "minecraft:weakness");
            sl.sendParticles(ParticleTypes.NOTE, e.getX(), e.getEyeY() + 0.4, e.getZ(), 3, 0.2, 0.2, 0.2, 0.5);
        }
        Fx.ring(sl, p.position(), 5.0, 0x8E56C8);
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.6f);
    }

    /** Calm Emotions (1 charge): within nine blocks every creature that was hunting drops its target and stays calm for twenty seconds; friends shake off fear and charm. */
    static void calm(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        List<LivingEntity> mobs = Wave3Util.near(sl, p.position(), 9.0, x -> Wave3Util.bends(p, x) && (x instanceof Enemy || ((Mob) x).getTarget() != null));
        List<LivingEntity> afflicted = new ArrayList<>();
        for (LivingEntity f : Wave3Util.friendsAround(p, 9.0)) if (hasMindEffect(f)) afflicted.add(f);
        if (mobs.isEmpty() && afflicted.isEmpty()) {
            Wave3Util.tell(p, "Calm Emotions: nothing here is angry or afraid; no charge was spent.", ChatFormatting.GRAY);
            return;
        }
        if (!spendDream(p, 1)) return;
        int n = 0;
        for (LivingEntity e : mobs) {
            if (n++ >= 12) break;
            Wave3Field.charm((Mob) e, p, 400, true);
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getEyeY(), e.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
        }
        for (LivingEntity f : afflicted) {
            for (MobEffectInstance inst : new ArrayList<>(f.getActiveEffects())) if (isMindEffect(inst.getEffect().value())) f.removeEffect(inst.getEffect());
        }
        Fx.ring(sl, p.position(), 9.0, 0x9DE8F0);
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 0.7f);
    }

    private static boolean hasMindEffect(LivingEntity e) {
        for (MobEffectInstance inst : e.getActiveEffects()) if (isMindEffect(inst.getEffect().value())) return true;
        return false;
    }

    /** Hallucinatory Terrain (2 charges): an illusion wraps you and the friends within five blocks: invisible for forty seconds with Presence Concealment lent, and hunters lose you. */
    static void terrain(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spendDream(p, 2)) return;
        ServerLevel sl = p.serverLevel();
        for (LivingEntity e : Wave3Util.friendsAround(p, 5.0)) {
            Wave3Util.give(e, 800, 0, "minecraft:invisibility");
            if (e instanceof ServerPlayer sp) Fx.lendSkill(sp, "tensura:presence_concealment", 800);   // the illusion hides their presence too (lent if Tensura has it)
        }
        int n = 0;
        for (LivingEntity e : Fx.foesAround(p, 16.0)) {
            if (n++ >= 12) break;
            if (e instanceof Mob m && m.getTarget() != null && Wave3Util.friend(p, m.getTarget())) m.setTarget(null);
        }
        Fx.ring(sl, p.position(), 5.0, 0x8E56C8);
        sl.sendParticles(ParticleTypes.WITCH, p.getX(), p.getY() + 1.0, p.getZ(), 40, 1.2, 0.8, 1.2, 0.05);
        sl.playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.2f, 0.6f);
    }

    /** Master Dreamer: you slip into a short dream on your feet: eight seconds of mending and drowsiness, and the phantoms forget you have not slept. */
    static void rest(ActCtx c) {
        ServerPlayer p = c.player();
        p.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        Wave3Util.give(p, 160, 2, "minecraft:regeneration");
        Wave3Util.give(p, 160, 1, "minecraft:slowness");
        for (MobEffectInstance inst : new ArrayList<>(p.getActiveEffects())) if (isSleepEffect(inst.getEffect().value())) p.removeEffect(inst.getEffect());
        ServerLevel sl = p.serverLevel();
        sl.sendParticles(ParticleTypes.NOTE, p.getX(), p.getEyeY() + 0.6, p.getZ(), 5, 0.3, 0.2, 0.3, 0.5);
        Wave3Util.tell(p, "You dream on your feet; the phantoms lose count of your sleepless nights.", ChatFormatting.LIGHT_PURPLE);
    }

    /** Shared Imagery: friends within sixteen blocks see in the dark for half a minute and are lent Magic Sense (the dreamer's sight), and every foe in that circle is shown to all as a glow. */
    static void imagery(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        for (LivingEntity e : Wave3Util.friendsAround(p, 16.0)) {
            Wave3Util.give(e, 600, 0, "minecraft:night_vision");
            if (e instanceof ServerPlayer sp) Fx.lendSkill(sp, "tensura:magic_sense", 600);          // they see what the dreamer sees: Magic Sense, lent for half a minute
        }
        for (LivingEntity e : Fx.foesAround(p, 16.0)) Wave3Util.give(e, 200, 0, "minecraft:glowing");
        Fx.ring(sl, p.position(), 6.0, 0xC9A0F2);
    }

    // ================================================================ Captain's Tricorn
    /** The charges of the hat now, with the dawn's recovery paid in (2, or 1d6+2 if its wearer is aboard a boat when the day turns). */
    static int tricornHave(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        CompoundTag d = Wave3Util.data(p);
        long today = Wave3Util.today(sl);
        int have = Wave3Rules.normalize(d.contains(K_TRI, Tag.TAG_INT) ? d.getInt(K_TRI) : -1, Wave3Rules.TRICORN_MAX);
        long last = d.contains(K_TRI_DAY, Tag.TAG_LONG) ? d.getLong(K_TRI_DAY) : today;
        if (today > last) {
            int gain = Wave3Rules.tricornDawn(p.getVehicle() instanceof Boat, 1 + sl.getRandom().nextInt(6));
            have = Wave3Rules.dawnRefill(have, Wave3Rules.TRICORN_MAX, last, today, gain);
        }
        d.putInt(K_TRI, have);
        d.putLong(K_TRI_DAY, today);
        return have;
    }

    /** The slow pass keeps the dawn's recovery on time for a wearer who never uses the hat. */
    static void tricornPass(ServerPlayer p) { tricornHave(p); }

    private static boolean spendTricorn(ServerPlayer p, int cost) {
        int have = tricornHave(p);
        if (!Wave3Rules.canSpend(have, cost)) {
            Wave3Util.tell(p, "The tricorn has " + have + " of " + Wave3Rules.TRICORN_MAX + " charges (" + cost + " needed): two come back at every dawn.", ChatFormatting.GRAY);
            return false;
        }
        int left = Wave3Rules.spend(have, cost);
        Wave3Util.data(p).putInt(K_TRI, left);
        Wave3Util.tell(p, "Tricorn charges: " + left + " of " + Wave3Rules.TRICORN_MAX, ChatFormatting.AQUA);
        return true;
    }

    /** Command (1 charge): "Halt!" at the creature you look at (within 20 blocks, not a player, not a boss): it stands frozen for four seconds and forgets its quarrel. */
    static void command(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        Wave3Util.Aim a = Wave3Util.aim(p, 20.0);
        if (a.hit() == null || !Wave3Util.bends(p, a.hit())) {
            Wave3Util.tell(p, "Command: look at a creature within 20 blocks that your existence can bend (not a player, not a boss); no charge was spent.", ChatFormatting.GRAY);
            return;
        }
        if (!spendTricorn(p, 1)) return;
        LivingEntity t = a.hit();
        Wave3Util.give(t, 80, 6, "minecraft:slowness");
        Wave3Util.give(t, 80, 1, "minecraft:weakness");
        Wave3Field.charm((Mob) t, p, 80, true);
        Wave3Util.trail(sl, p.getEyePosition(), t.getBoundingBox().getCenter(), ParticleTypes.CRIT);
        sl.sendParticles(ParticleTypes.CRIT, t.getX(), t.getEyeY() + 0.3, t.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
        sl.playSound(null, p.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0f, 1.3f);
    }

    /**
     * Captain's Order (2 charges per target, at most four targets): your pets, summons and shipmates within 24 blocks take up the order (strength,
     * speed and resilience for three minutes); then hostile creatures within twelve blocks are talked out of the fight for half a minute.
     */
    static void order(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        int cap = Wave3Rules.orderTargets(tricornHave(p), 4);
        if (cap <= 0) {
            Wave3Util.tell(p, "Captain's Order needs 2 charges per target; the tricorn has " + tricornHave(p) + ".", ChatFormatting.GRAY);
            return;
        }
        List<LivingEntity> targets = new ArrayList<>();
        for (LivingEntity e : Wave3Util.near(sl, p.position(), 24.0, x -> x != p && !(x instanceof ArmorStand) && Wave3Util.friend(p, x))) {
            if (targets.size() < cap) targets.add(e);
        }
        for (LivingEntity e : Wave3Util.near(sl, p.position(), 12.0, x -> x instanceof Enemy && Wave3Util.bends(p, x))) {
            if (targets.size() < cap) targets.add(e);
        }
        if (targets.isEmpty()) {
            Wave3Util.tell(p, "Captain's Order: no crew or foes in reach; no charge was spent.", ChatFormatting.GRAY);
            return;
        }
        if (!spendTricorn(p, 2 * targets.size())) return;
        for (LivingEntity e : targets) {
            if (Wave3Util.friend(p, e)) {
                Wave3Util.give(e, 3600, 0, "minecraft:strength");
                Wave3Util.give(e, 3600, 0, "minecraft:speed");
                Wave3Util.give(e, 3600, 0, "minecraft:resistance");
            } else {
                Wave3Field.charm((Mob) e, p, 600, false);
                Wave3Util.give(e, 100, 0, "minecraft:slowness");
            }
            sl.sendParticles(ParticleTypes.NOTE, e.getX(), e.getEyeY() + 0.4, e.getZ(), 3, 0.2, 0.2, 0.2, 1.0);
        }
        sl.playSound(null, p.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.2f, 0.9f);
        Fx.ring(sl, p.position(), 3.0, 0xC8D0E0);
    }

    // ================================================================ Godking Crown
    private record Mod(String attr, String name, double amount, Operation op) {}

    /** "+5 to all rolls": the ten minutes lift attack, armour, toughness, luck, steadiness and pace. */
    private static final List<Mod> ASCENT = List.of(
            new Mod("minecraft:generic.attack_damage", "gk_attack", 4.0, Operation.ADD_VALUE),
            new Mod("minecraft:generic.armor", "gk_armor", 5.0, Operation.ADD_VALUE),
            new Mod("minecraft:generic.armor_toughness", "gk_tough", 2.0, Operation.ADD_VALUE),
            new Mod("minecraft:generic.luck", "gk_luck", 5.0, Operation.ADD_VALUE),
            new Mod("minecraft:generic.knockback_resistance", "gk_kb", 0.5, Operation.ADD_VALUE),
            new Mod("minecraft:generic.movement_speed", "gk_speed", 0.15, Operation.ADD_MULTIPLIED_BASE));

    /** The skills the ascension lends (short leases, renewed each second while the crown burns: they are gone seconds after it ends). All verified in this repo. */
    private static final String[] ASCENT_SKILLS = {"tensura:danger_sense", "tensura:physical_attack_resistance", "tensura:sage"};

    /** Is the ascension running for this player (read-only)? */
    static boolean ascended(ServerPlayer p) {
        CompoundTag d = Wave3Util.peek(p);
        return d != null && d.getLong(K_GK) > 0;
    }

    /** Ascend: put the crown's power on. Ten minutes of flight, +5 on everything and three Tensura skills merged into it (Danger Sense, Physical Attack Resistance, Sage); then a radiant reckoning on you and everything within six blocks. */
    static void ascend(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        CompoundTag d = Wave3Util.data(p);
        long now = sl.getGameTime();
        if (d.getLong(K_GK) > now) {
            Wave3Util.tell(p, "The crown already burns.", ChatFormatting.GRAY);
            return;
        }
        for (Mod m : ASCENT) Wave3Util.setMod(p, m.attr(), m.name(), m.amount(), m.op());
        flightOn(p, d);
        Fx.lendSkills(p, 100, ASCENT_SKILLS);
        d.putLong(K_GK, now + Godking.ASCEND_TICKS);
        d.putInt(K_GK_WARN, 0);
        Fx.ring(sl, p.position(), 3.0, MINT);
        Fx.burst(sl, p.position(), MINT);
        sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5f, 0.8f);
        Wave3Util.say(p, "Godking Crown: you rise. Ten minutes of flight and a strength above your own; then the crown asks its price (a radiant blast on you and all within six blocks). Take it off, or dismiss it, to end the ascension in peace.");
    }

    /** Dismiss the ascension early: no flight, no price. */
    static void dismiss(ActCtx c) {
        ServerPlayer p = c.player();
        if (!ascended(p)) {
            Wave3Util.tell(p, "The crown is not burning.", ChatFormatting.GRAY);
            return;
        }
        end(p, false);
    }

    private static void flightOn(ServerPlayer p, CompoundTag d) {
        Wave3Util.setMod(p, NeoForgeMod.CREATIVE_FLIGHT, "gk_fly", 1.0, Operation.ADD_VALUE);
        Abilities ab = p.getAbilities();
        if (!ab.mayfly) {
            ab.mayfly = true;
            d.putBoolean(K_GK_FLY, true);
            p.onUpdateAbilities();
        }
    }

    private static void flightOff(ServerPlayer p, CompoundTag d) {
        Wave3Util.dropMod(p, NeoForgeMod.CREATIVE_FLIGHT, "gk_fly");
        if (!d.getBoolean(K_GK_FLY)) return;
        d.remove(K_GK_FLY);
        if (p.isCreative() || p.isSpectator() || p.getAttributeValue(NeoForgeMod.CREATIVE_FLIGHT) > 0) return;
        Abilities ab = p.getAbilities();
        ab.mayfly = false;
        ab.flying = false;
        p.onUpdateAbilities();
    }

    /** Ends the ascension. {@code natural}: the ten minutes ran out with the crown on, so the price falls due. */
    static void end(ServerPlayer p, boolean natural) {
        CompoundTag d = Wave3Util.data(p);
        d.putLong(K_GK, 0);
        d.putInt(K_GK_WARN, 0);
        for (Mod m : ASCENT) Wave3Util.dropMod(p, m.attr(), m.name());
        Wave3Util.give(p, Godking.FALL_GRACE, 0, "minecraft:slow_falling");                // whoever is still high up comes down gently
        p.fallDistance = 0;
        flightOff(p, d);
        if (natural) judgement(p);
        else Wave3Util.tell(p, "The crown's fire gutters out; nothing falls.", ChatFormatting.GRAY);
    }

    /** The reckoning: radiant damage on the wearer (never lethal) and on every foe within six blocks, who are also lit up for ten seconds. */
    private static void judgement(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        Vec3 at = p.position();
        Fx.ring(sl, at, Godking.BURST_RADIUS, MINT);
        Fx.burst(sl, at, MINT);
        sl.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1.0, at.z, 60, Godking.BURST_RADIUS * 0.5, 0.8, Godking.BURST_RADIUS * 0.5, 0.15);
        sl.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0f, 1.4f);
        for (LivingEntity e : Wave3Util.near(sl, at, Godking.BURST_RADIUS, x -> Wave3Util.target(p, x))) {
            Fx.damageHeavy(p, e, Godking.BURST_OTHERS, "minecraft:magic");
            Wave3Util.give(e, 200, 0, "minecraft:glowing");
        }
        float self = Godking.selfBurst(p.getHealth());
        if (self > 0f) p.hurt(p.damageSources().magic(), self);
        Wave3Util.give(p, 200, 0, "minecraft:weakness");
        Wave3Util.say(p, "Godking Crown: the ten minutes are over; the crown takes its price.");
    }

    /** Once a second for every player: runs the ascension's clock, its warnings, and its end (taking the crown off ends it without the price). */
    static void ascensionPass(ServerPlayer p) {
        CompoundTag d = Wave3Util.peek(p);
        if (d == null) return;
        long until = d.getLong(K_GK);
        if (until <= 0) return;
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        boolean worn = RelicFlags.has(p, Wave3Rules.F_GODKING);
        Holder<Attribute> probe = Attributes.ATTACK_DAMAGE;
        boolean powered = Wave3Util.hasMod(p, probe, "gk_attack");
        if (!worn || !powered) {                                                         // the crown came off, or a relog took the power: no price
            end(p, false);
            return;
        }
        if (now >= until) {
            end(p, true);
            return;
        }
        int remaining = (int) (until - now);
        int w = Godking.newWarning(d.getInt(K_GK_WARN), remaining);
        if (w > 0) {
            d.putInt(K_GK_WARN, w);
            Wave3Util.say(p, "Godking Crown: " + Godking.warnText(w) + " until the reckoning. Land and take the crown off, or dismiss it, to be spared.");
            Fx.ring(sl, p.position(), Godking.BURST_RADIUS, MINT);
        } else if (remaining <= 100) {
            Fx.ring(sl, p.position(), Godking.BURST_RADIUS, MINT);                       // the last five seconds: the circle shows who will be hit
        }
        Fx.lendSkills(p, 100, ASCENT_SKILLS);
        Abilities ab = p.getAbilities();
        if (!ab.mayfly && !p.isSpectator()) flightOn(p, d);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getEyeY() + 0.55, p.getZ(), 4, 0.25, 0.1, 0.25, 0.01);
    }

    /** A death or a logout ends the ascension at once, quietly. */
    static void forget(ServerPlayer p) {
        CompoundTag d = Wave3Util.peek(p);
        if (d == null || d.getLong(K_GK) <= 0) return;
        d.putLong(K_GK, 0);
        d.putInt(K_GK_WARN, 0);
        for (Mod m : ASCENT) Wave3Util.dropMod(p, m.attr(), m.name());
        flightOff(p, d);
    }
}
