package mcjty.lostcities.cubic;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event.PopulateCubeEvent;
import mcjty.lostcities.cubic.world.LostCityCubicGenerator;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.*;

// import io.github.terra121.EarthTerrainProcessor;

public class CubicCityWorldProcessor extends CubeCityGenerator {

    public static boolean isCubicWorld;
    private static boolean checkedCubicWorld;

    private static CubePrimer currentPrimer;
    private static ICubeGenerator terrainProcessor;

    public CubicCityWorldProcessor(World world)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        super(world);

        Class<?> clazz = Class.forName("io.github.terra121.EarthTerrainProcessor");
        Constructor<?> constructor = clazz.getConstructor(World.class);
        Object instance = constructor.newInstance(world);
        terrainProcessor = (ICubeGenerator) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz }, new MyHandler(instance));
                // new EarthTerrainProcessor(world);
    }

    // @Override
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
        currentPrimer = terrainProcessor.generateCube(cubeX, cubeY, cubeZ);
        return currentPrimer;
    }

    @Override
    public void populate(ICube cube) {
        terrainProcessor.populate(cube);
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

    public static class MyHandler implements InvocationHandler {
        private final Object o;

        public MyHandler(Object o) {
            this.o = o;
        }

        public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
            Method method = o.getClass().getMethod(m.getName(), m.getParameterTypes());
            return method.invoke(o, args);
        }
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
