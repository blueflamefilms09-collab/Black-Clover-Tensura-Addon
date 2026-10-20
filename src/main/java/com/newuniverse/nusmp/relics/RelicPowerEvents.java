package com.newuniverse.nusmp.relics;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.104.0, the attune lock on the server (docs/relic_attune_lock.md). ONE central rule, {@link RelicPower}: a relic that needs attunement and is not
 * attuned to the one who holds it deals a fraction of its damage.
 * <ul>
 *   <li><b>Melee</b> (vanilla attacks and Better Combat's, which end in the same {@code Player.attack}): a {@link LivingIncomingDamageEvent} whose damage
 *       is the wielder's own blow (player attack type, the striker is the credited attacker) scales the final amount by the factor of the weapon in
 *       the attacker's main hand at that moment. Once per hit; mobs that wear relics count as attuned.</li>
 *   <li><b>Arrows, bolts, thrown tridents</b>: when a projectile enters the world owned by a player who holds an unattuned relic launcher (the item in use,
 *       else a hand), it is tagged with the factor ({@link RelicPower#PROJECTILE_TAG}); the same event hook scales the hit of a tagged projectile. A
 *       projectile that is not tagged (a vanilla bow in the other hand, a spell) is never touched.</li>
 *   <li><b>Acts and passives</b>: they are already off while a relic is not attuned ({@link Attune#works}, enforced in {@link RelicEngine#activate} and
 *       in {@link Relics#loadout}); that gate is left as it is and is never stacked with a second cut. The act path still multiplies the act-power helper
 *       ({@code ActPower}) by {@link #actFactor}, which is 1.0 whenever the gate let the act through.</li>
 *   <li><b>Feedback</b>: a tooltip line on the weapon, and the first nerfed hit of a session shows an action-bar line.</li>
 * </ul>
 * Only {@code Relics.defOf(stack) != null && def.needsAttunement()} is ever affected: Djinn weapons, Requip, hero weapons, loot gear, Elsdocia and every
 * non-relic item are outside the rule.
 */
public final class RelicPowerEvents {
    private RelicPowerEvents() {}

    /** Players already told this session that an unattuned relic is weak (cleared at logout and server stop). */
    private static final Set<UUID> TOLD = ConcurrentHashMap.newKeySet();

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.LOW, RelicPowerEvents::onIncomingDamage);              // after the other damage hooks: the amount is the final one
        bus.addListener(RelicPowerEvents::onProjectileJoin);
        bus.addListener(RelicPowerEvents::onTooltip);
        bus.addListener(RelicPowerEvents::onLogout);
        bus.addListener(RelicPowerEvents::onServerStopping);
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { TOLD.remove(e.getEntity().getUUID()); }

    private static void onServerStopping(ServerStoppingEvent e) { TOLD.clear(); }

    /** Test hook: forget that this player was told. */
    static void forget(UUID id) { TOLD.remove(id); }

    // ---------------------------------------------------------------- the factor
    /** The damage factor of this stack in the hands of this wielder: 1.0 for a non-relic, a relic that needs no attunement, an attuned one, or a mob. */
    public static double factorOf(ItemStack stack, LivingEntity wielder) {
        return factorOf(stack, wielder, RelicPowerConfig.current());
    }

    static double factorOf(ItemStack stack, LivingEntity wielder, RelicPower.Config config) {
        RelicDef d = Relics.defOf(stack);
        if (d == null || wielder == null) return 1.0;
        return RelicPower.factor(d.id(), Attune.isMine(stack, wielder), d.needsAttunement(), config);
    }

    /** What an act's damage is multiplied by besides the tier: 1.0 today, because an act of an unattuned relic is refused before it runs. */
    public static double actFactor(ItemStack stack, LivingEntity wielder) { return factorOf(stack, wielder); }

    // ---------------------------------------------------------------- melee and projectile hits
    private static boolean attackType(DamageSource src) { return src.is(DamageTypes.PLAYER_ATTACK) || src.is(DamageTypes.MOB_ATTACK) || src.is(DamageTypes.MOB_ATTACK_NO_AGGRO); }

    /** The factor a hit carries, or 1.0 when it is not the blow or the shot of an unattuned relic. */
    static double hitFactor(DamageSource src) {
        Entity direct = src.getDirectEntity();
        if (direct instanceof Projectile pr) {
            CompoundTag d = pr.getPersistentData();
            return d.contains(RelicPower.PROJECTILE_TAG, Tag.TAG_DOUBLE) ? RelicPower.clampPower(d.getDouble(RelicPower.PROJECTILE_TAG)) : 1.0;
        }
        if (src.getEntity() instanceof Player p && RelicPower.isMeleeBlow(attackType(src), direct == p)) return factorOf(p.getMainHandItem(), p);
        return 1.0;
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (!RelicPowerConfig.lockOn()) return;
        double f = hitFactor(e.getSource());
        if (!RelicPower.nerfed(f)) return;
        e.setAmount(RelicPower.scale(e.getAmount(), f));
        if (e.getSource().getEntity() instanceof ServerPlayer p) tellOnce(p, f);
    }

    private static void tellOnce(ServerPlayer p, double f) {
        if (!TOLD.add(p.getUUID())) return;
        p.displayClientMessage(Component.translatableWithFallback("message.nusmp.relic.attune_lock.hit", "This relic only deals %s%% of its power until you attune it",
                RelicPower.percent(f)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    // ---------------------------------------------------------------- arrows, bolts, thrown weapons
    private static void onProjectileJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || e.loadedFromDisk() || !(e.getEntity() instanceof Projectile pr) || !(pr.getOwner() instanceof Player owner)) return;
        double f = launcherFactor(owner, pr);
        if (RelicPower.nerfed(f) && RelicPowerConfig.lockOn()) pr.getPersistentData().putDouble(RelicPower.PROJECTILE_TAG, f);
    }

    /** The factor of the relic that launched this projectile: the item in use (a drawn bow), else a hand holding a relic bow or crossbow; 1.0 when none. */
    static double launcherFactor(LivingEntity owner, Projectile pr) {
        ItemStack launcher = launcher(owner, pr);
        return launcher.isEmpty() ? 1.0 : factorOf(launcher, owner);
    }

    static ItemStack launcher(LivingEntity owner, Projectile pr) {
        ItemStack use = owner.isUsingItem() ? owner.getUseItem() : ItemStack.EMPTY;
        if (launchesRanged(use) || (pr instanceof ThrownTrident && Relics.defOf(use) != null)) return use;
        for (ItemStack hand : new ItemStack[]{owner.getMainHandItem(), owner.getOffhandItem()}) {
            if (launchesRanged(hand) || (pr instanceof ThrownTrident && Relics.defOf(hand) != null)) return hand;
        }
        return ItemStack.EMPTY;
    }

    /** A relic that shoots: a registered relic bow or crossbow, or an adopted item that is a vanilla-style projectile weapon. */
    static boolean launchesRanged(ItemStack s) {
        RelicDef d = Relics.defOf(s);
        return d != null && (d.isRanged() || s.getItem() instanceof ProjectileWeaponItem);
    }

    // ---------------------------------------------------------------- the tooltip
    private static void onTooltip(ItemTooltipEvent e) {
        Player p = e.getEntity();
        ItemStack s = e.getItemStack();
        if (p == null || s.isEmpty()) return;
        RelicDef d = Relics.defOf(s);
        if (d == null || d.slot() != RelicSlot.MAIN_HAND || !d.needsAttunement()) return;      // only weapons deal damage; the other relics simply do not answer
        double f = factorOf(s, p);
        if (!RelicPower.nerfed(f)) return;
        boolean other = Attune.owner(s) != null;
        List<Component> tip = e.getToolTip();
        tip.add(other
                ? Component.translatableWithFallback("tooltip.nusmp.relic.attuned_other", "Attuned to someone else: %s%% power. Hold sneak + use for 5 s to take it over", RelicPower.percent(f)).withStyle(ChatFormatting.RED)
                : Component.translatableWithFallback("tooltip.nusmp.relic.unattuned_power", "Unattuned: %s%% power. Hold sneak + use for 5 s to attune", RelicPower.percent(f)).withStyle(ChatFormatting.RED));
    }

    // ---------------------------------------------------------------- /multiverse relic power [player]
    /** The report of what a player carries: needs attunement?, attuned to whom, the factor in force. One component per line. */
    public static List<Component> report(ServerPlayer p) {
        List<Component> out = new ArrayList<>();
        RelicPower.Config cfg = RelicPowerConfig.current();
        out.add(Component.literal(p.getName().getString() + ": attune lock " + (cfg.lock() ? "on, unattuned power " + RelicPower.label(cfg.unattunedPower()) : "off (full power for everyone)")));
        int n = 0;
        ItemStack main = p.getMainHandItem();
        for (ItemStack s : RelicEngine.carriedStacks(p)) {
            RelicDef d = Relics.defOf(s);
            if (d == null) continue;
            n++;
            UUID owner = Attune.owner(s);
            String to = !d.needsAttunement() ? "-" : owner == null ? "nobody" : owner.equals(p.getUUID()) ? "this player" : nameOf(p.getServer(), owner);
            double f = factorOf(s, p, cfg);
            out.add(Component.literal("  " + d.cardName() + (s == main ? " (main hand)" : "") + ": needs attunement " + (d.needsAttunement() ? "yes" : "no")
                    + ", attuned to " + to + ", factor " + RelicPower.label(f)));
        }
        if (n == 0) out.add(Component.literal("  carries no relic"));
        return out;
    }

    private static String nameOf(MinecraftServer server, UUID id) {
        try {
            if (server != null) {
                ServerPlayer on = server.getPlayerList().getPlayer(id);
                if (on != null) return on.getName().getString();
                var cache = server.getProfileCache();
                if (cache != null) {
                    Optional<com.mojang.authlib.GameProfile> gp = cache.get(id);
                    if (gp.isPresent()) return gp.get().getName();
                }
            }
        } catch (RuntimeException ignored) {
            // fall through to the id
        }
        return id.toString().substring(0, 8);
    }
}
