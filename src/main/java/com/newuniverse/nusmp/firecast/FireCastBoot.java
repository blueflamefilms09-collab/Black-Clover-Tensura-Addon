package com.newuniverse.nusmp.firecast;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

/**
 * Why: the fire casting styles (Magna Swing, Mereoleona's fists, Fuegoleon's Salamander, Leopold's spiral) are one self-contained feature, so they have one entry point: the single line the mod
 * constructor calls. It registers Magna's Soul Bat on its own item register, the server config {@code nusmp-firecast-server.toml} ({@code magic.fireCasting}), the key packet, the game events and
 * the CI self-test {@code magic.fire_casting}. Safe to call twice. The client half (the Salamander key, the prop painters) is {@code client.firecast.FireCastClient}, called from the client block
 * of the constructor.
 */
public final class FireCastBoot {
    private FireCastBoot() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NUSMP.MODID);

    /** Magna's Soul Bat, the item Magna Swing casts through. */
    public static final DeferredItem<MagnasSoulBatItem> BAT = ITEMS.register("magnas_soul_bat", MagnasSoulBatItem::new);

    private static boolean done;

    /** The single entry point, called from the mod constructor on both sides. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        ITEMS.register(modBus);
        SalamanderSounds.SOUNDS.register(modBus);                                                    // the Salamander's creature sounds (docs/magic/fire_casting.md, 4.2)
        container.registerConfig(ModConfig.Type.SERVER, FireCastConfig.SPEC, "nusmp-firecast-server.toml");
        modBus.addListener(FireCastNet::registerPayloads);
        FireCastEvents.register(NeoForge.EVENT_BUS);
        FireCastSelfTests.register();
    }

    /** The items drawn as 3D models in hand (client.WeaponRenderer). */
    public static List<Supplier<? extends Item>> renderables() { return List.of(BAT); }

    /** Hands a Magna grimoire's new owner the Soul Bat, once per player (the Magna canon grimoire grant). */
    public static void grantBat(net.minecraft.server.level.ServerPlayer p) {
        var data = p.getPersistentData();
        if (data.getBoolean("nusmp_magna_bat")) return;
        data.putBoolean("nusmp_magna_bat", true);
        p.getInventory().placeItemBackInInventory(new net.minecraft.world.item.ItemStack(BAT.get()));
    }
}
