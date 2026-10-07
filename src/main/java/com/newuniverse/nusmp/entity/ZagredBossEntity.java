package com.newuniverse.nusmp.entity;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.GrimoireSummon;
import com.newuniverse.nusmp.book.KotodamaWords;
import com.newuniverse.nusmp.book.KotodamaWords.Source;
import com.newuniverse.nusmp.book.KotodamaWords.Word;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.book.UnderworldMatter;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 0.47: Zagred, the devil of Kotodama, as a 4-phase boss. Off by default (server config zagredBossEnabled); ops start it with
 * {@code /multiverse boss zagred}. Beating it is the future way to earn Kotodama (config kotodamaBossReward, also off).
 * <ol>
 *   <li><b>Phase 1 (100-75%):</b> the Word Soul: "Halt" paralyses and jams everyone around it; "Shatter" breaks whatever is
 *       fired at it back into magicules.</li>
 *   <li><b>Phase 2 (75-50%):</b> elemental adaptation: every 10 s it adapts to the element that hurt it most and takes only a
 *       fifth from it, so the party has to keep changing elements. Void lances join the words.</li>
 *   <li><b>Phase 3 (50-25%):</b> void flooding: underworld matter floods the arena from its centre again and again, and demon
 *       sword storms fall on its target.</li>
 *   <li><b>Phase 4 (25-0%):</b> the true Word Soul: it takes only 15% of any damage unless the hit is anti-magic, or it has just
 *       been struck by three different elements within 5 s (then it is "exposed" for 5 s and takes full damage). It speaks "Heal"
 *       once, and hurls its otherworldly trident.</li>
 * </ol>
 * A purple boss bar shows the phase; the arena's matter is put back when it dies or is removed.
 */
public class ZagredBossEntity extends Monster {
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(ZagredBossEntity.class, EntityDataSerializers.INT);
    public static final int VIOLET = KotodamaWords.VIOLET;

    private final ServerBossEvent bar = new ServerBossEvent(Component.literal("Zagred"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private final Map<String, Float> elementDamage = new HashMap<>();
    private final Map<String, Long> lastElementHit = new HashMap<>();
    private final Set<UUID> fighters = new HashSet<>();
    private BlockPos arena;
    private String adapted = "";
    private long exposedUntil, nextHalt = 100, nextShatter, nextLance = 160, nextFlood, nextSwords = 200, nextAdapt = 200;
    private boolean healed;

    public ZagredBossEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 600).add(Attributes.ATTACK_DAMAGE, 14).add(Attributes.ATTACK_KNOCKBACK, 1.2)
                .add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.FOLLOW_RANGE, 48).add(Attributes.ARMOR, 12).add(Attributes.ARMOR_TOUGHNESS, 6)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(PHASE, 1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 1..4 by health. */
    public int phase() { return entityData.get(PHASE); }

    /** Where the arena is (its centre): where it was summoned. */
    public void setArena(BlockPos p) { arena = p.immutable(); }

    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }

    // ---------------------------------------------------------------- boss bar
    @Override
    public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bar.addPlayer(p); }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }

    // ---------------------------------------------------------------- the fight
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) return;
        if (arena == null) arena = blockPosition();
        long t = tickCount;
        float frac = getHealth() / getMaxHealth();
        int ph = frac > 0.75f ? 1 : frac > 0.5f ? 2 : frac > 0.25f ? 3 : 4;
        if (ph != phase()) enterPhase(sl, ph);
        bar.setProgress(frac);
        bar.setName(Component.literal("Zagred  ·  Phase " + ph + (t < exposedUntil ? "  ·  EXPOSED" : !adapted.isEmpty() && ph >= 2 ? "  ·  adapted: " + adapted : ""))
                .withStyle(ChatFormatting.DARK_PURPLE));
        if (t % 60 == 0) VfxSpawn.sendFollowing(sl, VfxShape.KOTO_AURA, this, position(), VIOLET, 70, 1.6f);
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;

        // "Shatter" whenever something is flying at it
        if (t >= nextShatter && !sl.getEntitiesOfClass(Projectile.class, getBoundingBox().inflate(10), pr -> pr.getOwner() != this).isEmpty()) {
            KotodamaWords.speak(this, Word.SHATTER, Source.BOSS);
            nextShatter = t + (ph >= 3 ? 80 : 120);
        }
        if (t >= nextHalt && distanceToSqr(target) < 20 * 20) {
            KotodamaWords.speak(this, Word.HALT, Source.BOSS);
            nextHalt = t + (ph == 4 ? 200 : 280);
        }
        if (ph >= 2 && t >= nextAdapt) { adapt(sl); nextAdapt = t + 200; }
        if (ph >= 2 && t >= nextLance) {
            int n = ph == 4 ? 3 : 1;
            for (int k = 0; k < n; k++) { int kk = k; SpellRuntime.later(sl, kk * 8, () -> { if (isAlive() && getTarget() != null) lance(sl, getTarget()); }); }
            nextLance = t + (ph == 4 ? 70 : 100);
        }
        if (ph >= 3 && t >= nextFlood) {
            UnderworldMatter.start(sl, this, arena, 14 + 2 * (ph - 3), 500);
            nextFlood = t + 600;
        }
        if (ph >= 3 && t >= nextSwords) {
            KotodamaWords.speak(this, Word.SWORDS, Source.BOSS);
            nextSwords = t + (ph == 4 ? 160 : 240);
        }
        if (ph == 4 && !healed && frac < 0.1f) {
            healed = true;
            KotodamaWords.speak(this, Word.HEAL, Source.BOSS);
            say(sl, "\"Heal.\"");
        }
    }

    void enterPhase(ServerLevel sl, int ph) {
        entityData.set(PHASE, ph);
        String line = switch (ph) {
            case 2 -> "Zagred: \"Your magic... I have heard it before.\" (it adapts to the element that hurts it most)";
            case 3 -> "Zagred: \"Sink.\" (the arena floods with underworld matter)";
            case 4 -> "Zagred: \"Words are the only law.\" (only anti-magic, or three elements at once, can wound it now)";
            default -> "Zagred speaks.";
        };
        say(sl, line);
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, getBoundingBox().getCenter(), position(), VIOLET, 40, 3f);
        sl.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2f, 0.6f);
        if (ph >= 3) nextFlood = tickCount;
    }

    void say(ServerLevel sl, String line) {
        Component c = Component.literal(line).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(this) < 64 * 64) p.sendSystemMessage(c);
    }

    /** A void lance: a black-violet spear thrown along a line, striking everything on it a moment later. */
    void lance(ServerLevel sl, LivingEntity target) {
        Vec3 from = getEyePosition(), to = target.getBoundingBox().getCenter(), dir = to.subtract(from).normalize(), end = from.add(dir.scale(28));
        VfxSpawn.send(sl, VfxShape.KOTO_TRIDENT, from, end, VIOLET, 14, 1.2f);
        sl.playSound(null, blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 1.5f, 0.5f);
        SpellRuntime.later(sl, 5, () -> {
            for (LivingEntity e : KotodamaWords.foes(this, from.add(dir.scale(14)), 15)) {
                Vec3 c = e.getBoundingBox().getCenter();
                double s = Math.max(0, Math.min(28, c.subtract(from).dot(dir)));
                if (from.add(dir.scale(s)).distanceTo(c) > 1.2 + e.getBbWidth() / 2) continue;
                KotodamaWords.hurt(this, e, 12f);
                KotodamaWords.spirit(this, e, 2);
            }
        });
    }

    /** Phase 2+: adapt to the element that has hurt it most since the last adaptation. */
    void adapt(ServerLevel sl) {
        String best = "";
        float most = 0;
        for (var e : elementDamage.entrySet()) if (e.getValue() > most && !e.getKey().equals("physical")) { most = e.getValue(); best = e.getKey(); }
        elementDamage.clear();
        if (best.isEmpty() || best.equals(adapted)) return;
        adapted = best;
        say(sl, "Zagred adapts: " + best + " barely touches it now.");
        VfxSpawn.sendFollowing(sl, VfxShape.KOTO_AURA, this, position(), 0xFFC8A0FF, 30, 2f);
    }

    /** The element of a hit (from its damage type), for adaptation and the phase-4 exposure. */
    static String element(DamageSource s) {
        String path = s.typeHolder().unwrapKey().map(k -> k.location().getPath()).orElse("");
        if (path.contains("lightning")) return "lightning";
        if (path.contains("fire") || path.contains("lava") || s.is(DamageTypeTags.IS_FIRE)) return "fire";
        if (path.contains("water") || path.contains("drown")) return "water";
        if (path.contains("wind")) return "wind";
        if (path.contains("earth")) return "earth";
        if (path.contains("ice") || path.contains("freez") || s.is(DamageTypeTags.IS_FREEZING)) return "ice";
        if (path.contains("light") || path.contains("holy")) return "light";
        if (path.contains("dark") || path.contains("curse")) return "dark";
        if (path.contains("space") || path.contains("spatial") || path.contains("gravity")) return "space";
        if (s.is(DamageTypeTags.IS_EXPLOSION)) return "explosion";
        if (AntiMagic.isMagic(s)) return "magic";
        if (s.is(DamageTypeTags.IS_PROJECTILE)) return "projectile";
        return "physical";
    }

    /** Anti-magic: a demon sword, the Anti-Magic Lord's blows, or anyone fighting with their Anti-Magic grimoire out. */
    static boolean antiMagic(DamageSource s) {
        if (!(s.getEntity() instanceof Player p)) return false;
        if (BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).getPath().startsWith("demon_")) return true;
        if (AntiMagic.lord(p).isPresent() && s.getDirectEntity() == p) return true;
        return GrimoireSummon.isFloating(p, MagicType.ANTI_MAGIC)
                && GrimoirePages.grimoireOf(p).map(i -> GrimoirePages.magicOf(i) == MagicType.ANTI_MAGIC).orElse(false);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        if (source.getEntity() instanceof ServerPlayer p) fighters.add(p.getUUID());
        String el = element(source);
        long t = tickCount;
        int ph = phase();
        if (ph >= 2) {
            elementDamage.merge(el, amount, Float::sum);
            if (el.equals(adapted)) amount *= 0.2f;
        }
        if (ph == 4) {
            if (!el.equals("physical") && !el.equals("projectile") && !el.equals("magic")) {
                lastElementHit.put(el, t);
                lastElementHit.values().removeIf(at -> t - at > 100);
                if (lastElementHit.size() >= 3 && t >= exposedUntil && level() instanceof ServerLevel sl) {
                    exposedUntil = t + 100;
                    lastElementHit.clear();
                    say(sl, "Three elements at once! Zagred's word falters: EXPOSED.");
                    VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, getBoundingBox().getCenter(), position(), 0xFFFFFFFF, 30, 2.5f);
                }
            }
            if (!antiMagic(source) && t >= exposedUntil) amount *= 0.15f;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel sl)) return;
        UnderworldMatter.endAll(this);
        say(sl, "Zagred: \"...So words can be broken.\"");
        for (UUID id : fighters) {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(id);
            if (p != null) KotodamaWords.reward(p);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) UnderworldMatter.endAll(this);
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (arena != null) tag.put("Arena", NbtUtils.writeBlockPos(arena));
        tag.putBoolean("Healed", healed);
        tag.putString("Adapted", adapted);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Arena").ifPresent(p -> arena = p);
        healed = tag.getBoolean("Healed");
        adapted = tag.getString("Adapted");
        if (hasCustomName()) bar.setName(getDisplayName());
    }

    /** Summons the boss at 'at' with its arena there (the command). */
    public static ZagredBossEntity summon(ServerLevel sl, Vec3 at) {
        ZagredBossEntity z = NUEntities.ZAGRED.get().create(sl);
        if (z == null) return null;
        z.moveTo(at.x, at.y, at.z, 0, 0);
        z.setArena(BlockPos.containing(at));
        sl.addFreshEntity(z);
        VfxSpawn.send(sl, VfxShape.KOTO_SLUDGE, at.add(0, 0.05, 0), at, VIOLET, 60, 3f);
        sl.playSound(null, z.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3f, 0.5f);
        return z;
    }
}
