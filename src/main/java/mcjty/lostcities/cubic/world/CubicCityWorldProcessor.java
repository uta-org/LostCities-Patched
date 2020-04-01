package mcjty.lostcities.cubic.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event.PopulateCubeEvent;
import io.github.terra121.events.RoadPopulateEvent;
import mcjty.lostcities.cubic.CubeCityGenerator;
import mcjty.lostcities.cubic.world.driver.CubeDriver;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber
public class CubicCityWorldProcessor extends CubeCityGenerator
{
    @Nonnull
    public static CubeDriver driver = new CubeDriver();

    public static boolean isCubicWorld;
    private static boolean checkedCubicWorld;

    private static boolean createdProcessorInstance;

    private static ICubeGenerator terrainProcessor;

    public static World worldObj;

    // TODO: Missing dimension id && also profile the variable (RAM usage)
    public static Map<CubePos, CubePrimer> cachedPrimers = new HashMap<>();

    public static Map<CubePos, ICube> cachedCubes = new HashMap<>();

    public static Set<CubePos> cachedRoads = new HashSet<>();

    public CubicCityWorldProcessor(World world)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException
    {
        super(world);

        worldObj = world;
        driver.setWorld(world);
        driver.useLocal();

        // if(LostCitiesDebug.debug) System.out.println("Creating processor!");

        init();
    }

    private static void init()
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException
    {
        Class<?> clazz = Class.forName("io.github.terra121.EarthTerrainProcessor");
        Constructor<?> constructor = clazz.getConstructor(World.class);
        Object instance = constructor.newInstance(worldObj);
        terrainProcessor = addCubicPopulator(instance);

        CubeCityUtils.init(worldObj.getSeed());
    }

    private static ICubeGenerator addCubicPopulator(Object instance)
            throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException
    {
        // Thanks to: https://stackoverflow.com/questions/40461684/java-reflections-list-nosuchmethodexception
        Class<?> interfaze = Class.forName("io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator");

        Field fieldDefinition = instance.getClass().getDeclaredField("surfacePopulators");
        fieldDefinition.setAccessible(true);

        Object fieldValue = fieldDefinition.get(instance);

        Method myMethod = fieldValue.getClass().getDeclaredMethod("add", Object.class);
        myMethod.invoke(fieldValue, new CubicCityWorldPopulator());

        return (ICubeGenerator)interfaze.cast(instance);
    }

    @MethodsReturnNonnullByDefault
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
        CubePrimer primer = terrainProcessor.generateCube(cubeX, cubeY, cubeZ);

        CubePos key = new CubePos(cubeX, cubeY, cubeZ);
        cachedPrimers.put(key, primer);

        return primer;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void populate(ICube cube) {
        terrainProcessor.populate(cube);

        CubePos key = new CubePos(cube.getX(), cube.getY(), cube.getZ());
        cachedCubes.put(key, cube);
    }

    @SubscribeEvent
    public static void onCubePopulated(PopulateCubeEvent event) {

    }

    @SubscribeEvent
    public static void onRoadPopulated(RoadPopulateEvent event) {
        cachedRoads.add(event.cubePos);
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
    public class SortedEarthTreePopulator extends EarthTreePopulator {

        public SortedEarthTreePopulator(GeographicProjection proj) {
            super(proj);
        }
    }

    public class SortedRoadGenerator extends RoadGenerator {

        public SortedRoadGenerator(OpenStreetMaps osm, Heights heights, GeographicProjection proj) {
            super(osm, heights, proj);
        }
    }
    */

}
