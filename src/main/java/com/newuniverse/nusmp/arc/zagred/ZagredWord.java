package com.newuniverse.nusmp.arc.zagred;

/**
 * Why: the whole point of the remade Zagred is that every Word he speaks is a command the party can READ and ANSWER, so each Word needs one place that
 * says what the players are shown (the big word on their screen and the one-line counter under it), what colour it is written in and how it behaves
 * towards the Tensura-style rules of the mod (is it a hard control that must go through the control law, is it a plain blow a shield can stop, does
 * an anti-magic holder shrug it off). The boss class, the on-screen text, the verdicts and the tests all read this table, so the words, the counters
 * and the language keys cannot drift apart. It is pure (no game types) and only ever APPENDED to, because the ids appear in quest arguments, language
 * keys and the docs.
 *
 * <p>These are the names of Zagred's Kotodama words as they stand in {@code book/KotodamaWords}; the fight here is his own use of them, not the
 * players' spells, and it does not call the players' spell code (that stays untouched).
 */
public enum ZagredWord {
    /** The bind: a ring on the floor closes in; the band between the ring and his side is bound. */
    HALT("halt", "HALT", "Leave the ring, or stand close to him", 0x8A4CFF, true, false, true),
    /** Gravity: fliers are thrown down and a low crushing wave runs out from his feet. */
    FALL("fall", "FALL", "Jump the wave", 0x3A2A9A, false, false, false),
    /** The otherworldly trident, a lance along a lane written on the floor. */
    TRIDENT("trident", "TRIDENT", "Leave the line, or raise your shield", 0xD8C4FF, false, true, false),
    /** A storm of demon swords on marked spots. */
    SWORDS("swords", "SWORDS", "Raise your shield, or leave the marks", 0xF2194D, false, true, false),
    /** A chain of glyphs at the strongest caster: the first body on the line takes it, a wall breaks it. */
    SEAL("seal", "SEAL", "Break his sight, or hold anti-magic", 0x5B4BD6, true, false, true),
    /** The sentence of letters on the floor that detonate in reading order. */
    REDACT("redact", "REDACT", "Step between the letters; anti-magic cuts the sentence", 0xF2194D, false, false, false),
    /** A dome that erases every projectile for a moment. */
    SHATTER("shatter", "SHATTER", "Hold your fire: projectiles are erased", 0xB8A0FF, false, false, false),
    /** A window in which a share of every blow is turned back on the one who struck. */
    REVERSE("reverse", "REVERSE", "Hold your blows: they come back", 0xD8C4FF, false, false, false),
    /** A shove that clears a crowd from around him. */
    BANISH("banish", "BANISH", "Do not crowd him", 0x8A4CFF, false, false, true),
    /** Marks players one by one with a word of their own to answer. */
    VERDICT("verdict", "VERDICT", "Answer the word written over your head", 0xF2194D, false, false, false),
    /** The ultimate: a rule written over the arena, held by stones. */
    OVERWRITE("overwrite", "OVERWRITE", "Break three of the five stones", 0xF2194D, false, false, false),
    /** Only ever a verdict: terror, answered by meeting his gaze. */
    COWER("cower", "COWER", "Meet his gaze", 0x3A2A9A, true, false, true);

    private final String id, title, hint;
    private final int rgb;
    private final boolean hardControl, physical, antiMagicExempt;

    ZagredWord(String id, String title, String hint, int rgb, boolean hardControl, boolean physical, boolean antiMagicExempt) {
        this.id = id;
        this.title = title;
        this.hint = hint;
        this.rgb = rgb;
        this.hardControl = hardControl;
        this.physical = physical;
        this.antiMagicExempt = antiMagicExempt;
    }

    /** Lower-case id used in quest arguments, lang keys and the move table. */
    public String id() { return id; }

    /** The word as shown on the screen (English; the language file may translate it). */
    public String title() { return title; }

    /** The one-line counter shown under the word (English; the language file may translate it). */
    public String hint() { return hint; }

    /** The colour (0xRRGGBB) of its ring and its text. */
    public int rgb() { return rgb; }

    /** The same colour as an opaque ARGB for the effect packets. */
    public int argb() { return 0xFF000000 | rgb; }

    /** True when the word binds or silences: it may only land through {@code BalanceLaw.beginControl} (one hard control per twelve seconds). */
    public boolean hardControl() { return hardControl; }

    /** True when the blow is a plain attack, so a raised shield stops it (the "item" answer). */
    public boolean physical() { return physical; }

    /** True when someone holding anti-magic is not touched by it. */
    public boolean antiMagicExempt() { return antiMagicExempt; }

    /** The language key of the title ({@code message.nusmp.zagred.word.<id>}). */
    public String titleKey() { return "message.nusmp.zagred.word." + id; }

    /** The language key of the counter line. */
    public String hintKey() { return titleKey() + ".hint"; }

    /** The word with this id, or null. */
    public static ZagredWord byId(String id) {
        if (id == null) return null;
        for (ZagredWord w : values()) if (w.id.equals(id)) return w;
        return null;
    }
}
