package com.newuniverse.nusmp.arc.story;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcBossProfile;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcConfig;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.arc.dialogue.DialogueSituation;
import com.newuniverse.nusmp.boss.BossBuffTable;
import com.newuniverse.nusmp.boss.BossRegistry;
import com.newuniverse.nusmp.charter.CharterItems;
import com.newuniverse.nusmp.charter.CharterTable;
import com.newuniverse.nusmp.quests.NusmpEvents;
import com.newuniverse.nusmp.quests.QuestTeams;
import com.newuniverse.nusmp.quests.QuestsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the story of the arcs is a chain (each boss unlocks the next) and a record (who has beaten whom, whom they have met), and the dungeon, the arenas,
 * the NPC dialogue, the commands and the quests all need to ask the same questions about it. This class is the one game-side door: it keeps each player's
 * {@link StoryData} in an attachment, listens for {@link ArcBoss#ON_DEFEATED} and gives every fighter the credit, and answers "is this boss open to me",
 * "what stage am I at" and "which saga am I in" with the pure {@link StoryRules}. Quests and advancements hear about all of it through
 * {@link NusmpEvents} keys (listed in {@code docs/arc/core.md}); the lead wires the advancements to those keys.
 *
 * <p>It also does the boot-time wiring that every boss of the roster needs exactly once, from {@link #init}: the Charter Grimoire (the table row and the
 * item), the row of the global boss buff engine from the character's tier, and the boss registry entry that makes the entity a boss of this mod.
 */
public final class ArcStory {
    private ArcStory() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    /** Quest hook keys (all through {@link NusmpEvents#fire}; the argument is given after the slash). */
    public static final String KEY_DEFEATED = "arc.defeated", KEY_MET = "arc.met", KEY_TALK = "arc.talk", KEY_SAGA = "arc.saga",
            KEY_QUEST = "arc.quest", KEY_UNLOCKED = "arc.unlocked";

    private static boolean initialised;

    /**
     * Boot wiring. Call once from {@code ArcSystem.init}, after {@code ArcCharacters.bootstrap()} (the roster must be filled) and before
     * {@code NUItems.ITEMS} is registered on the mod bus (the grimoire items are added to it here).
     */
    public static synchronized void init(IEventBus modBus) {
        if (initialised) return;
        initialised = true;
        StoryAttachments.ATTACHMENTS.register(modBus);
        for (ArcCharacter c : ArcRoster.bosses()) wireBoss(c);
        ArcBoss.ON_DEFEATED.add(ArcStory::onDefeated);
    }

    /** The per-boss rows: buff profile, boss registry entry, Charter Grimoire (table row and item). A clash with an older boss is logged and skipped. */
    private static void wireBoss(ArcCharacter c) {
        if (com.newuniverse.nusmp.arc.world.ExistingBosses.owns(c.id())) return;                // 1.61.0: the seven older bosses already have their registry, buff and Charter rows (arc/world/ExistingBosses)
        if (!BossRegistry.addArc(c.id(), c.id())) LOG.warn("[nusmp] arc boss {} is not added to the boss registry (the id or entity path is taken)", c.id());
        if (!BossBuffTable.addArc(ArcBossProfile.profileFor(c))) LOG.warn("[nusmp] arc boss {} has no global buff row (the id is taken)", c.id());
        if (c.hasCharter()) {
            CharterTable.Boss b = CharterTable.addArc(c);
            if (b == null) LOG.warn("[nusmp] arc boss {} has no Charter Grimoire (no lawful skill left, or the id is taken)", c.id());
            else CharterItems.registerArc(b);
        }
    }

    // ================================================================ reading
    public static StoryData data(Player p) { return StoryAttachments.get(p); }

    private static boolean exists(String id) { return ArcRoster.byId(id) != null; }

    public static boolean hasDefeated(Player p, String id) { return data(p).hasDefeated(id); }

    public static boolean hasMet(Player p, String npcId) { return data(p).hasMet(npcId); }

    /** Are all of the character's prerequisites defeated by this player? Ids that are not in the roster are skipped. */
    public static boolean unlocked(Player p, ArcCharacter c) { return StoryRules.unlocked(data(p), c, ArcStory::exists); }

    /** The prerequisites of the character this player still has to defeat, in order. */
    public static List<String> missing(Player p, ArcCharacter c) { return StoryRules.missing(data(p), c, ArcStory::exists); }

    /** The story stage of a player: how many bosses of the roster they have defeated. */
    public static int stageOf(Player p) { return StoryRules.stage(data(p), ArcRoster.all()); }

    /** The saga the player is in. */
    public static ArcSaga sagaOf(Player p) { return StoryRules.currentSaga(data(p), ArcRoster.all()); }

    public static int questStage(Player p, String npcId) { return data(p).questStage(npcId); }

    /** The bosses this player can fight now and has not yet beaten. */
    public static List<ArcCharacter> available(Player p) { return StoryRules.available(data(p), ArcRoster.all(), ArcStory::exists); }

    /** The situation a dialogue is chosen for. */
    public static DialogueSituation situation(Player p, ArcCharacter c) {
        StoryData d = data(p);
        return DialogueSituation.of(StoryRules.stage(d, ArcRoster.all()), StoryRules.currentSaga(d, ArcRoster.all()).id(), d.defeatedSet(), d.questStage(c.id()), !d.hasMet(c.id()));
    }

    // ================================================================ the gate
    /**
     * May this boss be summoned for these players? With {@code arc.storyGate} off, always. Otherwise at least one of them must have it unlocked: a party
     * plays together, and the player who has walked the story far enough opens the way for the others.
     */
    public static boolean gateOpen(Collection<? extends Player> party, ArcCharacter c) {
        if (!ArcConfig.storyGate()) return true;
        for (Player p : party) if (unlocked(p, c)) return true;
        return false;
    }

    /** What the best-placed of these players still has to defeat before the boss opens (empty when it is open or nobody is there to ask). */
    public static List<String> gateMissing(Collection<? extends Player> party, ArcCharacter c) {
        List<String> best = null;
        for (Player p : party) {
            List<String> m = missing(p, c);
            if (best == null || m.size() < best.size()) best = m;
        }
        return best == null ? List.of() : best;
    }

    // ================================================================ writing
    /** What a defeat did for one player. */
    public record Credit(boolean fresh, List<ArcCharacter> opened, ArcSaga sagaNow, boolean sagaChanged) {}

    /**
     * Records that the player defeated a character: it counts for the unlock chain from now on. Fires {@value #KEY_DEFEATED} (and {@value #KEY_UNLOCKED}
     * for each boss it opened, {@value #KEY_SAGA} when the saga moved on). Does nothing the second time.
     */
    public static Credit defeated(ServerPlayer p, String id) {
        StoryData d = data(p);
        StoryData before = d.copy();
        if (!d.defeat(id)) return new Credit(false, List.of(), sagaOf(p), false);
        ArcSaga now = StoryRules.currentSaga(d, ArcRoster.all());
        String old = d.saga();
        boolean moved = !old.isEmpty() && !old.equals(now.id());
        d.setSaga(now.id());
        List<ArcCharacter> opened = StoryRules.newlyAvailable(before, d, ArcRoster.all(), ArcStory::exists);
        NusmpEvents.fire(p, KEY_DEFEATED, id.trim().toLowerCase(Locale.ROOT));
        for (ArcCharacter c : opened) NusmpEvents.fire(p, KEY_UNLOCKED, c.id());
        if (moved) NusmpEvents.fire(p, KEY_SAGA, now.id());
        return new Credit(true, opened, now, moved);
    }

    /** Records that the player has met (spoken to) an NPC. True the first time; fires {@value #KEY_MET}. */
    public static boolean met(ServerPlayer p, String npcId) {
        if (!data(p).meet(npcId)) return false;
        NusmpEvents.fire(p, KEY_MET, npcId.toLowerCase(Locale.ROOT));
        return true;
    }

    /** Sets the quest stage with an NPC (the quest system calls this when a quest is done: 2). Fires {@value #KEY_QUEST} with {@code npc:stage} when it changed. */
    public static boolean setQuestStage(ServerPlayer p, String npcId, int stage) {
        StoryData d = data(p);
        if (!d.setQuestStage(npcId, stage)) return false;
        NusmpEvents.fire(p, KEY_QUEST, npcId.toLowerCase(Locale.ROOT) + ":" + d.questStage(npcId));
        return true;
    }

    /** Credits the player with everything that must fall before {@code c}, so that it is open to them (the unlock command). Returns the ids newly credited. */
    public static List<String> unlock(ServerPlayer p, ArcCharacter c) {
        List<String> done = new ArrayList<>();
        for (String id : StoryRules.closure(c, ArcRoster::byId)) if (defeated(p, id).fresh()) done.add(id);
        return done;
    }

    /** Forgets all of the player's story (the reset command). */
    public static void reset(ServerPlayer p) { data(p).clear(); }

    // ================================================================ a boss fell
    /** Everyone who gets the credit for a defeat: the online fighters, and with the share switches on, the nearby members of their FTB teams. */
    public static List<ServerPlayer> credited(ArcBoss boss) {
        MinecraftServer server = boss.getServer();
        Map<UUID, ServerPlayer> out = new LinkedHashMap<>();
        if (server == null) return new ArrayList<>();
        for (UUID u : new ArrayList<>(boss.fighters())) {
            ServerPlayer p = server.getPlayerList().getPlayer(u);
            if (p != null) out.put(u, p);
        }
        if (ArcConfig.shareCredit() && QuestsConfig.shareSquadCredit()) {
            for (ServerPlayer p : new ArrayList<>(out.values())) {
                try {
                    for (ServerPlayer m : QuestTeams.nearbyMembers(p, QuestsConfig.shareRadius())) out.putIfAbsent(m.getUUID(), m);
                } catch (RuntimeException noTeams) { /* no FTB Teams, or its API changed: the fighters alone */ }
            }
        }
        return new ArrayList<>(out.values());
    }

    private static void onDefeated(ArcBoss boss) {
        if (!ArcConfig.enabled()) return;
        ArcCharacter c = boss.character();
        for (ServerPlayer p : credited(boss)) {
            try {
                tell(p, c, defeated(p, c.id()));
            } catch (RuntimeException e) {
                LOG.error("[nusmp] could not give {} the story credit for {}: {}", p.getGameProfile().getName(), c.id(), e.toString());
            }
        }
    }

    private static void tell(ServerPlayer p, ArcCharacter c, Credit cr) {
        if (!cr.fresh()) return;
        p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.fallen", "[Story] %s has fallen.", c.fullName()).withStyle(ChatFormatting.GOLD));
        if (!cr.opened().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (ArcCharacter o : cr.opened()) names.add(o.fullName());
            p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.opened", "[Story] Now open: %s", String.join(", ", names)).withStyle(ChatFormatting.AQUA));
        }
        if (cr.sagaChanged()) {
            MutableComponent m = Component.translatableWithFallback("message.nusmp.arc.saga", "[Story] A new saga begins: %s", cr.sagaNow().title());
            p.sendSystemMessage(m.withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
