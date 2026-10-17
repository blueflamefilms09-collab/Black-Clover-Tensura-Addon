package com.newuniverse.nusmp.avatar;

import com.newuniverse.nusmp.avatar.ai.AvatarAi;
import com.newuniverse.nusmp.avatar.ai.AvatarMeleeGoal;
import com.newuniverse.nusmp.avatar.ai.AvatarStrollGoal;
import com.newuniverse.nusmp.avatar.ai.CombatStyle;
import com.newuniverse.nusmp.avatar.ai.HasDndStats;
import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import com.newuniverse.nusmp.avatar.cast.VirtualSkillLoadout;
import com.newuniverse.nusmp.avatar.dnd.AvatarStats;
import com.newuniverse.nusmp.avatar.evolution.AvatarEvolution;
import com.newuniverse.nusmp.avatar.dnd.Stat;
import com.newuniverse.nusmp.avatar.goal.AvatarDefendSelfOrCreatorGoal;
import com.newuniverse.nusmp.avatar.goal.AvatarFollowCreatorGoal;
import com.newuniverse.nusmp.avatar.goal.AvatarHuntCreatorGoal;
import com.newuniverse.nusmp.avatar.goal.AvatarSignatureGoal;
import com.newuniverse.nusmp.avatar.inventory.AvatarGear;
import com.newuniverse.nusmp.avatar.inventory.AvatarInventory;
import com.newuniverse.nusmp.avatar.inventory.AvatarInventoryMenu;
import com.newuniverse.nusmp.avatar.menu.AvatarActionMenu;
import com.newuniverse.nusmp.avatar.party.AvatarPartyManager;
import com.newuniverse.nusmp.avatar.naming.TensuraNamingEngine;
import com.newuniverse.nusmp.avatar.outcome.AvatarOutcomes;
import com.newuniverse.nusmp.avatar.outcome.AvatarUniqueSkill;
import com.newuniverse.nusmp.avatar.outcome.Outcome;
import com.newuniverse.nusmp.avatar.race.AvatarRace;
import com.newuniverse.nusmp.avatar.race.AvatarRaces;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Why: the Avatar is the player-made NPC of the owner's "My Avatar's Path to Greatness" system. It is a mob (never a Player) that serves its creator,
 * carries the creator's luck, rolled scores, race and personalities, and three in-game days after creation rolls an outcome (betrayal, soul awakening,
 * ordinary existence) unless it was named or already holds a Unique skill. This class is the entity and its saved state; the rules live in
 * {@link AvatarOutcomes} (the outcome engine), {@link TensuraNamingEngine} (naming), {@link com.newuniverse.nusmp.avatar.AvatarCreation} (creation) and the
 * goals in {@code avatar.goal} and {@code avatar.ai}. Per-Avatar data is saved in the entity's own NBT (CreatorUUID, CreatorStoredLuck, AvatarName, IsNamed,
 * personalities, PermanentMagiculeTax, OutcomeTimer, HasUniqueOrUltimateSkill, AppearanceData, race, outcome state, stance, pockets): an entity's fields travel
 * with it through saves, chunk unloads and dimension changes for free, which is why only the player-side facts are NeoForge attachments ({@link AvatarPlayerData}).
 *
 * <p><b>Base class (the Avatar rebuild).</b> The brief says the Avatar must extend Tensura's native subordinate class. Tensura has no class by the brief's example
 * names; what it has (see {@code docs/avatar/av4_ai.md} for the evidence) is the interface {@code ISubordinate}, which Tensura's own mixin adds to the vanilla
 * {@link TamableAnimal}, so every tamable animal, including Tensura's own {@code TensuraTamableEntity}, is a subordinate by virtue of that parent. The Avatar now
 * extends {@code TamableAnimal} directly (it was a {@code PathfinderMob}): it is a Tensura subordinate exactly as Tensura's own monsters are, its owner record
 * is kept in step with its loyalty ({@link TensuraBond}), and Tensura's Follow / Stay command (the vanilla "ordered to sit" flag) stops it following. The entity
 * type id, every NBT key and the {@link OwnableEntity} behaviour are unchanged, so old Avatars load as before. The parts of {@code Animal} that make no sense for
 * a person (feeding, breeding, love mode) are switched off below.
 *
 * <p>The entity type is {@code nusmp:player_avatar}; the Devil Avatar boss ({@code nusmp:devil_avatar}) is a different thing.
 */
public class AvatarEntity extends TamableAnimal implements OwnableEntity, HasDndStats {
    public static final EntityDataAccessor<Boolean> IS_FRENZIED = SynchedEntityData.defineId(AvatarEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<CompoundTag> APPEARANCE = SynchedEntityData.defineId(AvatarEntity.class, EntityDataSerializers.COMPOUND_TAG);
    public static final EntityDataAccessor<String> PRIMARY = SynchedEntityData.defineId(AvatarEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> SECONDARY = SynchedEntityData.defineId(AvatarEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> RACE = SynchedEntityData.defineId(AvatarEntity.class, EntityDataSerializers.STRING);
    /** 1.58.0: the Magic Aptitudes chosen or rolled in the Soul Forge, as comma separated aptitude ids (synced so the client can tint the aura). */
    public static final EntityDataAccessor<String> APTITUDES = SynchedEntityData.defineId(AvatarEntity.class, EntityDataSerializers.STRING);
    /**
     * The brief's three more synched facts. There is no double serializer in vanilla, so the creator's luck is a float mirror of the exact double kept in the saved
     * field; the outcome timer is mirrored once a second (a value that changed every tick would cost a packet every tick); {@code SPECIAL} is the
     * HasUniqueOrUltimateSkill flag. The stance (0 follow, 1 stay, 2 delve) is mirrored for the menu and the renderer. The saved fields stay the truth.
     */
    public static final EntityDataAccessor<Float> STORED_LUCK = SynchedEntityData.defineId(AvatarEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Integer> OUTCOME_TIMER = SynchedEntityData.defineId(AvatarEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> SPECIAL = SynchedEntityData.defineId(AvatarEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> STANCE = SynchedEntityData.defineId(AvatarEntity.class, EntityDataSerializers.INT);

    private static final ResourceLocation RACE_PREFIX = ResourceLocation.fromNamespaceAndPath("nusmp", "avatar_race");
    private static final ResourceLocation PERK_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "avatar_perk");
    private static final ResourceLocation TIER_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "avatar_ordinary_tier");

    // ---- saved state. Field initialisers run after the Mob constructor (which already called registerGoals), so every read of these in
    // buildGoals() goes through an accessor that tolerates the not-yet-initialised value.
    private UUID creator;
    private double storedLuck;
    private String avatarName = "";
    private boolean named;
    private double permanentTax;
    private int outcomeTimer = 72000;
    private boolean hasUniqueOrUltimate;
    private Outcome outcome = Outcome.PENDING;
    private int[] stats = new int[0];
    private AvatarUniqueSkill.Perk perk;
    /** Not saved: the game time until which a betrayed Avatar must not attack (the creator's grace after a teleport). */
    private long graceUntil;
    /** Follow or Delve (Stay is the vanilla "ordered to sit" flag, see {@link #stance()}). */
    private Stance stance = Stance.FOLLOW;
    /** Hands, armour, trinkets and pockets in one container (see {@link AvatarInventory}). */
    private final AvatarInventory inventory = new AvatarInventory(this);
    /** Not saved: the way it fights right now, set by the weapon AI from what it holds. */
    private CombatStyle combatStyle = CombatStyle.MELEE;
    /** Not saved: a hand changed, so the weapon AI must look again at what it holds (the watcher goal does it within a tick). */
    private boolean styleStale = true;
    /** Not saved: where it stands while told to stay (the leash of its fights is measured from here). */
    private Vec3 stayPost;
    /** Not saved: Tensura's own "ordered to stay" as read once a second (in every case seen it IS the sit flag; read apart in case its command sets only its own state). */
    private boolean tensuraStays;

    protected AvatarEntity(EntityType<? extends AvatarEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        AttributeSupplier.Builder b = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.ARMOR, 0.0)
                .add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.ATTACK_KNOCKBACK, 0.2).add(Attributes.SCALE, 1.0);
        try {
            b.add(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MAX_MAGICULE, 100.0)
                    .add(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MAX_AURA, 100.0);
        } catch (Throwable tensuraMissing) { /* Tensura adds its own attributes to every living type; this is only a belt for builds that do not */ }
        return b;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(IS_FRENZIED, false).define(APPEARANCE, new CompoundTag()).define(PRIMARY, AvatarPersonality.LOYAL.name()).define(SECONDARY, "").define(RACE, "").define(APTITUDES, "")
                .define(STORED_LUCK, 0.0f).define(OUTCOME_TIMER, 72000).define(SPECIAL, false).define(STANCE, 0);
    }

    // ================================================================ accessors (the contract)
    public UUID creatorUuid() { return creator; }

    public ServerPlayer creatorPlayer() {
        if (creator == null || !(level() instanceof ServerLevel sl)) return null;
        return sl.getServer().getPlayerList().getPlayer(creator);
    }

    public String avatarName() { return avatarName.isEmpty() ? getName().getString() : avatarName; }

    public boolean isNamed() { return named; }

    public AvatarPersonality primary() { return AvatarPersonality.byName(getEntityData().get(PRIMARY)); }

    /** Null when the Avatar has no secondary personality. */
    public AvatarPersonality secondary() { return AvatarPersonality.byName(getEntityData().get(SECONDARY)); }

    public boolean frenzied() { return getEntityData().get(IS_FRENZIED); }

    public void setFrenzied(boolean on) { getEntityData().set(IS_FRENZIED, on); }

    public AppearanceData appearance() { return AppearanceData.fromTag(getEntityData().get(APPEARANCE)); }

    /** The creator's luck at creation. On the client this is the float mirror of the saved double (see {@link #STORED_LUCK}). */
    public double storedLuck() { return level().isClientSide ? getEntityData().get(STORED_LUCK) : storedLuck; }

    public String raceId() { return getEntityData().get(RACE); }

    /**
     * Multiplier on skill cooldowns, exactly the contract: 1.0 normally and 0.1 in a Yandere frenzy (90% reduction). The intelligence / wisdom and
     * personality scalings are applied by the AI module itself (it reads the scores through {@link HasDndStats}), so they are not counted here twice.
     */
    public float cooldownMultiplier() { return frenzied() ? 0.1f : 1.0f; }

    /** The rolled score by short name ("STR".."CHA"); 10 when the Avatar has none (read by the AI module's cooldown and casting maths). */
    @Override
    public int dndStat(String code) {
        Stat st = Stat.byName(code);
        return st == null || stats == null || stats.length <= st.ordinal() ? 10 : stats[st.ordinal()];
    }

    /** True when this personality (primary or secondary) is active for the Avatar. */
    public boolean has(AvatarPersonality p) { return primary() == p || secondary() == p; }

    // ---- additions
    public Outcome outcomeState() { return outcome == null ? Outcome.PENDING : outcome; }

    /** Ticks until the outcome roll. On the client this is the once-a-second mirror (see {@link #OUTCOME_TIMER}). */
    public int outcomeTimer() { return level().isClientSide ? getEntityData().get(OUTCOME_TIMER) : outcomeTimer; }

    public double permanentTax() { return permanentTax; }

    public boolean hasUniqueOrUltimate() { return level().isClientSide ? getEntityData().get(SPECIAL) : hasUniqueOrUltimate; }

    public int[] stats() { return stats == null ? new int[0] : stats.clone(); }

    public AvatarUniqueSkill.Perk perk() { return perk; }

    public boolean isBetrayed() { return outcomeState() == Outcome.BETRAYAL; }

    /** True after a Soul Awakening: the Avatar no longer takes orders and follows nobody. */
    public boolean isIndependent() { return outcomeState() == Outcome.SOUL_AWAKENING; }

    /** True while the Avatar serves its creator (everything except betrayal and independence). */
    public boolean isLoyal() { return creator != null && !isBetrayed() && !isIndependent(); }

    public long graceUntil() { return graceUntil; }

    public void setGraceUntil(long gameTime) { graceUntil = gameTime; }

    public void setCreator(UUID id) {
        creator = id;
        syncOwnerData();
    }

    public void setStoredLuck(double luck) {
        storedLuck = luck;
        getEntityData().set(STORED_LUCK, (float) luck);
    }

    public void setOutcomeState(Outcome o) {
        outcome = o == null ? Outcome.PENDING : o;
        syncOwnerData();
    }

    public void setOutcomeTimer(int ticks) {
        outcomeTimer = ticks;
        if (ticks <= 0 || ticks % 20 == 0) getEntityData().set(OUTCOME_TIMER, ticks);          // the mirror moves once a second (and at once when the roll is cancelled or done)
    }

    public void setPermanentTax(double fraction) { permanentTax = Math.max(0.0, fraction); }

    public void setNamed(boolean v) { named = v; }

    public void setHasUniqueOrUltimate(boolean v) {
        hasUniqueOrUltimate = v;
        getEntityData().set(SPECIAL, v);
    }

    /** 1.58.0: the aptitude ids (see {@code aptitude.AptitudeCatalog}); empty when none. */
    public java.util.List<String> magicAptitudes() {
        String s = getEntityData().get(APTITUDES);
        return s.isEmpty() ? java.util.List.of() : java.util.List.of(s.split(","));
    }

    /** 1.58.0: replaces the aptitudes (ids are cleaned to snake_case, duplicates dropped, at most 8 kept). */
    public void setMagicAptitudes(java.util.List<String> ids) {
        java.util.LinkedHashSet<String> clean = new java.util.LinkedHashSet<>();
        if (ids != null) for (String id : ids) if (id != null && id.matches("[a-z][a-z0-9_]*") && clean.size() < 8) clean.add(id);
        getEntityData().set(APTITUDES, String.join(",", clean));
    }

    private String chosenUnique = "", chosenUltimate = "";

    /** 1.58.0: the Unique and Ultimate skill ids picked in the Soul Forge ("tensura:..."), empty when none. */
    public String chosenUnique() { return chosenUnique; }

    public String chosenUltimate() { return chosenUltimate; }

    public void setChosenSkills(String uniqueId, String ultimateId) {
        chosenUnique = uniqueId == null ? "" : uniqueId;
        chosenUltimate = ultimateId == null ? "" : ultimateId;
    }

    public void setPersonalities(AvatarPersonality primary, AvatarPersonality secondary) {
        getEntityData().set(PRIMARY, (primary == null ? AvatarPersonality.LOYAL : primary).name());
        getEntityData().set(SECONDARY, secondary == null || secondary == primary ? "" : secondary.name());
    }

    // ---- the Avatar rebuild: stance, inventory, combat style
    /**
     * What the creator told it to do. Stay is not stored apart: it IS the vanilla "ordered to sit" flag, which is also what Tensura's Stay command sets, so
     * any of them (this mod's menu, Tensura's command) can give the order and any can take it back. Otherwise Follow or Delve.
     */
    public Stance stance() {
        if (level().isClientSide) return Stance.byOrdinal(getEntityData().get(STANCE));            // the sit flag and the saved stance are server facts; the mirror is what a client sees
        return isOrderedToSit() || tensuraStays ? Stance.STAY : stance;
    }

    /** Is the Avatar told to stay (by the sit flag, or by Tensura's own command as of the last second)? */
    public boolean staying() { return stance() == Stance.STAY; }

    /** Gives a stance order: Stay sits the Avatar down in every system ({@link TensuraBond#order}); Follow and Delve stand it up. */
    public void setStance(Stance s) {
        Stance next = s == null ? Stance.FOLLOW : s;
        if (next == Stance.STAY) {
            TensuraBond.order(this, true);
        } else {
            stance = next;
            TensuraBond.order(this, false);
        }
        getEntityData().set(STANCE, next.ordinal());
    }

    /** Hands, armour, trinkets and pockets. */
    public AvatarInventory inventory() { return inventory; }

    /** The way it fights right now (set by the weapon AI every few ticks). */
    public CombatStyle combatStyle() { return combatStyle; }

    public void setCombatStyle(CombatStyle s) { combatStyle = s == null ? CombatStyle.MELEE : s; }

    /** True once after a hand changed (the weapon AI re-reads what it holds when it sees this). */
    public boolean consumeStyleStale() {
        boolean was = styleStale;
        styleStale = false;
        return was;
    }

    /** Re-reading what it holds on the next tick of the watcher (a hand changed, or the ammunition ran out). */
    public void markStyleStale() { styleStale = true; }

    /** Where it stands while told to stay; null when it is not staying. A stay that was loaded from a save has no post yet: it is the spot where it woke up. */
    public Vec3 stayPost() {
        if (!staying()) return null;
        if (stayPost == null) stayPost = position();
        return stayPost;
    }

    @Override
    public void setOrderedToSit(boolean sit) {
        super.setOrderedToSit(sit);
        stayPost = sit ? position() : null;
    }

    /** A change of either hand makes the weapon AI look again at what the Avatar holds (a bow in the hand turns it into an archer within a tick). */
    @Override
    public void setItemSlot(net.minecraft.world.entity.EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);
        if (slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND || slot == net.minecraft.world.entity.EquipmentSlot.OFFHAND) styleStale = true;
    }

    /** Is this player the creator? */
    public boolean isCreator(Player p) { return p != null && creator != null && creator.equals(p.getUUID()); }

    public void setAppearance(AppearanceData data) {
        getEntityData().set(APPEARANCE, data.toTag());
        AttributeInstance scale = getAttribute(Attributes.SCALE);
        if (scale != null && data.scale() > 0.1f) scale.setBaseValue(data.scale());
    }

    /** Sets the display name (not a formal naming: that is {@link TensuraNamingEngine}). */
    public void setAvatarName(String name) {
        avatarName = name == null ? "" : name;
        setCustomName(avatarName.isEmpty() ? null : Component.literal(avatarName));
        setCustomNameVisible(!avatarName.isEmpty());
    }

    /**
     * The creator while the Avatar serves them, nobody after a betrayal or an awakening. The server answers from the saved facts; a client has none of them (the
     * creator is not synched), so it reads the vanilla owner value that {@link #syncOwnerData} keeps in step and the game synches the usual way.
     */
    @Override
    public UUID getOwnerUUID() { return level().isClientSide ? super.getOwnerUUID() : (isLoyal() ? creator : null); }

    /** Tensura (and the vanilla team and ally rules) treat the Avatar as tamed exactly while it serves its creator (a client reads the synched tame flag). */
    @Override
    public boolean isTame() { return level().isClientSide ? super.isTame() : isLoyal(); }

    /** Writes the owner and the tame flag into the vanilla synched data, so clients (and Tensura's client side) see what the server derives. Cheap: writes only a change. */
    private void syncOwnerData() {
        if (level().isClientSide) return;
        UUID wanted = isLoyal() ? creator : null;
        if (!java.util.Objects.equals(super.getOwnerUUID(), wanted)) setOwnerUUID(wanted);
        boolean tame = isLoyal();
        if (super.isTame() != tame) setTame(tame, false);
    }

    // ---- the parts of Animal that make no sense for a person: nobody feeds, breeds or lures an Avatar
    @Override public boolean isFood(ItemStack stack) { return false; }

    @Override public boolean canFallInLove() { return false; }

    @Override public boolean canMate(Animal other) { return false; }

    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) { return null; }

    /** An Avatar is never a child: the age system of the animal parent would make one in twenty a baby when it is spawned by a command or an egg. */
    @Override public boolean isBaby() { return false; }

    @Override public void setBaby(boolean baby) { /* never */ }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
                                                                   net.minecraft.world.entity.MobSpawnType type, net.minecraft.world.entity.SpawnGroupData data) {
        net.minecraft.world.entity.SpawnGroupData out = super.finalizeSpawn(level, difficulty, type, data);
        setAge(0);
        return out;
    }

    // ================================================================ creation and evolution
    /**
     * Fills a freshly made Avatar from the creation request. Called once by {@link AvatarCreation} before the entity is added to the world.
     */
    public void initFromCreation(ServerPlayer owner, AvatarRace race, int[] rolled, double luck, AvatarPersonality first, AvatarPersonality second,
                                 AppearanceData look, String name) {
        creator = owner.getUUID();
        storedLuck = luck;
        stats = rolled.clone();
        outcome = Outcome.PENDING;
        outcomeTimer = AvatarConfig.outcomeTicks();
        getEntityData().set(RACE, race.id());
        setPersonalities(first, second);
        setAppearance(look);
        setAvatarName(name);
        applyRace(race, 1.0);
        TensuraBridge.setNaming(this, null, null, true);
        setStoredLuck(luck);
        getEntityData().set(OUTCOME_TIMER, outcomeTimer);
        syncOwnerData();
        TensuraBond.sync(this);
        rebuildGoals();
    }

    /** (Re)applies the race: score-derived base values, the race's extra attributes and the EP pools. {@code epBonus} 1.0 = none (the naming surge passes more). */
    public void applyRace(AvatarRace race, double epBonus) {
        AvatarStats.Derived d = AvatarStats.derive(stats.length >= 6 ? stats : new int[]{12, 12, 12, 12, 12, 12}, race.multipliers(), race.baseEp(), epBonus);
        setBase(Attributes.MAX_HEALTH, d.maxHealth());
        setBase(Attributes.ATTACK_DAMAGE, d.attackDamage());
        setBase(Attributes.MOVEMENT_SPEED, d.moveSpeed());
        setBase(Attributes.ARMOR, d.armor());
        for (var e : race.baseAttributes().entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(e.getKey());
            if (id == null) continue;
            BuiltInRegistries.ATTRIBUTE.getHolder(id).ifPresent(h -> {
                AttributeInstance inst = getAttribute(h);
                if (inst == null) return;
                ResourceLocation mod = RACE_PREFIX.withSuffix("_" + id.getPath().replace('.', '_'));
                inst.removeModifier(mod);
                inst.addPermanentModifier(new AttributeModifier(mod, e.getValue(), AttributeModifier.Operation.ADD_VALUE));
            });
        }
        TensuraBridge.setEnergy(this, d.ep(), d.magicule(), d.aura());
        setHealth(getMaxHealth());
    }

    private void setBase(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a, double v) {
        AttributeInstance inst = getAttribute(a);
        if (inst != null) inst.setBaseValue(v);
    }

    /** Applies the passive half of a Soul Awakening perk as one permanent modifier and remembers the perk. */
    public void setPerk(AvatarUniqueSkill.Perk p) {
        perk = p;
        removePerkModifier(Attributes.ATTACK_DAMAGE);
        removePerkModifier(Attributes.MAX_HEALTH);
        removePerkModifier(Attributes.MOVEMENT_SPEED);
        removePerkModifier(Attributes.ARMOR);
        if (p == null) return;
        switch (p.passive()) {
            case ATTACK -> addPerk(Attributes.ATTACK_DAMAGE, p.passiveAmount(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            case HEALTH -> addPerk(Attributes.MAX_HEALTH, p.passiveAmount(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            case SPEED -> addPerk(Attributes.MOVEMENT_SPEED, p.passiveAmount() * 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            case ARMOR -> addPerk(Attributes.ARMOR, p.passiveAmount() * 20.0, AttributeModifier.Operation.ADD_VALUE);
            case REGEN -> { /* applied in the tick */ }
        }
    }

    private void removePerkModifier(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a) {
        AttributeInstance inst = getAttribute(a);
        if (inst != null) inst.removeModifier(PERK_ID);
    }

    private void addPerk(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = getAttribute(a);
        if (inst != null) inst.addPermanentModifier(new AttributeModifier(PERK_ID, amount, op));
    }

    /** A modest permanent lift for the "standard high-tier NPC" result of an Ordinary Existence. */
    public void applyOrdinaryTier() {
        AttributeInstance hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && !hp.hasModifier(TIER_ID)) hp.addPermanentModifier(new AttributeModifier(TIER_ID, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        AttributeInstance atk = getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null && !atk.hasModifier(TIER_ID)) atk.addPermanentModifier(new AttributeModifier(TIER_ID, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    // ================================================================ goals
    @Override
    protected void registerGoals() { buildGoals(); }

    /** Throws every goal away and builds the set the current state needs (called after creation, after loading, and whenever the outcome changes). */
    public void rebuildGoals() {
        goalSelector.removeAllGoals(g -> true);
        targetSelector.removeAllGoals(g -> true);
        buildGoals();
    }

    private void buildGoals() {
        Outcome o = outcomeState();
        goalSelector.addGoal(0, new FloatGoal(this));
        if (o == Outcome.BETRAYAL) {
            targetSelector.addGoal(1, new AvatarHuntCreatorGoal(this));
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        } else {
            goalSelector.addGoal(2, new AvatarMeleeGoal(this, 1.15, true));
            if (o != Outcome.SOUL_AWAKENING) {
                goalSelector.addGoal(4, new AvatarFollowCreatorGoal(this, o == Outcome.ORDINARY));
                targetSelector.addGoal(1, new AvatarDefendSelfOrCreatorGoal(this));
            } else {
                targetSelector.addGoal(1, new HurtByTargetGoal(this));
            }
            // the awakened perk's move: pressed by the virtual skill loadout now; the old stand-alone goal only runs when the loadout is switched off in the config
            if (perk != null && !AvatarAiConfig.loadoutEnabled()) goalSelector.addGoal(3, new AvatarSignatureGoal(this));
            AvatarAi.install(this, goalSelector, targetSelector);
        }
        goalSelector.addGoal(6, new AvatarStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, ServerPlayer.class, 8.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    // ================================================================ tick
    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;
        if (!AvatarConfig.enabled()) return;
        AvatarOutcomes.tick(this, sl);
        if (tickCount % 20 == 5) slowTick();
    }

    /** Once a second: the mirrors of the saved facts, Tensura's owner record, the party's bookkeeping. Never throws into the tick. */
    private void slowTick() {
        try {
            getEntityData().set(OUTCOME_TIMER, outcomeTimer);
            getEntityData().set(STANCE, stance().ordinal());                      // Tensura's own Stay command moves the flag without us: the mirror follows
            syncOwnerData();
            tensuraStays = TensuraBond.tensuraStays(this);
            TensuraBond.sync(this);
            AvatarPartyManager.tick(this);
            AvatarEvolution.tick(this);
        } catch (RuntimeException soft) {
            // a bookkeeping glitch must never stop the Avatar
        }
    }

    /** Heals a little each second for a REGEN perk (called by the outcome tick). */
    public void perkRegen() {
        if (perk != null && perk.passive() == AvatarUniqueSkill.Passive.REGEN && getHealth() < getMaxHealth()) heal((float) (getMaxHealth() * perk.passiveAmount() * 0.02));
    }

    // ================================================================ interaction
    /** The creator's empty hand opens the Avatar's action menu (AV3's screen); anything else falls through to the vanilla behaviour (items, relics, leads). */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && player.getItemInHand(hand).isEmpty() && creator != null && player.getUUID().equals(creator)) {
            if (!level().isClientSide && player instanceof ServerPlayer sp) {
                // sneak + empty hand opens the inventory (the interaction listener does it first, so a relic pack cannot get in the way; this is the fallback)
                if (player.isShiftKeyDown()) AvatarInventoryMenu.open(sp, this); else AvatarActionMenu.open(sp, this, infoLines());
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    /** A few plain lines about this Avatar for the info tab and the {@code /nusmp avatar info} command. */
    public List<String> infoLines() {
        AvatarRace r = race();
        List<String> out = new ArrayList<>();
        out.add(avatarName() + (named ? " (named)" : ""));
        out.add("Race: " + (r == null ? raceId() : r.displayName()) + ", " + primary().displayName + (secondary() == null ? "" : " / " + secondary().displayName));
        if (stats != null && stats.length == 6) {
            StringBuilder sb = new StringBuilder("Scores:");
            for (Stat st : Stat.values()) sb.append(' ').append(st.name()).append(' ').append(stats[st.ordinal()]);
            out.add(sb.toString());
        }
        out.add("Creator luck: " + String.format(java.util.Locale.ROOT, "%.1f", storedLuck));
        out.add("State: " + outcomeState().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ')
                + (outcomeState() == Outcome.PENDING && outcomeTimer > 0 ? ", fate in " + (outcomeTimer / 1200) + " min" : ""));
        if (permanentTax > 0) out.add("Magicule tax on its creator: " + Math.round(permanentTax * 100) + "%");
        if (perk != null) out.add("Unique skill: " + perk.describe());
        out.add("Stance: " + stance().name().toLowerCase(java.util.Locale.ROOT));
        String skills = VirtualSkillLoadout.describe(this);
        if (!skills.isEmpty()) out.add("Skills: " + skills);
        return out;
    }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    // ================================================================ death and removal
    @Override
    public void die(DamageSource source) {
        super.die(source);                  // the tamable parent also tells a loyal Avatar's owner how it died (the death message, when that game rule is on); the 1.51.0 line below is kept
        if (!(level() instanceof ServerLevel sl)) return;
        ServerPlayer owner = creatorPlayer();
        if (owner != null) owner.sendSystemMessage(Component.literal(avatarName() + " has fallen.").withStyle(net.minecraft.ChatFormatting.DARK_RED));
        cleanup(sl);
    }

    /** Everything the Avatar carries is dropped (or goes to its creator, see {@code avatar.dropGearOnDeath}); the vanilla equipment drop is not used. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        AvatarGear.onDeath(this, level);
    }

    /** A shield that blocks a hit wears out, like a player's (a mob's shield does not by default). */
    @Override
    protected void hurtCurrentlyUsedShield(float damage) {
        if (!useItem.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SHIELD_BLOCK) || damage < 3.0f) return;
        int wear = 1 + net.minecraft.util.Mth.floor(damage);
        InteractionHand hand = getUsedItemHand();
        useItem.hurtAndBreak(wear, this, LivingEntity.getSlotForHand(hand));
        if (useItem.isEmpty()) {
            setItemSlot(hand == InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            useItem = ItemStack.EMPTY;
            playSound(net.minecraft.sounds.SoundEvents.SHIELD_BREAK, 0.8f, 0.8f + level().random.nextFloat() * 0.4f);
        }
    }

    /** The arrow a bow or crossbow loads: a free plain one, or one from the pockets when {@code avatar.consumeAmmo} is on (EMPTY when there is none). */
    @Override
    public ItemStack getProjectile(ItemStack weapon) {
        return weapon.getItem() instanceof ProjectileWeaponItem ? AvatarGear.takeArrow(this) : ItemStack.EMPTY;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && level() instanceof ServerLevel sl) cleanup(sl);
        super.remove(reason);
    }

    private boolean cleaned;

    /** Drops the Avatar from the manager and the creator's list and lifts its tax; safe to call twice. */
    private void cleanup(ServerLevel sl) {
        if (cleaned) return;
        cleaned = true;
        AvatarManager.get(sl.getServer()).remove(getUUID());
        AvatarPartyManager.onAvatarGone(sl.getServer(), this);
        ServerPlayer owner = creatorPlayer();
        if (owner != null) {
            AvatarSystem.removeFromPlayer(owner, getUUID());
            TensuraNamingEngine.refreshTax(owner);
        }
    }

    // ================================================================ save and load
    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        if (creator != null) t.putUUID("CreatorUUID", creator);
        t.putDouble("CreatorStoredLuck", storedLuck);
        t.putString("AvatarName", avatarName);
        t.putBoolean("IsNamed", named);
        t.putString("PrimaryPersonality", getEntityData().get(PRIMARY));
        t.putString("SecondaryPersonality", getEntityData().get(SECONDARY));
        t.putDouble("PermanentMagiculeTax", permanentTax);
        t.putInt("OutcomeTimer", outcomeTimer);
        t.putBoolean("HasUniqueOrUltimateSkill", hasUniqueOrUltimate);
        t.putString("MagicAptitudes", getEntityData().get(APTITUDES));
        t.putString("ChosenUnique", chosenUnique);
        t.putString("ChosenUltimate", chosenUltimate);
        t.put("AppearanceData", getEntityData().get(APPEARANCE).copy());
        t.putString("RaceId", getEntityData().get(RACE));
        t.putString("OutcomeState", outcomeState().name());
        t.putIntArray("Stats", stats == null ? new int[0] : stats);
        t.putString("Stance", stance.name());
        net.minecraft.nbt.ListTag pockets = inventory.savePockets(registryAccess());
        if (!pockets.isEmpty()) t.put("Pockets", pockets);
        if (perk != null) {
            CompoundTag p = new CompoundTag();
            p.putString("Id", perk.id());
            p.putDouble("Amount", perk.passiveAmount());
            p.putDouble("Power", perk.signaturePower());
            p.putInt("Cooldown", perk.cooldownTicks());
            t.put("Perk", p);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        creator = t.hasUUID("CreatorUUID") ? t.getUUID("CreatorUUID") : null;
        storedLuck = t.getDouble("CreatorStoredLuck");
        avatarName = t.getString("AvatarName");
        named = t.getBoolean("IsNamed");
        getEntityData().set(PRIMARY, t.contains("PrimaryPersonality") ? t.getString("PrimaryPersonality") : AvatarPersonality.LOYAL.name());
        getEntityData().set(SECONDARY, t.getString("SecondaryPersonality"));
        permanentTax = t.getDouble("PermanentMagiculeTax");
        outcomeTimer = t.contains("OutcomeTimer") ? t.getInt("OutcomeTimer") : AvatarConfig.outcomeTicks();
        hasUniqueOrUltimate = t.getBoolean("HasUniqueOrUltimateSkill");
        getEntityData().set(APTITUDES, t.getString("MagicAptitudes"));
        chosenUnique = t.getString("ChosenUnique");
        chosenUltimate = t.getString("ChosenUltimate");
        if (t.contains("AppearanceData")) getEntityData().set(APPEARANCE, t.getCompound("AppearanceData"));
        getEntityData().set(RACE, t.getString("RaceId"));
        outcome = Outcome.byName(t.getString("OutcomeState"));
        stats = t.getIntArray("Stats");
        stance = Stance.byName(t.getString("Stance"));
        if (stance == Stance.STAY) stance = Stance.FOLLOW;                                  // Stay is the sit flag, which TamableAnimal saves itself
        getEntityData().set(STANCE, stance().ordinal());
        getEntityData().set(STORED_LUCK, (float) storedLuck);
        getEntityData().set(OUTCOME_TIMER, outcomeTimer);
        getEntityData().set(SPECIAL, hasUniqueOrUltimate);
        inventory.loadPockets(t.contains("Pockets", net.minecraft.nbt.Tag.TAG_LIST) ? t.getList("Pockets", net.minecraft.nbt.Tag.TAG_COMPOUND) : null, registryAccess());
        if (t.contains("Perk")) {
            CompoundTag p = t.getCompound("Perk");
            perk = AvatarUniqueSkill.restore(p.getString("Id"), p.getDouble("Amount"), p.getDouble("Power"), p.getInt("Cooldown"));
        }
        setPersistenceRequired();
        syncOwnerData();
        rebuildGoals();
    }

    /** Used by the race registry check in commands: the race record of this Avatar, or null. */
    public AvatarRace race() { return AvatarRaces.get(raceId()); }

    /** A seeded java Random from the entity's own random source (for the pure generators). */
    public Random seeded() { return new Random(getRandom().nextLong()); }
}
