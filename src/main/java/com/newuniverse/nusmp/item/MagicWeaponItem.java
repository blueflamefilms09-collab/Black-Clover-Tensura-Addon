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
        // 0.28: abilities renamed and rebuilt after the Black Clover wiki (ids kept so old worlds load)
        DEMON_SLASHER_KATANA(Tiers.DIAMOND, 3, -2.2f, "Infinite Slash", "A long flying anti-magic slash from overhead: cuts every spell in a 20-block line and strips a buff from all it hits.", 160, true),
        MIASMA_KATANA(Tiers.DIAMOND, 3, -2.3f, "Dark Cloaked Dimension Slash", "A 16-block cut of darkness that cuts spells and space first. Hits are left in darkness.", 200, false),
        SPELL_FORGED_RAPIER(Tiers.DIAMOND, 1, -1.8f, "Spatial Edge", "Blink 6 blocks forward, piercing everything on the way.", 120, false),
        SEVERING_GREATSWORD(Tiers.NETHERITE, 5, -3.2f, "Sword Rain", "Eight blades fall on the spot you aim at.", 300, false),
        DEMON_SLAYER(Tiers.NETHERITE, 6, -3.0f, "Black Divider", "Bats spells back with the flat of the blade, then a huge anti-magic sweep that strips magic from everything hit.", 160, true),
        DEMON_DWELLER(Tiers.NETHERITE, 3, -2.2f, "Black Slash", "A flying anti-magic slash that cuts spells out of its path and knocks its target back.", 120, true),
        DEMON_DESTROYER(Tiers.NETHERITE, 4, -2.6f, "Causality Break", "Undoes spell effects: cleanses you and allies nearby, erases spells around you and strips every buff from foes close by.", 300, true),
        LICHT_DWELLER(Tiers.NETHERITE, 4, -2.4f, "Conquering Eon", "Licht's white Demon-Dweller: a 20-block slash that grows with every ally near you, and heals you.", 240, false),
        LICHT_DESTROYER(Tiers.NETHERITE, 5, -2.6f, "Causality Break", "Licht's white Demon-Destroyer: strips every buff from foes in front of you and erases their spells.", 240, false),
        // 0.32: a cryo-lattice mana runeblade (docs/runeblade_spec.md)
        RIMEHEART(Tiers.NETHERITE, 5, -2.4f, "Glacial Matrix Burst", "Vents the blade's stored mana as a frost shockwave: erases spells within 5 blocks, freezes and slows everything caught. Critical hits shatter a smaller burst around the target; paired with a second magic sword the burst recharges 40% faster.", 200, false);

        final Tier tier; final int damage; final float speed; final String ability, desc; final int cooldown; final boolean demon;
        Kind(Tier t, int d, float s, String a, String desc, int cd, boolean demon) {
            tier = t; damage = d; speed = s; ability = a; this.desc = desc; cooldown = cd; this.demon = demon;
        }
    }

    public final Kind kind;

    public MagicWeaponItem(Kind kind) {
        super(kind.tier, new Item.Properties().attributes(attributes(kind))
                .rarity(kind.demon || kind == Kind.RIMEHEART ? Rarity.EPIC : Rarity.RARE).fireResistant());
        this.kind = kind;
    }

    /** 0.48: for a sword with its own item properties (the Genesis Demon-Slayer is unbreakable); the kind's attributes are added. */
    protected MagicWeaponItem(Kind kind, Item.Properties properties) {
        super(kind.tier, properties.attributes(attributes(kind)));
        this.kind = kind;
    }

    private static final net.minecraft.resources.ResourceLocation RIMEHEART_STANCE =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("nusmp", "rimeheart_stance");

    /** Sword attributes; the Rimeheart also roots its wielder a little (knockback resistance while held). */
    private static net.minecraft.world.item.component.ItemAttributeModifiers attributes(Kind kind) {
        var a = SwordItem.createAttributes(kind.tier, kind.damage, kind.speed);
        if (kind == Kind.RIMEHEART)
            a = a.withModifierAdded(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(RIMEHEART_STANCE, 0.25,
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
        return a;
    }

    /** Dual-wield synergy: another magic sword in the off hand. */
    static boolean pairedBlade(Player p) { return p.getOffhandItem().getItem() instanceof MagicWeaponItem; }

    int cooldownFor(Player p) {
        int base = kind == Kind.RIMEHEART && pairedBlade(p) ? Math.round(kind.cooldown * 0.6f) : kind.cooldown;
        return Math.max(1, (int) Math.round(base * com.newuniverse.nusmp.NUGameRules.spellCooldown(p.level())));   // 0.48: 40% shorter by default
    }

    /** A burst of frost mana: freezes, slows and hurts everything within r of c (the wielder and allies excepted). */
    private void frostBurst(ServerPlayer p, Vec3 c, double r, float dmg, LivingEntity skip) {
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            if (t == skip) continue;
            hit(p, t, dmg);
            t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() + 100, t.getTicksFrozen() + 120));
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        }
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

    /** A sword drawn from a grimoire (Sword Magic): bound to its caster and gone after {@code ticks}. */
    public static ItemStack summoned(Item item, Player owner, int ticks) {
        ItemStack s = bound(item, owner);
        CompoundTag t = s.get(DataComponents.CUSTOM_DATA).copyTag();
        t.putLong("ExpiresAt", owner.level().getGameTime() + ticks);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return s;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity holder, int slot, boolean selected) {
        if (level.isClientSide || level.getGameTime() % 20 != 0) return;
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        if (d == null) return;
        CompoundTag t = d.copyTag();
        if (t.contains("ExpiresAt") && level.getGameTime() >= t.getLong("ExpiresAt")) stack.setCount(0);   // the summoned sword fades
    }

    public static boolean usableBy(ItemStack s, Player p) {
        if (!(s.getItem() instanceof MagicWeaponItem w)) return true;
        CustomData d0 = s.get(DataComponents.CUSTOM_DATA);
        if (!w.kind.demon && (d0 == null || !d0.copyTag().contains("ExpiresAt"))) return true;
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
        p.getCooldowns().addCooldown(this, cooldownFor(p));
        p.displayClientMessage(Component.literal(kind.ability + "!").withStyle(kind.demon ? ChatFormatting.DARK_GRAY : ChatFormatting.AQUA, ChatFormatting.BOLD), true);
        return InteractionResultHolder.success(stack);
    }

    protected void hit(ServerPlayer p, LivingEntity t, float raw) {
        if (t == p || t.isAlliedTo(p)) return;
        t.hurt(p.damageSources().playerAttack(p), BalanceLaw.damage(p, t, raw, 0.6));
    }

    protected static void stripOne(LivingEntity t) {
        for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) if (e.getEffect().value().isBeneficial()) { t.removeEffect(e.getEffect()); return; }
    }

    protected static void eraseProjectiles(ServerPlayer p, AABB box) {
        for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, box)) if (pr.getOwner() != p) pr.discard();
    }

    private boolean technique(ServerPlayer p) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        switch (kind) {
            case DEMON_SLASHER_KATANA -> {
                Vec3 end = eye.add(look.scale(20));
                eraseProjectiles(p, new AABB(eye, end).inflate(1.6));
                for (LivingEntity t : GrimoireBook.along(p, eye, end, 1.6)) {
                    hit(p, t, 11);
                    stripOne(t);
                    var ex = TensuraStorages.getExistenceFrom(t);
                    if (ex != null) { ex.setMagicule(Math.max(0, ex.getMagicule() - EnergyHelper.getMaxMagicule(t) * 0.05)); ex.markDirty(); }
                }
                VfxSpawn.send(p.serverLevel(), VfxShape.ANTI_MAGIC_SLASH, eye, end, 0xFF2A0A30, 16, 1.6f);
            }
            case MIASMA_KATANA -> {
                Vec3 end = eye.add(look.scale(16));
                eraseProjectiles(p, new AABB(eye, end).inflate(1.2));
                for (LivingEntity t : GrimoireBook.along(p, eye, end, 1.2)) { hit(p, t, 12); t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0)); }
                VfxSpawn.send(p.serverLevel(), VfxShape.WIND_SLASH, eye, end, 0xFF1A0F2A, 16, 1.6f);
                VfxSpawn.send(p.serverLevel(), VfxShape.SPATIAL_RIFT, end, end.add(0, 1, 0), 0xFF5B3A8A, 16, 0.8f);
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
                for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(6))) {
                    if (pr.getOwner() == p) continue;
                    pr.setDeltaMovement(pr.getDeltaMovement().scale(-1.2));   // the flat of the blade bats spells back
                    pr.setOwner(p);
                    pr.hurtMarked = true;
                }
                for (LivingEntity t : GrimoireBook.around(p, p.position(), 6)) {
                    if (t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.3) continue;
                    hit(p, t, 13);
                    stripOne(t);
                }
                VfxSpawn.send(p.serverLevel(), VfxShape.ANTI_MAGIC_SLASH, eye, eye.add(look.scale(6)), 0xFF2A0A30, 18, 2.0f);
            }
            case DEMON_DWELLER -> {
                Vec3 end = eye.add(look.scale(16));
                eraseProjectiles(p, new AABB(eye, end).inflate(1.2));
                SpellRuntime.bolt(p, eye, look.scale(1.8), 1.0, 9, true, null, (b, t) -> {
                    hit(p, t, 10);
                    stripOne(t);
                    t.knockback(1.0, p.getX() - t.getX(), p.getZ() - t.getZ());
                }, null);
                VfxSpawn.send(p.serverLevel(), VfxShape.ANTI_MAGIC_SLASH, eye, end, 0xFF2A0A30, 16, 1.4f);
            }
            case DEMON_DESTROYER -> {
                List<LivingEntity> cleanse = new ArrayList<>(p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(4), x -> x == p || x.isAlliedTo(p)));
                for (LivingEntity t : cleanse) {
                    for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) if (!e.getEffect().value().isBeneficial()) t.removeEffect(e.getEffect());
                    t.getPersistentData().putLong("nusmp_misfortune_until", 0);
                    t.getPersistentData().putLong("nusmp_sealed_until", 0);
                }
                eraseProjectiles(p, p.getBoundingBox().inflate(5));
                for (LivingEntity t : GrimoireBook.around(p, p.position(), 3.5)) { for (int k = 0; k < 4; k++) stripOne(t); hit(p, t, 8); }
                VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.ANTI_MAGIC_SLASH, p, p.position().add(0, 1, 0).add(look), 0xFF2A0A30, 18, 1.2f);
            }
            case LICHT_DWELLER -> {
                int allies = 0;
                for (Player o : p.serverLevel().players()) if (o != p && o.distanceToSqr(p) < 16 * 16 && o.isAlliedTo(p)) allies++;
                Vec3 end = eye.add(look.scale(20));
                eraseProjectiles(p, new AABB(eye, end).inflate(2));
                for (LivingEntity t : GrimoireBook.along(p, eye, end, 2)) hit(p, t, 12 + 3 * Math.min(5, allies));
                BalanceLaw.heal(p, 4 + Math.min(5, allies));
                VfxSpawn.send(p.serverLevel(), VfxShape.WIND_SLASH, eye, end, 0xFFF4F8FF, 18, 2.2f);
            }
            case RIMEHEART -> {
                eraseProjectiles(p, p.getBoundingBox().inflate(5));
                frostBurst(p, p.position(), 5, 10, null);
                for (LivingEntity t : GrimoireBook.around(p, p.position(), 5)) {
                    Vec3 away = t.position().subtract(p.position()).normalize();
                    t.knockback(0.9, -away.x, -away.z);
                }
                VfxSpawn.send(p.serverLevel(), VfxShape.WATER_BURST, p.position().add(0, 0.6, 0), p.position(), 0xFF8FE9FF, 22, 1.7f);
                VfxSpawn.send(p.serverLevel(), VfxShape.WATER_RING, p.position().add(0, 0.1, 0), p.position().add(0, 1, 0), 0xFF3FE9FF, 20, 1.6f);
            }
            case LICHT_DESTROYER -> {
                eraseProjectiles(p, p.getBoundingBox().inflate(5));
                for (LivingEntity t : GrimoireBook.around(p, p.position(), 5)) {
                    if (t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.4) continue;
                    hit(p, t, 10);
                    for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) if (e.getEffect().value().isBeneficial()) t.removeEffect(e.getEffect());
                }
                VfxSpawn.send(p.serverLevel(), VfxShape.WIND_SLASH, eye, eye.add(look.scale(5)), 0xFFF4F8FF, 16, 1.6f);
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
                case MIASMA_KATANA -> target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
                case DEMON_SLAYER, DEMON_DWELLER, DEMON_SLASHER_KATANA -> stripOne(target);
                case DEMON_DESTROYER, LICHT_DESTROYER -> { for (int k = 0; k < 3; k++) stripOne(target); }
                case RIMEHEART -> {
                    target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 100, target.getTicksFrozen() + (pairedBlade(p) ? 80 : 50)));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
                    if (p.fallDistance > 0 && !p.onGround() && !p.isInWater()) {        // a critical hit shatters a mana burst
                        frostBurst(p, target.position(), 3, 5, target);
                        VfxSpawn.send(p.serverLevel(), VfxShape.WATER_BURST, target.position().add(0, target.getBbHeight() / 2, 0), target.position(), 0xFF8FE9FF, 16, 0.9f);
                    }
                }
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
        tip.add(Component.literal("  Cooldown: " + Math.round(kind.cooldown * com.newuniverse.nusmp.NUGameRules.cooldownShown() / 20) + " s").withStyle(ChatFormatting.DARK_GRAY));
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        if (kind == Kind.RIMEHEART) tip.add(Component.literal("  Off-hand magic sword: burst recharges 40% faster, hits freeze longer").withStyle(ChatFormatting.AQUA));
        if (d != null && d.copyTag().contains("ExpiresAt"))
            tip.add(Component.literal("Drawn from a grimoire: fades after a minute").withStyle(ChatFormatting.AQUA));
        if (kind.demon && d != null && d.copyTag().contains("OwnerName"))
            tip.add(Component.literal("Bound to " + d.copyTag().getString("OwnerName")).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
