package com.newuniverse.nusmp.heroweapon;

import java.util.List;
import java.util.Locale;

/**
 * Why: the catalogue of the 21 weapons of The Rising of the Shield Hero that the owner asked for (four Holy Weapons, seven Seven Star Weapons and ten more Vassal
 * Weapons), in ONE enum, because everything else is keyed by it: the item id, the forms file, the logic class, the 3D model, the lang keys. Entries are only
 * appended. The item id is the lower-case registry path ({@code legendary_shield}, {@code vassal_scythe} ...); the logic class of a weapon is found by
 * convention ({@link #logicClassName}), so a weapon builder owns exactly one package {@code heroweapon.weapon.<pkg>}. The holder is the canon wielder where the
 * owner's text names one (Naofumi, Ren, Motoyasu, Itsuki, L'Arc Berg, Glass, Fitoaria); the other holders vary and are stated by each weapon's doc.
 * Classes: see the fandom wiki's Vassal Weapons page (search summaries): Raphtalia's world has the Seven Star weapons (hammer, claw, axe, whip, staff,
 * gauntlet, projectile) and the carriage; Glass' world has fan, scythe, katana, book, mirror, musical instrument and harpoon (and a boat, which is not in the
 * owner's list); S'yne's world has the sewing set and the chain.
 */
public enum HeroKind {
    // ---- the four Holy (Cardinal) Weapons
    SHIELD("legendary_shield", "Legendary Shield", HeroClass.HOLY, HeroStyle.BLOCK, "Naofumi Iwatani", Sin.WRATH),
    SWORD("legendary_sword", "Legendary Sword", HeroClass.HOLY, HeroStyle.SWING, "Ren Amaki", Sin.GREED, Sin.GLUTTONY),
    SPEAR("legendary_spear", "Legendary Spear", HeroClass.HOLY, HeroStyle.SWING, "Motoyasu Kitamura", Sin.LUST),
    BOW("legendary_bow", "Legendary Bow", HeroClass.HOLY, HeroStyle.DRAW, "Itsuki Kawasumi", Sin.PRIDE),
    // ---- the Seven Star Weapons
    HAMMER("vassal_hammer", "Vassal Hammer", HeroClass.SEVEN_STAR, HeroStyle.SWING, ""),
    CLAW("vassal_claw", "Vassal Claw", HeroClass.SEVEN_STAR, HeroStyle.SWING, ""),
    AXE("vassal_axe", "Vassal Axe", HeroClass.SEVEN_STAR, HeroStyle.SWING, ""),
    WHIP("vassal_whip", "Vassal Whip", HeroClass.SEVEN_STAR, HeroStyle.SWING, ""),
    STAFF("vassal_staff", "Vassal Staff", HeroClass.SEVEN_STAR, HeroStyle.CAST, ""),
    GAUNTLET("vassal_gauntlet", "Vassal Gauntlet", HeroClass.SEVEN_STAR, HeroStyle.SWING, ""),
    PROJECTILE("vassal_projectile", "Vassal Projectile", HeroClass.SEVEN_STAR, HeroStyle.THROW, ""),
    // ---- the other Vassal Weapons
    CARRIAGE("vassal_carriage", "Vassal Carriage", HeroClass.VASSAL, HeroStyle.RIDE, "Fitoaria"),
    FAN("vassal_fan", "Vassal Fan", HeroClass.VASSAL, HeroStyle.SWING, "Glass"),
    SCYTHE("vassal_scythe", "Vassal Scythe", HeroClass.VASSAL, HeroStyle.SWING, "L'Arc Berg"),
    KATANA("vassal_katana", "Vassal Katana", HeroClass.VASSAL, HeroStyle.SWING, ""),
    BOOK("vassal_book", "Vassal Book", HeroClass.VASSAL, HeroStyle.CAST, ""),
    MIRROR("vassal_mirror", "Vassal Mirror", HeroClass.VASSAL, HeroStyle.CAST, ""),
    INSTRUMENT("vassal_instrument", "Vassal Musical Instrument", HeroClass.VASSAL, HeroStyle.CAST, ""),
    HARPOON("vassal_harpoon", "Vassal Harpoon", HeroClass.VASSAL, HeroStyle.THROW, ""),
    SEWING_SET("vassal_sewing_set", "Vassal Sewing Set", HeroClass.VASSAL, HeroStyle.CAST, ""),
    CHAIN("vassal_chain", "Vassal Chain", HeroClass.VASSAL, HeroStyle.SWING, "");

    private final String itemId, display, holder;
    private final HeroClass heroClass;
    private final HeroStyle style;
    private final List<Sin> sins;

    HeroKind(String itemId, String display, HeroClass heroClass, HeroStyle style, String holder, Sin... sins) {
        this.itemId = itemId;
        this.display = display;
        this.heroClass = heroClass;
        this.style = style;
        this.holder = holder;
        this.sins = List.of(sins);
    }

    /** The registry path of the item ({@code nusmp:<itemId>}); also the name of the forms file, the mesh and the textures. */
    public String itemId() { return itemId; }

    /** The English display name (the lang file carries the same). */
    public String display() { return display; }

    public HeroClass heroClass() { return heroClass; }

    public HeroStyle style() { return style; }

    /** The canon wielder named by the owner's text, or an empty string. */
    public String holder() { return holder; }

    /** The sins whose Curse Series canon ties to this weapon (empty for most vassals; a vassal builder may add its own series by design). */
    public List<Sin> sins() { return sins; }

    public boolean isHoly() { return heroClass == HeroClass.HOLY; }

    /** The lower-case package name of this weapon's logic: {@code heroweapon.weapon.<pkg>} (the enum name without underscores). */
    public String pkg() { return name().replace("_", "").toLowerCase(Locale.ROOT); }

    /** The class name of this weapon's logic, e.g. {@code SewingSetLogic}. */
    public String logicSimpleName() {
        StringBuilder b = new StringBuilder();
        for (String part : name().split("_")) b.append(part.charAt(0)).append(part.substring(1).toLowerCase(Locale.ROOT));
        return b.append("Logic").toString();
    }

    /** The fully qualified logic class, e.g. {@code com.newuniverse.nusmp.heroweapon.weapon.sewingset.SewingSetLogic}. */
    public String logicClassName() { return "com.newuniverse.nusmp.heroweapon.weapon." + pkg() + "." + logicSimpleName(); }

    /** The weapon with this item id (registry path, with or without the {@code nusmp:} prefix), or null. */
    public static HeroKind byItemId(String id) {
        if (id == null) return null;
        String path = id.startsWith("nusmp:") ? id.substring(6) : id;
        for (HeroKind k : values()) if (k.itemId.equals(path)) return k;
        return null;
    }

    /** The weapon with this enum name or item id (case-insensitive, {@code shield}, {@code legendary_shield}, {@code sewing_set}, {@code vassal_sewing_set}), or null. */
    public static HeroKind parse(String text) {
        if (text == null) return null;
        String t = text.trim().toLowerCase(Locale.ROOT);
        HeroKind byId = byItemId(t);
        if (byId != null) return byId;
        for (HeroKind k : values()) if (k.name().equalsIgnoreCase(t)) return k;
        return null;
    }
}
