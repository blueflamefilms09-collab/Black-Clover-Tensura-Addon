package com.newuniverse.nusmp.fairytail.storm;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.core.magic.profile.MagicProfiles;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

/**
 * Why: Storm Magic is a self-contained Fairy Tail feature (the Stormcaller Staff and its storms), so it has its own entry point: the one line the mod constructor calls. It registers the
 * staff on its own item register, the server config ({@code nusmp-storm-server.toml}), the storm manager's listeners and the self-test {@code magic.storm_staff}, and labels the old
 * Storm Magic as a Fairy Tail magic in the magic UI (a profile override of the display name; the enum constant and every id stay). Safe to call twice.
 */
public final class StormBoot {
    private StormBoot() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NUSMP.MODID);

    /** The Stormcaller Staff, the item Storm Magic is tied to. */
    public static final DeferredItem<StormStaffItem> STAFF = ITEMS.register("stormcaller_staff", StormStaffItem::new);

    /** What the magic UI calls the magic that used to be the Storm grimoire. */
    public static final String LABEL = "Storm Magic (Fairy Tail)";

    private static boolean done;

    /** The single entry point, called from the mod constructor on both sides. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        ITEMS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, StormConfig.SPEC, "nusmp-storm-server.toml");
        StormField.init();
        StormSelfTests.register();
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(StormBoot::label));          // after registration, when every class the profile needs is ready
    }

    /** Labels the old Storm Magic as a Fairy Tail magic in the magic UI: only the display name of its profile is replaced. */
    static void label() {
        try {
            MagicProfiles.override(MagicType.STORM, o -> o.displayName(LABEL));
        } catch (RuntimeException | LinkageError ignored) {
            // cosmetic only: the old name stays
        }
    }

    /** The items drawn as 3D models in hand (client.WeaponRenderer). */
    public static List<Supplier<? extends Item>> renderables() { return List.of(STAFF); }
}
