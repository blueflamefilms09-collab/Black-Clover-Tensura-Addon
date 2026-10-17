package com.newuniverse.nusmp.arc.sds;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.quests.NusmpEvents;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Why: Escanor, the Lion's Sin of Pride, is a fight against a clock. His power is the sun: at midnight a timid man, at noon the strongest of the Seven Deadly
 * Sins, and the owner asked that the game's time of day matter and that it still work in the sunless Mega-Dungeon. The documented mechanic is this: every
 * dimension shares the world's day time, so he reads it ({@link SunClock}); inside a fight it starts at the hour the world shows and runs on faster
 * (default three times the game's pace, a config), so a fight of a few minutes sees a good part of a day instead of one frozen hour; and once the Gates
 * are open Rhitta pours out the light it stored, so the sun cannot fall under a floor ({@link Sunshine}). What the sun does: his damage, his speed, how much
 * of a blow he shrugs off, the heat round his body, the form (and so the size) of his model, and, above all, which of his moves exist: the power the
 * scheduler sees is the lower of the power his health has reached and the power the sun allows. The counters are the series' own: fight him at night; stay
 * out of the heat or carry its resistances; break the Cruel Sun with a burst of damage while he winds it up, stand behind a block or a shield, or walk out
 * of it. The strikes are in {@link EscanorMoves}, the numbers in {@link EscanorTable}, {@link Sunshine} and {@link CruelSunMath}. Server side only; nothing
 * here has run in a game yet.
 */
public class EscanorBoss extends ArcBoss {
    public static final ArcCharacter CHARACTER = ArcRoster.register(SdsCharacters.ESCANOR);
    /** How the client draws him: the numbers of {@code tools/arc/manifest.json} for the model {@code escanor}, with a wider shadow for the large forms. */
    public static final ArcLook LOOK = new ArcLook("escanor", 2.0f, 1.16f, 2.06f, 1.10f, 1.83f, 0.81f, true);
    public static final DeferredHolder<EntityType<?>, EntityType<EscanorBoss>> TYPE =
            ArcEntities.boss(CHARACTER.id(), EscanorBoss::new, 0.78f, 2.0f, LOOK, EscanorBoss::createAttributes);

    private static final int GOLD = EscanorMoves.GOLD, PALE = EscanorMoves.PALE, WARN = EscanorMoves.WARN;
    private static final ResourceLocation SUN_SPEED = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "sds_escanor_sun");
    private static final double SWING_REACH = 5.8, SWING_HALF = Math.toRadians(70), SMASH_AHEAD = 3.8, SMASH_RADIUS = 3.0, SMASH_WAVE = 7.0, FLARE_RADIUS = 6.5,
            CHARGE_HALF_WIDTH = 1.6, CHARGE_RING = 5.0, HAZE_RADIUS = 3.2, PILLAR_RADIUS = 2.2, SPIN_RADIUS = 6.2;
    private static final int HAZE_TICKS = 90;

    // ---- the fight's state (all server side; a reload in the middle of a fight starts these afresh)
    private final SunClock clock = new SunClock();
    private double rawSun = 0.5, sunNow = 0.5;
    private SunClock.Phase phase = SunClock.Phase.MORNING;
    private double appliedSpeed = Double.NaN;
    private boolean auraAnnounced;
    private Vec3 aim = new Vec3(0, 0, 1);
    private double lockedLength;
    private List<Vec3> marks = List.of();
    private String lockedFor = "";
    private long lastQuip;
    private boolean sunWinding;
    private double sunRadius;
    private float sunStartHealth;
    private final Set<UUID> sunBreakers = new HashSet<>();

    public EscanorBoss(EntityType<EscanorBoss> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return ZogratisBoss.bossAttributes(CHARACTER.health(), CHARACTER.attack(), CHARACTER.speed(), CHARACTER.armor(), 10.0);
    }

    @Override public ArcCharacter character() { return CHARACTER; }

    // ================================================================ tuning: what the sun does
    /** The sun is in every blow: 0.74 of the framework's number at the darkest hour up to 1.40 at noon. */
    @Override protected float damageScale() { return (float) Sunshine.damageScale(sunNow); }

    /** The sun shields him: he takes 1.03 of a blow at night and 0.72 at noon (not while an ultimate has left him exposed, the framework skips this then). */
    @Override protected float mitigate(DamageSource source, float amount) { return (float) (amount * Sunshine.taken(sunNow)); }

    @Override protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7f; }

    /** Pride does not run and does not hide: he wants to be within a few blocks of whoever he fights. */
    @Override protected double preferredRange(double suggested) { return Math.max(2.5, Math.min(6.0, suggested)); }

    /** The Gates open into the large form, or the largest if the sun is at its highest. */
    @Override protected int gatesForm() { return Math.max(FORM_HYBRID, Sunshine.form(form(), sunNow)); }

    /** The frail man of the night, Pride in the daylight, The One: the hitbox follows and the renderer scales the model to it. */
    @Override
    protected float formScale(int form) {
        return form == FORM_DIVINE ? 1.42f : form == FORM_HYBRID ? 1.2f : 0.88f;
    }

    private void applySpeed() {
        double bonus = Sunshine.speedBonus(sunNow);
        if (!Double.isNaN(appliedSpeed) && Math.abs(bonus - appliedSpeed) < 0.004) return;
        appliedSpeed = bonus;
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.addOrReplacePermanentModifier(new AttributeModifier(SUN_SPEED, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    // ================================================================ the bar and the lines
    @Override
    public String barTitle() {
        String stored = gatesOpen() && rawSun < SdsaConfig.gatesSunFloor() - 0.005 ? " (stored light)" : "";
        return CHARACTER.name() + " - " + phase.label() + stored + " " + power() + "%";
    }

    @Override
    protected String powerLine(int old, int now) {
        if (now >= 80) return "Do you feel it? The sun is on my side today.";
        if (now >= 60) return "I am not even warm yet. Come closer.";
        return "I will fight, if I must...";
    }

    /** The framework announces the Gates in the devil's voice; the party reads his own. */
    @Override
    public void announce(String text) {
        super.announce(SdsaStrikes.QLIPHOTH_GATES.equals(text) ? "Escanor roars: Rhitta blazes with all the light it stored!" : text);
    }

    private void quip(String shy, String proud, String one) {
        if (tickCount - lastQuip < 140) return;
        lastQuip = tickCount;
        say(form() == FORM_DIVINE ? one : form() == FORM_HYBRID ? proud : shy);
    }

    /** Pushes the new phase name into the boss bar of everyone who watches (the bar's title is rebuilt when a player starts to see him; the framework's own refresh is private). */
    private void refreshTitle(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) < 64 * 64 && fighters().contains(p.getUUID())) {
                startSeenByPlayer(p);
                return;
            }
        }
    }

    // ================================================================ the fight's hooks
    @Override
    protected void onFightStart() {
        if (level() instanceof ServerLevel sl) startClock(sl);
        sunWinding = false;
        sunBreakers.clear();
        auraAnnounced = false;
        announce("Sunshine: Escanor's power is the sun. By the clock of this fight (" + String.format(java.util.Locale.ROOT, "%.0f", SdsaConfig.sunSpeed())
                + "x the game's pace) the sun's height sets his damage, speed and the heat round him, and which moves he has: only at noon does he use his strongest. "
                + "At night he is weak. Once the Gates open the sun cannot fall below " + Math.round(SdsaConfig.gatesSunFloor() * 100) + "%.");
    }

    @Override
    protected void onFightEnd(boolean won) {
        clock.stop();
        sunWinding = false;
        sunBreakers.clear();
        marks = List.of();
        lockedFor = "";
        super.onFightEnd(won);
    }

    @Override
    protected void onGatesOpened() {
        announce("Rhitta pours out the light it stored: the sun round Escanor cannot fall below " + Math.round(SdsaConfig.gatesSunFloor() * 100) + "% for the rest of the fight.");
    }

    private void startClock(ServerLevel sl) {
        clock.start(SdsaConfig.sunStart(), sl.getDayTime(), sl.getGameTime(), SdsaConfig.sunSpeed());
    }

    @Override
    protected void bossTick() {
        if (!(level() instanceof ServerLevel sl)) return;
        try {
            if (fightActive() && !clock.running()) startClock(sl);
            if (!fightActive() && clock.running()) clock.stop();
            long game = sl.getGameTime(), world = sl.getDayTime();
            rawSun = clock.sunshine(world, game);
            sunNow = Sunshine.effective(rawSun, gatesOpen(), SdsaConfig.gatesSunFloor());
            applySpeed();
            SunClock.Phase now = SunClock.phase(rawSun, SunClock.rising(clock.dayTime(world, game)));
            if (now != phase) {
                phase = now;
                if (fightActive()) {
                    announce("The sun: " + now.label() + ".");
                    refreshTitle(sl);
                }
            }
            if (!transforming()) updateForm(sl);
            if (tickCount % 20 == 0 && fightActive() && !transforming() && SdsaConfig.heatAura()) heat(sl);
            if (tickCount % 30 == 0 && sunNow >= SunClock.THE_ONE) {
                Vec3 head = position().add(0, getBbHeight() * 1.05, 0);
                SdsaStrikes.fx(sl, VfxShape.LIGHT_FLARE, head, head.add(0, 1, 0), PALE, 36, 2.2f, false);
            }
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Escanor's tick failed: {}", e.toString());
        }
    }

    /** The model follows the sun, with hysteresis so it never flickers at a threshold. */
    private void updateForm(ServerLevel sl) {
        int f = Sunshine.form(form(), sunNow);
        if (gatesOpen()) f = Math.max(f, FORM_HYBRID);
        if (f == form()) return;
        boolean up = f > form();
        setForm(f);
        Vec3 chest = position().add(0, getBbHeight() * 0.5, 0);
        SdsaStrikes.fx(sl, VfxShape.LIGHT_FLARE, chest, chest.add(0, 1, 0), PALE, 24, 4.0f, true);
        SdsaStrikes.sound(sl, position(), up ? SoundEvents.BEACON_POWER_SELECT : SoundEvents.BEACON_DEACTIVATE, 2.0f, up ? 1.2f : 0.7f);
        if (!fightActive()) return;
        announce(f == FORM_DIVINE ? "Noon: Escanor is The One. Nothing in the dungeon is stronger."
                : up ? "The sun climbs: Escanor's body swells, his timidity is gone. This is Pride."
                : f == FORM_BASE ? "The sun is low: Escanor shrinks back into the frail man. Strike now!" : "The sun slips: The One fades into plain Pride.");
        if (up) quip("...!", "The sun is on my side!", "I am the One!");
    }

    /** Once a second the heat of his body burns whoever stands near him while the sun is high. */
    private void heat(ServerLevel sl) {
        double r = Sunshine.auraRadius(sunNow);
        if (r <= 0) return;
        if (!auraAnnounced) {
            auraAnnounced = true;
            announce("The heat of Escanor's body burns everything within " + Math.round(r) + " blocks while the sun is high. Stay out of it, or carry flame and heat resistance.");
        }
        Tells.ring(sl, position(), r, GOLD, 0.9f, (int) Math.min(80, 16 + r * 5));
        float dmg = Sunshine.auraDamage(sunNow);
        for (LivingEntity e : SdsaStrikes.foesInDisc(this, position(), r)) SdsaStrikes.hit(this, e, dmg, TensuraDamageTypes.HEAT_WAVE);
    }

    // ================================================================ the damage that reaches him
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        if (sunWinding && source.getEntity() instanceof ServerPlayer p) sunBreakers.add(p.getUUID());
        return amount;
    }

    // ================================================================ choosing the next move
    /** The scheduler is shown the power the sun allows (never more than his health has reached), so a move that needs daylight does not exist at night. */
    @Override
    protected int chooseMove(List<Move> bk, MoveScheduler.Situation s, long t) {
        MoveScheduler.Situation capped = new MoveScheduler.Situation(s.distance(), s.healthFraction(), Sunshine.effectivePower(s.power(), sunNow), s.foes(), s.now(), s.mana(), s.ultimateGap());
        return super.chooseMove(bk, capped, t);
    }

    // ================================================================ the moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (EscanorTable.Spec s : EscanorTable.specs()) {
            Move.Builder b = Move.builder(s.id(), s.label()).minPower(s.minPower()).windUp(s.windUpTicks()).cooldown(s.cooldownTicks())
                    .range(s.minRange(), s.maxRange()).weight(s.weight()).mana(s.manaCost()).recovery(s.recovery());
            if (s.ultimate()) b.ultimate();
            if (s.crowd()) b.crowd();
            final String id = s.id();
            final Move.MoveRun run = runOf(id);
            b.tell((bs, t, left, total) -> {
                try {
                    tellOf(id, t, left, total);
                } catch (RuntimeException e) {
                    LogUtils.getLogger().warn("[nusmp] Escanor's tell of {} failed: {}", id, e.toString());
                }
            });
            out.add(b.run((bs, t) -> {
                if (!(level() instanceof ServerLevel)) return;
                run.run(bs, t);
            }));
        }
        return out;
    }

    // ---------------------------------------------------------------- locking what the wind-up shows
    private static boolean first(int left, int total) { return left >= total - 3; }

    private Vec3 faceOf(LivingEntity target) {
        return SdsaStrikes.unitFlat(target.position().subtract(position()), SdsaStrikes.unitFlat(getViewVector(1f), new Vec3(0, 0, 1)));
    }

    private void lockAim(String id, LivingEntity target) {
        aim = faceOf(target);
        lockedFor = id;
    }

    private Vec3 takeAim(String id, LivingEntity target) {
        Vec3 a = lockedFor.equals(id) ? aim : faceOf(target);
        lockedFor = "";
        return a;
    }

    private void lockMarks(String id, List<Vec3> list) {
        marks = list;
        lockedFor = id;
    }

    private List<Vec3> takeMarks(String id, LivingEntity target) {
        List<Vec3> m = lockedFor.equals(id) && !marks.isEmpty() ? marks : List.of(ZogratisBoss.ground(level(), target.position()));
        marks = List.of();
        lockedFor = "";
        return m;
    }

    private double hazeRadius() { return HAZE_RADIUS + (gatesOpen() ? 0.6 : 0.0); }

    // ---------------------------------------------------------------- the telegraphs
    private void tellOf(String id, LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        boolean f = first(left, total);
        Vec3 chest = position().add(0, getBbHeight() * 0.6, 0);
        switch (id) {
            case EscanorTable.SWING -> {
                if (f) lockAim(id, target);
                SdsaStrikes.tellCone(sl, position(), aim, SWING_REACH, SWING_HALF, GOLD);
                Tells.gather(sl, chest.add(aim.scale(1.0)), 0.5, PALE, 3);
            }
            case EscanorTable.SMASH -> {
                if (f) lockAim(id, target);
                Vec3 c = position().add(aim.scale(SMASH_AHEAD));
                Tells.ring(sl, c, SMASH_RADIUS, WARN, 1.4f, 40);
                Tells.ring(sl, c, Math.max(0.6, SMASH_RADIUS * left / Math.max(1, total)), PALE, 1.1f, 24);
                if (left % 6 == 0) Tells.ring(sl, c, SMASH_WAVE, GOLD, 1.0f, 56);
                Tells.column(sl, c, 4, PALE);
            }
            case EscanorTable.FLARE -> {
                Tells.ring(sl, position(), FLARE_RADIUS * (total - left) / Math.max(1, total), GOLD, 1.3f, 48);
                if (left % 6 == 0) Tells.ring(sl, position(), FLARE_RADIUS, WARN, 1.1f, 56);
                Tells.gather(sl, chest, 1.2, PALE, 5);
            }
            case EscanorTable.WAVE -> {
                if (f) lockAim(id, target);
                for (double a : waveAngles()) Tells.line(sl, chest, chest.add(SdsaStrikes.rotateY(aim, a).scale(16)), GOLD);
                Tells.gather(sl, chest, 0.6, PALE, 3);
            }
            case EscanorTable.CHARGE -> {
                if (f) {
                    lockAim(id, target);
                    lockedLength = Math.max(8.0, Math.min(24.0, distanceTo(target) + 3.0));
                }
                SdsaStrikes.tellStrip(sl, position(), aim, lockedLength, CHARGE_HALF_WIDTH, GOLD);
                Tells.ring(sl, position().add(aim.scale(lockedLength)), CHARGE_RING, WARN, 1.0f, 36);
                Tells.gather(sl, chest, 0.7, PALE, 4);
            }
            case EscanorTable.HAZE -> {
                if (f) lockMarks(id, SdsaStrikes.spread(this, target, Math.min(4, 1 + tuning().castSlots()), hazeRadius()));
                for (Vec3 m : marks) {
                    Tells.ring(sl, m, hazeRadius(), GOLD, 1.3f, (int) Math.min(64, 12 + hazeRadius() * 5));
                    Tells.ring(sl, m, Math.max(0.6, hazeRadius() * left / Math.max(1, total)), PALE, 1.0f, 24);
                }
            }
            case EscanorTable.PILLARS -> {
                if (f) lockMarks(id, SdsaStrikes.spread(this, target, 2 + tuning().castSlots(), PILLAR_RADIUS));
                for (Vec3 m : marks) {
                    Tells.ring(sl, m, PILLAR_RADIUS, WARN, 1.3f, 32);
                    Tells.ring(sl, m, Math.max(0.5, PILLAR_RADIUS * left / Math.max(1, total)), PALE, 1.0f, 20);
                    Tells.column(sl, m, 6, PALE);
                }
            }
            case EscanorTable.SPIN -> {
                Tells.ring(sl, position(), SPIN_RADIUS * (total - left) / Math.max(1, total), GOLD, 1.3f, 48);
                if (left % 6 == 0) Tells.ring(sl, position(), SPIN_RADIUS, WARN, 1.2f, 64);
                Tells.gather(sl, chest, 1.0, PALE, 4);
            }
            case EscanorTable.CRUEL_SUN -> tellCruelSun(sl, target, f, left, total);
            case EscanorTable.ZENITH -> {
                if (f) {
                    lockMarks(id, List.of(ZogratisBoss.ground(sl, target.position())));
                    announce("Zenith Cleave! Escanor will come down on the marked ring. Stay out of the middle, then jump the shock that runs out from it.");
                    sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3f, 0.5f);
                }
                for (Vec3 m : marks) {
                    Tells.ring(sl, m, CruelSunMath.ZENITH_CENTRE, WARN, 1.5f, 40);
                    Tells.ring(sl, m, Math.max(0.8, CruelSunMath.ZENITH_CENTRE * left / Math.max(1, total)), PALE, 1.1f, 24);
                    if (left % 9 == 0) Tells.ring(sl, m, CruelSunMath.ZENITH_REACH, GOLD, 1.0f, 80);
                    Tells.column(sl, m, 7, PALE);
                }
                Tells.gather(sl, chest, 2.0, PALE, 8);
            }
            default -> { }
        }
    }

    private void tellCruelSun(ServerLevel sl, LivingEntity target, boolean f, int left, int total) {
        if (f) {
            lockMarks(EscanorTable.CRUEL_SUN, List.of(ZogratisBoss.ground(sl, target.position())));
            sunRadius = CruelSunMath.radius(sunNow, gatesOpen());
            sunStartHealth = getHealth();
            sunWinding = true;
            sunBreakers.clear();
            announce("Cruel Sun! Escanor raises a small sun. Walk out of the ring, stand behind a block or raise a shield (it cuts the blow), or hurt him for "
                    + Math.round(CruelSunMath.INTERRUPT * 100) + "% of his health before it leaves his hand to break it.");
            sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3f, 0.5f);
            Vec3 hand = position().add(0, getBbHeight() * 1.1, 0);
            SdsaStrikes.fx(sl, VfxShape.LIGHT_FLARE, hand, hand.add(0, 1, 0), PALE, total, 3.5f, false);
        }
        if (marks.isEmpty()) return;
        Vec3 m = marks.get(0);
        Tells.ring(sl, m, sunRadius, WARN, 1.5f, (int) Math.min(96, 20 + sunRadius * 6));
        Tells.ring(sl, m, Math.max(0.8, sunRadius * left / Math.max(1, total)), PALE, 1.1f, 40);
        Tells.column(sl, m, 6, GOLD);
        Tells.gather(sl, position().add(0, getBbHeight() * 1.1, 0), 0.9, PALE, 8);
        if (left % 12 == 0) {
            double done = Math.max(0.0, sunStartHealth - getHealth()) / getMaxHealth() / CruelSunMath.INTERRUPT;
            SdsaStrikes.bar(sl, position(), 40, Component.literal("Cruel Sun: broken " + Math.min(100, Math.round(done * 100)) + "%").withStyle(ChatFormatting.GOLD));
        }
    }

    private double[] waveAngles() {
        int n = Math.min(3, tuning().castSlots());
        return n <= 1 ? new double[]{0.0} : n == 2 ? new double[]{-0.14, 0.14} : new double[]{-0.26, 0.0, 0.26};
    }

    // ---------------------------------------------------------------- what each move does
    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case EscanorTable.SWING -> (b, t) -> {
                EscanorMoves.cone(this, (ServerLevel) level(), takeAim(id, t), SWING_REACH, SWING_HALF, EscanorTable.SWING_DAMAGE, 0.7);
                quip("S-sorry...", "Out of my way!", "Kneel.");
            };
            case EscanorTable.SMASH -> (b, t) -> {
                EscanorMoves.smash(this, (ServerLevel) level(), takeAim(id, t), SMASH_AHEAD, SMASH_RADIUS, EscanorTable.SMASH_DAMAGE, SMASH_WAVE, 0.67f * EscanorTable.SMASH_DAMAGE);
                quip("I... I didn't want to...", "Feel the weight of Pride!", "Crushed.");
            };
            case EscanorTable.FLARE -> (b, t) -> EscanorMoves.flare(this, (ServerLevel) level(), FLARE_RADIUS, EscanorTable.FLARE_DAMAGE);
            case EscanorTable.WAVE -> (b, t) -> {
                ServerLevel sl = (ServerLevel) level();
                Vec3 a = takeAim(id, t), chest = position().add(0, getBbHeight() * 0.4, 0);
                for (double ang : waveAngles()) EscanorMoves.flameWave(this, sl, chest, SdsaStrikes.rotateY(a, ang), EscanorTable.WAVE_DAMAGE);
            };
            case EscanorTable.CHARGE -> (b, t) -> {
                Vec3 a = takeAim(id, t);
                EscanorMoves.charge(this, (ServerLevel) level(), a, lockedLength <= 0 ? 12 : lockedLength, CHARGE_HALF_WIDTH, EscanorTable.CHARGE_DAMAGE, CHARGE_RING, EscanorTable.CHARGE_RING_DAMAGE);
                quip("Please... get out of the way!", "Pride does not stop!", "Nothing stands before the sun.");
            };
            case EscanorTable.HAZE -> (b, t) -> EscanorMoves.haze(this, (ServerLevel) level(), takeMarks(id, t), hazeRadius(), HAZE_TICKS, EscanorTable.HAZE_DAMAGE);
            case EscanorTable.PILLARS -> (b, t) -> {
                EscanorMoves.pillars(this, (ServerLevel) level(), takeMarks(id, t), PILLAR_RADIUS, EscanorTable.PILLAR_DAMAGE);
                quip("The light...", "Burn in the sun's light!", "Behold the sun!");
            };
            case EscanorTable.SPIN -> (b, t) -> EscanorMoves.spin(this, (ServerLevel) level(), SPIN_RADIUS, EscanorTable.SPIN_DAMAGE);
            case EscanorTable.CRUEL_SUN -> (b, t) -> runCruelSun((ServerLevel) level(), t);
            case EscanorTable.ZENITH -> (b, t) -> {
                ServerLevel sl = (ServerLevel) level();
                Vec3 m = takeMarks(id, t).get(0);
                SdsaStrikes.fx(sl, VfxShape.SPATIAL_RIFT, position().add(0, 1, 0), position().add(0, 1.5, 0), PALE, 10, 0.8f, true);
                SdsaStrikes.dashTo(this, m);
                EscanorMoves.zenith(this, sl, m, CruelSunMath.ZENITH_CENTRE_DAMAGE, CruelSunMath.ZENITH_WAVE_DAMAGE);
                say("Zenith Cleave!");
            };
            default -> (b, t) -> { };
        };
    }

    /** The release of the Cruel Sun: it flies if the party did not break it, falls apart in his hand if they did. */
    private void runCruelSun(ServerLevel sl, LivingEntity target) {
        Vec3 mark = takeMarks(EscanorTable.CRUEL_SUN, target).get(0);
        boolean broken = CruelSunMath.broken(Math.max(0.0, sunStartHealth - getHealth()) / getMaxHealth());
        sunWinding = false;
        EscanorMoves.cruelSun(this, sl, mark, sunRadius <= 0 ? CruelSunMath.radius(sunNow, gatesOpen()) : sunRadius, CruelSunMath.damage(sunNow), CruelSunMath.scorchTicks(sunNow), broken);
        if (broken) {
            announce("The Cruel Sun is broken in his hand! Escanor is open.");
            for (UUID id : sunBreakers) {
                Entity who = sl.getEntity(id);
                try {
                    if (who instanceof ServerPlayer p) NusmpEvents.fire(p, "sds.cruel_sun_broken");
                } catch (RuntimeException e) {
                    LogUtils.getLogger().warn("[nusmp] the quest hook sds.cruel_sun_broken failed: {}", e.toString());
                }
            }
            quip("N-no...", "My sun!", "Impossible.");
        } else {
            say("Cruel Sun!");
        }
        sunBreakers.clear();
    }
}
