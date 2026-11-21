package com.newuniverse.nusmp.bossdrops2;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.bossdrops2.SignatureTable.Boon;
import com.newuniverse.nusmp.bossdrops2.SignatureTable.Tier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Why: the two items of the worthwhile boss drops, in one place with their own register (attached to the mod bus by {@link BossSigBoot}):
 * <ul>
 *   <li>{@code boss_signature}: ONE item for every boss's signature drop. WHICH boss it is lives in the stack ({@link #KEY_BOSS} in the custom data, the icon in {@code custom_model_data} = the boss's position
 *       in {@link BossSigCatalog}, the colour in the rarity), so 55 bosses are not 55 registry entries. Right-click absorbs it: the boon of the boss's table (a few effects for a few minutes, the same on
 *       every use, no stacking beyond what the effects themselves do), one second of cooldown.</li>
 *   <li>{@code boss_echo}: the fading copy of a boss's one-of-one, paid when the relic exists already. Right-click absorbs it for the (stronger, longer) echo boon of the table.</li>
 * </ul>
 * Both are plain items (a stack of 16, fire resistant). The hand look is the 2D icon (a drawn sprite per boss, see {@code tools/gen_boss_signature_assets.py}); they are not Blender models.
 */
public final class BossSigItems {
    private BossSigItems() {}

    public static final String KEY_BOSS = "nusmp_sig_boss", KEY_RELIC = "nusmp_sig_relic";

    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(NUSMP.MODID);
    public static final DeferredItem<Item> SIGNATURE = REGISTER.register("boss_signature", () -> new SigItem(new Item.Properties().stacksTo(16).fireResistant(), false));
    public static final DeferredItem<Item> ECHO = REGISTER.register("boss_echo", () -> new SigItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant(), true));

    // ================================================================ stacks
    /** A signature stack of this boss: its icon, its rarity by tier (apex also glints), its boss id in the custom data. */
    public static ItemStack signature(String boss, Tier tier, int count) {
        ItemStack s = new ItemStack(SIGNATURE.get(), Math.max(1, count));
        CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putString(KEY_BOSS, boss));
        s.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(Math.max(0, BossSigCatalog.modelData(boss))));
        s.set(DataComponents.RARITY, rarity(tier));
        if (tier == Tier.APEX) s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, Boolean.TRUE);
        return s;
    }

    /** The echo of the one-of-one {@code relic} of this boss. */
    public static ItemStack echo(String boss, String relic) {
        ItemStack s = new ItemStack(ECHO.get());
        CustomData.update(DataComponents.CUSTOM_DATA, s, t -> {
            t.putString(KEY_BOSS, boss);
            t.putString(KEY_RELIC, relic);
        });
        s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, Boolean.TRUE);
        return s;
    }

    public static Rarity rarity(Tier tier) {
        return switch (tier) {
            case LESSER -> Rarity.UNCOMMON;
            case MID -> Rarity.RARE;
            default -> Rarity.EPIC;
        };
    }

    public static String bossOf(ItemStack s) { return read(s, KEY_BOSS); }

    public static String relicOf(ItemStack s) { return read(s, KEY_RELIC); }

    private static String read(ItemStack s, String key) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        if (d == null) return "";
        var tag = d.getUnsafe();
        return tag.contains(key) ? tag.getString(key) : "";
    }

    /** The creative menu: a signature stack of every boss of the catalog, then an echo of every boss that has a one-of-one (the relic ids come from the catalog's index, so no data is needed). */
    public static void addTo(CreativeModeTab.Output out) {
        for (String boss : BossSigCatalog.BOSSES) out.accept(signature(boss, BossSigCatalog.tier(boss), 1));
        for (String boss : BossSigCatalog.BOSSES) {
            String relic = BossSigCatalog.relic(boss);
            if (!relic.isEmpty()) out.accept(echo(boss, relic));
        }
    }

    // ================================================================ the item
    private static final class SigItem extends Item {
        private final boolean echo;

        SigItem(Properties p, boolean echo) {
            super(p);
            this.echo = echo;
        }

        @Override
        public Component getName(ItemStack stack) {
            String boss = bossOf(stack);
            if (boss.isEmpty()) return super.getName(stack);
            if (echo) {
                String relic = relicOf(stack);
                return relic.isEmpty() ? super.getName(stack) : Component.translatable("item." + NUSMP.MODID + ".boss_echo", Component.translatable("item." + NUSMP.MODID + "." + relic));
            }
            return Component.translatable("bossdrops2.sig." + boss + ".name");
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
            String boss = bossOf(stack);
            if (boss.isEmpty()) return;
            if (echo) {
                String relic = relicOf(stack);
                if (!relic.isEmpty()) tip.add(Component.translatable("bossdrops2.tooltip.echo", Component.translatable("item." + NUSMP.MODID + "." + relic)).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                tip.add(Component.translatable("bossdrops2.sig." + boss + ".echo_boon").withStyle(ChatFormatting.AQUA));
                return;
            }
            tip.add(Component.translatable("bossdrops2.sig." + boss + ".lore").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            tip.add(Component.translatable("bossdrops2.sig." + boss + ".boon").withStyle(ChatFormatting.AQUA));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            List<Boon> boon = boonOf(stack);
            if (boon.isEmpty()) {
                sp.displayClientMessage(Component.translatable("bossdrops2.msg.faded"), true);
                return InteractionResultHolder.fail(stack);
            }
            for (Boon b : boon) {
                ResourceLocation id = ResourceLocation.tryParse(b.effect());
                if (id != null) BuiltInRegistries.MOB_EFFECT.getHolder(id).ifPresent(h -> sp.addEffect(new MobEffectInstance(h, b.seconds() * 20, b.amplifier(), false, true, true)));
            }
            sp.displayClientMessage(Component.translatable("bossdrops2.msg.used", stack.getHoverName()), true);
            sp.getCooldowns().addCooldown(this, 20);
            if (!sp.getAbilities().instabuild) stack.shrink(1);
            return InteractionResultHolder.consume(stack);
        }

        private List<Boon> boonOf(ItemStack stack) {
            SignatureTable.Table t = SignatureData.table(bossOf(stack));
            if (t == null) return List.of();
            return echo ? (t.hasUnique() ? t.unique().echoBoon() : List.of()) : t.signature().boon();
        }
    }
}
