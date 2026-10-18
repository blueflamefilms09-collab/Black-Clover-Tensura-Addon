package com.newuniverse.nusmp.elite.hub;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the shop and the quest dialogue are screens whose requests the server must trust only when the player really has them open, next to a real NPC. A MENU is the server's proof of that: the net
 * handlers act only for a player whose open container is a {@link QuestMenu} or a {@link ShopMenu}, and the menu itself closes (the game asks {@code stillValid} every tick) the moment the NPC is
 * gone, dead or more than 8 blocks away. The menus hold no slots and no items (nothing can be put into them); the content of the screen is sent in packets ({@link HubNet}). The NPC's entity id
 * travels in the open packet, so the client half knows which NPC it is talking to (for the title).
 */
public final class HubMenus {
    private HubMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, NUSMP.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<QuestMenu>> QUEST = MENUS.register("hub_quests", () -> IMenuTypeExtension.create((id, inv, buf) -> new QuestMenu(id, buf.readVarInt())));
    public static final DeferredHolder<MenuType<?>, MenuType<ShopMenu>> SHOP = MENUS.register("hub_shop", () -> IMenuTypeExtension.create((id, inv, buf) -> new ShopMenu(id, buf.readVarInt())));

    public static void init(IEventBus modBus) { MENUS.register(modBus); }

    /** How far (blocks, squared) a player may be from the NPC with the screen open. */
    public static final double REACH_SQR = 8.0 * 8.0;

    /** The common part: a slotless menu tied to one NPC. */
    public abstract static class NpcMenu extends AbstractContainerMenu {
        private final int npcId;

        protected NpcMenu(MenuType<?> type, int containerId, int npcId) {
            super(type, containerId);
            this.npcId = npcId;
        }

        /** The entity id of the NPC this screen belongs to. */
        public int npcId() { return npcId; }

        @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

        @Override
        public boolean stillValid(Player player) {
            if (!player.isAlive()) return false;
            Entity e = player.level().getEntity(npcId);
            return e instanceof HubNpcBase npc && npc.isAlive() && player.distanceToSqr(npc) <= REACH_SQR;
        }
    }

    /** The Quest Giver's dialogue. */
    public static final class QuestMenu extends NpcMenu {
        public QuestMenu(int containerId, int npcId) { super(QUEST.get(), containerId, npcId); }
    }

    /** The Merchant's shop. */
    public static final class ShopMenu extends NpcMenu {
        public ShopMenu(int containerId, int npcId) { super(SHOP.get(), containerId, npcId); }
    }
}
