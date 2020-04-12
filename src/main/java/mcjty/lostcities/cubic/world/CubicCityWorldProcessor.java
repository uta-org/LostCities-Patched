package mcjty.lostcities.cubic.world;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import mcjty.lostcities.cubic.CubeCityGenerator;
import mcjty.lostcities.cubic.world.driver.CubeDriver;
import mcjty.lostcities.cubic.world.generators.SpawnersGenerator;
import mcjty.lostcities.dimensions.world.LostCityChunkGenerator;
import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.BlockVine;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

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

    public static void doTodoPopulate(int chunkX, int chunkZ, ICommonGeneratorProvider provider, BuildingInfo info) {
        generateTrees(worldObj.rand, chunkX, chunkZ, worldObj, provider);
        generateVines(worldObj.rand, chunkX, chunkZ, worldObj, provider);

        generateLadders(info);
    }

    private static void generateLadders(BuildingInfo info) {
        IBlockState ladder = Blocks.LADDER.getDefaultState();
        char ladderChar = (char)Block.BLOCK_STATE_IDS.get(ladder);

        // TODO: Rotation?
                // .withRotation(Blocks.LADDER.getDefaultState(), Rotation.NONE);

         for(BlockPos bp : info.getLadderTodo()) {
             driver.current(bp.getX(), bp.getY(), bp.getZ());
             driver.add(ladderChar);
         }

         info.clearLadderTodo();
    }

    private static void generateTrees(Random random, int chunkX, int chunkZ, World world, ICommonGeneratorProvider provider) {
        BuildingInfo info = BuildingInfo.getBuildingInfo(chunkX, chunkZ, provider);
        for (BlockPos pos : info.getSaplingTodo()) {
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() == Blocks.SAPLING) {
                ((BlockSapling)Blocks.SAPLING).generateTree(world, pos, state, random);
            }
        }
        info.clearSaplingTodo();
    }

    private static void generateVines(Random random, int chunkX, int chunkZ, World world, ICommonGeneratorProvider provider) {
        int cx = chunkX * 16;
        int cz = chunkZ * 16;
        BuildingInfo info = BuildingInfo.getBuildingInfo(chunkX, chunkZ, provider);

        if (info.hasBuilding) {
            BuildingInfo adjacent = info.getXmax();
            int bottom = Math.max(adjacent.getCityGroundLevel() + 3, adjacent.hasBuilding ? adjacent.getMaxHeight() : (adjacent.getCityGroundLevel() + 3));
            for (int z = 0; z < 15; z++) {
                for (int y = bottom; y < (info.getMaxHeight()); y++) {
                    if (random.nextFloat() < provider.getProfile().VINE_CHANCE) {
                        createVineStrip(random, world, bottom, BlockVine.WEST, new BlockPos(cx + 16, y, cz + z), new BlockPos(cx + 15, y, cz + z));
                    }
                }
            }
        }
        if (info.getXmax().hasBuilding) {
            BuildingInfo adjacent = info.getXmax();
            int bottom = Math.max(info.getCityGroundLevel() + 3, info.hasBuilding ? info.getMaxHeight() : (info.getCityGroundLevel() + 3));
            for (int z = 0; z < 15; z++) {
                for (int y = bottom; y < (adjacent.getMaxHeight()); y++) {
                    if (random.nextFloat() < provider.getProfile().VINE_CHANCE) {
                        createVineStrip(random, world, bottom, BlockVine.EAST, new BlockPos(cx + 15, y, cz + z), new BlockPos(cx + 16, y, cz + z));
                    }
                }
            }
        }

        if (info.hasBuilding) {
            BuildingInfo adjacent = info.getZmax();
            int bottom = Math.max(adjacent.getCityGroundLevel() + 3, adjacent.hasBuilding ? adjacent.getMaxHeight() : (adjacent.getCityGroundLevel() + 3));
            for (int x = 0; x < 15; x++) {
                for (int y = bottom; y < (info.getMaxHeight()); y++) {
                    if (random.nextFloat() < provider.getProfile().VINE_CHANCE) {
                        createVineStrip(random, world, bottom, BlockVine.NORTH, new BlockPos(cx + x, y, cz + 16), new BlockPos(cx + x, y, cz + 15));
                    }
                }
            }
        }
        if (info.getZmax().hasBuilding) {
            BuildingInfo adjacent = info.getZmax();
            int bottom = Math.max(info.getCityGroundLevel() + 3, info.hasBuilding ? info.getMaxHeight() : (info.getCityGroundLevel() + 3));
            for (int x = 0; x < 15; x++) {
                for (int y = bottom; y < (adjacent.getMaxHeight()); y++) {
                    if (random.nextFloat() < provider.getProfile().VINE_CHANCE) {
                        createVineStrip(random, world, bottom, BlockVine.SOUTH, new BlockPos(cx + x, y, cz + 15), new BlockPos(cx + x, y, cz + 16));
                    }
                }
            }
        }
    }

    private static void createVineStrip(Random random, World world, int bottom, PropertyBool direction, BlockPos pos, BlockPos vineHolderPos) {
        if (world.isAirBlock(vineHolderPos)) {
            return;
        }
        if (!world.isAirBlock(pos)) {
            return;
        }
        world.setBlockState(pos, Blocks.VINE.getDefaultState().withProperty(direction, true));
        pos = pos.down();
        while (pos.getY() >= bottom && random.nextFloat() < .8f) {
            if (!world.isAirBlock(pos)) {
                return;
            }
            world.setBlockState(pos, Blocks.VINE.getDefaultState().withProperty(direction, true));
            pos = pos.down();
        }
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
}
