package com.newuniverse.nusmp.elite.nations.world;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.elite.nations.contract.ContractTemplate;
import com.newuniverse.nusmp.elite.nations.contract.ContractTemplates;
import com.newuniverse.nusmp.elite.nations.core.PerkTable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why: one reload listener for the data of the Nations system under {@code data/<ns>/elite/nations/}: {@code perks.json} (the perks per level) and {@code contracts/*.json} (the contract templates). The game
 * lists the files across every datapack in priority order, so a datapack can replace the perks or add or override a contract by id, and {@code /reload} applies it. A bad file is logged and skipped; when a
 * datapack leaves out the perks, the built-in table is used. The new sets are swapped in whole.
 */
public final class NationsLoader extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "elite/nations";
    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    public NationsLoader() { super(GSON, DIRECTORY); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        PerkTable perks = null;
        List<ContractTemplate> contracts = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
            String path = e.getKey().getPath();
            try {
                if (path.equals("perks")) {
                    perks = PerkTable.parse(e.getValue().getAsJsonObject());
                    for (String w : perks.warnings()) LOG.warn("[nusmp] nation perks: {}", w);
                } else if (path.startsWith("contracts/")) {
                    String id = path.substring("contracts/".length());
                    contracts.add(ContractTemplate.parse(id, e.getValue().getAsJsonObject()));
                }
            } catch (RuntimeException ex) {
                LOG.warn("[nusmp] nation data file {} skipped: {}", e.getKey(), ex.getMessage());
            }
        }
        NationPerks.setTable(perks);
        ContractTemplates.set(contracts);
        LOG.info("[nusmp] nations data: {} perks, {} contract templates", perks == null ? 0 : perks.perkCount(), contracts.size());
    }
}
