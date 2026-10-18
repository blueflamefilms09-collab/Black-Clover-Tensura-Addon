package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.newuniverse.nusmp.boss.rules.BossIdentity;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryMobEntity;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the one place where "the deeper, the stronger and smarter" becomes true for the mobs of the world. It listens to the join event, which every mob passes through however it was made
 * (the Dungeon Master's encounters, the Mob Foundry, a wandering mob, a Tensura monster that spawned by itself, a summon of a mechanic) and again every time its chunk is loaded, so no spawner
 * needs to know about the rule. A hostile, non-boss, un-owned mob that stands in the Mega-Dungeon gets the BORN FLOOR it was first seen on (saved on the mob, so a mob that walks to another floor
 * keeps the strength it was born with and a reload does not re-roll it): the strength bonuses of {@link DepthRules#strength} go on as permanent attribute modifiers with fixed ids (so applying them
 * twice replaces, never stacks), and a mob of mind 1 or more gets the {@link DepthMind} goal. Bosses are never touched (they have their own scaling and budget, {@link BossIdentity}); mobs of the
 * Dungeon Master and the Foundry already scale with the floor by their own rules, so they take only a share of the bonus (config {@code shareForScaledMobs}). Server thread only.
 */
public final class DepthMobs {
    private DepthMobs() {}

    private static final Logger LOG = LoggerFactory.getLogger("nusmp");
    private static boolean warned;

    /** Persistent data on the mob: the signed floor it was born on, and the mind level it got. */
    public static final String FLOOR_KEY = "nusmp_depth_floor", MIND_KEY = "nusmp_depth_mind";

    static final ResourceLocation HEALTH_ID = id("mega_depth_health"), DAMAGE_ID = id("mega_depth_damage"), SPEED_ID = id("mega_depth_speed"),
            ARMOR_ID = id("mega_depth_armor"), KNOCKBACK_ID = id("mega_depth_knockback"), FOLLOW_ID = id("mega_depth_follow");

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    // ---------------------------------------------------------------- the event
    public static void onJoin(EntityJoinLevelEvent e) {
        try {
            Entity en = e.getEntity();
            if (e.getLevel().isClientSide() || !(en instanceof Mob mob) || !(e.getLevel() instanceof ServerLevel level) || !Labyrinth.inMega(level)) return;
            if (!DepthConfig.get(DepthConfig.ENABLED) || !eligible(mob)) return;
            int floor = mob.getPersistentData().getInt(FLOOR_KEY);
            if (floor == 0) floor = MegaDungeon.floorAt(level, mob.blockPosition());
            if (floor == 0) return;                                                    // outside every sector (the Hub, the gaps): the rule does not apply
            apply(mob, floor);
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                LOG.warn("[nusmp] The depth rule failed on a mob (further failures are not logged)", t);
            }
        }
    }

    /** Is this a mob the rule is for: hostile, not a boss, not somebody's pet or summon? */
    public static boolean eligible(Mob mob) {
        if (!(mob instanceof Enemy)) return false;
        if (BossIdentity.isBoss(mob) || mob.getTags().contains(WandererDirector.TAG)) return false;
        return !(mob instanceof OwnableEntity o && o.getOwnerUUID() != null);
    }

    /** Does the mob already scale with the floor by the Dungeon Master's or the Foundry's own rule? */
    static boolean scaledByOthers(Mob mob) { return mob instanceof FoundryMobEntity || EncounterSpawner.isMine(mob); }

    // ---------------------------------------------------------------- the effect
    /** Gives the mob the strength and the mind of its floor. Safe to call again (the modifiers have fixed ids; the health is refilled only the first time). */
    public static void apply(Mob mob, int floor) {
        boolean first = mob.getPersistentData().getInt(FLOOR_KEY) == 0;
        mob.getPersistentData().putInt(FLOOR_KEY, floor);
        int depth = DepthRules.depthOf(floor);
        if (DepthConfig.get(DepthConfig.STRENGTH)) {
            double k = DepthConfig.get(DepthConfig.STRENGTH_SCALE) * (scaledByOthers(mob) ? DepthConfig.get(DepthConfig.SHARE_ALREADY_SCALED) : 1.0);
            DepthRules.Strength s = DepthRules.strength(depth).times(k);
            put(mob, Attributes.MAX_HEALTH, HEALTH_ID, s.health(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            put(mob, Attributes.ATTACK_DAMAGE, DAMAGE_ID, s.damage(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            put(mob, Attributes.MOVEMENT_SPEED, SPEED_ID, s.speed(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            put(mob, Attributes.ARMOR, ARMOR_ID, s.armor(), AttributeModifier.Operation.ADD_VALUE);
            put(mob, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, s.knockback(), AttributeModifier.Operation.ADD_VALUE);
            put(mob, Attributes.FOLLOW_RANGE, FOLLOW_ID, s.follow(), AttributeModifier.Operation.ADD_VALUE);
            if (first && s.health() > 0.0) mob.setHealth(mob.getMaxHealth());
        }
        if (DepthConfig.get(DepthConfig.SMARTS)) {
            int mind = DepthRules.mind(depth, DepthRules.parseThresholds(DepthConfig.get(DepthConfig.MIND_FLOORS)));
            mob.getPersistentData().putInt(MIND_KEY, mind);
            if (mind > 0) GoalAccess.add(mob, 2, new DepthMind(mob, mind));
        }
    }

    private static void put(Mob mob, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = mob.getAttribute(attribute);
        if (inst == null) return;
        if (amount > 0.0) inst.addOrReplacePermanentModifier(new AttributeModifier(id, amount, op));
        else inst.removeModifier(id);
    }

    // ---------------------------------------------------------------- for the command and the self-test
    /** The table of one floor: depth, mind level and the bonuses, one line each. */
    public static List<String> describe(int floor) {
        int depth = DepthRules.depthOf(floor);
        int[] th = DepthRules.parseThresholds(DepthConfig.get(DepthConfig.MIND_FLOORS));
        int mind = DepthRules.mind(depth, th);
        DepthRules.Strength s = DepthRules.strength(depth).times(DepthConfig.get(DepthConfig.STRENGTH_SCALE));
        List<String> out = new ArrayList<>();
        out.add("Floor " + floor + " (depth " + depth + "): mind " + mind + " of " + DepthRules.MAX_MIND + ", every thinking pass " + DepthRules.thinkEvery(mind) + " ticks.");
        out.add(String.format("Strength: health +%.0f%%, damage +%.0f%%, speed +%.1f%%, armour +%.1f, knockback resistance +%.2f, follow range +%.1f blocks.",
                s.health() * 100.0, s.damage() * 100.0, s.speed() * 100.0, s.armor(), s.knockback(), s.follow()));
        StringBuilder known = new StringBuilder();
        for (DepthRules.Behaviour b : DepthRules.Behaviour.values()) if (DepthRules.knows(mind, b)) known.append(known.length() > 0 ? ", " : "").append(b.name().toLowerCase());
        out.add("Knows: " + (known.length() == 0 ? "nothing yet" : known) + ". Next level at depth " + (mind < DepthRules.MAX_MIND ? th[mind] : "-") + ".");
        return out;
    }
}
