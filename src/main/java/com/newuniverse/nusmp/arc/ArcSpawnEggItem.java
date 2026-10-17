package com.newuniverse.nusmp.arc;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: the creative way to put a character into the world. A boss stands as it would after a summoning (its arena is the spot, the story gate does not
 * apply: the egg is a creative item, like the Zogratis eggs), a friendly character is placed with its post there. Which character is the stack's
 * {@code nusmp:arc_character} component; a stack without a valid one (a hand-typed {@code /give}) tells the player so instead of making a blank.
 */
public class ArcSpawnEggItem extends Item {
    public ArcSpawnEggItem(Properties props) {
        super(props);
    }

    @Override
    public Component getName(ItemStack stack) {
        ArcCharacter c = ArcRoster.byId(ArcItems.characterOf(stack));
        return c == null ? super.getName(stack) : Component.translatableWithFallback("item.nusmp.arc_spawn_egg.named", "%s Spawn Egg", c.name());
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        ArcCharacter c = ArcRoster.byId(ArcItems.characterOf(stack));
        if (c == null) {
            if (player != null) player.displayClientMessage(Component.translatableWithFallback("message.nusmp.arc.egg_blank", "This egg has no character in it."), true);
            return InteractionResult.FAIL;
        }
        if (!ArcConfig.enabled()) {
            if (player != null) player.displayClientMessage(Component.translatableWithFallback("message.nusmp.arc.disabled", "The characters of the arcs are switched off (arc.enabled)."), true);
            return InteractionResult.FAIL;
        }
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        Vec3 at = Vec3.atBottomCenterOf(pos);
        float yaw = player == null ? 0f : player.getYRot() + 180f;
        boolean made = c.isBoss()
                ? ArcSpawner.summon(sl, c, at, yaw, List.of(), true).outcome() == ArcSpawner.Outcome.SPAWNED
                : ArcSpawner.spawnNpc(sl, c, at, yaw) != null;
        if (!made) return InteractionResult.FAIL;
        if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.CONSUME;
    }
}
