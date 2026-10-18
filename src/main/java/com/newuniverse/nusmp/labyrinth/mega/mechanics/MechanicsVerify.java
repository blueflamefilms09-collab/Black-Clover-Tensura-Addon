package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: one verification of the LOADED library that the admin command {@code verify} and the in-game self-test {@code mech.library} share, so what an operator is told and what CI
 * checks can never differ. It answers: did the files parse cleanly, does the library hold exactly the number of mechanics the generator wrote (and the same per category), does
 * every mechanic pass the catalogue, are signatures and names unique, and does every primitive of the catalogue have an executor and every executor an entry. Returns the list of
 * problems; empty means the library is sound.
 */
public final class MechanicsVerify {
    private MechanicsVerify() {}

    public static List<String> run() {
        List<String> out = new ArrayList<>();
        OpCatalog cat = MechanicLibrary.catalog();
        if (cat == null) {
            out.add("the catalogue (ops.json) is not loaded");
            return out;
        }
        out.addAll(MechanicLibrary.problems());
        List<MechanicDef> all = MechanicLibrary.all();
        if (cat.total() > 0 && all.size() != cat.total()) out.add("the library holds " + all.size() + " mechanics but the generator wrote " + cat.total());
        Map<String, Integer> per = new HashMap<>();
        Set<String> sigs = new HashSet<>(), names = new HashSet<>();
        for (MechanicDef d : all) {
            per.merge(d.category().id(), 1, Integer::sum);
            out.addAll(cat.check(d));
            String sig = Signature.of(d, cat);
            if (!sigs.add(sig)) out.add(d.id() + ": same behaviour signature as another mechanic");
            if (!names.add(d.name().toLowerCase())) out.add(d.id() + ": same name as another mechanic");
            if (!d.sig().isEmpty() && !d.sig().equals(Signature.hash(sig))) out.add(d.id() + ": stored signature hash is stale");
        }
        for (Map.Entry<String, Integer> e : cat.counts().entrySet()) {
            int have = per.getOrDefault(e.getKey(), 0);
            if (have != e.getValue()) out.add("category " + e.getKey() + " holds " + have + " but the generator wrote " + e.getValue());
        }
        for (String op : cat.ops().keySet()) if (Prims.get(op) == null) out.add("primitive '" + op + "' has no executor");
        for (String op : Prims.ids()) if (cat.op(op) == null) out.add("executor '" + op + "' is not in the catalogue");
        return out;
    }
}
