package com.newuniverse.nusmp.client.companion;

import com.newuniverse.nusmp.companion.talk.TalkView;

/**
 * Why: there are two talk screens now (the classic {@link TalkScreen} and the Great Spirit one) and the three hooks the common packets call (open or update, end) must treat them alike. Both are
 * "a screen with a conversation": it has the entity it is about, takes the server's next view, and closes when the server ends the talk. Neither decides anything.
 */
public interface TalkHost {
    int entityId();

    /** A new screen of the conversation from the server (the answer to a pick). */
    void update(TalkView view);

    /** The server closed the conversation. */
    void closeFromServer();
}
