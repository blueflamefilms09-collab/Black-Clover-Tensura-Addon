package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.book.ForbiddenMagic;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.item.NUItems;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The Infernal Contract (the mod's Devil Contract, remade from the legendary card). The old bargain stays as it was: right-click and pay one of
 * the three prices to bind a devil to your grimoire. The card adds four contracts of its own, signed in blood from the same item:
 * <ul>
 *   <li><b>Blood</b>: the prime of youth. A full heal and, while the contract stands, +2 hearts, +1 attack damage, +4% speed and +10% maximum
 *       aura (the five ability points, spread as the devil likes).</li>
 *   <li><b>Riches</b>: the "15,000 gp" is paid at once in Magic Stones, crystals, emeralds and diamonds (whatever of them exists).</li>
 *   <li><b>Knowledge</b>: Luck +2 (advantage on one knowledge skill), Tensura's Sage kept lent while the contract stands, and a ritual: sneak and use
 *       a plain book to Contact Other Plane, which lends Sage, Analytical Appraisal and Magic Sense for a minute and lights up every hostile presence
 *       within 40 blocks for 10 s (25% of the time the answer is madness: nausea and weakness).</li>
 *   <li><b>Power</b>: the extra spell slots become a 15% larger magicule pool and 20% cheaper grimoire magic, for good; and the "three spells once a
 *       day" are three Tensura skills (flame, lightning, a ranged barrier) lent for ten minutes at each new day.</li>
 * </ul>
 * <b>One contract at a time.</b> Signing needs no devil pact and no earlier contract; either one refuses the other (this is also the rule the Devil
 * bond of the Djinn system reads: {@code ForbiddenMagic.devilOf(p) != null} or {@link #signed}).
 * <p>
 * <b>Curse: Damned.</b> Every signee who dies loses their soul to a crystal that drops where they fell: they come back Soulless (-30% maximum
 * health, weakened) until the crystal is broken (use it) while they are in the world, or one day has passed. The crystal is a plain item with the
 * soul written on it, so it can be carried, stolen or hidden, but it can not leave a signee imprisoned for ever.
 */
final class Wave0Contract {
    private Wave0Contract() {}

    enum Kind {
        BLOOD("Contract of Blood"), RICHES("Contract of Riches"), KNOWLEDGE("Contract of Knowledge"), POWER("Contract of Power");

        final String title;
        Kind(String title) { this.title = title; }

        String key() { return name().toLowerCase(Locale.ROOT); }

        static Kind byKey(String k) {
            for (Kind x : values()) if (x.key().equals(k)) return x;
            return null;
        }
    }

    static final String K_CONTRACT = "nusmp_infernal_contract", K_DAMNED_UNTIL = "nusmp_w0_damned_until", K_ORACLE_AT = "nusmp_w0_oracle_at";
    static final String SOUL_OF = "nusmp_w0_soul_of", SOUL_NAME = "nusmp_w0_soul_name";
    static final long DAMNED_TICKS = 24000L;
    static final String K_POWER_DAY = "nusmp_w0_power_day";
    static final String[] POWER_SPELLS = {"tensura:flame_manipulation", "tensura:lightning_manipulation", "tensura:ranged_barrier"};
    static final double POWER_DISCOUNT = 0.20;

    private static final ResourceLocation M_HP = id("w0_blood_hp"), M_STR = id("w0_blood_str"), M_DEX = id("w0_blood_dex"),
            M_LUCK = id("w0_knowledge_luck"), M_DAMNED = id("w0_damned_hp"), M_AURA = id("w0_blood_aura"), M_POOL = id("w0_power_pool");

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    /** The contract this player has signed ("blood", "riches", "knowledge", "power"), or "". */
    static String signed(net.minecraft.world.entity.player.Player p) { return Wave0Util.persisted(p).getString(K_CONTRACT); }

    /** Holds any Devil bond: a devil in the grimoire or a signed Infernal Contract. */
    static boolean bound(ServerPlayer p) { return !signed(p).isEmpty() || ForbiddenMagic.devilOf(p) != null; }

    // ---------------------------------------------------------------- signing
    static boolean sign(ServerPlayer p, Kind k) {
        ItemStack held = Wave0Util.held(p, () -> NUItems.DEVIL_CONTRACT.get());
        if (held.isEmpty()) { GrimoireBook.fail(p, "Hold the Infernal Contract to sign it."); return false; }
        if (bound(p)) { GrimoireBook.fail(p, "You can only have one contract at a time."); return false; }
        if (com.newuniverse.nusmp.relics.djinn.Bonds.refuse(p, com.newuniverse.nusmp.relics.djinn.Bonds.Kind.DEVIL, "an Infernal Contract")) return false;   // one bond: a Djinn or a Spirit forbids it
        Wave0Util.persisted(p).putString(K_CONTRACT, k.key());
        held.shrink(1);
        ServerLevel sl = p.serverLevel();
        p.sendSystemMessage(Component.literal("Signed in blood: the " + k.title + ". It is forever binding.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        Vec3 at = p.position();
        Wave0Util.send(sl, VfxShape.DEVIL_CIRCLE, at.add(0, 0.1, 0), at.add(0, 1, 0), 0xFFB01820, 40, 2.2f);
        Wave0Util.send(sl, VfxShape.CURSE_FX3, at.add(0, 1, 0), at.add(0, 1, 0), 0xFFB01820, 30, 1.2f);
        switch (k) {
            case BLOOD -> {
                applyBoons(p);
                p.setHealth(p.getMaxHealth());
                p.sendSystemMessage(Component.literal("Your youth returns: more life, more strength, a lighter step.").withStyle(ChatFormatting.RED));
            }
            case RICHES -> {
                Wave0Util.give(p, "tensura:magic_stone", 64);
                Wave0Util.give(p, "tensura:high_quality_magic_crystal", 4);
                Wave0Util.give(p, "tensura:medium_quality_magic_crystal", 8);
                Wave0Util.give(p, "minecraft:emerald", 32);
                Wave0Util.give(p, "minecraft:diamond", 8);
                p.sendSystemMessage(Component.literal("Wealth arrives in your hands: stones, crystals and gems.").withStyle(ChatFormatting.GOLD));
            }
            case KNOWLEDGE -> {
                applyBoons(p);
                p.sendSystemMessage(Component.literal("Knowledge floods in. Sneak and use a book to Contact Other Plane.").withStyle(ChatFormatting.AQUA));
                oracle(p, true);
            }
            case POWER -> {
                applyBoons(p);
                CompoundTag d = Wave0Util.persisted(p);
                d.putDouble("nusmp_mana_tax", Math.max(-0.5, d.getDouble("nusmp_mana_tax") - POWER_DISCOUNT));
                p.sendSystemMessage(Component.literal("More slots in your pages: a larger pool and grimoire magic 20% cheaper, for good.").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- boons that stand while the contract does
    /** Once every two seconds: the attribute modifiers of the signed contract and of the Damned curse. */
    static void applyBoons(ServerPlayer p) {
        String c = signed(p);
        boolean blood = Kind.BLOOD.key().equals(c), knowledge = Kind.KNOWLEDGE.key().equals(c), power = Kind.POWER.key().equals(c);
        modifier(p, Attributes.MAX_HEALTH, M_HP, 4.0, AttributeModifier.Operation.ADD_VALUE, blood);
        modifier(p, Attributes.ATTACK_DAMAGE, M_STR, 1.0, AttributeModifier.Operation.ADD_VALUE, blood);
        modifier(p, Attributes.MOVEMENT_SPEED, M_DEX, 0.04, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, blood);
        modifier(p, Attributes.LUCK, M_LUCK, 2.0, AttributeModifier.Operation.ADD_VALUE, knowledge);
        modifier(p, tensuraAttribute("max_aura"), M_AURA, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, blood);
        modifier(p, tensuraAttribute("max_magicule"), M_POOL, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, power);
        if (knowledge) Fx.lendSkill(p, "tensura:sage", 100);                                          // the knowledge is Tensura's Sage while the contract stands
        if (power) dailySpells(p);
        boolean damned = !c.isEmpty() && p.level().getGameTime() < Wave0Util.persisted(p).getLong(K_DAMNED_UNTIL);
        modifier(p, Attributes.MAX_HEALTH, M_DAMNED, -0.30, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, damned);
        if (damned) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, true, false));
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** A Tensura attribute by its path ("max_aura"), or null if Tensura does not have it. */
    private static Holder<Attribute> tensuraAttribute(String path) {
        return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.fromNamespaceAndPath("tensura", path)).map(h -> (Holder<Attribute>) h).orElse(null);
    }

    /** "Three spells as once-per-day abilities": at the first look of each new day the signee is lent three Tensura skills for ten minutes. */
    private static void dailySpells(ServerPlayer p) {
        CompoundTag d = Wave0Util.persisted(p);
        long today = Wave0Util.today(p);
        if (d.contains(K_POWER_DAY) && d.getLong(K_POWER_DAY) == today) return;
        d.putLong(K_POWER_DAY, today);
        int n = Fx.lendSkills(p, 12000, POWER_SPELLS);
        if (n > 0) p.sendSystemMessage(Component.literal("The pact grants today's three spells (" + n + " of them answered): flame, lightning, a barrier. Ten minutes.").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static void modifier(ServerPlayer p, Holder<Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op, boolean on) {
        if (attr == null) return;
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        boolean has = inst.hasModifier(id);
        if (on && !has) inst.addTransientModifier(new AttributeModifier(id, amount, op));
        else if (!on && has) inst.removeModifier(id);
    }

    // ---------------------------------------------------------------- Knowledge: Contact Other Plane
    /** The ritual: every hostile presence within 40 blocks (at most 24) glows for 10 s; a quarter of the time the answer is madness. */
    static boolean oracle(ServerPlayer p, boolean free) {
        long now = p.level().getGameTime();
        CompoundTag d = Wave0Util.persisted(p);
        if (!free && now < d.getLong(K_ORACLE_AT)) {
            GrimoireBook.fail(p, "The other plane is not listening yet (" + ((d.getLong(K_ORACLE_AT) - now + 19) / 20) + " s).");
            return false;
        }
        d.putLong(K_ORACLE_AT, now + 6000);
        Fx.lendSkills(p, 1200, "tensura:sage", "tensura:analytical_appraisal", "tensura:magic_sense");   // the ritual, merged from Tensura's three seeing skills
        int n = 0;
        for (LivingEntity e : GrimoireBook.around(p, p.position(), 40)) {
            if (n >= 24) break;
            if (!(e instanceof Enemy) && !(e instanceof Mob m && m.getTarget() != null)) continue;
            if (e instanceof net.minecraft.world.entity.player.Player pl && pl.isCreative()) continue;
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
            n++;
        }
        Wave0Util.send(p.serverLevel(), VfxShape.MAGIC_CIRCLE, p.position().add(0, 0.1, 0), p.position().add(0, 1, 0), 0xFF7FD0FF, 40, 2.4f);
        if (!Wave0Util.saves(p, 6, 0)) {                                                            // about one in four answers is madness (DC 6 against a plain wielder)
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
            p.sendSystemMessage(Component.literal("The other plane answers in madness.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        } else {
            p.sendSystemMessage(Component.literal("The other plane whispers: " + n + (n == 1 ? " presence stirs" : " presences stir") + " near you.").withStyle(ChatFormatting.AQUA));
        }
        return true;
    }

    // ---------------------------------------------------------------- Curse: Damned
    /** A signee died: the soul goes into a crystal where they fell. */
    static void onDeath(ServerPlayer p) {
        if (signed(p).isEmpty()) return;
        ServerLevel sl = p.serverLevel();
        Wave0Util.persisted(p).putLong(K_DAMNED_UNTIL, sl.getGameTime() + DAMNED_TICKS);
        Item crystal = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("tensura", "high_quality_magic_crystal")).orElse(Items.AMETHYST_SHARD);
        ItemStack s = new ItemStack(crystal);
        s.set(DataComponents.CUSTOM_NAME, Component.literal("Soul of " + p.getName().getString()).withStyle(ChatFormatting.DARK_RED));
        CompoundTag t = new CompoundTag();
        t.putUUID(SOUL_OF, p.getUUID());
        t.putString(SOUL_NAME, p.getName().getString());
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        ItemEntity drop = new ItemEntity(sl, p.getX(), p.getY() + 0.5, p.getZ(), s);
        drop.setDeltaMovement(0, 0.25, 0);
        sl.addFreshEntity(drop);
        Wave0Util.send(sl, VfxShape.CURSE_FX1, p.position().add(0, 1, 0), p.position().add(0, 2, 0), 0xFFB01820, 40, 1.5f);
    }

    /** Using the crystal frees the soul (if its owner is in the world); true if the stack was a soul crystal. */
    static boolean breakCrystal(ServerPlayer breaker, ItemStack s) {
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return false;
        CompoundTag t = cd.copyTag();
        if (!t.hasUUID(SOUL_OF)) return false;
        ServerPlayer owner = breaker.getServer() == null ? null : breaker.getServer().getPlayerList().getPlayer(t.getUUID(SOUL_OF));
        if (owner == null) {
            GrimoireBook.fail(breaker, "The soul in the crystal is far away: free it while its owner is in the world.");
            return true;
        }
        s.shrink(1);
        Wave0Util.persisted(owner).putLong(K_DAMNED_UNTIL, 0);
        applyBoons(owner);
        owner.sendSystemMessage(Component.literal("Your soul is freed from the crystal.").withStyle(ChatFormatting.AQUA));
        Wave0Util.send(breaker.serverLevel(), VfxShape.LIGHT_FLARE, breaker.position().add(0, 1.4, 0), breaker.position().add(0, 1.4, 0), 0xFFFFFFFF, 20, 1.6f);
        return true;
    }

    // ---------------------------------------------------------------- the clickable terms
    /** The four terms of the card, appended to the old three prices when the contract is used. */
    static void offerTerms(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl) || bound(p)) return;
        SpellRuntime.later(sl, 1, () -> {
            if (!p.isAlive()) return;
            p.sendSystemMessage(Component.literal("Or sign one of the four Infernal Contracts (you may have only one):").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            for (Kind k : Kind.values()) {
                p.sendSystemMessage(Component.literal(" [" + k.title + "]").withStyle(s -> s.withColor(ChatFormatting.RED)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/infernalpact " + k.key()))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(describe(k))))));
            }
        });
    }

    static String describe(Kind k) {
        return switch (k) {
            case BLOOD -> "The prime of your youth: +2 hearts, +1 attack damage, +4% speed, +10% maximum aura.";
            case RICHES -> "A fortune in Magic Stones, crystals and gems, paid at once.";
            case KNOWLEDGE -> "Luck +2, Tensura's Sage lent, and the ritual Contact Other Plane (sneak and use a book).";
            case POWER -> "A 15% larger magicule pool, grimoire magic 20% cheaper, and three Tensura spells lent every day.";
        };
    }

    static List<String> keys() { return List.of("blood", "riches", "knowledge", "power"); }

    static UUID soulOwner(ItemStack s) {
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return null;
        CompoundTag t = cd.copyTag();
        return t.hasUUID(SOUL_OF) ? t.getUUID(SOUL_OF) : null;
    }
}
