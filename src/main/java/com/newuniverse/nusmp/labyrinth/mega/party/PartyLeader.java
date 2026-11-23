package com.newuniverse.nusmp.labyrinth.mega.party;

import com.newuniverse.nusmp.quests.QuestTeams;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

/**
 * Why: the owner's rule (5.0.0): the party LEADER decides how deep the party starts. The leader of a party is the owner of the FTB team (the same person the squads call the captain),
 * read through the soft {@link QuestTeams} bridge, so nothing here needs FTB Teams to be installed: without it, or without a team, there is no leader and every answer is "the
 * player's own". {@link #pick} is the whole rule and is pure; {@link #leaderOf} only finds the leader's online player. Server thread only; never throws.
 */
public final class PartyLeader {
    private PartyLeader() {}

    /** The leader's deepest-landing chain index when there is one to follow, else the player's own. {@code leaderLevel < 0} means "no leader to follow". */
    public static int pick(int own, int leaderLevel) { return leaderLevel < 0 ? own : leaderLevel; }

    /** The online leader of the player's FTB team when the player is not the leader themselves; null otherwise (no team, the player leads, the leader is offline, FTB Teams absent). */
    public static ServerPlayer leaderOf(ServerPlayer p) {
        try {
            MinecraftServer server = p.getServer();
            if (server == null || !PartyConfig.get(PartyConfig.USE_FTB_TEAMS)) return null;
            Optional<Object> team = QuestTeams.teamOf(p);
            if (team.isEmpty()) return null;
            UUID owner = QuestTeams.ownerOf(team.get());
            if (owner == null || owner.equals(p.getUUID()) || (owner.getMostSignificantBits() == 0L && owner.getLeastSignificantBits() == 0L)) return null;
            return server.getPlayerList().getPlayer(owner);
        } catch (RuntimeException | LinkageError soft) {
            return null;
        }
    }
}
