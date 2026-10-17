package com.newuniverse.nusmp.arc.early;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
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
import java.util.List;

/**
 * Why: Vetto is the owner's reference for "anti-magic against overwhelming physical stats". In the anime he is the elf in a human body who kills fifty Magic
 * Knights alone, whose Beast Magic wraps animal auras round him (Bear Claw, Cheetah Charge ...), whose body closes any wound, and who finally releases all his
 * mana to bring the Seabed Temple down. Asta's anti-magic sword weakened the very magic that was strengthening his body. The fight is that as rules
 * ({@link VettoTable}).
 * <ul>
 *   <li><b>Beast auras</b> ({@link BeastAuras}): he always wears an aura: the bear (heavy, armoured, strong), the cheetah (fast), the eagle (leaps), and at the
 *       Gates the mythical aura of beasts that never lived. Each is a fixed set of factors on his damage, his speed and the share of a blow his body shrugs
 *       off. He changes aura with a telegraphed Aura Shift, and is open to blows while he does.</li>
 *   <li><b>Tearing an aura</b>: an anti-magic blow tears it off at once (a demon sword in hand, the Anti-Magic Lord); so does a burst of damage (a twelfth of
 *       his health inside three seconds, a little more for the mythical aura). A torn Vetto is weaker than a bare one for 8 seconds (4 after an overload) and
 *       cannot put an aura back on. So the party with anti-magic has the clean answer, and the party without it has the honest one: all at once.</li>
 *   <li><b>Mana Release</b> (the ultimate, from 60%): a long ritual, then a blast of fifteen blocks that falls off towards the edge and is stopped by walls. Leave
 *       it, hide behind a pillar, or hit him for a seventh of his health while he winds it up and it breaks and tears him. After it he has no aura and is open.</li>
 * </ul>
 * His closing of wounds is not a heal (the boss rules forbid one): the framework's single refill at the Gates is the Mythical Beast Magic. He changes no block,
 * spawns nothing, heals nothing. The Evil Eye of the anime (his hair turns white) is not used.
 */
public class VettoBoss extends EarlyBoss {
    public static final ArcCharacter CHARACTER = EarlyRoster.VETTO;
    public static final DeferredHolder<EntityType<?>, EntityType<VettoBoss>> TYPE = ArcEntities.boss(CHARACTER.id(), VettoBoss::new, 0.78f, 2.44f,
            new ArcLook(CHARACTER.id(), 2.44f, 1.46f, 2.6f, 0.65f, 1.65f, 0.73f, true), VettoBoss::createAttributes);

    private static final int WARN = 0xFF4A4A;
    private static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "vetto_aura_speed");

    private final BeastAuras auras = new BeastAuras();
    private double speedFactor = 1.0;
    private long lastAuraFx, lastTearNote;

    public VettoBoss(EntityType<VettoBoss> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return ZogratisBoss.bossAttributes(CHARACTER.health(), CHARACTER.attack(), CHARACTER.speed(), CHARACTER.armor(), CHARACTER.armor() * 0.5);
    }

    @Override public ArcCharacter character() { return CHARACTER; }

    // ================================================================ tuning
    @Override protected ChatFormatting chatColor() { return ChatFormatting.GOLD; }
    @Override protected float formScale(int form) { return form == FORM_HYBRID ? 1.15f : 1.0f; }
    @Override protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7f; }
    /** A brawler: he wants to be within a few blocks. */
    @Override protected double preferredRange(double suggested) { return Math.max(2.5, Math.min(4.5, suggested)); }
    @Override protected String guardedMove() { return VettoTable.RELEASE; }
    @Override protected double guardShare() { return VettoTable.RELEASE_BREAK; }
    /** The aura's factor on every blow of his. */
    @Override protected float damageScale() { return (float) stats().damage(); }

    private BeastAuras.Stats stats() { return auras == null || level() == null ? BeastAuras.stats(BeastAuras.Aura.NONE) : auras.now(level().getGameTime()); }
    private Vec3 chest() { return position().add(0, getBbHeight() * 0.6, 0); }
    private boolean gates() { return gatesOpen(); }
    private Vec3 facing() { return EarlyMoves.flat(getViewVector(1f), new Vec3(0, 0, 1)); }

    // ================================================================ lines
    @Override
    protected String powerLine(int old, int now) {
        if (now >= 80 && old < 80) return "Roar! More! Hit me harder, little ones!";
        if (now >= 60 && old < 60) return "Now this is a hunt!";
        if (now >= 40 && old < 40) return "You bite! I like that!";
        return "Come closer. I will not bite. Much.";
    }

    // ================================================================ the fight's hooks
    @Override
    protected void onFightStart() {
        auras.reset();
        speedFactor = 1.0;
        forgetMarks();
        if (level() instanceof ServerLevel sl) wear(sl, BeastAuras.Aura.BEAR);
        announce("Vetto wears a beast aura: it makes him stronger, faster or harder to hurt. Anti-magic tears it off at once; so does a burst of damage.");
        announce("A torn Vetto is weaker than a bare one for eight seconds and cannot put an aura back on.");
    }

    @Override
    protected void onFightEnd(boolean won) {
        auras.reset();
        setSpeedFactor(1.0);
        super.onFightEnd(won);
    }

    @Override
    protected void onGatesOpened() {
        announce("Vetto's body splits into beasts that never lived: the Mythical Beast Magic. It takes more than a burst to tear it.");
        if (level() instanceof ServerLevel sl) {
            VfxSpawn.send(sl, VfxShape.DEMON_BEAST_FX3, position(), position().add(0, 1, 0), 0xFFE0283C, 40, 3.0f);
            auras.reset();
            wear(sl, BeastAuras.Aura.MYTHICAL);
        }
    }

    @Override
    protected void onChannelBroken() {
        announce("The Mana Release breaks, and the magic tears back through Vetto!");
        say("Gah! My mana... it is turning on me!");
        forgetMarks();
        if (level() instanceof ServerLevel sl) {
            auras.strip(sl.getGameTime(), BeastAuras.TORN_BROKEN);
            setSpeedFactor(stats().speed());
            VfxSpawn.send(sl, VfxShape.DEMON_BEAST_FX3, chest(), chest(), 0xFFE0283C, 30, 2.4f);
            EarlyMoves.sound(sl, position(), SoundEvents.RAVAGER_STUNNED, 2f, 0.7f);
        }
        stagger(70, 1.3f);
    }

    @Override
    protected void bossTick() {
        super.bossTick();
        if (!(level() instanceof ServerLevel sl)) return;
        try {
            long now = sl.getGameTime();
            setSpeedFactor(auras.now(now).speed());
            if (gates() && auras.current() == BeastAuras.Aura.NONE && !auras.torn(now) && fightActive()) {
                wear(sl, BeastAuras.Aura.MYTHICAL);                                  // the beasts re-form as soon as the wound closes
                announce("The Mythical Beast Magic re-forms round Vetto.");
            }
            if (auras.current() != BeastAuras.Aura.NONE && now - lastAuraFx >= 70) {
                lastAuraFx = now;
                auraFx(sl);
            }
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Vetto's aura failed: {}", e.toString());
        }
    }

    // ================================================================ the aura
    private void wear(ServerLevel sl, BeastAuras.Aura a) {
        if (!auras.wear(a, sl.getGameTime())) return;
        setSpeedFactor(auras.now(sl.getGameTime()).speed());
        lastAuraFx = sl.getGameTime();
        auraFx(sl);
    }

    /** The speed of the aura is an attribute modifier, replaced only when it changes. */
    private void setSpeedFactor(double factor) {
        if (Math.abs(factor - speedFactor) < 1e-6) return;
        speedFactor = factor;
        AttributeInstance a = getAttribute(Attributes.MOVEMENT_SPEED);
        if (a == null) return;
        if (Math.abs(factor - 1.0) < 1e-6) a.removeModifier(SPEED_ID);
        else a.addOrReplacePermanentModifier(new AttributeModifier(SPEED_ID, factor - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /** The aura drawn on him for the next three seconds: a beast territory under his feet in the aura's colour (re-sent while it holds). */
    private void auraFx(ServerLevel sl) {
        BeastAuras.Aura a = auras.current();
        if (a == BeastAuras.Aura.NONE) return;
        VfxShape shape = a == BeastAuras.Aura.MYTHICAL ? VfxShape.DEMON_BEAST_FX2 : VfxShape.BEAST_FX2;
        int rgb = switch (a) {
            case BEAR -> 0xE8782A;
            case CHEETAH -> 0xFFD34A;
            case EAGLE -> 0xDDEEFF;
            default -> 0xE0283C;
        };
        VfxSpawn.sendFollowing(sl, shape, this, position(), 0xFF000000 | rgb, 80, a == BeastAuras.Aura.MYTHICAL ? 3.0f : 2.2f);
    }

    private static String auraName(BeastAuras.Aura a) {
        return switch (a) {
            case BEAR -> "Bear";
            case CHEETAH -> "Cheetah";
            case EAGLE -> "Eagle";
            case MYTHICAL -> "Mythical Beast";
            default -> "no";
        };
    }

    /** The tear: an anti-magic blow or a burst takes the aura off; a blow while he changes aura lands harder. */
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        float a = amount;
        try {
            if (level() instanceof ServerLevel sl) {
                long now = sl.getGameTime();
                BeastAuras.Aura worn = auras.current();
                BeastAuras.Hit hit = auras.hit(now, landed(source, amount), antiMagic(source), getMaxHealth());
                if (hit != BeastAuras.Hit.NONE) {
                    setSpeedFactor(auras.now(now).speed());
                    if (now - lastTearNote > 20) {
                        lastTearNote = now;
                        announce(hit == BeastAuras.Hit.ANTI_MAGIC ? "Anti-magic tears the " + auraName(worn) + " aura off Vetto!" : "The burst overloads Vetto's " + auraName(worn) + " aura and it tears away!");
                        say(hit == BeastAuras.Hit.ANTI_MAGIC ? "My magic! What is that blade?" : "Hah... you hit like a storm!");
                    }
                    VfxSpawn.send(sl, VfxShape.DEMON_BEAST_FX3, chest(), chest(), 0xFFE0283C, 24, 1.8f);
                    EarlyMoves.sound(sl, position(), SoundEvents.RAVAGER_STUNNED, 1.8f, 1.0f);
                    stagger(30, 1.15f);
                }
                Move casting = state() == STATE_CASTING ? castingMoveOrNull() : null;
                if (casting != null && casting.id().equals(VettoTable.SHIFT)) a *= VettoTable.SHIFT_BONUS;
            }
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Vetto's tear failed: {}", e.toString());
        }
        return super.filterDamage(source, a);
    }

    /** The aura's armour: a share of every ordinary blow is shrugged off (a torn Vetto has none). */
    @Override
    protected float mitigate(DamageSource source, float amount) { return (float) (amount * (1.0 - stats().mitigation())); }

    // ================================================================ moves
    @Override
    protected List<Move> moves() { return buildAll(VettoTable.rows(), this::runOf, this::tellOf); }

    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case VettoTable.CLAW -> this::runClaw;
            case VettoTable.FLURRY -> this::runFlurry;
            case VettoTable.CHARGE -> this::runCharge;
            case VettoTable.DIVE -> this::runDive;
            case VettoTable.POUND -> this::runPound;
            case VettoTable.ROAR -> this::runRoar;
            case VettoTable.SHIFT -> this::runShift;
            case VettoTable.STAMPEDE -> this::runStampede;
            case VettoTable.CLAWS -> this::runClaws;
            case VettoTable.RELEASE -> this::runRelease;
            default -> (b, t) -> { };
        };
    }

    private Move.MoveTell tellOf(String id) {
        return switch (id) {
            case VettoTable.CLAW -> (b, t, left, total) -> tellCone(VettoTable.CLAW_REACH, VettoTable.CLAW_HALF_ANGLE);
            case VettoTable.FLURRY -> (b, t, left, total) -> tellCone(VettoTable.FLURRY_REACH, VettoTable.CLAW_HALF_ANGLE);
            case VettoTable.CHARGE -> (b, t, left, total) -> tellLane(t, left, total, VettoTable.CHARGE_LENGTH, VettoTable.CHARGE_HALF, 0.5);
            case VettoTable.DIVE -> this::tellDive;
            case VettoTable.POUND -> (b, t, left, total) -> tellRing(VettoTable.POUND_RADIUS, left, total);
            case VettoTable.ROAR -> (b, t, left, total) -> tellRing(VettoTable.ROAR_RADIUS, left, total);
            case VettoTable.SHIFT -> this::tellShift;
            case VettoTable.STAMPEDE -> (b, t, left, total) -> tellLane(t, left, total, VettoTable.STAMPEDE_LENGTH, VettoTable.STAMPEDE_HALF, 0.5);
            case VettoTable.CLAWS -> this::tellClaws;
            case VettoTable.RELEASE -> this::tellRelease;
            default -> null;
        };
    }

    private void tellCone(double reach, double half) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 f = facing(), o = position().add(0, 1.0, 0);
        for (double a : new double[]{-half, 0, half}) {
            double[] r = EarlyGeometry.rotate(f.x, f.z, a);
            Tells.line(sl, o, o.add(r[0] * reach, 0, r[1] * reach), a == 0 ? WARN : 0xE8782A);
        }
        Tells.gather(sl, o, 0.7, 0xFFC27A, 4);
    }

    private void tellRing(double radius, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Tells.ring(sl, position(), Math.max(1.5, radius * (total - left) / Math.max(1, total)), 0xE8782A, 1.3f, 44);
        if (left % 6 == 0) Tells.ring(sl, position(), radius, WARN, 1.1f, 56);
        Tells.gather(sl, position().add(0, 0.4, 0), 1.2, 0xFFC27A, 5);
    }

    /** Two parallel dust lines along the aim, locked partway through the wind-up (turning red). */
    private void tellLane(LivingEntity t, int left, int total, double length, double half, double follow) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = aimMark(t, left, total, follow);
        Vec3 dir = EarlyMoves.flat(m.subtract(position()), getViewVector(1f)), o = position().add(0, 0.3, 0), end = o.add(dir.scale(length));
        Vec3 side = new Vec3(-dir.z, 0, dir.x).scale(half);
        boolean locked = (total - left) / (double) Math.max(1, total) >= follow;
        Tells.line(sl, o.add(side), end.add(side), locked ? WARN : 0xE8782A);
        Tells.line(sl, o.subtract(side), end.subtract(side), locked ? WARN : 0xE8782A);
        Tells.gather(sl, chest(), 0.7, 0xFFC27A, 4);
    }

    // ---------------------------------------------------------------- Bear Claw, Rending Flurry
    private void runClaw(ZogratisBoss b, LivingEntity t) { cone(VettoTable.CLAW_REACH, VettoTable.CLAW_HALF_ANGLE, VettoTable.CLAW_HIT, 1.2, t, 1.3f); quip("Roar!", "Bear Claw!"); }

    private void runFlurry(ZogratisBoss b, LivingEntity t) {
        for (int i = 0; i < 3; i++) later(i * 6, EarlyMoves.safe("rending flurry", () -> { if (isAlive()) cone(VettoTable.FLURRY_REACH, VettoTable.CLAW_HALF_ANGLE, VettoTable.FLURRY_HIT, 0.6, t, 0.8f); }));
        quip("Rip, rend, tear!");
    }

    private void cone(double reach, double half, float damage, double knock, LivingEntity target, float fx) {
        if (!(level() instanceof ServerLevel sl)) return;
        swing(InteractionHand.MAIN_HAND);
        Vec3 f = EarlyMoves.flat(target.position().subtract(position()), getViewVector(1f));
        Vec3 tip = position().add(f.scale(reach * 0.7)).add(0, 1.1, 0);
        VfxSpawn.send(sl, VfxShape.BEAST_FX3, tip, tip.add(f), 0xFFE8782A, 18, fx);
        EarlyMoves.sound(sl, position(), SoundEvents.RAVAGER_ATTACK, 1.4f, 0.9f);
        for (LivingEntity e : EarlyMoves.inCone(this, position(), f, reach, half)) if (strike(e, damage, false)) EarlyMoves.shove(e, position(), knock);
    }

    // ---------------------------------------------------------------- Cheetah Charge
    private void runCharge(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 dir = EarlyMoves.flat(takeMark(t).subtract(position()), getViewVector(1f));
        Vec3 end = EarlyMoves.stopAtWall(sl, chest(), chest().add(dir.scale(VettoTable.CHARGE_LENGTH)), this);
        double len = Math.max(3, end.subtract(chest()).multiply(1, 0, 1).length()), speed = 1.9;
        VfxSpawn.send(sl, VfxShape.BEAST_FX1, chest(), end, 0xFFFFD34A, 10, 2.2f);
        EarlyMoves.sound(sl, position(), SoundEvents.RAVAGER_ROAR, 1.6f, 1.2f);
        EarlyMoves.charge(this, dir, speed, (int) Math.ceil(len / speed), VettoTable.CHARGE_HALF, VettoTable.CHARGE_HIT, 1.2);
        quip("Cheetah Charge!");
    }

    // ---------------------------------------------------------------- Talon Dive
    private void tellDive(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = aimMark(t, left, total, 0.5);
        boolean locked = (total - left) / (double) Math.max(1, total) >= 0.5;
        Tells.ring(sl, m, VettoTable.DIVE_RADIUS, locked ? WARN : 0xE8782A, 1.4f, 32);
        Tells.ring(sl, m, Math.max(0.6, VettoTable.DIVE_RADIUS * left / Math.max(1, total)), 0xFFC27A, 1.1f, 20);
        Tells.column(sl, m, 5, 0xFFC27A);
    }

    /** He really leaps: up, over, and down on the mark, slamming where he lands (never into the air: no floor there, no dive). */
    private void runDive(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = takeMark(t);
        if (groundOrNull(sl, m) == null) {
            slam(sl, position());
            return;
        }
        getNavigation().stop();
        setDeltaMovement(0, 0.8, 0);
        EarlyMoves.sound(sl, position(), SoundEvents.RAVAGER_ROAR, 1.6f, 1.4f);
        VfxSpawn.send(sl, VfxShape.BEAST_FX3, position(), position().add(0, 1, 0), 0xFFE8782A, 14, 1.2f);
        boolean[] landed = {false};
        SpellRuntime.zone(sl, 28, 1, EarlyMoves.safe("talon dive", age -> {
            if (landed[0]) return;
            if (!isAlive() || isRemoved()) {
                landed[0] = true;
                return;
            }
            fallDistance = 0;
            Vec3 to = m.subtract(position()).multiply(1, 0, 1);
            double d = to.length();
            if (d > 0.4 && age < 22) {
                Vec3 v = to.scale(Math.min(1.6, d * 0.28) / d);
                setDeltaMovement(v.x, getDeltaMovement().y, v.z);
            }
            if ((age >= 4 && onGround()) || age >= 26) {
                landed[0] = true;
                slam(sl, position());
            }
        }));
        quip("Talon Dive!");
    }

    private void slam(ServerLevel sl, Vec3 at) {
        VfxSpawn.send(sl, VfxShape.BEAST_FX3, at, at.add(0, 1, 0), 0xFFE8782A, 26, 2.2f);
        EarlyMoves.sound(sl, at, SoundEvents.RAVAGER_ATTACK, 2f, 0.6f);
        for (LivingEntity e : EarlyMoves.inCircle(this, at, VettoTable.DIVE_RADIUS)) if (strike(e, VettoTable.DIVE_HIT, false)) EarlyMoves.shove(e, at, 1.3);
    }

    // ---------------------------------------------------------------- Ground Pound, Beast Roar
    private void runPound(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        VfxSpawn.send(sl, VfxShape.BEAST_FX3, position(), position().add(0, 1, 0), 0xFFE8782A, 26, 2.4f);
        EarlyMoves.sound(sl, position(), SoundEvents.RAVAGER_ATTACK, 2f, 0.5f);
        EarlyMoves.ringWave(this, position(), VettoTable.POUND_RADIUS, VettoTable.POUND_HIT, 1.1, false, 0xE8782A);
        quip("Feel the ground shake!", "Jump if you can!");
    }

    private void runRoar(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        VfxSpawn.send(sl, VfxShape.BEAST_FX3, chest(), chest().add(facing()), 0xFFFFC27A, 30, 3.0f);
        EarlyMoves.sound(sl, position(), SoundEvents.RAVAGER_ROAR, 2.5f, 0.7f);
        for (LivingEntity e : EarlyMoves.inCircle(this, position(), VettoTable.ROAR_RADIUS)) if (strike(e, VettoTable.ROAR_HIT, true)) EarlyMoves.shove(e, position(), 1.4);
        say("ROOOAAARR!");
    }

    // ---------------------------------------------------------------- Aura Shift
    private void tellShift(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total) && !auras.torn(sl.getGameTime())) announce("Vetto changes aura: he is open to blows while he does.");
        Tells.ring(sl, position(), 2.4, 0xFFC27A, 1.3f, 24);
        Tells.gather(sl, chest(), 1.0, 0xE8782A, 8);
    }

    private void runShift(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        if (auras.torn(now)) {
            announce("Vetto's wound is open: he cannot put an aura on.");
            return;
        }
        BeastAuras.Aura next = gates() ? BeastAuras.Aura.MYTHICAL : BeastAuras.next(auras.current());
        wear(sl, next);
        announce("Vetto wears the " + auraName(next) + " aura.");
        quip(next == BeastAuras.Aura.CHEETAH ? "Faster, faster!" : next == BeastAuras.Aura.EAGLE ? "Wings!" : "Heavy as a mountain!");
    }

    // ---------------------------------------------------------------- Stampede
    private void runStampede(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 dir = EarlyMoves.flat(takeMark(t).subtract(position()), getViewVector(1f));
        Vec3 end = EarlyMoves.stopAtWall(sl, chest(), chest().add(dir.scale(VettoTable.STAMPEDE_LENGTH)), this);
        double len = Math.max(4, end.subtract(chest()).multiply(1, 0, 1).length()), speed = 2.0;
        int ticks = (int) Math.ceil(len / speed);
        VfxSpawn.send(sl, VfxShape.BEAST_FX1, chest(), end, 0xFFE8782A, 10, 2.6f);
        EarlyMoves.sound(sl, position(), SoundEvents.RAVAGER_ROAR, 2f, 0.6f);
        EarlyMoves.charge(this, dir, speed, ticks, VettoTable.STAMPEDE_HALF, VettoTable.STAMPEDE_HIT, 1.4);
        Vec3 back = dir.scale(-1);
        later(ticks + 10, EarlyMoves.safe("stampede back", () -> {
            if (!isAlive()) return;
            VfxSpawn.send(sl, VfxShape.BEAST_FX1, chest(), chest().add(back.scale(len)), 0xFFE8782A, 10, 2.6f);
            EarlyMoves.charge(this, back, speed, ticks, VettoTable.STAMPEDE_HALF, VettoTable.STAMPEDE_HIT, 1.4);
        }));
        quip("Stampede!");
    }

    // ---------------------------------------------------------------- Demon Beast Claws
    private void tellClaws(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        double done = (total - left) / (double) Math.max(1, total);
        if (first(left, total) || done < 0.5) {
            List<Vec3> spots = new ArrayList<>();
            for (LivingEntity e : foes(VettoTable.CHARGE_LENGTH + 8)) {
                spots.add(ground(sl, e.position()));
                if (spots.size() >= VettoTable.clawMarks(tuning().castSlots())) break;
            }
            if (spots.isEmpty()) spots.add(ground(sl, t.position()));
            setMarks(spots);
        }
        for (Vec3 m : marks()) {
            Tells.ring(sl, m, VettoTable.CLAWS_RADIUS, done < 0.5 ? 0xFFC27A : WARN, 1.3f, 20);
            Tells.column(sl, m, 3, 0xE0283C);
        }
    }

    private void runClaws(ZogratisBoss b, LivingEntity t) {
        List<Vec3> spots = takeMarks();
        if (spots.isEmpty() && level() instanceof ServerLevel sl) spots = List.of(ground(sl, t.position()));
        EarlyMoves.batchStrike(this, spots, VettoTable.CLAWS_RADIUS, 1, VettoTable.CLAWS_HIT, false, 0xE0283C, 0xFFC27A,
                (lv, victim, centre) -> {
                    victim.setDeltaMovement(victim.getDeltaMovement().add(0, 0.4, 0));
                    victim.hurtMarked = true;
                },
                (lv, centre) -> {
                    VfxSpawn.send(lv, VfxShape.DEMON_BEAST_FX3, centre, centre.add(0, 1, 0), 0xFFE0283C, 24, 1.2f);
                    EarlyMoves.sound(lv, centre, SoundEvents.RAVAGER_ATTACK, 1.2f, 0.8f);
                });
        quip("Claws from below!");
    }

    // ---------------------------------------------------------------- Mana Release (the ultimate)
    private void tellRelease(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        double r = VettoTable.RELEASE_RADIUS;
        if (first(left, total)) {
            armGuard();
            announce("Vetto releases all his mana! Leave the ring, put a wall between you and him, or hit him hard to break the spell!");
            VfxSpawn.sendFollowing(sl, VfxShape.DEMON_BEAST_FX2, this, position(), 0xFFE0283C, total + 4, (float) r);
            EarlyMoves.sound(sl, position(), SoundEvents.WARDEN_SONIC_CHARGE, 3f, 0.4f);
        }
        Tells.ring(sl, position(), r, WARN, 1.5f, 80);
        Tells.ring(sl, position(), Math.max(1.0, r * left / Math.max(1, total)), 0xFFC27A, 1.2f, 40);
        Tells.gather(sl, chest(), 1.4, 0xFFC27A, 10);
    }

    private void runRelease(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        disarmGuard();
        double r = VettoTable.RELEASE_RADIUS;
        Vec3 from = chest();
        VfxSpawn.send(sl, VfxShape.BEAST_FX3, position(), position().add(0, 1, 0), 0xFFE8782A, 40, 6.0f);
        VfxSpawn.send(sl, VfxShape.DEMON_BEAST_FX3, from, from, 0xFFE0283C, 40, 4.0f);
        EarlyMoves.sound(sl, position(), SoundEvents.WITHER_SPAWN, 3f, 0.5f);
        EarlyMoves.sound(sl, position(), SoundEvents.RAVAGER_ROAR, 3f, 0.4f);
        int blocked = 0;
        for (LivingEntity e : foes(r + 2)) {
            double dist = Math.hypot(e.getX() - getX(), e.getZ() - getZ());
            double share = EarlyGeometry.falloff(dist, 5, r, VettoTable.RELEASE_EDGE);
            if (share <= 0) continue;
            if (!EarlyMoves.clear(sl, from, e.getBoundingBox().getCenter(), this)) {
                blocked++;
                EarlyMoves.tell(e, "message.nusmp.arc.early.vetto.shielded");
                continue;
            }
            if (strike(e, (float) (VettoTable.RELEASE_HIT * share), true)) EarlyMoves.shove(e, position(), 1.6 * share + 0.4);
        }
        if (blocked > 0) announce("A wall stood between Vetto's blast and " + blocked + (blocked == 1 ? " fighter." : " fighters."));
        long now = sl.getGameTime();
        auras.strip(now, BeastAuras.TORN_RELEASE);                                   // his mana is spent: no aura, a wound
        setSpeedFactor(auras.now(now).speed());
        say("Mana Release! Let the temple fall!");
    }
}
