package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.blackclover.GrimoireAcceptance;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.MagicType;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Grimoire Magic (Black Clover). Granted with a grimoire; its magic type, leaf count and devil
 * live in this skill's data. You must hold your own grimoire to cast.
 *
 * Modes: Bullet (beam), Burst (area), Ward (defense), Finisher (four-leaf or mastered),
 * Devil Union (five-leaf only).
 */
public class GrimoireMagicSkill extends Skill {
    private static final int BULLET = 0, BURST = 1, WARD = 2, FINISHER = 3, UNION = 4;
    private static final ResourceLocation LEGACY_ICON = ResourceLocation.withDefaultNamespace("textures/item/enchanted_book.png");
    /** The magic this skill is for (null = legacy generic skill that reads it from data). */
    private final MagicType fixed;
    private final ResourceLocation icon;

    public GrimoireMagicSkill() { this(null); }

    public GrimoireMagicSkill(MagicType fixed) {
        super(SkillType.UNIQUE);
        this.fixed = fixed;
        this.icon = fixed == null ? LEGACY_ICON
                : ResourceLocation.fromNamespaceAndPath("nusmp", "textures/skill/grimoire/" + fixed.name().toLowerCase() + ".png");
    }

    @Override public ResourceLocation getSkillIcon() { return icon; }

    public MagicType fixedMagic() { return fixed; }

    // ---------- stored data ----------
    static MagicType magic(ManasSkillInstance i) {
        if (i.getSkill() instanceof GrimoireMagicSkill g && g.fixed != null) return g.fixed;
        return MagicType.byName(i.getOrCreateTag().getString("Magic"));
    }
    static int leaves(ManasSkillInstance i) { return Math.max(3, i.getOrCreateTag().getInt("Leaves")); }
    static Devil devil(ManasSkillInstance i) { return Devil.byName(i.getOrCreateTag().getString("Devil")); }
    static boolean unionActive(ManasSkillInstance i, LivingEntity e) { return e.level().getGameTime() < i.getOrCreateTag().getLong("UnionUntil"); }

    private static com.newuniverse.nusmp.blackclover.GrimoireCover coverOf(ManasSkillInstance i) {
        return com.newuniverse.nusmp.blackclover.GrimoirePages.coverOf(i);
    }
    private static double power(ManasSkillInstance i) { return coverOf(i).damage; }
    private static double costMult(ManasSkillInstance i) { return coverOf(i).cost; }

    // ---------- modes ----------
    @Override public int getModes(ManasSkillInstance i) { return leaves(i) >= 5 ? 5 : 4; }

    @Override
    public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) {
        int n = getModes(i);
        return reverse ? (mode + n - 1) % n : (mode + 1) % n;
    }

    @Override public String getModeId(ManasSkillInstance i, int mode) { return "grimoire." + mode; }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        MagicType m = magic(i);
        return Component.literal(switch (mode) {
            case BULLET -> m.word + " Bullet";
            case BURST -> m.word + " Burst";
            case WARD -> m.word + " Ward";
            case FINISHER -> m.word + " Cataclysm";
            default -> "Devil Union" + (devil(i) != null ? ": " + devil(i).displayName : "");
        });
    }

    // ---------- casting ----------
    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!holdingOwnGrimoire(player, instance)) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, "Your grimoire's pages are still recharging."); return; }

        ServerLevel level = player.serverLevel();
        MagicType m = magic(instance);
        boolean mastered = instance.isMastered(player);
        double pow = power(instance) * (mastered ? 1.25 : 1.0) * NUConfig.DM_DAMAGE_MULT.get();
        double cost = costMult(instance) * NUConfig.DM_COST_MULT.get();

        switch (mode) {
            case BULLET -> {
                if (!SkillUtil.spendMagicules(player, 300 * cost)) return;
                Vec3 start = player.getEyePosition();
                Vec3 end = DMUtil.lookPoint(player, 40);
                Vec3 dir = end.subtract(start).normalize();
                Set<LivingEntity> hit = new HashSet<>();
                for (double d = 0; d < start.distanceTo(end); d += 0.5) {
                    Vec3 p = start.add(dir.scale(d));
                    level.sendParticles(m.particle(), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
                    hit.addAll(DMUtil.around(player, p, 1.2));
                    if (!hit.isEmpty() && m.hit != MagicType.HitEffect.PIERCE) break;
                }
                for (LivingEntity t : hit) strike(player, instance, t, 10 * pow);
                cooldown(instance, BULLET, 30);
            }
            case BURST -> {
                if (!SkillUtil.spendMagicules(player, 800 * cost)) return;
                Vec3 c = DMUtil.lookPoint(player, 32);
                double r = 5 + (leaves(instance) - 3);
                for (LivingEntity t : DMUtil.around(player, c, r)) strike(player, instance, t, 14 * pow);
                level.sendParticles(m.particle(), c.x, c.y + 1, c.z, 120, r / 2, 1, r / 2, 0.05);
                cooldown(instance, BURST, 120);
            }
            case WARD -> {
                if (!SkillUtil.spendMagicules(player, 600 * cost)) return;
                int ticks = (int) (600 * power(instance));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, leaves(instance) >= 4 ? 1 : 0));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, leaves(instance) - 2));
                player.addEffect(new MobEffectInstance(m.guardEffect(), ticks, 1));
                level.sendParticles(m.particle(), player.getX(), player.getY() + 1, player.getZ(), 40, 0.6, 0.8, 0.6, 0.02);
                cooldown(instance, WARD, 600);
            }
            case FINISHER -> {
                if (leaves(instance) < 4 && !mastered) { SkillUtil.fail(player, "Master your magic to unlock this page."); return; }
                if (!SkillUtil.spendMagicules(player, 3000 * cost)) return;
                Vec3 c = DMUtil.lookPoint(player, 48);
                for (LivingEntity t : DMUtil.around(player, c, 9)) strike(player, instance, t, 35 * pow);
                level.sendParticles(m.particle(), c.x, c.y + 1, c.z, 400, 4, 2, 4, 0.1);
                level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 0.7F);
                cooldown(instance, FINISHER, 1200);
            }
            default -> {
                Devil devil = devil(instance);
                if (devil == null) return;
                if (!SkillUtil.spendMagicules(player, 6000 * cost)) return;
                int ticks = NUConfig.DEVIL_UNION_SECONDS.get() * 20;
                instance.getOrCreateTag().putLong("UnionUntil", level.getGameTime() + ticks);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 2));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 1));
                level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1, player.getZ(), 150, 0.8, 1.2, 0.8, 0.05);
                level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.7F, 0.6F);
                player.getServer().getPlayerList().broadcastSystemMessage(Component.literal(player.getName().getString()
                        + " has entered Devil Union with " + devil.displayName + "!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
                cooldown(instance, UNION, 6000);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.3F);
        SkillUtil.castVfx(player, 0xFFFFD86B);
        instance.addMasteryPoint(player);
        instance.markDirty();
    }

    private static void cooldown(ManasSkillInstance i, int mode, int ticks) { i.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(ticks)), mode); }

    /** Damage + the magic type's effect. */
    private static void strike(ServerPlayer player, ManasSkillInstance instance, LivingEntity t, double damage) {
        MagicType m = magic(instance);
        ServerLevel level = player.serverLevel();
        double dmg = m.hit == MagicType.HitEffect.PIERCE ? damage * 1.3 : damage;
        t.hurt(player.damageSources().indirectMagic(player, player), (float) dmg);
        Vec3 away = t.position().subtract(player.position()).normalize();
        switch (m.hit) {
            case BURN -> t.igniteForSeconds(5);
            case FREEZE -> { t.setTicksFrozen(t.getTicksRequiredToFreeze() + 140); t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3)); }
            case SHOCK -> {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) { bolt.moveTo(t.getX(), t.getY(), t.getZ()); bolt.setVisualOnly(true); level.addFreshEntity(bolt); }
                t.hurt(player.damageSources().indirectMagic(player, player), (float) (damage * 0.3));
            }
            case SLOW -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3));
            case POISON -> t.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 1));
            case WITHER -> t.addEffect(new MobEffectInstance(MobEffects.WITHER, 140, 1));
            case BLIND -> { t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0)); t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0)); }
            case LEVITATE -> t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 1));
            case PULL -> { t.setDeltaMovement(away.scale(-1.2).add(0, 0.3, 0)); t.hurtMarked = true; }
            case PUSH -> { t.setDeltaMovement(away.scale(1.5).add(0, 0.4, 0)); t.hurtMarked = true; }
            case WEAKEN -> t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 2));
            case DRAIN -> player.heal((float) (damage * 0.25));
            case NULLIFY -> {
                for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) {
                    if (e.getEffect().value().isBeneficial()) t.removeEffect(e.getEffect());
                }
            }
            default -> { }
        }
    }

    /** True if the player holds their own grimoire. Summons it back if it's lost. */
    private static boolean holdingOwnGrimoire(ServerPlayer player, ManasSkillInstance instance) {
        for (ItemStack s : com.newuniverse.nusmp.blackclover.GrimoireSlot.ready(player)) {
            if (GrimoireItem.isOwnedBy(s, player.getUUID())) return true;
        }
        for (ItemStack s : player.getInventory().items) {
            if (GrimoireItem.isOwnedBy(s, player.getUUID())) {
                SkillUtil.fail(player, "Put your grimoire in your Grimoire Slot to cast.");
                return false;
            }
        }
        player.getInventory().placeItemBackInInventory(GrimoireItem.create(player, coverOf(instance), magic(instance), devil(instance)));
        player.displayClientMessage(Component.literal("Your grimoire returns to you.").withStyle(ChatFormatting.GOLD), true);
        return false;
    }

    // ---------- Devil Union powers ----------
    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return true; }

    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity living) {
        if (!(living instanceof ServerPlayer player) || player.tickCount % 20 != 0 || !unionActive(instance, player)) return;
        Devil devil = devil(instance);
        if (devil == null) return;
        ServerLevel level = player.serverLevel();
        for (LivingEntity t : DMUtil.around(player, player.position(), 8)) {
            switch (devil) {
                case ZAGRED -> { t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4)); t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 2)); }
                case MEGICULA -> t.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
                case LUCIFERO -> { t.setDeltaMovement(0, -1.5, 0); t.hurtMarked = true; t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 5)); }
                case ASTAROTH -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 6));
                case LILITH -> t.setTicksFrozen(t.getTicksRequiredToFreeze() + 40);
                case NAHAMAH -> t.igniteForSeconds(3);
                default -> { }
            }
        }
        if (devil == Devil.ASTAROTH) player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 2));
        level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 0.2, player.getZ(), 8, 1.5, 0.1, 1.5, 0.0);
    }

    @Override
    public boolean onDamageEntity(ManasSkillInstance instance, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        if (!unionActive(instance, owner)) return true;
        Devil devil = devil(instance);
        if (devil == Devil.BEELZEBUB) amount.set(amount.get() * 1.5F);
        if (devil == Devil.WALGNER) owner.heal(amount.get() * 0.3F);
        return true;
    }

    @Override
    public boolean onBeingDamaged(ManasSkillInstance instance, LivingEntity owner, DamageSource source, float amount) {
        // Liebe's Anti-Magic: magic can't touch an Anti-Magic user in Devil Union
        if (devil(instance) == Devil.LIEBE && unionActive(instance, owner)
                && (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(DamageTypeTags.WITCH_RESISTANT_TO))) {
            return false;
        }
        return true;
    }

    // ---------- Despair: a four-leaf can darken into a five-leaf ----------
    @Override
    public boolean onDeath(ManasSkillInstance instance, LivingEntity owner, DamageSource source) {
        if (!(owner instanceof ServerPlayer player)) return true;
        com.newuniverse.nusmp.blackclover.GrimoireCover darkened = coverOf(instance).despair();
        if (darkened == null) return true;
        if (player.getRandom().nextDouble() >= NUConfig.DESPAIR_CHANCE.get()) return true;
        Devil devil = GrimoireAcceptance.randomDevil(player.getRandom());
        CompoundTag tag = instance.getOrCreateTag();
        tag.putInt("Leaves", 5);
        tag.putString("Cover", darkened.name());
        tag.putString("Devil", devil.name());
        instance.markDirty();
        // replace their old grimoire with the darkened one
        player.getInventory().clearOrCountMatchingItems(s -> GrimoireItem.isOwnedBy(s, player.getUUID()), -1, player.inventoryMenu.getCraftSlots());
        com.newuniverse.nusmp.blackclover.GrimoireSlot.replaceOwned(player, old -> ItemStack.EMPTY);
        player.getServer().getPlayerList().broadcastSystemMessage(Component.literal("In " + player.getName().getString()
                + "'s final despair, their grimoire darkens... a fifth leaf appears. The devil " + devil.displayName + " has awakened.")
                .withStyle(ChatFormatting.DARK_RED), false);
        return true; // they still die; the grimoire returns to them as a five-leaf
    }

    /** Used by the admin command. */
    public static String describe(Player p, ManasSkillInstance i) {
        Devil d = devil(i);
        return coverOf(i).displayName() + " (" + coverOf(i).kingdom.displayName + ") of " + magic(i).displayName + (d != null ? " (devil: " + d.displayName + ")" : "");
    }
}
