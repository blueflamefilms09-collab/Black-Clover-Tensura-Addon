package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.client.relics.worn.lib.WornCatalog;
import com.newuniverse.nusmp.client.relics.worn.lib.WornCoverage;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The self-test {@code relic.worn_render} (run by the CI server smoke test, see {@link SelfTests}): every registered relic that is worn on the body has a worn model that exists in
 * the jar (its own entry, the entry of its slot and look, or the default look of its slot), so no worn relic is invisible. It reads the same catalog the client reads and needs
 * no client class that touches the game, so it runs on a dedicated server.
 */
public final class RelicWornSelfTests {
    private RelicWornSelfTests() {}

    private static final String BASE = "/assets/nusmp/relic_worn/";

    public static void register() { SelfTests.register("relic.worn_render", server -> run()); }

    static SelfTests.Result run() throws IOException {
        List<RelicDef> all = Relics.all();
        if (all.isEmpty()) return SelfTests.Result.fail("no relic is registered");
        List<String> lines = new ArrayList<>();
        try (InputStream in = RelicWornSelfTests.class.getResourceAsStream(BASE + "catalog.txt")) {
            if (in == null) return SelfTests.Result.fail("the worn catalog is not in the jar");
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String l;
                while ((l = r.readLine()) != null) lines.add(l);
            }
        }
        List<WornCoverage.Entry> entries = new ArrayList<>(all.size());
        for (RelicDef d : all) entries.add(new WornCoverage.Entry(d.id(), d.slot().name(), d.look().name()));
        WornCoverage.Report rep = WornCoverage.check(entries, WornCatalog.parse(lines), name -> RelicWornSelfTests.class.getResource(BASE + name + ".txt") != null);
        if (rep.worn() == 0) return SelfTests.Result.fail("no relic is worn on the body?");
        if (!rep.ok()) return SelfTests.Result.fail(rep.problems().size() + " problems, first: " + rep.problems().get(0));
        return SelfTests.Result.ok(rep.summary());
    }
}
