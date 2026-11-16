package com.newuniverse.nusmp.dragonballs;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** The items of the Dragon Balls: 2 sets x 8 stars (the 8th is only used when {@code dragonBalls.count} = 8) and the Dragon Radar. Always registered up to 8 so a world can switch 7 <-> 8 without losing items. */
public final class DbItems {
    private DbItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NUSMP.MODID);

    @SuppressWarnings("unchecked")
    private static final DeferredItem<DragonBallItem>[][] BALLS = new DeferredItem[BallSet.values().length][DbRules.MAX_COUNT];

    public static final DeferredItem<DragonRadarItem> RADAR = ITEMS.register("dragon_radar", DragonRadarItem::new);

    static {
        for (BallSet s : BallSet.values())
            for (int star = 1; star <= DbRules.MAX_COUNT; star++) {
                final BallSet set = s;
                final int n = star;
                BALLS[s.ordinal()][star - 1] = ITEMS.register(s.itemId(star), () -> new DragonBallItem(set, n));
            }
    }

    /** The item of one ball. */
    public static Item ball(BallSet set, int star) { return BALLS[set.ordinal()][Math.max(1, Math.min(DbRules.MAX_COUNT, star)) - 1].get(); }

    /** The creative tab's entries: both sets up to the configured count, then the radar. */
    public static List<Supplier<? extends Item>> creativeItems() {
        List<Supplier<? extends Item>> out = new ArrayList<>();
        for (BallSet s : BallSet.values()) for (int star = 1; star <= DbRules.MAX_COUNT; star++) out.add(BALLS[s.ordinal()][star - 1]);
        out.add(RADAR);
        return out;
    }
}
