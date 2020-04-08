package mcjty.lostcities.cubic.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import mcjty.lostcities.LostCitiesDebug;
import mcjty.lostcities.cubic.CubeCityGenerator;
import mcjty.lostcities.cubic.world.driver.CubeDriver;
import mcjty.lostcities.cubic.world.generators.SpawnersGenerator;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.BiPredicate;

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToCube;
import static mcjty.lostcities.cubic.world.CubicCityUtils.airChar;
import static mcjty.lostcities.cubic.world.CubicCityUtils.profile;

public class CubicCityWorldProcessor extends CubeCityGenerator
{
    @Nonnull
    public static CubeDriver driver = new CubeDriver();

    public static boolean isCubicWorld;

    private static ICubeGenerator terrainProcessor;
    public static CubicCityWorldPopulator populator;

    public static World worldObj;
    public static ICubicWorld cubicWorld;

    private static SpawnersGenerator spawnersGenerator;

    public CubicCityWorldProcessor(World world)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException
    {
        super(world);

        worldObj = world;
        driver.setWorld(world);
        driver.useLocal();

        populator = new CubicCityWorldPopulator();

        ICommonGeneratorProvider provider = init();

        spawnersGenerator = new SpawnersGenerator(CubicCityUtils.profile, provider);
    }

    private static ICommonGeneratorProvider init()
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException
    {
        Class<?> clazz = Class.forName("io.github.terra121.EarthTerrainProcessor");
        Constructor<?> constructor = clazz.getConstructor(World.class);
        Object instance = constructor.newInstance(worldObj);
        ICommonGeneratorProvider provider = addCubicPopulator(instance);

        CubicCityUtils.init(worldObj.getSeed());
        return provider;
    }

    private static ICommonGeneratorProvider addCubicPopulator(Object instance)
            throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException
    {
        // Thanks to: https://stackoverflow.com/questions/40461684/java-reflections-list-nosuchmethodexception
        Class<?> interfaze = Class.forName("io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator");

        Field fieldDefinition = instance.getClass().getDeclaredField("surfacePopulators");
        fieldDefinition.setAccessible(true);

        Object fieldValue = fieldDefinition.get(instance);

        CubicCityWorldPopulator populator = new CubicCityWorldPopulator();
        Method myMethod = fieldValue.getClass().getDeclaredMethod("add", Object.class);
        myMethod.invoke(fieldValue, populator);

        terrainProcessor = (ICubeGenerator)interfaze.cast(instance);
        return populator;
    }

    @MethodsReturnNonnullByDefault
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
        return terrainProcessor.generateCube(cubeX, cubeY, cubeZ);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void populate(ICube cube) {
        terrainProcessor.populate(cube);
        spawnersGenerator.populate(worldObj.rand, cube.getX(), cube.getZ(), world);
    }

    public static boolean checkForCubicWorld(World world) {
        if(cubicWorld != null)
            return isCubicWorld;

        try {
            cubicWorld = (ICubicWorld)world;
            isCubicWorld = cubicWorld != null;
        } catch(Exception ex) {
            isCubicWorld = false;
        }

        return isCubicWorld;
    }

    /*
    public static BlockPos getSurfaceBlock(CubePos pos) {
        return cubicWorld.getSurfaceForCube(pos, 0, 0, 0, ICubicWorld.SurfaceType.SOLID);
    }
    */

    public static BlockPos findTopBlock(CubePos pos) {
        /*
        BlockPos blockPos = pos.getCenterBlockPos();
        blockPos = new BlockPos(blockPos.getX(), blockPos.getY() + 8, blockPos.getZ());

        return findTopBlock(blockPos, blockPos.getY() - 16, blockPos.getY());
         */

        return cubicWorld.getSurfaceForCube(pos, 0, 0, 0, CubicCityWorldProcessor::canBeTopBlock);
    }

    private static BlockPos findTopBlock(BlockPos start, int minTopY, int maxTopY) {
        BlockPos pos = start;
        IBlockState startState = worldObj.getBlockState(start);
        if (canBeTopBlock(start, startState)) {
            // the top tested block is "top", don't use that one because we don't know what is above
            if(LostCitiesDebug.debug) System.out.println(start.toString()+" Top block isn't valid!");
            return null;
        }
        ICube cube = cubicWorld.getCubeFromBlockCoords(pos.down());
        while (pos.getY() >= minTopY) {
            BlockPos next = pos.down();
            if (blockToCube(next.getY()) != cube.getY()) {
                cube = cubicWorld.getCubeFromBlockCoords(next);
            }
            if (!cube.isEmpty()) {
                IBlockState state = cube.getBlockState(next);
                if (canBeTopBlock(next, state)) {
                    break;
                }
            }
            pos = next;
        }
        if (pos.getY() < minTopY || pos.getY() > maxTopY) {
            if(LostCitiesDebug.debug) System.out.println("Surpassed limits!");
            return null;
        }
        return pos;
    }

    private static boolean canBeTopBlock(BlockPos pos, IBlockState state) {
        char b = (char)Block.BLOCK_STATE_IDS.get(state);
        return b != airChar;
    }
}
