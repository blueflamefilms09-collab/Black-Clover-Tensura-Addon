package com.newuniverse.nusmp.unique.kind;

import com.newuniverse.nusmp.shishou.ShishouCap;
import com.newuniverse.nusmp.shishou.ShishouComponents;
import com.newuniverse.nusmp.shishou.ShishouData;
import com.newuniverse.nusmp.shishou.ShishouGrantService;
import com.newuniverse.nusmp.shishou.ShishouItem;
import com.newuniverse.nusmp.shishou.ShishouLive;
import com.newuniverse.nusmp.unique.UniqueKind;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

/**
 * Why: Shishou, the talking sword, is one thing in the world ({@link ShishouCap}): a flying entity, a dormant one, a thrown minion or the item. Only the item form is a copy this guardian watches; while the sword is any
 * other form {@link #exists} is false (a thrown sword is not lost). The valid copy is the stack with the current token. Back in the pool = the same step as {@code /multiverse shishou reset}: every body and stack becomes a
 * ghost and dissolves, the wielder is forgotten, his level, skills and Stomach are kept. Shishou's own recovery (the void, lava, a thief, a killed minion) still runs first and is untouched.
 */
public final class ShishouUnique implements UniqueKind {
    public static final String ID = "shishou";

    @Override public String name() { return "shishou"; }

    @Override public boolean owns(String id) { return ID.equals(id); }

    @Override public String idOf(ItemStack stack) { return stack.getItem() instanceof ShishouItem ? ID : null; }

    @Override public boolean valid(MinecraftServer server, String id, ItemStack stack) { return ShishouData.get(server).cap().validStack(ShishouComponents.token(stack)); }

    @Override public boolean exists(MinecraftServer server, String id) { return ShishouData.get(server).cap().form() == ShishouCap.Form.HELD; }

    @Override public boolean release(MinecraftServer server, String id) {
        ShishouData d = ShishouData.get(server);
        if (d.cap().form() != ShishouCap.Form.HELD) return false;
        d.cap().reset();
        d.touch();
        ShishouLive.clearAll();
        ShishouGrantService.syncAll(server);                                           // nobody is the wielder now: the Tensura skill goes
        return true;
    }

    @Override public String label(String id) { return "Shishou"; }

    @Override public String pool(String id) { return "A wild Shishou may appear near a random player at a small daily chance (shishou natural), or an operator wakes him (/multiverse shishou spawn)."; }
}
