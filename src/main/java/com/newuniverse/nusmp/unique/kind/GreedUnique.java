package com.newuniverse.nusmp.unique.kind;

import com.newuniverse.nusmp.greed.GreedComponents;
import com.newuniverse.nusmp.greed.GreedData;
import com.newuniverse.nusmp.greed.GreedItem;
import com.newuniverse.nusmp.greed.GreedService;
import com.newuniverse.nusmp.unique.UniqueKind;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Why: the Black Sword Greed is one item in the world, decided by the token in {@link GreedData}. The valid copy is the stack whose token is the world's. Back in the pool = the sword leaves the world exactly as its own
 * 14-day release and {@code /multiverse greed reset} do ({@code GreedService.release} for the wielder, then {@code GreedData.vanish}: no token, no wielder, the tier is kept), so every stack dissolves and the sword can enter
 * again through its normal sources.
 */
public final class GreedUnique implements UniqueKind {
    public static final String ID = "greed";

    @Override public String name() { return "greed"; }

    @Override public boolean owns(String id) { return ID.equals(id); }

    @Override public String idOf(ItemStack stack) { return stack.getItem() instanceof GreedItem ? ID : null; }

    @Override public boolean valid(MinecraftServer server, String id, ItemStack stack) { return GreedData.get(server).valid(GreedComponents.token(stack)); }

    @Override public boolean exists(MinecraftServer server, String id) { return GreedData.get(server).spawned(); }

    @Override public boolean release(MinecraftServer server, String id) {
        GreedData d = GreedData.get(server);
        if (!d.spawned()) return false;
        UUID owner = d.owner();
        if (owner != null) GreedService.release(server, owner, null, null);          // the wielder loses the skill and the devoured stats, as at the 14-day release
        d.vanish();
        return true;
    }

    @Override public String label(String id) { return "Black Sword Greed"; }

    @Override public String pool(String id) { return "A Crowned boss-tier monster may carry it again (greed crownedBearer), or an operator gives it (/multiverse greed give); the bare stack becomes the sword only while none exists."; }
}
