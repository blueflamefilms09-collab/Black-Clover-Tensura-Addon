package com.newuniverse.nusmp.skilltree;

import com.newuniverse.nusmp.skilltree.ft.FireDragonSkills;
import com.newuniverse.nusmp.skilltree.ft.IceMakeSkills;
import com.newuniverse.nusmp.skilltree.ft.LightningDragonSkills;
import com.newuniverse.nusmp.skilltree.ft.SkyDragonSkills;
import com.newuniverse.nusmp.skilltree.hk.HeavyKnightSkills;
import com.newuniverse.nusmp.skilltree.item.ItemsSystem;
import com.newuniverse.nusmp.skilltree.shop.ShopSystem;
import com.newuniverse.nusmp.skilltree.state.SkillTreeAttachments;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Skill Tree engine is five small systems (the core: data, state, network, commands; the items and the loot; the Fairy Tail content; the Black Market shop; and the screen on the client)
 * that must start in one fixed order, and the mod constructor should have exactly ONE line for all of it. The order: the attachment and the four skill registers on the mod bus (the
 * ManasCore skill registry must hear about the Fairy Tail skills before it freezes); then {@link TreeCore#init} (reload listener, payloads, events, config), then {@link ItemsSystem#init}
 * (the Skill Book and the SP Book, the loot modifier type), then {@link ShopSystem#init}. Each system registers its OWN config file, so no separate {@code registerConfig} line exists. The lead
 * owns this file. Safe to call twice.
 */
public final class SkillTreeSystem {
    private SkillTreeSystem() {}

    private static boolean done;

    /** The single entry point, called from the mod constructor on both sides. */
    public static synchronized void init(IEventBus modBus, ModContainer modContainer) {
        if (done) return;
        done = true;
        SkillTreeAttachments.ATTACHMENTS.register(modBus);
        FireDragonSkills.SKILLS.register(modBus);
        LightningDragonSkills.SKILLS.register(modBus);
        SkyDragonSkills.SKILLS.register(modBus);
        IceMakeSkills.SKILLS.register(modBus);
        HeavyKnightSkills.SKILLS.register(modBus);                  // 1.63.0: the Exiled Heavy Knight routes (Oath of the Heavy Armor, Basic Swordsmanship, Lady Luck)
        TreeCore.init(modBus, modContainer);
        ItemsSystem.init(modBus, modContainer);
        ShopSystem.init(modBus, modContainer);
    }

    /** What the creative tab offers: the Skill Books and SP Books (every route of the registry, every SP variant). */
    public static List<ItemStack> creativeItems() {
        return new ArrayList<>(ItemsSystem.creativeItems());
    }
}
