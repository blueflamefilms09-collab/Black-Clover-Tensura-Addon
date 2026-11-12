package com.newuniverse.nusmp.devil;

/**
 * Why: the devils a player can call since 3.76.0, one row each: the world it comes from, its name and title, how strong it is (the tier decides the assimilation numbers of 3.48.0), the
 * attribute it lends (the combat effect, {@link DevilRules#effect}) and its colour. A plain array, not a list or a map (the devil package keeps no static collection). Ids are save keys:
 * appended only, never renamed. The source of each name is in docs/devil_worlds.md (rule 10).
 */
public record WorldDevil(String id, DevilWorld world, String name, String title, DevilTier tier, DevilAttribute attribute, int color) {

    private static final WorldDevil[] ALL = {
            // Black Clover: the devils of the underworld
            new WorldDevil("liebe", DevilWorld.BLACK_CLOVER, "Liebe", "the Anti-Magic devil", DevilTier.HIGH, DevilAttribute.ANTI_MAGIC, 0x2A2A2A),
            new WorldDevil("lucifero", DevilWorld.BLACK_CLOVER, "Lucifero", "king of devils, Gravity", DevilTier.SUPREME, DevilAttribute.GRAVITY, 0x5A1E8A),
            new WorldDevil("beelzebub", DevilWorld.BLACK_CLOVER, "Beelzebub", "ruler of the underworld, Spatial", DevilTier.SUPREME, DevilAttribute.SPATIAL, 0x3A4AC8),
            new WorldDevil("astaroth", DevilWorld.BLACK_CLOVER, "Astaroth", "ruler of the underworld, Time", DevilTier.SUPREME, DevilAttribute.TIME, 0xF5D76E),
            new WorldDevil("megicula", DevilWorld.BLACK_CLOVER, "Megicula", "the Curse-Warding devil", DevilTier.HIGH, DevilAttribute.CURSE, 0x8A2A5A),
            new WorldDevil("lucifugus", DevilWorld.BLACK_CLOVER, "Lucifugus", "the Kotodama devil", DevilTier.HIGH, DevilAttribute.WORD, 0xB06AFF),
            // Chainsaw Man: devils born of a fear
            new WorldDevil("fox_devil", DevilWorld.CHAINSAW_MAN, "the Fox Devil", "it bites when you say \"Kon\"", DevilTier.MID, DevilAttribute.BEAST, 0xE08A2A),
            new WorldDevil("curse_devil", DevilWorld.CHAINSAW_MAN, "the Curse Devil", "paid in years of life", DevilTier.HIGH, DevilAttribute.CURSE, 0x4A1A6A),
            new WorldDevil("future_devil", DevilWorld.CHAINSAW_MAN, "the Future Devil", "it lives in your right eye", DevilTier.HIGH, DevilAttribute.FORESIGHT, 0x3AC8C8),
            new WorldDevil("ghost_devil", DevilWorld.CHAINSAW_MAN, "the Ghost Devil", "its hands reach from nowhere", DevilTier.MID, DevilAttribute.GHOST, 0x9AA8C8),
            // Black Butler
            new WorldDevil("sebastian", DevilWorld.BLACK_BUTLER, "Sebastian Michaelis", "one hell of a butler", DevilTier.SUPREME, DevilAttribute.SOUL, 0x7A3CC8),
            // Blue Exorcist: kin of the eight Demon Kings of Gehenna
            new WorldDevil("kin_of_iblis", DevilWorld.BLUE_EXORCIST, "a kin of Iblis", "King of Fire", DevilTier.MID, DevilAttribute.FLAME, 0xFF6A2A),
            new WorldDevil("kin_of_egyn", DevilWorld.BLUE_EXORCIST, "a kin of Egyn", "King of Water", DevilTier.MID, DevilAttribute.WATER, 0x3A8AE8),
            new WorldDevil("kin_of_amaimon", DevilWorld.BLUE_EXORCIST, "a kin of Amaimon", "King of Earth", DevilTier.MID, DevilAttribute.EARTH, 0x8A6A3A),
            new WorldDevil("kin_of_astaroth", DevilWorld.BLUE_EXORCIST, "a kin of Astaroth", "King of Rot", DevilTier.LOW, DevilAttribute.ROT, 0x6A8A2A),
            new WorldDevil("kin_of_lucifer", DevilWorld.BLUE_EXORCIST, "a kin of Lucifer", "King of Light", DevilTier.HIGH, DevilAttribute.LIGHT, 0xFFF0A8),
            // Tensura: daemons of the seven Primordial lines
            new WorldDevil("daemon_noir", DevilWorld.TENSURA, "a daemon of Noir's line", "the black Primordial's kin", DevilTier.SUPREME, DevilAttribute.CURSE, 0x1A1A24),
            new WorldDevil("daemon_blanc", DevilWorld.TENSURA, "a daemon of Blanc's line", "the white Primordial's kin", DevilTier.HIGH, DevilAttribute.LIGHT, 0xF0F0FF),
            new WorldDevil("daemon_jaune", DevilWorld.TENSURA, "a daemon of Jaune's line", "the yellow Primordial's kin", DevilTier.HIGH, DevilAttribute.FLAME, 0xF0D030),
            new WorldDevil("daemon_violet", DevilWorld.TENSURA, "a daemon of Violet's line", "the purple Primordial's kin", DevilTier.HIGH, DevilAttribute.ROT, 0x8A3AC8),
            new WorldDevil("daemon_rouge", DevilWorld.TENSURA, "a daemon of Rouge's line", "the red Primordial's kin", DevilTier.SUPREME, DevilAttribute.SOUL, 0xC81E2D),
            new WorldDevil("daemon_vert", DevilWorld.TENSURA, "a daemon of Vert's line", "the green Primordial's kin", DevilTier.HIGH, DevilAttribute.EARTH, 0x3AA85A),
            new WorldDevil("daemon_bleu", DevilWorld.TENSURA, "a daemon of Bleu's line", "the blue Primordial's kin", DevilTier.HIGH, DevilAttribute.WATER, 0x3A6AE8),
            // Dororo
            new WorldDevil("hall_demon", DevilWorld.DORORO, "a demon of the Hall of Hell", "it grants prosperity for a body", DevilTier.HIGH, DevilAttribute.BEAST, 0x8A6A3A),
            // Berserk
            new WorldDevil("god_hand", DevilWorld.BERSERK, "the God Hand", "answering the Behelit", DevilTier.SUPREME, DevilAttribute.CAUSALITY, 0x8A0A14),
            // Fairy Tail
            new WorldDevil("etherious", DevilWorld.FAIRY_TAIL, "an Etherious", "a demon of the Books of Zeref", DevilTier.HIGH, DevilAttribute.ETHER, 0x6A3CFF),
    };

    public static int count() { return ALL.length; }

    public static WorldDevil at(int i) { return ALL[i]; }

    /** The devil with this id, or null. */
    public static WorldDevil byId(String id) {
        if (id != null) for (WorldDevil d : ALL) if (d.id.equalsIgnoreCase(id)) return d;
        return null;
    }

    /** How many devils a world has. */
    public static int countOf(DevilWorld w) {
        int n = 0;
        for (WorldDevil d : ALL) if (d.world == w) n++;
        return n;
    }

    /** The lasting price this devil takes when the contract is signed (a bit mask of {@link Sacrifice#bit}); 0 for the worlds whose ritual is the whole price. */
    public int priceMask() {
        return switch (world) {
            case CHAINSAW_MAN -> switch (id) {
                case "fox_devil" -> Sacrifice.FLESH.bit();
                case "curse_devil" -> Sacrifice.HALF_HEALTH.bit();
                default -> Sacrifice.LOST_EYE_RIGHT.bit();                                  // the Future Devil lives in the right eye; the Ghost Devil took Himeno's right eye
            };
            case BLACK_BUTLER -> Sacrifice.LOST_EYE_RIGHT.bit() | Sacrifice.SOUL_DEBT.bit();
            case DORORO -> Sacrifice.LOST_EYE_LEFT.bit() | Sacrifice.FLESH.bit();
            case BERSERK -> Sacrifice.HALF_HEALTH.bit() | Sacrifice.BRANDED.bit();
            default -> 0;
        };
    }
}
