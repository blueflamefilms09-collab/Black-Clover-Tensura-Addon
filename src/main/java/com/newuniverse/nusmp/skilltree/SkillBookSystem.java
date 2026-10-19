package com.newuniverse.nusmp.skilltree;

import com.newuniverse.nusmp.skilltree.hk.HeavyKnightSkills;
import com.newuniverse.nusmp.skilltree.item.ItemsSystem;
import com.newuniverse.nusmp.skilltree.line.LineSystem;
import com.newuniverse.nusmp.skilltree.shop.ShopSystem;
import com.newuniverse.nusmp.skilltree.state.SkillTreeAttachments;
import com.newuniverse.nusmp.skilltree.unique.SkillBookSelfTests;
import com.newuniverse.nusmp.skilltree.unique.UniqueEvents;
import com.newuniverse.nusmp.skilltree.unique.UniqueSkills;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the skill book system is a few small systems (the unique skills one per book and the line engine that runs their arts and passives; the items and the loot; the Black Market shop) that must start in one
 * fixed order, and the mod constructor should have exactly ONE line for all of it. The order: the legacy record's attachment and the skill registers on the mod bus (the ManasCore skill registry must hear
 * about the unique skills before it freezes); then the line engine, the game-bus events, {@link ItemsSystem#init} (the Skill Book, the loot modifier types) and {@link ShopSystem#init}; the in-game self-tests
 * last. Each system registers its OWN config file, so no separate {@code registerConfig} line exists. 1.96.0: this replaced the Skill Tree engine (routes, SP, the tree screen), which was removed; the
 * attachment {@code nusmp:skill_tree} stays registered because old worlds hold it and the Black Market keeps its gold in it. Safe to call twice.
 */
public final class SkillBookSystem {
    private SkillBookSystem() {}

    private static boolean done;

    /** The single entry point, called from the mod constructor on both sides. */
    public static synchronized void init(IEventBus modBus, ModContainer modContainer) {
        if (done) return;
        done = true;
        SkillTreeAttachments.ATTACHMENTS.register(modBus);
        UniqueSkills.init(modBus);                                  // one Unique skill per book, in Tensura's Ability Selection screen (docs/skillbooks_unique.md)
        HeavyKnightSkills.SKILLS.register(modBus);                  // the technique classes of the four Heavy Knight books (run as modes of their unique skills)
        LineSystem.init();                                          // the arts and passives engine
        UniqueEvents.register();                                    // the data reload, the login migration
        ItemsSystem.init(modBus, modContainer);
        ShopSystem.init(modBus, modContainer);
        SkillBookSelfTests.register();
    }

    /** What the creative tab offers: one Skill Book per unique skill. */
    public static List<ItemStack> creativeItems() {
        return new ArrayList<>(ItemsSystem.creativeItems());
    }
}
