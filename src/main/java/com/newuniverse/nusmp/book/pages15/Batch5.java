package com.newuniverse.nusmp.book.pages15;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.ElementBook;
import com.newuniverse.nusmp.book.ForbiddenMagic;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.newuniverse.nusmp.book.pages15.Pages15Kit.NONE;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.all;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.bind;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.bolt;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.cast;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.cone;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.dash;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.empower;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.field;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.ignite;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.knock;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.leech;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.lift;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.line;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.mid;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.nova;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.signature;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.starter;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.strip;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.volley;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.zone;

/**
 * Batch 5 of the "15 spells for every grimoire" project: Light, Dark and Curse.
 *
 * <p>Books of this batch (the only books {@link Pages15} asks this class about; see {@link Pages15Plan}): {@code book_bone}, {@code book_dark}, {@code book_shadow}, {@code book_ash}, {@code book_blood}, {@code book_light}, {@code book_curse}, {@code book_anti_magic}, {@code forbidden_magic}, {@code book_curse_warding}, {@code book_demon_light}.
 *
 * <p>THE BUILDER OF THIS BATCH EDITS ONLY THIS FILE (plus tools/lang/pages15_batch5.json, src/test/resources/armour_policy/pages15_batch5_new_pages.json and docs/magic/pages15_batch5.md). How to write a
 * page: docs/magic/pages15_recipe.md. Shape of an entry, one {@code register} per book (the page counter script and the tests read this exact shape):
 * <pre>
 *     private static final List&lt;BookPage&gt; EXPLOSION = register(BOOK_NAME_HERE, List.of(
 *             Pages15Kit.mid(PAGE_ID_HERE, PAGE_NAME_HERE, Pages15Kit.nova(Tier.MID, 4, false, VfxShape.FLAME_EXPLOSION, Pages15Kit.ignite(3))).withCooldown(200)));
 * </pre>
 * (the real call takes string literals; they are not written out here because the armour-policy test scans the sources for page ids). Pages are appended AFTER every old page of the book, in the order
 * written here; never reorder or rename one once released (page ids are save keys).
 */
public final class Batch5 {
    private Batch5() {}

    /** Book registry path -> the pages this batch appends to it. */
    private static final Map<String, List<BookPage>> PAGES = new LinkedHashMap<>();

    private static List<BookPage> register(String book, List<BookPage> pages) {
        PAGES.put(book, List.copyOf(new ArrayList<>(pages)));
        return pages;
    }

    // ---------------------------------------------------------------- small helpers of this batch (not pages; every one is bounded like the kit)

    /** A hit rider that puts one potion effect on the target. */
    private static ElementBook.Rider rider(Holder<MobEffect> e, int ticks, int amp) {
        return Pages15Kit.effect(() -> new MobEffectInstance(e, ticks, amp));
    }

    /** A potion effect on the caster, for {@code empower}. */
    private static Supplier<MobEffectInstance> buff(Holder<MobEffect> e, int ticks, int amp) {
        return () -> new MobEffectInstance(e, ticks, amp);
    }

    /** Resistance (and optionally absorption) on the caster for a while. A pure self cast: no target, one packet. */
    private static BookPage.Cast ward(Tier t, int ticks, int resistanceAmp, int absorptionAmp, VfxShape shape) {
        return cast(t, c -> {
            ServerPlayer p = c.caster;
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, resistanceAmp));
            if (absorptionAmp >= 0) p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, absorptionAmp));
            c.circle(1f);
            c.fx(shape, p.position(), p.position().add(0, 1, 0), Math.min(ticks, 60), 1.4f);
            return true;
        });
    }

    /** Heals the caster by a fraction of the maximum health (through the balance law), optionally lifting every harmful effect. */
    private static BookPage.Cast selfHeal(Tier t, float fraction, boolean cleanse, VfxShape shape, float power) {
        return cast(t, c -> {
            ServerPlayer p = c.caster;
            if (p.getHealth() >= p.getMaxHealth() && !(cleanse && hasHarmful(p))) return c.fail("Nothing to mend.");
            BalanceLaw.heal(p, p.getMaxHealth() * Math.min(fraction, 0.4f));
            if (cleanse) for (MobEffectInstance e : new ArrayList<>(p.getActiveEffects())) if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) p.removeEffect(e.getEffect());
            c.circle(1f);
            c.fx(shape, p.position(), p.position().add(0, 1, 0), 30, power);
            return true;
        });
    }

    private static boolean hasHarmful(LivingEntity e) {
        for (MobEffectInstance m : e.getActiveEffects()) if (m.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) return true;
        return false;
    }

    /**
     * An armed trap: a circle is laid at the aim point and lies in wait for up to 100 ticks (one kit zone, checked every 4 ticks with one small foe query);
     * the first foe inside {@code triggerRadius} springs it once, and the foes in {@code blastRadius} (at most the tier's targets) take one standard hit and the rider.
     */
    private static BookPage.Cast trap(Tier t, double triggerRadius, double blastRadius, VfxShape armed, VfxShape sprung, ElementBook.Rider rider) {
        return cast(t, c -> {
            Vec3 at = c.aim(18).add(0, 0.1, 0);
            AtomicBoolean done = new AtomicBoolean();
            boolean laid = c.zone(100, 4, age -> {
                if (done.get() || c.foes(at, triggerRadius).isEmpty()) return;
                done.set(true);
                for (LivingEntity f : c.foes(at, blastRadius)) { c.hit(f); rider.apply(f, c.caster); }
                c.fx(sprung, at, at.add(0, 1, 0), 24, 1.4f);
                c.impact(at, 0.8f);
            });
            if (!laid) return false;
            c.circle(0.7f);
            c.fx(armed, at, at.add(0, 1, 0), 100, 1.1f);
            return true;
        });
    }

    /** Cuts down every projectile of somebody else within {@code r} of the caster (the box of the spell itself; the same sweep as the kit-free constructs of ElementBook). */
    private static int cutProjectiles(ServerPlayer p, double r) {
        int n = 0;
        for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(r))) {
            if (pr.getOwner() == p) continue;
            pr.discard();
            n++;
        }
        return n;
    }

    /** Dark Cloaked Dusk: the light fails round the caster. */
    private static BookPage.Cast dusk() {
        return cast(Tier.MID, c -> {
            ServerPlayer p = c.caster;
            for (LivingEntity f : c.foes(p.position(), 7 * c.size())) {
                f.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
                f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1));
            c.circle(1f);
            c.fx(VfxShape.DARKFX_BURST, p.position(), p.position().add(0, 1, 0), 30, 2f);
            return true;
        });
    }

    /** Demon-Dweller Sword: Protecting Light: speed and resistance for the caster and up to five allies within 8 blocks. */
    private static BookPage.Cast allies() {
        return cast(Tier.MID, c -> {
            ServerPlayer p = c.caster;
            int n = 0;
            for (ServerPlayer a : p.serverLevel().getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(8))) {
                if (a != p && !a.isAlliedTo(p)) continue;
                if (++n > 6) break;
                a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1));
                a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0));
            }
            c.circle(1f);
            c.fx(VfxShape.LIGHT_FLARE, p.position(), p.position().add(0, 2, 0), 40, 1.6f);
            return true;
        });
    }

    /** Black Guard: the blade across the body cuts the magic flying at the caster. */
    private static BookPage.Cast blackGuard() {
        return cast(Tier.STARTER, c -> {
            ServerPlayer p = c.caster;
            cutProjectiles(p, 4.5);
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 1));
            c.circle(0.8f);
            c.fx(VfxShape.ANTI_MAGIC_SLASH, p.getEyePosition(), p.getEyePosition().add(p.getViewVector(1f).scale(2)), 16, 1.2f);
            return true;
        });
    }

    /** Demon-Destroyer Sword: Causality Break: harmful effects on the caster are lifted, the foes in a cone lose a beneficial effect and take a hit. */
    private static BookPage.Cast causalityBreak() {
        ElementBook.Rider strip = strip();
        return cast(Tier.MID, c -> {
            ServerPlayer p = c.caster;
            boolean lifted = hasHarmful(p);
            for (MobEffectInstance e : new ArrayList<>(p.getActiveEffects())) if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) p.removeEffect(e.getEffect());
            Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
            int n = 0;
            for (LivingEntity f : c.foes(p.position(), 6 * c.size())) {
                if (f.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.3) continue;
                c.hit(f);
                strip.apply(f, p);
                n++;
            }
            if (!lifted && n == 0) return c.fail("Nothing to break.");
            c.circle(0.9f);
            c.fx(VfxShape.ANTI_MAGIC_SLASH, eye, eye.add(look.scale(6)), 16, 1.4f);
            return true;
        });
    }

    /** Magic-Negating Barrier: 4 seconds in which every foreign projectile within 5 blocks is cut down (a box query every 5 ticks). */
    private static BookPage.Cast negatingBarrier() {
        return cast(Tier.ZONE, c -> {
            ServerPlayer p = c.caster;
            if (!c.zone(80, 5, age -> cutProjectiles(p, 5))) return false;
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0));
            c.circle(1f);
            c.fx(VfxShape.NIHILITY_ZONE, p.position(), p.position().add(0, 1, 0), 80, 1.7f);
            return true;
        });
    }

    /** The devil's price: every forbidden page pays it first (health that stacks) and announces itself in a dark subtitle, like the older pages of the book. */
    private static BookPage.Cast priced(String title, BookPage.Cast inner) {
        return (b, i, p, mode) -> {
            if (!ForbiddenMagic.payPrice(p)) return false;
            ForbiddenMagic.subtitle(p, Component.literal(p.getName().getString() + " casts forbidden magic: " + title));
            return inner.cast(b, i, p, mode);
        };
    }

    // ---------------------------------------------------------------- the books of this batch, one register(...) each (cheap pages first)

    /** 11 pages appended to book_bone. */
    private static final List<BookPage> BONE = register("book_bone", List.of(
            starter("bone_sprout", "Bone Sprout",
                    line(Tier.STARTER, 6, 1.0, VfxShape.EARTH_SPIKES, lift(0.4))).withAnim("thrust"),
            starter("bone_blade", "Bone Blade",
                    cone(Tier.STARTER, 3.5, 0.5, VfxShape.WIND_SLASH, NONE)).withAnim("sweep"),
            starter("branching_bone", "Branching Bone",
                    volley(Tier.STARTER, 4, false, VfxShape.THREAD_LINE, NONE)).withAnim("out"),
            starter("bone_thicket", "Bone Thicket",
                    field(Tier.STARTER, 3.5, 60, 12, true, VfxShape.EARTH_SPIKES, rider(MobEffects.MOVEMENT_SLOWDOWN, 40, 1))).withAnim("slam"),
            mid("bone_extension", "Extending Bone",
                    line(Tier.MID, 14, 1.0, VfxShape.THREAD_LINE, knock(0.6))).withCooldown(200).withAnim("thrust"),
            mid("bone_wall", "Ossuary Wall",
                    ElementBook.wall(Blocks.BONE_BLOCK.defaultBlockState())).withCooldown(300).withAnim("slam"),
            mid("splinter_burst", "Splinter Burst",
                    nova(Tier.MID, 4.5, false, VfxShape.WATER_SPLASH, all(knock(0.7), rider(MobEffects.MOVEMENT_SLOWDOWN, 40, 1)))).withAnim("slam"),
            mid("bone_regrowth", "Bone Regrowth",
                    selfHeal(Tier.MID, 0.3f, false, VfxShape.EARTH_SPIKES, 1.2f)).withCooldown(400).withAnim("up"),
            zone("bone_forest", "Bone Forest",
                    field(Tier.ZONE, 5, 100, 10, true, VfxShape.EARTH_SPIKES, lift(0.3))).withCooldown(450).withAnim("slam"),
            zone("bone_cage", "Bone Cage",
                    nova(Tier.ZONE, 5, true, VfxShape.EARTH_SPIKES, rider(MobEffects.MOVEMENT_SLOWDOWN, 100, 3))).withCooldown(400).withAnim("slam"),
            signature("thousand_bone_grave", "Thousand Bone Grave",
                    field(Tier.SIGNATURE, 7, 100, 10, true, VfxShape.WATER_SPLASH, all(lift(0.3), rider(MobEffects.MOVEMENT_SLOWDOWN, 30, 1)))).withCooldown(900).withAnim("signature")));

    /** 14 pages appended to book_dark. */
    private static final List<BookPage> DARK = register("book_dark", List.of(
            starter("dark_cloaked_afterimage", "Dark Cloaked Afterimage",
                    dash(Tier.STARTER, true, 6, false, VfxShape.DARK_IAI, NONE)).withAnim("thrust"),
            starter("umbral_lance", "Umbral Lance",
                    bolt(Tier.STARTER, 0, VfxShape.DARK_THRUST, VfxShape.DARKFX_BURST, rider(MobEffects.MOVEMENT_SLOWDOWN, 40, 0))).withAnim("thrust"),
            starter("gloom_cloak", "Gloom Cloak",
                    empower(Tier.STARTER, 200, false, VfxShape.DARKFX_CAST, buff(MobEffects.MOVEMENT_SPEED, 200, 1))).withAnim("up"),
            starter("void_snare", "Void Snare",
                    bind(Tier.STARTER, 14, false, VfxShape.DARK_BLACK_HOLE, NONE)),
            mid("dark_cloaked_crossing_slash", "Dark Cloaked Crossing Slash",
                    cone(Tier.MID, 5, 0.4, VfxShape.DARK_SLASH_WILD, NONE)).withAnim("sweep"),
            mid("midnight_wave", "Midnight Wave",
                    line(Tier.MID, 14, 1.4, VfxShape.DARK_SLASH_AVIDYA, knock(0.6))).withAnim("sweep"),
            mid("devouring_shade", "Devouring Shade",
                    cone(Tier.MID, 6, 0.4, VfxShape.DARK_BLACK_HOLE, all(strip(), leech(1)))).withCooldown(240).withAnim("sweep"),
            mid("crescent_barrage", "Dark Cloaked Crescent Barrage",
                    volley(Tier.MID, 5, true, VfxShape.DARK_SLASH_BLADE, NONE)).withAnim("out"),
            zone("abyssal_pit", "Abyssal Pit",
                    field(Tier.ZONE, 5, 80, 10, true, VfxShape.DARKFX_ZONE, rider(MobEffects.MOVEMENT_SLOWDOWN, 30, 2))).withCooldown(450).withAnim("slam"),
            zone("eclipse_fall", "Eclipse Fall",
                    nova(Tier.ZONE, 6, true, VfxShape.DARK_BLACK_MOON, rider(MobEffects.DARKNESS, 100, 0))).withCooldown(400).withAnim("up"),
            starter("avidya_blunt_strike", "Dark Cloaked Avidya Blunt Strike",
                    cone(Tier.STARTER, 3.5, 0.6, VfxShape.DARK_SLASH_AVIDYA, all(knock(1.4), rider(MobEffects.CONFUSION, 40, 0)))).withAnim("sweep"),
            mid("black_cocoon", "Dark Cloaked Black Cocoon",
                    ward(Tier.MID, 100, 2, 1, VfxShape.DARKFX_ZONE)).withCooldown(500).withAnim("up"),
            mid("dusk", "Dark Cloaked Dusk",
                    dusk()).withCooldown(400).withAnim("out"),
            mid("black_blade_shadow_strike", "Black Blade Shadow Strike",
                    dash(Tier.MID, true, 8, true, VfxShape.DARK_SLASH_BLADE, NONE)).withCooldown(240).withAnim("sweep")));

    /** 12 pages appended to book_shadow. */
    private static final List<BookPage> SHADOW = register("book_shadow", List.of(
            starter("shadow_needles", "Shadow Needles",
                    volley(Tier.STARTER, 3, false, VfxShape.THREAD_LINE, NONE)).withAnim("out"),
            starter("grasping_hands", "Grasping Hands",
                    bind(Tier.STARTER, 14, false, VfxShape.SHADOW_POOL, NONE)),
            starter("shadow_slip", "Shadow Slip",
                    dash(Tier.STARTER, false, 8, false, VfxShape.SHADOW_UNITE, NONE)).withAnim("out"),
            starter("umbral_shroud", "Umbral Shroud",
                    empower(Tier.STARTER, 160, false, VfxShape.SHADOW_UNITE, buff(MobEffects.INVISIBILITY, 160, 0), buff(MobEffects.MOVEMENT_SPEED, 160, 0))).withAnim("up"),
            mid("doppelganger", "Doppelganger",
                    dash(Tier.MID, true, 8, true, VfxShape.SHADOW_POOL, NONE)).withCooldown(240).withAnim("sweep"),
            mid("shadow_eruption", "Shadow Eruption",
                    nova(Tier.MID, 4.5, false, VfxShape.SHADOW_POOL, lift(0.6))).withAnim("slam"),
            mid("veil_of_night", "Veil of Night",
                    cone(Tier.MID, 7, 0.4, VfxShape.DARKFX_BURST, rider(MobEffects.BLINDNESS, 60, 0))).withAnim("sweep"),
            mid("shadow_double", "Shadow Double",
                    ElementBook.decoy()).withCooldown(300).withAnim("out"),
            mid("unite_canis_equus", "Unite Mode: Canis x Equus",
                    empower(Tier.MID, 300, true, VfxShape.SHADOW_UNITE, buff(MobEffects.DAMAGE_BOOST, 300, 0), buff(MobEffects.DAMAGE_RESISTANCE, 300, 0))).withCooldown(700).withAnim("signature"),
            zone("endless_night", "Endless Night",
                    field(Tier.ZONE, 6, 100, 10, true, VfxShape.DARKFX_ZONE, all(rider(MobEffects.WEAKNESS, 40, 0), rider(MobEffects.DARKNESS, 60, 0)))).withCooldown(450).withAnim("slam"),
            zone("shadow_prison", "Shadow Prison",
                    nova(Tier.ZONE, 5, true, VfxShape.SHADOW_POOL, rider(MobEffects.MOVEMENT_SLOWDOWN, 100, 4))).withCooldown(400).withAnim("slam"),
            signature("walpurgis_night", "Walpurgis Night",
                    field(Tier.SIGNATURE, 8, 100, 10, false, VfxShape.SHADOW_UNITE, rider(MobEffects.MOVEMENT_SLOWDOWN, 30, 1))).withCooldown(1200).withAnim("signature")));

    /** 10 pages appended to book_ash. */
    private static final List<BookPage> ASH = register("book_ash", List.of(
            starter("ash_lash", "Ash Lash",
                    line(Tier.STARTER, 8, 0.9, VfxShape.ASH_FX1, rider(MobEffects.WITHER, 60, 0))).withAnim("sweep"),
            starter("pitfall_trap", "Pitfall Trap",
                    trap(Tier.STARTER, 2.0, 2.5, VfxShape.ASH_FX2, VfxShape.ASH_FX3, rider(MobEffects.MOVEMENT_SLOWDOWN, 80, 6))).withCooldown(160).withAnim("out"),
            starter("ash_step", "Ash Step",
                    dash(Tier.STARTER, false, 8, false, VfxShape.ASH_FX1, NONE)).withAnim("out"),
            mid("mine_trap", "Mine Trap",
                    trap(Tier.MID, 1.8, 3.5, VfxShape.ASH_FX2, VfxShape.ASH_FX3, knock(1.0))).withCooldown(240).withAnim("out"),
            mid("smothering_ash", "Smothering Ash",
                    cone(Tier.MID, 6, 0.4, VfxShape.ASH_FX1, all(rider(MobEffects.BLINDNESS, 60, 0), rider(MobEffects.MOVEMENT_SLOWDOWN, 60, 1)))).withAnim("sweep"),
            mid("ash_barricade", "Ash Barricade",
                    ElementBook.wall(Blocks.SMOOTH_BASALT.defaultBlockState())).withCooldown(300).withAnim("slam"),
            mid("cinder_rain", "Cinder Rain",
                    volley(Tier.MID, 5, false, VfxShape.ASH_FX1, ignite(2))).withAnim("up"),
            zone("smoldering_ground", "Smoldering Ground",
                    field(Tier.ZONE, 5, 100, 10, true, VfxShape.ASH_FX2, rider(MobEffects.WITHER, 40, 0))).withCooldown(450).withAnim("slam"),
            zone("ash_sentinels", "Ash Sentinels",
                    field(Tier.ZONE, 3.5, 100, 10, false, VfxShape.ASH_FX3, knock(0.4))).withCooldown(400).withAnim("up"),
            zone("ash_storm", "Ash Storm",
                    nova(Tier.ZONE, 6, false, VfxShape.ASH_FX3, all(knock(0.8), rider(MobEffects.BLINDNESS, 60, 0)))).withCooldown(400).withAnim("slam")));

    /** 10 pages appended to book_blood. */
    private static final List<BookPage> BLOOD = register("book_blood", List.of(
            starter("jets_of_blood", "Jets of Blood",
                    line(Tier.STARTER, 9, 0.8, VfxShape.THREAD_LINE, knock(0.4))).withAnim("thrust"),
            starter("blood_spikes", "Blood Spikes",
                    line(Tier.STARTER, 7, 1.0, VfxShape.EARTH_SPIKES, NONE)).withAnim("thrust"),
            starter("blood_surge", "Blood Surge",
                    dash(Tier.STARTER, true, 7, false, VfxShape.THREAD_LINE, leech(0.5f))).withAnim("thrust"),
            mid("scaffold_of_fresh_blood", "Scaffold of Fresh Blood",
                    nova(Tier.MID, 4, true, VfxShape.EARTH_SPIKES, rider(MobEffects.MOVEMENT_SLOWDOWN, 100, 5))).withCooldown(300).withAnim("slam"),
            mid("puppet_bloodflow", "Puppet Bloodflow",
                    bind(Tier.MID, 16, false, VfxShape.THREAD_LINE, NONE)).withCooldown(300),
            mid("curse_breaking_blood_cocoon", "Curse-Breaking Blood Cocoon",
                    selfHeal(Tier.MID, 0.25f, true, VfxShape.WATER_SPLASH, 1.4f)).withCooldown(400).withAnim("up"),
            mid("lifeblood_scythe", "Lifeblood Scythe",
                    cone(Tier.MID, 5, 0.1, VfxShape.WIND_RING, leech(1))).withAnim("sweep"),
            mid("blood_frenzy", "Blood Frenzy",
                    empower(Tier.MID, 240, false, VfxShape.SPIRIT_AURA, buff(MobEffects.DAMAGE_BOOST, 240, 0), buff(MobEffects.MOVEMENT_SPEED, 240, 0))).withAnim("up"),
            zone("thorn_sigil", "Crimson Thorn Sigil",
                    field(Tier.ZONE, 5, 80, 10, true, VfxShape.EARTH_SPIKES, leech(0.5f))).withCooldown(450).withAnim("slam"),
            zone("sanguine_eruption", "Sanguine Eruption",
                    nova(Tier.ZONE, 5, true, VfxShape.WATER_SPLASH, all(knock(0.8), lift(0.5)))).withCooldown(400).withAnim("slam")));

    /** 9 pages appended to book_light. */
    private static final List<BookPage> LIGHT = register("book_light", List.of(
            starter("photon_lance", "Photon Lance",
                    bolt(Tier.STARTER, 0, VfxShape.LIGHT_BLADE, VfxShape.LIGHT_FLARE, NONE)).withAnim("thrust"),
            starter("radiant_flare", "Radiant Flare",
                    nova(Tier.STARTER, 5, false, VfxShape.LIGHT_FLARE, rider(MobEffects.BLINDNESS, 40, 0))).withAnim("slam"),
            mid("radiant_ward", "Radiant Ward",
                    ward(Tier.MID, 120, 1, 1, VfxShape.LIGHT_FLARE)).withCooldown(400).withAnim("up"),
            mid("dawn_beam", "Dawn Beam",
                    line(Tier.MID, 18, 1.2, VfxShape.DIVINE_RAY, NONE)).withAnim("thrust"),
            mid("protecting_light", "Demon-Dweller Sword: Protecting Light",
                    allies()).withCooldown(500).withAnim("up"),
            zone("storm_of_light_imperial_swords", "Storm of Light Imperial Swords of Conviction",
                    volley(Tier.ZONE, 5, true, VfxShape.LIGHT_BLADE, NONE)).withCooldown(400).withAnim("up"),
            signature("quintet_sword_of_judgment", "Light Magic: Elemental Quintet",
                    line(Tier.SIGNATURE, 22, 2.5, VfxShape.LIGHT_BLADE, knock(0.8))).withCooldown(900).withAnim("signature"),
            mid("karna_gibbous_ray", "Moonlight Magic: Gibbous Ray",
                    bolt(Tier.MID, 4, VfxShape.DIVINE_RAY, VfxShape.LIGHT_FLARE, NONE)).withCooldown(240).withAnim("thrust"),
            mid("karna_moon_trap", "Moonlight Magic: Moon Trap",
                    trap(Tier.MID, 2.0, 3.5, VfxShape.MAGIC_CIRCLE, VfxShape.LIGHT_FLARE, rider(MobEffects.MOVEMENT_SLOWDOWN, 80, 4))).withCooldown(300).withAnim("out")));

    /** 7 pages appended to book_curse. */
    private static final List<BookPage> CURSE = register("book_curse", List.of(
            starter("hex_needle", "Hex Needle",
                    bolt(Tier.STARTER, 0, VfxShape.CURSE_FX1, VfxShape.CURSE_FX3, rider(MobEffects.WEAKNESS, 100, 0))).withAnim("thrust"),
            starter("wasting_touch", "Wasting Touch",
                    cone(Tier.STARTER, 3, 0.7, VfxShape.CURSE_FX1, all(rider(MobEffects.WITHER, 80, 0), rider(MobEffects.HUNGER, 100, 1)))).withAnim("thrust"),
            starter("jinx", "Jinx",
                    nova(Tier.STARTER, 3, true, VfxShape.CURSE_FX3, all(rider(MobEffects.UNLUCK, 200, 1), rider(MobEffects.MOVEMENT_SLOWDOWN, 40, 0)))).withAnim("up"),
            mid("plague_wind", "Plague Wind",
                    cone(Tier.MID, 8, 0.4, VfxShape.CURSE_FX1, all(rider(MobEffects.POISON, 100, 0), rider(MobEffects.WEAKNESS, 100, 0)))).withAnim("sweep"),
            mid("effigy_curse", "Effigy Curse",
                    bind(Tier.MID, 16, false, VfxShape.CURSE_FX3, rider(MobEffects.WEAKNESS, 120, 1))).withCooldown(300),
            mid("hex_swarm", "Hex Swarm",
                    volley(Tier.MID, 5, true, VfxShape.CURSE_FX1, rider(MobEffects.POISON, 60, 0))).withAnim("out"),
            zone("rotting_ground", "Rotting Ground",
                    field(Tier.ZONE, 5, 100, 10, true, VfxShape.CURSE_FX2, rider(MobEffects.WITHER, 40, 0))).withCooldown(450).withAnim("slam")));

    /** 7 pages appended to book_anti_magic. */
    private static final List<BookPage> ANTI_MAGIC = register("book_anti_magic", List.of(
            starter("dispelling_thrust", "Dispelling Thrust",
                    cone(Tier.STARTER, 3.5, 0.7, VfxShape.ANTI_MAGIC_SLASH, strip())).withAnim("thrust").withWeapon(com.newuniverse.nusmp.item.WeaponMagicHelper.GREATSWORDS),
            starter("black_sweep", "Black Sweep",
                    cone(Tier.STARTER, 5, 0.3, VfxShape.ANTI_MAGIC_SLASH, knock(0.6))).withAnim("sweep").withWeapon(com.newuniverse.nusmp.item.WeaponMagicHelper.GREATSWORDS),
            starter("black_guard", "Black Guard",
                    blackGuard()).withCooldown(160).withAnim("up"),
            mid("causality_break", "Demon-Destroyer Sword: Causality Break",
                    causalityBreak()).withCooldown(300).withAnim("sweep"),
            mid("black_slash_barrage", "Black Slash Barrage",
                    volley(Tier.MID, 5, false, VfxShape.ANTI_MAGIC_SLASH, NONE)).withAnim("sweep").withWeapon(com.newuniverse.nusmp.item.WeaponMagicHelper.GREATSWORDS),
            mid("dweller_flight", "Demon-Dweller Sword: Flight",
                    dash(Tier.MID, false, 12, false, VfxShape.ANTI_MAGIC_SLASH, NONE)).withCooldown(200).withAnim("out"),
            zone("magic_negating_barrier", "Magic-Negating Barrier",
                    negatingBarrier()).withCooldown(600).withAnim("up")));

    /** 7 pages appended to forbidden_magic. */
    private static final List<BookPage> FORBIDDEN = register("forbidden_magic", List.of(
            starter("devils_claw", "Devil's Claw",
                    priced("Devil's Claw", cone(Tier.STARTER, 4.5, 0.5, VfxShape.ANTI_MAGIC_SLASH, rider(MobEffects.WITHER, 60, 0)))).withAnim("sweep"),
            starter("contract_bolt", "Contract Bolt",
                    priced("Contract Bolt", bolt(Tier.STARTER, 2.5, VfxShape.THREAD_LINE, VfxShape.DEVIL_CIRCLE, ignite(3)))).withAnim("thrust"),
            mid("soul_siphon", "Soul Siphon",
                    priced("Soul Siphon", line(Tier.MID, 10, 1.0, VfxShape.THREAD_LINE, leech(2.5f)))).withCooldown(300).withAnim("thrust"),
            mid("devils_bargain", "Devil's Bargain",
                    priced("Devil's Bargain", empower(Tier.MID, 200, false, VfxShape.SPIRIT_AURA, buff(MobEffects.DAMAGE_BOOST, 200, 1), buff(MobEffects.MOVEMENT_SPEED, 200, 0)))).withCooldown(500).withAnim("up"),
            mid("damned_cry", "Cry of the Damned",
                    priced("Cry of the Damned", nova(Tier.MID, 5, false, VfxShape.DEVIL_CIRCLE, all(knock(1.0), rider(MobEffects.WEAKNESS, 80, 0))))).withAnim("slam"),
            mid("condemned_volley", "Condemned Volley",
                    priced("Condemned Volley", volley(Tier.MID, 4, true, VfxShape.ANTI_MAGIC_SLASH, NONE))).withAnim("out"),
            zone("abyssal_chains", "Abyssal Chains",
                    priced("Abyssal Chains", field(Tier.ZONE, 5, 100, 10, true, VfxShape.DEVIL_CIRCLE, rider(MobEffects.MOVEMENT_SLOWDOWN, 30, 2)))).withCooldown(450).withAnim("slam")));

    /** 4 pages appended to book_curse_warding. */
    private static final List<BookPage> CURSE_WARDING = register("book_curse_warding", List.of(
            starter("spell_severing_curse", "Spell-Severing Curse",
                    bolt(Tier.STARTER, 0, VfxShape.CURSE_WARDING_FX1, VfxShape.CURSE_WARDING_FX3, all(strip(), rider(MobEffects.WEAKNESS, 80, 0)))).withAnim("thrust"),
            mid("quickening_curse", "Quickening Curse",
                    empower(Tier.MID, 160, false, VfxShape.CURSE_WARDING_FX3, buff(MobEffects.REGENERATION, 160, 1))).withCooldown(400).withAnim("up"),
            mid("detonation_hex", "Detonation Hex",
                    nova(Tier.MID, 3.5, true, VfxShape.CURSE_WARDING_FX3, knock(0.8))).withCooldown(200).withAnim("slam"),
            zone("rune_field", "Rune Field",
                    field(Tier.ZONE, 5, 100, 10, true, VfxShape.CURSE_WARDING_FX2, rider(MobEffects.MOVEMENT_SLOWDOWN, 30, 2))).withCooldown(450).withAnim("slam")));

    /** 3 pages appended to book_demon_light. */
    private static final List<BookPage> DEMON_LIGHT = register("book_demon_light", List.of(
            starter("black_light_ray", "Black Light Ray",
                    line(Tier.STARTER, 16, 0.8, VfxShape.DEMON_LIGHT_FX1, NONE)).withAnim("thrust"),
            mid("demon_arrows_of_judgment", "Demon Light Magic: Arrows of Judgment",
                    volley(Tier.MID, 5, true, VfxShape.DEMON_LIGHT_FX1, NONE)).withAnim("up"),
            zone("demon_heaven_splitting_flash", "Demon Light Magic: Heaven-Splitting Flash",
                    nova(Tier.ZONE, 6, true, VfxShape.DEMON_LIGHT_FX3, rider(MobEffects.BLINDNESS, 60, 0))).withCooldown(400).withAnim("up")));

    /** The pages this batch appends to the book (empty for a book it has not written yet, or a book of another batch). Never null. */
    static List<BookPage> pages(String bookPath) {
        return PAGES.getOrDefault(bookPath, List.of());
    }
}
