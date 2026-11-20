package com.newuniverse.nusmp.lords;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Why: the rules of the four Mortal Sin weapons on the server: who may hold each (one stack in the world, one wielder; a copy, an old stack or a thief's stack dissolves or is torn away), the form switch (a swap of the
 * item id in the slot, opened by mastery), the arts and the Lord's decree, the Burden (the named drawback: it builds with use, strains from 50, overloads at 100 for six seconds, and always recovers), the mastery that
 * softens it, and the release of a weapon whose wielder falls to a player or stays away. Everything is decided here, on the server thread, keyed by the player's UUID. Cost: per online player one scan of the inventory
 * (41 slots, an item-class test each) every 20 ticks staggered by player id, and for a wielder one second-tick (four sins, a few float operations, at most one effect packet per three seconds); nothing is scheduled between,
 * the maps below hold a few ints per player and are dropped at logout and server stop.
 */
public final class LordService {
    private LordService() {}

    private static final Random RND = new Random();
    private static final Map<UUID, Long> LAST_SWITCH = new HashMap<>();
    private static final Map<UUID, int[]> LAST_HUD = new HashMap<>();
    /** Wielders who died a plain death with a weapon on them: it is made whole again at the respawn (a bit per sin). */
    private static final Map<UUID, Integer> RETURN = new HashMap<>();
    private static final ResourceLocation HOLLOW_MOD = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "lords_hollow_of_envy");
    private static final String DECREE_KEY = "nusmp_lord_decree_";

    // ---------------------------------------------------------------- gates
    public static boolean managed(LordRules.Sin s) { return s != null && s.isLord() && LordConfig.enabled() && LordData.bound(); }

    private static LordData data() { return LordData.get(); }

    /** The wielder, holding the valid stack of this weapon, with the setting on. */
    public static boolean mayUse(ServerPlayer p, ItemStack stack) {
        LordRules.Sin s = LordItems.sinOf(stack);
        if (!managed(s)) return false;
        return data().valid(s, LordComponents.token(stack)) && data().isOwner(s, p.getUUID()) && p.getUUID().equals(LordComponents.owner(stack));
    }

    private static void say(ServerPlayer p, String key, ChatFormatting color, Object... args) {
        p.displayClientMessage(Component.translatable(key, args).withStyle(color), true);
    }

    /** The weapon of this sin in the main hand or off hand, or EMPTY. */
    private static ItemStack held(ServerPlayer p, LordRules.Sin s) {
        if (LordItems.sinOf(p.getMainHandItem()) == s) return p.getMainHandItem();
        return LordItems.sinOf(p.getOffhandItem()) == s ? p.getOffhandItem() : ItemStack.EMPTY;
    }

    // ---------------------------------------------------------------- arts and hits
    /** The right click of form 1 or form 2: the art, gated by the owner check, the overload and the item cooldown, then charged to the burden. */
    public static void useArt(ServerPlayer p, ItemStack stack, LordRules.Sin sin, int form, Item item) {
        if (!LordConfig.enabled() || p.isSpectator()) return;
        if (!mayUse(p, stack)) {
            if (managed(sin)) say(p, "lords.msg.not_yours", ChatFormatting.DARK_RED);
            return;
        }
        if (p.getCooldowns().isOnCooldown(item)) return;
        if (!gate(p, sin)) {
            p.getCooldowns().addCooldown(item, 20);
            return;
        }
        LordState st = LordStore.of(p);
        LordArts.Result r = LordArts.art(p, sin, form, st.level(sin));
        if (!r.fired()) return;
        p.getCooldowns().addCooldown(item, LordRules.artCooldown(sin, form));
        charge(p, sin, LordRules.artBurden(sin, form, r.undead()));
        if (r.undead() > 0 && sin == LordRules.Sin.HOLY) say(p, "lords.msg.absolved", ChatFormatting.GOLD);
    }

    /** May this wielder use an art of this weapon now? False (with a message) while overloaded. */
    public static boolean gate(ServerPlayer p, LordRules.Sin sin) {
        if (!LordConfig.burden()) return true;
        long now = p.serverLevel().getGameTime();
        LordState st = LordStore.of(p);
        if (st.overloaded(sin, now)) {
            say(p, "lords.msg.overloaded", ChatFormatting.RED, Component.translatable(sin.drawbackNameKey()), (int) ((st.overloadUntil(sin) - now + 19) / 20));
            return false;
        }
        return true;
    }

    /** Charges an art or a hit to the burden of the weapon (the setting's speed, the mastery's softening); at 100 the overload starts. */
    public static void charge(ServerPlayer p, LordRules.Sin sin, float cost) {
        if (!LordConfig.burden() || !managed(sin)) return;
        LordState st = LordStore.of(p);
        long now = p.serverLevel().getGameTime();
        if (st.overloaded(sin, now)) return;
        float b = LordRules.gain(st.burden(sin), (float) (cost * LordConfig.burdenScale()), st.level(sin));
        st.setBurden(sin, b);
        if (LordRules.overloaded(b)) {
            st.setOverloadUntil(sin, now + LordRules.OVERLOAD_TICKS);
            say(p, "lords.msg.overload_start", ChatFormatting.DARK_RED, Component.translatable(sin.drawbackNameKey()), LordRules.OVERLOAD_TICKS / 20);
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.5f, 0.6f);
        }
        hud(p, sin, false);
    }

    /** A plain hit of the weapon: a sliver of burden (one float operation). */
    public static void onHit(ServerPlayer p, ItemStack stack, LivingEntity target, LordRules.Sin sin) {
        if (!managed(sin) || target == p || !mayUse(p, stack)) return;
        charge(p, sin, LordRules.HIT_BURDEN);
    }

    // ---------------------------------------------------------------- the Lord's decree (skill mode)
    public static void decree(ServerPlayer p, LordRules.Sin sin) {
        if (!managed(sin) || p.isSpectator()) return;
        ItemStack h = held(p, sin);
        if (h.isEmpty() || !mayUse(p, h)) {
            say(p, "lords.msg.hold_it", ChatFormatting.GRAY, Component.translatable(sin.formKey(0)));
            return;
        }
        LordState st = LordStore.of(p);
        if (!LordRules.ultimateOpen(st.level(sin))) {
            say(p, "lords.msg.decree_locked", ChatFormatting.GRAY, Component.translatable(sin.ultimateKey()), LordRules.ULTIMATE_LEVEL, Component.translatable(sin.masteryKey()));
            return;
        }
        long now = p.serverLevel().getGameTime();
        long ready = p.getPersistentData().getLong(DECREE_KEY + sin.id);
        if (!LordRules.ready(now, ready, LordRules.ULTIMATE_COOLDOWN_TICKS)) {
            say(p, "lords.msg.decree_wait", ChatFormatting.GRAY, Component.translatable(sin.ultimateKey()), (int) ((ready - now + 19) / 20));
            return;
        }
        if (!gate(p, sin)) return;
        LordArts.Result r = LordArts.decree(p, sin, st.level(sin));
        if (!r.fired()) return;
        p.getPersistentData().putLong(DECREE_KEY + sin.id, now + LordRules.ULTIMATE_COOLDOWN_TICKS);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 0.7f);
        charge(p, sin, LordRules.ULTIMATE_BURDEN);
    }

    // ---------------------------------------------------------------- ownership scan
    /** The once-a-second look at what a player carries: the valid stack of each weapon is kept (one only), a copy or an old stack dissolves, a bare stack is adopted if that weapon is not in the world, a thief's stack is torn away. */
    public static void scan(ServerPlayer p) {
        if (!LordConfig.enabled() || !LordData.bound()) return;
        LordData d = data();
        Inventory inv = p.getInventory();
        boolean[] kept = new boolean[LordRules.Sin.values().length];
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            LordRules.Sin sin = LordItems.sinOf(s);
            if (sin == null || !sin.isLord()) continue;
            int k = sin.ordinal();
            long token = LordComponents.token(s);
            if (token == 0L) {
                if (d.spawned(sin) || kept[k]) {
                    dissolve(p, s);
                    continue;
                }
                token = d.mint(sin, RND);
                LordComponents.write(s, null, token);
            } else if (!d.valid(sin, token)) {
                dissolve(p, s);
                continue;
            }
            UUID owner = d.owner(sin);
            if (owner == null) {
                if (kept[k] || p.isSpectator()) {
                    if (kept[k]) dissolve(p, s);
                    continue;
                }
                LordComponents.write(s, p.getUUID(), token);
                bind(p, sin, true);
                kept[k] = true;
            } else if (!owner.equals(p.getUUID())) {
                tear(p, s);
                continue;
            } else if (kept[k]) {
                dissolve(p, s);
                continue;
            } else {
                if (!p.getUUID().equals(LordComponents.owner(s))) LordComponents.write(s, p.getUUID(), token);
                kept[k] = true;
            }
            int level = LordStore.of(p).level(sin), f = LordItems.formOf(s);
            if (f > LordRules.topForm(level)) {                                           // a form the wielder's mastery has not opened: it settles into the highest open one
                inv.setItem(i, make(sin, LordRules.topForm(level), p.getUUID(), token));
                say(p, "lords.msg.form_locked", ChatFormatting.GRAY, Component.translatable(sin.formKey(f)), LordRules.formLevel(f), Component.translatable(sin.masteryKey()));
                f = LordRules.topForm(level);
            }
            if (f >= 0 && f != d.form(sin) && (s == p.getMainHandItem() || s == p.getOffhandItem())) d.setForm(sin, f);
        }
        for (LordRules.Sin sin : LordRules.Sin.values()) {
            if (sin.isLord() && kept[sin.ordinal()] && d.isOwner(sin, p.getUUID())) d.touch(sin, System.currentTimeMillis());
        }
    }

    private static ItemStack make(LordRules.Sin sin, int form, UUID owner, long token) {
        ItemStack n = new ItemStack(LordItems.of(sin, form));
        LordComponents.write(n, owner, token);
        return n;
    }

    private static void dissolve(ServerPlayer p, ItemStack s) {
        s.setCount(0);
        say(p, "lords.msg.dissolved", ChatFormatting.DARK_RED);
    }

    /** A non-wielder carries the weapon: it tears itself away and lies on the ground at their feet (only its wielder may pick it up). */
    private static void tear(ServerPlayer p, ItemStack s) {
        ItemStack copy = s.copy();
        s.setCount(0);
        ItemEntity e = new ItemEntity(p.serverLevel(), p.getX(), p.getY() + 0.5, p.getZ(), copy);
        e.setPickUpDelay(100);
        e.setInvulnerable(true);
        e.setUnlimitedLifetime();
        p.serverLevel().addFreshEntity(e);
        say(p, "lords.msg.torn", ChatFormatting.DARK_RED);
    }

    /** The weapon binds to a player: the skill, the HUD and the warning; a fresh bind is announced. */
    static void bind(ServerPlayer p, LordRules.Sin sin, boolean fresh) {
        data().bindOwner(sin, p.getUUID(), System.currentTimeMillis());
        LordGrantService.sync(p, sin);
        hud(p, sin, true);
        if (fresh) {
            LordText.warn(p, sin);
            if (p.getServer() != null) p.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("lords.msg.chosen", Component.translatable("lords.name." + sin.id)).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
        }
    }

    // ---------------------------------------------------------------- release and return
    /** The wielder loses the weapon (killed by a player, 14 days away, or {@code /multiverse lords release}): the skill and the burden go with it, the mastery stays with the player, a fresh unowned stack lies where they fell. */
    public static void release(MinecraftServer server, LordRules.Sin sin, UUID ownerId, ServerLevel dropLevel, Vec3 dropAt) {
        LordData d = LordData.get(server);
        if (!d.isOwner(sin, ownerId)) return;
        d.clearOwner(sin);
        long token = d.mint(sin, RND);
        ServerPlayer old = server.getPlayerList().getPlayer(ownerId);
        if (old != null) {
            LordStore.of(old).setBurden(sin, 0.0f);
            LordStore.of(old).setOverloadUntil(sin, 0L);
            applyBurden(old, sin);
            LordGrantService.sync(old, sin);
            hud(old, sin, true);
        }
        RETURN.computeIfPresent(ownerId, (k, v) -> v & ~(1 << sin.ordinal()));
        if (dropLevel != null && dropAt != null) {
            ItemStack fresh = make(sin, 0, null, token);
            ItemEntity e = new ItemEntity(dropLevel, dropAt.x, dropAt.y + 0.5, dropAt.z, fresh);
            e.setInvulnerable(true);
            e.setUnlimitedLifetime();
            dropLevel.addFreshEntity(e);
        }
    }

    /** A wielder who died a plain death: the weapon is not among the drops; it is made whole again at the respawn. */
    public static void markReturn(UUID id, LordRules.Sin sin) { RETURN.merge(id, 1 << sin.ordinal(), (a, b) -> a | b); }

    public static void onRespawn(ServerPlayer p) {
        if (!LordData.bound()) return;
        Integer mask = RETURN.remove(p.getUUID());
        for (LordRules.Sin s : LordRules.Sin.values()) {
            if (!s.isLord()) continue;
            if (mask != null && (mask & (1 << s.ordinal())) != 0 && data().isOwner(s, p.getUUID())) giveStack(p, s);
            LordStore.of(p).setBurden(s, 0.0f);                                           // a new body starts rested
            LordStore.of(p).setOverloadUntil(s, 0L);
            applyBurden(p, s);
            LordGrantService.sync(p, s);
            hud(p, s, true);
        }
    }

    /** Puts the one valid stack in the owner's inventory (or at their feet when it is full), in the form it was last in (the highest their mastery opens). */
    public static boolean giveStack(ServerPlayer p, LordRules.Sin sin) {
        LordData d = data();
        if (!d.isOwner(sin, p.getUUID())) return false;
        int f = Math.min(d.form(sin), LordRules.topForm(LordStore.of(p).level(sin)));
        ItemStack s = make(sin, f, p.getUUID(), d.token(sin));
        if (!p.getInventory().add(s)) p.drop(s, false);
        return true;
    }

    /** {@code /multiverse lords recover}: a wielder whose weapon was lost gets a new valid stack; every older stack dissolves (a new token). */
    public static boolean recover(ServerPlayer p, LordRules.Sin sin) {
        if (!managed(sin) || !data().isOwner(sin, p.getUUID())) return false;
        data().mint(sin, RND);
        return giveStack(p, sin);
    }

    /** The skill's Manifest mode for a managed weapon: a wielder who lost theirs calls it back (a new token, every older stack dissolves); one who carries it is told so. */
    public static void manifest(ServerPlayer p, LordRules.Sin sin) {
        if (!managed(sin) || !data().isOwner(sin, p.getUUID())) {
            say(p, "lords.msg.not_yours", ChatFormatting.DARK_RED);
            return;
        }
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (LordItems.sinOf(inv.getItem(i)) == sin) {
                say(p, "lords.msg.carried", ChatFormatting.GRAY, Component.translatable("lords.name." + sin.id));
                return;
            }
        }
        recover(p, sin);
    }

    /** 14 days without the wielder: the weapon leaves the world and may enter again. */
    public static void inactivityCheck(MinecraftServer server) {
        if (!LordConfig.enabled() || !LordConfig.INACTIVITY_RELEASE.get() || !LordData.bound()) return;
        LordData d = LordData.get(server);
        long now = System.currentTimeMillis();
        for (LordRules.Sin s : LordRules.Sin.values()) {
            if (!s.isLord() || !d.spawned(s) || d.owner(s) == null) continue;
            if (server.getPlayerList().getPlayer(d.owner(s)) != null) d.touch(s, now);
            else if (now - d.lastActiveMs(s) > LordRules.INACTIVITY_MS) d.vanish(s);
        }
    }

    // ---------------------------------------------------------------- dropped item
    public static void itemEntityTick(ItemEntity e, ItemStack stack, LordRules.Sin sin) {
        if (!e.isInvulnerable()) e.setInvulnerable(true);
        e.setUnlimitedLifetime();
        ServerLevel level = (ServerLevel) e.level();
        if (e.getY() < level.getMinBuildHeight() + 1.0) {
            var sp = level.getSharedSpawnPos();
            e.setPos(sp.getX() + 0.5, sp.getY() + 1.0, sp.getZ() + 0.5);
            e.setDeltaMovement(Vec3.ZERO);
        }
        if (e.tickCount % 40 == 0 && managed(sin)) {
            long t = LordComponents.token(stack);
            if (t != 0L && !data().valid(sin, t)) e.discard();
        }
    }

    // ---------------------------------------------------------------- the form switch
    /** Switches the held (or first carried) weapon to {@code formId}: only the wielder, only a form their mastery has opened, at most once every two seconds. */
    public static boolean switchForm(ServerPlayer p, LordRules.Sin sin, int formId) {
        if (!managed(sin) || formId < 0 || formId >= sin.forms() || !data().isOwner(sin, p.getUUID())) return false;
        long now = p.serverLevel().getGameTime();
        Long last = LAST_SWITCH.get(p.getUUID());
        if (last != null && now >= last && now - last < LordRules.FORM_SWITCH_GAP_TICKS) return false;
        int level = LordStore.of(p).level(sin);
        if (!LordRules.formOpen(formId, level)) {
            say(p, "lords.msg.form_locked", ChatFormatting.GRAY, Component.translatable(sin.formKey(formId)), LordRules.formLevel(formId), Component.translatable(sin.masteryKey()));
            return false;
        }
        Inventory inv = p.getInventory();
        int slot = -1;
        if (LordItems.sinOf(p.getMainHandItem()) == sin) slot = inv.selected;
        else for (int i = 0; i < inv.getContainerSize() && slot < 0; i++) if (LordItems.sinOf(inv.getItem(i)) == sin) slot = i;
        if (slot < 0) return false;
        ItemStack old = inv.getItem(slot);
        if (!mayUse(p, old) || LordItems.formOf(old) == formId) return false;
        inv.setItem(slot, make(sin, formId, p.getUUID(), LordComponents.token(old)));
        LAST_SWITCH.put(p.getUUID(), now);
        data().setForm(sin, formId);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 0.6f);
        say(p, "lords.msg.form", ChatFormatting.LIGHT_PURPLE, Component.translatable(sin.formKey(formId)));
        LordFx.follow(p.serverLevel(), sin, LordVfxCodes.LEVEL, p, p.position(), 2.0f);
        hud(p, sin, true);
        return true;
    }

    /** The next (or previous) open form of the weapon the player holds. */
    public static void cycleForm(ServerPlayer p, boolean back) {
        LordRules.Sin sin = LordItems.sinOf(p.getMainHandItem());
        if (!managed(sin) || !data().isOwner(sin, p.getUUID())) return;
        int cur = Math.max(0, LordItems.formOf(p.getMainHandItem())), level = LordStore.of(p).level(sin);
        for (int i = 1; i <= sin.forms(); i++) {
            int f = Math.floorMod(cur + (back ? -i : i), sin.forms());
            if (LordRules.formOpen(f, level)) {
                switchForm(p, sin, f);
                return;
            }
        }
    }

    // ---------------------------------------------------------------- the second tick of a wielder
    /** Once a second for each wielder: the burden recovers, the strained state grows the mastery and applies its named penalty, the HUD follows. */
    public static void tick(ServerPlayer p) {
        if (!LordConfig.enabled() || !LordData.bound() || p.isSpectator()) return;
        LordData d = data();
        LordState st = LordStore.peek(p);
        if (st == null) return;
        long now = p.serverLevel().getGameTime();
        for (LordRules.Sin sin : LordRules.Sin.values()) {
            if (!sin.isLord() || !d.isOwner(sin, p.getUUID())) continue;
            boolean held = !held(p, sin).isEmpty();
            int level = st.level(sin);
            float b = st.burden(sin);
            if (st.overloadUntil(sin) != 0L && now >= st.overloadUntil(sin)) {                  // the overload is over: the weapon lets go
                st.setOverloadUntil(sin, 0L);
                b = Math.min(b, LordRules.OVERLOAD_RESET);
                say(p, "lords.msg.overload_end", ChatFormatting.GREEN, Component.translatable(sin.drawbackNameKey()));
            } else if (!st.overloaded(sin, now)) {
                b = LordRules.decay(b, held, level);
            }
            st.setBurden(sin, b);
            if (held && LordRules.strained(b) && LordConfig.burden()) grow(p, st, sin, xpTick(p));
            applyBurden(p, sin);
            if (LordRules.strained(b) && held && ((p.tickCount + p.getId()) / 20) % 3 == 0) LordFx.follow(p.serverLevel(), sin, LordVfxCodes.STATE, p, p.position(), 1.4f);
            hud(p, sin, false);
        }
    }

    private static int xpTick(ServerPlayer p) {
        double s = LordConfig.masteryScale() * LordRules.XP_PER_SECOND;
        return s >= 1.0 ? (int) Math.round(s) : p.getRandom().nextDouble() < s ? 1 : 0;
    }

    /** Adds mastery experience; a level gained is announced, drawn, and tells what it opened. */
    public static void grow(ServerPlayer p, LordState st, LordRules.Sin sin, int xp) {
        if (xp <= 0) return;
        int before = st.level(sin);
        if (!st.addXp(sin, xp)) return;
        int now = st.level(sin);
        p.sendSystemMessage(Component.translatable("lords.msg.level", Component.translatable(sin.masteryKey()), now).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (sin.isLord()) {
            for (int f = 1; f < sin.forms(); f++) if (before < LordRules.formLevel(f) && now >= LordRules.formLevel(f)) p.sendSystemMessage(Component.translatable("lords.msg.form_open", Component.translatable(sin.formKey(f))).withStyle(ChatFormatting.LIGHT_PURPLE));
            if (before < LordRules.ULTIMATE_LEVEL && now >= LordRules.ULTIMATE_LEVEL) p.sendSystemMessage(Component.translatable("lords.msg.decree_open", Component.translatable(sin.ultimateKey())).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 0.7f);
        LordFx.follow(p.serverLevel(), sin, LordVfxCodes.LEVEL, p, p.position(), 3.0f);
    }

    /** The named penalty of the burden: strained and overloaded, per weapon. Re-applied every second; removed the moment it no longer holds. Never touches the food bar. */
    static void applyBurden(ServerPlayer p, LordRules.Sin sin) {
        LordState st = LordStore.of(p);
        boolean on = LordConfig.enabled() && LordConfig.burden() && LordData.bound() && data().isOwner(sin, p.getUUID());
        float b = on ? st.burden(sin) : 0.0f;
        boolean strained = LordRules.strained(b), over = on && st.overloaded(sin, p.serverLevel().getGameTime());
        switch (sin) {
            case SLOTH -> {                                                                    // Weight of Sloth: slower
                if (strained || over) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, over ? 1 : 0, true, false));
                if (over) p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 50, 0, true, false));
            }
            case ENVY -> {                                                                     // Hollow of Envy: less maximum health
                AttributeInstance hp = p.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) {
                    hp.removeModifier(HOLLOW_MOD);
                    if (strained || over) hp.addTransientModifier(new AttributeModifier(HOLLOW_MOD, over ? -8.0 : -4.0, AttributeModifier.Operation.ADD_VALUE));
                    if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
                }
                if (over) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0, true, false));
            }
            case VANITY -> {                                                                   // Cracked Mirror: you shine, and weaken
                if (strained || over) p.addEffect(new MobEffectInstance(MobEffects.GLOWING, 50, 0, true, false));
                if (over) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0, true, false));
            }
            case HOLY -> {                                                                     // Penance: healing halved (healFactor), weakness at the overload
                if (over) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0, true, false));
            }
            default -> { }
        }
    }

    /** The share of a heal the wielder still receives (1.0 for everybody but a strained Seifort wielder and a ravenous Greed wielder). One map-free read. */
    public static float healFactor(ServerPlayer p) {
        float f = 1.0f;
        if (LordConfig.enabled() && LordConfig.burden() && LordData.bound()) {
            LordState st = LordStore.peek(p);
            if (st != null && LordRules.strained(st.burden(LordRules.Sin.HOLY)) && data().isOwner(LordRules.Sin.HOLY, p.getUUID())) f *= 0.5f;
        }
        return f * com.newuniverse.nusmp.greed.GreedService.healFactor(p);
    }

    // ---------------------------------------------------------------- kills
    /** A kill by a player: the weapon in their main hand is fed (the burden eases) and its mastery grows with the victim's size; Seifort eases twice as much on the undead. */
    public static void onKill(LivingEntity victim, ServerPlayer killer) {
        if (!LordConfig.enabled() || !LordData.bound() || victim == killer) return;
        LordRules.Sin sin = LordItems.sinOf(killer.getMainHandItem());
        if (!managed(sin) || !data().isOwner(sin, killer.getUUID())) return;
        LordState st = LordStore.of(killer);
        float b = LordRules.relief(st.burden(sin));
        if (sin == LordRules.Sin.HOLY && victim.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD)) b = LordRules.relief(b);
        if (!st.overloaded(sin, killer.serverLevel().getGameTime())) st.setBurden(sin, b);
        double s = LordConfig.masteryScale();
        grow(killer, st, sin, (int) Math.round(LordRules.xpForKill(victim.getMaxHealth()) * s));
        applyBurden(killer, sin);
        hud(killer, sin, false);
    }

    // ---------------------------------------------------------------- the HUD
    /** Sends the state of one sin to its wielder when it changed (or when {@code force}); one small packet. A player who is no longer the wielder gets a "not the wielder" packet once so the client forgets it. */
    public static void hud(ServerPlayer p, LordRules.Sin sin, boolean force) {
        LordData d = LordData.bound() ? data() : null;
        boolean owner = d != null && sin.isLord() && LordConfig.enabled() && d.isOwner(sin, p.getUUID()) && !com.newuniverse.nusmp.sinweapons.SinSkillService.isOffById(p, sin.id);      // 4.64.1: a skill removed with /multiverse sin remove takes its side bar with it
        LordState st = LordStore.peek(p);
        if (!owner || st == null) {
            sendHud(p, sin, 0, 0, 0, 0, 0, force);
            return;
        }
        float b = st.burden(sin);
        long now = p.serverLevel().getGameTime();
        int flags = 1 | (LordRules.strained(b) ? 2 : 0) | (st.overloaded(sin, now) ? 4 : 0);
        sendHud(p, sin, st.level(sin), LordRules.progress(st.xp(sin)), Math.round(b), flags, Math.max(0, d.form(sin)), force);
    }

    /** Greed's own state through the same packet: the mastery of the Half-Starved state, the satiation as the meter, ravenous / dormant / half as flags. */
    public static void greedHud(ServerPlayer p, boolean owner, float satiation, boolean half, boolean force) {
        LordState st = LordStore.peek(p);
        if (!owner || st == null) {
            sendHud(p, LordRules.Sin.GREED, 0, 0, 0, 0, 0, force);
            return;
        }
        int flags = 1 | (LordRules.ravenous(satiation) ? 2 : 0) | (LordRules.dormant(satiation) ? 4 : 0) | (half ? 8 : 0);
        sendHud(p, LordRules.Sin.GREED, st.level(LordRules.Sin.GREED), LordRules.progress(st.xp(LordRules.Sin.GREED)), Math.round(satiation), flags, 0, force);
    }

    private static void sendHud(ServerPlayer p, LordRules.Sin sin, int level, int pct, int meter, int flags, int form, boolean force) {
        int key = (level & 0xF) | ((pct & 0x7F) << 4) | ((meter & 0x7F) << 11) | ((flags & 0xF) << 18) | ((form & 0x3) << 22);
        int[] last = LAST_HUD.computeIfAbsent(p.getUUID(), k -> new int[LordRules.Sin.values().length]);
        int idx = sin.ordinal();
        if (!force && last[idx] == key) return;
        if (flags == 0 && last[idx] == 0 && !force) return;                                // nothing was ever shown, nothing to clear
        last[idx] = key == 0 ? -1 : key;
        LordNet.send(p, new LordNet.Hud(sin.ordinal(), level, pct, meter, flags, form));
    }

    // ---------------------------------------------------------------- cleanup
    public static void forget(UUID id) {
        LAST_SWITCH.remove(id);
        LAST_HUD.remove(id);
        RETURN.remove(id);
    }

    public static void clearAll() {
        LAST_SWITCH.clear();
        LAST_HUD.clear();
        RETURN.clear();
    }

    public static int trackedPlayers() { return LAST_SWITCH.size() + LAST_HUD.size() + RETURN.size(); }

    /** Logs a player in: a rested body, the skills right, the HUD shown. */
    public static void onLogin(ServerPlayer p) {
        if (!LordData.bound()) return;
        for (LordRules.Sin s : LordRules.Sin.values()) {
            if (!s.isLord()) continue;
            LordStore.of(p).setOverloadUntil(s, 0L);
            applyBurden(p, s);
            LordGrantService.sync(p, s);
            hud(p, s, true);
        }
    }
}
