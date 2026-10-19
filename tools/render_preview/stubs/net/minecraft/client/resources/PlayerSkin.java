package net.minecraft.client.resources;

/** Preview stub of PlayerSkin: only the arm model (wide = Steve, slim = Alex) that armour layers read to fit the arms. */
public record PlayerSkin(Model model) {
    public enum Model { SLIM, WIDE }
}
