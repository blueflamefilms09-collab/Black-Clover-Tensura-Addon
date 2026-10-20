package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.rift.DimensionalRiftBlockEntity;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftConfig;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRules;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the bridge between the rift spawner (which has always decided WHETHER a rift opens near a player and whether it is a shortcut or a pocket) and the rarity table ({@link RiftTiers}, pure): for a
 * rift that is going to open, this rolls whether it is MEGA-RARE, with the chance moved by the depth of the floor, the player's Luck and their pity, picks the tomb it leads to (a weighted choice among
 * the tombs the floor is deep enough for, the same for the same seed: {@link PocketRules#pick}), aims the tear at it with the tomb's colours, and announces it: to the delvers near it like any rift, and
 * to the whole server if the config says so, because the canon makes these tombs a race between kingdoms. The pity counter lives in the player's persistent data and is copied across a death; it
 * moves only when a rift really opened, never when the roll found no spot, so a failed attempt costs nothing. A server with no tomb loaded (a datapack that removed them) simply never rolls mega.
 */
public final class MegaRifts {
    private MegaRifts() {}

    /** Persistent-data key of the number of rifts that opened near a player since their last mega-rare one. */
    public static final String PITY = "nusmp_rift_pity";

    /** The rarity knobs as the server configured them. */
    public static RiftTiers.Config config() {
        return new RiftTiers.Config(RiftConfig.get(RiftConfig.MEGA_ONE_IN), RiftConfig.get(RiftConfig.PITY_START), RiftConfig.get(RiftConfig.PITY_HARD), RiftConfig.get(RiftConfig.DEPTH_BOOST), RiftConfig.get(RiftConfig.LUCK_STEP));
    }

    public static int pity(ServerPlayer p) { return Math.max(0, p.getPersistentData().getInt(PITY)); }

    public static void setPity(ServerPlayer p, int v) { p.getPersistentData().putInt(PITY, Math.max(0, v)); }

    /** The chance (0..1) that the next rift to open near this player is mega-rare, for the admin's {@code rift tier}. */
    public static double chance(ServerPlayer p, int floor) {
        return RiftTiers.megaChance(config(), floor, p.getAttributeValue(Attributes.LUCK), pity(p));
    }

    /** The tomb a natural rift that is about to open for {@code p} on {@code floor} leads to, or null when it is an ordinary rift. Rolls once; changes nothing. */
    public static PocketDef rollNatural(ServerPlayer p, int floor, RandomSource rnd) {
        if (!RiftConfig.get(RiftConfig.MEGA_ENABLED) || PocketDefs.count() == 0) return null;
        int depth = Math.abs(floor);
        if (!RiftTiers.isMega(config(), depth, p.getAttributeValue(Attributes.LUCK), pity(p), rnd.nextDouble())) return null;
        return pickBySeed(rnd.nextLong(), depth);
    }

    /** The tomb for a seed on a floor depth (null when none is deep enough). */
    public static PocketDef pickBySeed(long seed, int depth) {
        List<PocketDef> all = PocketDefs.all();
        int i = PocketRules.pick(all, seed, depth);
        return i < 0 ? null : all.get(i);
    }

    /** An admin's choice: the tomb {@code id}, or (empty) a random one regardless of depth; null when the id is unknown or there are none. */
    public static PocketDef pickForced(String id, RandomSource rnd) {
        if (id == null || id.isEmpty()) {
            List<PocketDef> all = PocketDefs.all();
            return all.isEmpty() ? null : all.get(rnd.nextInt(all.size()));
        }
        return PocketDefs.get(id);
    }

    /** The pity after a rift opened for the player (reset by a mega-rare one). Called only after the rift really opened. */
    public static void opened(ServerPlayer p, boolean mega) {
        setPity(p, RiftTiers.nextPity(pity(p), mega ? RiftTier.MEGA_RARE : RiftTier.COMMON));
    }

    /** Makes an open pocket rift the tear of a tomb: its colours, its life. */
    public static void aim(DimensionalRiftBlockEntity be, PocketDef def) {
        be.aimExclusive(def.id(), def.accent(), def.sky(), RiftConfig.get(RiftConfig.MEGA_SECONDS) * 20);
    }

    /** Tells the delvers near a mega-rare tear, and (config) the whole server, that it opened. */
    public static void announce(ServerLevel level, BlockPos at, PocketDef def, ServerPlayer finder) {
        double r2 = (double) RiftRules.HINT_RADIUS * RiftRules.HINT_RADIUS;
        int floor = 0;
        if (finder != null) {
            try {
                var s = com.newuniverse.nusmp.labyrinth.mega.MegaData.get(level.getServer()).sectorAt(at);
                if (s != null) floor = s.firstFloor();
            } catch (RuntimeException ignored) {
                // the floor is only for the line of text
            }
        }
        List<ServerPlayer> near = new ArrayList<>();
        for (ServerPlayer q : level.players()) {
            double dx = at.getX() + 0.5 - q.getX(), dy = at.getY() - q.getY(), dz = at.getZ() + 0.5 - q.getZ();
            if (q.isSpectator() || dx * dx + dy * dy + dz * dz > r2) continue;
            near.add(q);
            q.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.rift.opened.mega", "The air SCREAMS open nearby: a tomb out of the old world, torn loose from its place.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), true);
            q.sendSystemMessage(Component.translatableWithFallback("message.nusmp.mega.rift.hint.mega", "[SYSTEM] A great rift opened %s blocks to the %s. It leads to %s. It will not stay open long.", (int) Math.round(Math.sqrt(dx * dx + dz * dz)),
                    RiftRules.compass(dx, dz), Component.translatableWithFallback("pocket.nusmp." + def.id() + ".name", def.name())).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        level.playSound(null, at, SoundEvents.END_PORTAL_SPAWN, SoundSource.AMBIENT, 2.5f, 0.5f);
        if (RiftConfig.get(RiftConfig.MEGA_BROADCAST) && level.getServer() != null) {
            Component line = Component.translatableWithFallback("message.nusmp.mega.rift.broadcast", "A great rift has torn open on floor %s of the dungeon. Every kingdom will want what is behind it.", FloorMap.label(floor)).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC);
            for (ServerPlayer q : level.getServer().getPlayerList().getPlayers()) if (!near.contains(q)) q.sendSystemMessage(line);
        }
        if (finder != null) NusmpEvents.fire(finder, "mega.rift", "mega");
    }
}
