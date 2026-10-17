package com.newuniverse.nusmp.relics.djinn.dark;

import com.newuniverse.nusmp.entity.zogratis.LuciusBoss;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import com.newuniverse.nusmp.relics.djinn.DjinnEngine;
import com.newuniverse.nusmp.relics.djinn.DjinnItems;
import com.newuniverse.nusmp.relics.djinn.DjinnLedger;
import com.newuniverse.nusmp.relics.djinn.DjinnState;
import com.newuniverse.nusmp.relics.djinn.VesselAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

/**
 * The Dark Djinn on the server: the glue between {@link DarkRules} (the numbers) and the Djinn engine. A Djinn turns dark by a choice
 * ({@link #drink}: the owner sneaks with a Dark Rukh Shard and the bound vessel) or, with a shard in the pack and a Full Equip run to the edge of
 * the bearer's nature, by itself ({@link #second}); it is cleansed with Holy Water ({@link #onRightClickItem}), by an admin, or by letting the
 * Djinn go. Nothing here deletes a skill, an item or a bond; the dark flag lives in the Djinn record ({@link DjinnState#dark()}), survives death,
 * and is told to every client in bit 3 (value 8) of the sync flags.
 */
public final class DarkDjinn {
    private DarkDjinn() {}

    /** The registry path of the Holy Water relic (relic wave 19) that cleanses. */
    public static final String HOLY_WATER = "holy_water";

    /** Called once from {@code DjinnEvents.register}. */
    public static void register(IEventBus bus) {
        bus.addListener(DarkDjinn::onRightClickItem);
        bus.addListener(EventPriority.LOWEST, DarkDjinn::onLuciusDeath);
    }

    // ---------------------------------------------------------------- the question everyone asks
    /** Is the Djinn this player calls dark? True only for an owner: a household member never has a dark Djinn. */
    public static boolean active(DjinnState s) { return s.dark() && s.owns(); }

    public static boolean active(ServerPlayer p) { return active(DjinnState.of(p)); }

    // ---------------------------------------------------------------- the drink
    /** The shard in one hand, the vessel in the other, sneaking: the Djinn drinks the black rukh. Nothing is spent unless it happens. */
    public static void drink(ServerPlayer p, ItemStack shard, ItemStack other) {
        DjinnState s = DjinnState.of(p);
        MinecraftServer server = p.getServer();
        if (server == null) return;
        Djinn d = s.djinn();
        boolean vessel = d != null && s.serial() != null && VesselAccess.boundDjinn(other) == d && s.serial().equals(VesselAccess.serial(other));
        int members = d == null ? 0 : DjinnLedger.get(server).household(d).size();
        boolean collapse = false;                                          // the Treasury Collapse went with the Living Dungeon (1.58.0); the rule keeps the flag
        DarkRules.Verdict v = DarkRules.drink(new DarkRules.Drink(p.isShiftKeyDown(), s.owns(), s.lent(), members, s.dark(), vessel, s.trust(), s.phase(),
                collapse, DjinnItems.isDarkShard(shard), p.isCreative()));
        if (v != DarkRules.Verdict.OK) {
            p.displayClientMessage(Component.literal(refusal(v)).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (!p.isCreative()) shard.shrink(1);
        darken(p, s);
        p.sendSystemMessage(Component.literal(d.displayName() + " drinks the black rukh. The vessel turns black and gold. Its power is greater, and it takes more of you for it.")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    private static String refusal(DarkRules.Verdict v) {
        return switch (v) {
            case NOT_SNEAKING -> "Sneak, with the Dark Rukh Shard in one hand and your vessel in the other, to feed it to your Djinn.";
            case NOT_OWNER -> "Only the owner of a Djinn can feed it the black rukh.";
            case HOUSEHOLD_LENT -> "A borrowed Djinn cannot drink the black rukh.";
            case HOUSEHOLD_MEMBERS -> "A dark Djinn cannot be lent: call your household back first (/multiverse djinn unshare).";
            case ALREADY_DARK -> "Your Djinn is already dark.";
            case NO_VESSEL -> "Hold the vessel of your own Djinn in the other hand.";
            case NEED_TRUST -> "Your Djinn does not trust you enough to drink it: " + DarkRules.DRINK_TRUST + " trust is needed.";
            case PHASE_ON -> "Dismiss the Djinn first.";
            case COLLAPSE -> "Not here: the Dungeon is coming down around you.";
            case NO_SHARD -> "You hold no Dark Rukh Shard.";
            default -> "Not now.";
        };
    }

    // ---------------------------------------------------------------- turning dark, turning back
    /** Sets the flag, re-applies what depends on it, tells the clients, and shows it. */
    public static void darken(ServerPlayer p, DjinnState s) {
        if (!s.owns()) return;
        s.setDark(true);
        DjinnEngine.refresh(p);
        ServerLevel l = p.serverLevel();
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.DARK, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.5, 0.9, 0.5, 0.02);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, p.getX(), p.getY() + 1.0, p.getZ(), 16, 0.5, 0.9, 0.5, 0.02);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.DARK, p.getX(), p.getY() + 1.0, p.getZ(), 24, 0.4, 0.8, 0.4, 0.05);
        l.playSound(null, p.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.0f, 0.5f);
        l.playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.0f, 0.5f);
    }

    /** The Djinn is cleansed (Holy Water, an admin): the flag goes, the Maso numbers return to the plain ones. */
    public static void purify(ServerPlayer p, DjinnState s) {
        if (!s.dark()) return;
        s.setDark(false);
        DjinnEngine.refresh(p);
        ServerLevel l = p.serverLevel();
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.5, 0.9, 0.5, 0.05);
        l.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.7f);
        l.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 1.2f);
    }

    // ---------------------------------------------------------------- Holy Water
    /** Sneak-using Holy Water with the vessel of your dark Djinn in the other hand cleanses it. Otherwise Holy Water is left to do what it did. */
    static void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isShiftKeyDown()) return;
        ItemStack water = e.getItemStack();
        if (!isHolyWater(water)) return;
        DjinnState s = DjinnState.of(p);
        if (!active(s)) return;
        ItemStack other = p.getItemInHand(e.getHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        Djinn d = s.djinn();
        if (s.serial() == null || VesselAccess.boundDjinn(other) != d || !s.serial().equals(VesselAccess.serial(other))) return;
        e.setCancellationResult(InteractionResult.SUCCESS);
        e.setCanceled(true);                                              // the water is poured over the vessel, not thrown
        if (!p.isCreative()) water.shrink(1);
        purify(p, s);
        p.sendSystemMessage(Component.literal("The blessed water runs over the vessel and the black rukh lets go of " + d.displayName() + ". It is its own again.")
                .withStyle(ChatFormatting.AQUA));
    }

    static boolean isHolyWater(ItemStack s) {
        RelicDef def = Relics.defOf(s);
        return def != null && HOLY_WATER.equals(def.id());
    }

    // ---------------------------------------------------------------- once a second, from the engine
    /**
     * Called by the engine once a second while a Djinn is out (phase 1-3). A dark Full Equip unsettles what is near; a plain Full Equip run to the
     * edge of the bearer's nature may fall to the black rukh, but only if the bearer carries a shard.
     */
    public static void second(ServerPlayer p, DjinnState s, int phase) {
        if (!s.owns() || p.isCreative() || p.isSpectator()) return;
        if (s.dark()) {
            if (phase >= 3) unease(p);
            return;
        }
        if (phase < 3 || s.assimilation() < DarkRules.FALL_ASSIMILATION) return;
        boolean has = hasShard(p);
        boolean collapse = false;                                          // same
        if (!DarkRules.falls(p.getRandom().nextDouble(), true, false, false, phase, s.assimilation(), has, collapse)) return;
        if (!takeShard(p)) return;
        darken(p, s);
        p.sendSystemMessage(Component.literal("The rukh turns black. The shard in your pack has been eaten, and your Djinn with it.").withStyle(ChatFormatting.DARK_PURPLE));
    }

    /** Within six blocks of a dark Full Equip, calm mobs are afraid and allied players feel a short, harmless chill. Creative and spectator players are untouched. */
    private static void unease(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        double r = DarkRules.AURA_RADIUS, r2 = r * r;
        List<LivingEntity> near = l.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                e -> e != p && e.isAlive() && !e.isSpectator() && e.distanceToSqr(p) <= r2);
        int scared = 0, uneasy = 0;
        UUID me = p.getUUID();
        for (LivingEntity e : near) {
            if (e instanceof Player o) {
                if (o.isCreative() || !o.isAlliedTo(p) || uneasy >= DarkRules.MAX_UNEASY) continue;
                uneasy++;
                o.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DarkRules.UNEASE_TICKS, 0, true, false));
            } else if (e instanceof Mob m) {
                if (m instanceof Enemy || m.getType().getCategory() == MobCategory.MONSTER) continue;          // hostile things are the Djinn's foes, not its audience
                if (m instanceof OwnableEntity own && me.equals(own.getOwnerUUID())) continue;                  // your own pets are spared
                if (scared >= DarkRules.MAX_FRIGHTENED) continue;
                scared++;
                Fx.addEffect(m, "tensura:fear", DarkRules.FEAR_TICKS, 0);
            }
        }
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.DARK, p.getX(), p.getY() + 1.0, p.getZ(), 3, 0.6, 0.8, 0.6, 0.01);
    }

    // ---------------------------------------------------------------- shards in the pack
    static boolean hasShard(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (DjinnItems.isDarkShard(inv.getItem(i))) return true;
        return false;
    }

    private static boolean takeShard(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (DjinnItems.isDarkShard(st)) { st.shrink(1); return true; }
        }
        return false;
    }

    // ---------------------------------------------------------------- where the shards come from
    /** A boss fell: one roll for a shard for the killer (or a drop where it fell when nobody is credited). Returns true if one was found. */
    public static boolean rollBossShard(ServerLevel level, ServerPlayer killer, Vec3 at, String bossName) {
        if (!DarkRules.wins(level.getRandom().nextDouble(), DarkRules.BOSS_CHANCE)) return false;
        ItemStack shard = DjinnItems.darkShardStack();
        if (killer != null && killer.isAlive()) {
            killer.getInventory().placeItemBackInInventory(shard);
            killer.sendSystemMessage(Component.literal("A splinter of black rukh falls from " + bossName + " and settles in your hands.").withStyle(ChatFormatting.DARK_PURPLE));
        } else {
            ItemEntity item = new ItemEntity(level, at.x, at.y + 0.5, at.z, shard);
            item.setGlowingTag(true);
            level.addFreshEntity(item);
        }
        return true;
    }

    /** Lucius dies: the same roll (his fall posts no collapse). Skipped when another mod cancelled the death. */
    static void onLuciusDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof LuciusBoss boss) || !(boss.level() instanceof ServerLevel level)) return;
        ServerPlayer killer = e.getSource().getEntity() instanceof ServerPlayer sp ? sp : boss.getKillCredit() instanceof ServerPlayer credit ? credit : null;
        rollBossShard(level, killer, boss.position(), "Lucius");
    }

    /** A vault chest: 2% of a shard, or EMPTY. */
    public static ItemStack vaultShard(net.minecraft.util.RandomSource random) {
        return DarkRules.wins(random.nextDouble(), DarkRules.VAULT_CHANCE) ? DjinnItems.darkShardStack() : ItemStack.EMPTY;
    }
}
