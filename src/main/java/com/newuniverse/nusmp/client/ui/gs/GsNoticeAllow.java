package com.newuniverse.nusmp.client.ui.gs;

/**
 * Which chat lines the alert crest may mirror. Only lines whose translation key is on this allow-list are mirrored (so the crest never echoes
 * somebody's chat, a command result or another mod's spam); the chat line itself is never cancelled or changed. Entries ending in '.' are
 * prefixes, the others are exact keys. New keys are appended here. Pure, unit tested.
 */
public final class GsNoticeAllow {
    private GsNoticeAllow() {}

    private static final String[] KEYS = {
            "nusmp.zone.enter",
            "nusmp.mana_zone.open",
            "nusmp.mana_zone.attuned",
            "nusmp.mana_zone.refused_head",
            "nusmp.dive.upgrade",
            "nusmp.spiritdive.tier.",
            "nusmp.noelle.black_exhausted",
            "nusmp.spirit_control.combat_on",
            "nusmp.ui.notice.",                       // the crest's own keys (page opened, ...)
    };

    public static boolean allowed(String key) {
        if (key == null || key.isEmpty() || key.length() > 120) return false;
        if (companionHeal(key)) return true;
        for (String k : KEYS) {
            if (k.endsWith(".") ? key.startsWith(k) : key.equals(k)) return true;
        }
        return false;
    }

    /** A companion looking after the player from her pouch ("nusmp.companion.<name>.use"): the one companion line the crest mirrors. */
    static boolean companionHeal(String key) {
        return key.startsWith("nusmp.companion.") && key.endsWith(".use") && key.indexOf('.', 16) == key.length() - 4;
    }

    /** The kind (colour) of a mirrored key: warnings and refusals read as WARN, exhaustion as BAD, the rest as INFO. */
    public static GsNoticeQueue.Kind kind(String key) {
        if (key == null) return GsNoticeQueue.Kind.INFO;
        if (companionHeal(key)) return GsNoticeQueue.Kind.OK;
        if (key.contains("refused") || key.contains("exhausted")) return GsNoticeQueue.Kind.WARN;
        if (key.contains("attuned") || key.contains("upgrade")) return GsNoticeQueue.Kind.OK;
        return GsNoticeQueue.Kind.INFO;
    }
}
