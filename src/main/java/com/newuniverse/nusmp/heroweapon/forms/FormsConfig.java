package com.newuniverse.nusmp.heroweapon.forms;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the forms system, {@code nusmp-heroweapon-forms-server.toml}, section {@code [forms]}: whether learning is automatic, what the gem takes in,
 * how fast mastery grows, the lock after a technique, the rate limit of absorbing and the small experience rewards. Its own file (not more keys in the core's)
 * so the forms builder never edits the core's spec, and a server owner who wants a different pace changes one file. Every value is a plain {@code define} /
 * {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the server is still loading) and every reader goes through {@link #get},
 * which answers the default while the spec is not loaded yet (the self-tests and the client run before any config exists).
 */
public final class FormsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue AUTO_LEARN, ABSORB_ANYTHING, ABSORB_ONLY_NEEDED, CYCLE_CURSE_FORMS, ANNOUNCE, EFFECTS;
    public static final ModConfigSpec.IntValue MASTERY_PER_HIT, MASTERY_PER_SKILL, MASTERY_PER_BLOCK, TECHNIQUE_LOCK_TICKS, ABSORB_PER_SECOND_LIMIT, LEARN_EXPERIENCE,
            MASTERY_EXPERIENCE, BOOK_REFRESH_TICKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("forms");
        AUTO_LEARN = b.comment("A form is learned as soon as the gem has absorbed what it asks for and its parents are learned. Off: the Weapon Book's Learn button does it.")
                .define("autoLearn", true);
        ABSORB_ANYTHING = b.comment("The gem takes in any item (it is counted, but only the items some form asks for ever matter), except tools, weapons, armour, enchanted or renamed items that no form asks for. Off: only items some form of the weapon asks for.")
                .define("absorbAnything", false);
        ABSORB_ONLY_NEEDED = b.comment("The gem takes only as many of an item as the forms still to be learned can use (the rest stays with the player). Off: the whole stack goes in.")
                .define("absorbOnlyWhatIsNeeded", true);
        MASTERY_PER_HIT = b.comment("Mastery points a melee hit adds to the active form.").defineInRange("masteryPerHit", 1, 0, 100);
        MASTERY_PER_SKILL = b.comment("Mastery points a technique adds to the active form.").defineInRange("masteryPerSkill", 2, 0, 100);
        MASTERY_PER_BLOCK = b.comment("Mastery points a successful block adds to the active form.").defineInRange("masteryPerBlock", 1, 0, 100);
        TECHNIQUE_LOCK_TICKS = b.comment("Ticks after a technique during which the form cannot be changed (40 is two seconds).").defineInRange("techniqueLockTicks", 40, 0, 1200);
        ABSORB_PER_SECOND_LIMIT = b.comment("At most this many absorptions per player per second (a guard against packet spam).").defineInRange("absorbPerSecondLimit", 10, 1, 200);
        LEARN_EXPERIENCE = b.comment("Weapon experience for learning a form, multiplied by the form's tier (at least 1).").defineInRange("learnExperience", 40, 0, 100000);
        MASTERY_EXPERIENCE = b.comment("Weapon experience for finishing a form's mastery, multiplied by the form's tier (at least 1).").defineInRange("masteryExperience", 80, 0, 100000);
        CYCLE_CURSE_FORMS = b.comment("The cycle-form key passes through unlocked Curse Series forms. Off: it skips them (they can still be chosen in the Weapon Book).")
                .define("cycleCurseForms", true);
        ANNOUNCE = b.comment("Chat lines when a form is learned and a title when one is mastered.").define("announce", true);
        EFFECTS = b.comment("Sounds and effects when the gem absorbs and when a form is learned.").define("effects", true);
        BOOK_REFRESH_TICKS = b.comment("How often, in ticks, an open Weapon Book is compared with the server and refreshed if it differs (so also the least gap between two refreshes).").defineInRange("bookRefreshTicks", 10, 2, 100);
        b.pop();
        SPEC = b.build();
    }

    private FormsConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
