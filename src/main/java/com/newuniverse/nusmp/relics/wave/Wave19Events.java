package com.newuniverse.nusmp.relics.wave;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Wave 19, the handlers: the Scroll of Absorption's reaction, the double radiant damage of Blood Chocolates, the iron fist of Mace Hand, charmed
 * creatures, the come-down of Fey Honey, the flaws of the Mug of The Boss, the crumbling of Facsimile copies. Per-player work runs once a second,
 * staggered by player id; the damage handler first checks the cheap things (a held scroll, a timer) before it looks at the damage type.
 */
final class Wave19Events {
    private Wave19Events() {}

    static final String K_MACE = "mace_until";

    // ---------------------------------------------------------------- damage
    static void onIncoming(LivingIncomingDamageEvent e) {
        float amount = e.getAmount();
        if (amount <= 0f) return;
        DamageSource src = e.getSource();
        Entity attacker = src.getEntity();
        // Mace Hand: a bare iron fist hits harder
        if (attacker instanceof ServerPlayer a && src.getDirectEntity() == a && a.getMainHandItem().isEmpty() && Wave19Util.running(a, K_MACE)) {
            e.setAmount(amount + (float) Wave19Rules.MACE_BONUS);
            amount = e.getAmount();
        }
        if (!(e.getEntity() instanceof ServerPlayer p) || src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        boolean holding = Wave19Scrolls.isAbsorption(p.getMainHandItem()) || Wave19Scrolls.isAbsorption(p.getOffhandItem());
        boolean vulnerable = Wave19Util.running(p, Wave19Goods.K_BLOOD);
        if (!holding && !vulnerable) return;
        String path = src.typeHolder().unwrapKey().map(k -> k.location().getPath()).orElse("");
        if (holding && attacker instanceof LivingEntity && Wave19Rules.isSpellPath(path)) {
            String id = src.typeHolder().unwrapKey().map(k -> k.location().toString()).orElse("minecraft:magic");
            if (Wave19Scrolls.absorb(p, amount, id)) {
                e.setCanceled(true);
                return;
            }
        }
        if (vulnerable && Wave19Rules.isRadiantPath(path)) e.setAmount(Wave19Rules.radiantDamage(amount, true, true));
    }

    /** A blow from a charmer frees a creature it had charmed. */
    static void onDamagePost(LivingDamageEvent.Post e) {
        Wave19Field.struck(e.getEntity(), e.getSource().getEntity());
    }

    static void onChangeTarget(LivingChangeTargetEvent e) {
        LivingEntity target = e.getNewAboutToBeSetTarget();
        if (target != null && Wave19Field.spares(e.getEntity(), target)) e.setCanceled(true);
    }

    // ---------------------------------------------------------------- the player's second
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        if ((p.tickCount + p.getId()) % 20 != 7) return;
        CompoundTag d = Wave19Util.peek(p);
        if (d != null) {
            if (d.getLong(Wave19Goods.K_HONEY) > 0) honeyPass(p, d);
            if (d.getLong(Wave19Goods.K_MUG) > 0) mugPass(p, d);
        }
        if (p.tickCount % 40 < 20) crumble(p);
    }

    /** The honey wears off: the Constitution save, and a wild magic surge on a failure. */
    private static void honeyPass(ServerPlayer p, CompoundTag d) {
        if (d.getLong(Wave19Goods.K_HONEY) > p.level().getGameTime()) return;
        d.remove(Wave19Goods.K_HONEY);
        boolean spared = Wave19Rules.honeySpared(Wave19Util.magiculeFraction(p), Wave19Rules.d20(p.getRandom()::nextInt));
        if (spared) {
            Wave19Util.tell(p, "Fey Honey: the sweetness fades, and you shrug off the aftermath.", ChatFormatting.AQUA);
            return;
        }
        Wave19Util.tell(p, "Fey Honey: the charm fades, and a burst of Fey energy erupts from you!", ChatFormatting.LIGHT_PURPLE);
        Wave19Rules.Surge s = Wave19Surge.roll(p);
        Wave19Util.say(p, "Wild magic: " + Wave19Surge.line(s));
    }

    /** The flaws of the title: a voice in the bath, a clumsy hand on the wheel, a little hunger for all the volunteering. */
    private static void mugPass(ServerPlayer p, CompoundTag d) {
        long now = p.level().getGameTime();
        if (d.getLong(Wave19Goods.K_MUG) <= now) {
            d.remove(Wave19Goods.K_MUG);
            Wave19Util.tell(p, "Mug of The Boss: the title fades. Back to being nobody in particular.", ChatFormatting.GRAY);
            return;
        }
        p.causeFoodExhaustion(0.05f);
        int second = p.tickCount / 20;
        ServerLevel sl = p.serverLevel();
        if (p.isInWater() && second % 4 == 0) {
            sl.sendParticles(ParticleTypes.NOTE, p.getX(), p.getY() + 2.3, p.getZ(), 3, 0.4, 0.2, 0.4, 1.0);
            sl.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BANJO.value(), SoundSource.PLAYERS, 0.8f, 0.6f + p.getRandom().nextFloat());
            if (second % 20 == 0) Wave19Util.tell(p, "You can't help singing in the water.", ChatFormatting.DARK_GRAY);
        }
        Entity v = p.getVehicle();
        if (v instanceof Boat || v instanceof AbstractMinecart) Wave19Util.give(p, "minecraft:unluck", 60, 2);
    }

    /** A facsimile copy past its time falls to dust (the inventory is looked at every other second, per player). */
    private static void crumble(ServerPlayer p) {
        long now = p.level().getGameTime();
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            CustomData cd = s.get(DataComponents.CUSTOM_DATA);
            if (cd == null || !cd.contains(Wave19Scrolls.K_FACE_UNTIL)) continue;
            if (cd.copyTag().getLong(Wave19Scrolls.K_FACE_UNTIL) > now) continue;
            inv.setItem(i, ItemStack.EMPTY);
            p.serverLevel().sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 1.0, p.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
            Wave19Util.tell(p, "A facsimile crumbles to dust in your pack.", ChatFormatting.GRAY);
        }
    }

    // ---------------------------------------------------------------- life and death
    static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        CompoundTag d = Wave19Util.peek(p);
        if (d != null) {
            d.remove(Wave19Goods.K_HONEY);
            d.remove(Wave19Goods.K_MUG);
            d.remove(Wave19Goods.K_BLOOD);
            d.remove(K_MACE);
        }
        Wave19Field.forget(p);
    }

    /** The summoned crab is not a farm: it drops nothing and gives no experience. */
    static void onDrops(LivingDropsEvent e) {
        if (e.getEntity().getPersistentData().getBoolean(Wave19Field.CRAB_MARK)) e.setCanceled(true);
    }

    static void onExperience(LivingExperienceDropEvent e) {
        if (e.getEntity().getPersistentData().getBoolean(Wave19Field.CRAB_MARK)) e.setCanceled(true);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Wave19Field.forget(p);
    }

    static void onServerTick(ServerTickEvent.Post e) { Wave19Field.tick(e.getServer()); }

    /** A crab saved by a stop or a crash is not one this session is tracking: it goes as it joins. */
    static void onJoin(EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide() && Wave19Field.orphan(e.getEntity())) e.setCanceled(true);
    }

    static void onStopping(ServerStoppingEvent e) { Wave19Field.clear(e.getServer()); }
}
