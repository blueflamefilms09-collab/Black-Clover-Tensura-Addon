package net.minecraft.server.packs.resources;

import net.minecraft.resources.ResourceLocation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Why: harness shim: reads src/main/resources/assets/NAMESPACE/PATH of the repo (set the system property harness.assets). */
public class ResourceManager {
    public static final Path ASSETS = Path.of(System.getProperty("harness.assets", "src/main/resources/assets"));
    public Optional<Resource> getResource(ResourceLocation rl) {
        Path p = ASSETS.resolve(rl.getNamespace()).resolve(rl.getPath());
        return Files.isRegularFile(p) ? Optional.of(new Resource(p)) : Optional.empty();
    }
}
