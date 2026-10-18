package com.newuniverse.nusmp.elite.hub;

/**
 * Why: the quests and the shop are data that a reload rebuilds, and the game side reads them from many places (the Quest Giver's board, the kill hook, the purchase check, the sync). They live in
 * two volatile fields that the reload listener ({@link HubReload}) replaces in one assignment each, so a reader never sees half a catalog. Before the first reload both are empty and every reader
 * copes with that (an empty board, an empty shop).
 */
public final class HubCatalogs {
    private HubCatalogs() {}

    private static volatile QuestCatalog quests = QuestCatalog.EMPTY;
    private static volatile ShopTable shop = ShopTable.EMPTY;

    public static QuestCatalog quests() { return quests; }

    public static ShopTable shop() { return shop; }

    static void install(QuestCatalog q, ShopTable s) {
        quests = q;
        shop = s;
    }

    /** Test hook and server stop: back to empty. */
    public static void clear() {
        quests = QuestCatalog.EMPTY;
        shop = ShopTable.EMPTY;
    }
}
