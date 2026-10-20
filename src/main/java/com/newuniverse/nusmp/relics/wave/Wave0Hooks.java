package com.newuniverse.nusmp.relics.wave;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.item.CuriosLink;
import com.newuniverse.nusmp.item.MagebaneShieldArts;
import com.newuniverse.nusmp.item.MagicWeaponItem;
import com.newuniverse.nusmp.item.NUItems;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicEngine;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Wave 0: the game-bus listeners of the remade sixteen. Every one starts with a cheap test (who is hit, what is held) and does its real work
 * only for the relics' wielders; nothing here scans the world on a tick. Features that need attunement read their flag with
 * {@link RelicFlags#has}, so an unattuned copy does none of them (the old right-click techniques of the items themselves are untouched).
 */
final class Wave0Hooks {
    private Wave0Hooks() {}

    static final String ONCE = "nusmp_w0_once_", DOOMED = "nusmp_w0_doomed_until", ABSORB_UNTIL = "nusmp_w0_absorb_until", ABSORB_TYPE = "nusmp_w0_absorb_type";
    static final String REACT_AT = "nusmp_w0_react_at", BROOCH = "nusmp_w0_brooch_until";

    static void register() {
        NeoForge.EVENT_BUS.addListener(Wave0Hooks::onDamagePost);
        NeoForge.EVENT_BUS.addListener(Wave0Hooks::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave0Hooks::onEffect);
        NeoForge.EVENT_BUS.addListener(Wave0Hooks::onHeal);
        NeoForge.EVENT_BUS.addListener(Wave0Hooks::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(Wave0Hooks::onVisibility);
        NeoForge.EVENT_BUS.addListener(Wave0Hooks::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wave0Hooks::onDeath);
        NeoForge.EVENT_BUS.addListener(Wave0Hooks::onLogout);
        NeoForge.EVENT_BUS.addListener(Wave0Hooks::onStopping);
        NeoForge.EVENT_BUS.addListener(Wave0Hooks::onRightClick);
        NeoForge.EVENT_BUS.addListener(Wave0Hooks::onCommands);
        io.github.manasmods.manascore.skill.api.SkillEvents.ACTIVATE_SKILL.register((skill, user, key, mode) -> {
            onSkill(user);
            return dev.architectury.event.EventResult.pass();
        });
    }

    /** At most once every {@code gap} ticks per creature and feature: the old techniques land several blows in one swing, the card's extras count once. */
    private static boolean once(LivingEntity t, String name, long gap) {
        long now = t.level().getGameTime();
        CompoundTag d = t.getPersistentData();
        if (now - d.getLong(ONCE + name) < gap) return false;
        d.putLong(ONCE + name, now);
        return true;
    }

    private static String typeId(DamageSource s) { return s.typeHolder().unwrapKey().map(k -> k.location().getPath()).orElse(""); }

    /** Spell damage: vanilla magic, or any Tensura damage that is not physical (the same test the Magebane Shield itself makes). */
    static boolean isMagic(DamageSource s) {
        if (s.is(DamageTypes.MAGIC) || s.is(DamageTypes.INDIRECT_MAGIC)) return true;
        var key = s.typeHolder().unwrapKey();
        return key.isPresent() && "tensura".equals(key.get().location().getNamespace())
                && !s.is(io.github.manasmods.tensura.data.TensuraTags.DamageTypes.IS_PHYSICAL);
    }

    // ================================================================ on every hit a player lands
    static void onDamagePost(LivingDamageEvent.Post e) {
        if (Wave0Util.busy()) return;
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof ServerPlayer p)) return;
        LivingEntity t = e.getEntity();
        if (t == p) return;
        boolean melee = src.is(DamageTypes.PLAYER_ATTACK);
        CompoundTag pd = p.getPersistentData();
        if (melee && pd.contains(ABSORB_UNTIL)) absorbedBlow(p, t);
        ItemStack mh = p.getMainHandItem();
        if (!(mh.getItem() instanceof MagicWeaponItem w)) return;
        boolean bolt = src.is(DamageTypes.INDIRECT_MAGIC);
        switch (w.kind) {
            case QUICKLINGS_IVY -> ivy(p, t, melee);
            case MAGEBANE_LONGSWORD -> longsword(p, t, melee, e.getNewDamage());
            case BOLT_THROWER -> thrower(p, t, bolt);
            case CROSSBOW_OF_SWIFT_DEATH -> swiftDeath(p, t, bolt, e.getNewDamage());
            case CLUB_OF_ROT -> club(p, t, melee);
            case DOOM_HAMMER -> doom(p, t, melee);
            case PRISM_BLADE -> prism(p, t, melee);
            case PERMAFROST_SABRE -> blade(p, t, melee, Wave0.F_SABRE, "sabre", "minecraft:freeze", 0xFFAEEBFF);
            case CYCLONE_RAPIER -> blade(p, t, melee, Wave0.F_RAPIER, "rapier", "minecraft:magic", 0xFF78F5E8);
            case BLOODBOUND_KATANA -> katana(p, t, melee);
            default -> { }
        }
    }

    private static boolean near(ServerPlayer p, LivingEntity t) { return p.distanceToSqr(t) <= 36; }

    // ---------------------------------------------------------------- Quickling's Ivy: +1d4 poison, the sprouted ivy, the free ivy on a critical hit
    private static void ivy(ServerPlayer p, LivingEntity t, boolean melee) {
        if (!melee || !near(p, t) || !RelicFlags.has(p, Wave0.F_IVY)) return;
        CompoundTag d = p.getPersistentData();
        boolean primed = d.getLong(Wave0Acts.K_IVY) > p.level().getGameTime();
        if (primed) d.putLong(Wave0Acts.K_IVY, 0);
        if (once(t, "ivy", 5)) Wave0Util.extra(p, t, Wave0Util.roll(p, 1, 4), "minecraft:magic");
        if ((primed || Wave0Util.crit(p)) && t.isAlive()) {
            Wave0Util.restrain(t, 60, 16, 0);
            Wave0Util.send(p.serverLevel(), VfxShape.TREE_ROOTS, t.position(), t.position(), 0xFF6CE05A, 40, 1.4f);
        }
    }

    // ---------------------------------------------------------------- Magebane Longsword: a hit on a caster breaks concentration
    private static void longsword(ServerPlayer p, LivingEntity t, boolean melee, float damage) {
        if (!melee || !t.isAlive() || !RelicFlags.has(p, Wave0.F_MBS)) return;
        boolean casting = t.isUsingItem();
        if (casting) t.stopUsingItem();
        if (!casting && !Wave0Util.hasBuff(t)) return;
        int dc = Math.max(10, (int) (damage / 2f));
        if (!Wave0Util.saves(t, dc, -1)) {                                                          // disadvantage on the concentration save
            Wave0Util.stripOne(t);
            Nullification.interfere(t, 3, 0.7f, 0.25f);
            Fx.addEffect(t, "tensura:magic_interference", 60, 0);
            Wave0Util.send(p.serverLevel(), VfxShape.KOTO_SHATTER, t.getBoundingBox().getCenter(), t.position(), 0xFF2FB6AE, 14, 0.9f);
        }
    }

    // ---------------------------------------------------------------- Bolt Thrower: +2 force, +1d8 against constructs
    private static void thrower(ServerPlayer p, LivingEntity t, boolean bolt) {
        if (!bolt || !t.isAlive() || !RelicFlags.has(p, Wave0.F_BOLT) || !once(t, "bolt", 2)) return;
        float extra = 2f + (Wave0Util.isConstruct(t) ? Wave0Util.roll(p, 1, 8) : 0);
        Wave0Util.extra(p, t, extra, "minecraft:indirect_magic");
    }

    // ---------------------------------------------------------------- Crossbow of Swift Death: a surprised creature is struck as by a crit, and saves against fear
    private static void swiftDeath(ServerPlayer p, LivingEntity t, boolean bolt, float damage) {
        if (!bolt || !t.isAlive() || !RelicFlags.has(p, Wave0.F_SWIFT)) return;
        if (!Wave0Util.surprised(p, t) || !once(t, "swift", 6)) return;
        Wave0Util.extra(p, t, damage * 0.6f, "minecraft:indirect_magic");                           // the surprise: the weight of a critical hit
        if (t.isAlive() && BalanceLaw.beginControl(p)) Wave0Util.frighten(p, t, 100, 12, 0);
        Vec3 mid = t.getBoundingBox().getCenter();
        Wave0Util.send(p.serverLevel(), VfxShape.BLOOD_FX1, mid, t.position(), 0xFFD0183A, 10, 0.7f);
    }

    // ---------------------------------------------------------------- Club of Rot: a critical hit rots for 2d6 necrotic and sickens (Constitution save DC 15)
    private static void club(ServerPlayer p, LivingEntity t, boolean melee) {
        if (!melee || !t.isAlive() || !near(p, t) || !Wave0Util.crit(p) || Wave0Util.isUndead(t) || Wave0Util.isConstruct(t)) return;
        if (!once(t, "club", 10)) return;
        Wave0Util.extra(p, t, Wave0Util.roll(p, 2, 6), "minecraft:wither");
        if (t.isAlive()) Wave0Util.sicken(t, 100, 15);
        Wave0Util.send(p.serverLevel(), VfxShape.CURSE_FX1, t.getBoundingBox().getCenter(), t.position(), 0xFF7BD148, 14, 0.8f);
    }

    // ---------------------------------------------------------------- Doom Hammer: the finishing blow, and a wound that will not close
    private static void doom(ServerPlayer p, LivingEntity t, boolean melee) {
        if (!melee || !t.isAlive() || !near(p, t) || Wave0Util.isUndead(t) || Wave0Util.isConstruct(t)) return;
        float frac = t.getHealth() / Math.max(1f, t.getMaxHealth());
        if (frac > 0.35f || !once(t, "doom", 10)) return;
        long now = t.level().getGameTime();
        t.getPersistentData().putLong(DOOMED, now + 160);                                           // can not be stabilised: healing is halved
        Fx.addEffect(t, "tensura:curse", 160, 0);
        if (frac <= 0.25f) {
            Wave0Util.extra(p, t, Wave0Util.roll(p, 5, 6), "minecraft:wither");
            Wave0Util.send(p.serverLevel(), VfxShape.CURSE_FX3, t.getBoundingBox().getCenter(), t.position(), 0xFFE0381C, 20, 1.1f);
            p.serverLevel().playSound(null, t.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.4f, 0.5f);
        }
    }

    // ---------------------------------------------------------------- Prism Blade: +1d8 radiant a hit, the Chromatic Strike
    private static void prism(ServerPlayer p, LivingEntity t, boolean melee) {
        if (!melee || !t.isAlive() || !near(p, t) || !RelicFlags.has(p, Wave0.F_PRISM)) return;
        if (once(t, "prism", 5)) Wave0Util.extra(p, t, Wave0Util.roll(p, 1, 8), "minecraft:magic");
        CompoundTag d = p.getPersistentData();
        if (d.getLong(Wave0Acts.K_CHROMA) > p.level().getGameTime() && t.isAlive()) {
            boolean charm = d.getBoolean(Wave0Acts.K_CHROMA_CHARM);
            d.putLong(Wave0Acts.K_CHROMA, 0);
            Wave0Util.extra(p, t, Wave0Util.roll(p, 4, 8), "minecraft:magic");
            if (t.isAlive()) {
                if (charm) Wave0Util.charm(p, t, 100, 15, 0);
                else Wave0Util.frighten(p, t, 100, 15, 0);
            }
            Vec3 mid = t.getBoundingBox().getCenter();
            Wave0Util.send(p.serverLevel(), VfxShape.LIGHT_FLARE, mid, mid, 0xFFB890FF, 16, 1.6f);
        }
    }

    // ---------------------------------------------------------------- Permafrost Sabre and Cyclone Rapier: +1d6 cold / force (2d6 as a set)
    private static void blade(ServerPlayer p, LivingEntity t, boolean melee, String flag, String name, String type, int colour) {
        if (!melee || !t.isAlive() || !near(p, t) || !RelicFlags.has(p, flag) || !once(t, name, 5)) return;
        boolean set = RelicFlags.has(p, Wave0.F_SABRE) && RelicFlags.has(p, Wave0.F_RAPIER);
        Wave0Util.extra(p, t, Wave0Util.roll(p, set ? 2 : 1, 6), type);
        if (flag.equals(Wave0.F_SABRE)) Fx.addEffect(t, "tensura:chill", 60, 0);
        Vec3 mid = t.getBoundingBox().getCenter();
        com.newuniverse.nusmp.particle.fx.Fx.spray(p.serverLevel(), FxThemes.RELIC_WAVE_0, FxTone.LIGHT, mid.x, mid.y, mid.z, 5, 0.25, 0.3, 0.25, 0.04);
    }

    // ---------------------------------------------------------------- Bloodbound Katana: the card's rules on top of the item's own charges
    private static int blood(ItemStack s) { return Math.max(0, Math.min(Wave0Rules.BLOOD_MAX, Wave0Util.stackInt(s, "BloodCharge", 0))); }

    private static void katana(ServerPlayer p, LivingEntity t, boolean melee) {
        if (!melee || !(p.level() instanceof ServerLevel sl)) return;
        final int before = blood(p.getMainHandItem());
        SpellRuntime.later(sl, 1, () -> {                                                           // after the item's own on-hit has added its charge
            if (!p.isAlive()) return;
            ItemStack s = p.getMainHandItem();
            if (!(s.getItem() instanceof MagicWeaponItem w) || w.kind != MagicWeaponItem.Kind.BLOODBOUND_KATANA) return;
            int after = blood(s), now = after;
            boolean feeds = !Wave0Util.isUndead(t) && !Wave0Util.isConstruct(t);
            if (after > before && !feeds) now = before;                                            // the dead and the made have no blood to give
            if (feeds && RelicEngine.isOn(p, "bloodbound_katana", "blood_sip")) {
                int cost = Wave0Rules.sipCost(Wave0Util.dice(p));
                if (Wave0Rules.canSip(p.getHealth(), cost)) {
                    p.hurt(p.damageSources().magic(), cost);
                    now = Math.min(Wave0Rules.BLOOD_MAX, now + 1);
                    Wave0Util.send(sl, VfxShape.BLOOD_FX1, p.position().add(0, 1, 0), t.getBoundingBox().getCenter(), 0xFFD0183A, 10, 0.6f);
                }
            }
            if (RelicEngine.isOn(p, "bloodbound_katana", "bloodflare") && t.isAlive()) {
                for (int i = 0, n = Wave0Rules.flares(now); i < n; i++) {
                    now -= Wave0Rules.FLARE_COST;
                    Wave0Util.extra(p, t, Wave0Util.roll(p, 1, 8), "minecraft:wither");
                    Wave0Util.send(sl, VfxShape.BLOOD_FX1, t.getBoundingBox().getCenter(), t.position(), 0xFFFF3A55, 10, 0.8f);
                }
            }
            if (Wave0Rules.overflowing(now) && p.getHealth() > 6f) p.hurt(p.damageSources().magic(), 1f);   // a full blade is unstable
            if (now != after) {
                final int v = now;
                Wave0Util.edit(s, tag -> tag.putInt("BloodCharge", v));
            }
        });
    }

    // ---------------------------------------------------------------- Magebane Shield: Absorb Elements' charged blow
    private static void absorbedBlow(ServerPlayer p, LivingEntity t) {
        CompoundTag d = p.getPersistentData();
        if (d.getLong(ABSORB_UNTIL) <= p.level().getGameTime() || !t.isAlive()) return;
        d.putLong(ABSORB_UNTIL, 0);
        Wave0Rules.Element el;
        try {
            el = Wave0Rules.Element.valueOf(d.getString(ABSORB_TYPE));
        } catch (IllegalArgumentException none) {
            el = Wave0Rules.Element.NONE;
        }
        Wave0Util.extra(p, t, Wave0Util.roll(p, 3, 6), Wave0Rules.elementDamageType(el));
        Vec3 mid = t.getBoundingBox().getCenter();
        Wave0Util.send(p.serverLevel(), VfxShape.BARRIER_FX3, mid, mid.add(0, 0.5, 0), 0xFF2FD6C8, 12, 0.9f);
    }

    // ================================================================ damage a player takes
    static void onIncoming(LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        Entity by = src.getEntity();
        if (by != null) {                                                                             // a charmed creature will not strike its charmer
            CompoundTag cd = by.getPersistentData();
            if (cd.hasUUID(Wave0Util.CHARM_BY) && cd.getLong(Wave0Util.CHARM_UNTIL) > by.level().getGameTime()
                    && cd.getUUID(Wave0Util.CHARM_BY).equals(e.getEntity().getUUID())) {
                e.setCanceled(true);
                return;
            }
        }
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getAmount() <= 0f || !RelicFlags.has(p, Wave0.F_MBSH)) return;
        if (MagebaneShieldArts.parryOpen(p)) return;                                                  // the parry already answers it
        float amt = e.getAmount();
        boolean magic = isMagic(src), blast = src.is(DamageTypes.EXPLOSION) || src.is(DamageTypes.PLAYER_EXPLOSION);
        ServerLevel sl = p.serverLevel();
        Vec3 at = p.position().add(0, 1.2, 0);
        // the set bonus: cover behind the shield from a spell that blasts an area (raised shield: certain; otherwise an even chance)
        if ((magic || blast) && amt >= 6f && RelicFlags.has(p, Wave0.F_MBS) && Wave0Util.usesLeft(p, "cover", 1) > 0) {
            Wave0Util.takeDaily(p, "cover", 1);
            boolean covered = p.isBlocking() || Wave0Rules.passes(Wave0Util.dice(p), 0.5);
            Wave0Util.send(sl, VfxShape.BARRIER_FX3, at, at.add(p.getViewVector(1f)), 0xFF2FD6C8, 14, 0.9f);
            sl.playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.5f, 0.9f);
            if (covered) {
                e.setCanceled(true);
                Wave0Util.note(p, "You cover behind the Magebane Shield: no damage.", ChatFormatting.AQUA);
            } else {
                e.setAmount(amt * 0.5f);
                Wave0Util.note(p, "The blast clips you past the shield: half damage.", ChatFormatting.AQUA);
            }
            return;
        }
        // Absorb Elements: the first elemental blow of the day is halved, and the next hit you land carries it back
        Wave0Rules.Element el = Wave0Rules.elementOf(typeId(src));
        if (el != Wave0Rules.Element.NONE && amt >= 3f && Wave0Util.takeDaily(p, "absorb", 1)) {
            e.setAmount(amt * 0.5f);
            p.getPersistentData().putLong(ABSORB_UNTIL, sl.getGameTime() + 600);
            p.getPersistentData().putString(ABSORB_TYPE, el.name());
            String resist = Wave0Rules.elementResistSkill(el);
            if (!resist.isEmpty()) Fx.lendSkill(p, resist, 600);                                      // the element's Tensura resistance, for half a minute
            Fx.addEffect(p, "tensura:magic_barrier", 200, 0);
            Wave0Util.send(sl, VfxShape.BARRIER_FX3, at, at.add(0, 0.6, 0), 0xFF2FD6C8, 14, 0.9f);
            Wave0Util.note(p, "Absorb Elements: your next hit returns it (3d6).", ChatFormatting.AQUA);
            return;
        }
        // advantage on saves against the spells of anyone within five blocks: a fifth less spell damage from near casters
        if (magic && src.getEntity() instanceof LivingEntity att && att != p && att.distanceToSqr(p) <= 25) e.setAmount(amt * 0.8f);
    }

    /** Advantage against the afflictions of a caster within five blocks: a harmful effect is shrugged off more often. */
    static void onEffect(MobEffectEvent.Applicable e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        MobEffectInstance inst = e.getEffectInstance();
        if (inst == null || inst.getEffect().value().isBeneficial() || !RelicFlags.has(p, Wave0.F_MBSH)) return;
        boolean near = false;
        Entity src = e.getEffectSource();
        if (src instanceof LivingEntity l) near = l != p && l.distanceToSqr(p) <= 25;
        else if (src == null) near = !p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(5), x -> x != p && x.isAlive() && x instanceof Enemy).isEmpty();
        if (!near) return;
        double chance = Wave0Rules.advantage(Wave0Rules.saveChance(14, Wave0Util.saveMod(p)));
        if (Wave0Rules.passes(Wave0Util.dice(p), chance)) e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    // ================================================================ small rules
    /** The Doom Hammer's wound: a doomed creature mends half as much. */
    static void onHeal(LivingHealEvent e) {
        LivingEntity t = e.getEntity();
        CompoundTag d = t.getPersistentData();
        if (d.contains(DOOMED) && d.getLong(DOOMED) > t.level().getGameTime()) e.setAmount(e.getAmount() * 0.5f);
    }

    /** Charmed creatures will not take their charmer for a target. */
    static void onChangeTarget(LivingChangeTargetEvent e) {
        LivingEntity next = e.getNewAboutToBeSetTarget();
        if (next == null) return;
        CompoundTag d = e.getEntity().getPersistentData();
        if (d.hasUUID(Wave0Util.CHARM_BY) && d.getLong(Wave0Util.CHARM_UNTIL) > e.getEntity().level().getGameTime()
                && d.getUUID(Wave0Util.CHARM_BY).equals(next.getUUID())) e.setCanceled(true);
    }

    /** The Brooch Pin folded into a brooch passes for jewellery: while it is worn and not drawn, creatures see its wearer a quarter less well. */
    static void onVisibility(LivingEvent.LivingVisibilityEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.getPersistentData().getLong(BROOCH) > p.level().getGameTime()) e.modifyVisibility(0.75);
    }

    // ================================================================ once a second per player
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 11) return;
        long now = p.level().getGameTime();
        // the Brooch Pin worn as a brooch (in a Curios slot, not drawn in a hand)
        boolean drawn = p.getMainHandItem().is(NUItems.BROOCH_PIN.get()) || p.getOffhandItem().is(NUItems.BROOCH_PIN.get());
        if (!drawn && CuriosLink.carries(p, MagicWeaponItem.Kind.BROOCH_PIN)) {
            p.getPersistentData().putLong(BROOCH, now + 50);
            Fx.addEffect(p, "tensura:presence_concealment", 60, 0);                                    // Tensura's own way of not being noticed
        }
        // Thundercleave's charges and the Crossbow of Swift Death's first notches, in the bag
        long today = Wave0Util.today(p);
        boolean storm = p.level().isThundering() && p.level().canSeeSky(p.blockPosition());
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (s.is(NUItems.THUNDERCLEAVE.get())) thunderUpkeep(p, s, today, now, storm);
            else if (s.is(NUItems.CROSSBOW_OF_SWIFT_DEATH.get())) notchUpkeep(p, s);
        }
        if (p.tickCount % 40 == 11) Wave0Contract.applyBoons(p);
    }

    private static void thunderUpkeep(ServerPlayer p, ItemStack s, long today, long now, boolean storm) {
        int cur = Wave0Acts.thunderCharges(s, today), next = cur;
        long lastStorm = Wave0Util.stackLong(s, Wave0Acts.T_STORM, 0);
        boolean aloft = storm && s == p.getMainHandItem();
        if (aloft) next = Wave0Rules.thunderStorm(cur, now - lastStorm);
        boolean stormTick = next > cur;
        if (next != Wave0Util.stackInt(s, Wave0Acts.T_CHARGES, Wave0Rules.THUNDER_MAX) || Wave0Util.stackLong(s, Wave0Acts.T_DAY, -1) != today) {
            final int v = next;
            Wave0Util.edit(s, t -> {
                t.putInt(Wave0Acts.T_CHARGES, v);
                t.putLong(Wave0Acts.T_DAY, today);
                if (stormTick) t.putLong(Wave0Acts.T_STORM, now);
            });
            if (stormTick) {
                Wave0Util.note(p, "Thundercleave drinks the storm: " + next + " of " + Wave0Rules.THUNDER_MAX + " charges.", ChatFormatting.AQUA);
                Wave0Util.send(p.serverLevel(), VfxShape.FX_LIGHTNING_ARC, p.position().add(0, 3, 0), p.getEyePosition(), 0xFF7FE6FF, 8, 0.9f);
            }
        }
    }

    /** "Most of these crossbows have 3d6 notches when first found": a frame with none yet gets its 3d6. */
    private static void notchUpkeep(ServerPlayer p, ItemStack s) {
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        if (cd != null && cd.copyTag().contains("Notches")) return;
        final int n = Wave0Util.roll(p, 3, 6);
        Wave0Util.edit(s, t -> t.putInt("Notches", n));
    }

    // ================================================================ the Magebane Longsword's reaction
    /** A creature within reach of an attuned Magebane Longsword begins a skill or spell: the wielder strikes it (one reaction every three seconds). */
    static void onSkill(LivingEntity user) {
        if (user == null || !(user.level() instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        for (net.minecraft.world.entity.player.Player other : sl.players()) {
            if (!(other instanceof ServerPlayer w)) continue;
            if (w == user || w.distanceToSqr(user) > 12.25 || !w.isAlive()) continue;
            if (!(w.getMainHandItem().getItem() instanceof MagicWeaponItem it) || it.kind != MagicWeaponItem.Kind.MAGEBANE_LONGSWORD) continue;
            if (!RelicFlags.has(w, Wave0.F_MBS) || !Wave0Util.fair(w, user)) continue;
            if (now - w.getPersistentData().getLong(REACT_AT) < 60) continue;
            w.getPersistentData().putLong(REACT_AT, now);
            Fx.damage(w, user, (float) w.getAttributeValue(Attributes.ATTACK_DAMAGE), "minecraft:player_attack");
            if (user.isUsingItem()) user.stopUsingItem();
            Nullification.interfere(user, 2, 0.5f, 0.2f);
            Wave0Util.send(sl, VfxShape.ANTI_MAGIC_SLASH, w.getEyePosition(), user.getBoundingBox().getCenter(), 0xFF2FB6AE, 10, 1.2f);
            Wave0Util.note(w, "Reaction: you strike the caster.", ChatFormatting.AQUA);
            return;
        }
    }

    // ================================================================ life and death
    static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Wave0Fusion.dismiss(p.getUUID());
        Wave0Contract.onDeath(p);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { Wave0Fusion.dismiss(e.getEntity().getUUID()); }

    static void onStopping(ServerStoppingEvent e) { Wave0Fusion.clear(); }

    // ================================================================ using things
    /** The soul crystal frees its soul; a book in a signed Knowledge hand opens the other plane; the contract offers the card's four terms. */
    static void onRightClick(PlayerInteractEvent.RightClickItem e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ItemStack s = e.getItemStack();
        if (s.isEmpty()) return;
        if (s.is(NUItems.DEVIL_CONTRACT.get())) { Wave0Contract.offerTerms(p); return; }
        if (s.is(Items.BOOK) && p.isShiftKeyDown() && Wave0Contract.Kind.KNOWLEDGE.key().equals(Wave0Contract.signed(p))) {
            Wave0Contract.oracle(p, false);
            e.setCancellationResult(InteractionResult.SUCCESS);
            e.setCanceled(true);
            return;
        }
        if (Wave0Contract.soulOwner(s) != null && Wave0Contract.breakCrystal(p, s)) {
            e.setCancellationResult(InteractionResult.SUCCESS);
            e.setCanceled(true);
        }
    }

    /** /infernalpact blood|riches|knowledge|power: sign one of the card's four contracts (hold the Infernal Contract). */
    static void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("infernalpact")
                .then(Commands.argument("terms", StringArgumentType.word())
                        .suggests((c, b) -> { for (String k : Wave0Contract.keys()) b.suggest(k); return b.buildFuture(); })
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            Wave0Contract.Kind k = Wave0Contract.Kind.byKey(StringArgumentType.getString(ctx, "terms"));
                            if (k == null) { ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Choose blood, riches, knowledge or power.")); return 0; }
                            return Wave0Contract.sign(p, k) ? 1 : 0;
                        })));
    }
}
