package com.newuniverse.nusmp.devil;

import com.newuniverse.nusmp.book.ForbiddenMagic;
import com.newuniverse.nusmp.devil.mastery.DevilKit;
import com.newuniverse.nusmp.devil.mastery.DevilScrollRules;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;

/**
 * Why (5.7.0, owner request): a player who gets a devil receives a Scroll of the Devil, a written book that explains how that devil works, what each step of its mastery needs and how mastery grows. The
 * text comes from {@link DevilScrollRules} (pure, tested). It is given once per devil (the ids already given are kept in the player's persistent data), {@code /devil scroll} gives it again, and
 * {@code devil.scroll.enabled = false} stops it. Cost: one book stack with up to 12 short pages, only when a contract is signed or asked for: no tick work, no packet of its own.
 */
public final class DevilScroll {
    private DevilScroll() {}

    static final String TAG = "anime_multiverse_devil_scroll", GIVEN = "anime_multiverse_devil_scrolls";
    private static final String AUTHOR = "The Devil";

    private static DevilScrollRules.Info info(WorldDevil d) {
        DevilRules.Effect fx = DevilRules.effect(d.attribute());
        return new DevilScrollRules.Info(d.name(), d.title(), d.world().label, d.tier().key(), d.attribute().key().replace('_', ' '), fx.blurb(), DevilRituals.priceText(d),
                DevilKit.of(d.id()), DevilConfig.masteryStart(), DevilConfig.masteryRateEffective(), DevilConfig.masteryDailyMinutes());
    }

    private static DevilScrollRules.Info info(ForbiddenMagic.PactDevil d, DevilContract c) {
        return new DevilScrollRules.Info(d.displayName, d.title, "the Forbidden Grimoire", c.tier.key(), c.attribute.key().replace('_', ' '), "", "",
                null, DevilConfig.masteryStart(), DevilConfig.masteryRateEffective(), DevilConfig.masteryDailyMinutes());
    }

    private static ItemStack book(String id, DevilScrollRules.Info i) {
        List<Filterable<Component>> pages = new ArrayList<>();
        for (String page : DevilScrollRules.pages(i)) pages.add(Filterable.passThrough(Component.literal(page)));
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(DevilScrollRules.titleFor(i.name())), AUTHOR, 0, pages, true));
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG, id);
        book.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return book;
    }

    /** A fresh scroll of a world devil. */
    public static ItemStack stack(WorldDevil d) { return book(d.id(), info(d)); }

    private static boolean carries(ServerPlayer p, String id) {
        for (ItemStack s : p.getInventory().items) {
            if (s.isEmpty() || !s.is(Items.WRITTEN_BOOK)) continue;
            CustomData cd = s.get(DataComponents.CUSTOM_DATA);
            if (cd != null && id.equals(cd.copyTag().getString(TAG))) return true;
        }
        return false;
    }

    private static boolean given(ServerPlayer p, String id) { return (";" + p.getPersistentData().getString(GIVEN) + ";").contains(";" + id + ";"); }

    private static void remember(ServerPlayer p, String id) {
        String have = p.getPersistentData().getString(GIVEN);
        String next = have.isEmpty() ? id : have + ";" + id;
        if (next.length() <= 1024) p.getPersistentData().putString(GIVEN, next);                    // 40 devils at most, the cap only guards a damaged tag
    }

    private static void hand(ServerPlayer p, ItemStack scroll) {
        p.getInventory().placeItemBackInInventory(scroll);
        p.sendSystemMessage(Component.literal("A scroll of the devil is yours: read it to learn how it works.").withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC));
    }

    /** A world devil was signed: the scroll is handed over once per devil. */
    public static void give(ServerPlayer p, WorldDevil d) {
        if (!DevilConfig.scrollOn() || given(p, d.id())) return;
        remember(p, d.id());
        hand(p, stack(d));
    }

    /** An old pact devil was bound (the grimoire): the same, with no kit. */
    public static void givePact(ServerPlayer p, DevilContract c) {
        ForbiddenMagic.PactDevil d = ForbiddenMagic.devilOf(p);
        if (!DevilConfig.scrollOn() || d == null || given(p, "pact_" + d.name())) return;
        remember(p, "pact_" + d.name());
        hand(p, book("pact_" + d.name(), info(d, c)));
    }

    /** {@code /devil scroll}: the scroll of the active devil again, unless it is already in the inventory. Returns the message to show (null = given). */
    public static String again(ServerPlayer p) {
        DevilContract c = DevilStore.peek(p);
        if (c == null || !c.hasContract()) return "You have no devil.";
        if (!DevilConfig.scrollOn()) return "The scrolls are switched off on this server.";
        WorldDevil d = WorldDevil.byId(c.devil);
        ForbiddenMagic.PactDevil pd = d == null ? ForbiddenMagic.devilOf(p) : null;
        if (d == null && pd == null) return "Your devil has no scroll.";
        String id = d != null ? d.id() : "pact_" + pd.name();
        if (carries(p, id)) return "You already carry it.";
        hand(p, d != null ? stack(d) : book(id, info(pd, c)));
        return null;
    }

    /** {@code /devil requirements}: the lines of what each mastery step opens, for the active devil. */
    public static String[] requirements(ServerPlayer p) {
        DevilContract c = DevilStore.peek(p);
        WorldDevil d = c == null ? null : WorldDevil.byId(c.devil);
        List<String> out = new ArrayList<>(DevilScrollRules.requirementLines(d == null ? null : DevilKit.of(d.id()), DevilConfig.masteryStart()));
        out.addAll(DevilScrollRules.growthLines(DevilConfig.masteryRateEffective(), DevilConfig.masteryDailyMinutes()));
        return out.toArray(new String[0]);
    }
}
