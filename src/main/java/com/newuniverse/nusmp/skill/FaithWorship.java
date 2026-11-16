package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.FaithRules;
import com.newuniverse.nusmp.skill.rules.FaithWorshipRules;
import com.newuniverse.nusmp.skill.rules.FaithWorshipRules.Worshipper;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the reference skill (Tensura More Skills, Mitama Faith) is built on worshippers, offerings and a chosen gift; this is that model for Faith (docs/faith_fix.md). A Faith holder invites a player they
 * look at (Recruit mode); the player accepts with {@code /faith accept <god>}; from then on the worshipper can make an offering ({@code /faith offering}: magicules become Faith for the god) and the god can
 * choose which skill to bestow from a clickable list (Gift mode, {@code /faith gift}). Every number lives in {@link FaithWorshipRules}. Everything is decided here, on the server: the commands are the only
 * "packets", each checks its sender, its arguments, the range and the cooldown again, and nothing here ticks. State: the worshippers are saved on the god's skill tag ({@code Worshippers}, by player UUID,
 * carried over when Faith evolves); the open invitations and the offering clocks are in two small maps keyed by player UUID, bounded, cleared on logout and server stop.
 */
public final class FaithWorship {
    private FaithWorship() {}

    /** The tag key of the saved worshipper list on the god's Faith skill. */
    public static final String WORSHIPPERS_KEY = "Worshippers";
    /** Slack (blocks) between the range a menu was opened at and the range its click is checked at: the player may have stepped. */
    static final double CLICK_SLACK = 4.0;
    /** The tag key of the game tick the last slow regeneration was settled at. */
    static final String STAMP_KEY = "WorshipStamp";
    static final int MAX_INVITES = 256;
    static final int MAX_CLOCKS = 512;
    private static final int STATUS_LINES = 20;

    private record Invite(UUID target, long created) {}

    /** One open invitation per god (god UUID -> who was asked). */
    private static final Map<UUID, Invite> INVITES = new ConcurrentHashMap<>();
    /** The game tick of each worshipper's last offering (worshipper UUID -> tick). */
    private static final Map<UUID, Long> OFFER_LAST = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------ saved state
    /** The skill instance holding Faith or Goddess of Faith, or null. */
    public static ManasSkillInstance faithOf(ServerPlayer p) {
        if (p == null) return null;
        Skills skills = SkillAPI.getSkillsFrom(p);
        if (skills == null) return null;
        var faith = skills.getSkill(NUSkills.FAITH.get().getRegistryName());
        if (faith.isEmpty()) faith = skills.getSkill(NUSkills.GODDESS_OF_FAITH.get().getRegistryName());
        return faith.orElse(null);
    }

    public static List<Worshipper> worshippers(ManasSkillInstance instance) {
        List<Worshipper> out = new ArrayList<>();
        if (instance == null) return out;
        ListTag list = instance.getOrCreateTag().getList(WORSHIPPERS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && out.size() < FaithWorshipRules.MAX_CAP; i++) {
            CompoundTag c = list.getCompound(i);
            if (!c.hasUUID("Id")) continue;
            out.add(new Worshipper(c.getUUID("Id"), FaithWorshipRules.shortName(c.getString("Name")),
                    Math.max(0, Math.min(FaithWorshipRules.MAX_WORSHIP, c.getInt("Worship")))));
        }
        return out;
    }

    public static void save(ManasSkillInstance instance, List<Worshipper> list) {
        ListTag out = new ListTag();
        for (Worshipper w : list) {
            CompoundTag c = new CompoundTag();
            c.putUUID("Id", w.id());
            c.putString("Name", w.name());
            c.putInt("Worship", w.worship());
            out.add(c);
        }
        instance.getOrCreateTag().put(WORSHIPPERS_KEY, out);
        instance.markDirty();
    }

    /** The online god this player worships (the first online Faith holder whose list has them), or null. */
    static ServerPlayer findGod(ServerPlayer follower) {
        var server = follower.getServer();
        if (server == null) return null;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p == follower) continue;
            ManasSkillInstance inst = faithOf(p);
            if (inst != null && FaithWorshipRules.isWorshipper(worshippers(inst), follower.getUUID())) return p;
        }
        return null;
    }

    /**
     * The slow regeneration: worshippers give their god Faith in proportion to how long has passed, settled here when the god uses the skill (no tick work). Without worshippers the clock only
     * moves forward, so nothing is banked. At most {@link FaithWorshipRules#MAX_TRICKLE_PERIODS} periods are paid at once.
     */
    static void settle(ServerPlayer god, ManasSkillInstance inst) {
        int period = NUConfig.faithTrickleTicks();
        if (period <= 0 || !NUConfig.faithWorship()) return;
        long now = god.level().getGameTime();
        CompoundTag tag = inst.getOrCreateTag();
        long stamp = tag.contains(STAMP_KEY) ? Math.min(now, tag.getLong(STAMP_KEY)) : now;
        int count = worshippers(inst).size();
        int periods = count == 0 ? 0 : FaithWorshipRules.periods(now - stamp, period);
        long next = count == 0 || periods >= FaithWorshipRules.MAX_TRICKLE_PERIODS ? now : stamp + (long) periods * period;
        if (periods <= 0 && tag.contains(STAMP_KEY) && count > 0) return;      // nothing due yet: leave the clock alone
        tag.putLong(STAMP_KEY, next);
        if (periods > 0) FaithSkill.setFaith(inst, FaithRules.add(FaithSkill.getFaith(inst), FaithWorshipRules.trickleGain(periods, count)));
        else inst.markDirty();
    }

    // ------------------------------------------------------------------ messages
    private static boolean off(ServerPlayer p) {
        if (NUConfig.faithWorship()) return false;
        SkillUtil.fail(p, SkillMsg.t("faith.worship_off", "Worship is switched off on this server."));
        return true;
    }

    private static void tell(ServerPlayer p, Component msg) { if (p != null) p.sendSystemMessage(msg.copy().withStyle(ChatFormatting.GOLD)); }

    private static MutableComponent button(Component label, String command, ChatFormatting color) {
        return label.copy().withStyle(Style.EMPTY.withColor(color).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

    // ------------------------------------------------------------------ invitations
    /** The god's Recruit mode, after its own checks: asks {@code target} to worship. */
    static void invite(ServerPlayer god, ServerPlayer target) {
        long now = god.level().getGameTime();
        if (INVITES.size() >= MAX_INVITES) {
            int ttl = NUConfig.faithInviteTicks();
            INVITES.values().removeIf(i -> !FaithWorshipRules.inviteLive(i.created(), now, ttl));
            Iterator<UUID> it = INVITES.keySet().iterator();
            if (INVITES.size() >= MAX_INVITES && it.hasNext()) { it.next(); it.remove(); }
        }
        INVITES.put(god.getUUID(), new Invite(target.getUUID(), now));
        String name = god.getName().getString();
        tell(god, SkillMsg.t("faith.invited", "You invite %s to worship you.", target.getName()));
        MutableComponent line = SkillMsg.t("faith.invite", "%s invites you to worship them. ", god.getName()).withStyle(ChatFormatting.GOLD);
        line.append(button(SkillMsg.t("faith.btn_accept", "[Accept]"), "/faith accept " + name, ChatFormatting.GREEN));
        line.append(Component.literal(" "));
        line.append(button(SkillMsg.t("faith.btn_decline", "[Decline]"), "/faith reject " + name, ChatFormatting.RED));
        target.sendSystemMessage(line);
    }

    /** {@code /faith accept <god>}: the invited player becomes the god's worshipper. Returns true when it worked. */
    public static boolean accept(ServerPlayer follower, ServerPlayer god) {
        if (off(follower)) return false;
        Invite inv = INVITES.get(god.getUUID());
        long now = follower.level().getGameTime();
        if (inv == null || !inv.target().equals(follower.getUUID()) || !FaithWorshipRules.inviteLive(inv.created(), now, NUConfig.faithInviteTicks())) {
            SkillUtil.fail(follower, SkillMsg.t("faith.no_invite", "%s has no open invitation for you.", god.getName()));
            return false;
        }
        INVITES.remove(god.getUUID(), inv);
        ManasSkillInstance inst = faithOf(god);
        if (inst == null) {
            SkillUtil.fail(follower, SkillMsg.t("faith.not_a_god", "%s no longer holds Faith.", god.getName()));
            return false;
        }
        List<Worshipper> list = worshippers(inst);
        FaithWorshipRules.Recruit r = FaithWorshipRules.canRecruit(list, god.getUUID(), follower.getUUID(), NUConfig.faithMaxWorshippers());
        if (r == FaithWorshipRules.Recruit.FULL) {
            SkillUtil.fail(follower, SkillMsg.t("faith.full", "%s has no room for another worshipper.", god.getName()));
            return false;
        }
        if (r != FaithWorshipRules.Recruit.OK) {
            SkillUtil.fail(follower, SkillMsg.t("faith.already", "You already worship %s.", god.getName()));
            return false;
        }
        leaveEveryoneElse(follower, god);
        save(inst, FaithWorshipRules.add(list, god.getUUID(), follower.getUUID(), follower.getName().getString(), NUConfig.faithMaxWorshippers()));
        tell(follower, SkillMsg.t("faith.joined", "You now worship %s. Make an offering with /faith offering.", god.getName()));
        tell(god, SkillMsg.t("faith.joined_god", "%s now worships you.", follower.getName()));
        SkillAnimeFx.sigil(follower.serverLevel(), SkillAnimeFx.FAITH, follower.position(), 1, 0xFFFFE08A, 30);
        return true;
    }

    /** {@code /faith reject <god>}: declines an open invitation. */
    public static boolean reject(ServerPlayer follower, ServerPlayer god) {
        Invite inv = INVITES.get(god.getUUID());
        if (inv == null || !inv.target().equals(follower.getUUID())) {
            SkillUtil.fail(follower, SkillMsg.t("faith.no_invite", "%s has no open invitation for you.", god.getName()));
            return false;
        }
        INVITES.remove(god.getUUID(), inv);
        tell(follower, SkillMsg.t("faith.declined", "You decline the invitation of %s.", god.getName()));
        tell(god, SkillMsg.t("faith.declined_god", "%s declines to worship you.", follower.getName()));
        return true;
    }

    /** A player belongs to one god: leaving every other online god's list when joining a new one. */
    private static void leaveEveryoneElse(ServerPlayer follower, ServerPlayer keep) {
        var server = follower.getServer();
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p == keep || p == follower) continue;
            ManasSkillInstance inst = faithOf(p);
            if (inst == null) continue;
            List<Worshipper> list = worshippers(inst);
            if (FaithWorshipRules.isWorshipper(list, follower.getUUID())) save(inst, FaithWorshipRules.remove(list, follower.getUUID()));
        }
    }

    /** {@code /faith leave}: stops worshipping. */
    public static boolean leave(ServerPlayer follower) {
        ServerPlayer god = findGod(follower);
        if (god == null) {
            SkillUtil.fail(follower, SkillMsg.t("faith.no_god", "You worship no one (or your god is not online)."));
            return false;
        }
        ManasSkillInstance inst = faithOf(god);
        if (inst != null) save(inst, FaithWorshipRules.remove(worshippers(inst), follower.getUUID()));
        tell(follower, SkillMsg.t("faith.left", "You no longer worship %s.", god.getName()));
        tell(god, SkillMsg.t("faith.left_god", "%s no longer worships you.", follower.getName()));
        return true;
    }

    /** {@code /faith status}: the god's Faith and worshippers, or the worshipper's god. */
    public static void status(ServerPlayer p) {
        ManasSkillInstance inst = faithOf(p);
        if (inst != null) {
            settle(p, inst);
            List<Worshipper> list = worshippers(inst);
            tell(p, SkillMsg.t("faith.status_god", "Faith: %s. Worshippers: %s of %s (offered %s).", FaithSkill.getFaith(inst), list.size(),
                    NUConfig.faithMaxWorshippers(), FaithWorshipRules.totalWorship(list)));
            for (int i = 0; i < list.size() && i < STATUS_LINES; i++) {
                Worshipper w = list.get(i);
                p.sendSystemMessage(Component.literal(" - " + w.name() + ": " + w.worship()).withStyle(ChatFormatting.YELLOW));
            }
            return;
        }
        ServerPlayer god = findGod(p);
        if (god == null) {
            tell(p, SkillMsg.t("faith.no_god", "You worship no one (or your god is not online)."));
            return;
        }
        tell(p, SkillMsg.t("faith.status_follower", "You worship %s.", god.getName()));
    }

    // ------------------------------------------------------------------ offerings
    /** {@code /faith offering}: the clickable list of offerings a worshipper can make to their god. */
    public static void openOffering(ServerPlayer follower) {
        if (off(follower)) return;
        ServerPlayer god = findGod(follower);
        if (god == null) {
            SkillUtil.fail(follower, SkillMsg.t("faith.no_god", "You worship no one (or your god is not online)."));
            return;
        }
        double per = NUConfig.faithOfferingCost();
        tell(follower, SkillMsg.t("faith.offering_title", "Offering to %s (1 Faith costs %s magicules):", god.getName(), (long) per));
        MutableComponent row = Component.empty();
        for (int units : FaithWorshipRules.OFFER_STEPS) {
            row.append(button(SkillMsg.t("faith.btn_offer", "[Offer %s Faith]", units), "/faith offer " + units, ChatFormatting.AQUA));
            row.append(Component.literal(" "));
        }
        follower.sendSystemMessage(row);
    }

    /** {@code /faith offer <units>}: the worshipper pays magicules, the god gains that much Faith. Returns true when it worked. */
    public static boolean offer(ServerPlayer follower, int units) {
        if (off(follower)) return false;
        if (!FaithWorshipRules.isOfferStep(units) || !follower.isAlive()) {
            SkillUtil.fail(follower, SkillMsg.t("faith.bad_offer", "That offering is not possible."));
            return false;
        }
        long now = follower.level().getGameTime();
        Long last = OFFER_LAST.get(follower.getUUID());
        if (!FaithWorshipRules.ready(last == null ? -1L : last, now, NUConfig.faithOfferingCooldown())) {
            SkillUtil.fail(follower, SkillMsg.t("faith.offer_cooldown", "Wait a moment before the next offering."));
            return false;
        }
        ServerPlayer god = findGod(follower);
        if (god == null) {
            SkillUtil.fail(follower, SkillMsg.t("faith.no_god", "You worship no one (or your god is not online)."));
            return false;
        }
        double range = NUConfig.faithOfferingRange();
        if (god.level() != follower.level() || god.distanceToSqr(follower) > range * range) {
            SkillUtil.fail(follower, SkillMsg.t("faith.too_far", "%s is too far away (within %s blocks).", god.getName(), (long) range));
            return false;
        }
        ManasSkillInstance inst = faithOf(god);
        if (inst == null) return false;
        if (!SkillUtil.spendOffering(follower, FaithWorshipRules.offerCost(units, NUConfig.faithOfferingCost()))) return false;
        if (OFFER_LAST.size() >= MAX_CLOCKS) OFFER_LAST.clear();      // bounded: the clocks are only a rate limit, losing them is harmless
        OFFER_LAST.put(follower.getUUID(), now);
        FaithSkill.setFaith(inst, FaithRules.add(FaithSkill.getFaith(inst), units));
        save(inst, FaithWorshipRules.addWorship(worshippers(inst), follower.getUUID(), units));
        NUSkill.grow(inst, god, 0.25);
        tell(follower, SkillMsg.t("faith.offered", "You offer %s Faith to %s.", units, god.getName()));
        tell(god, SkillMsg.t("faith.offered_god", "%s offers you %s Faith. Faith: %s", follower.getName(), units, FaithSkill.getFaith(inst)));
        var level = follower.serverLevel();
        SkillFx.spray(level, false, god.position().add(0, 2.1, 0), 8, 0.4, 0.1, 0.4, 0.01);
        SkillFx.sound(level, god.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.4F);
        return true;
    }

    // ------------------------------------------------------------------ chosen gifts
    /** The god's Gift mode: lists what {@code target} can be given, each a click to {@code /faith gift}. */
    static void openGiftMenu(ServerPlayer god, ServerPlayer target, int cost, List<String> pool) {
        if (pool.isEmpty()) {
            SkillUtil.fail(god, SkillMsg.t("faith.has_all", "%s already has every skill you can bestow.", target.getName()));
            return;
        }
        tell(god, SkillMsg.t("faith.gift_title", "Bestow upon %s (costs %s Faith). Choose:", target.getName(), cost));
        String who = target.getName().getString();
        int shown = 0;
        for (String id : pool) {
            if (shown >= FaithWorshipRules.MENU_MAX) break;
            ResourceLocation rl = ResourceLocation.tryParse(id);
            ManasSkill skill = rl == null ? null : SkillAPI.getSkillRegistry().get(rl);
            if (skill == null) continue;
            god.sendSystemMessage(Component.literal(" ").append(button(skill.getName(), "/faith gift " + who + " " + rl, ChatFormatting.YELLOW)));
            shown++;
        }
    }

    /** {@code /faith gift <player> <skill>}: the click on the gift list; the skill class re-checks everything. Returns true when it was sent on. */
    public static boolean gift(ServerPlayer god, ServerPlayer target, ResourceLocation id) {
        ManasSkillInstance inst = faithOf(god);
        if (inst == null || !(inst.getSkill() instanceof FaithSkill skill)) {
            SkillUtil.fail(god, SkillMsg.t("faith.not_a_holder", "You do not hold Faith."));
            return false;
        }
        skill.giftChosen(inst, god, target, id);
        return true;
    }

    // ------------------------------------------------------------------ cleanup
    /** Logout: the player's open invitations (as god or as the invited) and offering clock go. */
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        INVITES.remove(id);
        INVITES.values().removeIf(i -> i.target().equals(id));
        OFFER_LAST.remove(id);
    }

    /** Server stop: nothing outlives the world. */
    public static void clear() {
        INVITES.clear();
        OFFER_LAST.clear();
    }

    /** For the self-test: the number of open invitations and clocks. */
    static int openState() { return INVITES.size() + OFFER_LAST.size(); }
}
