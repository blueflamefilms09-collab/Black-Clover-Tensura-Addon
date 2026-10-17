package com.newuniverse.nusmp.labyrinth.mega.theme;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Why: the processor is asked about every block of every piece (a sector is hundreds of thousands of blocks), so the answer to "what does this
 * template block become in this theme" has to be one hash lookup. This class turns a {@link ThemeDef} into that lookup once: it asks
 * {@link ThemeSwap} for the target id of every palette block (and every explicit pair), resolves the ids through {@code BuiltInRegistries.BLOCK}
 * (the registry check the brief demands: an id that is not a registered block is never swapped to), and keeps an identity map from template
 * {@code Block} to target {@code Block}. The table is built lazily per theme on first use (the registries are bound by then, which is not true at
 * mod construction), cached for as long as the definition is the same object, and dropped on a datapack reload. Whatever is wrong with a theme
 * (a bad id, a missing slot, a palette block with no target) is logged ONCE per theme and line, as the brief asks, and costs only that block its
 * swap. Read-only after construction, so the processor may be called from any thread.
 */
final class ThemeTables {
    private ThemeTables() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<String, Table> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    private record Table(ThemeDef def, Map<Block, Block> swap) {}

    /** Forgets every derived table and every logged problem (a datapack reload). */
    static void clear() {
        CACHE.clear();
        LOGGED.clear();
    }

    /** The block a template block becomes in this theme, or null when it is left alone. */
    static Block targetOf(RegionTheme theme, Block from) {
        ThemeDef def = ThemeDefs.get(theme);
        Table t = CACHE.get(def.id());
        if (t == null || t.def() != def) {
            t = build(def);
            CACHE.put(def.id(), t);
        }
        return t.swap().get(from);
    }

    /** The shift of the invisible light blocks in a sector of this theme (see {@code ThemeDef.lightShift}); 0 for the baseline and for a theme without a file. */
    static int lightShiftOf(RegionTheme theme) { return ThemeDefs.get(theme).lightShift(); }

    /** How many blocks the theme swaps (for the status command); builds the table when needed. */
    static int size(RegionTheme theme) {
        ThemeDef def = ThemeDefs.get(theme);
        Table t = CACHE.get(def.id());
        if (t == null || t.def() != def) {
            t = build(def);
            CACHE.put(def.id(), t);
        }
        return t.swap().size();
    }

    private static Table build(ThemeDef def) {
        Predicate<String> exists = ThemeTables::registered;
        for (String problem : ThemeSwap.problems(def.families(), def.explicit(), exists)) {
            if (LOGGED.add(def.id() + "|" + problem)) LOG.warn("[nusmp] mega theme {}: {}", def.id(), problem);
        }
        IdentityHashMap<Block, Block> map = new IdentityHashMap<>();
        for (Map.Entry<String, ThemeSwap.Resolved> x : ThemeSwap.table(def.families(), def.explicit(), exists).entrySet()) {
            if (!x.getValue().swaps()) continue;
            Block from = block(x.getKey()), to = block(x.getValue().target());
            if (from != null && to != null && from != to) map.put(from, to);
        }
        return new Table(def, Collections.unmodifiableMap(map));
    }

    private static boolean registered(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && BuiltInRegistries.BLOCK.containsKey(rl);
    }

    private static Block block(String id) {
        ResourceLocation rl = id == null ? null : ResourceLocation.tryParse(id);
        return rl == null ? null : BuiltInRegistries.BLOCK.getOptional(rl).orElse(null);
    }
}
