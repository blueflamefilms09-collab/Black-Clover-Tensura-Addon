package com.newuniverse.nusmp.aura;

/**
 * 0.53: one player render layer slot per attribute of the Black Clover Magic and VFX expansion (see PlayerAuras). New slots only at the end.
 */
public enum Aura {
    BEAST,
    DEMON_BEAST,
    BODY,
    BLOOD,
    BONE,
    EYE,
    EYEBALL,
    CURSE,
    CURSE_WARDING,
    DEMON_FIRE,
    DEMON_ICE,
    DEMON_LIGHT,
    DEMON_WATER,
    GRAVITY,
    SEALING,
    BARRIER,
    IMITATION,
    KEY,
    CHAIN,
    BUTOH,
    BRIAR,
    CHERRY_BLOSSOM,
    FUNGUS,
    FOOD,
    CRYSTAL,
    CORUNDUM,
    BRONZE,
    COPPER,
    IRON,
    BLACK_OIL,
    GEL,
    GLASS,
    BUBBLE,
    ICE,
    ICE_WEDGE,
    LEGION,
    LIGHT,
    PAINTING,
    KOTODAMA,
    WORLD_TREE,
    DICE,
    SLASH,
    COMPASS,
    MERCURY,
    /** 1.27.0: the merged-magic armour; RETIRED in 1.93.0 (no painter is registered for it any more; the constant stays so the ordinals after it never move) */
    MERGED,
    /** 1.27.0: the temporary armour / aura of the 30 wiki magics; the style is the magic's WikiStyles index (client.aura.WikiAura) */
    WIKI,
    /** 1.93.0: the rebuilt temporary armour of Shakudo, Scale, Spike, Nail and Hair Magic (client.aura.<Name>Aura, pieces drawn by ShakudoPieceKit) */
    SHAKUDO,
    SCALE,
    SPIKE,
    NAIL,
    HAIR,
    /** 1.93.0 magic rebuild: the temporary armour of Song, Sound, Soul, Soul Corpse and Memory Magic (client.aura.<Name>Aura, drawn by SongArmourKit) */
    SONG,
    SOUND,
    SOUL,
    SOUL_CORPSE,
    MEMORY,
    /** 1.93.0 rebuild: the temporary armour sets of Smoke, Snow, Wing, Vortex, Air and Ash Magic (client.aura.<Name>Aura, pieces forged in by EquipAnimation) */
    SMOKE,
    SNOW,
    WING,
    VORTEX,
    AIR,
    ASH,
    /** 1.93.0 magic rebuild: the temporary armour of Spatial, Mirror and Permeation Magic (client.aura.SpatialAura / MirrorAura / PermeationAura) */
    SPATIAL,
    MIRROR,
    PERMEATION,
    /** 1.93.0 magic rebuild: the temporary armours of Transmutation, Recombination and Modification (Mercury uses MERCURY above) */
    TRANSMUTATION,
    RECOMBINATION,
    MODIFICATION,
    /** 1.93.0: the rebuilt temporary armours of Water, Dark, Lightning and Flame Magic (client.aura.WaterAura / DarkAura / LightningAura / FlameAura, pieces forged by EquipAnimation) */
    WATER,
    DARK,
    LIGHTNING,
    FLAME,
    /** 1.93.0 nature-magic rebuild: the temporary armours of Tree, Vine, Plant, Poison Plant, Mud and Mucus Magic (client.aura.TreeAura and its siblings) */
    TREE,
    VINE,
    PLANT,
    POISON_PLANT,
    MUD,
    MUCUS,
    /** Fairy Tail skills: the temporary armour of the timed forms (Dragon Force, Ice Devil's Rage, Iron Dragon Scales ...); the style is the form index (fairytail.FtForms), drawn by client.aura.FairyTailAura */
    FAIRY_TAIL,
    /** Requip: The Knight (Erza Scarlet): the armour render layer; the style is the worn armour's ordinal in requip.Armour (client.aura.ErzaAura, pieces forged in by EquipAnimation) */
    ERZA,
    /** 1.99.0 Great Spirits: the Spirit Dive of Wind, Water, Fire and Earth; the style is book.SpiritDivePieces.style(element, stage) (client.aura.SpiritDiveAura, pieces forged in by EquipAnimation) */
    SPIRIT_DIVE,
    /** Spirit Dive remake: Fuegoleon's flaming right arm while the Salamander is his; the style is book.FuegoleonArmRules.STYLE (client.aura.FuegoleonArmAura; also drawn in first person by client.aura.FireArmFirstPerson) */
    FUEGOLEON_ARM
}
