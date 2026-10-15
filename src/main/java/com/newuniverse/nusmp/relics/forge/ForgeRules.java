package com.newuniverse.nusmp.relics.forge;

/**
 * Why: the Relic Forge is a second doorway to rules that already exist (the tier ritual and the sockets), so this class holds NO numbers of those
 * rules; it only decides what the table itself must decide: which stack may sit in which slot, which of the five buttons is available for what is
 * in the slots, which refusal line the player is told, and the small engraving rules the table adds (a cap on engravings the forge itself adds, no
 * silent replacement, no conflicting pair). It has no Minecraft types on purpose, so every decision is unit-tested; the menu asks the real world
 * (is this a Spirit Core? how many sockets?) and hands this class plain booleans and ints, so the server is the only place anything is decided and the
 * client screen only mirrors the answers through synced container data.
 */
public final class ForgeRules {
    private ForgeRules() {}

    /** Menu slot indexes (the player's inventory follows). */
    public static final int SLOT_RELIC = 0, SLOT_CATALYST_A = 1, SLOT_CATALYST_B = 2, SLOT_CORE = 3, SLOT_PREVIEW = 4, TABLE_SLOTS = 5;

    /** The five buttons, by the id the client sends to {@code clickMenuButton}. Entries are only appended. */
    public enum Button {
        UPGRADE, SOCKET, DRAW_OUT, CYCLE, ENGRAVE;

        public int id() { return ordinal(); }

        /** The button of an id, or null for an id nobody sends (the menu ignores it). */
        public static Button of(int id) { return id < 0 || id >= values().length ? null : values()[id]; }
    }

    /** What the item in the core slot is. */
    public enum CoreKind { NONE, SOCKET_CORE, EMPTY_CORE, ENGRAVING_BOOK, OTHER }

    /** What the item in the relic slot is: a relic, a mod weapon that is not a relic, or something else. */
    public enum Subject { NONE, RELIC, WEAPON, OTHER }

    /**
     * Every line the table can show, in the Great Sage's voice (lang key {@code nusmp.forge.msg.<lower name>}). OK is "nothing to refuse". The tier
     * ritual's own refusals are mapped onto the UPGRADE_* entries by the menu so the screen can show them inside the window.
     */
    public enum Msg {
        OK, NO_ITEM, NOT_ALLOWED, NO_SOCKETS, SOCKETS_FULL, NEED_SOCKET_CORE, NEED_EMPTY_CORE, NOTHING_BOUND,
        NO_CATALYST, AT_TOP, NEED_BOOK, NOT_A_WEAPON, NOT_AN_ENGRAVING, ENGRAVING_CAP, ENGRAVING_PRESENT, ENGRAVING_CONFLICT,
        UPGRADE_OK, UPGRADE_COOLDOWN, UPGRADE_UNATTUNED, UPGRADE_WRONG_CATALYST, UPGRADE_NEED_COUNT, UPGRADE_NO_ENERGY, UPGRADE_FAILED_KEPT, UPGRADE_FAILED_LOST,
        SOCKET_REFUSED, SOCKET_OK, DRAWN_OUT, ENGRAVED;

        public String langKey() { return "nusmp.forge.msg." + name().toLowerCase(java.util.Locale.ROOT); }

        public static Msg of(int ordinal) { return ordinal < 0 || ordinal >= values().length ? OK : values()[ordinal]; }

        /** True for the lines that report a thing done (shown green), false for refusals and failures. */
        public boolean good() { return this == OK || this == UPGRADE_OK || this == SOCKET_OK || this == DRAWN_OUT || this == ENGRAVED; }
    }

    // ---------------------------------------------------------------- slot acceptance
    /** The relic slot takes any non-consumable relic, and a mod weapon (for the engraving panel). */
    public static boolean acceptsSubject(boolean relicNotConsumable, boolean modWeapon) { return relicNotConsumable || modWeapon; }

    /** A catalyst slot takes only the items some tier jump accepts as a catalyst. */
    public static boolean acceptsCatalyst(boolean isCatalystItem) { return isCatalystItem; }

    /** The core slot takes a Spirit/Devil Core, the Empty Element Core and an engraving book. */
    public static boolean acceptsCore(CoreKind k) { return k == CoreKind.SOCKET_CORE || k == CoreKind.EMPTY_CORE || k == CoreKind.ENGRAVING_BOOK; }

    // ---------------------------------------------------------------- the table's picture of its slots
    /**
     * @param subject       what the relic slot holds
     * @param atTop         the relic already stands at the top grade (only meaningful for a RELIC)
     * @param catalystStacks how many of the two catalyst slots hold something
     * @param sockets       sockets in use
     * @param socketTotal   sockets the relic has
     * @param core          what the core slot holds
     * @param cycle         the socket index CYCLE has chosen
     * @param forgeEngravings engravings this table has already added to the weapon
     * @param bookEngravings  engravings on the book in the core slot (0 if none)
     * @param engravable    the item in the relic slot is one of the mod's weapons (a relic weapon counts; a relic ring or armour does not)
     */
    public record State(Subject subject, boolean atTop, int catalystStacks, int sockets, int socketTotal, CoreKind core, int cycle,
                        int forgeEngravings, int bookEngravings, boolean engravable) {}

    /** The refusal for pressing a button with this state, or {@link Msg#OK} when the button may be pressed. The per-rule detail (cooldown, attunement, energy) is the ritual's own. */
    public static Msg check(Button b, State s) {
        if (b == null) return Msg.NOT_ALLOWED;
        switch (b) {
            case UPGRADE -> {
                if (s.subject() != Subject.RELIC) return s.subject() == Subject.NONE ? Msg.NO_ITEM : Msg.NOT_ALLOWED;
                if (s.atTop()) return Msg.AT_TOP;
                if (s.catalystStacks() <= 0) return Msg.NO_CATALYST;
                return Msg.OK;
            }
            case SOCKET -> {
                if (s.subject() != Subject.RELIC) return s.subject() == Subject.NONE ? Msg.NO_ITEM : Msg.NOT_ALLOWED;
                if (s.socketTotal() <= 0) return Msg.NO_SOCKETS;
                if (s.core() != CoreKind.SOCKET_CORE) return Msg.NEED_SOCKET_CORE;
                if (s.sockets() >= s.socketTotal()) return Msg.SOCKETS_FULL;
                return Msg.OK;
            }
            case DRAW_OUT -> {
                if (s.subject() != Subject.RELIC) return s.subject() == Subject.NONE ? Msg.NO_ITEM : Msg.NOT_ALLOWED;
                if (s.core() != CoreKind.EMPTY_CORE) return Msg.NEED_EMPTY_CORE;
                if (s.sockets() <= 0) return Msg.NOTHING_BOUND;
                return Msg.OK;
            }
            case CYCLE -> {
                if (s.subject() != Subject.RELIC) return s.subject() == Subject.NONE ? Msg.NO_ITEM : Msg.NOT_ALLOWED;
                return s.sockets() > 0 ? Msg.OK : Msg.NOTHING_BOUND;
            }
            case ENGRAVE -> {
                if (s.subject() == Subject.NONE) return Msg.NO_ITEM;
                if (!s.engravable()) return Msg.NOT_A_WEAPON;
                if (s.core() != CoreKind.ENGRAVING_BOOK) return Msg.NEED_BOOK;
                if (s.bookEngravings() <= 0) return Msg.NOT_AN_ENGRAVING;
                return engraveCap(s.forgeEngravings(), s.bookEngravings());
            }
        }
        return Msg.NOT_ALLOWED;
    }

    /** Is the button available (the screen greys it out otherwise)? */
    public static boolean enabled(Button b, State s) { return check(b, s) == Msg.OK; }

    /** The bit mask of enabled buttons: bit {@code id} set when that button may be pressed (synced to the client as one number). */
    public static int enabledMask(State s) {
        int m = 0;
        for (Button b : Button.values()) if (enabled(b, s)) m |= 1 << b.id();
        return m;
    }

    // ---------------------------------------------------------------- sockets: cycling
    /** The next socket index after {@code cycle} among {@code used} bound Cores (0 when none; wraps). */
    public static int cycleNext(int cycle, int used) { return used <= 0 ? 0 : (Math.max(0, cycle) + 1) % used; }

    /** A cycle index kept inside the bound Cores (a Core was drawn out, the relic was swapped). */
    public static int clampCycle(int cycle, int used) { return used <= 0 ? 0 : Math.max(0, Math.min(cycle, used - 1)); }

    // ---------------------------------------------------------------- engravings
    /**
     * The most engravings the FORGE adds to one weapon. The mod has no cap of its own (a lore engraving list is applied once and is never counted
     * here, so a weapon that came with five is not locked out), so the table brings one: three.
     */
    public static final int ENGRAVING_CAP = 3;

    /** OK, or ENGRAVING_CAP when adding {@code adding} more would pass the cap. */
    public static Msg engraveCap(int alreadyAdded, int adding) {
        return alreadyAdded + Math.max(0, adding) > ENGRAVING_CAP ? Msg.ENGRAVING_CAP : Msg.OK;
    }

    /** The detailed check once the real enchantments are known: the same engraving is never replaced (even by a higher level), and an incompatible pair is refused. */
    public static Msg engraveEntry(boolean weaponHasIt, boolean compatible) {
        if (weaponHasIt) return Msg.ENGRAVING_PRESENT;
        if (!compatible) return Msg.ENGRAVING_CONFLICT;
        return Msg.OK;
    }

    /** The level that is put on: the book's, never above the engraving's own maximum, never below 1. */
    public static int engraveLevel(int bookLevel, int maxLevel) { return Math.max(1, Math.min(bookLevel, Math.max(1, maxLevel))); }
}
