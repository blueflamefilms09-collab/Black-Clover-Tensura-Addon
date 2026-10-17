package com.newuniverse.nusmp.labyrinth.vault;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.LabyrinthConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * Why: the owner wants the Relics of Many Worlds to come only from the Labyrinth (its vault chests, its Relic Pedestals, its bosses), never
 * from a plain Overworld chest or a crafting table. Two code paths used to put relics into the wild: the global loot modifier
 * {@code nusmp:relic_chests} (any "chests/" table with a dungeon-like word) and the Mimic drop handler in {@code RelicLoot}. Both now ask this
 * class first. The rules are pure static methods over strings and booleans so a JUnit test can pin them without a server; the two
 * Minecraft-facing overloads only gather the inputs (the queried table id, the dimension, the config value) and delegate.
 * <p>
 * {@code labyrinth.relicsExclusive = false} restores the 1.24 behaviour exactly: every method answers true and the old paths run unchanged.
 * The config is read through {@code LabyrinthConfig.get}, which hands back the default (true) before the config is loaded, so a loot roll during
 * world load is already exclusive instead of leaking a relic.
 */
public final class RelicExclusivity {
    private RelicExclusivity() {}

    /** The namespace of the only tables allowed to yield relics while exclusive. */
    static final String NAMESPACE = "nusmp";

    /** The two tables of the Labyrinth that may yield relics: the grand vault chests and the Relic Pedestal. */
    static final Set<String> VAULT_TABLES = Set.of("chests/dungeon_relic_vault", "chests/labyrinth_pedestal");

    /**
     * Pure rule: may the loot modifier add a relic to this chest table? Not exclusive: always (the old behaviour). Exclusive: only the two
     * Labyrinth tables, and only in our own namespace (another mod's "chests/dungeon_relic_vault" is not ours).
     */
    public static boolean allowsChest(String namespace, String path, boolean exclusive) {
        if (!exclusive) return true;
        return NAMESPACE.equals(namespace) && path != null && VAULT_TABLES.contains(path);
    }

    /** Pure rule: may a Mimic (or any relic-dropping mob) drop a relic here? Not exclusive: always. Exclusive: only inside a dungeon dimension. */
    public static boolean allowsMobDrop(boolean inDungeon, boolean exclusive) {
        return !exclusive || inDungeon;
    }

    /** Whether the Labyrinth config says relics are exclusive right now (true before the config is loaded, and if the config class is missing). */
    public static boolean exclusive() {
        try {
            return Boolean.TRUE.equals(LabyrinthConfig.get(LabyrinthConfig.RELICS_EXCLUSIVE));
        } catch (RuntimeException | LinkageError notThere) {
            return true;
        }
    }

    /** The chest rule with the live inputs: the queried table id (null never passes) and the config. */
    public static boolean allowsChest(ResourceLocation table) {
        return table != null && allowsChest(table.getNamespace(), table.getPath(), exclusive());
    }

    /**
     * 1.54.0 pure rule: the same with the Mega-Dungeon in mind. In the singular dungeon the Crucible of Choice is the ONLY source of relics (exactly one per
     * region), so a Mimic drops none there, whatever the config says, even though the level is a dungeon; every other level answers as the two-argument rule does.
     */
    public static boolean allowsMobDrop(boolean inDungeon, boolean inMega, boolean exclusive) {
        return !inMega && allowsMobDrop(inDungeon, exclusive);
    }

    /**
     * 1.54.0 pure rule: may the world's loot (a chest table, the {@code nusmp:relic} loot entry, a Mimic) roll a relic in this kind of level? Never in the
     * Mega-Dungeon, whatever the config says: there a Crucible is the only source of relics of Unique and above, exactly one per region, so a stray chest
     * table or a Mimic cannot add a second. Everywhere else the answer is yes (the rules above decide where the old paths apply).
     */
    public static boolean allowsWildRoll(boolean inMega) { return !inMega; }

    /** The same with the live level; a null level is not the Mega-Dungeon. */
    public static boolean allowsWildRoll(Level level) { return allowsWildRoll(inMega(level)); }

    /** The mob-drop rule with the live inputs: whether the level is a Labyrinth dimension, whether it is the Mega-Dungeon (1.54.0: none there), and the config. */
    public static boolean allowsMobDrop(Level level) {
        return allowsMobDrop(inDungeon(level), inMega(level), exclusive());
    }

    /** The Mega-Dungeon dimension (1.54.0). Never throws. */
    static boolean inMega(Level level) {
        try { return Labyrinth.inMega(level); } catch (RuntimeException | LinkageError ignored) { return false; }
    }

    /** A dungeon dimension (the Labyrinth's). Never throws (a missing class counts as "not in it"). */
    static boolean inDungeon(Level level) {
        if (level == null) return false;
        try { return Labyrinth.in(level); } catch (RuntimeException | LinkageError ignored) { return false; /* the Labyrinth is not loaded: not in it */ }
    }
}
