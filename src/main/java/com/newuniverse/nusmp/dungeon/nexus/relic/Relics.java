package com.newuniverse.nusmp.dungeon.nexus.relic;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.NexusZones;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The relics' face to the rest of the Dungeon (docs/living_dungeon.md section 7): the vault and zone code ask whether a player is anchored,
 * the immune system asks whether their mana is masked, and the builders place pedestals and mana gates. Everything else about the relics
 * happens by itself, through the events of {@link RelicEvents}.
 */
public final class Relics {
    private Relics() {}

    /** Does this player wear an attuned Band of the Spatial Anchor: immune to the Dungeon's spatial traps, free to place blocks in its volatile zones? */
    public static boolean anchored(Player p) { return p != null && Worn.has(p, Relic.BAND_OF_THE_SPATIAL_ANCHOR); }

    /** Does this player wear this relic, attuned? */
    public static boolean wears(Player p, Relic relic) { return p != null && Worn.has(p, relic); }

    /**
     * Does this player wear an attuned Amulet of the Juggernaut's Blindness (one of their two relic slots): is their mana signature masked, so that
     * the Dungeon's immune system does not answer the massive spells they cast? (The Sentient Dungeon's immune system asks this before it spawns a Juggernaut.)
     */
    public static boolean masksMana(Player p) { return p != null && Worn.has(p, Relic.AMULET_OF_THE_JUGGERNAUTS_BLINDNESS); }

    /**
     * Writes a relic pedestal holding {@code relic} at {@code at}. A pedestal already there whose relic has been taken is left alone, so a
     * resumed builder never gives a relic back. Taking a central relic posts the collapse event (see {@link ArtifactBlock}).
     */
    public static void placePedestal(ServerLevel level, BlockPos at, Relic relic, boolean central) {
        BlockState there = level.getBlockState(at);
        if (there.is(RelicBlocks.ARTIFACT_BLOCK.get()) && there.getValue(ArtifactBlock.TAKEN)) return;
        BlockState state = ((ArtifactBlock) RelicBlocks.ARTIFACT_BLOCK.get()).holding(relic, central);
        if (there == state) return;
        DungeonGuard.internal(() -> level.setBlock(at, state, 3));
    }

    /** Writes one block of the mana-locked gate (closed) at {@code at}. */
    public static void placeManaGate(ServerLevel level, BlockPos at) {
        if (level.getBlockState(at).is(RelicBlocks.MANA_GATE.get())) return;               // already there, open or shut: left as it is
        BlockState state = RelicBlocks.MANA_GATE.get().defaultBlockState();
        DungeonGuard.internal(() -> level.setBlock(at, state, 3));
    }

    /**
     * The zone a collapse event names for this column: the one the Nexus there was built as (a zone forced by an admin, or an old Golden City
     * adopted by the Nexus, is not what the hash of the cell says), else the hash. Never throws.
     */
    static NexusZone zoneAt(ServerLevel level, int x, int z) {
        try {
            return NexusZones.zoneAt(level, x, z);
        } catch (RuntimeException | LinkageError unavailable) {
            return NexusZone.at(x, z);
        }
    }

    /** Writes into the Nexus's record that its central relic was taken (the pedestal never fails because of it). */
    static void markCentreRelicTaken(ServerLevel level, BlockPos at) {
        try {
            NexusZones.markCentreRelicTaken(level, at);
        } catch (RuntimeException | LinkageError unavailable) {
            // the record is a convenience: the collapse itself does not depend on it
        }
    }
}
