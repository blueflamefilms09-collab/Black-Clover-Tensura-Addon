package com.newuniverse.nusmp.devil;

/**
 * Why: since 3.76.0 a devil can come from eight anime worlds, and a devil of each world is called and bargained with in that world's own way (docs/devil_worlds.md gives the source of
 * every claim). This pure enum is the world itself: its name, the name of its ritual, what the ritual asks of the caller, how long it runs, and its colour. What the ritual DOES tick by tick
 * is {@code DevilRituals}; whether a caller may begin is {@link DevilRitualRules}. Stored BY NAME, appended only.
 */
public enum DevilWorld {
    BLACK_CLOVER("Black Clover", "Devil-Binding Ritual", 200, 0xC81E2D,
            "Bring a relic tied to the devil (an Infernal Contract in your inventory, it is spent). The devil is called into a circle and tries to take your body: stand your ground inside the circle through four blows.",
            "No lasting price: the two of you are bound as equals."),
    CHAINSAW_MAN("Chainsaw Man", "A bargain of flesh", 80, 0xB01020,
            "Nothing to prepare: call the devil's name and it comes. It names its price and takes it from your body.",
            "A piece of you: flesh, an eye or years of your life, set by the devil."),
    BLACK_BUTLER("Black Butler", "Faustian contract", 140, 0x7A3CC8,
            "A demon answers only despair: your health must be at half or lower.",
            "The seal is branded on your right eye, and the demon is owed your soul: every death takes all your experience levels."),
    BLUE_EXORCIST("Blue Exorcist", "Tamer's summoning circle", 160, 0x3A7BFF,
            "A sheet of paper for the circle (it is spent) and a drop of your blood (two hearts now). Keep your will: stay in the circle and above a third of your health, or the demon turns on you.",
            "No lasting price: it serves while your will holds."),
    TENSURA("That Time I Got Reincarnated as a Slime", "Daemon summoning", 180, 0x3A2A5A,
            "An offering of bodies (32 rotten flesh, spent) and a name: naming the demon costs half of your maximum magicules.",
            "No lasting price: it serves the one who gave it a body and a name."),
    DORORO("Dororo", "Pact in the Hall of Hell", 160, 0x8A6A3A,
            "A roof over your head (no open sky): the demons of the hall answer a wish for prosperity.",
            "They take their share of your body (your left eye and a piece of your flesh) and pay in riches."),
    BERSERK("Berserk", "The Behelit's Eclipse", 200, 0x8A0A14,
            "It wakes only at the lowest point: your health at a quarter or lower, and 30 experience levels to offer.",
            "What is most precious: all your experience levels and half your life, and you carry the Brand (monsters hit you harder)."),
    FAIRY_TAIL("Fairy Tail", "A Book of Zeref", 140, 0x6A3CFF,
            "A book to open (it is spent) and ether to wake it: a third of your maximum magicules.",
            "No lasting price beyond the ether.");

    public final String label, ritual, need, price;
    /** How long the ritual runs, in ticks. */
    public final int ticks;
    public final int color;

    DevilWorld(String label, String ritual, int ticks, int color, String need, String price) {
        this.label = label;
        this.ritual = ritual;
        this.ticks = ticks;
        this.color = color;
        this.need = need;
        this.price = price;
    }

    public String key() { return name().toLowerCase(java.util.Locale.ROOT); }

    public static DevilWorld byName(String s) {
        if (s != null) for (DevilWorld w : values()) if (w.name().equalsIgnoreCase(s)) return w;
        return null;
    }
}
