package com.newuniverse.nusmp.entity.riven;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.antimagic.NihilityZone;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.NUEntities;
import com.newuniverse.nusmp.entity.TensuraCaster;
import com.newuniverse.nusmp.entity.ZagredBossEntity;
import com.newuniverse.nusmp.grimoire.CanonBook;
import com.newuniverse.nusmp.skill.codex.AnimeSkill;
import com.newuniverse.nusmp.skill.codex.AnimeSkillCodex;
import com.newuniverse.nusmp.skill.NUSkills;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Marquis Remake, the Black Bulls' Bard: an adaptive raid boss. A utility brain ({@link RivenBrain}) rescans the target
 * ({@link ThreatScan}), scores every codex skill ({@link KillPlan}) and casts the best one ({@link RivenAttacks}). Four phases by
 * health: I The Black Bulls' Bard, II Fictional Remake, III Rift, and IV Final Form. Quirk: he rewrites the fight, but not for a player who does not
 * believe: anti-magic, Nihility or a raised guard resists the rewrite unless he spends story charge.
 */
public class RivenBossEntity extends Monster {
    private static final Logger LOG = LogUtils.getLogger();

    public static final int STATE_IDLE = 0, STATE_COMBAT = 1, STATE_CASTING = 2, STATE_STAGGER = 3;
    public static final float STORY_COST = 25f;
    /** Animation clips by id; the id is synced, the client plays the clip of the same name (see {@link RivenClips}). */
    public static final String[] CLIPS = RivenClips.NAMES;
    private static final int[] CLIP_TICKS = RivenClips.TICKS;

    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(RivenBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CLIP = SynchedEntityData.defineId(RivenBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CLIP_SEQ = SynchedEntityData.defineId(RivenBossEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bar = new ServerBossEvent(Component.literal("Marquis Remake"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private final RivenBrain brain = new RivenBrain(this);
    private final Set<UUID> fighters = new HashSet<>();
    private BlockPos arena;
    private AnimeSkill casting, forced, lastCast;
    private LivingEntity castTarget;
    private long castAt, nextThink, nextBark, clipEnd, stagger, transitionUntil, guardUntil, songUntil, hexUntil, bondUntil, nextSync, nextZagredBanter;
    private UUID hexTarget, bondTarget;
    private UUID lastPlayerAttacker;
    private float story = 20f, recentDamage;
    private int chainLeft, lastPlayers = 1, clientClipStart;
    private boolean opened, healedOnScale;
    private boolean initializedExistence;
    private float calmDamage;
    private int boredSeconds;
    private long portalAt, portalReadyAt, phaseIFramesUntil, phaseTwoAt;
    private long nextSignatureAt;
    private long nextNewOrderAt;
    private String nullType = "";
    private long nullUntil;
    private boolean phaseTwoMagicNullStarted, finalNullStarted;
    private boolean rewriteUsed, rewritePending, riftOpen;
    private long riftUntil, nextRiftPull, unwrittenUntil, audienceReadyAt, finalPageReadyAt;
    private long rewriteUntil;
    private UUID soulNoteTarget;
    private float soulNoteDamage;
    private long soulNotePayoutAt, bullWardUntil;
    private final Set<net.minecraft.resources.ResourceLocation> bossBoundSkills = new LinkedHashSet<>();
    private long fightTicks, nextCombatRoll = 200, compressAt, phaseThreeAt, statusUntil;
    private String combatType = "Caster", drawnGrimoire = "", statusText = "";
    private boolean newOrderUsed, compressUsed, creatorUsed;
    private int evilEyePhase;
    private int kineticCharge;
    private long faJinExpires;
    private boolean avatarPending;
    private int avatarScheduledPhase;
    private long avatarSpawnAt;

    public RivenBossEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        setPersistenceRequired();
    }

    boolean faJinReady() {
        if (tickCount >= faJinExpires) kineticCharge = 0;
        return kineticCharge >= 5;
    }

    void addFaJinCharge() {
        if (tickCount >= faJinExpires) kineticCharge = 0;
        kineticCharge = Math.min(5, kineticCharge + 1);
        faJinExpires = tickCount + 200;
    }

    void clearFaJin() {
        kineticCharge = 0;
        faJinExpires = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1200).add(Attributes.ATTACK_DAMAGE, 10).add(Attributes.ATTACK_KNOCKBACK, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.30).add(Attributes.FOLLOW_RANGE, 48).add(Attributes.ARMOR, 6).add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(PHASE, 1);
        b.define(CLIP, 0);
        b.define(CLIP_SEQ, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new RivenMoveGoal());
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ---------------------------------------------------------------- accessors the brain and attacks use
    public int phase() { return entityData.get(PHASE); }
    public int clip() { return entityData.get(CLIP); }
    public int clipStartTick() { return clientClipStart; }
    public boolean signatureCasting() { return RivenCombat.signature(casting); }
    public String drawnGrimoire() { return drawnGrimoire; }
    public LivingEntity lastPlayerAttacker() {
        if (lastPlayerAttacker == null || !(level() instanceof ServerLevel sl)) return null;
        Entity attacker = sl.getEntity(lastPlayerAttacker);
        return attacker instanceof LivingEntity living && living.isAlive() ? living : null;
    }
    public float story() { return story; }
    AnimeSkill lastCastSkill() { return lastCast; }
    BlockPos arenaPosition() { return arena == null ? blockPosition() : arena; }
    public RivenBrain brain() { return brain; }
    public boolean emotionalHigh() { return phase() >= 3; }
    public boolean signatureReady() { return RivenCombat.signatureReady(tickCount, nextSignatureAt); }
    public void signatureHit() { nextSignatureAt = RivenCombat.nextSignatureAt(tickCount); }
    boolean isStaggeredNow() { return tickCount < stagger; }
    public boolean hasSongBuff() { return tickCount < songUntil; }
    public void markSongBuff(int ticks) { songUntil = tickCount + ticks; }
    public void guardFor(int ticks) { guardUntil = tickCount + ticks; }
    public void addStory(float v) { story = Math.min(100f, story + v); }
    public boolean isHexed(LivingEntity e) { return tickCount < hexUntil && e.getUUID().equals(hexTarget); }
    public void hex(LivingEntity e, int ticks) { hexTarget = e.getUUID(); hexUntil = tickCount + ticks; }
    public void bond(LivingEntity e, int ticks) { bondTarget = e.getUUID(); bondUntil = tickCount + ticks; }
    public void setArena(BlockPos p) { arena = p.immutable(); }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }

    public int maxConstructs() { return phase() >= 2 ? Math.min(4, 1 + Math.max(0, lastPlayers - 1)) : 0; }

    public boolean canManifest() {
        if (!(level() instanceof ServerLevel sl)) return false;
        int n = sl.getEntitiesOfClass(StoryConstructEntity.class, getBoundingBox().inflate(48), c -> c.ownedBy(getUUID())).size();
        return n < maxConstructs();
    }

    /** A player who "doesn't believe": anti-magic (Black Form, a demon sword, the book), a Nihility zone, or a raised guard. */
    public boolean rewriteShield(Player p) {
        boolean demon = BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).getPath().startsWith("demon_");
        boolean nihility = level() instanceof ServerLevel sl && NihilityZone.inside(sl, p.position());
        return AntiMagic.isUser(p) || demon || nihility || p.isBlocking();
    }

    /** True if the rewrite lands. A shielded player resists it unless he spends 10 story charge. */
    public boolean rewriteAllowed(LivingEntity e) {
        if (!(e instanceof Player p) || !rewriteShield(p)) return true;
        if (story >= 10f) { story -= 10f; return true; }
        return false;
    }

    // ---------------------------------------------------------------- clips
    public void playClip(String name) {
        for (int i = 0; i < CLIPS.length; i++) if (CLIPS[i].equals(name)) { playClip(i); return; }
    }

    private void playClip(int id) {
        if (level().isClientSide) return;
        entityData.set(CLIP, id);
        entityData.set(CLIP_SEQ, entityData.get(CLIP_SEQ) + 1);
        clipEnd = tickCount + CLIP_TICKS[id];
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key.equals(CLIP_SEQ)) clientClipStart = tickCount;
    }

    // ---------------------------------------------------------------- boss bar
    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        bar.addPlayer(p);
        PacketDistributor.sendToPlayer(p, statePayload());
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }

    private RivenStatePayload statePayload() {
        int left = casting == null ? 0 : (int) Math.max(0, castAt - tickCount);
        return new RivenStatePayload(getId(), phase(), getHealth() / getMaxHealth(), (int) story, casting == null ? "" : casting.name(), left,
                maxConstructs(), combatType, drawnGrimoire, statusText);
    }

    private String subtitle() {
        return switch (phase()) {
            case 1 -> "I · The Black Bulls' Bard";
            case 2 -> "II · Fictional Remake";
            case 3 -> "III · Rift";
            default -> "IV · Fictional Remake — Final Form";
        };
    }

    // ---------------------------------------------------------------- the fight
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) return;
        long t = tickCount;
        fightTicks++;
        if (arena == null) arena = blockPosition();
        if (!initializedExistence) initializeExistence();
        if (NihilityZone.inside(sl, position()) && tickCount >= stagger) staggerByAntiMagic(sl);
        float frac = getHealth() / getMaxHealth();
        int ph = RivenCombat.phase(frac, fightTicks, phase());
        int nextPhase = RivenCombat.nextPhase(phase(), ph);
        if (nextPhase > phase()) enterPhase(sl, nextPhase);
        rollCombatType(sl);
        bar.setProgress(frac);
        String damageType = casting == null ? "" : casting.has("physical_damage") ? "PHYSICAL "
                : casting.has("magic_damage") ? "MAGIC " : "UTILITY ";
        String castName = casting == null ? "" : " · " + damageType + casting.name();
        String nullName = tickCount < nullUntil ? " · NULL: " + nullType.toUpperCase(java.util.Locale.ROOT) : "";
        bar.setName(Component.literal("Marquis Remake — " + subtitle() + " · " + this.combatType
                + (drawnGrimoire.isEmpty() ? "" : " · " + drawnGrimoire)
                + (statusText.isEmpty() ? "" : " · " + statusText) + castName + nullName));
        recentDamage *= 0.97f;
        calmDamage *= 0.99f;
        if (t % 100 == 0) scaleForPlayers(sl);
        if (t % 200 == 0) TensuraCaster.ensureMana(this, Math.max(1_000_000, EnergyHelper.getMaxMagicule(this)));
        if (phase() == 2 && !phaseTwoMagicNullStarted && fightTicks - phaseTwoAt >= 240) {
            phaseTwoMagicNullStarted = true;
            startNullWindow("magic", 80, sl, "Hack roll: magic is out. Try another page.");
        }
        if (phase() == 4 && !finalNullStarted && getHealth() <= getMaxHealth() * 0.25f) {
            finalNullStarted = true;
            startNullWindow("physical", 40, sl, "The final rewrite nullifies physical force.");
            SpellRuntime.later(sl, 40, () -> {
                if (isAlive() && phase() == 4) startNullWindow("magic", 40, sl, "The rewrite turns. Magic is nullified.");
            });
        }
        if (phase() >= 3 && riftOpen && t < riftUntil && t >= nextRiftPull) {
            nextRiftPull = t + 20;
            pullIntoRift(sl);
        }
        if (t >= soulNotePayoutAt && soulNotePayoutAt > 0) RivenAttacks.paySoulNote(this, sl);
        if (t % 200 == 1) grantBossBoundSkills();
        story = Math.min(100f, story + 0.03f + lastPlayers * 0.01f + (emotionalHigh() ? 0.03f : 0f));
        if (t % 400 == 0) maybeBanterWithZagred(sl);
        if (t >= clipEnd && clip() != 0 && !isDeadOrDying()) entityData.set(CLIP, 0);
        if (blockPosition().distSqr(arena) > 48 * 48) { teleportTo(arena.getX() + 0.5, arena.getY(), arena.getZ() + 0.5); brain.invalidate(); }
        if (t >= nextSync) { nextSync = t + 10; PacketDistributor.sendToPlayersTrackingEntity(this, statePayload()); }
        if (bondTarget != null && t >= bondUntil) bondTarget = null;
        if (!statusText.isEmpty() && t >= statusUntil) statusText = "";
        if (portalAt > 0 && t >= portalAt) {
            portalAt = 0;
            LivingEntity pt = getTarget();
            if (pt != null && pt.isAlive()) RivenPortal.emerge(this, sl, pt);
        }
        if (t < transitionUntil || t < stagger || t < unwrittenUntil || t < rewriteUntil) {
            getNavigation().stop();
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) { casting = null; return; }
        processAvatarSummon(sl, target);
        fighters.add(target.getUUID());
        if (!opened) { opened = true; open(sl, target); return; }
        checkBored(sl, target);
        tryNewOrder(sl, target);
        tryEvilEye(sl, target);
        tryCompress(sl, target);
        if (t < transitionUntil) return;

        if (casting != null) {
            getNavigation().stop();
            getLookControl().setLookAt(castTarget != null ? castTarget : target, 60, 60);
            if (t >= castAt) finishCast(sl, target);
            return;
        }

        if (t < nextThink) return;
        nextThink = t + 8;
        AnimeSkill pick = forced != null ? forced : brain.choose(target, t);
        forced = null;
        if (pick != null) beginCast(sl, pick, target);
    }

    private void rollCombatType(ServerLevel sl) {
        if (fightTicks < nextCombatRoll || phase() >= 4) return;
        combatType = RivenCombat.combatType(getRandom().nextInt(6));
        nextCombatRoll = fightTicks + RivenCombat.rollInterval(phase());
        var pages = RivenCombat.grimoirePages(AnimeSkillCodex.all(), phase());
        if (pages.isEmpty()) {
            drawnGrimoire = "";
            LOG.warn("[marquis] no validated Black Clover codex pages are available for this draw.");
            return;
        }
        AnimeSkill page = pages.get(getRandom().nextInt(pages.size()));
        CanonBook tome = CanonBook.values()[getRandom().nextInt(CanonBook.values().length)];
        drawnGrimoire = tome.owner;
        forced = page;
        brain.invalidate();
        statusText = "Grimoire drawn";
        statusUntil = tickCount + 40;
        VfxSpawn.send(sl, VfxShape.SPELL_CARD, position().add(0, 1.5, 0), position().add(0, 2, 0),
                getRandom().nextInt(), 24, 1.0f);
        LivingEntity target = getTarget();
        if (phase() >= 2 && "Hack".equals(combatType) && target != null && target.isAlive()) {
            boolean kiting = ThreatScan.of(this, target).has("kiter");
            MarquisStatus.applyGearshift(kiting ? target : this, !kiting, 80);
            statusText = "Gearshift · " + (kiting ? "Low" : "Top");
            statusUntil = tickCount + 80;
            VfxSpawn.send(sl, VfxShape.THREAD_LINE, kiting ? target.getEyePosition() : getEyePosition(),
                    kiting ? target.getEyePosition().add(0, 0.1, 0) : getEyePosition().add(getLookAngle().scale(3)),
                    kiting ? 0xFFFF5555 : 0xFF4C9FFF, 12, 0.7f);
        }
    }

    private void tryNewOrder(ServerLevel sl, LivingEntity target) {
        if (phase() != 1 || fightTicks < 40 || tickCount < nextNewOrderAt || distanceToSqr(target) > 36 || casting != null) return;
        newOrderUsed = true;
        nextNewOrderAt = tickCount + 160;
        ThreatScan threat = ThreatScan.of(this, target);
        String orderId = RivenCombat.newOrder(threat.has("melee") && target.getAttribute(Attributes.ATTACK_SPEED) != null,
                threat.has("kiter"), threat.has("caster"), target instanceof ServerPlayer, threat.has("flier"));
        boolean heavy = "HEAVY_WEAPON".equals(orderId);
        boolean low = "GEARSHIFT_LOW".equals(orderId);
        boolean slowCasts = "SLOW_GRIMOIRE_CASTS".equals(orderId);
        boolean ground = "GROUND_STONE".equals(orderId);
        String order = heavy ? "That weapon is heavy." : low ? "Your escape is slower."
                : slowCasts ? "Your magic takes longer to form." : ground ? "The ground is stone." : "I am faster.";
        statusText = "New Order";
        statusUntil = tickCount + 180;
        say(sl, order);
        playClip("cast_grimoire");
        VfxSpawn.send(sl, VfxShape.ALCHEMY_CIRCLE, target.position(), target.position().add(0, 0.05, 0), 0xFFF2E5FF, 20, 1.5f);
        SpellRuntime.later(sl, 20, () -> {
            if (!isAlive() || isStaggeredNow() || !target.isAlive() || distanceToSqr(target) > 64
                    || target.blockPosition().distSqr(arenaPosition()) > 48L * 48L
                    || target instanceof Player p && rewriteShield(p)) {
                statusText = "New Order fizzled";
                statusUntil = tickCount + 30;
                return;
            }
            if (heavy) MarquisStatus.applyHeavyWeaponOrder(target, 120);
            else if (low) MarquisStatus.applyGearshift(target, false, 80);
            else if (slowCasts && target instanceof ServerPlayer player) MarquisStatus.slowNextGrimoireCasts(player, 2, 160);
            else if (ground) MarquisStatus.createStonePlatform(sl, target, arenaPosition());
            else MarquisStatus.applyGearshift(this, true, 80);
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.2f, 1.4f);
            statusText = "New Order · " + (heavy ? "Heavy" : low ? "Gearshift Low" : slowCasts ? "Slow next two pages"
                    : ground ? "Stone footing" : "Gearshift Top");
            statusUntil = tickCount + 160;
        });
    }

    private void tryEvilEye(ServerLevel sl, LivingEntity target) {
        if (phase() < 2 || evilEyePhase == phase() || (phase() == 2 && fightTicks < phaseTwoAt + 160)
                || (phase() == 3 && fightTicks < phaseThreeAt + 40)
                || distanceToSqr(target) > 18 * 18 || !hasLineOfSight(target) || casting != null) return;
        evilEyePhase = phase();
        int duration = phase() == 4 ? 160 : 120;
        statusText = "Evil Eye";
        statusUntil = tickCount + duration;
        playClip("cast_grimoire");
        say(sl, "This page ends.");
        VfxSpawn.send(sl, VfxShape.ANTI_MAGIC_SLASH, getEyePosition(), target.getEyePosition(), 0xFF9F76FF, 16, 1.2f);
        MarquisStatus.markEvilEye(target, duration);
    }

    private void tryCompress(ServerLevel sl, LivingEntity target) {
        if (phase() < 2 || compressUsed || fightTicks < compressAt || casting != null
                || !(target instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()
                || distanceToSqr(player) > 8 * 8 || !hasLineOfSight(player)) return;
        compressUsed = true;
        statusText = "Compress";
        statusUntil = tickCount + 80;
        say(sl, "A moment between the pages.");
        VfxSpawn.send(sl, VfxShape.DREAM_MANIFEST, player.position(), player.position().add(0, 1, 0), 0xFF4B285F, 20, 0.7f);
        SpellRuntime.later(sl, 20, () -> {
            if (!isAlive() || isStaggeredNow() || !player.isAlive() || player.isCreative() || player.isSpectator()
                    || player.distanceToSqr(this) > 10 * 10) {
                statusText = "";
                return;
            }
            Vec3 anchor = position().add(getLookAngle().multiply(1.0, 0, 1.0));
            if (MarquisStatus.compress(player, anchor, 60)) {
                statusText = "Compress · 3s";
                statusUntil = tickCount + 60;
                VfxSpawn.send(sl, VfxShape.DREAM_TRANSITION, anchor, anchor.add(0, 0.8, 0), 0xFF4B285F, 60, 0.65f);
            }
        });
    }

    private void processAvatarSummon(ServerLevel sl, LivingEntity target) {
        if (!avatarPending || fightTicks < avatarSpawnAt || target == null || !target.isAlive()) return;
        avatarPending = false;
        if (isStaggeredNow()) return;
        if (!sl.getEntitiesOfClass(StoryConstructEntity.class, getBoundingBox().inflate(48),
                c -> c.ownedBy(getUUID()) && c.isAvatar()).isEmpty()) return;
        ThreatScan scan = ThreatScan.of(this, target);
        String name = RivenCombat.avatarFor(avatarScheduledPhase, scan.has("tank"), scan.has("healer"), scan.has("flier"),
                scan.has("magic_null"), scan.has("caster"));
        if (name.isEmpty()) return;
        Vec3 at = position().add(getLookAngle().multiply(-2, 0, -2));
        StoryConstructEntity avatar = StoryConstructEntity.spawnAvatar(sl, this, name, at);
        if (avatar != null) {
            statusText = "Avatar · " + name;
            statusUntil = tickCount + 240;
            say(sl, name + ", take the stage.");
            if ("Zeus".equals(name)) MarquisStatus.applyGearshift(avatar, true, 80);
            if ("The Creator".equals(name)) {
                statusText = "The Creator · Healing sealed";
                statusUntil = tickCount + 160;
                MarquisStatus.suppressHealingInArena(sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(48),
                        e -> e != this && e.isAlive()), 160);
            }
        }
    }

    /**
     * Boredom: counts seconds in which the fight poses no threat (little damage taken lately, a live target, phase 1-2). Once past
     * {@code boredSeconds} he rolls {@code boredChance} each second; a hit that hurts him properly cools the boredom down.
     */
    private void checkBored(ServerLevel sl, LivingEntity target) {
        if (!RivenConfig.BORED_PORTAL.get() || phase() >= 3 || casting != null || tickCount % 20 != 0) return;
        if (calmDamage > getMaxHealth() * 0.05f) { boredSeconds = Math.max(0, boredSeconds - 3); return; }
        boredSeconds++;
        if (boredSeconds < RivenConfig.BORED_SECONDS.get() || tickCount < portalReadyAt) return;
        if (getRandom().nextDouble() >= RivenConfig.BORED_CHANCE.get() || distanceToSqr(target) > 40 * 40) return;
        boredSeconds = 0;
        portalReadyAt = tickCount + RivenConfig.BORED_COOLDOWN.get() * 20L;
        transitionUntil = tickCount + RivenPortal.LOCK_TICKS;
        portalAt = tickCount + RivenPortal.OPEN_TICKS;
        casting = null;
        RivenPortal.open(this, sl, target);
    }

    private void open(ServerLevel sl, LivingEntity target) {
        say(sl, "Chaos builds the best stories.");
        AnimeSkill inspire = AnimeSkillCodex.get("nusmp:bardic_inspiration");
        if (inspire != null) beginCast(sl, inspire, target);
    }

    private void beginCast(ServerLevel sl, AnimeSkill s, LivingEntity target) {
        if (RivenCombat.signature(s) && !signatureReady()) { forced = s; nextThink = tickCount + 10; return; }
        if (s.nativeId().equals("final_page") && !finalPageReady()) { forced = s; nextThink = tickCount + 10; return; }
        if (s.nativeId().equals("audience_collapse") && !canStartAudienceCollapse()) { nextThink = tickCount + 20; return; }
        casting = s;
        castTarget = target;
        int ticks = RivenCombat.castTicks(s.castTicks(), phase());
        castAt = tickCount + ticks;
        nextSync = 0;
        playClip(s.has("melee_arc") && !s.has("projectile") ? "sword_combo_" + (1 + getRandom().nextInt(3)) : s.animation());
        VfxSpawn.sendFollowing(sl, VfxShape.MAGIC_CIRCLE, this, position().add(0, 1.2, 0), RivenAttacks.VIOLET, ticks + 6, 0.8f);
        if (RivenCombat.signature(s)) {
            if (phase() == 4) nextSignatureAt = RivenCombat.nextSignatureAt(tickCount);
            if (s.nativeId().equals("final_page")) markFinalPageCast();
            Vec3 tell = target.position();
            VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, tell, tell.add(0, 0.05, 0), 0xFF39224F, ticks + 4, 1.8f);
            VfxSpawn.sendFollowing(sl, VfxShape.SPIRIT_AURA, this, position().add(0, 1, 0), 0xFF7058C8, ticks + 4, 1.4f);
            VfxSpawn.send(sl, VfxShape.ALCHEMY_CIRCLE, tell, tell.add(0, 0.05, 0), 0xFF4B3D68, ticks + 4, 1.8f);
            sl.playSound(null, blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.HOSTILE, 1.4f, 0.7f);
        }
        if (!s.line().isBlank() && tickCount >= nextBark) { say(sl, s.line()); nextBark = tickCount + 90; }
    }

    private void finishCast(ServerLevel sl, LivingEntity target) {
        AnimeSkill s = casting;
        casting = null;
        try {
            RivenAttacks.execute(this, s, castTarget != null ? castTarget : target, sl, 0);
        } catch (RuntimeException e) {
            LOG.warn("[riven] skill {} failed: {}", s.id(), e.toString());
        }
        brain.cooldown(s, tickCount);
        if (s.tier() >= 3 && !s.nativeId().equals("audience_collapse")) story = Math.max(0, story - STORY_COST);
        lastCast = s;
        // phase 3: the story where he wins: sometimes two skills back to back
        if (phase() == 4 && chainLeft == 0 && s.tier() >= 4 && brain.plan() != null) {
            KillPlan.Option next = brain.plan().next(s);
            if (next != null && target.isAlive() && !RivenCombat.signature(next.skill())) { chainLeft = 1; beginCast(sl, next.skill(), target); return; }
        }
        chainLeft = 0;
        nextThink = tickCount + 10;
    }

    private void enterPhase(ServerLevel sl, int ph) {
        entityData.set(PHASE, ph);
        transitionUntil = tickCount + 30;
        boolean kamiTenchi = !sl.getEntitiesOfClass(StoryConstructEntity.class, getBoundingBox().inflate(24),
                avatar -> avatar.ownedBy(getUUID()) && avatar.isAvatar() && "Kami Tenchi".equals(avatar.getCustomName() == null ? "" : avatar.getCustomName().getString())).isEmpty();
        phaseIFramesUntil = kamiTenchi ? tickCount : tickCount + 30;
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(0.30 * (1 + 0.10 * (ph - 1)));
        casting = null;
        playClip(ph >= 4 ? "final_form" : ph == 2 ? "phase2" : "phase2");
        addStory(40f);
        brain.invalidate();
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, position(), position().add(0, 1, 0), RivenAttacks.VIOLET, 34, 2.4f);
        sl.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.5f, ph == 2 ? 1.0f : 0.7f);
        say(sl, switch (ph) {
            case 2 -> "Act two. Now the story starts to bite.";
            case 3 -> "The rift opens. Try not to get lost.";
            default -> "Final chapter. No more rehearsal.";
        });
        if (ph == 2) forced = AnimeSkillCodex.get("nusmp:story_manifestation");
        if (ph == 2) {
            phaseTwoAt = fightTicks;
            compressAt = fightTicks + 240;
            phaseTwoMagicNullStarted = false;
            nextCombatRoll = fightTicks + 240;
            scheduleAvatar(2);
            startNullWindow("physical", 80, sl, "Hack roll: physical force is nullified.");
        }
        if (ph == 3) {
            phaseThreeAt = fightTicks;
            nextCombatRoll = fightTicks + 160;
            scheduleAvatar(3);
            startNullWindow("spatial", 80, sl, "Rift entry: spatial magic is nullified.");
            openStoryRift(sl);
        }
        if (ph == 4) {
            finalNullStarted = false;
            if (!creatorUsed) {
                creatorUsed = true;
                scheduleAvatar(4);
            }
        }
    }

    private void scheduleAvatar(int ph) {
        avatarPending = true;
        avatarScheduledPhase = ph;
        avatarSpawnAt = fightTicks + 28;
    }

    private void startNullWindow(String type, int ticks, ServerLevel sl, String line) {
        nullType = type;
        nullUntil = tickCount + ticks;
        say(sl, line);
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.4f, 0.6f);
    }

    void startNullWindowForAttack(String type, int ticks, ServerLevel sl, String line) {
        startNullWindow(type, ticks, sl, line);
    }

    private void openStoryRift(ServerLevel sl) {
        riftOpen = true;
        riftUntil = tickCount + 1200;
        nextRiftPull = tickCount;
        Vec3 center = Vec3.atBottomCenterOf(arena);
        VfxSpawn.send(sl, VfxShape.SPACE_PORTAL, center.add(0, 0.1, 0), center.add(0, 2, 0), 0xFFB088FF, 1200, 2.4f);
        sl.playSound(null, blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.8f, 0.8f);
        say(sl, "The story rift stays open for one minute. It will pull you back into the scene.");
    }

    private void pullIntoRift(ServerLevel sl) {
        Vec3 center = Vec3.atBottomCenterOf(arena);
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            Vec3 delta = center.subtract(p.position());
            double distance = delta.length();
            if (distance <= 6 || distance >= 16) continue;
            Vec3 pull = delta.normalize().scale(Math.min(0.38, (distance - 6) * 0.045)).add(0, 0.08, 0);
            p.setDeltaMovement(p.getDeltaMovement().add(pull));
            p.hurtMarked = true;
        }
        if (tickCount % 100 == 0) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, center.x, center.y + 0.15, center.z, 24, 8, 0.1, 8, 0.04);
        }
    }

    private void grantBossBoundSkills() {
        try {
            var skills = SkillAPI.getSkillsFrom(this);
            for (var holder : NUSkills.RIVEN_PASSIVES.values()) {
                var skill = holder.get();
                var id = holder.getId();
                bossBoundSkills.add(id);
                if (skills.getSkill(id).isEmpty()) skills.learnSkill(skill.createDefaultInstance());
            }
            skills.markDirty();
        } catch (RuntimeException e) {
            LOG.warn("[riven] could not grant boss-bound hack skills: {}", e.toString());
        }
    }

    private void denyBossBoundPlunder() {
        int count = bossBoundSkills.size();
        try {
            var skills = SkillAPI.getSkillsFrom(this);
            for (var id : bossBoundSkills) skills.forgetSkill(id);
            skills.markDirty();
        } catch (RuntimeException e) {
            LOG.warn("[riven] could not strip boss-bound skills before death: {}", e.toString());
        }
        LOG.info("[riven] denied plunder of {} boss-bound skills.", count);
        bossBoundSkills.clear();
    }

    void beginUnwrittenEnding(ServerLevel sl) {
        unwrittenUntil = tickCount + 30;
        setInvisible(true);
        ServerPlayer lowest = sl.players().stream().filter(p -> p.isAlive() && !p.isCreative() && !p.isSpectator()
                        && p.distanceToSqr(arenaPosition().getX() + 0.5, arenaPosition().getY(), arenaPosition().getZ() + 0.5) < 48 * 48)
                .min(java.util.Comparator.comparingDouble(Player::getHealth)).orElse(null);
        say(sl, "The page goes blank. Find me if you can.");
        if (lowest == null) { unwrittenUntil = 0; setInvisible(false); return; }
        long endingAt = unwrittenUntil;
        SpellRuntime.later(sl, 30, () -> {
            if (!isAlive() || !lowest.isAlive() || unwrittenUntil != endingAt) {
                if (unwrittenUntil == endingAt) unwrittenUntil = 0;
                setInvisible(false);
                return;
            }
            unwrittenUntil = 0;
            Vec3 direction = lowest.position().subtract(position()).multiply(1, 0, 1);
            if (direction.lengthSqr() < 1e-4) direction = new Vec3(0, 0, 1);
            Vec3 pos = lowest.position().subtract(direction.normalize().scale(2.0));
            teleportTo(pos.x, lowest.getY(), pos.z);
            setInvisible(false);
            VfxSpawn.send(sl, VfxShape.SLASH_WAVE, position().add(0, 1, 0), lowest.getBoundingBox().getCenter(), 0xFFF1E9FF, 8, 2.2f);
            boolean hit = lowest.hurt(damageSources().mobAttack(this), 85f);
            if (hit) {
                Vec3 at = lowest.getBoundingBox().getCenter();
                VfxSpawn.send(sl, VfxShape.DARK_SLASH_DIMENSION, getEyePosition(), at, 0xFF8F76FF, 10, 0.55f);
                VfxSpawn.send(sl, VfxShape.BARRIER_FX3, at, at.add(0, 0.2, 0), 0xFFE0D7FF, 5, 1.35f);
            }
            signatureHit();
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.6f, 0.65f);
        });
    }

    boolean canRewriteRound() { return phase() == 4 && !rewriteUsed && !rewritePending; }
    boolean canStartAudienceCollapse() {
        return phase() == 4 && story >= STORY_COST && tickCount >= audienceReadyAt && tickCount >= finalPageReadyAt;
    }
    void markRewriteRoundUsed() { rewriteUsed = true; rewritePending = false; }
    void startRewriteStartup() { rewritePending = true; rewriteUntil = tickCount + 12; }
    void cancelRewriteStartup() { rewritePending = false; rewriteUntil = tickCount; }
    long rewriteStartupEndsAt() { return rewriteUntil; }
    void startSoulNote(ServerPlayer target) {
        soulNoteTarget = target.getUUID();
        soulNoteDamage = 0;
        soulNotePayoutAt = tickCount + 100;
    }
    void addSoulNoteDamage(ServerPlayer attacker, float amount) {
        if (soulNoteTarget != null && soulNoteTarget.equals(attacker.getUUID()) && tickCount < soulNotePayoutAt)
            soulNoteDamage += amount;
    }
    UUID soulNoteTarget() { return soulNoteTarget; }
    float takeSoulNoteDamage() { float amount = soulNoteDamage; soulNoteDamage = 0; soulNoteTarget = null; soulNotePayoutAt = 0; return amount; }
    void startBullWard(int ticks) { bullWardUntil = tickCount + ticks; }
    boolean bullWardActive() { return tickCount < bullWardUntil; }
    boolean startAudienceCollapse() {
        if (!canStartAudienceCollapse()) return false;
        story -= STORY_COST;
        audienceReadyAt = tickCount + 120;
        finalPageReadyAt = Math.max(finalPageReadyAt, tickCount + 120);
        return true;
    }
    boolean finalPageReady() { return tickCount >= finalPageReadyAt; }
    void markFinalPageCast() {
        finalPageReadyAt = tickCount + 120;
        nextSignatureAt = Math.max(nextSignatureAt, RivenCombat.nextSignatureAt(tickCount));
    }
    void markAudienceCast() { finalPageReadyAt = Math.max(finalPageReadyAt, tickCount + 120); }

    private void initializeExistence() {
        initializedExistence = true;
        try {
            EnergyHelper.setBaseMaxEP(this, 300_000_000d);
            var existence = TensuraStorages.getExistenceFrom(this);
            if (existence != null) {
                existence.setEP(300_000_000d);
                existence.setSkippingEPDrop(true);
                existence.markDirty();
            }
            TensuraCaster.ensureMana(this, Math.max(1_000_000, EnergyHelper.getMaxMagicule(this)));
        } catch (RuntimeException e) {
            LOG.warn("[riven] Tensura EP initialization unavailable: {}", e.toString());
        }
    }

    /** Health scales linearly to four players and retains the same fraction when party size changes. */
    private void scaleForPlayers(ServerLevel sl) {
        int n = Math.min(4, Math.max(1, (int) sl.players().stream().filter(p -> !p.isSpectator() && p.blockPosition().distSqr(arena) < 48 * 48).count()));
        lastPlayers = n;
        double max = RivenConfig.BASE_HEALTH.get() + (double) RivenConfig.PER_PLAYER_HEALTH.get() * (n - 1);
        var attr = getAttribute(Attributes.MAX_HEALTH);
        if (attr != null && Math.abs(attr.getBaseValue() - max) > 0.5) {
            float frac = getHealth() / getMaxHealth();
            attr.setBaseValue(max);
            setHealth((float) (max * frac));
        }
        healedOnScale = true;
    }

    private void maybeBanterWithZagred(ServerLevel sl) {
        if (tickCount < nextZagredBanter) return;
        if (!sl.getEntitiesOfClass(ZagredBossEntity.class, getBoundingBox().inflate(24)).isEmpty()) {
            say(sl, "Zagred, buddy! Your story's got a lot of footnotes.");
            nextZagredBanter = tickCount + 1200;
        }
    }

    // ---------------------------------------------------------------- damage
    static String category(DamageSource s) {
        if (AntiMagic.isMagic(s)) return "magic";
        return s.is(DamageTypeTags.IS_PROJECTILE) ? "projectile" : "physical";
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return super.hurt(source, amount);
        boolean antiMagic = isAntiMagicSource(source);
        if (antiMagic && level() instanceof ServerLevel sl) staggerByAntiMagic(sl);
        if (tickCount < phaseIFramesUntil || tickCount < unwrittenUntil || tickCount < rewriteUntil) return false;
        if (source.getEntity() instanceof StoryConstructEntity || source.getEntity() instanceof RivenBossEntity) return false;
        if (tickCount < nullUntil) {
            boolean nullifies = switch (nullType) {
                case "magic" -> AntiMagic.isMagic(source);
                case "physical" -> !AntiMagic.isMagic(source);
                case "spatial" -> isSpatialDamage(source);
                default -> false;
            };
            if (nullifies) return false;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        if (source.getEntity() instanceof ServerPlayer p) fighters.add(p.getUUID());
        if (source.getEntity() instanceof ServerPlayer p) lastPlayerAttacker = p.getUUID();
        else if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile projectile
                && projectile.getOwner() instanceof ServerPlayer p) lastPlayerAttacker = p.getUUID();
        brain.noteTaken(category(source));
        amount *= 0.92f;                                                           // Jack of All Trades
        float totalBefore = getHealth() + getAbsorptionAmount();
        float absorptionBefore = getAbsorptionAmount();
        if (tickCount < guardUntil || bullWardActive()) amount *= 0.5f;
        if (bondTarget != null && tickCount < bondUntil && level() instanceof ServerLevel sl && sl.getEntity(bondTarget) instanceof LivingEntity b && b.isAlive() && b != source.getEntity()) {
            b.hurt(damageSources().indirectMagic(this, this), amount * 0.25f);       // Soul Bond: a quarter of it falls on the bonded
            amount *= 0.75f;
        }
        float floor = phase() == 1 ? 0.75f * getMaxHealth() : phase() == 2 ? 0.5f * getMaxHealth()
                : phase() == 3 ? 0.25f * getMaxHealth() : 0f;   // one burst can't skip a phase
        if (floor > 0 && getHealth() - amount < floor) amount = Math.max(0f, getHealth() - floor + 0.5f);
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof ServerPlayer p)
            addSoulNoteDamage(p, Math.max(0, totalBefore - getHealth() - getAbsorptionAmount()));
        if (bullWardActive() && absorptionBefore > 0 && getAbsorptionAmount() <= 0) {
            bullWardUntil = 0;
            guardFor(0);
            RivenAttacks.bullWardBreak(this, (ServerLevel) level());
        }
        if (!hurt || !(level() instanceof ServerLevel sl)) return hurt;
        recentDamage += amount;
        calmDamage += amount;
        if (casting == null && clip() == 0) playClip("hit");
        if (recentDamage > getMaxHealth() * 0.1f && getTarget() != null && tickCount >= nextThink - 4) {   // Shadow Step out of trouble
            AnimeSkill step = AnimeSkillCodex.get("nusmp:shadow_step");
            if (step != null && casting == null) { recentDamage = 0; forced = step; nextThink = 0; }
        }
        return hurt;
    }

    private static boolean isAntiMagicSource(DamageSource source) {
        if (!(source.getEntity() instanceof Player player) || source.is(DamageTypeTags.IS_PROJECTILE)) return false;
        return AntiMagic.isUser(player)
                || BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).getPath().startsWith("demon_");
    }

    private void staggerByAntiMagic(ServerLevel sl) {
        if (tickCount < stagger) return;
        stagger = tickCount + 16;
        casting = null;
        nullUntil = Math.max(tickCount, nullUntil - 20);
        phaseIFramesUntil = Math.max(tickCount, phaseIFramesUntil - 20);
        unwrittenUntil = Math.max(tickCount, unwrittenUntil - 20);
        rewriteUntil = Math.max(tickCount, rewriteUntil - 20);
        rewritePending = false;
        avatarPending = false;
        clearFaJin();
        for (StoryConstructEntity construct : sl.getEntitiesOfClass(StoryConstructEntity.class, getBoundingBox().inflate(48),
                c -> c.ownedBy(getUUID()) && c.isAvatar())) construct.breakAvatar();
        if (!drawnGrimoire.isEmpty()) {
            if (forced != null && forced.anime().equals("black_clover")) forced = null;
            drawnGrimoire = "";
            statusText = "Grimoire burned";
            statusUntil = tickCount + 40;
        }
        playClip("stagger");
        brain.invalidate();
        VfxSpawn.send(sl, VfxShape.ANTI_MAGIC_SLASH, getEyePosition(), getEyePosition().add(getLookAngle().scale(2.5)),
                0xFF8A70FF, 8, 0.9f);
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.5f, 0.7f);
        if (tickCount >= nextBark) {
            say(sl, "That page... wasn't in my story.");
            nextBark = tickCount + 100;
        }
    }

    private static boolean isSpatialDamage(DamageSource source) {
        String type = source.type().msgId();
        if (type.contains("spatial") || type.contains("rift") || type.contains("teleport")) return true;
        if (source.getDirectEntity() != null) {
            String entity = BuiltInRegistries.ENTITY_TYPE.getKey(source.getDirectEntity().getType()).getPath();
            return entity.contains("spatial") || entity.contains("rift") || entity.contains("portal");
        }
        return false;
    }

    @Override
    public void die(DamageSource source) {
        denyBossBoundPlunder();
        super.die(source);
        if (!(level() instanceof ServerLevel sl)) return;
        playClip("death");
        say(sl, "Huh. So that's how it ends. ...Good story.");
    }

    @Override
    protected void tickDeath() {                                                 // the book closes, the lightning fades: a 2 s death
        deathTime++;
        if (deathTime >= 40 && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    // ---------------------------------------------------------------- talk
    public void say(ServerLevel sl, String line) {
        Component msg = Component.literal("<Marquis Remake> ").withStyle(ChatFormatting.LIGHT_PURPLE).append(Component.literal(line).withStyle(ChatFormatting.WHITE));
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(this) < 48 * 48) p.sendSystemMessage(msg);
        sl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.HOSTILE, 1.4f, 1.0f + getRandom().nextFloat() * 0.4f);
        playClipIfFree("talk");
    }

    private void playClipIfFree(String name) { if (casting == null && clip() == 0) playClip(name); }

    // ---------------------------------------------------------------- save
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (arena != null) tag.put("Arena", NbtUtils.writeBlockPos(arena));
        tag.putFloat("Story", story);
        tag.putBoolean("Opened", opened);
        tag.putBoolean("Scaled", healedOnScale);
        tag.putBoolean("RewriteUsed", rewriteUsed);
        tag.putLong("FightTicks", fightTicks);
        tag.putBoolean("NewOrderUsed", newOrderUsed);
        tag.putBoolean("CompressUsed", compressUsed);
        tag.putBoolean("CreatorUsed", creatorUsed);
        tag.putInt("EvilEyePhase", evilEyePhase);
        tag.putInt("Phase", phase());
        tag.putLong("PhaseTwoAt", phaseTwoAt);
        tag.putLong("PhaseThreeAt", phaseThreeAt);
        tag.putLong("CompressAt", compressAt);
        tag.putLong("NextCombatRoll", nextCombatRoll);
        tag.putBoolean("AvatarPending", avatarPending);
        tag.putInt("AvatarScheduledPhase", avatarScheduledPhase);
        tag.putLong("AvatarSpawnAt", avatarSpawnAt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Arena").ifPresent(p -> arena = p);
        if (tag.contains("Story")) story = tag.getFloat("Story");
        opened = tag.getBoolean("Opened");
        healedOnScale = tag.getBoolean("Scaled");
        rewriteUsed = tag.getBoolean("RewriteUsed");
        fightTicks = tag.getLong("FightTicks");
        newOrderUsed = tag.getBoolean("NewOrderUsed");
        compressUsed = tag.getBoolean("CompressUsed");
        creatorUsed = tag.getBoolean("CreatorUsed");
        evilEyePhase = tag.getInt("EvilEyePhase");
        phaseTwoAt = tag.getLong("PhaseTwoAt");
        phaseThreeAt = tag.getLong("PhaseThreeAt");
        compressAt = tag.getLong("CompressAt");
        nextCombatRoll = tag.contains("NextCombatRoll") ? tag.getLong("NextCombatRoll") : fightTicks + 200;
        avatarPending = tag.getBoolean("AvatarPending");
        avatarScheduledPhase = tag.getInt("AvatarScheduledPhase");
        avatarSpawnAt = tag.getLong("AvatarSpawnAt");
        entityData.set(PHASE, Math.max(1, Math.min(4, tag.contains("Phase") ? tag.getInt("Phase") : 1)));
        if (hasCustomName()) bar.setName(getDisplayName());
    }

    /** Summons the boss at 'at' with the arena there (/nusmp riven summon). */
    public static RivenBossEntity summon(ServerLevel sl, Vec3 at) {
        RivenBossEntity r = NUEntities.RIVEN.get().create(sl);
        if (r == null) return null;
        r.moveTo(at.x, at.y, at.z, 0, 0);
        r.setArena(BlockPos.containing(at));
        var attr = r.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) attr.setBaseValue(RivenConfig.BASE_HEALTH.get());
        r.setHealth(r.getMaxHealth());
        sl.addFreshEntity(r);
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, at, at.add(0, 1, 0), RivenAttacks.VIOLET, 40, 2.4f);
        sl.playSound(null, r.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 2.5f, 0.8f);
        return r;
    }

    // ---------------------------------------------------------------- movement
    /** A bard keeps his distance: backs off when pressed, closes when out of range, circles in between. */
    private final class RivenMoveGoal extends Goal {
        RivenMoveGoal() { setFlags(EnumSet.of(Flag.MOVE)); }

        @Override public boolean canUse() { LivingEntity t = getTarget(); return t != null && t.isAlive() && casting == null && tickCount >= stagger && tickCount >= transitionUntil; }
        @Override public boolean canContinueToUse() { return canUse(); }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            getLookControl().setLookAt(t, 30, 30);
            double d = distanceTo(t), pref = brain.preferredDistance();
            Vec3 away = position().subtract(t.position()).multiply(1, 0, 1);
            Vec3 dir = away.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : away.normalize();
            if (d < pref - 2) { Vec3 p = position().add(dir.scale(4)); getNavigation().moveTo(p.x, p.y, p.z, 1.2); }
            else if (d > pref + 3) getNavigation().moveTo(t, 1.1);
            else if (tickCount % 30 == 0) { Vec3 p = t.position().add(dir.yRot((tickCount % 60 == 0 ? 0.5f : -0.5f)).scale(pref)); getNavigation().moveTo(p.x, p.y, p.z, 1.0); }
        }
    }
}
