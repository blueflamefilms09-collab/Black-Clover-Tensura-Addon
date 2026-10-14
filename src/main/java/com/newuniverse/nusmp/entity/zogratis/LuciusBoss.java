package com.newuniverse.nusmp.entity.zogratis;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.boss.LuciusProfile;
import com.newuniverse.nusmp.boss.LuciusSpellTable;
import com.newuniverse.nusmp.boss.LuciusSpells;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Lucius Zogratis, the king of the Spade Kingdom, the main antagonist and the endgame boss of the Zogratis family: two souls in one
 * body, the contract of Astaroth (Time) and the absorbed power of his siblings. 3600 health, 22 armour, the fastest thing in the
 * mod; the fight is a long, readable duel with a calm god who answers everything, and who is not quite alone in his body.
 * <ul>
 *   <li><b>Soul Magic</b> (innate): Soul Motes, the Soul Drain (heals him), Soul Paladins (powerful mobs made loyal and immortal while he
 *       lives, see {@link SoulPaladins}), Resurrection, Memory Manipulation (the last spell is forgotten for six seconds), and the Soul
 *       Purge, which strips devil power from the party and cuts the Dark Triad off from its devils ({@link LuciusMoves}).</li>
 *   <li><b>Two Souls</b>: at 75%, 55%, 35% and 12% health the will of the Wizard King Julius Novachrono surfaces for eight seconds
 *       ({@link JuliusWill}): Lucius stumbles and his moves fail, his precognition is gone, he takes 40% more damage, the boss bar becomes
 *       Julius's, and the party is blessed (healed, cleansed, remembers every spell, cannot be stopped in time).</li>
 *   <li><b>Time Magic</b> (Astaroth, {@link ChronoMoves}): <i>precognition</i> ({@link Precognition}: a few charges that dodge a blow and lash
 *       the attacker; beaten by two attackers at once, by anti-magic, by the open moments after an ultimate, or by waiting for the charges
 *       to spend), steps beyond light, the
 *       Chrono Stasis, the Time Stop (three seconds at most, marked blows afterwards), Time Reversal (he goes back to where he was and
 *       takes back some wounds, shown before it happens) and Chrono Acceleration.</li>
 *   <li><b>Grimoire Tower Access</b> ({@link TowerSpells}): any page of any magic, cast as the spell of its tier and named on the screen.</li>
 *   <li><b>Sibling Assimilation</b>: Dante's gravity (Lucifero's), Vanica's blood and Zenon's bone through the siblings' own move
 *       functions, and a slow mending of his flesh that anti-magic stops.</li>
 *   <li><b>Clones</b> ({@link ZogratisCloneEntity}): the Mirror Court and the Swap Strike. He has no Dark Disciples; he has Paladins.</li>
 *   <li><b>Forms</b>: his human body until the Gates of the Tree of Qliphoth open at the last quarter of his health, then the divine
 *       form (three pairs of wings, a halo of gears, the grimoire open before him): all his numbers at their peak.</li>
 * </ul>
 */
public class LuciusBoss extends ZogratisBoss {
    private static final EntityDataAccessor<Integer> JULIUS = SynchedEntityData.defineId(LuciusBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHARGES = SynchedEntityData.defineId(LuciusBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FLAGS = SynchedEntityData.defineId(LuciusBoss.class, EntityDataSerializers.INT);

    /** Bits of {@link #flags()}. */
    public static final int FLAG_ACCELERATED = 1, FLAG_STOPPING = 2;

    private static final float JULIUS_DAMAGE = 1.4f;
    private static final ResourceLocation JULIUS_SLOW = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "lucius_julius_slow");

    private final ChronoRing ring = new ChronoRing(ChronoMath.ringCapacity());
    private final Precognition precog = new Precognition();
    private final JuliusWill julius = new JuliusWill();
    private final PaladinLedger<UUID> paladins = new PaladinLedger<>();
    private final SoulMath.Budget budget = new SoulMath.Budget();
    private final ServerBossEvent juliusBar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10);

    // the marks a wind-up locks for its move
    private TowerSpells.Entry pendingTower;
    private Vec3 stepMark, haltSpot, fieldMark;
    private List<Vec3> courtSpots = List.of();
    private UUID swapPartnerId;
    private long reverseStart;

    /** The foes he has stopped in time, so that the end of the fight can let them go at once. */
    private final Set<UUID> held = new HashSet<>();

    private long accelUntil, lastAntiMagic = -1000, bodySuppressedUntil, lastQuip, lastDodgeNote, lastBodyNote;
    private int fallenClones, restoreMask = -1, restoreLeft;
    private boolean ready, hintedDown, hintedEmpty, flagStopping;
    private ListTag restorePaladins;

    public LuciusBoss(EntityType<LuciusBoss> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() { return ZogratisBoss.bossAttributes(3600, 22, 0.36, 22, 12); }

    @Override public Devil devil() { return Devil.ASTAROTH; }

    @Override
    public String bossName() { return entityData != null && entityData.get(JULIUS) > 0 ? "Julius Novachrono" : "Lucius Zogratis"; }

    @Override
    public String barTitle() {
        return julius() ? "Julius Novachrono - the Wizard King's will" : super.barTitle();
    }

    @Override
    protected ChatFormatting chatColor() { return julius() ? ChatFormatting.WHITE : super.chatColor(); }

    // ================================================================ state for the client and the moves
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(JULIUS, 0);
        b.define(CHARGES, 1);
        b.define(FLAGS, 0);
    }

    /** Is Julius's will in control (the body stumbles, the bar is his)? Works on the client too. */
    public boolean julius() { return entityData.get(JULIUS) > 0; }

    /** Ticks left of Julius's window (0 when he sleeps), synced about every half second. */
    public int juliusLeft() { return entityData.get(JULIUS); }

    /** Precognition charges he holds now (the renderer shows an hourglass mote for each). */
    public int precogCharges() { return entityData.get(CHARGES); }

    /** {@link #FLAG_ACCELERATED} (Chrono Acceleration is running), {@link #FLAG_STOPPING} (a Time Stop is being cast). */
    public int flags() { return entityData.get(FLAGS); }

    /** How far the divine form hovers above the ground, in blocks (0 in the human form). */
    public float hover() { return form() == FORM_DIVINE ? 0.4f + 0.06f * (float) Math.sin(tickCount * 0.12) : 0f; }

    ChronoRing ring() { return ring; }
    Precognition precog() { return precog; }
    PaladinLedger<UUID> paladins() { return paladins; }
    SoulMath.Budget budget() { return budget; }
    boolean juliusActive() { return julius.active(level().getGameTime()); }
    int fallenClones() { return fallenClones; }
    void accelerateUntil(long tick) { accelUntil = tick; syncFlags(); }

    /** The live clones of this Lucius. */
    public List<ZogratisCloneEntity> clones() {
        if (!(level() instanceof ServerLevel sl)) return List.of();
        UUID me = getUUID();
        return sl.getEntitiesOfClass(ZogratisCloneEntity.class, getBoundingBox().inflate(96), c -> c.isAlive() && me.equals(c.ownerId()));
    }

    void noteCloneFallen() { fallenClones = Math.min(6, fallenClones + 1); }

    /** A foe was stopped in time by him (remembered, bounded). */
    void noteHeld(LivingEntity foe) {
        if (held.size() > 64) held.clear();
        held.add(foe.getUUID());
    }

    /** Lets every foe he stopped go now (the fight is over, he fell or left the world), with a short grace against stops of any kind. */
    private void releaseHeld(ServerLevel sl) {
        for (UUID id : held) if (sl.getEntity(id) instanceof LivingEntity le && TimeStop.isFrozen(le)) TimeStop.immune(le, 40);
        held.clear();
    }

    boolean takeFallenClone() {
        if (fallenClones <= 0) return false;
        fallenClones--;
        return true;
    }

    /** A Paladin was struck down: the ledger starts its revive timer, and the first time the party is told how to keep it down. */
    void paladinFell(Mob mob) {
        paladins.down(mob.getUUID(), level().getGameTime(), SoulMath.reviveTicks(power()));
        if (!hintedDown) {
            hintedDown = true;
            announce("A Soul Paladin falls, and kneels. Its soul is tied to Lucius: it rises again in " + SoulMath.reviveTicks(power()) / 20
                    + " seconds. Cut the soul with anti-magic or holy damage while it kneels, or defeat Lucius.");
        }
    }

    private void syncFlags() {
        int f = (level().getGameTime() < accelUntil ? FLAG_ACCELERATED : 0) | (flagStopping ? FLAG_STOPPING : 0);
        if (entityData.get(FLAGS) != f) entityData.set(FLAGS, f);
    }

    /** One of 'lines' now and then (at most every eight seconds), so the chat is not a wall of speech. */
    void quip(String... lines) {
        if (tickCount - lastQuip < 160) return;
        lastQuip = tickCount;
        say(lines[getRandom().nextInt(lines.length)]);
    }

    // ================================================================ tuning
    @Override protected int gatesForm() { return FORM_DIVINE; }
    @Override protected float formScale(int form) { return form == FORM_DIVINE ? 1.2f : form == FORM_BASE ? 1.05f : 1.0f; }
    @Override protected float damageScale() { return 1.25f; }

    /** His plain blow is light (a god does not need his hands); none at all while Julius holds him. */
    @Override
    protected float meleeDamage() {
        if (julius()) return 0f;
        return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7f * (level().getGameTime() < accelUntil ? ChronoMath.ACCEL_MELEE : 1f);
    }

    /** He keeps his distance: a calm caster at eight to fourteen blocks, closer in the divine form. */
    @Override
    protected double preferredRange(double suggested) {
        return form() == FORM_DIVINE ? Math.max(5, Math.min(11, suggested)) : Math.max(7, Math.min(14, suggested));
    }

    // ================================================================ the boss overhaul (boss/LuciusProfile)
    /** His nullifications as fair cuts of the hit; none in an open window and none against anti-magic (the designed counter). */
    @Override
    protected float mitigate(DamageSource source, float amount) {
        if (LuciusFx.antiMagicHit(source)) return amount;
        return LuciusProfile.mitigate(this, source, amount);
    }

    /** Abnormal Condition Nullification: harmful effects do not land, except in an open window. */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (LuciusProfile.refuses(this, effect)) return false;
        return super.canBeAffected(effect);
    }

    /** Intangibility, Dead End Rainbow and the safe Usurper ride on his blows. */
    @Override
    public boolean strike(LivingEntity victim, float base, boolean magic) {
        return LuciusProfile.strike(this, victim, base, magic, super::strike);
    }

    @Override protected String[] tensuraKeywords() { return new String[]{"soul", "spirit", "holy", "time", "gravity"}; }
    @Override protected int tensuraKitSize() { return 4; }

    // ================================================================ lines
    @Override protected String openingLine() { return "Another pack of ants climbing towards heaven. Very well. I will show you how far away it is."; }

    @Override
    protected String powerLine(int old, int now) {
        if (now >= 80 && old < 80) return "Eighty percent of Astaroth. The towers sing, the dead stand, and you are still here. Admirable. Pointless.";
        if (now >= PowerCurve.INFINITE_MANA_FROM && old < PowerCurve.INFINITE_MANA_FROM) return "Sixty percent. Time is not something I fight against. It is something I am allowed to hold.";
        if (now >= 40 && old < 40) return "Forty percent of the Supreme Time Devil. You are already slow.";
        return "I draw " + now + "% of Astaroth's power. Do try to keep up.";
    }

    @Override protected String gatesLine() { return "The Gates of the Tree of Qliphoth open. Behold the soul that wears many faces: a god, awake."; }
    @Override protected String deathLine() { return "So... this is how the story ends. Julius... did you win after all?"; }

    private static final String[] JULIUS_LINES = {
            "Lucius... enough. I can hold him only for a moment. Please, strike now!",
            "Forgive me, everyone. This war is mine to answer for. Go on, I will bear what I can.",
            "Do you see, Lucius? They do not stop. And neither will I.",
            "If I cannot save him, then let me at least end this with you. Together, one last time!"};

    // ================================================================ the fight's hooks
    @Override
    protected void onFightStart() {
        resetFightState();
        announce("Precognition: Lucius reads the next blow and dodges it, a few times. Strike together (a second attacker is out of sequence), hit him with anti-magic, or wait until his charges are spent.");
        announce("Two souls share this body: at 75%, 55%, 35% and 12% health the will of Julius Novachrono surfaces for 8 seconds. Strike him then.");
    }

    @Override
    protected void onFightEnd(boolean won) {
        if (level() instanceof ServerLevel sl) {
            SoulPaladins.releaseAll(this, sl);
            for (ZogratisCloneEntity c : clones()) c.fadeAway();
            releaseHeld(sl);
        }
        resetFightState();
    }

    private void resetFightState() {
        ChronoMoves.decelerate(this);
        AttributeInstance sp = getAttribute(Attributes.MOVEMENT_SPEED);
        if (sp != null) sp.removeModifier(JULIUS_SLOW);
        accelUntil = 0;
        flagStopping = false;
        julius.reset();
        precog.reset(power(), level().getGameTime());
        ring.clear();
        budget.reset();
        fallenClones = 0;
        hintedDown = false;
        hintedEmpty = false;
        pendingTower = null;
        stepMark = haltSpot = fieldMark = null;
        courtSpots = List.of();
        swapPartnerId = null;
        reverseStart = 0;
        bodySuppressedUntil = 0;
        juliusBar.removeAllPlayers();                                          // Julius's bar has watchers only while his will is up
        LuciusProfile.reset(this);
        if (entityData.get(JULIUS) != 0) entityData.set(JULIUS, 0);
        entityData.set(CHARGES, precog.charges());
        entityData.set(FLAGS, 0);
    }

    @Override
    protected void onPowerChanged(int old, int now) {
        precog.refill(now, level().getGameTime());
        entityData.set(CHARGES, precog.charges());
    }

    @Override
    protected void onGatesOpened() {
        announce("Lucius's soul awakens: three pairs of wings, a halo of gears, the grimoire of the towers open before him. His precognition and his time magic are at their peak.");
        precog.refill(PowerCurve.FULL, level().getGameTime());
        entityData.set(CHARGES, precog.charges());
    }

    // ================================================================ the tick
    @Override
    protected void bossTick() {
        if (!(level() instanceof ServerLevel sl)) return;
        try {
            long now = sl.getGameTime();
            if (!ready) firstTick(sl, now);
            if (isAlive() && !transforming() && tickCount % ChronoMath.SNAP_EVERY == 0) ring.add(now, getX(), getY(), getZ(), getHealth());
            if (transforming() && tickCount % 4 == 0) {
                Vec3 mid = position().add(0, getBbHeight() * 0.5, 0);
                LuciusFx.sparkle(sl, mid, 8, 1.0 + transformProgress());
                Tells.column(sl, position(), 2.0 + 4.0 * transformProgress(), LuciusFx.GOLD);
            }
            if (!fightActive() || !isAlive()) return;
            precog.tick(now, power());
            if (entityData.get(CHARGES) != precog.charges()) entityData.set(CHARGES, precog.charges());
            if (flagStopping && state() != STATE_CASTING) {                      // the Time Stop wind-up was cancelled (no target left): the sign goes out
                flagStopping = false;
                syncFlags();
            }
            if (!transforming()) updateJulius(sl, now);
            if (tickCount % 10 == 0) {
                paladinUpkeep(sl, now);
                if (form() == FORM_DIVINE) LuciusFx.sparkle(sl, position().add(0, getBbHeight() + 0.2, 0), 3, 0.5);
                if (precog.charges() > 0 && !julius()) Tells.gather(sl, position().add(0, getBbHeight() + 0.5, 0), 0.35, LuciusFx.GOLD, precog.charges() * 2);
            }
            if (now >= accelUntil && accelUntil != 0) {
                accelUntil = 0;
                ChronoMoves.decelerate(this);
                syncFlags();
            } else if (accelUntil != 0 && tickCount % 3 == 0) {
                LuciusFx.dust(sl, position().add(0, 1.0, 0), LuciusFx.GOLD, 1.3f, 3, 0.4);
            }
            if (tickCount % 20 == 0) bodyMends(sl, now);
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Lucius's tick failed: {}", e.toString());
        }
    }

    /** The first tick after the boss exists or was loaded: precognition full, saved state put back. */
    private void firstTick(ServerLevel sl, long now) {
        ready = true;
        precog.reset(power(), now);
        if (restoreMask >= 0) julius.restore(restoreMask, restoreLeft, now);
        if (restorePaladins != null) {
            for (int i = 0; i < restorePaladins.size(); i++) {
                CompoundTag t = restorePaladins.getCompound(i);
                if (!t.hasUUID("Id")) continue;
                UUID id = t.getUUID("Id");
                paladins.add(id, 8);
                if (t.getBoolean("Down")) paladins.down(id, now, Math.max(1, t.getInt("Rise")));
            }
            restorePaladins = null;
        }
        if (julius.active(now)) {
            entityData.set(JULIUS, Math.max(1, julius.left(now)));
            juliusBar.setName(Component.literal("Julius Novachrono surfaces - strike now!").withStyle(ChatFormatting.WHITE));
            juliusBar.setColor(BossEvent.BossBarColor.WHITE);
            juliusBar.setProgress(julius.fractionLeft(now));
            showJuliusBar(sl);
            AttributeInstance sp = getAttribute(Attributes.MOVEMENT_SPEED);
            if (sp != null) sp.addOrUpdateTransientModifier(new AttributeModifier(JULIUS_SLOW, -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        entityData.set(CHARGES, precog.charges());
    }

    /** His assimilated Body Magic: a slow mending of the flesh when he is hurt, which anti-magic stops for eight seconds. */
    private void bodyMends(ServerLevel sl, long now) {
        if (getHealth() >= getMaxHealth() * 0.9f || julius()) return;
        if (now < bodySuppressedUntil) {
            if (now - lastBodyNote > 240) {
                lastBodyNote = now;
                announce("Anti-magic stops Lucius's flesh from mending.");
            }
            return;
        }
        heal(getMaxHealth() * 0.0006f * (1 + PowerCurve.stage(power())));
    }

    // ================================================================ Julius
    private void updateJulius(ServerLevel sl, long now) {
        JuliusWill.Event ev = julius.update(getHealth() / getMaxHealth(), now, fightActive() && isAlive() && !transforming());
        if (ev == JuliusWill.Event.SURFACE) surface(sl, now);
        else if (ev == JuliusWill.Event.END) conceal(sl);
        if (!julius.active(now)) return;
        int left = julius.left(now);
        juliusBar.setProgress(julius.fractionLeft(now));
        if (left % 10 == 0) entityData.set(JULIUS, Math.max(1, left));
        if (tickCount % 4 == 0) {
            Vec3 mid = position().add(0, getBbHeight() * 0.6, 0);
            LuciusFx.sparkle(sl, mid, 4, 0.6);
            LuciusFx.dust(sl, mid, LuciusFx.WHITE, 1.2f, 3, 0.5);
        }
        if (left == JuliusWill.DURATION / 2) say(julius.lastIndex() == 3 ? "I am here, Lucius. I was always here." : "Hurry. He is almost back.");
    }

    /** Julius's will surfaces: Lucius stumbles, the bar is Julius's, the party is blessed. */
    private void surface(ServerLevel sl, long now) {
        entityData.set(JULIUS, JuliusWill.DURATION);
        AttributeInstance sp = getAttribute(Attributes.MOVEMENT_SPEED);
        if (sp != null) sp.addOrUpdateTransientModifier(new AttributeModifier(JULIUS_SLOW, -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        precog.suspend(now + JuliusWill.DURATION + 100);
        getNavigation().stop();
        juliusBar.setName(Component.literal("Julius Novachrono surfaces - strike now!").withStyle(ChatFormatting.WHITE));
        juliusBar.setColor(BossEvent.BossBarColor.WHITE);
        juliusBar.setProgress(1f);
        showJuliusBar(sl);
        refreshMainBar(sl);
        announce("The will of Julius Novachrono surfaces. Lucius stumbles: his spells fail, his copies fade, his precognition is gone, he takes 40% more damage for 8 seconds.");
        say(JULIUS_LINES[Math.max(0, Math.min(JULIUS_LINES.length - 1, julius.lastIndex()))]);
        Vec3 mid = position().add(0, getBbHeight() * 0.5, 0);
        Tells.burst(sl, mid, LuciusFx.WHITE, 60);
        LuciusFx.sparkle(sl, mid, 40, 1.2);
        LuciusFx.sound(sl, position(), SoundEvents.BEACON_POWER_SELECT, 3f, 0.8f);
        VfxSpawn.send(sl, VfxShape.TIME_REWIND, position().add(0, 0.05, 0), position().add(0, 1, 0), 0, 50, 2.2f);
        for (ZogratisCloneEntity c : clones()) c.fadeAway();                          // his copies waver and are gone while Julius holds the body
        List<ServerPlayer> party = new ArrayList<>(LuciusFx.players(foes(64)));
        for (UUID id : fighters()) if (sl.getPlayerByUUID(id) instanceof ServerPlayer p && p.isAlive() && !party.contains(p)) party.add(p);
        for (ServerPlayer p : party) bless(p);
    }

    /** The blessing of Julius: healed, cleansed, every spell remembered, the Time Stop cannot take them. */
    private void bless(ServerPlayer p) {
        for (MobEffectInstance i : new ArrayList<>(p.getActiveEffects())) {
            Holder<MobEffect> h = i.getEffect();
            if (h.value().getCategory() == MobEffectCategory.HARMFUL) p.removeEffect(h);
        }
        BalanceLaw.heal(p, p.getMaxHealth() * 0.12f);
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 0));
        TimeStop.immune(p, 240);
        LuciusMoves.remember(p);
        p.displayClientMessage(Component.literal("Julius's blessing is upon you.").withStyle(ChatFormatting.WHITE), true);
    }

    /** The window closes: Lucius is himself again. */
    private void conceal(ServerLevel sl) {
        AttributeInstance sp = getAttribute(Attributes.MOVEMENT_SPEED);
        if (sp != null) sp.removeModifier(JULIUS_SLOW);
        entityData.set(JULIUS, 0);
        juliusBar.removeAllPlayers();
        refreshMainBar(sl);
        say("Julius. Always so sentimental. Sleep now.");
        LuciusFx.sound(sl, position(), SoundEvents.BEACON_DEACTIVATE, 2f, 0.7f);
    }

    /** Puts everyone within 128 blocks on Julius's bar (it shows only to its watchers). */
    private void showJuliusBar(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(this) < 128 * 128) juliusBar.addPlayer(p);
    }

    /** Re-sends the main boss bar's title to everyone near (the framework refreshes it when a player starts to see the boss). */
    private void refreshMainBar(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(this) < 128 * 128) startSeenByPlayer(p);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        if (julius()) juliusBar.addPlayer(p);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        juliusBar.removePlayer(p);
    }

    // ================================================================ precognition and Julius in the damage path
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel sl)) return amount;
        try {
            long now = sl.getGameTime();
            boolean anti = LuciusFx.antiMagicHit(source);
            if (anti) {
                lastAntiMagic = now;
                bodySuppressedUntil = now + 160;
            }
            if (julius.active(now)) return amount * JULIUS_DAMAGE;
            if (exposed()) return amount;                                       // an ultimate leaves him open: precognition cannot save him now
            Entity attacker = source.getEntity();
            Precognition.Verdict v = precog.onHit(now, attacker == null ? -1 : attacker.getId(), anti, power());
            entityData.set(CHARGES, precog.charges());
            switch (v) {
                case DODGED -> {
                    dodge(sl, attacker);
                    return 0f;
                }
                case ANTI_MAGIC -> note(now, "Anti-magic lands through Lucius's precognition and burns a charge (" + precog.charges() + " left).");
                case OUT_OF_SEQUENCE -> note(now, "A second attacker is out of Lucius's sequence: the blow lands, and costs him nothing.");
                case EMPTY -> {
                    if (!hintedEmpty) {
                        hintedEmpty = true;
                        announce("Lucius's precognition is spent. Strike now: a charge returns in " + precog.ticksToCharge(now, power()) / 20 + " seconds.");
                    }
                }
                default -> { }
            }
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Lucius's precognition failed: {}", e.toString());
        }
        return amount;
    }

    private void note(long now, String text) {
        if (now - lastDodgeNote < 60) return;
        lastDodgeNote = now;
        announce(text);
    }

    /** He saw it coming: an afterimage where he stood, a step aside (if he is not casting), and a lash at the attacker. */
    private void dodge(ServerLevel sl, Entity attacker) {
        Vec3 here = position().add(0, 1, 0);
        LuciusFx.dust(sl, here, LuciusFx.GOLD, 1.6f, 16, 0.5);
        LuciusFx.sparkle(sl, here, 8, 0.4);
        VfxSpawn.send(sl, VfxShape.TIME_REWIND, position().add(0, 0.05, 0), position().add(0, 1, 0), 0, 12, 0.6f);
        LuciusFx.chime(sl, position(), 1.8f);
        if (state() != STATE_CASTING && state() != STATE_TRANSFORMING && attacker != null) {
            Vec3 away = LuciusFx.flatDir(attacker.position(), position(), getViewVector(1f));
            Vec3 side = new Vec3(-away.z, 0, away.x).scale(getRandom().nextBoolean() ? 2.6 : -2.6);
            Vec3 to = LuciusFx.standable(sl, this, position().add(side));
            if (to != null) blinkTo(to);
        }
        if (attacker instanceof ServerPlayer p) {
            p.displayClientMessage(Component.literal("Lucius read the blow (" + precog.charges() + " precognition charge" + (precog.charges() == 1 ? "" : "s") + " left).")
                    .withStyle(ChatFormatting.GOLD), true);
        }
        if (attacker instanceof LivingEntity victim && victim.isAlive() && isFoe(victim)) {
            later(2, LuciusFx.safe("time lash", () -> {
                if (!victim.isAlive()) return;
                if (strike(victim, LuciusTable.LASH, true)) victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            }));
        }
        if (precog.charges() == 0) quip("I saw that coming, as I see everything.", "You are a moment too late. You always will be.");
    }

    // ================================================================ Soul Paladins, once a second
    private void paladinUpkeep(ServerLevel sl, long now) {
        if (paladins.isEmpty()) return;
        List<UUID> due = paladins.due(now);
        List<LivingEntity> foes = null;
        for (UUID id : paladins.ids()) {
            if (!(sl.getEntity(id) instanceof Mob m) || !m.isAlive()) {
                paladins.remove(id);
                continue;
            }
            Vec3 body = m.position().add(0, m.getBbHeight() * 0.6, 0);
            if (paladins.stateOf(id) == PaladinLedger.State.DOWNED) {
                if (due.contains(id)) {
                    paladins.raise(id);
                    SoulPaladins.rise(m);
                    announce("A Soul Paladin rises again.");
                } else {
                    LuciusFx.beam(sl, body, position().add(0, getBbHeight() * 0.8, 0), LuciusFx.SOUL);
                    LuciusFx.souls(sl, body, 3, 0.3);
                }
                continue;
            }
            if (!m.hasEffect(ZogratisEffects.SOUL_PALADIN)) SoulPaladins.renew(m);
            LivingEntity tg = m.getTarget();
            if (tg == null || !tg.isAlive() || ZogratisEntities.isFamily(tg) || SoulPaladins.isPaladin(tg)) {
                if (foes == null) foes = foes(48);
                if (!foes.isEmpty()) m.setTarget(foes.get(0));
            }
            LuciusFx.dust(sl, body, LuciusFx.GOLD, 1.0f, 3, m.getBbWidth() * 0.4);
            LuciusFx.sparkle(sl, body, 1, 0.3);
        }
    }

    // ================================================================ moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (LuciusTable.Spec s : LuciusSpellTable.allSpecs()) {                            // his table, then Megiddo, Dimension Cutter and True Darkness
            Move.Builder mb = Move.builder(s.id(), s.label()).minPower(s.minPower()).windUp(s.windUpTicks()).cooldown(s.cooldownTicks())
                    .range(s.minRange(), s.maxRange()).weight(s.weight()).mana(s.manaCost()).recovery(s.recovery());
            if (s.ultimate()) mb.ultimate();
            if (s.crowd()) mb.crowd();
            Move.MoveTell tell = tellOf(s.id());
            if (tell != null) mb.tell(tell);
            final Move.MoveRun code = runOf(s.id());
            out.add(mb.run((boss, t) -> {
                if (fizzles()) return;
                code.run(boss, t);
            }));
        }
        return out;
    }

    /** While Julius holds the body, a spell does not come: the hand trembles, the circle fades. */
    private boolean fizzles() {
        if (!julius.active(level().getGameTime())) return false;
        announce("Lucius's hand trembles: Julius's will holds the spell back.");
        return true;
    }

    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case LuciusTable.SOUL_MOTES -> (boss, t) -> {
                LuciusMoves.soulMotes(this, t);
                quip("Souls are lighter than you think.", "Go on. Run. They follow.");
            };
            case LuciusTable.SOUL_DRAIN -> (boss, t) -> {
                LuciusMoves.soulDrain(this, t);
                quip("Lend me what you are not using.", "How warm. I will take it.");
            };
            case LuciusTable.LIGHT_STEP -> (boss, t) -> {
                ChronoMoves.lightStep(this, takeStep(t), LuciusTable.STEP);
                quip("Too slow.", "Light is only the fastest thing you know.");
            };
            case LuciusTable.TOWER_SPELL -> (boss, t) -> {
                TowerSpells.Entry e = pendingTower != null ? pendingTower : TowerSpells.draw(getRandom());
                pendingTower = null;
                if (getRandom().nextInt(4) == 0 && castTensura(t)) return;
                TowerSpells.cast(this, e, t, getRandom().nextInt(2));
                quip("Every spell ever cast is in my library.", "The towers remember what you have forgotten.");
            };
            case LuciusTable.BONE_ARSENAL -> (boss, t) -> {
                LuciusMoves.boneArsenal(this, t);
                quip("Zenon's bone. Dense, patient, merciless.");
            };
            case LuciusTable.MIRROR_COURT -> (boss, t) -> {
                int n = LuciusMoves.mirrorCourt(this, takeCourt(t));
                if (n > 0) announce("Lucius splits into " + n + " copies. Which one is real? Only his marks tell.");
                else LuciusMoves.soulMotes(this, t);
                quip("Which of us is real?", "Do not trust your eyes. They are the least reliable part of you.");
            };
            case LuciusTable.BLOOD_TIDE -> (boss, t) -> {
                LuciusMoves.bloodTide(this, t);
                quip("Vanica's blood, my hands.");
            };
            case LuciusTable.GRAVITY_CRUSH -> (boss, t) -> {
                LuciusMoves.gravityCrush(this, takeField(t));
                quip("Kneel. Gravity is a courtesy I extend to the small.");
            };
            case LuciusTable.SWAP_STRIKE -> (boss, t) -> {
                LuciusMoves.swapStrike(this, t, takeSwap(t), takeStep(t));
                quip("Not there. Here.");
            };
            case LuciusTable.MEMORY_SCRAMBLE -> (boss, t) -> {
                int n = LuciusMoves.memoryScramble(this);
                if (n > 0) announce("Memory Manipulation: " + n + (n == 1 ? " fighter forgets" : " fighters forget") + " the spell just cast. Anti-magic protects; Julius gives it back.");
                quip("What was the last thing you did? Think. It is gone.");
            };
            case LuciusTable.TIME_HALT -> (boss, t) -> {
                ChronoMoves.timeHalt(this, t, takeHalt(t), LuciusTable.HALT_BLOW);
                quip("A moment is all I need.");
            };
            case LuciusTable.ACCELERATE -> (boss, t) -> {
                ChronoMoves.accelerate(this);
                announce("Chrono Acceleration: Lucius is faster for 8 seconds, and has a precognition charge back.");
                quip("Let me show you how fast an hour can be.");
            };
            case LuciusTable.PALADIN_CALL -> (boss, t) -> {
                if (LuciusMoves.paladinCall(this) == 0) {
                    announce("There is no body here worth taking, and no room for another Paladin.");
                    LuciusMoves.soulMotes(this, t);
                }
                quip("Rise. You are mine now, and so you cannot die.");
            };
            case LuciusTable.TIME_REVERSAL -> (boss, t) -> {
                boolean refused = reverseStart > 0 && lastAntiMagic >= reverseStart;
                reverseStart = 0;
                ChronoMoves.timeReversal(this, refused);
                quip("Four seconds ago, you had not hurt me yet.");
            };
            case LuciusTable.RESURRECTION -> (boss, t) -> {
                if (!LuciusMoves.resurrection(this)) {
                    announce("There is no one for Lucius to raise.");
                    LuciusMoves.soulMotes(this, t);
                }
                quip("Death is an inconvenience I have learned to refuse.");
            };
            case LuciusTable.LIGHT_COMBO -> (boss, t) -> {
                float dmg = LuciusTable.STEP * 0.8f;
                ChronoMoves.lightStep(this, takeStep(t), dmg);
                ChronoMoves.comboNext(this, t, ChronoMath.comboSteps(power()) - 1, dmg);
                quip("Beyond the speed of light, there is only me.");
            };
            case LuciusTable.SOUL_PURGE -> (boss, t) -> {
                LuciusMoves.soulPurge(this);
                say("Be cleansed of the devils that lend you their strength.");
            };
            case LuciusTable.SIBLINGS -> (boss, t) -> LuciusMoves.siblings(this, t, takeField(t));
            case LuciusTable.TIME_STOP -> (boss, t) -> {
                flagStopping = false;
                syncFlags();
                ChronoMoves.timeStop(this, t);
                say("The world will wait while I work.");
            };
            case LuciusTable.TOWER_STORM -> (boss, t) -> {
                TowerSpells.storm(this, t, 6 + PowerCurve.stage(power()));
                say("Every tower in the kingdom, answer me.");
            };
            default -> LuciusSpells.run(this, id);
        };
    }

    // ---------------------------------------------------------------- marks: where the big moves land
    private Vec3 takeStep(LivingEntity target) {
        Vec3 m = stepMark != null ? stepMark : ChronoMoves.planStep(this, target);
        stepMark = null;
        return m;
    }

    private Vec3 takeHalt(LivingEntity target) {
        Vec3 m = haltSpot != null ? haltSpot : ZogratisBoss.ground(level(), target.position());
        haltSpot = null;
        return m;
    }

    private Vec3 takeField(LivingEntity target) {
        Vec3 m = fieldMark != null ? fieldMark : ZogratisBoss.ground(level(), target.position());
        fieldMark = null;
        return m;
    }

    private List<Vec3> takeCourt(LivingEntity target) {
        List<Vec3> m = courtSpots.isEmpty() ? LuciusMoves.courtSpots(this, target) : courtSpots;
        courtSpots = List.of();
        return m;
    }

    private ZogratisCloneEntity takeSwap(LivingEntity target) {
        ZogratisCloneEntity c = null;
        if (swapPartnerId != null && level() instanceof ServerLevel sl && sl.getEntity(swapPartnerId) instanceof ZogratisCloneEntity found && found.isAlive()) c = found;
        if (c == null) c = LuciusMoves.swapPartner(this, target);
        swapPartnerId = null;
        return c;
    }

    // ================================================================ telegraphs
    /** The first call of a wind-up (the tell runs every third tick, so the first is within three ticks of the start). */
    private static boolean first(int left, int total) { return left >= total - 3; }

    private Move.MoveTell tellOf(String id) {
        return switch (id) {
            case LuciusTable.SOUL_MOTES -> (boss, t, left, total) -> {
                if (!(level() instanceof ServerLevel sl)) return;
                Vec3 hand = position().add(0, getBbHeight() * 0.75, 0);
                Tells.gather(sl, hand, 0.6, LuciusFx.SOUL, 6);
                Tells.line(sl, hand, t.getBoundingBox().getCenter(), LuciusFx.SOUL);
            };
            case LuciusTable.SOUL_DRAIN -> (boss, t, left, total) -> tellDrain(t);
            case LuciusTable.LIGHT_STEP, LuciusTable.LIGHT_COMBO -> (boss, t, left, total) -> {
                if (!(level() instanceof ServerLevel sl)) return;
                if (first(left, total)) stepMark = ChronoMoves.planStep(this, t);
                if (stepMark != null) ChronoMoves.tellStep(this, sl, stepMark);
            };
            case LuciusTable.TOWER_SPELL -> (boss, t, left, total) -> tellTower(t, left, total);
            case LuciusTable.BONE_ARSENAL -> (boss, t, left, total) -> {
                if (!(level() instanceof ServerLevel sl)) return;
                Vec3 sh = position().add(0, getBbHeight() * 0.8, 0);
                Tells.gather(sl, sh, 0.9, 0xE9E4D4, 8);
                Tells.line(sl, sh, t.getBoundingBox().getCenter(), 0xE9E4D4);
            };
            case LuciusTable.MIRROR_COURT -> (boss, t, left, total) -> {
                if (!(level() instanceof ServerLevel sl)) return;
                if (first(left, total)) courtSpots = LuciusMoves.courtSpots(this, t);
                for (Vec3 s : courtSpots) {
                    Tells.column(sl, s, 2.4, LuciusFx.GOLD);
                    Tells.ring(sl, s, 1.1, LuciusFx.PALE);
                }
                Tells.gather(sl, position().add(0, 1.0, 0), 0.8, LuciusFx.PALE, 8);
            };
            case LuciusTable.BLOOD_TIDE -> (boss, t, left, total) -> {
                if (!(level() instanceof ServerLevel sl)) return;
                Vec3 sh = position().add(0, getBbHeight() * 0.8, 0);
                Tells.gather(sl, sh, 0.8, 0xB0121E, 8);
                Tells.line(sl, sh, t.getBoundingBox().getCenter(), 0xB0121E);
            };
            case LuciusTable.GRAVITY_CRUSH -> (boss, t, left, total) -> tellField(t, left, total, 9 + PowerCurve.stage(power()), 0x5B2A8C);
            case LuciusTable.SWAP_STRIKE -> (boss, t, left, total) -> tellSwap(t, left, total);
            case LuciusTable.MEMORY_SCRAMBLE -> (boss, t, left, total) -> {
                if (!(level() instanceof ServerLevel sl)) return;
                Vec3 eye = position().add(0, getBbHeight() * 0.85, 0);
                for (LivingEntity f : foes(40)) {
                    Tells.line(sl, eye, f.getEyePosition(), LuciusFx.PALE);
                    Tells.ring(sl, f.position(), 0.9, LuciusFx.GOLD, 1.1f, 16);
                }
            };
            case LuciusTable.TIME_HALT -> (boss, t, left, total) -> {
                if (!(level() instanceof ServerLevel sl)) return;
                if (first(left, total)) {
                    haltSpot = ZogratisBoss.ground(sl, t.position());
                    VfxSpawn.send(sl, VfxShape.TIME_CLOCK, haltSpot, haltSpot.add(0, 5, 0), 0, total, 2.5f);
                }
                if (haltSpot != null) Tells.ring(sl, haltSpot, ChronoMath.BLOW_RADIUS, LuciusFx.GOLD, 1.2f, 32);
            };
            case LuciusTable.ACCELERATE -> (boss, t, left, total) -> {
                if (!(level() instanceof ServerLevel sl)) return;
                if (first(left, total)) VfxSpawn.sendFollowing(sl, VfxShape.TIME_ACCEL, this, position().add(0, 1, 0), 0, total, 1.0f);
                Tells.gather(sl, position().add(0, 1.0, 0), 0.8, LuciusFx.GOLD, 8);
            };
            case LuciusTable.PALADIN_CALL -> (boss, t, left, total) -> {
                if (!(level() instanceof ServerLevel sl)) return;
                Vec3 chest = position().add(0, getBbHeight() * 0.8, 0);
                int n = 0;
                for (Mob m : sl.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(40), SoulPaladins::eligible)) {
                    if (++n > SoulMath.paladinCap(power())) break;
                    Tells.line(sl, chest, m.position().add(0, m.getBbHeight() * 0.6, 0), LuciusFx.PALE);
                    Tells.ring(sl, m.position(), 1.4, LuciusFx.GOLD, 1.2f, 20);
                }
                Tells.gather(sl, chest, 0.9, LuciusFx.PALE, 8);
            };
            case LuciusTable.TIME_REVERSAL -> (boss, t, left, total) -> {
                if (!(level() instanceof ServerLevel sl)) return;
                if (first(left, total)) {
                    reverseStart = sl.getGameTime();
                    VfxSpawn.send(sl, VfxShape.TIME_REWIND, position().add(0, 0.05, 0), position().add(0, 1, 0), 0, total, 1.6f);
                    announce("Time Reversal: Lucius will return to the spot marked in gold. Anti-magic now refuses it.");
                }
                ChronoRing.Snap s = ChronoMoves.rewindTarget(this, sl.getGameTime());
                if (s != null) {
                    Vec3 to = new Vec3(s.x(), s.y(), s.z());
                    Tells.ring(sl, to, 1.4, LuciusFx.GOLD, 1.3f, 24);
                    Tells.column(sl, to, 3.0, LuciusFx.GOLD);
                }
            };
            case LuciusTable.RESURRECTION -> (boss, t, left, total) -> {
                if (!(level() instanceof ServerLevel sl)) return;
                for (UUID pid : paladins.downedIds()) {
                    if (sl.getEntity(pid) instanceof Mob m) {
                        Tells.ring(sl, m.position(), 1.6, LuciusFx.GOLD);
                        LuciusFx.souls(sl, m.position().add(0, 0.5, 0), 4, 0.3);
                    }
                }
                LuciusFx.souls(sl, position().add(0, 1, 0), 6, 1.0);
                Tells.ring(sl, position(), 4.0, LuciusFx.PALE);
            };
            case LuciusTable.SOUL_PURGE -> (boss, t, left, total) -> {
                if (!(level() instanceof ServerLevel sl)) return;
                if (first(left, total)) announce("Soul Purge: a wave of white and gold crosses the ground out to " + LuciusMoves.PURGE_RADIUS
                        + " blocks. Jump over it. It strips devil power, and the Dark Triad's.");
                Tells.ring(sl, position(), Math.min(LuciusMoves.PURGE_RADIUS, 4 + (total - left) * 0.5), LuciusFx.GOLD);
                if (left % 6 == 0) Tells.ring(sl, position(), LuciusMoves.PURGE_RADIUS, LuciusFx.WARN, 1.2f, 96);
                Tells.gather(sl, position().add(0, 1.2, 0), 1.2, LuciusFx.PALE, 10);
            };
            case LuciusTable.SIBLINGS -> (boss, t, left, total) -> tellSiblings(t, left, total);
            case LuciusTable.TIME_STOP -> (boss, t, left, total) -> tellStop(t, left, total);
            case LuciusTable.TOWER_STORM -> (boss, t, left, total) -> {
                if (!(level() instanceof ServerLevel sl)) return;
                if (first(left, total)) {
                    VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, position().add(0, 0.05, 0), position().add(0, 1, 0), LuciusFx.GOLD_ARGB, total, 7f);
                    announce("Grimoire Tower Storm: the towers answer, one magic after another. Every impact is marked on the ground for a second first.");
                }
                Tells.ring(sl, position(), 6 + (total - left) * 0.2, LuciusFx.GOLD);
                Tells.gather(sl, position().add(0, 1.5, 0), 1.5, LuciusFx.PALE, 10);
            };
            default -> LuciusSpells.tell(this, id);
        };
    }

    private void tellDrain(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 chest = position().add(0, getBbHeight() * 0.7, 0);
        int n = 0;
        for (LivingEntity f : foes(22)) {
            if (++n > 3) break;
            Tells.line(sl, chest, f.getBoundingBox().getCenter(), LuciusFx.SOUL);
            Tells.ring(sl, f.position(), 1.0, LuciusFx.SOUL, 1.1f, 16);
        }
        Tells.gather(sl, chest, 0.8, LuciusFx.SOUL, 6);
    }

    private void tellTower(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total) || pendingTower == null) {
            pendingTower = TowerSpells.draw(getRandom());
            TowerSpells.announce(this, pendingTower);
            announce(pendingTower.announcement());
            VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, position().add(0, 0.05, 0), position().add(0, 1, 0), pendingTower.colorArgb(), total, 1.4f);
        }
        Tells.gather(sl, position().add(0, 1.4, 0), 0.9, pendingTower.rgb(), 8);
        Tells.ring(sl, target.position(), 1.5 + left * 0.08, pendingTower.rgb());
    }

    /** A ring at the spot the target stands on when the wind-up begins; it stays there, so moving off it is the answer. */
    private void tellField(LivingEntity target, int left, int total, double radius, int rgb) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) fieldMark = ZogratisBoss.ground(sl, target.position());
        if (fieldMark == null) return;
        Tells.ring(sl, fieldMark, radius, rgb, 1.4f, (int) Math.min(80, 14 + radius * 5));
        Tells.ring(sl, fieldMark, Math.max(0.6, radius * left / Math.max(1, total)), LuciusFx.PALE, 1.1f, 28);
        Tells.column(sl, fieldMark, 4.0, LuciusFx.PALE);
    }

    private void tellSwap(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            stepMark = null;                                       // nothing left over from a light step that was never cast
            ZogratisCloneEntity c = LuciusMoves.swapPartner(this, target);
            swapPartnerId = c == null ? null : c.getUUID();
            if (c != null) {
                c.mark(total + 4);
                announce("Lucius will trade places with the copy marked in gold.");
            }
        }
        if (swapPartnerId != null && sl.getEntity(swapPartnerId) instanceof ZogratisCloneEntity c) {
            Tells.line(sl, position().add(0, 1.4, 0), c.position().add(0, 1.4, 0), LuciusFx.GOLD);
            Tells.ring(sl, c.position(), 5.0, LuciusFx.WARN, 1.2f, 40);
        } else {                                                    // no copy to trade with: the move is a light step, and its strip stays on the ground
            if (first(left, total) || stepMark == null) stepMark = ChronoMoves.planStep(this, target);
            ChronoMoves.tellStep(this, sl, stepMark);
        }
        Tells.ring(sl, position(), 2.0, LuciusFx.GOLD, 1.1f, 20);
    }

    private void tellSiblings(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            fieldMark = ZogratisBoss.ground(sl, target.position());
            announce("Sibling Assimilation: gravity falls on the violet ring at once, then bone, then blood, then flesh. Leave the ring.");
        }
        double progress = (total - left) / (double) Math.max(1, total);
        if (fieldMark != null) {
            double r = 11 + PowerCurve.stage(power()) * 0.5;
            Tells.ring(sl, fieldMark, r, 0x5B2A8C, 1.5f, (int) Math.min(88, 14 + r * 5));
            Tells.column(sl, fieldMark, 5.0, 0xB58CFF);
        }
        if (progress > 0.33) Tells.ring(sl, position(), 3.0 + progress * 3, 0xE9E4D4, 1.3f, 32);
        if (progress > 0.6) {
            Vec3 chest = position().add(0, 1.4, 0);
            for (LivingEntity f : foes(40)) Tells.line(sl, chest, f.getBoundingBox().getCenter(), 0xB0121E);
        }
        Tells.gather(sl, position().add(0, 1.2, 0), 1.0, LuciusFx.PALE, 8);
    }

    private void tellStop(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            flagStopping = true;
            syncFlags();
            int n = 0;
            for (LivingEntity f : foes(44)) {
                if (++n > ChronoMath.MAX_HELD) break;
                VfxSpawn.send(sl, VfxShape.TIME_CLOCK, f.position(), f.position().add(0, 5, 0), 0, total, 1.6f);
            }
            LuciusFx.bar(sl, position(), 64, Component.literal("TIME WILL STOP: hold anti-magic or break his line of sight. Nothing hurts while stopped; the blows after are marked.")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
        Vec3 chest = position().add(0, getBbHeight() * 0.8, 0);
        for (LivingEntity f : foes(44)) {
            boolean held = ChronoMoves.holdable(this, f);
            Tells.ring(sl, f.position(), 1.3, held ? LuciusFx.GOLD : LuciusFx.WHITE, 1.2f, 20);
            if (held) Tells.line(sl, chest, f.getBoundingBox().getCenter(), LuciusFx.PALE);
        }
        Tells.ring(sl, position(), 3.0 + (total - left) * 0.12, LuciusFx.GOLD);
    }

    // ================================================================ death and removal
    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            juliusBar.removeAllPlayers();
            if (reason.shouldDestroy() && level() instanceof ServerLevel sl) {
                SoulPaladins.releaseAll(this, sl);
                for (ZogratisCloneEntity c : clones()) c.fadeAway();
                releaseHeld(sl);
                ChronoMoves.decelerate(this);
            }
        }
        super.remove(reason);
    }

    // ================================================================ save data
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        long now = level().getGameTime();
        tag.putInt("LJuliusMask", julius.armedMask());
        tag.putInt("LJuliusLeft", julius.left(now));
        tag.putInt("LBudget", budget.used());
        tag.putInt("LFallen", fallenClones);
        ListTag list = new ListTag();
        for (UUID id : paladins.ids()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", id);
            boolean down = paladins.stateOf(id) == PaladinLedger.State.DOWNED;
            t.putBoolean("Down", down);
            t.putInt("Rise", paladins.riseIn(id, now));
            list.add(t);
        }
        tag.put("LPaladins", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("LJuliusMask")) {
            restoreMask = tag.getInt("LJuliusMask");
            restoreLeft = tag.getInt("LJuliusLeft");
        }
        budget.restore(tag.getInt("LBudget"));
        fallenClones = Math.max(0, Math.min(6, tag.getInt("LFallen")));
        restorePaladins = tag.getList("LPaladins", Tag.TAG_COMPOUND);
        ready = false;
    }
}
