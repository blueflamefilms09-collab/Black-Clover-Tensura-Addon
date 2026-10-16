package com.newuniverse.nusmp.avatar;

import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import com.newuniverse.nusmp.avatar.appearance.DynamicAppearanceEngine;
import com.newuniverse.nusmp.avatar.dnd.StatRoller;
import com.newuniverse.nusmp.avatar.item.GenesisCoreItem;
import com.newuniverse.nusmp.avatar.luck.LuckPoller;
import com.newuniverse.nusmp.avatar.naming.NameRules;
import com.newuniverse.nusmp.avatar.naming.TensuraNamingEngine;
import com.newuniverse.nusmp.avatar.race.AvatarRace;
import com.newuniverse.nusmp.avatar.race.AvatarRaces;
import com.newuniverse.nusmp.avatar.race.RaceConnectionChecker;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Why: this is the server-side door of the creation GUI, and the only place where an Avatar comes into being. The client only ever sends choices (a race id,
 * personalities, an appearance, a name); the scores, the luck and the costs are computed here. The roll is stored in a per-player {@link AvatarSession} the
 * client cannot touch, finalize reads the scores from it, and nothing else: so a modified client cannot pick its stats, skip the Genesis Core, ignore a race's
 * connection conditions or exceed the Avatar limit. Creation consumes the core only when it succeeds (opening the GUI never does).
 */
public final class AvatarCreation {
    private AvatarCreation() {}

    /** What the client asks for. The appearance is untrusted: it is sanitised against the race before use. {@code secondary} and {@code name} may be null. */
    public record CreationRequest(String raceId, AvatarPersonality primary, AvatarPersonality secondary, CompoundTag appearance, String name) {}

    /** A roll the server made. {@code stats.length == 0} means the roll was refused (see {@link #refused()}). */
    public record Roll(int[] stats, boolean fiveDice, double luck, long token) {
        public boolean ok() { return stats != null && stats.length == 6; }
        public static Roll refused() { return new Roll(new int[0], false, 0.0, 0L); }
    }

    public record Result(boolean ok, String message) {}

    // ================================================================ roll
    /** Rolls (or rerolls) the six scores for this player. A refused roll (system off, no Genesis Core, reroll limit, cannot pay) is {@link Roll#refused()}. */
    public static Roll rollStats(ServerPlayer p) {
        if (!AvatarConfig.enabled() || !GenesisCoreItem.hasCore(p)) return Roll.refused();
        AvatarSession s = p.getData(AvatarSystem.SESSION);
        if (s.active) {
            if (s.rerolls >= AvatarConfig.maxRerolls()) return Roll.refused();
            if (!p.isCreative() && !TensuraBridge.spendMagicule(p, AvatarConfig.rerollCostMagicule())) return Roll.refused();
            s.rerolls++;
        }
        double luck = LuckPoller.total(p);
        boolean five = StatRoller.usesFiveDice(luck, AvatarConfig.luckBonusThreshold());
        Random rng = new Random(p.getRandom().nextLong() ^ System.nanoTime());
        int[] stats = StatRoller.roll(rng, five);
        long token = rng.nextLong() | 1L;
        s.active = true;
        s.stats = stats;
        s.fiveDice = five;
        s.luck = luck;
        s.token = token;
        return new Roll(stats.clone(), five, luck, token);
    }

    // ================================================================ finalize
    public static Result finalizeAvatar(ServerPlayer p, CreationRequest req) { return finalizeAvatar(p, req, 0L); }

    /** As above, and when {@code token} is not zero it must be the token of the roll the server made (a stale GUI is refused). */
    public static Result finalizeAvatar(ServerPlayer p, CreationRequest req, long token) {
        if (!AvatarConfig.enabled()) return fail("The Avatar system is disabled on this server.");
        if (req == null || req.raceId() == null) return fail("No race chosen.");
        AvatarRace race = AvatarRaces.get(req.raceId());
        if (race == null || !race.selectable()) return fail("That race is not available.");
        if (!(p.level() instanceof ServerLevel level)) return fail("Not on the server.");
        AvatarManager mgr = AvatarManager.get(p.getServer());
        if (mgr.count(p.getUUID()) >= AvatarConfig.maxAvatarsPerPlayer()) return fail("You already have " + AvatarConfig.maxAvatarsPerPlayer() + " Avatar(s).");
        boolean creative = p.isCreative();
        if (!GenesisCoreItem.hasCore(p)) return fail("You need a Genesis Core.");

        RaceConnectionChecker.Result conn = RaceConnectionChecker.check(p, race);
        if (!conn.ok()) return fail("You do not meet the requirements of " + race.displayName() + ": " + String.join(", ", conn.failed()));

        AvatarPersonality first = req.primary(), second = req.secondary();
        if (first == null) return fail("Choose a primary personality.");
        if (second == first) second = null;
        if (!race.allows(first) || !race.allows(second)) return fail(race.displayName() + " cannot have that personality.");

        AvatarSession s = p.getData(AvatarSystem.SESSION);
        int[] stats;
        double luck;
        if (race.requiresDndRoll()) {
            if (!s.active || s.stats.length != 6) return fail("Roll your stats first.");
            if (token != 0L && token != s.token) return fail("That roll is out of date: roll again.");
            stats = s.stats.clone();
            luck = s.luck;
        } else {
            stats = new int[6];
            Arrays.fill(stats, 12);
            luck = LuckPoller.total(p);
        }

        String name = NameRules.clean(req.name());
        if (!name.isEmpty()) {
            NameRules.Verdict v = NameRules.check(name, mgr.names(p.getUUID()));
            if (v != NameRules.Verdict.OK) return fail(NameRules.message(v));
        }
        if (!creative && !TensuraBridge.spendMagicule(p, race.baseMagiculeCost())) return fail("You need " + (long) race.baseMagiculeCost() + " magicules to make a " + race.displayName() + ".");

        AppearanceData look = req.appearance() == null || req.appearance().isEmpty()
                ? AppearanceData.defaults(race.id(), race.defaultScale(), p.getRandom().nextLong())
                : AppearanceData.fromTag(req.appearance());
        look = DynamicAppearanceEngine.sanitize(look, race);

        AvatarEntity a = spawn(p, level, race, stats, luck, first, second, look, name);
        if (a == null) {
            // roll back what was spent
            if (!creative) TensuraBridge.refundMagicule(p, race.baseMagiculeCost());
            return fail("There is no room for the Avatar here.");
        }
        if (!creative) GenesisCoreItem.consume(p);
        s.clear();
        p.sendSystemMessage(Component.literal("Your Avatar stands before you: a " + race.displayName() + " (" + first.displayName + (second != null ? " / " + second.displayName : "") + "). In three days its fate is decided, unless you name it.")
                .withStyle(ChatFormatting.GOLD));
        NusmpEvents.fire(p, "avatar.created", race.id());
        return new Result(true, a.getUUID().toString());
    }

    /**
     * Builds the entity beside the creator and adds it to the world; null when it could not be added. No checks and no costs: the callers (finalize and
     * the admin command) do those. Also registers it with the creator's data and the manager.
     */
    public static AvatarEntity spawn(ServerPlayer p, ServerLevel level, AvatarRace race, int[] stats, double luck, AvatarPersonality first, AvatarPersonality second,
                                     AppearanceData look, String name) {
        AvatarEntity a = AvatarEntities.AVATAR.get().create(level);
        if (a == null) return null;
        Vec3 at = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(1.6));
        a.moveTo(at.x, p.getY(), at.z, p.getYRot() + 180.0f, 0.0f);
        a.initFromCreation(p, race, stats, luck, first, second, look, name);
        if (!level.addFreshEntity(a)) return null;
        p.setData(AvatarSystem.PLAYER_DATA, p.getData(AvatarSystem.PLAYER_DATA).withAvatar(a.getUUID()).withCreated());
        AvatarManager.get(p.getServer()).update(a);
        return a;
    }

    // ================================================================ naming
    public static Result nameAvatar(ServerPlayer p, UUID avatarId, String name) {
        Entity found = null;
        for (ServerLevel l : p.getServer().getAllLevels()) {
            found = l.getEntity(avatarId);
            if (found != null) break;
        }
        if (!(found instanceof AvatarEntity a)) return fail("That Avatar is not here.");
        return TensuraNamingEngine.name(p, a, name);
    }

    /** The names a creator already uses (for the GUI's duplicate check). */
    public static List<String> namesOf(ServerPlayer p) { return AvatarManager.get(p.getServer()).names(p.getUUID()); }

    private static Result fail(String msg) { return new Result(false, msg); }
}
