package com.newuniverse.nusmp.devil;

import com.newuniverse.nusmp.item.NUItems;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxTone;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import com.newuniverse.nusmp.relics.djinn.Bonds;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the rituals of the devils of eight anime worlds (3.76.0, docs/devil_worlds.md), on the server. A ritual is asked for with {@code /devil call <devil>}; {@link #begin} checks the world's
 * conditions ({@link DevilRitualRules}), takes what the ritual itself costs (the relic, the paper and the blood, the offering and the magicules, the book) and starts the run, which lives in the
 * caller's contract attachment. {@link #tick} plays the world's own summoning (pictures from the effect library, chant lines, sounds; the blows of the Devil-Binding Ritual, the will of the Tamer)
 * and breaks it when the caller leaves the circle. At the end the devil makes its offer (two clickable words in chat); {@link #sign} takes the lasting price and writes the contract, which from
 * then on is the 3.48.0 contract (assimilation key, Devil Union, markings) with the devil's tier and attribute.
 *
 * <p>Cost (rule 11): nothing for a player with no ritual (one attachment lookup a tick). A running ritual is a distance check a tick, at most 6 pictures in its 4 to 10 seconds (one packet each to
 * players within 96 blocks, through the effect budget), at most 6 particles every 10 ticks through the particle budget, and a handful of chat lines to the caller. No entity is spawned, no block
 * is changed, nothing is scanned, nothing is kept in a static map.
 */
public final class DevilRituals {
    private DevilRituals() {}

    /** The particles that hang in the air of each world's ritual (themes of particle_themes.json), by {@link DevilWorld#ordinal}. */
    private static final ThemeKey[] THEMES = {
            ThemeKey.of("magic.ANTI_MAGIC"), ThemeKey.of("magic.BLOOD"), ThemeKey.of("magic.WIND"), ThemeKey.of("magic.DEMON_FIRE"),
            ThemeKey.of("magic.SOUL"), ThemeKey.of("magic.CURSE"), ThemeKey.of("magic.BLOOD"), ThemeKey.of("magic.KOTODAMA")};

    static boolean on() { return DevilConfig.enabled() && DevilConfig.worlds(); }

    // ---------------------------------------------------------------- facts
    private static double magiculeShare(ServerPlayer p) {
        if (p.isCreative()) return 1.0;
        try {
            IExistence ex = TensuraStorages.getExistenceFrom(p);
            double max = EnergyHelper.getMaxMagicule(p);
            return ex == null || max <= 0 ? 0.0 : ex.getMagicule() / max;
        } catch (RuntimeException | LinkageError unknown) {
            return 0.0;
        }
    }

    private static double maxMagicules(ServerPlayer p) {
        try {
            return Math.max(0.0, EnergyHelper.getMaxMagicule(p));
        } catch (RuntimeException | LinkageError unknown) {
            return 0.0;
        }
    }

    static boolean offerOpen(DevilContract c, long now) { return c != null && !c.offer.isEmpty() && DevilRitualRules.offerOpen(c.offerAt, now); }

    static DevilRitualRules.Facts facts(ServerPlayer p, DevilContract c, long now) {
        boolean bond = (c != null && c.hasContract()) || !Bonds.canBind(p, Bonds.Kind.DEVIL);
        boolean busy = c != null && (c.ritual != null || offerOpen(c, now));
        long since = c == null || c.lastRitual < 0 ? -1 : now - c.lastRitual;
        float max = Math.max(1f, p.getMaxHealth());
        return new DevilRitualRules.Facts(on(), bond, busy, since, p.getHealth() / max, p.getHealth(), has(p, NUItems.DEVIL_CONTRACT.get(), 1), has(p, Items.PAPER, 1), has(p, Items.BOOK, 1),
                p.isCreative() ? DevilRitualRules.OFFERING_FLESH : p.getInventory().countItem(Items.ROTTEN_FLESH), p.isCreative() ? 999 : p.experienceLevel, magiculeShare(p),
                p.level().canSeeSky(p.blockPosition().above()));
    }

    private static boolean has(ServerPlayer p, Item item, int n) { return p.isCreative() || p.getInventory().countItem(item) >= n; }

    private static void take(ServerPlayer p, Item item, int n) {
        if (!p.isCreative()) p.getInventory().clearOrCountMatchingItems(s -> s.is(item), n, p.inventoryMenu.getCraftSlots());
    }

    // ---------------------------------------------------------------- begin
    /** Starts the ritual that calls this devil, or tells the caller the first thing that is wrong. */
    static DevilRitualRules.Denial begin(ServerPlayer p, WorldDevil d) {
        long now = DevilContracts.time(p);
        DevilContract c = DevilStore.peek(p);
        DevilRitualRules.Denial no = DevilRitualRules.check(d.world(), facts(p, c, now));
        if (no != DevilRitualRules.Denial.NONE) {
            say(p, no.text, ChatFormatting.RED);
            return no;
        }
        c = DevilStore.of(p);
        switch (d.world()) {                                                                 // what the ritual itself costs, paid when it begins (a broken ritual does not give it back)
            case BLACK_CLOVER -> take(p, NUItems.DEVIL_CONTRACT.get(), 1);
            case BLUE_EXORCIST -> {
                take(p, Items.PAPER, 1);
                if (!p.isCreative()) p.hurt(p.damageSources().magic(), DevilRitualRules.EXORCIST_BLOOD);
            }
            case TENSURA -> {
                take(p, Items.ROTTEN_FLESH, DevilRitualRules.OFFERING_FLESH);
                DevilContracts.spendMana(p, maxMagicules(p) * DevilRitualRules.TENSURA_NAMING);
            }
            case FAIRY_TAIL -> {
                take(p, Items.BOOK, 1);
                DevilContracts.spendMana(p, maxMagicules(p) * DevilRitualRules.ZEREF_ETHER);
            }
            default -> { }
        }
        Vec3 look = p.getLookAngle();
        double len = Math.hypot(look.x, look.z);
        double fx = len < 1.0E-4 ? 0 : look.x / len, fz = len < 1.0E-4 ? 1 : look.z / len;
        c.ritual = new DevilRitualRun(d.id(), now, p.getX(), p.getY(), p.getZ(), fx, fz, p.level().dimension().location().toString());
        c.lastRitual = now;
        c.offer = "";
        p.sendSystemMessage(Component.literal(d.world().ritual + " (" + d.world().label + ")").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        stage(p, d, c.ritual, 0);
        return DevilRitualRules.Denial.NONE;
    }

    // ---------------------------------------------------------------- the run
    /** Once a tick for every player; does nothing unless a ritual runs. */
    static void tick(ServerPlayer p) {
        DevilContract c = DevilStore.peek(p);
        if (c == null || c.ritual == null) return;
        DevilRitualRun r = c.ritual;
        WorldDevil d = WorldDevil.byId(r.devil);
        if (d == null || !p.isAlive() || !on() || !r.dimension.equals(p.level().dimension().location().toString())) {
            c.ritual = null;
            return;
        }
        int t = (int) Math.max(0, DevilContracts.time(p) - r.start);
        if (t == 0) return;                                                                  // tick 0 was played by begin
        float max = Math.max(1f, p.getMaxHealth());
        String why = DevilRitualRules.broken(d.world(), Math.sqrt(p.distanceToSqr(r.x, r.y, r.z)), p.getHealth() / max);
        if (why != null) {
            c.ritual = null;
            say(p, why, ChatFormatting.RED);
            if (d.world() == DevilWorld.BLUE_EXORCIST && !p.isCreative()) p.hurt(p.damageSources().magic(), DevilRitualRules.EXORCIST_BACKLASH);
            sound(p, SoundEvents.ELDER_GUARDIAN_CURSE, 1.0f, 0.7f);
            return;
        }
        stage(p, d, r, t);
        if (t >= d.world().ticks) {
            c.ritual = null;
            offer(p, c, d);
        }
    }

    private static void shape(ServerPlayer p, VfxShape s, Vec3 from, Vec3 to, int rgb, int ticks, double power) {
        VfxSpawn.send(p.serverLevel(), s, from, to, 0xFF000000 | (rgb & 0xFFFFFF), ticks, (float) power);
    }

    private static void sound(ServerPlayer p, SoundEvent s, float vol, float pitch) { p.serverLevel().playSound(null, p.blockPosition(), s, SoundSource.PLAYERS, vol, pitch); }

    private static void say(ServerPlayer p, String text, ChatFormatting color) { p.sendSystemMessage(Component.literal(text).withStyle(color)); }

    private static void chant(ServerPlayer p, String text) { p.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true); }

    /** What tick {@code t} of the ritual looks and sounds like, world by world. */
    private static void stage(ServerPlayer p, WorldDevil d, DevilRitualRun r, int t) {
        ServerLevel l = p.serverLevel();
        DevilWorld w = d.world();
        Vec3 a = new Vec3(r.x, r.y + 0.05, r.z), up = a.add(0, 1, 0), f = a.add(r.faceX * 3.0, 0, r.faceZ * 3.0);
        int wc = w.color, dc = d.color();
        if (t % 10 == 5) Fx.spray(l, THEMES[w.ordinal()], w == DevilWorld.BLACK_BUTLER ? FxTone.DARK : FxTone.LIGHT, a.x, a.y + (w == DevilWorld.BLACK_BUTLER ? 3.2 : 1.0), a.z, 6, 1.4, 0.5, 1.4, 0.01);
        switch (w) {
            case BLACK_CLOVER -> {
                if (t == 0) { shape(p, VfxShape.DEVIL_CIRCLE, a, up, wc, 220, 3.5); shape(p, VfxShape.DARKFX_ZONE, a, up, wc, 200, 3.5); sound(p, SoundEvents.WITHER_SPAWN, 0.8f, 0.6f); say(p, "The relic burns to nothing. A devil is called into the circle.", ChatFormatting.GRAY); }
                if (t == 40) chant(p, d.name() + " stands in the circle. It wants your body.");
                if (DevilRitualRules.blowAt(t)) {
                    shape(p, VfxShape.DARKFX_BURST, up, up.add(0, 1, 0), dc, 24, 1.5);
                    float blow = DevilRitualRules.bindingBlow(p.getHealth(), p.getMaxHealth());
                    if (blow > 0f && !p.isCreative()) p.hurt(p.damageSources().magic(), blow);
                    sound(p, SoundEvents.WARDEN_SONIC_BOOM, 0.6f, 0.6f);
                    chant(p, "Stand your ground.");
                }
                if (t == w.ticks) { shape(p, VfxShape.MAGIC_CIRCLE_EXPLOSION, a, up, dc, 30, 3.0); say(p, d.name() + " yields.", ChatFormatting.GRAY); }
            }
            case CHAINSAW_MAN -> {
                if (t == 0) { shape(p, VfxShape.SHADOW_POOL, f, f.add(0, 1, 0), wc, 90, 3.0); chant(p, "You call its name."); sound(p, SoundEvents.WARDEN_HEARTBEAT, 1.2f, 0.7f); }
                if (t == 25) { shape(p, VfxShape.FOOD_MAW, f.add(0, 1.2, 0), up, dc, 50, 2.5); sound(p, SoundEvents.RAVAGER_ROAR, 1.0f, 0.6f); }
                if (t == 55) { shape(p, VfxShape.BLOOD_DOWNPOUR, a, up, wc, 30, 3.0); chant(p, d.name() + " names its price."); }
                if (t == w.ticks) shape(p, VfxShape.BLOOD_SIGIL, a, up, wc, 40, 2.5);
            }
            case BLACK_BUTLER -> {
                if (t == 0) { shape(p, VfxShape.DARKFX_ZONE, a, up, wc, 150, 3.0); chant(p, "In your despair, something listens."); sound(p, SoundEvents.BELL_RESONATE, 1.0f, 0.5f); }
                if (t == 50) chant(p, "A calm voice asks what you would give to have your wish.");
                if (t == 90) { shape(p, VfxShape.ALCHEMY_CIRCLE, f.add(0, 1.5, 0), up.add(0, 0.5, 0), wc, 50, 1.5); chant(p, "The seal is drawn."); sound(p, SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 0.6f); }
                if (t == w.ticks) shape(p, VfxShape.LIGHT_FLARE, a.add(0, 1.6, 0), a.add(0, 1.6, 0), wc, 20, 1.0);
            }
            case BLUE_EXORCIST -> {
                if (t == 0) { shape(p, VfxShape.MAGIC_CIRCLE, a, up, wc, 180, 1.2); say(p, "A drop of your blood falls on the circle.", ChatFormatting.GRAY); sound(p, SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 1.2f); }
                if (t == 40) chant(p, "You speak the words that come to you.");
                if (t == 80) { chant(p, "The circle answers your blood."); VfxSpawn.sendFollowing(l, VfxShape.SPIRIT_AURA, p, up, 0xFF000000 | wc, 60, 1.0f); }
                if (t == 120) chant(p, "Something looks back at you. Do not look away.");
                if (t == w.ticks) { shape(p, VfxShape.FLAMEFX_BURST, f.add(0, 0.5, 0), f.add(0, 1.5, 0), dc, 28, 1.6); sound(p, SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 1.2f); }
            }
            case TENSURA -> {
                if (t == 0) { shape(p, VfxShape.SPACE_RINGGATE, f.add(0, 1.6, 0), up, wc, 200, 2.2); shape(p, VfxShape.SOUL_VORTEX, a, up, wc, 120, 4.0); say(p, "The offering is taken. A gate to the underworld opens.", ChatFormatting.GRAY); sound(p, SoundEvents.PORTAL_TRIGGER, 0.6f, 0.6f); }
                if (t == 60) chant(p, "Something old looks out of the gate. It wants a body.");
                if (t == 110) chant(p, "You give it a name. The magicules pour out of you.");
                if (t == w.ticks) { shape(p, VfxShape.DIVINE_PILLAR, f, f.add(0, 1, 0), dc, 40, 2.0); sound(p, SoundEvents.END_PORTAL_SPAWN, 0.5f, 0.8f); }
            }
            case DORORO -> {
                if (t == 0) { shape(p, VfxShape.BOSS_STONE_RING, a, up, wc, 170, 4.5); chant(p, "The statues of the hall are listening."); sound(p, SoundEvents.BELL_RESONATE, 1.0f, 0.4f); }
                if (t == 60) { shape(p, VfxShape.CURSE_FX1, f.add(0, 1, 0), up, wc, 30, 1.5); chant(p, "You ask the hall for prosperity, whatever it costs."); }
                if (t == 115) { shape(p, VfxShape.LIGHTNING_SPEAR, a.add(0, 14, 0), a, 0xB8E4FF, 12, 1.5); shape(p, VfxShape.BLOOD_ARCS, up, f.add(0, 1, 0), 0x8A0A14, 24, 1.5); sound(p, SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0f, 1.0f); }
                if (t == w.ticks) shape(p, VfxShape.DARKFX_BURST, up, up.add(0, 1, 0), wc, 24, 1.5);
            }
            case BERSERK -> {
                if (t == 0) { shape(p, VfxShape.DARK_BLACK_MOON, a.add(0, 13, 0), a.add(0, 14, 0), wc, 220, 4.0); say(p, "The Behelit weeps blood. The sun goes dark.", ChatFormatting.DARK_RED); sound(p, SoundEvents.WITHER_SPAWN, 0.8f, 0.4f); }
                if (t == 50) { shape(p, VfxShape.NIHILITY_ZONE, a, up, wc, 150, 6.0); chant(p, "Four shapes stand in the dark sun. They ask what you will give."); }
                if (t == 120) { shape(p, VfxShape.BLOOD_DOWNPOUR, a, up, wc, 70, 5.0); sound(p, SoundEvents.WARDEN_HEARTBEAT, 1.5f, 0.5f); }
                if (t == w.ticks) { shape(p, VfxShape.BLOOD_SIGIL, a, up, wc, 50, 3.0); chant(p, "The Brand waits for your word."); }
            }
            case FAIRY_TAIL -> {
                if (t == 0) { shape(p, VfxShape.RUNE_WRITING, a, up, wc, 150, 2.0); chant(p, "The book opens by itself."); sound(p, SoundEvents.BOOK_PAGE_TURN, 1.5f, 0.6f); }
                if (t == 50) { shape(p, VfxShape.KOTO_WORDS, a.add(0, 1.6, 0), f.add(0, 1.6, 0), wc, 60, 1.5); chant(p, "The letters lift off the page."); }
                if (t == 100) shape(p, VfxShape.MAGIC_CIRCLE, f, f.add(0, 1, 0), wc, 50, 2.0);
                if (t == w.ticks) { shape(p, VfxShape.DEMON_FIRE_FX1, a.add(0, 1.2, 0), f.add(0, 1, 0), dc, 30, 1.5); sound(p, SoundEvents.GENERIC_EXPLODE.value(), 0.7f, 0.8f); }
            }
        }
    }

    // ---------------------------------------------------------------- the offer
    private static String priceText(WorldDevil d) {
        int m = d.priceMask();
        if (m == 0) return d.world().price;
        StringBuilder sb = new StringBuilder();
        for (Sacrifice s : Sacrifice.values()) {
            if ((m & s.bit()) == 0) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(switch (s) {
                case HALF_HEALTH -> "half of your maximum health, for good";
                case LOST_EYE_LEFT -> "your left eye";
                case LOST_EYE_RIGHT -> "your right eye";
                case FLESH -> "a piece of your body (two hearts of maximum health)";
                case SOUL_DEBT -> "your soul (every death takes all your experience levels)";
                case BRANDED -> "the Brand (monsters hit you a fifth harder)";
            });
        }
        if (d.world() == DevilWorld.BERSERK) sb.append(", and all your experience levels now");
        if (d.world() == DevilWorld.DORORO) sb.append("; it pays you in emeralds, diamonds and gold");
        return sb.toString();
    }

    private static void offer(ServerPlayer p, DevilContract c, WorldDevil d) {
        c.offer = d.id();
        c.offerAt = DevilContracts.time(p);
        DevilRules.Effect fx = DevilRules.effect(d.attribute());
        p.sendSystemMessage(Component.literal(cap(d.name()) + ", " + d.title() + " (" + d.world().label + "), offers you a contract.").withColor(d.color() | 0x404040));
        say(p, "  Power: a " + d.tier().key() + " devil of " + d.attribute().key().replace('_', ' ') + ". " + fx.blurb(), ChatFormatting.GRAY);
        say(p, "  Price: " + priceText(d), ChatFormatting.GRAY);
        MutableComponent line = Component.literal("  ");
        line.append(Component.literal("[Sign]").withStyle(s -> s.withColor(ChatFormatting.DARK_RED).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/devil sign"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("The price is taken at once and stays through death.")))));
        line.append(Component.literal("   "));
        line.append(Component.literal("[Refuse]").withStyle(s -> s.withColor(ChatFormatting.GRAY).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/devil refuse"))));
        line.append(Component.literal("   (30 seconds)").withStyle(ChatFormatting.DARK_GRAY));
        p.sendSystemMessage(line);
    }

    private static String cap(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    /** The caller signs: the lasting price is taken and the contract written. False (with a line) when there is nothing to sign or it can no longer be signed. */
    static boolean sign(ServerPlayer p) {
        DevilContract c = DevilStore.peek(p);
        long now = DevilContracts.time(p);
        if (!offerOpen(c, now)) { say(p, "No devil is waiting for your answer (or it grew tired of waiting).", ChatFormatting.RED); return false; }
        WorldDevil d = WorldDevil.byId(c.offer);
        c.offer = "";
        if (d == null || !on()) return false;
        if (c.hasContract() || Bonds.refuse(p, Bonds.Kind.DEVIL, "a devil contract")) return false;
        if (d.world() == DevilWorld.BERSERK && !p.isCreative()) {
            if (p.experienceLevel < DevilRitualRules.BERSERK_LEVELS) { say(p, DevilRitualRules.Denial.NO_LEVELS.text, ChatFormatting.RED); return false; }
            p.giveExperienceLevels(-p.experienceLevel);
        }
        if (d.world() == DevilWorld.DORORO) {                                               // the pact was for prosperity: it is paid
            p.getInventory().placeItemBackInInventory(new ItemStack(Items.EMERALD, 32));
            p.getInventory().placeItemBackInInventory(new ItemStack(Items.DIAMOND, 8));
            p.getInventory().placeItemBackInInventory(new ItemStack(Items.GOLD_INGOT, 16));
        }
        c.tier = d.tier();
        c.attribute = d.attribute();
        c.devil = d.id();
        int mask = d.priceMask();
        for (Sacrifice s : Sacrifice.values()) if ((mask & s.bit()) != 0) c.sacrifices.add(s);
        c.resetLive();
        DevilContracts.applySacrifices(p, c);
        DevilContracts.applyStats(p, c);
        DevilContracts.syncNow(p, c);
        Vec3 at = p.position().add(0, 0.05, 0);
        shape(p, VfxShape.DEVIL_CIRCLE, at, at.add(0, 1, 0), d.color(), 40, 2.0);
        sound(p, SoundEvents.WITHER_SPAWN, 0.7f, 1.2f);
        p.sendSystemMessage(Component.literal("The contract is signed. " + cap(d.name()) + " is yours, and you are its.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        say(p, "Let it in with the Devil key (N by default); /devil status shows the contract.", ChatFormatting.GRAY);
        try {
            com.newuniverse.nusmp.quests.NusmpEvents.fire(p, "spirit.contract", "devil");
        } catch (RuntimeException ignored) {
            // a quest hook must never undo a contract
        }
        return true;
    }

    /** The caller refuses: the offer closes; what the ritual cost stays paid. */
    static boolean refuse(ServerPlayer p) {
        DevilContract c = DevilStore.peek(p);
        if (c == null || c.offer.isEmpty()) return false;
        WorldDevil d = WorldDevil.byId(c.offer);
        c.offer = "";
        say(p, (d == null ? "The devil" : cap(d.name())) + " withdraws.", ChatFormatting.GRAY);
        return true;
    }
}
