package com.newuniverse.nusmp.valkyrielance;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.book.ModeArmorValkyrieSelfTests;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

/**
 * Why: the Valkyrie Drill Lance and the Valkyrie armour v2 are one small self-contained feature, so they have one entry point: the single line the mod constructor calls. It registers the lance (a spear-class
 * item) on its own item register, listens to a player leaving and to the server stopping (both forget the hit-burst map) and registers the CI self-test {@code magic.valkyrie_armor} (the armour's budgets and
 * meshes, the lance's registration, numbers, engravings and caps; pure checks, they run on the dedicated server). The lance is creative-only: it is in the creative tab and nowhere else (no loot table, no
 * recipe, no drop, no trade). Its 3D model, skins, glow and engravings are data ({@code assets/nusmp/weapon_meshes}, {@code item/WeaponEngravings}). Safe to call twice. The client halves are
 * {@code client/WeaponRenderer} (the model, its turning drill) and {@code client/mode/ModeArmorValkyrieV2} (the armour layer).
 */
public final class ValkyrieLanceBoot {
    private ValkyrieLanceBoot() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NUSMP.MODID);

    /** The Valkyrie Drill Lance. */
    public static final DeferredItem<ValkyrieDrillLanceItem> LANCE = ITEMS.register(ValkyrieLanceRules.ID, ValkyrieDrillLanceItem::new);

    private static boolean done;

    /** The single entry point, called from the mod constructor on both sides. */
    public static synchronized void init(IEventBus modBus) {
        if (done) return;
        done = true;
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> ValkyrieDrillLanceItem.forget(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> ValkyrieDrillLanceItem.clearAll());
        ModeArmorValkyrieSelfTests.register();
    }

    /** The items drawn as 3D models in hand (client.WeaponRenderer). */
    public static List<Supplier<? extends Item>> renderables() { return List.of(LANCE); }

    /** The creative tab's entries. */
    public static List<Supplier<? extends Item>> creativeItems() { return List.of(LANCE); }
}
