package com.newuniverse.nusmp.arc.spade;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.entity.zogratis.DanteMoves;
import com.newuniverse.nusmp.entity.zogratis.GravityMath;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.entity.zogratis.ZogratisVfx;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: Morris Libardirt is the scholar behind the Spade Kingdom's gate, and the story's fight with him is a fight with a man who treats the fighters as
 * specimens and edits things: the Black Bulls' answer to him was anti-magic and sheer force, and the turn of that fight was the moment his borrowed
 * gravity and the devil inside him took over. A reskinned brute would lose all of that. So the fight has two rules only he has, each with a counter the
 * players can learn, and a third turn at the Gates:
 * <ul>
 *   <li><b>Structural Tuning</b> (Re-Structure): he re-forms his body against the kind of hit that has hurt him most (melee, ranged or magic) and takes less
 *       from it for twelve seconds, but every OTHER kind finds the new structure weaker. Switch kind and the tuning works for you; burst him (5% of his
 *       health) while he re-forms and it never happens; anti-magic is not a kind he can edit. Spell Rewrite does the same to the first arrow or spell
 *       that touches him: it comes back as needles.</li>
 *   <li><b>The Specimen thread</b>: he marks the strongest of you and draws a thread that drags and drains the specimen for eight seconds and ends in a
 *       dissection. An ally standing on the thread cuts it, a wall between them breaks it, 6% of his health in a burst snaps it, anti-magic dissolves it,
 *       and walking away from him loses it. He stands still while it is out.</li>
 *   <li><b>Lucifero surfaces</b>: from 60% the gravity of the contract comes in as a wedge and a heavy field, and at the Gates his glow turns violet, the
 *       Dissection Theatre closes its cage with doors, and The Second Gate opens in waves.</li>
 * </ul>
 * He keeps his distance, throws needles, drags with tendrils, and calls his test subjects (Lucifero's lesser devils). All damage goes through the
 * framework's damage law (his spells ignore armour, anti-magic cuts them to 40%); hard drags ask the Balance Law first. Numbers: {@link MorrisTable},
 * {@link StructuralTuning}, {@link Tether}, {@link CageMath}.
 */
public class MorrisBoss extends ArcBoss {
    public static final ArcCharacter CHARACTER = SpadeRoster.MORRIS;

    /**
     * How he is drawn: the numbers of {@code tools/arc/manifest.json} for {@code arc_morris_libardirt} (strides, and the culling box and shadow of the BIGGER of his two
     * forms, the host form with horns and wings). Copy them again whenever {@code tools/arc/char_morris_libardirt.py} changes.
     */
    private static final ArcLook LOOK = new ArcLook("morris_libardirt", 2.0f, 0.88f, 1.63f, 0.68f, 1.47f, 0.97f, true);
    public static final DeferredHolder<EntityType<?>, EntityType<MorrisBoss>> TYPE =
            ArcEntities.boss("morris_libardirt", MorrisBoss::new, 0.6f, LOOK.height(), LOOK, MorrisBoss::createAttributes);

    private static final String WHO = "Morris";
    private static final int ORANGE = MorrisMoves.ORANGE_RGB, PALE = MorrisMoves.PALE_RGB, WARN = MorrisMoves.WARN_RGB;
    private static final int TETHER_LIFE = 160, BURST_PERCENT = 5;

    private final StructuralTuning tuning = new StructuralTuning();
    private final BurstWatch restructureWatch = new BurstWatch(), tetherWatch = new BurstWatch();
    private Tether tether;
    private UUID specimenId, specimenPick;
    private boolean rewriteSpent, antiMagicHit, tuningAnnounced;
    private long rewriteUntil, staggerUntil, lastQuip;
    private List<Vec3> tendrilDirs = List.of();
    private Vec3 pinOrigin, pinDir, mark, cageCenter;
    private double[] doors = new double[0];

    public MorrisBoss(EntityType<MorrisBoss> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return ZogratisBoss.bossAttributes(CHARACTER.health(), CHARACTER.attack(), CHARACTER.speed(), CHARACTER.armor(), 8);
    }

    @Override public ArcCharacter character() { return CHARACTER; }

    // ================================================================ tuning
    @Override protected float formScale(int form) { return form == FORM_HYBRID ? 1.1f : 1.0f; }
    @Override protected float damageScale() { return 1.1f * SpadeConfig.morrisDamage(); }
    @Override protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7f; }
    /** A scholar does not brawl: seven to fourteen blocks, close enough to drag, far enough to watch. */
    @Override protected double preferredRange(double suggested) { return Math.max(7, Math.min(14, suggested)); }

    /** Orange while he is himself, the gravity violet once Lucifero surfaces at the Gates. */
    @Override public int styleColor() { return gatesOpen() ? SpadeRoster.LUCIFERO_COLOR : super.styleColor(); }

    @Override public int styleAccent() { return gatesOpen() ? SpadeRoster.LUCIFERO_ACCENT : super.styleAccent(); }

    @Override protected ChatFormatting chatColor() { return gatesOpen() ? ChatFormatting.DARK_PURPLE : ChatFormatting.GOLD; }

    @Override
    public String barTitle() {
        return bossName() + " - " + power() + "%" + (gatesOpen() ? " - Lucifero surfaces" : "");
    }

    // ================================================================ lines
    @Override
    protected String powerLine(int old, int now) {
        if (now >= 80 && old < 80) return "Eighty percent... the contract is pulling harder than I calculated. Fascinating. Alarming. Fascinating!";
        if (now >= PowerCurve.INFINITE_MANA_FROM && old < PowerCurve.INFINITE_MANA_FROM) return "Gravity, borrowed. Hold still, I have not finished measuring you.";
        if (now >= 40 && old < 40) return "Your structure is more interesting than I was told. Let me look inside.";
        return "A little more of the contract: " + now + "%.";
    }

    private void quip(String... lines) {
        if (tickCount - lastQuip < 160) return;
        lastQuip = tickCount;
        say(lines[getRandom().nextInt(lines.length)]);
    }

    // ================================================================ the fight's hooks
    @Override
    protected void onFightStart() {
        resetState();
        announce("Morris re-forms his body against the kind of hit that hurts him most (melee, arrows or spells) and is open to the others: switch kind. A burst while he re-forms stops it.");
        announce("He draws a thread to the strongest of you. Cut it by standing on it, break line of sight, burst him, or hold anti-magic.");
    }

    @Override
    protected void onFightEnd(boolean won) {
        super.onFightEnd(won);
        endTether(null);
        resetState();
    }

    private void resetState() {
        tuning.reset();
        restructureWatch.clear();
        tetherWatch.clear();
        tether = null;
        specimenId = null;
        specimenPick = null;
        rewriteUntil = 0;
        rewriteSpent = false;
        staggerUntil = 0;
        tuningAnnounced = false;
        tendrilDirs = List.of();
        pinOrigin = null;
        pinDir = null;
        mark = null;
        cageCenter = null;
        doors = new double[0];
    }

    @Override
    protected void onPowerChanged(int old, int now) {
        int before = PowerCurve.stage(old), after = PowerCurve.stage(now);
        if (after <= before || after >= 4) return;
        if (summonDisciples(Math.min(2, after)) > 0) announce("Morris's test subjects answer his call.");
    }

    @Override
    protected void onGatesOpened() {
        announce("The contract tightens: Lucifero surfaces through Morris. His glow turns violet, the gravity hits harder, and The Second Gate begins to open.");
        if (level() instanceof ServerLevel sl) {
            ZogratisVfx.gravityAura(sl, this, 1.6, 240, SpadeRoster.LUCIFERO_COLOR);
            ZogratisVfx.well(sl, position(), 12, 70, SpadeRoster.LUCIFERO_COLOR, false);
        }
        for (int delay : new int[]{120, 400}) {
            later(delay, SpadeFx.safe(WHO, "subject wave", () -> {
                if (fightActive() && summonDisciples(2) > 0) announce("More of Morris's test subjects step out of the gate.");
            }));
        }
    }

    // ================================================================ the passive rules
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
     * In order: Spell Rewrite turns the first arrow or spell into needles; every blow that lands is written into the ledger of his tuning and counted by
     * the two burst watches; a burst during Re-Structure interrupts it; anti-magic is noted for the thread; a staggered scholar (the thread was cut or snapped) takes
     * 15% more for two seconds.
     */
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel sl)) return amount;
        try {
            float eff = effective(source, amount);
            if (eff <= 0f) return amount;
            long now = sl.getGameTime();
            DamageKind kind = SpadeFx.kindOf(source);
            if (now < rewriteUntil && !rewriteSpent && (kind == DamageKind.RANGED || kind == DamageKind.MAGIC)
                    && source.getEntity() instanceof LivingEntity shooter && shooter.isAlive()) {
                rewriteSpent = true;
                rewriteUntil = 0;
                announce("Spell Rewrite! Morris turned the first arrow or spell back on its caster as needles.");
                MorrisMoves.needles(this, shooter, 2 + PowerCurve.stage(power()), MorrisTable.REWRITE_NEEDLE, 2);
                quip("Thank you for the sample.", "Interesting. I will keep it.");
                return 0f;
            }
            tuning.record(now, kind, eff);
            restructureWatch.add(now, eff);
            tetherWatch.add(now, eff);
            if (kind == DamageKind.ANTI_MAGIC) antiMagicHit = true;
            Move casting = castingMoveOrNull();
            if (casting != null && MorrisTable.RESTRUCTURE.equals(casting.id()) && restructureWatch.tripped()) {
                interruptCast();
                restructureWatch.clear();
                announce("Re-Structure is interrupted! Morris has not re-formed his body.");
                quip("Not now! The structure is not stable!");
            }
            return now < staggerUntil ? amount * 1.15f : amount;
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Morris's defence failed: {}", e.toString());
            return amount;
        }
    }

    /** His tuning: the tuned kind is cut, every other kind finds a hole (skipped by the framework while he is exposed). */
    @Override
    protected float mitigate(DamageSource source, float amount) {
        if (!SpadeConfig.tuning() || !(level() instanceof ServerLevel sl)) return amount;
        return amount * tuning.factor(SpadeFx.kindOf(source), sl.getGameTime());
    }

    @Override
    protected void bossTick() {
        if (!(level() instanceof ServerLevel sl)) return;
        try {
            long now = sl.getGameTime();
            if (!fightActive()) {
                if (tether != null) endTether(null);
                return;
            }
            if (tuning.active(now)) {
                tuningAnnounced = true;
                if (tickCount % 20 == 0) VfxSpawn.sendFollowing(sl, VfxShape.WIKI_MAGIC_FIELD, this, position().add(0, 1, 0), MorrisMoves.MODIFICATION_ARGB, 24, 0.45f);
            } else if (tuningAnnounced) {
                tuningAnnounced = false;
                announce("Morris's tuning has worn off: every kind of hit is back to normal.");
            }
            if (rewriteUntil != 0 && now >= rewriteUntil) {
                rewriteUntil = 0;
                if (!rewriteSpent) announce("Spell Rewrite fades unused.");
            }
            if (tether != null) tickTether(sl, now);
            antiMagicHit = false;
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Morris's tick failed: {}", e.toString());
        }
    }

    // ================================================================ the thread
    private LivingEntity specimen(ServerLevel sl) {
        return specimenId != null && sl.getEntity(specimenId) instanceof LivingEntity le && le.isAlive() ? le : null;
    }

    private void startTether(ServerLevel sl, LivingEntity fallback) {
        LivingEntity picked = specimenPick != null && sl.getEntity(specimenPick) instanceof LivingEntity le && le.isAlive() ? le : null;
        LivingEntity s = picked != null ? picked : MorrisMoves.mostInteresting(this, fallback);
        specimenPick = null;
        if (s == null || !isFoe(s)) return;
        if (SpadeFx.antiMagic(s)) {
            announce("The thread cannot take hold of " + s.getName().getString() + ": anti-magic dissolves it.");
            return;
        }
        long now = sl.getGameTime();
        tether = new Tether(TETHER_LIFE);
        specimenId = s.getUUID();
        tetherWatch.begin(now, TETHER_LIFE, getMaxHealth() * 0.06);
        announce("Specimen Mark: " + s.getName().getString() + " is tethered to Morris. Cut the thread (an ally stands on it), break line of sight, hit Morris for 6% of his health, or hold anti-magic.");
        if (s instanceof Player p) SpadeFx.hint(p, "message.nusmp.spade.specimen", "You are the specimen: the thread drags and drains you. Have an ally stand on it, break line of sight, or hold anti-magic.");
        quip("You. Yes, you. Do hold still.", "A fine specimen. The best of the lot.");
    }

    private void tickTether(ServerLevel sl, long now) {
        LivingEntity s = specimen(sl);
        if (s == null) {
            announce("The thread loses its specimen.");
            endTether(null);
            return;
        }
        boolean crossed = false;
        for (LivingEntity e : foes(30)) {
            if (e != s && TetherMath.cuts(e.getX(), e.getZ(), getX(), getZ(), s.getX(), s.getZ())) { crossed = true; break; }
        }
        Tether.End end = tether.tick(crossed, !hasLineOfSight(s), SpadeFx.antiMagic(s) || antiMagicHit, tetherWatch.tripped(), distanceTo(s));
        if (end != Tether.End.HOLD) {
            finishTether(sl, s, end);
            return;
        }
        holdStill();
        Vec3 to = SpadeFx.flatUnit(position().subtract(s.position()));
        double pull = tether.pull() * 0.5;
        SpadeFx.grounded(s);
        Vec3 v = s.getDeltaMovement();
        s.setDeltaMovement(v.x + to.x * pull, v.y, v.z + to.z * pull);
        s.hurtMarked = true;
        s.fallDistance = 0;
        if (tickCount % 10 == 0) {
            try {
                s.addEffect(new MobEffectInstance(SpadeEffects.SPECIMEN, 30, 0, false, true, true));
            } catch (RuntimeException notRegistered) {
                s.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
            }
        }
        if (tether.age() % 20 == 10) strike(s, MorrisTable.SPECIMEN_DRAIN, true);
        if (tickCount % 3 == 0) MorrisMoves.drawThread(sl, position().add(0, 1.3, 0), s.position().add(0, 1.0, 0));
    }

    /**
     * A scholar at his work stands still while the thread is out. Without this the framework's positioning goal would back him off as the specimen is
     * drawn in (it keeps seven to fourteen blocks from its target), the specimen could never come within reach, and no thread would ever end in a dissection.
     * It is also the tell and the opening: a Morris who has stopped moving is a Morris the party can reach and burst.
     */
    private void holdStill() {
        getNavigation().stop();
        getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0);
    }

    private void finishTether(ServerLevel sl, LivingEntity s, Tether.End end) {
        switch (end) {
            case REACHED -> {
                announce("The thread reaches its specimen: dissection!");
                VfxSpawn.send(sl, VfxShape.WIKI_MAGIC_BURST, s.position(), s.position().add(0, 1, 0), MorrisMoves.MODIFICATION_ARGB, 20, 2f);
                if (strike(s, (float) (MorrisTable.DISSECT * SpadeFx.magicGuard(s)), true)) s.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
                Vec3 away = LuciferoMoves.stepDestination(this, s);
                if (away != null) blinkTo(away);
                quip("Splendid. Now, the rest of you.");
            }
            case CUT -> {
                announce("The thread is cut! Morris recoils.");
                staggerUntil = sl.getGameTime() + 40;
                Tells.burst(sl, position().add(0, 1.3, 0), PALE, 30);
            }
            case BURST -> {
                announce("The thread snaps under the blow! Morris recoils.");
                staggerUntil = sl.getGameTime() + 40;
            }
            case ANTI_MAGIC -> announce("Anti-magic dissolves the thread.");
            case LOST_SIGHT -> announce("The thread is lost: nothing to hold it.");
            case TOO_FAR -> announce("The thread slackens: too far.");
            case TIMEOUT -> announce("The thread runs out.");
            default -> { }
        }
        endTether(s);
    }

    private void endTether(LivingEntity s) {
        if (s == null && level() instanceof ServerLevel sl) s = specimen(sl);
        tether = null;
        specimenId = null;
        tetherWatch.clear();
        if (s != null) {
            try {
                s.removeEffect(SpadeEffects.SPECIMEN);
            } catch (RuntimeException notRegistered) {
                // nothing was applied
            }
        }
    }

    // ================================================================ moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (MorrisTable.Spec s : MorrisTable.specs()) {
            Move.Builder b = Move.builder(s.id(), s.label()).minPower(s.minPower()).windUp(s.windUpTicks()).cooldown(s.cooldownTicks())
                    .range(s.minRange(), s.maxRange()).weight(s.weight()).mana(s.manaCost()).recovery(s.recovery());
            if (s.ultimate()) b.ultimate();
            if (s.crowd()) b.crowd();
            b.tell(tellOf(s.id()));
            out.add(b.run(runOf(s.id())));
        }
        return out;
    }

    private static boolean first(int left, int total) { return left >= total - 3; }

    private double progress(int left, int total) { return (double) (total - left) / Math.max(1, total); }

    private Move.MoveTell tellOf(String id) {
        return switch (id) {
            case MorrisTable.NEEDLES -> (bs, t, left, total) -> tellNeedles(t);
            case MorrisTable.STRUCTURE_SHOCK -> (bs, t, left, total) -> tellShock(left, total);
            case MorrisTable.TENDRILS -> (bs, t, left, total) -> tellTendrils(t, left, total);
            case MorrisTable.SPECIMEN -> (bs, t, left, total) -> tellSpecimen(t, left, total);
            case MorrisTable.SPELL_REWRITE, MorrisTable.TEST_SUBJECTS -> (bs, t, left, total) -> tellGather(id, left, total);
            case MorrisTable.RESTRUCTURE -> (bs, t, left, total) -> tellRestructure(left, total);
            case MorrisTable.PIN_DOWN -> (bs, t, left, total) -> tellPin(t, left, total);
            case MorrisTable.BORROWED_WEIGHT -> (bs, t, left, total) -> tellWeight(t, left, total);
            case MorrisTable.DISSECTION_THEATRE -> (bs, t, left, total) -> tellTheatre(t, left, total);
            case MorrisTable.SECOND_GATE -> (bs, t, left, total) -> tellGate(left, total);
            default -> (bs, t, left, total) -> { };
        };
    }

    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case MorrisTable.NEEDLES -> (b, t) -> {
                MorrisMoves.needles(this, t, GravityMath.swordCount(tuning().castSlots()), MorrisTable.NEEDLE, 3);
                quip("A small incision.", "Hold still. Just a few samples.");
            };
            case MorrisTable.STRUCTURE_SHOCK -> (b, t) -> {
                MorrisMoves.shock(this, MorrisMoves.SHOCK_RADIUS, MorrisTable.SHOCK);
                later(6, SpadeFx.safe(WHO, "shock retreat", () -> {
                    Vec3 away = LuciferoMoves.stepDestination(this, t);
                    if (away != null) blinkTo(away);
                }));
                quip("Keep your distance. I am working.", "Back, back. You are in my light.");
            };
            case MorrisTable.TENDRILS -> (b, t) -> {
                List<Vec3> dirs = tendrilDirs.isEmpty() ? List.of(SpadeFx.flatUnit(t.position().subtract(position()))) : tendrilDirs;
                int n = MorrisMoves.tendrils(this, dirs, MorrisTable.TENDRIL);
                tendrilDirs = List.of();
                if (n > 0) quip("Come closer, come closer.", "Hold. Still. Please.");
            };
            case MorrisTable.SPECIMEN -> (b, t) -> {
                if (level() instanceof ServerLevel sl) startTether(sl, t);
            };
            case MorrisTable.SPELL_REWRITE -> (b, t) -> {
                rewriteUntil = level().getGameTime() + 80;
                rewriteSpent = false;
                announce("Spell Rewrite is up for 4 s: the first arrow or spell that touches Morris comes back as needles. Melee and anti-magic go through.");
                quip("Throw something at me. Go on.");
            };
            case MorrisTable.TEST_SUBJECTS -> (b, t) -> {
                if (summonDisciples(2 + (gatesOpen() ? 1 : 0)) > 0 && level() instanceof ServerLevel sl) {
                    VfxSpawn.send(sl, VfxShape.WIKI_MAGIC_BURST, position(), position().add(0, 1, 0), MorrisMoves.MODIFICATION_ARGB, 20, 2f);
                    quip("My test subjects. Do not worry, they are quite stable now.");
                }
            };
            case MorrisTable.RESTRUCTURE -> (b, t) -> reStructure();
            case MorrisTable.PIN_DOWN -> (b, t) -> {
                Vec3 origin = pinOrigin != null ? pinOrigin : position();
                Vec3 dir = pinDir != null ? pinDir : SpadeFx.flatUnit(t.position().subtract(position()));
                MorrisMoves.pinDown(this, origin, dir, gatesOpen() ? 70 : 50, Math.min(2.6f, 1.0f + 0.4f * PowerCurve.stage(power())), MorrisTable.PIN_TICK);
                pinOrigin = null;
                pinDir = null;
                quip("The weight is not mine. I am only borrowing it.");
            };
            case MorrisTable.BORROWED_WEIGHT -> (b, t) -> {
                Vec3 at = mark != null ? mark : ZogratisBoss.ground(level(), t.position());
                mark = null;
                DanteMoves.gravityField(this, at, 8 + PowerCurve.stage(power()), gatesOpen() ? 120 : 100, gatesOpen() ? 2.2f : 1.6f);
                quip("Lucifero's gravity. Terribly heavy, isn't it?");
            };
            case MorrisTable.DISSECTION_THEATRE -> (b, t) -> {
                Vec3 c = cageCenter != null ? cageCenter : ZogratisBoss.ground(level(), t.position());
                double[] d = doors.length > 0 ? doors : CageMath.doors(MorrisMoves.openDirections(this, c), CageMath.DOORS);
                MorrisMoves.theatre(this, c, d, 120, MorrisTable.THEATRE_WALL, MorrisTable.THEATRE_SQUEEZE);
                cageCenter = null;
                doors = new double[0];
                quip("The theatre is open. The doors are quite generous.");
            };
            case MorrisTable.SECOND_GATE -> (b, t) -> secondGate();
            default -> (b, t) -> { };
        };
    }

    // ---------------------------------------------------------------- telegraphs
    private void tellNeedles(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = position().add(0, getBbHeight() * 0.75, 0);
        Vec3 aim = target.getBoundingBox().getCenter().subtract(from);
        if (aim.lengthSqr() < 1e-4) return;
        aim = aim.normalize();
        for (double a : GravityMath.fan(Math.min(5, GravityMath.swordCount(tuning().castSlots())))) Tells.line(sl, from, from.add(SpadeFx.rotateY(aim, a).scale(16)), ORANGE);
        Tells.gather(sl, from, 0.7, PALE, 4);
    }

    private void tellShock(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Tells.ring(sl, position(), Math.max(1.5, MorrisMoves.SHOCK_RADIUS * progress(left, total)), ORANGE, 1.3f, 40);
        if (left % 6 == 0) Tells.ring(sl, position(), MorrisMoves.SHOCK_RADIUS, WARN, 1.1f, 48);
        Tells.gather(sl, position().add(0, 1.2, 0), 0.9, PALE, 5);
    }

    private void tellTendrils(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            List<Vec3> dirs = new ArrayList<>();
            for (LivingEntity e : MorrisMoves.tendrilTargets(this, target)) dirs.add(SpadeFx.flatUnit(e.position().subtract(position())));
            tendrilDirs = dirs;
            for (Vec3 d : dirs) ZogratisVfx.lane(sl, position().add(0, 1, 0), position().add(0, 1, 0).add(d.scale(MorrisMoves.TENDRIL_REACH)), 2.6, total + 2, ORANGE, true);
        }
        Vec3 o = position().add(0, 1.0, 0);
        for (Vec3 d : tendrilDirs) Tells.line(sl, o, o.add(d.scale(MorrisMoves.TENDRIL_REACH)), WARN);
        Tells.gather(sl, o, 0.8, PALE, 4);
    }

    private void tellSpecimen(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            LivingEntity s = MorrisMoves.mostInteresting(this, target);
            specimenPick = s == null ? null : s.getUUID();
            if (s != null) announce("Morris sets his eyes on " + s.getName().getString() + ".");
        }
        LivingEntity s = specimenPick != null && sl.getEntity(specimenPick) instanceof LivingEntity le && le.isAlive() ? le : target;
        Tells.ring(sl, s.position(), 1.6, ORANGE, 1.3f, 28);
        Tells.line(sl, position().add(0, 1.3, 0), s.position().add(0, 1.0, 0), PALE);
        Tells.column(sl, s.position(), 3, ORANGE);
    }

    private void tellGather(String id, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Tells.gather(sl, position().add(0, 1.3, 0), 1.0, PALE, 6);
        Tells.ring(sl, position(), 2.5 + 2 * progress(left, total), ORANGE, 1.2f, 32);
        if (MorrisTable.SPELL_REWRITE.equals(id) && first(left, total))
            VfxSpawn.sendFollowing(sl, VfxShape.WIKI_MAGIC_FIELD, this, position().add(0, 1, 0), MorrisMoves.MODIFICATION_ARGB, total + 80, 0.45f);
    }

    private void tellRestructure(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            restructureWatch.begin(sl.getGameTime(), total + 2, getMaxHealth() * BURST_PERCENT / 100.0);
            announce("Morris begins to re-form his body. Burst him for 5% of his health before it finishes to stop it!");
            VfxSpawn.send(sl, VfxShape.ALCHEMY_CIRCLE, position().add(0, 0.05, 0), position().add(0, 1, 0), MorrisMoves.MODIFICATION_ARGB, total, 1.4f);
        }
        Tells.gather(sl, position().add(0, 1.2, 0), 1.1, PALE, 8);
        Tells.ring(sl, position(), 2.0 + 1.5 * progress(left, total), ORANGE, 1.4f, 36);
    }

    private void tellPin(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            pinOrigin = position();
            pinDir = SpadeFx.flatUnit(target.position().subtract(position()));
        }
        if (pinOrigin == null || pinDir == null) return;
        for (double side : new double[]{-Math.toRadians(38), Math.toRadians(38)})
            Tells.line(sl, pinOrigin.add(0, 0.2, 0), pinOrigin.add(SpadeFx.rotateY(pinDir, side).scale(MorrisMoves.PIN_REACH)).add(0, 0.2, 0), MorrisMoves.VIOLET_ACCENT_RGB);
        Tells.ring(sl, pinOrigin, MorrisMoves.PIN_REACH * 0.5, WARN, 1.1f, 36);
        Tells.gather(sl, position().add(0, 1.5, 0), 1.0, MorrisMoves.VIOLET_ACCENT_RGB, 6);
    }

    private void tellWeight(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        double r = 8 + PowerCurve.stage(power());
        if (first(left, total)) {
            mark = ZogratisBoss.ground(sl, target.position());
            ZogratisVfx.well(sl, mark, r, total + 2, MorrisMoves.VIOLET_RGB, true);
        }
        if (mark == null) return;
        Tells.ring(sl, mark, r, WARN, 1.4f, (int) Math.min(80, 14 + r * 5));
        Tells.ring(sl, mark, Math.max(0.6, r * left / Math.max(1, total)), MorrisMoves.VIOLET_ACCENT_RGB, 1.1f, 28);
        Tells.column(sl, mark, 4, MorrisMoves.VIOLET_ACCENT_RGB);
    }

    private void tellTheatre(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            List<LivingEntity> foes = foes(26);
            double x = target.getX(), z = target.getZ();
            if (!foes.isEmpty()) {
                x = 0;
                z = 0;
                for (LivingEntity e : foes) { x += e.getX(); z += e.getZ(); }
                x /= foes.size();
                z /= foes.size();
            }
            cageCenter = ZogratisBoss.ground(sl, new Vec3(x, target.getY(), z));
            doors = CageMath.doors(MorrisMoves.openDirections(this, cageCenter), CageMath.DOORS);
            announce("Dissection Theatre: a cage of tendrils will close round the party. Leave through a door (a gap in the ring) before it shuts.");
            SpadeFx.hintAround(this, 64, "message.nusmp.spade.cage_order", "DISSECTION THEATRE! Leave the ring through one of its doors (the gaps).");
            VfxSpawn.send(sl, VfxShape.WIKI_MAGIC_FIELD, cageCenter, cageCenter.add(0, 1, 0), MorrisMoves.MODIFICATION_ARGB, total, (float) (CageMath.START / 3));
        }
        if (cageCenter == null) return;
        SpadeFx.ringWithDoors(sl, cageCenter, CageMath.START, WARN, doors, 96);
        for (double d : doors) SpadeFx.spoke(sl, cageCenter.add(Math.cos(d) * (CageMath.START - 2.5), 0, Math.sin(d) * (CageMath.START - 2.5)), d, 5.0, PALE);
        Tells.gather(sl, position().add(0, 1.6, 0), 1.4, PALE, 8);
    }

    private void tellGate(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            announce("THE SECOND GATE: waves of weight roll out of Morris, one after another. Jump each one, or keep out of its ring.");
            VfxSpawn.send(sl, VfxShape.DEVIL_CIRCLE, position().add(0, 0.05, 0), position().add(0, 1, 0), 0xFF000000 | SpadeRoster.LUCIFERO_COLOR, total, 3f);
        }
        for (int k = 0; k < 5; k++) if (left % 6 == 0) Tells.ring(sl, position(), 7 + 4 * k, k == 4 ? WARN : MorrisMoves.VIOLET_RGB, 1.1f, 48);
        Tells.gather(sl, position().add(0, 2.0, 0), 1.6, MorrisMoves.VIOLET_ACCENT_RGB, 8);
    }

    // ---------------------------------------------------------------- the moves that need more than a call
    /** Re-Structure: he tunes his body against the kind of hit that has hurt him most (melee if he has learned nothing yet). */
    private void reStructure() {
        if (!(level() instanceof ServerLevel sl)) return;
        restructureWatch.clear();
        if (!SpadeConfig.tuning()) {
            announce("Morris's body does not take the new structure (structuralTuning is off).");
            return;
        }
        long now = sl.getGameTime();
        DamageKind kind = tuning.dominant(now, getMaxHealth() * 0.02);
        if (kind == null) kind = tuning.last() != null ? tuning.last() : DamageKind.MELEE;
        double cut = StructuralTuning.cutFor(SpadeConfig.tuningCut(), PowerCurve.stage(power()));
        tuning.tune(kind, now, cut);
        tuningAnnounced = true;
        announce("Re-Structure: Morris is tuned against " + kind.word() + " (" + Math.round(cut * 100) + "% less) for 12 s, and open to everything else (15% more). Switch kind of attack!");
        VfxSpawn.send(sl, VfxShape.WIKI_MAGIC_BURST, position(), position().add(0, 1, 0), MorrisMoves.MODIFICATION_ARGB, 20, 2f);
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2f, 1.2f);
        quip("There. Try that again. I dare you.", "Adapted. Do try something different.");
    }

    /** The Second Gate: five waves of weight 14 ticks apart (7 to 23 blocks), and two more test subjects. Each wave is jumped or outrun. */
    private void secondGate() {
        for (int k = 0; k < 5; k++) {
            final int wave = k;
            later(k * 14, SpadeFx.safe(WHO, "gate wave", () -> DanteMoves.stompWave(this, position(), 7 + 4 * wave, MorrisTable.GATE_WAVE, 1.0)));
        }
        if (summonDisciples(2) > 0) announce("Two more test subjects step out of the gate.");
        say("The second gate... open!");
    }
}
