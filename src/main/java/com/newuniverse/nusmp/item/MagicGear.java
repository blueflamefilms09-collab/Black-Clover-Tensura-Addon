package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.Kingdom;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.ForbiddenMagic;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** All robe set bonuses, relic behaviour and magic tool conduction. Numbers come from NUConfig. */
public final class MagicGear {
    private MagicGear() {}

    public static RobeItem.Kind robe(Player p) {
        return p.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof RobeItem r ? r.kind : null;
    }

    /** All four pieces of the same set worn: bonuses x1.5. */
    public static double setScale(Player p) {
        RobeItem.Kind k = robe(p);
        if (k == null) return 1;
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET})
            if (!(p.getItemBySlot(s).getItem() instanceof RobeItem r) || r.kind != k) return 1;
        return 1.5;
    }

    private static double scaled(Player p, double bonusFraction) { return bonusFraction * setScale(p); }

    // ---------------------------------------------------------------- multipliers used by grimoire casts
    public static double damageMult(Player p, MagicType m) {
        RobeItem.Kind k = robe(p);
        if (k == null) return 1;
        double el = scaled(p, NUConfig.GEAR_ELEMENT_BONUS.get() / 100.0);
        return switch (k) {
            case SENIOR -> 1 + scaled(p, NUConfig.GEAR_SENIOR_DAMAGE.get() / 100.0);
            case GOLDEN_DAWN -> m == MagicType.LIGHT || m == MagicType.STAR ? 1 + el : 1;
            case BLACK_BULL -> m == MagicType.DARK || m == MagicType.SHADOW ? 1 + el : 1;
            case SILVER_EAGLE -> m == MagicType.MERCURY || m == MagicType.STEEL ? 1 + el : 1;
            case CRIMSON_LION -> m == MagicType.FLAME || m == MagicType.EXPLOSION || m == MagicType.MAGMA ? 1 + el : 1;
            case CORAL_PEACOCK -> m == MagicType.WATER || m == MagicType.ICE || m == MagicType.MIST ? 1 + el : 1;
            case SPADE -> 1 + scaled(p, NUConfig.GEAR_SPADE_DAMAGE.get() / 100.0);
            case DEVIL -> 1 + scaled(p, NUConfig.GEAR_DEVIL_DAMAGE.get() / 100.0);
            default -> 1;
        };
    }

    public static double costMult(Player p) { return robe(p) == RobeItem.Kind.HEART ? 1 - Math.min(0.9, scaled(p, NUConfig.GEAR_HEART_COST.get() / 100.0)) : 1; }
    public static double cooldownMult(Player p) { return robe(p) == RobeItem.Kind.DIAMOND ? 1 - Math.min(0.9, scaled(p, NUConfig.GEAR_DIAMOND_COOLDOWN.get() / 100.0)) : 1; }
    public static double selfDamageMult(Player p) { return robe(p) == RobeItem.Kind.BLACK_BULL ? 1 - Math.min(1.0, scaled(p, NUConfig.GEAR_RECOIL_REDUCTION.get() / 100.0)) : 1; }
    public static void onCast(ServerPlayer p) {}

    // ---------------------------------------------------------------- per-second robe effects
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        com.newuniverse.nusmp.book.GrimoireKeeper.heal(p);
        antiBird(p);
        RobeItem.Kind k = robe(p);
        if (k == null) return;
        var ex = TensuraStorages.getExistenceFrom(p);
        double max = EnergyHelper.getMaxMagicule(p);
        double regen = switch (k) {
            case JUNIOR -> NUConfig.GEAR_REGEN_JUNIOR.get();
            case SENIOR -> NUConfig.GEAR_REGEN_SENIOR.get();
            case SPADE -> -NUConfig.GEAR_SPADE_REGEN_PENALTY.get();
            default -> 0;
        };
        regen *= setScale(p);
        if (ex != null && regen != 0) { ex.setMagicule(Math.max(0, Math.min(max, ex.getMagicule() + max * regen / 100.0))); ex.markDirty(); }
        switch (k) {
            case CORAL_PEACOCK -> p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 40, 0, true, false));
            case DIAMOND -> p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 0, true, false));
            case CRIMSON_LION -> GrimoirePages.grimoireOf(p).ifPresent(g -> {
                if (g.getOrCreateTag().getLong("DiveUntil") > p.level().getGameTime()) p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false));
            });
            case DEVIL -> devilCoat(p);
            default -> {}
        }
    }

    private static void devilCoat(ServerPlayer p) {
        boolean forbidden = GrimoirePages.grimoireOf(p).map(g -> GrimoirePages.coverOf(g).isForbidden()).orElse(false);
        if (!forbidden) {
            ItemStack coat = p.getItemBySlot(EquipmentSlot.CHEST).copy();
            p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            if (!p.getInventory().add(coat)) p.drop(coat, false);
            GrimoireBook.fail(p, "The Devil-bound Coat rejects you. Only a devil's mage may wear it.");
            return;
        }
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, true, false));
        long since = p.level().getGameTime() - p.getPersistentData().getLong("nusmp_devil_paid");
        if (since > NUConfig.GEAR_DEVIL_GRACE.get() * 20L && p.tickCount % 600 == 0) {
            p.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
            p.displayClientMessage(Component.literal("The coat is hungry. Pay the devil.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
        }
    }

    private static void antiBird(ServerPlayer p) {
        if (p.tickCount % 400 != 0 || !p.getInventory().contains(new ItemStack(NUItems.ANTI_BIRD_CHARM.get()))) return;
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PARROT_AMBIENT, SoundSource.PLAYERS, 0.6f, 1.2f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SPIRIT_AURA, p, p.position().add(0, 1, 0), 0xFF3A3A3A, 40, 0.5f);
    }

    // ---------------------------------------------------------------- damage events: conduction, eagle, misfortune
    private static boolean conducting;

    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (e.getEntity() instanceof Player victim && robe(victim) == RobeItem.Kind.SILVER_EAGLE)
            e.setAmount(e.getAmount() * (float) (1 - Math.min(0.9, scaled(victim, NUConfig.GEAR_EAGLE_REDUCTION.get() / 100.0))));
        if (e.getSource().getEntity() instanceof ServerPlayer holder && e.getSource().getDirectEntity() == holder
                && !MagicWeaponItem.usableBy(holder.getMainHandItem(), holder)) {
            e.setAmount(1f);
            holder.displayClientMessage(Component.literal("The demon sword refuses you.").withStyle(ChatFormatting.DARK_RED), true);
            return;
        }
        if (conducting || !(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        boolean melee = e.getSource().getDirectEntity() == p && p.getMainHandItem().getItem() instanceof MagicToolSword;
        boolean arrow = e.getSource().getDirectEntity() instanceof AbstractArrow a && a.getPersistentData().getBoolean("nusmp_conduct");
        if (!melee && !arrow) return;
        Optional<ManasSkillInstance> g = GrimoirePages.grimoireOf(p);
        if (g.isEmpty() || !(g.get().getSkill() instanceof GrimoireBook book)) return;
        conducting = true;
        try {
            book.hurt(g.get(), p, e.getEntity(), 0, NUConfig.GEAR_CONDUCTION_DAMAGE.get().floatValue());
            MagicType m = book.magic;
            if (m == MagicType.FLAME || m == MagicType.MAGMA || m == MagicType.EXPLOSION) e.getEntity().igniteForSeconds(2);
            if (m == MagicType.ICE) e.getEntity().setTicksFrozen(e.getEntity().getTicksRequiredToFreeze() + 40);
            book.impact(p, e.getEntity().getBoundingBox().getCenter(), 0.4f);
        } finally { conducting = false; }
    }

    // ---------------------------------------------------------------- kills & deaths
    public static void onDeath(LivingDeathEvent e) {
        if (e.getSource().getEntity() instanceof ServerPlayer killer && robe(killer) == RobeItem.Kind.GOLDEN_DAWN)
            killer.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        if (e.getEntity() instanceof ServerPlayer dead) {   // losing a true love cracks the heart
            String partner = ForbiddenMagic.data(dead).getString("nusmp_bond");
            if (partner.isEmpty()) return;
            ServerPlayer other = dead.getServer().getPlayerList().getPlayer(UUID.fromString(partner));
            ForbiddenMagic.data(dead).remove("nusmp_bond");
            if (other == null) return;
            CompoundTag od = ForbiddenMagic.data(other);
            od.remove("nusmp_bond");
            other.getPersistentData().put(Player.PERSISTED_NBT_TAG, od);
            GrimoirePages.grimoireOf(other).ifPresent(g -> {
                if (GrimoirePages.coverOf(g) == GrimoireCover.TWO_HEART) setCover(other, g, GrimoireCover.CRACKED_HEART);
            });
        }
    }

    // ---------------------------------------------------------------- relics
    public static boolean consumeSpiritCharm(ServerPlayer p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(NUItems.SPIRIT_CHARM.get())) { inv.getItem(i).shrink(1); return true; }
        }
        return false;
    }

    public static void setCover(ServerPlayer p, ManasSkillInstance g, GrimoireCover cover) {
        g.getOrCreateTag().putString("Cover", cover.name());
        g.getOrCreateTag().putInt("Leaves", cover.tier);
        g.markDirty();
        var inv = p.getInventory();
        com.newuniverse.nusmp.blackclover.GrimoireSlot.replaceOwned(p, old -> GrimoireItem.create(p, cover, GrimoirePages.magicOf(g), com.newuniverse.nusmp.blackclover.Devil.byName(g.getOrCreateTag().getString("Devil"))));
        for (int s = 0; s < inv.getContainerSize(); s++) {
            if (GrimoireItem.isOwnedBy(inv.getItem(s), p.getUUID()))
                inv.setItem(s, GrimoireItem.create(p, cover, GrimoirePages.magicOf(g), com.newuniverse.nusmp.blackclover.Devil.byName(g.getOrCreateTag().getString("Devil"))));
        }
        p.displayClientMessage(Component.literal("Your grimoire's cover changes: " + cover.displayName()).withStyle(ChatFormatting.GOLD), false);
    }

    // Bond Thread: both players sneak-use the thread on each other within 10 s.
    private static final Map<UUID, UUID> BOND_REQ = new HashMap<>();
    private static final Map<UUID, Long> BOND_AT = new HashMap<>();

    public static void bondRequest(ServerPlayer p, ServerPlayer other) {
        long now = p.level().getGameTime();
        if (p.getUUID().equals(BOND_REQ.get(other.getUUID())) && now - BOND_AT.getOrDefault(other.getUUID(), 0L) < 200) {
            BOND_REQ.remove(other.getUUID());
            for (ServerPlayer[] pair : new ServerPlayer[][]{{p, other}, {other, p}}) {
                CompoundTag d = ForbiddenMagic.data(pair[0]);
                d.putString("nusmp_bond", pair[1].getUUID().toString());
                pair[0].getPersistentData().put(Player.PERSISTED_NBT_TAG, d);
                GrimoirePages.grimoireOf(pair[0]).ifPresent(g -> {
                    GrimoireCover c = GrimoirePages.coverOf(g);
                    if (c.kingdom == Kingdom.HEART) setCover(pair[0], g, GrimoireCover.TWO_HEART);
                });
                pair[0].sendSystemMessage(Component.literal("A red thread binds you to " + pair[1].getName().getString() + ".").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            p.getMainHandItem().shrink(1);
            VfxSpawn.send(p.serverLevel(), VfxShape.THREAD_LINE, p.getEyePosition(), other.getEyePosition(), 0xFFFF6FA8, 60, 1.2f);
            return;
        }
        BOND_REQ.put(p.getUUID(), other.getUUID());
        BOND_AT.put(p.getUUID(), now);
        p.displayClientMessage(Component.literal("You offer the Bond Thread. They must offer theirs within 10 s.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        other.displayClientMessage(Component.literal(p.getName().getString() + " offers you a Bond Thread. Sneak-use yours on them to accept.").withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    // Communication Magic Device: right-click another device holder to link; chat lines starting with % go to linked devices.
    public static String channel(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d == null ? "" : d.copyTag().getString("Channel");
    }

    public static void link(ServerPlayer p, ItemStack stack, ServerPlayer other) {
        ItemStack theirs = other.getMainHandItem().is(NUItems.COMMUNICATION_DEVICE.get()) ? other.getMainHandItem() : other.getOffhandItem();
        if (!theirs.is(NUItems.COMMUNICATION_DEVICE.get())) { GrimoireBook.fail(p, "They need to hold a Communication Magic Device."); return; }
        String ch = channel(stack).isEmpty() ? Integer.toHexString(p.getRandom().nextInt(0xFFFFFF)) : channel(stack);
        for (ItemStack s : new ItemStack[]{stack, theirs}) {
            CompoundTag t = new CompoundTag();
            t.putString("Channel", ch);
            s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        }
        p.displayClientMessage(Component.literal("Devices linked (channel " + ch + "). Start a chat message with % to use it.").withStyle(ChatFormatting.AQUA), false);
        other.displayClientMessage(Component.literal("Devices linked (channel " + ch + "). Start a chat message with % to use it.").withStyle(ChatFormatting.AQUA), false);
    }

    private static String deviceChannel(Player p) {
        for (ItemStack s : p.getInventory().items) if (s.is(NUItems.COMMUNICATION_DEVICE.get()) && !channel(s).isEmpty()) return channel(s);
        return "";
    }

    public static void onChat(ServerChatEvent e) {
        String msg = e.getRawText();
        if (!msg.startsWith("%")) return;
        String ch = deviceChannel(e.getPlayer());
        if (ch.isEmpty()) return;
        e.setCanceled(true);
        Component line = Component.literal("[Device] " + e.getPlayer().getName().getString() + ": " + msg.substring(1).trim()).withStyle(ChatFormatting.AQUA);
        for (ServerPlayer o : e.getPlayer().getServer().getPlayerList().getPlayers()) if (ch.equals(deviceChannel(o))) o.sendSystemMessage(line);
    }

    // Grimoire Chain: nobody but the owner can pick up a chained owner's grimoire.
    public static void onPickup(ItemEntityPickupEvent.Pre e) {
        ItemStack s = e.getItemEntity().getItem();
        if (!(s.getItem() instanceof GrimoireItem)) return;
        CompoundTag d = GrimoireItem.data(s);
        if (!d.hasUUID("Owner") || d.getUUID("Owner").equals(e.getPlayer().getUUID())) return;
        ServerPlayer owner = e.getPlayer().getServer() == null ? null : e.getPlayer().getServer().getPlayerList().getPlayer(d.getUUID("Owner"));
        if (owner != null && owner.getInventory().contains(new ItemStack(NUItems.GRIMOIRE_CHAIN.get()))) e.setCanPickup(TriState.FALSE);
    }

    // Mana Method Rune Stone: store your last page, then place it as a trap.
    public static void storeRune(ServerPlayer p, ItemStack stack, ManasSkillInstance g) {
        CompoundTag t = new CompoundTag();
        t.putString("Book", g.getSkillId().toString());
        t.putInt("Mode", g.getOrCreateTag().getInt("LastMode"));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        p.displayClientMessage(Component.literal("The rune stone drinks in your last page. Use it on the ground to arm it.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    public static boolean placeRune(ServerPlayer p, ItemStack stack, BlockPos pos) {
        Optional<ManasSkillInstance> g = GrimoirePages.grimoireOf(p);
        if (stack.get(DataComponents.CUSTOM_DATA) == null || g.isEmpty() || !(g.get().getSkill() instanceof GrimoireBook book)) {
            GrimoireBook.fail(p, "Store a page first (use the stone in the air)."); return false;
        }
        stack.shrink(1);
        Vec3 c = Vec3.atBottomCenterOf(pos);
        UUID owner = p.getUUID();
        boolean[] fired = {false};
        VfxSpawn.send(p.serverLevel(), VfxShape.ELF_CIRCLE, c, c.add(0, 1, 0), book.color, 100, 0.6f);
        SpellRuntime.zone(p.serverLevel(), 6000, 10, age -> {
            if (fired[0]) return;
            if (age % 100 == 0) VfxSpawn.send(p.serverLevel(), VfxShape.ELF_CIRCLE, c, c.add(0, 1, 0), book.color, 100, 0.6f);
            var hostiles = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(1.5), e -> e instanceof Enemy && e.isAlive());
            if (hostiles.isEmpty()) return;
            ServerPlayer o = p.getServer().getPlayerList().getPlayer(owner);
            if (o == null) return;
            fired[0] = true;
            for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(3), e -> e instanceof Enemy && e.isAlive()))
                book.hurt(g.get(), o, t, 0, 10f);
            VfxSpawn.send(p.serverLevel(), VfxShape.MAGIC_CIRCLE_EXPLOSION, c, c.add(0, 1, 0), book.color, 24, 1.2f);
        });
        p.displayClientMessage(Component.literal("Rune armed for 5 minutes.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }
}
