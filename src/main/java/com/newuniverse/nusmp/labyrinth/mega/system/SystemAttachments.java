package com.newuniverse.nusmp.labyrinth.mega.system;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.NUSMP;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.Optional;

/**
 * Why: the System's per-player memory is a NeoForge DATA ATTACHMENT (the 1.21 form of the "capability" the owner's brief asks for: a permanent stat boost
 * capability), registered under the contract's id {@code nusmp:system_state}. An attachment, not a world table or the player's persistent NBT, because it is
 * saved with the player and follows the player for free through dimensions, restarts and death ({@code copyOnDeath()}: the stat points must survive dying, or
 * the reward would be a joke). It is serialised with a {@link Codec} built from the plain {@link SystemState.Data}, with an {@code optionalFieldOf} for every
 * field so a save from before a field existed, a hand-edited file or a damaged value still loads (the state class clamps what it reads). It is not synced by
 * NeoForge: the client gets {@link SystemNet.Sync}, which also carries things that are not in the attachment (the floor, the road). An empty state is not
 * written at all. Register {@link #ATTACHMENTS} on the mod bus.
 */
public final class SystemAttachments {
    private SystemAttachments() {}

    /** The quest, field by field. */
    public static final Codec<QuestProgress.Data> QUEST_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("id", "").forGetter(QuestProgress.Data::id),
            Codec.STRING.optionalFieldOf("kind", "").forGetter(QuestProgress.Data::kind),
            Codec.INT.optionalFieldOf("floor", 0).forGetter(QuestProgress.Data::floor),
            Codec.INT.optionalFieldOf("target", 1).forGetter(QuestProgress.Data::target),
            Codec.INT.optionalFieldOf("progress", 0).forGetter(QuestProgress.Data::progress),
            Codec.STRING.optionalFieldOf("state", "ACTIVE").forGetter(QuestProgress.Data::state),
            Codec.BOOL.optionalFieldOf("no_magic", false).forGetter(QuestProgress.Data::noMagic),
            Codec.INT.optionalFieldOf("time_left", -1).forGetter(QuestProgress.Data::timeLeft),
            Codec.INT.listOf().optionalFieldOf("counted", List.of()).forGetter(QuestProgress.Data::counted)
    ).apply(i, QuestProgress.Data::new));

    /** The whole state, field by field. */
    public static final Codec<SystemState.Data> DATA_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.listOf().optionalFieldOf("points", List.of()).forGetter(SystemState.Data::points),
            Codec.INT.optionalFieldOf("cores", 0).forGetter(SystemState.Data::cores),
            Codec.INT.optionalFieldOf("max_floor", 0).forGetter(SystemState.Data::maxFloor),
            Codec.STRING.optionalFieldOf("last_kind", "").forGetter(SystemState.Data::lastKind),
            Codec.INT.optionalFieldOf("done", 0).forGetter(SystemState.Data::done),
            Codec.INT.optionalFieldOf("failed", 0).forGetter(SystemState.Data::failed),
            QUEST_CODEC.optionalFieldOf("quest").forGetter(d -> Optional.ofNullable(d.quest())),
            Codec.BOOL.optionalFieldOf("hint", false).forGetter(SystemState.Data::hint)
    ).apply(i, (points, cores, maxFloor, lastKind, done, failed, quest, hint) ->
            new SystemState.Data(points, cores, maxFloor, lastKind, done, failed, quest.orElse(null), hint)));

    public static final Codec<SystemState> CODEC = DATA_CODEC.xmap(SystemState::of, SystemState::toData);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);

    /** {@code nusmp:system_state}: the player's quest, stat points and counters. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SystemState>> STATE = ATTACHMENTS.register("system_state",
            () -> AttachmentType.builder(() -> new SystemState()).serialize(CODEC, SystemState::hasContent).copyOnDeath().build());
}
