package com.newuniverse.nusmp.arc;

import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Why: the Zogratis framework (moves with telegraphs, the percentage power curve, the arena that resets when the party is gone, the per-second damage
 * budget, the boss bar, the second health bar, the Charter drop) is already the tested way this mod makes a boss, and the Goliath, the Charybdis spawn
 * and the Devil Avatar all stand on it. A character of the arcs is the same kind of fight with a different face, so this class is only a thin layer on
 * top: it answers the framework's questions (name, colours, lines) from the character's {@link ArcCharacter} record instead of from a devil, and tells
 * the story that a character fell. A concrete boss supplies {@link #character()}, {@link #moves()} (inherited, abstract) and its attributes
 * (see {@link ZogratisBoss#bossAttributes}); it is registered with {@link ArcEntities#boss}.
 */
public abstract class ArcBoss extends ZogratisBoss {
    /** Listeners told when an arc boss was beaten (the story unlock, quests); the party is {@link #fighters()}. Registered once at boot, read on the server thread. */
    public static final List<Consumer<ArcBoss>> ON_DEFEATED = new CopyOnWriteArrayList<>();

    protected ArcBoss(EntityType<? extends ArcBoss> type, Level level) {
        super(type, level);
    }

    /** Who this boss is. A constant of the concrete class ({@code public static final ArcCharacter CHARACTER}), returned here. */
    public abstract ArcCharacter character();

    @Override public String bossName() { return character().name(); }

    /** Never null: neutral ({@link Devil#LILITH}) unless the character is bound to a devil. The framework's own lines that name the devil are all overridden below. */
    @Override public Devil devil() { return character().devil(); }

    @Override public int styleColor() { return character().color(); }

    @Override public int styleAccent() { return character().accent(); }

    /** The bar reads the character's name and strength; the framework's version also names a devil, which means nothing for a Magic Knight or a monster of the Dungeon. */
    @Override public String barTitle() {
        return bossName() + " - " + power() + "%" + (gatesOpen() ? " - Gates open" : "");
    }

    @Override protected String openingLine() {
        String line = character().opening();
        return line.isEmpty() ? character().fullName() + " stands before you." : line;
    }

    @Override protected String powerLine(int old, int now) { return character().name() + " draws " + now + "% of their strength."; }

    @Override protected String gatesLine() {
        String line = character().gates();
        return line.isEmpty() ? character().name() + " shows their true strength!" : line;
    }

    @Override protected String deathLine() {
        String line = character().death();
        return line.isEmpty() ? "..." : line;
    }

    /** The boss bar colour of the character's palette: the nearest of the vanilla bar colours to its main colour. */
    @Override protected BossEvent.BossBarColor barColor() { return nearestBar(character().color()); }

    @Override protected void onFightEnd(boolean won) {
        super.onFightEnd(won);
        if (!won) return;
        for (Consumer<ArcBoss> l : ON_DEFEATED) {
            try {
                l.accept(this);
            } catch (RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().error("[nusmp] an arc defeat listener failed for {}: {}", character().id(), e.toString());
            }
        }
    }

    /** The vanilla bar colour closest to an RGB colour (pure arithmetic on the hue, saturation and value). */
    public static BossEvent.BossBarColor nearestBar(int rgb) {
        float r = (rgb >> 16 & 255) / 255f, g = (rgb >> 8 & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        if (max < 0.25f || d < 0.12f) return max > 0.7f ? BossEvent.BossBarColor.WHITE : BossEvent.BossBarColor.PURPLE;
        float h;
        if (max == r) h = ((g - b) / d + 6f) % 6f;
        else if (max == g) h = (b - r) / d + 2f;
        else h = (r - g) / d + 4f;
        h *= 60f;
        if (h < 20f || h >= 340f) return BossEvent.BossBarColor.RED;
        if (h < 70f) return BossEvent.BossBarColor.YELLOW;
        if (h < 170f) return BossEvent.BossBarColor.GREEN;
        if (h < 260f) return BossEvent.BossBarColor.BLUE;
        if (h < 300f) return BossEvent.BossBarColor.PURPLE;
        return BossEvent.BossBarColor.PINK;
    }
}
