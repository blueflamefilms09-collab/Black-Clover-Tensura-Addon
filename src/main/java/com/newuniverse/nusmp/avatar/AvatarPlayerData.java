package com.newuniverse.nusmp.avatar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the creator needs a few facts that outlive any one Avatar (which Avatars are theirs, whether they were already handed their guaranteed first Genesis
 * Core, how many they ever made), and they belong to the player, so they are a NeoForge data attachment on the player, serialised with a codec and copied on
 * death (the owner asked for the Data Attachments API). Per-Avatar data stays in the Avatar's own NBT because an entity's fields are the right place for them
 * (they are saved, loaded and moved with the entity for free, including across dimensions). Immutable: every change makes a new record and sets it back.
 */
public record AvatarPlayerData(List<UUID> avatars, boolean coreGranted, int created) {
    public static final AvatarPlayerData EMPTY = new AvatarPlayerData(List.of(), false, 0);

    public static final Codec<AvatarPlayerData> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.listOf().optionalFieldOf("avatars", List.of()).forGetter(AvatarPlayerData::avatars),
            Codec.BOOL.optionalFieldOf("core_granted", false).forGetter(AvatarPlayerData::coreGranted),
            Codec.INT.optionalFieldOf("created", 0).forGetter(AvatarPlayerData::created)).apply(i, AvatarPlayerData::new));

    public AvatarPlayerData withAvatar(UUID id) {
        List<UUID> l = new ArrayList<>(avatars);
        if (!l.contains(id)) l.add(id);
        return new AvatarPlayerData(List.copyOf(l), coreGranted, created);
    }

    public AvatarPlayerData withoutAvatar(UUID id) {
        List<UUID> l = new ArrayList<>(avatars);
        l.remove(id);
        return new AvatarPlayerData(List.copyOf(l), coreGranted, created);
    }

    public AvatarPlayerData withCoreGranted() { return new AvatarPlayerData(avatars, true, created); }

    public AvatarPlayerData withCreated() { return new AvatarPlayerData(avatars, coreGranted, created + 1); }
}
