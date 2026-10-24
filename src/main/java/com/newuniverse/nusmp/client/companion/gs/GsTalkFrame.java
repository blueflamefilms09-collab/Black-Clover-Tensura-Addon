package com.newuniverse.nusmp.client.companion.gs;

import com.newuniverse.nusmp.client.ui.gs.GsSkin;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the painter needs to draw one frame of the Great Spirit talk screen, as plain strings and numbers (no Minecraft type), so the real screen, the preview tool and the tests fill the same value.
 * The screen keeps ONE of these and refills it each frame (no allocation): texts are resolved into the player's language when a view arrives, never per frame. Public fields on purpose: it is a
 * value holder, not a model.
 */
public final class GsTalkFrame {
    // ---------------------------------------------------------------- label ids (the texts the painter needs, resolved by the screen)
    public static final int L_CLOSE = 0, L_GIVE = 1, L_LIKES = 2, L_FIVE = 3, L_FOLLOW = 4, L_WAIT = 5, L_PANEL = 6, L_USE_ON = 7, L_USE_OFF = 8, L_PUT = 9, L_TAKE_ALL = 10, L_TASTES = 11,
            L_STORY = 12, L_PICKER = 13, L_GIFT_HINT = 14, L_HAND = 15, L_NO_STORY = 16, L_RING_READY = 17, L_RING_EMPTY = 18, L_RING_OFF = 19, L_RING_LOCKED = 20, L_STANCE0 = 21,
            L_STANCE1 = 22, L_STANCE2 = 23, L_MOOD = 24, L_NO_TASTES = 25, L_FOLLOWING = 26, L_WAITING = 27, L_PUT_HINT = 28, L_COUNT = 29;

    public GsSkin skin = GsSkin.B;
    /** Quality HIGH: glows, ripples, sparkles, corners. */
    public boolean fancy = true;
    /** The screen's clock in ticks (a whole tick plus the partial tick). */
    public float time;
    public int color = 0xF4EAD0;
    public String name = "", sub = "", tierName = "", moodName = "";
    public final String[] tabNames = {"", "", "", ""};
    public final String[] labels = new String[L_COUNT];
    public int tab;

    // ---------------------------------------------------------------- portrait
    public FaceInfo face = FaceInfo.NONE;
    public Mood mood = Mood.NEUTRAL;
    public float moodAge = 1000f;

    // ---------------------------------------------------------------- rapport
    public int rapport = -1;
    public float rapportShown;
    public float rapportGlow;
    public int litLeaf = -1;
    public float sinceRise = 1000f;
    public int toNext;
    /** The rapport number, the line under the leaves ("a friend  14") and the "+3" to the next leaf: built when the view arrives, never per frame. */
    public String rapportText = "", tierLine = "", nextText = "";

    // ---------------------------------------------------------------- dialogue
    public String[] dialogue = new String[0];
    public int typed = Integer.MAX_VALUE;
    public boolean dialogueDone = true;

    // ---------------------------------------------------------------- hover and ripple
    public int hover = GsHit.NONE;
    public float rippleX, rippleY, rippleAt = -1000f;

    // ---------------------------------------------------------------- the plain rows
    public final List<Row> rows = new ArrayList<>();
    public int scroll;

    /** One plain choice. */
    public record Row(String label, boolean blocked) {}

    // ---------------------------------------------------------------- interact tab
    /** Present flags (the widget is drawn only when the server offered the choice). */
    public boolean hasFive, fiveBlocked, hasPanel, panelBlocked, hasFollow, followBlocked;
    /** True when she walks with the player (the view offers "wait"), false when she waits (it offers "join"). */
    public boolean following;

    // ---------------------------------------------------------------- bond tab
    public boolean hasGift, giftBlocked, hasLikes;
    /** The selected inventory index for the gift slot, or -1; its display count and reaction hint. */
    public int heldIndex = -1, heldCount;
    public GiftMemory.Reaction heldReaction;
    public boolean pickerOpen;
    /** The notebook: up to 8 remembered items (their reactions; the items themselves come through {@link GsItemRef#taste}). */
    public final List<GiftMemory.Reaction> tasteReactions = new ArrayList<>();
    /** The inventory index of every picker slot that holds an item (a bit per index) and the remembered reaction of each (or null). */
    public long inventoryFilled;
    public final GiftMemory.Reaction[] inventoryReactions = new GiftMemory.Reaction[36];
    public int stance = -1;
    public boolean stanceOffered;
    public final boolean[] stanceBlocked = new boolean[3];
    public String stanceTip = "";

    // ---------------------------------------------------------------- the story
    public String[] story = new String[0];
    public BondStory.Kind[] storyKinds = new BondStory.Kind[0];
    public float storyAge = 1000f;

    // ---------------------------------------------------------------- pouch tab
    /** Counts to draw in the corner of each pouch slot ("" = empty). */
    public final String[] pouchCounts = {"", "", "", "", "", "", "", "", ""};
    public int useIt = -1;
    public PouchRing.State ring = PouchRing.State.LOCKED;
    public int ringSlot = -1;
    public boolean hasPut, putBlocked, hasTakeAll, hasUse;
    /** Counts of the hotbar strip are drawn by the item decoration itself; this is only the selected strip slot (the held inventory index when it is 0..8). */
    public String hint = "";

    public GsTalkFrame() {
        for (int i = 0; i < L_COUNT; i++) labels[i] = "";
    }

    public String label(int id) { return id >= 0 && id < L_COUNT && labels[id] != null ? labels[id] : ""; }
}
