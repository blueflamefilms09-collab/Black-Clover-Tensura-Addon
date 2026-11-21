package com.newuniverse.nusmp.dragonballs;

import com.newuniverse.nusmp.elite.hub.CoinWallet;
import com.newuniverse.nusmp.relics.RelicCatalog;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * What each wish does, on the server, one wish at a time. Every wish is validated first and returns {@link Outcome#ok} false WITHOUT being used up when it cannot be granted (nobody to revive, the power
 * ceiling reached, no coins in the game, a skill already known, the Saiyan switch refused), so a summoner never loses a wish to a refusal. The permanent power of a player is stored as a small level
 * counter in the player's persistent data and applied as two permanent attribute modifiers (max health, attack damage), re-applied at login, respawn and clone. No wish touches the world, other players'
 * items or minds; the revive wish only moves a player who died inside the revive window back to where they died and heals them. No hunger is touched anywhere.
 */
public final class DbWishes {
    private DbWishes() {}

    public record Outcome(boolean ok, Component msg) {}

    static final String KEY = "nusmp_dragonballs";
    private static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "dragonballs_power_health");
    private static final ResourceLocation ATTACK_ID = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "dragonballs_power_attack");
    private static final Random RNG = new Random();

    private static Outcome no(BallSet set, String key, Object... a) { return new Outcome(false, DbMsg.say(set, key, a)); }

    private static Outcome yes(BallSet set, String key, Object... a) { return new Outcome(true, DbMsg.say(set, key, a)); }

    /** Validates and applies one wish for the summoner. {@code text} is the normalised sentence (used for a named player or a named skill). */
    public static Outcome grant(ServerPlayer p, BallSet set, WishRules.Wish w, String text, DbSession s) {
        return switch (w.kind()) {
            case REVIVE -> revive(p, set, text);
            case POWER -> power(p, set);
            case COINS -> coins(p, set);
            case SKILL -> skill(p, set, text, s);
            case ITEM -> item(p, set, text);
            case WEAPON -> DbConfig.overpowered() ? DbWishesPlus.weapon(p, set, text) : weapon(p, set);
            case SAIYAN -> saiyan(p, set);
            case RACE -> DbConfig.overpowered() ? DbWishesPlus.race(p, set, text) : no(set, "unknown");
            case RANK -> DbConfig.overpowered() ? DbWishesPlus.rank(p, set, text) : no(set, "unknown");
        };
    }

    // ================================================================ revive
    private static Outcome revive(ServerPlayer p, BallSet set, String text) {
        MinecraftServer srv = p.getServer();
        List<ServerPlayer> targets = new ArrayList<>();
        if (set == BallSet.SUPER) {
            double r2 = 96.0 * 96.0;
            for (ServerPlayer q : srv.getPlayerList().getPlayers()) {
                DbManager.Death d = DbManager.death(q.getUUID());
                if (d == null || !q.isAlive() || targets.size() >= 16) continue;
                if (d.dim().equals(p.level().dimension().location().toString()) && (d.x() - p.getX()) * (d.x() - p.getX()) + (d.z() - p.getZ()) * (d.z() - p.getZ()) <= r2) targets.add(q);
            }
        } else {
            ServerPlayer named = null;
            for (ServerPlayer q : srv.getPlayerList().getPlayers()) if (DbRules.contains(text, q.getGameProfile().getName())) { named = q; break; }
            ServerPlayer t = named == null ? p : named;
            if (DbManager.death(t.getUUID()) != null && t.isAlive()) targets.add(t);
        }
        if (targets.isEmpty()) return no(set, DbConfig.overpowered() ? "revive_none_op" : "revive_none", DbConfig.reviveMinutes());
        int done = 0;
        for (ServerPlayer t : targets) {
            DbManager.Death d = DbManager.death(t.getUUID());
            ServerLevel l = d == null ? null : DbManager.levelOf(srv, d.dim());
            if (l == null) continue;
            double x = d.x(), y = d.y(), z = d.z();
            if (y < l.getMinBuildHeight() + 2 || y > l.getMaxBuildHeight()) {                    // died in the void: back to the world spawn, never into the void again
                BlockPos sp = l.getSharedSpawnPos();
                x = sp.getX() + 0.5;
                y = sp.getY();
                z = sp.getZ() + 0.5;
            }
            t.teleportTo(l, x, y, z, t.getYRot(), t.getXRot());
            t.setHealth(t.getMaxHealth());
            t.removeAllEffects();
            t.fallDistance = 0f;
            DbManager.forgetDeath(t.getUUID());
            if (t != p) t.sendSystemMessage(DbMsg.say(set, "revived_you"));
            done++;
        }
        if (done == 0) return no(set, DbConfig.overpowered() ? "revive_none_op" : "revive_none", DbConfig.reviveMinutes());
        return yes(set, "revived", done);
    }

    // ================================================================ permanent power
    static CompoundTag state(ServerPlayer p) { return p.getPersistentData().getCompound(KEY); }

    public static int powerLevels(ServerPlayer p) { return state(p).getInt("power"); }

    private static Outcome power(ServerPlayer p, BallSet set) {
        int cur = powerLevels(p);
        boolean op = DbConfig.overpowered();
        int add = WishRules.addable(cur, WishRules.powerLevels(set, op), DbConfig.powerCapNow());
        if (add <= 0) return no(set, "power_cap");
        CompoundTag st = state(p);
        st.putInt("power", cur + add);
        p.getPersistentData().put(KEY, st);
        applyPower(p);
        p.setHealth(p.getMaxHealth());
        return yes(set, "power", add, cur + add, DbConfig.powerCapNow());
    }

    /** Puts the two permanent modifiers of the player's power level on (or off at level 0). Idempotent: the old ones are removed first. */
    public static void applyPower(ServerPlayer p) {
        int lv = Math.max(0, Math.min(WishRules.POWER_CAP_MAX, powerLevels(p)));
        AttributeInstance h = p.getAttribute(Attributes.MAX_HEALTH), a = p.getAttribute(Attributes.ATTACK_DAMAGE);
        if (h != null) {
            h.removeModifier(HEALTH_ID);
            if (lv > 0) h.addPermanentModifier(new AttributeModifier(HEALTH_ID, lv * WishRules.healthPerLevel(), AttributeModifier.Operation.ADD_VALUE));
        }
        if (a != null) {
            a.removeModifier(ATTACK_ID);
            if (lv > 0) a.addPermanentModifier(new AttributeModifier(ATTACK_ID, lv * WishRules.attackPerLevel(), AttributeModifier.Operation.ADD_VALUE));
        }
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) { if (e.getEntity() instanceof ServerPlayer p && powerLevels(p) > 0) applyPower(p); }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p && powerLevels(p) > 0) applyPower(p); }

    /** The counter survives death even where persistent data is not copied by the loader. */
    public static void onClone(PlayerEvent.Clone e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !e.getOriginal().getPersistentData().contains(KEY)) return;
        p.getPersistentData().put(KEY, e.getOriginal().getPersistentData().getCompound(KEY).copy());
        applyPower(p);
    }

    // ================================================================ coins, items, weapon
    private static Outcome coins(ServerPlayer p, BallSet set) {
        if (!CoinWallet.available()) return no(set, "coins_none");
        long n = WishRules.coins(set, DbConfig.overpowered());
        new CoinWallet().give(p, n);
        return yes(set, "coins", n / 10_000L);
    }

    private static Outcome item(ServerPlayer p, BallSet set, String text) {
        return DbConfig.overpowered() ? DbWishesPlus.item(p, set, text) : itemClassic(p, set);
    }

    /** The old item wish: one gift of the curated table, at random. */
    static Outcome itemClassic(ServerPlayer p, BallSet set) {
        List<WishRules.Gift> table = WishRules.items(set);
        for (int tries = 0; tries < 6; tries++) {
            WishRules.Gift g = WishRules.pick(table, RNG);
            ResourceLocation rl = ResourceLocation.tryParse(g.id());
            Item it = rl == null ? null : BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
            if (it == null) continue;
            ItemStack st = new ItemStack(it, Math.max(1, Math.min(g.count(), it.getDefaultMaxStackSize())));
            Component name = st.getHoverName();
            give(p, st);
            return yes(set, "item", st.getCount(), name);
        }
        return no(set, "item_none");
    }

    private static Outcome weapon(ServerPlayer p, BallSet set) {
        CompoundTag st = state(p);
        if (st.getBoolean("weapon")) return no(set, "weapon_had");
        Supplier<? extends Item> sup = RelicCatalog.itemOf(WishRules.WEAPON_RELIC);
        Item it = null;
        try { it = sup == null ? null : sup.get(); } catch (RuntimeException ignored) { }
        if (it == null) return no(set, "weapon_none");
        st.putBoolean("weapon", true);
        p.getPersistentData().put(KEY, st);
        ItemStack stack = new ItemStack(it);
        give(p, stack);
        return yes(set, "weapon", stack.getHoverName());
    }

    static void give(ServerPlayer p, ItemStack st) {
        if (!p.getInventory().add(st) && !st.isEmpty()) p.drop(st, false);
    }

    // ================================================================ skill, race
    private static Outcome skill(ServerPlayer p, BallSet set, String text, DbSession s) {
        return DbConfig.overpowered() ? DbWishesPlus.skill(p, set, text, s) : skillClassic(p, set, text, s);
    }

    /** The old skill wish: Earth a random skill of its short list, Super names one of its list. */
    static Outcome skillClassic(ServerPlayer p, BallSet set, String text, DbSession s) {
        List<String> list = WishRules.skills(set);
        String named = WishRules.namedSkill(list, text);
        if (set == BallSet.SUPER && named == null) {                                             // Super Shenron lets the summoner choose: show the list, the wish is not used up
            DbSession.skillMenu(p, s);
            return new Outcome(false, DbMsg.say(set, "skill_pick"));
        }
        List<String> order = new ArrayList<>(list);
        if (named != null) { order.clear(); order.add(named); } else Collections.shuffle(order, RNG);
        Skills skills = SkillAPI.getSkillsFrom(p);
        boolean known = false;
        for (String id : order) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            ManasSkill sk = rl == null ? null : SkillAPI.getSkillRegistry().get(rl);
            if (sk == null) continue;
            if (skills.getSkill(sk.getRegistryName()).isPresent()) { known = true; continue; }
            if (skills.learnSkill(sk.createDefaultInstance(), Component.translatable(DbMsg.P + "skill_learned", WishRules.skillName(id)).withStyle(ChatFormatting.GOLD))) return yes(set, "skill", WishRules.skillName(id));
        }
        return no(set, known ? "skill_known" : "skill_none");
    }

    private static Outcome saiyan(ServerPlayer p, BallSet set) {
        if (!DbConfig.saiyanWish()) return no(set, "saiyan_off");
        return DbHooks.makeSaiyan.test(p) ? yes(set, "saiyan") : no(set, "saiyan_no");
    }
}
