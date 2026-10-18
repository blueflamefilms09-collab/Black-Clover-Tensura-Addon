package com.newuniverse.nusmp.elite.hub;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.NUSMP;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.Map;

/**
 * Why: the "IPlayerQuestData capability" of the brief is, in NeoForge 1.21, a DATA ATTACHMENT (the replacement for capabilities that holds saved per-player data), and the mod already does it this
 * way for the System ({@code SystemAttachments}). {@code nusmp:hub_quests} holds the {@link PlayerQuestData} (accepted quests, done quests, the day book-keeping, the deepest floor reached and
 * the shop's per-day counters) and {@code nusmp:hub_map} holds the {@link MapMemory} (the rooms seen, the floors mapped). Both are saved with the player, follow the player through dimensions and
 * restarts and are copied on death (a quest chain or a map must not be lost by dying). Each is serialised with a {@link Codec} built from the plain pure {@code Data} record, with
 * {@code optionalFieldOf} on every field so a save from before a field existed, a hand-edited file or a damaged value still loads (the pure classes clamp what they read); an empty value is not
 * written at all. Neither is synced by NeoForge: the client gets {@link HubNet} packets, which also carry things that are not in the attachment. Register {@link #ATTACHMENTS} on the mod bus.
 */
public final class HubAttachments {
    private HubAttachments() {}

    public static final Codec<PlayerQuestData.ActiveData> ACTIVE_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("id", "").forGetter(PlayerQuestData.ActiveData::id),
            Codec.INT.optionalFieldOf("progress", 0).forGetter(PlayerQuestData.ActiveData::progress),
            Codec.INT.optionalFieldOf("target", 1).forGetter(PlayerQuestData.ActiveData::target),
            Codec.STRING.optionalFieldOf("state", "ACTIVE").forGetter(PlayerQuestData.ActiveData::state),
            Codec.LONG.optionalFieldOf("accepted", 0L).forGetter(PlayerQuestData.ActiveData::accepted)
    ).apply(i, PlayerQuestData.ActiveData::new));

    public static final Codec<PlayerQuestData.Data> QUEST_DATA_CODEC = RecordCodecBuilder.create(i -> i.group(
            ACTIVE_CODEC.listOf().optionalFieldOf("active", List.of()).forGetter(PlayerQuestData.Data::active),
            Codec.STRING.listOf().optionalFieldOf("completed", List.of()).forGetter(PlayerQuestData.Data::completed),
            Codec.LONG.optionalFieldOf("daily_day", -1L).forGetter(PlayerQuestData.Data::dailyDay),
            Codec.STRING.listOf().optionalFieldOf("daily_done", List.of()).forGetter(PlayerQuestData.Data::dailyDone),
            Codec.INT.optionalFieldOf("max_holy", 0).forGetter(PlayerQuestData.Data::maxHoly),
            Codec.INT.optionalFieldOf("max_abyssal", 0).forGetter(PlayerQuestData.Data::maxAbyssal),
            Codec.LONG.optionalFieldOf("shop_day", -1L).forGetter(PlayerQuestData.Data::shopDay),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("shop_bought", Map.of()).forGetter(PlayerQuestData.Data::shopBought),
            Codec.INT.optionalFieldOf("total_done", 0).forGetter(PlayerQuestData.Data::totalDone)
    ).apply(i, PlayerQuestData.Data::new));

    public static final Codec<PlayerQuestData> QUEST_CODEC = QUEST_DATA_CODEC.xmap(PlayerQuestData::of, PlayerQuestData::toData);

    public static final Codec<MapMemory.Data> MAP_DATA_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.listOf().optionalFieldOf("visited", List.of()).forGetter(MapMemory.Data::visited),
            Codec.LONG.listOf().optionalFieldOf("mapped", List.of()).forGetter(MapMemory.Data::mapped)
    ).apply(i, MapMemory.Data::new));

    public static final Codec<MapMemory> MAP_CODEC = MAP_DATA_CODEC.xmap(MapMemory::of, MapMemory::toData);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);

    /** {@code nusmp:hub_quests}: the player's quests, their done list and the shop counters. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerQuestData>> QUESTS = ATTACHMENTS.register("hub_quests",
            () -> AttachmentType.builder(() -> new PlayerQuestData()).serialize(QUEST_CODEC, PlayerQuestData::hasContent).copyOnDeath().build());

    /** {@code nusmp:hub_map}: the rooms the player has seen and the floors they have mapped. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<MapMemory>> MAP = ATTACHMENTS.register("hub_map",
            () -> AttachmentType.builder(() -> new MapMemory()).serialize(MAP_CODEC, MapMemory::hasContent).copyOnDeath().build());
}
