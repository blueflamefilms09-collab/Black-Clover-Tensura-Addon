package com.newuniverse.nusmp.skilltree;

import com.newuniverse.nusmp.skilltree.item.SkillItems;
import com.newuniverse.nusmp.skilltree.shop.ShopSystem;
import com.newuniverse.nusmp.skilltree.state.SkillTreeAttachments;
import com.newuniverse.nusmp.skilltree.unique.LegacyMigration;
import com.newuniverse.nusmp.skilltree.unique.LegacySkills;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

import java.util.List;

/**
 * Why: the Skill Books (the Skill Book and the SP Book items), the 284 unique skills they taught, the line engine that ran those skills and the book loot were removed at the owner's request (see
 * {@code docs/skill_books_removed.md}); the mod constructor still has exactly ONE line for what is left of this package, and this is it. What stays, in this order: the attachment {@code nusmp:skill_tree} (old worlds
 * hold it and the Black Market keeps its gold in it); the four data component ids the books carried ({@link SkillItems}); the inert stubs of the retired skill ids ({@link LegacySkills}) and the login hook that
 * forgets them from a player ({@link LegacyMigration}); and the Black Market ({@link ShopSystem}), whose witch buys goods for gold. Safe to call twice.
 */
public final class SkillBookSystem {
    private SkillBookSystem() {}

    private static boolean done;

    /** The single entry point, called from the mod constructor on both sides. */
    public static synchronized void init(IEventBus modBus, ModContainer modContainer) {
        if (done) return;
        done = true;
        SkillTreeAttachments.ATTACHMENTS.register(modBus);
        SkillItems.init(modBus);                                    // the four legacy data components (old stacks and worlds still load)
        LegacySkills.init(modBus);                                  // the retired skill ids, as inert stubs (the ManasCore skill registry must hear about them before it freezes)
        LegacyMigration.register();                                 // login: forget the stubs, clear the old Skill Tree record
        ShopSystem.init(modBus, modContainer);
    }

    /** What the creative tab offers from this package: nothing any more (there are no Skill Books). Kept so the creative tab's call site still compiles; the call site can be removed. */
    public static List<ItemStack> creativeItems() {
        return List.of();
    }
}
