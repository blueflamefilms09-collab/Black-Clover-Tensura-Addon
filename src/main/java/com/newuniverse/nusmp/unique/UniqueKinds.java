package com.newuniverse.nusmp.unique;

import com.newuniverse.nusmp.unique.kind.CharterUnique;
import com.newuniverse.nusmp.unique.kind.DragonBallUnique;
import com.newuniverse.nusmp.unique.kind.GreedUnique;
import com.newuniverse.nusmp.unique.kind.RelicUnique;
import com.newuniverse.nusmp.unique.kind.ShishouUnique;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Why: the one list of the kinds of 1-of-1 the guardian knows. The order matters: the kinds with an item class of their own come first and the relic kind (which owns every other unique relic) comes last, so an item is
 * never claimed by two kinds. Adding a kind is one line here plus its adapter. Server thread only (the list is copy-on-write only so a self-test can add a stand-in kind).
 */
public final class UniqueKinds {
    private UniqueKinds() {}

    public record Ident(UniqueKind kind, String id) {}

    private static final List<UniqueKind> KINDS = new CopyOnWriteArrayList<>(List.of(new GreedUnique(), new ShishouUnique(), new CharterUnique(), new DragonBallUnique(), new RelicUnique()));

    public static List<UniqueKind> all() { return KINDS; }

    /** Which unique is this stack a copy of, or null. An allocation only when it is one. */
    public static Ident identify(ItemStack s) {
        if (s == null || s.isEmpty()) return null;
        for (UniqueKind k : KINDS) {
            String id = k.idOf(s);
            if (id != null) return new Ident(k, id);
        }
        return null;
    }

    public static UniqueKind kindOfId(String id) {
        if (id == null) return null;
        for (UniqueKind k : KINDS) if (k.owns(id)) return k;
        return null;
    }

    /** A self-test stand-in kind goes first so it claims its own stacks. */
    static void addForTest(UniqueKind k) { KINDS.add(0, k); }

    static void removeForTest(UniqueKind k) { KINDS.remove(k); }
}
