package com.newuniverse.nusmp.greed;

import com.newuniverse.nusmp.avatar.TensuraBridge;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.storage.TensuraStorages;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the rules of the Black Sword Greed on the server: who may hold it (one stack in the world, one wielder; a copy, an old stack or a thief's stack dissolves or is torn away), the form switch (a swap of the item id
 * in the slot), the pressed modes of the Ultimate skill (Secret Technique, Half-Starvation, Offer), devouring (a kill absorbs a share of the victim's EP into stats, satiation and, sometimes, a skill in the vault), the hunger
 * clock and the release of the sword when its wielder falls to a player, starves or stays away. Everything is decided here, on the server thread, keyed by the player's UUID. Cost: per online player one scan of the inventory
 * (41 slots, an item-class test each) every 20 ticks staggered by player id, and for the wielder the hunger clock once a second (at most one box query of 8 blocks while starving); nothing is scheduled between.
 */
public final class GreedService {
    private GreedService() {}

    private static final Random RND = new Random();
    private static final ResourceLocation HP_MOD = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "greed_bonus_hp"), ATK_MOD = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "greed_bonus_atk"),
            ARMOR_MOD = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "greed_bonus_armor");

    private static final Map<UUID, Long> LAST_SWITCH = new HashMap<>(), LAST_TECHNIQUE = new HashMap<>();
    private static final Map<UUID, Integer> LAST_HUD = new HashMap<>();
    /** Wielders who died a plain death with the sword on them: it is made whole again at the respawn (value: the world's token at the time). */
    private static final Set<UUID> RETURN = new HashSet<>();

    // ---------------------------------------------------------------- gates
    private static GreedData data() { return GreedData.bound() ? GreedData.get() : null; }

    /** The wielder, holding a valid stack, with the mod enabled. */
    public static boolean mayUse(ServerPlayer p, ItemStack stack) {
        if (!GreedConfig.enabled() || !GreedData.bound()) return false;
        GreedData d = data();
        return d.valid(GreedComponents.token(stack)) && d.isOwner(p.getUUID()) && p.getUUID().equals(GreedComponents.owner(stack));
    }

    private static ItemStack held(ServerPlayer p) {
        ItemStack m = p.getMainHandItem();
        if (m.getItem() instanceof GreedItem) return m;
        ItemStack o = p.getOffhandItem();
        return o.getItem() instanceof GreedItem ? o : ItemStack.EMPTY;
    }

    private static void say(ServerPlayer p, String key, String fallback, ChatFormatting color, Object... args) {
        p.displayClientMessage(Component.translatableWithFallback(key, fallback, args).withStyle(color), true);
    }

    // ---------------------------------------------------------------- the form's click
    public static void use(ServerPlayer p, ItemStack stack, GreedItem item) {
        if (!mayUse(p, stack)) {
            say(p, "greed.msg.not_yours", "The sword does not answer you.", ChatFormatting.DARK_RED);
            return;
        }
        if (p.getCooldowns().isOnCooldown(item)) return;
        int cd = GreedForms.use(p, item.form(), GreedStore.of(p));
        if (cd > 0) p.getCooldowns().addCooldown(item, cd);
    }

    public static void release(ServerPlayer p, ItemStack stack, int charged) {
        if (!mayUse(p, stack) || p.getCooldowns().isOnCooldown(stack.getItem())) return;
        GreedForms.bolt(p, charged, GreedStore.of(p).bonusAtk);
        p.getCooldowns().addCooldown(stack.getItem(), 10);
    }

    public static void onHit(ServerPlayer p, ItemStack stack, LivingEntity target, GreedRules.Form form) {
        if (form == GreedRules.Form.SCYTHE && mayUse(p, stack)) GreedForms.soulSever(target, p.serverLevel().getGameTime(), 160);
        if (GreedHands.isTwin(form.itemId()) && mayUse(p, stack)) GreedForms.offBlow(p, target);                      // gauntlets and dual blades: the off hand strikes too
    }

    // ---------------------------------------------------------------- ownership scan
    /** The once-a-second look at what a player carries: the valid stack is kept (one only), a copy or an old stack dissolves, a bare stack is adopted if no sword exists, a thief's stack is torn away. */
    public static void scan(ServerPlayer p) {
        if (!GreedConfig.enabled() || !GreedData.bound()) return;
        GreedData d = data();
        Inventory inv = p.getInventory();
        boolean kept = false;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!(s.getItem() instanceof GreedItem)) continue;
            long token = GreedComponents.token(s);
            if (token == 0L) {
                if (d.spawned() || kept) {
                    dissolve(p, s, "greed.msg.dissolved", "The black sword in your hand was only a shadow: it dissolves.");
                    continue;
                }
                token = d.mint(RND);
                GreedComponents.write(s, null, token);
            } else if (!d.valid(token)) {
                dissolve(p, s, "greed.msg.dissolved", "The black sword in your hand was only a shadow: it dissolves.");
                continue;
            }
            UUID owner = d.owner();
            if (owner == null) {
                if (kept || p.isSpectator()) { if (kept) dissolve(p, s, "greed.msg.dissolved", "The black sword in your hand was only a shadow: it dissolves."); continue; }
                GreedComponents.write(s, p.getUUID(), token);
                bind(p, d, true);
                kept = true;
            } else if (!owner.equals(p.getUUID())) {
                tear(p, s, d);
            } else if (kept) {
                dissolve(p, s, "greed.msg.dissolved", "The black sword in your hand was only a shadow: it dissolves.");
            } else {
                if (!p.getUUID().equals(GreedComponents.owner(s))) GreedComponents.write(s, p.getUUID(), token);
                kept = true;
            }
        }
        if (kept && d.isOwner(p.getUUID())) {
            d.touch(System.currentTimeMillis());
            ItemStack h = held(p);
            if (h.getItem() instanceof GreedItem g && g.form().id() != d.form()) d.setForm(g.form().id());
        }
    }

    private static void dissolve(ServerPlayer p, ItemStack s, String key, String fallback) {
        s.setCount(0);
        say(p, key, fallback, ChatFormatting.DARK_RED);
    }

    /** A non-wielder carries the sword: it tears itself away and lies on the ground at their feet (only its wielder may pick it up). */
    private static void tear(ServerPlayer p, ItemStack s, GreedData d) {
        ItemStack copy = s.copy();
        s.setCount(0);
        ItemEntity e = new ItemEntity(p.serverLevel(), p.getX(), p.getY() + 0.5, p.getZ(), copy);
        e.setPickUpDelay(100);
        e.setInvulnerable(true);
        e.setUnlimitedLifetime();
        p.serverLevel().addFreshEntity(e);
        say(p, "greed.msg.torn", "The black sword tears itself from your hands: it has a host.", ChatFormatting.DARK_RED);
    }

    /** The sword binds to a player: the skill, the stats and the HUD; a fresh bind is announced. */
    static void bind(ServerPlayer p, GreedData d, boolean fresh) {
        d.bindOwner(p.getUUID(), System.currentTimeMillis());
        GreedGrantService.sync(p);
        applyAttributes(p, GreedStore.of(p));
        hud(p, true);
        if (fresh) com.newuniverse.nusmp.lords.LordText.warn(p, com.newuniverse.nusmp.lords.LordRules.Sin.GREED);   // what the blade takes, how much, how it recovers, how to avoid it
        if (fresh && p.getServer() != null) {
            p.getServer().getPlayerList().broadcastSystemMessage(Component.translatableWithFallback("greed.msg.chosen", "A Mortal Sin has chosen its host...").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
        }
    }

    // ---------------------------------------------------------------- release and return
    /** The wielder loses the sword (killed by a player, starved, 14 days away): the skill and the devoured stats go with it, the tier stays with the sword, and a fresh unowned stack lies where they fell. */
    public static void release(MinecraftServer server, UUID ownerId, ServerLevel dropLevel, Vec3 dropAt) {
        GreedData d = GreedData.get(server);
        if (!d.isOwner(ownerId)) return;
        d.clearOwner();
        long token = d.mint(RND);
        ServerPlayer old = server.getPlayerList().getPlayer(ownerId);
        if (old != null) {
            GreedStore.of(old).reset();
            applyAttributes(old, GreedStore.of(old));
            GreedGrantService.sync(old);
            hud(old, true);
            GreedForms.forget(ownerId);
        }
        RETURN.remove(ownerId);
        if (dropLevel != null && dropAt != null) {
            ItemStack fresh = new ItemStack(GreedItems.of(GreedRules.Form.byId(d.form())));
            GreedComponents.write(fresh, null, token);
            ItemEntity e = new ItemEntity(dropLevel, dropAt.x, dropAt.y + 0.5, dropAt.z, fresh);
            e.setInvulnerable(true);
            e.setUnlimitedLifetime();
            dropLevel.addFreshEntity(e);
        }
    }

    /** A wielder who died a plain death: the sword is not among the drops; it is made whole again at the respawn. */
    public static void markReturn(UUID id) { RETURN.add(id); }

    public static void onRespawn(ServerPlayer p) {
        GreedData d = data();
        if (d == null) return;
        if (RETURN.remove(p.getUUID()) && d.isOwner(p.getUUID())) giveStack(p, d);
        GreedGrantService.sync(p);
        applyAttributes(p, GreedStore.of(p));
        hud(p, true);
    }

    /** Puts the one valid stack in the owner's inventory (or at their feet when it is full). Returns false when the player is not the wielder. */
    public static boolean giveStack(ServerPlayer p, GreedData d) {
        if (!d.isOwner(p.getUUID())) return false;
        ItemStack s = new ItemStack(GreedItems.of(GreedRules.Form.byId(d.form())));
        GreedComponents.write(s, p.getUUID(), d.token());
        if (!p.getInventory().add(s)) p.drop(s, false);
        return true;
    }

    /** {@code /greed recover}: a wielder whose sword was lost gets a new valid stack; every older stack dissolves (a new token). */
    public static boolean recover(ServerPlayer p) {
        GreedData d = data();
        if (d == null || !d.isOwner(p.getUUID())) return false;
        d.mint(RND);
        return giveStack(p, d);
    }

    /** 14 days without the wielder: the sword leaves the world and may enter again. */
    public static void inactivityCheck(MinecraftServer server) {
        if (!GreedConfig.enabled() || !GreedConfig.INACTIVITY_RELEASE.get() || !GreedData.bound()) return;
        GreedData d = GreedData.get(server);
        if (!d.spawned() || d.owner() == null) return;
        long now = System.currentTimeMillis();
        if (server.getPlayerList().getPlayer(d.owner()) != null) {
            d.touch(now);
            return;
        }
        if (now - d.lastActiveMs() > GreedRules.INACTIVITY_MS) d.vanish();
    }

    // ---------------------------------------------------------------- dropped item
    public static void itemEntityTick(ItemEntity e, ItemStack stack) {
        if (!e.isInvulnerable()) e.setInvulnerable(true);
        e.setUnlimitedLifetime();
        ServerLevel level = (ServerLevel) e.level();
        if (e.getY() < level.getMinBuildHeight() + 1.0) {
            var sp = level.getSharedSpawnPos();
            e.setPos(sp.getX() + 0.5, sp.getY() + 1.0, sp.getZ() + 0.5);
            e.setDeltaMovement(Vec3.ZERO);
        }
        if (e.tickCount % 40 == 0 && GreedData.bound()) {
            long t = GreedComponents.token(stack);
            if (t != 0L && !data().valid(t)) e.discard();
        }
    }

    // ---------------------------------------------------------------- the form switch
    /** Switches the held (or first carried) stack to {@code formId}: only the wielder, only an open form, at most once every two seconds. */
    public static boolean switchForm(ServerPlayer p, int formId) {
        if (!GreedConfig.enabled() || !GreedData.bound() || formId < 0 || formId >= GreedRules.FORMS) return false;
        GreedData d = data();
        if (!d.isOwner(p.getUUID())) return false;
        long now = p.serverLevel().getGameTime();
        Long last = LAST_SWITCH.get(p.getUUID());
        if (last != null && now - last < GreedRules.SWITCH_GAP_TICKS) return false;
        if (!GreedRules.formOpen(formId, d.tier())) {
            say(p, "greed.msg.form_locked", "That form is not open yet (tier %s).", ChatFormatting.GRAY, formId);
            return false;
        }
        Inventory inv = p.getInventory();
        int slot = -1;
        if (p.getMainHandItem().getItem() instanceof GreedItem) slot = inv.selected;
        else for (int i = 0; i < inv.getContainerSize() && slot < 0; i++) if (inv.getItem(i).getItem() instanceof GreedItem) slot = i;
        if (slot < 0) return false;
        ItemStack old = inv.getItem(slot);
        if (!mayUse(p, old)) return false;
        GreedRules.Form to = GreedRules.Form.byId(formId);
        if (old.getItem() instanceof GreedItem g && g.form() == to) return false;
        ItemStack n = new ItemStack(GreedItems.of(to));
        GreedComponents.write(n, p.getUUID(), GreedComponents.token(old));
        inv.setItem(slot, n);
        LAST_SWITCH.put(p.getUUID(), now);
        d.setForm(formId);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_SCREAM, SoundSource.PLAYERS, 0.6f, 0.5f);
        say(p, "greed.msg.form", "Form: %s", ChatFormatting.RED, Component.translatableWithFallback(to.langKey(), to.english()));
        GreedFx.follow(p.serverLevel(), com.newuniverse.nusmp.vfx.VfxShape.GREED_AWAKEN, p, p.position(), GreedFx.CRIMSON, 24, formId + 1);      // VFX wave 2: black lightning, the red eyes (at most once every two seconds, the gate above)
        hud(p, true);
        return true;
    }

    /** The next (or previous) open form from the current one. */
    public static void cycleForm(ServerPlayer p, boolean back) {
        if (!GreedData.bound()) return;
        GreedData d = data();
        int tier = d.tier(), cur = d.form();
        for (int i = 1; i <= GreedRules.FORMS; i++) {
            int f = Math.floorMod(cur + (back ? -i : i), GreedRules.FORMS);
            if (GreedRules.formOpen(f, tier)) {
                switchForm(p, f);
                return;
            }
        }
    }

    // ---------------------------------------------------------------- the pressed modes of the Ultimate skill
    /** Mode 0: the Secret Technique of the form in hand. */
    public static boolean secretTechnique(ServerPlayer p) {
        if (!GreedConfig.enabled() || !GreedData.bound()) return false;
        GreedData d = data();
        if (!d.isOwner(p.getUUID())) return false;
        ItemStack h = held(p);
        if (h.isEmpty() || !mayUse(p, h)) {
            say(p, "greed.msg.hold_it", "Hold the sword to use its Secret Technique.", ChatFormatting.GRAY);
            return false;
        }
        long now = p.serverLevel().getGameTime();
        Long last = LAST_TECHNIQUE.get(p.getUUID());
        if (last != null && now - last < GreedRules.TECHNIQUE_GAP_TICKS) return false;
        GreedState st = GreedStore.of(p);
        if (st.bonusAtk < 2.0 && st.bonusHp < 4.0) {
            say(p, "greed.msg.not_enough", "Not enough devoured stats for a Secret Technique.", ChatFormatting.RED);
            return false;
        }
        GreedRules.Form form = ((GreedItem) h.getItem()).form();
        double power = st.sacrifice(form.share());
        st.setSatiation(st.satiation + (float) (form.share() * 150.0));
        LAST_TECHNIQUE.put(p.getUUID(), now);
        applyAttributes(p, st);
        GreedForms.secret(p, form, power);
        checkTier(p, st, d);
        hud(p, true);
        return true;
    }

    /** Mode 1: Half-Starvation (double devouring, 1.5 times the stats, hunger 2.5 times as fast). */
    public static void toggleHalf(ServerPlayer p) {
        if (!GreedData.bound() || !data().isOwner(p.getUUID())) return;
        GreedState st = GreedStore.of(p);
        st.half = !st.half;
        applyAttributes(p, st);
        say(p, st.half ? "greed.msg.half_on" : "greed.msg.half_off", st.half ? "Half-Starvation: ACTIVE (double absorption)" : "Half-Starvation: off", st.half ? ChatFormatting.DARK_RED : ChatFormatting.GRAY);
        if (st.half) com.newuniverse.nusmp.lords.LordFx.follow(p.serverLevel(), com.newuniverse.nusmp.lords.LordRules.Sin.GREED, com.newuniverse.nusmp.lords.LordVfxCodes.HALF, p, p.position(), 3.0f);
        hud(p, true);
    }

    /** Mode 2: offers a tenth of the magicule (at least 100) as stored EP and fifteen points of satiation. */
    public static void offer(ServerPlayer p) {
        if (!GreedData.bound() || !data().isOwner(p.getUUID())) return;
        try {
            var ex = TensuraStorages.getExistenceFrom(p);
            if (ex == null) return;
            double mp = ex.getMagicule() * 0.10;
            if (mp < 100.0) {
                say(p, "greed.msg.offer_low", "Not enough magicule to offer.", ChatFormatting.GRAY);
                return;
            }
            ex.setMagicule(ex.getMagicule() - mp);
            ex.markDirty();
            GreedState st = GreedStore.of(p);
            st.add(0, 0, 0, mp * 2.0);
            st.setSatiation(st.satiation + 15.0f);
            checkTier(p, st, data());
            hud(p, true);
        } catch (RuntimeException | LinkageError noTensura) {
            // no Tensura data: nothing to offer
        }
    }

    static void checkTier(ServerPlayer p, GreedState st, GreedData d) {
        int reached = Math.max(d.tier(), GreedRules.tierFor(st.storedEp));
        if (reached > d.tier()) {
            d.setTier(reached);
            p.sendSystemMessage(Component.translatableWithFallback("greed.msg.tier", "\"Not bad, partner... we have opened Tier %s.\"", reached).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.8f, 1.4f);
            GreedFx.follow(p.serverLevel(), com.newuniverse.nusmp.vfx.VfxShape.GREED_AWAKEN, p, p.position(), GreedFx.CRIMSON, 30, 9.0f);      // a new tier: the awakening, bigger
        }
    }

    // ---------------------------------------------------------------- devouring
    /** A kill by the wielder: absorb a share of the victim's EP into stats, satiation and stored EP; steal a skill into the vault. */
    public static void onKill(LivingEntity victim, ServerPlayer killer) {
        if (!GreedConfig.enabled() || !GreedData.bound() || victim == killer) return;
        GreedData d = data();
        if (!d.isOwner(killer.getUUID())) return;
        GreedState st = GreedStore.of(killer);
        double ep = TensuraBridge.ep(victim);
        if (ep <= 0.0) ep = victim.getMaxHealth() * 10.0;
        double absorbed = ep * GreedRules.absorbShare(st.half) * GreedConfig.absorb();
        double[] g = GreedRules.gains(absorbed);
        st.add(g[0], g[1], g[2], absorbed);
        st.setSatiation(st.satiation + GreedRules.feed(absorbed));
        if (GreedConfig.STEAL_SKILLS.get() && (victim instanceof ServerPlayer || killer.getRandom().nextDouble() < GreedRules.STEAL_MOB)) stealSkill(killer, victim, st);
        if (com.newuniverse.nusmp.lords.LordConfig.hungerOfTheBlade() && st.half)
            com.newuniverse.nusmp.lords.LordService.grow(killer, com.newuniverse.nusmp.lords.LordStore.of(killer), com.newuniverse.nusmp.lords.LordRules.Sin.GREED, (int) Math.round(com.newuniverse.nusmp.lords.LordRules.xpForKill(victim.getMaxHealth()) * com.newuniverse.nusmp.lords.LordConfig.masteryScale()));   // feeding in the state teaches it
        applyAttributes(killer, st);
        if (GreedFx.mawReady(killer.serverLevel().getGameTime()))
            GreedFx.send(killer.serverLevel(), com.newuniverse.nusmp.vfx.VfxShape.GREED_MAW, GreedFx.chest(victim), GreedFx.chest(killer), GreedFx.CRIMSON, 24, victim.getBbHeight());      // VFX wave 2: the sword devours (at most one maw every half second, so a technique that kills eight sends one)
        checkTier(killer, st, d);
        hud(killer, false);
    }

    private static void stealSkill(ServerPlayer killer, LivingEntity victim, GreedState st) {
        try {
            Skills skills = SkillAPI.getSkillsFrom(victim);
            if (skills == null) return;
            ArrayList<ManasSkillInstance> pool = new ArrayList<>();
            for (ManasSkillInstance i : new ArrayList<>(skills.getLearnedSkills())) {
                if (!(i.getSkill() instanceof Skill s) || s.getType() == Skill.SkillType.ULTIMATE) continue;
                if (i.getSkill() == com.newuniverse.nusmp.skill.NUSkills.LORD_OF_GLUTTONOUS_GREED.get()) continue;
                pool.add(i);
            }
            if (pool.isEmpty()) return;
            ManasSkillInstance pick = pool.get(killer.getRandom().nextInt(pool.size()));
            ResourceLocation id = pick.getSkill().getRegistryName();
            if (id != null && st.addSkill(id.toString())) say(killer, "greed.msg.stolen", "Devoured a skill: %s", ChatFormatting.DARK_PURPLE, pick.getSkill().getName());
        } catch (RuntimeException | LinkageError noSkills) {
            // the victim has no skill data
        }
    }

    // ---------------------------------------------------------------- the vault
    /** The vault entry's Tensura skill, or null. */
    public static ManasSkill skillOf(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : SkillAPI.getSkillRegistry().get(rl);
    }

    public static String vaultEquip(ServerPlayer p, int index) {
        GreedState st = GreedStore.of(p);
        if (!isWielder(p) || index < 0 || index >= st.vaultSize()) return "greed.vault.none";
        String id = st.vaultIds().get(index);
        ManasSkill sk = skillOf(id);
        if (sk == null) return "greed.vault.none";
        Skills skills = SkillAPI.getSkillsFrom(p);
        if (skills.getSkill(sk.getRegistryName()).isPresent()) return "greed.vault.have";
        skills.learnSkill(sk.createDefaultInstance(), Component.translatableWithFallback("greed.vault.equipped", "Greed lends you a devoured skill.").withStyle(ChatFormatting.DARK_PURPLE));
        return "greed.vault.ok";
    }

    public static String vaultFavourite(ServerPlayer p, int index) {
        GreedState st = GreedStore.of(p);
        if (!isWielder(p) || index < 0 || index >= st.vaultSize()) return "greed.vault.none";
        st.toggleFavourite(st.vaultIds().get(index));
        return "greed.vault.ok";
    }

    /** Sacrifices a vault skill (not a favourite): the skill leaves the vault and the wielder, its EP value is stored, satiation rises. */
    public static String vaultSacrifice(ServerPlayer p, int index) {
        GreedState st = GreedStore.of(p);
        if (!isWielder(p) || index < 0 || index >= st.vaultSize()) return "greed.vault.none";
        String id = st.vaultIds().get(index);
        if (st.favourited(id)) return "greed.vault.fav";
        ManasSkill sk = skillOf(id);
        String type = sk instanceof Skill s ? s.getType().name() : "";
        st.removeSkill(id);
        if (sk != null) {
            try {
                SkillAPI.getSkillsFrom(p).forgetSkill(sk.getRegistryName(), Component.translatableWithFallback("greed.vault.sacrificed", "Greed devours the skill.").withStyle(ChatFormatting.DARK_RED));
            } catch (RuntimeException ignored) {
                // not learned: only the vault entry goes
            }
        }
        st.add(0, 0, 0, GreedRules.sacrificeEp(type));
        st.setSatiation(st.satiation + GreedRules.SACRIFICE_FEED);
        checkTier(p, st, data());
        hud(p, true);
        return "greed.vault.ok";
    }

    private static boolean isWielder(ServerPlayer p) { return GreedData.bound() && data().isOwner(p.getUUID()); }

    // ---------------------------------------------------------------- attributes
    /** Writes the devoured stats as three transient attribute modifiers (removed when there are none, 1.5 times in Half-Starvation). */
    public static void applyAttributes(ServerPlayer p, GreedState st) {
        boolean wielder = GreedData.bound() && data().isOwner(p.getUUID());
        boolean blade = com.newuniverse.nusmp.lords.LordConfig.hungerOfTheBlade();
        double m = st.half ? (blade ? com.newuniverse.nusmp.lords.LordRules.halfStats(halfLevel(p)) : 1.5) : 1.0;
        if (blade && GreedConfig.STARVATION.get() && GreedData.bound() && data().tier() < GreedRules.MAX_TIER) m *= com.newuniverse.nusmp.lords.LordRules.bonusShare(st.satiation);     // a ravenous blade lends 70% of the devoured stats, a sleeping one none
        mod(p.getAttribute(Attributes.MAX_HEALTH), HP_MOD, wielder ? st.bonusHp * m : 0.0);
        mod(p.getAttribute(Attributes.ATTACK_DAMAGE), ATK_MOD, wielder ? st.bonusAtk * m : 0.0);
        mod(p.getAttribute(Attributes.ARMOR), ARMOR_MOD, wielder ? st.bonusArmor * m : 0.0);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    private static void mod(AttributeInstance a, ResourceLocation id, double amount) {
        if (a == null) return;
        a.removeModifier(id);
        if (amount > 0.0) a.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    // ---------------------------------------------------------------- hunger
    /** The hunger clock, once a second for the wielder. */
    public static void hungerTick(ServerPlayer p) {
        if (!GreedConfig.enabled() || !GreedData.bound()) return;
        GreedData d = data();
        if (!d.isOwner(p.getUUID())) return;
        GreedState st = GreedStore.of(p);
        boolean blade = com.newuniverse.nusmp.lords.LordConfig.hungerOfTheBlade();
        if (GreedConfig.STARVATION.get() && !p.isCreative() && !p.isSpectator()) {
            double dec = GreedRules.decayPerSecond(st.bonusHp, st.bonusAtk, st.bonusArmor, st.half && !blade, d.tier(), GreedConfig.hunger());
            if (blade && st.half) dec *= com.newuniverse.nusmp.lords.LordRules.halfHunger(halfLevel(p));       // Half-Starved Mastery: the hunger of the state eases with each level
            st.setSatiation(st.satiation - (float) dec);
            if (GreedRules.starving(st.satiation, d.tier())) {
                if (blade) hungerOfTheBlade(p, st, d);
                else starve(p, st, d);
            }
        }
        if (blade && st.half && !p.isCreative() && !p.isSpectator()) com.newuniverse.nusmp.lords.LordService.grow(p, com.newuniverse.nusmp.lords.LordStore.of(p), com.newuniverse.nusmp.lords.LordRules.Sin.GREED, halfXp(p));   // the mastery of the state grows with the seconds spent in it
        hud(p, false);
    }

    /** The level of Half-Starved Mastery (0 when the lords package is off). */
    public static int halfLevel(ServerPlayer p) { return com.newuniverse.nusmp.lords.LordStore.of(p).level(com.newuniverse.nusmp.lords.LordRules.Sin.GREED); }

    private static int halfXp(ServerPlayer p) {
        double s = com.newuniverse.nusmp.lords.LordConfig.masteryScale() * com.newuniverse.nusmp.lords.LordRules.XP_PER_SECOND;
        return s >= 1.0 ? (int) Math.round(s) : p.getRandom().nextDouble() < s ? 1 : 0;
    }

    /** The share of a heal the wielder still receives: below 20% satiation the blade takes half of it (Half-Starved Mastery gives it back); 1.0 otherwise or when the old rules are chosen. */
    public static float healFactor(ServerPlayer p) {
        if (!com.newuniverse.nusmp.lords.LordConfig.hungerOfTheBlade() || !GreedConfig.enabled() || !GreedData.bound() || !data().isOwner(p.getUUID())) return 1.0f;
        GreedState st = GreedStore.peek(p);
        return st != null && st.satiation < com.newuniverse.nusmp.lords.LordRules.RAVENOUS_BELOW && data().tier() < GreedRules.MAX_TIER ? com.newuniverse.nusmp.lords.LordRules.ravenousHeal(halfLevel(p)) : 1.0f;
    }

    /** Hunger of the Blade (the new rules): a ravenous blade keeps its night sight and speed, slows the weak around, and drinks 1.5% of the wielder's magicule a second; it never touches the food bar, never hurts the wielder and never leaves: at 0% it only sleeps (bonuses off) until it is fed. */
    private static void hungerOfTheBlade(ServerPlayer p, GreedState st, GreedData d) {
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 60, 0, true, false));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, true, false));
        ServerLevel level = p.serverLevel();
        int n = 0;
        for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(8.0), x -> x.isAlive() && x.getMaxHealth() < p.getMaxHealth())) {
            if (n++ >= GreedRules.MAX_TARGETS) break;
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4, true, false));
        }
        try {
            var ex = TensuraStorages.getExistenceFrom(p);
            if (ex != null && ex.getMagicule() > 0.0) {
                ex.setMagicule(ex.getMagicule() * (1.0 - com.newuniverse.nusmp.lords.LordRules.RAVENOUS_DRAIN_PERCENT / 100.0));
                ex.markDirty();
            }
        } catch (RuntimeException | LinkageError noTensura) {
            // no Tensura data: nothing to drink
        }
        com.newuniverse.nusmp.lords.LordFx.follow(level, com.newuniverse.nusmp.lords.LordRules.Sin.GREED, com.newuniverse.nusmp.lords.LordVfxCodes.STATE, p, p.position(), 1.4f);
        if (st.satiation <= 0.0f && p.tickCount % 400 < 20) say(p, "lords.msg.greed_dormant", "The blade sleeps: feed it by killing, or offer magicule.", ChatFormatting.DARK_RED);
        applyAttributes(p, st);
    }

    private static void starve(ServerPlayer p, GreedState st, GreedData d) {
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 60, 0, true, false));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, true, false));
        ServerLevel level = p.serverLevel();
        int n = 0;
        for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(8.0), x -> x.isAlive() && x.getMaxHealth() < p.getMaxHealth())) {
            if (n++ >= GreedRules.MAX_TARGETS) break;
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4, true, false));
        }
        com.newuniverse.nusmp.perf.PerfParticles.send(level, new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.9f, 0.05f, 0.1f), 0.8f), p.getX(), p.getEyeY(), p.getZ(), 2, 0.2, 0.05, 0.2, 0.0);
        if (st.satiation <= 0.0f) {
            float dmg = 2.0f;
            if (p.getHealth() <= dmg + 0.5f) {
                p.sendSystemMessage(Component.translatableWithFallback("greed.msg.abandoned", "\"You have nothing left to feed me... I am leaving.\"").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                release(p.getServer(), p.getUUID(), level, p.position());
                return;
            }
            p.hurt(level.damageSources().magic(), dmg);   // the sword devours its wielder: magic damage, not the starve type (hunger ban)
        }
    }

    // ---------------------------------------------------------------- the HUD
    /** Sends the state to the wielder when it changed (or when {@code force}); one small packet. Others get a "not the wielder" packet once so their vignette clears. */
    public static void hud(ServerPlayer p, boolean force) {
        boolean owner = GreedData.bound() && data().isOwner(p.getUUID());
        GreedData d = GreedData.bound() ? data() : null;
        GreedState st = owner ? GreedStore.of(p) : null;
        int sat = st == null ? 100 : Math.round(st.satiation), tier = d == null ? 0 : d.tier(), form = d == null ? 0 : d.form();
        int flags = (owner ? 1 : 0) | (st != null && GreedRules.starving(st.satiation, tier) ? 2 : 0) | (st != null && st.half ? 4 : 0);
        int vault = st == null ? 0 : Math.min(255, st.vaultSize());
        com.newuniverse.nusmp.lords.LordService.greedHud(p, owner, sat, st != null && st.half, force);        // Half-Starved Mastery (its own deduplicated packet)
        int key = (sat & 0x7F) | (tier << 7) | (form << 10) | (flags << 13) | (vault << 16);
        Integer last = LAST_HUD.get(p.getUUID());
        if (!force && last != null && last == key) return;
        if (!owner && last == null && !force) return;
        LAST_HUD.put(p.getUUID(), key);
        GreedNet.send(p, new GreedNet.Hud(sat, tier, form, flags, vault));
    }

    // ---------------------------------------------------------------- cleanup
    public static void forget(UUID id) {
        LAST_SWITCH.remove(id);
        LAST_TECHNIQUE.remove(id);
        LAST_HUD.remove(id);
        RETURN.remove(id);
        GreedForms.forget(id);
    }

    public static void clearAll() {
        LAST_SWITCH.clear();
        LAST_TECHNIQUE.clear();
        LAST_HUD.clear();
        RETURN.clear();
        GreedForms.clearAll();
    }

    public static int trackedPlayers() { return LAST_SWITCH.size() + LAST_TECHNIQUE.size() + LAST_HUD.size() + RETURN.size(); }
}
