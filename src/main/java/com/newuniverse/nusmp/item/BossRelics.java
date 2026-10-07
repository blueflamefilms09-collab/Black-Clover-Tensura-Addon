package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

/**
 * 0.52: what Zagred drops (docs/zagred_boss_gdd.md, section 3) and the two relics' passive effects.
 * <ul>
 *   <li><b>Heart of Words</b>: a crafting material, one for everyone who hurt him.</li>
 *   <li><b>Last Word</b> (Quill-blade, see {@link MagicWeaponItem.Kind#LAST_WORD}), <b>Shroud of Margins</b> and
 *       <b>Circlet of Quickened Thought</b>: one of the three per fighter, never the same one twice until all three have been
 *       found (the pity mask, kept through death).</li>
 *   <li><b>Shroud of Margins</b> (carried): three barrier layers; each absorbs one blow of up to 15% of max health and re-forms
 *       after 20 s. Spiritual damage is not stopped.</li>
 *   <li><b>Circlet of Quickened Thought</b> (carried): grimoire page cooldowns -12%; sneak + use for Overclock (6 s of haste and
 *       speed, 90 s cooldown).</li>
 * </ul>
 */
public final class BossRelics {
    private BossRelics() {}

    static final String K_LOOT = "nusmp_zagred_loot", K_MARGINS = "nusmp_margins_ready";
    static final int LAYERS = 3, LAYER_RECHARGE = 400;

    public static boolean carrying(Player p, Item item) {
        if (p.getOffhandItem().is(item)) return true;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(item)) return true;
        return false;
    }

    /** Circlet of Quickened Thought: page cooldowns x0.88. */
    public static double cooldownMult(Player p) { return carrying(p, NUItems.CIRCLET_OF_THOUGHT.get()) ? 0.88 : 1.0; }

    // ---------------------------------------------------------------- Shroud of Margins
    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getAmount() <= 0 || !carrying(p, NUItems.SHROUD_OF_MARGINS.get())) return;
        long now = p.level().getGameTime();
        long[] ready = p.getPersistentData().getLongArray(K_MARGINS);
        if (ready.length != LAYERS) ready = new long[LAYERS];
        for (int i = 0; i < LAYERS; i++) {
            if (now < ready[i]) continue;
            float absorbed = Math.min(e.getAmount(), p.getMaxHealth() * 0.15f);
            e.setAmount(e.getAmount() - absorbed);
            ready[i] = now + LAYER_RECHARGE;
            p.getPersistentData().putLongArray(K_MARGINS, ready);
            VfxSpawn.send(p.serverLevel(), VfxShape.KOTO_SHATTER, p.getBoundingBox().getCenter(), p.position(), 0xFFB8A0FF, 12, 0.8f);
            p.displayClientMessage(Component.literal("A margin holds (" + (LAYERS - 1 - i) + " left)").withStyle(ChatFormatting.DARK_PURPLE), true);
            return;
        }
    }

    // ---------------------------------------------------------------- Circlet: Overclock
    public static boolean overclock(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, 2));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0));
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.KOTO_AURA, p, p.position(), 0xFFFFFFFF, 40, 0.8f);
        p.displayClientMessage(Component.literal("Overclock!").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
        return true;
    }

    // ---------------------------------------------------------------- the drops
    /** One Heart of Words and one of the three items (no repeat until all three have dropped). */
    public static void give(ServerPlayer p) {
        CompoundTag root = p.getPersistentData();
        CompoundTag kept = root.getCompound(Player.PERSISTED_NBT_TAG);
        int mask = kept.getInt(K_LOOT);
        if (mask >= 7) mask = 0;
        List<Item> items = List.of(NUItems.LAST_WORD.get(), NUItems.SHROUD_OF_MARGINS.get(), NUItems.CIRCLET_OF_THOUGHT.get());
        int[] weight = {40, 35, 25};
        int total = 0;
        for (int i = 0; i < 3; i++) if ((mask & (1 << i)) == 0) total += weight[i];
        int roll = p.getRandom().nextInt(Math.max(1, total)), pick = 0;
        for (int i = 0; i < 3; i++) {
            if ((mask & (1 << i)) != 0) continue;
            if (roll < weight[i]) { pick = i; break; }
            roll -= weight[i];
        }
        kept.putInt(K_LOOT, mask | (1 << pick));
        root.put(Player.PERSISTED_NBT_TAG, kept);
        hand(p, new ItemStack(NUItems.HEART_OF_WORDS.get()));
        hand(p, new ItemStack(items.get(pick)));
        p.sendSystemMessage(Component.literal("Zagred's words fall silent. You take: ").withStyle(ChatFormatting.DARK_PURPLE)
                .append(new ItemStack(items.get(pick)).getHoverName().copy().withStyle(ChatFormatting.LIGHT_PURPLE)));
    }

    private static void hand(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }
}
