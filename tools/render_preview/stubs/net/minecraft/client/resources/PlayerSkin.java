package net.minecraft.client.resources;

/** Preview stub of PlayerSkin: only the arm model is needed (wide or slim arms). */
public class PlayerSkin {
    public enum Model { SLIM, WIDE }
    private final Model model;
    public PlayerSkin(Model model) { this.model = model; }
    public Model model() { return model; }
}
