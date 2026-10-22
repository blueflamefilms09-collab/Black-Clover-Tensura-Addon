package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the casts of the pages {@link CharacterPagesRest} appends (docs/magic/character_spells_rest.md). Each one is a plain {@link BookPage.Cast}: the server decides everything (targets, damage,
 * effects) with the helpers every other page uses, and the picture is a LAYERED effect of the existing {@link VfxShape}s (three to five shapes per cast in the character's own colours, so a
 * character's page reads richer than a basic one) sent to nearby players only through {@link VfxSpawn}, which is inside the performance packet budget. No new shape, texture or sound.
 *
 * <p>Worst case per cast (the doc repeats this): at most 7 effect packets (a zone adds one packet per 40 ticks at most), one zone per cast that lasts at most 10 s and runs every 20 ticks, at
 * most 8 live targets hurt per hit loop, no entity is spawned, no chunk is loaded, no block is placed. The cooldowns are 10 to 90 s. The two spells that cost the caster blood (Vanica's, and
 * Gadjah's Ultimate Magic that burns life force) refuse to be cast when the price would leave under 1 heart and charge it through the usual self-damage law.
 * Effects the sources did not describe precisely are marked "inferred" in the doc: the numbers are this mod's, the names and the intent are the series'.
 */
public final class CharacterArtsRest {
    private CharacterArtsRest() {}

    private static final int MAX_HITS = 8;

    // the character palettes (ARGB, like every book colour)
    static final int BLUE_ROSE = 0xFF3A6AD8, BLUE_DEEP = 0xFF1E3A9A, RED_ROSE = 0xFFC8202C;
    static final int PAINT_BLUE = 0xFF3A7BFF, PAINT_FIRE = 0xFFFF5A3A, PAINT_ICE = 0xFF8AE6FF, PAINT_EARTH = 0xFFB0864A, PAINT_WIND = 0xFF7CF0B0, PAINT_GOLD = 0xFFFFC84A;
    static final int BLOOD_RED = 0xFFB01020, BLOOD_DARK = 0xFF5A0A12;
    static final int BOLT_GOLD = 0xFFFFE65A, BOLT_WHITE = 0xFFFFF6C8;
    static final int MINERAL_BLUE = 0xFF78A8C8, MINERAL_PALE = 0xFFD8E8F4;
    static final int SONG_ROSE = 0xFFE8C0F0, SONG_GOLD = 0xFFE8D8A0;

    private static boolean gone(ServerPlayer p) { return p.isRemoved() || !p.isAlive(); }

    private static void fx(ServerPlayer p, VfxShape shape, Vec3 from, Vec3 to, int color, int ticks, float power) {
        VfxSpawn.send(p.serverLevel(), shape, from, to, color, ticks, power);
    }

    private static void push(LivingEntity t, Vec3 dir, double h, double up) {
        t.setDeltaMovement(dir.x * h, up, dir.z * h);
        t.hurtMarked = true;
    }

    /** The price in blood or life force: refused (nothing happens, no cooldown) when it would leave less than one heart. */
    private static boolean pay(ServerPlayer p, float hp, String what) {
        float cost = (float) (hp * com.newuniverse.nusmp.item.MagicGear.selfDamageMult(p));
        if (p.getHealth() <= cost + 2) { GrimoireBook.fail(p, what); return false; }
        p.hurt(p.damageSources().magic(), cost);
        return true;
    }

    // ================================================================ Charlotte Roselei: Briar Magic
    /** Blue Rose Paradise: two concentric magic circles grow briars over a spot where everything is tangled, slowed and weakened; curses are drawn into the blue roses (a curse here is a poison or wither on a foe, which is cleared). */
    public static boolean blueRoseParadise(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 5f * GrimoireBook.size(i, p);
        Vec3 c = GrimoireBook.aim(p, 22);
        b.castCircle(p, 1.1f);
        fx(p, VfxShape.ELF_CIRCLE, c.add(0, 0.05, 0), c.add(0, 1, 0), BLUE_ROSE, 100, r);                // the outer circle
        fx(p, VfxShape.ELF_CIRCLE, c.add(0, 0.08, 0), c.add(0, 1, 0), BLUE_DEEP, 100, r * 0.55f);       // the inner circle: two concentric circles
        fx(p, VfxShape.BRIAR_FX2, c, c.add(0, 1, 0), BLUE_ROSE, 100, r);
        fx(p, VfxShape.BRIAR_FX3, c, c.add(0, 1, 0), BLUE_ROSE, 24, 1.2f);
        SpellRuntime.zone(p.serverLevel(), 100, 20, age -> {
            if (gone(p)) return;
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (n++ >= MAX_HITS) break;
                b.hurt(i, p, t, mode, 3f);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                t.removeEffect(MobEffects.WITHER);                                                       // the blue roses drink the curse
            }
            if (age % 40 == 20) fx(p, VfxShape.BRIAR_FX3, c, c.add(0, 1, 0), BLUE_ROSE, 20, 1.0f);
        });
        return true;
    }

    /** Scarlet-Braided Beautiful Battle Spear (named in the wiki's True Briar list, no description found: effect inferred from the name): red briars braid into a spear that runs 16 blocks through everything on its line. */
    public static boolean scarletBraidedSpear(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 a = p.getEyePosition(), dir = p.getViewVector(1f), end = a.add(dir.scale(16 * s));
        b.castCircle(p, 0.9f);
        int n = 0;
        for (LivingEntity t : GrimoireBook.along(p, a, end, 1.3)) {
            if (n++ >= MAX_HITS) break;
            b.hurt(i, p, t, mode, 13f);
            t.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            push(t, dir, 0.5, 0.2);
        }
        b.vfx(p, VfxShape.BRIAR_FX1, a, end, 18, 1.3f * s);
        fx(p, VfxShape.THREAD_LINE, a, end, RED_ROSE, 16, 1.0f);
        fx(p, VfxShape.BRIAR_FX3, end, end.add(0, 1, 0), RED_ROSE, 22, 1.1f);
        return true;
    }

    // ================================================================ Rill Boismortier: Painting Magic
    /** Souterrain Groundswell: figures riding waves of dirt crash into the target and bury it in earth (slowed and blinded for a moment). */
    public static boolean souterrainGroundswell(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 a = p.position().add(0, 0.3, 0), dir = p.getViewVector(1f).multiply(1, 0, 1).normalize(), end = a.add(dir.scale(13 * s));
        b.castCircle(p, 1f);
        int n = 0;
        for (LivingEntity t : GrimoireBook.along(p, a, end, 2.0)) {
            if (n++ >= MAX_HITS) break;
            b.hurt(i, p, t, mode, 9f);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
            push(t, dir, 0.9, 0.3);
        }
        fx(p, VfxShape.PAINT_STROKE, p.getEyePosition(), end, PAINT_EARTH, 14, 1.2f);
        b.vfx(p, VfxShape.PAINT_TRAIL, a, end, 18, 1.2f * s);
        fx(p, VfxShape.EARTH_RISE, end, end.add(0, 1, 0), PAINT_EARTH, 22, 1.4f);
        return true;
    }

    /** Sirene's Game: mermaids of water crash into the target and drench it (slowed, pushed back, any fire put out). */
    public static boolean sirenesGame(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 a = p.getEyePosition(), dir = p.getViewVector(1f), end = a.add(dir.scale(13 * s));
        b.castCircle(p, 1f);
        int n = 0;
        for (LivingEntity t : GrimoireBook.along(p, a, end, 1.8)) {
            if (n++ >= MAX_HITS) break;
            b.hurt(i, p, t, mode, 9f);
            t.clearFire();
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            push(t, dir, 1.0, 0.25);
        }
        fx(p, VfxShape.PAINT_STROKE, a, end, PAINT_BLUE, 14, 1.2f);
        b.vfx(p, VfxShape.PAINT_BEAST, a.add(dir.scale(2)), end, 20, 0.9f * s);
        fx(p, VfxShape.WATER_SPLASH, end, end.add(0, 1, 0), PAINT_ICE, 20, 1.5f);
        fx(p, VfxShape.WATER_RING, end, end.add(0, 1, 0), PAINT_BLUE, 20, 2.0f);
        return true;
    }

    /** Four-Headed Lindworm: a four-headed serpent painted in the air; each head breathes a different element in a fan (fire burns, ice chills, wind lifts, earth slows). */
    public static boolean fourHeadedLindworm(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f), side = new Vec3(-look.z, 0, look.x).normalize();
        double range = 14 * s;
        b.castCircle(p, 1.2f);
        int n = 0;
        for (LivingEntity t : GrimoireBook.around(p, p.position(), range)) {
            if (n >= MAX_HITS) break;
            Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
            if (to.lengthSqr() < 1e-4 || to.normalize().dot(look) < 0.55) continue;
            n++;
            b.hurt(i, p, t, mode, 8f);
            switch (n % 4) {
                case 0 -> t.igniteForSeconds(3);
                case 1 -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                case 2 -> push(t, to.normalize(), 0.5, 0.6);
                default -> t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            }
        }
        b.vfx(p, VfxShape.PAINT_BEAST, p.position().add(0, 1.5, 0), eye.add(look.scale(range)), 30, 1.3f * s);
        fx(p, VfxShape.PAINT_TRAIL, eye.add(side.scale(-1.2)), eye.add(look.scale(range)).add(side.scale(-3)), PAINT_FIRE, 18, 1.1f);
        fx(p, VfxShape.PAINT_TRAIL, eye.add(side.scale(-0.4)), eye.add(look.scale(range)).add(side.scale(-1)), PAINT_ICE, 18, 1.1f);
        fx(p, VfxShape.PAINT_TRAIL, eye.add(side.scale(0.4)), eye.add(look.scale(range)).add(side.scale(1)), PAINT_WIND, 18, 1.1f);
        fx(p, VfxShape.PAINT_TRAIL, eye.add(side.scale(1.2)), eye.add(look.scale(range)).add(side.scale(3)), PAINT_EARTH, 18, 1.1f);
        return true;
    }

    /** Phantom Dragon Vouivre's Shout: a winged serpent-woman is painted and shouts a magical blast where the brush points; a gust under you keeps you aloft for a few seconds. */
    public static boolean vouivresShout(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 1.2f);
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));
        b.vfx(p, VfxShape.PAINT_EMERGE, p.position(), p.position().add(0, 2, 0), 40, 1.4f * s);
        fx(p, VfxShape.PAINT_BEAST, p.position().add(0, 1.5, 0), eye.add(dir.scale(6)), PAINT_GOLD, 40, 1.5f * s);
        SpellRuntime.bolt(p, eye, dir.scale(2.2), 1.0 * s, 20, false, null, (bolt, t) -> { }, (bolt, at) -> {
            fx(p, VfxShape.MAGIC_CIRCLE_EXPLOSION, at, at.add(0, 1, 0), PAINT_GOLD, 22, 2.0f * s);
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, at, 3.5 * s)) {
                if (n++ >= MAX_HITS) break;
                b.hurt(i, p, t, mode, 15f);
                push(t, t.position().subtract(at).normalize(), 1.0, 0.4);
            }
        });
        fx(p, VfxShape.PAINT_TRAIL, eye, eye.add(dir.scale(44)), PAINT_GOLD, 20, 1.3f * s);
        return true;
    }

    /** Souterrain Giant: the upper body of a giant rises out of the ground where you aim (dense earth that can be broken): it slams the ground three times in 3 s. */
    public static boolean souterrainGiant(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 4.5f * GrimoireBook.size(i, p);
        Vec3 c = GrimoireBook.aim(p, 20);
        b.castCircle(p, 1.1f);
        fx(p, VfxShape.PAINT_EMERGE, c, c.add(0, 2, 0), PAINT_EARTH, 70, 1.8f);
        fx(p, VfxShape.PAINT_BEAST, c, c.add(0, 4, 0), PAINT_EARTH, 70, 2.0f);
        fx(p, VfxShape.EARTH_RISE, c, c.add(0, 1, 0), PAINT_EARTH, 30, r);
        for (int k = 0; k < 3; k++) {
            SpellRuntime.later(p.serverLevel(), 20 + k * 20, () -> {
                if (gone(p)) return;
                int n = 0;
                for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                    if (n++ >= MAX_HITS) break;
                    b.hurt(i, p, t, mode, 7f);
                    push(t, t.position().subtract(c).normalize(), 0.8, 0.35);
                }
                fx(p, VfxShape.EARTH_FISSURE, c, c.add(0, 1, 0), PAINT_EARTH, 16, r * 0.6f);
            });
        }
        return true;
    }

    /** Twilight of Valhalla: a many-storeyed hall and winged shield-maidens are painted around you; after a short delay (3 s) its secondary effect begins: allies inside gain Regeneration, Resistance and Absorption for 8 s (the delay is the weakness the wiki names). */
    public static boolean twilightOfValhalla(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 6f * GrimoireBook.size(i, p);
        Vec3 c = p.position();
        b.castCircle(p, 1.4f);
        fx(p, VfxShape.PAINT_EMERGE, c, c.add(0, 4, 0), PAINT_GOLD, 220, 2.2f);
        fx(p, VfxShape.PAINT_BEAST, c, c.add(0, 4, 0), PAINT_GOLD, 220, 2.4f);
        fx(p, VfxShape.DIVINE_HALO, c.add(0, 4, 0), c.add(0, 5, 0), PAINT_GOLD, 220, 1.6f);
        SpellRuntime.zone(p.serverLevel(), 220, 20, age -> {
            if (gone(p) || age < 60) return;                                                              // the delay
            int n = 0;
            for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), e -> e == p || (e.isAlive() && e.isAlliedTo(p)))) {
                if (n++ >= MAX_HITS) break;
                if (t.distanceToSqr(c) > r * r) continue;
                t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 1));
                t.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1));
                t.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60, 2));
            }
            if (age == 60) fx(p, VfxShape.MAGIC_CIRCLE_EXPLOSION, c, c.add(0, 1, 0), PAINT_GOLD, 24, r * 0.5f);
        });
        return true;
    }

    // ================================================================ Vanica Zogratis: Blood Magic
    /** Red Beast: a giant monster of blood rises beside you and stays attached to you for 8 s: it rakes everything within 5 blocks of you every half second (up to 6 foes) and feeds you a little of what it takes. */
    public static boolean redBeast(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!pay(p, 3f, "Not enough blood left to shape a beast.")) return false;
        float r = 5f * GrimoireBook.size(i, p);
        b.castCircle(p, 1.2f);
        fx(p, VfxShape.BLOOD_SIGIL, p.position().add(0, 0.1, 0), p.position().add(0, 1, 0), BLOOD_RED, 30, 2.2f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BLOOD_FX1, p, p.position().add(0, 1, 0), BLOOD_RED, 160, 1.8f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BLOOD_ARCS, p, p.position().add(0, 1, 0), BLOOD_DARK, 160, 1.4f);
        SpellRuntime.zone(p.serverLevel(), 160, 10, age -> {
            if (gone(p)) return;
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) {
                if (n++ >= 6) break;
                b.hurt(i, p, t, mode, 3f);
                BalanceLaw.heal(p, 0.4f);
            }
            if (age % 40 == 0) fx(p, VfxShape.BLOOD_CRESCENT, p.position().add(0, 1, 0), p.position().add(p.getViewVector(1f).scale(r)).add(0, 1, 0), BLOOD_RED, 14, 1.2f);
        });
        return true;
    }

    /** Herd of Red Beasts: a large amount of blood is expelled and shaped into several smaller beasts that are not attached to you: they run through a spot of 7 blocks where you aim for 5 s. */
    public static boolean herdOfRedBeasts(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!pay(p, 4f, "Not enough blood left to shape a herd.")) return false;
        float r = 7f * GrimoireBook.size(i, p);
        Vec3 c = GrimoireBook.aim(p, 24);
        b.castCircle(p, 1.3f);
        fx(p, VfxShape.BLOOD_SIGIL, c.add(0, 0.1, 0), c.add(0, 1, 0), BLOOD_RED, 100, r);
        fx(p, VfxShape.BLOOD_DOWNPOUR, c, c.add(0, 1, 0), BLOOD_DARK, 100, r * 0.8f);
        fx(p, VfxShape.BLOOD_FX1, c, c.add(0, 1, 0), BLOOD_RED, 100, 1.6f);
        SpellRuntime.zone(p.serverLevel(), 100, 10, age -> {
            if (gone(p)) return;
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (n++ >= MAX_HITS) break;
                b.hurt(i, p, t, mode, 3.5f);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            }
            if (age % 30 == 0) fx(p, VfxShape.BLOOD_LANCE, c.add(0, 4, 0), c.add((p.getRandom().nextDouble() - 0.5) * r, 0, (p.getRandom().nextDouble() - 0.5) * r), BLOOD_RED, 12, 1.1f);
        });
        return true;
    }

    // ================================================================ Gadjah: Lightning Magic (the Heart Kingdom's True Lightning)
    /** Tenjiolia: lightning shot at great speed from the held-out right palm; rune arrays make it bend after a target, so it runs 30 blocks and pulls its line a little toward the nearest foe. */
    public static boolean tenjiolia(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 hand = p.getEyePosition().add(p.getViewVector(1f).cross(new Vec3(0, 1, 0)).normalize().scale(0.35)).add(0, -0.4, 0);
        Vec3 dir = p.getViewVector(1f);
        LivingEntity near = GrimoireBook.target(p, 30);
        if (near != null) dir = dir.scale(0.4).add(near.getBoundingBox().getCenter().subtract(hand).normalize().scale(0.6)).normalize();      // the rune array bends the line toward what you look at
        Vec3 end = hand.add(dir.scale(30 * s));
        b.castCircle(p, 0.9f);
        int n = 0;
        for (LivingEntity t : GrimoireBook.along(p, hand, end, 1.0)) {
            if (n++ >= MAX_HITS) break;
            b.hurt(i, p, t, mode, 14f);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
        }
        b.vfx(p, VfxShape.LIGHTNING_SPEAR, hand, end, 12, 1.2f * s);
        fx(p, VfxShape.FX_LIGHTNING_ARC, hand, end, BOLT_GOLD, 12, 1.0f);
        fx(p, VfxShape.LIGHTNINGFX_CAST, hand, hand.add(dir), BOLT_WHITE, 14, 0.9f);
        fx(p, VfxShape.LIGHTNINGFX_BURST, end, end.add(0, 1, 0), BOLT_GOLD, 18, 1.2f);
        return true;
    }

    /** Apocalypra Astrauza (Ultimate Magic): several interconnected natural-mana arrays gather around the right hand, life force is turned into magic, and a great beam of lightning runs 40 blocks. Costs 4 hearts of life force (refused when you do not have them). */
    public static boolean apocalypraAstrauza(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!pay(p, 8f, "Your life force is too low for the Ultimate Magic.")) return false;
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), dir = p.getViewVector(1f);
        Vec3 end = eye.add(dir.scale(40 * s));
        b.castCircle(p, 1.6f);
        for (int k = 1; k <= 3; k++) fx(p, VfxShape.ELF_CIRCLE, eye.add(dir.scale(k * 1.2)), eye.add(dir.scale(k * 1.2 + 0.1)), BOLT_GOLD, 16, 1.4f - 0.25f * k);      // the interconnected arrays in a row before the hand
        SpellRuntime.later(p.serverLevel(), 12, () -> {
            if (gone(p)) return;
            Vec3 e2 = p.getEyePosition(), d2 = p.getViewVector(1f), end2 = e2.add(d2.scale(40 * s));
            int n = 0;
            for (LivingEntity t : GrimoireBook.along(p, e2, end2, 2.4)) {
                if (n++ >= MAX_HITS) break;
                b.hurt(i, p, t, mode, 34f);
                push(t, d2, 1.2, 0.35);
            }
            fx(p, VfxShape.LIGHTNING_GOD, e2, end2, BOLT_GOLD, 24, 2.0f * s);
            fx(p, VfxShape.FX_LIGHTNING_ARC, e2, end2, BOLT_WHITE, 20, 1.6f);
            fx(p, VfxShape.LIGHTNINGFX_BURST, end2, end2.add(0, 1, 0), BOLT_GOLD, 26, 2.4f);
        });
        return true;
    }

    // ================================================================ Mars: Mineral Magic
    /** Laevateinn: a great sword of ore is made and swung over a 7 block cone in front of you (the wiki lists it as a mineral offensive spell, no description found: effect inferred from the name). */
    public static boolean laevateinn(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f), end = eye.add(look.scale(7 * s));
        b.castCircle(p, 1.4f);
        int n = 0;
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 7 * s)) {
            if (n >= MAX_HITS) break;
            Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
            if (to.lengthSqr() < 1e-4 || to.normalize().dot(look) < 0.4) continue;
            n++;
            b.hurt(i, p, t, mode, 22f);
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            push(t, to.normalize(), 1.2, 0.3);
        }
        b.vfx(p, VfxShape.EARTHFAM_CAST, eye, end, 20, 1.6f * s);
        fx(p, VfxShape.CRYSTAL_FX1, eye, end, MINERAL_BLUE, 20, 1.4f * s);
        fx(p, VfxShape.EARTHFAM_BURST, end, end.add(0, 1, 0), MINERAL_PALE, 22, 1.8f * s);
        return true;
    }

    // ================================================================ Kahono: Song Magic
    /** Heal Lullaby: a gentle song that closes wounds: you and allies within 8 blocks heal a quarter of their health (the usual healing decay applies) and the song rings out in notes. */
    public static boolean healLullaby(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        BalanceLaw.heal(p, p.getMaxHealth() * 0.25f);
        int n = 0;
        for (ServerPlayer ally : p.serverLevel().getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(8 * s))) {
            if (ally == p || !ally.isAlliedTo(p)) continue;
            if (n++ >= MAX_HITS) break;
            BalanceLaw.heal(ally, ally.getMaxHealth() * 0.25f);
        }
        b.castCircle(p, 1.2f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SONG_NOTES, p, p.position().add(0, 1.5, 0), SONG_ROSE, 60, 1.4f * s);
        fx(p, VfxShape.SONG_RING, p.position().add(0, 0.2, 0), p.position().add(0, 1, 0), SONG_GOLD, 40, 3.0f * s);
        fx(p, VfxShape.ELF_CIRCLE, p.position().add(0, 0.1, 0), p.position().add(0, 1, 0), SONG_ROSE, 40, 2.0f * s);
        return true;
    }

    /** Mother Lullaby: a lullaby sung at one target within 16 blocks puts it to sleep: it is held still, blind and weak for 4 s (a hard control, it shares the 12 s lock and a defensive spell can still block it). */
    public static boolean motherLullaby(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null) { GrimoireBook.fail(p, "No one to sing to."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 80);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9));
        t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 2));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SONG_NOTES, p, p.position().add(0, 1.5, 0), SONG_ROSE, 60, 1.2f);
        fx(p, VfxShape.SONG_RING, t.position().add(0, 0.2, 0), t.position().add(0, 1, 0), SONG_GOLD, ticks, 1.6f);
        fx(p, VfxShape.DREAM_TRANSITION, t.position(), t.position().add(0, 1.5, 0), SONG_ROSE, ticks, 1.0f);
        return true;
    }
}
