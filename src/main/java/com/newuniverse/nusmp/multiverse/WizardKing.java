package com.newuniverse.nusmp.multiverse;

import com.newuniverse.nusmp.skill.NUSkills;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * 1.6.0: the Wizard King is a title only one player on the server can hold. It counts as an <b>awakening</b> of its own (the
 * Black Clover answer to a true hero or a demon lord: there is no choosing between those, the crown is a third path): the holder gets
 * the awakening's kind of power, a large jump in health, defence, attack and speed, mana that refills itself, Saint Stage
 * casting, and the Wizard King's own skill (see {@link WizardKingSkill}). Take the crown from the holder by earning more net stars
 * than they have (a Grand Magic Knight with at least the configured stars), or have an admin appoint someone. Losing the crown takes
 * the awakening and the skill back.
 */
public final class WizardKing {
    private WizardKing() {}

    // ---------------------------------------------------------------- the registry (one holder, saved with the world)
    static final class Registry extends SavedData {
        UUID holder;
        String name = "";
        int stars;

        static Registry get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Registry::new, Registry::load, null), "nusmp_wizard_king");
        }

        private static Registry load(CompoundTag tag, HolderLookup.Provider provider) {
            Registry r = new Registry();
            if (tag.hasUUID("Holder")) r.holder = tag.getUUID("Holder");
            r.name = tag.getString("Name");
            r.stars = tag.getInt("Stars");
            return r;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            if (holder != null) tag.putUUID("Holder", holder);
            tag.putString("Name", name);
            tag.putInt("Stars", stars);
            return tag;
        }
    }

    public static Optional<UUID> holder(MinecraftServer server) { return server == null ? Optional.empty() : Optional.ofNullable(Registry.get(server).holder); }

    public static String holderName(MinecraftServer server) { return server == null ? "" : Registry.get(server).name; }

    public static boolean isKing(Player p) {
        MinecraftServer s = p.getServer();
        return s != null && p.getUUID().equals(Registry.get(s).holder);
    }

    // ---------------------------------------------------------------- appointing
    /** True while the crown itself is changing a rank (so a plain "rank set wizard_king" has to go through the crown). */
    static boolean appointing;

    public static void appoint(MinecraftServer server, ServerPlayer p) {
        Registry r = Registry.get(server);
        if (p.getUUID().equals(r.holder)) return;
        if (r.holder != null) {
            ServerPlayer old = server.getPlayerList().getPlayer(r.holder);
            if (old != null) demote(old);                                       // an offline holder is demoted when they next log in
        }
        r.holder = p.getUUID();
        r.name = p.getName().getString();
        r.stars = MultiverseProfile.netStars(p);
        r.setDirty();
        crown(p);
        server.getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString() + " is the new Wizard King! The mana of the whole kingdom bows to them.")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
    }

    public static void dethrone(MinecraftServer server) {
        Registry r = Registry.get(server);
        if (r.holder == null) return;
        ServerPlayer old = server.getPlayerList().getPlayer(r.holder);
        r.holder = null;
        r.name = "";
        r.setDirty();
        if (old != null) demote(old);
        server.getPlayerList().broadcastSystemMessage(Component.literal("The Wizard King's crown is vacant.").withStyle(ChatFormatting.GRAY), false);
    }

    private static void crown(ServerPlayer p) {
        appointing = true;
        try { MultiverseProfile.setRank(p, KnightRank.WIZARD_KING, 0); } finally { appointing = false; }
        var skills = SkillAPI.getSkillsFrom(p);
        if (skills.getSkill(NUSkills.WIZARD_KING.getId()).isEmpty()) {
            ManasSkillInstance inst = NUSkills.WIZARD_KING.get().createDefaultInstance();
            skills.learnSkill(inst, Component.literal("The Wizard King's awakening: the crown's own power is yours.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
        var c = p.position().add(0, 1, 0);
        VfxSpawn.send(p.serverLevel(), VfxShape.MAGIC_CIRCLE_EXPLOSION, c, c.add(0, 1, 0), 0xFFFFD86B, 40, 2.2f);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.5f, 0.7f);
        MultiverseSync.markDirty(p);
    }

    private static void demote(ServerPlayer p) {
        appointing = true;
        try {
            if (MultiverseProfile.rank(p) == KnightRank.WIZARD_KING) MultiverseProfile.setRank(p, KnightRank.GRAND, 0);
        } finally { appointing = false; }
        SkillAPI.getSkillsFrom(p).forgetSkill(NUSkills.WIZARD_KING.getId());
        p.sendSystemMessage(Component.literal("You are no longer the Wizard King. The awakening leaves you.").withStyle(ChatFormatting.GRAY));
        MultiverseSync.markDirty(p);
    }

    /** Takes the crown by earning more net stars than the holder. Returns a reason on failure, or null. */
    public static String claim(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        if (isKing(p)) return "You already wear the crown.";
        if (MultiverseProfile.step(p) < 15) return "Only a Grand Magic Knight may stand for the crown.";
        int min = MultiverseConfig.get(MultiverseConfig.WK_MIN_STARS), stars = MultiverseProfile.netStars(p);
        if (stars < min) return "The crown needs at least " + min + " net stars (you have " + stars + ").";
        Registry r = Registry.get(server);
        if (r.holder != null && stars <= r.stars) return "The Wizard King, " + r.name + ", has " + r.stars + " net stars. Earn more.";
        appoint(server, p);
        return null;
    }

    /** The holder's star count follows them. */
    public static void onStars(ServerPlayer p) {
        if (!isKing(p)) return;
        Registry r = Registry.get(p.getServer());
        r.stars = MultiverseProfile.netStars(p);
        r.setDirty();
    }

    /** Every 10 s: the crown and the skill belong to exactly the holder (also after an offline change of holder). */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 200 != 77) return;
        boolean king = isKing(p);
        boolean hasSkill = SkillAPI.getSkillsFrom(p).getSkill(NUSkills.WIZARD_KING.getId()).isPresent();
        if (king && (!hasSkill || MultiverseProfile.rank(p) != KnightRank.WIZARD_KING)) crown(p);
        else if (!king && (hasSkill || MultiverseProfile.rank(p) == KnightRank.WIZARD_KING)) demote(p);
    }
}
