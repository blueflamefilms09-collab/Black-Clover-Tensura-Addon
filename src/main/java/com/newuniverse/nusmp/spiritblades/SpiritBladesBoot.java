package com.newuniverse.nusmp.spiritblades;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

/**
 * Why: the two Wind Spirit blades are one small self-contained feature, so they have one entry point: the single line the mod constructor calls. It registers Spirit of Zephyr (a sword) and
 * Spirit of Boreas (a bow) on their own item register, listens to the server tick (the arrow trail), to a player leaving and to the server stopping (both forget their state), and registers the CI
 * self-test {@code magic.spirit_blades}. Both items are creative-only: they are in the creative tab and nowhere else (no loot table, no recipe, no drop, no trade). Their 3D models, skins, glow and
 * engravings are data ({@code assets/nusmp/weapon_meshes}, {@code item/WeaponEngravings}). Safe to call twice. The client half is {@code client/SpiritBladesFx}, called by the weapon renderer.
 */
public final class SpiritBladesBoot {
    private SpiritBladesBoot() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NUSMP.MODID);

    /** Spirit of Zephyr: the long glowing green sword. */
    public static final DeferredItem<ZephyrSwordItem> ZEPHYR = ITEMS.register("spirit_of_zephyr", ZephyrSwordItem::new);
    /** Spirit of Boreas: the leaf bow. */
    public static final DeferredItem<BoreasBowItem> BOREAS = ITEMS.register("spirit_of_boreas", BoreasBowItem::new);

    private static boolean done;

    /** The single entry point, called from the mod constructor on both sides. */
    public static synchronized void init(IEventBus modBus) {
        if (done) return;
        done = true;
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> SpiritBladesTrail.tick());
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> ZephyrSwordItem.forget(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> {
            SpiritBladesTrail.clearAll();
            ZephyrSwordItem.clearAll();
        });
        SpiritBladesSelfTests.register();
    }

    /** The items drawn as 3D models in hand (client.WeaponRenderer). */
    public static List<Supplier<? extends Item>> renderables() { return List.of(ZEPHYR, BOREAS); }

    /** The creative tab's entries. */
    public static List<Supplier<? extends Item>> creativeItems() { return List.of(ZEPHYR, BOREAS); }
}
