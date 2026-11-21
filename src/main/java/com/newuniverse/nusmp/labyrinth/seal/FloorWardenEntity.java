package com.newuniverse.nusmp.labyrinth.seal;

import com.newuniverse.nusmp.boss.budget.BossBudget;
import com.newuniverse.nusmp.boss.budget.BudgetedBoss;
import com.newuniverse.nusmp.boss.rules.BossRulesConfig;
import com.newuniverse.nusmp.dungeon.DungeonEntities;
import com.newuniverse.nusmp.dungeon.rework.WardenBrain;
import com.newuniverse.nusmp.dungeon.LivingArmorEntity;
import com.newuniverse.nusmp.labyrinth.LabyrinthConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: the Floor Warden is the Labyrinth's mini-boss, the thing that drops the Shadow Key. It is a subclass of the Living Armor
 * ({@link LivingArmorEntity}, the Living Dungeon's tier-2 armour colony) so it shares that model, texture pipeline, armour layer and melee
 * AI, and so the lore holds: the Warden is the Dungeon's own armour grown huge on the Spire's magicules, not a new kind of creature. The
 * price is that {@link com.newuniverse.nusmp.dungeon.DungeonCreature} was written for the food web, and three of its rules are wrong for a
 * boss, so each is undone here: (1) STARVATION: {@code hunger} is protected, so {@link #aiStep} pins it to {@code HUNGER_MAX} before the
 * parent's tick runs (the decrement is 1 per 100 ticks and never reaches 0), and {@link #hurt} drops {@code starve} damage outright as a
 * second lock; (2) CARCASS: until 1.58.0 the parent left a Carcass block where it died (in the Living Dungeon) and this class removed any that
 * appeared; the carcass went with the Living Dungeon, so there is nothing to undo any more; (3) LOOT:
 * {@code dropFromLootTable} and {@code dropCustomDeathLoot} are overridden empty in the parent, so {@link #dropFromLootTable} rolls the
 * entity's loot table itself, the way {@code LivingEntity} does, and {@link #dropCustomDeathLoot} drops the Shadow Key from code: a
 * guaranteed progression item must not depend on a loot table a datapack can edit, on Looting or on luck. Max health is an attribute (static
 * per entity type), so the builder says 200 and {@link #finalizeSpawn} sets the base value from the server config afterwards. The boss bar is
 * a {@link ServerBossEvent} fed by {@code startSeenByPlayer} / {@code stopSeenByPlayer}, the pattern of the Zogratis bosses.
 */
public class FloorWardenEntity extends LivingArmorEntity implements BudgetedBoss {
    private static final int SLAM_COOLDOWN = 120, STEP_COOLDOWN = 160;
    private static final double SLAM_REACH = 5.0, SLAM_RADIUS = 4.0, STEP_MIN = 8.0, STEP_MAX = 24.0;
    private static final float SLAM_DAMAGE = 8f;

    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.anime_multiverse.floor_warden.bar"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_6);
    private int slamCooldown = 60, stepCooldown = 80;
    /** The two Living Armors it calls at half health: once per life. */
    private boolean summoned;
    /** Dungeon-boss rework: the move set (null until the first server tick) and the damage budget, both used only while {@link #reworkOn()} (the Labyrinth Guardian has its own budget and its own fight). */
    private WardenBrain moves;
    private final BossBudget wardenBudget = new BossBudget();

    public FloorWardenEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        xpReward = 60;
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 200).add(Attributes.ARMOR, 12).add(Attributes.ATTACK_DAMAGE, 12)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.FOLLOW_RANGE, 40);
    }

    /** A boss hunts every survival player in range; the food-web goals (sleep, scavenge, hunt when hungry) are not registered. */
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true) {
            @Override public boolean canUse() { return !casting() && super.canUse(); }

            @Override public boolean canContinueToUse() { return !casting() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, com.newuniverse.nusmp.boss.ai.BossEngageGoal.lookAt(this, 12f));          // boss AI engage rule: the look goals stand down while it has a target
        goalSelector.addGoal(8, com.newuniverse.nusmp.boss.ai.BossEngageGoal.lookAround(this));
        targetSelector.addGoal(0, com.newuniverse.nusmp.boss.ai.BossEngageGoal.create(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, e -> !e.isSpectator() && !((Player) e).isCreative()));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, type, data);
        applyConfigHealth();
        setPersistenceRequired();
        return out;
    }

    /** Max health from {@code labyrinth.wardenHealth}: the attribute's base value is per instance, the supplier's 200 is only the default. */
    private void applyConfigHealth() {
        AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
        if (health == null) return;
        double hp = LabyrinthConfig.get(LabyrinthConfig.WARDEN_HEALTH);
        health.setBaseValue(hp);
        setHealth((float) hp);
    }

    /** The Warden wears a full netherite set: found armour, not loot (drop chance 0, like the parent's iron and gold). */
    @Override
    protected void populateDefaultEquipmentSlots(RandomSource rnd, DifficultyInstance difficulty) {
        ItemStack[] set = {new ItemStack(Items.NETHERITE_HELMET), new ItemStack(Items.NETHERITE_CHESTPLATE), new ItemStack(Items.NETHERITE_LEGGINGS), new ItemStack(Items.NETHERITE_BOOTS)};
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) {
            setItemSlot(slots[i], set[i]);
            setDropChance(slots[i], 0f);
        }
    }

    // ---------------------------------------------------------------- the tick: no hunger, the bar, the two abilities, the summon
    @Override
    public void aiStep() {
        hunger = HUNGER_MAX;                                                              // pinned every tick: the parent's decrement never reaches 0
        super.aiStep();
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;
        bar.setProgress(Math.max(0f, Math.min(1f, getHealth() / getMaxHealth())));
        com.newuniverse.nusmp.boss.bars.BossBarHub.track(this, bar);              // 1.106.0: the custom boss bar's state (Decennial phases, lair actions, adds ...) goes to the watchers
        if (slamCooldown > 0) slamCooldown--;
        if (stepCooldown > 0) stepCooldown--;
        if (!isAlive()) return;
        LivingEntity target = getTarget();
        if (reworkOn()) {
            if (moves == null) moves = new WardenBrain();
            moves.tick(this, sl);
        } else if (customMoves()) {
            customMovesTick(sl);                                                          // rework 2: a subclass with a move set of its own (the Labyrinth Guardian) runs it instead of the old two moves
        } else if (target != null && target.isAlive()) {
            double d2 = distanceToSqr(target);
            if (slamCooldown <= 0 && d2 <= SLAM_REACH * SLAM_REACH) {
                groundSlam(sl);
                slamCooldown = SLAM_COOLDOWN;
            } else if (stepCooldown <= 0 && d2 >= STEP_MIN * STEP_MIN && d2 <= STEP_MAX * STEP_MAX && shadowStep(sl, target)) {
                stepCooldown = STEP_COOLDOWN;
            }
        }
        if (!summoned && getHealth() < getMaxHealth() * 0.5f) {
            summoned = true;
            summonGuards(sl);
        }
    }

    /** Dungeon-boss rework (docs/dungeon_bosses_rework.md): true for the plain Floor Warden while {@code dungeonBosses.warden} is on; the Labyrinth Guardian and any other subclass keep their own fight. */
    public boolean reworkOn() { return getClass() == FloorWardenEntity.class && BossRulesConfig.dungeonWarden(); }

    /** Is a move of the new set winding up or landing (the melee goal then stands still)? */
    protected boolean casting() { return (moves != null && moves.activeMove() >= 0) || customCasting(); }

    /** Rework 2 hooks for a subclass with its own telegraphed move set (the Labyrinth Guardian): is it on, run one tick of it, is one of its moves winding up. The Warden has none. */
    protected boolean customMoves() { return false; }

    protected void customMovesTick(ServerLevel sl) {}

    protected boolean customCasting() { return false; }

    /** The new set's phase change: the bar turns pink at half health and red at a quarter. */
    public void setBarPhase(int phase) {
        bar.setColor(phase >= 3 ? BossEvent.BossBarColor.RED : phase == 2 ? BossEvent.BossBarColor.PINK : BossEvent.BossBarColor.PURPLE);
    }

    /** The move set's moves, or null (for the self-test and the debug line). */
    public WardenBrain moves() { return moves; }

    @Override public BossBudget dpsBudget() { return wardenBudget; }

    @Override public int budgetStage() { return moves == null ? 0 : moves.phase() - 1; }

    @Override public int budgetFinalStage() { return reworkOn() ? 2 : 0; }

    @Override
    public void baseTick() {
        super.baseTick();
        if (reworkOn()) wardenBudget.tick(this, this);
    }

    @Override
    public void setHealth(float health) {
        super.setHealth(wardenBudget == null || !reworkOn() ? health : wardenBudget.setHealth(this, health));          // null while the constructors of the parents run
    }

    /** 1.54.0: the Ground Slam's damage as an overridable value (the Labyrinth Guardian scales it with its sector's threat); the Warden's stays 8. */
    protected float slamDamage() { return SLAM_DAMAGE; }

    /** 1.54.0: the Ground Slam's radius as an overridable value (the Guardian's gravity trait widens it); the Warden's stays 4. */
    protected double slamRadius() { return SLAM_RADIUS; }

    /** 1.54.0: called once after every Ground Slam (the Guardian's storm trait hangs its chain lightning here); nothing happens for the Warden. */
    protected void onGroundSlam(ServerLevel sl) {}

    /** Ground Slam: 8 damage and a shove to everything but armour within 4 blocks, with the anvil's crash. */
    private void groundSlam(ServerLevel sl) {
        double radius = slamRadius();
        AABB ring = getBoundingBox().inflate(radius);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, ring, e -> e != this && e.isAlive() && !(e instanceof LivingArmorEntity)
                && !(e instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            if (e.hurt(damageSources().mobAttack(this), slamDamage())) e.knockback(1.4, getX() - e.getX(), getZ() - e.getZ());
        }
        com.newuniverse.nusmp.audio.MobVoice.play(this, soundFamily(), com.newuniverse.nusmp.audio.Cue.ATTACK);       // 1.99.0: its family's own blow, not an anvil
        Fx.spray(sl, FxThemes.DUNGEON_SEAL, FxOld.EXPLOSION, getX(), getY() + 0.1, getZ(), 1, 0, 0, 0, 0);
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI / 12.0;
            Fx.spray(sl, FxThemes.DUNGEON_SEAL, FxOld.LARGE_SMOKE, getX() + Math.cos(a) * radius, getY() + 0.2, getZ() + Math.sin(a) * radius, 1, 0, 0, 0, 0.02);
        }
        onGroundSlam(sl);
    }

    /** Shadow Step: it is suddenly beside a target that kept its distance (behind it when that spot is free), in a burst of smoke. */
    private boolean shadowStep(ServerLevel sl, LivingEntity target) {
        Vec3 look = target.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.length() < 1e-3) flat = new Vec3(1, 0, 0);
        flat = flat.normalize().scale(1.8);
        Vec3[] spots = {target.position().subtract(flat), target.position().add(flat.z, 0, -flat.x), target.position().add(-flat.z, 0, flat.x), target.position().add(flat)};
        for (Vec3 c : spots) {
            BlockPos feet = BlockPos.containing(c);
            if (sl.getBlockState(feet.below()).getCollisionShape(sl, feet.below()).isEmpty()) continue;          // no floor there
            if (!sl.noCollision(this, getBoundingBox().move(c.subtract(position())))) continue;                  // would be inside a wall
            Vec3 from = position();
            Fx.spray(sl, FxThemes.DUNGEON_SEAL, FxOld.LARGE_SMOKE, from.x, from.y + 1.0, from.z, 24, 0.5, 1.0, 0.5, 0.02);
            com.newuniverse.nusmp.audio.MobVoice.play(this, soundFamily(), com.newuniverse.nusmp.audio.Cue.CAST);
            teleportTo(c.x, c.y, c.z);
            getNavigation().stop();
            lookAt(target, 180f, 180f);
            Fx.spray(sl, FxThemes.DUNGEON_SEAL, FxOld.LARGE_SMOKE, c.x, c.y + 1.0, c.z, 24, 0.5, 1.0, 0.5, 0.02);
            Fx.spray(sl, FxThemes.DUNGEON_SEAL, FxOld.WITCH, c.x, c.y + 1.2, c.z, 10, 0.4, 0.6, 0.4, 0.0);
            return true;
        }
        return false;
    }

    /** At half health, once: two Living Armors step out of its shadow and take its target. */
    private void summonGuards(ServerLevel sl) {
        for (int i = 0; i < 2; i++) {
            LivingArmorEntity guard = DungeonEntities.LIVING_ARMOR.get().create(sl);
            if (guard == null) continue;
            double side = i == 0 ? 1.6 : -1.6;
            guard.moveTo(getX() + side, getY(), getZ() - side * 0.5, getYRot(), 0f);
            guard.finalizeSpawn(sl, sl.getCurrentDifficultyAt(guard.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            guard.setPersistenceRequired();
            guard.addTag("nusmp_servant");                                                 // 1.54.0: a boss's minion is no faction's fighter (the Mega-Dungeon's turf wars leave it alone)
            guard.setTarget(getTarget());
            sl.addFreshEntity(guard);
            Fx.spray(sl, FxThemes.DUNGEON_SEAL, FxOld.WITCH, guard.getX(), guard.getY() + 1.0, guard.getZ(), 20, 0.4, 0.8, 0.4, 0.02);
        }
        com.newuniverse.nusmp.audio.MobVoice.play(this, soundFamily(), com.newuniverse.nusmp.audio.Cue.AGGRO);
    }

    // ---------------------------------------------------------------- what the food web would do, undone
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.STARVE)) return false;
        if (wardenBudget != null && reworkOn()) return wardenBudget.hurt(this, source, amount, x -> super.hurt(source, x));
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (moves != null) moves.reset();
        bar.setProgress(0f);
        bar.removeAllPlayers();
    }

    /** The entity loot table ({@code anime_multiverse:entities/floor_warden}), rolled the way {@code LivingEntity} does: the parent turned it off. */
    @Override
    protected void dropFromLootTable(DamageSource source, boolean hitByPlayer) {
        if (!(level() instanceof ServerLevel sl)) return;
        ResourceKey<LootTable> key = getLootTable();
        LootTable table = sl.getServer().reloadableRegistries().getLootTable(key);
        LootParams.Builder b = new LootParams.Builder(sl).withParameter(LootContextParams.THIS_ENTITY, this).withParameter(LootContextParams.ORIGIN, position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source).withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity());
        if (hitByPlayer && lastHurtByPlayer != null) b.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, lastHurtByPlayer).withLuck(lastHurtByPlayer.getLuck());
        table.getRandomItems(b.create(LootContextParamSets.ENTITY), getLootTableSeed(), this::spawnAtLocation);
    }

    /** The Shadow Key: always one, a second one in five. Code, not the table, so no datapack, Looting level or luck can take it away. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean hitByPlayer) {
        spawnAtLocation(new ItemStack(SealRegistry.SHADOW_KEY.get()));
        if (random.nextFloat() < 0.2f) spawnAtLocation(new ItemStack(SealRegistry.SHADOW_KEY.get()));
    }

    @Override public boolean removeWhenFarAway(double distance) { return false; }

    // ---------------------------------------------------------------- boss bar
    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        if (hasCustomName()) bar.setName(getDisplayName());
        bar.addPlayer(p);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        bar.removePlayer(p);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        bar.removeAllPlayers();
    }

    // ---------------------------------------------------------------- save
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SlamCooldown", slamCooldown);
        tag.putInt("StepCooldown", stepCooldown);
        tag.putBoolean("Summoned", summoned);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        wardenBudget.bypass(() -> super.readAdditionalSaveData(tag));                          // loading the saved health is not damage
        slamCooldown = tag.getInt("SlamCooldown");
        stepCooldown = tag.getInt("StepCooldown");
        summoned = tag.getBoolean("Summoned");
        hunger = HUNGER_MAX;
    }
}
