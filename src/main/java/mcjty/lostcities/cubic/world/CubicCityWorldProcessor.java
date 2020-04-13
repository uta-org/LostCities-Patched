package mcjty.lostcities.cubic.world;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import mcjty.lostcities.LostCitiesDebug;
import mcjty.lostcities.cubic.CubeCityGenerator;
import mcjty.lostcities.cubic.world.driver.CubeDriver;
import mcjty.lostcities.cubic.world.generators.BuildingGenerator;
import mcjty.lostcities.cubic.world.generators.SpawnersGenerator;
import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.BlockVine;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static mcjty.lostcities.cubic.world.CubicCityUtils.airChar;
import static mcjty.lostcities.cubic.world.CubicCityUtils.getRandom;

public class CubicCityWorldProcessor extends CubeCityGenerator {
    @Nonnull
    public static CubeDriver driver = new CubeDriver();

    public static boolean isCubicWorld;

    private static ICubeGenerator terrainProcessor;
    public static CubicCityWorldPopulator populator;

    public static World worldObj;
    public static ICubicWorld cubicWorld;

    private static SpawnersGenerator spawnersGenerator;

    public CubicCityWorldProcessor(World world)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException {
        super(world);

        worldObj = world;
        driver.setWorld(world);
        driver.useLocal();

        populator = new CubicCityWorldPopulator();

        ICommonGeneratorProvider provider = init();

        spawnersGenerator = new SpawnersGenerator(CubicCityUtils.profile, provider);
    }

    private static ICommonGeneratorProvider init()
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException {
        Class<?> clazz = Class.forName("io.github.terra121.EarthTerrainProcessor");
        Constructor<?> constructor = clazz.getConstructor(World.class);
        Object instance = constructor.newInstance(worldObj);
        ICommonGeneratorProvider provider = addCubicPopulator(instance);

        CubicCityUtils.init(worldObj.getSeed());
        return provider;
    }

    private static ICommonGeneratorProvider addCubicPopulator(Object instance)
            throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, InstantiationException {
        // Thanks to: https://stackoverflow.com/questions/40461684/java-reflections-list-nosuchmethodexception
        Class<?> interfaze = Class.forName("io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator");

        Field fieldDefinition = instance.getClass().getDeclaredField("surfacePopulators");
        fieldDefinition.setAccessible(true);

        Object fieldValue = fieldDefinition.get(instance);

        CubicCityWorldPopulator populator = new CubicCityWorldPopulator();
        Method myMethod = fieldValue.getClass().getDeclaredMethod("add", Object.class);
        myMethod.invoke(fieldValue, populator);

        terrainProcessor = (ICubeGenerator) interfaze.cast(instance);
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
        generateDoors(info, provider);
    }

    // TODO: Remove
    private static final boolean GEN_DOOR = true;

    private static void generateDoors(BuildingInfo info, ICommonGeneratorProvider provider) {
        char filler = info.getCompiledPalette().get(info.getBuilding().getFillerBlock());

        for (BuildingGenerator.DoorModel door : info.getDoorTodo()) {
            driver.setLocalBlock(door.getCoord().getChunkX(), 0, door.getCoord().getChunkZ());
            driver.current(0, door.getHeight(), 0);

            // CubicCityUtils.buildingGenerator.generateDoors(info, door.getHeight() + 1, door.getFloor());
            // continue;

            // System.out.println("Generating doors at: "+driver.toString());

            if (!GEN_DOOR) {
                CubicCityUtils.buildingGenerator.generateDoors(info, door.getHeight() + 1, door.getFloor());
            } else {
                boolean eastFacing = worldObj.rand.nextBoolean();
                boolean westFacing = worldObj.rand.nextBoolean();
                boolean northFacing = worldObj.rand.nextBoolean();
                boolean southFacing = worldObj.rand.nextBoolean();

                if (!(eastFacing && westFacing && northFacing && southFacing)) {
                    // int v = getFacingDirection();
                    int v = (int)getRandom(0, 4);

                    switch (v) {
                        case 0:
                            eastFacing = true;
                            break;

                        case 1:
                            westFacing = true;
                            break;

                        case 2:
                            northFacing = true;
                            break;

                        case 3:
                            southFacing = true;
                            break;
                    }
                }

                // North, south: x...
                int height = door.getHeight() + 1;
                // int height = door.getHeight();

                int pos = 7;
                int upper = 15;
                int down = 0;

                if (northFacing) {
                    boolean isAdjacentBuilding = isAdjacentBuilding(EnumFacing.NORTH, provider);

                    if (!isAdjacentBuilding)
                        debugDoors(height, pos, down, EnumFacing.NORTH);

                    generateDoors(info, height, pos, down, EnumFacing.NORTH, isAdjacentBuilding, filler);
                    // generateDoors(info, height, 8, 0, EnumFacing.NORTH, isAdjacentBuilding, filler);
                } else if (southFacing) {
                    boolean isAdjacentBuilding = isAdjacentBuilding(EnumFacing.SOUTH, provider);

                    if (!isAdjacentBuilding)
                        debugDoors(height, pos, upper, EnumFacing.SOUTH);

                    generateDoors(info, height, pos, upper, EnumFacing.SOUTH, isAdjacentBuilding, filler);
                    // generateDoors(info, height, 8, 15, EnumFacing.SOUTH, isAdjacentBuilding, filler);
                } else if (westFacing) {
                    boolean isAdjacentBuilding = isAdjacentBuilding(EnumFacing.WEST, provider);

                    if (!isAdjacentBuilding)
                        debugDoors(height, down, pos, EnumFacing.WEST);

                    generateDoors(info, height, down, pos, EnumFacing.WEST, isAdjacentBuilding, filler);
                    // generateDoors(info, height, 0, 8, EnumFacing.WEST, isAdjacentBuilding, filler);
                } else if (eastFacing) {
                    boolean isAdjacentBuilding = isAdjacentBuilding(EnumFacing.EAST, provider);

                    if (!isAdjacentBuilding)
                        debugDoors(height, upper, pos, EnumFacing.EAST);

                    generateDoors(info, height, upper, pos, EnumFacing.EAST, isAdjacentBuilding, filler);
                    // generateDoors(info, height, 15, 8, EnumFacing.EAST, isAdjacentBuilding, filler);
                }
            }
        }

        info.clearDoorTodo();
    }

    // TODO: Get facing direction of the building to avoid walls and doors incoherences
    private static int getFacingDirection() {
        return -1;
    }

    private static void debugDoors(int height, int x, int z, EnumFacing facing) {
        if (!LostCitiesDebug.debug)
            return;

        System.out.println("Generating doors at: (" + driver.getTp() + ") [height: " + height + ", x: " + x + ", z: " + z + ", facing: " + facing.toString() + "]");
    }

    private static void generateDoors(BuildingInfo info, int height, int x, int z, EnumFacing facing, boolean isAdjacentBuilding, char filler) {
        height--;       // Start generating doors one below for the filler

        for(int i = 0; i <= 1; ++i) {
            // 7, 8
            boolean isX = x == 7;

            // 6, 9
            int sx = x + (isX ? (i == 0 ? -1 : 2) : 0);
            int sz = z + (!isX ? (i == 0 ? -1 : 2) : 0);

            driver.setBlockRange(sx, height, sz, height + 4, filler);
            driver.setBlockRange(sz, height, sz, height + 4, filler);

            sx = x + (isX ? i : 0);
            sz = z + (!isX ? i : 0);

            if (isAdjacentBuilding) {
                // Create connection between buildings
                return;
            }

            driver.current(sx, height, sz)
                    .add(filler)
                    .add(getDoor(info.doorBlock, false, false, facing))
                    .add(getDoor(info.doorBlock, true, false, facing))
                    .add(filler);
            driver.current(sx, height, sz)
                    .add(filler)
                    .add(getDoor(info.doorBlock, false, true, facing))
                    .add(getDoor(info.doorBlock, true, true, facing))
                    .add(filler);

            // TODO: Clear blocks and do border
        }
    }

    /*
    private static int get(int v, EnumFacing facing, boolean isX) {
        if(!isX) {
            if(facing == EnumFacing.NORTH) {
                v -= 1;
            } else if(facing == EnumFacing.SOUTH) {
                v += 1;
            }
        }
        else {
            if(facing == EnumFacing.WEST) {
                v -= 1;
            } else if(facing == EnumFacing.EAST) {
                v += 1;
            }
        }

        return v;
    }
    */

    private static char getDoor(Block door, boolean upper, boolean left, EnumFacing facing) {
        IBlockState bs = door.getDefaultState()
                .withProperty(BlockDoor.HALF, upper ? BlockDoor.EnumDoorHalf.UPPER : BlockDoor.EnumDoorHalf.LOWER)
                .withProperty(BlockDoor.HINGE, left ? BlockDoor.EnumHingePosition.LEFT : BlockDoor.EnumHingePosition.RIGHT)
                .withProperty(BlockDoor.FACING, facing);

        return (char) Block.BLOCK_STATE_IDS.get(bs);
    }

    private static boolean isAdjacentBuilding(EnumFacing facing, ICommonGeneratorProvider provider) {
        BuildingInfo adjacent = getAdjacent(facing, provider);
        return adjacent.hasBuilding;
    }

    private static BuildingInfo getAdjacent(EnumFacing facing, ICommonGeneratorProvider provider) {
        int x = driver.getLocalX();
        int z = driver.getLocalZ();

        if (facing == EnumFacing.NORTH) {
            z -= 1;
        } else if (facing == EnumFacing.SOUTH) {
            z += 1;
        } else if (facing == EnumFacing.WEST) {
            x -= 1;
        } else if (facing == EnumFacing.EAST) {
            x += 1;
        }

        return BuildingInfo.getBuildingInfo(x, z, provider);
    }

    private static void generateLadders(BuildingInfo info) {
        IBlockState ladder = Blocks.LADDER.getDefaultState();
        char ladderChar = (char) Block.BLOCK_STATE_IDS.get(ladder);

        // TODO: Rotation?
        // .withRotation(Blocks.LADDER.getDefaultState(), Rotation.NONE);

        for (BlockPos bp : info.getLadderTodo()) {
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
                ((BlockSapling) Blocks.SAPLING).generateTree(world, pos, state, random);
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
        if (cubicWorld != null)
            return isCubicWorld;

        try {
            cubicWorld = (ICubicWorld) world;
            isCubicWorld = cubicWorld != null;
        } catch (Exception ex) {
            isCubicWorld = false;
        }

        return isCubicWorld;
    }
}
