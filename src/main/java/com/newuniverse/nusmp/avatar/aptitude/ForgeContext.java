package com.newuniverse.nusmp.avatar.aptitude;

/**
 * Why: everything the server must know about the creator at the moment of a Finalize request, read FRESH from the game (never from the packet): their luck,
 * their magicule and EP reserves, the permanent tax their living Avatars already cost them, how many Genesis Cores they hold, whether they are in creative
 * mode, and the aptitude the server rolled for them. The validator is a pure function of this record, the choices and the rules, which is what lets a test
 * play the part of a hacked client. {@code ritual.ForgeReserves} fills it in the real game; the client fills a look-alike from the state packet to light the
 * Finalize button (the server's answer is the only one that counts).
 *
 * @param luck              the creator's total luck now
 * @param magicule          current magicule
 * @param maxMagicule       maximum magicule (the pool the tax is a share of)
 * @param maxEp             maximum EP (what a skill's EP gate is compared with)
 * @param existingTax       the permanent tax already on the creator, as a fraction of the maximum
 * @param cores             Genesis Cores in the creator's inventory
 * @param creative          creative mode: reserves, EP gates and cores are not charged
 * @param rolledAptitude    the aptitude the server rolled for this ritual, "" when none
 * @param rolledToken       the token of that roll, 0 when none
 * @param raceMagiculeCost  the race's own creation price (charged by the creation code, counted here so the quote shows the whole bill)
 */
public record ForgeContext(double luck, double magicule, double maxMagicule, double maxEp, double existingTax, int cores, boolean creative,
                           String rolledAptitude, long rolledToken, double raceMagiculeCost) {
    public ForgeContext {
        rolledAptitude = rolledAptitude == null ? "" : rolledAptitude;
        existingTax = Math.max(0.0, existingTax);
        maxMagicule = Math.max(0.0, maxMagicule);
        magicule = Math.max(0.0, magicule);
        raceMagiculeCost = Math.max(0.0, raceMagiculeCost);
    }
}
