package com.newuniverse.nusmp.ultimate;

import java.util.List;

/**
 * Why: the data of ONE Ultimate Magic (docs/magic/ultimate_magic.md), a plain record with no Minecraft types, so the registry ({@link UltimateRegistry}), the tests and the CI self-test read the same table the game
 * uses. Everything the game does for a cast comes from this record and the bounds in {@link UltimateRules}; adding an attribute's Ultimate is one more row in the registry plus one lang line.
 *
 * @param id            the page id (lower snake_case, "ultimate_" + the element), a save key: never renamed
 * @param bookPath      the registry path of the grimoire this page is appended to (e.g. {@code book_fire})
 * @param magic         the {@code MagicType} name of that book (for the tint and the particle)
 * @param name          what the player reads (the canon name when there is one)
 * @param element       the element tag ("fire", "water", ...), also the damage flavour in the docs
 * @param origin        {@code original} or {@code canon: <source>}, the mark of every page of this mod's page docs
 * @param windupTicks   the length of the wind-up before the release (clamped to 100..400 by {@link UltimateRules#windupTicks})
 * @param radius        blocks around the caster that the release reaches (6..24)
 * @param damage        the raw damage of the first pulse's full share (10..80), handed to the grimoire's balance law per target
 * @param debuffs       at most four potion effects every target gets (the duration is halved on a boss)
 * @param igniteSeconds fire for the targets (0 = none, at most 15)
 * @param freezeTicks   freezing for the targets (0 = none, at most 300)
 * @param push          knockback away from the centre (negative = pulled in), at most 2.5 either way
 * @param lift          upward kick on the targets (0..1)
 * @param chargePower   the VfxShape power of the wind-up picture (for the ground sigils this is their radius in blocks)
 * @param burstPower    the VfxShape power of the release picture
 * @param echoPower     the VfxShape power of the two lingering pulses
 * @param chargeShape   the VfxShape that follows the caster during the wind-up (re-sent every 40 ticks, at most 6 times)
 * @param burstShape    the VfxShape of the release at the centre
 * @param echoShape     the VfxShape of the two lingering pulses
 */
public record UltimateDef(String id, String bookPath, String magic, String name, String element, String origin, int windupTicks, double radius, float damage,
                          List<Debuff> debuffs, int igniteSeconds, int freezeTicks, double push, double lift, float chargePower, float burstPower, float echoPower, String chargeShape, String burstShape, String echoShape) {

    /** One potion effect of the release: the registry id of the effect (e.g. {@code minecraft:slowness}), the ticks and the amplifier. */
    public record Debuff(String effect, int ticks, int amplifier) {}

    public boolean canon() { return origin != null && origin.startsWith("canon:"); }

    /** The lang key of the page's description (the same key the grimoire's "new page" message uses). */
    public String langKey() { return "nusmp.page." + bookPath + "." + id; }

    /** The first problem with this row, or null when it is fine (used by the tests and the self-test). */
    public String problem() {
        if (id == null || !id.matches("ultimate_[a-z]+")) return "bad id " + id;
        if (bookPath == null || !bookPath.matches("book_[a-z_]+")) return id + ": bad book " + bookPath;
        if (name == null || name.length() < 8 || name.length() > 64 || !name.equals(name.strip())) return id + ": bad name";
        if (origin == null || !(origin.equals("original") || origin.matches("canon: .{3,}"))) return id + ": the origin is 'original' or 'canon: <source>'";
        if (windupTicks < UltimateRules.MIN_WINDUP_TICKS || windupTicks > UltimateRules.MAX_WINDUP_TICKS) return id + ": windup out of bounds";
        if (radius < UltimateRules.MIN_RADIUS || radius > UltimateRules.MAX_RADIUS) return id + ": radius out of bounds";
        if (damage < UltimateRules.MIN_RAW || damage > UltimateRules.MAX_RAW) return id + ": damage out of bounds";
        if (debuffs == null || debuffs.isEmpty() || debuffs.size() > UltimateRules.MAX_DEBUFFS) return id + ": 1 to " + UltimateRules.MAX_DEBUFFS + " debuffs";
        for (Debuff d : debuffs) {
            if (d.effect() == null || !d.effect().matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) return id + ": bad effect id " + d.effect();
            if (d.ticks() <= 0 || d.ticks() > UltimateRules.MAX_DEBUFF_TICKS) return id + ": debuff ticks out of bounds";
            if (d.amplifier() < 0 || d.amplifier() > UltimateRules.MAX_DEBUFF_AMPLIFIER) return id + ": debuff amplifier out of bounds";
        }
        if (igniteSeconds < 0 || igniteSeconds > UltimateRules.MAX_IGNITE_SECONDS) return id + ": ignite out of bounds";
        if (freezeTicks < 0 || freezeTicks > UltimateRules.MAX_FREEZE_TICKS) return id + ": freeze out of bounds";
        if (Math.abs(push) > UltimateRules.MAX_PUSH) return id + ": push out of bounds";
        if (lift < 0 || lift > UltimateRules.MAX_LIFT) return id + ": lift out of bounds";
        for (float pw : new float[]{chargePower, burstPower, echoPower}) if (pw < 0.5f || pw > 24f) return id + ": a picture power is out of bounds";
        if (chargeShape == null || burstShape == null || echoShape == null) return id + ": a shape is missing";
        return null;
    }
}
