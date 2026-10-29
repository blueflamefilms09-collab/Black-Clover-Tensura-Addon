package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.relics.ActBody;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the active abilities of wave 101 that are not one of the shapes of {@link Wave71Acts}: the Puzzle's shadow game, the Eye's mind reading, the Ring's dowsing and drawing of power, the
 * Rod's control of a mind (through the domination of {@link Wave30Events}, so a controlled mob is the same kind of ally everywhere), the Key's soul room, the Scale's weighing of a heart, the
 * Necklace's foresight and the Dragon Balls' wish. Every body runs inside {@link SafeCast#tick} (a fault is logged once and never reaches the server tick), takes its foes only from {@link Fx#foesAround} and
 * {@link Wave20Util} (hostile, never allies, pets, summons, creative or spectator players), deals every blow through {@link Fx} (the balance law and the cap on players), clamps every radius, count
 * and duration, grows its damage and healing by {@link Wave101Set#power} (the set bonus), and draws its effect with the mod's own {@link VfxShape}s and Rukh particles. Nothing here keeps state
 * except the marks of {@link Wave101Marks}.
 */
final class Wave101Acts {
    private Wave101Acts() {}

    /** True while an act of this wave runs: the blows it deals are the act's own, so a weapon's passive and the marks do not stack on them. Server thread only. */
    private static boolean running;

    static boolean running() { return running; }

    static final int MAX_HITS = 12;
    static final String[] MIND_EFFECTS = {"tensura:fear", "tensura:confusion", "tensura:insanity", "tensura:hypnosis", "tensura:sleep", "tensura:drowsiness", "minecraft:nausea", "minecraft:darkness"};

    /** Runs the body under the shared guard, once per use. */
    static ActBody guard(String key, ActBody body) {
        return ctx -> {
            if (ctx.player() == null || !ctx.player().isAlive()) return;
            boolean before = running;
            running = true;
            try {
                SafeCast.tick("relic.w101." + key, () -> body.run(ctx));
            } finally {
                running = before;
            }
        };
    }

    /** Two bodies, one after the other. */
    static ActBody both(ActBody a, ActBody b) {
        return ctx -> {
            a.run(ctx);
            b.run(ctx);
        };
    }

    // ================================================================ helpers
    private static void fx(ServerLevel l, VfxShape s, Vec3 from, Vec3 to, int rgb, int ticks, float power) {
        if (l == null || s == null) return;
        VfxSpawn.send(l, s, from, to, 0xFF000000 | (rgb & 0xFFFFFF), Mth.clamp(ticks, 4, 60), power);
    }

    private static Vec3 chest(ServerPlayer p) { return p.position().add(0, p.getBbHeight() * 0.7, 0); }

    private static Vec3 body(LivingEntity e) { return e.position().add(0, e.getBbHeight() * 0.55, 0); }

    private static int glow(ActCtx c) { return c.def().paletteGlow(); }

    private static float scaled(ActCtx c, float amount) { return (float) (amount * Wave101Set.power(c.player())); }

    private static long now(ServerPlayer p) { return p.level().getGameTime(); }

    /** Whom the Scale weighs. */
    static boolean sinful(LivingEntity t) {
        if (t == null) return false;
        String path = Wave20Util.path(t);
        return Wave101Rules.sinful(t instanceof Player, t instanceof Enemy, t.getType().is(EntityTypeTags.UNDEAD), Wave71Rules.isFiend(path), Wave30Kit.isBoss(t));
    }

    /** Takes the good effects off a creature (a foe's buffs). */
    private static void dispel(LivingEntity t) {
        for (MobEffectInstance i : new ArrayList<>(t.getActiveEffects())) if (i.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) t.removeEffect(i.getEffect());
    }

    /** Takes the effects of the mind (fear, confusion, sleep, nausea, darkness) off a creature. */
    static void clearMind(LivingEntity e) {
        for (String id : MIND_EFFECTS) Wave20Util.removeEffect(e, id);
    }

    // ================================================================ the Puzzle
    /** Shadow Game: every foe round is drawn into a game of shadows: it is hurt, slowed, weakened and marked, so the wielder deals more to it for a while, and the wielder stands guarded. */
    static ActBody shadowGame(double radius, float dmg, int secs) {
        return guard("shadow_game", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            fx(l, VfxShape.SHADOW_POOL, p.position(), p.position().add(0, 1, 0), glow(c), 40, (float) radius);
            FxRelic.ring(l, p.position(), Math.min(radius, 12.0), glow(c));
            long until = now(p) + secs * 20L;
            int n = 0;
            for (LivingEntity t : Fx.foesAround(p, radius)) {
                if (n++ >= MAX_HITS) break;
                Fx.damage(p, t, scaled(c, dmg), "minecraft:magic");
                Fx.addEffect(t, "minecraft:slowness", secs * 20, 3);
                Fx.addEffect(t, "minecraft:weakness", secs * 20, 1);
                Fx.addEffect(t, "minecraft:glowing", secs * 20, 0);
                Wave101Marks.put(t, p.getUUID(), Wave101Rules.Mark.PENALTY, until);
            }
            Fx.addEffect(p, "minecraft:resistance", secs * 20, 0);
            Fx.shield(p, 40f, secs * 20);
            if (n == 0) Fx.say(p, "The shadows find no one to play with.");
        });
    }

    // ================================================================ the Eye
    /** Mind Read: every foe round has its intentions read: it glows through walls and is marked, so the wielder deals more to it; the wielder sees ahead for a moment. */
    static ActBody readMinds(double radius, int secs) {
        return guard("mind_read", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            FxRelic.ring(l, p.position(), Math.min(radius, 12.0), glow(c));
            long until = now(p) + secs * 20L;
            int n = 0;
            for (LivingEntity t : Fx.foesAround(p, radius)) {
                if (n++ >= 24) break;
                Fx.addEffect(t, "minecraft:glowing", secs * 20, 0);
                Wave101Marks.put(t, p.getUUID(), Wave101Rules.Mark.READ, until);
                fx(l, VfxShape.MIRROR_PANE, body(t), p.getEyePosition(), glow(c), 12, 0.8f);
            }
            Fx.lendSkill(p, "tensura:future_vision", secs * 20);
            if (n == 0) Fx.say(p, "No mind near enough to read.");
        });
    }

    /** The Eye's beam: a line of light from the Eye of Wdjat; every foe on it is hurt, weakened and marked as read. */
    static ActBody eyeBeam(double range, double width, float dmg, int secs) {
        return guard("truth_gaze", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            double len = Wave20Util.lineLength(p, Mth.clamp(range, 2, 40));
            Vec3 from = p.getEyePosition().add(0, -0.2, 0), to = from.add(p.getLookAngle().scale(len));
            fx(l, VfxShape.MIRROR_RAY, from, to, glow(c), 14, (float) Math.max(0.8, width));
            long until = now(p) + secs * 20L;
            for (LivingEntity t : Wave20Util.foesOnLine(p, range, width, MAX_HITS)) {
                Fx.damage(p, t, scaled(c, dmg), "minecraft:magic");
                Fx.addEffect(t, "minecraft:weakness", secs * 20, 2);
                Wave101Marks.put(t, p.getUUID(), Wave101Rules.Mark.READ, until);
            }
        });
    }

    // ================================================================ the Ring
    /** Dowsing: the five needles swing to every living thing near: the hostile ones glow through walls, and the wielder senses magic for a while. */
    static ActBody dowse(double radius, int secs) {
        return guard("dowse", c -> {
            ServerPlayer p = c.player();
            FxRelic.ring(p.serverLevel(), p.position(), 12.0, glow(c));
            Wave31Acts.mark(c, radius, secs);
            Fx.lendSkill(p, "tensura:magic_sense", (secs + 20) * 20);
        });
    }

    /** Draw Power: the Ring pulls power out of every foe round: they are hurt and the wielder takes back magicules and some life. */
    static ActBody drawPower(double radius, float dmg) {
        return guard("draw_power", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            fx(l, VfxShape.ASH_FORMATION, p.position(), p.position().add(0, 1, 0), glow(c), 18, (float) radius);
            int n = 0;
            for (LivingEntity t : Fx.foesAround(p, radius)) {
                if (n >= MAX_HITS) break;
                Fx.damage(p, t, scaled(c, dmg), "minecraft:magic");
                fx(l, VfxShape.THREAD_LINE, body(t), chest(p), glow(c), 8, 1.0f);
                n++;
            }
            if (n > 0) {
                Wave2Mana.give(p, Wave101Rules.draw(n) * Wave101Set.power(p));
                Fx.heal(p, Math.min(60f, scaled(c, 12f) * n));
            } else {
                Fx.say(p, "There is no power near to draw.");
            }
        });
    }

    // ================================================================ the Rod
    /** Mind Control: the mob in sight (not a boss, not a player) is yours for a few seconds: it turns on its own kind. A will too strong to bend only falters. */
    static ActBody mindControl(double reach, int ticks) {
        return guard("mind_control", c -> {
            ServerPlayer p = c.player();
            LivingEntity t = Wave30Kit.target(p, reach);
            if (t == null || t == p) {
                Fx.say(p, "No mind in sight.");
                return;
            }
            fx(p.serverLevel(), VfxShape.THREAD_STRINGS, chest(p), body(t), glow(c), 16, 1.0f);
            if (t instanceof Mob m && Wave30Kit.controllable(p, t)) {
                Wave30Events.dominate(m, p, now(p) + ticks);
                Fx.say(p, t.getName().getString() + " bends to your will for " + ticks / 20 + " seconds.");
            } else if (!Wave20Util.friend(p, t) && !(t instanceof Player o && !Wave30Kit.harmable(p, o))) {
                Fx.addEffect(t, "minecraft:slowness", 80, 3);
                Fx.addEffect(t, "minecraft:weakness", 80, 1);
                Fx.say(p, "Its will is too strong to bend: it only falters.");
            }
        });
    }

    /** Pharaoh's Command: up to {@code max} mobs within the radius (no boss, no player) fall under the wielder's will for a short time. */
    static ActBody command(double radius, int max, int ticks) {
        return guard("command", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            FxRelic.ring(l, p.position(), Math.min(radius, 12.0), glow(c));
            int n = 0;
            for (LivingEntity t : Fx.foesAround(p, radius)) {
                if (n >= Mth.clamp(max, 1, 5)) break;
                if (t instanceof Mob m && Wave30Kit.controllable(p, t)) {
                    Wave30Events.dominate(m, p, now(p) + ticks);
                    fx(l, VfxShape.THREAD_STRINGS, chest(p), body(t), glow(c), 12, 0.8f);
                    n++;
                }
            }
            Fx.say(p, n == 0 ? "No mind near enough to command." : n + (n == 1 ? " mind answers" : " minds answer") + " your command for " + ticks / 20 + " seconds.");
        });
    }

    // ================================================================ the Key
    /** Open the Soul Room: the wielder opens the door of every foe's mind: its good effects are stripped, it is hurt and weakened; the wielder and the friends round are cleared of fear and confusion. */
    static ActBody soulRoom(double radius, float dmg) {
        return guard("soul_room", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            fx(l, VfxShape.SPACE_PORTAL, p.position().add(0, 1, 0), p.position().add(p.getLookAngle()), glow(c), 24, (float) radius * 0.4f);
            int n = 0;
            for (LivingEntity t : Fx.foesAround(p, radius)) {
                if (n++ >= MAX_HITS) break;
                if (!(t instanceof Player)) dispel(t);
                Fx.damage(p, t, scaled(c, dmg), "minecraft:magic");
                Fx.addEffect(t, "minecraft:weakness", 160, 1);
            }
            clearMind(p);
            for (Player o : l.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(Math.min(radius, 16.0)), o -> o != p && o.isAlive() && !o.isSpectator() && o.isAlliedTo(p))) clearMind(o);
        });
    }

    /** Mind Garden: a calm place inside the mind: the wielder and the friends round are healed, cleared of ailments and kept warm with regeneration and resistance. */
    static ActBody mindGarden(double radius, int secs, float heal) {
        return guard("mind_garden", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            FxRelic.ring(l, p.position(), Math.min(radius, 12.0), glow(c));
            Wave31Acts.cleanse(c);
            clearMind(p);
            Wave31Acts.allies(c, radius, "minecraft:regeneration", secs, 2);
            Wave31Acts.allies(c, radius, "minecraft:resistance", secs, 1);
            for (Player o : l.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(Math.min(radius, 16.0)), o -> o != p && o.isAlive() && !o.isSpectator() && o.isAlliedTo(p))) {
                clearMind(o);
                if (o instanceof ServerPlayer sp) Fx.heal(sp, scaled(c, heal) * 0.5f);
            }
            Fx.heal(p, scaled(c, heal));
        });
    }

    // ================================================================ the Scale
    /** Weigh the Heart: the monster in sight has its heart set against the feather; a heavy heart is hurt, weakened and marked, a light one (an animal, a friend) is spared. */
    static ActBody weighHeart(double reach, float dmg) {
        return guard("weigh_heart", c -> {
            ServerPlayer p = c.player();
            LivingEntity t = Wave20Util.lookTarget(p, reach);
            if (t == null) {
                Fx.say(p, "Nothing in sight to weigh.");
                return;
            }
            ServerLevel l = p.serverLevel();
            fx(l, VfxShape.MIRROR_PANE, chest(p), body(t), glow(c), 16, 1.0f);
            if (!sinful(t)) {
                Fx.say(p, "Its heart is lighter than the feather. It is spared.");
                return;
            }
            Fx.damage(p, t, scaled(c, dmg), "minecraft:magic");
            Fx.addEffect(t, "minecraft:weakness", 200, 2);
            Fx.addEffect(t, "minecraft:glowing", 200, 0);
            Wave101Marks.put(t, p.getUUID(), Wave101Rules.Mark.WEIGHED, now(p) + 240L);
            FxRelic.burst(l, body(t), glow(c));
            if (!t.isAlive()) Fx.say(p, "The heart was heavier than the feather.");
        });
    }

    /** Ammit's Verdict: every heavy heart within the radius is weighed at once: each is hurt and marked, and the wielder is mended for every heart that was devoured (up to five). */
    static ActBody verdict(double radius, float dmg) {
        return guard("verdict", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            fx(l, VfxShape.SEAL_CHAINS, p.position(), p.position().add(0, 1, 0), glow(c), 24, (float) radius * 0.3f);
            FxRelic.ring(l, p.position(), Math.min(radius, 12.0), glow(c));
            int hearts = 0;
            for (LivingEntity t : Fx.foesAround(p, radius)) {
                if (hearts >= MAX_HITS) break;
                if (!sinful(t)) continue;
                Fx.damage(p, t, scaled(c, dmg), "minecraft:magic");
                Fx.addEffect(t, "minecraft:glowing", 200, 0);
                Wave101Marks.put(t, p.getUUID(), Wave101Rules.Mark.WEIGHED, now(p) + 240L);
                hearts++;
            }
            if (hearts > 0) Fx.heal(p, scaled(c, Wave101Rules.verdictHeal(hearts)));
            else Fx.say(p, "No heavy heart is near.");
        });
    }

    // ================================================================ the Necklace
    /** Foresight: the wielder sees the next moments: quickness and resistance, every foe round marked, the future vision lent. */
    static ActBody foresee(double radius, int secs) {
        return guard("foresight", c -> {
            ServerPlayer p = c.player();
            FxRelic.ring(p.serverLevel(), p.position(), Math.min(radius, 12.0), glow(c));
            Wave31Acts.mark(c, radius, secs);
            Fx.addEffect(p, "minecraft:speed", secs * 20, 1);
            Fx.addEffect(p, "minecraft:resistance", secs * 20, 1);
            Fx.shield(p, 40f, secs * 20);
            Fx.lendSkill(p, "tensura:future_vision", secs * 20);
        });
    }

    /** Prophecy: the wielder has seen how this ends and steps out of it: mended, cleansed, and for three seconds nothing can hurt them much. */
    static ActBody prophecy() {
        return guard("prophecy", c -> {
            ServerPlayer p = c.player();
            FxRelic.ring(p.serverLevel(), p.position(), 3.0, glow(c));
            fx(p.serverLevel(), VfxShape.TIME_REWIND, p.position(), p.position().add(0, 1, 0), glow(c), 30, 2.0f);
            float missing = p.getMaxHealth() - p.getHealth();
            Fx.heal(p, scaled(c, missing * 0.6f));
            Wave31Acts.cleanse(c);
            Fx.addEffect(p, "minecraft:resistance", 60, 3);
            Fx.shield(p, 60f, 200);
        });
    }

    // ================================================================ 3.35.0: the powers the wiki lists and the first version of the items did not have
    /** Longest a sealed or controlled mob stays so, whatever the caller asks for: five minutes at most. */
    static final int MAX_SEAL_TICKS = Wave101Rules.MAX_SEAL_TICKS;

    /** Holds a creature still: a sealed soul does not move, strike or cast (the effects of the mod, then the vanilla ones; an unknown id is skipped). */
    private static void hold(LivingEntity t, int ticks) {
        int ticks2 = Wave101Rules.holdTicks(ticks);
        Fx.addEffect(t, "tensura:paralysis", ticks2, 0);
        Fx.addEffect(t, "minecraft:slowness", ticks2, 6);
        Fx.addEffect(t, "minecraft:weakness", ticks2, 3);
        Fx.addEffect(t, "minecraft:mining_fatigue", ticks2, 3);
        if (t instanceof Mob m) m.setTarget(null);
    }

    /** Duel Monster (Puzzle: it summons and controls Duel Monsters): one spectral monster fights for the wielder for a while (the summon helper keeps at most six allies per player). */
    static ActBody duelMonster(int ticks) {
        return guard("duel_monster", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            fx(l, VfxShape.MAGIC_CIRCLE_EXPLOSION, p.position(), p.position().add(p.getLookAngle().scale(2.0)).add(0, 0.2, 0), glow(c), 40, 3.0f);
            LivingEntity m = Fx.summon(p, "minecraft:iron_golem", Mth.clamp(ticks, 100, 1200));
            if (m == null) m = Fx.summon(p, "minecraft:skeleton", Mth.clamp(ticks, 100, 1200));
            if (m == null) {
                Fx.say(p, "No Duel Monster answers the summons.");
                return;
            }
            m.setCustomName(net.minecraft.network.chat.Component.literal("Duel Monster"));
            Fx.addEffect(m, "minecraft:strength", Mth.clamp(ticks, 100, 1200), 1);
            Fx.say(p, "A Duel Monster takes the field.");
        });
    }

    // ---------------------------------------------------------------- the Eye
    /** Seal in a Card (Eye: it seals souls inside cards): the mob in sight is held in a soul card for a few seconds (a boss only falters), and the wielder deals more to it while it lasts. */
    static ActBody sealInCard(double reach, int secs) {
        return guard("seal_in_card", c -> {
            ServerPlayer p = c.player();
            LivingEntity t = Wave20Util.lookTarget(p, Mth.clamp(reach, 4, 32));
            if (t == null) {
                Fx.say(p, "No soul in sight to seal.");
                return;
            }
            ServerLevel l = p.serverLevel();
            fx(l, VfxShape.SEAL_CHAINS, chest(p), body(t), glow(c), 24, 1.2f);
            if (Wave30Kit.isBoss(t) || t instanceof Player) {
                Fx.addEffect(t, "minecraft:slowness", 60, 2);
                Fx.say(p, "Its soul is too great for a card: it only falters.");
                return;
            }
            hold(t, secs * 20);
            Fx.addEffect(t, "minecraft:glowing", secs * 20, 0);
            Wave101Marks.put(t, p.getUUID(), Wave101Rules.Mark.READ, now(p) + secs * 20L);
            FxRelic.burst(l, body(t), glow(c));
            Fx.say(p, t.getName().getString() + " is sealed in a card for " + secs + " seconds.");
        });
    }

    /** Sight of the Dead (Eye: visions of the dead): the undead and the dying near the wielder are shown through the walls and marked, and the wielder sees in the dark. */
    static ActBody sightOfTheDead(double radius, int secs) {
        return guard("sight_of_the_dead", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            FxRelic.ring(l, p.position(), Math.min(radius, 12.0), glow(c));
            long until = now(p) + secs * 20L;
            int n = 0;
            for (LivingEntity t : Fx.foesAround(p, radius)) {
                boolean dead = t.getType().is(EntityTypeTags.UNDEAD), dying = t.getHealth() < t.getMaxHealth() * 0.25f;
                if (!dead && !dying) continue;
                if (n++ >= 24) break;
                Fx.addEffect(t, "minecraft:glowing", secs * 20, 0);
                Wave101Marks.put(t, p.getUUID(), Wave101Rules.Mark.READ, until);
                fx(l, VfxShape.MIRROR_PANE, body(t), p.getEyePosition(), glow(c), 12, 0.8f);
            }
            Fx.addEffect(p, "minecraft:night_vision", secs * 20, 0);
            Fx.lendSkill(p, "tensura:magic_sense", secs * 20);
            Fx.say(p, n == 0 ? "The dead are quiet here." : n + (n == 1 ? " soul" : " souls") + " of the dead and the dying are shown to you.");
        });
    }

    // ---------------------------------------------------------------- the Ring
    /** Seal a Soul (Ring: it seals souls into objects): the soul of a small mob in sight is sealed in the Ring (it dies) and its power feeds the wielder; a great soul is only hurt. */
    static ActBody sealSoul(double reach, float dmg, float maxSealable) {
        return guard("seal_soul", c -> {
            ServerPlayer p = c.player();
            LivingEntity t = Wave20Util.lookTarget(p, Mth.clamp(reach, 4, 24));
            if (t == null) {
                Fx.say(p, "No soul in sight to seal.");
                return;
            }
            ServerLevel l = p.serverLevel();
            fx(l, VfxShape.ASH_FORMATION, body(t), chest(p), glow(c), 20, 1.0f);
            boolean small = Wave101Rules.sealable(Wave30Kit.isBoss(t), t instanceof Player, t.getMaxHealth(), maxSealable);
            if (!small) {
                Fx.damage(p, t, scaled(c, dmg), "minecraft:magic");
                Fx.say(p, "Its soul is too great to seal: the Ring only draws at it.");
                return;
            }
            Fx.damage(p, t, 400f, "minecraft:magic");
            if (t.isAlive()) return;
            Wave2Mana.give(p, 0.08 * Wave101Set.power(p));
            Fx.heal(p, Math.min(40f, scaled(c, 10f)));
            FxRelic.burst(l, body(t), glow(c));
            Fx.say(p, "A soul is sealed in the Ring.");
        });
    }

    // ---------------------------------------------------------------- the Rod
    /** Seal in a Tablet (Rod: it seals spirit monsters in stone tablets): up to three mobs in a line are turned to stone tablets for a while (a boss only falters). */
    static ActBody sealInTablet(double range, int secs) {
        return guard("seal_in_tablet", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            double len = Wave20Util.lineLength(p, Mth.clamp(range, 4, 32));
            Vec3 from = chest(p), to = from.add(p.getLookAngle().scale(len));
            fx(l, VfxShape.SEAL_CHAINS, from, to, glow(c), 24, 1.4f);
            int n = 0;
            for (LivingEntity t : Wave20Util.foesOnLine(p, range, 1.6, 3)) {
                if (Wave30Kit.isBoss(t) || t instanceof Player) {
                    Fx.addEffect(t, "minecraft:slowness", 60, 2);
                    continue;
                }
                hold(t, secs * 20);
                Fx.addEffect(t, "minecraft:resistance", secs * 20, 4);                  // stone does not break (and does not heal either)
                FxRelic.burst(l, body(t), glow(c));
                n++;
            }
            Fx.say(p, n == 0 ? "No spirit monster on the line to seal." : n + (n == 1 ? " spirit is" : " spirits are") + " sealed in stone for " + secs + " seconds.");
        });
    }

    // ---------------------------------------------------------------- the Key
    /** Redecorate the Soul (Key: it redecorates a soul to make anyone a puppet): the mob in sight (no boss, no player) becomes the wielder's puppet for a long while. */
    static ActBody redecorateSoul(double reach, int ticks) {
        return guard("redecorate_soul", c -> {
            ServerPlayer p = c.player();
            LivingEntity t = Wave30Kit.target(p, Mth.clamp(reach, 4, 24));
            if (t == null || t == p) {
                Fx.say(p, "No soul in sight to redecorate.");
                return;
            }
            ServerLevel l = p.serverLevel();
            fx(l, VfxShape.SPACE_PORTAL, chest(p), body(t), glow(c), 30, 1.2f);
            if (t instanceof Mob m && Wave30Kit.controllable(p, t)) {
                int ticks2 = Mth.clamp(ticks, 100, MAX_SEAL_TICKS);
                Wave30Events.dominate(m, p, now(p) + ticks2);
                fx(l, VfxShape.THREAD_STRINGS, chest(p), body(t), glow(c), 20, 1.0f);
                Fx.say(p, t.getName().getString() + " is your puppet for " + ticks2 / 20 + " seconds.");
            } else {
                Fx.say(p, "That soul cannot be redecorated.");
            }
        });
    }

    // ---------------------------------------------------------------- the Scale
    /** Fuse Monsters (Scale: it fuses monsters): up to five foes are pulled together into one place and fused in a single blow, harder for every foe in the knot; each is marked as weighed. */
    static ActBody fuseMonsters(double radius, float dmg) {
        return guard("fuse_monsters", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            fx(l, VfxShape.SEAL_CHAINS, p.position(), p.position().add(0, 1, 0), glow(c), 24, (float) radius * 0.3f);
            List<LivingEntity> knot = new ArrayList<>();
            for (LivingEntity t : Fx.foesAround(p, radius)) {
                if (knot.size() >= Wave101Rules.FUSE_MAX) break;
                knot.add(t);
            }
            if (knot.size() < Wave101Rules.FUSE_MIN) {
                Fx.say(p, "There are not two monsters to fuse.");
                return;
            }
            Vec3 centre = Vec3.ZERO;
            for (LivingEntity t : knot) centre = centre.add(t.position());
            centre = centre.scale(1.0 / knot.size());
            float each = scaled(c, dmg) * Wave101Rules.fuseFactor(knot.size());
            long until = now(p) + 240L;
            for (LivingEntity t : knot) {
                Vec3 pull = centre.subtract(t.position());
                t.setDeltaMovement(pull.scale(0.35).add(0, 0.2, 0));
                t.hurtMarked = true;
                Fx.damage(p, t, each, "minecraft:magic");
                Fx.addEffect(t, "minecraft:slowness", 100, 3);
                Wave101Marks.put(t, p.getUUID(), Wave101Rules.Mark.WEIGHED, until);
            }
            FxRelic.burst(l, centre.add(0, 1, 0), glow(c));
            Fx.say(p, knot.size() + " monsters are fused.");
        });
    }

    // ---------------------------------------------------------------- the Necklace
    /** Sight of the Past (Necklace: it sees into the past): whoever hurt the wielder (the last attacker, and every foe round that has them in its sights) is shown through the walls and marked. */
    static ActBody sightOfThePast(double radius, int secs) {
        return guard("sight_of_the_past", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            FxRelic.ring(l, p.position(), Math.min(radius, 12.0), glow(c));
            fx(l, VfxShape.TIME_REWIND, p.position(), p.position().add(0, 1, 0), glow(c), 30, 2.0f);
            long until = now(p) + secs * 20L;
            int n = 0;
            LivingEntity last = p.getLastHurtByMob();
            if (last != null && last.isAlive() && last != p && !last.isAlliedTo(p) && p.tickCount - p.getLastHurtByMobTimestamp() < 1200 && last.distanceToSqr(p) < 64.0 * 64.0) {
                Fx.addEffect(last, "minecraft:glowing", secs * 20, 0);
                Wave101Marks.put(last, p.getUUID(), Wave101Rules.Mark.READ, until);
                fx(l, VfxShape.MIRROR_PANE, body(last), p.getEyePosition(), glow(c), 12, 0.8f);
                n++;
            }
            for (LivingEntity t : Fx.foesAround(p, radius)) {
                if (n >= 12) break;
                if (t == last || !(t instanceof Mob m) || m.getTarget() != p) continue;
                Fx.addEffect(t, "minecraft:glowing", secs * 20, 0);
                Wave101Marks.put(t, p.getUUID(), Wave101Rules.Mark.READ, until);
                n++;
            }
            Fx.lendSkill(p, "tensura:future_vision", secs * 20);
            Fx.say(p, n == 0 ? "The past shows nothing that wishes you harm." : "The past shows who means you harm.");
        });
    }

    // ================================================================ the Dragon Balls
    /** Shenron's Wish: the wielder is made whole (all the missing health, ailments cleansed, a great shield, regeneration and strength) and the friends round are mended too. */
    static ActBody wish() {
        return guard("wish", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            fx(l, VfxShape.SPACE_PORTAL, p.position().add(0, 6, 0), p.position(), glow(c), 40, 3.0f);
            FxRelic.ring(l, p.position(), 10.0, glow(c));
            Fx.heal(p, Math.max(0f, p.getMaxHealth() - p.getHealth()));
            Wave31Acts.cleanse(c);
            Fx.shield(p, 100f, 300);
            Fx.addEffect(p, "minecraft:regeneration", 200, 2);
            Fx.addEffect(p, "minecraft:strength", 600, 1);
            Wave31Acts.allies(c, 12.0, "minecraft:regeneration", 10, 2);
            Wave31Acts.allies(c, 12.0, "minecraft:absorption", 20, 2);
        });
    }
}
