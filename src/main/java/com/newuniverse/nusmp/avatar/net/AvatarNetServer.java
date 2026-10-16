package com.newuniverse.nusmp.avatar.net;

import com.newuniverse.nusmp.avatar.AvatarConfig;
import com.newuniverse.nusmp.avatar.AvatarCreation;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import com.newuniverse.nusmp.avatar.appearance.DynamicAppearanceEngine;
import com.newuniverse.nusmp.avatar.menu.AvatarBuilderMenu;
import com.newuniverse.nusmp.avatar.race.AvatarRace;
import com.newuniverse.nusmp.avatar.race.AvatarRaces;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Why: every packet the creation screen sends is untrusted. These handlers (all on the server's main thread) apply the same three checks to each one:
 * the sender really has the creation screen open (or, for naming, is the creator standing next to the Avatar), they are not spamming, and every
 * value is cleaned (ids by whitelist, the look through {@code DynamicAppearanceEngine.sanitize}). They then hand over to AV1's {@code AvatarCreation},
 * which decides, and answer with the server's own words. The client never chooses a stat, a luck value or a price.
 */
public final class AvatarNetServer {
    private AvatarNetServer() {}

    static final RollLimiter ROLL_LIMIT = new RollLimiter(), NAME_LIMIT = new RollLimiter();

    private static boolean hasBuilderOpen(ServerPlayer p) { return p.containerMenu instanceof AvatarBuilderMenu; }

    private static void reply(ServerPlayer p, boolean ok, String kind, String msg) {
        PacketDistributor.sendToPlayer(p, new S2C_CreationResultPayload(ok, kind, NetText.clean(msg, 240)));
    }

    public static void onRoll(ServerPlayer p) {
        if (!AvatarConfig.enabled() || !hasBuilderOpen(p)) return;
        if (!ROLL_LIMIT.allow(p.getUUID(), System.currentTimeMillis(), AvatarNetConfig.rollGapMillis())) return;
        AvatarCreation.Roll r = AvatarCreation.rollStats(p);
        if (r == null || !r.ok()) { reply(p, false, "roll", "nusmp.avatar.gui.err.roll_refused"); return; }
        PacketDistributor.sendToPlayer(p, new S2C_RollPayload(r.stats(), r.fiveDice(), r.luck(), r.token(), AvatarConfig.rerollCostMagicule()));
    }

    public static void onFinalize(ServerPlayer p, C2S_FinalizeAvatarPayload m) {
        if (!AvatarConfig.enabled() || !hasBuilderOpen(p)) return;
        String raceId = NetText.clean(m.raceId(), NetText.ID_MAX);
        AvatarRace race = AvatarRaces.get(raceId);
        AvatarPersonality primary = AvatarPersonality.byName(NetText.clean(m.primary(), NetText.ID_MAX));
        AvatarPersonality secondary = AvatarPersonality.byName(NetText.clean(m.secondary(), NetText.ID_MAX));
        if (race == null) { reply(p, false, "create", "nusmp.avatar.gui.err.race"); return; }
        if (primary == null) { reply(p, false, "create", "nusmp.avatar.gui.err.primary"); return; }
        if (secondary == primary) secondary = null;
        AppearanceData look = DynamicAppearanceEngine.sanitize(AppearanceData.fromTag(m.appearance()), race);
        String name = NetText.clean(m.name(), NetText.NAME_MAX).strip();
        AvatarCreation.Result res = AvatarCreation.finalizeAvatar(p, new AvatarCreation.CreationRequest(raceId, primary, secondary, look.toTag(), name), m.token());
        if (res == null) { reply(p, false, "create", "nusmp.avatar.gui.err.create"); return; }
        reply(p, res.ok(), "create", res.message() == null ? "" : res.message());
        if (res.ok()) p.closeContainer();
    }

    public static void onName(ServerPlayer p, C2S_NameAvatarPayload m) {
        if (!AvatarConfig.enabled()) return;
        if (!NAME_LIMIT.allow(p.getUUID(), System.currentTimeMillis(), AvatarNetConfig.nameGapMillis())) return;
        Entity e = p.serverLevel().getEntity(m.avatarId());
        if (!(e instanceof AvatarEntity a) || !p.getUUID().equals(a.creatorUuid()) || a.distanceToSqr(p) > (double) AvatarNetConfig.nameReach() * AvatarNetConfig.nameReach()) {
            reply(p, false, "name", "nusmp.avatar.gui.err.name_target");
            return;
        }
        AvatarCreation.Result res = AvatarCreation.nameAvatar(p, m.avatarId(), NetText.clean(m.name(), NetText.NAME_MAX).strip());
        if (res == null) { reply(p, false, "name", "nusmp.avatar.gui.err.name"); return; }
        reply(p, res.ok(), "name", res.message() == null ? "" : res.message());
    }

    public static void onLogout(ServerPlayer p) {
        ROLL_LIMIT.forget(p.getUUID());
        NAME_LIMIT.forget(p.getUUID());
    }
}
