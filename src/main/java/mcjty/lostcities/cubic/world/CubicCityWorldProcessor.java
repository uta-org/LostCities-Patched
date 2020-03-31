package mcjty.lostcities.cubic.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.ICubicPopulator;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event.PopulateCubeEvent;
import mcjty.lostcities.LostCitiesDebug;
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
import java.util.Map;

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
        /*if(createdProcessorInstance)
            return;

        createdProcessorInstance = true;*/

        Class<?> clazz = Class.forName("io.github.terra121.EarthTerrainProcessor");
        Constructor<?> constructor = clazz.getConstructor(World.class);
        Object instance = constructor.newInstance(worldObj);
        terrainProcessor = addCubicPopulator(instance);

        CubeCityUtils.init(worldObj.getSeed());
    }

    private static ICubeGenerator addCubicPopulator(Object instance)
            throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException
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

        //LostCityCubicGenerator generator = new LostCityCubicGenerator();
        //generator.spawnInChunk(cube.getX(), cube.getY(), cube.getZ());
    }

    @SubscribeEvent
    public static void onCubePopulated(PopulateCubeEvent event) {
        /*
        if(event.getClass() == PopulateCubeEvent.Pre.class)
            return;

        LostCityCubicGenerator generator = new LostCityCubicGenerator();
        generator.spawnInChunk(event.getCubeX(), event.getCubeY(), event.getCubeZ());
         */
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
