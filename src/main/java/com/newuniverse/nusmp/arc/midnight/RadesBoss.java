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
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: Rades Spirito is the Soul Corpse mage of the Royal Capital arc, cast out of the Purple Orca for what his magic is, who set the dead on the city and
 * who left through a spatial gate when the fight turned against him. His fight is a question of choosing targets, and three mechanics belong only to him.
 * <ul>
 *   <li><b>The ward of the dead</b> ({@link RadesMath#wardFactor}): every corpse that stands soaks 7% of each blow that reaches him, up to 42%. The party
 *       can cut the corpses down (they are weak) or cut the threads: in the story Asta's anti-magic sword stopped the mana that moved the dead, and a
 *       blow of anti-magic here severs the ward for eight seconds.</li>
 *   <li><b>The Soul Abductor</b> ({@link SoulLedger}): the spell that calls a sacrificed soul back to its body. He calls back the corpses that fell in the
 *       last fifteen seconds, once each; a severed thread cannot do it.</li>
 *   <li><b>The retreat through a gate</b> ({@link RadesMath#shouldRetreat}): pressed hard (12% of his health in three seconds) he leaves through a spatial
 *       gate to the far side of the arena and raises his dead behind him; he can do it once in twenty seconds.</li>
 * </ul>
 * His risen dead are vanilla zombies ({@link RadesThralls}) that drop nothing and vanish with him; Jimmy's cursed bullets and Alfred's thunder are the two
 * named corpses of the story as effects. At the Gates he is the Rades of the betrayal ("his power awakened fully after his rage at being betrayed"): the Wrath
 * of the Betrayed is added and his army is at its largest. His existing exclusive-caster pages (the player's Soul Corpse book, the named puppets) are not touched.
 */
public class RadesBoss extends MidnightBoss {
    private static final int SOUL = 0x9BE8B0, STEEL = 0x9AA6C8, VIOLET = 0xB9A2E0, BOLT = 0xE0E8FF;

    public static final ArcCharacter CHARACTER = ArcCharacter.boss("rades_spirito", "Rades Spirito")
            .title("the Soul Corpse Mage").source("Black Clover").saga(ArcSaga.ELF_REINCARNATION).tier(2)
            .colors(0x4A5578, 0x9AA6C8).magic("soul_corpse").stats(1400, 11, 0.27, 8)
            .floors(Polarity.ABYSSAL, 30, 55)
            .themes("necropolis", "cursed_archive", "haunted_manor", "shadow", "drowned_crypt")
            .after("heath_grice", "lotus_whomalt")
            .charter(0.25, "create_lesser_undead", "create_greater_undead", "demon_marionette", "curse", "curse_bind", "mortal_fear",
                    "spiritual_attack_resistance", "darkness")
            .lines("The kingdom threw me out for what I am. Fine. Let the dead show you what that kingdom made of me.",
                    "Betrayed again, by the one I trusted most. Then I owe the living nothing. Rise! All of you, rise!",
                    "So even the dead may rest after all... I never asked for more than that.")
            .build();

    public static final DeferredHolder<EntityType<?>, EntityType<RadesBoss>> TYPE = ArcEntities.boss("rades_spirito", RadesBoss::new, 0.62f, 1.91f,
            new ArcLook("rades_spirito", 1.91f, 1.11f, 1.97f, 0.47f, 1.19f, 1.2f, true), () -> attributesOf(CHARACTER, 4.0));

    static {
        RadesThralls.install();
    }

    private final List<UUID> thralls = new ArrayList<>();
    private final Map<UUID, Long> born = new HashMap<>();
    private final SoulLedger ledger = new SoulLedger();
    private final DamageWindow lost = new DamageWindow();
    private List<UUID> marked = List.of();
    private float lastHealth = -1f;
    private long severedUntil, retreatReadyAt;
    private boolean wardHinted;

    public RadesBoss(EntityType<RadesBoss> type, Level level) { super(type, level); }

    @Override public ArcCharacter character() { return CHARACTER; }

    // ================================================================ looks and numbers
    @Override protected String word() { return "Soul Corpse"; }

    @Override protected String gatesWord() { return "Betrayed"; }

    @Override protected String gatesAnnouncement() { return "Rades's rage at the betrayal breaks the last of his restraint: the dead answer him without limit!"; }

    @Override protected ChatFormatting chatColor() { return ChatFormatting.DARK_AQUA; }

    /** He keeps his dead between himself and the party. */
    @Override protected double preferredRange(double suggested) { return Math.max(5.0, Math.min(13.0, suggested)); }

    @Override protected String[] tensuraKeywords() { return new String[]{"dark", "curse"}; }

    @Override protected int tensuraKitSize() { return 3; }

    private boolean severed() { return tickCount < severedUntil; }

    // ================================================================ words
    @Override
    protected String powerLine(int old, int now) {
        if (now >= 80 && old < 80) return "I have buried better fighters than you.";
        if (now >= 60 && old < 60) return "Another, and another. Count them if you can.";
        if (now >= 40 && old < 40) return "They rise faster than you can cut them.";
        return "I draw " + now + "% of my dead.";
    }

    @Override
    protected void fightBegins() {
        dismiss(false);
        lost.clear();
        ledger.clear();
        lastHealth = -1f;
        severedUntil = 0;
        retreatReadyAt = 0;
        wardHinted = false;
        announce("Rades keeps his dead between himself and you: every corpse that stands takes a share of each blow. Cut them down, or cut their threads with anti-magic.");
    }

    @Override
    protected void fightEnds(boolean won) {
        dismiss(true);
        lost.clear();
        ledger.clear();
    }

    @Override
    protected void onGatesOpened() {
        if (level() instanceof ServerLevel sl) {
            fx(VfxShape.CORPSE_RAISE, position(), position().add(0, 1, 0), SOUL, 50, 2.6f);
            MidnightFx.sound(sl, position(), SoundEvents.WITHER_SPAWN, 2.5f, 0.5f);
        }
        announce("Betrayed by the one he trusted, Rades has nothing left to hold back.");
    }

    /** A boss that leaves the world takes its army with it. */
    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason.shouldDestroy()) dismiss(true);
        super.remove(reason);
    }

    // ================================================================ the army
    private int cap() { return RadesMath.thrallCap(power(), MidnightConfig.thrallLimit()); }

    private int alive() {
        prune();
        return thralls.size();
    }

    /** Forgets the corpses that are gone, notes the ones that fell (the Soul Abductor can call them back) and crumbles the ones that are too old. */
    private void prune() {
        if (!(level() instanceof ServerLevel sl)) {
            thralls.clear();
            return;
        }
        for (Iterator<UUID> it = thralls.iterator(); it.hasNext(); ) {
            UUID id = it.next();
            Entity e = sl.getEntity(id);
            boolean gone = e == null || e.isRemoved();
            if (!gone && e instanceof LivingEntity le && le.isDeadOrDying()) {
                ledger.fell(tickCount, e.getX(), e.getY(), e.getZ());
                gone = true;
            } else if (!gone && tickCount - born.getOrDefault(id, 0L) > RadesMath.THRALL_LIFE) {
                Tells.burst(sl, e.position().add(0, 1, 0), VIOLET, 12);
                e.discard();
                gone = true;
            }
            if (gone) {
                it.remove();
                born.remove(id);
            }
        }
    }

    /** Removes every corpse he has raised (the fight is over, or he is gone). */
    private void dismiss(boolean show) {
        if (level() instanceof ServerLevel sl) {
            for (UUID id : thralls) {
                Entity e = sl.getEntity(id);
                if (e != null && !e.isRemoved()) {
                    if (show) Tells.burst(sl, e.position().add(0, 1, 0), VIOLET, 10);
                    e.discard();
                }
            }
        }
        thralls.clear();
        born.clear();
        marked = List.of();
    }

    private boolean raiseAt(Vec3 at) {
        if (!(level() instanceof ServerLevel sl) || alive() >= cap()) return false;
        Zombie z = RadesThralls.raise(sl, this, at, power(), getTarget());
        if (z == null) return false;
        thralls.add(z.getUUID());
        born.put(z.getUUID(), (long) tickCount);
        fx(VfxShape.CORPSE_RAISE, at, at.add(0, 1, 0), SOUL, 24, 1.0f);
        return true;
    }

    private void frenzy() {
        if (!(level() instanceof ServerLevel sl)) return;
        for (UUID id : thralls) {
            Entity e = sl.getEntity(id);
            if (e instanceof LivingEntity le && le.isAlive()) {
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, RadesMath.FRENZY_TICKS, 1));
                le.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, RadesMath.FRENZY_TICKS, 0));
            }
        }
    }

    // ================================================================ the ward, and what cuts it
    @Override
    protected float onIncoming(DamageSource source, float amount) {
        if (DanteMoves.antiMagicHit(source) && !severed()) {
            severedUntil = tickCount + RadesMath.SEVER_TICKS;
            announce("Anti-magic cuts the threads that move the dead! His ward is gone for " + MidnightText.seconds(RadesMath.SEVER_TICKS) + " s.");
            fx(VfxShape.MIRROR_SHATTER, position().add(0, 1.2, 0), position().add(0, 1.2, 0), STEEL, 20, 1.6f);
        }
        return amount;
    }

    @Override
    protected float guard(DamageSource source, float amount) {
        return (float) (amount * RadesMath.wardFactor(alive(), severed()));
    }

    // ================================================================ every tick
    @Override
    protected void tickMidnight() {
        if (!(level() instanceof ServerLevel sl)) return;
        float hp = getHealth();
        if (lastHealth < 0f) lastHealth = hp;
        if (hp < lastHealth) lost.add(tickCount, lastHealth - hp);          // the health he actually lost (the damage budget already cut the blows)
        lastHealth = hp;
        if (tickCount % 10 == 0) prune();
        if (tickCount % 40 == 0) lost.prune(tickCount, 200);
        if (fightActive() && !staggered() && RadesMath.shouldRetreat(lost.sum(tickCount, RadesMath.RETREAT_WINDOW), getMaxHealth(), tickCount, retreatReadyAt)) retreat(sl);
        if (tickCount % 8 == 0 && isAlive()) Tells.gather(sl, position().add(0, 1.0, 0), 0.7, SOUL, 1 + Math.min(4, thralls.size()));      // the dead's mana, drawn to him
    }

    // ================================================================ moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (MidnightSpec s : RadesTable.specs()) out.add(build(s, tellOf(s.id()), runOf(s.id())));
        return out;
    }

    private Move.MoveTell tellOf(String id) {
        return switch (id) {
            case RadesTable.RAISE_DEAD -> (b, t, left, total) -> tellGraves(t, left, total);
            case RadesTable.JIMMY -> (b, t, left, total) -> tellBullets(t, left, total);
            case RadesTable.ALFRED -> (b, t, left, total) -> tellThunder(t, left, total);
            case RadesTable.SOUL_THREAD -> (b, t, left, total) -> tellThread(t, left, total);
            case RadesTable.DEAD_HANDS -> (b, t, left, total) -> tellHands(t, left, total);
            case RadesTable.WRAITH_WAIL -> (b, t, left, total) -> tellRing(RadesMath.WAIL_RADIUS, left, total);
            case RadesTable.SPECTRAL_GATE -> (b, t, left, total) -> tellGate(t, left, total);
            case RadesTable.CURSE_MAGIC -> (b, t, left, total) -> tellGather();
            case RadesTable.CORPSE_BURST -> (b, t, left, total) -> tellBurst(t, left, total);
            case RadesTable.SOUL_ABDUCTOR -> (b, t, left, total) -> tellAbductor(left, total);
            case RadesTable.LEGION -> (b, t, left, total) -> tellLegion(t, left, total);
            case RadesTable.BETRAYED_WRATH -> (b, t, left, total) -> tellRing(RadesMath.WAIL_RADIUS + 4, left, total);
            default -> null;
        };
    }

    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case RadesTable.RAISE_DEAD -> (b, t) -> runGraves(t);
            case RadesTable.JIMMY -> (b, t) -> runBullets(t, RadesMath.bulletCount(power()));
            case RadesTable.ALFRED -> (b, t) -> runThunder(t);
            case RadesTable.SOUL_THREAD -> (b, t) -> runThread(t);
            case RadesTable.DEAD_HANDS -> (b, t) -> runHands(t);
            case RadesTable.WRAITH_WAIL -> (b, t) -> wail(RadesMath.WAIL_RADIUS, RadesTable.WAIL, 0);
            case RadesTable.SPECTRAL_GATE -> (b, t) -> runGate(t);
            case RadesTable.CURSE_MAGIC -> (b, t) -> {
                if (!castTensura(t)) runBullets(t, 3);
            };
            case RadesTable.CORPSE_BURST -> (b, t) -> runBurst(t);
            case RadesTable.SOUL_ABDUCTOR -> (b, t) -> runAbductor(t);
            case RadesTable.LEGION -> (b, t) -> runLegion(t);
            case RadesTable.BETRAYED_WRATH -> (b, t) -> runWrath();
            default -> (b, t) -> { };
        };
    }

    private Vec3 flatAimAt(LivingEntity t) { return MidnightFx.flatDir(position(), t.position(), getViewVector(1f)); }

    // ---------------------------------------------------------------- Raise the Dead
    private void tellGraves(LivingEntity t, int left, int total) {
        if (first(left, total)) {
            int room = Math.max(0, cap() - alive());
            int n = Math.min(RadesMath.graveCount(power()), room);
            List<Vec3> graves = new ArrayList<>();
            Vec3 fwd = flatAimAt(t);
            for (int i = 0; i < n; i++) {
                double side = (i - (n - 1) / 2.0) * 2.2, dist = 3.0 + getRandom().nextDouble() * 3.0;
                Vec3 g = ground(position().add(fwd.scale(dist)).add(-fwd.z * side, 0, fwd.x * side));
                graves.add(g);
                fx(VfxShape.CORPSE_RAISE, g, g.add(0, 1, 0), SOUL, total, 1.0f);
            }
            marks = graves;
            if (n <= 0) announce("Rades's army is at its height. The Soul Corpse magic bursts out of him instead.");
        }
        if (level() instanceof ServerLevel sl) {
            for (Vec3 g : marks) Tells.ring(sl, g, 1.3, SOUL, 1.2f, 16);
            if (marks.isEmpty()) Tells.ring(sl, position(), 5.0, SOUL, 1.3f, 40);
            Tells.gather(sl, position().add(0, 1, 0), 0.9, SOUL, 6);
        }
    }

    private void runGraves(LivingEntity t) {
        List<Vec3> graves = List.copyOf(marks);
        marks = List.of();
        int made = 0;
        for (Vec3 g : graves) if (raiseAt(g)) made++;
        MidnightFx.sound(level(), position(), SoundEvents.ZOMBIE_AMBIENT, 2f, 0.5f);
        if (made == 0) soulBurst(position(), 5.0, RadesTable.SOUL_BURST);                  // an army at its height: the magic goes into a blast instead
        else quip("Rise.", "Get up. They are still standing.", "The dead do not tire.");
        if (made > 0 && !wardHinted) {
            wardHinted = true;
            announce("Each corpse that stands soaks a share of every blow he takes. Cut them down, or strike him with anti-magic to sever the threads.");
        }
    }

    /** A ring of soul-fire round a spot: a blow of {@code damage} and a shove. */
    private void soulBurst(Vec3 c, double radius, float damage) {
        fx(VfxShape.CORPSE_RAISE, c, c.add(0, 1, 0), SOUL, 20, (float) Math.max(1.0, radius / 3));
        for (LivingEntity e : MidnightFx.inRing(this, c, radius)) {
            if (MidnightFx.hit(this, e, damage, true)) MidnightFx.shove(e, c, 0.9, 0.15);
        }
    }

    // ---------------------------------------------------------------- No.4 Jimmy: Cursed Bullets
    private Vec3 gunner() { return position().add(MidnightFx.rotateY(getViewVector(1f).multiply(1, 0, 1), -1.3).scale(1.1)).add(0, 1.4, 0); }

    private void tellBullets(LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = gunner();
        if (first(left, total)) fx(VfxShape.CORPSE_RAISE, from.subtract(0, 1.2, 0), from, SOUL, total, 0.8f);          // Jimmy, rising at his shoulder
        if (left > 8 || aim.lengthSqr() < 1e-4) aim = t.getBoundingBox().getCenter().subtract(from);
        if (aim.lengthSqr() < 1e-4) return;
        Vec3 dir = aim.normalize();
        int n = RadesMath.bulletCount(power());
        for (double a : MidnightMath.fan(Math.min(n, 5), RadesMath.BULLET_SPREAD)) {
            Tells.line(sl, from, MidnightFx.clipWall(sl, this, from, from.add(MidnightFx.rotateY(dir, a).scale(28))), left <= 8 ? MidnightFx.WARN : SOUL);
        }
    }

    private void runBullets(LivingEntity t, int n) {
        if (!(level() instanceof ServerLevel sl)) return;
        final Vec3 from = gunner();
        final Vec3 dir = aim.lengthSqr() > 1e-4 ? aim.normalize() : t.getBoundingBox().getCenter().subtract(from).normalize();
        final VolleyBudget budget = new VolleyBudget(RadesMath.BULLET_HITS_PER_FOE);
        double[] fan = MidnightMath.fan(n, RadesMath.BULLET_SPREAD * Math.min(1.6, 0.5 + n * 0.15));
        for (int i = 0; i < n; i++) {
            final Vec3 d = MidnightFx.rotateY(dir, fan[i]);
            Runnable fire = MidnightFx.safe("cursed bullet", () -> {
                Tells.burst(sl, from, SOUL, 6);
                MidnightBolt.launch(this, from, d.scale(RadesMath.BULLET_SPEED), RadesMath.BULLET_LIFE, RadesMath.BULLET_RADIUS, SOUL, false, budget,
                        (bolt, v) -> {
                            if (MidnightFx.hit(this, v, RadesTable.BULLET, true)) v.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));      // cursed
                        }, null, null);
            });
            if (i == 0) fire.run();
            else later(i * 2, fire);
        }
        MidnightFx.sound(sl, from, SoundEvents.SKELETON_SHOOT, 1.6f, 0.6f);
        quip("Jimmy, fire.", "No.4, a bullet apiece.");
    }

    // ---------------------------------------------------------------- No.2 Alfred: Thunder
    private void tellThunder(LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) marks = spotsAround(t, RadesMath.thunderCount(power()), 4.0);
        drawMarks(marks, RadesMath.THUNDER_RADIUS, left, total, BOLT);
        Tells.gather(sl, position().add(0, 3.0, 0), 0.6, BOLT, 6);                      // Alfred, floating over him and crackling
    }

    private void runThunder(LivingEntity t) {
        List<Vec3> spots = marks.isEmpty() ? List.of(ground(t.position())) : List.copyOf(marks);
        marks = List.of();
        for (Vec3 c : spots) {
            fx(VfxShape.LIGHTNING_GOD, c, c, BOLT, 16, (float) RadesMath.THUNDER_RADIUS);
            for (LivingEntity e : MidnightFx.inRing(this, c, RadesMath.THUNDER_RADIUS)) {
                if (MidnightFx.hit(this, e, RadesTable.THUNDER, true)) MidnightFx.shove(e, c, 0.3, 0.4);
            }
        }
        MidnightFx.sound(level(), position(), SoundEvents.LIGHTNING_BOLT_IMPACT, 1.6f, 1.5f);
    }

    // ---------------------------------------------------------------- Soul Thread
    private void tellThread(LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) fx(VfxShape.SOUL_VORTEX, t.position(), t.position().add(0, 1, 0), SOUL, total, 2.0f);
        Vec3 from = position().add(0, 1.3, 0);
        Tells.line(sl, from, t.getBoundingBox().getCenter(), left <= RadesMath.THREAD_LOCK ? MidnightFx.WARN : SOUL);
        Tells.gather(sl, from, 0.4, SOUL, 4);
    }

    private void runThread(LivingEntity t) {
        if (!t.isAlive() || t.distanceTo(this) > RadesMath.THREAD_REACH || !hasLineOfSight(t)) return;
        Vec3 from = position().add(0, 1.3, 0);
        fx(VfxShape.SOUL_VORTEX, t.position(), t.position().add(0, 1, 0), SOUL, 16, 1.6f);
        if (MidnightFx.antiMagic(t)) {                                              // a blade of anti-magic cuts the thread, and the ward with it
            severedUntil = Math.max(severedUntil, tickCount + 80);
            if (t instanceof net.minecraft.server.level.ServerPlayer p) p.displayClientMessage(net.minecraft.network.chat.Component.literal("Your blade cuts the soul thread."), true);
            return;
        }
        MidnightFx.hit(this, t, RadesTable.THREAD, true);
        Vec3 flat = position().subtract(t.position()).multiply(1, 0, 1);
        double d = flat.length();
        if (d > RadesMath.THREAD_STOP) {
            Vec3 pull = flat.normalize().scale(Math.min(2.2, (d - RadesMath.THREAD_STOP) * 0.3));
            if (t instanceof net.minecraft.world.entity.player.Player p && p.isFallFlying()) p.stopFallFlying();
            t.setDeltaMovement(pull.x, 0.3, pull.z);
            t.hurtMarked = true;
            t.fallDistance = 0;
        }
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        if (level() instanceof ServerLevel sl) Tells.line(sl, from, t.getBoundingBox().getCenter(), SOUL);
    }

    // ---------------------------------------------------------------- Dead Hands
    private void tellHands(LivingEntity t, int left, int total) {
        if (first(left, total)) marks = List.of(ground(t.position()));
        drawMarks(marks, RadesMath.HANDS_RADIUS, left, total, SOUL);
    }

    private void runHands(LivingEntity t) {
        Vec3 c = marks.isEmpty() ? ground(t.position()) : marks.get(0);
        marks = List.of();
        fx(VfxShape.SHADOW_POOL, c, c, SOUL, 30, (float) RadesMath.HANDS_RADIUS);
        MidnightFx.sound(level(), c, SoundEvents.SOUL_ESCAPE.value(), 1.6f, 0.6f);
        boolean gate = MidnightFx.controlAllowed(this);
        for (LivingEntity e : MidnightFx.inRing(this, c, RadesMath.HANDS_RADIUS)) {
            if (MidnightFx.hit(this, e, RadesTable.HANDS, true)) {
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
                if (gate) MidnightFx.root(e, 25);
            }
        }
    }

    // ---------------------------------------------------------------- Wraith Wail and the Wrath
    private void tellRing(double radius, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Tells.ring(sl, position(), radius, SOUL, 1.3f, 56);
        Tells.ring(sl, position(), Math.max(1.0, radius * (total - left) / Math.max(1, total)), STEEL, 1.1f, 32);
        Tells.gather(sl, position().add(0, 1, 0), 0.9, SOUL, 5);
    }

    private void tellGather() {
        if (level() instanceof ServerLevel sl) {
            Tells.gather(sl, hand(), 0.8, SOUL, 6);
            Tells.ring(sl, position(), 2.2, SOUL);
        }
    }

    /** One wail runs out from him at a block a tick and strikes each foe it passes once, unless they jumped it. */
    private void wail(double reach, float damage, int delay) {
        if (!(level() instanceof ServerLevel sl)) return;
        final Vec3 centre = position();
        later(delay, MidnightFx.safe("wail", () -> {
            fx(VfxShape.SONG_RING, centre, centre.add(0, 1, 0), SOUL, RadesMath.wailTicks(reach), (float) reach);
            MidnightFx.sound(sl, centre, SoundEvents.SOUL_ESCAPE.value(), 2f, 0.5f);
            final Set<Integer> hit = new HashSet<>();
            SpellRuntime.zone(sl, RadesMath.wailTicks(reach), 1, MidnightFx.safe("wail wave", age -> {
                if (!isAlive() || isRemoved()) return;
                double r = age * RadesMath.WAIL_SPEED;
                if (age % 2 == 0) Tells.ring(sl, centre, Math.min(r, reach), SOUL, 1.4f, (int) Math.min(80, 12 + Math.min(r, reach) * 6));
                for (LivingEntity e : foesAround(centre, reach + 3)) {
                    if (hit.contains(e.getId())) continue;
                    double dist = Math.hypot(e.getX() - centre.x, e.getZ() - centre.z);
                    if (dist > reach + e.getBbWidth() / 2 || Math.abs(dist - r) > RadesMath.WAIL_BAND + e.getBbWidth() / 2) continue;
                    if (MidnightFx.floorHeight(sl, e) > RadesMath.WAIL_JUMP) continue;
                    hit.add(e.getId());
                    if (MidnightFx.hit(this, e, damage, true)) MidnightFx.shove(e, centre, 1.0, 0.1);
                }
            }));
        }));
    }

    private void runWrath() {
        for (int k = 0; k < 3; k++) wail(RadesMath.WAIL_RADIUS + 4, RadesTable.WRATH_WAIL, k * 12);
        frenzy();
        say("Rise! All of you, rise!");
    }

    // ---------------------------------------------------------------- Spatial Retreat
    /** A spot {@link RadesMath#RETREAT_DISTANCE} blocks away from the foes, on the floor, where he fits; null if there is none. */
    private Vec3 retreatSpot() {
        if (!(level() instanceof ServerLevel sl)) return null;
        List<LivingEntity> foes = foes(40);
        Vec3 centroid = foes.isEmpty() ? position().add(getViewVector(1f)) : Vec3.ZERO;
        if (!foes.isEmpty()) {
            double x = 0, z = 0;
            for (LivingEntity e : foes) {
                x += e.getX();
                z += e.getZ();
            }
            centroid = new Vec3(x / foes.size(), position().y, z / foes.size());
        }
        Vec3 away = MidnightFx.flatDir(centroid, position(), getViewVector(1f));
        for (double turn : new double[]{0, 0.6, -0.6, 1.2, -1.2, 1.8, -1.8}) {
            Vec3 want = position().add(MidnightFx.rotateY(away, turn).scale(RadesMath.RETREAT_DISTANCE));
            Vec3 g = ZogratisBoss.groundOrNull(sl, want);
            if (g != null && sl.noCollision(this, getBoundingBox().move(g.subtract(position())))) return g;
        }
        return null;
    }

    private void tellGate(LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            Vec3 dest = retreatSpot();
            marks = dest == null ? List.of() : List.of(dest);
            fx(VfxShape.SPACE_PORTAL, position(), position().add(0, 1, 0), STEEL, total, 1.8f);
            if (dest != null) fx(VfxShape.SPACE_PORTAL, dest, dest.add(0, 1, 0), STEEL, total, 1.8f);
        }
        for (Vec3 m : marks) Tells.ring(sl, m, 2.0, STEEL, 1.2f, 24);
        Tells.ring(sl, position(), 1.8, STEEL, 1.2f, 24);
    }

    private void runGate(LivingEntity t) {
        Vec3 old = position();
        Vec3 dest = marks.isEmpty() ? null : marks.get(0);
        marks = List.of();
        if (dest != null && blink(dest)) {
            MidnightFx.sound(level(), dest, SoundEvents.ENDERMAN_TELEPORT, 1.6f, 0.6f);
            raiseAt(old);                                                          // one corpse is left where he stood
        } else {
            soulBurst(position(), 4.5, RadesTable.GATE_BURST);
        }
    }

    /** Pressed too hard, he leaves through a gate to the far side and raises his dead behind him. */
    private void retreat(ServerLevel sl) {
        Vec3 old = position();
        Vec3 dest = retreatSpot();
        if (dest == null) return;
        retreatReadyAt = tickCount + RadesMath.RETREAT_COOLDOWN;
        lost.clear();
        interruptCast();
        fx(VfxShape.SPACE_PORTAL, old, old.add(0, 1, 0), STEEL, 24, 2.0f);
        if (blink(dest)) fx(VfxShape.SPACE_PORTAL, dest, dest.add(0, 1, 0), STEEL, 24, 2.0f);
        MidnightFx.sound(sl, dest, SoundEvents.ENDERMAN_TELEPORT, 2f, 0.5f);
        for (int i = 0; i < 2; i++) raiseAt(old.add(getRandom().nextDouble() * 4 - 2, 0, getRandom().nextDouble() * 4 - 2));
        announce("Rades is pressed too hard and leaves through a spatial gate, as he did in the capital. His dead close the way behind him.");
        quip("Not yet.", "You will have to dig for me.");
    }

    // ---------------------------------------------------------------- Corpse Burst
    private void tellBurst(LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            prune();
            List<UUID> picked = new ArrayList<>();
            for (UUID id : thralls) {
                Entity e = sl.getEntity(id);
                if (e instanceof LivingEntity le && le.isAlive() && le.distanceTo(this) <= 30) {
                    picked.add(id);
                    le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, total + 4, 6));                // the marked corpse stands and swells
                }
            }
            marked = picked;
            marks = picked.isEmpty() ? spotsAround(t, 2, 3.0) : List.of();
            if (!picked.isEmpty()) announce("Rades marks his corpses to burst: get away from them, or kill them far from you.");
        }
        for (UUID id : marked) {
            Entity e = sl.getEntity(id);
            if (e != null && e.isAlive()) {
                Tells.ring(sl, e.position(), RadesMath.BURST_RADIUS, MidnightFx.WARN, 1.3f, 28);
                Tells.gather(sl, e.position().add(0, 1, 0), 0.5, SOUL, 4);
            }
        }
        for (Vec3 m : marks) Tells.ring(sl, m, RadesMath.BURST_RADIUS, MidnightFx.WARN, 1.3f, 28);
    }

    private void runBurst(LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        List<UUID> burst = List.copyOf(marked);
        List<Vec3> fallback = List.copyOf(marks);
        marked = List.of();
        marks = List.of();
        for (UUID id : burst) {
            Entity e = sl.getEntity(id);
            if (e == null || !e.isAlive()) continue;
            Vec3 at = e.position();
            e.discard();
            thralls.remove(id);
            born.remove(id);
            soulBurst(at, RadesMath.BURST_RADIUS, RadesTable.BURST);
        }
        for (Vec3 c : fallback) soulBurst(c, RadesMath.BURST_RADIUS, RadesTable.BURST);
        MidnightFx.sound(sl, position(), SoundEvents.GENERIC_EXPLODE.value(), 1.4f, 1.4f);
    }

    // ---------------------------------------------------------------- Soul Abductor
    private void tellAbductor(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) fx(VfxShape.SOUL_VORTEX, position(), position().add(0, 1, 0), SOUL, total, 4.0f);
        Tells.gather(sl, position().add(0, 1.2, 0), 1.2, SOUL, 8);
    }

    private void runAbductor(LivingEntity t) {
        if (severed()) {
            announce("The Soul Abductor fizzles: the threads are cut.");
            return;
        }
        int room = Math.max(0, cap() - alive());
        List<double[]> fallen = ledger.take(tickCount, RadesMath.ABDUCT_WINDOW, room);
        int back = 0;
        for (double[] p : fallen) if (raiseAt(new Vec3(p[0], p[1], p[2]))) back++;
        if (back > 0) announce("Soul Abductor: " + back + " of the fallen are called back to their bodies, whole.");
        else if (room > 0) raiseAt(position().add(MidnightFx.rotateY(getViewVector(1f).multiply(1, 0, 1), 0.8).scale(3)));
        MidnightFx.sound(level(), position(), SoundEvents.SOUL_ESCAPE.value(), 2f, 1.2f);
    }

    // ---------------------------------------------------------------- Legion of the Dead (the ultimate)
    private void tellLegion(LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            int room = Math.max(1, cap() - alive());
            marks = spotsAround(t, Math.min(room, 6), 4.5);
            for (Vec3 g : marks) fx(VfxShape.CORPSE_RAISE, g, g.add(0, 1, 0), SOUL, total, 1.2f);
            announce("LEGION OF THE DEAD: the ground opens round you. Kill the corpses fast, or cut Rades's threads with anti-magic.");
        }
        for (Vec3 g : marks) Tells.ring(sl, g, 1.5, SOUL, 1.2f, 20);
        Tells.ring(sl, position(), 3 + (total - left) * 0.15, STEEL, 1.2f, 40);
    }

    private void runLegion(LivingEntity t) {
        List<Vec3> graves = marks.isEmpty() ? List.of(ground(t.position())) : List.copyOf(marks);
        marks = List.of();
        for (Vec3 g : graves) raiseAt(g);
        frenzy();
        wail(8.0, RadesTable.LEGION_WAIL, 0);
        MidnightFx.sound(level(), position(), SoundEvents.WITHER_SPAWN, 2.5f, 0.6f);
        say("Rise. Rise, every one of you.");
    }
}
