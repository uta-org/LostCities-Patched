package mcjty.lostcities.cubic.world;

import mcjty.lostcities.api.IChunkPrimerFactory;
import mcjty.lostcities.api.ILostChunkGenerator;
import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.dimensions.world.ChunkHeightmap;
import mcjty.lostcities.dimensions.world.lost.cityassets.WorldStyle;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public interface ICommonGeneratorProvider extends ILostChunkGenerator {
    IChunkPrimerFactory otherGenerator = null;

    int getDimensionId();

    // @Nonnull
    WorldStyle getWorldStyle();

    long getSeed();

    LostCityProfile getProfile();

    LostCityProfile getOutsideProfile();

    World getWorld();

    boolean hasMansion(int chunkX, int chunkZ);

    boolean hasOceanMonument(int chunkX, int chunkZ);

    ICommonHeightmap getHeightmap(int chunkX, int chunkZ);
}
