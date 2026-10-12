package com.newuniverse.nusmp.entity.zogratis;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Zenon Zogratis, the youngest of the Dark Triad and the quietest, contracted to the devil Beelzebub. A fast assassin-caster of
 * medium health (950) who keeps his distance, says almost nothing and strikes from angles nobody was looking at.
 * <ul>
 *   <li><b>Native magic, Bone Magic</b> ({@link ZenonMoves}): dense near-indestructible bone grown from his body and thrown. The Bone
 *       Spear Barrage (an arch of spears launched in a fan), the Bone Blade Dance (a close combo), the Dense Bone Lance (one line, locked
 *       in the wind-up), the Bone Garden (rings that erupt, and bone columns that stay a while when the griefBlocks rule allows),
 *       and the Bone Armour (a shell that swallows a share of his health in damage, and bites back when it shatters).</li>
 *   <li><b>Devil magic, Spatial Magic</b>: every blink goes through a visible entry portal and a visible exit portal. The Rift
 *       Backstab (the exit portal opens behind you in the wind-up), the Portal Link (spears enter portals beside him and come out of
 *       portals round you), the Warp Barrage (blinks round you, a spear from each), and Spatial Evasion: the first real blow after a
 *       few seconds is slipped through a rift. He never slips a blow while he casts, while he is exposed, or against anti-magic.</li>
 *   <li><b>Domain Expansion, Spatial Mana Domination</b> (60% and up): a cube of warped space ({@link SpellSeal}) in which everyone
 *       who is not a Zogratis is cut off from mana: no grimoire spells, no Tensura skills, spell damage cut to a fifth. It is shown for
 *       a second in the wind-up, closes for another, can be walked out of, ends with its time, and shatters if he is hurt enough.
 *       At the Gates it is larger, lasts longer and the portals chain. He is exposed for three seconds after opening it.</li>
 *   <li><b>Power</b>: at each step of the curve a Beelzebub-infused Dark Disciple answers him; at 100% (Gates open, hybrid form)
 *       the disciples come in waves, the cube is larger and his evasion comes back every three seconds.</li>
 * </ul>
 * Every big move is telegraphed for at least 8 ticks (the domain for three seconds), has a counter, and the places it lands are
 * locked where they were drawn, so moving is always the answer.
 */
public class ZenonBoss extends ZogratisBoss {
    private static final int RGB = DevilStyle.color(Devil.BEELZEBUB), ACCENT = DevilStyle.accent(Devil.BEELZEBUB), WARN = 0xFF4A4A, BONE_RGB = 0xE9E4D4;

    /** Cells of bone columns a garden grew, planted a few per tick and taken back after {@link BoneMath#GROWTH_LIFE} ticks. */
    private static final class Batch {
        final ArrayDeque<BlockPos> cells;
        int placed;
        long retractAt = -1;

        Batch(List<BlockPos> cells) { this.cells = new ArrayDeque<>(cells); }
    }

    private static final int PLANT_PER_TICK = 24, RETRACT_PER_TICK = 48;

    private final BoneShell shell = new BoneShell();
    private final ArrayDeque<Batch> batches = new ArrayDeque<>();
    private List<Vec3> marks = List.of();
    private String marksOf = "";
    private Vec3 lanceDir;
    private boolean shellUp;
    private long evadeReady, lastQuip, lastShellNote;

    public ZenonBoss(EntityType<ZenonBoss> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return ZogratisBoss.bossAttributes(ZenonTable.HEALTH, ZenonTable.ATTACK, ZenonTable.SPEED, ZenonTable.ARMOR, ZenonTable.TOUGHNESS);
    }

    @Override public String bossName() { return "Zenon Zogratis"; }
    @Override public Devil devil() { return Devil.BEELZEBUB; }

    // ================================================================ tuning
    @Override protected float formScale(int form) { return form == FORM_HYBRID ? ZenonTable.HYBRID_SCALE : 1.0f; }

    /** Precise rather than heavy; and a little more while his own cube stands (he rules the space). */
    @Override
    protected float damageScale() {
        return ZenonTable.DAMAGE_SCALE * (SpellSeal.active(getUUID()) ? (float) DomainMath.RULER_DAMAGE : 1f);
    }

    /** He does not brawl: the plain blow is a stab, lighter than the telegraphed combos. */
    @Override protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7f; }

    /** An assassin-caster: he keeps six to eleven blocks from his target and lets the moves close the distance. */
    @Override protected double preferredRange(double suggested) { return Math.max(6, Math.min(11, suggested)); }

    @Override protected String[] tensuraKeywords() { return new String[]{"space", "dimension"}; }
    @Override protected int tensuraKitSize() { return 2; }

    // ================================================================ lines (cold, quiet, few words)
    @Override protected String openingLine() { return "...Trespassers. This space belongs to us."; }

    @Override
    protected String powerLine(int old, int now) {
        if (now >= 80 && old < 80) return "Eighty. Beelzebub folds the space for me.";
        if (now >= PowerCurve.INFINITE_MANA_FROM && old < PowerCurve.INFINITE_MANA_FROM) return "Sixty. My mana has no end now.";
        if (now >= 40 && old < 40) return "Forty.";
        return "I draw " + now + "% of Beelzebub.";
    }

    @Override protected String gatesLine() { return "The Gates open. Beelzebub... all of space, then."; }
    @Override protected String deathLine() { return "...Lucius. The space... slips."; }

    /** One of 'lines' now and then (at most every eight seconds), so the chat stays quiet. */
    private void quip(String... lines) {
        if (tickCount - lastQuip < 160) return;
        lastQuip = tickCount;
        say(lines[getRandom().nextInt(lines.length)]);
    }

    // ================================================================ the fight's hooks
    @Override
    protected void onFightStart() {
        takeMarks("");
        lanceDir = null;
        shell.drop();
        shellUp = false;
        batches.clear();
        evadeReady = level().getGameTime() + 100;
        announce("Spatial Evasion: Zenon slips through a rift to dodge one real blow every few seconds, but never while he casts, while he is exposed, or against anti-magic.");
        announce("Spatial Mana Domination severs the mana of everyone inside its cube: leave it on foot, or hurt Zenon enough to shatter it.");
    }

    @Override
    protected void onFightEnd(boolean won) {
        SpellSeal.clear(getUUID());
        shell.drop();
        shellUp = false;
        batches.clear();
        takeMarks("");
        lanceDir = null;
    }

    @Override
    protected void onPowerChanged(int old, int now) {
        int before = PowerCurve.stage(old), after = PowerCurve.stage(now);
        if (after > before && after < 4 && summonDisciples(after) > 0) announce("Beelzebub's Dark Disciples answer Zenon, without a word.");
    }

    @Override
    protected void onGatesOpened() {
        announce("Space folds round Zenon. His portals chain, his cube widens, and his evasion returns every three seconds.");
        for (int delay : new int[]{80, 260, 460}) {
            later(delay, ZenonMoves.safe("disciple wave", () -> {
                if (fightActive() && summonDisciples(2) > 0) announce("A wave of Dark Disciples steps out of Beelzebub's rifts.");
            }));
        }
    }

    @Override
    protected void bossTick() {
        if (!(level() instanceof ServerLevel sl)) return;
        try {
            long now = sl.getGameTime();
            if (shellUp) {
                if (!shell.active(now)) {
                    shellUp = false;
                    shell.drop();
                    announce("Zenon's bone armour crumbles.");
                } else if (tickCount % 8 == 0) Tells.gather(sl, position().add(0, 1, 0), 0.7, BONE_RGB, 3);
            }
            tickGrowth(sl, now);
            if (tickCount % 10 == 0 && SpellSeal.active(getUUID()) && state() != STATE_TRANSFORMING) Tells.gather(sl, position().add(0, 1, 0), 1.0, ACCENT, 6);
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Zenon's tick failed: {}", e.toString());
        }
    }

    // ================================================================ damage: the domain, evasion, the armour
    /** What a blow will really do to him once the invulnerability frames and the exposed bonus have had their say (0 = nothing). */
    private float effective(DamageSource source, float amount) {
        float incoming = exposed() ? amount * ZogratisBoss.EXPOSED_BONUS : amount;
        if (invulnerableTime > 10 && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
            if (incoming <= lastHurt) return 0f;
            incoming -= lastHurt;
        }
        return incoming;
    }

    /**
     * In order: the first real blow after a while is slipped through a rift (and wears nothing down); every blow that lands wears down
     * his domain, armour or not; the bone armour, if he wears it, swallows what is left.
     */
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel sl)) return amount;
        try {
            float eff = effective(source, amount);
            if (eff <= 0f) return amount;
            long now = sl.getGameTime();
            if (canEvade(source, eff, now)) {
                Vec3 from = source.getEntity() != null ? source.getEntity().position() : null;
                if (ZenonMoves.slip(this, from)) {
                    evadeReady = now + ZenonTable.evadeCooldown(power());
                    quip("Too slow.", "Predictable.");
                    return 0f;
                }
            }
            if (SpellSeal.active(getUUID()) && SpellSeal.hurtDomain(getUUID(), eff, SpellSeal.antiMagicHit(source))) domainBroken();
            if (shell.active(now)) return throughShell(sl, now, amount, eff);
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Zenon's defence failed: {}", e.toString());
        }
        return amount;
    }

    private boolean canEvade(DamageSource source, float eff, long now) {
        return now >= evadeReady && state() == STATE_COMBAT && eff >= ZenonTable.EVADE_MIN && source.getEntity() != null
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !SpellSeal.antiMagicHit(source);
    }

    private float throughShell(ServerLevel sl, long now, float amount, float eff) {
        double left = shell.absorb(now, eff);
        sl.playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.2f, 1.3f);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState()), getX(), getY() + 1.0, getZ(), 6, 0.3, 0.4, 0.3, 0.05);
        if (!shell.active(now)) {
            shellUp = false;
            shell.drop();
            announce("The bone armour shatters!");
            ZenonMoves.shardBurst(this, ZenonTable.SHARDS);
        } else if (now - lastShellNote > 60) {
            lastShellNote = now;
            announce("Zenon's bone armour holds (" + Math.round(shell.fraction() * 100) + "% left). Burst it, or wait it out.");
        }
        float bonus = exposed() ? ZogratisBoss.EXPOSED_BONUS : 1f;
        return Math.max(0f, amount - (float) (eff - left) / bonus);
    }

    /** The cube was shattered: he is staggered for three seconds (Slowness III) and says so. */
    private void domainBroken() {
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        quip("...Tch.", "My space... cut.");
    }

    // ================================================================ bone growths: planted a few per tick, taken back later
    private void tickGrowth(ServerLevel sl, long now) {
        if (batches.isEmpty()) return;
        if (!fightActive()) {
            batches.clear();
            return;
        }
        int budget = Math.min(blockBudget(), gatesOpen() ? PLANT_PER_TICK * 2 : PLANT_PER_TICK);        // the Gates: bone grows twice as fast
        for (Batch b : batches) {
            while (budget > 0 && !b.cells.isEmpty()) {
                BlockPos cell = b.cells.poll();
                if (!sl.getEntities((Entity) null, new AABB(cell)).isEmpty()) continue;       // never close a block on someone
                if (tryChange(cell, Blocks.BONE_BLOCK.defaultBlockState())) {
                    b.placed++;
                    budget--;
                }
            }
            if (b.cells.isEmpty() && b.retractAt < 0) b.retractAt = now + BoneMath.GROWTH_LIFE;
        }
        Batch oldest = batches.peekFirst();
        if (oldest != null && oldest.cells.isEmpty() && oldest.retractAt >= 0 && now >= oldest.retractAt) {
            int n = Math.min(RETRACT_PER_TICK, oldest.placed);
            if (n > 0) ledger().restoreStep(n);
            oldest.placed -= n;
            if (oldest.placed <= 0) batches.pollFirst();
        }
    }

    // ================================================================ moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (ZenonTable.Spec s : ZenonTable.specs()) {
            Move.Builder b = Move.builder(s.id(), s.label()).minPower(s.minPower()).windUp(s.windUpTicks()).cooldown(s.cooldownTicks())
                    .range(s.minRange(), s.maxRange()).weight(s.weight()).mana(s.manaCost()).recovery(s.recovery());
            if (s.ultimate()) b.ultimate();
            if (s.crowd()) b.crowd();
            Move.MoveTell tell = tellOf(s.id());
            if (tell != null) b.tell(tell);
            out.add(b.run(runOf(s.id())));
        }
        return out;
    }

    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case ZenonTable.BONE_SPEARS -> (b, t) -> {
                ZenonMoves.boneSpears(this, t, BoneMath.spearCount(tuning().castSlots()), ZenonTable.SPEAR);
                quip("Pierce.", "Hold still.", "Bone does not break.");
            };
            case ZenonTable.BONE_BLADES -> (b, t) -> {
                ZenonMoves.boneBlades(this, t, BoneMath.bladeSlashes(tuning().castSlots()), ZenonTable.BLADE);
                quip("Close.", "Too near.");
            };
            case ZenonTable.BONE_GARDEN -> (b, t) -> {
                List<Vec3> spots = takeMarks(ZenonTable.BONE_GARDEN);
                if (spots.isEmpty()) spots = gardenSpots(t);
                List<BlockPos> cells = ZenonMoves.boneGarden(this, spots, ZenonTable.GARDEN, gatesOpen());
                if (!cells.isEmpty()) batches.add(new Batch(cells));
                quip("Grow.", "The ground has teeth.");
            };
            case ZenonTable.BACKSTAB -> (b, t) -> {
                List<Vec3> m = takeMarks(ZenonTable.BACKSTAB);
                Vec3 behind = m.isEmpty() ? null : m.get(0);
                ZenonMoves.riftBackstab(this, t, behind, ZenonTable.BACKSTAB_HIT);
                quip("Behind you.", "Turn around.");
            };
            case ZenonTable.PORTAL_LINK -> (b, t) -> {
                ZenonMoves.portalLink(this, t, PortalMath.linkPairs(power()), ZenonTable.PORTAL_SPEAR, PortalMath.linkWaves(power()));
                quip("Through the rift.", "Watch the gates.");
            };
            case ZenonTable.BONE_LANCE -> (b, t) -> {
                Vec3 from = position().add(0, getBbHeight() * 0.75, 0);
                Vec3 dir = lanceDir != null ? lanceDir : t.getBoundingBox().getCenter().subtract(from);
                lanceDir = null;
                ZenonMoves.boneLance(this, from, dir, ZenonTable.LANCE);
                quip("Dense bone. Indestructible.");
            };
            case ZenonTable.BONE_ARMOR -> (b, t) -> {
                long now = level().getGameTime();
                shell.raise(BoneMath.shellPool(getMaxHealth(), power()), now + BoneMath.SHELL_TICKS);
                shellUp = true;
                if (level() instanceof ServerLevel sl) {
                    Vec3 at = position().add(0, 1, 0);
                    VfxSpawn.send(sl, VfxShape.BONE_FX3, at, at, ZenonMoves.BONE, 20, 0.8f);
                    sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.6f, 0.5f);
                }
                announce("Zenon grows a shell of bone: it takes " + Math.round(shell.pool()) + " damage and breaks outward. Burst it, or wait 8 s.");
            };
            case ZenonTable.WARP_BARRAGE -> (b, t) -> {
                ZenonMoves.warpBarrage(this, t, PortalMath.warpHops(power()), ZenonTable.WARP_SPEAR);
                quip("Everywhere.", "Where am I?");
            };
            case ZenonTable.TENSURA_SPACE -> (b, t) -> {
                if (!castTensura(t)) ZenonMoves.boneSpears(this, t, 3, ZenonTable.SPEAR);
                quip("Cut.");
            };
            case ZenonTable.CALL_DISCIPLES -> (b, t) -> {
                int made = summonDisciples(2 + (gatesOpen() ? 1 : 0));
                if (made > 0) {
                    if (level() instanceof ServerLevel sl)
                        VfxSpawn.send(sl, VfxShape.DEVIL_CIRCLE, position().add(0, 0.05, 0), position().add(0, 1, 0), ZenonMoves.INDIGO, 40, 2f);
                    quip("Go.");
                } else quip("...All of you.");
            };
            case ZenonTable.DOMINATION -> (b, t) -> runDomain(t);
            default -> (b, t) -> { };
        };
    }

    /** Spatial Mana Domination: the cube where the wind-up drew it, with Zenon inside it; at the Gates the portals chain through it. */
    private void runDomain(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        List<Vec3> locked = takeMarks(ZenonTable.DOMINATION);
        Vec3 centre = locked.isEmpty() ? ZogratisBoss.ground(sl, target.position()) : locked.get(0);
        double half = DomainMath.half(power());
        int ticks = DomainMath.ticks(power());
        DomainCube cube = DomainCube.around(centre.x, centre.y, centre.z, half);
        if (!cube.contains(getX(), getY() + 1, getZ())) {
            double[] r = PortalMath.retreat(getX(), getZ(), centre.x, centre.z, half * 0.6);
            Vec3 in = ZenonMoves.landing(sl, this, new Vec3(r[0], centre.y, r[1]));
            if (in != null) blinkTo(in);
        }
        ZenonMoves.spatialDomain(this, centre, half, ticks);
        say("Spatial Mana Domination.");
        announce("A cube of warped space, " + Math.round(half * 2) + " blocks across, closes round " + Math.round(centre.x) + ", " + Math.round(centre.z) + ". For "
                + ticks / 20 + " s nothing inside can use mana. Leave it, or hurt Zenon to break it.");
        int waves = DomainMath.chainWaves(power());
        for (int w = 0; w < waves; w++) {
            later(DomainClock.FORM_TICKS + 30 + w * 80, ZenonMoves.safe("domain chain", () -> {
                LivingEntity tg = getTarget();
                if (tg == null || !tg.isAlive() || !SpellSeal.active(getUUID())) return;
                announce("The portals chain: more rifts open in Zenon's space!");
                ZenonMoves.portalLink(this, tg, PortalMath.linkPairs(power()), ZenonTable.PORTAL_SPEAR, 1);
            }));
        }
    }

    // ================================================================ marks: where the big moves land
    /** Remembers the places the wind-up of move 'id' drew, so the move lands exactly there. */
    private void lockMarks(String id, List<Vec3> list) {
        marks = list;
        marksOf = id;
    }

    /** The places the wind-up of move 'id' drew (none if a different move drew the last ones); forgets them. */
    private List<Vec3> takeMarks(String id) {
        List<Vec3> out = marksOf.equals(id) ? marks : List.of();
        marks = List.of();
        marksOf = "";
        return out;
    }

    /** The garden's spots, planned round the target and the other foes. */
    private List<Vec3> gardenSpots(LivingEntity target) {
        List<double[]> others = new ArrayList<>();
        for (LivingEntity e : foes(40)) if (e != target) others.add(new double[]{e.getX(), e.getZ()});
        List<double[]> plan = BoneMath.gardenPlan(BoneMath.gardenCount(tuning().castSlots()), target.getX(), target.getZ(),
                others.toArray(new double[0][]), BoneMath.GARDEN_RADIUS, getRandom().nextDouble() * Math.PI * 2);
        List<Vec3> out = new ArrayList<>();
        for (double[] p : plan) out.add(ZogratisBoss.ground(level(), new Vec3(p[0], target.getY(), p[1])));
        return out;
    }

    // ================================================================ telegraphs
    /** The first call of a wind-up (the tell runs every third tick, so the first is within three ticks of the start). */
    private static boolean first(int left, int total) { return left >= total - 3; }

    private Move.MoveTell tellOf(String id) {
        return switch (id) {
            case ZenonTable.BONE_SPEARS -> (b, t, left, total) -> tellSpears(t);
            case ZenonTable.BONE_BLADES -> (b, t, left, total) -> tellBlades(t);
            case ZenonTable.BONE_GARDEN -> (b, t, left, total) -> tellGarden(t, left, total);
            case ZenonTable.BACKSTAB -> (b, t, left, total) -> tellBackstab(t, left, total);
            case ZenonTable.PORTAL_LINK -> (b, t, left, total) -> tellLink(t);
            case ZenonTable.BONE_LANCE -> (b, t, left, total) -> tellLance(t, left, total);
            case ZenonTable.BONE_ARMOR -> (b, t, left, total) -> {
                if (level() instanceof ServerLevel sl) Tells.gather(sl, position().add(0, 1.0, 0), 0.9, BONE_RGB, 8);
            };
            case ZenonTable.WARP_BARRAGE -> (b, t, left, total) -> tellWarp(t);
            case ZenonTable.CALL_DISCIPLES -> (b, t, left, total) -> {
                if (level() instanceof ServerLevel sl) {
                    Tells.ring(sl, position(), 5, RGB);
                    Tells.gather(sl, position().add(0, 1.2, 0), 1.0, ACCENT, 6);
                }
            };
            case ZenonTable.DOMINATION -> (b, t, left, total) -> tellDomain(t, left, total);
            default -> null;
        };
    }

    private void tellSpears(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        int n = BoneMath.spearCount(tuning().castSlots());
        for (int i = 0; i < n; i++) {
            double[] g = BoneMath.growthPoint(i, n, getYRot());
            Tells.gather(sl, position().add(g[0], g[1], g[2]), 0.12, BONE_RGB, 2);
        }
        Vec3 from = position().add(0, getBbHeight() * 0.75, 0), centre = target.getBoundingBox().getCenter(), aim = centre.subtract(from);
        if (aim.lengthSqr() < 1e-4) return;
        aim = aim.normalize();
        double reach = Math.min(24, from.distanceTo(centre) + 6);
        for (double a : new double[]{BoneMath.spread(0, n), 0, BoneMath.spread(n - 1, n)})
            Tells.line(sl, from, from.add(ZenonMoves.rotateY(aim, a).scale(reach)), RGB);
    }

    private void tellBlades(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 flat = target.position().subtract(position()).multiply(1, 0, 1);
        if (flat.lengthSqr() < 1e-4) return;
        Vec3 dir = flat.normalize(), o = position().add(0, 0.4, 0);
        Tells.line(sl, o, o.add(ZenonMoves.rotateY(dir, BoneMath.BLADE_HALF_ARC).scale(BoneMath.BLADE_REACH)), BONE_RGB);
        Tells.line(sl, o, o.add(ZenonMoves.rotateY(dir, -BoneMath.BLADE_HALF_ARC).scale(BoneMath.BLADE_REACH)), BONE_RGB);
        Tells.gather(sl, position().add(0, 1.2, 0), 0.5, BONE_RGB, 4);
        if (flat.length() > BoneMath.BLADE_REACH * 0.85) Tells.line(sl, o, target.position().add(0, 0.4, 0), ACCENT);
    }

    /** Rings at the spots the garden will erupt at; they are locked when the wind-up starts, so moving off them is the answer. */
    private void tellGarden(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) lockMarks(ZenonTable.BONE_GARDEN, gardenSpots(target));
        if (!marksOf.equals(ZenonTable.BONE_GARDEN)) return;
        for (Vec3 s : marks) {
            Tells.ring(sl, s, BoneMath.GARDEN_RADIUS, WARN, 1.4f, 28);
            Tells.ring(sl, s, Math.max(0.5, BoneMath.GARDEN_RADIUS * left / Math.max(1, total)), ACCENT, 1.1f, 18);
            Tells.column(sl, s, 3, BONE_RGB);
        }
    }

    /** The exit portal opens behind the target for the whole wind-up: turn, or step aside. */
    private void tellBackstab(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            double[] p = PortalMath.behind(target.getX(), target.getZ(), target.getYRot(), 2.0);
            Vec3 spot = ZogratisBoss.ground(sl, new Vec3(p[0], target.getY(), p[1]));
            lockMarks(ZenonTable.BACKSTAB, List.of(spot));
            ZenonMoves.portal(sl, spot, target.position(), total + PortalMath.BLINK_TICKS + 4);
            announce("A rift opens behind " + target.getName().getString() + ".");
        }
        if (!marksOf.equals(ZenonTable.BACKSTAB) || marks.isEmpty()) return;
        Vec3 m = marks.get(0);
        Tells.ring(sl, m, 1.6, WARN, 1.3f, 20);
        Tells.line(sl, m.add(0, 1, 0), target.position().add(0, 1, 0), RGB);
        Tells.column(sl, m, 3, ACCENT);
    }

    private void tellLink(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        int pairs = PortalMath.linkPairs(power());
        for (double[] e : PortalMath.entryArc(getX(), getZ(), getYRot(), pairs, 1.7, 2.2)) {
            Vec3 at = new Vec3(e[0], getY(), e[1]);
            Tells.ring(sl, at, 0.8, RGB, 1.2f, 14);
            Tells.gather(sl, at.add(0, 1.2, 0), 0.3, ACCENT, 2);
        }
        Tells.ring(sl, target.position(), 6, ACCENT, 1.0f, 30);
        Tells.ring(sl, target.position(), 9, ACCENT, 1.0f, 36);
    }

    /** The line of the lance, locked when the wind-up starts and drawn two blocks wide to the first wall. */
    private void tellLance(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = position().add(0, getBbHeight() * 0.75, 0);
        if (first(left, total) || lanceDir == null) {
            Vec3 aim = target.getBoundingBox().getCenter().subtract(from);
            lanceDir = aim.lengthSqr() < 1e-4 ? getViewVector(1f) : aim.normalize();
        }
        Vec3 end = ZenonMoves.rayEnd(sl, this, from, lanceDir, ZenonMoves.LANCE_RANGE);
        Vec3 side = new Vec3(-lanceDir.z, 0, lanceDir.x);
        if (side.lengthSqr() > 1e-6) side = side.normalize().scale(1.0);
        int rgb = left * 3 < total ? WARN : RGB;
        Tells.line(sl, from.add(side), end.add(side), rgb);
        Tells.line(sl, from.subtract(side), end.subtract(side), rgb);
        Tells.gather(sl, from, 0.4, BONE_RGB, 4);
    }

    private void tellWarp(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        Tells.ring(sl, target.position(), 7.5, WARN, 1.2f, 36);
        Tells.ring(sl, target.position(), 9.0, WARN, 1.2f, 40);
        Tells.gather(sl, position().add(0, 1, 0), 0.8, ACCENT, 6);
    }

    /** The cube, drawn four edges at a time from the start of the wind-up, and its footprint on the ground. It is locked where the target stood. */
    private void tellDomain(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        double half = DomainMath.half(power());
        if (first(left, total)) {
            lockMarks(ZenonTable.DOMINATION, List.of(ZogratisBoss.ground(sl, target.position())));
            announce("Zenon spreads Spatial Mana Domination: a cube of warped space " + Math.round(half * 2) + " blocks across, closing in "
                    + Math.round((left + DomainClock.FORM_TICKS) / 20.0) + " s. Leave it now, on foot.");
            sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3f, 0.5f);
        }
        if (!marksOf.equals(ZenonTable.DOMINATION) || marks.isEmpty()) return;
        Vec3 m = marks.get(0);
        DomainCube cube = DomainCube.around(m.x, m.y, m.z, half);
        int start = ((total - left) / 3 * 4) % DomainCube.EDGES;
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(DevilStyle.red(ACCENT), DevilStyle.green(ACCENT), DevilStyle.blue(ACCENT)), 1.5f);
        for (int k = 0; k < 4; k++)
            for (double[] p : cube.edgeSamples((start + k) % DomainCube.EDGES, 2.0)) sl.sendParticles(dust, p[0], p[1], p[2], 1, 0, 0, 0, 0);
        Tells.ring(sl, m, half, WARN, 1.4f, (int) Math.min(64, 20 + half * 3));
        Tells.column(sl, m, 5, RGB);
        Tells.gather(sl, position().add(0, 1.2, 0), 1.2, ACCENT, 8);
    }
}
