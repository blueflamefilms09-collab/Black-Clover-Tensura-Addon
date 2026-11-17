package com.newuniverse.nusmp.relics.djinn;

/**
 * The canon Belial's row (owner notes: Hakuryuu Ren's Djinn, a white scythe that severs the spirit, bat wings, bone), kept next to the old placeholder row that stays in
 * {@link Djinn#BELIAL}. {@link #on()} says which one is in force (server setting {@code belial.belialCanon}, default true); {@link Djinn}'s accessors answer with this row
 * while it is on and with the old row while it is off, so everything that reads a Djinn (engine, Extreme Magic, HUD, roster) follows without a second code path.
 *
 * <p><b>Affinity.</b> LIGHT: of the eight kinds of the Magi guide it is the only white, pale one, and it is drawn with the divine rays and sigils (a clean cut of light),
 * and it needs no new enum value (so no switch over Affinity can miss it). Colours: white bone 0xE8EEF2, teal-blue glow 0x58B8D8. No flames.
 * <p><b>Look.</b> Spike horns (bone), membrane wings (bat), no tail, no halo. The part set {@code djinn/belial_bone/} of the model worker is NOT hooked in: the loader
 * ({@code client.relics.djinn.DjinnLook#dir}) only knows {@code djinn/<id>/}, {@code u_<id>} and {@code d_<id>}.
 */
public final class BelialCanon {
    private BelialCanon() {}

    public static final String TITLE = "Severer of Spirits", FORM_NAME = "Pale-Scythe Maso";
    public static final Affinity AFFINITY = Affinity.LIGHT;
    public static final int COLOR = 0xE8EEF2, GLOW = 0x58B8D8;
    public static final Djinn.Look LOOK = new Djinn.Look(Djinn.Look.Horns.SPIKE, Djinn.Look.Wings.MEMBRANE, Djinn.Look.Tail.NONE, Djinn.Look.Halo.NONE, false);
    public static final Djinn.Trait TRAIT = new Djinn.Trait(Djinn.Special.SLICE, "minecraft:weakness", 60, 0, 0.30, 7.5,
            "Every blow is a thin pale cut: the foe's spirit bleeds and its strength fails.");
    public static final Djinn.Extreme EXTREME = new Djinn.Extreme("Belial Belior Zauto: Severance of the Spirit", Djinn.Shape.NOVA, 16.0, 30.0, "minecraft:weakness", 120, 1,
            "The pale scythe sweeps once round you: everything within reach is cut loose from its spirit, whatever guards it.");

    private static volatile Boolean forced;

    /** Is the canon Belial in force? A test may force it; otherwise the server setting (false while the config is not loaded). */
    public static boolean on() {
        Boolean f = forced;
        if (f != null) return f;
        try {
            return BelialConfig.canon();
        } catch (RuntimeException | LinkageError unavailable) {
            return false;
        }
    }

    /** For tests: true / false forces the switch, null gives it back to the setting. */
    public static void force(Boolean value) { forced = value; }
}
