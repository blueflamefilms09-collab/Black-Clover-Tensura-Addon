package com.newuniverse.nusmp.client.relics.worn.lib;

/**
 * Where the upgraded look of a relic comes from (1.106.0, the merge of the two upgrade slices). The lookup order is fixed:
 * <ol>
 *   <li>{@link #OVERRIDES}: the override table of the Charms / Genesis slice (assets/nusmp/visual_upgrade/overrides.txt, read by {@code client.relics.vu.VuModels});</li>
 *   <li>{@link #GOD_CATALOG}: the catalog of the God worn slice (assets/nusmp/visual_upgrade/catalog.txt, {@link UpgradeCatalog});</li>
 *   <li>{@link #NONE}: the older catalogs (relic_worn/catalog.txt, relic_hand/catalog.txt) answer, the look of 1.101.</li>
 * </ol>
 * One switch decides for both ({@code visual.upgrade}); a relic is in at most one of the two tables (a test checks it), so the order only settles a mistake. Pure.
 */
public enum UpgradeSource {
    NONE, OVERRIDES, GOD_CATALOG;

    /** The decision: switched off = {@link #NONE}; else the override table first, then the God catalog. */
    public static UpgradeSource choose(boolean enabled, boolean inOverrides, boolean inGodCatalog) {
        if (!enabled) return NONE;
        if (inOverrides) return OVERRIDES;
        return inGodCatalog ? GOD_CATALOG : NONE;
    }
}
