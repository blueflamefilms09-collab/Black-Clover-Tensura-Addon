package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the casts of the pages that {@link CharacterPagesGold} appends (docs/magic/character_spells_gold.md). Each one is a plain {@link BookPage.Cast}: the server decides everything (targets,
 * damage, effects) with the helpers every other page uses, and the picture is a LAYERED effect of existing {@link VfxShape}s (three to six shapes per cast: a core, a trail or lane, an impact, a
 * ground or air accent) in the character's own palette, sent to nearby players only through {@link VfxSpawn} (inside the packet budget). The fire pages are cast by the fire casting styles of
 * Mereoleona (punch combo) and Leopold (wound spiral) with no extra code, because the style wraps every Fire family page.
 *
 * <p>Worst case per cast (the doc repeats this): at most 9 effect packets (the heaviest are the staggered volleys), at most one zone per cast that lasts at most 20 s and runs every 5 to 20 ticks, at
 * most 8 live targets hurt or moved per step ({@link #MAX_HITS}), at most 4 enemy projectiles removed per step, no entity is spawned, no chunk is loaded, no block is placed, no per-tick world scan
 * outside a zone step. The cooldowns are 10 to 120 s. Effects that no source described precisely are marked "inferred" in the doc: the numbers are this mod's, the names and the intent the series'.
 */
public final class CharacterArtsGold {
    private CharacterArtsGold() {}

    private static final int MAX_HITS = 8, MAX_SHOTS = 4;

    // signature palettes (ARGB); the same colour is passed to every layer of one cast so the layers read as one spell
    private static final int CRIMSON = 0xFFE0401C, EMBER = 0xFFFFB347, LEO_RED = 0xFFC8321E, LEO_GOLD = 0xFFFFD27A;
    private static final int PINK = 0xFFF2A0C8, LEAF = 0xFFA8E070, BLOOM_GOLD = 0xFFF8E080;
    private static final int EMERALD = 0xFF3CE08A, BARK = 0xFF8A6238;
    private static final int STEEL = 0xFFB8C8D8, SPARK = 0xFFFFF0A0, THUNDER = 0xFFA8D8FF;
    private static final int TEAL = 0xFF5AE0F0, VOID = 0xFF8A5AE0;
    private static final int MOON = 0xFFC8D4FF, MOON_RED = 0xFFD0384A, NIGHT = 0xFF2A3E8E;
    private static final int VIOLET = 0xFF9A6AE0;

    private static boolean gone(ServerPlayer p) { return p.isRemoved() || !p.isAlive(); }

    private static void fx(ServerPlayer p, VfxShape shape, Vec3 from, Vec3 to, int color, int ticks, float power) {
        VfxSpawn.send(p.serverLevel(), shape, from, to, color, ticks, power);
    }

    private static void follow(ServerPlayer p, Entity who, VfxShape shape, Vec3 to, int color, int ticks, float power) {
        VfxSpawn.sendFollowing(p.serverLevel(), shape, who, to, color, ticks, power);
    }

    private static void push(LivingEntity t, Vec3 dir, double h, double up) {
        t.setDeltaMovement(dir.x * h, up, dir.z * h);
        t.hurtMarked = true;
    }

    /** The end of a straight line of length {@code len}, stopped by the first wall. */
    private static Vec3 reach(ServerPlayer p, Vec3 from, Vec3 dir, double len) {
        Vec3 end = from.add(dir.scale(len));
        return p.level().clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getLocation();
    }

    /** A direction turned about the vertical axis. */
    private static Vec3 turn(Vec3 v, double degrees) {
        double a = Math.toRadians(degrees), c = Math.cos(a), s = Math.sin(a);
        return new Vec3(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
    }

    /** Takes a few enemy projectiles (spells in flight) out of the air around a point: bounded per step. */
    private static void swallowShots(ServerPlayer p, Vec3 at, double r) {
        SpellRuntime.dissolveBolts(p.serverLevel(), at, r, p, MAX_SHOTS);
        int n = 0;
        for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, new AABB(at, at).inflate(r))) {
            if (n >= MAX_SHOTS) break;
            if (pr.getOwner() == p || (pr.getOwner() instanceof LivingEntity o && o.isAlliedTo(p))) continue;
            pr.discard();
            n++;
        }
    }

    // ================================================================ Mereoleona Vermillion: Flame Magic (the fist)
    /** Calidus Brachium Barrage (Mana Zone): the flames of the fist come at the target from every side, four blasts in 16 ticks. */
    public static boolean brachiumBarrage(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        LivingEntity t = GrimoireBook.target(p, 14);
        Vec3 c = t != null ? t.getBoundingBox().getCenter() : GrimoireBook.aim(p, 8);
        b.castCircle(p, 1.2f);
        for (int k = 0; k < 4; k++) {
            final int step = k;
            SpellRuntime.later(p.serverLevel(), k * 4, () -> {
                if (gone(p)) return;
                double a = step * Math.PI / 2 + p.getRandom().nextDouble() * 0.5;
                Vec3 from = c.add(Math.cos(a) * 5, 1.0 + (step % 2), Math.sin(a) * 5);
                fx(p, VfxShape.FLAME_TRAIL, from, c, CRIMSON, 8, 1.0f * s);
                int n = 0;
                for (LivingEntity e : GrimoireBook.around(p, c, 2.4 * s)) {
                    if (n++ >= MAX_HITS) break;
                    b.hurt(i, p, e, mode, 5f);
                    e.igniteForSeconds(3);
                }
            });
        }
        SpellRuntime.later(p.serverLevel(), 16, () -> { if (!gone(p)) fx(p, VfxShape.FIRE_BURST, c, c.add(0, 1, 0), EMBER, 18, 1.5f * s); });
        fx(p, VfxShape.FLAMEFX_ZONE, c, c.add(0, 1, 0), CRIMSON, 24, 1.4f * s);
        return true;
    }

    /** The body of flame the Purgatory family shares: strength, fire immunity and a ring of fire that burns what stands next to you, every second. */
    private static void flameBody(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, int ticks, int strength, double radius, float dps, float power, int speed) {
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, strength));
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks + 40, 0));
        if (speed > 0) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, speed - 1));
        follow(p, p, VfxShape.FLAMEFX_ZONE, p.position().add(0, 1, 0), CRIMSON, ticks, power);
        follow(p, p, VfxShape.SPIRIT_AURA, p.position().add(0, 1, 0), EMBER, ticks, power * 0.8f);
        SpellRuntime.zone(p.serverLevel(), ticks, 20, age -> {
            if (gone(p)) return;
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, p.position(), radius)) {
                if (n++ >= MAX_HITS) break;
                b.hurt(i, p, t, mode, dps);
                t.igniteForSeconds(3);
            }
            if (age % 100 == 0) fx(p, VfxShape.FLAMEFX_BURST, p.position().add(0, 0.2, 0), p.position().add(0, 1, 0), EMBER, 14, (float) radius * 0.5f);
        });
    }

    /** Calidus Brachium Purgatory: she becomes the flame: 20 s of Strength II, fire immunity and a ring of fire that burns what stands within 3.5 blocks. */
    public static boolean purgatory(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        b.castCircle(p, 1.4f);
        flameBody(b, i, p, mode, 400, 1, 3.5 * s, 3f, 1.1f, 0);
        fx(p, VfxShape.FIRE_PILLAR, p.position(), p.position().add(0, 1, 0), CRIMSON, 30, 1.3f * s);
        return true;
    }

    /** Calidus Brachium Purgatory: Abyss: the deeper form: 15 s of Strength III, a wider and hotter ring and a shock of flame that throws everything within 7 blocks back when it starts. */
    public static boolean purgatoryAbyss(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 c = p.position();
        b.castCircle(p, 1.8f);
        int n = 0;
        for (LivingEntity t : GrimoireBook.around(p, c, 7 * s)) {
            if (n++ >= MAX_HITS) break;
            b.hurt(i, p, t, mode, 14f);
            t.igniteForSeconds(5);
            push(t, t.position().subtract(c).normalize(), 1.4, 0.4);
        }
        fx(p, VfxShape.FX_FIRE_ERUPTION, c, c.add(0, 1, 0), CRIMSON, 28, 1.5f * s);
        flameBody(b, i, p, mode, 300, 2, 4.5 * s, 5f, 1.4f, 1);
        return true;
    }

    /** Hellfire Incarnate: her whole body turns to mana and flame: 12 s of Strength III, Speed II, absorption and a ring of 6 blocks that burns everything in it, with an eruption at the start. */
    public static boolean hellfireIncarnate(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 c = p.position();
        b.castCircle(p, 2.0f);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 240, 2));
        int n = 0;
        for (LivingEntity t : GrimoireBook.around(p, c, 9 * s)) {
            if (n++ >= MAX_HITS) break;
            b.hurt(i, p, t, mode, 20f);
            t.igniteForSeconds(8);
            push(t, t.position().subtract(c).normalize(), 1.6, 0.5);
        }
        fx(p, VfxShape.FX_FIRE_ERUPTION, c, c.add(0, 1, 0), CRIMSON, 34, 2.0f * s);
        fx(p, VfxShape.FIRE_PILLAR, c, c.add(0, 1, 0), LEO_GOLD, 40, 2.0f * s);
        flameBody(b, i, p, mode, 240, 2, 6 * s, 6f, 1.8f, 2);
        return true;
    }

    // ================================================================ Leopold Vermillion: Flame Magic (the spiral)
    /** Deep Spiral Flame: spirals of flame leave him in four directions at once, 9 blocks each, burning what they cross. */
    public static boolean deepSpiralFlame(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 base = p.position().add(0, 1.1, 0), fwd = TreeKit.forward(p);
        Set<LivingEntity> hit = new HashSet<>();
        b.castCircle(p, 1.3f);
        for (int k = 0; k < 4; k++) {
            Vec3 dir = turn(fwd, 45 + 90 * k), end = base.add(dir.scale(9 * s));
            for (LivingEntity t : GrimoireBook.along(p, base, end, 1.8)) {
                if (hit.size() >= MAX_HITS) break;
                if (!hit.add(t)) continue;
                b.hurt(i, p, t, mode, 10f);
                t.igniteForSeconds(4);
                push(t, dir, 0.5, 0.25);
            }
            if (k == 0) b.vfx(p, VfxShape.FIRE_SPIRAL, base, end, 22, 1.4f * s);
            else fx(p, VfxShape.FIRE_SPIRAL, base, end, LEO_RED, 22, 1.4f * s);
        }
        fx(p, VfxShape.FIRE_BURST, p.position(), p.position().add(0, 1, 0), LEO_GOLD, 18, 1.2f * s);
        return true;
    }

    /** True Flame Magic: Crimson Eruption: a formula is written wide over the ground where you aim and, a second later, the whole circle erupts. */
    public static boolean crimsonEruption(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 c = GrimoireBook.aim(p, 26);
        double r = 7 * s;
        b.castCircle(p, 1.6f);
        fx(p, VfxShape.FLAMEFX_ZONE, c, c.add(0, 1, 0), LEO_RED, 30, (float) r);
        SpellRuntime.later(p.serverLevel(), 24, () -> {
            if (gone(p)) return;
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (n++ >= MAX_HITS) break;
                b.hurt(i, p, t, mode, 22f);
                t.igniteForSeconds(6);
                push(t, new Vec3(0, 0, 0), 0, 0.9);
            }
            fx(p, VfxShape.FX_FIRE_ERUPTION, c, c.add(0, 1, 0), LEO_RED, 30, (float) r * 0.3f);
            fx(p, VfxShape.FIRE_PILLAR, c, c.add(0, 1, 0), LEO_GOLD, 36, (float) r * 0.3f);
            fx(p, VfxShape.FLAMEFX_BURST, c, c.add(0, 1, 0), EMBER, 24, (float) r * 0.4f);
        });
        return true;
    }

    // ================================================================ Mimosa Vermillion: Plant Magic (healing flowers)
    /** Magic Flower Cannon: a giant flower opens in front of her and fires a laser of pure mana up to 24 blocks. */
    public static boolean flowerCannon(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), dir = p.getViewVector(1f), end = reach(p, eye, dir, 24 * s);
        Vec3 mouth = p.position().add(TreeKit.forward(p).scale(1.8)).add(0, 1.5, 0);
        b.castCircle(p, 1.0f);
        fx(p, VfxShape.PLANT_FX2, mouth, mouth.add(0, 1, 0), PINK, 18, 1.4f);
        fx(p, VfxShape.LIGHT_FLARE, mouth, mouth, BLOOM_GOLD, 14, 1.0f);
        fx(p, VfxShape.PLANT_FX1, mouth, end, LEAF, 14, 1.3f * s);
        int n = 0;
        for (LivingEntity t : GrimoireBook.along(p, eye, end, 1.5)) {
            if (n++ >= MAX_HITS) break;
            b.hurt(i, p, t, mode, 14f);
            push(t, dir, 0.6, 0.2);
        }
        fx(p, VfxShape.PLANT_FX3, end, end.add(0, 1, 0), PINK, 22, 1.2f);
        return true;
    }

    /** The one you look at (an ally or a player) or, failing that, yourself. */
    private static LivingEntity mendTarget(ServerPlayer p, double range) {
        LivingEntity t = GrimoireBook.target(p, range);
        return t != null && (t instanceof Player || t.isAlliedTo(p)) ? t : p;
    }

    /** Dream-Healing Flower Cradle: a cradle of vines and flowers closes round an ally (or her): four pulses of 12 percent of the maximum health in 1.6 s and the poison taken out. */
    public static boolean healingCradle(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = mendTarget(p, 16);
        b.castCircle(p, 1.0f);
        fx(p, VfxShape.PLANT_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), LEAF, 14, 0.9f);
        follow(p, t, VfxShape.PLANT_FX2, t.position().add(0, 1, 0), PINK, 50, 2.2f);
        follow(p, t, VfxShape.PLANT_FX3, t.position().add(0, 1, 0), BLOOM_GOLD, 50, 1.2f);
        t.removeEffect(MobEffects.POISON);
        t.removeEffect(MobEffects.WITHER);
        SpellRuntime.zone(p.serverLevel(), 40, 10, age -> {
            if (gone(p) || !t.isAlive()) return;
            BalanceLaw.heal(t, t.getMaxHealth() * 0.12f);
        });
        return true;
    }

    /** Princess-Healing Flower Robe: a robe of flowers on an ally (or her) for 20 s: Regeneration II, absorption and a little toughness. */
    public static boolean healingRobe(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = mendTarget(p, 16);
        b.castCircle(p, 1.0f);
        t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
        t.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1));
        t.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 0));
        fx(p, VfxShape.PLANT_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), PINK, 14, 0.8f);
        follow(p, t, VfxShape.PLANT_FX3, t.position().add(0, 1, 0), PINK, 160, 1.4f);
        follow(p, t, VfxShape.PLANT_FX2, t.position().add(0, 0.1, 0), LEAF, 60, 1.6f);
        return true;
    }

    /** Flower Princess Utopia: a garden of flowers opens round her for 14 s: allies inside mend 8 percent of their health a second, a fatal wound is closed once (40 percent), foes are slowed. */
    public static boolean princessUtopia(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 c = p.position();
        double r = 8 * s;
        Set<UUID> saved = new HashSet<>();
        b.castCircle(p, 2.0f);
        fx(p, VfxShape.PLANT_FX2, c, c.add(0, 1, 0), PINK, 280, (float) r);
        fx(p, VfxShape.PLANT_FX3, c, c.add(0, 2, 0), BLOOM_GOLD, 280, (float) r * 0.6f);
        fx(p, VfxShape.TREE_FX2, c, c.add(0, 1, 0), LEAF, 120, (float) r * 0.5f);
        fx(p, VfxShape.LIGHT_FLARE, c.add(0, 6, 0), c.add(0, 6, 0), BLOOM_GOLD, 40, 2.0f);
        SpellRuntime.zone(p.serverLevel(), 280, 20, age -> {
            if (gone(p)) return;
            int n = 0;
            for (LivingEntity a : TreeKit.friends(p, c, r)) {
                if (n++ >= MAX_HITS) break;
                BalanceLaw.heal(a, a.getMaxHealth() * 0.08f);
                if (a.getHealth() < a.getMaxHealth() * 0.25f && saved.add(a.getUUID())) BalanceLaw.heal(a, a.getMaxHealth() * 0.4f);
            }
            n = 0;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (n++ >= MAX_HITS) break;
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            }
        });
        return true;
    }

    // ================================================================ William Vangeance: World Tree Magic
    /** Magic Warding Ash: a great ash tree stands over you for 8 s and its branches bind the enemy spells that fly at you (up to 4 a step, 6 blocks). */
    public static boolean wardingAsh(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 c = p.position();
        b.castCircle(p, 1.3f);
        fx(p, VfxShape.TREE_CANOPY, c, c.add(0, 1, 0), EMERALD, 170, 4f * s);
        fx(p, VfxShape.WORLD_TREE_FX2, c, c.add(0, 1, 0), EMERALD, 170, 5f * s);
        fx(p, VfxShape.TREE_ROOTS, c, c.add(0, 1, 0), BARK, 40, 5f * s);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0));
        SpellRuntime.zone(p.serverLevel(), 160, 5, age -> {
            if (gone(p)) return;
            swallowShots(p, p.position().add(0, 1, 0), 6 * s);
        });
        return true;
    }

    /** Great Tree Mistilteinn: world-tree roots spread 9 blocks round you for 6 s, hold the foes in them and drink their mana for you; the last pulse roots them. */
    public static boolean greatTreeMistilteinn(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 c = p.position();
        double r = 9 * s;
        b.castCircle(p, 1.6f);
        fx(p, VfxShape.TREE_CANOPY, c, c.add(0, 1, 0), EMERALD, 130, 6f * s);
        fx(p, VfxShape.TREE_ROOTS, c, c.add(0, 1, 0), BARK, 130, (float) r);
        fx(p, VfxShape.WORLD_TREE_FX1, c, c.add(0, 1, 0), EMERALD, 130, (float) r * 0.7f);
        SpellRuntime.zone(p.serverLevel(), 120, 20, age -> {
            if (gone(p)) return;
            boolean last = age >= 100;
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (n++ >= MAX_HITS) break;
                b.hurt(i, p, t, mode, 3f);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, last ? 5 : 2));
                if (n <= 3) {
                    EnergyBridge.drain(t, p, 0.01);
                    if (age == 0) fx(p, VfxShape.TREE_DRAIN, t.getBoundingBox().getCenter(), p.getBoundingBox().getCenter(), EMERALD, 40, 0.8f);
                }
            }
            if (n > 0) BalanceLaw.heal(p, p.getMaxHealth() * 0.01f * Math.min(n, 3));
        });
        return true;
    }

    /** Mistilteinn Blade: a blade of world-tree wood cuts a wide arc 8 blocks long (11 damage, up to 8 foes); the sap that is cut mends you a little. */
    public static boolean mistilteinnBlade(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), dir = p.getViewVector(1f), end = reach(p, eye, dir, 8 * s);
        b.castCircle(p, 0.9f);
        int n = 0;
        Vec3 first = null;
        for (LivingEntity t : GrimoireBook.along(p, eye, end, 2.0)) {
            if (n++ >= MAX_HITS) break;
            b.hurt(i, p, t, mode, 11f);
            push(t, dir, 0.5, 0.15);
            if (first == null) first = t.getBoundingBox().getCenter();
        }
        b.vfx(p, VfxShape.WORLD_TREE_FX1, eye.add(0, -0.4, 0), end, 14, 1.3f * s);
        fx(p, VfxShape.TREE_FX3, end, end.add(0, 1, 0), EMERALD, 18, 1.0f);
        if (first != null) {
            fx(p, VfxShape.TREE_DRAIN, first, p.getBoundingBox().getCenter(), EMERALD, 20, 0.6f);
            BalanceLaw.heal(p, 2f);
        }
        return true;
    }

    // ================================================================ Klaus Lunettes: Steel Magic
    /** Blazing Piercing Lance: a lance of steel flies up to 29 blocks through everything in its line (14 damage). */
    public static boolean piercingLance(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 1.0f);
        fx(p, VfxShape.WEAPON_CONSTRUCTS, eye.add(dir.scale(1.2)), eye.add(dir.scale(3)), STEEL, 12, 0.9f);
        fx(p, VfxShape.THREAD_LINE, eye, eye.add(dir.scale(29)), STEEL, 12, 1.5f * s);
        SpellRuntime.bolt(p, eye, dir.scale(2.4), 0.7 * s, 12, true, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 14f);
            t.knockback(0.6, -bolt.vel.x, -bolt.vel.z);
        }, (bolt, at) -> fx(p, VfxShape.MAGIC_CIRCLE_EXPLOSION, at, at.add(0, 1, 0), STEEL, 16, 0.8f));
        return true;
    }

    /** Steel Castle's Armored Wall: a wall of steel rises round you for 7 s: Resistance II, absorption, and the enemy spells that reach it are stopped. */
    public static boolean castleWall(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = p.position(), side = TreeKit.forward(p), left = new Vec3(-side.z, 0, side.x);
        b.castCircle(p, 1.2f);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 140, 1));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 140, 1));
        fx(p, VfxShape.WEAPON_CONSTRUCTS, c, c.add(0, 2, 0), STEEL, 140, 3.5f);
        fx(p, VfxShape.FX_EARTH_SPIKES, c, c.add(0, 1, 0), STEEL, 40, 3f);
        for (int k = -1; k <= 1; k += 2) {
            Vec3 from = c.add(side.scale(2.5)).add(left.scale(k * 1.6));
            fx(p, VfxShape.THREAD_LINE, from, from.add(0, 4.5, 0), SPARK, 60, 1.1f);
        }
        SpellRuntime.zone(p.serverLevel(), 140, 5, age -> {
            if (gone(p)) return;
            swallowShots(p, p.position().add(0, 1, 0), 4.5);
        });
        return true;
    }

    /** Violent Spiral Lance: a drill of steel turns through everything on a line of 22 blocks (16 damage, thrown up and held for a moment). */
    public static boolean spiralLance(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 1.2f);
        fx(p, VfxShape.WEAPON_CONSTRUCTS, eye.add(dir.scale(1.2)), eye.add(dir.scale(3.5)), STEEL, 14, 1.1f);
        b.vfx(p, VfxShape.THREAD_LINE, eye, eye.add(dir.scale(22)), 14, 2.0f * s);
        fx(p, VfxShape.WIND_GALE, eye, eye.add(dir.scale(22)), STEEL, 14, 1.2f * s);
        SpellRuntime.bolt(p, eye, dir.scale(2.0), 1.0 * s, 11, true, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 16f);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3));
            t.setDeltaMovement(t.getDeltaMovement().add(0, 0.5, 0));
            t.hurtMarked = true;
        }, (bolt, at) -> {
            fx(p, VfxShape.EARTH_SPIKES, at, at.add(0, 1, 0), STEEL, 20, 1.2f);
            fx(p, VfxShape.MAGIC_CIRCLE_EXPLOSION, at, at.add(0, 1, 0), SPARK, 16, 0.9f);
        });
        return true;
    }

    /** Thunder Arrow (with Luck's lightning and Puli's wings): a steel arrow that crosses 40 blocks at once, pierces everything (20 damage, stunned 1.5 s) and cuts spells; it cannot be steered and leaves Klaus slowed a moment. */
    public static boolean thunderArrow(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), dir = p.getViewVector(1f), end = reach(p, eye, dir, 40 * s);
        b.castCircle(p, 1.4f);
        CanonSpells.cutSpells(p, new AABB(eye, end).inflate(1.5));
        int n = 0;
        for (LivingEntity t : GrimoireBook.along(p, eye, end, 1.3)) {
            if (n++ >= MAX_HITS) break;
            b.hurt(i, p, t, mode, 20f);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3));
            push(t, dir, 0.9, 0.2);
        }
        fx(p, VfxShape.LIGHTNING_SPEAR, eye, end, THUNDER, 12, 1.6f * s);
        fx(p, VfxShape.THREAD_LINE, eye, end, STEEL, 14, 1.2f * s);
        fx(p, VfxShape.FX_LIGHTNING_ARC, end, end.add(0, 1, 0), THUNDER, 20, 1.4f);
        fx(p, VfxShape.LIGHTNINGFX_BURST, end, end.add(0, 1, 0), SPARK, 20, 1.2f);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        return true;
    }

    // ================================================================ Langris Vaude: Spatial Magic
    /** Archangel's Crash: a stroke of space that removes whatever it touches along with the space itself: 18 blocks, 16 damage, enemy spells on the line cut away. */
    public static boolean archangelCrash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), dir = p.getViewVector(1f), end = reach(p, eye, dir, 18 * s);
        b.castCircle(p, 1.2f);
        CanonSpells.cutSpells(p, new AABB(eye, end).inflate(1.5));
        int n = 0;
        for (LivingEntity t : GrimoireBook.along(p, eye, end, 1.6)) {
            if (n++ >= MAX_HITS) break;
            b.hurt(i, p, t, mode, 16f);
            push(t, dir, 0.5, 0.2);
        }
        fx(p, VfxShape.SPATIAL_FX1, eye, eye.add(dir.scale(2)), TEAL, 18, 1.0f);
        b.vfx(p, VfxShape.SPACE_CRACK, eye, end, 16, 1.5f * s);
        fx(p, VfxShape.SPACE_TEAR, end, end.add(0, 1, 0), VOID, 22, 1.3f);
        return true;
    }

    /** Archangel's Annihilation (inferred from the name: the greater form): three strokes of space fan out over 12 ticks, 12 damage each. */
    public static boolean archangelAnnihilation(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        b.castCircle(p, 1.8f);
        fx(p, VfxShape.SPACE_RINGGATE, eye.add(look.scale(2)), eye.add(look.scale(3)), TEAL, 40, 1.6f);
        for (int k = 0; k < 3; k++) {
            final Vec3 dir = turn(look, (k - 1) * 22);
            final int idx = k;
            SpellRuntime.later(p.serverLevel(), k * 6, () -> {
                if (gone(p)) return;
                Vec3 from = p.getEyePosition(), end = reach(p, from, dir, 20 * s);
                CanonSpells.cutSpells(p, new AABB(from, end).inflate(1.5));
                int n = 0;
                for (LivingEntity t : GrimoireBook.along(p, from, end, 1.6)) {
                    if (n++ >= MAX_HITS) break;
                    b.hurt(i, p, t, mode, 12f);
                    push(t, dir, 0.5, 0.2);
                }
                fx(p, VfxShape.SPACE_CRACK, from, end, idx == 1 ? VOID : TEAL, 14, 1.6f * s);
            });
        }
        Vec3 mid = reach(p, eye, look, 20 * s);
        SpellRuntime.later(p.serverLevel(), 14, () -> { if (!gone(p)) fx(p, VfxShape.SPACE_SHATTER, mid, mid.add(0, 1, 0), VOID, 24, 1.8f); });
        return true;
    }

    /** Spatial Auto-Guard (after the elf awakening): space itself deflects what comes at you for 12 s: enemy spells within 5 blocks are taken out of the air. */
    public static boolean spatialAutoGuard(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.2f);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 240, 0));
        follow(p, p, VfxShape.SPACE_RINGGATE, p.position().add(0, 1, 0), TEAL, 240, 1.8f);
        follow(p, p, VfxShape.SPATIAL_FX2, p.position().add(0, 1, 0), VOID, 240, 1.4f);
        fx(p, VfxShape.SPACE_CUBE, p.position(), p.position().add(0, 1, 0), TEAL, 30, 1.2f);
        SpellRuntime.zone(p.serverLevel(), 240, 5, age -> {
            if (gone(p)) return;
            swallowShots(p, p.position().add(0, 1, 0), 5);
        });
        return true;
    }

    // ================================================================ Karna Freese: Moonlight Magic
    /** Crescent Cut: three crescents of moonlight fan out from the hand, 14 blocks each (7 damage each). */
    public static boolean crescentCut(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        b.castCircle(p, 1.0f);
        fx(p, VfxShape.LIGHT_FLARE, eye.add(look.scale(1.2)), eye.add(look.scale(1.2)), MOON, 12, 0.9f);
        Vec3 last = eye;
        for (int k = -1; k <= 1; k++) {
            Vec3 dir = turn(look, k * 14), end = reach(p, eye, dir, 14 * s);
            int n = 0;
            for (LivingEntity t : GrimoireBook.along(p, eye, end, 1.0)) {
                if (n++ >= MAX_HITS) break;
                b.hurt(i, p, t, mode, 7f);
            }
            fx(p, VfxShape.LIGHT_BLADE, eye.add(0, -0.3, 0), end, k == 0 ? MOON : MOON_RED, 12, 1.0f * s);
            if (k == 0) last = end;
        }
        fx(p, VfxShape.LIGHT_FX1, last, last.add(0, 1, 0), MOON, 18, 1.0f);
        return true;
    }

    /** Bewitching Moonlight: the moonlight settles on the one you look at (and up to two foes beside it) and takes hold of the body: held for 3 s, weakened for 6 s. */
    public static boolean bewitchingMoonlight(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null) { GrimoireBook.fail(p, "No one in the moonlight."); return false; }
        b.castCircle(p, 1.2f);
        fx(p, VfxShape.LIGHT_BLADE, p.getEyePosition(), t.getBoundingBox().getCenter(), MOON, 10, 0.5f);
        int n = 0;
        for (LivingEntity e : GrimoireBook.around(p, t.position(), 5)) {
            if (e != t && n++ >= 2) continue;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 6));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 2));
            follow(p, e, VfxShape.LIGHT_FX2, e.position().add(0, 1, 0), MOON, 60, 1.3f);
            if (e == t) follow(p, e, VfxShape.SPIRIT_AURA, e.position().add(0, 1, 0), MOON_RED, 60, 1.0f);
        }
        return true;
    }

    /** Full Moon Shell: a sphere of dark moon-mana closes round you for 6 s: foes within 3.8 blocks are knocked away every half second and enemy spells are stopped. */
    public static boolean fullMoonShell(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.2f);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0));
        follow(p, p, VfxShape.MAGIC_CIRCLE_HOLD, p.position().add(0, 1, 0), NIGHT, 120, 1.6f);
        follow(p, p, VfxShape.SPIRIT_AURA, p.position().add(0, 1, 0), NIGHT, 120, 1.5f);
        fx(p, VfxShape.LIGHT_FX3, p.position(), p.position().add(0, 1, 0), MOON, 24, 1.8f);
        SpellRuntime.zone(p.serverLevel(), 120, 10, age -> {
            if (gone(p)) return;
            swallowShots(p, p.position().add(0, 1, 0), 3.8);
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, p.position(), 3.8)) {
                if (n++ >= MAX_HITS) break;
                push(t, t.position().subtract(p.position()).normalize(), 0.9, 0.2);
            }
        });
        return true;
    }

    /** Crescent Cut Full Moon: the shell closes and then fires crescents erratically from every side of it: five of them over 40 ticks, 6 damage each. */
    public static boolean crescentCutFullMoon(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        b.castCircle(p, 1.4f);
        follow(p, p, VfxShape.MAGIC_CIRCLE_HOLD, p.position().add(0, 1, 0), NIGHT, 60, 1.5f);
        fx(p, VfxShape.LIGHT_FX3, p.position(), p.position().add(0, 1, 0), MOON_RED, 24, 1.6f);
        SpellRuntime.zone(p.serverLevel(), 40, 8, age -> {
            if (gone(p)) return;
            Vec3 from = p.position().add(0, 1.2, 0), dir = turn(new Vec3(1, 0, 0), p.getRandom().nextDouble() * 360), end = reach(p, from, dir, 12 * s);
            int n = 0;
            for (LivingEntity t : GrimoireBook.along(p, from, end, 1.0)) {
                if (n++ >= MAX_HITS) break;
                b.hurt(i, p, t, mode, 6f);
            }
            fx(p, VfxShape.LIGHT_BLADE, from, end, age % 16 == 0 ? MOON_RED : MOON, 10, 0.9f * s);
        });
        return true;
    }

    /** Lunatic Crisis: she gathers the mana round the red moon for 1.5 s and lets it go as one enormous ray of moonlight, 40 blocks long (26 damage, up to 8 foes). */
    public static boolean lunaticCrisis(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 up = p.position().add(0, 6, 0);
        b.castCircle(p, 2.0f);
        fx(p, VfxShape.LIGHT_FLARE, up, up, MOON_RED, 36, 3.0f);
        SpellRuntime.later(p.serverLevel(), 30, () -> {
            if (gone(p)) return;
            Vec3 eye = p.getEyePosition(), dir = p.getViewVector(1f), end = reach(p, eye, dir, 40 * s);
            CanonSpells.cutSpells(p, new AABB(eye, end).inflate(2));
            int n = 0;
            for (LivingEntity t : GrimoireBook.along(p, eye, end, 2.4)) {
                if (n++ >= MAX_HITS) break;
                b.hurt(i, p, t, mode, 26f);
                push(t, dir, 1.4, 0.3);
            }
            fx(p, VfxShape.LIGHT_BLADE, eye.add(0, -0.3, 0), end, MOON, 18, 3.0f * s);
            fx(p, VfxShape.LIGHT_FX2, eye, end, MOON_RED, 18, 2.0f);
            fx(p, VfxShape.LIGHT_FLARE, end, end, MOON, 24, 2.4f);
        });
        return true;
    }

    // ================================================================ Kaiser Granvorka: Vortex Magic
    /** Vortex Magic: Area Helix: a huge spherical vortex turns where you aim for 8 s: foes inside are dragged to its middle and slowed, enemy spells are swallowed. */
    public static boolean areaHelix(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 c = GrimoireBook.aim(p, 22);
        double r = 8 * s;
        b.castCircle(p, 1.8f);
        fx(p, VfxShape.VORTEX_FX1, c, c.add(0, 1, 0), VIOLET, 170, (float) r);
        fx(p, VfxShape.VORTEX_FX2, c, c.add(0, 1, 0), VIOLET, 170, (float) r * 0.7f);
        fx(p, VfxShape.VORTEX_FX3, c, c.add(0, 1, 0), 0xFFD0C0E0, 170, (float) r * 0.4f);
        fx(p, VfxShape.WIND_RING, c, c.add(0, 1, 0), VIOLET, 30, (float) r);
        SpellRuntime.zone(p.serverLevel(), 160, 5, age -> {
            if (gone(p)) return;
            swallowShots(p, c, r);
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (n++ >= MAX_HITS) break;
                Vec3 to = c.subtract(t.position());
                t.setDeltaMovement(to.x * 0.08, Math.max(0.0, t.getDeltaMovement().y), to.z * 0.08);
                t.hurtMarked = true;
                if (age % 20 == 0) {
                    b.hurt(i, p, t, mode, 3f);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
                }
            }
        });
        return true;
    }
}
