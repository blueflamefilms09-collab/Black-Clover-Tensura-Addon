package com.newuniverse.nusmp.avatar;

/**
 * Why: the thirteen anime personality tropes of the Avatar system (owner brief "My Avatar's Path to Greatness"). An Avatar has one primary and optionally
 * one secondary personality; the chatter engine, the AI goals and the Yandere frenzy read them. The constants are only ever appended. Behaviour tables
 * live in the classes that use them (AvatarChatterEngine, AvatarAi), never here, so this enum stays a stable save format (stored by name).
 */
public enum AvatarPersonality {
    LOYAL("Loyal"), INDEPENDENT("Independent"), AGGRESSIVE("Aggressive"), MERCHANT("Merchant"), HEROIC("Heroic"), TSUNDERE("Tsundere"),
    YANDERE("Yandere"), LAZY("Lazy"), SCHOLAR("Scholar"), SADISTIC("Sadistic"), CHUNIBYO("Chunibyo"), GLUTTONOUS("Gluttonous"),
    COWARDLY("Cowardly"), STOIC("Stoic");

    public final String displayName;

    AvatarPersonality(String displayName) { this.displayName = displayName; }

    public static AvatarPersonality byName(String name) {
        if (name == null) return null;
        for (AvatarPersonality p : values()) if (p.name().equalsIgnoreCase(name)) return p;
        return null;
    }
}
