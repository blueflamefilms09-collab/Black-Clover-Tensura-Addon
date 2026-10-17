package com.newuniverse.nusmp.arc.early;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
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
 * Why: Revchi Salik is the first villain of the first chapter, the former Magic Knight of the Purple Orca who steals Yuno's new grimoire and ties Asta up
 * in chains until Asta's anti-magic sword cuts them. He is the first fight of the story, so it has to teach the one lesson the whole of Black Clover's
 * villainy rests on: a binding spell is only as strong as the way you answer it. Everything of his is Chain Magic ({@link ChainTable}).
 * <ul>
 *   <li><b>Magic Binding</b> ({@link BindLedger}): a chain that holds a foe, slows them and seals their grimoire for about three seconds. The party can
 *       tear it (every blow the bound foe lands on him takes a link off: two links, so a fighter is free in a few blows), cut it with anti-magic (a demon
 *       sword in hand, the Anti-Magic Lord: immune and free at once), or never be hit by it (the line is drawn in the wind-up and locked a moment before the
 *       chain flies; a pillar or a wall in the way stops it). One bind at a time per foe, and the balance law's shared control lock keeps them twelve seconds
 *       apart.</li>
 *   <li><b>Prey</b>: a bound foe takes 30% more from every blow of his (he feeds on prey that cannot move), so a bind is worth getting out of.</li>
 *   <li><b>Iron Chain Formation</b>: a ring of chains closes on a marked circle (from 40%): leave the ring before it shuts.</li>
 *   <li><b>Total Lockdown</b> (the ultimate, from 60%): chains at everyone he can see, locked a moment before they fly; hide behind a wall, step off the
 *       line, or hit him for a tenth of his health while he winds it up and the spell breaks (he is left open).</li>
 * </ul>
 * He changes no block, spawns nothing, heals nothing; the framework's Gates give him the one refill, and his chains double there.
 */
public class RevchiSalikBoss extends EarlyBoss {
    public static final ArcCharacter CHARACTER = EarlyRoster.REVCHI_SALIK;
    public static final DeferredHolder<EntityType<?>, EntityType<RevchiSalikBoss>> TYPE = ArcEntities.boss(CHARACTER.id(), RevchiSalikBoss::new, 0.55f, 1.94f,
            new ArcLook(CHARACTER.id(), 1.94f, 1.16f, 2.06f, 0.35f, 0.94f, 0.64f, true), RevchiSalikBoss::createAttributes);

    private static final int RGB = CHARACTER.color(), ACCENT = CHARACTER.accent(), WARN = 0xFF4A4A;
    private static final int ARGB = 0xFF000000 | RGB, ARGB_ACCENT = 0xFF000000 | ACCENT;
    private static final String SEALED = "nusmp_sealed_until";

    private BindLedger binds = new BindLedger(ChainTable.LINKS, ChainTable.LINK_FLOOR);
    private List<UUID> lockdownTargets = List.of();

    public RevchiSalikBoss(EntityType<RevchiSalikBoss> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return ZogratisBoss.bossAttributes(CHARACTER.health(), CHARACTER.attack(), CHARACTER.speed(), CHARACTER.armor(), CHARACTER.armor() * 0.5);
    }

    @Override public ArcCharacter character() { return CHARACTER; }

    // ================================================================ tuning
    @Override protected ChatFormatting chatColor() { return ChatFormatting.GRAY; }
    @Override protected float formScale(int form) { return form == FORM_HYBRID ? 1.1f : 1.0f; }
    /** The plain blow comes every second or so, so it is lighter than the telegraphed chains, which are the real hits. */
    @Override protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7f; }
    /** A skirmisher: he wants to be a few blocks away, where the chains reach and a blade does not. */
    @Override protected double preferredRange(double suggested) { return Math.max(4, Math.min(9, suggested)); }
    @Override protected String guardedMove() { return ChainTable.LOCKDOWN; }
    @Override protected double guardShare() { return ChainTable.LOCKDOWN_BREAK; }

    private boolean gates() { return gatesOpen(); }
    private double cageRadius() { return ChainTable.CAGE_RADIUS * (gates() ? ChainTable.GATES_REACH : 1.0); }
    private Vec3 chest() { return position().add(0, getBbHeight() * 0.65, 0); }

    // ================================================================ lines
    @Override
    protected String powerLine(int old, int now) {
        if (now >= 80 && old < 80) return "No more games! These chains do not break!";
        if (now >= 60 && old < 60) return "You are tougher than a rookie. Fine, I will tie you tighter.";
        if (now >= 40 && old < 40) return "Hold still. I am only getting started.";
        return "Tch. Stay still!";
    }

    // ================================================================ the fight's hooks
    @Override
    protected void onFightStart() {
        binds = new BindLedger(ChainTable.LINKS, BindLedger.linkDamage(getMaxHealth(), ChainTable.LINK_SHARE, ChainTable.LINK_FLOOR));
        lockdownTargets = List.of();
        forgetMarks();
        announce("Revchi's Magic Binding chains a foe, slows them and seals their grimoire.");
        announce("Strike him to tear the links, cut them with anti-magic, or keep out of the drawn line.");
    }

    @Override
    protected void onFightEnd(boolean won) {
        if (level() instanceof ServerLevel sl) for (UUID id : binds.ids()) if (sl.getEntity(id) instanceof LivingEntity e) unseal(e, binds.until(id));
        binds.clear();
        lockdownTargets = List.of();
        super.onFightEnd(won);
    }

    @Override
    protected void onGatesOpened() {
        announce("Revchi's scar burns. His chains double: four lashes, a wider cage, eight chains in the lockdown.");
        if (level() instanceof ServerLevel sl) VfxSpawn.send(sl, VfxShape.CHAIN_FX3, position(), position().add(0, 1, 0), ARGB, 40, 2.2f);
    }

    @Override
    protected void onChannelBroken() {
        announce("The chains fall slack: Revchi's Total Lockdown breaks!");
        say("Tch! You cut my spell!");
        forgetMarks();
        lockdownTargets = List.of();
        stagger(60, 1.25f);
        if (level() instanceof ServerLevel sl) {
            VfxSpawn.send(sl, VfxShape.CHAIN_FX3, chest(), chest(), ARGB, 28, 1.6f);
            EarlyMoves.sound(sl, position(), SoundEvents.CHAIN_BREAK, 2f, 0.7f);
        }
    }

    /** Bound foes stay slowed and sealed while the chain holds; a foe who is gone or dead is forgotten. */
    @Override
    protected void bossTick() {
        super.bossTick();
        if (!(level() instanceof ServerLevel sl) || binds.size() == 0 || tickCount % 5 != 0) return;
        try {
            long now = sl.getGameTime();
            for (UUID id : binds.ids()) {
                if (!(sl.getEntity(id) instanceof LivingEntity e) || !e.isAlive()) {
                    binds.free(id);
                    continue;
                }
                if (!binds.bound(id, now)) continue;
                EarlyMoves.slow(e, 12, 2);
                if (e instanceof Player && e.getPersistentData().getLong(SEALED) < binds.until(id)) e.getPersistentData().putLong(SEALED, binds.until(id));
            }
        } catch (RuntimeException ex) {
            LogUtils.getLogger().warn("[nusmp] Revchi's chains failed: {}", ex.toString());
        }
    }

    // ================================================================ the binding
    /** The prey rule: a bound foe takes more from every blow of his. */
    @Override
    public boolean strike(LivingEntity victim, float base, boolean magic) {
        boolean prey = victim != null && binds != null && level() != null && binds.bound(victim.getUUID(), level().getGameTime());
        return super.strike(victim, prey ? base * ChainTable.PREY : base, magic);
    }

    /** The bound foe's blows tear links; anti-magic cuts the whole chain. */
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        try {
            Entity attacker = source.getEntity();
            if (attacker instanceof LivingEntity who && level() instanceof ServerLevel sl && binds.bound(who.getUUID(), sl.getGameTime())) {
                UUID id = who.getUUID();
                if (antiMagic(source)) {
                    unseal(who, binds.until(id));
                    binds.free(id);
                    EarlyMoves.tell(who, "message.nusmp.arc.early.revchi.cut");
                    VfxSpawn.send(sl, VfxShape.CHAIN_FX3, who.position().add(0, 1, 0), who.position().add(0, 1, 0), ARGB_ACCENT, 20, 0.9f);
                    EarlyMoves.sound(sl, who.position(), SoundEvents.CHAIN_BREAK, 1.5f, 1.2f);
                } else {
                    long until = binds.until(id);
                    switch (binds.struggle(id, sl.getGameTime(), landed(source, amount))) {
                        case LINK -> {
                            EarlyMoves.tell(who, "message.nusmp.arc.early.revchi.link", binds.linksLeft(id));
                            EarlyMoves.sound(sl, who.position(), SoundEvents.CHAIN_HIT, 1.2f, 1.0f);
                        }
                        case FREE -> {
                            unseal(who, until);
                            EarlyMoves.tell(who, "message.nusmp.arc.early.revchi.free");
                            VfxSpawn.send(sl, VfxShape.CHAIN_FX3, who.position().add(0, 1, 0), who.position().add(0, 1, 0), ARGB_ACCENT, 20, 0.9f);
                            EarlyMoves.sound(sl, who.position(), SoundEvents.CHAIN_BREAK, 1.5f, 1.0f);
                        }
                        default -> { }
                    }
                }
            }
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Revchi's binding failed: {}", e.toString());
        }
        return super.filterDamage(source, amount);
    }

    /** Takes the seal and the slow off a foe whose chain was cut (the seal only if it is still the one this chain wrote). */
    private void unseal(LivingEntity e, long until) {
        if (e.getPersistentData().getLong(SEALED) == until) e.getPersistentData().putLong(SEALED, 0L);
        e.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
    }

    /** Chains a foe: refused by anti-magic, shortened by the balance law's repeat rule, never added to a bind that is still on. Returns whether it held. */
    private boolean bind(ServerLevel sl, LivingEntity victim, int baseTicks) {
        if (carriesAntiMagic(victim)) {
            EarlyMoves.tell(victim, "message.nusmp.arc.early.revchi.cut");
            VfxSpawn.send(sl, VfxShape.CHAIN_FX3, victim.position().add(0, 1, 0), victim.position().add(0, 1, 0), ARGB_ACCENT, 20, 0.9f);
            return false;
        }
        if (binds.bound(victim.getUUID(), sl.getGameTime())) return false;
        int ticks = BalanceLaw.controlTicks(victim, baseTicks);
        if (!binds.bind(victim.getUUID(), sl.getGameTime(), ticks)) return false;
        long until = binds.until(victim.getUUID());
        if (victim instanceof Player) victim.getPersistentData().putLong(SEALED, until);
        EarlyMoves.slow(victim, ticks, 2);
        VfxSpawn.sendFollowing(sl, VfxShape.SEAL_CHAINS, victim, victim.position(), ARGB, ticks, 1.0f);
        EarlyMoves.tell(victim, "message.nusmp.arc.early.revchi.bound", String.valueOf(Math.max(1, ticks / 20)));
        return true;
    }

    // ================================================================ moves
    @Override
    protected List<Move> moves() { return buildAll(ChainTable.rows(), this::runOf, this::tellOf); }

    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case ChainTable.VIPER -> this::runViper;
            case ChainTable.SCORPIO -> this::runScorpio;
            case ChainTable.HEAVY -> this::runHeavy;
            case ChainTable.BINDING -> this::runBinding;
            case ChainTable.WHIRL -> this::runWhirl;
            case ChainTable.RUSH -> this::runRush;
            case ChainTable.PILLARS -> this::runPillars;
            case ChainTable.FORMATION -> this::runFormation;
            case ChainTable.LOCKDOWN -> this::runLockdown;
            default -> (b, t) -> { };
        };
    }

    private Move.MoveTell tellOf(String id) {
        return switch (id) {
            case ChainTable.VIPER -> this::tellViper;
            case ChainTable.SCORPIO -> (b, t, left, total) -> tellLine(t, left, total, 0.5, ChainTable.CHAIN_RANGE - 4, false);
            case ChainTable.HEAVY -> this::tellHeavy;
            case ChainTable.BINDING -> (b, t, left, total) -> tellLine(t, left, total, 0.45, ChainTable.CHAIN_RANGE, true);
            case ChainTable.WHIRL -> this::tellWhirl;
            case ChainTable.RUSH -> (b, t, left, total) -> tellLine(t, left, total, 0.5, ChainTable.RUSH_LENGTH, false);
            case ChainTable.PILLARS -> this::tellPillars;
            case ChainTable.FORMATION -> this::tellFormation;
            case ChainTable.LOCKDOWN -> this::tellLockdown;
            default -> null;
        };
    }

    // ---------------------------------------------------------------- Dance of the Pitless Viper
    private void tellViper(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 f = EarlyMoves.flat(getViewVector(1f), Vec3.ZERO), o = position().add(0, 1.0, 0);
        for (double a : new double[]{-ChainTable.LASH_HALF_ANGLE, 0, ChainTable.LASH_HALF_ANGLE}) {
            double[] r = EarlyGeometry.rotate(f.x, f.z, a);
            Tells.line(sl, o, o.add(r[0] * ChainTable.LASH_REACH, 0, r[1] * ChainTable.LASH_REACH), a == 0 ? WARN : RGB);
        }
        Tells.gather(sl, o, 0.6, ACCENT, 4);
    }

    private void runViper(ZogratisBoss b, LivingEntity t) {
        int n = ChainTable.lashes(gates());
        for (int i = 0; i < n; i++) later(i * 5, EarlyMoves.safe("viper lash", () -> lash(t)));
        quip("Squirm all you like!", "Feel that? Pitless Viper bites!", "Too slow!");
    }

    private void lash(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl) || !isAlive()) return;
        swing(InteractionHand.MAIN_HAND);
        Vec3 aim = EarlyMoves.flat(target.position().subtract(position()), getViewVector(1f));
        VfxSpawn.send(sl, VfxShape.CHAIN_FX1, chest(), target.getBoundingBox().getCenter(), ARGB, 8, 0.9f);
        EarlyMoves.sound(sl, position(), SoundEvents.CHAIN_HIT, 1.2f, 1.1f);
        for (LivingEntity e : EarlyMoves.inCone(this, position(), aim, ChainTable.LASH_REACH, ChainTable.LASH_HALF_ANGLE)) {
            if (strike(e, ChainTable.LASH, false)) EarlyMoves.shove(e, position(), 0.35);
        }
    }

    // ---------------------------------------------------------------- Scorpio Chain
    private void runScorpio(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = chest(), dir = EarlyMoves.flat(takeMark(t).subtract(position()), getViewVector(1f));
        Vec3 to = EarlyMoves.stopAtWall(sl, from, from.add(dir.scale(ChainTable.CHAIN_RANGE - 4)), this);
        List<LivingEntity> in = EarlyMoves.inCorridor(this, from, to, ChainTable.LINE_HALF);
        if (in.isEmpty()) {
            VfxSpawn.send(sl, VfxShape.CHAIN_FX1, from, to, ARGB, 8, 1.0f);
            VfxSpawn.send(sl, VfxShape.CHAIN_FX3, to, to, ARGB, 20, 0.8f);
            return;
        }
        LivingEntity v = in.get(0);
        VfxSpawn.send(sl, VfxShape.CHAIN_FX1, from, v.getBoundingBox().getCenter(), ARGB, 8, 1.1f);
        if (strike(v, ChainTable.SCORPIO_HIT, false)) {
            EarlyMoves.slow(v, 30, 1);
            Vec3 haul = position().add(dir.scale(2.4));
            SpellRuntime.zone(sl, 6, 1, EarlyMoves.safe("scorpio haul", age -> {
                if (v.isAlive() && isAlive() && v.distanceToSqr(haul) > 2.5) EarlyMoves.pull(v, haul, 0.9);
            }));
            VfxSpawn.send(sl, VfxShape.CHAIN_FX3, v.position().add(0, 1, 0), v.position().add(0, 1, 0), ARGB, 20, 0.8f);
        }
        quip("Come here!", "Got you!");
    }

    // ---------------------------------------------------------------- Heavy Chain
    private void tellHeavy(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = aimMark(t, left, total, 0.55);
        if (first(left, total)) VfxSpawn.send(sl, VfxShape.CHAIN_FX2, m, m.add(0, 1, 0), ARGB, total + 2, (float) ChainTable.HEAVY_RADIUS);
        Tells.ring(sl, m, ChainTable.HEAVY_RADIUS, WARN, 1.4f, 32);
        Tells.ring(sl, m, Math.max(0.6, ChainTable.HEAVY_RADIUS * left / Math.max(1, total)), ACCENT, 1.1f, 20);
        Tells.column(sl, m, 4, ACCENT);
    }

    private void runHeavy(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = takeMark(t);
        VfxSpawn.send(sl, VfxShape.CHAIN_FX1, chest(), m.add(0, 1, 0), ARGB, 7, 1.3f);
        VfxSpawn.send(sl, VfxShape.CHAIN_FX3, m, m.add(0, 1, 0), ARGB, 24, 1.3f);
        EarlyMoves.sound(sl, m, SoundEvents.ANVIL_LAND, 1.4f, 0.6f);
        for (LivingEntity e : EarlyMoves.inCircle(this, m, ChainTable.HEAVY_RADIUS)) {
            if (!strike(e, ChainTable.HEAVY_HIT, false)) continue;
            EarlyMoves.slow(e, 60, 2);
            Vec3 v = e.getDeltaMovement();
            e.setDeltaMovement(v.x * 0.2, Math.min(v.y, 0) - 0.6, v.z * 0.2);
            e.hurtMarked = true;
        }
        quip("Flat!", "Down you go!");
    }

    // ---------------------------------------------------------------- Magic Binding
    /** A line from him to the spot the target stood on when it began; it follows for the first part of the wind-up, then stays (and turns red when it has locked). */
    private void tellLine(LivingEntity t, int left, int total, double follow, double length, boolean binding) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = aimMark(t, left, total, follow);
        Vec3 o = chest(), dir = EarlyMoves.flat(m.subtract(position()), getViewVector(1f));
        double done = (total - left) / (double) Math.max(1, total);
        boolean locked = done >= follow;
        Vec3 end = o.add(dir.scale(Math.min(length, Math.max(4, m.distanceTo(position()) + 3))));
        Tells.line(sl, o, end, locked ? WARN : ACCENT);
        if (binding) {
            Vec3 side = new Vec3(-dir.z, 0, dir.x).scale(ChainTable.LINE_HALF);
            Tells.line(sl, o.add(side), end.add(side), locked ? WARN : RGB);
            Tells.line(sl, o.subtract(side), end.subtract(side), locked ? WARN : RGB);
        }
        Tells.gather(sl, o, 0.5, ACCENT, 3);
    }

    private void runBinding(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = chest(), dir = EarlyMoves.flat(takeMark(t).subtract(position()), getViewVector(1f));
        Vec3 to = EarlyMoves.stopAtWall(sl, from, from.add(dir.scale(ChainTable.CHAIN_RANGE)), this);
        VfxSpawn.send(sl, VfxShape.CHAIN_FX1, from, to, ARGB, 8, 1.3f);
        EarlyMoves.sound(sl, position(), SoundEvents.CHAIN_PLACE, 1.6f, 0.8f);
        List<LivingEntity> in = EarlyMoves.inCorridor(this, from, to, ChainTable.LINE_HALF);
        if (in.isEmpty()) {
            VfxSpawn.send(sl, VfxShape.CHAIN_FX3, to, to, ARGB, 20, 0.8f);
            return;
        }
        LivingEntity v = in.get(0);
        boolean may = BalanceLaw.beginControl(this);
        if (strike(v, ChainTable.BINDING_HIT, true) && may) bind(sl, v, ChainTable.BIND_TICKS);
        VfxSpawn.send(sl, VfxShape.CHAIN_FX3, v.position().add(0, 1, 0), v.position().add(0, 1, 0), ARGB, 22, 1.0f);
        quip("Hold still, kid!", "Your magic is mine to lock away!");
    }

    // ---------------------------------------------------------------- Chain Whirl
    private void tellWhirl(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Tells.ring(sl, position(), Math.max(1.5, ChainTable.WHIRL_RADIUS * (total - left) / Math.max(1, total)), RGB, 1.3f, 40);
        if (left % 6 == 0) Tells.ring(sl, position(), ChainTable.WHIRL_RADIUS, WARN, 1.1f, 48);
        Tells.gather(sl, position().add(0, 1.0, 0), 0.8, ACCENT, 4);
    }

    private void runWhirl(ZogratisBoss b, LivingEntity t) {
        for (int k = 0; k < 2; k++) later(k * 8, EarlyMoves.safe("chain whirl", this::whirl));
        quip("Stay off me!", "Back!");
    }

    private void whirl() {
        if (!(level() instanceof ServerLevel sl) || !isAlive()) return;
        swing(InteractionHand.MAIN_HAND);
        VfxSpawn.send(sl, VfxShape.CHAIN_FX3, position(), position().add(0, 1, 0), ARGB, 22, 1.4f);
        EarlyMoves.sound(sl, position(), SoundEvents.CHAIN_HIT, 1.4f, 0.8f);
        for (LivingEntity e : EarlyMoves.inCircle(this, position(), ChainTable.WHIRL_RADIUS)) if (strike(e, ChainTable.WHIRL_HIT, false)) EarlyMoves.shove(e, position(), 1.0);
    }

    // ---------------------------------------------------------------- Thief's Rush
    private void runRush(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 dir = EarlyMoves.flat(takeMark(t).subtract(position()), getViewVector(1f));
        Vec3 end = EarlyMoves.stopAtWall(sl, chest(), chest().add(dir.scale(ChainTable.RUSH_LENGTH)), this);
        double len = Math.max(3, end.subtract(chest()).multiply(1, 0, 1).length());
        double speed = 1.6;
        VfxSpawn.send(sl, VfxShape.CHAIN_FX1, chest(), end, ARGB, 8, 1.0f);
        EarlyMoves.charge(this, dir, speed, (int) Math.ceil(len / speed), ChainTable.RUSH_HALF, ChainTable.RUSH_HIT, 0.9);
        quip("Out of my way!");
    }

    // ---------------------------------------------------------------- Creation Magic: Chain Pillars
    private void tellPillars(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        double done = (total - left) / (double) Math.max(1, total);
        if (first(left, total) || done < 0.5) {
            List<Vec3> spots = new ArrayList<>();
            for (LivingEntity e : foes(ChainTable.CHAIN_RANGE)) {
                spots.add(ground(sl, e.position()));
                if (spots.size() >= ChainTable.pillarMarks(tuning().castSlots())) break;
            }
            if (spots.isEmpty()) spots.add(ground(sl, t.position()));
            setMarks(spots);
        }
        for (Vec3 m : marks()) {
            Tells.ring(sl, m, ChainTable.PILLAR_RADIUS, done < 0.5 ? ACCENT : WARN, 1.3f, 24);
            Tells.column(sl, m, 3, ACCENT);
        }
    }

    private void runPillars(ZogratisBoss b, LivingEntity t) {
        List<Vec3> spots = takeMarks();
        if (spots.isEmpty() && level() instanceof ServerLevel sl) spots = List.of(ground(sl, t.position()));
        EarlyMoves.batchStrike(this, spots, ChainTable.PILLAR_RADIUS, 1, ChainTable.PILLAR_HIT, false, RGB, ACCENT,
                (sl, victim, centre) -> EarlyMoves.slow(victim, 40, 1),
                (sl, centre) -> {
                    VfxSpawn.send(sl, VfxShape.CHAIN_FX3, centre, centre.add(0, 1, 0), ARGB, 24, 0.9f);
                    EarlyMoves.sound(sl, centre, SoundEvents.CHAIN_PLACE, 1.0f, 0.9f);
                });
        quip("Chains from the floor itself!");
    }

    // ---------------------------------------------------------------- Iron Chain Formation
    private void tellFormation(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = aimMark(t, left, total, 0.4);
        double r = cageRadius();
        if (first(left, total)) {
            VfxSpawn.send(sl, VfxShape.CHAIN_FX2, m, m.add(0, 1, 0), ARGB, total + 14, (float) r);
            announce("Revchi raises an Iron Chain Formation: leave the ring before it shuts!");
        }
        Tells.ring(sl, m, r, left * 2 < total ? WARN : RGB, 1.4f, (int) Math.min(72, 14 + r * 5));
        Tells.ring(sl, m, Math.max(0.8, r * left / Math.max(1, total)), ACCENT, 1.1f, 28);
    }

    private void runFormation(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = takeMark(t);
        boolean may = BalanceLaw.beginControl(this);
        VfxSpawn.send(sl, VfxShape.CHAIN_FX3, m, m.add(0, 1, 0), ARGB, 30, 2.2f);
        EarlyMoves.sound(sl, m, SoundEvents.CHAIN_BREAK, 2f, 0.6f);
        for (LivingEntity e : EarlyMoves.inCircle(this, m, cageRadius())) {
            if (strike(e, ChainTable.CAGE_HIT, true) && may) bind(sl, e, ChainTable.BIND_TICKS);
        }
        quip("Caught like rats!", "The cage is shut!");
    }

    // ---------------------------------------------------------------- Total Lockdown (the ultimate)
    private void tellLockdown(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            armGuard();
            List<UUID> ids = new ArrayList<>();
            List<LivingEntity> near = new ArrayList<>(foes(ChainTable.CHAIN_RANGE));
            near.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(this)));
            for (LivingEntity e : near) {
                if (ids.size() >= ChainTable.lockdownTargets(gates())) break;
                if (EarlyMoves.clear(sl, chest(), e.getBoundingBox().getCenter(), this)) ids.add(e.getUUID());
            }
            lockdownTargets = ids;
            announce("Revchi throws chains at everyone in sight! Break his line of sight, step off the line, or hit him hard to break the spell.");
            EarlyMoves.sound(sl, position(), SoundEvents.CHAIN_PLACE, 3f, 0.5f);
        }
        boolean locked = left <= 9;
        if (!locked || marks().isEmpty()) {
            List<Vec3> points = new ArrayList<>();
            for (UUID id : lockdownTargets) if (sl.getEntity(id) instanceof LivingEntity e && e.isAlive()) points.add(e.getBoundingBox().getCenter());
            setMarks(points);
        }
        for (Vec3 p : marks()) {
            Tells.line(sl, chest(), p, locked ? WARN : ACCENT);
            Tells.ring(sl, ground(sl, p), 1.2, locked ? WARN : RGB, 1.0f, 12);
        }
        Tells.ring(sl, position(), Math.min(14, 3 + (total - left) * 0.25), RGB, 1.3f, 40);
    }

    private void runLockdown(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        disarmGuard();
        List<Vec3> points = takeMarks();
        if (points.isEmpty()) points = List.of(t.getBoundingBox().getCenter());
        lockdownTargets = List.of();
        boolean may = BalanceLaw.beginControl(this);
        Vec3 from = chest();
        Set<Integer> hit = new HashSet<>();
        for (Vec3 p : points) {
            Vec3 dir = p.subtract(from);
            if (dir.lengthSqr() < 1e-4) continue;
            Vec3 end = EarlyMoves.stopAtWall(sl, from, from.add(dir.normalize().scale(Math.min(ChainTable.CHAIN_RANGE + 4, dir.length() * 1.15 + 1))), this);
            VfxSpawn.send(sl, VfxShape.CHAIN_FX1, from, end, ARGB, 9, 1.2f);
            for (LivingEntity e : EarlyMoves.inCorridor(this, from, end, ChainTable.LINE_HALF)) {
                if (!hit.add(e.getId())) continue;
                if (strike(e, ChainTable.LOCKDOWN_HIT, true)) {
                    if (may) bind(sl, e, ChainTable.BIND_TICKS);
                    VfxSpawn.send(sl, VfxShape.CHAIN_FX3, e.position().add(0, 1, 0), e.position().add(0, 1, 0), ARGB, 24, 1.1f);
                }
            }
        }
        EarlyMoves.sound(sl, position(), SoundEvents.CHAIN_BREAK, 3f, 0.5f);
        say("Total Lockdown! Nobody walks away!");
    }
}
