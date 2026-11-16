package com.newuniverse.nusmp.sinweapons;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

/**
 * Why: the five Berserk of Gluttony weapons (Black Axe of Sloth, Black Gunblade of Envy, Black Spear of Vanity, Black Chakrams, Seifort Holy Sword) are one small self-contained feature, so they have one entry point:
 * the single line the mod constructor calls. It registers the items on their own register, the server config {@code nusmp-sinweapons-server.toml} and the CI self-test {@code berserk.weapons}. They are in the
 * creative tab and nowhere else (no loot table, no recipe, no drop; an operator's /give, like the Black Sword Greed). Their 3D models are data ({@code weapon_meshes}), drawn by {@code client/WeaponRenderer}.
 * Safe to call twice.
 */
public final class SinWeaponBoot {
    private SinWeaponBoot() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NUSMP.MODID);

    public static final DeferredItem<SinWeaponItem> SLOTH = ITEMS.register(SinRules.SLOTH_ID, () -> new SinWeaponItem(SinRules.Kind.SLOTH));
    public static final DeferredItem<SinWeaponItem> ENVY = ITEMS.register(SinRules.ENVY_ID, () -> new SinWeaponItem(SinRules.Kind.ENVY));
    public static final DeferredItem<SinWeaponItem> VANITY = ITEMS.register(SinRules.VANITY_ID, () -> new SinWeaponItem(SinRules.Kind.VANITY));
    public static final DeferredItem<SinWeaponItem> CHAKRAMS = ITEMS.register(SinRules.CHAKRAMS_ID, () -> new SinWeaponItem(SinRules.Kind.CHAKRAMS));
    public static final DeferredItem<SinWeaponItem> HOLY = ITEMS.register(SinRules.HOLY_ID, () -> new SinWeaponItem(SinRules.Kind.HOLY));

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        ITEMS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, SinWeaponConfig.SPEC, "nusmp-sinweapons-server.toml");
        SinWeaponSelfTests.register();
    }

    /** The items drawn as 3D models in hand (client.WeaponRenderer). */
    public static List<Supplier<? extends Item>> renderables() { return List.of(SLOTH, ENVY, VANITY, CHAKRAMS, HOLY); }

    /** The creative tab's entries. */
    public static List<Supplier<? extends Item>> creativeItems() { return renderables(); }
}
