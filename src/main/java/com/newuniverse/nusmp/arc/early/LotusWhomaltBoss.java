package com.newuniverse.nusmp.arc.early;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
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
import java.util.UUID;

/**
 * Why: Lotus Whomalt is the Diamond Kingdom general of the Dungeon Exploration Arc who would rather nap. He sends his subordinates ahead, watches from the
 * edge of the room, and wins by being patient: in the anime he had been running a weakening spell on Luck since the moment Luck arrived, trapped him in a
 * Smoke Binding Cross Prison, hid the Black Bulls in a veil of thick smoke, and when Asta broke his arm from inside the smoke he simply left. A fight that is
 * "wait and see" cannot be played, but the slow burn can be made visible and made learnable ({@link LotusTable}).
 * <ul>
 *   <li><b>The smoke dose</b> ({@link DoseLedger}): every foe carries a dose from 0 to 10, shown on the action bar. It climbs while they stand in his clouds
 *       (a garden, the veil, the full bloom: {@link CloudField}) and, slowly and never above {@value DoseLedger#SIGHT_CAP}, while he can see them; it falls when
 *       they keep clear. Weakness from 3, slowness from 6, and at 10 they lose their balance and stumble. Stay out of the rings, break his line of sight, or
 *       finish him before it tells.</li>
 *   <li><b>The sideline</b>: he keeps ten to sixteen blocks away and backs off when approached (the Smoke Burst shoves a party that reaches him and he steps
 *       away), so the answer to the poison is to catch him, not to wait it out. At the Gates he is cornered: he stays closer and the clouds thicken.</li>
 *   <li><b>Prisons</b>: the Cross and the Degenerate King's Smoke Prison are drawn on the ground and locked a moment before they close; step out of them. An
 *       anti-magic blade cuts straight through, as Asta's did.</li>
 *   <li><b>Full Bloom</b> (the ultimate, from 60%): a huge garden that thickens fast and then blooms, spending every dose in it as damage; leave it, or hit him
 *       for an eighth of his health while he winds it up and he drops the spell.</li>
 * </ul>
 * He changes no block, spawns nothing but smoke discs and wisps (no entity), heals nothing; the framework's Gates give him the one refill.
 */
public class LotusWhomaltBoss extends EarlyBoss {
    public static final ArcCharacter CHARACTER = EarlyRoster.LOTUS_WHOMALT;
    public static final DeferredHolder<EntityType<?>, EntityType<LotusWhomaltBoss>> TYPE = ArcEntities.boss(CHARACTER.id(), LotusWhomaltBoss::new, 0.55f, 2.06f,
            new ArcLook(CHARACTER.id(), 2.06f, 0.92f, 1.7f, 0.44f, 1.12f, 0.79f, true), LotusWhomaltBoss::createAttributes);

    private static final int RGB = CHARACTER.color(), ACCENT = CHARACTER.accent(), WARN = 0xFF4A4A;
    private static final int ARGB = 0xFF000000 | RGB;

    private final DoseLedger dose = new DoseLedger();
    private final CloudField clouds = new CloudField(LotusTable.CLOUD_CAP);
    private boolean taughtGarden, taughtVeil;

    public LotusWhomaltBoss(EntityType<LotusWhomaltBoss> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return ZogratisBoss.bossAttributes(CHARACTER.health(), CHARACTER.attack(), CHARACTER.speed(), CHARACTER.armor(), CHARACTER.armor() * 0.5);
    }

    @Override public ArcCharacter character() { return CHARACTER; }

    // ================================================================ tuning
    @Override protected ChatFormatting chatColor() { return ChatFormatting.DARK_PURPLE; }
    @Override protected float formScale(int form) { return form == FORM_HYBRID ? 1.1f : 1.0f; }
    @Override protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.6f; }
    /** The sideline general: ten to sixteen blocks away, and at the Gates (cornered) seven to eleven. */
    @Override protected double preferredRange(double suggested) { return gatesOpen() ? Math.max(7, Math.min(11, suggested)) : Math.max(10, Math.min(16, suggested)); }
    @Override protected String guardedMove() { return LotusTable.BLOOM; }
    @Override protected double guardShare() { return LotusTable.BLOOM_BREAK; }

    private Vec3 mouth() { return position().add(0, getBbHeight() * 0.8, 0); }
    private int cloudBonus() { return gatesOpen() ? 1 : 0; }

    // ================================================================ lines
    @Override
    protected String powerLine(int old, int now) {
        if (now >= 80 && old < 80) return "Fine, fine! Everyone breathe in, all the way!";
        if (now >= 60 && old < 60) return "You are still standing? That is rude.";
        if (now >= 40 && old < 40) return "Getting a little tired. Wake me when you are done.";
        return "Mm. Keep breathing.";
    }

    // ================================================================ the fight's hooks
    @Override
    protected void onFightStart() {
        dose.clear();
        clouds.clear();
        taughtGarden = false;
        taughtVeil = false;
        forgetMarks();
        announce("Lotus fights with slow smoke. Every foe carries a dose from 0 to 10 (see the action bar): it climbs in his clouds and in his sight, and falls when you keep clear.");
        announce("Weakness from 3, slowness from 6, a stumble at 10. Break his line of sight, stay out of the rings, or catch him: he backs away when you come close.");
    }

    @Override
    protected void onFightEnd(boolean won) {
        dose.clear();
        clouds.clear();
        super.onFightEnd(won);
    }

    @Override
    protected void onGatesOpened() {
        announce("Lotus gets serious. He stays closer, and every cloud of his is one dose thicker.");
        if (level() instanceof ServerLevel sl) VfxSpawn.send(sl, VfxShape.ASH_FORMATION, position(), position().add(0, 1, 0), ARGB, 60, 6f);
    }

    @Override
    protected void onChannelBroken() {
        announce("Lotus drops the Full Bloom: the spell collapses and he is left open!");
        say("Ow! Hey! I was resting!");
        forgetMarks();
        stagger(LotusTable.STAGGER_TICKS, LotusTable.STAGGER_BONUS);
        if (level() instanceof ServerLevel sl) VfxSpawn.send(sl, VfxShape.ASH_FORMATION, position(), position().add(0, 1, 0), ARGB, 30, 4f);
    }

    /** Once a second the smoke is counted: who stands in which cloud, who is in his sight, and what each dose does. */
    @Override
    protected void bossTick() {
        super.bossTick();
        if (!(level() instanceof ServerLevel sl) || tickCount % 20 != 0 || !fightActive()) return;
        try {
            upkeep(sl);
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Lotus's smoke failed: {}", e.toString());
        }
    }

    private void upkeep(ServerLevel sl) {
        long now = sl.getGameTime();
        boolean aura = (now / 20) % (LotusTable.AURA_PERIOD / 20) == 0, decayBeat = (now / 20) % (LotusTable.DECAY_PERIOD / 20) == 0;
        Set<UUID> seen = new HashSet<>();
        Vec3 eye = getEyePosition();
        for (LivingEntity e : foes(48)) {
            UUID id = e.getUUID();
            seen.add(id);
            int cloud = clouds.gainAt(e.getX(), e.getY(), e.getZ(), now);
            if (cloud > 0) cloud += cloudBonus();
            boolean sight = distanceTo(e) <= LotusTable.AURA_REACH && EarlyMoves.clear(sl, eye, e.getBoundingBox().getCenter(), this);
            int before = dose.dose(id);
            int after = dose.set(id, DoseLedger.step(before, cloud, sight, aura, decayBeat));
            applyDose(sl, e, after);
            if (after >= 3 || after != before) EarlyMoves.tell(e, "message.nusmp.arc.early.lotus.dose", after, DoseLedger.MAX);
        }
        if (decayBeat) for (UUID id : dose.ids()) if (!seen.contains(id)) dose.add(id, -1);       // a foe who left the fight sobers up slowly
    }

    /** The ladder: weakness, slowness, and at the top the stumble (a hard control, so it waits for the balance law's lock and tries again next second). */
    private void applyDose(ServerLevel sl, LivingEntity e, int d) {
        int w = DoseLedger.weakness(d), s = DoseLedger.slowness(d);
        if (w >= 0) EarlyMoves.weaken(e, 50, w);
        if (s >= 0) EarlyMoves.slow(e, 50, s);
        if (!DoseLedger.falls(d) || !BalanceLaw.beginControl(this)) return;
        EarlyMoves.slow(e, BalanceLaw.controlTicks(e, 30), 4);
        Vec3 v = e.getDeltaMovement();
        e.setDeltaMovement(v.x * 0.3, Math.min(v.y, 0) - 0.5, v.z * 0.3);
        e.hurtMarked = true;
        dose.set(e.getUUID(), DoseLedger.AFTER_FALL);
        EarlyMoves.tell(e, "message.nusmp.arc.early.lotus.fall");
        VfxSpawn.send(sl, VfxShape.ASH_FORMATION, e.position(), e.position().add(0, 1, 0), ARGB, 30, 2.2f);
        quip("There. Down you go.", "Told you to breathe deep.");
    }

    /** A disc of smoke: remembered for the dose, drawn for the party. */
    private void addCloud(ServerLevel sl, Vec3 c, double radius, int life, int gain) {
        clouds.add(new CloudField.Cloud(c.x, c.y, c.z, radius, sl.getGameTime() + life, gain), sl.getGameTime());
        VfxSpawn.send(sl, VfxShape.ASH_FORMATION, c, c.add(0, 1, 0), ARGB, life, (float) radius);
        EarlyMoves.sound(sl, c, SoundEvents.FIRE_EXTINGUISH, 1.6f, 0.6f);
    }

    // ================================================================ moves
    @Override
    protected List<Move> moves() { return buildAll(LotusTable.rows(), this::runOf, this::tellOf); }

    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case LotusTable.PUFF -> this::runPuff;
            case LotusTable.GARDEN -> this::runGarden;
            case LotusTable.CROSS -> this::runCross;
            case LotusTable.PUPPETS -> this::runPuppets;
            case LotusTable.BURST -> this::runBurst;
            case LotusTable.STEP -> this::runStep;
            case LotusTable.VEIL -> this::runVeil;
            case LotusTable.PULSE -> this::runPulse;
            case LotusTable.SPHERE -> this::runSphere;
            case LotusTable.BLOOM -> this::runBloom;
            default -> (b, t) -> { };
        };
    }

    private Move.MoveTell tellOf(String id) {
        return switch (id) {
            case LotusTable.PUFF -> this::tellPuff;
            case LotusTable.GARDEN -> (b, t, left, total) -> tellDisc(t, left, total, LotusTable.GARDEN_RADIUS, 0.6);
            case LotusTable.CROSS -> this::tellCross;
            case LotusTable.PUPPETS -> this::tellPuppets;
            case LotusTable.BURST -> (b, t, left, total) -> tellRing(LotusTable.BURST_RADIUS, left, total);
            case LotusTable.STEP -> this::tellStep;
            case LotusTable.VEIL -> (b, t, left, total) -> tellDisc(t, left, total, LotusTable.VEIL_RADIUS, 0.6);
            case LotusTable.PULSE -> this::tellPulse;
            case LotusTable.SPHERE -> (b, t, left, total) -> tellDisc(t, left, total, LotusTable.SPHERE_RADIUS, 0.5);
            case LotusTable.BLOOM -> this::tellBloom;
            default -> null;
        };
    }

    /** A ring on the spot the target stood on when the wind-up began (followed for the first part, then locked and red). */
    private void tellDisc(LivingEntity t, int left, int total, double radius, double follow) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = aimMark(t, left, total, follow);
        boolean locked = (total - left) / (double) Math.max(1, total) >= follow;
        Tells.ring(sl, m, radius, locked ? WARN : ACCENT, 1.4f, (int) Math.min(72, 14 + radius * 5));
        Tells.ring(sl, m, Math.max(0.6, radius * left / Math.max(1, total)), RGB, 1.1f, 24);
        Tells.gather(sl, mouth(), 0.5, ACCENT, 3);
    }

    private void tellRing(double radius, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Tells.ring(sl, position(), Math.max(1.5, radius * (total - left) / Math.max(1, total)), RGB, 1.3f, 32);
        if (left % 6 == 0) Tells.ring(sl, position(), radius, WARN, 1.1f, 36);
        Tells.gather(sl, position().add(0, 1, 0), 0.8, ACCENT, 4);
    }

    // ---------------------------------------------------------------- Smoke Puff
    private void tellPuff(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Tells.line(sl, mouth(), t.getBoundingBox().getCenter(), ACCENT);
        Tells.gather(sl, mouth(), 0.5, RGB, 4);
    }

    private void runPuff(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        swing(InteractionHand.MAIN_HAND);
        Vec3 from = mouth(), aim = t.getBoundingBox().getCenter().subtract(from);
        if (aim.lengthSqr() < 1e-4) aim = getViewVector(1f);
        Vec3 dir = aim.normalize(), end = EarlyMoves.stopAtWall(sl, from, from.add(dir.scale(30)), this);
        VfxSpawn.send(sl, VfxShape.MIST_MAGIC, from, end, ARGB, (int) Math.max(6, from.distanceTo(end) / 1.1), 1.2f);
        EarlyMoves.sound(sl, position(), SoundEvents.FIRE_EXTINGUISH, 1.0f, 1.2f);
        EarlyMoves.bolt(this, from, dir, 1.1, 30, 0.7, LotusTable.PUFF_HIT, true, (lv, where, victim) -> {
            VfxSpawn.send(lv, VfxShape.ASH_FORMATION, where, where.add(0, 0.5, 0), ARGB, 20, 1.6f);
            if (victim != null) dose.add(victim.getUUID(), 1);
        });
        quip("Just a puff.", "Don't mind the smoke.");
    }

    // ---------------------------------------------------------------- Smoke Garden of Usurpation
    private void runGarden(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        addCloud(sl, takeMark(t), LotusTable.GARDEN_RADIUS, LotusTable.GARDEN_LIFE, LotusTable.GARDEN_GAIN);
        if (!taughtGarden) {
            taughtGarden = true;
            announce("A Smoke Garden of Usurpation blooms: a dose a second to whoever stands in it. Stay out of the ring.");
        }
        quip("A little garden. Do sit.", "It takes a while to work. I can wait.");
    }

    // ---------------------------------------------------------------- Smoke Binding Cross Prison
    private void tellCross(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = aimMark(t, left, total, 0.5);
        Vec3 d = EarlyMoves.flat(position().subtract(m), getViewVector(1f)), p = new Vec3(-d.z, 0, d.x);
        boolean locked = (total - left) / (double) Math.max(1, total) >= 0.5;
        int rgb = locked ? WARN : ACCENT;
        for (Vec3 axis : new Vec3[]{d, p}) {
            Vec3 side = new Vec3(-axis.z, 0, axis.x).scale(LotusTable.CROSS_HALF), a = m.subtract(axis.scale(LotusTable.CROSS_ARM)), c = m.add(axis.scale(LotusTable.CROSS_ARM));
            Tells.line(sl, a.add(side), c.add(side), rgb);
            Tells.line(sl, a.subtract(side), c.subtract(side), rgb);
        }
        Tells.ring(sl, m, 2.2, rgb, 1.2f, 20);
        Tells.gather(sl, mouth(), 0.5, ACCENT, 3);
    }

    private void runCross(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = takeMark(t);
        Vec3 d = EarlyMoves.flat(position().subtract(m), getViewVector(1f)), p = new Vec3(-d.z, 0, d.x);
        boolean may = BalanceLaw.beginControl(this);
        VfxSpawn.send(sl, VfxShape.ASH_FORMATION, m, m.add(0, 1, 0), ARGB, 50, 3.4f);
        for (double k : new double[]{-LotusTable.CROSS_ARM * 0.7, LotusTable.CROSS_ARM * 0.7}) {
            VfxSpawn.send(sl, VfxShape.ASH_FORMATION, m.add(d.scale(k)), m.add(d.scale(k)).add(0, 1, 0), ARGB, 40, 2.0f);
            VfxSpawn.send(sl, VfxShape.ASH_FORMATION, m.add(p.scale(k)), m.add(p.scale(k)).add(0, 1, 0), ARGB, 40, 2.0f);
        }
        EarlyMoves.sound(sl, m, SoundEvents.CHAIN_PLACE, 1.6f, 0.5f);
        Set<Integer> hit = new HashSet<>();
        for (Vec3 axis : new Vec3[]{d, p}) {
            for (LivingEntity e : EarlyMoves.inCorridor(this, m.subtract(axis.scale(LotusTable.CROSS_ARM)), m.add(axis.scale(LotusTable.CROSS_ARM)), LotusTable.CROSS_HALF)) {
                if (!hit.add(e.getId())) continue;
                if (carriesAntiMagic(e)) {
                    EarlyMoves.tell(e, "message.nusmp.arc.early.lotus.cut");
                    continue;
                }
                if (!strike(e, LotusTable.CROSS_HIT, true)) continue;
                dose.add(e.getUUID(), 2);
                if (may) EarlyMoves.slow(e, BalanceLaw.controlTicks(e, 50), 4);
            }
        }
        quip("Cross-shaped. Fancy, no?");
    }

    // ---------------------------------------------------------------- Smoke Puppets
    private void tellPuppets(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        int n = 0;
        for (LivingEntity e : foes(30)) {
            if (++n > LotusTable.puppets(tuning().castSlots())) break;
            Tells.line(sl, mouth(), e.getBoundingBox().getCenter(), RGB);
        }
        Tells.gather(sl, mouth(), 0.9, ACCENT, 6);
    }

    private void runPuppets(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        List<Vec3> starts = new ArrayList<>();
        for (int i = 0; i < LotusTable.puppets(tuning().castSlots()); i++) {
            double a = getRandom().nextDouble() * Math.PI * 2;
            Vec3 p = ground(sl, position().add(Math.cos(a) * 2.5, 0, Math.sin(a) * 2.5)).add(0, 0.9, 0);
            starts.add(p);
            VfxSpawn.send(sl, VfxShape.ASH_FORMATION, p, p.add(0, 0.5, 0), ARGB, 20, 1.0f);
        }
        EarlyMoves.sound(sl, position(), SoundEvents.FIRE_EXTINGUISH, 1.6f, 0.8f);
        EarlyMoves.wisps(this, starts, 0.22, 84, LotusTable.PUPPET_RADIUS, LotusTable.PUPPET_HIT, true, ACCENT,
                (lv, victim, centre) -> dose.add(victim.getUUID(), 1),
                (lv, where) -> {
                    VfxSpawn.send(lv, VfxShape.ASH_FORMATION, where, where.add(0, 1, 0), ARGB, 20, (float) LotusTable.PUPPET_RADIUS);
                    EarlyMoves.sound(lv, where, SoundEvents.FIRE_EXTINGUISH, 1.0f, 1.4f);
                });
        quip("Go on, my little ones.");
    }

    // ---------------------------------------------------------------- Smoke Burst
    private void runBurst(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        VfxSpawn.send(sl, VfxShape.ASH_FORMATION, position(), position().add(0, 1, 0), ARGB, 30, (float) LotusTable.BURST_RADIUS);
        EarlyMoves.sound(sl, position(), SoundEvents.FIRE_EXTINGUISH, 2f, 0.7f);
        for (LivingEntity e : EarlyMoves.inCircle(this, position(), LotusTable.BURST_RADIUS)) {
            if (!strike(e, LotusTable.BURST_HIT, true)) continue;
            EarlyMoves.shove(e, position(), 1.2);
            dose.add(e.getUUID(), 2);
        }
        Vec3 away = EarlyMoves.flat(position().subtract(t.position()), getViewVector(1f).scale(-1));
        EarlyMoves.charge(this, away, 1.2, 6, 1.5, 0f, 0);              // and he steps back
        quip("Personal space, please.", "Too close.");
    }

    // ---------------------------------------------------------------- Smoke Step
    private void tellStep(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 dest = stepDestination(t);
        Tells.ring(sl, dest, 1.8, ACCENT, 1.2f, 20);
        Tells.column(sl, dest, 3, RGB);
        Tells.gather(sl, position().add(0, 1, 0), 0.8, ACCENT, 5);
    }

    /** Eleven blocks from the target, on the far side from them. */
    private Vec3 stepDestination(LivingEntity t) {
        Vec3 away = EarlyMoves.flat(position().subtract(t.position()), getViewVector(1f).scale(-1));
        return ground(level(), t.position().add(away.scale(11)));
    }

    private void runStep(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 dest = stepDestination(t), from = position();
        if (!sl.noCollision(this, getBoundingBox().move(dest.subtract(from)))) return;
        addCloud(sl, from, 4.0, 100, LotusTable.GARDEN_GAIN);
        teleportTo(dest.x, dest.y, dest.z);
        getNavigation().stop();
        VfxSpawn.send(sl, VfxShape.ASH_FORMATION, dest, dest.add(0, 1, 0), ARGB, 24, 2.4f);
        quip("Over here, then.");
    }

    // ---------------------------------------------------------------- Hermit's Thick Smoke
    private void runVeil(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        addCloud(sl, takeMark(t), LotusTable.VEIL_RADIUS, LotusTable.VEIL_LIFE, LotusTable.VEIL_GAIN);
        if (!taughtVeil) {
            taughtVeil = true;
            announce("Hermit's Thick Smoke: inside the veil the dose climbs twice as fast. Get out of it.");
        }
        quip("Nothing to see in here.");
    }

    // ---------------------------------------------------------------- Weakening Bloom
    private void tellPulse(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        for (LivingEntity e : foes(32)) if (dose.dose(e.getUUID()) >= 3) Tells.line(sl, mouth(), e.getBoundingBox().getCenter(), WARN);
        Tells.gather(sl, mouth(), 1.0, ACCENT, 6);
    }

    private void runPulse(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        boolean any = false;
        for (LivingEntity e : foes(32)) {
            UUID id = e.getUUID();
            int d = dose.dose(id);
            float dmg = DoseLedger.bloom(d, LotusTable.PULSE_PER_DOSE);
            if (dmg <= 0f) continue;
            any = true;
            if (strike(e, dmg, true)) {
                dose.set(id, Math.max(0, d - 6));
                VfxSpawn.send(sl, VfxShape.ASH_FORMATION, e.position(), e.position().add(0, 1, 0), ARGB, 24, 2.0f);
            }
        }
        if (!any) for (LivingEntity e : foes(32)) if (EarlyMoves.clear(sl, getEyePosition(), e.getBoundingBox().getCenter(), this)) dose.add(e.getUUID(), 2);
        EarlyMoves.sound(sl, position(), SoundEvents.FIRE_EXTINGUISH, 2f, 0.5f);
        quip("It blooms in you now.", "Usurpation. Thank you for breathing.");
    }

    // ---------------------------------------------------------------- Degenerate King's Smoke Prison
    private void runSphere(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = takeMark(t);
        boolean may = BalanceLaw.beginControl(this);
        VfxSpawn.send(sl, VfxShape.ASH_FORMATION, m, m.add(0, 1, 0), ARGB, 60, (float) LotusTable.SPHERE_RADIUS);
        EarlyMoves.sound(sl, m, SoundEvents.CHAIN_PLACE, 1.6f, 0.5f);
        for (LivingEntity e : EarlyMoves.inCircle(this, m, LotusTable.SPHERE_RADIUS)) {
            if (carriesAntiMagic(e)) {
                EarlyMoves.tell(e, "message.nusmp.arc.early.lotus.cut");
                continue;
            }
            if (!strike(e, LotusTable.SPHERE_HIT, true)) continue;
            dose.add(e.getUUID(), 3);
            if (may) {
                EarlyMoves.slow(e, BalanceLaw.controlTicks(e, 60), 4);
                EarlyMoves.weaken(e, 80, 1);
            }
        }
        quip("The Degenerate King's prison. Do not mind the walls.");
    }

    // ---------------------------------------------------------------- Full Bloom of Usurpation (the ultimate)
    private void tellBloom(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = aimMark(t, left, total, 0.5);
        if (first(left, total)) {
            armGuard();
            announce("Lotus winds up a Full Bloom: a huge garden that thickens fast and then blooms. Get out of the ring, or hit him hard to break the spell!");
            VfxSpawn.send(sl, VfxShape.ASH_FORMATION, m, m.add(0, 1, 0), ARGB, total + 10, (float) LotusTable.BLOOM_RADIUS);
            EarlyMoves.sound(sl, position(), SoundEvents.WARDEN_SONIC_CHARGE, 3f, 0.6f);
        }
        boolean locked = (total - left) / (double) Math.max(1, total) >= 0.5;
        Tells.ring(sl, m, LotusTable.BLOOM_RADIUS, locked ? WARN : ACCENT, 1.5f, 72);
        Tells.ring(sl, m, Math.max(1.0, LotusTable.BLOOM_RADIUS * left / Math.max(1, total)), RGB, 1.2f, 32);
        Tells.gather(sl, mouth(), 1.2, ACCENT, 8);
    }

    private void runBloom(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        disarmGuard();
        Vec3 m = takeMark(t);
        addCloud(sl, m, LotusTable.BLOOM_RADIUS, LotusTable.BLOOM_LIFE, LotusTable.BLOOM_GAIN);
        say("Full Bloom of Usurpation. Take a nice long breath.");
        later(LotusTable.BLOOM_LIFE - 10, EarlyMoves.safe("full bloom", () -> {
            if (!isAlive()) return;
            VfxSpawn.send(sl, VfxShape.ASH_FORMATION, m, m.add(0, 1, 0), ARGB, 30, (float) LotusTable.BLOOM_RADIUS);
            EarlyMoves.sound(sl, m, SoundEvents.WITHER_SPAWN, 2f, 1.4f);
            for (LivingEntity e : EarlyMoves.inCircle(this, m, LotusTable.BLOOM_RADIUS)) {
                UUID id = e.getUUID();
                int d = dose.dose(id);
                float dmg = DoseLedger.bloom(d, LotusTable.BLOOM_PER_DOSE);
                if (dmg > 0f && strike(e, dmg, true)) dose.set(id, Math.max(0, d - 6));
            }
        }));
    }
}
