package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.newuniverse.nusmp.boss.budget.BossBudget;
import com.newuniverse.nusmp.boss.budget.BudgetedBoss;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.guard.GuardianScaling.Trait;
import com.newuniverse.nusmp.labyrinth.seal.FloorWardenEntity;
import com.newuniverse.nusmp.labyrinth.seal.PeakAltarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Why: the boss of a sector. It IS a Floor Warden (the Labyrinth's own mini-boss: same model, armour layer, melee AI, Ground Slam, Shadow
 * Step, two Living Armors at half health, no starvation, boss bar) because the lore holds and the proven code is reused, and it differs in
 * exactly the ways the Mega-Dungeon needs. (1) It SCALES: {@link GuardianScaling} turns the sector's threat into health, damage, armour and
 * size (the SCALE attribute, so the hitbox and the model grow together), and what the max-health attribute cannot hold
 * becomes a damage divisor. (2) It has a THEME TRAIT: the sector's theme picks one of nine abilities ({@link GuardianTraits}); the second
 * phase (under half health, when the Warden's two Living Armors step out) strengthens it. (3) It OBEYS THE BOSS RULES: it is a boss for
 * {@code BossIdentity} (it is a Floor Warden), it is a {@link BudgetedBoss} so the per-second damage budget keeps a max-EP build from
 * deleting it, boss heals are dropped in combat (so the BLOOD trait feeds a shield, never health), and it carries the {@code nusmp_peak_boss}
 * tag, which keeps the 1.36.0 economy, the HP buffer and the Vital Strike off it. (4) It is LEASHED to its hall: a Guardian led out of the
 * arena is carried back to its altar, because a boss that can be kited through the sector is a different, broken fight. (5) It drops
 * NOTHING: no loot table and none of the Warden's Shadow Keys; the Mana Core is forged by its altar when the death is confirmed, so the
 * progression has exactly one owner ({@link GuardianAltarBlockEntity}) and a Guardian spawned by an admin for a test cannot unseal a
 * sector. Its boss-bar title carries the theme ({@code entity.nusmp.labyrinth_guardian.bar}) and, when the sector can say which floor it
 * gates, that floor ({@code entity.nusmp.labyrinth_guardian.bar_floor}, "Floor +10 Warden (Frost)"), through the custom name the Warden's bar
 * already shows. (6) It is COLOSSAL (1.55.0: "the colossal boss that gates every tenth floor"): the SCALE attribute starts at
 * {@code GuardConfig.guardianScale} (1.8) and grows a little with the threat; the hitbox follows by itself ({@code LivingEntity.getDimensions}
 * scales the type's size by the attribute) and so does the model, because {@code LivingEntityRenderer.render} applies the attribute before
 * the Warden renderer's own fixed 1.75 (the two multiply). (7) The modules that act on a sector's boss find it by two keys: the scoreboard
 * tag {@link #SECTOR_TAG} and the persistent-data int {@link #SECTOR_KEY} (the sector id, -1 when it belongs to none), both written when
 * it is configured and again when it is loaded.
 */
public class LabyrinthGuardianEntity extends FloorWardenEntity implements BudgetedBoss {
    /** The tag every Labyrinth Guardian carries (the Peak Altar's boss tag is added too: the economy, the HP buffer and the Vital Strike read that one). */
    public static final String TAG = "nusmp_mega_guardian";
    /** 1.55.0: the scoreboard tag the modules that act on a sector's colossal boss look for (the Metal Vessel of the Rukh flow, the guaranteed Rare piece of the gear module). */
    public static final String SECTOR_TAG = "nusmp_sector_guardian";
    /** 1.55.0: the persistent-data int that goes with {@link #SECTOR_TAG}: the id of the sector this Guardian gates, -1 for a test Guardian that belongs to none. */
    public static final String SECTOR_KEY = "nusmp_sector";
    /** Half the width of the arena footprint (45 blocks) and the slack beyond its walls before the leash pulls. */
    private static final double HALL_HALF = 22.0, LEASH_SLACK = 3.0;

    /**
     * 1.94.0: what the renderer needs to draw the boss as the creature it is (docs/labyrinth/abyssal_bosses.md): the id of its abyssal look ("" = the look the Guardian always had), the phase it is in (1 to 3:
     * the model it wears), and the game time and kind (1 a move, 2 a change of phase) of its last cast, which the cast clip plays. Written on the server only; synced like any entity data.
     */
    private static final EntityDataAccessor<String> LOOK = SynchedEntityData.defineId(LabyrinthGuardianEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> LOOK_PHASE = SynchedEntityData.defineId(LabyrinthGuardianEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> CAST_AT = SynchedEntityData.defineId(LabyrinthGuardianEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> CAST_KIND = SynchedEntityData.defineId(LabyrinthGuardianEntity.class, EntityDataSerializers.INT);

    private final BossBudget dpsBudget = new BossBudget();
    private final GuardianTraits.State traits = new GuardianTraits.State();
    /** 1.65.0: the D&D layer (legendary resistances and actions, lair actions, phases, mythic trait) of the boss of this floor. */
    private final DndBoss dnd = new DndBoss();
    private int floorNo = 1;
    private int sectorId = -1, threat = 1;
    private RegionTheme theme = RegionTheme.GRAVITY;
    private Trait trait = Trait.GRAVITY;
    /** The altar that called it (its leash and where its death is reported); null for a Guardian summoned by hand. */
    private BlockPos anchor;
    private boolean configured, phaseTwo;
    /** The signed floor it gates as the screen writes it ("+10"), or null when no sector could say (a test Guardian). */
    private String floorLabel;
    /** Incoming damage is divided by this (1 or more): the health the max-health attribute could not hold. */
    private double divisor = 1.0;

    public LabyrinthGuardianEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        stamp();
    }

    /** Puts the three tags and the sector key on it: at construction, when it is configured and when it is loaded (an old save or a cleared tag list keeps all of them). */
    private void stamp() {
        addTag(PeakAltarBlockEntity.BOSS_TAG);
        addTag(TAG);
        addTag(SECTOR_TAG);
        getPersistentData().putInt(SECTOR_KEY, sectorId);
    }

    /** The Warden's attributes plus the two a scaled, shielded Guardian reads. */
    public static AttributeSupplier.Builder guardianAttributes() {
        return FloorWardenEntity.attributes().add(Attributes.SCALE, 1.0).add(Attributes.MAX_ABSORPTION, 0.0);
    }

    // ---------------------------------------------------------------- 1.94.0: the look
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LOOK, "");
        builder.define(LOOK_PHASE, 1);
        builder.define(CAST_AT, -1000L);
        builder.define(CAST_KIND, 0);
    }

    /** The id of the abyssal look it wears ("" = the look the Guardian always had); read on the client by the renderer. */
    public String lookId() { return entityData.get(LOOK); }

    /** The phase of the look, 1 to 3. */
    public int lookPhase() { return Math.max(1, Math.min(3, entityData.get(LOOK_PHASE))); }

    /** The game time of its last cast and its kind (0 none, 1 a move, 2 a change of phase). */
    public long castAt() { return entityData.get(CAST_AT); }

    public int castKind() { return entityData.get(CAST_KIND); }

    /** Server: it has just cast (a legendary action, a lair action, a change of phase): the cast clip plays for a second on every client. */
    void noteCast(int kind) {
        if (level().isClientSide) return;
        entityData.set(CAST_AT, level().getGameTime());
        entityData.set(CAST_KIND, kind);
    }

    /** Puts the look its profile (or its demon sector) gives it, and its phase, into the synced data. Called whenever the profile is set. */
    void syncLook() {
        DndProfile p = dnd.profile;
        entityData.set(LOOK, GuardConfig.get(GuardConfig.DND) ? AbyssalLooks.resolve(p.look(), theme.id(), p.side(), p.tier()) : "");
        entityData.set(LOOK_PHASE, dnd.phase());
    }

    // ---------------------------------------------------------------- creation
    /**
     * Makes a Guardian for a sector (or a plain threat-1 one for {@code sector == null}) standing at {@code feet}: configured, finalised as a
     * structure spawn and persistent, but NOT yet added to the level (the caller does that). {@code anchor} is the altar, or null for a test
     * Guardian that has no leash and reports its death to nobody.
     */
    public static LabyrinthGuardianEntity create(ServerLevel sl, BlockPos feet, Sector sector, BlockPos anchor) {
        LabyrinthGuardianEntity g = GuardRegistry.GUARDIAN.get().create(sl);
        if (g == null) return null;
        g.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, sl.getRandom().nextFloat() * 360f, 0f);
        g.configure(sector, anchor);
        g.finalizeSpawn(sl, sl.getCurrentDifficultyAt(feet), MobSpawnType.STRUCTURE, null);
        g.setPersistenceRequired();
        return g;
    }

    /** Makes this the boss of {@code sector} (null: a plain threat-1 test Guardian), calling from {@code anchor} (the altar, or null) and rescales it. */
    public void configure(Sector sector, BlockPos anchor) {
        floorNo = sector == null ? 1 : sector.lastFloor();
        configure(sector == null ? -1 : sector.id, sector == null ? 1 : com.newuniverse.nusmp.labyrinth.mega.FloorMap.threat(sector.lastFloor()),   // 1.55.0: the decennial boss stands on the sector's LAST floor, so it is as tough as that floor's monsters and then some
                 sector == null ? RegionTheme.GRAVITY : sector.theme, anchor,
                sector == null ? null : FloorMap.label(sector.lastFloor()));
    }

    /**
     * 1.72.0: the secret boss of a pocket realm (a rift): it gates no sector (id -1) and calls from no altar, but it is scaled and given its D&D profile by the signed floor the rift was opened on, and
     * the boss bar names that floor.
     */
    public void configureForFloor(int floor, RegionTheme theme) {
        floorNo = Math.max(1, Math.abs(floor));
        configure(-1, FloorMap.threat(floor), theme, null, FloorMap.label(floor));
    }

    /** Sets which sector's boss this is (a bare id, for tests) and rescales it; the boss bar then names no floor. */
    public void configure(int sectorId, int threat, RegionTheme theme, BlockPos anchor) {
        floorNo = Math.max(1, threat);
        configure(sectorId, threat, theme, anchor, null);
    }

    private void configure(int sectorId, int threat, RegionTheme theme, BlockPos anchor, String floorLabel) {
        this.sectorId = sectorId;
        this.threat = GuardianScaling.clampThreat(threat);
        this.theme = theme == null ? RegionTheme.GRAVITY : theme;
        this.trait = GuardianScaling.traitOf(this.theme);
        this.anchor = anchor == null ? null : anchor.immutable();
        this.floorLabel = floorLabel;
        this.configured = true;
        stamp();
        dnd.configure(DndProfiles.forBoss(floorNo, sectorId));
        syncLook();
        applyScaling();
    }

    /** The Warden's finalize sets its own health from the config; the Guardian puts its sector's numbers back (or reads the sector under it when summoned by hand). */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, type, data);
        if (configured) applyScaling();
        else {
            configure(MegaDungeon.sectorAt(level.getLevel(), blockPosition()), null);
        }
        return out;
    }

    /** Writes the threat's numbers into the attributes, fills health, names the boss bar and resizes the body. */
    private void applyScaling() {
        double base = GuardConfig.get(GuardConfig.GUARDIAN_BASE_HEALTH), perThreat = GuardConfig.get(GuardConfig.THREAT_HEALTH_SCALE);
        double hp = GuardianScaling.health(base, perThreat, threat);
        divisor = GuardianScaling.damageDivisor(base, perThreat, threat);
        put(Attributes.MAX_HEALTH, hp);
        put(Attributes.ATTACK_DAMAGE, GuardianScaling.attackDamage(GuardConfig.get(GuardConfig.THREAT_DAMAGE_SCALE), threat));
        put(Attributes.ARMOR, GuardianScaling.armor(threat));
        put(Attributes.SCALE, GuardianScaling.scale(GuardConfig.get(GuardConfig.GUARDIAN_SCALE), threat));
        if (trait == Trait.GRAVITY) put(Attributes.KNOCKBACK_RESISTANCE, 1.0);
        if (trait == Trait.BLOOD) put(Attributes.MAX_ABSORPTION, hp * GuardianScaling.spec(Trait.BLOOD, true).count() / 100.0);
        if (dnd.profile.absorbMagic() || dnd.profile.legendary().contains("life_drain")) put(Attributes.MAX_ABSORPTION, Math.max(getAttributeValue(Attributes.MAX_ABSORPTION), hp * 0.1));      // room for the shield that eaten magic becomes
        dpsBudget.bypass(() -> setHealth((float) hp));                                      // a fresh boss starts full; that is not a heal
        Component themeName = Component.translatable("message.nusmp.mega.mgd.theme." + theme.id());
        setCustomName(floorLabel == null ? Component.translatable("entity.nusmp.labyrinth_guardian.bar", themeName)
                : Component.translatable("entity.nusmp.labyrinth_guardian.bar_floor", floorLabel, themeName));
        setCustomNameVisible(false);
        refreshDimensions();
        traits.restart(level().getGameTime(), 80);
        refreshDndName(dnd.profile, dnd.phase);
    }

    private void put(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        AttributeInstance a = getAttribute(attribute);
        if (a != null) a.setBaseValue(value);
    }

    // ---------------------------------------------------------------- what the sector made it
    public int sectorId() { return sectorId; }

    public int threat() { return threat; }

    public RegionTheme theme() { return theme; }

    public Trait trait() { return trait; }

    public boolean phaseTwo() { return phaseTwo; }

    public BlockPos anchor() { return anchor; }

    /** The runtime state of its trait (cooldowns, trail, stars in the air): the in-game self-tests read and steer it. */
    GuardianTraits.State traitState() { return traits; }

    /** Its melee damage right now (the scaled attribute): what the traits take their share of. */
    public double attackDamage() { return getAttributeValue(Attributes.ATTACK_DAMAGE); }

    private boolean traitsOn() { return GuardConfig.get(GuardConfig.TRAITS); }

    /** The flame Guardian does not burn; everyone else keeps the type's rule (the Guardian's type is not fire immune on its own). */
    @Override
    public boolean fireImmune() { return trait == Trait.FLAME || super.fireImmune(); }

    // ---------------------------------------------------------------- the Warden's slam, scaled
    @Override
    protected float slamDamage() { return (float) Math.max(8.0, attackDamage() * 0.85); }

    /** Four blocks at its size, more with its size, and the gravity trait widens it. */
    @Override
    protected double slamRadius() {
        double r = 4.0 * Math.max(1.0, getScale());
        if (trait == Trait.GRAVITY && traitsOn()) r += GuardianScaling.spec(Trait.GRAVITY, phaseTwo).count();
        return r;
    }

    @Override
    protected void onGroundSlam(ServerLevel sl) {
        if (trait == Trait.STORM && traitsOn()) GuardianTraits.onSlam(this, sl, phaseTwo);
    }

    // ---------------------------------------------------------------- the tick: phase, leash, trait
    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !(level() instanceof ServerLevel sl) || !isAlive()) return;
        try {
            if (!phaseTwo && GuardianScaling.phaseTwo(getHealth(), getMaxHealth())) enterPhaseTwo(sl);
            if (GuardConfig.get(GuardConfig.LEASH) && tickCount % 20 == 0) leash(sl);
            if (traitsOn()) GuardianTraits.tick(this, sl, traits, trait, phaseTwo);
            dnd.tick(this, sl);
        } catch (RuntimeException e) {
            GuardLog.failed("guardian tick", e);                                           // an entity that throws in its tick would crash the server: the fight goes on without the extras
        }
    }

    /** Under half health: a roar, a flash, a word to the party (the Warden's two Living Armors step out on their own at the same line) and a stronger trait. */
    private void enterPhaseTwo(ServerLevel sl) {
        phaseTwo = true;
        traits.restart(sl.getGameTime(), 40);
        com.newuniverse.nusmp.audio.MobVoice.play(this, soundFamily(), com.newuniverse.nusmp.audio.Cue.AGGRO);       // 1.99.0: the roar of its own family (a lich whispers, a dragon roars)
        sl.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + getBbHeight() * 0.5, getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + getBbHeight() * 0.5, getZ(), 60, 1.2, 1.6, 1.2, 0.12);
        for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(48.0), Player::isAlive)) {
            p.displayClientMessage(Component.translatable("message.nusmp.mega.mgd.phase_two").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE, net.minecraft.ChatFormatting.ITALIC), true);
        }
    }

    /** Is a floor point inside the hall and its doorway tunnels (the altar's footprint plus a little)? Always true for a Guardian with no altar. */
    public boolean insideLeash(double x, double z) {
        if (anchor == null) return true;
        double limit = HALL_HALF + LEASH_SLACK;
        return Math.abs(x - (anchor.getX() + 0.5)) <= limit && Math.abs(z - (anchor.getZ() + 0.5)) <= limit;
    }

    /** Carried back to the altar when it has left its hall or fallen out of it (once a second at most). */
    private void leash(ServerLevel sl) {
        if (anchor == null) return;
        boolean out = !insideLeash(getX(), getZ()) || Math.abs(getY() - anchor.getY()) > 20.0;
        if (!out) return;
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.4, getZ(), 30, 0.5, 1.2, 0.5, 0.1);
        teleportTo(anchor.getX() + 0.5, anchor.getY() + 1.0, anchor.getZ() + 0.5);
        getNavigation().stop();
        setTarget(null);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.4, getZ(), 30, 0.5, 1.2, 0.5, 0.1);
        com.newuniverse.nusmp.audio.MobVoice.play(this, soundFamily(), com.newuniverse.nusmp.audio.Cue.CAST);
    }

    // ---------------------------------------------------------------- the voice (1.99.0, audio/MobSoundSet)
    /** The family of the bespoke voice: the {@code sound_set} of its D&D profile, else its look, its name or its side (see {@code BossSoundSets}); living armour when it has no profile. */
    @Override
    public String soundFamily() {
        try {
            DndProfile p = dnd.profile;
            return com.newuniverse.nusmp.audio.BossSoundSets.family(p.id(), lookId().isEmpty() ? p.look() : lookId(), p.name(), p.dnd(), p.side());
        } catch (RuntimeException e) {
            return "armor";
        }
    }

    // ---------------------------------------------------------------- blood: a shield fed by its own hits
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && trait == Trait.BLOOD && traitsOn() && level() instanceof ServerLevel sl) GuardianTraits.onHit(this, sl, target, phaseTwo);
        return hit;
    }

    // ---------------------------------------------------------------- the boss rules: the damage budget, the divisor, no heal in combat
    @Override public BossBudget dpsBudget() { return dpsBudget; }

    @Override public int budgetStage() { return phaseTwo ? 1 : 0; }

    @Override public int budgetFinalStage() { return 1; }

    @Override
    public void baseTick() {
        super.baseTick();
        dpsBudget.tick(this, this);
    }

    @Override
    public void setHealth(float health) {
        super.setHealth(dpsBudget == null ? health : dpsBudget.setHealth(this, health));         // null while the constructors of the parents run
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        float effective = divisor > 1.0 ? (float) (amount / divisor) : amount;                   // health the attribute cannot hold is paid as divided damage
        effective = dnd.incoming(this, source, effective);                                         // 1.65.0: magic eaten, the Word of Ending's concentration
        boolean hit = dpsBudget.hurt(this, source, effective, x -> super.hurt(source, x));
        if (hit && level() instanceof ServerLevel sl && isAlive()) dnd.reacted(this, sl, source);  // a legendary action between the party's turns
        return hit;
    }

    // ---------------------------------------------------------------- death: report it, drop nothing
    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (isAlive()) return;                                                                      // 1.65.0: the Mythic Trait cancelled the death (DndEvents): nothing to report
        try {
            if (anchor != null && level() instanceof ServerLevel sl && sl.isLoaded(anchor) && sl.getBlockEntity(anchor) instanceof GuardianAltarBlockEntity altar) {
                altar.onBossDied(getUUID());
            }
        } catch (RuntimeException e) {
            GuardLog.failed("guardian death notice", e);                                   // the altar also sees the dead boss on its own next poll
        }
    }

    /** No loot table: the Mana Core comes from the altar. */
    @Override
    protected void dropFromLootTable(DamageSource source, boolean hitByPlayer) {}

    /** And not the Warden's Shadow Keys. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean hitByPlayer) {}

    // ---------------------------------------------------------------- 1.65.0: the D&D layer
    /** The name over the boss bar: the profile's name and the form of the phase it is in, then the floor. Nothing changes while the profile is the default one. */
    void refreshDndName(DndProfile profile, int phase) {
        phaseLook(phase);
        if (profile == DndProfile.DEFAULT || !GuardConfig.get(GuardConfig.DND)) return;
        setCustomName(Component.literal(profile.name() + " \u2014 " + profile.form(phase) + (floorLabel == null ? "" : " (Floor " + floorLabel + ")")));
    }

    /**
     * 1.94.0: the look moves to the model of the phase it has reached. A step UP (the boss gets its second form, or its mythic third) is announced above the hotbar with the caption of that phase of its
     * look (the lich's phylactery cracks ...) and plays the cast clip once.
     */
    private void phaseLook(int phase) {
        if (level().isClientSide) return;
        int before = entityData.get(LOOK_PHASE), now = Math.max(1, Math.min(3, phase));
        if (now == before) return;
        entityData.set(LOOK_PHASE, now);
        String look = lookId();
        if (now > before && !look.isEmpty() && level() instanceof ServerLevel sl) {
            noteCast(2);
            AbyssalLooks.Look l = AbyssalLooks.get(look);
            String caption = l == null ? "" : l.phase(now).caption();
            if (!caption.isEmpty()) {
                Component line = Component.translatableWithFallback("message.nusmp.mega.dnd.look." + look + ".p" + now, caption).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE, net.minecraft.ChatFormatting.ITALIC);
                for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(48.0), Player::isAlive)) p.displayClientMessage(line, true);
            }
        }
    }

    /** Health put back without it counting as a heal (the budget lets it through). */
    void restoreHealth(float health) { dpsBudget.bypass(() -> setHealth(health)); }

    /** A shield of eaten magic: absorption up to the attribute's room. */
    void addShield(float amount) { setAbsorptionAmount((float) Math.min(getAbsorptionAmount() + amount, Math.max(0.0, getAttributeValue(Attributes.MAX_ABSORPTION)))); }

    /** The death is about to happen: true when the Mythic Trait spends itself instead (the caller cancels the death). */
    boolean dndMythic(ServerLevel sl) { return dnd.tryMythic(this, sl); }

    /** A harmful effect is about to land on it: true when a Legendary Resistance turns it away. */
    boolean dndResist(ServerLevel sl, String effectId) { return dnd.resist(this, sl, effectId); }

    DndBoss dnd() { return dnd; }

    public int floorNo() { return floorNo; }

    // ---------------------------------------------------------------- save
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GuardSector", sectorId);
        tag.putInt("GuardThreat", threat);
        tag.putString("GuardTheme", theme.id());
        if (floorLabel != null) tag.putString("GuardFloor", floorLabel);
        if (anchor != null) tag.putLong("GuardAnchor", anchor.asLong());
        tag.putBoolean("GuardPhaseTwo", phaseTwo);
        tag.putInt("GuardFloorNo", floorNo);
        dnd.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        dpsBudget.bypass(() -> super.readAdditionalSaveData(tag));                                 // loading the saved health is not damage
        configured = tag.contains("GuardThreat");
        sectorId = tag.contains("GuardSector") ? tag.getInt("GuardSector") : -1;
        threat = GuardianScaling.clampThreat(tag.contains("GuardThreat") ? tag.getInt("GuardThreat") : 1);
        theme = RegionTheme.fromId(tag.getString("GuardTheme"));
        floorLabel = tag.contains("GuardFloor") ? tag.getString("GuardFloor") : null;
        trait = GuardianScaling.traitOf(theme);
        anchor = tag.contains("GuardAnchor") ? BlockPos.of(tag.getLong("GuardAnchor")) : null;
        phaseTwo = tag.getBoolean("GuardPhaseTwo");
        divisor = GuardianScaling.damageDivisor(GuardConfig.get(GuardConfig.GUARDIAN_BASE_HEALTH), GuardConfig.get(GuardConfig.THREAT_HEALTH_SCALE), threat);
        floorNo = tag.contains("GuardFloorNo") ? tag.getInt("GuardFloorNo") : Math.max(1, threat);
        dnd.configure(DndProfiles.forBoss(floorNo, sectorId));
        dnd.load(tag);
        syncLook();                                                                                // 1.94.0: the look and the phase it was saved in
        stamp();                                                                                // loading clears and restores the tag list: both keys are written again
    }
}
