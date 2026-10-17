package com.newuniverse.nusmp.arc.danmachi;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcConfig;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.dialogue.TalkPlan;
import com.newuniverse.nusmp.arc.story.ArcStory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Why: four of the friends of the Dungeon ask the player for the fall of one monster (the dialogue offers it; see {@link QuestBook}), and the arc system's
 * quest stage only moves when somebody tells it to. This is that somebody: when a DanMachi boss falls, every fighter who is credited with the kill and whose
 * friend asked for exactly that boss has the request marked done ({@code ArcStory.setQuestStage(..., 2)}), so the friend thanks them the next time they
 * talk, and the quest hook key {@code arc.quest} fires for the advancements. It registers itself once, when the first DanMachi boss class is loaded, so the lead
 * has no line to add. The {@code quests} switch of the DanMachi config turns it off.
 */
public final class DanMachiQuests {
    private DanMachiQuests() {}

    private static boolean registered;

    /** Adds the listener to {@link ArcBoss#ON_DEFEATED}, once (a second call does nothing). */
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ArcBoss.ON_DEFEATED.add(DanMachiQuests::onDefeated);
    }

    private static void onDefeated(ArcBoss boss) {
        if (!ArcConfig.enabled() || !DanMachiConfig.quests()) return;
        List<String> askers = QuestBook.askersOf(boss.character().id());
        if (askers.isEmpty()) return;
        for (ServerPlayer p : ArcStory.credited(boss)) {
            for (String npc : askers) {
                try {
                    finish(p, npc);
                } catch (RuntimeException e) {
                    LogUtils.getLogger().error("[nusmp] could not finish the request of {} for {}: {}", npc, p.getGameProfile().getName(), e.toString());
                }
            }
        }
    }

    /** Marks the request of {@code npcId} done for this player (not again once it is done or thanked). True when the stage moved. */
    static boolean finish(ServerPlayer p, String npcId) {
        if (ArcStory.questStage(p, npcId) >= TalkPlan.QUEST_DONE) return false;
        if (!ArcStory.setQuestStage(p, npcId, TalkPlan.QUEST_DONE)) return false;
        ArcCharacter c = ArcRoster.byId(npcId);
        String name = c == null ? npcId : c.name();
        p.sendSystemMessage(Component.literal("[Request] " + name + " has been waiting for news of this fight. Go and talk to them.").withStyle(ChatFormatting.AQUA));
        return true;
    }
}
