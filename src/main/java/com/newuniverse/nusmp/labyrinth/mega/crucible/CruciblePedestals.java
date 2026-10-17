package com.newuniverse.nusmp.labyrinth.mega.crucible;

import com.newuniverse.nusmp.labyrinth.mega.Marker;
import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.vault.RelicPedestalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: "which pedestal is slot 2" and "who is standing in the vault" are asked by the offer, the pick, the catastrophes and the command, and they
 * must always get the same answer. The generator saves the pedestal blocks of a sector as a list ({@code Sector.pedestals}, from the PEDESTAL markers
 * of the vault template) but does not promise the list is in the markers' {@code slot} order, and a slot number is what the saved offer is indexed
 * by; so the order is rebuilt here from the saved markers (the list order breaks ties), which also keeps an old save readable. The vault box is
 * centred on the sector's {@code vaultCenter}, or, when the generator found no VAULT_CENTER marker, on the middle of the pedestals. A pedestal's
 * block entity is only ever looked up when its chunk is loaded: the vault ticker and the catastrophes run from event handlers, and the project
 * rule is that an event handler never loads a chunk.
 */
final class CruciblePedestals {
    private CruciblePedestals() {}

    /** The sector's pedestal blocks in slot order (the PEDESTAL markers' {@code slot} argument; the saved list order for ties and for a sector without markers). */
    static List<BlockPos> ordered(Sector s) {
        List<BlockPos> list = new ArrayList<>(s.pedestals);
        if (list.size() < 2) return list;
        Map<BlockPos, Integer> slotOf = new HashMap<>();
        for (Marker m : s.markers(MarkerKind.PEDESTAL)) slotOf.put(m.pos().below(), m.intArg("slot", Integer.MAX_VALUE));
        list.sort(Comparator.comparingInt(p -> slotOf.getOrDefault(p, Integer.MAX_VALUE)));        // List.sort is stable: unmarked pedestals keep the saved order
        return list;
    }

    /** The vault's standing-cell centre: the saved landmark, else the middle of the pedestals one above them; null when the sector has neither. */
    static BlockPos center(Sector s) {
        if (s.vaultCenter != null) return s.vaultCenter;
        if (s.pedestals.isEmpty()) return null;
        long x = 0, y = 0, z = 0;
        for (BlockPos p : s.pedestals) {
            x += p.getX();
            y += p.getY();
            z += p.getZ();
        }
        int n = s.pedestals.size();
        return new BlockPos((int) Math.floorDiv(x, (long) n), (int) Math.floorDiv(y, (long) n) + 1, (int) Math.floorDiv(z, (long) n));
    }

    /** Is the position inside the vault box of the sector? */
    static boolean inBox(Sector s, BlockPos p) { return inBox(center(s), p); }

    static boolean inBox(BlockPos center, BlockPos p) {
        return center != null && CrucibleRules.inVaultBox(p.getX() - center.getX(), p.getY() - center.getY(), p.getZ() - center.getZ());
    }

    /** The players (not spectators) inside the vault box of the sector. */
    static List<ServerPlayer> playersInBox(ServerLevel level, Sector s) { return playersInBox(level, center(s)); }

    static List<ServerPlayer> playersInBox(ServerLevel level, BlockPos center) {
        List<ServerPlayer> out = new ArrayList<>();
        if (center == null) return out;
        for (ServerPlayer p : level.players()) if (!p.isSpectator() && inBox(center, p.blockPosition())) out.add(p);
        return out;
    }

    /** The players the catastrophes hurt: inside the box, in survival or adventure (a creative player takes no damage and no effects). */
    static List<ServerPlayer> victimsInBox(ServerLevel level, BlockPos center) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : playersInBox(level, center)) if (!p.isCreative() && p.isAlive()) out.add(p);
        return out;
    }

    /** The pedestal block entity at the position, or null when it is not there or its chunk is not loaded (never loads a chunk). */
    static RelicPedestalBlockEntity entity(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof RelicPedestalBlockEntity be ? be : null;
    }
}
