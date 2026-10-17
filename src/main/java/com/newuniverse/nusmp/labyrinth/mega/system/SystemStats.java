package com.newuniverse.nusmp.labyrinth.mega.system;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the "permanent stat boost" of the brief has to be REAL attribute strength, it has to stay right whatever happens to the player, and since 1.58.0 it has to
 * work ONLY inside the dungeon dimensions (the Mega-Dungeon and the Hub; config {@code system.buffOnlyInDungeon}, default on). The same fixed modifier id per
 * stat ({@code nusmp:system/str} ...) means applying is idempotent (a second call finds the modifier already correct and changes nothing, and a changed table
 * value replaces the old one instead of stacking), it can be removed by id alone, and no other system can mistake it for its own. The amounts come from the
 * table (a point's worth and its ceiling are data), and every attribute id is resolved at run time and SKIPPED when it is not registered or the player does not
 * have it, so the Tensura magicule effect simply does nothing in a game without Tensura instead of crashing.
 *
 * <p>The modifiers are TRANSIENT (not saved with the player). That is the way that cannot leave a stale modifier behind: a crash, a kill of the server, a
 * disconnect while outside the dungeon or a missed event can only ever lose a bonus that the next look puts back; they can never keep one that should be gone,
 * because nothing of it is on disk. The saved stat points in the attachment are the truth, the modifiers are only their effect. They are put on by {@link #apply}
 * when the player logs in or respawns inside the dungeon, arrives in it, or earns a point, taken off when they leave it, and a safety net ({@link #reconcile},
 * every two seconds per player, cheap) repairs any change that was missed. Worlds saved by 1.55.0 to 1.57.0 carry the modifiers as PERMANENT ones: the login and
 * respawn refresh ({@link SystemQuests#refreshFresh}) removes them first, so they too become transient. The one cost of "not saved" is the health above the plain maximum (a
 * VIT bonus): the game loads a player with their health already cut to the plain maximum, so the logout hands it to the next login ({@link #stashHealth},
 * {@link #restoreHealth}) and a relog costs no hearts. Server thread only.
 */
public final class SystemStats {
    private SystemStats() {}

    /** What was last put on (true) or taken off (false) each player; the safety net compares it with what the gate says now. Cleared on logout and server stop. */
    private static final Map<UUID, Boolean> APPLIED = new HashMap<>();

    /** The fixed id of a stat's modifier on every attribute it feeds. */
    public static ResourceLocation modifierId(Stat stat) {
        return ResourceLocation.fromNamespaceAndPath("nusmp", "system/" + stat.id());
    }

    // ================================================================ the gate
    /** The id ({@code namespace:path}) of the dimension the player is in. */
    static String dimensionId(ServerPlayer p) { return p.level().dimension().location().toString(); }

    /** Does the dimension gate say the bonus works where this player is? (Not asked: whether the System is switched on.) */
    public static boolean gate(ServerPlayer p) {
        return BuffGate.active(dimensionId(p), SystemConfig.buffOnlyInDungeon());
    }

    /** Should the player carry the bonus now: the System is on and the gate is open. */
    public static boolean wanted(ServerPlayer p) { return SystemConfig.enabled() && gate(p); }

    // ================================================================ putting the bonus on and off
    /** Makes the player's attributes match their saved points AND the gate: the modifiers are put on inside the dungeon, taken off outside. Safe to call any number of times. */
    public static void apply(ServerPlayer p) {
        apply(p, SystemQuests.state(p));
    }

    public static void apply(ServerPlayer p, SystemState state) {
        boolean on = gate(p);
        if (on) applyActive(p, state);
        else remove(p);
        APPLIED.put(p.getUUID(), on);
    }

    /** The refresh of a player who came back (login, respawn, a new dimension): like {@link #apply}, but the System being switched off also takes the bonus away. */
    public static void sync(ServerPlayer p) {
        boolean on = wanted(p);
        if (on) applyActive(p, SystemQuests.state(p));
        else remove(p);
        APPLIED.put(p.getUUID(), on);
    }

    /**
     * The safety net, called every couple of seconds for every player: if what was last done to the player is not what the gate says now (a dimension change
     * that no event reported, an admin command, another mod's teleport) the modifiers are put right. A player nobody has looked at yet gets their first look.
     */
    public static void reconcile(ServerPlayer p) {
        if (BuffGate.needsChange(APPLIED.get(p.getUUID()), wanted(p))) sync(p);
    }

    /** The player left the server. */
    public static void forget(UUID player) { APPLIED.remove(player); }

    // ================================================================ health across a relog
    /** The key in the player's persistent data that carries their health from a logout to the next login. */
    static final String HEALTH_KEY = "nusmp_system_hp";

    /**
     * Logout (the event fires before the player is saved): while the bonus is on, remembers the player's health. A transient VIT bonus is not saved, so the game
     * would load the player with their health cut to the plain maximum before the bonus is put back; {@link #restoreHealth} gives it back, so a relog costs no hearts.
     */
    public static void stashHealth(ServerPlayer p) {
        if (Boolean.TRUE.equals(APPLIED.get(p.getUUID())) && p.isAlive() && p.getHealth() > 0f) p.getPersistentData().putFloat(HEALTH_KEY, p.getHealth());
    }

    /**
     * Login, after {@link #sync}: gives back the health the player logged out with, but only while the bonus is on and never above their maximum or below what they
     * have. The note is removed either way, so it can never be used twice or outlive one session.
     */
    public static void restoreHealth(ServerPlayer p) {
        CompoundTag data = p.getPersistentData();
        if (!data.contains(HEALTH_KEY, Tag.TAG_ANY_NUMERIC)) return;
        float was = data.getFloat(HEALTH_KEY);
        data.remove(HEALTH_KEY);
        if (Boolean.TRUE.equals(APPLIED.get(p.getUUID())) && p.isAlive() && was > p.getHealth()) p.setHealth(Math.min(was, p.getMaxHealth()));
    }

    /** Respawn: a new body starts as the game makes it; any note of a past logout is thrown away. */
    public static void dropHealth(ServerPlayer p) { p.getPersistentData().remove(HEALTH_KEY); }

    /** Server stop. */
    public static void clear() { APPLIED.clear(); }

    /** Puts the modifiers on (or corrects them), whatever the gate says: for {@link #apply}, {@link #sync} and the self-tests. */
    public static void applyActive(ServerPlayer p, SystemState state) {
        StatTable table = SystemData.stats();
        for (Stat stat : Stat.values()) {
            StatTable.Def def = table.def(stat);
            if (def == null) continue;
            int points = state.points(stat);
            ResourceLocation id = modifierId(stat);
            for (StatTable.Effect e : def.effects()) {
                AttributeInstance inst = instance(p, e.attribute());
                if (inst == null) continue;
                double amount = e.amount(points);
                if (points <= 0 || amount == 0.0) {
                    if (inst.hasModifier(id)) inst.removeModifier(id);
                    continue;
                }
                AttributeModifier.Operation op = operation(e.op());
                AttributeModifier current = inst.getModifier(id);
                if (current != null && current.amount() == amount && current.operation() == op) continue;
                if (current != null) inst.removeModifier(id);                     // also clears a permanent one of an old save, which addOrUpdateTransientModifier would leave in the saved set
                inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
            }
        }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** Takes every System modifier off the player (leaving the dungeon, an admin reset, or a stat table that is being replaced). The saved points are not touched. */
    public static void remove(ServerPlayer p) {
        for (Stat stat : Stat.values()) {
            StatTable.Def def = SystemData.stats().def(stat);
            if (def == null) continue;
            for (StatTable.Effect e : def.effects()) {
                AttributeInstance inst = instance(p, e.attribute());
                if (inst != null && inst.hasModifier(modifierId(stat))) inst.removeModifier(modifierId(stat));
            }
        }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** The modifier amount a stat's first effect currently has on the player (0 when it has none, or the attribute is absent): for status output and tests. */
    public static double currentAmount(ServerPlayer p, Stat stat, int effectIndex) {
        StatTable.Def def = SystemData.stats().def(stat);
        if (def == null || effectIndex < 0 || effectIndex >= def.effects().size()) return 0.0;
        AttributeInstance inst = instance(p, def.effects().get(effectIndex).attribute());
        AttributeModifier m = inst == null ? null : inst.getModifier(modifierId(stat));
        return m == null ? 0.0 : m.amount();
    }

    /** The player's instance of the attribute with this registry id, or null when the id is bad, the attribute is not registered, or the player has no such attribute. */
    static AttributeInstance instance(ServerPlayer p, String attributeId) {
        ResourceLocation rl = ResourceLocation.tryParse(attributeId);
        if (rl == null) return null;
        Holder<Attribute> holder = BuiltInRegistries.ATTRIBUTE.getHolder(rl).map(h -> (Holder<Attribute>) h).orElse(null);
        return holder == null ? null : p.getAttribute(holder);
    }

    private static AttributeModifier.Operation operation(StatTable.Op op) {
        return switch (op) {
            case ADD_VALUE -> AttributeModifier.Operation.ADD_VALUE;
            case ADD_MULTIPLIED_BASE -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case ADD_MULTIPLIED_TOTAL -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        };
    }
}
