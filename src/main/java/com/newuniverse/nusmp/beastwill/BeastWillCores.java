package com.newuniverse.nusmp.beastwill;

import java.util.List;
import java.util.function.Predicate;

/**
 * Why: the creative tab is built on the client, where the data-driven {@link BeastWillCatalog} does not exist (it is loaded on the server), so the 27 cores need a static table: the will id, the beast's name and the
 * will's own lowest rank (so the rank gate of {@link BeastWillAcquire#check} passes and the ask only greys for EP, the will cap or race). A pure test pins every row to its data file. A bare core (no component, e.g. a
 * plain /give) takes the first will of this table the player does not hold yet ({@link #firstFree}). Pure: no game class.
 */
public final class BeastWillCores {
    private BeastWillCores() {}

    public record Entry(String will, String beast, BeastRank rank) {}

    public static final List<Entry> ALL = List.of(
            new Entry("ammit_will", "Ammit, Devourer of the Dead", BeastRank.S),
            new Entry("apophis_will", "Apophis", BeastRank.SS),
            new Entry("cerberus_will", "Cerberus, Three-Headed Hound of Hades", BeastRank.S),
            new Entry("crested_boar_will", "Crested Boar Elder", BeastRank.A),
            new Entry("cu_sith_will", "Cu Sith, the Spectral Green Hound of the Moors", BeastRank.A),
            new Entry("dragon_will", "Mana Dragon", BeastRank.SS),
            new Entry("elderwood_guardian_will", "Elderwood Guardian", BeastRank.S),
            new Entry("fenrir_will", "Fenrir, the chained wolf", BeastRank.SS),
            new Entry("flame_serpent_will", "Flame-Serpent Elder", BeastRank.A),
            new Entry("hafgufa_will", "Hafgufa, the Island-Sized Sea Monster", BeastRank.S),
            new Entry("hydra_will", "Lernaean Hydra", BeastRank.A),
            new Entry("jormungandr_will", "Jormungandr, the Midgard Serpent", BeastRank.S),
            new Entry("kitsune_will", "Kitsune, the Nine-Tailed Fox Spirit", BeastRank.S),
            new Entry("kraken_will", "Kraken, the Fleet-Dragger", BeastRank.S),
            new Entry("lion_will", "World Lion", BeastRank.A),
            new Entry("minotaur_will", "Asterion the Minotaur of the Labyrinth", BeastRank.A),
            new Entry("nidhoggr_will", "Nidhoggr, Gnawer of the World-Tree Roots", BeastRank.S),
            new Entry("nue_will", "Nue, the Chimera of Dread", BeastRank.A),
            new Entry("pegasus_will", "Pegasus, the Divine Winged Stallion", BeastRank.A),
            new Entry("ridge_wolf_will", "Ridge-Wolf Elder", BeastRank.A),
            new Entry("scylla_charybdis_will", "Scylla and Charybdis, the Strait Pair", BeastRank.A),
            new Entry("shadow_panther_will", "Shadow Panther", BeastRank.A),
            new Entry("sphinx_will", "The Sphinx of the Riddle", BeastRank.S),
            new Entry("thunder_fang_chimera_will", "Thunder-Fang Chimera", BeastRank.S),
            new Entry("typhon_will", "Typhon, the Deadliest Monster", BeastRank.SS),
            new Entry("wind_skimmer_will", "Gale Skimmer Elder", BeastRank.A),
            new Entry("yamata_no_orochi_will", "Yamata no Orochi, the Eight-Headed Dragon", BeastRank.SS));

    /** The first entry whose will the player does not hold ({@code held} answers by will id); the first entry when they hold every one, so a bare core still opens an ask that names its reason. */
    public static Entry firstFree(Predicate<String> held) {
        for (Entry e : ALL) if (!held.test(e.will())) return e;
        return ALL.get(0);
    }
}
