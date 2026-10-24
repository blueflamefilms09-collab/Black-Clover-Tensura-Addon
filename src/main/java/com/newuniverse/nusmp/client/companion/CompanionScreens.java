package com.newuniverse.nusmp.client.companion;

import com.newuniverse.nusmp.client.companion.gs.BondStory;
import com.newuniverse.nusmp.client.companion.gs.CompanionCues;
import com.newuniverse.nusmp.client.companion.gs.CompanionStyle;
import com.newuniverse.nusmp.client.companion.gs.GiftMemory;
import com.newuniverse.nusmp.client.companion.gs.GsTalkScreen;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.companion.talk.TalkView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;

/**
 * Why: the one place that decides which talk screen a conversation gets (the classic plain one or the Great Spirit one, by the client option {@code companions.screenStyle}, whose AUTO follows the theme) and
 * that keeps the little memories the Great Spirit screen shares between openings: what she said about each gift (so the hints appear after the first reaction) and the bond story (the last events with each
 * companion). Both are bounded (8 companions, 24 items, 6 events each), live in memory only, belong to this client alone (the server sends nothing for them) and are cleared on logout. It also listens, read
 * only, for the line a companion says when she looks after the player from her pouch (an action-bar line the server already sends), to put it in the story.
 */
public final class CompanionScreens {
    private CompanionScreens() {}

    private static final GiftMemory GIFTS = new GiftMemory();
    private static final BondStory STORY = new BondStory();
    private static int storyVersion;

    public static GiftMemory gifts() { return GIFTS; }

    public static BondStory story() { return STORY; }

    /** Counts up whenever the story gets a new line (the open screen rebuilds and replays its compile text). */
    public static int storyVersion() { return storyVersion; }

    public static void storyChanged() { storyVersion++; }

    /** Which look the next talk screen gets. */
    public static CompanionStyle style() { return CompanionStyle.choose(CompanionClientConfig.screenChoice(), UiTheme.greatSpirit()); }

    /** The talk screen for a conversation. Both kinds are {@link TalkHost}s. */
    public static Screen create(int entityId, TalkView view) {
        return style() == CompanionStyle.GREAT_SPIRIT ? new GsTalkScreen(entityId, view) : new TalkScreen(entityId, view);
    }

    public static void clear() {
        GIFTS.clear();
        STORY.clear();
        storyVersion++;
    }

    /** Read only: a companion's "looking after you" line goes into her story. Never cancels or changes the message. */
    public static void onChat(ClientChatReceivedEvent e) {
        Component m = e.getMessage();
        if (m == null || !(m.getContents() instanceof TranslatableContents tc)) return;
        if (CompanionCues.cueOf(tc.getKey()) != CompanionCues.Cue.USE) return;
        String prefix = CompanionCues.prefixOf(tc.getKey());
        Minecraft mc = Minecraft.getInstance();
        if (prefix == null || mc.level == null) return;
        STORY.add(prefix, BondStory.Kind.HEAL, Component.translatable("nusmp.companion.gs.ev.heal").getString(), mc.level.getGameTime());
        storyVersion++;
    }
}
