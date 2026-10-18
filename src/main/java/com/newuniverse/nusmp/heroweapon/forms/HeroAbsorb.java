package com.newuniverse.nusmp.heroweapon.forms;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.heroweapon.HeroItems;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the owner's mechanic is "weapons absorb raw materials, monster parts or items into a central gem to unlock new specialised forms". This class is the gem's
 * mouth. It takes a stack (from the off hand, from a Weapon Book click, or from an operator's command), asks {@link AbsorbRules} whether the gem wants it and how
 * many, removes that many from the stack, adds them to the bond's absorbed counts, tells the wielder (action bar, sound, a swirl into the gem, the quest hook
 * {@code hero.absorb}), and then lets {@link FormLearning} open whatever became available. The counting is pure and tested; this class only moves items and
 * reports. Everything is re-checked here whatever the caller already checked, because the Weapon Book's packets are not trusted.
 */
public final class HeroAbsorb {
    private HeroAbsorb() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<UUID, AbsorbRules.Limiter> LIMITERS = new HashMap<>();

    /**
     * Gear and named things: with {@code absorbAnything} on, the gem still does not swallow what a form did not ask for when it is a tool, a weapon, a piece of armour (anything
     * with durability), enchanted or renamed, because the off hand usually holds a shield or a totem and a sneaking technique must not eat it.
     */
    private static boolean precious(ItemStack stack) { return stack.isDamageableItem() || stack.isEnchanted() || stack.has(DataComponents.CUSTOM_NAME); }

    /** The registry id of a stack's item as text ({@code minecraft:leather}). */
    public static String idOf(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }

    /** The sneak + use route: the off hand's stack goes into the gem. True only when something was taken (the core then skips the sneak technique). */
    static boolean offhand(ServerPlayer player) { return absorb(player, player.getOffhandItem(), false, false) > 0; }

    /**
     * Takes (part of) a stack into the gem.
     *
     * @param stack   the live stack in the player's inventory or hand (it is shrunk)
     * @param whole   take the whole stack whatever the forms still need
     * @param verbose tell the player when the gem refuses (the off-hand route stays silent, because the off hand usually holds ordinary things)
     * @return how many items were taken, 0 when none
     */
    public static int absorb(ServerPlayer player, ItemStack stack, boolean whole, boolean verbose) {
        try {
            return absorbUnsafe(player, stack, whole, verbose);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] hero forms: absorbing failed", e);
            return 0;
        }
    }

    private static int absorbUnsafe(ServerPlayer player, ItemStack stack, boolean whole, boolean verbose) {
        HeroBond bond = HeroBonds.of(player);
        if (!bond.isBound() || stack.isEmpty()) return 0;
        if (HeroItems.kindOf(stack) != null) {
            if (verbose) FormsFeedback.bar(player, FormsLang.t("hero.forms.absorb.weapon", "A hero weapon cannot be absorbed."));
            return 0;
        }
        boolean any = FormsConfig.get(FormsConfig.ABSORB_ANYTHING);
        String id = idOf(stack);
        List<HeroForm> forms = HeroForms.of(bond.kind());
        boolean asked = AbsorbRules.accepts(id, forms, FormTags.MATCHER);
        if (!asked && (!any || precious(stack))) {
            if (verbose) FormsFeedback.bar(player, FormsLang.t("hero.forms.absorb.unwanted", "The gem has no use for %s.", stack.getHoverName()));
            return 0;
        }
        if (!limiter(player).tryAcquire(player.level().getGameTime())) return 0;
        int needed = AbsorbRules.needed(id, forms, bond.unlocked(), bond.absorbedMap(), FormTags.MATCHER);
        int take = AbsorbRules.take(stack.getCount(), needed, FormsConfig.get(FormsConfig.ABSORB_ONLY_NEEDED), whole, any);
        if (take <= 0) {
            if (verbose) FormsFeedback.bar(player, FormsLang.t("hero.forms.absorb.enough", "The gem has taken in enough %s.", stack.getHoverName()));
            return 0;
        }
        Component name = stack.getHoverName();
        stack.shrink(take);
        bond.absorb(id, take);
        FormsFeedback.bar(player, FormsLang.t("hero.forms.absorbed", "The %s absorbs %s x%s.", bond.kind().display(), name, take));
        FormsFeedback.sound(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f, 1.2f);
        FormsFeedback.swirl(player, FormsFeedback.gemColor(bond));
        NusmpEvents.fire(player, "hero.absorb", id);
        FormLearning.learnAvailable(player, name, false);
        FormsSync.markDirty(player);
        return take;
    }

    /**
     * The Weapon Book's Feed button: absorbs the item with this id from the player's inventory (main inventory and off hand, never armour), stack after stack, until
     * the gem has what it needs or the items run out. Returns how many were taken.
     */
    public static int feed(ServerPlayer player, String itemId, boolean whole) {
        HeroBond bond = HeroBonds.of(player);
        if (!bond.isBound() || itemId == null || itemId.isEmpty()) return 0;
        int total = 0;
        Inventory inv = player.getInventory();
        for (ItemStack s : inv.items) total += feedStack(player, s, itemId, whole);
        for (ItemStack s : inv.offhand) total += feedStack(player, s, itemId, whole);
        if (total == 0) FormsFeedback.bar(player, FormsLang.t("hero.forms.absorb.nothing", "The gem takes nothing from that."));
        return total;
    }

    private static int feedStack(ServerPlayer player, ItemStack stack, String itemId, boolean whole) {
        if (stack.isEmpty() || !idOf(stack).equals(itemId)) return 0;
        return absorb(player, stack, whole, false);
    }

    /** The command: the main-hand stack when it is not the weapon itself, else the off hand. Returns how many were taken. */
    public static int fromHands(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        ItemStack use = !main.isEmpty() && HeroItems.kindOf(main) == null ? main : player.getOffhandItem();
        return absorb(player, use, false, true);
    }

    private static AbsorbRules.Limiter limiter(ServerPlayer player) {
        return LIMITERS.computeIfAbsent(player.getUUID(), k -> new AbsorbRules.Limiter(FormsConfig.get(FormsConfig.ABSORB_PER_SECOND_LIMIT)));
    }

    static void forget(UUID player) { LIMITERS.remove(player); }

    static void clear() { LIMITERS.clear(); }
}
