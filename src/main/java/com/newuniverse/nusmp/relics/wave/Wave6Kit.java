package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;
import java.util.function.IntSupplier;

/**
 * Wave 6: the small glue every armour and robe of the wave shares (what is worn, the charge pools on the stack, effects and attribute
 * modifiers by registry id, which blows are "magic", "slashing" or "bludgeoning", who counts as a foe). Statics only; all server side.
 */
public final class Wave6Kit {
    private Wave6Kit() {}

    private static final String P = "w6_";

    // ---------------------------------------------------------------- what is worn
    /**
     * The worn stack of the relic with this id, or EMPTY. All ten relics of the wave sit in the chest slot, so that slot is read directly (cheap);
     * anything else falls back to the core's list. The stack is the live one in the slot, so writing its data sticks.
     */
    public static ItemStack find(LivingEntity e, String relicId) {
        ItemStack chest = e.getItemBySlot(EquipmentSlot.CHEST);
        RelicDef c = Relics.defOf(chest);
        if (c != null && relicId.equals(c.id())) return chest;
        for (ItemStack s : Relics.equipped(e)) {
            Optional<RelicDef> d = Relics.of(s);
            if (d.isPresent() && relicId.equals(d.get().id())) return s;
        }
        return ItemStack.EMPTY;
    }

    /** A short line over the hotbar (for things that happen by themselves; acts speak in chat through Fx.say). */
    public static void hint(ServerPlayer p, String text) { p.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.GOLD), true); }

    // ---------------------------------------------------------------- charge pools on the stack
    /** The day number dawn refills count by: the Overworld's, so sleeping through the night is a new dawn everywhere. */
    public static long today(ServerLevel any) { return any.getServer().overworld().getDayTime() / 24000L; }

    public static CompoundTag tag(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }

    /** The pool {@code name} of this stack, dawn refill applied (and written back when it changed or did not exist yet: a fresh relic starts full). */
    public static Wave6Pool.State pool(ItemStack s, String name, int max, long today, IntSupplier gainPerDay) {
        CompoundTag t = tag(s);
        boolean known = t.contains(P + name + "_c");
        Wave6Pool.State st = known ? new Wave6Pool.State(t.getInt(P + name + "_c"), t.getLong(P + name + "_d")) : Wave6Pool.State.fresh(max, today);
        Wave6Pool.State now = Wave6Pool.rollDay(st, today, max, gainPerDay);
        if (!known || !now.equals(st)) put(s, name, now);
        return now;
    }

    /** The pool {@code kind} of this stack for {@code p} (its dice are the player's random source). */
    public static Wave6Pool.State pool(ItemStack s, Wave6Pools kind, ServerPlayer p) {
        return pool(s, kind.key(), kind.max(), today(p.serverLevel()), () -> kind.gain(n -> p.getRandom().nextInt(n)));
    }

    public static void put(ItemStack s, Wave6Pools kind, Wave6Pool.State st) { put(s, kind.key(), st); }

    public static void put(ItemStack s, String name, Wave6Pool.State st) {
        CustomData.update(DataComponents.CUSTOM_DATA, s, t -> { t.putInt(P + name + "_c", st.charges()); t.putLong(P + name + "_d", st.day()); });
    }

    // ---------------------------------------------------------------- effects by id
    public static Optional<Holder<MobEffect>> effect(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? Optional.empty() : BuiltInRegistries.MOB_EFFECT.getHolder(rl).map(h -> (Holder<MobEffect>) h);
    }

    /** Applies an effect by id; {@code quiet} = ambient with no particles (the way relic passives show). Absent mods' effects are skipped. */
    public static void give(LivingEntity e, String id, int ticks, int amp, boolean quiet) {
        effect(id).ifPresent(h -> e.addEffect(new MobEffectInstance(h, ticks, amp, quiet, !quiet, true)));
    }

    public static void lift(LivingEntity e, String id) { effect(id).ifPresent(e::removeEffect); }

    public static boolean has(LivingEntity e, String id) { return effect(id).map(e::hasEffect).orElse(false); }

    // ---------------------------------------------------------------- attribute modifiers by id
    public static Optional<Holder<Attribute>> attribute(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? Optional.empty() : BuiltInRegistries.ATTRIBUTE.getHolder(rl).map(h -> (Holder<Attribute>) h);
    }

    /** Switches a transient modifier of this wave on or off (idempotent). Transient: it never reaches the save file, the tick re-applies it. */
    public static void setMod(LivingEntity e, String attributeId, String name, double amount, AttributeModifier.Operation op, boolean on) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("nusmp", "w6_" + name);
        attribute(attributeId).ifPresent(h -> {
            AttributeInstance inst = e.getAttribute(h);
            if (inst == null) return;
            if (on) { if (!inst.hasModifier(id)) inst.addTransientModifier(new AttributeModifier(id, amount, op)); }
            else if (inst.hasModifier(id)) inst.removeModifier(id);
        });
    }

    // ---------------------------------------------------------------- damage classes
    /** Spell damage: vanilla magic, or any Tensura damage type that is not physical (the same test the Magebane Shield uses). */
    public static boolean isMagic(DamageSource s) {
        if (s.is(DamageTypes.MAGIC) || s.is(DamageTypes.INDIRECT_MAGIC)) return true;
        var key = s.typeHolder().unwrapKey();
        return key.isPresent() && "tensura".equals(key.get().location().getNamespace())
                && !s.is(io.github.manasmods.tensura.data.TensuraTags.DamageTypes.IS_PHYSICAL);
    }

    /** The weapon of a melee blow (the attacker's main hand), or EMPTY for anything that is not a melee blow. */
    private static ItemStack blow(DamageSource s) {
        Entity a = s.getEntity();
        return a instanceof LivingEntity l && s.getDirectEntity() == a ? l.getMainHandItem() : ItemStack.EMPTY;
    }

    /** "Slashing": a melee blow with a sword or an axe. */
    public static boolean slashing(DamageSource s) {
        Item it = blow(s).getItem();
        return it instanceof SwordItem || it instanceof AxeItem;
    }

    /** "Bludgeoning": a mace, pick, shovel or hoe, and a hard landing. */
    public static boolean bludgeoning(DamageSource s) {
        ItemStack w = blow(s);
        Item it = w.getItem();
        return w.is(Items.MACE) || it instanceof PickaxeItem || it instanceof ShovelItem || it instanceof HoeItem || s.is(DamageTypeTags.IS_FALL);
    }

    /** Effects that hold a body in place: the "grapple" the Slime Armour slips (Tensura webs and locks, and a crushing slowness). */
    public static boolean restraint(MobEffectInstance inst) {
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(inst.getEffect().value());
        if (id == null) return false;
        String path = id.getPath();
        if ("tensura".equals(id.getNamespace())) return path.equals("webbed") || path.equals("earth_lock") || path.equals("movement_interference") || path.equals("infinite_imprisonment");
        return "minecraft".equals(id.getNamespace()) && path.equals("slowness") && inst.getAmplifier() >= 3;
    }

    /** The damage type id to hit with: {@code preferred} (a Tensura type) when the damage type registry has it, else {@code fallback}. */
    public static String damageType(ServerLevel level, String preferred, String fallback) {
        ResourceLocation rl = ResourceLocation.tryParse(preferred);
        try {
            if (rl != null && level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(rl)) return preferred;
        } catch (RuntimeException none) { /* no such registry entry: the fallback */ }
        return fallback;
    }

    // ---------------------------------------------------------------- who is a foe
    /** A living thing the wearer's area effects may hurt: hostile mobs, anything that is hunting the wearer, and players the wearer is allowed to hit. */
    public static boolean isFoe(ServerPlayer owner, LivingEntity e) {
        if (e == owner || !e.isAlive() || e instanceof ArmorStand) return false;
        if (e instanceof Player pl) return !pl.isCreative() && !pl.isSpectator() && owner.canHarmPlayer(pl);
        if (isAlly(owner, e)) return false;
        return e instanceof Enemy || (e instanceof Mob m && m.getTarget() == owner);
    }

    /** Team mates, and tamed animals that belong to {@code who}. */
    public static boolean isAlly(LivingEntity who, LivingEntity e) {
        return e == who || e.isAlliedTo(who) || (e instanceof TamableAnimal t && t.isOwnedBy(who));
    }
}
