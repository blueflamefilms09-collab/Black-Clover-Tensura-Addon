package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Wave 17: the game-bus side. Each listener starts with the cheapest test (is a wearer even involved), so the other thousand events a second cost
 * nothing: Fury of the Fallen (double damage and the red glow below 20% health), Life's Emergency (the death it catches), the Sheriff Badge's bluff
 * (raiders who stop hunting you), the Madama Cupcake's hunger floor, the Crystal Dragon's clock, and the /pouch command with which a linked companion
 * opens the pouches without holding them.
 */
public final class Wave17Events {
    private Wave17Events() {}

    static final String F_FURY = "w17_fury", F_BADGE = "w17_badge";
    private static final String K_BLUFFED = "w17_bluffed_until";
    private static final int RED = 0xFF2A3A;

    /** Players whose badge is lit now (so the message comes once when it lights). */
    private static final Set<UUID> LIT = new HashSet<>();

    public static void register() {
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(Wave17Events::onIncomingDamage);
        bus.addListener(Wave17Events::onDeath);
        bus.addListener(Wave17Events::onChangeTarget);
        bus.addListener(Wave17Events::onPlayerTick);
        bus.addListener(Wave17Events::onServerTick);
        bus.addListener(Wave17Events::onLogout);
        bus.addListener(Wave17Events::onServerStopping);
        bus.addListener(Wave17Events::onCommands);
    }

    // ---------------------------------------------------------------- Fury of the Fallen
    /** Below 20% health every weapon blow of the wearer does double damage (melee, and arrows and bolts they loosed). */
    private static void onIncomingDamage(LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof ServerPlayer atk)) return;
        if (!Wave17Rules.furyActive(atk.getHealth(), atk.getMaxHealth())) return;
        boolean weapon = src.getDirectEntity() == atk || src.is(DamageTypeTags.IS_PROJECTILE);
        if (!weapon || src.is(DamageTypeTags.IS_EXPLOSION) || !RelicFlags.has(atk, F_FURY)) return;
        float before = e.getAmount(), after = Wave17Rules.furyDamage(before);
        if (e.getEntity() instanceof net.minecraft.world.entity.player.Player victim) after = Math.min(after, Math.max(before, victim.getMaxHealth() * 0.3f));   // never past the 30% player cap
        e.setAmount(after);
    }

    // ---------------------------------------------------------------- Life's Emergency
    /** A killing blow on a wearer whose cuff holds stored life: the death is caught, the stored life restores health, and the cycle starts again. */
    private static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (!Relics.worn(p, "lifes_emergency")) return;
        ItemStack cuff = Wave17Kit.find(p, "lifes_emergency");
        RelicDef def = Relics.defOf(cuff);
        if (cuff.isEmpty() || def == null) return;
        int stored = Wave17Acts.lifeStored(p, cuff, def.grade().tier());
        if (stored <= 0) return;
        float heal = Wave17Rules.lifeHeal(stored, p.getMaxHealth());
        e.setCanceled(true);
        p.setHealth(Math.max(1f, Math.min(p.getMaxHealth(), heal)));
        p.removeAllEffects();
        p.setRemainingFireTicks(0);
        Wave17Kit.give(p, "minecraft:regeneration", 100, 1);
        Wave17Kit.give(p, "minecraft:resistance", 100, 1);
        Wave17Acts.lifeSpent(p, cuff);
        ServerLevel level = p.serverLevel();
        Fx.burst(level, p.position(), 0xE0364A);
        Fx.ring(level, p.position(), 2.5, 0xE0364A);
        Fx.say(p, "Life's Emergency: the cuff spends its " + stored + " stored life at death's door and you are back on your feet. It starts to fill again at the next dawn.");
    }

    // ---------------------------------------------------------------- Plastic Sheriff Badge
    /** Raiders (illagers, ravagers, witches) who would go for the badge's wearer test the bluff once; if they believe it they leave the wearer alone for ten seconds. */
    private static void onChangeTarget(LivingChangeTargetEvent e) {
        if (!(e.getNewAboutToBeSetTarget() instanceof ServerPlayer p)) return;
        LivingEntity mob = e.getEntity();
        if (!mob.getType().is(EntityTypeTags.RAIDERS) || !RelicFlags.has(p, F_BADGE)) return;
        long now = p.serverLevel().getGameTime();
        var data = mob.getPersistentData();
        if (data.getLong(K_BLUFFED) > now) { e.setCanceled(true); return; }
        if (Wave17Rules.bluff(Wave17Rules.die(n -> p.getRandom().nextInt(n), 20), Wave17Rules.bluffDc(mob.getMaxHealth()))) {
            data.putLong(K_BLUFFED, now + Wave17Rules.BLUFF_CALM_TICKS);
            e.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------- the player pass
    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        int phase = (p.tickCount + p.getId()) % 40;
        if (phase == 0) cupcake(p);
        if (phase % 10 == 0) fury(p);
    }

    /** While a cupcake feeds you, hunger never drops below 17. */
    private static void cupcake(ServerPlayer p) {
        long until = Wave17Kit.peekLong(p, "cupcake_until");
        if (until <= 0L) return;
        long now = Wave17Kit.clock(p.serverLevel());
        int food = p.getFoodData().getFoodLevel();
        int want = Wave17Rules.cupcakeFood(food, now, until);
        if (want != food) p.getFoodData().setFoodLevel(want);
        if (now < until && p.hasEffect(MobEffects.HUNGER)) p.removeEffect(MobEffects.HUNGER);
    }

    /** The red glow of the badge: dust and sparks around the wearer, once, with a word, when it lights. */
    private static void fury(ServerPlayer p) {
        if (!Wave17Rules.furyActive(p.getHealth(), p.getMaxHealth())) { if (!LIT.isEmpty()) LIT.remove(p.getUUID()); return; }
        if (!RelicFlags.has(p, F_FURY)) return;
        ServerLevel level = p.serverLevel();
        RukhFx.spray(level, Wave17Kit.align(RED), p.getX(), p.getY() + 1.0, p.getZ(), 6, 0.45, 0.7, 0.45, 0.0);
        RukhFx.spray(level, RukhFx.Alignment.LIGHT, p.getX(), p.getY() + 1.0, p.getZ(), 2, 0.4, 0.6, 0.4, 0.0);
        if (LIT.add(p.getUUID())) Wave17Kit.hint(p, "Fury of the Fallen: you glow red, and every blow you land hits twice as hard.", ChatFormatting.RED);
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { LIT.remove(e.getEntity().getUUID()); }

    // ---------------------------------------------------------------- the server pass
    private static void onServerTick(ServerTickEvent.Post e) { Wave17Dragon.tick(e.getServer()); }

    private static void onServerStopping(ServerStoppingEvent e) {
        Wave17Dragon.clear();
        LIT.clear();
    }

    // ---------------------------------------------------------------- /pouch
    private static void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("pouch")
                .executes(ctx -> list(ctx.getSource()))
                .then(Commands.literal("open")
                        .then(Commands.argument("owner", StringArgumentType.word()).suggests((c, b) -> owners(c, b, Wave17Vault.KIND_SHARED))
                                .executes(ctx -> open(ctx.getSource(), StringArgumentType.getString(ctx, "owner"), Wave17Vault.KIND_SHARED, 0, "Shared Pouches of Transfer"))))
                .then(Commands.literal("far")
                        .then(Commands.argument("owner", StringArgumentType.word()).suggests((c, b) -> owners(c, b, Wave17Vault.KIND_TRANSFER))
                                .executes(ctx -> open(ctx.getSource(), StringArgumentType.getString(ctx, "owner"), Wave17Vault.KIND_TRANSFER, 1, "Far Bag")))));
    }

    private static List<Wave17Vault.Vault> mine(CommandSourceStack src, ServerPlayer p) { return Wave17Vault.get(src.getServer()).companionOf(p.getUUID()); }

    private static CompletableFuture<Suggestions> owners(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b, String kind) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) return b.buildFuture();
        Stream<String> names = mine(ctx.getSource(), p).stream().filter(v -> v.kind.equals(kind)).map(v -> v.ownerName);
        return SharedSuggestionProvider.suggest(names, b);
    }

    private static int list(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        if (p == null) return 0;
        List<Wave17Vault.Vault> links = mine(src, p);
        if (links.isEmpty()) {
            src.sendSuccess(() -> Component.literal("No one has linked you to their pouches. (Whoever carries Shared Pouches of Transfer or Bags of Transferrence can link you: you both crouch beside each other, they use the relic with sneak.)"), false);
            return 1;
        }
        for (Wave17Vault.Vault v : links) {
            String line = v.kind.equals(Wave17Vault.KIND_TRANSFER)
                    ? v.ownerName + "'s Bags of Transferrence: /pouch far " + v.ownerName
                    : v.ownerName + "'s Shared Pouches of Transfer: /pouch open " + v.ownerName;
            src.sendSuccess(() -> Component.literal(line), false);
        }
        return links.size();
    }

    private static int open(CommandSourceStack src, String owner, String kind, int side, String title) {
        ServerPlayer p = src.getPlayer();
        if (p == null) return 0;
        for (Wave17Vault.Vault v : mine(src, p)) {
            if (!v.kind.equals(kind) || !v.ownerName.equalsIgnoreCase(owner)) continue;
            v.open(p, side, title + " (" + v.ownerName + ")");
            return 1;
        }
        src.sendFailure(Component.literal("No one named " + owner + " has linked you to that."));
        return 0;
    }
}
