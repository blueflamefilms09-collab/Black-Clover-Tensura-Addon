package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Black Clover swords. Right-click uses the sword's technique (vanilla item cooldown).
 * Demon swords are bound to the Anti-Magic user who drew them from their grimoire.
 */
public class MagicWeaponItem extends SwordItem {
    public enum Kind {
        DEMON_SLASHER_KATANA(Tiers.DIAMOND, 3, -2.2f, "Mana Suppression Arc", "A crescent that cuts mana: damages, strips a buff and drains 5% magicule.", 160, false),
        MIASMA_KATANA(Tiers.DIAMOND, 3, -2.3f, "Dimension Slash", "A 12-block cut that destroys projectiles first. Hits wither.", 200, false),
        SPELL_FORGED_RAPIER(Tiers.DIAMOND, 1, -1.8f, "Spatial Edge", "Blink 6 blocks forward, piercing everything on the way.", 120, false),
        SEVERING_GREATSWORD(Tiers.NETHERITE, 5, -3.2f, "Sword Rain", "Eight blades fall on the spot you aim at.", 300, false),
        DEMON_SLAYER(Tiers.NETHERITE, 6, -3.0f, "Demon-Slayer Cut", "Erases nearby projectiles, then a huge cut that strips magic from everything hit.", 160, true),
        DEMON_DWELLER(Tiers.NETHERITE, 3, -2.2f, "Black Slash", "A flying anti-magic slash that cuts spells out of its path.", 120, true),
        DEMON_DESTROYER(Tiers.NETHERITE, 4, -2.6f, "Undo", "Cleanses curses, misfortune and harmful effects from you and allies nearby. Hits strip every buff.", 300, true);

        final Tier tier; final int damage; final float speed; final String ability, desc; final int cooldown; final boolean demon;
        Kind(Tier t, int d, float s, String a, String desc, int cd, boolean demon) {
            tier = t; damage = d; speed = s; ability = a; this.desc = desc; cooldown = cd; this.demon = demon;
        }
    }

    public final Kind kind;

    public MagicWeaponItem(Kind kind) {
        super(kind.tier, new Item.Properties().attributes(SwordItem.createAttributes(kind.tier, kind.damage, kind.speed))
                .rarity(kind.demon ? Rarity.EPIC : Rarity.RARE).fireResistant());
        this.kind = kind;
    }

    // ---------------------------------------------------------------- binding (demon swords)
    public static ItemStack bound(Item item, Player owner) {
        ItemStack s = new ItemStack(item);
        CompoundTag t = new CompoundTag();
        t.putUUID("Owner", owner.getUUID());
        t.putString("OwnerName", owner.getName().getString());
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return s;
    }

    public static boolean usableBy(ItemStack s, Player p) {
        if (!(s.getItem() instanceof MagicWeaponItem w) || !w.kind.demon) return true;
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        if (d == null || !d.copyTag().hasUUID("Owner")) return true;
        UUID o = d.copyTag().getUUID("Owner");
        return o.equals(p.getUUID());
    }

    // ---------------------------------------------------------------- technique
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.pass(stack);
        if (!usableBy(stack, p)) { GrimoireBook.fail(p, "This sword answers only to its owner."); return InteractionResultHolder.fail(stack); }
        if (!technique(p)) return InteractionResultHolder.fail(stack);
        p.getCooldowns().addCooldown(this, kind.cooldown);
        p.displayClientMessage(Component.literal(kind.ability + "!").withStyle(kind.demon ? ChatFormatting.DARK_GRAY : ChatFormatting.AQUA, ChatFormatting.BOLD), true);
        return InteractionResultHolder.success(stack);
    }

    private void hit(ServerPlayer p, LivingEntity t, float raw) {
        if (t == p || t.isAlliedTo(p)) return;
        t.hurt(p.damageSources().playerAttack(p), BalanceLaw.damage(t, raw, 0.6));
    }

    private static void stripOne(LivingEntity t) {
        for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) if (e.getEffect().value().isBeneficial()) { t.removeEffect(e.getEffect()); return; }
    }

    private static void eraseProjectiles(ServerPlayer p, AABB box) {
        for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, box)) if (pr.getOwner() != p) pr.discard();
    }

    private boolean technique(ServerPlayer p) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        switch (kind) {
            case DEMON_SLASHER_KATANA -> {
                for (LivingEntity t : GrimoireBook.around(p, p.position(), 5)) {
                    if (t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.5) continue;
                    hit(p, t, 9);
                    stripOne(t);
                    var ex = TensuraStorages.getExistenceFrom(t);
                    if (ex != null) { ex.setMagicule(Math.max(0, ex.getMagicule() - EnergyHelper.getMaxMagicule(t) * 0.05)); ex.markDirty(); }
                }
                VfxSpawn.send(p.serverLevel(), VfxShape.WIND_SLASH, eye, eye.add(look.scale(5)), 0xFFE04050, 14, 1.3f);
            }
            case MIASMA_KATANA -> {
                Vec3 end = eye.add(look.scale(12));
                eraseProjectiles(p, new AABB(eye, end).inflate(1.2));
                for (LivingEntity t : GrimoireBook.along(p, eye, end, 1.2)) { hit(p, t, 11); t.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0)); }
                VfxSpawn.send(p.serverLevel(), VfxShape.WIND_SLASH, eye, end, 0xFF8A5AD0, 16, 1.6f);
            }
            case SPELL_FORGED_RAPIER -> {
                Vec3 from = p.position(), dir = look.multiply(1, 0, 1).normalize(), dest = null;
                for (double d = 6; d >= 1; d -= 0.5) {
                    Vec3 c = from.add(dir.scale(d));
                    if (p.level().noCollision(p, p.getBoundingBox().move(c.subtract(from)))) { dest = c; break; }
                }
                if (dest == null) { GrimoireBook.fail(p, "No room to move."); return false; }
                for (LivingEntity t : GrimoireBook.along(p, from.add(0, 1, 0), dest.add(0, 1, 0), 1.2)) hit(p, t, 8);
                p.teleportTo(dest.x, dest.y, dest.z);
                VfxSpawn.send(p.serverLevel(), VfxShape.SPATIAL_RIFT, from, from.add(0, 1, 0), 0xFFB088FF, 12, 0.6f);
                VfxSpawn.send(p.serverLevel(), VfxShape.THREAD_LINE, from.add(0, 1, 0), dest.add(0, 1, 0), 0xFFE0E8FF, 10, 1f);
            }
            case SEVERING_GREATSWORD -> {
                Vec3 c = p.pick(20, 1f, false).getLocation();
                var rnd = p.getRandom();
                for (int k = 0; k < 8; k++) {
                    Vec3 at = c.add(rnd.nextGaussian() * 1.6, 0, rnd.nextGaussian() * 1.6);
                    SpellRuntime.later(p.serverLevel(), 4 + k * 3, () -> {
                        VfxSpawn.send(p.serverLevel(), VfxShape.LIGHTNING_SPEAR, at.add(0, 10, 0), at, 0xFFD8E2FF, 8, 0.8f);
                        for (LivingEntity t : GrimoireBook.around(p, at, 1.5)) hit(p, t, 5);
                    });
                }
                VfxSpawn.send(p.serverLevel(), VfxShape.WEAPON_CONSTRUCTS, c, c.add(0, 1, 0), 0xFFD8E2FF, 40, 1.2f);
            }
            case DEMON_SLAYER -> {
                eraseProjectiles(p, p.getBoundingBox().inflate(5));
                for (LivingEntity t : GrimoireBook.around(p, p.position(), 4.5)) {
                    if (t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.4) continue;
                    hit(p, t, 12);
                    stripOne(t);
                }
                VfxSpawn.send(p.serverLevel(), VfxShape.ANTI_MAGIC_SLASH, eye, eye.add(look.scale(4.5)), 0xFF2A0A30, 18, 1.6f);
            }
            case DEMON_DWELLER -> {
                Vec3 end = eye.add(look.scale(16));
                eraseProjectiles(p, new AABB(eye, end).inflate(1.2));
                SpellRuntime.bolt(p, eye, look.scale(1.8), 1.0, 9, true, null, (b, t) -> { hit(p, t, 10); stripOne(t); }, null);
                VfxSpawn.send(p.serverLevel(), VfxShape.ANTI_MAGIC_SLASH, eye, end, 0xFF2A0A30, 16, 1.4f);
            }
            case DEMON_DESTROYER -> {
                List<LivingEntity> cleanse = new ArrayList<>(p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(4), x -> x == p || x.isAlliedTo(p)));
                for (LivingEntity t : cleanse) {
                    for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) if (!e.getEffect().value().isBeneficial()) t.removeEffect(e.getEffect());
                    t.getPersistentData().putLong("nusmp_misfortune_until", 0);
                    t.getPersistentData().putLong("nusmp_sealed_until", 0);
                }
                VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.ANTI_MAGIC_SLASH, p, p.position().add(0, 1, 0).add(look), 0xFF2A0A30, 18, 1.2f);
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- on hit
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean r = super.hurtEnemy(stack, target, attacker);
        if (attacker instanceof ServerPlayer p) {
            switch (kind) {
                case MIASMA_KATANA -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                case DEMON_SLAYER, DEMON_DWELLER -> stripOne(target);
                case DEMON_DESTROYER -> { for (int k = 0; k < 3; k++) stripOne(target); }
                default -> {}
            }
            if (kind.demon) AntiMagic.addAmp(p, 10);
        }
        return r;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Right-click: " + kind.ability).withStyle(ChatFormatting.GOLD));
        tip.add(Component.literal("  " + kind.desc).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("  Cooldown: " + kind.cooldown / 20 + " s").withStyle(ChatFormatting.DARK_GRAY));
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        if (kind.demon && d != null && d.copyTag().contains("OwnerName"))
            tip.add(Component.literal("Bound to " + d.copyTag().getString("OwnerName")).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
