package com.newuniverse.nusmp.elite.nations.contract;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Why: the loaded contract templates ("data nusmp/elite/nations/contracts/*.json"), swapped in whole by the reload listener so a board refill in the middle of a /reload sees the old set or the new set,
 * never half of one. Pure holder.
 */
public final class ContractTemplates {
    private ContractTemplates() {}

    private static volatile List<ContractTemplate> all = List.of();

    public static List<ContractTemplate> all() { return all; }

    public static void set(List<ContractTemplate> list) {
        List<ContractTemplate> copy = new ArrayList<>(list);
        copy.sort(Comparator.comparing(ContractTemplate::id));
        all = List.copyOf(copy);
    }

    public static ContractTemplate byId(String id) {
        for (ContractTemplate t : all) if (t.id().equals(id)) return t;
        return null;
    }
}
