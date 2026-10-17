package com.newuniverse.nusmp.arc.midnight;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.DanteMoves;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.VolleyBudget;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.entity.zogratis.ZogratisVfx;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: Fana is the Hatred of the Third Eye, an elf soul in a Diamond Kingdom orphan's body who burns forests when she is angry and calls the fire spirit
 * Salamander to fight beside her; the Witches' Forest raid of the first saga is her fight. It is a fire fight made of things the party can read, and three
 * mechanics belong only to her, each taken from the story and each with a counter.
 * <ul>
 *   <li><b>Her fireball can be struck back</b> ({@link ReflectRules}): Asta's anti-magic sword sent Salamander's fireball back at it. A swing of a demon sword
 *       or of the Anti-Magic Lord near the flying fireball, a Full Reflection mirror or a live mirror array turns it, and it strikes her and leaves her open.</li>
 *   <li><b>The Phoenix Feathers Robe</b> ({@link PhoenixRobe}): for four seconds a mantle of flame takes 60% off fire and 45% off every other blow and scorches
 *       whoever strikes it from close; water and ice go through whole, and a blow of anti-magic tears it off.</li>
 *   <li><b>The Evil Heart</b> ({@link EvilHeart}), the move of the Gates: the Third Eye drives her to gather everything into a sphere and take the world with
 *       her. The sphere grows for a countdown; the party breaks it by taking a share of her health before it ends (in the story the Eye went out and the
 *       girl under it woke); if they do the Eye shatters and she is open, if not it detonates over a wide circle that falls off with distance and is cut by
 *       walls and a raised shield.</li>
 * </ul>
 * Salamander itself is an effect of the fight (flames drawn at her side), not a model or an entity. She changes no block and heals nothing. Her fire is
 * the mod's own fire effects; the fire she lights is on the bodies it hits, never on the floor.
 */
public class FanaBoss extends MidnightBoss {
    private static final int FIRE = 0xFF7A2A, EMBER = 0xFFB347, HOT = 0xFFE9B0, RED = 0xFF4A1A;

    public static final ArcCharacter CHARACTER = ArcCharacter.boss("fana", "Fana")
            .title("the Hatred of the Third Eye").source("Black Clover").saga(ArcSaga.ELF_REINCARNATION).tier(2)
            .colors(0xE8421A, 0xFFB347).magic("flame").stats(1500, 12, 0.30, 6)
            .floors(Polarity.ABYSSAL, 35, 60)
            .themes("flame", "infernal_foundry", "verdant", "abyss")
            .after("mars", "vetto")
            .charter(0.25, "flame_manipulation", "flame_domination", "flame_breath", "fire_storm", "flame_wall", "summon_greater_elemental",
                    "flame_attack_resistance", "heat_wave")
            .lines("Humans... every one of you smells of the same lie. Salamander, burn them down to the roots.",
                    "Everything, all of it, I will make it disappear! Salamander, stay with me to the very end!",
                    "The fire went quiet. Who is crying here... Mars? Is that you?")
            .build();

    public static final DeferredHolder<EntityType<?>, EntityType<FanaBoss>> TYPE = ArcEntities.boss("fana", FanaBoss::new, 0.55f, 1.75f,
            new ArcLook("fana", 1.75f, 1.01f, 1.79f, 0.5f, 1.22f, 1.27f, true), () -> attributesOf(CHARACTER, 3.0));

    static {
        ThirdEyeBond.install();
    }

    private EvilHeart heart = new EvilHeart(MidnightConfig.evilHeartTicks());
    private final BreakWatch heartWatch = new BreakWatch();
    private long robeUntil;
    private boolean fireballHinted;

    public FanaBoss(EntityType<FanaBoss> type, Level level) { super(type, level); }

    @Override public ArcCharacter character() { return CHARACTER; }

    // ================================================================ looks and numbers
    @Override protected String word() { return "Salamander"; }

    @Override protected String gatesWord() { return "Evil Heart"; }

    @Override protected String gatesAnnouncement() { return "Fana's Third Eye opens: the Evil Heart wakes and Salamander flares with it!"; }

    @Override protected ChatFormatting chatColor() { return ChatFormatting.GOLD; }

    /** A caster who likes the middle distance, and a clawing girl when someone gets close. */
    @Override protected double preferredRange(double suggested) { return Math.max(3.0, Math.min(11.0, suggested)); }

    @Override protected String[] tensuraKeywords() { return new String[]{"fire", "flame", "heat"}; }

    @Override protected int tensuraKitSize() { return 3; }

    @Override protected boolean frozen() { return heart.charging(); }

    private boolean robeActive() { return tickCount < robeUntil; }

    // ================================================================ words
    @Override
    protected String powerLine(int old, int now) {
        if (now >= 80 && old < 80) return "You cannot put out what you did not light!";
        if (now >= 60 && old < 60) return "Burn! Salamander, burn all of it!";
        if (now >= 40 && old < 40) return "Salamander is awake now. Be afraid.";
        return "I draw " + now + "% of Salamander's fire.";
    }

    @Override
    protected void fightBegins() {
        heart.reset();
        heartWatch.disarm();
        robeUntil = 0;
        fireballHinted = false;
        announce("Fana fights with Salamander's fire. Her fireball can be struck back by an anti-magic blade or a mirror; water and ice go through her robe of feathers.");
    }

    @Override
    protected void fightEnds(boolean won) {
        heart.reset();
        heartWatch.disarm();
        robeUntil = 0;
    }

    @Override
    protected void onGatesOpened() {
        if (level() instanceof ServerLevel sl) {
            Vec3 eye = position().add(0, getBbHeight() + 0.5, 0);
            fx(VfxShape.EYE_FX3, eye, eye.add(0, 1, 0), RED, 40, 1.8f);
            MidnightFx.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 2.5f, 0.4f);
        }
        announce("The Evil Heart stirs under her Third Eye. At full power it will gather everything into one sphere: break the Eye before it ends.");
    }

    // ================================================================ the robe
    @Override
    protected float guard(DamageSource source, float amount) {
        if (!robeActive()) return amount;
        if (DanteMoves.antiMagicHit(source)) {                         // a blade of anti-magic tears the feathers off
            robeUntil = 0;
            announce("Anti-magic tears Fana's Phoenix Feathers Robe off her!");
            fx(VfxShape.FIRE_BURST, position().add(0, 1, 0), position().add(0, 1, 0), EMBER, 16, 1.6f);
            return amount;
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity le && le.distanceToSqr(this) <= PhoenixRobe.THORNS_RADIUS * PhoenixRobe.THORNS_RADIUS) le.igniteForSeconds(PhoenixRobe.THORNS_SECONDS);
        String id = source.typeHolder().unwrapKey().map(k -> k.location().toString()).orElse("");
        return (float) (amount * PhoenixRobe.factor(PhoenixRobe.classify(id)));
    }

    // ================================================================ every tick
    @Override
    protected void tickMidnight() {
        if (!(level() instanceof ServerLevel sl)) return;
        if (heart.charging()) tickHeart(sl);
        if (tickCount % 6 == 0 && isAlive()) Tells.gather(sl, position().add(0, 0.9, 0), 0.6, FIRE, robeActive() ? 4 : 1);              // embers round her
        if (robeUntil != 0 && !robeActive()) {
            robeUntil = 0;
            announce("The Phoenix Feathers Robe burns out.");
        }
    }

    // ================================================================ moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (MidnightSpec s : FanaTable.specs()) out.add(build(s, tellOf(s.id()), runOf(s.id())));
        return out;
    }

    private Move.MoveTell tellOf(String id) {
        return switch (id) {
            case FanaTable.FIREBALL -> (b, t, left, total) -> tellFireball(t);
            case FanaTable.BREATH -> (b, t, left, total) -> tellCone(t, left, FanaMath.BREATH_REACH, FanaMath.BREATH_HALF_ANGLE);
            case FanaTable.CLAW -> (b, t, left, total) -> tellCone(t, left, FanaMath.CLAW_REACH, FanaMath.CLAW_HALF_ANGLE);
            case FanaTable.CHARGE -> (b, t, left, total) -> tellCharge(t, left, total);
            case FanaTable.SPIRAL -> (b, t, left, total) -> tellSpiral(t, left);
            case FanaTable.COLUMNS -> (b, t, left, total) -> tellColumns(t, left, total);
            case FanaTable.ROAR -> (b, t, left, total) -> tellRing(FanaMath.ROAR_RADIUS, left, total);
            case FanaTable.FIRE_MAGIC -> (b, t, left, total) -> tellGather();
            case FanaTable.ERUPTION -> (b, t, left, total) -> tellEruption(left, total);
            case FanaTable.ROBE -> (b, t, left, total) -> tellRobe();
            case FanaTable.ARMS -> (b, t, left, total) -> tellArms(t, left, total);
            case FanaTable.EMBERS -> (b, t, left, total) -> tellEmbers(left, total);
            case FanaTable.EVIL_HEART -> (b, t, left, total) -> tellHeart(left, total);
            default -> null;
        };
    }

    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case FanaTable.FIREBALL -> (b, t) -> runFireballs(t, FanaMath.fireballCount(power()));
            case FanaTable.BREATH -> (b, t) -> runBreath(t);
            case FanaTable.CLAW -> (b, t) -> runClaw(t);
            case FanaTable.CHARGE -> (b, t) -> runCharge(t);
            case FanaTable.SPIRAL -> (b, t) -> runSpiral(t);
            case FanaTable.COLUMNS -> (b, t) -> runColumns(t);
            case FanaTable.ROAR -> (b, t) -> runRoar();
            case FanaTable.FIRE_MAGIC -> (b, t) -> {
                if (!castTensura(t)) runFireballs(t, 1);
            };
            case FanaTable.ERUPTION -> (b, t) -> runEruption();
            case FanaTable.ROBE -> (b, t) -> runRobe();
            case FanaTable.ARMS -> (b, t) -> runArms(t);
            case FanaTable.EMBERS -> (b, t) -> runEmbers(t);
            case FanaTable.EVIL_HEART -> (b, t) -> runHeart();
            default -> (b, t) -> { };
        };
    }

    /** The point on her body the breath and the spiral leave from: Salamander's mouth, a little above and ahead of her. */
    private Vec3 mouth(Vec3 dir) { return position().add(0, 1.3, 0).add(dir.multiply(1, 0, 1).normalize().scale(0.8)); }

    private Vec3 flatAimAt(LivingEntity t) { return MidnightFx.flatDir(position(), t.position(), getViewVector(1f)); }

    // ---------------------------------------------------------------- Maximum Exploding Fireball (can be struck back)
    private void tellFireball(LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = hand();
        Vec3 d = t.getBoundingBox().getCenter().subtract(from);
        if (d.lengthSqr() < 1e-4) return;
        Vec3 dir = d.normalize();
        int n = FanaMath.fireballCount(power());
        for (double a : MidnightMath.fan(n, FanaMath.FIREBALL_SPREAD * Math.max(0, n - 1))) {
            Tells.line(sl, from, MidnightFx.clipWall(sl, this, from, from.add(MidnightFx.rotateY(dir, a).scale(30))), FIRE);
        }
        Tells.gather(sl, from, 0.7, EMBER, 6);
    }

    private void runFireballs(LivingEntity t, int n) {
        if (!(level() instanceof ServerLevel sl)) return;
        final Vec3 from = hand();
        final Vec3 dir = t.getBoundingBox().getCenter().subtract(from).normalize();
        double[] fan = MidnightMath.fan(n, FanaMath.FIREBALL_SPREAD * Math.max(0, n - 1));
        for (int i = 0; i < n; i++) {
            final Vec3 d = MidnightFx.rotateY(dir, fan[i]);
            Runnable fire = MidnightFx.safe("fireball", () -> {
                Vec3 end = MidnightFx.clipWall(sl, this, from, from.add(d.scale(FanaMath.FIREBALL_SPEED * FanaMath.FIREBALL_LIFE)));
                fx(VfxShape.FIRE_SPEAR, from, end, FIRE, FanaMath.FIREBALL_LIFE, 1.4f);
                MidnightBolt.launch(this, from, d.scale(FanaMath.FIREBALL_SPEED), FanaMath.FIREBALL_LIFE, FanaMath.FIREBALL_RADIUS, FIRE, true, new VolleyBudget(1),
                        (bolt, v) -> { }, (bolt, at, struck) -> { if (!bolt.reversed()) explode(at); }, (bolt, by, kind) -> turnedBack(kind));
                MidnightFx.sound(sl, from, SoundEvents.FIRECHARGE_USE, 1.6f, 0.7f);
            });
            if (i == 0) fire.run();
            else later(i * 6, fire);
        }
        if (!fireballHinted) {
            fireballHinted = true;
            announce("Salamander's fireball is slow: strike it with an anti-magic blade, or catch it in a mirror, and it goes back to her.");
        }
        quip("Burn!", "Salamander, take them!", "You cannot hide from fire!");
    }

    private void explode(Vec3 at) {
        fx(VfxShape.FLAME_EXPLOSION, at, at, FIRE, 20, 1.5f);
        MidnightFx.sound(level(), at, SoundEvents.DRAGON_FIREBALL_EXPLODE, 1.6f, 0.8f);
        for (LivingEntity e : MidnightFx.inRing(this, at, FanaMath.FIREBALL_BLAST)) {
            if (MidnightFx.hit(this, e, FanaTable.FIREBALL_HIT, true)) {
                e.igniteForSeconds(3);
                MidnightFx.shove(e, at, 0.9, 0.2);
            }
        }
    }

    /** Her own fireball came back on her: Salamander is scorched, and she is open for a moment. */
    private void turnedBack(ReflectRules.Kind kind) {
        fx(VfxShape.FIRE_BURST, position().add(0, 1, 0), position().add(0, 1, 0), HOT, 18, 1.8f);
        openUp(ReflectRules.openTicks(kind), "Salamander is scorched by its own fire! Fana is open!");
    }

    // ---------------------------------------------------------------- Salamander's Breath and Claw
    private void tellCone(LivingEntity t, int left, double reach, double half) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (left > 8 || aim.lengthSqr() < 1e-4) aim = flatAimAt(t);
        MidnightFx.coneTell(sl, position().add(0, 0.2, 0), aim, reach, half, left <= 8 ? MidnightFx.WARN : FIRE);
        Tells.gather(sl, position().add(0, 1.2, 0), 0.5, EMBER, 4);
    }

    private Vec3 lockedAim(LivingEntity t) { return aim.lengthSqr() > 1e-4 ? aim.multiply(1, 0, 1).normalize() : flatAimAt(t); }

    private void runBreath(LivingEntity t) {
        Vec3 dir = lockedAim(t);
        Vec3 from = mouth(dir);
        fx(VfxShape.FLAME_TRAIL, from, from.add(dir.scale(FanaMath.BREATH_REACH)), FIRE, 14, 1.6f);
        fx(VfxShape.FIRE_BURST, from.add(dir.scale(FanaMath.BREATH_REACH * 0.8)), from.add(dir.scale(FanaMath.BREATH_REACH * 0.8)), EMBER, 14, 1.2f);
        MidnightFx.sound(level(), from, SoundEvents.BLAZE_SHOOT, 1.8f, 0.6f);
        for (LivingEntity e : MidnightFx.inCone(this, position(), dir, FanaMath.BREATH_REACH, FanaMath.BREATH_HALF_ANGLE)) {
            if (MidnightFx.hit(this, e, FanaTable.BREATH_HIT, true)) {
                e.igniteForSeconds(3);
                MidnightFx.shove(e, position(), 0.5, 0);
            }
        }
        quip("Salamander, breathe!", "Burn to the roots!");
    }

    private void runClaw(LivingEntity t) {
        Vec3 dir = lockedAim(t);
        Vec3 mid = position().add(0, 1.0, 0).add(dir.scale(FanaMath.CLAW_REACH * 0.55));
        fx(VfxShape.FIRE_BURST, mid, mid, FIRE, 12, 1.3f);
        fx(VfxShape.FLAME_TRAIL, mid.add(MidnightFx.rotateY(dir, 1.1).scale(2.2)), mid.add(MidnightFx.rotateY(dir, -1.1).scale(2.2)), EMBER, 8, 1.3f);
        MidnightFx.sound(level(), position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.6f, 0.6f);
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        for (LivingEntity e : MidnightFx.inCone(this, position(), dir, FanaMath.CLAW_REACH, FanaMath.CLAW_HALF_ANGLE)) {
            if (MidnightFx.hit(this, e, FanaTable.CLAW_HIT, false)) {
                e.igniteForSeconds(2);
                MidnightFx.shove(e, position(), 1.1, 0.15);
            }
        }
    }

    // ---------------------------------------------------------------- Salamander's Charge
    private void tellCharge(LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            Vec3 d = t.position().subtract(position()).multiply(1, 0, 1);
            double len = Math.min(FanaMath.CHARGE_MAX, Math.max(3.0, d.length()));
            Vec3 want = position().add(MidnightFx.flatDir(position(), t.position(), getViewVector(1f)).scale(len));
            marks = List.of(ground(want));
        }
        if (marks.isEmpty()) return;
        Vec3 end = marks.get(0);
        Tells.line(sl, position().add(0, 0.5, 0), end.add(0, 0.5, 0), FIRE);
        Tells.line(sl, position().add(0.9, 0.5, 0), end.add(0.9, 0.5, 0), FIRE);
        Tells.line(sl, position().add(-0.9, 0.5, 0), end.add(-0.9, 0.5, 0), FIRE);
        Tells.ring(sl, end, 2.2, MidnightFx.WARN, 1.3f, 32);
    }

    private void runCharge(LivingEntity t) {
        Vec3 from = position();
        Vec3 end = marks.isEmpty() ? ground(t.position()) : marks.get(0);
        marks = List.of();
        fx(VfxShape.FIRE_LION, from, end, FIRE, 10, 1.5f);
        for (LivingEntity e : MidnightFx.inLane(this, from, end, FanaMath.CHARGE_HALF_WIDTH)) {
            if (MidnightFx.hit(this, e, FanaTable.CHARGE_HIT, true)) {
                e.igniteForSeconds(2);
                MidnightFx.shoveAside(e, from, end, 1.0);
            }
        }
        if (!blink(end)) blink(from.add(end.subtract(from).scale(0.8)));
        MidnightFx.sound(level(), end, SoundEvents.BLAZE_SHOOT, 2f, 0.5f);
        quip("Salamander, ride!");
    }

    // ---------------------------------------------------------------- Profound Spiral Flames
    private void tellSpiral(LivingEntity t, int left) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = mouth(flatAimAt(t));
        if (left > FanaMath.SPIRAL_LOCK || aim.lengthSqr() < 1e-4) aim = t.getBoundingBox().getCenter().subtract(from);
        if (aim.lengthSqr() < 1e-4) return;
        Vec3 end = MidnightFx.clipWall(sl, this, from, from.add(aim.normalize().scale(FanaMath.SPIRAL_LENGTH)));
        int color = left <= FanaMath.SPIRAL_LOCK ? MidnightFx.WARN : FIRE;
        Tells.line(sl, from, end, color);
        Tells.line(sl, from.add(0, 0.7, 0), end.add(0, 0.7, 0), color);
        Tells.gather(sl, from, 0.5, EMBER, 4);
    }

    private void runSpiral(LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = mouth(flatAimAt(t));
        Vec3 dir = aim.lengthSqr() > 1e-4 ? aim.normalize() : t.getBoundingBox().getCenter().subtract(from).normalize();
        Vec3 end = MidnightFx.clipWall(sl, this, from, from.add(dir.scale(FanaMath.SPIRAL_LENGTH)));
        fx(VfxShape.FIRE_SPIRAL, from, end, FIRE, 14, 1.3f);
        MidnightFx.sound(sl, from, SoundEvents.BLAZE_SHOOT, 1.8f, 0.8f);
        for (LivingEntity e : MidnightFx.inLane(this, from, end, FanaMath.SPIRAL_HALF_WIDTH)) {
            if (MidnightFx.hit(this, e, FanaTable.SPIRAL_HIT, true)) {
                e.igniteForSeconds(3);
                MidnightFx.shove(e, from, 0.5, 0.1);
            }
        }
    }

    // ---------------------------------------------------------------- Ignis Columna
    private void tellColumns(LivingEntity t, int left, int total) {
        if (first(left, total)) marks = spotsAround(t, FanaMath.pillarCount(power()), 3.5);
        drawMarks(marks, FanaMath.COLUMN_RADIUS, left, total, FIRE);
    }

    private void runColumns(LivingEntity t) {
        List<Vec3> spots = marks.isEmpty() ? List.of(ground(t.position())) : List.copyOf(marks);
        marks = List.of();
        for (Vec3 c : spots) {
            fx(VfxShape.FIRE_PILLAR, c, c, FIRE, 18, (float) FanaMath.COLUMN_RADIUS);
            for (LivingEntity e : MidnightFx.inRing(this, c, FanaMath.COLUMN_RADIUS)) {
                if (MidnightFx.hit(this, e, FanaTable.COLUMN_HIT, true)) {
                    e.igniteForSeconds(3);
                    MidnightFx.shove(e, c, 0.3, 0.5);
                }
            }
        }
        MidnightFx.sound(level(), position(), SoundEvents.BLAZE_SHOOT, 1.6f, 0.5f);
    }

    // ---------------------------------------------------------------- Salamander's Roar
    private void tellRing(double radius, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Tells.ring(sl, position(), radius, FIRE, 1.3f, 56);
        Tells.ring(sl, position(), Math.max(1.0, radius * (total - left) / Math.max(1, total)), EMBER, 1.1f, 32);
        Tells.gather(sl, position().add(0, 1, 0), 1.0, EMBER, 6);
    }

    private void runRoar() {
        if (!(level() instanceof ServerLevel sl)) return;
        fx(VfxShape.FIRE_BURST, position().add(0, 1, 0), position().add(0, 1, 0), FIRE, 18, 2.4f);
        ZogratisVfx.gravityImpact(sl, position(), FanaMath.ROAR_RADIUS, FIRE);
        MidnightFx.sound(sl, position(), SoundEvents.BLAZE_SHOOT, 2.5f, 0.4f);
        for (LivingEntity e : MidnightFx.inRing(this, position(), FanaMath.ROAR_RADIUS)) {
            if (MidnightFx.hit(this, e, FanaTable.ROAR_HIT, true)) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                MidnightFx.shove(e, position(), 1.0, 0.1);
            }
        }
    }

    private void tellGather() {
        if (level() instanceof ServerLevel sl) {
            Tells.gather(sl, hand(), 0.8, EMBER, 6);
            Tells.ring(sl, position(), 2.2, FIRE);
        }
    }

    // ---------------------------------------------------------------- Crimson Eruption
    private void tellEruption(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        for (int k = 0; k < FanaMath.eruptionWaves(power()); k++) Tells.ring(sl, position(), FanaMath.eruptionReach(k), k == 0 ? FIRE : EMBER, 1.2f, 56);
        Tells.ring(sl, position(), Math.max(1.0, FanaMath.eruptionReach(0) * (total - left) / Math.max(1, total)), HOT, 1.1f, 32);
        Tells.gather(sl, position().add(0, 0.4, 0), 1.2, EMBER, 6);
    }

    private void runEruption() {
        if (!(level() instanceof ServerLevel sl)) return;
        final Vec3 centre = position();
        for (int k = 0; k < FanaMath.eruptionWaves(power()); k++) {
            final double reach = FanaMath.eruptionReach(k);
            later(k * FanaMath.ERUPTION_GAP, MidnightFx.safe("eruption", () -> eruptionWave(sl, centre, reach)));
        }
        MidnightFx.sound(sl, centre, SoundEvents.DRAGON_FIREBALL_EXPLODE, 2f, 0.5f);
    }

    /** One ring of fire runs out from the centre at a block a tick and strikes each foe it passes once, unless they jumped it. */
    private void eruptionWave(ServerLevel sl, Vec3 centre, double reach) {
        fx(VfxShape.FIRE_WILD, centre, centre.add(0, 1, 0), FIRE, FanaMath.eruptionTicks(reach), (float) reach);
        final Set<Integer> hit = new HashSet<>();
        SpellRuntime.zone(sl, FanaMath.eruptionTicks(reach), 1, MidnightFx.safe("eruption wave", age -> {
            if (!isAlive() || isRemoved()) return;
            double r = age * FanaMath.ERUPTION_SPEED;
            if (age % 2 == 0) Tells.ring(sl, centre, Math.min(r, reach), FIRE, 1.4f, (int) Math.min(80, 12 + Math.min(r, reach) * 6));
            for (LivingEntity e : foesAround(centre, reach + 3)) {
                if (hit.contains(e.getId())) continue;
                double dist = Math.hypot(e.getX() - centre.x, e.getZ() - centre.z);
                if (dist > reach + e.getBbWidth() / 2 || Math.abs(dist - r) > FanaMath.ERUPTION_BAND + e.getBbWidth() / 2) continue;
                if (MidnightFx.floorHeight(sl, e) > FanaMath.ERUPTION_JUMP) continue;                          // jumped it
                hit.add(e.getId());
                if (MidnightFx.hit(this, e, FanaTable.ERUPTION_HIT, true)) {
                    e.igniteForSeconds(2);
                    MidnightFx.shove(e, centre, 0.9, 0.2);
                }
            }
        }));
    }

    // ---------------------------------------------------------------- Phoenix Feathers Robe
    private void tellRobe() {
        if (level() instanceof ServerLevel sl) {
            Tells.gather(sl, position().add(0, 1.0, 0), 1.0, EMBER, 8);
            Tells.ring(sl, position(), 2.0, EMBER);
        }
    }

    private void runRobe() {
        robeUntil = tickCount + PhoenixRobe.DURATION;
        if (level() instanceof ServerLevel sl) {
            ZogratisVfx.gravityAura(sl, this, 1.6, PhoenixRobe.DURATION, EMBER);
            MidnightFx.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 0.8f);
        }
        announce("Fana wraps herself in the Phoenix Feathers Robe: fire and blows barely touch her, whoever strikes it from close is scorched. Water, ice or anti-magic go through.");
    }

    // ---------------------------------------------------------------- Calidus Brachium: Flame Burial
    private void tellArms(LivingEntity t, int left, int total) {
        if (first(left, total)) marks = List.of(ground(t.position()));
        drawMarks(marks, FanaMath.ARMS_RADIUS, left, total, FIRE);
    }

    private void runArms(LivingEntity t) {
        Vec3 c = marks.isEmpty() ? ground(t.position()) : marks.get(0);
        marks = List.of();
        fx(VfxShape.FIRE_PILLAR, c, c, FIRE, 24, (float) FanaMath.ARMS_RADIUS);
        fx(VfxShape.FIRE_BURST, c, c, EMBER, 16, 1.6f);
        MidnightFx.sound(level(), c, SoundEvents.DRAGON_FIREBALL_EXPLODE, 1.8f, 0.6f);
        boolean gate = MidnightFx.controlAllowed(this);
        for (LivingEntity e : MidnightFx.inRing(this, c, FanaMath.ARMS_RADIUS)) {
            if (MidnightFx.hit(this, e, FanaTable.ARMS_HIT, true)) {
                e.igniteForSeconds(3);
                if (gate) MidnightFx.root(e, 25);
            }
        }
    }

    // ---------------------------------------------------------------- Rain of Embers
    private void tellEmbers(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Tells.gather(sl, position().add(0, 4, 0), 1.8, EMBER, 8);
        Tells.ring(sl, position(), 3 + (total - left) * 0.25, FIRE, 1.2f, 36);
    }

    private void runEmbers(LivingEntity t) {
        List<Vec3> spots = spotsAround(t, FanaMath.emberCount(power()), 5.0);
        for (int i = 0; i < spots.size(); i++) {
            final Vec3 spot = spots.get(i);
            later(i * FanaMath.EMBER_GAP, MidnightFx.safe("ember", () -> markedStrike(List.of(spot), FanaMath.EMBER_RADIUS, FanaMath.EMBER_WARN, FIRE, c -> {
                fx(VfxShape.FIRE_BURST, c.add(0, 0.3, 0), c.add(0, 0.3, 0), FIRE, 14, 1.0f);
                for (LivingEntity e : MidnightFx.inRing(this, c, FanaMath.EMBER_RADIUS)) {
                    if (MidnightFx.hit(this, e, FanaTable.EMBER_HIT, true)) e.igniteForSeconds(2);
                }
            })));
        }
    }

    // ---------------------------------------------------------------- Evil Heart (the Gates; can be broken)
    private void tellHeart(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            Vec3 eye = position().add(0, getBbHeight() + 0.5, 0);
            fx(VfxShape.EYE_FX3, eye, eye.add(0, 1, 0), RED, total + 6, 1.8f);
            announce("Fana's Third Eye burns: THE EVIL HEART! She is gathering everything into one sphere. Take " + Math.round(MidnightConfig.evilHeartBreak() * 100)
                    + "% of her health before it ends to shatter the Eye, or run, hide behind walls, raise a shield.");
        }
        Tells.ring(sl, position(), 3 + (total - left) * 0.2, RED, 1.3f, 48);
        Tells.gather(sl, position().add(0, 1.3, 0), 1.2, EMBER, 8);
    }

    private void runHeart() {
        heart = new EvilHeart(MidnightConfig.evilHeartTicks());
        heart.begin();
        heartWatch.arm(getHealth());
        getNavigation().stop();
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, heart.total() + 10, 6, false, false));
        MidnightFx.sound(level(), position(), SoundEvents.WITHER_SPAWN, 2.5f, 0.5f);
        say("Everything, all of it, I will make it disappear!");
    }

    private void tickHeart(ServerLevel sl) {
        boolean broken = heartWatch.broken(getHealth(), getMaxHealth(), MidnightConfig.evilHeartBreak());
        EvilHeart.Phase phase = heart.tick(broken);
        Vec3 c = position().add(0, 1.2, 0);
        if (phase == EvilHeart.Phase.CHARGING) {
            if (tickCount % 4 == 0) {
                Tells.ring(sl, position(), heart.radius(), RED, 1.4f, (int) Math.min(90, 16 + heart.radius() * 5));
                Tells.gather(sl, c, Math.max(0.8, heart.radius() * 0.25), EMBER, 10);
                float p = heartWatch.progress(getHealth(), getMaxHealth(), MidnightConfig.evilHeartBreak());
                MidnightFx.bar(sl, position(), 64, Component.literal("Evil Heart " + MidnightText.seconds(heart.left()) + " s  " + MidnightText.gauge(p, 12) + "  break the Third Eye!")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            }
            if (tickCount % 20 == 0) fx(VfxShape.FIRE_BURST, c, c, FIRE, 24, (float) (1.0 + heart.radius() * 0.25));
        } else if (phase == EvilHeart.Phase.BROKEN) {
            breakHeart(sl, c);
        } else if (phase == EvilHeart.Phase.DETONATE) {
            detonate(sl, c);
        }
    }

    private void breakHeart(ServerLevel sl, Vec3 c) {
        heartWatch.disarm();
        fx(VfxShape.MIRROR_SHATTER, c.add(0, 0.6, 0), c.add(0, 0.6, 0), HOT, 24, 2.4f);
        fx(VfxShape.EYE_FX3, c.add(0, 0.8, 0), c.add(0, 1.8, 0), HOT, 20, 1.2f);
        MidnightFx.sound(sl, position(), SoundEvents.GLASS_BREAK, 2.5f, 0.6f);
        removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        say("My eye... why is it so quiet...");
        openUp(FanaMath.HEART_BROKEN_OPEN_TICKS, "The Third Eye shatters and the sphere dies. Fana is open!");
    }

    private void detonate(ServerLevel sl, Vec3 c) {
        heartWatch.disarm();
        double radius = EvilHeart.MAX_RADIUS;
        fx(VfxShape.FIRE_WILD, position(), position().add(0, 1, 0), FIRE, 30, (float) radius);
        fx(VfxShape.FLAME_EXPLOSION, c, c, FIRE, 24, 3.2f);
        MidnightFx.sound(sl, position(), SoundEvents.DRAGON_FIREBALL_EXPLODE, 3f, 0.4f);
        MidnightFx.sound(sl, position(), SoundEvents.WITHER_SPAWN, 2f, 0.6f);
        removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        for (LivingEntity e : foes(FanaMath.HEART_REACH)) {
            double dist = Math.hypot(e.getX() - getX(), e.getZ() - getZ());
            double fall = EvilHeart.falloff(dist, radius);
            if (fall <= 0) continue;
            boolean walled = MidnightFx.walled(sl, c, e);
            double shelter = EvilHeart.shelter(walled, e.isBlocking(), MidnightFx.antiMagic(e));
            if (MidnightFx.hitRaw(this, e, (float) (FanaTable.DETONATION * fall * shelter), true)) {
                e.igniteForSeconds(4);
                MidnightFx.shove(e, position(), 1.5 * fall, 0.4);
            }
        }
        openUp(FanaMath.HEART_SPENT_OPEN_TICKS, "The sphere is spent. Fana is open!");
    }
}
