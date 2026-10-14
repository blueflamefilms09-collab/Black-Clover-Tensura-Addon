package com.newuniverse.nusmp.entity.zogratis;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Vanica Zogratis, the cheerful, cruel middle sibling of the Dark Triad, contracted to the devil Megicula. A fast striker-caster who
 * lives at mid range (900 health, quick, hits through armour): she opens the fight smiling and the fight is a lesson in not standing
 * in what she draws on the ground.
 * <ul>
 *   <li><b>Native magic, Blood Magic</b> ({@link VanicaMoves}): she shapes blood into lances, a scythe, a whip, a dash, a burst and a
 *       rain of spears, and into Blood Beasts that hunt for her. Every wound she opens feeds her a little (blood drain: a share of the
 *       damage, capped per hit and per two seconds; a Blood Frenzy doubles it). The cure is burst damage while she is not draining
 *       (the ultimate leaves her exposed) and killing the beasts, which heal on their bites too.</li>
 *   <li><b>Devil magic, Curse-Warding</b> ({@link VanicaCurse}): the Rotting Field spreads from a ring on the ground, weakens and withers
 *       everyone in it and rots the land (reversibly, only with griefBlocks on); the Curse of Decay brands foes standing in a sigil,
 *       weakens their blows and cuts their healing, and bites harder the longer they stay: leave the sigil, cleanse it or hold
 *       anti-magic and it breaks.</li>
 *   <li><b>Power</b>: from 20% to 80% the numbers grow with the curve; at 100% (Gates open, hybrid form) the field spreads in a second
 *       and a half and grows by a third, the beasts are tougher and up to eight can stand at once, lances multiply to six, the curse
 *       is level 3, and Dark Disciples arrive in waves. At each step of the curve a Disciple answers her call.</li>
 *   <li><b>Crimson Crown</b> (60% and up): a ring of blood thorns erupts at rings drawn for over two seconds, a stampede of
 *       short-lived beasts rises between them and a rotting field spreads from her; she is exposed afterwards.</li>
 * </ul>
 * Every big move is telegraphed for at least 8 ticks, has a counter, and the zones of the big ones are locked where they were drawn,
 * so moving is always the answer.
 */
public class VanicaBoss extends ZogratisBoss {
    private static final int RGB = DevilStyle.color(Devil.MEGICULA), ACCENT = DevilStyle.accent(Devil.MEGICULA), WARN = 0xFF4A4A, BLOOD = VanicaMoves.BLOOD;

    private final VanicaMath.Drain drain = new VanicaMath.Drain();
    private List<Vec3> marks = List.of();
    private Vec3 dashEnd;
    private long frenzyUntil, lastQuip;
    /** Hints already given in this fight: each move's warning goes to the chat once, the rings say the rest. */
    private final Set<String> hinted = new HashSet<>();

    public VanicaBoss(EntityType<VanicaBoss> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() { return ZogratisBoss.bossAttributes(1300, 13, 0.36, 9, 5); }

    @Override public String bossName() { return "Vanica Zogratis"; }
    @Override public Devil devil() { return Devil.MEGICULA; }

    // ================================================================ tuning
    @Override protected float formScale(int form) { return form == FORM_HYBRID ? 1.1f : 1.0f; }
    @Override protected float damageScale() { return 1.15f; }
    /** The plain blow is light: the real damage is in the telegraphed moves. */
    @Override protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7f * frenzyFactor(); }
    /** A striker-caster: she wants to be five to nine blocks away, close enough to dash and cut, far enough to lance and curse. */
    @Override protected double preferredRange(double suggested) { return Math.max(4.5, Math.min(9, suggested)); }
    @Override protected String[] tensuraKeywords() { return new String[]{"curse", "darkness", "shadow"}; }
    @Override protected int tensuraKitSize() { return 3; }

    /** In a Blood Frenzy: faster, her drain doubles and her blows hit a fifth harder. */
    public boolean frenzied() { return level().getGameTime() < frenzyUntil; }
    private float frenzyFactor() { return frenzied() ? 1.2f : 1f; }
    /** The blood drain's per-window limiter (used by {@link VanicaMoves}). */
    VanicaMath.Drain drainLimiter() { return drain; }

    private double burstRadius() { return 7.5 + (gatesOpen() ? 1.5 : 0); }
    private double fieldRadius() { return VanicaMath.rotMaxRadius(power()); }

    // ================================================================ lines
    @Override protected String openingLine() { return "Ahaha! A guest in the Spade Kingdom? How lovely! Do bleed nicely for me~"; }

    @Override
    protected String powerLine(int old, int now) {
        if (now >= 80 && old < 80) return "Eighty percent of Megicula! Oh, you are going to make such a lovely stain!";
        if (now >= PowerCurve.INFINITE_MANA_FROM && old < PowerCurve.INFINITE_MANA_FROM)
            return "Sixty percent~ and my mana never runs out now. Isn't that unfair? Hee hee!";
        if (now >= 40 && old < 40) return "Forty percent... Megicula's curses are waking up. Can you feel them crawling?";
        return "Megicula lends me " + now + "%. Smile for me!";
    }

    @Override protected String gatesLine() { return "The Gates of the Tree of Qliphoth open! Megicula, my sweet devil, show them what a curse really is! One hundred percent!"; }
    @Override protected String deathLine() { return "Oh... oh no. I'm bleeding for real. Hee... hee hee... Lucius, big brother... it's up to you now."; }

    /** One of 'lines' now and then (at most every eight seconds), so the chat is not a wall of giggling. */
    private void quip(String... lines) {
        if (tickCount - lastQuip < 160) return;
        lastQuip = tickCount;
        say(lines[getRandom().nextInt(lines.length)]);
    }

    /** A warning for the chat, once per fight and key (the rings and lines on the ground repeat it every time). */
    private void hint(String key, String text) {
        if (hinted.add(key)) announce(text);
    }

    // ================================================================ the fight's hooks
    @Override
    protected void onFightStart() {
        hinted.clear();
        drain.reset();
        marks = List.of();
        dashEnd = null;
        frenzyUntil = 0;
        announce("Blood Magic: Vanica heals a little from every wound she opens. Hit her hard and fast while she is exposed, and kill her beasts.");
        announce("Her Curse of Decay breaks if you leave the sigil, hold anti-magic (a demon sword, the Anti-Magic Lord) or cleanse it.");
    }

    @Override
    protected void onFightEnd(boolean won) {
        marks = List.of();
        dashEnd = null;
        frenzyUntil = 0;
        VanicaMoves.dismissBeasts(this);
        VanicaCurse.liftAll(this);
    }

    @Override
    protected void onPowerChanged(int old, int now) {
        int before = PowerCurve.stage(old), after = PowerCurve.stage(now);
        if (after > before && after < 4 && summonDisciples(after) > 0) announce("Megicula's Dark Disciples answer Vanica's call.");
    }

    @Override
    protected void onGatesOpened() {
        if (level() instanceof ServerLevel sl) ZogratisVfx.bloodSigil(sl, position(), 9, 70, VanicaMoves.BLOOD_ARGB, false);
        announce("Vanica's veins glow with Megicula's power. Her curses spread in a breath, her beasts grow, and her blood lances multiply.");
        for (int delay : new int[]{80, 260, 460}) {
            later(delay, VanicaMoves.safe("disciple wave", () -> {
                if (fightActive() && summonDisciples(2) > 0) announce("A wave of Dark Disciples steps out of Megicula's shadow.");
            }));
        }
    }

    @Override
    protected void bossTick() {
        if (!(level() instanceof ServerLevel sl)) return;
        try {
            if (frenzied() && tickCount % 6 == 0) Tells.gather(sl, position().add(0, 1.0, 0), 0.7, BLOOD, 6);
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Vanica's tick failed: {}", e.toString());
        }
    }

    // ================================================================ moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (VanicaTable.Spec s : VanicaTable.specs()) {
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
                try { fxTell(mid, t, left, total); } catch (RuntimeException e) { LogUtils.getLogger().warn("[nusmp] Vanica's tell effect failed: {}", e.toString()); }
            });
            out.add(b.run((bs, t) -> {
                try { fxRun(mid); } catch (RuntimeException e) { LogUtils.getLogger().warn("[nusmp] Vanica's strike effect failed: {}", e.toString()); }
                run.run(bs, t);
            }));
        }
        return out;
    }

    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case VanicaTable.BLOOD_LANCES -> (b, t) -> {
                VanicaMoves.bloodLances(this, t, VanicaMath.lanceCount(tuning().castSlots(), gatesOpen()), VanicaTable.LANCE * frenzyFactor());
                quip("Pretty, aren't they? Hold still~", "Catch!", "Oops, did that one hurt?");
            };
            case VanicaTable.BLOOD_SCYTHE -> (b, t) -> {
                VanicaMoves.scythe(this, VanicaTable.SCYTHE * frenzyFactor());
                quip("Snip snip!", "Closer, closer~", "Let me see your insides!");
            };
            case VanicaTable.BLOOD_WHIP -> (b, t) -> {
                VanicaMoves.whip(this, t, VanicaTable.WHIP * frenzyFactor());
                quip("Come here, little one!", "Don't run, it's rude!");
            };
            case VanicaTable.CRIMSON_DASH -> (b, t) -> {
                Vec3 end = dashEnd != null ? dashEnd : position().add(t.position().subtract(position()).multiply(1, 0, 1));
                dashEnd = null;
                VanicaMoves.dash(this, end, VanicaTable.DASH * frenzyFactor());
                quip("Too slow~", "Boo!");
            };
            case VanicaTable.BLOOD_BURST -> (b, t) -> {
                VanicaMoves.burst(this, burstRadius(), VanicaTable.BURST * frenzyFactor());
                quip("Pop!", "Bleed for me!");
            };
            case VanicaTable.CRIMSON_RAIN -> (b, t) -> {
                List<Vec3> spots = marks.isEmpty() ? pickRain(t) : marks;
                marks = List.of();
                VanicaMoves.strikeSpots(this, spots, VanicaMath.RAIN_RADIUS, VanicaTable.RAIN * frenzyFactor(), 2, 0.45, power() >= 40);
                quip("It's raining, it's pouring~", "Dance for me!");
            };
            case VanicaTable.ROT_FIELD -> (b, t) -> {
                VanicaMoves.curseRot(this, takeMark(t), fieldRadius(), VanicaMath.rotTicks(power()));
                quip("Everything rots eventually. Let me hurry it along!", "Smell that? That's your future~");
            };
            case VanicaTable.DECAY_CURSE -> (b, t) -> {
                int n = VanicaCurse.sigil(this, takeMark(t), t, VanicaMath.curseTicks(power()), VanicaMath.curseLevel(power()));
                if (n > 0) quip("Megicula, bless them with decay!", "A little curse, just for you~");
                else announce("The sigil closes on empty ground. Vanica pouts.");
            };
            case VanicaTable.BLOOD_BEASTS -> (b, t) -> {
                int made = VanicaMoves.spawnBeasts(this, VanicaMath.beastsPerCast(tuning().castSlots(), power()), false);
                if (made > 0) quip("Playtime, my darlings! Fetch!", "Good boys, go get them!");
                else quip("I have enough friends here already.");
            };
            case VanicaTable.BLOOD_FRENZY -> (b, t) -> {
                frenzyUntil = level().getGameTime() + 160;
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 0));
                if (level() instanceof ServerLevel sl) VfxSpawn.send(sl, VfxShape.BLOOD_FX2, position(), position().add(0, 1, 0), VanicaMoves.BLOOD_ARGB, 60, 2.2f);
                announce("Vanica's blood boils: Blood Frenzy. She is faster, hits a fifth harder, and drains twice as much, for 8 s.");
                quip("Oooh, I'm so excited I could burst!");
            };
            case VanicaTable.CALL_DISCIPLES -> (b, t) -> {
                int made = summonDisciples(2 + (gatesOpen() ? 1 : 0));
                if (made > 0) {
                    if (level() instanceof ServerLevel sl)
                        VfxSpawn.send(sl, VfxShape.DEVIL_CIRCLE, position().add(0, 0.05, 0), position().add(0, 1, 0), 0xFF000000 | RGB, 40, 2f);
                    quip("Come, my pretty ones, show them Megicula's love!");
                } else quip("Come on, then. All of you!");
            };
            case VanicaTable.TENSURA_HEX -> (b, t) -> {
                if (!castTensura(t)) VanicaMoves.bloodLances(this, t, 3, VanicaTable.LANCE);
                quip("A little hex, for flavour~");
            };
            case VanicaTable.REAPER_DANCE -> (b, t) -> {
                VanicaMoves.scythe(this, VanicaTable.SCYTHE * 0.8f * frenzyFactor());
                for (int k = 1; k <= 2; k++) {                                 // two more cuts, each shown 16 ticks before it lands
                    final int cut = k;
                    if (level() instanceof ServerLevel sl) {
                        Vec3 f = VanicaMoves.facing(this), o = position().add(0, 1.1, 0);
                        ZogratisVfx.crescent(sl, o, o.add(f), VanicaMath.SCYTHE_REACH, 16 * cut + 2, VanicaMoves.BLOOD_ARGB, true);
                    }
                    later(16 * cut, VanicaMoves.safe("reaper dance", () -> VanicaMoves.scythe(this, VanicaTable.SCYTHE * 0.8f * frenzyFactor())));
                }
                quip("Snip, snip, snip~", "Dance with me!");
            };
            case VanicaTable.BLOOD_STORM -> (b, t) -> {
                int n = VanicaMath.lanceCount(tuning().castSlots(), gatesOpen());
                VanicaMoves.bloodLances(this, t, n, VanicaTable.LANCE * 0.8f * frenzyFactor());
                for (int k = 1; k <= 2; k++)
                    later(18 * k, VanicaMoves.safe("blood storm", () -> {
                        LivingEntity tg = getTarget();
                        if (tg != null && tg.isAlive()) VanicaMoves.bloodLances(this, tg, n, VanicaTable.LANCE * 0.8f * frenzyFactor());
                    }));
                quip("A storm of blood! Run, run~");
            };
            case VanicaTable.CRIMSON_CROWN -> (b, t) -> {
                List<Vec3> thorns = marks.isEmpty() && level() instanceof ServerLevel sl
                        ? VanicaMoves.thornSpots(sl, position(), power(), getRandom().nextDouble() * Math.PI * 2) : marks;
                marks = List.of();
                VanicaMoves.crown(this, thorns);
                say("Crimson Crown! Kneel before the queen of blood!");
            };
            default -> (b, t) -> { };
        };
    }

    // ================================================================ marks: where the big moves land
    /** The spot the wind-up showed (the target's feet when it began), or the target's feet now if there was none. */
    private Vec3 takeMark(LivingEntity target) {
        Vec3 m = marks.isEmpty() ? ZogratisBoss.ground(level(), target.position()) : marks.get(0);
        marks = List.of();
        return m;
    }

    /** The spots of the Crimson Rain: on the target, on the other foes, then round the target, never stacked. */
    private List<Vec3> pickRain(LivingEntity target) {
        List<double[]> others = new ArrayList<>();
        for (LivingEntity e : foes(44)) if (e != target) others.add(new double[]{e.getX(), e.getZ()});
        List<double[]> plan = VanicaMath.planSpots(3 + tuning().castSlots(), target.getX(), target.getZ(), others.toArray(new double[0][]),
                VanicaMath.RAIN_RING, VanicaMath.RAIN_SPACING, getRandom().nextDouble() * Math.PI * 2);
        List<Vec3> out = new ArrayList<>();
        for (double[] s : plan) out.add(ZogratisBoss.ground(level(), new Vec3(s[0], target.getY(), s[1])));
        return out;
    }

    // ================================================================ 1.35.0 effects: crimson arcs, lances, crescents, sigils
    private void fxTell(String id, LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl) || !first(left, total)) return;
        final int dur = total + 2;
        final int argb = VanicaMoves.BLOOD_ARGB;
        switch (id) {
            case VanicaTable.BLOOD_LANCES, VanicaTable.BLOOD_STORM -> {
                Vec3 from = position().add(0, getBbHeight() * 0.75, 0), aim = target.getBoundingBox().getCenter().subtract(from);
                if (aim.lengthSqr() < 1e-4) return;
                aim = aim.normalize();
                for (double a : VanicaMath.lanceFan(VanicaMath.lanceCount(tuning().castSlots(), gatesOpen())))
                    ZogratisVfx.bloodLance(sl, from, from.add(VanicaMoves.rotateY(aim, a).scale(16)), 1.0, dur, argb, true);
            }
            case VanicaTable.BLOOD_SCYTHE, VanicaTable.REAPER_DANCE -> {
                Vec3 f = VanicaMoves.facing(this), o = position().add(0, 1.1, 0);
                ZogratisVfx.crescent(sl, o, o.add(f), VanicaMath.SCYTHE_REACH, dur, argb, true);
            }
            case VanicaTable.BLOOD_WHIP -> {
                Vec3 from = position().add(0, getBbHeight() * 0.7, 0), aim = target.getBoundingBox().getCenter().subtract(from);
                if (aim.lengthSqr() > 1e-4) ZogratisVfx.bloodLance(sl, from, from.add(aim.normalize().scale(VanicaMath.WHIP_LENGTH)), 0.8, dur, argb, true);
            }
            case VanicaTable.CRIMSON_DASH -> {
                if (dashEnd == null) return;
                ZogratisVfx.bloodLance(sl, position().add(0, 1, 0), dashEnd.add(0, 1, 0), 1.6, dur, argb, true);
                ZogratisVfx.bloodSigil(sl, dashEnd, 1.8, dur, argb, true);
            }
            case VanicaTable.BLOOD_BURST -> ZogratisVfx.bloodSigil(sl, position(), burstRadius(), dur, argb, true);
            case VanicaTable.CRIMSON_RAIN -> { for (Vec3 m : marks) ZogratisVfx.bloodSigil(sl, m, VanicaMath.RAIN_RADIUS + 0.4, dur, argb, true); }
            case VanicaTable.ROT_FIELD -> { if (!marks.isEmpty()) ZogratisVfx.bloodSigil(sl, marks.get(0), fieldRadius(), dur, VanicaMoves.CURSE_ARGB, true); }
            case VanicaTable.DECAY_CURSE -> { if (!marks.isEmpty()) ZogratisVfx.bloodSigil(sl, marks.get(0), VanicaMath.sigilRadius(VanicaMath.curseLevel(power())), dur, VanicaMoves.CURSE_ARGB, true); }
            case VanicaTable.BLOOD_BEASTS -> ZogratisVfx.bloodSigil(sl, position(), 4.5, dur, argb, true);
            case VanicaTable.BLOOD_FRENZY -> ZogratisVfx.bloodSigil(sl, position(), 2.6, dur, argb, true);
            case VanicaTable.CALL_DISCIPLES -> ZogratisVfx.bloodSigil(sl, position(), 5, dur, VanicaMoves.CURSE_ARGB, true);
            case VanicaTable.TENSURA_HEX -> ZogratisVfx.bloodSigil(sl, position(), 2.2, dur, VanicaMoves.CURSE_ARGB, true);
            case VanicaTable.CRIMSON_CROWN -> {
                ZogratisVfx.bloodSigil(sl, position(), VanicaMath.crownRadius(power()), dur, argb, true);
                for (Vec3 m : marks) ZogratisVfx.bloodSigil(sl, m, VanicaMoves.THORN_RADIUS, dur, argb, true);
            }
            default -> { }
        }
    }

    private void fxRun(String id) {
        if (!(level() instanceof ServerLevel sl)) return;
        switch (id) {
            case VanicaTable.BLOOD_FRENZY -> ZogratisVfx.bloodSigil(sl, position(), 3.2, 60, VanicaMoves.BLOOD_ARGB, false);
            case VanicaTable.CALL_DISCIPLES -> ZogratisVfx.bloodSigil(sl, position(), 4.5, 30, VanicaMoves.CURSE_ARGB, false);
            case VanicaTable.BLOOD_BEASTS -> ZogratisVfx.bloodSigil(sl, position(), 5, 30, VanicaMoves.BLOOD_ARGB, false);
            default -> { }
        }
    }

    // ================================================================ telegraphs
    /** The first call of a wind-up (the tell runs every third tick, so the first is within three ticks of the start). */
    private static boolean first(int left, int total) { return left >= total - 3; }

    private Move.MoveTell tellOf(String id) {
        return switch (id) {
            case VanicaTable.BLOOD_LANCES -> (b, t, left, total) -> tellLances(t);
            case VanicaTable.BLOOD_SCYTHE -> (b, t, left, total) -> tellScythe();
            case VanicaTable.BLOOD_WHIP -> (b, t, left, total) -> tellWhip(t);
            case VanicaTable.CRIMSON_DASH -> (b, t, left, total) -> tellDash(t, left, total);
            case VanicaTable.BLOOD_BURST -> (b, t, left, total) -> tellBurst(left, total);
            case VanicaTable.CRIMSON_RAIN -> (b, t, left, total) -> tellRain(t, left, total);
            case VanicaTable.ROT_FIELD -> (b, t, left, total) -> tellMark(t, left, total, fieldRadius(), RGB, VanicaTable.ROT_FIELD, "Vanica lets the rot loose. The rot will spread across the whole ring: leave it, and keep out until it fades.");
            case VanicaTable.DECAY_CURSE -> (b, t, left, total) -> tellMark(t, left, total, VanicaMath.sigilRadius(VanicaMath.curseLevel(power())), WARN, VanicaTable.DECAY_CURSE,
                    "Vanica draws a sigil of decay. Leave the ring before it closes, or the curse will brand you.");
            case VanicaTable.BLOOD_BEASTS -> (b, t, left, total) -> {
                if (level() instanceof ServerLevel sl) {
                    Tells.gather(sl, position().add(0, 0.3, 0), 3.0, BLOOD, 10);
                    Tells.ring(sl, position(), 4.5, RGB, 1.2f, 30);
                }
            };
            case VanicaTable.BLOOD_FRENZY -> (b, t, left, total) -> {
                if (level() instanceof ServerLevel sl) Tells.gather(sl, position().add(0, 1.0, 0), 0.9, BLOOD, 8);
            };
            case VanicaTable.CALL_DISCIPLES -> (b, t, left, total) -> {
                if (level() instanceof ServerLevel sl) {
                    Tells.ring(sl, position(), 5, RGB);
                    Tells.gather(sl, position().add(0, 1.2, 0), 1.0, ACCENT, 6);
                }
            };
            case VanicaTable.CRIMSON_CROWN -> (b, t, left, total) -> tellCrown(left, total);
            default -> null;
        };
    }

    private void tellLances(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = position().add(0, getBbHeight() * 0.75, 0);
        Vec3 aim = target.getBoundingBox().getCenter().subtract(from);
        if (aim.lengthSqr() < 1e-4) return;
        aim = aim.normalize();
        for (double a : VanicaMath.lanceFan(VanicaMath.lanceCount(tuning().castSlots(), gatesOpen())))
            Tells.line(sl, from, from.add(VanicaMoves.rotateY(aim, a).scale(16)), RGB);
        Tells.gather(sl, from, 0.7, ACCENT, 4);
    }

    private void tellScythe() {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 f = VanicaMoves.facing(this), o = position().add(0, 0.15, 0);
        double half = VanicaMath.SCYTHE_HALF_ANGLE, reach = VanicaMath.SCYTHE_REACH;
        Vec3 prev = null;
        for (int i = 0; i <= 6; i++) {
            Vec3 p = o.add(VanicaMoves.rotateY(f, -half + i * (2 * half / 6)).scale(reach));
            if (prev != null) Tells.line(sl, prev, p, WARN);
            prev = p;
        }
        Tells.line(sl, o, o.add(VanicaMoves.rotateY(f, -half).scale(reach)), WARN);
        Tells.line(sl, o, o.add(VanicaMoves.rotateY(f, half).scale(reach)), WARN);
    }

    private void tellWhip(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = position().add(0, getBbHeight() * 0.7, 0);
        Vec3 aim = target.getBoundingBox().getCenter().subtract(from);
        if (aim.lengthSqr() < 1e-4) return;
        Vec3 end = from.add(aim.normalize().scale(VanicaMath.WHIP_LENGTH));
        Tells.line(sl, from, end, WARN);
        Vec3 flat = aim.multiply(1, 0, 1);
        if (flat.lengthSqr() < 1e-4) return;
        Vec3 n = flat.normalize(), side = new Vec3(-n.z, 0, n.x).scale(VanicaMath.WHIP_WIDTH);
        Tells.line(sl, from.add(side), end.add(side), RGB);
        Tells.line(sl, from.subtract(side), end.subtract(side), RGB);
    }

    /** The line is drawn when the wind-up begins and stays: step off it. */
    private void tellDash(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total) || dashEnd == null) {
            Vec3 flat = target.position().subtract(position()).multiply(1, 0, 1);
            if (flat.lengthSqr() > 1e-4) dashEnd = position().add(flat.normalize().scale(VanicaMath.dashLength(flat.length())));
        }
        if (dashEnd == null) return;
        Vec3 from = position().add(0, 0.2, 0), to = dashEnd.add(0, 0.2, 0);
        Vec3 flat = to.subtract(from).multiply(1, 0, 1);
        if (flat.lengthSqr() < 1e-4) return;
        Vec3 n = flat.normalize(), side = new Vec3(-n.z, 0, n.x).scale(VanicaMath.DASH_WIDTH);
        Tells.line(sl, from.add(side), to.add(side), WARN);
        Tells.line(sl, from.subtract(side), to.subtract(side), WARN);
        Tells.line(sl, from, to, RGB);
        Tells.ring(sl, to, 1.6, ACCENT, 1.2f, 20);
    }

    private void tellBurst(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        double r = burstRadius();
        Tells.ring(sl, position(), Math.max(1.5, r * (total - left) / Math.max(1, total)), RGB, 1.3f, 48);
        if (left % 6 == 0) Tells.ring(sl, position(), r, WARN, 1.1f, 56);
        Tells.gather(sl, position().add(0, 0.8, 0), 1.2, BLOOD, 6);
    }

    /** A ring at the spot the target stands on when the wind-up begins; it stays there, so moving off it is the answer. */
    private void tellMark(LivingEntity target, int left, int total, double radius, int rgb, String key, String warning) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            marks = List.of(ZogratisBoss.ground(sl, target.position()));
            Vec3 m = marks.get(0);
            VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, m.add(0, 0.05, 0), m.add(0, 1, 0), VanicaMoves.CURSE_ARGB, total, (float) (radius / 1.6));
            hint(key, warning);
        }
        if (marks.isEmpty()) return;
        Vec3 m = marks.get(0);
        Tells.ring(sl, m, radius, rgb, 1.4f, (int) Math.min(80, 14 + radius * 5));
        Tells.ring(sl, m, Math.max(0.6, radius * left / Math.max(1, total)), ACCENT, 1.1f, 28);
        Tells.column(sl, m, 4, ACCENT);
    }

    private void tellRain(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            marks = pickRain(target);
            hint(VanicaTable.CRIMSON_RAIN, "Blood gathers above the marked rings. Leave them before it falls!");
            sl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 2f, 0.4f);
        }
        for (Vec3 m : marks) {
            Tells.ring(sl, m, VanicaMath.RAIN_RADIUS, WARN, 1.3f, 24);
            Tells.ring(sl, m, Math.max(0.5, VanicaMath.RAIN_RADIUS * left / Math.max(1, total)), ACCENT, 1.1f, 16);
            Tells.column(sl, m, 5, BLOOD);
        }
    }

    private void tellCrown(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            marks = VanicaMoves.thornSpots(sl, position(), power(), getRandom().nextDouble() * Math.PI * 2);
            VfxSpawn.send(sl, VfxShape.BLOOD_FX2, position().add(0, 0.05, 0), position().add(0, 1, 0), VanicaMoves.BLOOD_ARGB, total + 12, 5f);
            VfxSpawn.send(sl, VfxShape.DEVIL_CIRCLE, position().add(0, 0.05, 0), position().add(0, 1, 0), 0xFF000000 | RGB, total, 3f);
            hint(VanicaTable.CRIMSON_CROWN, "Vanica raises a crown of blood. Thorns will erupt in the marked rings: stand between them, and mind the beasts and the rot!");
            sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3f, 0.5f);
        }
        for (Vec3 m : marks) {
            Tells.ring(sl, m, VanicaMoves.THORN_RADIUS, WARN, 1.3f, 20);
            Tells.column(sl, m, 4, ACCENT);
        }
        Tells.ring(sl, position(), Math.max(2, VanicaMath.crownRadius(power()) * (total - left) / Math.max(1, total)), RGB, 1.3f, 72);
        Tells.gather(sl, position().add(0, 1.4, 0), 1.6, BLOOD, 8);
    }

    // ================================================================ save data
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("VFrenzy", (int) Math.max(0, frenzyUntil - level().getGameTime()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        frenzyUntil = level().getGameTime() + tag.getInt("VFrenzy");
    }
}
