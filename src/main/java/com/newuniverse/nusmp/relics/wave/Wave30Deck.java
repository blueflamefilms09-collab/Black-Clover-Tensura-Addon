package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.gen.LabyrinthLocate;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.RelicCatalog;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicLoot;
import com.newuniverse.nusmp.relics.RelicSlot;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.UseRules;
import com.newuniverse.nusmp.relics.order.UniqueLedger;
import com.newuniverse.nusmp.relics.pair.PairGrant;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Why: the deck is the wave's most dangerous toy; keeping every card effect in one place makes the fairness rules (no lethal outcome, no theft of
 * relics) easy to audit against the table of cards in the docs.
 * <p>
 * Wave 30: the Deck of Many Things in the game. Three acts say how many cards to draw (one, two, three: you draw exactly the number you chose, never
 * more), a fourth looks at the deck. The drawing itself is {@link Wave30DeckMath}; this class plays each of the twenty-two cards as a bounded game effect:
 * nothing kills outright, nothing takes a relic or a ledger item, nothing locks a player out; the terrible cards are timed debuffs and hostile summons
 * that cannot take the last heart and drop nothing. After the twenty-second card the deck crumbles to dust.
 */
final class Wave30Deck {
    private Wave30Deck() {}

    // ================================================================ the acts
    static void draw(ActCtx c, int wanted) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        ItemStack stack = c.stack();
        long now = level.getGameTime();
        long today = UseRules.day(now);
        long seed = Wave30Data.stackLong(stack, Wave30Data.DECK_SEED);
        if (seed == 0L) {
            seed = Wave30DeckMath.freshSeed(System.nanoTime(), now ^ p.getUUID().getLeastSignificantBits());
            Wave30Data.putStackLong(stack, Wave30Data.DECK_SEED, seed);
        }
        int drawn = Wave30Data.stackInt(stack, Wave30Data.DECK_DRAWN);
        int used = Wave30Data.stackLong(stack, Wave30Data.DECK_DAY) == today ? Wave30Data.stackInt(stack, Wave30Data.DECK_USED) : 0;
        int n = Wave30DeckMath.allowed(wanted, drawn, used);
        if (n <= 0) {
            Wave30Kit.say(p, used >= Wave30DeckMath.MAX_PER_DAY ? "You have drawn your three cards for today; the deck rests until dawn." : "The deck has no cards left.");
            return;
        }
        if (n < wanted) Wave30Kit.say(p, "The deck will give only " + n + " card" + (n == 1 ? "" : "s") + " (three a day, and the cards left): you draw " + n + ".");
        Wave30DeckMath.Draw d = Wave30DeckMath.draw(seed, drawn, n);
        int first = drawn;
        Wave30Data.putStackInt(stack, Wave30Data.DECK_DRAWN, d.drawnAfter());
        Wave30Data.putStackLong(stack, Wave30Data.DECK_DAY, today);
        Wave30Data.putStackInt(stack, Wave30Data.DECK_USED, used + n);
        Fx.ring(level, p.position(), 2.0, 0xE6B94A);
        if (d.extra() > 0) Wave30Kit.say(p, "The cards chain: " + d.extra() + " more come up of themselves.");
        for (int i = 0; i < d.cards().size(); i++) {
            Wave30DeckMath.Card card = d.cards().get(i);
            ChatFormatting col = card.mood == Wave30DeckMath.Mood.GOOD ? ChatFormatting.GREEN : card.mood == Wave30DeckMath.Mood.BAD ? ChatFormatting.RED : ChatFormatting.AQUA;
            Wave30Kit.shout(p, "Card " + (first + i + 1) + " of 22: " + card.title.toUpperCase(java.util.Locale.ROOT), col);
            try {
                play(p, level, card, seed, first + i);
            } catch (RuntimeException | LinkageError failed) {
                org.slf4j.LoggerFactory.getLogger("nusmp").error("[nusmp] deck card {} failed", card, failed);
                Wave30Kit.say(p, "The card flickers and goes out.");
            }
        }
        if (Wave30DeckMath.crumbles(d.drawnAfter())) {
            Wave30Kit.shout(p, "The twenty-second card turns: the Deck of Many Things crumbles to dust in your hands.", ChatFormatting.GOLD);
            RukhFx.burst(level, p.position().add(0, 1, 0), RukhFx.Alignment.DARK, 12, 0.6);
            stack.shrink(1);
        }
    }

    static void peek(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack stack = c.stack();
        long seed = Wave30Data.stackLong(stack, Wave30Data.DECK_SEED);
        int drawn = Wave30Data.stackInt(stack, Wave30Data.DECK_DRAWN);
        long today = UseRules.day(p.level().getGameTime());
        int used = Wave30Data.stackLong(stack, Wave30Data.DECK_DAY) == today ? Wave30Data.stackInt(stack, Wave30Data.DECK_USED) : 0;
        Wave30Kit.say(p, "The deck holds " + Wave30DeckMath.remaining(drawn) + " cards. You may draw " + Math.max(0, Wave30DeckMath.MAX_PER_DAY - used) + " more today.");
        if (seed != 0L && drawn > 0) Wave30Kit.say(p, "Drawn so far: " + String.join(", ", Wave30DeckMath.names(seed, drawn)) + ".");
    }

    // ================================================================ the twenty-two cards
    private static void play(ServerPlayer p, ServerLevel level, Wave30DeckMath.Card card, long seed, int index) {
        RandomSource rng = p.getRandom();
        long now = level.getGameTime();
        switch (card) {
            case BALANCE -> {
                int n = Wave30Kit.cleanse(p);
                Wave30Kit.lift(p, "minecraft:unluck");
                Fx.addEffect(p, "minecraft:resistance", 6000, 0);
                Wave30Kit.say(p, "Your fortunes are balanced: " + n + " ills wash away, and a calm resistance settles for five minutes.");
            }
            case COMET -> {
                Wave30Data.putLong(p, Wave30Data.COMET_UNTIL, now + UseRules.DAY);
                Wave30Kit.say(p, "A comet marks you: slay the next hostile monster alone, within the day, and you gain " + Wave30Tune.COMET_LEVELS + " levels.");
            }
            case DONJON -> {
                Fx.addEffect(p, "minecraft:slowness", Wave30Tune.DONJON_TICKS, 1);
                Fx.addEffect(p, "minecraft:weakness", Wave30Tune.DONJON_TICKS, 0);
                Fx.addEffect(p, "minecraft:mining_fatigue", Wave30Tune.DONJON_TICKS, 0);
                Fx.ring(level, p.position(), 1.6, 0x1A1A24);
                Wave30Kit.say(p, "A sphere of dark glass closes round you: for three minutes you are slowed, weak and unable to dig. You are not harmed, and it will fade.");
            }
            case EURYALE -> {
                Fx.addEffect(p, "minecraft:unluck", Wave30Tune.EURYALE_TICKS, 1);
                Fx.addEffect(p, "minecraft:weakness", Wave30Tune.EURYALE_TICKS, 0);
                Wave30Kit.say(p, "Euryale's curse settles on you for half an hour (bad luck and weakness). Milk, a Balance card or the Fates will lift it.");
            }
            case FATES -> {
                for (String id : new String[]{"minecraft:unluck", "minecraft:weakness", "minecraft:mining_fatigue"}) Wave30Kit.lift(p, id);
                Fx.addEffect(p, "minecraft:luck", Wave30Tune.FATES_TICKS, 1);
                Wave30Kit.say(p, "The Fates unpick your last misfortune and bless you with luck for ten minutes.");
            }
            case FLAMES -> {
                var foe = Wave30Kit.foe(p, "minecraft:wither_skeleton", Wave30Tune.FOE_LIFE_TICKS, "Infernal Envoy", 40, 6);
                Wave30Kit.say(p, foe != null ? "Flames: an infernal envoy rises to settle an old grudge. Beat it or outlast it: it fades in a minute, and cannot take your last heart." : "Flames lick at your boots and find nothing to hold.");
            }
            case FOOL -> {
                p.giveExperienceLevels(-Wave30Tune.FOOL_LEVELS);
                Wave30Kit.say(p, "The Fool: " + Wave30Tune.FOOL_LEVELS + " levels of your learning slip away. The deck draws again.");
            }
            case GEM -> {
                Wave30Kit.give(p, new ItemStack(Items.EMERALD, Wave30Tune.GEM_COUNT));
                Wave30Kit.say(p, "Gem: " + Wave30Tune.GEM_COUNT + " emeralds spill into your hands.");
            }
            case IDIOT -> {
                Fx.addEffect(p, "minecraft:mining_fatigue", Wave30Tune.IDIOT_TICKS, 0);
                Fx.addEffect(p, "minecraft:weakness", Wave30Tune.IDIOT_TICKS, 0);
                Wave30Kit.say(p, "Idiot: your thoughts go thick and slow; ten minutes of fatigue and weakness.");
            }
            case JESTER -> {
                if (Wave30DeckMath.jesterDraws(seed, index)) {
                    Wave30Kit.say(p, "The Jester grins and deals two more cards.");
                } else {
                    p.giveExperienceLevels(Wave30Tune.JESTER_LEVELS);
                    Wave30Kit.say(p, "The Jester laughs: " + Wave30Tune.JESTER_LEVELS + " levels of cheer.");
                }
            }
            case KEY -> key(p, level, rng);
            case KNIGHT -> {
                var knight = Fx.summon(p, "minecraft:iron_golem", Wave30Tune.KNIGHT_LIFE_TICKS);
                Wave30Kit.say(p, knight != null ? "A knight in iron kneels to you and fights at your side for five minutes (the summon limit; the card's day, shortened)." : "A knight bows, but cannot come.");
            }
            case MOON -> moon(p, seed, index, rng);
            case ROGUE -> {
                var foe = Wave30Kit.foe(p, "minecraft:vindicator", Wave30Tune.FOE_LIFE_TICKS, "The Turncoat", 30, 5);
                Wave30Kit.say(p, foe != null ? "Rogue: someone you trusted turns on you. Fight or flee: they leave in a minute, and cannot take your last heart." : "A friend turns away, and nothing more.");
            }
            case RUIN -> ruin(p, rng);
            case SKULL -> {
                var foe = Wave30Kit.foe(p, "minecraft:vex", 1800, "Avatar of Death", 50, 5);
                Wave30Kit.say(p, foe != null ? "Skull: an avatar of death comes for you. Defeat it or flee: it fades in ninety seconds, and cannot take your last heart." : "A skull grins at you and is gone.");
            }
            case STAR -> star(p);
            case SUN -> sun(p, level, rng);
            case TALONS -> talons(p, rng);
            case THRONE -> {
                Fx.addEffect(p, "minecraft:hero_of_the_village", Wave30Tune.THRONE_TICKS, 1);
                Fx.addEffect(p, "minecraft:absorption", Wave30Tune.THRONE_TICKS, 1);
                Wave30Kit.give(p, new ItemStack(Items.GOLD_INGOT, 16));
                Wave30Data.putString(p, Wave30Data.TITLE, "Lord of the Keep");
                Wave30Kit.say(p, "Throne: you are named Lord of a keep. The villagers hail you for a day, and its treasury gives sixteen gold.");
            }
            case VIZIER -> vizier(p, level);
            case VOID -> {
                Fx.addEffect(p, "minecraft:slowness", Wave30Tune.VOID_TICKS, 3);
                Fx.addEffect(p, "minecraft:blindness", Wave30Tune.VOID_TICKS, 0);
                Fx.addEffect(p, "minecraft:resistance", Wave30Tune.VOID_TICKS, 4);
                Fx.ring(level, p.position(), 1.6, 0x0A0A10);
                Wave30Kit.say(p, "The Void takes your soul for two minutes: you stand blind and still, and nothing in the world can harm you.");
            }
        }
    }

    // ---------------------------------------------------------------- card helpers
    private static void key(ServerPlayer p, ServerLevel level, RandomSource rng) {
        List<ItemStack> got = keyWeapon(level, rng);
        if (got.isEmpty()) {
            Wave30Kit.give(p, new ItemStack(Items.DIAMOND, 8));
            Wave30Kit.say(p, "Key: every magic weapon of this grade is already in the world; the key turns instead to eight diamonds.");
            return;
        }
        for (ItemStack s : got) Wave30Kit.give(p, s);
        Wave30Kit.say(p, "Key: a magic weapon appears in your hands (" + got.get(0).getHoverName().getString() + ").");
    }

    /** A relic weapon of grade RARE that the world does not have yet (its ledger entry is stamped); a pair gives both halves; any RARE relic if no weapon is left. */
    static List<ItemStack> keyWeapon(ServerLevel level, RandomSource rng) {
        List<ItemStack> out = new ArrayList<>();
        if (level.getServer() == null) return out;
        Set<String> live = UniqueLedger.liveIds(level.getServer());
        List<RelicDef> pool = new ArrayList<>();
        for (RelicDef d : Relics.all()) {
            if (d.slot() != RelicSlot.MAIN_HAND || d.grade() != Grade.RARE || !d.isUnique() || d.isPairLeft() || live.contains(d.id())) continue;
            if (d.hasPair() && live.contains(d.pairPartnerId())) continue;
            pool.add(d);
        }
        Collections.shuffle(pool, new java.util.Random(rng.nextLong()));
        for (int tries = 0; tries < Math.min(8, pool.size()); tries++) {
            RelicDef d = pool.get(tries);
            if (d.hasPair()) {
                List<ItemStack> pair = PairGrant.issueNewPair(level, d);
                if (!pair.isEmpty()) return pair;
            } else {
                ItemStack s = RelicCatalog.newStack(d);
                if (!s.isEmpty() && UniqueLedger.issue(level, d.id(), s)) {
                    out.add(s);
                    return out;
                }
            }
        }
        ItemStack any = RelicLoot.rollUnissued(level, rng, Grade.RARE, Grade.RARE);
        if (!any.isEmpty()) out.add(any);
        return out;
    }

    private static final String[][] BOONS = {
            {"minecraft:regeneration", "1", "Regeneration"}, {"minecraft:resistance", "1", "Resistance"}, {"minecraft:strength", "1", "Strength"},
            {"minecraft:luck", "2", "Luck"}, {"minecraft:absorption", "3", "Absorption"}, {"minecraft:speed", "1", "Swiftness"}, {"minecraft:haste", "1", "Haste"}};

    private static void moon(ServerPlayer p, long seed, int index, RandomSource rng) {
        int n = Wave30DeckMath.moonBoons(seed, index);
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < BOONS.length; i++) order.add(i);
        Collections.shuffle(order, new java.util.Random(rng.nextLong()));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            String[] b = BOONS[order.get(i)];
            Fx.addEffect(p, b[0], Wave30Tune.MOON_TICKS, Integer.parseInt(b[1]));
            sb.append(i == 0 ? "" : ", ").append(b[2]);
        }
        Wave30Kit.say(p, "Moon: " + n + (n == 1 ? " wish is" : " wishes are") + " granted for ten minutes: " + sb + ".");
    }

    private static void ruin(ServerPlayer p, RandomSource rng) {
        int lost = Wave30Kit.take(p, Items.EMERALD, Wave30Tune.RUIN_MAX);
        Item stone = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("tensura:magic_stone")).orElse(null);
        if (stone != null && lost < Wave30Tune.RUIN_MAX) lost += Wave30Kit.take(p, stone, Wave30Tune.RUIN_MAX - lost);
        Fx.addEffect(p, "minecraft:unluck", Wave30Tune.RUIN_UNLUCK_TICKS, 1);
        Wave30Kit.say(p, "Ruin: your fortune turns to dust: " + lost + " emeralds and magic stones are gone, and poverty dogs you for ten minutes. Your relics and your tools are untouched.");
    }

    private static void star(ServerPlayer p) {
        int stars = Wave30Data.getInt(p, Wave30Data.STARS);
        if (stars >= Wave30Tune.STAR_CAP) {
            p.giveExperienceLevels(3);
            Wave30Kit.say(p, "Star: you shine as brightly as a mortal can; the light becomes 3 levels instead.");
            return;
        }
        Wave30Data.putInt(p, Wave30Data.STARS, stars + 1);
        applyStars(p);
        Wave30Kit.say(p, "Star: a lasting gift: +1 heart of maximum health and +1 attack damage, for good (" + (stars + 1) + " of " + Wave30Tune.STAR_CAP + ").");
    }

    /** The Star's permanent modifiers from the player's own counter (re-applied after a death or a relog, which do not carry modifiers over). */
    static void applyStars(ServerPlayer p) {
        int stars = Math.min(Wave30Tune.STAR_CAP, Wave30Data.getInt(p, Wave30Data.STARS));
        Wave30Kit.setPermanent(p, Wave30Tune.MC_MAX_HEALTH, "star_health", stars * Wave30Tune.STAR_HEALTH, AttributeModifier.Operation.ADD_VALUE);
        Wave30Kit.setPermanent(p, Wave30Tune.MC_ATTACK, "star_attack", stars * Wave30Tune.STAR_ATTACK, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void sun(ServerPlayer p, ServerLevel level, RandomSource rng) {
        ItemStack relic = RelicLoot.rollUnissued(level, rng, Grade.UNIQUE, Grade.UNIQUE);
        p.giveExperienceLevels(Wave30Tune.SUN_LEVELS);
        if (relic.isEmpty()) {
            Wave30Kit.give(p, new ItemStack(Items.DIAMOND, 16));
            Wave30Kit.say(p, "Sun: every relic of this grade is already in the world; the light gives " + Wave30Tune.SUN_LEVELS + " levels and sixteen diamonds instead.");
        } else {
            Wave30Kit.give(p, relic);
            Wave30Kit.say(p, "Sun: a blaze of gold, " + Wave30Tune.SUN_LEVELS + " levels, and a relic: " + relic.getHoverName().getString() + ".");
        }
    }

    private static final Item[] TALON_FOOD = {Items.EMERALD, Items.DIAMOND, Items.GOLD_INGOT, Items.IRON_INGOT, Items.GOLDEN_APPLE, Items.ENDER_PEARL,
            Items.EXPERIENCE_BOTTLE, Items.GOLDEN_CARROT, Items.COOKED_BEEF, Items.COOKED_PORKCHOP};

    private static void talons(ServerPlayer p, RandomSource rng) {
        var inv = p.getInventory();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || Relics.defOf(s) != null || s.isEnchanted() || s.isDamaged()) continue;
            for (Item it : TALON_FOOD) if (s.is(it)) { slots.add(i); break; }
        }
        if (slots.isEmpty()) {
            Wave30Kit.say(p, "Talons: the claws rake your pack and find nothing worth taking.");
            return;
        }
        ItemStack s = inv.getItem(slots.get(rng.nextInt(slots.size())));
        int n = Math.min(Wave30Tune.TALONS_MAX, Math.max(1, s.getCount() / 2));
        String name = s.getHoverName().getString();
        s.shrink(n);
        Wave30Kit.say(p, "Talons: claws from nowhere tear " + n + " of your " + name + " to shreds. (Only plain valuables and food: never a relic, a tool or enchanted gear.)");
    }

    private static void vizier(ServerPlayer p, ServerLevel level) {
        try {
            BlockPos here = p.blockPosition();
            if (Labyrinth.in(level)) {
                StructureStart start = LabyrinthLocate.nearestStart(level, Labyrinth.SPIRE_TAG, here);
                BlockPos pad = LabyrinthLocate.arrivalPadOf(start);
                if (pad == null) {
                    Wave30Kit.say(p, "Vizier: the sage looks long over the Labyrinth and names no Spire within " + LabyrinthLocate.LOCATE_RADIUS_CHUNKS + " chunks.");
                    return;
                }
                BoundingBox box = start.getBoundingBox();
                Wave30Kit.say(p, "Vizier: the sage answers: the nearest Spire's arrival pad is at " + pad.getX() + ", " + pad.getY() + ", " + pad.getZ()
                        + " (" + (int) Math.sqrt(pad.distSqr(here)) + " blocks away; it spans y " + box.minY() + " to " + box.maxY() + ").");
            } else if (level.dimension() == Level.OVERWORLD) {
                BlockPos gate = LabyrinthLocate.nearestGate(level, here);
                Wave30Kit.say(p, gate == null ? "Vizier: the sage names no Dungeon Gate within " + LabyrinthLocate.LOCATE_RADIUS_CHUNKS + " chunks."
                        : "Vizier: the sage answers: the nearest Dungeon Gate stands at " + gate.getX() + ", " + gate.getY() + ", " + gate.getZ() + " (" + (int) Math.sqrt(gate.distSqr(here)) + " blocks away).");
            } else {
                Wave30Kit.say(p, "Vizier: Gates stand in the Overworld and Spires in the Labyrinth; the sage has nothing to tell you here.");
            }
        } catch (RuntimeException | LinkageError failed) {
            Wave30Kit.say(p, "Vizier: the sage's answer is lost in the static.");
        }
    }
}
