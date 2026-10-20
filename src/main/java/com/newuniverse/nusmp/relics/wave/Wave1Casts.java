package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Wave 1: what the hand-held relics do when used. Every spell is server side, spends something (a charge from the relic's own pool,
 * a use of the day, a cooldown) and is bounded: a handful of particles, at most 24 foes (from {@code Fx.foesAround}), timers through
 * the repo's {@link SpellRuntime}. Tabletop rolls and saves are Minecraft numbers here: a "Wisdom or Strength save" is the target's
 * knockback resistance and size, an "advantage" is a few points of damage, "per long rest" is per Minecraft day.
 */
final class Wave1Casts {
    private Wave1Casts() {}

    // ---------------------------------------------------------------- damage types and keys
    static final String LIGHT = TensuraDamageTypes.LIGHT_ELEMENTAL.location().toString();
    static final String MAGIC = DamageTypes.MAGIC.location().toString();

    static final String K_GUIDED = "nusmp_w1_guided";          // on a foe: the Guiding Bolt's mark, until this game time
    static final String K_SMITE = "nusmp_w1_smite";            // on a player: the Divine Smite rides the next blow, until
    static final String K_FLAME = "nusmp_w1_flame";            // on a player: the Sacred Flame answers, until
    static final String K_AIR = "nusmp_w1_air";                // on a player: the Air Bubble holds, until
    static final String K_GUARD_UNTIL = "nusmp_w1_guard_until"; // on a player: the moonlit spirits stand guard, until
    static final String K_GUARD_N = "nusmp_w1_guard_n";        // ... and how many blows they still turn aside
    static final String K_RAINBOW = "nusmp_w1_rainbow_cd";     // on a player: the Rainbow Rapier's vanishing may start again at
    static final String K_EXPIRE = "nusmp_w1_expire";          // on a conjured armor stand: when it fades

    /**
     * Tensura skills the relics lend (Fx.lendSkill: a saved lease, gone when the time is up, skipped silently when the id is not in the registry).
     * Ids named after the registry's constants: the five VERIFIED in this repo are magic_sense, sage, danger_sense, physical_attack_resistance and
     * poison_resistance (their ids are exactly the lower-cased constant names, which is the rule the rest follow); every other id here is INFERRED
     * that way and gated at run time, so a wrong one only means the Minecraft part of the ability runs alone.
     */
    static final String SK_MAGIC_SENSE = "tensura:magic_sense", SK_DANGER_SENSE = "tensura:danger_sense";                       // verified
    static final String SK_RANGED_BARRIER = "tensura:ranged_barrier", SK_MULTILAYER_BARRIER = "tensura:multilayer_barrier", SK_MAGIC_AURA = "tensura:magic_aura";
    static final String SK_MUSICIAN = "tensura:musician", SK_SOUND = "tensura:sound_manipulation", SK_WIND_TRANSFORM = "tensura:wind_transform";
    static final String SK_WATER_CURRENT = "tensura:water_current_control", SK_WATER_MANIP = "tensura:water_manipulation", SK_WATER_BLADE = "tensura:water_blade";
    static final String SK_COLD_RESIST = "tensura:cold_resistance", SK_BEAST = "tensura:beast_transformation", SK_THOUGHT = "tensura:thought_communication";
    static final String SK_TELEPATHY = "tensura:telepathy", SK_SPATIAL_MOTION = "tensura:spatial_motion", SK_SPATIAL_MANIP = "tensura:spatial_manipulation";
    static final String SK_FARSIGHT = "tensura:farsight", SK_APPRAISAL = "tensura:analytical_appraisal", SK_LIGHT_RESIST = "tensura:light_attack_resistance";
    static final String SK_STRENGTHEN_BODY = "tensura:strengthen_body", SK_BODY_ARMOR = "tensura:body_armor";
    static final String FEAR = "tensura:fear", PARALYSIS = "tensura:paralysis";                                                  // Tensura mob effects, skipped when absent
    static final String HOLY = TensuraDamageTypes.HOLY_DAMAGE.location().toString();

    // ---------------------------------------------------------------- the spell table and its dispatcher
    @FunctionalInterface
    interface Cast {
        /** Runs the spell; false means it did nothing (no target, wrong place) and nothing is spent. */
        boolean run(ServerPlayer p);
    }

    /** A spell of a multi-spell relic: its price in pool charges, its own cooldown, and how many times a day it may be cast (0 = any). */
    record Spell(String key, String name, int cost, int cooldown, int perDay, Cast cast) {}

    static final Wave1Rules.Pool MOON = new Wave1Rules.Pool("moon", 10, 4, 6);        // 10 charges, 1d6+4 at dawn
    static final Wave1Rules.Pool VIOLA = new Wave1Rules.Pool("viola", 5, 1, 4);       // 5 charges, 1d4+1 at dawn
    static final Wave1Rules.Pool WALKER = new Wave1Rules.Pool("walker", 3, 0, 0);     // 3 charges, all back at dawn

    static final Spell[] MOON_SPELLS = {
            new Spell("bolt", "Guiding Bolt", 1, 30, 0, Wave1Casts::guidingBolt),
            new Spell("beam", "Moonbeam", 2, 80, 0, Wave1Casts::moonbeam),
            new Spell("wall", "Lightwall", 5, 400, 0, Wave1Casts::lightwall),
            new Spell("sun", "Sunbeam", 6, 400, 0, Wave1Casts::sunbeam),
            new Spell("crown", "Star Crown", 7, 900, 0, Wave1Casts::starCrown),
    };

    static final Spell[] VIOLA_SPELLS = {
            new Spell("play", "Spirit Performance", 0, 1200, 0, Wave1Casts::spiritPerformance),
            new Spell("whisper", "Dissonant Whispers", 1, 100, 0, Wave1Casts::dissonantWhispers),
            new Spell("beam", "Moonbeam", 2, 120, 0, Wave1Casts::moonbeam),
            new Spell("gas", "Gaseous Form", 3, 600, 0, Wave1Casts::gaseousForm),
    };

    static final Spell[] ASTRAL_SPELLS = {
            new Spell("stars", "Blinding Stars", 0, 200, 1, Wave1Casts::blindingStars),
            new Spell("door", "Dimension Door", 1, 60, 0, Wave1Casts::dimensionDoor),
            new Spell("scry", "Scrying", 2, 200, 0, Wave1Casts::scrying),
            new Spell("shift", "Planeshift", 3, 1200, 0, Wave1Casts::planeshift),
    };

    static final Spell[] CAT_SPELLS = {
            new Spell("jump", "Jump", 0, 40, 0, Wave1Casts::catJump),
            new Spell("speak", "Speak with Cats", 0, 400, 0, Wave1Casts::speakWithCats),
            new Spell("form", "Feline Form", 0, 200, 1, Wave1Casts::felineForm),
    };

    static final Spell[] BOOK_SPELLS = {
            new Spell("message", "Message", 0, 40, 1, Wave1Casts::massage),
            new Spell("armour", "Mage Armour", 0, 40, 1, Wave1Casts::mageArmoir),
            new Spell("hold", "Hold Person", 0, 40, 1, Wave1Casts::foldPerson),
            new Spell("dead", "Speak with Dead", 0, 40, 1, Wave1Casts::speakWithDad),
            new Spell("ice", "Ice Storm", 0, 40, 1, Wave1Casts::ickStorm),
            new Spell("tele", "Teleport", 0, 40, 1, Wave1Casts::telepoot),
            new Spell("wish", "Wish", 0, 40, 1, Wave1Casts::wash),
    };

    /** The pool as this wearer holds it: the Dimension Walker is 3 charges, plus the 2 the Amulet of the Sea of Stars adds (Wave4.walkerBonus). */
    static Wave1Rules.Pool sized(Wave1Rules.Pool pool, ServerPlayer p) {
        if (pool == null || !pool.key().equals(WALKER.key())) return pool;
        return new Wave1Rules.Pool(pool.key(), pool.max() + Wave4.walkerBonus(p), pool.base(), pool.die());
    }

    /** Casts the spell the relic has selected (sneak + use cycles the selection), after checking and then spending its price. */
    static void cast(ActCtx c, Wave1Rules.Pool pool0, Spell[] spells, boolean mayGutter) {
        ServerPlayer p = c.player();
        Wave1Rules.Pool pool = sized(pool0, p);
        ItemStack stack = Wave1Store.stack(c);
        Wave1Rules.Bag bag = Wave1Store.bag(stack);
        long now = p.level().getGameTime();
        Spell s = spells[Wave1Rules.selected(bag, spells.length)];
        if (!Wave1Rules.ready(bag, s.key(), now)) {
            Fx.say(p, s.name() + " is not ready for " + (Wave1Rules.remaining(bag, s.key(), now) + 19) / 20 + " s.");
            return;
        }
        if (s.perDay() > 0 && Wave1Rules.usesLeft(bag, s.key(), now, s.perDay()) <= 0) {
            Fx.say(p, s.name() + " is spent until dawn.");
            return;
        }
        if (pool != null && s.cost() > 0) {
            int have = Wave1Rules.charges(bag, now, pool);
            if (have < s.cost()) {
                Fx.say(p, s.name() + " needs " + s.cost() + " charges; " + have + " left until dawn.");
                return;
            }
        }
        if (!s.cast().run(p)) return;
        if (s.perDay() > 0) Wave1Rules.takeUse(bag, s.key(), now, s.perDay());
        Wave1Rules.startCooldown(bag, s.key(), now, s.cooldown());
        if (pool != null && s.cost() > 0) {
            Wave1Rules.spend(bag, now, pool, s.cost());
            int left = Wave1Rules.charges(bag, now, pool);
            Fx.say(p, s.name() + ". Charges " + left + "/" + pool.max() + ".");
            if (mayGutter && left <= 0 && p.getRandom().nextInt(20) == 0) {
                Wave1Rules.dim(bag, pool);
                Fx.say(p, "The last of the moonlight gutters out of the staff: it stays dark through the next dawn.");
            }
        }
    }

    /** Sneak + use: moves the selection to the next spell and says which it is. */
    static void select(ActCtx c, Wave1Rules.Pool pool0, Spell[] spells) {
        ServerPlayer p = c.player();
        Wave1Rules.Pool pool = sized(pool0, p);
        Wave1Rules.Bag bag = Wave1Store.bag(Wave1Store.stack(c));
        Spell s = spells[Wave1Rules.cycle(bag, spells.length)];
        StringBuilder sb = new StringBuilder("Selected: ").append(s.name());
        if (s.cost() > 0) sb.append(" (").append(s.cost()).append(s.cost() == 1 ? " charge)" : " charges)");
        if (pool != null) sb.append("  Charges ").append(Wave1Rules.charges(bag, p.level().getGameTime(), pool)).append('/').append(pool.max());
        Fx.say(p, sb.toString());
    }

    // ================================================================ Staff of Moonlight (and the Viola's Moonbeam)
    static boolean guidingBolt(ServerPlayer p) {
        LivingEntity t = Wave1Aim.aimedFoe(p, 28.0);
        if (t == null) {
            Fx.say(p, "The bolt needs a foe in sight.");
            return false;
        }
        ServerLevel l = p.serverLevel();
        Wave1Aim.beam(l, p.getEyePosition().add(0, -0.25, 0), t.getBoundingBox().getCenter(), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_1, FxTone.LIGHT), 24);
        Fx.damage(p, t, Wave1Aim.radiant(t, 4.0f), LIGHT);
        Fx.addEffect(t, "minecraft:glowing", 120, 0);
        t.getPersistentData().putLong(K_GUIDED, l.getGameTime() + 120);        // the next blow on it lands truer (the "advantage")
        return true;
    }

    static boolean moonbeam(ServerPlayer p) {
        Vec3 at = Wave1Aim.point(p, 22.0);
        ServerLevel l = p.serverLevel();
        UUID who = p.getUUID();
        SpellRuntime.zone(l, 60, 10, age -> {
            ServerPlayer owner = l.getServer().getPlayerList().getPlayer(who);
            if (owner == null || !owner.isAlive()) return;
            Wave1Aim.column(l, at, 6.0, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_1, FxTone.LIGHT));
            for (LivingEntity e : Wave1Aim.foesAt(owner, at, 2.5)) {
                Fx.damage(owner, e, Wave1Aim.radiant(e, 0.85f), LIGHT);
                Fx.addEffect(e, "minecraft:glowing", 40, 0);
            }
        });
        return true;
    }

    static boolean lightwall(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        flat = flat.lengthSqr() < 1.0e-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        Vec3 centre = p.position().add(flat.scale(3.0));
        Vec3 side = new Vec3(-flat.z, 0.0, flat.x);
        Vec3 a = centre.subtract(side.scale(3.5)), b = centre.add(side.scale(3.5));
        Vec3 normal = flat;
        ServerLevel l = p.serverLevel();
        UUID who = p.getUUID();
        Fx.lendSkills(p, 200, SK_RANGED_BARRIER, SK_MULTILAYER_BARRIER);              // the wall merges with Tensura's barriers for as long as it stands
        SpellRuntime.zone(l, 160, 5, age -> {
            ServerPlayer owner = l.getServer().getPlayerList().getPlayer(who);
            if (owner == null || !owner.isAlive()) return;
            for (int i = 0; i <= 7; i++) {
                Vec3 q = a.lerp(b, i / 7.0);
                for (int h = 0; h < 4; h++) com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.LIGHT, q.x, q.y + h, q.z, 1, 0.0, 0.1, 0.0, 0.0);
            }
            AABB box = new AABB(Math.min(a.x, b.x) - 1.0, centre.y - 1.0, Math.min(a.z, b.z) - 1.0, Math.max(a.x, b.x) + 1.0, centre.y + 4.0, Math.max(a.z, b.z) + 1.0);
            int seen = 0;
            for (Projectile pr : l.getEntitiesOfClass(Projectile.class, box)) {
                if (++seen > 16) break;
                var owner2 = pr.getOwner();
                if (owner2 != null && (owner2 == owner || owner2.isAlliedTo(owner))) continue;
                if (Wave1Aim.distToSegment(a, b, new Vec3(pr.getX(), a.y, pr.getZ())) <= 1.0) {
                    com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.LIGHT, pr.getX(), pr.getY(), pr.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                    pr.discard();
                }
            }
            for (LivingEntity e : Wave1Aim.foesAt(owner, centre, 5.0)) {
                Vec3 flatPos = new Vec3(e.getX(), a.y, e.getZ());
                if (Wave1Aim.distToSegment(a, b, flatPos) > 1.1 || e.getY() < centre.y - 1.5 || e.getY() > centre.y + 4.0) continue;
                double sideOf = e.position().subtract(centre).dot(normal) >= 0 ? 1.0 : -1.0;
                e.setDeltaMovement(e.getDeltaMovement().add(normal.x * sideOf * 0.7, 0.12, normal.z * sideOf * 0.7));
                e.hurtMarked = true;
            }
        });
        return true;
    }

    static boolean sunbeam(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        Vec3 from = p.getEyePosition().add(0, -0.2, 0);
        Vec3 to = Wave1Aim.point(p, 20.0);
        Wave1Aim.beam(l, from, to, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_1, FxTone.LIGHT), 40);
        Wave1Aim.beam(l, from, to, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_1, FxTone.LIGHT), 20);
        for (LivingEntity e : Fx.foesAround(p, 22.0)) {
            if (Wave1Aim.distToSegment(from, to, e.getBoundingBox().getCenter()) > 1.4) continue;
            Fx.damage(p, e, Wave1Aim.radiant(e, 5.5f), LIGHT);
            Fx.addEffect(e, "minecraft:blindness", 80, 0);
            Fx.addEffect(e, "tensura:flashed_blindness", 60, 0);
        }
        return true;
    }

    static boolean starCrown(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        UUID who = p.getUUID();
        Fx.shield(p, 4.0f, 300);
        Fx.lendSkill(p, SK_MAGIC_AURA, 300);                                           // an aura of the stars lent to the wearer's own magic
        SpellRuntime.zone(l, 180, 30, age -> {
            ServerPlayer owner = l.getServer().getPlayerList().getPlayer(who);
            if (owner == null || !owner.isAlive()) return;
            FxRelic.ring(l, owner.position().add(0.0, 1.9, 0.0), 0.7, 0xFFF3E6A0);
            LivingEntity nearest = null;
            double best = 100.0;
            for (LivingEntity e : Fx.foesAround(owner, 10.0)) {
                double d = e.distanceToSqr(owner);
                if (d < best) {
                    best = d;
                    nearest = e;
                }
            }
            if (nearest == null) return;
            Wave1Aim.beam(l, owner.position().add(0.0, 2.1, 0.0), nearest.getBoundingBox().getCenter(), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_1, FxTone.LIGHT), 12);
            Fx.damage(owner, nearest, Wave1Aim.radiant(nearest, 0.85f), LIGHT);
        });
        return true;
    }

    // ================================================================ Viola of Moonlit Spirits
    static boolean spiritPerformance(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        boolean night = l.isNight();
        int spirits = night ? 3 : 1;
        p.getPersistentData().putLong(K_GUARD_UNTIL, l.getGameTime() + 400);
        p.getPersistentData().putInt(K_GUARD_N, spirits);
        Fx.lendSkill(p, SK_MUSICIAN, 400);                                             // the instrument lends its player the Musician's gift while the spirits listen
        float[] notes = {0.75f, 0.9f, 1.12f, 1.5f};
        for (int i = 0; i < notes.length; i++) {
            float pitch = notes[i];
            SpellRuntime.later(l, i * 6, () -> l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, pitch));
        }
        for (int i = 0; i < spirits; i++) {
            double ang = Math.PI * 2.0 * i / spirits;
            com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.DARK, p.getX() + Math.cos(ang) * 1.6, p.getY() + 1.0, p.getZ() + Math.sin(ang) * 1.6, 8, 0.15, 0.4, 0.15, 0.02);
        }
        FxRelic.ring(l, p.position().add(0.0, 0.2, 0.0), 1.6, 0xFF9FD8FF);
        Fx.say(p, night ? "Three ghosts of the viola's past owners rise to guard you." : "By day only one faint ghost answers the strings.");
        return true;
    }

    static boolean dissonantWhispers(ServerPlayer p) {
        LivingEntity t = Wave1Aim.aimedFoe(p, 14.0);
        if (t == null) {
            Fx.say(p, "The whispers need a foe in sight.");
            return false;
        }
        ServerLevel l = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.LIGHT, t.getX(), t.getEyeY() + 0.4, t.getZ(), 6, 0.3, 0.2, 0.3, 0.0);
        Fx.damage(p, t, 4.0f, MAGIC);
        Fx.addEffect(t, "minecraft:slowness", 60, 1);
        Fx.addEffect(t, "minecraft:weakness", 60, 0);
        Fx.addEffect(t, FEAR, 60, 0);                                                  // "must use its reaction to move away": Tensura's fear
        Fx.lendSkill(p, SK_SOUND, 200);
        if (t instanceof Mob m) {
            m.setTarget(null);
            m.getNavigation().stop();
        }
        Vec3 away = t.position().subtract(p.position());
        if (away.lengthSqr() > 1.0e-4) {
            away = away.normalize().scale(0.6);
            t.setDeltaMovement(t.getDeltaMovement().add(away.x, 0.1, away.z));
            t.hurtMarked = true;
        }
        return true;
    }

    static boolean gaseousForm(ServerPlayer p) {
        Fx.addEffect(p, "minecraft:invisibility", 160, 0);
        Fx.addEffect(p, "minecraft:slow_falling", 160, 0);
        Fx.addEffect(p, "minecraft:resistance", 160, 2);
        Fx.addEffect(p, "minecraft:speed", 160, 0);
        Fx.lendSkill(p, SK_WIND_TRANSFORM, 160);                                       // Tensura's wind transform: the body really does become air
        ServerLevel l = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.LIGHT, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.4, 0.6, 0.4, 0.02);
        Fx.say(p, "You thin to mist for 8 s: unseen, drifting, and hard to hurt.");
        return true;
    }

    // ================================================================ Liquid Sword
    static boolean liquidLure(ServerPlayer p) {
        LivingEntity t = Wave1Aim.aimedFoe(p, 12.0);
        if (t == null) {
            Fx.say(p, "The watery whip needs a foe in sight.");
            return false;
        }
        ServerLevel l = p.serverLevel();
        Fx.lendSkills(p, 200, SK_WATER_CURRENT, SK_WATER_MANIP);                       // the whip is Tensura's water control for a moment
        Wave1Aim.beam(l, p.getEyePosition().add(0, -0.3, 0), t.getBoundingBox().getCenter(), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_1, FxTone.LIGHT), 24);
        Wave1Aim.beam(l, p.getEyePosition().add(0, -0.3, 0), t.getBoundingBox().getCenter(), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_1, FxTone.LIGHT), 12);
        if (t.getBbWidth() > 2.6f || t.getBbHeight() > 3.2f || t.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 0.8) {
            Fx.say(p, "The whip slips off: too big, or too well planted.");        // the Strength save
            return true;
        }
        Vec3 to = p.position().subtract(t.position());
        double d = to.length();
        if (d <= 1.8) return true;
        double speed = Math.max(0.4, Math.min(1.6, (d - 1.6) / 6.5));
        Vec3 dir = to.scale(1.0 / d);
        t.setDeltaMovement(dir.x * speed, 0.38, dir.z * speed);
        t.hurtMarked = true;
        return true;
    }

    // ================================================================ Marid's Valor
    /**
     * Flips the blade between solid (0, how it is found and how the 3D model is drawn) and liquid (1); the reach and the cold are read from
     * this by the hooks, and the matching Tensura skill is lent (the water blade for the liquid state, the cold ward for the solid one).
     */
    static boolean maridState(ServerPlayer p, ItemStack stack) {
        Wave1Rules.Bag bag = Wave1Store.bag(stack);
        boolean wasLiquid = bag.get("state") == 1;
        bag.put("state", wasLiquid ? 0 : 1);
        ServerLevel l = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.LIGHT, p.getX(), p.getY() + 1.2, p.getZ(), 16, 0.4, 0.4, 0.4, 0.02);
        Fx.say(p, wasLiquid ? "The blade sets solid: cold bites, and you may freeze water." : "The blade runs liquid: reach +2 blocks, and the water answers your hand.");
        Fx.lendSkill(p, wasLiquid ? SK_COLD_RESIST : SK_WATER_BLADE, 400);
        return true;
    }

    static boolean maridSolid(ItemStack stack) {
        return Wave1Store.bag(stack).get("state") != 1;
    }

    /** The blade's tip touches a body of water and freezes a small patch of it (3 x 3 at the surface) for a minute. */
    static boolean flashFreeze(ServerPlayer p, ItemStack stack) {
        ServerLevel l = p.serverLevel();
        if (!maridSolid(stack)) {
            Fx.say(p, "Only the solid blade freezes water: change its state first.");
            return false;
        }
        if (Labyrinth.in(l)) {
            Fx.say(p, "The Dungeon's water will not freeze.");
            return false;
        }
        HitResult hr = p.pick(6.0, 1.0F, true);
        if (!(hr instanceof BlockHitResult bhr) || hr.getType() != HitResult.Type.BLOCK) {
            Fx.say(p, "Touch the water with the blade.");
            return false;
        }
        BlockPos c = bhr.getBlockPos();
        if (!l.getFluidState(c).is(FluidTags.WATER)) {
            Fx.say(p, "Touch the water with the blade.");
            return false;
        }
        List<BlockPos> frozen = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos q = c.offset(dx, 0, dz);
                if (!l.isLoaded(q) || !l.mayInteract(p, q)) continue;
                if (l.getBlockState(q).is(Blocks.WATER) && l.getFluidState(q).isSource()) {
                    l.setBlock(q, Blocks.ICE.defaultBlockState(), 3);
                    frozen.add(q.immutable());
                }
            }
        }
        if (frozen.isEmpty()) {
            Fx.say(p, "There is no still water here to freeze.");
            return false;
        }
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.LIGHT, c.getX() + 0.5, c.getY() + 1.0, c.getZ() + 0.5, 24, 1.0, 0.3, 1.0, 0.02);
        SpellRuntime.later(l, 1200, () -> {
            for (BlockPos q : frozen) {
                if (l.isLoaded(q) && l.getBlockState(q).is(Blocks.ICE)) l.setBlock(q, Blocks.WATER.defaultBlockState(), 3);
            }
        });
        return true;
    }

    // ================================================================ Catstaff
    static boolean catJump(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        flat = flat.lengthSqr() < 1.0e-4 ? Vec3.ZERO : flat.normalize().scale(0.95);
        p.setDeltaMovement(flat.x, 0.72, flat.z);
        p.hurtMarked = true;
        p.fallDistance = 0.0f;
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.CAT_AMBIENT, SoundSource.PLAYERS, 1.6f, 1.1f);
        return true;
    }

    static boolean speakWithCats(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        int n = 0;
        for (Cat cat : l.getEntitiesOfClass(Cat.class, p.getBoundingBox().inflate(14.0))) {
            if (++n > 6) break;
            if (!cat.isTame()) cat.tame(p);
            cat.heal(4.0f);
            com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.LIGHT, cat.getX(), cat.getY() + 0.7, cat.getZ(), 4, 0.25, 0.2, 0.25, 0.0);
        }
        if (n == 0) {
            Fx.say(p, "No cat is near enough to hear you.");
            return false;
        }
        Fx.lendSkills(p, 1200, SK_THOUGHT, SK_TELEPATHY);                              // "speaks in cat": Tensura's thought communication for a minute
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.CAT_PURR, SoundSource.PLAYERS, 1.0f, 1.0f);
        Fx.say(p, "You say it in cat. The cats around you trust you, and are mended.");
        return true;
    }

    static boolean felineForm(ServerPlayer p) {
        Fx.addEffect(p, "minecraft:speed", 1800, 1);
        Fx.addEffect(p, "minecraft:jump_boost", 1800, 1);
        Fx.addEffect(p, "minecraft:night_vision", 1800, 0);
        Fx.lendSkill(p, SK_BEAST, 1800);                                               // the polymorph: Tensura's beast transformation, lent for the 90 s
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.CAT_AMBIENT, SoundSource.PLAYERS, 1.6f, 0.8f);
        com.newuniverse.nusmp.particle.fx.Fx.spray(((ServerLevel) p.level()), FxThemes.RELIC_WAVE_1, FxTone.LIGHT, p.getX(), p.getY() + 0.8, p.getZ(), 20, 0.4, 0.5, 0.4, 0.04);
        Fx.say(p, "Feline Form for 90 s: quick, springing and sure-eyed. Once a day.");
        return true;
    }

    // ================================================================ Rainbow Rapier
    private static final float[][] RAINBOW = {{1f, 0.15f, 0.15f}, {1f, 0.6f, 0.1f}, {1f, 0.95f, 0.2f}, {0.2f, 0.9f, 0.3f}, {0.2f, 0.6f, 1f}, {0.6f, 0.3f, 1f}};

    static boolean colourSpray(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        for (int i = 0; i < 36; i++) {
            double dist = 1.0 + (i % 6) * 1.1;
            double spread = (i / 6 - 2.5) * 0.14;
            Vec3 side = new Vec3(-look.z, 0.0, look.x);
            Vec3 q = eye.add(look.scale(dist)).add(side.scale(spread * dist * 0.6));
            float[] c = RAINBOW[i / 6];
            com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.LIGHT, q.x, q.y - 0.2, q.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        int hit = 0;
        for (LivingEntity e : Fx.foesAround(p, 8.0)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d < 0.1 || to.dot(look) / d < 0.55 || !p.hasLineOfSight(e)) continue;
            Fx.addEffect(e, "minecraft:blindness", 100, 0);
            Fx.addEffect(e, "tensura:flashed_blindness", 60, 0);
            Fx.addEffect(e, "minecraft:glowing", 100, 0);
            hit++;
        }
        l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.4f);
        Fx.say(p, hit == 0 ? "A wave of colours sweeps out, and finds no one." : "Colour Spray: " + hit + (hit == 1 ? " foe is" : " foes are") + " dazzled blind.");
        return true;
    }

    /** One touch: a sheep steps one dye round; else the block touched does (wool, carpet, concrete, glass, terracotta ...). */
    static boolean recolour(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        LivingEntity best = null;
        double bestCos = 0.93;
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(5.0))) {
            if (!(e instanceof Sheep) || !e.isAlive()) continue;
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d < 0.05 || d > 5.0) continue;
            double cos = to.dot(look) / d;
            if (cos > bestCos) {
                bestCos = cos;
                best = e;
            }
        }
        if (best instanceof Sheep s) {
            s.setColor(DyeColor.byId((s.getColor().getId() + 1) % 16));
            return touched(l, best.position().add(0.0, 1.0, 0.0), p);
        }
        HitResult hr = p.pick(5.0, 1.0F, false);
        if (hr instanceof BlockHitResult bhr && hr.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = bhr.getBlockPos();
            BlockState st = l.getBlockState(pos);
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(st.getBlock());
            String next = "minecraft".equals(key.getNamespace()) ? Wave1Words.nextDye(key.getPath()) : null;
            if (next != null && !Labyrinth.in(l) && l.mayInteract(p, pos)) {
                ResourceLocation id = ResourceLocation.withDefaultNamespace(next);
                if (BuiltInRegistries.BLOCK.containsKey(id)) {
                    BlockState to = BuiltInRegistries.BLOCK.get(id).defaultBlockState();
                    for (Property<?> prop : st.getProperties()) to = copy(st, to, prop);
                    l.setBlock(pos, to, 3);
                    return touched(l, Vec3.atCenterOf(pos), p);
                }
            }
        }
        Fx.say(p, "Touch a sheep, or a block of wool, carpet, glass, concrete or terracotta.");
        return false;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> prop) {
        return to.hasProperty(prop) ? to.setValue(prop, from.getValue(prop)) : to;
    }

    private static boolean touched(ServerLevel l, Vec3 at, ServerPlayer p) {
        for (int i = 0; i < RAINBOW.length; i++) {
            float[] c = RAINBOW[i];
            com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.LIGHT, at.x, at.y, at.z, 3, 0.35, 0.35, 0.35, 0.0);
        }
        l.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.6f);
        return true;
    }

    // ================================================================ Astralis
    static boolean blindingStars(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        Fx.lendSkill(p, SK_LIGHT_RESIST, 100);                                         // the stars do not burn their caster
        Fx.aoe(p, 8.0, 6.0f, LIGHT, "minecraft:blindness", 100, 0);
        FxRelic.ring(l, p.position().add(0.0, 1.0, 0.0), 6.0, 0xFFE9D8FF);
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.LIGHT, p.getX(), p.getY() + 1.4, p.getZ(), 40, 3.0, 1.2, 3.0, 0.05);
        Fx.say(p, "A flurry of stars: everything near you is scorched and blinded.");
        return true;
    }

    static boolean dimensionDoor(ServerPlayer p) {
        if (Labyrinth.in(p.level())) {
            Fx.say(p, "The Dungeon's walls keep the door shut.");
            return false;
        }
        Fx.lendSkill(p, SK_SPATIAL_MOTION, 200);                                       // the door is Tensura's spatial motion plus a safe-landing step
        if (!Fx.teleportLook(p, 32.0)) {
            Fx.say(p, "There is no safe place to land there.");
            return false;
        }
        return true;
    }

    static boolean scrying(ServerPlayer p) {
        int n = 0;
        for (LivingEntity e : Fx.foesAround(p, 48.0)) {
            Fx.addEffect(e, "minecraft:glowing", 200, 0);
            n++;
        }
        if (n == 0) {
            Fx.say(p, "The eye finds no foe within 48 blocks.");
            return false;
        }
        Fx.lendSkills(p, 600, SK_FARSIGHT, SK_MAGIC_SENSE, SK_APPRAISAL);              // scrying merges far sight, magic sense and appraisal for 30 s
        Fx.say(p, "The eye of the universe shows you " + n + (n == 1 ? " foe" : " foes") + " for 10 s.");
        return true;
    }

    /** To your bed, or the world's spawn: 3 charges and a long wait, and never out of or into the Living Dungeon. */
    static boolean planeshift(ServerPlayer p) {
        if (Labyrinth.in(p.level())) {
            Fx.say(p, "The Dungeon holds the way shut.");
            return false;
        }
        MinecraftServer srv = p.getServer();
        if (srv == null) return false;
        ServerLevel dest = null;
        BlockPos around = p.getRespawnPosition();
        if (around != null && p.getRespawnDimension() != null) dest = srv.getLevel(p.getRespawnDimension());
        if (dest == null || Labyrinth.in(dest)) {
            dest = srv.overworld();
            around = dest.getSharedSpawnPos();
        }
        Fx.lendSkill(p, SK_SPATIAL_MANIP, 200);                                        // the plane is crossed with Tensura's spatial manipulation
        BlockPos spot = safeSpot(dest, around);
        if (spot == null) {
            Fx.say(p, "No safe ground at your anchor.");
            return false;
        }
        ServerLevel from = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.spray(from, FxThemes.RELIC_WAVE_1, FxTone.DARK, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.4, 0.8, 0.4, 0.1);
        p.teleportTo(dest, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, p.getYRot(), p.getXRot());
        com.newuniverse.nusmp.particle.fx.Fx.spray(dest, FxThemes.RELIC_WAVE_1, FxTone.DARK, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.4, 0.8, 0.4, 0.1);
        return true;
    }

    private static BlockPos safeSpot(ServerLevel l, BlockPos near) {
        l.getChunk(near.getX() >> 4, near.getZ() >> 4);                                 // the anchor may lie in an unloaded chunk (another dimension): load it, as a respawn does
        BlockPos[] tries = {near, new BlockPos(near.getX(), l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, near.getX(), near.getZ()), near.getZ())};
        for (BlockPos base : tries) {
            for (int up = 0; up <= 3; up++) {
                BlockPos q = base.above(up);
                if (!l.isLoaded(q)) continue;
                if (l.getBlockState(q).getCollisionShape(l, q).isEmpty() && l.getBlockState(q.above()).getCollisionShape(l, q.above()).isEmpty()
                        && !l.getBlockState(q.below()).getCollisionShape(l, q.below()).isEmpty() && l.getFluidState(q).isEmpty()) return q;
            }
        }
        return null;
    }

    // ================================================================ Wand of Bubbles
    static boolean blowBubbles(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        Vec3 from = p.getEyePosition().add(p.getLookAngle().scale(0.8)).add(0.0, -0.3, 0.0);
        Vec3 look = p.getLookAngle();
        for (int i = 0; i < 18; i++) {
            Vec3 q = from.add(look.scale(i * 0.35));
            com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.LIGHT, q.x, q.y, q.z, 1, 0.12, 0.12, 0.12, 0.01);
        }
        l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.PLAYERS, 0.8f, 1.5f);
        return true;
    }

    static boolean airBubble(ServerPlayer p) {
        LivingEntity t = Wave1Aim.aimedFriend(p, 8.0);
        if (t == null) t = p;
        ServerLevel l = p.serverLevel();
        Fx.addEffect(t, "minecraft:water_breathing", 6000, 0);
        t.getPersistentData().putLong(K_AIR, l.getGameTime() + 6000);
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.LIGHT, t.getX(), t.getEyeY(), t.getZ(), 20, 0.3, 0.3, 0.3, 0.02);
        Fx.say(p, "A bubble of air holds round the head for 5 minutes, or until it takes a hit.");
        return true;
    }

    // ================================================================ Deliverance
    static boolean divineSmite(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        long until = l.getGameTime() + 160;
        p.getPersistentData().putLong(K_SMITE, until);
        p.getPersistentData().putLong(K_FLAME, until);
        Fx.lendSkill(p, SK_STRENGTHEN_BODY, 160);                                      // the smite fills the body with a righteous strength
        FxRelic.ring(l, p.position().add(0.0, 0.3, 0.0), 1.4, 0xFFFFE9A0);
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.LIGHT, p.getX(), p.getY() + 1.0, p.getZ(), 14, 0.4, 0.7, 0.4, 0.03);
        Fx.say(p, "Divine Smite: your next blow burns with light, and the sacred flame will answer for 8 s (sneak + use).");
        return true;
    }

    static boolean sacredFlame(ServerPlayer p) {
        long now = p.level().getGameTime();
        if (p.getPersistentData().getLong(K_FLAME) < now) {
            Fx.say(p, "Smite first: the sacred flame answers only after a Divine Smite.");
            return false;
        }
        LivingEntity t = Wave1Aim.aimedFoe(p, 16.0);
        if (t == null) {
            Fx.say(p, "The flame needs a foe in sight.");
            return false;
        }
        ServerLevel l = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.LIGHT, t.getX(), t.getY() + 0.2, t.getZ(), 18, 0.25, 0.9, 0.25, 0.03);
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.LIGHT, t.getX(), t.getY() + 1.0, t.getZ(), 10, 0.25, 0.9, 0.25, 0.03);
        Fx.damage(p, t, Wave1Aim.radiant(t, 4.0f), Wave1Aim.undead(t) ? HOLY : LIGHT);   // Tensura's holy damage on the dead
        t.igniteForSeconds(2.0f);
        return true;
    }

    // ================================================================ Book of Mispells
    private static LivingEntity anyAimed(ServerPlayer p, double range) {
        LivingEntity t = Wave1Aim.aimedFoe(p, range);
        return t != null ? t : Wave1Aim.aimedFriend(p, range);
    }

    private static boolean reveal(ServerPlayer p, int index) {
        Fx.say(p, Wave1Words.MISSPELLS[index].reveal());
        return true;
    }

    static boolean massage(ServerPlayer p) {
        LivingEntity t = anyAimed(p, 20.0);
        if (t == null) t = p;
        Fx.addEffect(t, "minecraft:regeneration", 160, 0);
        t.heal(2.0f);
        if (t instanceof Mob m) m.setTarget(null);
        com.newuniverse.nusmp.particle.fx.Fx.spray(((ServerLevel) p.level()), FxThemes.RELIC_WAVE_1, FxTone.LIGHT, t.getX(), t.getEyeY() + 0.3, t.getZ(), 6, 0.3, 0.2, 0.3, 0.0);
        return reveal(p, 0);
    }

    static boolean mageArmoir(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        Vec3 flat = new Vec3(p.getLookAngle().x, 0.0, p.getLookAngle().z);
        flat = flat.lengthSqr() < 1.0e-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        Vec3 at = p.position().add(flat.scale(2.0));
        ArmorStand stand = new ArmorStand(l, at.x, at.y, at.z);
        stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        stand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
        stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        stand.setCustomName(Component.literal("Fancy Wardrobe"));
        stand.setCustomNameVisible(true);
        stand.setInvulnerable(true);
        stand.setShowArms(true);
        stand.setYRot(p.getYRot() + 180.0f);
        stand.getPersistentData().putLong(K_EXPIRE, l.getGameTime() + 600);
        l.addFreshEntity(stand);
        SpellRuntime.later(l, 600, stand::discard);
        Fx.shield(p, 4.0f, 600);
        Fx.lendSkill(p, SK_BODY_ARMOR, 600);                                           // the wardrobe lends a coat of Tensura's body armour for 30 s
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.LIGHT, at.x, at.y + 1.0, at.z, 16, 0.3, 0.6, 0.3, 0.02);
        return reveal(p, 1);
    }

    static boolean foldPerson(ServerPlayer p) {
        LivingEntity t = Wave1Aim.aimedFoe(p, 14.0);
        if (t == null) {
            Fx.say(p, "Nobody to fold.");
            return false;
        }
        ServerLevel l = p.serverLevel();
        if (t.getBbWidth() > 2.6f || t.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 0.6) {
            com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.DARK, t.getX(), t.getY() + 1.0, t.getZ(), 8, 0.3, 0.4, 0.3, 0.01);
            Fx.say(p, "Fold Person: they would not bend.");                     // the Wisdom save
            return true;
        }
        Fx.addEffect(t, "minecraft:slowness", 60, 6);
        Fx.addEffect(t, "minecraft:weakness", 60, 0);
        Fx.addEffect(t, PARALYSIS, 40, 0);                                             // restrained: Tensura's paralysis where it exists
        if (t instanceof Mob m) {
            m.setTarget(null);
            m.getNavigation().stop();
        }
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.LIGHT, t.getX(), t.getY() + 1.0, t.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
        return reveal(p, 2);
    }

    static boolean speakWithDad(ServerPlayer p) {
        if (p.level().dimension() != Level.OVERWORLD) {
            Fx.say(p, "No signal: Dad is on another plane.");
            return false;
        }
        Fx.addEffect(p, "minecraft:resistance", 1200, 0);
        Fx.lendSkill(p, SK_THOUGHT, 1200);                                             // a minute of thought communication with Dad
        Fx.say(p, Wave1Words.pick(Wave1Words.DAD, p.level().getGameTime() / 20 + p.getUUID().hashCode()));
        return reveal(p, 3);
    }

    static boolean ickStorm(ServerPlayer p) {
        Vec3 at = Wave1Aim.point(p, 16.0);
        ServerLevel l = p.serverLevel();
        UUID who = p.getUUID();
        SpellRuntime.zone(l, 400, 10, age -> {
            ServerPlayer owner = l.getServer().getPlayerList().getPlayer(who);
            if (owner == null || !owner.isAlive()) return;
            com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.LIGHT, at.x, at.y + 3.0, at.z, 14, 3.0, 0.4, 3.0, 0.0);
            com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.DARK, at.x, at.y + 0.3, at.z, 4, 3.0, 0.1, 3.0, 0.0);
            for (LivingEntity e : Wave1Aim.foesAt(owner, at, 4.0)) Fx.addEffect(e, "minecraft:slowness", 30, 1);
        });
        return reveal(p, 4);
    }

    static boolean telepoot(ServerPlayer p) {
        Vec3 at = Wave1Aim.point(p, 18.0);
        ServerLevel l = p.serverLevel();
        l.playSound(null, at.x, at.y, at.z, SoundEvents.PUFFER_FISH_BLOW_OUT, SoundSource.PLAYERS, 0.7f, 0.6f);
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.LIGHT, at.x, at.y + 0.2, at.z, 6, 0.15, 0.1, 0.15, 0.01);
        return reveal(p, 5);
    }

    static boolean wash(ServerPlayer p) {
        List<MobEffectInstance> harmful = new ArrayList<>();
        for (MobEffectInstance inst : p.getActiveEffects()) if (inst.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) harmful.add(inst);
        for (MobEffectInstance inst : harmful) p.removeEffect(inst.getEffect());
        p.clearFire();
        ServerLevel l = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.LIGHT, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.4, 0.7, 0.4, 0.05);
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_1, FxTone.LIGHT, p.getX(), p.getY() + 1.0, p.getZ(), 14, 0.4, 0.7, 0.4, 0.02);
        return reveal(p, 6);
    }

    /** A conjured wardrobe that outlived its time (the server stopped first) is not allowed back. */
    static boolean expiredWardrobe(net.minecraft.world.entity.Entity e, long now) {
        return e instanceof ArmorStand && e.getPersistentData().contains(K_EXPIRE) && e.getPersistentData().getLong(K_EXPIRE) < now;
    }
}
