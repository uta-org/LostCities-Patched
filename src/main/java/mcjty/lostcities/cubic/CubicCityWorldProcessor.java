package mcjty.lostcities.cubic;

import io.github.opencubicchunks.cubicchunks.api.util.Box;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.CubePopulatorEvent;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.ICubicPopulator;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event.PopulateCubeEvent;
import io.github.opencubicchunks.cubicchunks.cubicgen.BasicCubeGenerator;
import io.github.terra121.EarthTerrainProcessor;
import mcjty.lostcities.cubic.world.LostCityCubicGenerator;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class CubicCityWorldProcessor extends EarthTerrainProcessor {

    public static boolean isCubicWorld;
    private static boolean checkedCubicWorld;

    private static World world;
    private static CubePrimer currentPrimer;

    public CubicCityWorldProcessor(World _world) {
        super(_world);
        world = _world;
    }

    @Override
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
        currentPrimer = super.generateCube(cubeX, cubeY, cubeZ);
        return currentPrimer;
    }

    @SubscribeEvent
    public void generate(PopulateCubeEvent event) {
        LostCityCubicGenerator generator = new LostCityCubicGenerator(event);
        generator.spawnInChunk(currentPrimer, event.getCubeX(), event.getCubeY(), event.getCubeZ());
    }

    public static boolean checkForCubicWorld(World world) {
        if(checkedCubicWorld)
            return isCubicWorld;

        checkedCubicWorld = true;

        try {
            ICubicWorld cubicWorld = (ICubicWorld)world;
            isCubicWorld = cubicWorld != null;
        } catch(Exception ex) {
            isCubicWorld = false;
        }

        return isCubicWorld;
    }

    /*
    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        CubicLostCityGenerator generator = new CubicLostCityGenerator();
        generator.spawnInChunk(world, random, chunkX, 0, chunkZ);
    }
    */

    /*
    // Use Deprecated CubePopulatorEvent because Terra121 uses it.
    @SubscribeEvent
    public void generate(CubePopulatorEvent event) {
        World world = event.getWorld();
        CubePos pos = event.getCube().getCoords();
        this.generate(world, world.rand, pos, event.getCube().getBiome(pos.getCenterBlockPos()), event);
    }

    @Override
    public void generate(World world, Random random, CubePos pos, Biome biome) {
        this.generate(world, random, pos, biome, new CubePopulatorEvent(world, null));
    }

    private void generate(World world, Random random, CubePos pos, Biome biome, CubePopulatorEvent event) {
        LostCityCubicGenerator generator = new LostCityCubicGenerator(world, random);
        generator.spawnInChunk(world, random, pos.chunkPos().x, pos.getY(), pos.chunkPos().z);
    }
    */
}
