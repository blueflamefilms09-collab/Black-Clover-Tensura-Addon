package com.newuniverse.nusmp.book.pages15;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.ElementBook;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Batch 7 of the "15 spells for every grimoire" project: Nature and Beast.
 *
 * <p>Books of this batch (the only books {@link Pages15} asks this class about; see {@link Pages15Plan}): {@code book_world_tree}, {@code book_poison}, {@code book_cotton}, {@code book_plant}, {@code book_cherry_blossom}, {@code book_briar}, {@code book_tree}, {@code book_vine}, {@code book_poison_plant}, {@code book_beast}, {@code book_body}, {@code book_fungus}, {@code book_demon_beast}.
 *
 * <p>86 pages, appended AFTER every old page of their book (page ids are save keys: never rename or reorder). Most pages use the kit's standard casts; the private helpers below
 * ({@code groupFx}, {@code foeFx}, {@code leap}, {@code pull}, {@code single}, {@code selfFx}, {@code siphon}, {@code pulse}, {@code wall}) are small custom casts built ONLY on the kit's bounded
 * {@link Pages15Kit.Ctx} (at most the tier's targets, hits through the book's balance law, at most 10 effect packets, one zone of at most 100 ticks at 20-tick steps, no entity spawned, no chunk loaded).
 * Every page is listed with its origin (canon or original), reuse and worst case in docs/magic/pages15_batch7.md.
 */
public final class Batch7 {
    private Batch7() {}

    /** Book registry path -> the pages this batch appends to it. */
    private static final Map<String, List<BookPage>> PAGES = new LinkedHashMap<>();

    private static List<BookPage> register(String book, List<BookPage> pages) {
        PAGES.put(book, List.copyOf(new ArrayList<>(pages)));
        return pages;
    }

    // ---------------------------------------------------------------- small helpers (all bounded; see the class comment)
    /** A potion effect as a hit rider. */
    private static ElementBook.Rider r(Holder<MobEffect> effect, int ticks, int amp) {
        return Pages15Kit.effect(() -> new MobEffectInstance(effect, ticks, amp));
    }

    /** A potion effect to hand to a friend or to yourself. */
    private static Supplier<MobEffectInstance> s(Holder<MobEffect> effect, int ticks, int amp) {
        return () -> new MobEffectInstance(effect, ticks, amp);
    }

    /** The caster and at most 7 allied players within {@code radius} (at most 10) of {@code centre}: one box query of the spell's own size. */
    private static List<ServerPlayer> alliesOf(Pages15Kit.Ctx c, Vec3 centre, double radius) {
        double r = Math.min(10.0, Math.max(0.5, radius));
        List<ServerPlayer> out = new ArrayList<>();
        out.add(c.caster);
        for (ServerPlayer a : c.caster.serverLevel().getEntitiesOfClass(ServerPlayer.class, new AABB(centre, centre).inflate(r),
                x -> x != c.caster && x.isAlive() && !x.isSpectator() && x.isAlliedTo(c.caster) && x.distanceToSqr(centre) <= r * r)) {
            if (out.size() >= 8) break;
            out.add(a);
        }
        return out;
    }

    /** Removes the harmful potion effects (the same walk as the kit's strip rider, over a copy). */
    private static void cleanse(ServerPlayer a) {
        for (MobEffectInstance e : new ArrayList<>(a.getActiveEffects())) if (!e.getEffect().value().isBeneficial()) a.removeEffect(e.getEffect());
    }

    /** You and your allies within {@code radius}: an optional heal (at most 25 % of the maximum health, healing decay applies), an optional cleanse, and potion effects. */
    @SafeVarargs
    private static BookPage.Cast groupFx(Tier t, double radius, float healFrac, boolean cleanse, VfxShape shape, Supplier<MobEffectInstance>... effects) {
        return Pages15Kit.cast(t, c -> {
            double r = Math.min(10.0, radius * c.size());
            for (ServerPlayer a : alliesOf(c, c.caster.position(), r)) {
                if (healFrac > 0f) BalanceLaw.heal(a, a.getMaxHealth() * Math.min(healFrac, 0.25f));
                if (cleanse) cleanse(a);
                for (Supplier<MobEffectInstance> e : effects) a.addEffect(e.get());
            }
            c.circle(1f);
            c.fx(shape, c.caster.position(), c.caster.position().add(0, 1, 0), 30, (float) Math.max(0.5, r / 3));
            return true;
        });
    }

    /** Potion effects on yourself only. */
    @SafeVarargs
    private static BookPage.Cast selfFx(Tier t, VfxShape shape, Supplier<MobEffectInstance>... effects) {
        return Pages15Kit.cast(t, c -> {
            for (Supplier<MobEffectInstance> e : effects) c.caster.addEffect(e.get());
            c.circle(1f);
            c.fx(shape, c.caster.position(), c.caster.position().add(0, 1, 0), 30, 0.8f);
            return true;
        });
    }

    /** Foes in a sphere round you or the aim point (at most the tier's targets, radius at most 8): an optional standard hit and potion effects. */
    @SafeVarargs
    private static BookPage.Cast foeFx(Tier t, double radius, boolean atAim, boolean hit, VfxShape shape, Supplier<MobEffectInstance>... effects) {
        return Pages15Kit.cast(t, c -> {
            Vec3 at = atAim ? c.aim(24) : c.caster.position();
            double r = Math.min(8.0, radius * c.size());
            for (LivingEntity f : c.foes(at, r)) {
                if (hit) c.hit(f);
                for (Supplier<MobEffectInstance> e : effects) f.addEffect(e.get());
            }
            c.circle(1f);
            c.fx(shape, at, at.add(0, 1, 0), 24, (float) Math.max(0.5, r / 3));
            return true;
        });
    }

    /** One foe under the crosshair: a standard hit and a rider. */
    private static BookPage.Cast single(Tier t, double range, VfxShape trail, VfxShape burst, ElementBook.Rider rider) {
        return Pages15Kit.cast(t, c -> {
            LivingEntity f = c.target(range);
            if (f == null) return c.fail("No target.");
            c.hit(f);
            rider.apply(f, c.caster);
            c.circle(0.7f);
            c.fx(trail, c.caster.getEyePosition(), f.getBoundingBox().getCenter(), 10, 0.8f);
            c.fx(burst, f.position(), f.position().add(0, 1, 0), 16, 0.7f);
            return true;
        });
    }

    /** A standard hit on the foe you look at that also drinks a little of its magicules into you (the same drain as the World Tree book's pages). */
    private static BookPage.Cast siphon(Tier t, double range, double drainFrac, VfxShape shape) {
        return Pages15Kit.cast(t, c -> {
            LivingEntity f = c.target(range);
            if (f == null) return c.fail("No one to drink from.");
            c.hit(f);
            EnergyBridge.drain(f, c.caster, Math.min(drainFrac, 0.03));
            c.circle(0.7f);
            c.fx(shape, f.getBoundingBox().getCenter(), c.caster.getBoundingBox().getCenter(), 16, 0.6f);
            return true;
        });
    }

    /**
     * A hop: you are thrown {@code fwd} blocks a tick forward and {@code up} up (slow falling for 2 seconds, no fall damage). With {@code blast}, 14 ticks later a burst of 3.5 blocks hits the foes where you
     * are (one delayed task, the tier's targets).
     */
    private static BookPage.Cast leap(Tier t, double fwd, double up, boolean blast, VfxShape shape, ElementBook.Rider rider) {
        return Pages15Kit.cast(t, c -> {
            ServerPlayer p = c.caster;
            Vec3 look = p.getLookAngle();
            Vec3 dir = new Vec3(look.x, 0, look.z);
            dir = dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : dir.normalize();
            p.setDeltaMovement(dir.x * Math.min(fwd, 1.6), Math.min(up, 1.0), dir.z * Math.min(fwd, 1.6));
            p.hurtMarked = true;
            p.fallDistance = 0;
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false));
            c.circle(0.7f);
            c.fx(shape, p.position(), p.position().add(dir.scale(fwd * 4)).add(0, 1, 0), 14, 1f);
            if (blast) c.later(14, () -> {
                Vec3 at = p.position();
                for (LivingEntity f : c.foes(at, 3.5)) {
                    c.hit(f);
                    rider.apply(f, p);
                }
                p.fallDistance = 0;
                c.fx(shape, at, at.add(0, 1, 0), 16, 1f);
            });
            return true;
        });
    }

    /** Hooks the foes round the aim point (radius at most 8) and hauls them toward you; a standard hit each. */
    private static BookPage.Cast pull(Tier t, double range, double radius, VfxShape shape, ElementBook.Rider rider) {
        return Pages15Kit.cast(t, c -> {
            Vec3 at = c.aim(range);
            List<LivingEntity> foes = c.foes(at, Math.min(8.0, radius * c.size()));
            if (foes.isEmpty()) return c.fail("Nothing to hook.");
            for (LivingEntity f : foes) {
                c.hit(f);
                rider.apply(f, c.caster);
                Vec3 d = c.caster.position().subtract(f.position());
                d = new Vec3(d.x, 0, d.z);
                d = d.lengthSqr() < 1e-4 ? Vec3.ZERO : d.normalize().scale(1.1);
                f.setDeltaMovement(d.x, 0.35, d.z);
                f.hurtMarked = true;
            }
            c.circle(0.8f);
            c.fx(shape, c.caster.getEyePosition(), at, 14, 1f);
            return true;
        });
    }

    /**
     * A standing field of at most 100 ticks (pulses every {@code step} ticks, at least 4): the foes inside take the tier's pulse damage when {@code dmg}, the {@code foe} effects and a drain of at most 3 % of their
     * magicules; you and your allied players inside mend {@code healFrac} (at most 25 %) and get the {@code ally} effects. One of the player's two kit zones; one effect packet for the whole field.
     */
    private static BookPage.Cast pulse(Tier t, double radius, int ticks, int step, boolean atAim, VfxShape shape, boolean dmg, float healFrac,
                                       List<Supplier<MobEffectInstance>> ally, List<Supplier<MobEffectInstance>> foe, double drainFrac) {
        return Pages15Kit.cast(t, c -> {
            Vec3 centre = atAim ? c.aim(20) : c.caster.position();
            double r = Math.min(8.0, radius * c.size());
            boolean ok = c.zone(ticks, step, age -> {
                Vec3 at = atAim ? centre : c.caster.position();
                for (LivingEntity f : c.foes(at, r)) {
                    if (dmg) c.hit(f, t.pulse);
                    for (Supplier<MobEffectInstance> e : foe) f.addEffect(e.get());
                    if (drainFrac > 0) EnergyBridge.drain(f, c.caster, Math.min(drainFrac, 0.03));
                }
                if (healFrac > 0f || !ally.isEmpty()) {
                    for (ServerPlayer a : alliesOf(c, at, r)) {
                        if (healFrac > 0f) BalanceLaw.heal(a, a.getMaxHealth() * Math.min(healFrac, 0.25f));
                        for (Supplier<MobEffectInstance> e : ally) a.addEffect(e.get());
                    }
                }
            });
            if (!ok) return false;
            c.circle(1f);
            c.fx(shape, centre, centre.add(0, 1, 0), ticks, (float) Math.max(0.8, r / 3));
            return true;
        });
    }

    /** A temporary wall of one block in front of you (the shared ElementBook wall: at most 12 blocks for 8 seconds, only into free space). */
    private static BookPage.Cast wall(Block block) {
        return ElementBook.wall(block.defaultBlockState());
    }

    // ---------------------------------------------------------------- the books of this batch, one register(...) each (order = the order of the plan)

    private static final List<BookPage> WORLD_TREE = register("book_world_tree", List.of(
            Pages15Kit.mid("dracaena_formation", "Dracaena Formation",
                    Pages15Kit.nova(Tier.MID, 4.5, true, VfxShape.WORLD_TREE_FX3, Pages15Kit.all(r(MobEffects.MOVEMENT_SLOWDOWN, 70, 3), Pages15Kit.lift(0.25)))).withCooldown(200).withAnim("slam"),
            Pages15Kit.starter("sapling_lance", "Sapling Lance",
                    Pages15Kit.bolt(Tier.STARTER, 1.5, VfxShape.WORLD_TREE_FX1, VfxShape.WORLD_TREE_FX3, Pages15Kit.leech(1f))).withAnim("thrust"),
            Pages15Kit.starter("mana_root_sip", "Mana Root Sip",
                    siphon(Tier.STARTER, 16, 0.02, VfxShape.TREE_DRAIN)).withCooldown(120),
            Pages15Kit.starter("mistilteinn_thorn_barrage", "Mistilteinn Thorn Barrage",
                    Pages15Kit.volley(Tier.STARTER, 4, true, VfxShape.WORLD_TREE_FX1, r(MobEffects.MOVEMENT_SLOWDOWN, 30, 0))).withCooldown(100),
            Pages15Kit.starter("branch_sweep", "Branch Sweep",
                    Pages15Kit.cone(Tier.STARTER, 6, 0.35, VfxShape.TREE_CANOPY, Pages15Kit.knock(0.8))).withAnim("sweep"),
            Pages15Kit.starter("root_spring", "Root Spring",
                    Pages15Kit.dash(Tier.STARTER, true, 8, false, VfxShape.TREE_ROOTS, Pages15Kit.knock(0.5))).withCooldown(100),
            Pages15Kit.mid("canopy_shelter", "Canopy Shelter",
                    groupFx(Tier.MID, 6, 0f, false, VfxShape.TREE_CANOPY, s(MobEffects.DAMAGE_RESISTANCE, 160, 0), s(MobEffects.ABSORPTION, 160, 1))).withCooldown(300),
            Pages15Kit.mid("heartroot_mending", "Heartroot Mending",
                    Pages15Kit.healAllies(7, 0.15f)).withCooldown(300),
            Pages15Kit.mid("warding_root_net", "Warding Root Net",
                    foeFx(Tier.MID, 4, true, true, VfxShape.TREE_ROOTS, s(MobEffects.MOVEMENT_SLOWDOWN, 80, 4), s(MobEffects.WEAKNESS, 80, 0))).withCooldown(220),
            Pages15Kit.mid("living_root_wall", "Living Root Wall",
                    wall(Blocks.MANGROVE_ROOTS)).withCooldown(260),
            Pages15Kit.zone("spirit_light_tree", "Spirit Light Tree",
                    pulse(Tier.ZONE, 8, 100, 20, false, VfxShape.WORLD_TREE_FX2, false, 0.04f, List.of(s(MobEffects.REGENERATION, 50, 0), s(MobEffects.MOVEMENT_SPEED, 50, 0)), List.of(), 0)).withCooldown(400),
            Pages15Kit.zone("root_eruption", "Great Root Eruption",
                    Pages15Kit.nova(Tier.ZONE, 6, false, VfxShape.TREE_ROOTS, Pages15Kit.all(Pages15Kit.lift(0.6), r(MobEffects.MOVEMENT_SLOWDOWN, 60, 3)))).withCooldown(320).withAnim("slam"),
            Pages15Kit.zone("mana_drinking_field", "Mana-Drinking Field",
                    pulse(Tier.ZONE, 6, 80, 20, false, VfxShape.TREE_DRAIN, true, 0f, List.of(), List.of(s(MobEffects.MOVEMENT_SLOWDOWN, 30, 1)), 0.015)).withCooldown(450),
            Pages15Kit.signature("world_tree_awakening", "World Tree Awakening",
                    pulse(Tier.SIGNATURE, 8, 100, 20, false, VfxShape.WORLD_TREE_FX2, true, 0.05f, List.of(s(MobEffects.REGENERATION, 50, 1)), List.of(s(MobEffects.MOVEMENT_SLOWDOWN, 40, 4)), 0.02)).withCooldown(1200).withAnim("up")));

    private static final List<BookPage> POISON = register("book_poison", List.of(
            Pages15Kit.starter("venom_needle_volley", "Venom Needle Volley",
                    Pages15Kit.volley(Tier.STARTER, 4, false, VfxShape.POISON_PLANT_FX1, r(MobEffects.POISON, 80, 0))).withCooldown(90),
            Pages15Kit.starter("blight_spit", "Blight Spit",
                    Pages15Kit.bolt(Tier.STARTER, 1.5, VfxShape.POISON_BREATH, VfxShape.POISON_CURTAIN, r(MobEffects.POISON, 100, 1))).withAnim("thrust"),
            Pages15Kit.starter("badger_burrow", "Poison Badger Burrow",
                    Pages15Kit.dash(Tier.STARTER, true, 7, false, VfxShape.POISON_BREATH, r(MobEffects.POISON, 80, 0))).withCooldown(110),
            Pages15Kit.starter("withering_touch", "Withering Touch",
                    Pages15Kit.cone(Tier.STARTER, 3.5, 0.5, VfxShape.POISON_BREATH, r(MobEffects.WITHER, 60, 0))),
            Pages15Kit.mid("miasma_mantle", "Miasma Mantle",
                    Pages15Kit.empower(Tier.MID, 200, true, VfxShape.POISON_CURTAIN, s(MobEffects.DAMAGE_RESISTANCE, 200, 0))).withCooldown(360),
            Pages15Kit.mid("antidote_draught", "Antidote Draught",
                    groupFx(Tier.MID, 6, 0.1f, true, VfxShape.POISON_CURTAIN, s(MobEffects.REGENERATION, 60, 0))).withCooldown(300),
            Pages15Kit.mid("paralytic_sting", "Paralytic Sting",
                    Pages15Kit.bind(Tier.MID, 14, false, VfxShape.POISON_CURTAIN, r(MobEffects.POISON, 100, 1))).withCooldown(300),
            Pages15Kit.mid("poison_fog_bomb", "Poison Fog Bomb",
                    Pages15Kit.nova(Tier.MID, 4, true, VfxShape.POISON_CURTAIN, Pages15Kit.all(r(MobEffects.POISON, 80, 1), r(MobEffects.BLINDNESS, 60, 0)))),
            Pages15Kit.zone("venom_rain", "Venom Rain",
                    Pages15Kit.field(Tier.ZONE, 5, 80, 20, true, VfxShape.POISON_CURTAIN, r(MobEffects.POISON, 60, 1))).withCooldown(450),
            Pages15Kit.zone("pestilence_stream", "Pestilence Stream",
                    Pages15Kit.line(Tier.ZONE, 14, 2.5, VfxShape.POISON_BREATH, Pages15Kit.all(r(MobEffects.POISON, 100, 1), r(MobEffects.WEAKNESS, 100, 0)))).withCooldown(380),
            Pages15Kit.signature("deluge_of_venom", "Deluge of Venom",
                    pulse(Tier.SIGNATURE, 8, 100, 20, true, VfxShape.POISON_CURTAIN, true, 0f, List.of(), List.of(s(MobEffects.POISON, 80, 1), s(MobEffects.WEAKNESS, 80, 0)), 0)).withCooldown(1500)));

    private static final List<BookPage> COTTON = register("book_cotton", List.of(
            Pages15Kit.starter("cotton_ball_toss", "Cotton Ball Toss",
                    Pages15Kit.bolt(Tier.STARTER, 1.5, VfxShape.COTTON_SHEEP, VfxShape.WIKI_MAGIC_BURST, Pages15Kit.knock(0.6))).withAnim("thrust"),
            Pages15Kit.starter("lamb_charge", "Lamb Charge",
                    Pages15Kit.dash(Tier.STARTER, true, 8, false, VfxShape.COTTON_SHEEP, Pages15Kit.knock(0.9))).withCooldown(100),
            Pages15Kit.starter("wool_volley", "Wool Volley",
                    Pages15Kit.volley(Tier.STARTER, 5, true, VfxShape.COTTON_SHEEP, r(MobEffects.MOVEMENT_SLOWDOWN, 30, 1))).withCooldown(100),
            Pages15Kit.starter("cotton_spring", "Cotton Spring",
                    leap(Tier.STARTER, 1.3, 0.85, false, VfxShape.COTTON_SHEEP, Pages15Kit.NONE)).withCooldown(120).withAnim("up"),
            Pages15Kit.starter("cotton_candy_snack", "Cotton Candy Snack",
                    groupFx(Tier.STARTER, 4, 0.06f, false, VfxShape.COTTON_SHEEP, s(MobEffects.SATURATION, 20, 1))).withCooldown(200),
            Pages15Kit.mid("wool_wall", "Wool Wall",
                    wall(Blocks.WHITE_WOOL)).withCooldown(240),
            Pages15Kit.mid("drowsy_wool_mist", "Drowsy Wool Mist",
                    foeFx(Tier.MID, 4, true, true, VfxShape.COTTON_SHEEP, s(MobEffects.MOVEMENT_SLOWDOWN, 100, 3), s(MobEffects.WEAKNESS, 100, 1))),
            Pages15Kit.mid("warm_wool_blanket", "Warm Wool Blanket",
                    groupFx(Tier.MID, 6, 0.08f, false, VfxShape.COTTON_SHEEP, s(MobEffects.REGENERATION, 100, 0), s(MobEffects.ABSORPTION, 200, 0))).withCooldown(320),
            Pages15Kit.zone("sheep_stampede", "Sheep Stampede",
                    Pages15Kit.line(Tier.ZONE, 14, 3, VfxShape.COTTON_SHEEP, Pages15Kit.all(Pages15Kit.knock(1.3), r(MobEffects.MOVEMENT_SLOWDOWN, 40, 1)))).withCooldown(380).withAnim("slam"),
            Pages15Kit.zone("cotton_meadow", "Cotton Meadow",
                    pulse(Tier.ZONE, 6, 100, 20, true, VfxShape.COTTON_SHEEP, false, 0.04f, List.of(s(MobEffects.REGENERATION, 50, 0)), List.of(s(MobEffects.MOVEMENT_SLOWDOWN, 40, 2)), 0)).withCooldown(450),
            Pages15Kit.signature("sleepy_sheep_parade", "Sleepy Sheep Parade",
                    Pages15Kit.volley(Tier.SIGNATURE, 5, true, VfxShape.COTTON_SHEEP, Pages15Kit.all(r(MobEffects.MOVEMENT_SLOWDOWN, 80, 4), r(MobEffects.WEAKNESS, 100, 1)))).withCooldown(1200)));

    private static final List<BookPage> PLANT = register("book_plant", List.of(
            Pages15Kit.starter("magic_flower_bullet", "Magic Flower Bullet",
                    Pages15Kit.bolt(Tier.STARTER, 1, VfxShape.PLANT_FX1, VfxShape.PLANT_FX3, Pages15Kit.NONE)).withAnim("thrust"),
            Pages15Kit.starter("pollen_puff", "Pollen Puff",
                    Pages15Kit.cone(Tier.STARTER, 5, 0.5, VfxShape.PLANT_FX3, Pages15Kit.all(r(MobEffects.WEAKNESS, 80, 0), r(MobEffects.MOVEMENT_SLOWDOWN, 60, 1)))),
            Pages15Kit.starter("bloom_leap", "Bloom Leap",
                    leap(Tier.STARTER, 1.2, 0.8, true, VfxShape.PLANT_FX3, r(MobEffects.MOVEMENT_SLOWDOWN, 40, 2))).withCooldown(120).withAnim("up"),
            Pages15Kit.mid("sunflower_beam", "Sunflower Beam",
                    Pages15Kit.line(Tier.MID, 16, 1.5, VfxShape.PLANT_FX1, r(MobEffects.GLOWING, 100, 0))).withCooldown(220),
            Pages15Kit.mid("wisteria_ward", "Wisteria Ward",
                    groupFx(Tier.MID, 5, 0f, false, VfxShape.PLANT_FX3, s(MobEffects.ABSORPTION, 200, 1), s(MobEffects.DAMAGE_RESISTANCE, 100, 0))).withCooldown(320),
            Pages15Kit.mid("lullaby_blossom", "Lullaby Blossom",
                    Pages15Kit.bind(Tier.MID, 16, false, VfxShape.PLANT_FX2, r(MobEffects.WEAKNESS, 100, 1))).withCooldown(300),
            Pages15Kit.zone("princess_healing_flower_garden", "Princess Healing Flower Garden",
                    pulse(Tier.ZONE, 7, 100, 20, false, VfxShape.PLANT_FX2, false, 0.05f, List.of(s(MobEffects.REGENERATION, 50, 0)), List.of(), 0)).withCooldown(500)));

    private static final List<BookPage> CHERRY_BLOSSOM = register("book_cherry_blossom", List.of(
            Pages15Kit.starter("petal_burst", "Petal Burst",
                    Pages15Kit.nova(Tier.STARTER, 3.5, false, VfxShape.CHERRY_BLOSSOM_FX3, Pages15Kit.knock(0.9))).withAnim("out"),
            Pages15Kit.starter("blossom_spring", "Blossom Spring",
                    leap(Tier.STARTER, 1.1, 0.9, false, VfxShape.CHERRY_BLOSSOM_FX3, Pages15Kit.NONE)).withCooldown(120).withAnim("up"),
            Pages15Kit.mid("sakura_lance", "Sakura Lance",
                    Pages15Kit.line(Tier.MID, 16, 1.2, VfxShape.CHERRY_BLOSSOM_FX1, r(MobEffects.BLINDNESS, 40, 0))).withCooldown(200).withAnim("thrust"),
            Pages15Kit.mid("petal_cyclone", "Petal Cyclone",
                    Pages15Kit.nova(Tier.MID, 5, false, VfxShape.CHERRY_BLOSSOM_FX2, Pages15Kit.all(r(MobEffects.BLINDNESS, 50, 0), Pages15Kit.knock(0.9)))).withCooldown(240).withAnim("sweep"),
            Pages15Kit.mid("scent_of_spring", "Scent of Spring",
                    groupFx(Tier.MID, 6, 0.08f, false, VfxShape.CHERRY_BLOSSOM_FX3, s(MobEffects.MOVEMENT_SPEED, 100, 1), s(MobEffects.REGENERATION, 100, 0))).withCooldown(320),
            Pages15Kit.zone("falling_petal_rain", "Falling Petal Rain",
                    Pages15Kit.field(Tier.ZONE, 6, 80, 20, true, VfxShape.CHERRY_BLOSSOM_FX2, r(MobEffects.BLINDNESS, 30, 0))).withCooldown(450)));

    private static final List<BookPage> BRIAR = register("book_briar", List.of(
            Pages15Kit.starter("thorn_spike_row", "Thorn Spike Row",
                    Pages15Kit.line(Tier.STARTER, 10, 1.2, VfxShape.BRIAR_FX1, r(MobEffects.POISON, 60, 0))).withAnim("slam"),
            Pages15Kit.starter("thorn_javelin", "Thorn Javelin",
                    Pages15Kit.bolt(Tier.STARTER, 0, VfxShape.BRIAR_FX1, VfxShape.BRIAR_FX3, r(MobEffects.POISON, 60, 0))).withAnim("thrust"),
            Pages15Kit.mid("bramble_cage", "Bramble Cage",
                    Pages15Kit.bind(Tier.MID, 16, false, VfxShape.BRIAR_FX2, r(MobEffects.POISON, 80, 0))).withCooldown(300),
            Pages15Kit.mid("briar_whirl", "Briar Whirl",
                    Pages15Kit.nova(Tier.MID, 4.5, false, VfxShape.BRIAR_FX3, Pages15Kit.all(Pages15Kit.knock(0.8), r(MobEffects.POISON, 60, 0)))).withCooldown(220).withAnim("sweep"),
            Pages15Kit.mid("healing_rose", "Healing Rose",
                    groupFx(Tier.MID, 5, 0.1f, true, VfxShape.BRIAR_FX3, s(MobEffects.REGENERATION, 80, 0))).withCooldown(320),
            Pages15Kit.zone("wild_rose_thicket", "Wild Rose Thicket",
                    Pages15Kit.field(Tier.ZONE, 6, 100, 20, true, VfxShape.BRIAR_FX2, Pages15Kit.all(r(MobEffects.POISON, 60, 0), r(MobEffects.MOVEMENT_SLOWDOWN, 40, 2)))).withCooldown(450)));

    private static final List<BookPage> TREE = register("book_tree", List.of(
            Pages15Kit.starter("acorn_barrage", "Acorn Barrage",
                    Pages15Kit.volley(Tier.STARTER, 4, false, VfxShape.TREE_FX1, Pages15Kit.knock(0.4))).withCooldown(100),
            Pages15Kit.starter("branch_snap", "Branch Snap",
                    Pages15Kit.cone(Tier.STARTER, 5, 0.4, VfxShape.TREE_FX3, Pages15Kit.knock(0.7))).withAnim("sweep"),
            Pages15Kit.mid("sap_trap", "Sap Trap",
                    Pages15Kit.bind(Tier.MID, 16, false, VfxShape.TREE_FX2, r(MobEffects.WEAKNESS, 100, 0))).withCooldown(300),
            Pages15Kit.mid("rooted_stance", "Rooted Stance",
                    Pages15Kit.empower(Tier.MID, 200, true, VfxShape.TREE_FX3, s(MobEffects.DAMAGE_RESISTANCE, 200, 1))).withCooldown(360),
            Pages15Kit.zone("whirling_leaf_storm", "Whirling Leaf Storm",
                    Pages15Kit.field(Tier.ZONE, 6, 100, 20, false, VfxShape.TREE_FX2, Pages15Kit.all(r(MobEffects.BLINDNESS, 30, 0), r(MobEffects.MOVEMENT_SLOWDOWN, 30, 1)))).withCooldown(450)));

    private static final List<BookPage> VINE = register("book_vine", List.of(
            Pages15Kit.starter("hooking_vine", "Hooking Vine",
                    pull(Tier.STARTER, 16, 2.5, VfxShape.VINE_FX1, Pages15Kit.NONE)).withCooldown(120),
            Pages15Kit.starter("thrashing_vines", "Thrashing Vines",
                    Pages15Kit.cone(Tier.STARTER, 5, 0.4, VfxShape.VINE_FX3, r(MobEffects.MOVEMENT_SLOWDOWN, 40, 1))).withAnim("sweep"),
            Pages15Kit.mid("vine_whirl", "Vine Whirl",
                    Pages15Kit.nova(Tier.MID, 4.5, false, VfxShape.VINE_FX2, Pages15Kit.all(Pages15Kit.knock(0.9), r(MobEffects.MOVEMENT_SLOWDOWN, 50, 1)))).withCooldown(220).withAnim("sweep"),
            Pages15Kit.mid("sapping_vine", "Sapping Vine",
                    Pages15Kit.bolt(Tier.MID, 2, VfxShape.VINE_FX1, VfxShape.VINE_FX3, Pages15Kit.leech(2f))).withCooldown(200).withAnim("thrust"),
            Pages15Kit.zone("creeping_overgrowth", "Creeping Overgrowth",
                    Pages15Kit.field(Tier.ZONE, 6, 100, 20, true, VfxShape.VINE_FX2, r(MobEffects.MOVEMENT_SLOWDOWN, 40, 3))).withCooldown(450)));

    private static final List<BookPage> POISON_PLANT = register("book_poison_plant", List.of(
            Pages15Kit.starter("nightshade_dart", "Nightshade Dart",
                    Pages15Kit.bolt(Tier.STARTER, 0, VfxShape.POISON_PLANT_FX1, VfxShape.POISON_PLANT_FX3, r(MobEffects.POISON, 100, 1))).withAnim("thrust"),
            Pages15Kit.starter("stinging_nettle_whip", "Stinging Nettle Whip",
                    Pages15Kit.line(Tier.STARTER, 8, 1.2, VfxShape.POISON_PLANT_FX1, Pages15Kit.all(r(MobEffects.POISON, 60, 0), r(MobEffects.MOVEMENT_SLOWDOWN, 40, 1)))).withAnim("sweep"),
            Pages15Kit.mid("venom_pod_burst", "Venom Pod Burst",
                    Pages15Kit.nova(Tier.MID, 3.5, true, VfxShape.POISON_PLANT_FX3, Pages15Kit.all(r(MobEffects.POISON, 100, 1), r(MobEffects.WITHER, 40, 0)))),
            Pages15Kit.mid("foxglove_seize", "Foxglove Seize",
                    Pages15Kit.bind(Tier.MID, 14, false, VfxShape.POISON_PLANT_FX2, r(MobEffects.POISON, 100, 1))).withCooldown(300),
            Pages15Kit.zone("hemlock_haze", "Hemlock Haze",
                    Pages15Kit.field(Tier.ZONE, 6, 100, 20, true, VfxShape.POISON_PLANT_FX2, Pages15Kit.all(r(MobEffects.POISON, 60, 1), r(MobEffects.BLINDNESS, 30, 0)))).withCooldown(450)));

    private static final List<BookPage> BEAST = register("book_beast", List.of(
            Pages15Kit.starter("tiger_pounce", "Tiger Pounce",
                    leap(Tier.STARTER, 1.4, 0.55, true, VfxShape.BEAST_FX3, r(MobEffects.MOVEMENT_SLOWDOWN, 30, 1))).withCooldown(120).withAnim("up"),
            Pages15Kit.starter("hawk_eye", "Hawk Eye",
                    foeFx(Tier.STARTER, 14, false, false, VfxShape.BEAST_FX1, s(MobEffects.GLOWING, 160, 0))).withCooldown(200),
            Pages15Kit.mid("holy_hunting", "Holy Hunting",
                    Pages15Kit.dash(Tier.MID, true, 12, true, VfxShape.BEAST_FX1, r(MobEffects.GLOWING, 100, 0))).withCooldown(200).withAnim("thrust"),
            Pages15Kit.mid("bogeyman", "Bogeyman",
                    Pages15Kit.nova(Tier.MID, 5, false, VfxShape.BEAST_FX3, Pages15Kit.all(r(MobEffects.BLINDNESS, 60, 0), r(MobEffects.WEAKNESS, 100, 1)))),
            Pages15Kit.zone("holy_howling", "Holy Howling",
                    Pages15Kit.cone(Tier.ZONE, 10, 0.3, VfxShape.BEAST_FX2, Pages15Kit.all(Pages15Kit.knock(1.2), r(MobEffects.WEAKNESS, 100, 0)))).withCooldown(380).withAnim("out")));

    private static final List<BookPage> BODY = register("book_body", List.of(
            Pages15Kit.starter("shoulder_tackle", "Shoulder Tackle",
                    Pages15Kit.dash(Tier.STARTER, true, 6, false, VfxShape.BODY_FX1, Pages15Kit.knock(1.0))),
            Pages15Kit.starter("pressure_point_jab", "Pressure Point Jab",
                    single(Tier.STARTER, 5, VfxShape.BODY_FX1, VfxShape.BODY_FX3, r(MobEffects.WEAKNESS, 80, 0))).withCooldown(100).withAnim("thrust"),
            Pages15Kit.mid("adrenaline_surge", "Adrenaline Surge",
                    selfFx(Tier.MID, VfxShape.BODY_FX3, s(MobEffects.MOVEMENT_SPEED, 160, 1), s(MobEffects.DAMAGE_BOOST, 160, 0), s(MobEffects.JUMP, 160, 0))).withCooldown(360),
            Pages15Kit.mid("second_wind", "Second Wind",
                    groupFx(Tier.MID, 4, 0.15f, true, VfxShape.BODY_FX3)).withCooldown(320),
            Pages15Kit.zone("titan_shockwave", "Titan Shockwave",
                    Pages15Kit.nova(Tier.ZONE, 6, false, VfxShape.BODY_FX2, Pages15Kit.all(Pages15Kit.knock(1.2), Pages15Kit.lift(0.4)))).withCooldown(380).withAnim("slam")));

    private static final List<BookPage> FUNGUS = register("book_fungus", List.of(
            Pages15Kit.starter("mushroom_cap_bounce", "Mushroom Cap Bounce",
                    leap(Tier.STARTER, 1.0, 0.95, true, VfxShape.FUNGUS_FX3, r(MobEffects.POISON, 60, 0))).withCooldown(120).withAnim("up"),
            Pages15Kit.mid("puffball_barrage", "Puffball Barrage",
                    Pages15Kit.volley(Tier.MID, 5, true, VfxShape.FUNGUS_FX1, r(MobEffects.POISON, 60, 0))).withCooldown(200),
            Pages15Kit.zone("deathcap_garden", "Deathcap Garden",
                    Pages15Kit.field(Tier.ZONE, 5, 100, 20, true, VfxShape.FUNGUS_FX2, Pages15Kit.all(r(MobEffects.POISON, 60, 1), r(MobEffects.WEAKNESS, 40, 0)))).withCooldown(450)));

    private static final List<BookPage> DEMON_BEAST = register("book_demon_beast", List.of(
            Pages15Kit.starter("chimera_tail_lash", "Chimera Tail Lash",
                    Pages15Kit.line(Tier.STARTER, 7, 1.5, VfxShape.DEMON_BEAST_FX1, r(MobEffects.WEAKNESS, 60, 0))).withAnim("sweep"),
            Pages15Kit.mid("cerberus_triple_bite", "Cerberus Triple Bite",
                    Pages15Kit.volley(Tier.MID, 3, true, VfxShape.DEMON_BEAST_FX1, Pages15Kit.leech(1f))).withCooldown(200),
            Pages15Kit.zone("underworld_stampede", "Underworld Stampede",
                    Pages15Kit.line(Tier.ZONE, 14, 3, VfxShape.DEMON_BEAST_FX2, Pages15Kit.all(Pages15Kit.knock(1.2), r(MobEffects.WEAKNESS, 80, 1)))).withCooldown(400).withAnim("slam")));


    /** The pages this batch appends to the book (empty for a book it has not written yet, or a book of another batch). Never null. */
    static List<BookPage> pages(String bookPath) {
        return PAGES.getOrDefault(bookPath, List.of());
    }
}
