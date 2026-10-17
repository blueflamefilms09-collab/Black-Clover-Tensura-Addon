package com.newuniverse.nusmp.arc.early;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: Heath Grice is the Eye of the Midnight Sun's ice mage, the man with the pocket watch who attacked Saussy Village on the Black Bulls' first mission. In
 * the anime he combined his ice with the Mist Magic of his subordinates, turned Magna's fireballs aside with a shield of snowflake crystals (which Asta's
 * sword cut through), lost his temper when things did not go as he had planned, and at the end froze himself and his men with a device in his body. The fight
 * is those four things ({@link HeathTable}).
 * <ul>
 *   <li><b>The Timetable</b> ({@link Timetable}): the Endless Ice Cage comes at the next stroke of his watch, on a fixed rhythm that a party can learn (a bell
 *       tolls in the wind-up, the ticks of the watch speed up). It rains icicles on a disc from every direction, in three waves, and leaves one corridor
 *       open that is shown in the wind-up and faces him ({@link HailPattern}). A party that dodges the whole cage has ruined the plan: he is
 *       <i>flustered</i> (open to a blow, starting nothing) for three and a half seconds.</li>
 *   <li><b>Crystal Ice Shield</b> ({@link FrostShield}): ranged damage (spells, arrows, anything from further than four and a half blocks) is swallowed by a pool
 *       of ice; a blade up close passes through and cracks it; anti-magic shatters it. It is a wall against range, never against a blade.</li>
 *   <li><b>Ice Funeral</b> (the ultimate, from 80%): the device in his body freezes everything round him, and him with it. Leave the ring, or hit him for an
 *       eighth of his health while it winds up and the device fails. He is open for three seconds after it, frozen by his own hand.</li>
 * </ul>
 * He changes no block, spawns nothing, heals nothing; the framework's Gates give him the one refill (the model swaps to the Purified form there).
 */
public class HeathGriceBoss extends EarlyBoss {
    public static final ArcCharacter CHARACTER = EarlyRoster.HEATH_GRICE;
    public static final DeferredHolder<EntityType<?>, EntityType<HeathGriceBoss>> TYPE = ArcEntities.boss(CHARACTER.id(), HeathGriceBoss::new, 0.55f, 2.09f,
            new ArcLook(CHARACTER.id(), 2.09f, 1.31f, 2.33f, 0.89f, 1.77f, 1.2f, true), HeathGriceBoss::createAttributes);

    private static final int RGB = CHARACTER.color(), ACCENT = CHARACTER.accent(), WARN = 0xFF4A4A, WHITE = 0xF4FBFF;
    private static final int ARGB = 0xFF000000 | RGB, ARGB_ACCENT = 0xFF000000 | ACCENT;

    private final FrostShield shield = new FrostShield();
    private final Timetable table = new Timetable(HeathTable.CHIME_TICKS);
    private long cageSeed, lastShieldNote;
    private int cageHits;
    private boolean cageOpen;

    public HeathGriceBoss(EntityType<HeathGriceBoss> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return ZogratisBoss.bossAttributes(CHARACTER.health(), CHARACTER.attack(), CHARACTER.speed(), CHARACTER.armor(), CHARACTER.armor() * 0.5);
    }

    @Override public ArcCharacter character() { return CHARACTER; }

    // ================================================================ tuning
    @Override protected ChatFormatting chatColor() { return ChatFormatting.AQUA; }
    @Override protected float formScale(int form) { return 1.0f; }
    @Override protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7f; }
    /** A caster who keeps his distance: where the cage and the icicles reach and his shield is strongest. */
    @Override protected double preferredRange(double suggested) { return Math.max(6, Math.min(12, suggested)); }
    @Override protected String guardedMove() { return HeathTable.FUNERAL; }
    @Override protected double guardShare() { return HeathTable.FUNERAL_BREAK; }

    private int stage() { return PowerCurve.stage(power()); }
    private double cageRadius() { return HeathTable.CAGE_RADIUS * (gatesOpen() ? 1.2 : 1.0); }
    private double funeralRadius() { return HeathTable.FUNERAL_RADIUS * (gatesOpen() ? 1.2 : 1.0); }
    private Vec3 hand() { return position().add(0, getBbHeight() * 0.7, 0); }

    // ================================================================ lines
    @Override
    protected String powerLine(int old, int now) {
        if (now >= 80 && old < 80) return "Enough! The rest of the day is cancelled!";
        if (now >= 60 && old < 60) return "Eleven seconds lost. Unacceptable.";
        if (now >= 40 && old < 40) return "Behind schedule. I will make up the time.";
        return "Tick. Tock.";
    }

    // ================================================================ the fight's hooks
    @Override
    protected void onFightStart() {
        shield.drop();
        table.reset();
        cageHits = 0;
        cageOpen = false;
        forgetMarks();
        announce("Heath keeps a timetable: the Endless Ice Cage comes at the stroke of his watch, with one corridor left open. Dodge all of it and his plan fails.");
        announce("His Crystal Ice Shield swallows anything thrown from afar. Close in, or cut it with anti-magic.");
    }

    @Override
    protected void onFightEnd(boolean won) {
        shield.drop();
        cageOpen = false;
        super.onFightEnd(won);
    }

    @Override
    protected void onGatesOpened() {
        announce("Heath's wings unfold. The cage and the funeral grow by a fifth, and the watch runs faster.");
        if (level() instanceof ServerLevel sl) VfxSpawn.send(sl, VfxShape.ICE_FX3, position(), position().add(0, 1, 0), ARGB, 40, 3.2f);
    }

    @Override
    protected void onChannelBroken() {
        announce("The device in Heath's chest sputters: the Ice Funeral fails!");
        say("No! The device! That was not in the plan!");
        forgetMarks();
        stagger(80, 1.3f);
        if (level() instanceof ServerLevel sl) {
            VfxSpawn.send(sl, VfxShape.ICE_FX3, position(), position().add(0, 1, 0), ARGB, 28, 2.2f);
            EarlyMoves.sound(sl, position(), SoundEvents.GLASS_BREAK, 2f, 0.6f);
        }
    }

    @Override
    protected void bossTick() {
        super.bossTick();
        if (level() instanceof ServerLevel sl && shield.up(sl.getGameTime()) && tickCount % 8 == 0) Tells.gather(sl, position().add(0, getBbHeight() * 0.5, 0), 0.9, WHITE, 4);
    }

    // ================================================================ the appointment book
    /** The cage is not drawn at random: when the watch strikes and the cage may be cast, it is the next move. */
    @Override
    protected int chooseMove(List<Move> bk, MoveScheduler.Situation s, long t) {
        if (staggered()) return -1;
        try {
            int cage = moveIndex(HeathTable.CAGE);
            if (cage >= 0 && cage < bk.size() && table.due(t) && moveEligible(bk.get(cage), s)) return cage;
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Heath's timetable failed: {}", e.toString());
        }
        return super.chooseMove(bk, s, t);
    }

    // ================================================================ the shield
    /** Ranged damage is swallowed by the shield; a blade passes and cracks it; anti-magic shatters it. (Not asked while he is exposed after the funeral.) */
    @Override
    protected float mitigate(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel sl)) return amount;
        try {
            long now = sl.getGameTime();
            if (!shield.up(now)) return amount;
            Entity attacker = source.getEntity();
            boolean direct = attacker != null && source.getDirectEntity() == attacker;
            FrostShield.Blow kind = FrostShield.classify(direct, antiMagic(source), attacker == null ? 99 : attacker.distanceTo(this));
            FrostShield.Result r = shield.absorb(now, amount, kind);
            if (r.broke()) {
                announce(kind == FrostShield.Blow.ANTI_MAGIC ? "Anti-magic shatters Heath's Crystal Ice Shield!" : "Heath's Crystal Ice Shield breaks!");
                VfxSpawn.send(sl, VfxShape.ICE_FX3, position().add(0, 1, 0), position().add(0, 1, 0), ARGB, 24, 1.8f);
                EarlyMoves.sound(sl, position(), SoundEvents.GLASS_BREAK, 1.8f, 1.1f);
            } else if (r.through() < amount && attacker instanceof LivingEntity who && now - lastShieldNote > 30) {
                lastShieldNote = now;
                EarlyMoves.tell(who, "message.nusmp.arc.early.heath.shielded");
                EarlyMoves.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
            }
            return (float) r.through();
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Heath's shield failed: {}", e.toString());
            return amount;
        }
    }

    // ================================================================ moves
    @Override
    protected List<Move> moves() { return buildAll(HeathTable.rows(), this::runOf, this::tellOf); }

    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case HeathTable.FANG -> this::runFang;
            case HeathTable.VOLLEY -> this::runVolley;
            case HeathTable.RIME -> this::runRime;
            case HeathTable.SHIELD -> this::runShield;
            case HeathTable.SLIDE -> this::runSlide;
            case HeathTable.MIST_STEP -> this::runMistStep;
            case HeathTable.COFFIN -> this::runCoffin;
            case HeathTable.CAGE -> this::runCage;
            case HeathTable.FUNERAL -> this::runFuneral;
            default -> (b, t) -> { };
        };
    }

    private Move.MoveTell tellOf(String id) {
        return switch (id) {
            case HeathTable.FANG -> this::tellFang;
            case HeathTable.VOLLEY -> this::tellVolley;
            case HeathTable.RIME -> (b, t, left, total) -> tellRing(HeathTable.RIME_RADIUS, left, total);
            case HeathTable.SHIELD -> (b, t, left, total) -> {
                if (level() instanceof ServerLevel sl) {
                    Tells.ring(sl, position(), 1.8, ACCENT, 1.2f, 20);
                    Tells.gather(sl, position().add(0, 1, 0), 0.9, WHITE, 6);
                }
            };
            case HeathTable.SLIDE -> this::tellSlide;
            case HeathTable.MIST_STEP -> this::tellMistStep;
            case HeathTable.COFFIN -> this::tellCoffin;
            case HeathTable.CAGE -> this::tellCage;
            case HeathTable.FUNERAL -> this::tellFuneral;
            default -> null;
        };
    }

    private void tellRing(double radius, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Tells.ring(sl, position(), Math.max(1.5, radius * (total - left) / Math.max(1, total)), RGB, 1.3f, 36);
        if (left % 6 == 0) Tells.ring(sl, position(), radius, WARN, 1.1f, 44);
        Tells.gather(sl, position().add(0, 1.0, 0), 0.8, WHITE, 4);
    }

    /** Two parallel dust lines along the aim: the lane the spikes will run down, locked halfway through the wind-up. */
    private void tellLane(LivingEntity t, int left, int total, double length) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = aimMark(t, left, total, 0.5);
        Vec3 dir = EarlyMoves.flat(m.subtract(position()), getViewVector(1f)), o = position().add(0, 0.2, 0);
        Vec3 side = new Vec3(-dir.z, 0, dir.x).scale(1.4), end = o.add(dir.scale(length));
        boolean locked = (total - left) / (double) Math.max(1, total) >= 0.5;
        Tells.line(sl, o.add(side), end.add(side), locked ? WARN : RGB);
        Tells.line(sl, o.subtract(side), end.subtract(side), locked ? WARN : RGB);
        Tells.gather(sl, hand(), 0.6, WHITE, 3);
    }

    // ---------------------------------------------------------------- Heavenly Ice Fang
    private void tellFang(ZogratisBoss b, LivingEntity t, int left, int total) { tellLane(t, left, total, HeathTable.FANG_LENGTH); }

    private void runFang(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 dir = EarlyMoves.flat(takeMark(t).subtract(position()), getViewVector(1f));
        Vec3 from = position().add(dir.scale(1.5)), to = EarlyMoves.stopAtWall(sl, from.add(0, 1, 0), from.add(0, 1, 0).add(dir.scale(HeathTable.FANG_LENGTH)), this).subtract(0, 1, 0);
        double len = Math.max(2, to.distanceTo(from));
        VfxSpawn.send(sl, VfxShape.ICE_FX1, hand(), to.add(0, 1, 0), ARGB, 14, 3.0f);
        EarlyMoves.sound(sl, position(), SoundEvents.POWDER_SNOW_BREAK, 1.6f, 0.7f);
        swing(InteractionHand.MAIN_HAND);
        Set<Integer> hit = new HashSet<>();
        for (double along = 2; along <= len; along += 2) {
            final double a = along;
            later((int) a, EarlyMoves.safe("ice fang", () -> {
                Vec3 p = from.add(dir.scale(a));
                VfxSpawn.send(sl, VfxShape.ICE_FX3, p, p.add(0, 1, 0), ARGB, 12, 0.7f);
                for (LivingEntity e : EarlyMoves.inCircle(this, p, 1.8)) {
                    if (!hit.add(e.getId())) continue;
                    if (strike(e, HeathTable.FANG_HIT, true)) {
                        EarlyMoves.slow(e, 40, 1);
                        EarlyMoves.chill(e, 60);
                        e.setDeltaMovement(e.getDeltaMovement().add(0, 0.45, 0));
                        e.hurtMarked = true;
                    }
                }
            }));
        }
        quip("Stay in your lane.", "Cold enough?");
    }

    // ---------------------------------------------------------------- Icicle Volley
    private void tellVolley(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 o = hand(), aim = t.getBoundingBox().getCenter().subtract(o);
        if (aim.lengthSqr() < 1e-4) return;
        aim = aim.normalize();
        for (double a : EarlyGeometry.fan(Math.min(5, HeathTable.volley(tuning().castSlots())), 0.7)) {
            double[] r = EarlyGeometry.rotate(aim.x, aim.z, a);
            Tells.line(sl, o, o.add(r[0] * 14, aim.y * 14, r[1] * 14), RGB);
        }
        Tells.gather(sl, o, 0.6, WHITE, 3);
    }

    private void runVolley(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        int n = HeathTable.volley(tuning().castSlots());
        double[] fan = EarlyGeometry.fan(n, 0.7);
        for (int i = 0; i < n; i++) {
            final double off = fan[i];
            later(i * 2, EarlyMoves.safe("icicle", () -> {
                if (!isAlive()) return;
                Vec3 from = hand(), aim = t.isAlive() ? t.getBoundingBox().getCenter().subtract(from) : getViewVector(1f);
                if (aim.lengthSqr() < 1e-4) aim = getViewVector(1f);
                double[] r = EarlyGeometry.rotate(aim.x, aim.z, off);
                Vec3 dir = new Vec3(r[0], aim.y, r[1]).normalize();
                Vec3 end = EarlyMoves.stopAtWall(sl, from, from.add(dir.scale(34)), this);
                VfxSpawn.send(sl, VfxShape.ICE_FX1, from, end, ARGB, (int) Math.max(6, from.distanceTo(end) / 1.3), 1.0f);
                EarlyMoves.sound(sl, position(), SoundEvents.SNOW_BREAK, 0.9f, 1.4f);
                EarlyMoves.bolt(this, from, dir, 1.3, 26, 0.55, HeathTable.ICICLE, true, (lv, where, victim) -> {
                    VfxSpawn.send(lv, VfxShape.ICE_FX3, where, where.add(0, 0.5, 0), ARGB, 10, 0.5f);
                    if (victim != null) {
                        EarlyMoves.chill(victim, 40);
                        EarlyMoves.slow(victim, 20, 0);
                    }
                });
            }));
        }
        quip("Icicles, as scheduled.", "Mind the cold.");
    }

    // ---------------------------------------------------------------- Rime Burst
    private void runRime(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        swing(InteractionHand.MAIN_HAND);
        VfxSpawn.send(sl, VfxShape.ICE_FX3, position(), position().add(0, 1, 0), ARGB, 26, 2.2f);
        EarlyMoves.sound(sl, position(), SoundEvents.GLASS_BREAK, 1.4f, 0.8f);
        for (LivingEntity e : EarlyMoves.inCircle(this, position(), HeathTable.RIME_RADIUS)) {
            if (!strike(e, HeathTable.RIME_HIT, true)) continue;
            EarlyMoves.shove(e, position(), 1.3);
            EarlyMoves.slow(e, 40, 1);
            EarlyMoves.chill(e, 80);
        }
        quip("Too close.", "Back, commoner.");
    }

    // ---------------------------------------------------------------- Crystal Ice Shield
    private void runShield(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        shield.raise(sl.getGameTime(), HeathTable.SHIELD_TICKS, HeathTable.shieldPool(getMaxHealth()));
        VfxSpawn.sendFollowing(sl, VfxShape.ICE_FX2, this, position(), ARGB, HeathTable.SHIELD_TICKS, 1.8f);
        EarlyMoves.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6f, 1.5f);
        announce("Heath raises a Crystal Ice Shield: it swallows anything thrown from afar. Close in, or cut it with anti-magic.");
        quip("Throw what you like.");
    }

    // ---------------------------------------------------------------- Frost Slide
    private void tellSlide(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 away = EarlyMoves.flat(position().subtract(t.position()), getViewVector(1f).scale(-1)), o = position().add(0, 0.2, 0);
        Tells.line(sl, o, o.add(away.scale(HeathTable.SLIDE_LENGTH)), ACCENT);
        Tells.ring(sl, position(), 2.2, RGB, 1.2f, 20);
    }

    private void runSlide(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 away = EarlyMoves.flat(position().subtract(t.position()), getViewVector(1f).scale(-1));
        Vec3 end = EarlyMoves.stopAtWall(sl, hand(), hand().add(away.scale(HeathTable.SLIDE_LENGTH)), this);
        double len = Math.max(2, end.subtract(hand()).multiply(1, 0, 1).length());
        VfxSpawn.send(sl, VfxShape.ICE_FX1, position(), position().add(away.scale(len)).add(0, 0.3, 0), ARGB, 8, 2.0f);
        EarlyMoves.charge(this, away, 1.3, (int) Math.ceil(len / 1.3), 1.8, HeathTable.SLIDE_HIT, 0.7);
        quip("Excuse me.");
    }

    // ---------------------------------------------------------------- Mist Step
    private void tellMistStep(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = aimMark(t, left, total, 0.4), dest = stepDestination(m);
        Tells.ring(sl, dest, 2.0, RGB, 1.3f, 24);
        Tells.column(sl, dest, 3, ACCENT);
        Tells.gather(sl, position().add(0, 1, 0), 0.9, WHITE, 6);
        if (first(left, total)) VfxSpawn.send(sl, VfxShape.MIST_MAGIC, position().add(0, 1, 0), dest.add(0, 1, 0), ARGB, total, 1.4f);
    }

    /** The spot behind the target, on the line from him through them. */
    private Vec3 stepDestination(Vec3 target) {
        Vec3 dir = EarlyMoves.flat(target.subtract(position()), getViewVector(1f));
        return ground(level(), target.add(dir.scale(HeathTable.STEP_BEHIND)));
    }

    private void runMistStep(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 dest = stepDestination(takeMark(t));
        Vec3 from = position();
        if (sl.noCollision(this, getBoundingBox().move(dest.subtract(from)))) {
            VfxSpawn.send(sl, VfxShape.MIST_MAGIC, from.add(0, 1, 0), from.add(0, 2, 0), ARGB, 16, 1.4f);
            teleportTo(dest.x, dest.y, dest.z);
            getNavigation().stop();
            VfxSpawn.send(sl, VfxShape.MIST_MAGIC, dest.add(0, 1, 0), dest.add(0, 2, 0), ARGB, 16, 1.4f);
            EarlyMoves.sound(sl, dest, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, 1.2f);
            VfxSpawn.send(sl, VfxShape.ICE_FX3, dest, dest.add(0, 1, 0), ARGB, 20, 1.4f);
            for (LivingEntity e : EarlyMoves.inCircle(this, dest, 3.5)) {
                if (!strike(e, HeathTable.STEP_HIT, true)) continue;
                EarlyMoves.shove(e, dest, 0.8);
                EarlyMoves.chill(e, 40);
            }
        }
        quip("Right on time.");
    }

    // ---------------------------------------------------------------- Ice Coffin
    private void tellCoffin(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = aimMark(t, left, total, 0.5);
        if (first(left, total)) VfxSpawn.send(sl, VfxShape.ICE_FX2, m, m.add(0, 1, 0), ARGB, total + 2, (float) HeathTable.COFFIN_RADIUS);
        Tells.ring(sl, m, HeathTable.COFFIN_RADIUS, WARN, 1.4f, 24);
        Tells.ring(sl, m, Math.max(0.5, HeathTable.COFFIN_RADIUS * left / Math.max(1, total)), ACCENT, 1.1f, 16);
        Tells.column(sl, m, 4, WHITE);
    }

    private void runCoffin(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = takeMark(t);
        boolean may = BalanceLaw.beginControl(this);
        VfxSpawn.send(sl, VfxShape.ICE_FX3, m, m.add(0, 1, 0), ARGB, 28, 2.6f);
        EarlyMoves.sound(sl, m, SoundEvents.GLASS_BREAK, 1.6f, 0.6f);
        for (LivingEntity e : EarlyMoves.inCircle(this, m, HeathTable.COFFIN_RADIUS)) {
            if (!strike(e, HeathTable.COFFIN_HIT, true)) continue;
            EarlyMoves.chill(e, 140);
            if (may) EarlyMoves.slow(e, BalanceLaw.controlTicks(e, 40), 4);
            else EarlyMoves.slow(e, 20, 1);
        }
        quip("Sleep.", "A coffin, as requested.");
    }

    // ---------------------------------------------------------------- Endless Ice Cage
    private void tellCage(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = aimMark(t, left, total, 0.4);
        double r = cageRadius();
        if (first(left, total)) {
            cageSeed = getRandom().nextLong();
            announce("The watch chimes. Heath's Endless Ice Cage: find the open corridor, white on the ground, and stand in it!");
            EarlyMoves.sound(sl, position(), SoundEvents.BELL_BLOCK, 3f, 1.6f);
            for (int k = 0; k < 4; k++) {
                double a = k * Math.PI / 2 + 0.4;
                Vec3 rim = m.add(Math.cos(a) * r, 1, Math.sin(a) * r);
                VfxSpawn.send(sl, VfxShape.MIST_MAGIC, rim, m.add(0, 1, 0), ARGB, total + 10, 1.6f);
            }
            VfxSpawn.send(sl, VfxShape.ICE_FX2, m, m.add(0, 1, 0), ARGB, total + 20, (float) r);
        }
        double opening = Math.atan2(position().z - m.z, position().x - m.x);
        double ox = Math.cos(opening), oz = Math.sin(opening), half = HeathTable.CAGE_CORRIDOR;
        boolean locked = (total - left) / (double) Math.max(1, total) >= 0.4;
        Tells.ring(sl, m, r, locked ? WARN : RGB, 1.4f, (int) Math.min(72, 14 + r * 5));
        Vec3 axisEnd = m.add(ox * r, 0, oz * r), side = new Vec3(-oz, 0, ox).scale(half);
        Tells.line(sl, m.add(side), axisEnd.add(side), WHITE);
        Tells.line(sl, m.subtract(side), axisEnd.subtract(side), WHITE);
        if (left % 6 == 0) EarlyMoves.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 0.8f + 0.8f * (total - left) / Math.max(1, total));
    }

    private void runCage(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = takeMark(t);
        double r = cageRadius();
        double opening = Math.atan2(position().z - m.z, position().x - m.x);
        List<HailPattern.Strike> plan = HailPattern.plan(cageSeed, m.x, m.z, r, HeathTable.CAGE_WAVES, HeathTable.strikesPerWave(stage()), HeathTable.CAGE_IMPACT,
                opening, HeathTable.CAGE_CORRIDOR);
        cageHits = 0;
        cageOpen = true;
        table.kept(sl.getGameTime(), tuning().cooldownMultiplier());
        for (int w = 0; w < HeathTable.CAGE_WAVES; w++) {
            List<Vec3> spots = new ArrayList<>();
            for (HailPattern.Strike s : HailPattern.wave(plan, w)) spots.add(ground(sl, new Vec3(s.x(), m.y, s.z())));
            later(w * HeathTable.CAGE_WAVE_GAP, EarlyMoves.safe("hail wave", () -> EarlyMoves.batchStrike(this, spots, HeathTable.CAGE_IMPACT, HeathTable.CAGE_WARN,
                    HeathTable.HAIL_HIT, true, RGB, ACCENT, (lv, victim, centre) -> {
                        cageHits++;
                        EarlyMoves.slow(victim, 30, 1);
                        EarlyMoves.chill(victim, 50);
                    }, (lv, centre) -> {
                        VfxSpawn.send(lv, VfxShape.ICE_FX1, centre.add(0, 7, 0), centre, ARGB, 6, 1.4f);
                        VfxSpawn.send(lv, VfxShape.ICE_FX3, centre, centre.add(0, 0.5, 0), ARGB, 14, 0.7f);
                    })));
        }
        int done = (HeathTable.CAGE_WAVES - 1) * HeathTable.CAGE_WAVE_GAP + HeathTable.CAGE_WARN + 4;
        later(done, EarlyMoves.safe("plan check", () -> {
            if (!cageOpen) return;
            cageOpen = false;
            if (cageHits == 0 && isAlive()) {
                announce("Nobody was touched: Heath's plan has failed, and he is flustered!");
                say("That was not in the schedule!");
                stagger(HeathTable.FLUSTER_TICKS, HeathTable.FLUSTER_BONUS);
            }
        }));
        say("The Endless Ice Cage. On the stroke.");
    }

    // ---------------------------------------------------------------- Ice Funeral (the ultimate)
    private void tellFuneral(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        double r = funeralRadius();
        if (first(left, total)) {
            armGuard();
            announce("Heath encases himself in an Ice Funeral: run from the ring, or hit him hard to break the device in his chest!");
            VfxSpawn.sendFollowing(sl, VfxShape.ICE_FX2, this, position(), ARGB, total + 4, (float) r);
            EarlyMoves.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 3f, 0.5f);
        }
        Tells.ring(sl, position(), r, WARN, 1.5f, (int) Math.min(80, 16 + r * 5));
        Tells.ring(sl, position(), Math.max(1.0, r * left / Math.max(1, total)), ACCENT, 1.2f, 32);
        Tells.gather(sl, position().add(0, 1, 0), 1.2, WHITE, 8);
    }

    private void runFuneral(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        disarmGuard();
        double r = funeralRadius();
        boolean may = BalanceLaw.beginControl(this);
        VfxSpawn.send(sl, VfxShape.ICE_FX3, position(), position().add(0, 1, 0), ARGB, 40, 5.0f);
        VfxSpawn.send(sl, VfxShape.ICE_FX2, position(), position().add(0, 1, 0), ARGB, 40, (float) r);
        EarlyMoves.sound(sl, position(), SoundEvents.GLASS_BREAK, 3f, 0.4f);
        for (LivingEntity e : EarlyMoves.inCircle(this, position(), r)) {
            if (!strike(e, HeathTable.FUNERAL_HIT, true)) continue;
            EarlyMoves.chill(e, 140);
            if (may) EarlyMoves.slow(e, BalanceLaw.controlTicks(e, HeathTable.FUNERAL_FREEZE), 4);
            else EarlyMoves.slow(e, 30, 2);
        }
        say("Ice Funeral. Nothing leaves this room on schedule.");
    }
}
