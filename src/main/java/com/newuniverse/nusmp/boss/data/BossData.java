package com.newuniverse.nusmp.boss.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.boss.BossEntities;
import com.newuniverse.nusmp.boss.BossStatBuff;
import com.newuniverse.nusmp.boss.rules.BossRulesConfig;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: Leon study fix 9. Loads {@code data/anime_multiverse/arc_bosses/<boss id>.json} on every data reload ({@link BossDataTable} reads one file) and swaps the result in as one immutable
 * map, exactly like {@code BossDropData}. When a registered boss joins the world its health, attack, speed and armor become the file's numbers and its move cooldowns the file's scale, through one
 * permanent multiplied-total modifier per attribute under a fixed id (so the Java number stays the base, the floor scale, the Black Clover x3 and the party scale still multiply with it, and a
 * saved boss keeps it). No file ships with the mod, so nothing changes until a pack adds one. A bad file is logged (at most {@link BossDataTable#MAX_LOGGED} lines a reload) and skipped. With
 * {@code bossRules.bossData} off every override is taken off again when the boss next joins. A reload does not touch bosses already in the world.
 * Cost: nothing per tick; one registry lookup per entity join, and at most five modifier writes for a boss that has a file.
 */
public final class BossData extends SimpleJsonResourceReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    public static final String DIRECTORY = "arc_bosses";
    private static volatile Map<String, BossDataTable.Override> files = Map.of();

    private static final ResourceLocation HEALTH = id("health"), ATTACK = id("attack"), SPEED = id("speed"), ARMOR = id("armor");

    private static ResourceLocation id(String s) { return ResourceLocation.fromNamespaceAndPath("anime_multiverse", "boss_data/" + s); }

    public BossData() { super(new Gson(), DIRECTORY); }

    /** The override of a boss id (the file name), or null. */
    public static BossDataTable.Override get(String bossId) { return files.get(bossId); }

    public static int size() { return files.size(); }

    /** Self-test hook: swaps the loaded files (null = none) and returns the old ones. */
    public static Map<String, BossDataTable.Override> install(Map<String, BossDataTable.Override> next) {
        Map<String, BossDataTable.Override> old = files;
        files = next == null ? Map.of() : Map.copyOf(next);
        return old;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> found, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, BossDataTable.Override> out = new HashMap<>();
        int bad = 0;
        for (Map.Entry<ResourceLocation, JsonElement> e : found.entrySet()) {
            String id = e.getKey().getPath();
            try {
                if (!BossDataTable.validId(id)) throw new IllegalArgumentException("not a boss id");
                if (!e.getValue().isJsonObject()) throw new IllegalArgumentException("not an object");
                out.put(id, BossDataTable.parse(e.getValue().getAsJsonObject()));
            } catch (RuntimeException ex) {
                if (++bad <= BossDataTable.MAX_LOGGED) LOG.warn("[anime_multiverse] boss data file {} ignored: {}", e.getKey(), ex.getMessage());
            }
        }
        if (bad > BossDataTable.MAX_LOGGED) LOG.warn("[anime_multiverse] {} more boss data files ignored", bad - BossDataTable.MAX_LOGGED);
        files = Map.copyOf(out);
        if (!out.isEmpty()) LOG.info("[anime_multiverse] {} boss data file(s) loaded", out.size());
    }

    /** Server side, once per join of a registered boss. */
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof LivingEntity b)) return;
        String id = BossEntities.bossId(b);
        if (id != null) apply(b, id);
    }

    /** Puts the file of 'bossId' (when the setting is on) on this boss, or takes every override off. Safe to call again: a change is a replace, no change is no write. */
    public static void apply(LivingEntity b, String bossId) {
        BossDataTable.Override o = BossRulesConfig.bossData() ? files.get(bossId) : null;
        set(b, Attributes.MAX_HEALTH, HEALTH, o == null ? null : o.health());
        set(b, Attributes.ATTACK_DAMAGE, ATTACK, o == null ? null : o.attack());
        set(b, Attributes.MOVEMENT_SPEED, SPEED, o == null ? null : o.speed());
        set(b, Attributes.ARMOR, ARMOR, o == null ? null : o.armor());
        if (b instanceof ZogratisBoss z) z.setMoveCooldownScale(o == null || o.moveCooldownScale() == null ? 1.0 : o.moveCooldownScale());
    }

    private static void set(LivingEntity b, Holder<Attribute> attribute, ResourceLocation id, Double wanted) {
        AttributeInstance inst = b.getAttribute(attribute);
        if (inst == null) return;
        BossStatBuff.setFactor(b, attribute, id, BossDataTable.factor(wanted, inst.getBaseValue()));
    }
}
