package com.newuniverse.nusmp.client.skilltree;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Why: every word the Skill Tree screen and its menu button put on the screen is a translation key, because the mod ships one language file and a player may play in another; the
 * keys live in this one class (and nowhere else as a literal) so that the lang fragment, the preview tool and the screen are checked against the same list, and a typo is a compile
 * error instead of a raw {@code gui.nusmp.skilltree.byu} on somebody's screen. The prefix {@code gui.nusmp.skilltree.} keeps the whole screen in its own corner of
 * {@code en_us.json}, away from the Grimoire menus' keys, which is the same isolation the design asks of the code. The text that belongs to a route or a node (its name, description,
 * book name, lore) is NOT here: it is data from the synced registry and is shown as it arrives.
 */
final class SkillTreeLang {
    private SkillTreeLang() {}

    private static final String P = "gui.nusmp.skilltree.";

    // the menu button
    static final String BUTTON = P + "button", BUTTON_TIP = P + "button.tip";
    // the header
    static final String TITLE = P + "title", CLOSE = P + "close", SP = P + "sp", SP_TIP_HAVE = P + "sp.tip.have", SP_TIP_SPENT = P + "sp.tip.spent", CLASS = P + "class", CLASS_NONE = P + "class.none";
    // the route list
    static final String ROUTES = P + "routes", STATE_UNLOCKED = P + "state.unlocked", STATE_LOCKED = P + "state.locked", STATE_ACTIVE = P + "state.active",
            MAGIC = P + "magic.";                                                 // MAGIC + family id; a family nobody translated falls back to its prettified id
    // a sealed route
    static final String REQUIRES = P + "requires", FIND_HINT = P + "find_hint", LOCKED_TITLE = P + "locked.title", LOCKED_BODY = P + "locked.body";
    // the class button
    static final String SET_ACTIVE = P + "set_active", SET_ACTIVE_NOW = P + "set_active.now", SET_ACTIVE_WAIT = P + "set_active.wait";
    // the detail card
    static final String LEVEL = P + "level", LEVEL_MAX = P + "level.max", PRICE_NEXT = P + "price.next", SP_YOU_HAVE = P + "price.have", NEEDS = P + "needs", NEEDS_LINE = P + "needs.line",
            VALUES = P + "values", COST = P + "cost", TAG_ACTIVE = P + "tag.active", TAG_PASSIVE = P + "tag.passive", NO_SELECTION = P + "no_selection", ROUTE_SUMMARY = P + "route.summary";
    // what a node is, in words
    static final String NODE_SEALED = P + "node.sealed", NODE_PARENTS = P + "node.parents", NODE_READY = P + "node.ready", NODE_NO_SP = P + "node.no_sp", NODE_OWNED = P + "node.owned",
            NODE_MAXED = P + "node.maxed";
    // the Buy button
    static final String BUY = P + "buy", BUY_PRICE = P + "buy.price", BUY_MAXED = P + "buy.maxed", BUY_WAIT = P + "buy.wait", BUY_NEED_SP = P + "buy.need_sp", BUY_LOCKED = P + "buy.locked",
            BUY_SEALED = P + "buy.sealed";
    // the canvas chips and hints
    static final String ZOOM_OUT_TIP = P + "zoom.out", ZOOM_IN_TIP = P + "zoom.in", FIT = P + "fit", FIT_TIP = P + "fit.tip", HINT_LONG = P + "hint.long", HINT_SHORT = P + "hint.short";
    // notices
    static final String NOTICE_BOUGHT = P + "notice.bought", NOTICE_DENIED = P + "notice.denied", NOTICE_CLASS_DENIED = P + "notice.class_denied", NOTICE_CLASS_SET = P + "notice.class_set";
    // an empty registry, and a server that has the Skill Tree switched off
    static final String NO_ROUTES = P + "no_routes", NO_ROUTES_HINT = P + "no_routes.hint", OFF = P + "off";

    static MutableComponent tr(String key, Object... args) { return Component.translatable(key, args); }

    static String str(String key, Object... args) { return Component.translatable(key, args).getString(); }
}
