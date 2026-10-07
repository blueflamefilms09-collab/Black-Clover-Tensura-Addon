package net.minecraft.server.packs.resources;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** Preview stub of ResourceManager. */
public interface ResourceManager {
    Optional<Resource> getResource(ResourceLocation location);
}
