package com.newuniverse.nusmp.entity.avatar;

/**
 * What the Devil Avatar says (pure, so the wording is checked in tests): the Avatar of Lucifero is cold and regal, the Avatar of Megicula
 * gleeful and cruel. Opening, power steps, the Gates, death, a line for each move, and the line the server hears when it falls.
 */
public final class AvatarText {
    private AvatarText() {}

    private static boolean megicula(int variant) { return AvatarTable.variantOf(variant) == AvatarTable.MEGICULA; }

    public static String opening(int variant) {
        return megicula(variant)
                ? "Oh, fresh little souls at my Gate! Let me taste what you are most afraid of~"
                : "The Gate remembers every soul that dared its floor. Kneel, little lights: Lucifero's shadow is heavy.";
    }

    /** The line for a power step from 'old' to 'now' (a step of the curve; 60 is where the mana is unbound). */
    public static String power(int variant, int old, int now) {
        boolean m = megicula(variant);
        if (now >= 80 && old < 80) return m ? "Eighty percent. Rot is only love that takes its time." : "Eighty percent. Even the floor bows to me.";
        if (now >= 60 && old < 60) return m ? "Sixty percent~ and my mana has no bottom now. How unfair!" : "Sixty percent. The Underworld holds no limit for me now.";
        if (now >= 40 && old < 40) return m ? "Forty percent... can you feel my curses crawling up your legs? Hee hee." : "Forty percent. The ceiling grows tired of waiting for you.";
        return m ? "Megicula lends me " + now + "%. Smile for me!" : "Lucifero's weight comes to me: " + now + "%.";
    }

    public static String gates(int variant) {
        return megicula(variant)
                ? "The Gates of the Tree of Qliphoth open! Megicula, my sweet devil, curse them all! One hundred percent!"
                : "The Gates of the Tree of Qliphoth open. Behold the weight of a true devil! One hundred percent!";
    }

    public static String death(int variant) {
        return megicula(variant)
                ? "Ah... the curses are turning on me... Megicula, finish what I began..."
                : "The Gate... closes... Lucifero... the weight is yours again.";
    }

    /** What the server is told when the Avatar falls (the Treasury Collapse follows). */
    public static String fell(String name, String killer) {
        String by = killer == null || killer.isBlank() ? "" : " at the hands of " + killer;
        return name + " is undone" + by + ". The Underworld Gate goes dark behind it.";
    }

    /** The lines a move may be accompanied by (never empty for a move of the table). */
    public static String[] quips(int variant, String move) {
        boolean m = megicula(variant);
        return switch (move) {
            case AvatarTable.VOID_CLAWS -> m ? new String[]{"Snip snip!", "Closer, little light~"} : new String[]{"Kneel.", "Heavy, aren't I?"};
            case AvatarTable.HEX_VOLLEY -> m ? new String[]{"Catch!", "Ooh, what a pretty bruise!"} : new String[]{"Hold still. Gravity does the rest.", "Small things, small curses."};
            case AvatarTable.SHADOW_STEP -> m ? new String[]{"Peekaboo~", "Boo!"} : new String[]{"Behind you.", "The dark is quicker than your eyes."};
            case AvatarTable.CEILING_PULL -> m ? new String[]{"Float away, little balloons~", "Up you go!"} : new String[]{"Up. The floor has no use for you.", "The ceiling asks for you."};
            case AvatarTable.FLOOR_CRUSH -> m ? new String[]{"Splat!", "Flatten for me~"} : new String[]{"Down. All the way down.", "The floor will have you."};
            case AvatarTable.DREAD_BRAND -> m ? new String[]{"I can smell the coward at the back~", "Tag, you're it!"}
                    : new String[]{"The one who hides: I see you.", "Run to the end of the hall. The brand will follow."};
            case AvatarTable.INVERSION_FIELD -> m ? new String[]{"Wheee! Up and down and up~", "Dizzy yet?"}
                    : new String[]{"Up, down: which way is the Gate, little ones?", "Gravity is a habit. Break it."};
            case AvatarTable.CURSE_NOVA -> m ? new String[]{"Ripples of rot~", "Jump, jump, little frogs!"}
                    : new String[]{"Feel the weight of every curse this Gate has heard.", "Fall with the wave."};
            case AvatarTable.CALL_DISCIPLES -> m ? new String[]{"Come, my pretty followers!", "Playtime, my darlings!"}
                    : new String[]{"Rise, my Disciples. Show them the Underworld's welcome.", "Come. Kneel them for me."};
            case AvatarTable.BRIMSTONE_ERUPTION -> m ? new String[]{"Brimstone blooms~ stand in the gaps!", "Hot, hot, hot!"}
                    : new String[]{"The Underworld splits. Mind the gaps.", "Brimstone: the Gate's own breath."};
            case AvatarTable.ABYSSAL_COLLAPSE -> m ? new String[]{"Abyssal Collapse! Everything comes down in the end~"} : new String[]{"Abyssal Collapse. Fall into the Gate."};
            default -> new String[]{"..."};
        };
    }
}
