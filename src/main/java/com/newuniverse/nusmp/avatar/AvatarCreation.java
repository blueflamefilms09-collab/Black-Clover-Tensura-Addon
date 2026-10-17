package com.newuniverse.nusmp.avatar;

import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import com.newuniverse.nusmp.avatar.appearance.DynamicAppearanceEngine;
import com.newuniverse.nusmp.avatar.aptitude.Aptitude;
import com.newuniverse.nusmp.avatar.aptitude.AptitudeCatalog;
import com.newuniverse.nusmp.avatar.aptitude.ForgeChoices;
import com.newuniverse.nusmp.avatar.aptitude.ForgeContext;
import com.newuniverse.nusmp.avatar.aptitude.ForgeCosts;
import com.newuniverse.nusmp.avatar.aptitude.ForgePlan;
import com.newuniverse.nusmp.avatar.aptitude.ForgeRules;
import com.newuniverse.nusmp.avatar.aptitude.ForgeSkill;
import com.newuniverse.nusmp.avatar.aptitude.ForgeValidator;
import com.newuniverse.nusmp.avatar.dnd.StatRoller;
import com.newuniverse.nusmp.avatar.item.GenesisCoreItem;
import com.newuniverse.nusmp.avatar.luck.LuckPoller;
import com.newuniverse.nusmp.avatar.naming.NameRules;
import com.newuniverse.nusmp.avatar.naming.TensuraNamingEngine;
import com.newuniverse.nusmp.avatar.outcome.AvatarUniqueSkill;
import com.newuniverse.nusmp.avatar.outcome.Outcome;
import com.newuniverse.nusmp.avatar.race.AvatarRace;
import com.newuniverse.nusmp.avatar.race.AvatarRaces;
import com.newuniverse.nusmp.avatar.race.RaceConnectionChecker;
import com.newuniverse.nusmp.avatar.ritual.ForgeReserves;
import com.newuniverse.nusmp.avatar.ritual.ForgeSkillResolver;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Why: this is the server-side door of the creation GUI, and the only place where an Avatar comes into being. The client only ever sends choices (a race id,
 * personalities, an appearance, a name); the scores, the luck and the costs are computed here. The roll is stored in a per-player {@link AvatarSession} the
 * client cannot touch, finalize reads the scores from it, and nothing else: so a modified client cannot pick its stats, skip the Genesis Core, ignore a race's
 * connection conditions or exceed the Avatar limit. Creation consumes the core only when it succeeds (opening the GUI never does).
 *
 * <p>1.58.0 (the Soul Forge): the same door also serves the new ritual screen. {@link #finalizeForge} runs EVERY check of the classic creation, then the
 * Soul Forge's own: it re-reads the creator's luck, magicule, EP, tax and cores from the game, lets the pure {@link ForgeValidator} turn their choices into a
 * {@link ForgePlan} (or a refusal), and only then spends the magicule, consumes the Genesis Core(s), applies the permanent tax, writes the aptitudes and chosen
 * skills onto the new Avatar, and rolls the Instant Skill Awakening (a d100 below 5 + the creator's luck, or a guaranteed one the creator paid for), which
 * secures a Special skill for the Avatar and lifts the three-day betrayal roll. {@link #precheckForge} does the same without side effects, so the ritual can
 * refuse a bad request before its spectacle starts and refuse again at the moment of birth. The classic {@link #finalizeAvatar} is unchanged in behaviour.
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

    /** Where a Soul Forge Avatar is born: a position and the way it faces. */
    public record Spot(double x, double y, double z, float yaw) {}

    /**
     * The Soul Forge half of a creation request: the creator's cleaned choices, the aptitude the server rolled for them (the token in the choices must match),
     * and where the Avatar should step out ({@code spot} null = beside the creator, the classic way).
     */
    public record ForgeInput(ForgeChoices choices, String rolledAptitude, long rolledToken, Spot spot) {
        public static ForgeInput none() { return new ForgeInput(ForgeChoices.NONE, "", 0L, null); }
    }

    /** The outcome of a Soul Forge creation. {@code avatar} and {@code plan} are set when {@code ok}; {@code awakened} says the Instant Skill Awakening happened. */
    public record ForgeResult(boolean ok, String message, AvatarEntity avatar, ForgePlan plan, boolean awakened) {
        static ForgeResult fail(String msg) { return new ForgeResult(false, msg, null, null, false); }
    }

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

    // ================================================================ the shared checks
    /** What a successful check found, ready for the commit. */
    private record Checked(AvatarRace race, AvatarPersonality first, AvatarPersonality second, int[] stats, double luck, String name) {}

    private record Check(Checked ok, String failure) {
        static Check bad(String msg) { return new Check(null, msg); }
    }

    /**
     * Every check of a creation that needs no side effect and no Soul Forge data: the system is on, the race exists and is selectable, the Avatar limit, a
     * Genesis Core, the race's connection conditions, the personalities, the roll (and its token) and the optional name. The messages and their order are the
     * 1.51.0 ones; {@code token} 0 accepts any roll of the session.
     */
    private static Check check(ServerPlayer p, CreationRequest req, long token) {
        if (!AvatarConfig.enabled()) return Check.bad("The Avatar system is disabled on this server.");
        if (req == null || req.raceId() == null) return Check.bad("No race chosen.");
        AvatarRace race = AvatarRaces.get(req.raceId());
        if (race == null || !race.selectable()) return Check.bad("That race is not available.");
        if (!(p.level() instanceof ServerLevel)) return Check.bad("Not on the server.");
        AvatarManager mgr = AvatarManager.get(p.getServer());
        if (mgr.count(p.getUUID()) >= AvatarConfig.maxAvatarsPerPlayer()) return Check.bad("You already have " + AvatarConfig.maxAvatarsPerPlayer() + " Avatar(s).");
        if (!GenesisCoreItem.hasCore(p)) return Check.bad("You need a Genesis Core.");

        RaceConnectionChecker.Result conn = RaceConnectionChecker.check(p, race);
        if (!conn.ok()) return Check.bad("You do not meet the requirements of " + race.displayName() + ": " + String.join(", ", conn.failed()));

        AvatarPersonality first = req.primary(), second = req.secondary();
        if (first == null) return Check.bad("Choose a primary personality.");
        if (second == first) second = null;
        if (!race.allows(first) || !race.allows(second)) return Check.bad(race.displayName() + " cannot have that personality.");

        AvatarSession s = p.getData(AvatarSystem.SESSION);
        int[] stats;
        double luck;
        if (race.requiresDndRoll()) {
            if (!s.active || s.stats.length != 6) return Check.bad("Roll your stats first.");
            if (token != 0L && token != s.token) return Check.bad("That roll is out of date: roll again.");
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
            if (v != NameRules.Verdict.OK) return Check.bad(NameRules.message(v));
        }
        return new Check(new Checked(race, first, second, stats, luck, name), null);
    }

    private static AppearanceData look(ServerPlayer p, AvatarRace race, CreationRequest req) {
        AppearanceData look = req.appearance() == null || req.appearance().isEmpty()
                ? AppearanceData.defaults(race.id(), race.defaultScale(), p.getRandom().nextLong())
                : AppearanceData.fromTag(req.appearance());
        return DynamicAppearanceEngine.sanitize(look, race);
    }

    // ================================================================ finalize (the classic screen, the admin tools)
    public static Result finalizeAvatar(ServerPlayer p, CreationRequest req) { return finalizeAvatar(p, req, 0L); }

    /** As above, and when {@code token} is not zero it must be the token of the roll the server made (a stale GUI is refused). */
    public static Result finalizeAvatar(ServerPlayer p, CreationRequest req, long token) {
        Check c = check(p, req, token);
        if (c.ok() == null) return fail(c.failure());
        Checked k = c.ok();
        boolean creative = p.isCreative();
        if (!creative && !TensuraBridge.spendMagicule(p, k.race().baseMagiculeCost())) return fail("You need " + (long) k.race().baseMagiculeCost() + " magicules to make a " + k.race().displayName() + ".");
        AppearanceData look = look(p, k.race(), req);
        AvatarEntity a = spawn(p, (ServerLevel) p.level(), k.race(), k.stats(), k.luck(), k.first(), k.second(), look, k.name());
        if (a == null) {
            // roll back what was spent
            if (!creative) TensuraBridge.refundMagicule(p, k.race().baseMagiculeCost());
            return fail("There is no room for the Avatar here.");
        }
        if (!creative) GenesisCoreItem.consume(p);
        p.getData(AvatarSystem.SESSION).clear();
        p.sendSystemMessage(Component.literal("Your Avatar stands before you: a " + k.race().displayName() + " (" + k.first().displayName + (k.second() != null ? " / " + k.second().displayName : "")
                + "). In three days its fate is decided, unless you name it.").withStyle(ChatFormatting.GOLD));
        NusmpEvents.fire(p, "avatar.created", k.race().id());
        return new Result(true, a.getUUID().toString());
    }

    // ================================================================ finalize (the Soul Forge)
    /**
     * Everything {@link #finalizeForge} would check, with no side effect: the classic checks, then the Soul Forge's own against FRESH reserves. A refusal carries
     * the sentence to show the player. Used by the ritual to turn a bad Finalize away before its spectacle starts.
     */
    public static Result precheckForge(ServerPlayer p, CreationRequest req, long statToken, ForgeInput in) {
        Check c = check(p, req, statToken);
        if (c.ok() == null) return fail(c.failure());
        ForgeValidator.Verdict v = judge(p, c.ok().race(), in);
        return v.ok() ? new Result(true, "") : fail(v.refusal().text());
    }

    private static ForgeValidator.Verdict judge(ServerPlayer p, AvatarRace race, ForgeInput in) {
        ForgeContext ctx = ForgeReserves.read(p, in.rolledAptitude(), in.rolledToken(), race.baseMagiculeCost());
        return ForgeValidator.validate(in.choices(), ctx, AvatarSoulForgeConfig.rules(), AptitudeCatalog::byId, ForgeSkillResolver::lookup);
    }

    /**
     * Creates an Avatar from the Soul Forge. Validates everything again (the ritual's spectacle took a second or two, and the world is not frozen), then in
     * this order: spends the magicule (the race's price plus what the aptitudes, skills and awakening cost up front), builds the Avatar with its aptitudes,
     * chosen skills, permanent tax and awakening, adds it to the world, takes the Genesis Core(s), clears the roll, tells the creator and fires the quest keys.
     * Nothing is taken from the creator unless the Avatar was placed.
     */
    public static ForgeResult finalizeForge(ServerPlayer p, CreationRequest req, long statToken, ForgeInput in) {
        Check c = check(p, req, statToken);
        if (c.ok() == null) return ForgeResult.fail(c.failure());
        Checked k = c.ok();
        ForgeValidator.Verdict v = judge(p, k.race(), in);
        if (!v.ok()) return ForgeResult.fail(v.refusal().text());
        ForgePlan plan = v.plan();
        boolean creative = p.isCreative();
        double price = k.race().baseMagiculeCost() + plan.upfrontMagicule();
        if (!creative && !TensuraBridge.spendMagicule(p, price)) return ForgeResult.fail("You need " + Math.round(price) + " magicules to bind this Avatar.");
        AppearanceData look = look(p, k.race(), req);
        ForgeRules rules = AvatarSoulForgeConfig.rules();
        boolean[] awakened = {false};
        AvatarEntity a = spawn(p, (ServerLevel) p.level(), k.race(), k.stats(), k.luck(), k.first(), k.second(), look, k.name(), in.spot(), e -> awakened[0] = applyForge(p, e, plan, rules));
        if (a == null) {
            if (!creative) TensuraBridge.refundMagicule(p, price);
            return ForgeResult.fail("There is no room for the Avatar here.");
        }
        GenesisCoreItem.consume(p, plan.coresNeeded());
        p.getData(AvatarSystem.SESSION).clear();
        TensuraNamingEngine.refreshTax(p);
        p.sendSystemMessage(Component.literal("Your Avatar stands before you: a " + k.race().displayName() + " (" + k.first().displayName + (k.second() != null ? " / " + k.second().displayName : "")
                + "). " + (a.hasUniqueOrUltimate() ? "It holds a Special skill: the three-day fate no longer threatens it." : "In three days its fate is decided, unless you name it.")).withStyle(ChatFormatting.GOLD));
        String gifts = describe(plan, awakened[0]);
        if (!gifts.isEmpty()) p.sendSystemMessage(Component.literal(gifts).withStyle(ChatFormatting.LIGHT_PURPLE));
        NusmpEvents.fire(p, "avatar.created", k.race().id());
        for (String apt : plan.aptitudes()) NusmpEvents.fire(p, "avatar.aptitude", apt);
        if (!plan.unique().isEmpty()) NusmpEvents.fire(p, "avatar.skill_bound", plan.unique());
        if (!plan.ultimate().isEmpty()) NusmpEvents.fire(p, "avatar.skill_bound", plan.ultimate());
        if (awakened[0]) NusmpEvents.fire(p, "avatar.awakened_instant", plan.paidAwakening() ? "paid" : "luck");
        return new ForgeResult(true, a.getUUID().toString(), a, plan, awakened[0]);
    }

    /**
     * Writes a validated plan onto a fresh Avatar (before it is added to the world): the aptitudes, the chosen skills, the permanent tax, and the Instant Skill
     * Awakening. The awakening is a d100 strictly below 5 + the creator's luck (read now, at spawn), or certain when the creator paid for it; it gives the Avatar a
     * generated Unique perk. Holding any Special skill (the awakening, or a chosen Unique / Ultimate one while {@code chosenSkillsSecure} is on) sets the
     * Unique-or-Ultimate flag, which makes the outcome engine skip the three-day betrayal roll. Returns whether the awakening happened.
     */
    private static boolean applyForge(ServerPlayer p, AvatarEntity a, ForgePlan plan, ForgeRules rules) {
        a.setMagicAptitudes(plan.aptitudes());
        a.setChosenSkills(plan.unique(), plan.ultimate());
        a.setPermanentTax(plan.permanentTax());
        boolean awakened = plan.paidAwakening() || ForgeCosts.awakens(rules, LuckPoller.total(p), p.getRandom().nextInt(100) + 1);
        if (awakened) a.setPerk(AvatarUniqueSkill.generate(a.seeded()));
        boolean secured = awakened || (rules.chosenSkillsSecure() && plan.hasChosenSkill());
        if (secured) {
            a.setHasUniqueOrUltimate(true);
            a.setOutcomeState(Outcome.SKIPPED_UNIQUE);
            a.setOutcomeTimer(-1);
        }
        if (awakened) a.rebuildGoals();            // the perk adds its signature goal
        return awakened;
    }

    /** One chat line about what was bound (empty when nothing was). */
    private static String describe(ForgePlan plan, boolean awakened) {
        List<String> parts = new ArrayList<>();
        if (!plan.aptitudes().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (String id : plan.aptitudes()) {
                Aptitude a = AptitudeCatalog.byId(id);
                names.add((a == null ? id : a.name()) + (id.equals(plan.rolledAptitude()) ? " (rolled)" : ""));
            }
            parts.add("Aptitudes: " + String.join(", ", names));
        }
        if (!plan.unique().isEmpty() || !plan.ultimate().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (String id : new String[]{plan.unique(), plan.ultimate()}) {
                if (id.isEmpty()) continue;
                ForgeSkill s = ForgeSkillResolver.lookup(id);
                names.add(s == null ? id : s.name());
            }
            parts.add("Skills: " + String.join(", ", names));
        }
        if (awakened) parts.add(plan.paidAwakening() ? "Awakening: guaranteed" : "Awakening: the dice favoured you");
        if (plan.permanentTax() > 1.0e-9) parts.add("Permanent tax: " + Math.round(plan.permanentTax() * 1000.0) / 10.0 + "% of your maximum magicule");
        return String.join(". ", parts) + (parts.isEmpty() ? "" : ".");
    }

    /**
     * Builds the entity beside the creator and adds it to the world; null when it could not be added. No checks and no costs: the callers (finalize and
     * the admin command) do those. Also registers it with the creator's data and the manager.
     */
    public static AvatarEntity spawn(ServerPlayer p, ServerLevel level, AvatarRace race, int[] stats, double luck, AvatarPersonality first, AvatarPersonality second,
                                     AppearanceData look, String name) {
        return spawn(p, level, race, stats, luck, first, second, look, name, null, null);
    }

    /** As above with a chosen birth spot (null = beside the creator) and a hook that runs on the new Avatar before it is added to the world (the Soul Forge's gifts). */
    public static AvatarEntity spawn(ServerPlayer p, ServerLevel level, AvatarRace race, int[] stats, double luck, AvatarPersonality first, AvatarPersonality second,
                                     AppearanceData look, String name, Spot spot, Consumer<AvatarEntity> beforeAdd) {
        AvatarEntity a = AvatarEntities.AVATAR.get().create(level);
        if (a == null) return null;
        if (spot != null) {
            a.moveTo(spot.x(), spot.y(), spot.z(), spot.yaw(), 0.0f);
        } else {
            Vec3 at = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(1.6));
            a.moveTo(at.x, p.getY(), at.z, p.getYRot() + 180.0f, 0.0f);
        }
        a.initFromCreation(p, race, stats, luck, first, second, look, name);
        if (beforeAdd != null) beforeAdd.accept(a);
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
