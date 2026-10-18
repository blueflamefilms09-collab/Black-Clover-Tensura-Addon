package com.newuniverse.nusmp.elite.forge.store;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.elite.api.EliteHooks;
import com.newuniverse.nusmp.elite.forge.ForgeConfig;
import com.newuniverse.nusmp.elite.forge.rules.ForgeProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.UUID;

/**
 * Why: "Crafting Progress Storage" in one place. The progress of a player ({@link ForgeProgress}: items crafted per tier and quality, best quality, specialization,
 * station knowledge) is a NeoForge attachment {@code nusmp:forge_progress} (saved with the player, copied to the new body on death, absent from the save of
 * anyone who never forged, tolerant of missing keys through {@link ProgressTag}). The totals the other systems ask for through {@code EliteHooks} are mirrored in
 * the world {@link ForgeLedger} so a UUID of an offline player answers too; {@link #install} sets the two hooks this subsystem owns. A hook answers 0 while the
 * server is not running or the master switch is off.
 */
public final class ForgeStore {
    private ForgeStore() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);

    private static final Codec<ForgeProgress> CODEC = CompoundTag.CODEC.xmap(ProgressTag::read, ProgressTag::write);

    /** {@code nusmp:forge_progress}. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ForgeProgress>> PROGRESS = ATTACHMENTS.register("forge_progress",
            () -> AttachmentType.builder(() -> new ForgeProgress()).serialize(CODEC, p -> !p.isEmpty()).copyOnDeath().build());

    private static volatile MinecraftServer server;

    /** The player's live progress object (created empty on first use). */
    public static ForgeProgress of(Player player) { return player.getData(PROGRESS); }

    public static void serverStarted(MinecraftServer s) { server = s; }

    public static void serverStopped() { server = null; }

    public static MinecraftServer server() { return server; }

    /** Copies the player's totals into the ledger (after a forge and at login). */
    public static void mirror(ServerPlayer p) {
        MinecraftServer s = p.getServer();
        if (s == null) return;
        ForgeProgress pr = of(p);
        ForgeLedger.get(s).put(p.getUUID(), pr.crafted(), pr.best());
    }

    /** Items the player has forged, by UUID (online or not); 0 when the system is off or the server is not running. */
    public static int craftedCount(UUID id) {
        MinecraftServer s = server;
        if (s == null || !ForgeConfig.enabled()) return 0;
        ServerPlayer online = s.getPlayerList().getPlayer(id);
        if (online != null) return of(online).crafted();
        return ForgeLedger.get(s).crafted(id);
    }

    /** The best quality (0 none, 1 Common .. 6 Mythical) the player has forged, by UUID. */
    public static int bestQuality(UUID id) {
        MinecraftServer s = server;
        if (s == null || !ForgeConfig.enabled()) return 0;
        ServerPlayer online = s.getPlayerList().getPlayer(id);
        if (online != null) return of(online).best();
        return ForgeLedger.get(s).best(id);
    }

    /** Sets the two hooks this subsystem owns (called once from the registry's init). */
    public static void install() {
        EliteHooks.forgeCraftedCount = ForgeStore::craftedCount;
        EliteHooks.forgeBestQuality = ForgeStore::bestQuality;
    }
}
