package com.newuniverse.nusmp.entity.riven;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.entity.NUEntities;
import com.newuniverse.nusmp.item.NUItems;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Riven Remake, "The Black Bulls' Bard" (alias Fictional Remake): a raid boss that threat-scans each target and counter-picks from the Anime Skill
 * Codex (see {@link RivenBrain}, {@link ThreatScan}, {@link KillPlan}). Phases: 1 (100-70%), 2 (70-30%: Story Manifestation, a story-charge row),
 * 3 (below 30%, the emotional high: every skill one tier up, lethal plans x1.5, Final Form, two skills may chain). Anti-magic staggers him and
 * forces a replan. Start with {@code /multiverse boss riven} or the spawn egg. Zagred is untouched and shares no state with him.
 */
public class RivenBossEntity extends Monster {
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(RivenBossEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bar = new ServerBossEvent(Component.literal("Riven Remake"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private final RivenBrain brain = new RivenBrain(this);
    private final RivenPassives passives = new RivenPassives(this);
    private boolean untargetable, riftOpen, aggroSeen;
    private final Set<UUID> fighters = new HashSet<>();
    private BlockPos arena;
    private float charge = 40f;
    private int scaledFor = 1;
    long busyUntil, staggerUntil;
    private String clip = "", skill = "";
    // client copies (RivenStatePayload)
    private int clientPhase = 1, clientCharge;
    private String clientClip = "", clientSkill = "";
    private int clientClipTick;

    public RivenBossEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 400;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 600).add(Attributes.ATTACK_DAMAGE, 6).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 48).add(Attributes.ARMOR, 4).add(Attributes.KNOCKBACK_RESISTANCE, 0.6).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(PHASE, 1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }

    RivenPassives passives() { return passives; }
    BlockPos arenaPos() { return arena != null ? arena : blockPosition(); }
    boolean isStaggered(long t) { return t < staggerUntil; }
    void replan() { brain.abort(); }
    void setUntargetable(boolean b) { untargetable = b; }
    boolean canRewriteNow() { ThreatScan sc = brain.scan(); return sc == null || passives.canRewrite(sc); }

    /** Unbelieved: a target he cannot rewrite is told by a bark, and he replans. */
    void unbelieved(ServerLevel sl, LivingEntity t) {
        say(sl, "...You're not in my story. Fine. Different page.");
        brain.abort();
    }

    /** Unwritten Ending: the grimoire closes and he cannot be targeted for 1.5 s (the final_form clip plays). */
    void beginUnwrittenEnding() {
        setUntargetable(true);
        brain.abort();
        staggerUntil = tickCount + 30;
        setClip("final_form", "Unwritten Ending");
        if (level() instanceof ServerLevel sl) say(sl, "The grimoire closes.");
    }

    public int phase() { return entityData.get(PHASE); }
    public void setArena(BlockPos p) { arena = p.immutable(); }

    // ---------------------------------------------------------------- client copies
    public int clientPhase() { return clientPhase; }
    public int clientCharge() { return clientCharge; }
    public String clientClip() { return clientClip; }
    public String clientSkill() { return clientSkill; }
    public int clientClipAge() { return tickCount - clientClipTick; }

    public void applyState(int phase, String clip, String skill, int charge) {
        if (!clip.equals(clientClip) || !skill.equals(clientSkill)) clientClipTick = tickCount;
        clientPhase = phase; clientClip = clip; clientSkill = skill; clientCharge = charge;
    }

    // ---------------------------------------------------------------- story charge
    boolean canAfford(float cost) { return charge >= cost; }

    boolean spendCharge(float cost) {
        if (charge < cost) return false;
        charge -= cost;
        return true;
    }

    /** Sets the clip (animation name) and the skill name shown on the bar; "" clears them. Sent to everyone tracking him. */
    void setClip(String c, String s) {
        if (c.equals(clip) && s.equals(skill)) return;
        clip = c; skill = s;
        sync();
    }

    private void sync() {
        if (level().isClientSide) return;
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(this, new RivenStatePayload(getId(), phase(), clip, skill, Math.round(charge)));
    }

    void say(ServerLevel sl, String line) {
        Component c = Component.literal("Riven Remake: ").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD).append(Component.literal("\"" + line + "\"").withStyle(ChatFormatting.AQUA));
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(this) < 64 * 64) p.sendSystemMessage(c);
    }

    void announce(ServerLevel sl, String skillName) { setClip(clip, skillName); }

    // ---------------------------------------------------------------- the fight
    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        bar.addPlayer(p);
        sync();
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) return;
        if (arena == null) arena = blockPosition();
        long t = tickCount;
        float frac = getHealth() / getMaxHealth();
        int ph = frac > 0.7f ? 1 : frac > 0.3f ? 2 : 3;
        if (ph != phase()) enterPhase(sl, ph, t);
        bar.setProgress(frac);
        bar.setName(Component.literal("Riven Remake  \u00b7  " + (ph == 1 ? "The Black Bulls' Bard" : ph == 2 ? "Fictional Remake" : "Final Form")).withStyle(ChatFormatting.DARK_PURPLE));
        if (t % 6 == 0) charge = Math.min(100f, charge + (ph >= 2 ? 0.8f : 0.5f) * passives.chargeGain(sl, t));
        if (t % 20 == 0) { sync(); scaleForPlayers(sl); }
        if (distanceToSqr(Vec3.atBottomCenterOf(arena)) > 48 * 48) {                         // leash: he does not spawn-camp or wander off
            teleportTo(arena.getX() + 0.5, arena.getY(), arena.getZ() + 0.5);
            getNavigation().stop();
        }
        if (t == 1) passives.grantUpTo(phase());
        LivingEntity tg = getTarget();
        if (!aggroSeen && tg != null) { aggroSeen = true; collapseRift(sl); }
        if (riftOpen && t % 10 == 0) arenaBarrier(sl);
        passives.tick(sl, t, tg);
        passives.jack(sl, t, brain.scan());
        if (untargetable || t < staggerUntil) { getNavigation().stop(); return; }
        brain.tick(sl, t);
    }

    private void scaleForPlayers(ServerLevel sl) {
        Vec3 c = Vec3.atBottomCenterOf(arena);
        int n = Math.min(5, Math.max(1, sl.getPlayers(p -> p.distanceToSqr(c) < 48 * 48 && !p.isCreative() && !p.isSpectator()).size()));
        var attr = getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;
        double want = RivenConfig.BASE_HEALTH.get() + (double) RivenConfig.PER_PLAYER_HEALTH.get() * (Math.max(n, scaledFor) - 1);
        if (Math.abs(attr.getBaseValue() - want) > 0.5) {
            float frac = getHealth() / getMaxHealth();
            attr.setBaseValue(want);
            setHealth((float) (want * frac));
        }
        scaledFor = Math.max(scaledFor, n);
    }

    private void enterPhase(ServerLevel sl, int ph, long t) {
        entityData.set(PHASE, ph);
        brain.abort();
        passives.grantUpTo(ph);
        if (ph == 3) passives.emotionalHigh(t);
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, position().add(0, 1.4, 0), position(), 0xFFFFC94A, 40, 1.4f);   // the Black Bulls' emblem cracks
        if (ph == 2) {
            say(sl, "I might not be the strongest, but I'll make sure my story hits harder than anyone expected.");
            busyUntil = t + 28;
            setClip("phase2", "Phase II");
        } else if (ph == 3) {
            say(sl, "It's not just a game. It's my other life.");
            busyUntil = t + 44;
            charge = 100f;
            passives.finalFormReset();
            passives.doomsGate(sl);
            setClip("final_form", "???");
        }
        VfxSpawn.sendFollowing(sl, VfxShape.LIGHTNING_FIEND, this, position(), RivenAttacks.BLUE_VIOLET, 60, 2f);
        sl.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2f, 1.4f);
        sync();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && source.getEntity() instanceof ServerPlayer p) {
            fighters.add(p.getUUID());
            if (AntiMagic.lord(p).isPresent() && tickCount >= staggerUntil + 60) {            // anti-magic staggers him and forces a replan; he can still Shadow Step out
                staggerUntil = tickCount + 16;
                brain.abort();
                setClip("stagger", "Staggered");
                if (level() instanceof ServerLevel sl) sl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.2f, 1.2f);
            }
        }
        if (!level().isClientSide) {
            if (untargetable && tickCount >= staggerUntil) setUntargetable(false);
            float out = passives.damage(source, amount, tickCount);
            if (out < 0f) return false;
            amount = out;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        passives.denyPlunder();
        passives.clear();
        riftOpen = false;
        super.die(source);
        if (!(level() instanceof ServerLevel sl)) return;
        setClip("death", "");
        say(sl, "Looks like this chapter closes here... good story, though.");
        for (UUID id : fighters) {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(id);
            if (p == null) continue;
            ItemStack relic = new ItemStack(NUItems.BLACK_BULL_BARD_RELIC.get());
            if (!p.getInventory().add(relic)) p.drop(relic, false);
        }
    }

    /** Tensura resistances shorten harmful effects instead of nullifying them; his own script effects (beneficial) pass untouched. */
    @Override
    public boolean addEffect(MobEffectInstance e, net.minecraft.world.entity.Entity cause) {
        if (!level().isClientSide && e.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
            e = new MobEffectInstance(e.getEffect(), passives.effectDuration(e), e.getAmplifier(), e.isAmbient(), e.isVisible(), e.showIcon());
        }
        return super.addEffect(e, cause);
    }

    /** Pain Nullification (Final Form only): no knockback. Below that he flinches like anyone else. */
    @Override
    public void knockback(double strength, double x, double z) {
        if (phase() >= 3 && !level().isClientSide) return;
        super.knockback(strength, x, z);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok && target instanceof LivingEntity le) com.newuniverse.nusmp.item.RivenEngravings.onHit(this, le, getMainHandItem(), passives.isBondTarget(le), tickCount);
        return ok;
    }

    @Override
    public boolean killedEntity(ServerLevel sl, LivingEntity victim) {
        if (victim instanceof ServerPlayer p) passives.logKill(p, brain.planName());            // Page Memory: the one passive that writes to the debug log
        return super.killedEntity(sl, victim);
    }

    /** The forest rift: a vertical gold story-rift in the treeline. Particles only, no dimension. */
    void openRift(ServerLevel sl) {
        riftOpen = true;
        Vec3 at = position().add(0, 1.4, 0);
        VfxSpawn.send(sl, VfxShape.SPATIAL_RIFT, at, at, 0xFFFFC94A, 80, 2.4f);
    }

    /** On aggro the rift collapses into the arena barrier (a particle ring) and the page-shards shimmer. */
    private void collapseRift(ServerLevel sl) {
        riftOpen = true;
        VfxSpawn.send(sl, VfxShape.SPACE_PORTAL, position().add(0, 1.2, 0), position(), 0xFFFFC94A, 50, 2.2f);
        sl.playSound(null, blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.4f, 1.1f);
    }

    private void arenaBarrier(ServerLevel sl) {
        Vec3 c = Vec3.atBottomCenterOf(arenaPos());
        double a = (tickCount % 360) * Math.PI / 18;
        for (int i = 0; i < 6; i++) {
            double ang = a + i * Math.PI / 3;
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, c.x + Math.cos(ang) * 40, c.y + 1 + (i % 3), c.z + Math.sin(ang) * 40, 1, 0, 0.4, 0, 0.01);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (arena != null) tag.put("Arena", NbtUtils.writeBlockPos(arena));
        tag.putFloat("Charge", charge);
        passives.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Arena").ifPresent(p -> arena = p);
        passives.load(tag);
        charge = tag.contains("Charge") ? tag.getFloat("Charge") : 40f;
        float frac = getHealth() / getMaxHealth();
        entityData.set(PHASE, frac > 0.7f ? 1 : frac > 0.3f ? 2 : 3);
        if (hasCustomName()) bar.setName(getDisplayName());
    }

    /** Summons the boss at 'at' (the command and the spawn egg both end up as a normal entity). */
    public static RivenBossEntity summon(ServerLevel sl, Vec3 at) {
        RivenBossEntity r = NUEntities.RIVEN_REMAKE.get().create(sl);
        if (r == null) return null;
        r.moveTo(at.x, at.y, at.z, 0, 0);
        r.setArena(BlockPos.containing(at));
        sl.addFreshEntity(r);
        r.passives.grantUpTo(1);
        r.openRift(sl);
        VfxSpawn.send(sl, VfxShape.SPACE_PORTAL, at.add(0, 1.2, 0), at, RivenAttacks.BLUE_VIOLET, 60, 2f);
        sl.playSound(null, r.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2f, 0.7f);
        return r;
    }
}
