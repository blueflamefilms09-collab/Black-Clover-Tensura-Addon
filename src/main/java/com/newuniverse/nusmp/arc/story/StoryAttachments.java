package com.newuniverse.nusmp.arc.story;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.Map;

/**
 * Why: the story progress of a player has to live with the player, survive death and the trip through the End, and stay out of the save file of anyone who
 * never touched the arcs. A NeoForge attachment {@code nusmp:arc_story} does exactly that (it is also how the dungeon alignment and the discovery list are
 * stored). It is serialised with a codec of four optional fields, so a save written by an older build, edited by hand, or missing a field still loads, and
 * the predicate on the serializer skips an empty story. The attachment is not synced to the client: nothing on the client reads it (the dialogue is chosen
 * on the server and sent as chat). Register {@link #ATTACHMENTS} on the mod bus ({@code ArcStory.init} does).
 */
public final class StoryAttachments {
    private StoryAttachments() {}

    /** The stored form: {@code {defeated: [ids], met: [ids], quest: {npc: stage}, saga: "id"}}, every key optional. */
    public static final Codec<StoryData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("defeated", List.of()).forGetter(StoryData::defeatedList),
            Codec.STRING.listOf().optionalFieldOf("met", List.of()).forGetter(StoryData::metList),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("quest", Map.of()).forGetter(StoryData::questMap),
            Codec.STRING.optionalFieldOf("saga", "").forGetter(StoryData::saga)
    ).apply(i, StoryData::of));

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);

    /** {@code nusmp:arc_story}: saved with the player (unless still empty), copied to the new body on death and on the End return. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<StoryData>> STORY = ATTACHMENTS.register("arc_story",
            () -> AttachmentType.builder(() -> new StoryData()).serialize(CODEC, d -> !d.isEmpty()).copyOnDeath().build());

    /** The player's story (created empty on first use). The returned object is live: changing it changes the player's save. */
    public static StoryData get(Player player) { return player.getData(STORY); }
}
