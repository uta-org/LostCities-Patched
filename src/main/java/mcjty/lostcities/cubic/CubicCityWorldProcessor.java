package mcjty.lostcities.cubic;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event.PopulateCubeEvent;
import mcjty.lostcities.LostCitiesDebug;
import mcjty.lostcities.cubic.utils.ClassFactory;
import mcjty.lostcities.cubic.world.LostCityCubicGenerator;
import mcjty.lostcities.varia.ChunkCoord;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;

// import io.github.terra121.EarthTerrainProcessor;

@Mod.EventBusSubscriber
public class CubicCityWorldProcessor extends CubeCityGenerator {

    public static boolean isCubicWorld;
    private static boolean checkedCubicWorld;

    // Not used
    private static CubePrimer currentPrimer;
    private static ICubeGenerator terrainProcessor;

    private static World world;

    private static Map<CubePos, ICube> cubes = new HashMap<>();

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

        CubePos cubePos = cube.getCoords();
        cubes.put(cubePos, cube);
    }

    @SubscribeEvent
    public static void onCubePopulated(PopulateCubeEvent event) {
        // TODO: Test performance
        CubePos cubePos = new CubePos(event.getCubeX(), event.getCubeY(), event.getCubeZ());
        ICube cube = cubes.get(cubePos);

        LostCityCubicGenerator generator = new LostCityCubicGenerator(world);
        generator.spawnInChunk(cube);

        cubes.remove(cubePos);
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