package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;

/**
 * Spirit Dive, Djinn-equip style: the sound of the forge. The Djinn Full Equip forges on with vanilla sounds sent from the server at the change of phase (amethyst resonance for the half equip,
 * a beacon and a sonic charge for the full equip, amethyst breaking when it ends: relics.djinn.DjinnEngine); the dive does the same at the same three moments, so every player in earshot hears it with
 * no packet of ours (the game's own sound packet, to the players who are near). The decisions are pure ({@link #cue}, {@link #pitch}, {@link #allowed}); {@link SpiritDiveSound#play} is the one place that touches
 * the level. At most {@link #MAX_SOUNDS} sounds a cue and one cue per player per {@link #COOLDOWN_TICKS} ticks (a player who toggles the dive again and again is silent in between), so the cost of a
 * dive is two sound packets at most, whatever anyone does. The server switch {@code spiritDive.forgeSound} turns it off. Nothing is kept in a static table: the last cue's tick is in the player's own data.
 */
public final class SpiritDiveForge {
    private SpiritDiveForge() {}

    /** The moments of a dive that sound. */
    public enum Kind { BEGIN, UPGRADE, END }

    /** One sound: its id in {@link SoundEvents} (the names the unit test checks), volume, and a pitch multiplier. */
    public record Cue(String sound, float volume, float pitchScale) {}

    public static final int MAX_SOUNDS = 3;
    public static final int COOLDOWN_TICKS = 20;
    /** The key in the player's persistent data that holds the game time of the last cue. */
    public static final String KEY = "nusmp_dive_cue";

    /** The pitch of an element: wind high, water clear, fire low, earth lowest. */
    public static float pitch(Element e) {
        return switch (e) {
            case WIND -> 1.3f;
            case WATER -> 1.0f;
            case FIRE -> 0.8f;
            case EARTH -> 0.6f;
        };
    }

    /** The sounds of a moment: the Djinn's own set, scaled to a dive. A half dive is the half equip's resonance; a full dive adds the beacon; the character tier a third, brighter note. */
    public static Cue[] cue(Kind kind, Stage stage, boolean character) {
        switch (kind) {
            case END:
                return new Cue[]{new Cue("AMETHYST_BLOCK_BREAK", 1.0f, 1.0f)};
            case UPGRADE:
                return new Cue[]{new Cue("AMETHYST_BLOCK_CHIME", 1.0f, 1.2f), new Cue("AMETHYST_BLOCK_RESONATE", 1.2f, 0.9f)};
            default:
                if (!SpiritDivePieces.full(stage)) return new Cue[]{new Cue("AMETHYST_BLOCK_RESONATE", 1.2f, 1.0f)};
                if (!character) return new Cue[]{new Cue("BEACON_ACTIVATE", 1.4f, 0.9f), new Cue("AMETHYST_BLOCK_RESONATE", 1.2f, 1.0f)};
                return new Cue[]{new Cue("BEACON_ACTIVATE", 1.8f, 0.8f), new Cue("AMETHYST_BLOCK_RESONATE", 1.4f, 1.0f), new Cue("BEACON_POWER_SELECT", 1.2f, 1.2f)};
        }
    }

    /** True when a player whose last cue was at {@code last} (0 = never) may be given another at game time {@code now}. */
    public static boolean allowed(long last, long now) { return last <= 0L || now - last >= COOLDOWN_TICKS || now < last; }
}
