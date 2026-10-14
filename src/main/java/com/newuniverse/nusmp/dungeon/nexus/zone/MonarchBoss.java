package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.dungeon.wild.Dweller;
import com.newuniverse.nusmp.dungeon.wild.Wildlife;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * The Blood-Red Commander itself (the Monarch of the Monarch's Vault, whose persistent marks keep their old names so old worlds load) and its
 * shadow soldiers, made out of the real mobs of Tensura (docs/living_dungeon.md section 5, zone 1): The Commander is a
 * {@code tensura:arch_daemon} (a {@code tensura:greater_daemon} if the archdaemon is missing), grown into a god-tier boss with attribute
 * modifiers, dressed in red (a red team for its glowing outline and name, crimson dust off its body) and tagged {@link #FLOOR_BOSS} so the
 * Dungeon's stones know it for a floor boss; the soldiers are {@code tensura:lesser_daemon} and {@code tensura:greater_daemon}, buffed and
 * marked. The types are looked up by id at run time. A creature is recognised by marks in its persistent data, so it is found again after a
 * restart. This class makes and marks them; {@link MonarchDriver} runs the fight.
 */
public final class MonarchBoss {
    private MonarchBoss() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    /** Persistent-data keys: the Monarch itself, the floor block in the middle of its Nexus, asleep, has woken once, has fallen (its fall is seen to once), and (on a soldier) the Monarch it serves. */
    static final String KEY = "nusmp_monarch", CENTRE = "nusmp_monarch_centre", ASLEEP = "nusmp_monarch_asleep", WOKEN = "nusmp_monarch_woken",
            FELL = "nusmp_monarch_fell", FINAL = "nusmp_monarch_final", SHADOW = "nusmp_shadow", BORN = "nusmp_shadow_born";
    /** The scoreboard tag every floor boss of the Dungeon carries (the Dungeon's stones give a mob that has it a boss's share), and the name of the red team that colours the Commander's outline. */
    public static final String FLOOR_BOSS = "nusmp_floor_boss";
    static final String TEAM = "nusmp_commander";
    /** Crimson dust shed by its body. */
    private static final DustParticleOptions CRIMSON = new DustParticleOptions(new Vector3f(0.86f, 0.05f, 0.10f), 1.5f);

    // ---------------------------------------------------------------- finding out
    public static boolean is(Entity e) { return e instanceof Mob && e.getPersistentData().getBoolean(KEY); }

    public static boolean asleep(Entity e) { return e.getPersistentData().getBoolean(ASLEEP); }

    public static boolean isSoldier(Entity e) { return e instanceof Mob && e.getPersistentData().contains(SHADOW); }

    /** The floor block in the middle of the Nexus this Monarch sleeps in. */
    public static BlockPos centreOf(Entity e) { return BlockPos.of(e.getPersistentData().getLong(CENTRE)); }

    /** The Monarch whose soldier this is, or null. */
    static UUID ownerOf(Entity e) {
        try {
            return UUID.fromString(e.getPersistentData().getString(SHADOW));
        } catch (IllegalArgumentException bad) {
            return null;
        }
    }

    /** Where it sleeps and stands guard: on top of the dais, in the middle of the hall. */
    public static Vec3 anchor(BlockPos centre) { return new Vec3(centre.getX() + 0.5, centre.getY() + MonarchVaultPlan.DAIS_TOP + 1, centre.getZ() + 0.5); }

    // ---------------------------------------------------------------- making the Monarch
    /** The entity type of the Monarch: the archdaemon, else the greater daemon, else null. */
    static EntityType<?> type() {
        EntityType<?> t = Wildlife.typeOf("tensura:arch_daemon");
        return t != null ? t : Wildlife.typeOf("tensura:greater_daemon");
    }

    /**
     * Makes The Monarch asleep on its dais and adds it to the world. Returns it, or null if the mob types of Tensura are missing or the mob
     * could not be made (logged). It is set up before it joins the level, so the driver sees it with its marks from its first tick.
     */
    public static Mob spawn(ServerLevel level, BlockPos centre) {
        EntityType<?> type = type();
        if (type == null) {
            LOG.warn("[nusmp] The Monarch's Vault has no Monarch: neither tensura:arch_daemon nor tensura:greater_daemon is registered");
            return null;
        }
        Mob m = make(level, type, anchor(centre));
        if (m == null) return null;
        configure(m, centre);
        dress(level, m);
        sleep(m, centre);
        level.addFreshEntity(m);
        return m;
    }

    /** Creates a mob of that type standing at `at`, finalized as an event spawn so the Dungeon's spawn hook leaves it alone. Null if it could not be made. */
    private static Mob make(ServerLevel level, EntityType<?> type, Vec3 at) {
        try {
            Entity made = type.create(level);
            if (!(made instanceof Mob m)) {
                if (made != null) made.discard();
                return null;
            }
            m.moveTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360f, 0f);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.EVENT, null);
            m.setPersistenceRequired();
            return m;
        } catch (RuntimeException | LinkageError ex) {
            LOG.warn("[nusmp] Could not make a {} for the Monarch's Vault", Wildlife.idOf(type), ex);
            return null;
        }
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, path); }

    /** Sets an attribute to a total by adding the difference to what the mob has (replacing the modifier of an earlier call). */
    private static void total(Mob m, Holder<Attribute> attribute, String name, double target) {
        AttributeInstance a = m.getAttribute(attribute);
        if (a == null) return;
        a.removeModifier(id(name));
        a.addPermanentModifier(new AttributeModifier(id(name), target - a.getValue(), AttributeModifier.Operation.ADD_VALUE));
    }

    private static void scale(Mob m, Holder<Attribute> attribute, String name, double factor) {
        AttributeInstance a = m.getAttribute(attribute);
        if (a == null) return;
        a.removeModifier(id(name));
        a.addPermanentModifier(new AttributeModifier(id(name), factor - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    /** Grows the mob into the Monarch: health, damage, armour, weight, reach and size, its name, its marks. */
    private static void configure(Mob m, BlockPos centre) {
        total(m, Attributes.MAX_HEALTH, "monarch_health", MonarchRules.BASE_HEALTH);
        total(m, Attributes.ATTACK_DAMAGE, "monarch_attack", MonarchRules.ATTACK);
        total(m, Attributes.KNOCKBACK_RESISTANCE, "monarch_poise", 1.0);
        total(m, Attributes.FOLLOW_RANGE, "monarch_sight", 64.0);
        total(m, Attributes.STEP_HEIGHT, "monarch_stride", 1.2);
        total(m, Attributes.SCALE, "monarch_size", MonarchRules.SCALE);
        AttributeInstance armor = m.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.removeModifier(id("monarch_armor"));
            armor.addPermanentModifier(new AttributeModifier(id("monarch_armor"), MonarchRules.ARMOR, AttributeModifier.Operation.ADD_VALUE));
        }
        scale(m, Attributes.MOVEMENT_SPEED, "monarch_pace", 1.0 + MonarchRules.SPEED_GAIN);
        m.setHealth(m.getMaxHealth());
        m.setCustomName(Component.literal(MonarchText.NAME));
        m.setCustomNameVisible(true);
        m.getPersistentData().putBoolean(KEY, true);
        m.getPersistentData().putLong(CENTRE, centre.asLong());
    }

    // ---------------------------------------------------------------- the red of it
    /**
     * Gives the Commander its looks and its tag, and does it again whenever it joins the level (a Monarch made by 1.21.0 is renamed and tagged
     * when its chunk loads): its name, the {@link #FLOOR_BOSS} tag, and a place in a team coloured red, which is what makes the outline of its
     * glow red (and its name). Safe to call any number of times; a failure of the team is logged once and costs only the colour.
     */
    static void dress(ServerLevel level, Mob m) {
        m.setCustomName(Component.literal(MonarchText.NAME));
        m.setCustomNameVisible(true);
        if (!m.getTags().contains(FLOOR_BOSS)) m.addTag(FLOOR_BOSS);
        try {
            Scoreboard board = level.getScoreboard();
            PlayerTeam team = board.getPlayerTeam(TEAM);
            if (team == null) team = board.addPlayerTeam(TEAM);
            if (team.getColor() != ChatFormatting.RED) team.setColor(ChatFormatting.RED);
            String who = m.getScoreboardName();
            if (board.getPlayersTeam(who) != team) board.addPlayerToTeam(who, team);
        } catch (RuntimeException | LinkageError ex) {
            if (!teamWarned) {
                teamWarned = true;
                LOG.warn("[nusmp] The Blood-Red Commander could not be put in its red team (its outline stays white)", ex);
            }
        }
    }

    private static boolean teamWarned;

    /** Takes the Commander out of its team (it has fallen: the scoreboard keeps no name of a dead mob). */
    static void undress(ServerLevel level, Mob m) {
        try {
            Scoreboard board = level.getScoreboard();
            PlayerTeam team = board.getPlayerTeam(TEAM);
            String who = m.getScoreboardName();
            if (team != null && board.getPlayersTeam(who) == team) board.removePlayerFromTeam(who, team);
        } catch (RuntimeException | LinkageError ignored) {
            // a stale name in a team harms nothing
        }
    }

    /** A little crimson dust and a few spores off the body: bounded (two packets), called every few ticks while it is awake and rarely while it sleeps. */
    static void shed(ServerLevel level, Mob m, boolean awake) {
        double w = Math.max(0.4, m.getBbWidth() * 0.5), h = Math.max(1.0, m.getBbHeight());
        level.sendParticles(CRIMSON, m.getX(), m.getY() + h * 0.55, m.getZ(), awake ? 6 : 2, w, h * 0.4, w, 0.0);
        level.sendParticles(ParticleTypes.CRIMSON_SPORE, m.getX(), m.getY() + h * 0.5, m.getZ(), awake ? 2 : 1, w, h * 0.4, w, 0.0);
    }

    // ---------------------------------------------------------------- sleeping and waking
    /** Puts it to sleep on its dais: no brain, no target, full health, back on its mark. */
    static void sleep(Mob m, BlockPos centre) {
        Vec3 at = anchor(centre);
        m.getPersistentData().putBoolean(ASLEEP, true);
        m.setGlowingTag(false);                                                  // it glows red only while it fights
        m.setNoAi(true);
        m.setTarget(null);
        m.getNavigation().stop();
        m.setHealth(m.getMaxHealth());
        m.setDeltaMovement(Vec3.ZERO);
        m.moveTo(at.x, at.y, at.z, m.getYRot(), 0f);
    }

    /** Wakes it: the brain comes back. Returns true if it was asleep. */
    static boolean wake(Mob m) {
        boolean was = asleep(m);
        m.getPersistentData().putBoolean(ASLEEP, false);
        m.setGlowingTag(true);
        m.setNoAi(false);
        return was;
    }

    // ---------------------------------------------------------------- the soldiers
    /** The type of a soldier: the lesser daemon, or the greater daemon; null if Tensura does not have it. */
    static EntityType<?> soldierType(boolean greater) { return Wildlife.typeOf(greater ? "tensura:greater_daemon" : "tensura:lesser_daemon"); }

    /**
     * Brings a shadow soldier into the world at `at`: a real daemon of Tensura, stronger the later the phase, marked as the Monarch's, named,
     * outlined so it can be told from the rest, and given its start time. Null if the type is missing or it could not be made.
     */
    static Mob soldier(ServerLevel level, boolean greater, Vec3 at, UUID owner, int phase) {
        EntityType<?> type = soldierType(greater);
        if (type == null && greater) type = soldierType(false);
        if (type == null) return null;
        Mob m = make(level, type, at);
        if (m == null) return null;
        if (!level.noCollision(m.getBoundingBox())) {
            m.discard();
            return null;
        }
        scale(m, Attributes.MAX_HEALTH, "monarch_soldier_health", MonarchRules.soldierHealth(phase));
        scale(m, Attributes.ATTACK_DAMAGE, "monarch_soldier_attack", MonarchRules.soldierDamage(phase));
        scale(m, Attributes.MOVEMENT_SPEED, "monarch_soldier_pace", 1.1);
        m.setHealth(m.getMaxHealth());
        m.setCustomName(Component.literal(MonarchText.SOLDIER));
        m.setCustomNameVisible(false);
        m.setGlowingTag(true);
        m.getPersistentData().putString(SHADOW, owner.toString());
        m.getPersistentData().putLong(BORN, level.getGameTime());
        level.addFreshEntity(m);
        return m;
    }

    /** The Dungeon's food web must not take a soldier for one of its own: its hunger is held where it neither starves, hunts nor falls asleep. */
    static void pinHunger(Mob m) {
        try {
            if (m.hasData(Dweller.DWELLER.get())) m.getData(Dweller.DWELLER).appetite.set(60);
        } catch (RuntimeException | LinkageError ignored) {
            // a mob the food web does not know has nothing to pin
        }
    }
}
