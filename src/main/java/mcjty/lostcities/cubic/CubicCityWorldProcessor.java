package mcjty.lostcities.cubic;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event.PopulateCubeEvent;
import mcjty.lostcities.LostCitiesDebug;
import mcjty.lostcities.cubic.world.LostCityCubicGenerator;
import mcjty.lostcities.cubic.world.driver.CubeDriver;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber
public class CubicCityWorldProcessor extends CubeCityGenerator {

    @Nonnull
    public static CubeDriver driver = new CubeDriver();

    public static boolean isCubicWorld;
    private static boolean checkedCubicWorld;

    private static ICubeGenerator terrainProcessor;

    public static World worldObj;

    // TODO: Missing dimension id
    public static Map<CubePos, CubePrimer> cachedPrimers = new HashMap<>();

    public static Map<CubePos, ICube> cachedCubes = new HashMap<>();

    public CubicCityWorldProcessor(World world)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        super(world);

        worldObj = world;

        if(LostCitiesDebug.debug) System.out.println("Creating processor!");

        if(terrainProcessor != null) return;

        Class<?> clazz = Class.forName("io.github.terra121.EarthTerrainProcessor");
        Constructor<?> constructor = clazz.getConstructor(World.class);
        Object instance = constructor.newInstance(world);
        Class<?> interfaze = Class.forName("io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator");
        terrainProcessor = (ICubeGenerator) interfaze.cast(instance);
    }

    // @Override
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
        CubePrimer primer = terrainProcessor.generateCube(cubeX, cubeY, cubeZ);

        CubePos key = new CubePos(cubeX, cubeY, cubeZ);
        cachedPrimers.put(key, primer);
        return primer;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void populate(ICube cube) {
        // driver.setCube(cube);

        CubePos key = new CubePos(cube.getX(), cube.getY(), cube.getZ());
        cachedCubes.put(key, cube);

        terrainProcessor.populate(cube);
    }

    @SubscribeEvent
    public static void onCubePopulated(PopulateCubeEvent event) {
        LostCityCubicGenerator generator = new LostCityCubicGenerator();
        generator.spawnInChunk(event.getCubeX(), event.getCubeY(), event.getCubeZ());
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
