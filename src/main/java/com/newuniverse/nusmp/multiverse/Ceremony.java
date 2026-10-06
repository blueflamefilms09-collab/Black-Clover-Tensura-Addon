package com.newuniverse.nusmp.multiverse;

import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.blackclover.GrimoireAcceptance;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.Kingdom;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.blackclover.NightmareSouls;
import com.newuniverse.nusmp.skill.NUSkills;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.time.LocalDate;

/**
 * The Grimoire Acceptance Ceremony. Once a year (the configured real-world month, March by default, or whenever an admin runs
 * /multiverse ceremony start) grimoires choose every eligible player who comes to a Grimoire Tower. A player who missed it accepts
 * later at any Grimoire Altar (the top floor of every tower).
 *
 * <p>Eligible = no grimoire yet (and, when everyoneEligible is off, flagged eligible by an admin). The grant is transactional:
 * the new grimoire item (with its own GrimoireId UUID) is built and handed over before anything is consumed.
 */
public final class Ceremony {
    private Ceremony() {}

    // ---------------------------------------------------------------- state
    /** Manual override: ON forces the ceremony, OFF stops this month's automatic one. Saved with the world. */
    static final class State extends SavedData {
        boolean forcedOn;
        String stoppedMonth = "";

        static State get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(State::new, State::load, null), "nusmp_ceremony");
        }

        private static State load(CompoundTag tag, HolderLookup.Provider provider) {
            State s = new State();
            s.forcedOn = tag.getBoolean("ForcedOn");
            s.stoppedMonth = tag.getString("StoppedMonth");
            return s;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            tag.putBoolean("ForcedOn", forcedOn);
            tag.putString("StoppedMonth", stoppedMonth);
            return tag;
        }
    }

    private static String thisMonth() { LocalDate d = LocalDate.now(); return d.getYear() + "-" + d.getMonthValue(); }

    public static boolean isActive(MinecraftServer server) {
        if (server == null) return false;
        State s = State.get(server);
        if (s.forcedOn) return true;
        if (!MultiverseConfig.get(MultiverseConfig.CEREMONY_REAL_DATE)) return false;
        return LocalDate.now().getMonthValue() == MultiverseConfig.get(MultiverseConfig.CEREMONY_MONTH) && !thisMonth().equals(s.stoppedMonth);
    }

    public static void start(MinecraftServer server) {
        State s = State.get(server);
        s.forcedOn = true;
        s.stoppedMonth = "";
        s.setDirty();
        server.getPlayerList().broadcastSystemMessage(Component.literal("The Grimoire Acceptance Ceremony has begun. Grimoires await at the Grimoire Towers.")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
    }

    public static void stop(MinecraftServer server) {
        State s = State.get(server);
        s.forcedOn = false;
        s.stoppedMonth = thisMonth();
        s.setDirty();
        server.getPlayerList().broadcastSystemMessage(Component.literal("The Grimoire Acceptance Ceremony has ended. Those who missed it may still pray at a Grimoire Altar.")
                .withStyle(ChatFormatting.GRAY), false);
    }

    public static boolean isEligible(ServerPlayer p) {
        if (GrimoirePages.grimoireOf(p).isPresent()) return false;
        return MultiverseConfig.get(MultiverseConfig.EVERYONE_ELIGIBLE) || MultiverseProfile.eligibleFlag(p);
    }

    // ---------------------------------------------------------------- the tower
    /** Every 5 s: during the ceremony, eligible players at a tower are chosen; outside it they are pointed to the altar. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 100 != 0 || !isEligible(p)) return;
        var site = WorldSites.get(p.getServer()).nearest(WorldSites.Kind.TOWER, p.level().dimension(), p.blockPosition());
        if (site.isEmpty()) return;
        int radius = MultiverseConfig.get(MultiverseConfig.CEREMONY_TOWER_RADIUS);
        if (site.get().pos().distSqr(p.blockPosition()) > (double) radius * radius) return;
        if (isActive(p.getServer())) choose(p, "the Grimoire Tower");
        else if (p.tickCount % 600 == 0)
            p.displayClientMessage(Component.literal("The tower is quiet. Pray at the altar on its top floor to be chosen.").withStyle(ChatFormatting.GOLD), true);
    }

    // ---------------------------------------------------------------- the grant
    /** Rarity ladder: 3-leaf -> 4-leaf / kingdom covers -> 5-leaf -> black magic / god-tier. */
    private enum Tier { COMMON, UNCOMMON, RARE, BLACK, GOD }

    private static Tier rollTier(ServerPlayer p) {
        double c = MultiverseConfig.get(MultiverseConfig.WEIGHT_COMMON), u = MultiverseConfig.get(MultiverseConfig.WEIGHT_UNCOMMON);
        double r = MultiverseConfig.get(MultiverseConfig.WEIGHT_RARE), b = MultiverseConfig.get(MultiverseConfig.WEIGHT_BLACK);
        double g = MultiverseConfig.get(MultiverseConfig.WEIGHT_GOD);
        double x = p.getRandom().nextDouble() * Math.max(1e-6, c + u + r + b + g);
        if (x < c) return Tier.COMMON;
        if (x < c + u) return Tier.UNCOMMON;
        if (x < c + u + r) return Tier.RARE;
        if (x < c + u + r + b) return Tier.BLACK;
        return Tier.GOD;
    }

    /** A grimoire chooses this player. Returns false if they already have one. */
    public static boolean choose(ServerPlayer p, String where) {
        if (GrimoirePages.grimoireOf(p).isPresent()) return false;
        Tier tier = rollTier(p);
        String soul = NightmareSouls.soulTypeOf(p);
        if (tier == Tier.BLACK) {
            GrimoireAcceptance.grantExact(p, com.newuniverse.nusmp.blackclover.GrimoireCover.BLACK_MAGIC, MagicType.ANTI_MAGIC, Devil.LIEBE);
        } else if (tier == Tier.GOD) {
            var pool = GrimoireAcceptance.starterPool(soul);
            GrimoireAcceptance.grantExact(p, com.newuniverse.nusmp.blackclover.GrimoireCover.GOD_TIER, pool.get(p.getRandom().nextInt(pool.size())), null);
        } else {
            GrimoireAcceptance.grant(p, tier == Tier.COMMON ? 3 : tier == Tier.UNCOMMON ? 4 : 5, soul == null ? "" : soul);
        }
        if (GrimoirePages.grimoireOf(p).isEmpty()) return false;       // the grant did not go through (another mod refused the skill)
        MultiverseProfile.setAccepted(p, true, where);
        GrimoireAcceptance.markRolled(p, true);
        Vec3 above = p.position().add(0, 3.2, 0);
        VfxSpawn.send(p.serverLevel(), VfxShape.MAGIC_CIRCLE, above, p.position(), 0xFFFFD86B, 50, 1.4f);
        VfxSpawn.send(p.serverLevel(), VfxShape.MAGIC_CIRCLE_EXPLOSION, p.position().add(0, 1, 0), p.position().add(0, 2, 0), 0xFFFFD86B, 30, 1.0f);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.2f, 0.7f);
        p.sendSystemMessage(Component.literal("A grimoire drifts down from the shelves of " + where + " and chooses you.").withStyle(ChatFormatting.GOLD));
        return true;
    }

    /** Admin reset: the grimoire (skill and bound items) is taken back and the player may be chosen again. */
    public static void reset(ServerPlayer p) {
        for (var id : NUSkills.allGrimoireSkillIds()) SkillAPI.getSkillsFrom(p).forgetSkill(id);
        var inv = p.getInventory();
        com.newuniverse.nusmp.blackclover.GrimoireSlot.replaceOwned(p, old -> ItemStack.EMPTY);
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            if (GrimoireItem.isOwnedBy(inv.getItem(slot), p.getUUID())) inv.setItem(slot, ItemStack.EMPTY);
        }
        GrimoireAcceptance.markRolled(p, false);
        MultiverseProfile.setAccepted(p, false, "");
        MultiverseProfile.setEligible(p, true);
    }
}
