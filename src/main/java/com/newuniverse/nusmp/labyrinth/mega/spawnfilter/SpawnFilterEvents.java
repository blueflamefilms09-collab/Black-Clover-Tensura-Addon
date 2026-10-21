package com.newuniverse.nusmp.labyrinth.mega.spawnfilter;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.eco.FactionGraph;
import com.newuniverse.nusmp.labyrinth.mega.eco.FactionRegistry;
import com.newuniverse.nusmp.labyrinth.mega.hub.HubRules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the game glue of the Dungeon's spawn filter ({@link SpawnFilterRules} holds the decision). Three nets, because creatures reach a level in different
 * ways. {@code MobSpawnEvent.PositionCheck}: a natural spawn attempt of a banned type is failed before it costs anything. {@code FinalizeSpawnEvent}: a spawn
 * that goes through the normal spawn path is cancelled (and an operator's command or egg is remembered as such). {@code EntityJoinLevelEvent}: the net under
 * both, for every mod that adds a creature directly, for the mod's own encounter director, and for a creature loaded from an old save: the banned type is
 * refused there whatever brought it, and a themed spawn that does not belong on the floor is refused. All three act only in a dungeon dimension (the
 * Mega-Dungeon with its pockets, the Spire, the Sanctuary, an old Living Dungeon), never throw into the game loop, and never touch a creature that is not
 * on the list or off the floor's palette.
 */
public final class SpawnFilterEvents {
    private SpawnFilterEvents() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** Persistent-data mark left on a creature an operator spawned, so the join net can tell it apart. */
    public static final String ADMIN_KEY = "nusmp_spawn_admin";
    /** The tag of creature types that never exist in a dungeon dimension (datapacks add to it; the config list adds to it too). */
    public static final TagKey<EntityType<?>> BANNED = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("nusmp", "dungeon/banned"));
    private static final TagKey<EntityType<?>> BOSS = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("nusmp", "boss"));

    private static boolean warned;
    private static List<? extends String> parsedFrom;
    private static SpawnBlacklist parsed = SpawnBlacklist.none();
    private static FactionGraph paletteGraph;
    private static double paletteBar = -1.0;
    private static final Map<String, FloorThemeMobFilter.Palette> PALETTES = new LinkedHashMap<>();

    public static void register(IEventBus gameBus) {
        gameBus.addListener(EventPriority.HIGHEST, SpawnFilterEvents::onPositionCheck);
        gameBus.addListener(EventPriority.HIGHEST, SpawnFilterEvents::onFinalizeSpawn);
        gameBus.addListener(EventPriority.HIGHEST, SpawnFilterEvents::onEntityJoin);
        gameBus.addListener((net.neoforged.neoforge.event.TagsUpdatedEvent e) -> clearCaches());
    }

    // ---------------------------------------------------------------- the facts
    /** Is this level a dungeon dimension: the Mega-Dungeon (its pockets too), the Spire, the Sanctuary, or an old Living Dungeon? */
    public static boolean inDungeonDimension(Level level) {
        return level != null && (Labyrinth.in(level) || Labyrinth.inSanctuary(level) || Labyrinth.inRemovedDungeon(level));
    }

    /** The blacklist of the config, parsed (and kept until the config's list changes). */
    public static synchronized SpawnBlacklist blacklist() {
        List<? extends String> raw = SpawnFilterConfig.get(SpawnFilterConfig.BLACKLIST);
        if (parsedFrom == null || !parsedFrom.equals(raw)) {
            parsed = SpawnBlacklist.parse(raw);
            parsedFrom = raw == null ? null : List.copyOf(raw);
        }
        return parsed;
    }

    /** Is this entity type banned: on the config list or in the banned tag? */
    public static boolean isBanned(EntityType<?> type) {
        if (type == null) return false;
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return type.is(BANNED) || (key != null && blacklist().matches(key.toString()));
    }

    /** Is this an operator's spawn: a command or a spawn egg? */
    public static boolean isAdminSpawn(MobSpawnType type) { return type == MobSpawnType.COMMAND || type == MobSpawnType.SPAWN_EGG; }

    private static boolean isBoss(Entity e) { return e.getType().is(Tags.EntityTypes.BOSSES) || e.getType().is(BOSS); }

    /** The palette of a theme from the factions that are loaded now (cached until the factions, the tags or the minimum weight change). */
    public static synchronized FloorThemeMobFilter.Palette paletteFor(String themeId) {
        FactionGraph g = FactionRegistry.graph();
        double bar = SpawnFilterConfig.get(SpawnFilterConfig.THEME_MIN_WEIGHT);
        if (g != paletteGraph || bar != paletteBar) {
            PALETTES.clear();
            paletteGraph = g;
            paletteBar = bar;
        }
        FloorThemeMobFilter.Palette p = PALETTES.get(themeId);
        if (p == null) {
            Map<String, Double> w = new LinkedHashMap<>();
            for (String id : g.ids()) w.put(id, g.themeWeight(id, themeId));
            p = FloorThemeMobFilter.palette(themeId, w, bar);
            PALETTES.put(themeId, p);
        }
        return p;
    }

    static synchronized void clearCaches() {
        PALETTES.clear();
        paletteGraph = null;
        parsedFrom = null;
    }

    /**
     * The verdict for one creature that is about to exist in a level. Public so the self-test drives the very code the events run. {@code fresh} = a new
     * creature (false = loaded from a saved chunk); {@code admin} = an operator spawned it.
     */
    public static SpawnFilterRules.Verdict verdictFor(Entity entity, boolean fresh, boolean admin) {
        Level level = entity.level();
        if (!inDungeonDimension(level)) return SpawnFilterRules.Verdict.ALLOW;
        boolean banned = isBanned(entity.getType());
        boolean themeOn = SpawnFilterConfig.get(SpawnFilterConfig.THEME_FILTER);
        FloorThemeMobFilter.Palette palette = null;
        String faction = null;
        boolean exempt = false;
        if (!banned && themeOn && fresh && !admin) {
            faction = FactionRegistry.factionOf(entity.getType());
            if (faction != null) {
                exempt = isBoss(entity) || entity.hasCustomName() || (entity instanceof Mob m && HubRules.isOwned(m));
                if (!exempt) {
                    RegionTheme theme = MegaDungeon.themeAt(level, entity.blockPosition());
                    if (theme != null) palette = paletteFor(theme.id());
                }
            }
        }
        return SpawnFilterRules.judge(banned, admin, SpawnFilterConfig.get(SpawnFilterConfig.ADMIN_BYPASS), exempt, fresh, themeOn, palette, faction);
    }

    // ---------------------------------------------------------------- the three nets
    private static void onPositionCheck(MobSpawnEvent.PositionCheck e) {
        try {
            if (e.getSpawnType() == null || isAdminSpawn(e.getSpawnType())) return;
            Level level = e.getLevel().getLevel();
            if (!inDungeonDimension(level) || !isBanned(e.getEntity().getType())) return;
            e.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        } catch (RuntimeException ex) {
            failed("position check", ex);
        }
    }

    public static void onFinalizeSpawn(FinalizeSpawnEvent e) {
        try {
            Mob mob = e.getEntity();
            if (!inDungeonDimension(e.getLevel().getLevel())) return;
            boolean admin = isAdminSpawn(e.getSpawnType());
            if (admin) mob.getPersistentData().putBoolean(ADMIN_KEY, true);
            if (verdictFor(mob, true, admin) != SpawnFilterRules.Verdict.ALLOW) {
                e.setSpawnCancelled(true);
                e.setCanceled(true);
            }
        } catch (RuntimeException ex) {
            failed("spawn filter", ex);
        }
    }

    public static void onEntityJoin(EntityJoinLevelEvent e) {
        try {
            Entity entity = e.getEntity();
            if (e.getLevel().isClientSide() || !inDungeonDimension(e.getLevel())) return;
            boolean admin = entity.getPersistentData().getBoolean(ADMIN_KEY);
            if (verdictFor(entity, !e.loadedFromDisk(), admin) != SpawnFilterRules.Verdict.ALLOW) e.setCanceled(true);
        } catch (RuntimeException ex) {
            failed("join filter", ex);
        }
    }

    private static void failed(String where, RuntimeException ex) {
        if (warned) return;
        warned = true;
        LOG.warn("[nusmp] the dungeon spawn filter ({}) hit a fault and lets the creature through (logged once): {}", where, ex.toString());
    }
}
