package mcjty.lostcities.cubic;

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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@Mod.EventBusSubscriber
public class CubicCityWorldProcessor extends CubeCityGenerator {

    @Nonnull
    public static CubeDriver driver = new CubeDriver();

    public static boolean isCubicWorld;
    private static boolean checkedCubicWorld;

    private static ICubeGenerator terrainProcessor;

    public static World worldObj;

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
                // (ICubeGenerator) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz }, new ClassFactory(instance));
    }

    // @Override
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
        return terrainProcessor.generateCube(cubeX, cubeY, cubeZ);
    }

    @Override
    public void populate(ICube cube) {
        terrainProcessor.populate(cube);

        driver.setCube(cube);
    }

    @SubscribeEvent
    public static void onCubePopulated(PopulateCubeEvent event) {
        // TODO: Test performance

        LostCityCubicGenerator generator = new LostCityCubicGenerator();
        generator.spawnInChunk();
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