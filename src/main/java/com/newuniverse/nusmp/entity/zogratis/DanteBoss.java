package com.newuniverse.nusmp.entity.zogratis;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Dante Zogratis, the eldest of the Dark Triad and the king of the Spade Kingdom's fists, contracted to the devil Lucifero. A
 * proud, loud bruiser who wants to be close and does not hide behind anything: he is the tank-fast one of the Triad (1200 health,
 * heavy blows, fast for his size) and the fight is a lesson in not standing still.
 * <ul>
 *   <li><b>Native magic, Body Magic (passive)</b>: superhuman strength (the Titan Fist, the stomp, the leap) and flesh that closes
 *       mortal wounds. {@link BodyRegen} decides when: a hit that would leave him under 30% health triggers a pulse (rate limited,
 *       weaker every time, doubled at the Gates). The party beats it with anti-magic (8 s with no regeneration), with a burst of
 *       damage (a tenth of his health in two seconds halves the pulse, a fifth denies it), or by hitting him while he is exposed
 *       after the Singularity (half strength). The chat tells them all of this.</li>
 *   <li><b>Devil magic, Gravity</b> ({@link DanteMoves}): Lucifero's Domain (a field that slows, pins and crushes), Gravity Pull and
 *       its slam, Lucifero's Weight, Weighted Leap, and the ultimate Gravity Singularity (60% and up): black spheres over marked
 *       zones that collapse after a countdown, bite the ground when griefBlocks is on and are put back when the fight ends.</li>
 *   <li><b>Special attacks</b>: compressed rock swords in fans, stone spikes along a line.</li>
 *   <li><b>Power</b>: at 100% (Gates open, hybrid form) fields grow and chain, the Singularity is cast twice in a row once, Body
 *       Magic doubles and the Dark Disciples arrive in waves. At each step of the curve a Disciple answers his call.</li>
 * </ul>
 * Every big move is telegraphed for at least 8 ticks (the ultimate for over two seconds), has a counter, and the zones of the
 * big ones are locked where they were drawn, so moving is always the answer.
 */
public class DanteBoss extends ZogratisBoss {
    private static final int RGB = DevilStyle.color(Devil.LUCIFERO), ACCENT = DevilStyle.accent(Devil.LUCIFERO), WARN = 0xFF4A4A, BODY_RGB = 0xFF8A4A;
    private static final double FIST_REACH = 4.6, LEAP_REACH = 7.0, WEIGHT_RADIUS = 2.8;

    private final BodyRegen body = new BodyRegen();
    private List<Vec3> marks = List.of();
    private boolean doubleUsed;
    private long surgeUntil, lastQuip, lastBodyNote, quietUntil;

    public DanteBoss(EntityType<DanteBoss> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() { return ZogratisBoss.bossAttributes(1700, 18, 0.30, 15, 9); }

    @Override public String bossName() { return "Dante Zogratis"; }
    @Override public Devil devil() { return Devil.LUCIFERO; }

    // ================================================================ tuning
    @Override protected float formScale(int form) { return form == FORM_HYBRID ? 1.18f : 1.0f; }
    @Override protected float damageScale() { return 1.1f; }
    /** The plain blow comes every half to full second, so it is lighter than the telegraphed Titan Fist, which is the real heavy hit. */
    @Override protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.75f * surgeFactor(); }
    /** A brawler: it wants to be within a few blocks, whatever the ranged moves ask for. */
    @Override protected double preferredRange(double suggested) { return Math.max(2.5, Math.min(4.5, suggested)); }
    @Override protected String[] tensuraKeywords() { return new String[]{"gravity", "burden", "earth"}; }
    @Override protected int tensuraKitSize() { return 3; }

    /** The three rings of the Titan Quake (the Gates widen them). */
    private double quakeReach(int wave) { return (5.0 + 3.5 * wave) + (gatesOpen() ? 1.5 : 0); }

    private boolean surging() { return level().getGameTime() < surgeUntil; }
    private float surgeFactor() { return surging() ? 1.25f : 1f; }
    private int stage() { return PowerCurve.stage(power()); }
    private float singularityPower() { return Math.max(0.5f, Math.min(2.0f, power() / 80f)); }
    private double fieldRadius() { return (8 + stage()) * (gatesOpen() ? 1.35 : 1.0); }
    private float fieldStrength() { return Math.min(3.4f, 1.2f + 0.6f * stage()); }
    private double stompReach() { return gatesOpen() ? 11 : 9; }

    // ================================================================ lines
    @Override protected String openingLine() { return "Heh! You actually walked into the Spade Kingdom. Good. I was bored. Show me what you've got!"; }

    @Override
    protected String powerLine(int old, int now) {
        if (now >= 80 && old < 80) return "Eighty percent of Lucifero! Few have lived to see it. Be proud of that, then fall!";
        if (now >= PowerCurve.INFINITE_MANA_FROM && old < PowerCurve.INFINITE_MANA_FROM)
            return "Sixty percent! Hahaha, feel that? My mana has no bottom now. Keep up if you can!";
        if (now >= 40 && old < 40) return "Forty percent... Lucifero's weight is waking up. Your knees bending yet?";
        return "I draw " + now + "% of Lucifero's power. Bear it!";
    }

    @Override protected String gatesLine() { return "The Gates of the Tree of Qliphoth open! Lucifero, lend me everything! One hundred percent!"; }
    @Override protected String deathLine() { return "Hah... so that is what losing feels like. Not bad. Not bad at all... Lucius, brother... it is yours now."; }

    /** One of 'lines' now and then (at most every eight seconds), so the chat is not a wall of shouting. */
    private void quip(String... lines) {
        if (tickCount - lastQuip < 160) return;
        lastQuip = tickCount;
        say(lines[getRandom().nextInt(lines.length)]);
    }

    // ================================================================ the fight's hooks
    @Override
    protected void onFightStart() {
        body.reset();
        doubleUsed = false;
        surgeUntil = 0;
        quietUntil = 0;
        marks = List.of();
        announce("Body Magic: Dante's flesh closes a mortal wound, but only once every half minute and a little weaker each time.");
        announce("Hit him with everything at once, cut it with anti-magic (a demon sword, the Anti-Magic Lord), or strike while he is exposed after his ultimate.");
    }

    @Override
    protected void onFightEnd(boolean won) {
        body.reset();
        marks = List.of();
        surgeUntil = 0;
        quietUntil = 0;
    }

    /** While anti-magic holds the Body Magic cut, nothing closes his wounds: not even the framework's slow regeneration. */
    @Override
    public void heal(float amount) {
        if (body != null && !level().isClientSide && body.suppressed(level().getGameTime())) return;
        super.heal(amount);
    }

    @Override
    protected void onPowerChanged(int old, int now) {
        int before = PowerCurve.stage(old), after = PowerCurve.stage(now);
        if (after > before && after < 4 && summonDisciples(after) > 0) announce("Lucifero's Dark Disciples answer Dante's call.");
    }

    @Override
    protected void onGatesOpened() {
        announce("Dante's body swells with Lucifero's power. His gravity fields grow, and his flesh mends twice as fast.");
        if (level() instanceof ServerLevel sl) { ZogratisVfx.gravityAura(sl, this, 1.6, 240, DanteMoves.BODY); ZogratisVfx.well(sl, position(), 14, 70, DanteMoves.VIOLET, false); }
        for (int delay : new int[]{80, 260, 460}) {
            later(delay, DanteMoves.safe("disciple wave", () -> {
                if (fightActive() && summonDisciples(2) > 0) announce("A wave of Dark Disciples steps out of Lucifero's shadow.");
            }));
        }
    }

    @Override
    protected void bossTick() {
        if (!(level() instanceof ServerLevel sl)) return;
        try {
            if (tickCount % 20 == 0 && isAlive() && getHealth() < getMaxHealth()) {
                float t = body.trickle(sl.getGameTime(), getMaxHealth(), power(), exposed());
                if (t > 0f) setHealth(Math.min(getMaxHealth(), getHealth() + t));
            }
            if (surging() && tickCount % 6 == 0) Tells.gather(sl, position().add(0, 1.0, 0), 0.7, BODY_RGB, 5);
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Dante's tick failed: {}", e.toString());
        }
    }

    // ================================================================ Body Magic: the passive
    /**
     * The flesh answers every hit: a pulse if a mortal wound is coming and the Body Magic is whole, a note if it is not. The hit is
     * judged before his armour (a slight overestimate, which only ever favours the party), and a blow the invulnerability frames
     * would swallow is not counted.
     */
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel sl)) return amount;
        try {
            bodyAnswers(sl, source, amount);
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Dante's Body Magic failed: {}", e.toString());
        }
        return amount;
    }

    private void bodyAnswers(ServerLevel sl, DamageSource source, float amount) {
        if (com.newuniverse.nusmp.boss.rules.BossRulesConfig.stripRegeneration()) return;       // 1.37.0: Body Magic's mortal-wound pulse is a second wind; kept behind stripRegeneration=false
        long now = sl.getGameTime();
        float incoming = exposed() ? amount * ZogratisBoss.EXPOSED_BONUS : amount;
        if (invulnerableTime > 10 && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
            if (incoming <= lastHurt) return;                        // inside the invulnerability frames vanilla ignores it, so the flesh does too
            incoming -= lastHurt;                                    // and only what rises above the last blow counts
        }
        boolean anti = DanteMoves.antiMagicHit(source);
        BodyRegen.Decision d = body.onHit(now, getHealth(), getMaxHealth(), incoming, power(), exposed(), anti);
        switch (d.result()) {
            case FIRED -> {
                setHealth(Math.min(getMaxHealth(), getHealth() + d.heal()));
                Vec3 at = position();
                VfxSpawn.send(sl, VfxShape.BODY_FX3, at.add(0, 0.9, 0), at.add(0, 1.9, 0), DanteMoves.BODY, 28, 1.6f);
                sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2f, 0.6f);
                announce("Body Magic closes the wound (+" + Math.round(d.heal()) + " health). It can mend again in " + body.cooldownLeft(now) / 20
                        + " s, and weaker.");
                lastQuip = 0;
                quip("You call that a mortal wound? My body does not know how to die!", "Ha! Try again, my flesh heals faster than you cut!");
            }
            case SUPPRESSED -> {
                if (now - lastBodyNote > 80) {
                    lastBodyNote = now;
                    announce(anti ? "Anti-magic tears into Dante's Body Magic: no regeneration for " + body.suppressedLeft(now) / 20 + " s!"
                            : "Dante's Body Magic is cut: " + body.suppressedLeft(now) / 20 + " s left. Finish him!");
                    if (anti) quip("Tch! That cursed anti-magic... my flesh will not close!");
                }
            }
            case COOLDOWN -> {
                if (now - lastBodyNote > 100) {
                    lastBodyNote = now;
                    announce("Dante's Body Magic is still knitting (" + body.cooldownLeft(now) / 20 + " s). Now is the time to burst him down.");
                }
            }
            case OVERLOADED, OVERWHELMED -> {
                if (now - lastBodyNote > 80) {
                    lastBodyNote = now;
                    announce("The burst is too much for Body Magic: the wound stays open!");
                    quip("Gah... too much, too fast!");
                }
            }
            default -> { }
        }
    }

    // ================================================================ moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (DanteTable.Spec s : DanteTable.specs()) {
            Move.Builder b = Move.builder(s.id(), s.label()).minPower(s.minPower()).windUp(s.windUpTicks()).cooldown(s.cooldownTicks())
                    .range(s.minRange(), s.maxRange()).weight(s.weight()).mana(s.manaCost()).recovery(s.recovery());
            if (s.ultimate()) b.ultimate();
            if (s.crowd()) b.crowd();
            final String mid = s.id();
            final Move.MoveTell base = tellOf(mid);
            final Move.MoveRun run = runOf(mid);
            b.tell((bs, t, left, total) -> {                                  // 1.35.0: the dust telegraph (kept) and the effect that goes with it
                if (base != null) base.tell(bs, t, left, total);
                else if (level() instanceof ServerLevel sl && castingMoveOrNull() != null) defaultTell(sl, castingMoveOrNull(), t, left);
                try { fxTell(mid, t, left, total); } catch (RuntimeException e) { LogUtils.getLogger().warn("[nusmp] Dante's tell effect failed: {}", e.toString()); }
            });
            out.add(b.run((bs, t) -> {
                try { fxRun(mid, t); } catch (RuntimeException e) { LogUtils.getLogger().warn("[nusmp] Dante's strike effect failed: {}", e.toString()); }
                run.run(bs, t);
            }));
        }
        return out;
    }

    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case DanteTable.ROCK_SWORDS -> (b, t) -> {
                DanteMoves.rockSwordVolley(this, t, GravityMath.swordCount(tuning().castSlots()), DanteTable.SWORD);
                quip("Chew on this!", "Stone enough for you?", "A gift from the mountain!");
            };
            case DanteTable.TITAN_FIST -> (b, t) -> {
                DanteMoves.titanFist(this, FIST_REACH, DanteTable.FIST * surgeFactor());
                quip("Down you go!", "Too slow!", "Feel that? That is real strength!");
            };
            case DanteTable.CRUSHING_STOMP -> (b, t) -> {
                DanteMoves.stompWave(this, position(), stompReach(), DanteTable.STOMP * surgeFactor(), 1.0);
                quip("Jump all you like, the floor will catch you!", "Get off my ground!");
            };
            case DanteTable.GRAVITY_DOMAIN -> (b, t) -> {
                Vec3 m = takeMark(t);
                double r = fieldRadius();
                float s = fieldStrength();
                DanteMoves.gravityField(this, m, r, gatesOpen() ? 120 : 100, s);
                if (gatesOpen()) chainField(t, r, s);
                quip("Kneel! Lucifero's weight is on you!", "Heavy, isn't it? The Underworld is heavier.");
            };
            case DanteTable.GRAVITY_PULL -> (b, t) -> {
                DanteMoves.pullAndSlam(this, 30, 1.0 + 0.15 * stage(), DanteTable.PULL_SLAM);
                quip("Come here! I am not done with you!", "Closer! I like it close!");
            };
            case DanteTable.WEIGHTED_LEAP -> (b, t) -> {
                Vec3 land = takeMark(t);
                if (ZogratisBoss.groundOrNull(level(), land) != null && blinkTo(land))                 // never into the air: he would fall and be hurt
                    DanteMoves.stompWave(this, position(), LEAP_REACH, DanteTable.LEAP * surgeFactor(), 1.2);
                else DanteMoves.stompWave(this, position(), 6, DanteTable.LEAP, 1.0);
                quip("Right on top of you!", "Falling in!");
            };
            case DanteTable.STONE_SPIKES -> (b, t) -> {
                Vec3 aim = takeMark(t);
                DanteMoves.stoneSpikes(this, aim.subtract(position()), position().distanceTo(aim) + 4, DanteTable.SPIKES);
                quip("The ground bites back!");
            };
            case DanteTable.LUCIFERO_WEIGHT -> (b, t) -> {
                DanteMoves.weightColumn(this, takeMark(t), WEIGHT_RADIUS, DanteTable.WEIGHT);
                quip("Crushed flat!", "Down, down, down!");
            };
            case DanteTable.BODY_SURGE -> (b, t) -> {
                surgeUntil = level().getGameTime() + 160;
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 0));
                if (!body.suppressed(level().getGameTime())) DanteMoves.regenerate(this, 0.03f);        // anti-magic cuts this too
                if (level() instanceof ServerLevel sl)
                    VfxSpawn.send(sl, VfxShape.BODY_FX2, position(), position().add(0, 1, 0), DanteMoves.BODY, 80, 3f);
                announce("Dante's muscles swell: Body Magic surge. His blows hit harder for 8 s.");
                quip("My body does not know how to give up!");
            };
            case DanteTable.CALL_DISCIPLES -> (b, t) -> {
                int made = summonDisciples(2 + (gatesOpen() ? 1 : 0));
                if (made > 0) {
                    if (level() instanceof ServerLevel sl)
                        VfxSpawn.send(sl, VfxShape.DEVIL_CIRCLE, position().add(0, 0.05, 0), position().add(0, 1, 0), DanteMoves.VIOLET, 40, 2f);
                    quip("Up, my Disciples! Show them Lucifero's blessing!");
                } else quip("Come on, then. All of you!");
            };
            case DanteTable.TENSURA_GRAVITY -> (b, t) -> {
                if (!castTensura(t)) DanteMoves.rockSwordVolley(this, t, 3, DanteTable.SWORD);
                quip("A little extra!");
            };
            case DanteTable.SINGULARITY -> (b, t) -> runSingularity(t);
            case DanteTable.TWIN_WELLS -> (b, t) -> {
                List<Vec3> ms = marks.size() >= 2 ? marks : twinMarks(t);
                marks = List.of();
                for (Vec3 m : ms) DanteMoves.gravityField(this, m, fieldRadius() * 0.8, 90, fieldStrength() * 0.85f);
                quip("Two for the price of one!", "Lucifero's weight, doubled!");
            };
            case DanteTable.TITAN_QUAKE -> (b, t) -> {
                for (int k = 0; k < 3; k++) {
                    final int wave = k;
                    later(k * 14, DanteMoves.safe("titan quake", () -> DanteMoves.stompWave(this, position(), quakeReach(wave), DanteTable.STOMP * 0.8f * surgeFactor(), 1.0)));
                }
                quip("Feel the mountain shake!", "Jump! Jump! Jump!");
            };
            default -> (b, t) -> { };
        };
    }

    /** The Gravity Singularity: a sphere on every mark the wind-up showed; once at the Gates, a second set follows the first. */
    private void runSingularity(LivingEntity target) {
        float p = singularityPower();
        List<Vec3> ms = marks.isEmpty() ? pickMarks(target) : marks;
        marks = List.of();
        for (Vec3 m : ms) DanteMoves.singularity(this, m, p);
        say("Gravity Singularity! Let us see you crawl out of this crater!");
        if (gatesOpen() && !doubleUsed) {
            doubleUsed = true;
            later(GravityMath.GATHER + 30, DanteMoves.safe("second singularity", () -> {
                LivingEntity tg = getTarget();
                if (tg == null || !tg.isAlive()) return;
                announce("Dante is not done: a second set of spheres rises over the survivors!");
                say("Again! Lucifero does not stop at one!");
                for (Vec3 m : pickMarks(tg)) DanteMoves.singularity(this, m, p);
            }));
        }
    }

    /** At the Gates a field is followed by a second one on another foe: shown for 24 ticks, then it takes hold. */
    private void chainField(LivingEntity target, double r, float s) {
        later(60, DanteMoves.safe("chained field", () -> {
            if (!(level() instanceof ServerLevel sl) || sl.getGameTime() < quietUntil) return;
            List<LivingEntity> foes = foes(40);
            LivingEntity next = foes.isEmpty() ? target : foes.get(getRandom().nextInt(foes.size()));
            if (next == null || !next.isAlive()) return;
            Vec3 m = ZogratisBoss.ground(sl, next.position());
            announce("Lucifero's Domain spreads: another field takes hold in two seconds!");
            SpellRuntime.zone(sl, 24, 4, DanteMoves.safe("chained field tell", age -> {
                Tells.ring(sl, m, r, WARN, 1.3f, (int) Math.min(72, 12 + r * 4));
                Tells.column(sl, m, 4, ACCENT);
            }));
            ZogratisVfx.well(sl, m, r, 26, RGB, true);
            later(24, DanteMoves.safe("chained field", () -> {
                if (sl.getGameTime() >= quietUntil) DanteMoves.gravityField(this, m, r, 80, s);
            }));
        }));
    }

    // ================================================================ marks: where the big moves land
    /** The spot the wind-up showed (the target's feet when it began), or the target's feet now if there was none. */
    private Vec3 takeMark(LivingEntity target) {
        Vec3 m = marks.isEmpty() ? ZogratisBoss.ground(level(), target.position()) : marks.get(0);
        marks = List.of();
        return m;
    }

    /** The spheres of the Singularity: on the target, on the other foes, then round the target, never stacked. */
    private List<Vec3> pickMarks(LivingEntity target) {
        List<LivingEntity> foes = foes(44);
        List<double[]> others = new ArrayList<>();
        for (LivingEntity e : foes) if (e != target) others.add(new double[]{e.getX(), e.getZ()});
        double r = GravityMath.zoneRadius(singularityPower());
        List<double[]> plan = GravityMath.planMarks(Math.max(1, GravityMath.markCount(power())), target.getX(), target.getZ(),
                others.toArray(new double[0][]), r, getRandom().nextDouble() * Math.PI * 2);
        List<Vec3> out = new ArrayList<>();
        for (double[] m : plan) out.add(ZogratisBoss.ground(level(), new Vec3(m[0], target.getY(), m[1])));
        return out;
    }

    /** Two wells: on the target and on the far side of the boss, 120 degrees round him. */
    private List<Vec3> twinMarks(LivingEntity target) {
        Vec3 a = ZogratisBoss.ground(level(), target.position());
        Vec3 off = a.subtract(position()).multiply(1, 0, 1);
        Vec3 b = ZogratisBoss.ground(level(), position().add(DanteMoves.rotateY(off, 2.1)));
        return List.of(a, b);
    }

    // ================================================================ 1.35.0 effects: gravity wells, warped rings, lanes
    /** The effect of a move's wind-up, started once (the tell runs every third tick); it lasts the wind-up and its rim closes with it. */
    private void fxTell(String id, LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl) || !first(left, total)) return;
        final int dur = total + 2;
        switch (id) {
            case DanteTable.ROCK_SWORDS -> {
                Vec3 from = position(), aim = target.position().subtract(from).multiply(1, 0, 1);
                if (aim.lengthSqr() < 1e-4) return;
                aim = aim.normalize();
                for (double a : GravityMath.fan(Math.min(5, GravityMath.swordCount(tuning().castSlots()))))
                    ZogratisVfx.lane(sl, from, from.add(DanteMoves.rotateY(aim, a).scale(16)), 1.4, dur, RGB, true);
            }
            case DanteTable.TITAN_FIST -> {
                Vec3 look = getViewVector(1f).multiply(1, 0, 1);
                if (look.lengthSqr() > 1e-4) ZogratisVfx.lane(sl, position(), position().add(look.normalize().scale(FIST_REACH)), 2.6, dur, BODY_RGB, true);
                ZogratisVfx.gravityAura(sl, this, 1.2, dur + 4, DanteMoves.BODY);
            }
            case DanteTable.CRUSHING_STOMP -> ZogratisVfx.well(sl, position(), stompReach(), dur, RGB, true);
            case DanteTable.GRAVITY_DOMAIN -> { if (!marks.isEmpty()) ZogratisVfx.well(sl, marks.get(0), fieldRadius(), dur, RGB, true); }
            case DanteTable.GRAVITY_PULL -> {
                ZogratisVfx.well(sl, position(), 8, dur, RGB, true);
                int n = 0;
                for (LivingEntity e : foes(30)) { if (++n > 4) break; ZogratisVfx.lane(sl, e.position(), position(), 1.4, dur, RGB, true); }
            }
            case DanteTable.WEIGHTED_LEAP -> { if (!marks.isEmpty()) ZogratisVfx.well(sl, marks.get(0), LEAP_REACH, dur, WARN, true); }
            case DanteTable.STONE_SPIKES -> {
                if (marks.isEmpty()) return;
                Vec3 flat = marks.get(0).subtract(position()).multiply(1, 0, 1);
                if (flat.lengthSqr() < 1e-4) return;
                ZogratisVfx.lane(sl, position(), position().add(flat.normalize().scale(Math.min(28, flat.length() + 4))), 3.4, dur, RGB, true);
            }
            case DanteTable.LUCIFERO_WEIGHT -> { if (!marks.isEmpty()) ZogratisVfx.well(sl, marks.get(0), WEIGHT_RADIUS, dur, WARN, true); }
            case DanteTable.BODY_SURGE -> ZogratisVfx.gravityAura(sl, this, 1.4, dur + 6, DanteMoves.BODY);
            case DanteTable.CALL_DISCIPLES -> ZogratisVfx.well(sl, position(), 5, dur, RGB, true);
            case DanteTable.TENSURA_GRAVITY -> ZogratisVfx.gravityAura(sl, this, 1.0, dur, DanteMoves.VIOLET);
            case DanteTable.TWIN_WELLS -> {
                marks = twinMarks(target);
                for (Vec3 m : marks) ZogratisVfx.well(sl, m, fieldRadius() * 0.8, dur, RGB, true);
                announce("Dante opens two gravity wells at once. Leave both rings before they close!");
            }
            case DanteTable.TITAN_QUAKE -> {
                for (int k = 0; k < 3; k++) ZogratisVfx.well(sl, position(), quakeReach(k), dur + 14 * k, k == 2 ? WARN : RGB, true);
                ZogratisVfx.gravityAura(sl, this, 1.4, dur, DanteMoves.BODY);
                announce("Dante gathers a Titan Quake: three rings, one after the other. Jump each one!");
            }
            default -> { }
        }
    }

    /** The strike of a move that DanteMoves does not draw itself. */
    private void fxRun(String id, LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        switch (id) {
            case DanteTable.ROCK_SWORDS -> {
                Vec3 from = position(), aim = target.position().subtract(from).multiply(1, 0, 1);
                if (aim.lengthSqr() < 1e-4) return;
                aim = aim.normalize();
                for (double a : GravityMath.fan(Math.min(5, GravityMath.swordCount(tuning().castSlots()))))
                    ZogratisVfx.lane(sl, from, from.add(DanteMoves.rotateY(aim, a).scale(16)), 1.2, 16, DanteMoves.VIOLET, false);
            }
            case DanteTable.BODY_SURGE -> ZogratisVfx.gravityAura(sl, this, 1.6, 160, DanteMoves.BODY);
            case DanteTable.CALL_DISCIPLES -> ZogratisVfx.gravityImpact(sl, position(), 4.5, DanteMoves.VIOLET);
            case DanteTable.TENSURA_GRAVITY -> ZogratisVfx.lane(sl, position(), target.position(), 1.6, 14, DanteMoves.VIOLET, false);
            default -> { }
        }
    }

    // ================================================================ telegraphs
    /** The first call of a wind-up (the tell runs every third tick, so the first is within three ticks of the start). */
    private static boolean first(int left, int total) { return left >= total - 3; }

    private Move.MoveTell tellOf(String id) {
        return switch (id) {
            case DanteTable.ROCK_SWORDS -> (b, t, left, total) -> tellSwords(t);
            case DanteTable.TITAN_FIST -> (b, t, left, total) -> tellFist(left, total);
            case DanteTable.CRUSHING_STOMP -> (b, t, left, total) -> tellStomp(stompReach(), left, total);
            case DanteTable.GRAVITY_DOMAIN -> (b, t, left, total) -> tellMark(t, left, total, fieldRadius(), RGB);
            case DanteTable.GRAVITY_PULL -> (b, t, left, total) -> tellPull();
            case DanteTable.WEIGHTED_LEAP -> (b, t, left, total) -> tellMark(t, left, total, LEAP_REACH, WARN);
            case DanteTable.STONE_SPIKES -> (b, t, left, total) -> tellSpikes(t, left, total);
            case DanteTable.LUCIFERO_WEIGHT -> (b, t, left, total) -> tellMark(t, left, total, WEIGHT_RADIUS, WARN);
            case DanteTable.BODY_SURGE -> (b, t, left, total) -> {
                if (level() instanceof ServerLevel sl) Tells.gather(sl, position().add(0, 1.0, 0), 0.9, BODY_RGB, 8);
            };
            case DanteTable.CALL_DISCIPLES -> (b, t, left, total) -> {
                if (level() instanceof ServerLevel sl) {
                    Tells.ring(sl, position(), 5, RGB);
                    Tells.gather(sl, position().add(0, 1.2, 0), 1.0, ACCENT, 6);
                }
            };
            case DanteTable.SINGULARITY -> (b, t, left, total) -> tellSingularity(t, left, total);
            default -> null;
        };
    }

    private void tellSwords(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = position().add(0, getBbHeight() * 0.75, 0);
        Vec3 aim = target.getBoundingBox().getCenter().subtract(from);
        if (aim.lengthSqr() < 1e-4) return;
        aim = aim.normalize();
        for (double a : GravityMath.fan(Math.min(5, GravityMath.swordCount(tuning().castSlots()))))
            Tells.line(sl, from, from.add(DanteMoves.rotateY(aim, a).scale(16)), RGB);
        Tells.gather(sl, from, 0.7, ACCENT, 4);
    }

    private void tellFist(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 look = getViewVector(1f).multiply(1, 0, 1);
        Tells.ring(sl, position(), 2.0 + (FIST_REACH - 2.0) * (total - left) / Math.max(1, total), WARN, 1.2f, 32);
        if (look.lengthSqr() > 1e-4) Tells.line(sl, position().add(0, 1, 0), position().add(0, 1, 0).add(look.normalize().scale(FIST_REACH)), WARN);
    }

    private void tellStomp(double reach, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Tells.ring(sl, position(), Math.max(1.5, reach * (total - left) / Math.max(1, total)), RGB, 1.3f, 48);
        if (left % 6 == 0) Tells.ring(sl, position(), reach, WARN, 1.1f, 56);
        Tells.gather(sl, position().add(0, 0.3, 0), 1.2, ACCENT, 5);
    }

    /** A ring at the spot the target stands on when the wind-up begins; it stays there, so moving off it is the answer. */
    private void tellMark(LivingEntity target, int left, int total, double radius, int rgb) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            marks = List.of(ZogratisBoss.ground(sl, target.position()));
            Vec3 m = marks.get(0);
            VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, m.add(0, 0.05, 0), m.add(0, 1, 0), DanteMoves.VIOLET, total, (float) (radius / 1.6));
        }
        if (marks.isEmpty()) return;
        Vec3 m = marks.get(0);
        Tells.ring(sl, m, radius, rgb, 1.4f, (int) Math.min(80, 14 + radius * 5));
        Tells.ring(sl, m, Math.max(0.6, radius * left / Math.max(1, total)), ACCENT, 1.1f, 28);
        Tells.column(sl, m, 4, ACCENT);
    }

    private void tellSpikes(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) marks = List.of(ZogratisBoss.ground(sl, target.position()));
        if (marks.isEmpty()) return;
        Vec3 flat = marks.get(0).subtract(position()).multiply(1, 0, 1);
        if (flat.lengthSqr() < 1e-4) return;
        Vec3 d = flat.normalize(), side = new Vec3(-d.z, 0, d.x).scale(1.7);
        Vec3 from = position().add(0, 0.2, 0), to = from.add(d.scale(Math.min(28, flat.length() + 4)));
        Tells.line(sl, from.add(side), to.add(side), RGB);
        Tells.line(sl, from.subtract(side), to.subtract(side), RGB);
    }

    private void tellPull() {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 at = position().add(0, 1, 0);
        Tells.ring(sl, position(), 5.5, WARN, 1.3f, 40);
        int n = 0;
        for (LivingEntity e : foes(30)) {
            if (++n > 6) break;
            Tells.line(sl, e.position().add(0, 1, 0), at, RGB);
        }
    }

    private void tellSingularity(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            DanteMoves.releaseFields(this);                                   // nobody is held in a field while the spheres are dodged
            quietUntil = sl.getGameTime() + total + GravityMath.GATHER + (gatesOpen() && !doubleUsed ? 70 : 10);
            marks = pickMarks(target);
            int n = marks.size();
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n;
                Vec3 o = position().add(Math.cos(a) * 2.4, 3.2, Math.sin(a) * 2.4);
                VfxSpawn.send(sl, VfxShape.DARK_BLACK_HOLE, o, o.add(0, 1, 0), DanteMoves.VIOLET, left + 4, 1.6f);
            }
            announce("Dante raises " + n + " black spheres. Leave the marked rings, jump on the last tick, or guard!");
            sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3f, 0.4f);
        }
        double r = GravityMath.zoneRadius(singularityPower());
        for (Vec3 m : marks) {
            Tells.ring(sl, m, r, WARN, 1.4f, (int) Math.min(80, 16 + r * 5));
            Tells.ring(sl, m, Math.max(0.8, r * left / Math.max(1, total)), ACCENT, 1.1f, 28);
            Tells.column(sl, m, 5, RGB);
        }
        Tells.gather(sl, position().add(0, 3.2, 0), 2.2, ACCENT, 8);
    }

    // ================================================================ save data
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        long now = level().getGameTime();
        tag.putInt("DPulseCd", body.cooldownLeft(now));
        tag.putInt("DCut", body.suppressedLeft(now));
        tag.putInt("DUsed", body.used());
        tag.putBoolean("DDouble", doubleUsed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        body.restore(level().getGameTime(), tag.getInt("DPulseCd"), tag.getInt("DCut"), tag.getInt("DUsed"));
        doubleUsed = tag.getBoolean("DDouble");
    }
}
