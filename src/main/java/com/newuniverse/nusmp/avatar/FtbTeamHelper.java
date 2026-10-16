package com.newuniverse.nusmp.avatar;

import com.newuniverse.nusmp.multiverse.Squads;
import com.newuniverse.nusmp.quests.QuestTeams;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

/**
 * Why: a Yandere Avatar must never hurt its creator's FTB squadmates, and "who is on whose team" lives in FTB Teams, which this mod only reaches by
 * reflection. This helper does NOT repeat that reflection: it asks {@link QuestTeams} (team membership) and {@link Squads} (party members), both
 * already soft dependencies, and answers false for everything when FTB Teams is absent. Server side only.
 */
public final class FtbTeamHelper {
    private FtbTeamHelper() {}

    /** True when both are the same player, or share an FTB team / party. Offline players can only be answered through an online team mate. */
    public static boolean sameTeam(UUID a, UUID b) {
        if (a == null || b == null) return false;
        if (a.equals(b)) return true;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;
        ServerPlayer pa = server.getPlayerList().getPlayer(a), pb = server.getPlayerList().getPlayer(b);
        return in(pa, b) || in(pb, a);
    }

    private static boolean in(ServerPlayer p, UUID other) {
        if (p == null) return false;
        try {
            var team = QuestTeams.teamOf(p);
            if (team.isPresent() && QuestTeams.memberIds(team.get()).contains(other)) return true;
            return Squads.partyMembers(p).contains(other);
        } catch (RuntimeException | LinkageError soft) {
            return false;
        }
    }

    /**
     * Is this entity one the Avatar must treat as on its creator's side because of teams: a squadmate player, or a pet / Avatar owned by a
     * squadmate. (The creator and the creator's own pets and Avatars are handled by the goals themselves.)
     */
    public static boolean isSquadmate(AvatarEntity avatar, LivingEntity other) {
        UUID creator = avatar.creatorUuid();
        if (creator == null || other == null) return false;
        if (other instanceof ServerPlayer p) return sameTeam(creator, p.getUUID());
        if (other instanceof AvatarEntity av && av.creatorUuid() != null) return sameTeam(creator, av.creatorUuid());
        if (other instanceof OwnableEntity o && o.getOwnerUUID() != null) return sameTeam(creator, o.getOwnerUUID());
        return false;
    }
}
