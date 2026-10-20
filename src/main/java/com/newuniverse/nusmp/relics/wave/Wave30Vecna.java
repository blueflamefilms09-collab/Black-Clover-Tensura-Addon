package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: the pair is one design (the risk of the Eye, the coercion of the Hand, the set that lifts both), so its code sits together; nothing here ever
 * kills a player, steals control of a character or locks anyone out: the "devouring" and the "coercion" of the tabletop are timed, loud and answerable.
 * <p>
 * Wave 30: the Eye and the Hand of Vecna. The Eye: Eye of Truth, night sight and X-ray (the glow of mobs behind walls), five acts (Clairvoyance, Crown
 * of Madness, Disintegrate, Dominate Monster, Eyebite), each with a 5% chance of Vecna's Hunger. The Hand: strength, cold blows, three acts (Touch of
 * Death, Rimed Grasp, Skeleton to Jelly) and the Wish, each Hand act with a chance of Vecna's Whisper. Worn together they are a set: see {@link #set}.
 */
final class Wave30Vecna {
    private Wave30Vecna() {}

    /** Do the Eye and the Hand both work for this player (worn and attuned)? */
    static boolean set(ServerPlayer p) { return Wave30Rules.vecnaSet(RelicFlags.has(p, Wave30Tune.F_EYE), RelicFlags.has(p, Wave30Tune.F_HAND)); }

    // ================================================================ the Eye
    /** Clairvoyance: the five nearest living things within 96 blocks, named with their coordinates, glowing for ten seconds. */
    static void clairvoyance(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        double r = Wave30Tune.CLAIR_RADIUS;
        AABB box = p.getBoundingBox().inflate(r, Math.min(r, 128), r);
        List<LivingEntity> all = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive() && !e.isSpectator()
                && !(e instanceof net.minecraft.world.entity.decoration.ArmorStand) && !(e instanceof Player o && (o.isCreative() || o.isSpectator()))
                && e.distanceToSqr(p) <= r * r);
        all.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        if (all.isEmpty()) {
            Wave30Kit.say(p, "The Eye looks far and wide and sees nothing living within " + (int) r + " blocks.");
        } else {
            Wave30Kit.say(p, "The Eye sees:");
            int n = 0;
            for (LivingEntity e : all) {
                if (n++ >= Wave30Tune.CLAIR_COUNT) break;
                Wave30Kit.say(p, "  " + e.getName().getString() + " at " + e.getBlockX() + ", " + e.getBlockY() + ", " + e.getBlockZ() + " (" + (int) Math.sqrt(e.distanceToSqr(p)) + " blocks)");
                Fx.addEffect(e, "minecraft:glowing", Wave30Tune.CLAIR_GLOW, 0);
            }
        }
        com.newuniverse.nusmp.particle.fx.Fx.gather(level, FxThemes.RELIC_WAVE_30, p, FxTone.DARK, 12);
        afterEyeAct(p);
    }

    /** Crown of Madness: the mob in front fights its own allies for ten seconds. */
    static void crown(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = Wave30Kit.target(p, Wave30Tune.CROWN_REACH);
        if (t == null || !Wave30Kit.controllable(p, t)) {
            Wave30Kit.say(p, "The Crown needs a mob (not a boss) in your sight.");
            return;
        }
        Wave30Events.madden((Mob) t, t.level().getGameTime() + Wave30Tune.CROWN_TICKS);
        Fx.addEffect(t, "minecraft:glowing", Wave30Tune.CROWN_TICKS, 0);
        com.newuniverse.nusmp.particle.fx.Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_30, t, FxTone.DARK, 12);
        Wave30Kit.say(p, "Madness crowns " + t.getName().getString() + ": it turns on its own for ten seconds.");
        afterEyeAct(p);
    }

    /** Disintegrate: a hitscan beam. Heavy damage, no block removed; a player is capped and left on a heart at least. */
    static void disintegrate(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        LivingEntity t = Wave30Kit.target(p, Wave30Tune.DISINTEGRATE_REACH);
        Vec3 from = p.getEyePosition().add(0, -0.2, 0);
        if (t == null) {
            Vec3 end = from.add(p.getLookAngle().scale(Wave30Tune.DISINTEGRATE_REACH));
            for (int i = 0; i < 3; i++) com.newuniverse.nusmp.particle.fx.Fx.stream(level, FxThemes.RELIC_WAVE_30, from, end, FxTone.DARK, 6);
            Wave30Kit.say(p, "The beam finds nothing to unmake.");
            afterEyeAct(p);
            return;
        }
        Vec3 to = t.position().add(0, t.getBbHeight() * 0.5, 0);
        for (int i = 0; i < 3; i++) com.newuniverse.nusmp.particle.fx.Fx.stream(level, FxThemes.RELIC_WAVE_30, from, to, FxTone.DARK, 6);
        com.newuniverse.nusmp.particle.fx.Fx.burst(level, FxThemes.RELIC_WAVE_30, to, FxTone.DARK, 12, 0.6);
        Wave30Kit.hit(p, t, Wave30Tune.DISINTEGRATE_DAMAGE, "minecraft:magic", true);
        Wave30Kit.say(p, "A grey beam unmakes the air: " + t.getName().getString() + " is struck.");
        afterEyeAct(p);
    }

    /** Dominate Monster: a mob (not a boss) is your ally for a minute. */
    static void dominate(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = Wave30Kit.target(p, Wave30Tune.DOMINATE_REACH);
        if (t == null || !Wave30Kit.controllable(p, t)) {
            Wave30Kit.say(p, "Dominate needs a mob (not a boss) in your sight.");
            return;
        }
        Wave30Events.dominate((Mob) t, p, t.level().getGameTime() + Wave30Tune.DOMINATE_TICKS);
        com.newuniverse.nusmp.particle.fx.Fx.gather(p.serverLevel(), FxThemes.RELIC_WAVE_30, t, FxTone.DARK, 12);
        Wave30Kit.say(p, t.getName().getString() + " bends to your will for a minute.");
        afterEyeAct(p);
    }

    /** Eyebite: a cone of fear, weakness and slowness; players are afflicted for a few seconds only. */
    static void eyebite(ActCtx c) {
        ServerPlayer p = c.player();
        List<LivingEntity> hit = Wave30Kit.cone(p, Wave30Tune.EYEBITE_RANGE, Wave30Tune.EYEBITE_COS);
        int n = 0;
        for (LivingEntity e : hit) {
            boolean player = e instanceof Player;
            int ticks = player ? Wave30Tune.EYEBITE_PLAYER_TICKS : Wave30Tune.EYEBITE_TICKS;
            if (Wave30Kit.isBoss(e)) ticks = Math.min(ticks, 40);
            Fx.addEffect(e, Wave30Tune.EF_FEAR, ticks, 0);
            Fx.addEffect(e, "minecraft:weakness", ticks, 1);
            Fx.addEffect(e, "minecraft:slowness", ticks, 1);
            n++;
        }
        Vec3 look = p.getLookAngle();
        Vec3 at = p.getEyePosition().add(look.scale(3.0));
        com.newuniverse.nusmp.particle.fx.Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_30, at, FxTone.DARK, 12, 1.2);
        Wave30Kit.say(p, n == 0 ? "Your gaze falls on empty air." : "Your gaze bites " + n + (n == 1 ? " foe: fear, weakness and slowness." : " foes: fear, weakness and slowness."));
        afterEyeAct(p);
    }

    /** Every use of the Eye has a 5% chance of Vecna's Hunger. */
    static void afterEyeAct(ServerPlayer p) {
        if (Wave30Rules.hungerStrikes(p.getRandom().nextDouble())) hunger(p);
    }

    /**
     * Vecna's Hunger: for twenty seconds the bearer is in darkness, slowed, and hunted by a spectral hand (a summon hostile to the bearer only, killable).
     * Loud, never lethal (the hand cannot take the last heart), and it takes nothing: no control of the character is ever lost.
     */
    static void hunger(ServerPlayer p) {
        long now = p.level().getGameTime();
        if (Wave30Data.getLong(p, Wave30Data.HUNGER_UNTIL) > now) return;                 // an episode is under way: it is never stacked
        Wave30Data.putLong(p, Wave30Data.HUNGER_UNTIL, now + Wave30Tune.HUNGER_TICKS);
        Fx.addEffect(p, "minecraft:darkness", Wave30Tune.HUNGER_TICKS, 0);
        Fx.addEffect(p, "minecraft:slowness", Wave30Tune.HUNGER_TICKS, 1);
        Mob hand = Wave30Kit.foe(p, "minecraft:vex", Wave30Tune.HUNGER_TICKS, "Vecna's Hand", Wave30Tune.HUNGER_HAND_HEALTH, Wave30Tune.HUNGER_HAND_DAMAGE);
        if (hand == null) Fx.addEffect(p, "minecraft:weakness", Wave30Tune.HUNGER_TICKS, 0);
        com.newuniverse.nusmp.particle.fx.Fx.gather(p.serverLevel(), FxThemes.RELIC_WAVE_30, p, FxTone.DARK, 12);
        Wave30Kit.shout(p, "VECNA'S HUNGER: the Eye turns inward and devours at your will. For twenty seconds a withered hand comes for you: strike it down.", ChatFormatting.DARK_RED);
    }

    // ================================================================ the Hand
    /** Touch of Death: the Finger of Death's touch. Wither and heavy damage; capped on players and never lethal to them. */
    static void touch(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = Wave30Kit.target(p, Wave30Tune.TOUCH_REACH);
        if (t == null) {
            Wave30Kit.say(p, "The Hand finds nothing to touch.");
            afterHandAct(p);
            return;
        }
        Wave30Kit.hit(p, t, Wave30Tune.TOUCH_DAMAGE, "minecraft:wither", true);
        Fx.addEffect(t, "minecraft:wither", t instanceof Player ? 100 : 200, 1);
        com.newuniverse.nusmp.particle.fx.Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_30, t, FxTone.DARK, 12);
        Wave30Kit.say(p, "Your cold fingers close on " + t.getName().getString() + ": the life drains from it.");
        afterHandAct(p);
    }

    /** Rimed Grasp: frost on every foe within six blocks. */
    static void grasp(ActCtx c) {
        ServerPlayer p = c.player();
        int n = 0;
        for (LivingEntity t : Fx.foesAround(p, Wave30Tune.GRASP_RADIUS)) {
            Wave30Kit.hit(p, t, Wave30Tune.GRASP_DAMAGE, "minecraft:freeze", false);
            int ticks = t instanceof Player ? 80 : Wave30Tune.GRASP_TICKS;
            Fx.addEffect(t, "minecraft:slowness", ticks, 2);
            Fx.addEffect(t, Wave30Tune.EF_FROST, ticks, 0);
            t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() + 80, t.getTicksFrozen() + 120));
            n++;
        }
        FxRelic.ring(p.serverLevel(), p.position(), Wave30Tune.GRASP_RADIUS, 0xBFE8FF);
        Wave30Kit.say(p, n == 0 ? "Rime spreads from your palm, but nothing stands near." : "Rime crawls from your palm over " + n + (n == 1 ? " foe." : " foes."));
        afterHandAct(p);
    }

    /** Skeleton to Jelly (the pair's touch): 30% of the foe's maximum health, 10% on a boss; a player is capped and left on a heart at least. */
    static void jelly(ActCtx c) {
        ServerPlayer p = c.player();
        if (!set(p)) {
            Wave30Kit.say(p, "Without the Eye the Hand cannot unmake a skeleton: wear both.");
            return;
        }
        LivingEntity t = Wave30Kit.target(p, Wave30Tune.TOUCH_REACH);
        if (t == null || (t instanceof Player o && !Wave30Kit.harmable(p, o))) {
            Wave30Kit.say(p, "Nothing within touch.");
            return;
        }
        float amount = Wave30Rules.jelly(t.getMaxHealth(), t.getHealth(), t instanceof Player, Wave30Kit.isBoss(t));
        if (t instanceof Player o) {
            if (amount > 0f) o.hurt(p.damageSources().indirectMagic(p, p), amount);
        } else {
            Wave30Kit.hurtShare(p, t, amount);
        }
        int ticks = t instanceof Player ? 100 : Wave30Tune.JELLY_TICKS;
        Fx.addEffect(t, "minecraft:slowness", ticks, 2);
        Fx.addEffect(t, "minecraft:weakness", ticks, 2);
        Fx.addEffect(t, "minecraft:glowing", ticks, 0);
        com.newuniverse.nusmp.particle.fx.Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_30, t, FxTone.DARK, 12);
        Wave30Kit.say(p, t.getName().getString() + "'s bones go soft as jelly: it sags, slowed and weakened.");
        afterHandAct(p);
    }

    /** Coercion: each Hand act may be followed by Vecna's Whisper. */
    static void afterHandAct(ServerPlayer p) {
        if (Wave30Rules.whispers(p.getRandom().nextDouble())) whisper(p);
    }

    /** Vecna's Whisper: a few seconds of Hunger and Mining Fatigue and a line in chat, the Hand's coercion towards an evil act. */
    static void whisper(ServerPlayer p) {
        Fx.addEffect(p, "minecraft:mining_fatigue", Wave30Tune.WHISPER_TICKS, 0);                // 1.99.0, the hunger ban: the Hunger effect it also gave is gone
        String[] lines = {"\"Take it. They would never know.\"", "\"Why spare them? Mercy is for the weak.\"", "\"The hand wants what the hand wants.\"",
                "\"Your friends are only waiting to be used.\""};
        Wave30Kit.shout(p, "Vecna whispers: " + lines[p.getRandom().nextInt(lines.length)], ChatFormatting.DARK_PURPLE);
    }

    // ================================================================ the Wish (the pair)
    /** Cycles the wish the Hand will grant (no cost, no cooldown). */
    static void wishPick(ActCtx c) {
        ServerPlayer p = c.player();
        if (!set(p)) {
            Wave30Kit.say(p, "The Wish is for the one who wears both the Hand and the Eye.");
            return;
        }
        int next = Wave30Rules.nextIndex(Wave30Data.getInt(p, Wave30Data.WISH_PICK), Wave30Rules.VecnaWish.values().length);
        Wave30Data.putInt(p, Wave30Data.WISH_PICK, next);
        Wave30Rules.VecnaWish w = Wave30Rules.VecnaWish.values()[next];
        long last = Wave30Data.getLong(p, Wave30Data.WISH_LAST);
        int days = Wave30Rules.wishDaysLeft(last, System.currentTimeMillis());
        Wave30Kit.say(p, "Wish chosen: " + w.title + " (" + w.text + "). " + (days == 0 ? "The Wish is ready: use Make the Wish." : "The Wish sleeps for " + days + " more days."));
    }

    /** The Wish: once in 30 real days, the chosen safe effect. */
    static void wish(ActCtx c) {
        ServerPlayer p = c.player();
        if (!set(p)) {
            Wave30Kit.say(p, "The Wish is for the one who wears both the Hand and the Eye.");
            return;
        }
        long now = System.currentTimeMillis();
        long last = Wave30Data.getLong(p, Wave30Data.WISH_LAST);
        if (!Wave30Rules.wishReady(last, now)) {
            Wave30Kit.say(p, "The Wish sleeps for " + Wave30Rules.wishDaysLeft(last, now) + " more days.");
            return;
        }
        Wave30Rules.VecnaWish[] all = Wave30Rules.VecnaWish.values();
        Wave30Rules.VecnaWish w = all[Math.floorMod(Wave30Data.getInt(p, Wave30Data.WISH_PICK), all.length)];
        ServerLevel level = p.serverLevel();
        switch (w) {
            case FULL_RESTORE -> {
                Wave30Kit.cleanse(p);
                p.setHealth(p.getMaxHealth());
                p.getFoodData().setFoodLevel(20);
                p.getFoodData().setSaturation(20f);
            }
            case DAY_OF_RESILIENCE -> {
                Fx.addEffect(p, "minecraft:regeneration", Wave30Tune.WISH_RESILIENCE_TICKS, 1);
                Fx.addEffect(p, "minecraft:resistance", Wave30Tune.WISH_RESILIENCE_TICKS, 1);
            }
            case CLEAR_SKIES -> level.getServer().overworld().setWeatherParameters(24000, 0, false, false);
            case SUMMON_RAIN -> level.getServer().overworld().setWeatherParameters(0, 6000, true, false);
        }
        Wave30Data.putLong(p, Wave30Data.WISH_LAST, now);
        FxRelic.burst(level, p.position(), 0xFFD27A);
        Wave30Kit.shout(p, "THE WISH IS GRANTED: " + w.title + ". It will not come again for thirty days.", ChatFormatting.GOLD);
    }

    // ================================================================ the Eye's X-ray and the pair's gifts (once a second from the player tick)
    /** X-ray: mobs and players within 24 blocks that are out of sight glow through the walls. Costs a little hunger unless the pair is worn. */
    static void xray(ServerPlayer p, boolean pair) {
        ServerLevel level = p.serverLevel();
        double r = Wave30Tune.XRAY_RADIUS;
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), e -> e != p && e.isAlive() && !e.isSpectator()
                && !(e instanceof net.minecraft.world.entity.decoration.ArmorStand) && !(e instanceof Player o && (o.isCreative() || o.isSpectator()))
                && e.distanceToSqr(p) <= r * r);
        near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        List<LivingEntity> hidden = new ArrayList<>();
        for (LivingEntity e : near) {
            if (hidden.size() >= 24) break;
            if (!p.hasLineOfSight(e)) hidden.add(e);
        }
        for (LivingEntity e : hidden) Fx.addEffect(e, "minecraft:glowing", Wave30Tune.XRAY_GLOW, 0);
        if (!hidden.isEmpty() && !pair && !p.isCreative()) Wave2Mana.take(p, Wave30Tune.XRAY_MANA);   // 1.99.0, the hunger ban: the unpaired X-ray costs a sliver of magicules, not exhaustion
    }

    /** The pair's gifts, once a second: poison and disease cleansed, the danger-sense and resistance skills lent, and a health every two seconds. */
    static void pairTick(ServerPlayer p) {
        for (String id : new String[]{"minecraft:poison", "minecraft:hunger", "tensura:magicule_poison", "tensura:fatal_poison", "tensura:infection", "tensura:paralysis"}) {
            Wave30Kit.lift(p, id);
        }
        if ((p.tickCount / 20) % 2 == 0) Fx.lendSkills(p, 100, Wave30Tune.SK_POISON_RES, Wave30Tune.SK_PARALYSIS_RES, Wave30Tune.SK_DANGER_SENSE);
        if (p.getHealth() < p.getMaxHealth() && (p.tickCount / 20) % (Wave30Tune.REGEN_EVERY / 20) == 0) p.heal(Wave30Tune.REGEN_AMOUNT);
    }
}
