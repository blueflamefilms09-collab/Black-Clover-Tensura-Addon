package com.newuniverse.nusmp.companion.talk;

import net.minecraft.server.level.ServerPlayer;

/**
 * Why: the contract of anything that can be spoken to with the talk screen. The SERVER owns everything: {@link #talkOpen} returns the first screen, {@link #talkChoose} answers a pick and returns the next
 * screen ({@link TalkView#closed()} closes it), {@link #talkClosed} is told when the conversation ends for any reason. A client can only pick an id the entity put on the screen it was last sent; a pick from more
 * than {@link TalkRules#REACH} blocks away, from a dead entity, or too fast never reaches the entity. Open the screen from {@code mobInteract} with {@link Talk#open}. Anything implementing this is also never
 * attacked by a companion ({@code Bond.foe}).
 */
public interface Talkable {
    /** The first screen of a conversation with this player. Never null. */
    TalkView talkOpen(ServerPlayer player);

    /** The answer to a pick (an id that was on the last screen): the next screen, or {@link TalkView#closed()}. */
    TalkView talkChoose(ServerPlayer player, String id);

    /** The conversation ended (the player closed it, walked away, died, left, changed dimension, or the entity went). Optional. */
    default void talkClosed(ServerPlayer player) {}
}
