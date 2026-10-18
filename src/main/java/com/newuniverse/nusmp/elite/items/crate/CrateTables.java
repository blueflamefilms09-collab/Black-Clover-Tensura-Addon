package com.newuniverse.nusmp.elite.items.crate;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Why: loads the three reward tables from {@code data/nusmp/elite/items/crates/<tier>.json} on every datapack reload, so an operator's datapack can retune or replace a tier. A file with a mistake does
 * not break the crate: bad entries are dropped and logged by {@link CrateTableParser}, entries whose type is unknown or whose {@link RewardType#validate} complains (an item id that does not exist) are
 * dropped and logged here, and a tier with nothing usable keeps a small built-in table, so a crate can always be opened. The tables are swapped in one volatile assignment, so an opening that is running
 * while a reload happens uses one consistent table.
 */
public final class CrateTables extends SimpleJsonResourceReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    private static volatile Map<CrateTier, CrateTable> tables = fallbackAll();

    public CrateTables() { super(new Gson(), "elite/items/crates"); }

    /** Mod bus is not involved: register on the game bus ({@code NeoForge.EVENT_BUS.addListener(CrateTables::onReload)}). */
    public static void onReload(AddReloadListenerEvent e) { e.addListener(new CrateTables()); }

    /** The table of a tier (never null). */
    public static CrateTable of(CrateTier tier) { return tables.get(tier); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<CrateTier, CrateTable> next = new EnumMap<>(CrateTier.class);
        for (CrateTier tier : CrateTier.values()) {
            JsonElement el = files.get(ResourceLocation.fromNamespaceAndPath("nusmp", tier.id()));
            CrateTable table = null;
            if (el != null && el.isJsonObject()) {
                CrateTableParser.Result res = CrateTableParser.parse(tier.id(), el.getAsJsonObject(), RewardSerializers::known);
                for (String err : res.errors()) LOG.warn("[nusmp] crate table: {}", err);
                if (res.table() != null) table = checked(tier, res.table());
            } else if (el != null) {
                LOG.warn("[nusmp] crate table {}: the file is not a JSON object", tier.id());
            }
            if (table == null) {
                LOG.warn("[nusmp] crate table {}: using the built-in fallback table", tier.id());
                table = fallback(tier);
            }
            next.put(tier, table);
        }
        tables = next;
    }

    /** Drops the entries whose type refuses them (an unknown item id). */
    static CrateTable checked(CrateTier tier, CrateTable t) {
        List<RewardEntry> ok = new ArrayList<>();
        for (RewardEntry e : t.entries()) {
            RewardType type = RewardSerializers.get(e.type());
            String problem = type == null ? "unknown type" : type.validate(e);
            if (problem != null) LOG.warn("[nusmp] crate table {}: dropped an entry of type {}: {}", tier.id(), e.type(), problem);
            else ok.add(e);
        }
        return ok.isEmpty() ? null : new CrateTable(t.tier(), t.rollsMin(), t.rollsMax(), ok);
    }

    static CrateTable fallback(CrateTier tier) {
        return new CrateTable(tier.id(), 1, 1, List.of(
                new RewardEntry("item", 3, "minecraft:iron_ingot", 3, 6, ""),
                new RewardEntry("xp", 1, "", 100, 200, "")));
    }

    private static Map<CrateTier, CrateTable> fallbackAll() {
        Map<CrateTier, CrateTable> m = new EnumMap<>(CrateTier.class);
        for (CrateTier t : CrateTier.values()) m.put(t, fallback(t));
        return m;
    }
}
