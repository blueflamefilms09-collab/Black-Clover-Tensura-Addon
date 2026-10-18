package com.newuniverse.nusmp.elite.records;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.elite.records.rule.Bonuses;
import com.newuniverse.nusmp.elite.records.rule.Catalog;
import com.newuniverse.nusmp.elite.records.rule.Defs.RecordDef;
import com.newuniverse.nusmp.elite.records.rule.Defs.TitleDef;
import com.newuniverse.nusmp.elite.records.rule.Engine;
import com.newuniverse.nusmp.elite.records.rule.Facts;
import com.newuniverse.nusmp.elite.records.rule.FactsBuilder;
import com.newuniverse.nusmp.elite.records.rule.Live;
import com.newuniverse.nusmp.elite.records.rule.RecordData;
import com.newuniverse.nusmp.elite.records.rule.Rarity;
import com.newuniverse.nusmp.elite.records.rule.StatNames;
import com.newuniverse.nusmp.elite.records.rule.Tracking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the server half of the records system, in one place. On a staggered beat (every {@code beatTicks} ticks per player) it samples the world (the biome, now and then the structure the player stands
 * in), reads the live game ({@link LiveReader}), folds it into the saved data, runs the pure unlock loop ({@link Engine}) and, when something unlocked, toasts and tells the player (and everyone, for a
 * legendary or mythic record), then makes the player's attribute modifiers match their active title and synergies ({@link TitleApplier}). Between beats it counts the things nobody else counts for it,
 * from the game events: kills (by group and by entity), the hardest single hit, deaths. The other elite subsystems feed it through {@link RecordEvents}, which only sets counters and asks for an early
 * beat. It also implements the three custom stats it owns ({@code xp_bonus_percent}, {@code kill_heal}, {@code boss_damage_percent}) and owns the four {@code EliteHooks} readers. Everything is guarded:
 * a failure on a beat is logged once and the next beat tries again; a player's tick is never broken by this system.
 */
public final class RecordsService {
    private RecordsService() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final Set<UUID> SOON = new HashSet<>();
    private static final Map<UUID, Long> LAST_REQUEST = new HashMap<>();
    private static boolean warned;

    /** What the hooks can still say about a player who has logged out since the server started. */
    private record Snapshot(Set<String> records, Set<String> titles, String active) {}

    private static final Map<UUID, Snapshot> OFFLINE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<UUID, Snapshot> eldest) { return size() > 2048; }
    };

    static void register() {
        NeoForge.EVENT_BUS.addListener(RecordsService::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(RecordsService::onDeath);
        NeoForge.EVENT_BUS.addListener(RecordsService::onDamaged);
        NeoForge.EVENT_BUS.addListener(RecordsService::onIncoming);
        NeoForge.EVENT_BUS.addListener(RecordsService::onXp);
        NeoForge.EVENT_BUS.addListener(RecordsService::onLogin);
        NeoForge.EVENT_BUS.addListener(RecordsService::onLogout);
    }

    // ------------------------------------------------------------------ the beat
    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        boolean soon = !SOON.isEmpty() && SOON.remove(p.getUUID());
        if (!soon && (p.tickCount + p.getId()) % RecordsConfig.beatTicks() != 0) return;
        if (!RecordsConfig.enabled()) {
            TitleApplier.apply(p, Bonuses.NONE);
            return;
        }
        try {
            beat(p, false);
        } catch (RuntimeException | LinkageError ex) {
            if (!warned) {
                warned = true;
                LOG.warn("[nusmp] The records beat failed for {} (it will try again): {}", p.getGameProfile().getName(), ex.toString(), ex);
            }
        }
    }

    /** Asks for an early beat for the player (their next tick). Used by {@link RecordEvents} so a toast comes at once. */
    static void evaluateSoon(UUID id) {
        if (id != null) SOON.add(id);
    }

    /**
     * One full evaluation of one player: sample, read, fold, unlock, announce, apply. {@code sync} forces a state packet even when nothing unlocked (the screen asked for it).
     * Returns what unlocked.
     */
    static Engine.Result beat(ServerPlayer p, boolean sync) {
        Catalog cat = RecordsCatalog.get();
        RecordData d = RecordsAttachments.of(p);
        sampleWorld(p, d);
        Live live = LiveReader.read(p);
        Facts facts = FactsBuilder.build(d, live);
        Engine.Result r = Engine.run(cat, facts, d, System.currentTimeMillis() / 1000L);
        if (r.any()) announce(p, cat, r);
        TitleApplier.apply(p, TitleApplier.desired(cat, d));
        if (r.any() || sync) RecordsSync.sendState(p, facts);
        return r;
    }

    private static void sampleWorld(ServerPlayer p, RecordData d) {
        var biome = p.level().getBiome(p.blockPosition()).unwrapKey();
        if (biome.isPresent()) Tracking.biome(d, biome.get().location().toString());
        if (!RecordsConfig.trackStructures() || (p.tickCount / Math.max(1, RecordsConfig.beatTicks())) % 4 != 0) return;
        ServerLevel level = p.serverLevel();
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (Structure s : level.structureManager().getAllStructuresAt(p.blockPosition()).keySet()) {
            ResourceLocation key = registry.getKey(s);
            if (key != null) Tracking.structure(d, key.toString());
        }
    }

    private static void announce(ServerPlayer p, Catalog cat, Engine.Result r) {
        boolean defs = false;
        for (String id : r.newRecords()) {
            RecordDef def = cat.record(id);
            if (def == null) continue;
            MutableComponent name = Component.literal(def.name()).withStyle(Style.EMPTY.withColor(def.rarity().color));
            if (!RecordsSync.wired(p)) continue;                              // a stand-in player (self-test) has nobody to tell
            p.sendSystemMessage(RecordsText.tr(RecordsText.MSG_RECORD, name).withStyle(ChatFormatting.GRAY));
            if (RecordsConfig.toasts()) RecordsSync.toast(p, 0, def.id(), def.name(), def.rarity().ordinal());
            if (def.hidden()) defs = true;
            if (def.rarity().ordinal() >= Rarity.LEGENDARY.ordinal() && RecordsConfig.announceRare() && p.getServer() != null) {
                Component line = RecordsText.tr(RecordsText.MSG_ANNOUNCE, p.getDisplayName(), RecordsText.rarity(def.rarity()), name).withStyle(ChatFormatting.GOLD);
                p.getServer().getPlayerList().broadcastSystemMessage(line, false);
            }
            p.playNotifySound(def.rarity().ordinal() >= Rarity.EPIC.ordinal() ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7f, 1.0f);
        }
        for (String id : r.newTitles()) {
            TitleDef def = cat.title(id);
            if (def == null || !RecordsSync.wired(p)) continue;
            MutableComponent name = Component.literal(def.name()).withStyle(Style.EMPTY.withColor(def.rarity().color));
            p.sendSystemMessage(RecordsText.tr(RecordsText.MSG_TITLE, name).withStyle(ChatFormatting.GRAY));
            if (RecordsConfig.toasts()) RecordsSync.toast(p, 1, def.id(), def.name(), def.rarity().ordinal());
        }
        if (defs) RecordsSync.sendDefs(p);
    }

    // ------------------------------------------------------------------ client requests
    static void onRequest(ServerPlayer p) {
        if (!RecordsConfig.enabled()) return;
        long now = p.serverLevel().getGameTime();
        Long last = LAST_REQUEST.get(p.getUUID());
        if (last != null && now - last < 10) return;
        LAST_REQUEST.put(p.getUUID(), now);
        try {
            beat(p, true);
        } catch (RuntimeException | LinkageError ex) {
            LOG.warn("[nusmp] Records request failed for {}: {}", p.getGameProfile().getName(), ex.toString());
        }
    }

    static void onSetActive(ServerPlayer p, String titleId) {
        if (!RecordsConfig.enabled()) return;
        RecordData d = RecordsAttachments.of(p);
        Catalog cat = RecordsCatalog.get();
        boolean wired = RecordsSync.wired(p);
        if (titleId == null || titleId.isEmpty()) {
            d.setActive("");
            if (wired) p.displayClientMessage(RecordsText.tr(RecordsText.MSG_ACTIVE_CLEARED), true);
        } else if (cat.title(titleId) != null && d.setActive(titleId)) {
            TitleDef t = cat.title(titleId);
            if (wired) p.displayClientMessage(RecordsText.tr(RecordsText.MSG_ACTIVE_SET, Component.literal(t.name()).withStyle(Style.EMPTY.withColor(t.rarity().color))), true);
        } else if (wired) {
            p.displayClientMessage(RecordsText.tr(RecordsText.MSG_ACTIVE_REFUSED).withStyle(ChatFormatting.RED), true);
        }
        TitleApplier.apply(p, TitleApplier.desired(cat, d));
        RecordsSync.sendState(p, null);
    }

    // ------------------------------------------------------------------ game events
    private static void onDeath(LivingDeathEvent e) {
        if (!RecordsConfig.enabled()) return;
        LivingEntity victim = e.getEntity();
        if (victim instanceof ServerPlayer sp) Tracking.death(RecordsAttachments.of(sp));
        Entity src = e.getSource().getEntity();
        if (!(src instanceof ServerPlayer killer) || killer == victim) return;
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        MobCategory cat = victim.getType().getCategory();
        boolean player = victim instanceof Player, hostile = cat == MobCategory.MONSTER, boss = victim.getType().is(Tags.EntityTypes.BOSSES);
        boolean passive = !player && !hostile && cat != MobCategory.MISC;
        Tracking.kill(RecordsAttachments.of(killer), new Tracking.Victim(key.toString(), hostile, passive, player, boss));
        double heal = TitleApplier.stat(killer.getUUID(), StatNames.KILL_HEAL);
        if (heal > 0 && killer.isAlive()) killer.heal((float) heal);
    }

    private static void onDamaged(LivingDamageEvent.Post e) {
        if (!RecordsConfig.enabled()) return;
        if (e.getSource().getEntity() instanceof ServerPlayer attacker && attacker != e.getEntity()) Tracking.hit(RecordsAttachments.of(attacker), e.getNewDamage());
    }

    private static void onIncoming(LivingIncomingDamageEvent e) {
        if (!RecordsConfig.enabled() || !(e.getSource().getEntity() instanceof ServerPlayer attacker) || !e.getEntity().getType().is(Tags.EntityTypes.BOSSES)) return;
        double pct = TitleApplier.stat(attacker.getUUID(), StatNames.BOSS_DAMAGE_PERCENT);
        if (pct > 0) e.setAmount((float) (e.getAmount() * (1.0 + pct / 100.0)));
    }

    private static void onXp(LivingExperienceDropEvent e) {
        if (!RecordsConfig.enabled() || e.getAttackingPlayer() == null) return;
        double pct = TitleApplier.stat(e.getAttackingPlayer().getUUID(), StatNames.XP_BONUS_PERCENT);
        if (pct > 0) e.setDroppedExperience((int) Math.round(e.getDroppedExperience() * (1.0 + pct / 100.0)));
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        OFFLINE.remove(p.getUUID());
        if (!RecordsConfig.enabled()) return;
        RecordsSync.sendDefs(p);
        evaluateSoon(p.getUUID());
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        RecordData d = RecordsAttachments.of(p);
        OFFLINE.put(p.getUUID(), new Snapshot(new HashSet<>(d.unlockedIds()), new HashSet<>(d.heldTitles()), d.active()));
        TitleApplier.forget(p.getUUID());
        SOON.remove(p.getUUID());
        LAST_REQUEST.remove(p.getUUID());
    }

    // ------------------------------------------------------------------ administration (commands, self-tests, other subsystems)
    /** Unlocks a record by id regardless of its condition (quest rewards, commands). Returns true when it was new. */
    static boolean grantRecord(ServerPlayer p, String id) {
        Catalog cat = RecordsCatalog.get();
        RecordDef def = cat.record(id);
        if (def == null || !RecordsAttachments.of(p).unlock(id, System.currentTimeMillis() / 1000L)) return false;
        announce(p, cat, new Engine.Result(java.util.List.of(id), java.util.List.of()));
        evaluateSoon(p.getUUID());
        return true;
    }

    static boolean revokeRecord(ServerPlayer p, String id) {
        boolean had = RecordsAttachments.of(p).lock(id);
        if (had) evaluateSoon(p.getUUID());
        return had;
    }

    static boolean grantTitle(ServerPlayer p, String id) {
        Catalog cat = RecordsCatalog.get();
        TitleDef def = cat.title(id);
        if (def == null || !RecordsAttachments.of(p).grantTitle(id, System.currentTimeMillis() / 1000L)) return false;
        announce(p, cat, new Engine.Result(java.util.List.of(), java.util.List.of(id)));
        evaluateSoon(p.getUUID());
        return true;
    }

    static boolean revokeTitle(ServerPlayer p, String id) {
        boolean had = RecordsAttachments.of(p).revokeTitle(id);
        if (had) evaluateSoon(p.getUUID());
        return had;
    }

    static void reset(ServerPlayer p) {
        RecordsAttachments.of(p).clear();
        TitleApplier.clear(p);
        RecordsSync.sendState(p, null);
        RecordsSync.sendDefs(p);
    }

    // ------------------------------------------------------------------ the EliteHooks readers
    private static ServerPlayer online(UUID id) {
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        return s == null || id == null ? null : s.getPlayerList().getPlayer(id);
    }

    static boolean recordUnlocked(UUID id, String record) {
        if (!RecordsConfig.enabled() || record == null) return false;
        ServerPlayer p = online(id);
        if (p != null) return RecordsAttachments.of(p).isUnlocked(record);
        Snapshot s = OFFLINE.get(id);
        return s != null && s.records().contains(record);
    }

    static boolean hasTitle(UUID id, String title) {
        if (!RecordsConfig.enabled() || title == null) return false;
        ServerPlayer p = online(id);
        if (p != null) return RecordsAttachments.of(p).holds(title);
        Snapshot s = OFFLINE.get(id);
        return s != null && s.titles().contains(title);
    }

    static String activeTitle(UUID id) {
        if (!RecordsConfig.enabled()) return "";
        ServerPlayer p = online(id);
        if (p != null) return RecordsAttachments.of(p).active();
        Snapshot s = OFFLINE.get(id);
        return s == null ? "" : s.active();
    }

    static int heldTitleCount(UUID id) {
        if (!RecordsConfig.enabled()) return 0;
        ServerPlayer p = online(id);
        if (p != null) return RecordsAttachments.of(p).heldCount();
        Snapshot s = OFFLINE.get(id);
        return s == null ? 0 : s.titles().size();
    }

    /** The key a pure test or the screen may use to name an entity type. */
    static String entityKey(LivingEntity e) {
        ResourceKey<?> k = BuiltInRegistries.ENTITY_TYPE.getResourceKey(e.getType()).orElse(null);
        return k == null ? "" : k.location().toString();
    }
}
