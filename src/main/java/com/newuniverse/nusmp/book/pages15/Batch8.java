package com.newuniverse.nusmp.book.pages15;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.ElementBook.Rider;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.Tier;
import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Batch 8 of the "15 spells for every grimoire" project: Seal, Transform and Body.
 *
 * <p>Books of this batch (the only books {@link Pages15} asks this class about; see {@link Pages15Plan}): {@code book_sealing}, {@code book_skin}, {@code book_tongue}, {@code book_switching}, {@code book_recombination}, {@code book_transmutation}, {@code book_butoh}, {@code book_modification}, {@code book_hair}, {@code book_nail}, {@code book_scale}, {@code book_spike}, {@code book_chain}, {@code book_barrier}.
 *
 * <p>94 pages, appended AFTER every old page of their book in the order written here (page ids are save keys: never reorder or rename one once released). Most pages use the kit's standard
 * casts; the rest are custom casts built from a few bounded helpers at the bottom of this class (every one goes through {@link Pages15Kit.Ctx}: at most the tier's targets, at most
 * {@value Pages15Rules#MAX_PACKETS} packets, zones of at most 100 ticks in steps of 4+, nothing spawned, nothing placed, no scan beyond the spell's own box). The research record, the canon /
 * original mark of every page and the worst case are in docs/magic/pages15_batch8.md.
 *
 * <p>Recombination Magic is special: {@code ElementBook.vfx} spawns a prop entity for every effect of that book, so its pages never use the kit's standard casts; they send their
 * effects straight through {@link VfxSpawn} (no entity at all) with {@link Fx}.
 */
public final class Batch8 {
    private Batch8() {}

    /** Book registry path -> the pages this batch appends to it. */
    private static final Map<String, List<BookPage>> PAGES = new LinkedHashMap<>();

    private static List<BookPage> register(String book, List<BookPage> pages) {
        PAGES.put(book, List.copyOf(new ArrayList<>(pages)));
        return pages;
    }


    // ---------------------------------------------------------------- the pictures (each book's own effects, see docs/magic/page_counts.md section 8)
    private static final VfxShape S1 = VfxShape.SEALING_FX1, S2 = VfxShape.SEALING_FX2, S3 = VfxShape.SEALING_FX3;
    private static final VfxShape W = VfxShape.WIKI_MAGIC_CAST, WF = VfxShape.WIKI_MAGIC_FIELD, WB = VfxShape.WIKI_MAGIC_BURST;
    private static final VfxShape R1 = VfxShape.RECOMBINATION_FX1, R2 = VfxShape.RECOMBINATION_FX2, R3 = VfxShape.RECOMBINATION_FX3;
    private static final VfxShape T1 = VfxShape.TRANSMUTATION_FX1, T2 = VfxShape.TRANSMUTATION_FX2, T3 = VfxShape.TRANSMUTATION_FX3;
    private static final VfxShape B1 = VfxShape.BUTOH_FX1, B2 = VfxShape.BUTOH_FX2, B3 = VfxShape.BUTOH_FX3;
    private static final VfxShape M1 = VfxShape.MODIFICATION_FX1, M2 = VfxShape.MODIFICATION_FX2;
    private static final VfxShape HR1 = VfxShape.HAIR_FX1, HR2 = VfxShape.HAIR_FX2;
    private static final VfxShape NL1 = VfxShape.NAIL_FX1, NL2 = VfxShape.NAIL_FX2, NL3 = VfxShape.NAIL_FX3;
    private static final VfxShape SC1 = VfxShape.SCALE_FX1, SC2 = VfxShape.SCALE_FX2, SC3 = VfxShape.SCALE_FX3;
    private static final VfxShape SP1 = VfxShape.SPIKE_FX1, SP2 = VfxShape.SPIKE_FX2;
    private static final VfxShape CH1 = VfxShape.CHAIN_FX1, CH2 = VfxShape.CHAIN_FX2, CH3 = VfxShape.CHAIN_FX3;
    private static final VfxShape BR2 = VfxShape.BARRIER_FX2, BR3 = VfxShape.BARRIER_FX3;

    // ---------------------------------------------------------------- the books of this batch, one register(...) each
    private static final List<BookPage> SEALING = register("book_sealing", List.of(
            Pages15Kit.starter("seal_stamp", "Seal Magic: Sealing Stamp",
                    Pages15Kit.bolt(Tier.STARTER, 1.5, S1, S3, Pages15Kit.all(seal(40), slow(40, 1)))).withAnim("thrust"),
            Pages15Kit.starter("sealing_needles", "Seal Magic: Sealing Needles",
                    Pages15Kit.volley(Tier.STARTER, 4, true, S1, jam(30))).withCooldown(120),
            Pages15Kit.starter("seal_ward_mark", "Seal Magic: Ward Mark",
                    Pages15Kit.cast(Tier.STARTER, c -> {
                    c.caster.getPersistentData().putLong(ManaLaw.SEALED, 0L);
                    return buffRun(c, S2, 1.2f, eff(MobEffects.ABSORPTION, 160, 1), eff(MobEffects.DAMAGE_RESISTANCE, 160, 0));
                })),
            Pages15Kit.mid("seal_rune_lattice", "Seal Magic: Rune Lattice",
                    Pages15Kit.cone(Tier.MID, 6, 0.5, S2, Pages15Kit.all(slow(50, 2), jam(40)))).withCooldown(200).withAnim("sweep"),
            Pages15Kit.mid("seal_gate_closing", "Seal Magic: Closing Gate",
                    Pages15Kit.line(Tier.MID, 14, 1.5, S1, Pages15Kit.all(Pages15Kit.knock(0.8), jam(50)))).withCooldown(220).withAnim("thrust"),
            Pages15Kit.mid("protective_seal", "Seal Magic: Protective Seal",
                    teamBuff(Tier.MID, S2, 8, eff(MobEffects.ABSORPTION, 200, 1), eff(MobEffects.DAMAGE_RESISTANCE, 200, 0))).withCooldown(400),
            Pages15Kit.mid("seal_sigil_burst", "Seal Magic: Sigil Burst",
                    Pages15Kit.nova(Tier.MID, 4.5, true, S3, Pages15Kit.all(jam(40), Pages15Kit.knock(0.5)))).withCooldown(200).withAnim("slam"),
            Pages15Kit.zone("seal_sigil_field", "Seal Magic: Sigil Field",
                    Pages15Kit.field(Tier.ZONE, 5, 80, 10, true, S2, Pages15Kit.all(slow(30, 1), jam(30)))).withCooldown(450),
            Pages15Kit.zone("seal_pillars", "Seal Magic: Pillars of Seal",
                    pillars(Tier.ZONE, 12, S2, Pages15Kit.all(jam(40), slow(40, 1)), 0.6)).withCooldown(400).withAnim("slam"),
            Pages15Kit.signature("forbidden_seal_art", "Seal Magic: Forbidden Seal Art",
                    Pages15Kit.cast(Tier.SIGNATURE, c -> {
                    LivingEntity main = c.target(18);
                    if (main == null) return c.fail("No one to seal.");
                    List<LivingEntity> foes = c.foes(main.position(), 5);
                    if (foes.isEmpty()) return c.fail("No one to seal.");
                    if (!GrimoireBook.control(c.caster)) return false;
                    Vec3 at = main.position();
                    Fx fx = new Fx(c);
                    c.circle(1.6f);
                    fx.shape(S2, at.add(0, 0.05, 0), at.add(0, 1, 0), 80, 2.4f);
                    Rider seals = Pages15Kit.all(seal(160), jam(100), weak(100, 1));
                    for (LivingEntity e : foes) {
                        c.hit(e);
                        seals.apply(e, c.caster);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(e, 80), 9));
                        fx.shape(VfxShape.SEAL_CHAINS, centre(e), e.position(), 60, Math.max(0.8f, e.getBbHeight() / 1.8f));
                    }
                    fx.shape(S3, at, at.add(0, 1, 0), 30, 1.6f);
                    return true;
                })).withCooldown(1500).withAnim("seal_crush")));

    private static final List<BookPage> SKIN = register("book_skin", List.of(
            Pages15Kit.starter("skin_leather_lash", "Skin Magic: Leather Lash",
                    Pages15Kit.cone(Tier.STARTER, 4, 0.45, W, Pages15Kit.knock(0.7))).withAnim("sweep"),
            Pages15Kit.starter("skin_hardened_hide", "Skin Magic: Hardened Hide",
                    selfBuff(Tier.STARTER, WF, 0.5f, eff(MobEffects.DAMAGE_RESISTANCE, 100, 1), eff(MobEffects.MOVEMENT_SLOWDOWN, 100, 0))).withCooldown(160),
            Pages15Kit.starter("soft_skin", "Skin Magic: Soft Skin",
                    selfBuff(Tier.STARTER, WF, 0.5f, eff(MobEffects.ABSORPTION, 160, 2), eff(MobEffects.SLOW_FALLING, 160, 0))).withCooldown(160),
            Pages15Kit.mid("peeling_skin", "Skin Magic: Peeling Skin",
                    Pages15Kit.volley(Tier.MID, 4, true, W, weak(60, 0))).withCooldown(200),
            Pages15Kit.mid("skin_bristling_hide", "Skin Magic: Bristling Hide",
                    Pages15Kit.empower(Tier.MID, 200, true, WF)).withCooldown(300),
            Pages15Kit.mid("skin_graft", "Skin Magic: Skin Graft",
                    cleanse(Tier.MID, WF, 0.2f)).withCooldown(400),
            Pages15Kit.mid("skin_stretch", "Skin Magic: Stretching Skin",
                    Pages15Kit.dash(Tier.MID, true, 9, false, W, Pages15Kit.knock(0.6))).withCooldown(180).withAnim("thrust"),
            Pages15Kit.zone("skin_wrap", "Skin Magic: Skin Wrap",
                    Pages15Kit.bind(Tier.ZONE, 14, false, W, weak(80, 1))).withCooldown(450),
            Pages15Kit.zone("skin_blistering_field", "Skin Magic: Blistering Field",
                    Pages15Kit.field(Tier.ZONE, 5, 80, 10, true, WF, Pages15Kit.all(slow(30, 1), weak(30, 0)))).withCooldown(450),
            Pages15Kit.signature("skin_cast_off", "Skin Magic: Cast-Off Skin",
                    Pages15Kit.cast(Tier.SIGNATURE, c -> {
                    Vec3 at = c.caster.position();
                    Fx fx = new Fx(c);
                    c.circle(1.5f);
                    Rider flung = Pages15Kit.all(Pages15Kit.knock(1.1), weak(80, 1));
                    for (LivingEntity e : c.foes(at, 6)) {
                        c.hit(e);
                        flung.apply(e, c.caster);
                    }
                    fx.shape(WB, at.add(0, 0.8, 0), at.add(0, 1.8, 0), 36, 2.2f);
                    vanish(c, 80);
                    c.caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1));
                    c.caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 1));
                    return true;
                })).withCooldown(1200).withAnim("slam")));

    private static final List<BookPage> TONGUE = register("book_tongue", List.of(
            Pages15Kit.starter("tongue_lash", "Tongue Magic: Tongue Lash",
                    Pages15Kit.line(Tier.STARTER, 8, 0.9, W, slow(30, 1))).withAnim("thrust"),
            Pages15Kit.starter("tongue_snatch", "Tongue Magic: Tongue Snatch",
                    snatch(Tier.STARTER, 14, W, Pages15Kit.NONE)).withCooldown(160),
            Pages15Kit.starter("tongue_acid_spit", "Tongue Magic: Acid Spit",
                    Pages15Kit.bolt(Tier.STARTER, 1.5, W, WB, poison(60, 0))),
            Pages15Kit.mid("tongue_whip_crack", "Tongue Magic: Whip Crack",
                    Pages15Kit.cone(Tier.MID, 6, 0.3, W, Pages15Kit.knock(1.1))).withCooldown(200).withAnim("sweep"),
            Pages15Kit.mid("tongue_swing", "Tongue Magic: Tongue Swing",
                    swing(Tier.MID, 22, W)).withCooldown(240),
            Pages15Kit.mid("tongue_savouring_lick", "Tongue Magic: Savouring Lick",
                    Pages15Kit.cone(Tier.MID, 3.5, 0.6, W, Pages15Kit.leech(2.5f))).withCooldown(200).withAnim("sweep"),
            Pages15Kit.mid("tongue_numbing_saliva", "Tongue Magic: Numbing Saliva",
                    Pages15Kit.bolt(Tier.MID, 3, W, WB, Pages15Kit.all(slow(60, 2), weak(60, 0)))).withCooldown(220),
            Pages15Kit.zone("wrapping_tongue", "Tongue Magic: Wrapping Tongue",
                    Pages15Kit.bind(Tier.ZONE, 16, false, W, slow(60, 3))).withCooldown(450),
            Pages15Kit.zone("tongue_lashing_barrage", "Tongue Magic: Lashing Barrage",
                    flurry(Tier.ZONE, 5, 5, 16, W, Pages15Kit.knock(0.2))).withCooldown(400).withAnim("thrust"),
            Pages15Kit.signature("tongue_great_swallow", "Tongue Magic: Great Swallow",
                    Pages15Kit.cast(Tier.SIGNATURE, c -> {
                    LivingEntity e = c.target(14);
                    if (e == null || e.isAlliedTo(c.caster)) return c.fail("Nothing to swallow.");
                    if (!GrimoireBook.control(c.caster)) return false;
                    Fx fx = new Fx(c);
                    c.circle(1.4f);
                    if (!BalanceLaw.isBoss(e)) {
                        Vec3 front = c.caster.position().add(c.caster.getViewVector(1f).multiply(1, 0, 1).normalize().scale(2.0));
                        if (fits(e, front)) place(e, front);
                    }
                    fx.shape(W, c.caster.getEyePosition(), centre(e), 16, 1.6f);
                    c.hit(e);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(e, 60), 9));
                    BalanceLaw.heal(c.caster, 4f);
                    c.later(14, () -> {
                        if (!e.isAlive()) return;
                        c.hit(e, c.tier.hit * 0.5f);
                        fx.shape(WB, centre(e), centre(e).add(0, 1, 0), 20, 1.4f);
                    });
                    return true;
                })).withCooldown(1500).withAnim("slam")));

    private static final List<BookPage> SWITCHING = register("book_switching", List.of(
            Pages15Kit.starter("switching_place_swap", "Switching Magic: Place Swap",
                    swapWithTarget(Tier.STARTER, 16, WB)),
            Pages15Kit.starter("blink_switch", "Switching Magic: Blink Switch",
                    Pages15Kit.dash(Tier.STARTER, false, 7, false, W, Pages15Kit.NONE)).withCooldown(100),
            Pages15Kit.starter("switching_inversion_pulse", "Switching Magic: Inversion Pulse",
                    Pages15Kit.nova(Tier.STARTER, 4.5, false, WB, Pages15Kit.knock(-0.9))),
            Pages15Kit.mid("ailment_swap", "Switching Magic: Ailment Swap",
                    Pages15Kit.cast(Tier.MID, c -> {
                    LivingEntity e = c.target(16);
                    if (e == null || e == c.caster || e.isAlliedTo(c.caster)) return c.fail("No one to pass your ailments to.");
                    List<MobEffectInstance> bad = new ArrayList<>();
                    for (MobEffectInstance m : c.caster.getActiveEffects()) if (!m.getEffect().value().isBeneficial()) bad.add(m);
                    if (bad.isEmpty()) return c.fail("You carry no ailment.");
                    int n = 0;
                    for (MobEffectInstance m : bad) {
                        if (n++ >= 3) break;
                        c.caster.removeEffect(m.getEffect());
                        e.addEffect(new MobEffectInstance(m.getEffect(), soft(e, Math.min(m.getDuration(), 100)), Math.min(m.getAmplifier(), 2)));
                    }
                    c.hit(e, c.tier.hit * 0.5f);
                    c.circle(0.8f);
                    new Fx(c).shape(WB, c.caster.getEyePosition(), centre(e), 16, 1f);
                    return true;
                })).withCooldown(240),
            Pages15Kit.mid("aggro_switch", "Switching Magic: Aggro Switch",
                    Pages15Kit.cast(Tier.MID, c -> {
                    List<Mob> mobs = new ArrayList<>();
                    for (LivingEntity e : c.foes(c.caster.position(), 10)) if (e instanceof Mob m && !BalanceLaw.isBoss(m)) mobs.add(m);
                    if (mobs.size() < 2) return c.fail("Needs two creatures to turn on each other.");
                    Fx fx = new Fx(c);
                    c.circle(1f);
                    for (int k = 0; k < mobs.size(); k++) {
                        Mob m = mobs.get(k);
                        m.setTarget(mobs.get((k + 1) % mobs.size()));
                        nausea(40).apply(m, c.caster);
                        fx.shape(WB, centre(m), centre(m).add(0, 1, 0), 16, 0.7f);
                    }
                    return true;
                })).withCooldown(300),
            Pages15Kit.mid("foe_swap", "Switching Magic: Foe Swap",
                    Pages15Kit.cast(Tier.MID, c -> {
                    List<LivingEntity> foes = c.foes(c.aim(20), 8);
                    if (foes.size() < 2) return c.fail("Needs two foes to switch.");
                    LivingEntity a = foes.get(0), b = foes.get(1);
                    Vec3 pa = a.position(), pb = b.position();
                    if (!fits(a, pb) || !fits(b, pa)) return c.fail("No room to switch them.");
                    place(a, pb);
                    place(b, pa);
                    for (LivingEntity e : List.of(a, b)) {
                        c.hit(e, c.tier.hit * 0.6f);
                        nausea(50).apply(e, c.caster);
                    }
                    c.circle(0.9f);
                    Fx fx = new Fx(c);
                    fx.shape(WB, pa.add(0, 0.2, 0), pa.add(0, 1.4, 0), 24, 1f);
                    fx.shape(WB, pb.add(0, 0.2, 0), pb.add(0, 1.4, 0), 24, 1f);
                    return true;
                })).withCooldown(240),
            Pages15Kit.mid("up_down_switch", "Switching Magic: Up-Down Switch",
                    Pages15Kit.nova(Tier.MID, 4, true, WB, Pages15Kit.all(Pages15Kit.lift(0.8), Pages15Kit.effect(() -> new MobEffectInstance(MobEffects.LEVITATION, 25, 0))))).withCooldown(220).withAnim("up"),
            Pages15Kit.zone("switching_tangle_field", "Switching Magic: Tangle Field",
                    Pages15Kit.field(Tier.ZONE, 5, 80, 10, true, WF, Pages15Kit.all(nausea(50), Pages15Kit.knock(-0.4)))).withCooldown(450),
            Pages15Kit.zone("shuffling_barrage", "Switching Magic: Shuffling Barrage",
                    Pages15Kit.volley(Tier.ZONE, 5, true, W, Pages15Kit.knock(-0.4))).withCooldown(350),
            Pages15Kit.signature("grand_reversal", "Switching Magic: Grand Reversal",
                    Pages15Kit.cast(Tier.SIGNATURE, c -> {
                    List<LivingEntity> foes = c.foes(c.caster.position(), 8);
                    if (foes.size() < 2) return c.fail("Needs at least two foes to shuffle.");
                    List<Vec3> spots = new ArrayList<>();
                    for (LivingEntity e : foes) spots.add(e.position());
                    Fx fx = new Fx(c);
                    c.circle(1.5f);
                    for (int k = 0; k < foes.size(); k++) {
                        LivingEntity e = foes.get(k);
                        Vec3 to = spots.get((k + 1) % spots.size());
                        if (fits(e, to)) {
                            fx.shape(WB, e.position(), e.position().add(0, 1.2, 0), 20, 1f);
                            place(e, to);
                        }
                        c.hit(e);
                        nausea(80).apply(e, c.caster);
                    }
                    Vec3 at = c.caster.position();
                    fx.shape(WF, at.add(0, 0.05, 0), at.add(0, 1, 0), 40, 2.6f);
                    return true;
                })).withCooldown(1500).withAnim("slam")));

    private static final List<BookPage> RECOMBINATION = register("book_recombination", List.of(
            Pages15Kit.starter("house_door_slam", "Recombination: Door Slam",
                    xCone(Tier.STARTER, 4, 0.4, R3, Pages15Kit.knock(0.9))).withAnim("sweep"),
            Pages15Kit.starter("house_plank_volley", "Recombination: Plank Volley",
                    xFan(Tier.STARTER, 3, 12, 14, 0.9, R1, Pages15Kit.NONE)).withCooldown(100).withAnim("thrust"),
            Pages15Kit.starter("house_stair_leap", "Recombination: Stair Leap",
                    leap(Tier.STARTER, 0.9, 0.9, 40, R2)).withCooldown(120),
            Pages15Kit.mid("house_chimney_cannon", "Recombination: Chimney Cannon",
                    xLine(Tier.MID, 18, 1.2, R1, Pages15Kit.knock(0.9))).withCooldown(240).withAnim("thrust"),
            Pages15Kit.mid("house_trapdoor", "Recombination: Trapdoor",
                    xNova(Tier.MID, 3.5, true, R2, Pages15Kit.all(slow(60, 3), drop()))).withCooldown(220).withAnim("slam"),
            Pages15Kit.mid("house_scaffold_span", "Recombination: Scaffold Span",
                    teamBuff(Tier.MID, R2, 8, eff(MobEffects.MOVEMENT_SPEED, 160, 0), eff(MobEffects.JUMP, 160, 1), eff(MobEffects.SLOW_FALLING, 160, 0))).withCooldown(400),
            Pages15Kit.zone("house_roof_slam", "Recombination: Roof Slam",
                    xNova(Tier.ZONE, 5, true, R3, Pages15Kit.all(slow(40, 2), Pages15Kit.knock(0.6)))).withCooldown(400).withAnim("slam"),
            Pages15Kit.zone("house_hallway_maze", "Recombination: Hallway Maze",
                    xField(Tier.ZONE, 5, 80, 10, true, R2, Pages15Kit.all(slow(30, 1), nausea(40)))).withCooldown(450),
            Pages15Kit.signature("house_collapse", "Recombination: House Collapse",
                    Pages15Kit.cast(Tier.SIGNATURE, c -> {
                    Vec3 at = c.aim(24);
                    Fx fx = new Fx(c);
                    c.circle(1.6f);
                    fx.shape(R2, at.add(0, 0.05, 0), at.add(0, 1, 0), 30, 7f / 1.5f);
                    fx.shape(R1, at.add(0, 12, 0), at, 16, 1.6f);
                    Rider crush = Pages15Kit.all(slow(50, 2), Pages15Kit.knock(0.6), Pages15Kit.lift(0.4));
                    c.later(16, () -> {
                        for (LivingEntity e : c.foes(at, 7)) {
                            c.hit(e);
                            crush.apply(e, c.caster);
                        }
                        fx.shape(R3, at, at.add(0, 1.5, 0), 36, 2.4f);
                    });
                    return true;
                })).withCooldown(1500).withAnim("slam")));

    private static final List<BookPage> TRANSMUTATION = register("book_transmutation", List.of(
            Pages15Kit.starter("alchemic_copper_bolts", "Transmutation: Copper Bolts",
                    Pages15Kit.bolt(Tier.STARTER, 1.2, T1, T3, weak(40, 0))),
            Pages15Kit.starter("alchemic_sand_shower", "Transmutation: Sand Shower",
                    Pages15Kit.cone(Tier.STARTER, 5, 0.5, T1, blind(40))).withAnim("sweep"),
            Pages15Kit.mid("alchemic_rubber_spring", "Transmutation: Rubber Spring",
                    leap(Tier.MID, 1.2, 0.5, 80, T2)).withCooldown(200),
            Pages15Kit.mid("alchemic_lead_weights", "Transmutation: Lead Weights",
                    Pages15Kit.bind(Tier.MID, 14, false, T1, weak(60, 1))).withCooldown(220),
            Pages15Kit.mid("alchemic_brass_whips", "Transmutation: Brass Whips",
                    Pages15Kit.line(Tier.MID, 10, 1.5, T1, Pages15Kit.knock(0.8))).withCooldown(200).withAnim("sweep"),
            Pages15Kit.zone("alchemic_salt_flats", "Transmutation: Salt Flats",
                    Pages15Kit.field(Tier.ZONE, 5, 100, 10, true, T2, Pages15Kit.all(slow(30, 1), wither(30, 0)))).withCooldown(480),
            Pages15Kit.zone("alchemic_iron_rain", "Transmutation: Iron Rain",
                    Pages15Kit.field(Tier.ZONE, 4.5, 80, 8, true, T2, Pages15Kit.knock(0.2))).withCooldown(400),
            Pages15Kit.signature("living_statue", "Transformation: Living Statue",
                    Pages15Kit.cast(Tier.SIGNATURE, c -> {
                    Vec3 at = c.aim(20);
                    List<LivingEntity> foes = c.foes(at, 5);
                    if (foes.isEmpty()) return c.fail("No one to turn to stone.");
                    if (!GrimoireBook.control(c.caster)) return false;
                    Fx fx = new Fx(c);
                    c.circle(1.5f);
                    fx.shape(T2, at.add(0, 0.05, 0), at.add(0, 1, 0), 60, 3f);
                    for (LivingEntity e : foes) {
                        c.hit(e);
                        int tk = BalanceLaw.controlTicks(e, 80);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, tk, 9));
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, tk, 1));
                        e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, tk, 2));
                        fx.shape(T3, centre(e), centre(e).add(0, 1, 0), 30, 1f);
                    }
                    return true;
                })).withCooldown(1500).withAnim("slam")));

    private static final List<BookPage> BUTOH = register("book_butoh", List.of(
            Pages15Kit.starter("butoh_hand_sway", "Swaying Hand",
                    Pages15Kit.cone(Tier.STARTER, 4.5, 0.4, B1, slow(30, 1))).withAnim("sweep"),
            Pages15Kit.starter("butoh_heel_stamp", "Heel Stamp",
                    Pages15Kit.nova(Tier.STARTER, 3.5, false, B3, Pages15Kit.knock(0.8))),
            Pages15Kit.mid("butoh_mirror_step", "Mirror Step",
                    Pages15Kit.dash(Tier.MID, true, 9, true, B1, Pages15Kit.knock(0.4))).withCooldown(200).withAnim("thrust"),
            Pages15Kit.mid("butoh_serpent_arm", "Serpent Arm",
                    Pages15Kit.line(Tier.MID, 9, 1.0, B1, Pages15Kit.knock(-0.7))).withCooldown(200).withAnim("thrust"),
            Pages15Kit.mid("butoh_echo_step", "Echo of the Dancer",
                    Pages15Kit.cast(Tier.MID, c -> {
                    Vec3 at = c.caster.position();
                    Fx fx = new Fx(c);
                    c.circle(0.8f);
                    fx.shape(VfxShape.BUTOH_AFTERIMAGE, at, at.add(0, 1, 0), 50, 1f);
                    fx.shape(VfxShape.BUTOH_DANCE_RIPPLE, at.add(0, 0.05, 0), at, 40, 1.4f);
                    vanish(c, 60);
                    c.caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0));
                    return true;
                })).withCooldown(300),
            Pages15Kit.zone("butoh_crescendo", "Crescendo",
                    Pages15Kit.field(Tier.ZONE, 5, 80, 10, false, B2, Pages15Kit.all(jam(30), slow(30, 1)))).withCooldown(450)));

    private static final List<BookPage> MODIFICATION = register("book_modification", List.of(
            Pages15Kit.starter("modification_scalpel_cut", "Modification: Scalpel Cut",
                    Pages15Kit.line(Tier.STARTER, 6, 0.8, M1, wither(40, 0))).withAnim("sweep"),
            Pages15Kit.starter("modification_adrenal_edit", "Modification: Adrenal Edit",
                    selfBuff(Tier.STARTER, M2, 0.8f, eff(MobEffects.MOVEMENT_SPEED, 120, 1), eff(MobEffects.DAMAGE_BOOST, 120, 0))).withCooldown(200),
            Pages15Kit.mid("modification_nerve_edit", "Modification: Nerve Edit",
                    Pages15Kit.bind(Tier.MID, 14, false, M1, weak(60, 1))).withCooldown(220),
            Pages15Kit.mid("modification_regeneration", "Modification: Cell Regrowth",
                    cleanse(Tier.MID, M2, 0.25f)).withCooldown(400),
            Pages15Kit.zone("modification_mutagen_field", "Modification: Mutagen Field",
                    Pages15Kit.field(Tier.ZONE, 5, 80, 10, true, M2, Pages15Kit.all(poison(40, 0), weak(40, 0)))).withCooldown(450)));

    private static final List<BookPage> HAIR = register("book_hair", List.of(
            Pages15Kit.starter("hair_reach", "Hair Reach",
                    Pages15Kit.line(Tier.STARTER, 16, 0.8, HR1, slow(30, 1))).withAnim("thrust"),
            Pages15Kit.starter("hair_needles", "Hair Needles",
                    Pages15Kit.volley(Tier.STARTER, 5, true, HR1, Pages15Kit.NONE)).withCooldown(120),
            Pages15Kit.mid("hair_wall", "Hair Wall",
                    Pages15Kit.cast(Tier.MID, c -> {
                    if (!c.zone(60, 4, age -> SpellRuntime.dissolveBolts(c.caster.serverLevel(), c.caster.position(), 4.0, c.caster, 4))) return false;
                    c.circle(1f);
                    Vec3 at = c.caster.position();
                    new Fx(c).shape(HR2, at.add(0, 0.05, 0), at.add(0, 1.5, 0), 70, 1.6f);
                    c.caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0));
                    return true;
                })).withCooldown(300),
            Pages15Kit.mid("hair_spear", "Hair Spear",
                    Pages15Kit.line(Tier.MID, 12, 1.0, HR1, Pages15Kit.all(Pages15Kit.knock(0.6), weak(40, 0)))).withCooldown(200).withAnim("thrust"),
            Pages15Kit.zone("hair_forest", "Hair Forest",
                    Pages15Kit.field(Tier.ZONE, 5, 80, 10, true, HR2, slow(40, 2))).withCooldown(450)));

    private static final List<BookPage> NAIL = register("book_nail", List.of(
            Pages15Kit.starter("nail_claws", "Elongated Nails",
                    Pages15Kit.cone(Tier.STARTER, 3.5, 0.5, NL1, wither(40, 0))).withAnim("sweep"),
            Pages15Kit.starter("nail_hook", "Nail Hook",
                    swing(Tier.STARTER, 18, NL1)).withCooldown(200),
            Pages15Kit.mid("nail_lance", "Nail Lance",
                    Pages15Kit.line(Tier.MID, 12, 1.0, NL1, Pages15Kit.all(slow(40, 1), Pages15Kit.knock(0.4)))).withCooldown(200).withAnim("thrust"),
            Pages15Kit.mid("nail_hardening", "Hardened Nails",
                    Pages15Kit.empower(Tier.MID, 240, false, NL3)).withCooldown(300),
            Pages15Kit.zone("nail_thicket", "Nail Thicket",
                    Pages15Kit.field(Tier.ZONE, 4.5, 80, 10, true, NL2, Pages15Kit.all(slow(30, 1), wither(30, 0)))).withCooldown(450)));

    private static final List<BookPage> SCALE = register("book_scale", List.of(
            Pages15Kit.starter("scale_dart", "Scale Dart",
                    Pages15Kit.bolt(Tier.STARTER, 1.2, SC1, SC3, weak(40, 0))),
            Pages15Kit.starter("scale_slide", "Scale Slide",
                    Pages15Kit.dash(Tier.STARTER, false, 8, false, SC1, Pages15Kit.NONE)).withCooldown(100),
            Pages15Kit.mid("scale_cyclone", "Scale Cyclone",
                    Pages15Kit.nova(Tier.MID, 4.5, false, SC3, Pages15Kit.knock(0.9))).withCooldown(200).withAnim("slam"),
            Pages15Kit.mid("scale_dragon_tail", "Dragon Tail Sweep",
                    Pages15Kit.line(Tier.MID, 8, 2.0, SC1, Pages15Kit.knock(1.0))).withCooldown(200).withAnim("sweep"),
            Pages15Kit.zone("scale_reef", "Scale Reef",
                    Pages15Kit.field(Tier.ZONE, 5, 80, 10, true, SC2, Pages15Kit.all(slow(30, 1), weak(30, 0)))).withCooldown(450)));

    private static final List<BookPage> SPIKE = register("book_spike", List.of(
            Pages15Kit.starter("spike_jab", "Spike Jab",
                    Pages15Kit.cone(Tier.STARTER, 3.5, 0.65, SP1, slow(30, 1))).withAnim("thrust"),
            Pages15Kit.starter("spike_vault", "Spike Vault",
                    leap(Tier.STARTER, 1.0, 0.6, 40, SP2)).withCooldown(120),
            Pages15Kit.mid("spike_lance", "Spike Lance",
                    Pages15Kit.line(Tier.MID, 12, 1.0, SP1, Pages15Kit.knock(0.4))).withCooldown(200).withAnim("thrust"),
            Pages15Kit.mid("spike_snare", "Spike Snare",
                    Pages15Kit.bind(Tier.MID, 14, false, SP2, weak(40, 0))).withCooldown(220),
            Pages15Kit.zone("spike_garden", "Spike Garden",
                    Pages15Kit.field(Tier.ZONE, 5, 100, 10, true, SP2, slow(30, 1))).withCooldown(450)));

    private static final List<BookPage> CHAIN = register("book_chain", List.of(
            Pages15Kit.starter("chain_hook_throw", "Hooked Chain",
                    Pages15Kit.bolt(Tier.STARTER, 0, CH1, CH3, Pages15Kit.all(slow(30, 1), Pages15Kit.knock(-0.7)))).withAnim("thrust"),
            Pages15Kit.mid("chain_whirl", "Chain Whirl",
                    Pages15Kit.nova(Tier.MID, 5, false, CH3, Pages15Kit.all(Pages15Kit.knock(0.8), slow(40, 1)))).withCooldown(200).withAnim("sweep"),
            Pages15Kit.zone("chain_downpour", "Chain Downpour",
                    Pages15Kit.field(Tier.ZONE, 5, 80, 10, true, CH2, Pages15Kit.all(slow(30, 1), Pages15Kit.knock(0.2)))).withCooldown(450)));


    /** The pages this batch appends to the book (empty for a book it has not written yet, or a book of another batch). Never null. */
    static List<BookPage> pages(String bookPath) {
        return PAGES.getOrDefault(bookPath, List.of());
    }

    // ================================================================ riders (what a hit does besides damage)
    /** Bosses get half the time of a status effect. */
    private static int soft(LivingEntity t, int ticks) { return BalanceLaw.isBoss(t) ? Math.max(5, ticks / 2) : ticks; }

    private static Rider status(Holder<MobEffect> e, int ticks, int amp) {
        int tk = Math.min(ticks, 160), am = Math.min(amp, 4);
        return (t, p) -> t.addEffect(new MobEffectInstance(e, soft(t, tk), am));
    }

    private static Rider slow(int ticks, int amp) { return status(MobEffects.MOVEMENT_SLOWDOWN, ticks, amp); }

    private static Rider weak(int ticks, int amp) { return status(MobEffects.WEAKNESS, ticks, amp); }

    private static Rider blind(int ticks) { return status(MobEffects.BLINDNESS, ticks, 0); }

    private static Rider nausea(int ticks) { return status(MobEffects.CONFUSION, ticks, 0); }

    private static Rider poison(int ticks, int amp) { return status(MobEffects.POISON, ticks, amp); }

    private static Rider wither(int ticks, int amp) { return status(MobEffects.WITHER, ticks, amp); }

    /** Magic Jamming: the foe's casting is cut (a boss for 1 s at most), as the Butoh and Chain books do. */
    private static Rider jam(int ticks) {
        int tk = Math.min(ticks, 100);
        return (t, p) -> EnergyBridge.effect(t, "silence", BalanceLaw.isBoss(t) ? Math.min(tk, 20) : tk, 0);
    }

    /** The seal of the Sealing book: a mage's grimoire shuts (a boss for 1 s at most). */
    private static Rider seal(int ticks) {
        int tk = Math.min(ticks, 160);
        return (t, p) -> {
            if (t instanceof Player) t.getPersistentData().putLong(ManaLaw.SEALED, t.level().getGameTime() + (BalanceLaw.isBoss(t) ? 20 : tk));
        };
    }

    /** The foe is pressed to the ground. */
    private static Rider drop() {
        return (t, p) -> {
            t.setDeltaMovement(t.getDeltaMovement().x * 0.3, -1.0, t.getDeltaMovement().z * 0.3);
            t.hurtMarked = true;
        };
    }

    private static Supplier<MobEffectInstance> eff(Holder<MobEffect> e, int ticks, int amp) {
        int tk = Math.min(ticks, 400), am = Math.min(amp, 3);
        return () -> new MobEffectInstance(e, tk, am);
    }

    // ================================================================ effects: the kit's counter plus a way past the prop of Recombination
    /** Sends the effects of one cast: through the kit (at most 10 packets) or, for Recombination Magic (whose book would spawn a prop entity per effect), straight through VfxSpawn. */
    private static final class Fx {
        private final Pages15Kit.Ctx c;
        private final boolean direct;
        private int n;

        Fx(Pages15Kit.Ctx c) {
            this.c = c;
            this.direct = c.book.magic == MagicType.RECOMBINATION;
        }

        void shape(VfxShape s, Vec3 from, Vec3 to, int ticks, float power) {
            if (n >= Pages15Rules.MAX_PACKETS - 1) return;
            n++;
            int tk = Math.max(4, Math.min(ticks, 120));
            float pw = Math.max(0.2f, Math.min(power, 4f));
            if (direct) VfxSpawn.send(c.caster.serverLevel(), s, from, to, c.book.color, tk, pw);
            else c.fx(s, from, to, tk, pw);
        }
    }

    private static float sz(Pages15Kit.Ctx c) { return Math.max(0.8f, Math.min(c.size(), 1.6f)); }

    private static boolean fits(LivingEntity who, Vec3 at) {
        return who.level().noCollision(who, who.getBoundingBox().move(at.subtract(who.position())));
    }

    private static void place(LivingEntity e, Vec3 to) {
        if (e instanceof ServerPlayer sp) sp.teleportTo(to.x, to.y, to.z);
        else {
            e.moveTo(to.x, to.y, to.z);
            if (e instanceof Mob m) m.getNavigation().stop();
        }
        e.setDeltaMovement(Vec3.ZERO);
        e.hurtMarked = true;
        e.fallDistance = 0;
    }

    private static Vec3 centre(LivingEntity e) { return e.getBoundingBox().getCenter(); }

    // ================================================================ custom casts (all bounded through the Ctx)
    /** You gain the effects. */
    @SafeVarargs
    private static boolean buffRun(Pages15Kit.Ctx c, VfxShape shape, float power, Supplier<MobEffectInstance>... effects) {
        c.circle(0.8f);
        for (Supplier<MobEffectInstance> e : effects) c.caster.addEffect(e.get());
        Vec3 at = c.caster.position();
        new Fx(c).shape(shape, at.add(0, 0.05, 0), at.add(0, 1.2, 0), 40, power);
        return true;
    }

    @SafeVarargs
    private static BookPage.Cast selfBuff(Tier t, VfxShape shape, float power, Supplier<MobEffectInstance>... effects) {
        return Pages15Kit.cast(t, c -> buffRun(c, shape, power, effects));
    }

    /** You and up to five allies within {@code radius} (at most 8) gain the effects. */
    @SafeVarargs
    private static BookPage.Cast teamBuff(Tier t, VfxShape shape, double radius, Supplier<MobEffectInstance>... effects) {
        double r = Math.min(radius, 8.0);
        return Pages15Kit.cast(t, c -> {
            List<ServerPlayer> who = c.caster.serverLevel().getEntitiesOfClass(ServerPlayer.class, c.caster.getBoundingBox().inflate(r),
                    a -> a.isAlive() && !a.isSpectator() && (a == c.caster || a.isAlliedTo(c.caster)) && a.distanceToSqr(c.caster) <= r * r);
            c.circle(1f);
            Fx fx = new Fx(c);
            int n = 0;
            for (ServerPlayer a : who) {
                if (n++ >= 6) break;
                for (Supplier<MobEffectInstance> e : effects) a.addEffect(e.get());
                if (a != c.caster) fx.shape(shape, a.position().add(0, 0.05, 0), a.position().add(0, 1.2, 0), 30, 0.8f);
            }
            Vec3 at = c.caster.position();
            fx.shape(shape, at.add(0, 0.05, 0), at.add(0, 1.2, 0), 50, (float) (r / 2.5));
            return true;
        });
    }

    /** Harmful effects leave you and a part of your health returns (the heal decays like every heal). */
    private static BookPage.Cast cleanse(Tier t, VfxShape shape, float fraction) {
        return Pages15Kit.cast(t, c -> {
            for (var e : new ArrayList<>(c.caster.getActiveEffects())) if (!e.getEffect().value().isBeneficial()) c.caster.removeEffect(e.getEffect());
            c.caster.getPersistentData().putLong(ManaLaw.SEALED, 0L);
            BalanceLaw.heal(c.caster, c.caster.getMaxHealth() * Math.min(fraction, 0.25f));
            c.circle(0.8f);
            Vec3 at = c.caster.position();
            new Fx(c).shape(shape, at.add(0, 0.05, 0), at.add(0, 1.2, 0), 40, 1.1f);
            return true;
        });
    }

    /** You are thrown {@code up} and {@code forward} (a hop, a vault, a spring) and fall softly for a moment. */
    private static BookPage.Cast leap(Tier t, double up, double forward, int slowFall, VfxShape shape) {
        double u = Math.min(up, 1.3), f = Math.min(forward, 1.2);
        return Pages15Kit.cast(t, c -> {
            Vec3 look = c.caster.getViewVector(1f), flat = new Vec3(look.x, 0, look.z).normalize();
            Vec3 from = c.caster.position();
            c.caster.setDeltaMovement(flat.x * f, u, flat.z * f);
            c.caster.hurtMarked = true;
            c.caster.fallDistance = 0;
            if (slowFall > 0) c.caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, slowFall, 0));
            c.circle(0.6f);
            new Fx(c).shape(shape, from.add(0, 0.05, 0), from.add(flat.scale(2)).add(0, u * 2, 0), 16, 1f);
            return true;
        });
    }

    /** A line anchors on the block or foe you aim at (3 to {@code range} blocks) and pulls you across. */
    private static BookPage.Cast swing(Tier t, double range, VfxShape shape) {
        double rg = Math.min(range, 26.0);
        return Pages15Kit.cast(t, c -> {
            Vec3 eye = c.caster.getEyePosition(), at = c.aim(rg);
            double dist = at.distanceTo(eye);
            if (dist < 3.0) return c.fail("Too close to anchor.");
            if (dist > rg - 0.3) return c.fail("Nothing to hook.");
            Vec3 pull = at.subtract(c.caster.position().add(0, 1, 0)).normalize().scale(Math.min(2.2, 0.8 + dist * 0.07));
            c.caster.setDeltaMovement(pull.x, pull.y + 0.25, pull.z);
            c.caster.hurtMarked = true;
            c.caster.fallDistance = 0;
            c.caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 50, 0));
            c.circle(0.6f);
            new Fx(c).shape(shape, eye, at, 14, 0.9f);
            return true;
        });
    }

    /** You and the foe under your crosshair change places (the foe is slowed a moment). */
    private static BookPage.Cast swapWithTarget(Tier t, double range, VfxShape shape) {
        return Pages15Kit.cast(t, c -> {
            LivingEntity e = c.target(Math.min(range, 24.0));
            if (e == null || e == c.caster || e.isAlliedTo(c.caster)) return c.fail("No one to switch with.");
            Vec3 a = c.caster.position(), b = e.position();
            if (!fits(c.caster, b) || !fits(e, a)) return c.fail("No room to switch.");
            place(c.caster, b);
            place(e, a);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, soft(e, 30), 1));
            c.circle(0.7f);
            Fx fx = new Fx(c);
            fx.shape(shape, a.add(0, 0.2, 0), a.add(0, 1.4, 0), 24, 1f);
            fx.shape(shape, b.add(0, 0.2, 0), b.add(0, 1.4, 0), 24, 1f);
            return true;
        });
    }

    /** The foe under your crosshair is hit and reeled to you (hard control: shares the 12 s lock; a boss is only hit). */
    private static BookPage.Cast snatch(Tier t, double range, VfxShape shape, Rider rider) {
        return Pages15Kit.cast(t, c -> {
            LivingEntity e = c.target(Math.min(range, 24.0));
            if (e == null) return c.fail("No one to snatch.");
            if (!GrimoireBook.control(c.caster)) return false;
            c.hit(e);
            rider.apply(e, c.caster);
            if (!BalanceLaw.isBoss(e)) {
                Vec3 d = c.caster.position().subtract(e.position());
                double len = Math.max(0.1, d.length());
                e.setDeltaMovement(d.scale(1.0 / len).scale(Math.min(1.6, 0.5 + len * 0.08)).add(0, 0.2, 0));
                e.hurtMarked = true;
            }
            c.circle(0.7f);
            new Fx(c).shape(shape, c.caster.getEyePosition(), centre(e), 14, 1f);
            return true;
        });
    }

    /** {@code hits} blows on the foe under your crosshair, {@code gap} ticks apart (the whole volley stays inside 100 ticks and about 1.3 hits of the tier). */
    private static BookPage.Cast flurry(Tier t, int hits, int gap, double range, VfxShape shape, Rider rider) {
        int n = Math.max(2, Math.min(hits, 6)), g = Math.max(2, Math.min(gap, 100 / n));
        return Pages15Kit.cast(t, c -> {
            LivingEntity e = c.target(Math.min(range, 24.0));
            if (e == null) return c.fail("No target.");
            float each = t.hit * 1.3f / n;
            Fx fx = new Fx(c);
            c.circle(0.7f);
            for (int k = 0; k < n; k++) {
                int kk = k;
                Runnable blow = () -> {
                    if (!e.isAlive()) return;
                    c.hit(e, each);
                    rider.apply(e, c.caster);
                    fx.shape(shape, c.caster.getEyePosition(), centre(e), 10, 0.8f);
                };
                if (kk == 0) blow.run();
                else c.later(kk * g, blow);
            }
            return true;
        });
    }

    /** A line of pillars rising one after another ahead of you: each foe on it is hit, thrown up a little and gets the rider. */
    private static BookPage.Cast pillars(Tier t, double length, VfxShape shape, Rider rider, double lift) {
        double len = Math.min(length, 16.0);
        return Pages15Kit.cast(t, c -> {
            Vec3 flat = c.caster.getViewVector(1f).multiply(1, 0, 1).normalize();
            Vec3 a = c.caster.position(), b = a.add(flat.scale(len * sz(c)));
            c.circle(0.8f);
            Fx fx = new Fx(c);
            for (int k = 1; k <= 6; k++) {
                Vec3 p = a.add(b.subtract(a).scale(k / 6.0));
                fx.shape(shape, p, p.add(0, 1.4, 0), 16 + k * 2, 0.8f);
            }
            for (LivingEntity e : c.foesAlong(a.add(0, 0.5, 0), b.add(0, 0.5, 0), 1.4)) {
                c.hit(e);
                rider.apply(e, c.caster);
                e.setDeltaMovement(e.getDeltaMovement().add(0, Math.min(lift, 0.8), 0));
                e.hurtMarked = true;
            }
            return true;
        });
    }

    /** Mobs hunting you lose your trail and you fade for {@code ticks}. */
    private static void vanish(Pages15Kit.Ctx c, int ticks) {
        for (Mob m : c.caster.serverLevel().getEntitiesOfClass(Mob.class, c.caster.getBoundingBox().inflate(16), m -> m.getTarget() == c.caster)) m.setTarget(null);
        c.caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Math.min(ticks, 100), 0));
    }

    // ---- the same shapes as the kit's standard casts, for the books whose effects must not go through ElementBook.vfx (Recombination)
    private static BookPage.Cast xCone(Tier t, double range, double width, VfxShape shape, Rider rider) {
        return Pages15Kit.cast(t, c -> {
            Vec3 eye = c.caster.getEyePosition(), look = c.caster.getViewVector(1f);
            double r = Math.min(range * sz(c), 12.0);
            for (LivingEntity e : c.foes(c.caster.position(), r)) {
                if (centre(e).subtract(eye).normalize().dot(look) < width) continue;
                c.hit(e);
                rider.apply(e, c.caster);
            }
            c.circle(0.6f);
            new Fx(c).shape(shape, eye, eye.add(look.scale(r)), 14, (float) r / 4f);
            return true;
        });
    }

    private static BookPage.Cast xLine(Tier t, double length, double width, VfxShape shape, Rider rider) {
        return Pages15Kit.cast(t, c -> {
            Vec3 a = c.caster.getEyePosition(), b = a.add(c.caster.getViewVector(1f).scale(Math.min(length * sz(c), 24.0)));
            for (LivingEntity e : c.foesAlong(a, b, Math.min(width, 3.0))) {
                c.hit(e);
                rider.apply(e, c.caster);
            }
            c.circle(0.8f);
            new Fx(c).shape(shape, a, b, 18, 1.2f);
            return true;
        });
    }

    /** {@code n} (2..5) lines in a fan of {@code spread} degrees; a foe on several of them is hit once. */
    private static BookPage.Cast xFan(Tier t, int n, double spread, double length, double width, VfxShape shape, Rider rider) {
        int k = Math.max(2, Math.min(n, 5));
        return Pages15Kit.cast(t, c -> {
            Vec3 a = c.caster.getEyePosition(), look = c.caster.getViewVector(1f);
            Set<LivingEntity> seen = new LinkedHashSet<>();
            Fx fx = new Fx(c);
            c.circle(0.8f);
            for (int i = 0; i < k; i++) {
                double ang = Math.toRadians(spread * (i - (k - 1) / 2.0));
                Vec3 d = new Vec3(look.x * Math.cos(ang) - look.z * Math.sin(ang), look.y, look.x * Math.sin(ang) + look.z * Math.cos(ang)).normalize();
                Vec3 b = a.add(d.scale(Math.min(length, 24.0)));
                fx.shape(shape, a, b, 14, 0.8f);
                seen.addAll(c.foesAlong(a, b, Math.min(width, 3.0)));
            }
            int hits = 0;
            for (LivingEntity e : seen) {
                if (hits++ >= t.maxTargets) break;
                c.hit(e, t.hit * 0.9f);
                rider.apply(e, c.caster);
            }
            return true;
        });
    }

    private static BookPage.Cast xNova(Tier t, double radius, boolean atAim, VfxShape shape, Rider rider) {
        return Pages15Kit.cast(t, c -> {
            Vec3 centre = atAim ? c.aim(24) : c.caster.position();
            double r = Math.min(radius * sz(c), 8.0);
            for (LivingEntity e : c.foes(centre, r)) {
                c.hit(e);
                rider.apply(e, c.caster);
            }
            c.circle(1f);
            new Fx(c).shape(shape, centre, centre.add(0, 1, 0), 24, (float) (r / 3));
            return true;
        });
    }

    private static BookPage.Cast xField(Tier t, double radius, int ticks, int interval, boolean atAim, VfxShape shape, Rider rider) {
        int tk = Math.max(Pages15Rules.MIN_ZONE_STEP, Math.min(ticks, Pages15Rules.MAX_ZONE_TICKS)), step = Math.max(Pages15Rules.MIN_ZONE_STEP, interval);
        return Pages15Kit.cast(t, c -> {
            Vec3 fixed = atAim ? c.aim(24) : c.caster.position();
            double r = Math.min(radius * sz(c), 8.0);
            c.circle(1f);
            Fx fx = new Fx(c);
            if (!c.zone(tk, step, age -> {
                Vec3 at = atAim ? fixed : c.caster.position();
                for (LivingEntity e : c.foes(at, r)) {
                    c.hit(e, t.pulse);
                    rider.apply(e, c.caster);
                }
                fx.shape(shape, at, at.add(0, 1, 0), Math.max(10, step + 6), (float) r / 3f);
            })) return false;
            return true;
        });
    }
}
