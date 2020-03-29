package mcjty.lostcities.cubic;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event.PopulateCubeEvent;
import mcjty.lostcities.LostCitiesDebug;
import mcjty.lostcities.cubic.utils.ClassFactory;
import mcjty.lostcities.cubic.world.LostCityCubicGenerator;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.*;

// import io.github.terra121.EarthTerrainProcessor;

public class CubicCityWorldProcessor extends CubeCityGenerator {

    public static boolean isCubicWorld;
    private static boolean checkedCubicWorld;

    // Not used
    private static CubePrimer currentPrimer;
    private static ICubeGenerator terrainProcessor;

    private static World world;

    public CubicCityWorldProcessor(World _world)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        super(_world);

        world = _world;

        if(LostCitiesDebug.debug) System.out.println("Creating processor!");

        if(terrainProcessor != null) return;

        Class<?> clazz = Class.forName("io.github.terra121.EarthTerrainProcessor");
        Constructor<?> constructor = clazz.getConstructor(World.class);
        Object instance = constructor.newInstance(world);
        Class<?> interfaze = Class.forName("io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator");
        terrainProcessor = (ICubeGenerator) interfaze.cast(instance);
                // (ICubeGenerator) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz }, new ClassFactory(instance));
    }

    // @Override
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
        currentPrimer = terrainProcessor.generateCube(cubeX, cubeY, cubeZ);
        return currentPrimer;
    }

    @Override
    public void populate(ICube cube) {
        terrainProcessor.populate(cube);

        LostCityCubicGenerator generator = new LostCityCubicGenerator(world);
        generator.spawnInChunk(cube);
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

}