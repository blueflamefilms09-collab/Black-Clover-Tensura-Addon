package com.newuniverse.nusmp.elite.items.weapon;

import com.newuniverse.nusmp.NUGameRules;
import com.newuniverse.nusmp.elite.items.EliteItemsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the six weapons of the elite item set (Astral Edge, Void Edge, Fenrir Sword and the three Runic Swords) are ONE item class whose techniques are DATA: right-click runs the weapon's primary
 * technique, sneak + right-click its second one (on its own cooldown, kept in the player's data like the other weapons' second techniques), and a hit with the attack bar at 90% or more adds the
 * weapon's bonus strike, exactly the pattern of the older weapons (WeaponArts). What a technique IS comes from {@code data/nusmp/elite/items/abilities/<item>.json} by the item's registry path
 * ({@link AbilityTables}), so the Void Edge is "paired" with the Void Edge skill of another builder only through that file and a late-bound predicate. Engravings are NOT done here: they are listed in
 * {@code item/WeaponEngravings} (owner rule 6) and applied by its existing inventory scan. The model in hand is the real 3D one drawn by {@code client/elite/items/EliteItemsClient} through the
 * mod's {@code WeaponRenderer}; the inventory keeps the 16x16 icon.
 */
public class EliteWeaponItem extends SwordItem {
    /** The six weapons: id, material tier, attack damage bonus, attack speed, rarity. Appended to only. */
    public enum Def {
        ASTRAL_EDGE("astral_edge", Tiers.NETHERITE, 4, -2.4f, Rarity.EPIC),
        VOID_EDGE("void_edge", Tiers.NETHERITE, 5, -2.6f, Rarity.EPIC),
        FENRIR_SWORD("fenrir_sword", Tiers.NETHERITE, 6, -2.8f, Rarity.EPIC),
        RUNIC_SWORD_RENEWED("runic_sword_renewed", Tiers.DIAMOND, 3, -2.4f, Rarity.UNCOMMON),
        RUNIC_SWORD_ANCIENT("runic_sword_ancient", Tiers.NETHERITE, 3, -2.4f, Rarity.RARE),
        RUNIC_SWORD_MYTHICAL("runic_sword_mythical", Tiers.NETHERITE, 6, -2.2f, Rarity.EPIC);

        final String id;
        final Tier tier;
        final int damage;
        final float speed;
        final Rarity rarity;

        Def(String id, Tier tier, int damage, float speed, Rarity rarity) {
            this.id = id;
            this.tier = tier;
            this.damage = damage;
            this.speed = speed;
            this.rarity = rarity;
        }

        public String id() { return id; }

        public boolean runic() { return RuneRules.slots(id) > 0; }
    }

    static final String ALT_KEY = "nusmp_elite_alt_";
    /** Game bus: the attack charge of the swing about to land (set on the attack event, read by {@link #hurtEnemy}). */
    private static final Map<UUID, Float> CHARGE = new HashMap<>();

    final Def def;

    public EliteWeaponItem(Def def) {
        super(def.tier, new Item.Properties().attributes(SwordItem.createAttributes(def.tier, def.damage, def.speed)).rarity(def.rarity).fireResistant());
        this.def = def;
    }

    public Def def() { return def; }

    // ------------------------------------------------------------------ events
    /** Remembers how charged the swing is before the hit lands. */
    public static void onAttack(AttackEntityEvent e) {
        Player p = e.getEntity();
        if (!p.level().isClientSide && p.getMainHandItem().getItem() instanceof EliteWeaponItem) CHARGE.put(p.getUUID(), p.getAttackStrengthScale(0.5f));
    }

    public static void forget(UUID id) { CHARGE.remove(id); }

    // ------------------------------------------------------------------ use
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.pass(stack);
        if (!EliteItemsConfig.enabled() || !EliteItemsConfig.get(EliteItemsConfig.ABILITIES_ENABLED)) return InteractionResultHolder.pass(stack);
        WeaponAbilities wa = AbilityTables.of(def.id);
        if (wa == null) return InteractionResultHolder.pass(stack);
        AbilityRunner.Ctx ctx = new AbilityRunner.Ctx(p, wa, runeCount(stack, p));
        if (p.isShiftKeyDown() && wa.alt() != wa.primary()) {
            long now = level.getGameTime();
            CompoundTag d = p.getPersistentData();
            long ready = d.getLong(ALT_KEY + def.id);
            if (now < ready) {
                p.displayClientMessage(Component.literal(wa.alt().name() + " is not ready: " + (ready - now + 19) / 20 + " s").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            if (!AbilityRunner.run(ctx, wa.alt())) return InteractionResultHolder.fail(stack);
            d.putLong(ALT_KEY + def.id, now + cooldown(p, wa.alt()));
            announce(p, wa.alt());
            return InteractionResultHolder.success(stack);
        }
        if (!AbilityRunner.run(ctx, wa.primary())) return InteractionResultHolder.fail(stack);
        p.getCooldowns().addCooldown(this, cooldown(p, wa.primary()));
        announce(p, wa.primary());
        return InteractionResultHolder.success(stack);
    }

    private static int cooldown(ServerPlayer p, AbilitySpec s) {
        double v = s.cooldown() * EliteItemsConfig.get(EliteItemsConfig.ABILITY_COOLDOWN_SCALE) * NUGameRules.spellCooldown(p.level());
        return Math.max(1, (int) Math.round(v));
    }

    private static void announce(ServerPlayer p, AbilitySpec s) {
        p.displayClientMessage(Component.literal(s.name() + "!").withStyle(ChatFormatting.BOLD, s.dark() ? ChatFormatting.DARK_PURPLE : ChatFormatting.AQUA), true);
    }

    /** The slotted runes that count; 0 for a weapon that has no slots. Overridden by the Runic Swords. */
    int runeCount(ItemStack stack, ServerPlayer p) { return 0; }

    // ------------------------------------------------------------------ full swing
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean r = super.hurtEnemy(stack, target, attacker);
        if (attacker instanceof ServerPlayer p && EliteItemsConfig.enabled() && EliteItemsConfig.get(EliteItemsConfig.ABILITIES_ENABLED)) {
            Float charge = CHARGE.remove(p.getUUID());
            WeaponAbilities wa = AbilityTables.of(def.id);
            if (charge != null && charge >= 0.9f && wa != null && target != p) AbilityRunner.swing(new AbilityRunner.Ctx(p, wa, runeCount(stack, p)), target);
        }
        return r;
    }

    // ------------------------------------------------------------------ tooltip
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("tooltip.nusmp." + def.id + ".lore", "").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tip.add(Component.translatableWithFallback("tooltip.nusmp." + def.id + ".primary", "Right-click: its technique.").withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatableWithFallback("tooltip.nusmp." + def.id + ".alt", "Sneak + right-click: its second technique.").withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.translatableWithFallback("tooltip.nusmp.elite_weapon.swing", "A full-strength swing adds its bonus strike.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
