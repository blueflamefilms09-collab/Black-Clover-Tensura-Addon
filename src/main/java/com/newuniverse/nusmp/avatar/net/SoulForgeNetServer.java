package com.newuniverse.nusmp.avatar.net;

import com.newuniverse.nusmp.avatar.AvatarChatterEngine;
import com.newuniverse.nusmp.avatar.AvatarConfig;
import com.newuniverse.nusmp.avatar.AvatarCreation;
import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.AvatarSoulForgeConfig;
import com.newuniverse.nusmp.avatar.ChatEvent;
import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import com.newuniverse.nusmp.avatar.appearance.DynamicAppearanceEngine;
import com.newuniverse.nusmp.avatar.aptitude.Aptitude;
import com.newuniverse.nusmp.avatar.aptitude.AptitudeCatalog;
import com.newuniverse.nusmp.avatar.aptitude.AptitudeLook;
import com.newuniverse.nusmp.avatar.aptitude.ForgeChoices;
import com.newuniverse.nusmp.avatar.aptitude.ForgeContext;
import com.newuniverse.nusmp.avatar.aptitude.ForgeSkill;
import com.newuniverse.nusmp.avatar.chatter.ChatterTemplate;
import com.newuniverse.nusmp.avatar.race.AvatarRace;
import com.newuniverse.nusmp.avatar.race.AvatarRaces;
import com.newuniverse.nusmp.avatar.ritual.ForgeReserves;
import com.newuniverse.nusmp.avatar.ritual.ForgeSkillResolver;
import com.newuniverse.nusmp.avatar.ritual.SoulForgeRitual;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Why: every packet the Soul Forge screen sends is untrusted, exactly like the classic screen's. These handlers (all on the server's main thread) apply the
 * same three guards to each one: the sender really has the Soul Forge screen open IN a running ritual (the menu's token must match the ritual's), they are not
 * spamming (a per-player gap for each kind of request), and every string is cleaned before it is read. They then hand over to the ritual, which decides, and
 * answer with the server's own words. The client never chooses a price, a tax, a luck value or the result of the free aptitude roll: Finalize carries only
 * ids and a roll token, and {@link SoulForgeRitual#finalizeRequest} re-reads the creator's reserves from the game and runs the pure validator before anything
 * is spent. This class also builds the state packet that gives the screen the catalogs and the creator's reserves ({@link #sendState}).
 */
public final class SoulForgeNetServer {
    private SoulForgeNetServer() {}

    static final RollLimiter ROLL_LIMIT = new RollLimiter(), ACTION_LIMIT = new RollLimiter(), FINAL_LIMIT = new RollLimiter();
    /** The moments a sample line can be about (the screen shows which). */
    private static final ChatEvent[] SAMPLES = {ChatEvent.COMBAT_START, ChatEvent.SKILL_CAST, ChatEvent.IDLE, ChatEvent.CREATOR_LOW_HP};

    private static void reply(ServerPlayer p, boolean ok, String kind, String msg) {
        PacketDistributor.sendToPlayer(p, new S2C_CreationResultPayload(ok, kind, NetText.clean(msg, 240)));
    }

    // ================================================================ state
    /** Sends the catalogs, the rules and the creator's fresh reserves (the screen opens on this). */
    public static void sendState(ServerPlayer p, SoulForgeRitual.Ritual r) {
        List<ForgeAptitudeRow> apt = new ArrayList<>();
        for (Aptitude a : AptitudeCatalog.all()) {
            if (apt.size() >= ForgeAptitudeRow.MAX_ROWS) break;
            apt.add(ForgeAptitudeRow.of(a, AptitudeLook.of(a)));
        }
        List<ForgeSkillRow> skills = new ArrayList<>();
        for (ForgeSkill s : ForgeSkillResolver.available()) {
            if (skills.size() >= ForgeSkillRow.MAX_ROWS) break;
            skills.add(ForgeSkillRow.of(s));
        }
        ForgeContext ctx = ForgeReserves.read(p, r.rolledAptitude(), r.rolledToken(), 0.0);
        PacketDistributor.sendToPlayer(p, new S2C_SoulForgeStatePayload(AvatarSoulForgeConfig.rules(), ctx, r.rollsLeft(), apt, skills));
    }

    // ================================================================ the free aptitude roll
    public static void onAptitudeRoll(ServerPlayer p, C2S_AptitudeRollPayload m) {
        if (!AvatarConfig.enabled() || SoulForgeRitual.withScreenOpen(p) == null) return;
        if (!ROLL_LIMIT.allow(p.getUUID(), System.currentTimeMillis(), AvatarSoulForgeConfig.rollGapMillis())) return;
        SoulForgeRitual.AptitudeRolled o = SoulForgeRitual.rollAptitude(p, cleanIds(m.picked()));
        if (!o.ok()) { reply(p, false, "apt_roll", o.refusal()); return; }
        PacketDistributor.sendToPlayer(p, new S2C_AptitudeRollPayload(o.aptitude(), o.token(), o.rollsLeft(), o.reelSeed()));
    }

    // ================================================================ the sample line
    public static void onBark(ServerPlayer p, C2S_BarkRequestPayload m) {
        if (!AvatarConfig.enabled() || SoulForgeRitual.withScreenOpen(p) == null) return;
        if (!ACTION_LIMIT.allow(p.getUUID(), System.currentTimeMillis(), AvatarSoulForgeConfig.actionGapMillis())) return;
        AvatarPersonality primary = AvatarPersonality.byName(NetText.clean(m.primary(), NetText.ID_MAX));
        if (primary == null) return;
        AvatarPersonality secondary = AvatarPersonality.byName(NetText.clean(m.secondary(), NetText.ID_MAX));
        if (secondary == primary) secondary = null;
        ChatEvent ev = SAMPLES[p.getRandom().nextInt(SAMPLES.length)];
        String template = AvatarChatterEngine.choose(p.getRandom(), primary, secondary, ev);
        Map<String, String> values = new HashMap<>();
        values.put("name", "Your Avatar");
        values.put("creator", p.getGameProfile().getName());
        values.put("target", "the enemy");
        values.put("skill", "Mana Burst");
        values.put("grimoire", "grimoire");
        values.put("dungeon", "the Dungeon");
        values.put("relic", "that relic");
        values.put("magicule", "magicules");
        reply(p, true, "bark", ev.name() + "|" + (template == null ? "" : ChatterTemplate.fill(template, values)));
    }

    // ================================================================ Finalize
    public static void onFinalize(ServerPlayer p, C2S_FinalizeSoulForgePayload m) {
        if (!AvatarConfig.enabled() || SoulForgeRitual.withScreenOpen(p) == null) return;
        if (!FINAL_LIMIT.allow(p.getUUID(), System.currentTimeMillis(), AvatarSoulForgeConfig.finalizeGapMillis())) { reply(p, false, "forge", "Please wait a moment."); return; }
        String raceId = NetText.clean(m.raceId(), NetText.ID_MAX);
        AvatarRace race = AvatarRaces.get(raceId);
        if (race == null) { reply(p, false, "forge", "That race is not available."); return; }
        AvatarPersonality primary = AvatarPersonality.byName(NetText.clean(m.primary(), NetText.ID_MAX));
        if (primary == null) { reply(p, false, "forge", "Choose a primary personality."); return; }
        AvatarPersonality secondary = AvatarPersonality.byName(NetText.clean(m.secondary(), NetText.ID_MAX));
        if (secondary == primary) secondary = null;
        AppearanceData look = DynamicAppearanceEngine.sanitize(AppearanceData.fromTag(m.appearance()), race);
        String name = NetText.clean(m.name(), NetText.NAME_MAX).strip();
        ForgeChoices choices = new ForgeChoices(cleanIds(m.aptitudes()), m.rollToken(), cleanId(m.unique()), cleanId(m.ultimate()), m.paidAwakening());
        AvatarCreation.CreationRequest req = new AvatarCreation.CreationRequest(raceId, primary, secondary, look.toTag(), name);
        AvatarCreation.Result res = SoulForgeRitual.finalizeRequest(p, req, m.statToken(), choices);
        if (!res.ok()) reply(p, false, "forge", res.message() == null || res.message().isBlank() ? "The Soul Forge refused." : res.message());
        // on success the ritual closes the screen itself and the spectacle begins
    }

    // ================================================================ cleaning
    private static String cleanId(String s) { return NetText.clean(s, NetText.ID_MAX).strip().toLowerCase(Locale.ROOT); }

    /** Cleaned and lower-cased, at most eight (the screen can never legitimately send more), in the order sent; duplicates and blanks stay so the validator names them. */
    private static List<String> cleanIds(List<String> in) {
        List<String> out = new ArrayList<>();
        if (in != null) for (String s : in) {
            if (out.size() >= C2S_FinalizeSoulForgePayload.MAX_APTITUDES) break;
            out.add(cleanId(s));
        }
        return out;
    }

    public static void onLogout(ServerPlayer p) {
        ROLL_LIMIT.forget(p.getUUID());
        ACTION_LIMIT.forget(p.getUUID());
        FINAL_LIMIT.forget(p.getUUID());
    }
}
