package com.newuniverse.nusmp.companion.bond;

import com.newuniverse.nusmp.arc.ArcNpcEntity;
import com.newuniverse.nusmp.avatar.FtbTeamHelper;
import com.newuniverse.nusmp.companion.CompanionConfig;
import com.newuniverse.nusmp.companion.CompanionRules;
import com.newuniverse.nusmp.companion.talk.TalkRules;
import com.newuniverse.nusmp.companion.talk.TalkView;
import com.newuniverse.nusmp.companion.voice.VoiceLines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import com.newuniverse.nusmp.companion.talk.Talkable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the shared bond system, the same for every companion: it adds the shared choices to a talk screen ({@link #options}), answers them ({@link #choose}: a gift, the high five, a stance, the pouch, what she
 * likes), looks after the player from the pouch ({@link #tick}) and decides who a companion may attack ({@link #foe}, {@link #wants}). A companion only states who she is (the {@link Companion} methods) and her lines;
 * the numbers are {@link CompanionRules}. Every id it handles begins {@code k:}. Server thread only; nothing is kept in memory (rapport and the daily counts live in the player's data, {@link CompanionData}; the
 * stance, pouch and switch live in her {@link PersonState}).
 */
public final class Bond {
    private Bond() {}

    private static final String K = "nusmp.companion.bond.";
    public static final String FIVE = "k:five", GIFT = "k:gift", LIKES = "k:likes", STANCE = "k:stance:", PUT = "k:put", TAKE = "k:take:", TAKE_ALL = "k:takeall", USE_ON = "k:use:on", USE_OFF = "k:use:off";

    // ================================================================ the screen
    /** Adds the shared choices to the view being built: the high five (Interact), gifts, what she likes and stances (Bond), the pouch (Pouch), and the status the Bond and Pouch tabs draw. */
    public static void options(TalkView.Builder b, Companion c, ServerPlayer p) {
        b.addSystem(c.socialKind() != Companion.NONE
                ? TalkView.Option.blocked(FIVE, Component.translatable(K + "five"), TalkView.INTERACT, Component.translatable(K + "busy"))
                : TalkView.Option.of(FIVE, Component.translatable(K + "five"), TalkView.INTERACT));
        if (!c.customGifts()) {
            b.addSystem(held(p).isEmpty()
                    ? TalkView.Option.blocked(GIFT, Component.translatable(K + "gift"), TalkView.BOND, Component.translatable(K + "gift.empty"))
                    : TalkView.Option.of(GIFT, Component.translatable(K + "gift"), TalkView.BOND));
            b.addSystem(TalkView.Option.of(LIKES, Component.translatable(K + "likes"), TalkView.BOND));
        }
        boolean stances = CompanionRules.stanceOffered(c.fighter(), c.bondOwner(), p.getUUID());
        if (stances) for (Stance s : Stance.values()) b.addSystem(TalkView.Option.of(STANCE + s.id(), Component.translatable(K + "stance." + s.id()), TalkView.BOND));
        List<ItemStack> pouch = List.of();
        int useIt = TalkView.NONE;
        if (c.canCarry()) {
            PersonState st = c.bond();
            if (pouchOpen(c, p)) {
                b.addSystem(held(p).isEmpty()
                        ? TalkView.Option.blocked(PUT, Component.translatable(K + "put"), TalkView.POUCH, Component.translatable(K + "gift.empty"))
                        : TalkView.Option.of(PUT, Component.translatable(K + "put"), TalkView.POUCH));
                for (int i = 0; i < st.pouch.size(); i++) {
                    ItemStack s = st.pouch.get(i);
                    if (!s.isEmpty()) b.addSystem(TalkView.Option.of(TAKE + i, Component.translatable(K + "take", s.getHoverName(), s.getCount()), TalkView.POUCH));
                }
                if (!st.isEmpty()) b.addSystem(TalkView.Option.of(TAKE_ALL, Component.translatable(K + "takeall"), TalkView.POUCH));
                b.addSystem(TalkView.Option.of(st.useIt ? USE_OFF : USE_ON, Component.translatable(K + (st.useIt ? "use.off" : "use.on")), TalkView.POUCH));
                useIt = st.useIt ? 1 : 0;
                pouch = new ArrayList<>(st.pouch);
            } else {
                b.addSystem(TalkView.Option.blocked(PUT, Component.translatable(K + "put"), TalkView.POUCH, Component.translatable(K + "pouch.locked")));
            }
        }
        b.status(c.bondRapport(p), stances ? c.bond().stance.ordinal() : TalkView.NONE, useIt, pouch);
    }

    // ================================================================ the answers
    /**
     * Handles an id of the bond system. Returns the line she says (to put on the next view), or null when the id is not a {@code k:} id (it is the companion's own). An unknown {@code k:} id is answered with
     * a neutral line and changes nothing.
     */
    public static Component choose(Companion c, ServerPlayer p, String id) {
        if (id == null || !id.startsWith(TalkRules.BOND_PREFIX)) return null;
        Component line = switch (id) {
            case FIVE -> five(c, p);
            case GIFT -> c.customGifts() ? null : gift(c, p);
            case LIKES -> c.customGifts() ? null : likes(c, p);
            case PUT -> put(c, p);
            case TAKE_ALL -> takeAll(c, p);
            case USE_ON -> setUse(c, p, true);
            case USE_OFF -> setUse(c, p, false);
            default -> id.startsWith(STANCE) ? stance(c, p, id.substring(STANCE.length())) : id.startsWith(TAKE) ? take(c, p, id.substring(TAKE.length())) : null;
        };
        if (line == null) return Component.translatable(K + "nothing");
        speak(c, line);
        return line;
    }

    private static void speak(Companion c, Component line) {
        c.bondSpeak(line);
        if (c instanceof Entity e) VoiceLines.play(e.level(), e.position().add(0, 1.0, 0), line, SoundSource.NEUTRAL);
    }

    private static ItemStack held(Player p) {
        ItemStack main = p.getMainHandItem();
        return main.isEmpty() ? p.getOffhandItem() : main;
    }

    private static boolean pouchOpen(Companion c, ServerPlayer p) {
        return c.canCarry() && CompanionRules.pouchOpen(c.bondOwner(), p.getUUID(), c.bondTier(p));
    }

    private static Component five(Companion c, ServerPlayer p) {
        if (c.socialKind() != Companion.NONE) return Component.translatable(K + "busy");
        long today = CompanionRules.day(p.level().getGameTime());
        if (CompanionRules.fiveEarns(CompanionData.lastFiveDay(p, c.bondKey()), today)) {
            CompanionData.setFiveDay(p, c.bondKey(), today);
            c.bondAddRapport(p, CompanionRules.HIGH_FIVE);
        }
        c.social(Companion.FIVE, CompanionRules.FIVE_TICKS);
        return Component.translatable(c.langPrefix() + "act.five");
    }

    private static Component gift(Companion c, ServerPlayer p) {
        ItemStack held = held(p);
        if (held.isEmpty()) return Component.translatable(K + "gift.empty");
        Component item = held.getHoverName();
        CompanionRules.Gift g = Favourites.classify(c.bondKey(), held, held.has(DataComponents.FOOD));
        if (!g.taken()) return Component.translatable(c.langPrefix() + "gift.no", item);
        long today = CompanionRules.day(p.level().getGameTime());
        int lovedToday = CompanionData.lovedToday(p, c.bondKey(), today);
        if (g == CompanionRules.Gift.LOVED) CompanionData.setLovedToday(p, c.bondKey(), today, lovedToday + 1);
        int delta = CompanionRules.giftRapport(g, lovedToday);
        if (!p.getAbilities().instabuild) held.shrink(1);
        c.bondAddRapport(p, delta);
        c.bondFeel(g != CompanionRules.Gift.HATED, 60);
        String key = g == CompanionRules.Gift.LOVED ? "gift.love" : g == CompanionRules.Gift.HATED ? "gift.hate" : "gift.ok";
        return Component.translatable(c.langPrefix() + key, item);
    }

    private static Component likes(Companion c, ServerPlayer p) {
        Favourites.Taste t = Favourites.of(c.bondKey());
        if (t.loves().isEmpty() && t.hates().isEmpty()) return Component.translatable(K + "likes.none");
        MutableComponent out = Component.empty();
        if (!t.loves().isEmpty()) out.append(Component.translatable(c.langPrefix() + "likes.love", names(t.loves().stream().limit(4).toList())));
        if (!t.hates().isEmpty() && c.bondTier(p) >= CompanionRules.FRIEND) {
            if (!out.getSiblings().isEmpty()) out.append(Component.literal(" "));
            out.append(Component.translatable(c.langPrefix() + "likes.hate", names(t.hates().stream().limit(4).toList())));
        }
        return out;
    }

    private static Component names(List<Item> items) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) out.append(Component.literal(", "));
            out.append(new ItemStack(items.get(i)).getHoverName());
        }
        return out;
    }

    private static Component stance(Companion c, ServerPlayer p, String name) {
        if (!CompanionRules.stanceOffered(c.fighter(), c.bondOwner(), p.getUUID())) return Component.translatable(K + "nothing");
        Stance s = null;
        for (Stance x : Stance.values()) if (x.id().equals(name)) s = x;
        if (s == null) return null;
        c.bond().stance = s;
        return Component.translatable(c.langPrefix() + "stance." + s.id());
    }

    private static Component put(Companion c, ServerPlayer p) {
        if (!pouchOpen(c, p)) return Component.translatable(K + "pouch.locked");
        ItemStack held = held(p);
        if (held.isEmpty()) return Component.translatable(K + "gift.empty");
        Component name = held.getHoverName();
        int moved = c.bond().put(held);
        return moved <= 0 ? Component.translatable(K + "pouch.full") : Component.translatable(K + "pouch.put", name, moved);
    }

    private static Component take(Companion c, ServerPlayer p, String slot) {
        if (!pouchOpen(c, p)) return Component.translatable(K + "pouch.locked");
        int i;
        try {
            i = Integer.parseInt(slot);
        } catch (NumberFormatException bad) {
            return null;
        }
        ItemStack s = c.bond().take(i);
        if (s.isEmpty()) return Component.translatable(K + "pouch.empty");
        Component name = s.getHoverName();
        int n = s.getCount();
        give(p, s);
        return Component.translatable(K + "pouch.took", name, n);
    }

    private static Component takeAll(Companion c, ServerPlayer p) {
        if (!pouchOpen(c, p)) return Component.translatable(K + "pouch.locked");
        PersonState st = c.bond();
        if (st.isEmpty()) return Component.translatable(K + "pouch.empty");
        for (int i = 0; i < st.pouch.size(); i++) {
            ItemStack s = st.take(i);
            if (!s.isEmpty()) give(p, s);
        }
        return Component.translatable(K + "pouch.tookall");
    }

    private static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s) && !s.isEmpty()) p.drop(s, false);
    }

    private static Component setUse(Companion c, ServerPlayer p, boolean on) {
        if (!pouchOpen(c, p)) return Component.translatable(K + "pouch.locked");
        c.bond().useIt = on;
        return Component.translatable(K + (on ? "use.set.on" : "use.set.off"));
    }

    // ================================================================ the pouch looking after the player
    /**
     * Call every tick from the entity (server side); it works once a second. When the player she walks with is near and their health is below two fifths, she uses a healing potion, else the most nourishing food,
     * from her pouch on them, at most every 30 seconds (the player can switch it off; the server setting {@code companions.pouchUse} too). A potion's bottle is not given back.
     */
    public static void tick(Companion c) {
        if (!(c instanceof LivingEntity self) || !(self.level() instanceof ServerLevel level) || self.tickCount % 20 != 7) return;
        UUID ownerId = c.bondOwner();
        if (ownerId == null || !CompanionConfig.pouchUse() || !c.canCarry()) return;
        if (!(level.getPlayerByUUID(ownerId) instanceof ServerPlayer owner) || !owner.isAlive() || owner.isSpectator() || owner.distanceToSqr(self) > 12.0 * 12.0) return;
        PersonState st = c.bond();
        long now = level.getGameTime();
        if (!CompanionRules.usePouchNow(true, st.useIt, owner.getHealth(), owner.getMaxHealth(), now, st.lastUse)) return;
        int slot = bestSlot(st);
        if (slot < 0) return;
        ItemStack s = st.pouch.get(slot);
        Component item = s.getHoverName();
        applyTo(owner, self, s);
        s.shrink(1);
        if (s.isEmpty()) st.pouch.set(slot, ItemStack.EMPTY);
        st.lastUse = now;
        Component line = Component.translatable(c.langPrefix() + "use", item);
        owner.displayClientMessage(line, true);
        speak(c, line);
    }

    /** What a stack is worth to the pouch's heal rule: {kind, value} (the client's heal-ready ring asks the same question). */
    public static int[] kindAndValue(ItemStack s) {
        if (s.isEmpty()) return new int[]{CompanionRules.NOT_USEFUL, 0};
        if (s.is(Items.POTION)) {
            PotionContents pc = s.get(DataComponents.POTION_CONTENTS);
            if (pc != null) for (MobEffectInstance e : pc.getAllEffects()) {
                if (e.getEffect().value() == MobEffects.HEAL.value() || e.getEffect().value() == MobEffects.REGENERATION.value()) return new int[]{CompanionRules.POTION_KIND, e.getAmplifier() + 1};
            }
            return new int[]{CompanionRules.NOT_USEFUL, 0};
        }
        FoodProperties f = s.get(DataComponents.FOOD);
        return f == null ? new int[]{CompanionRules.NOT_USEFUL, 0} : new int[]{CompanionRules.FOOD_KIND, f.nutrition()};
    }

    /** The pouch slot she would use, or -1 (a potion of healing first, else the most nourishing food). */
    public static int bestSlot(PersonState st) {
        int n = st.pouch.size();
        int[] kinds = new int[n], values = new int[n];
        for (int i = 0; i < n; i++) {
            int[] kv = kindAndValue(st.pouch.get(i));
            kinds[i] = kv[0];
            values[i] = kv[1];
        }
        return CompanionRules.bestHeal(kinds, values);
    }

    private static void applyTo(ServerPlayer owner, LivingEntity giver, ItemStack s) {
        if (s.is(Items.POTION)) {
            PotionContents pc = s.get(DataComponents.POTION_CONTENTS);
            if (pc == null) return;
            for (MobEffectInstance e : pc.getAllEffects()) {
                if (e.getEffect().value().isInstantenous()) e.getEffect().value().applyInstantenousEffect(giver, giver, owner, e.getAmplifier(), 1.0);
                else owner.addEffect(new MobEffectInstance(e));
            }
            return;
        }
        FoodProperties f = s.get(DataComponents.FOOD);
        if (f == null) return;
        owner.getFoodData().eat(f);
        owner.heal(f.nutrition());
    }

    // ================================================================ who she may attack
    private record Facts(boolean foe, boolean hostile, boolean involved) {}

    private static Facts facts(Entity self, Player owner, LivingEntity cand) {
        if (cand == null || cand == self || cand == owner) return new Facts(false, false, false);
        boolean player = cand instanceof Player;
        boolean person = cand instanceof AbstractVillager || cand instanceof ArcNpcEntity;
        boolean companion = cand instanceof Companion || cand instanceof Talkable;
        boolean pet = false, ally = false;
        if (owner != null) {
            if (cand instanceof OwnableEntity o) {
                UUID ou = o.getOwnerUUID();
                if (ou != null) {
                    pet = ou.equals(owner.getUUID());
                    ally = !pet && FtbTeamHelper.sameTeam(owner.getUUID(), ou);
                }
            }
            ally |= cand.isAlliedTo(owner);
        }
        if (self != null) ally |= cand.isAlliedTo(self);
        boolean hostile = cand instanceof Enemy || (owner != null && cand instanceof Mob m && m.getTarget() == owner);
        boolean involved = owner != null && ((owner.getLastHurtByMob() == cand && FoeRules.recent(owner.getLastHurtByMobTimestamp(), owner.tickCount))
                || (owner.getLastHurtMob() == cand && FoeRules.recent(owner.getLastHurtMobTimestamp(), owner.tickCount)));
        return new Facts(FoeRules.isFoe(cand.isAlive(), player, person, companion, pet, ally, hostile, involved), hostile, involved);
    }

    /**
     * May this companion (any entity) attack the candidate? A foe is something hostile, or whoever the player struck or was struck by a moment ago; NEVER a player, a person (a villager, a character of the arcs),
     * another companion or anything that can be talked to, a pet of the same player, or an ally (a squadmate's pet, a scoreboard teammate). {@code owner} may be null (a companion that walks with nobody).
     */
    public static boolean foe(Entity self, Player owner, LivingEntity candidate) { return facts(self, owner, candidate).foe(); }

    /** The stance-aware version of {@link #foe}: should this companion go for the candidate now, in her current stance? */
    public static boolean wants(Companion c, Entity self, Player owner, LivingEntity candidate) {
        Facts f = facts(self, owner, candidate);
        if (!f.foe() || candidate == null || self == null) return false;
        double toSelf = candidate.distanceToSqr(self);
        double toOwner = owner == null ? toSelf : candidate.distanceToSqr(owner);
        return StanceRules.wants(c.bond().stance, true, f.hostile(), f.involved(), toOwner, toSelf);
    }
}
