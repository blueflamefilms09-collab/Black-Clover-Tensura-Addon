package com.newuniverse.nusmp.dungeon.nexus.vault;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * The blocks of the Dungeon's hidden vaults and gates (docs/living_dungeon.md, "Hidden Mana Vaults" and "Navigation"): the two Illusion
 * rocks that wall a vault, the Descent Gate, the Warp Trap, and the Nutrient Bloom with the nectar it gives. All of them are unbreakable
 * like the rest of the Dungeon; none drops anything.
 */
public final class NexusBlocks {
    private NexusBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);

    /** The blocks that look like rock and have no collision; the Ki-Resonance Lantern burns blue before them. */
    public static final TagKey<Block> ILLUSION_STONES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "illusion_stones"));

    private static BlockBehaviour.Properties illusion(MapColor color, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(-1f, 3600000f).sound(sound).noCollission().forceSolidOn().noLootTable()
                .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false).pushReaction(PushReaction.BLOCK);
    }

    public static final DeferredBlock<Block> ILLUSION_STONE = BLOCKS.register("illusion_stone", () -> new IllusionBlock(illusion(MapColor.STONE, SoundType.STONE)));
    public static final DeferredBlock<Block> ILLUSION_DEEPSLATE = BLOCKS.register("illusion_deepslate", () -> new IllusionBlock(illusion(MapColor.DEEPSLATE, SoundType.DEEPSLATE)));

    /** The way down to the next floor (and, with rising=true, the way back up). It cannot be broken or pushed. */
    public static final DeferredBlock<Block> DESCENT_GATE = BLOCKS.register("descent_gate", () -> new DescentGateBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollission().forceSolidOn().strength(-1f, 3600000f).sound(SoundType.SCULK)
                    .lightLevel(s -> s.getValue(DescentGateBlock.RISING) ? 10 : 12).noLootTable().pushReaction(PushReaction.BLOCK)
                    .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));

    public static final DeferredBlock<Block> WARP_TRAP = BLOCKS.register("warp_trap", () -> new WarpTrapBlock(
            BlockBehaviour.Properties.of().mapColor(s -> s.getValue(WarpTrapBlock.DEEP) ? MapColor.DEEPSLATE : MapColor.STONE).strength(-1f, 3600000f)
                    .sound(SoundType.STONE).noLootTable().pushReaction(PushReaction.BLOCK)));

    public static final DeferredBlock<Block> NUTRIENT_BLOOM = BLOCKS.register("nutrient_bloom", () -> new NutrientBloomBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).noCollission().instabreak().randomTicks().sound(SoundType.MOSS)
                    .lightLevel(s -> 3 + 2 * s.getValue(NutrientBloomBlock.AGE)).noLootTable().pushReaction(PushReaction.DESTROY)));

    public static final DeferredItem<BlockItem> ILLUSION_STONE_ITEM = NUItems.ITEMS.register("illusion_stone", () -> new BlockItem(ILLUSION_STONE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ILLUSION_DEEPSLATE_ITEM = NUItems.ITEMS.register("illusion_deepslate", () -> new BlockItem(ILLUSION_DEEPSLATE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DESCENT_GATE_ITEM = NUItems.ITEMS.register("descent_gate", () -> new BlockItem(DESCENT_GATE.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<BlockItem> WARP_TRAP_ITEM = NUItems.ITEMS.register("warp_trap", () -> new BlockItem(WARP_TRAP.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> NUTRIENT_BLOOM_ITEM = NUItems.ITEMS.register("nutrient_bloom", () -> new BlockItem(NUTRIENT_BLOOM.get(), new Item.Properties()));

    /** What a ripe Nutrient Bloom gives: a rich, safe food that is the Dungeon's own. */
    public static final DeferredItem<Item> NUTRIENT_NECTAR = NUItems.ITEMS.register("nutrient_nectar", () -> new Nectar(new Item.Properties().rarity(Rarity.UNCOMMON).food(
            new FoodProperties.Builder().nutrition(7).saturationModifier(1.4f).alwaysEdible()
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100, 0), 1f)
                    .effect(() -> new MobEffectInstance(MobEffects.SATURATION, 20, 0), 1f).build())));

    public static void init() {}

    /** Nectar: nothing but a line saying where it comes from. */
    private static final class Nectar extends Item {
        Nectar(Item.Properties props) { super(props); }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
            lines.add(Component.literal("Pressed from a Nutrient Bloom. Rich, safe, and kind to the stomach.").withStyle(ChatFormatting.GRAY));
        }
    }
}
