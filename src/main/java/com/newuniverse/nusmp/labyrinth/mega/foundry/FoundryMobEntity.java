package com.newuniverse.nusmp.labyrinth.mega.foundry;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Why: ONE entity type, {@code nusmp:foundry_mob}, is all 400 mobs of the Mob Foundry; which mob it is, is a synced string (the pattern of {@code arc/ArcNpcEntity}, which does the
 * same for the friendly characters). Its stats come from the definition's tier, archetype and the floor it was born on ({@link FoundryScaling}), its behaviour from goals that
 * look at the archetype's style ({@link FoundryGoals}), its special moves from the ability library ({@link FoundryBrain}, {@link FoundryAbilityRunner}), and its look from the model
 * the art kit wrote for the id (client: {@code FoundryRenderer}). It carries the Dungeon Master's tag ({@link MegaDungeon#DM_TAG}) when the Director or the admin command made it,
 * so the economy, the gear drops and the health buffer treat it like any other monster of the dungeon; a summoner's MINION does not (no loot, no experience: a summoner is not a farm).
 * It implements {@link Enemy} so every system that asks "is this hostile?" says yes. All state that matters survives a restart (identity, floor, owner); the cast, dash and aura
 * timers do not (a mob that reloads mid-cast simply starts over).
 */
public class FoundryMobEntity extends PathfinderMob implements Enemy {
    private static final EntityDataAccessor<String> IDENTITY = SynchedEntityData.defineId(FoundryMobEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> CAST_LEFT = SynchedEntityData.defineId(FoundryMobEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CAST_MAX = SynchedEntityData.defineId(FoundryMobEntity.class, EntityDataSerializers.INT);

    /** The scoreboard tag of a summoned minion: no drops, no experience. */
    public static final String MINION_TAG = "nusmp_foundry_minion";

    private FoundryDef def;
    private int defVersion = -1;
    private String defId = "";
    private FoundryBrain brain = new FoundryBrain(java.util.List.of(), 30);
    private int floor;
    private UUID owner;
    /** Timers of the abilities in progress (server side only). */
    final FoundryAbilityRunner.State fx = new FoundryAbilityRunner.State();

    public FoundryMobEntity(EntityType<? extends FoundryMobEntity> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 28.0).add(Attributes.ARMOR, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.0).add(Attributes.ATTACK_KNOCKBACK, 0.3)
                .add(Attributes.SCALE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IDENTITY, "");
        builder.define(CAST_LEFT, 0);
        builder.define(CAST_MAX, 0);
    }

    // ================================================================ who it is
    /** The id of the roster entry this mob is ({@code tunnel_kobold}), or empty for a blank one. */
    public String foundryId() { return entityData.get(IDENTITY); }

    /** The definition (server side only: a client never sees data packs), or null for a blank mob or an id the data lost. */
    public FoundryDef def() {
        String id = foundryId();
        if (id.isEmpty()) return null;
        if (def == null || defVersion != FoundryData.version() || !id.equals(defId)) {
            def = FoundryData.roster().byId(id);
            defVersion = FoundryData.version();
            defId = id;
        }
        return def;
    }

    public FoundryArchetype archetype() {
        FoundryDef d = def();
        return d == null ? FoundryArchetype.FALLBACK : FoundryData.archetypeOf(d);
    }

    public FoundryArchetype.Style style() { return archetype().style(); }

    public int tier() {
        FoundryDef d = def();
        return d == null ? 1 : d.tier();
    }

    public int floor() { return floor; }

    public Polarity road() {
        FoundryDef d = def();
        return d == null ? Polarity.HOLY : d.road();
    }

    public FoundryBrain brain() { return brain; }

    /** The faction id of this mob (one of the eleven of the contract), or null for a blank mob. */
    public String faction() {
        FoundryDef d = def();
        return d == null ? null : d.faction();
    }

    /** Becomes this mob: sets the identity and every stat for the floor it stands on (a floor of 0 takes the middle of the tier). Fills the health bar. */
    public void setFoundry(FoundryDef d, int floorNumber) {
        entityData.set(IDENTITY, d.id());
        this.def = d;
        this.defId = d.id();
        this.defVersion = FoundryData.version();
        this.floor = floorNumber != 0 ? floorNumber : (d.tier() - 1) * FoundryScaling.FLOORS_PER_TIER + FoundryScaling.FLOORS_PER_TIER / 2;
        if (d.road() == Polarity.ABYSSAL && this.floor > 0) this.floor = -this.floor;
        applyStats(true);
    }

    private void applyStats(boolean heal) {
        FoundryDef d = def();
        if (d == null) return;
        FoundryArchetype a = FoundryData.archetypeOf(d);
        int f = Math.abs(floor);
        base(Attributes.MAX_HEALTH, FoundryScaling.health(a, d.tier(), f) * FoundryConfig.get(FoundryConfig.HEALTH_MULT));
        base(Attributes.ATTACK_DAMAGE, FoundryScaling.damage(a, d.tier(), f) * FoundryConfig.get(FoundryConfig.DAMAGE_MULT));
        base(Attributes.ARMOR, FoundryScaling.armor(a, d.tier()));
        base(Attributes.MOVEMENT_SPEED, FoundryScaling.speed(a, d.tier()));
        base(Attributes.SCALE, FoundryScaling.scaleAttribute(d.height(), d.size()));
        base(Attributes.KNOCKBACK_RESISTANCE, a.armor() >= 1.5 ? 0.6 : a.armor() >= 1.0 ? 0.2 : 0.0);
        this.xpReward = 4 + 6 * d.tier();
        brain = new FoundryBrain(FoundryData.abilitiesOf(d), a.abilityEvery());
        brain.reset(tickCount);
        if (heal) setHealth(getMaxHealth());
        refreshDimensions();
    }

    private void base(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        AttributeInstance i = getAttribute(attribute);
        if (i != null) i.setBaseValue(value);
    }

    /** Marks it as the minion of a summoner: no loot, no experience, and it remembers whose it is. */
    public void makeMinion(UUID ownerId) {
        this.owner = ownerId;
        addTag(MINION_TAG);
        this.xpReward = 0;
    }

    public UUID owner() { return owner; }

    public boolean isMinion() { return owner != null || getTags().contains(MINION_TAG); }

    /** Marks it as a monster of the Dungeon Master (the economy, the gear drops and the health buffer ask for the tag). */
    public void makeDungeonMonster() { addTag(MegaDungeon.DM_TAG); }

    // ================================================================ names and sounds
    @Override
    protected Component getTypeName() {
        FoundryDef d = def();
        return d == null ? super.getTypeName() : Component.literal(d.name());
    }

    private String sound() {
        FoundryDef d = def();
        return d == null ? "" : d.faction();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return switch (sound()) {
            case "undead", "cultist" -> SoundEvents.ZOMBIE_AMBIENT;
            case "beast" -> SoundEvents.WOLF_GROWL;
            case "demon" -> SoundEvents.PIGLIN_BRUTE_ANGRY;
            case "slime" -> SoundEvents.SLIME_SQUISH_SMALL;
            case "construct" -> SoundEvents.IRON_GOLEM_STEP;
            case "insect" -> SoundEvents.SPIDER_AMBIENT;
            case "ogre" -> SoundEvents.RAVAGER_AMBIENT;
            case "elemental" -> SoundEvents.BLAZE_AMBIENT;
            case "aquatic" -> SoundEvents.DROWNED_AMBIENT;
            default -> SoundEvents.PILLAGER_AMBIENT;
        };
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return switch (sound()) {
            case "undead", "cultist" -> SoundEvents.ZOMBIE_HURT;
            case "beast" -> SoundEvents.WOLF_HURT;
            case "demon" -> SoundEvents.PIGLIN_BRUTE_HURT;
            case "slime" -> SoundEvents.SLIME_HURT_SMALL;
            case "construct" -> SoundEvents.IRON_GOLEM_HURT;
            case "insect" -> SoundEvents.SPIDER_HURT;
            case "ogre" -> SoundEvents.RAVAGER_HURT;
            case "elemental" -> SoundEvents.BLAZE_HURT;
            case "aquatic" -> SoundEvents.DROWNED_HURT;
            default -> SoundEvents.PILLAGER_HURT;
        };
    }

    @Override
    protected SoundEvent getDeathSound() {
        return switch (sound()) {
            case "undead", "cultist" -> SoundEvents.ZOMBIE_DEATH;
            case "beast" -> SoundEvents.WOLF_DEATH;
            case "demon" -> SoundEvents.PIGLIN_BRUTE_DEATH;
            case "slime" -> SoundEvents.SLIME_DEATH_SMALL;
            case "construct" -> SoundEvents.IRON_GOLEM_DEATH;
            case "insect" -> SoundEvents.SPIDER_DEATH;
            case "ogre" -> SoundEvents.RAVAGER_DEATH;
            case "elemental" -> SoundEvents.BLAZE_DEATH;
            case "aquatic" -> SoundEvents.DROWNED_DEATH;
            default -> SoundEvents.PILLAGER_DEATH;
        };
    }

    // ================================================================ casting (synced to the client for the cast clips)
    /** Ticks of the wind-up still to go (0 = not casting). */
    public int castLeft() { return entityData.get(CAST_LEFT); }

    public int castMax() { return entityData.get(CAST_MAX); }

    public boolean casting() { return castLeft() > 0; }

    void setCast(int left, int max) {
        if (entityData.get(CAST_LEFT) != left) entityData.set(CAST_LEFT, left);
        if (entityData.get(CAST_MAX) != max) entityData.set(CAST_MAX, max);
    }

    // ================================================================ AI
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FoundryGoals.AbilityGoal(this));
        FoundryGoals.addStyleGoals(this, goalSelector);
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new FoundryGoals.RivalGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && level() instanceof ServerLevel sl) FoundryAbilityRunner.tick(this, sl);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean r = super.hurt(source, amount);
        if (r && !level().isClientSide) FoundryAbilityRunner.onHurt(this, source);
        return r;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && level() instanceof ServerLevel sl && !isDeadOrDying()) FoundryAbilityRunner.onDeath(this, sl);
        super.die(source);
    }

    /** A minion is not a farm: no loot table of its own and no experience; the standard drop systems also skip it (see {@link FoundryDrops}). */
    @Override
    protected void dropFromLootTable(DamageSource source, boolean hitByPlayer) {}

    @Override
    public boolean canBeLeashed() { return false; }

    // ================================================================ save data
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Foundry", foundryId());
        tag.putInt("FoundryFloor", floor);
        if (owner != null) tag.putUUID("FoundryOwner", owner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        String id = tag.getString("Foundry");
        if (!id.isEmpty()) {
            entityData.set(IDENTITY, id);
            floor = tag.getInt("FoundryFloor");
            if (tag.hasUUID("FoundryOwner")) owner = tag.getUUID("FoundryOwner");
            if (def() != null) {
                float health = getHealth();
                applyStats(false);                    // the base values again, the saved modifiers and the saved health stay
                setHealth(Math.min(health, getMaxHealth()));
            }
        }
    }

    /** The mob that killed this one counts for the Dungeon Master's sweep like any other; nothing special here. Kept for readability of the loot rules. */
    public boolean isSummoned() { return isMinion(); }

    /** Convenience for the runner: the living entity this mob is fighting, or null. */
    LivingEntity foe() {
        LivingEntity t = getTarget();
        return t != null && t.isAlive() ? t : null;
    }

    /** Is this entity something this mob would not hurt (a mob of its own side)? */
    boolean friend(Entity e) {
        if (e == this) return true;
        if (e instanceof FoundryMobEntity f) {
            if (owner != null && owner.equals(f.owner)) return true;
            if (owner != null && owner.equals(f.getUUID())) return true;
            if (f.owner != null && f.owner.equals(getUUID())) return true;
            String a = faction(), b = f.faction();
            return a != null && a.equals(b);
        }
        return false;
    }
}
