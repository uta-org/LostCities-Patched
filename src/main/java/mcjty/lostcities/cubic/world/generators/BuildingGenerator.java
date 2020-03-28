package mcjty.lostcities.cubic.world.generators;

import mcjty.lostcities.config.LostCityConfiguration;
import mcjty.lostcities.cubic.world.LostCityCubicGenerator;
import mcjty.lostcities.cubic.world.driver.CubeDriver;
import mcjty.lostcities.dimensions.world.ChunkHeightmap;
import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import mcjty.lostcities.dimensions.world.lost.Orientation;
import mcjty.lostcities.dimensions.world.lost.Transform;
import mcjty.lostcities.dimensions.world.lost.cityassets.BuildingPart;
import mcjty.lostcities.dimensions.world.lost.cityassets.CompiledPalette;
import mcjty.lostcities.dimensions.world.lost.cityassets.IBuildingPart;
import mcjty.lostcities.dimensions.world.lost.cityassets.Palette;
import net.minecraft.block.*;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BuildingGenerator {
    private static Set<Character> rotatableChars = null;
    private static Set<Character> charactersNeedingTodo = null;
    private static Set<Character> charactersNeedingLightingUpdate = null;
    private static Set<Character> railChars = null;

    public static Set<Character> getRailChars() {
        if (railChars == null) {
            railChars = new HashSet<>();
            addStates(Blocks.RAIL, railChars);
            addStates(Blocks.GOLDEN_RAIL, railChars);
        }
        return railChars;
    }

    public static Set<Character> getCharactersNeedingTodo() {
        if (charactersNeedingTodo == null) {
            charactersNeedingTodo = new HashSet<>();
            charactersNeedingTodo.add((char) Block.BLOCK_STATE_IDS.get(Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.ACACIA)));
            charactersNeedingTodo.add((char) Block.BLOCK_STATE_IDS.get(Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.BIRCH)));
            charactersNeedingTodo.add((char) Block.BLOCK_STATE_IDS.get(Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.OAK)));
            charactersNeedingTodo.add((char) Block.BLOCK_STATE_IDS.get(Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.SPRUCE)));
            charactersNeedingTodo.add((char) Block.BLOCK_STATE_IDS.get(Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.DARK_OAK)));
            charactersNeedingTodo.add((char) Block.BLOCK_STATE_IDS.get(Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.JUNGLE)));
            charactersNeedingTodo.add((char) Block.BLOCK_STATE_IDS.get(Blocks.RED_FLOWER.getDefaultState()));
            charactersNeedingTodo.add((char) Block.BLOCK_STATE_IDS.get(Blocks.YELLOW_FLOWER.getDefaultState()));
        }
        return charactersNeedingTodo;
    }

    public static Set<Character> getCharactersNeedingLightingUpdate() {
        if (charactersNeedingLightingUpdate == null) {
            charactersNeedingLightingUpdate = new HashSet<>();
            for (String s : LostCityConfiguration.BLOCKS_REQUIRING_LIGHTING_UPDATES) {
                Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(s));
                if (block != null) {
                    addStates(block, charactersNeedingLightingUpdate);
                }
            }
        }
        return charactersNeedingLightingUpdate;
    }

    public static Set<Character> getRotatableChars() {
        if (rotatableChars == null) {
            rotatableChars = new HashSet<>();
            addStates(Blocks.ACACIA_STAIRS, rotatableChars);
            addStates(Blocks.BIRCH_STAIRS, rotatableChars);
            addStates(Blocks.BRICK_STAIRS, rotatableChars);
            addStates(Blocks.QUARTZ_STAIRS, rotatableChars);
            addStates(Blocks.STONE_BRICK_STAIRS, rotatableChars);
            addStates(Blocks.DARK_OAK_STAIRS, rotatableChars);
            addStates(Blocks.JUNGLE_STAIRS, rotatableChars);
            addStates(Blocks.NETHER_BRICK_STAIRS, rotatableChars);
            addStates(Blocks.OAK_STAIRS, rotatableChars);
            addStates(Blocks.PURPUR_STAIRS, rotatableChars);
            addStates(Blocks.RED_SANDSTONE_STAIRS, rotatableChars);
            addStates(Blocks.SANDSTONE_STAIRS, rotatableChars);
            addStates(Blocks.SPRUCE_STAIRS, rotatableChars);
            addStates(Blocks.STONE_STAIRS, rotatableChars);
            addStates(Blocks.LADDER, rotatableChars);
        }
        return rotatableChars;
    }

    private static void addStates(Block block, Set<Character> set) {
        for (int m = 0; m < 16; m++) {
            try {
                IBlockState state = block.getStateFromMeta(m);
                set.add((char) Block.BLOCK_STATE_IDS.get(state));
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    private CubeDriver driver;

    private char baseChar;
    private char airChar;
    private char liquidChar;
    private char hardAirChar;

    private BuildingGenerator() {}

    public BuildingGenerator(CubeDriver driver) {
        this.driver = driver;

        this.baseChar = LostCityCubicGenerator.baseChar;
        this.airChar = LostCityCubicGenerator.airChar;
        this.liquidChar = LostCityCubicGenerator.liquidChar;
        this.hardAirChar = LostCityCubicGenerator.hardAirChar;
    }

    private void setBlocksFromPalette(int x, int y, int z, int y2, CompiledPalette palette, char character) {
        if (palette.isSimple(character)) {
            char b = palette.get(character);
            driver.setBlockRangeSafe(x, y, z, y2, b);
        } else {
            driver.current(x, y, z);
            while (y < y2) {
                driver.add(palette.get(character));
                y++;
            }
        }
    }

    public void generate(BuildingInfo info, ChunkHeightmap heightmap) {
        int lowestLevel = info.getCityGroundLevel() - info.floorsBelowGround * 6;

        Character borderBlock = info.getCityStyle().getBorderBlock();
        CompiledPalette palette = info.getCompiledPalette();
        char fillerBlock = info.getBuilding().getFillerBlock();

        if (info.profile.isFloating()) {
            // For floating worldgen we try to fit the underside of the building better with the island
            // We also remove all blocks from the inside because we generate buildings on top of
            // generated chunks as opposed to blank chunks with non-floating worlds
            for (int x = 0; x < 16; ++x) {
                for (int z = 0; z < 16; ++z) {
                    int index = (x << 12) | (z << 8);
                    int height = heightmap.getHeight(x, z);
                    if (height > 1 && height < lowestLevel - 1) {
                        driver.setBlockRange(x, height+1, z, lowestLevel, baseChar);
                    }
                    // Also clear the inside of buildings to avoid geometry that doesn't really belong there
                    clearRange(info, x, z, lowestLevel, info.getCityGroundLevel() + info.getNumFloors() * 6, info.waterLevel > info.groundLevel);
                }
            }
        } else if (info.profile.isSpace()) {
            fillToGround(info, lowestLevel, borderBlock);
            // Also clear the inside of buildings to avoid geometry that doesn't really belong there
            for (int x = 0; x < 16; ++x) {
                for (int z = 0; z < 16; ++z) {
                    clearRange(info, x, z, lowestLevel, info.getCityGroundLevel() + info.getNumFloors() * 6, false);     // Never water in bubbles?
                }
            }
        } else {
            // For normal worldgen (non floating) or cavern we have a thin layer of 'border' blocks because that looks nicer
            for (int x = 0; x < 16; ++x) {
                for (int z = 0; z < 16; ++z) {
                    if (isSide(x, z)) {
                        driver.setBlockRange(x, info.profile.BEDROCK_LAYER, z, lowestLevel - 10, baseChar);
                        int y = lowestLevel - 10;
                        driver.current(x, y, z);
                        while (y < lowestLevel) {
                            driver.add(palette.get(borderBlock));
                            y++;
                        }
                    } else if (info.profile.isDefault()) {
                        driver.setBlockRange(x, info.profile.BEDROCK_LAYER, z, lowestLevel, baseChar);
                    }
                    if (driver.getBlock(x, lowestLevel, z) == airChar) {
                        char filler = palette.get(fillerBlock);
                        driver.current(x, lowestLevel, z).block(filler); // There is nothing below so we fill this with the filler
                    }

                    if (info.profile.isCavern()) {
                        // Also clear the inside of buildings to avoid geometry that doesn't really belong there
//                        clearRange(primer, index, lowestLevel, info.getCityGroundLevel() + info.getNumFloors() * 6, waterLevel > mainGroundLevel);
                        clearRange(info, x, z, lowestLevel, info.getCityGroundLevel() + info.getNumFloors() * 6, info.waterLevel > info.groundLevel);
                    }
                }
            }
        }

        int height = lowestLevel;
        for (int f = -info.floorsBelowGround; f <= info.getNumFloors(); f++) {
            BuildingPart part = info.getFloor(f);
            generatePart(info, part, Transform.ROTATE_NONE, 0, height, 0, false);
            part = info.getFloorPart2(f);
            if (part != null) {
                generatePart(info, part, Transform.ROTATE_NONE, 0, height, 0, false);
            }

            // Check for doors
            boolean isTop = f == info.getNumFloors();   // The top does not need generated doors
            if (!isTop) {
                generateDoors(info, height + 1, f);
            }

            height += 6;    // We currently only support 6 here
        }

        if (info.floorsBelowGround > 0) {
            // Underground we replace the glass with the filler
            for (int x = 0; x < 16; x++) {
                // Use safe version because this may end up being lower
                setBlocksFromPalette(x, lowestLevel, 0, Math.min(info.getCityGroundLevel(), info.getZmin().getCityGroundLevel()) + 1, palette, fillerBlock);
                setBlocksFromPalette(x, lowestLevel, 15, Math.min(info.getCityGroundLevel(), info.getZmax().getCityGroundLevel()) + 1, palette, fillerBlock);
            }
            for (int z = 1; z < 15; z++) {
                setBlocksFromPalette(0, lowestLevel, z, Math.min(info.getCityGroundLevel(), info.getXmin().getCityGroundLevel()) + 1, palette, fillerBlock);
                setBlocksFromPalette(15, lowestLevel, z, Math.min(info.getCityGroundLevel(), info.getXmax().getCityGroundLevel()) + 1, palette, fillerBlock);
            }
        }

        if (info.floorsBelowGround >= 1) {
            // We have to potentially connect to corridors
            generateCorridorConnections(info);
        }
    }

    private void clearRange(BuildingInfo info, int x, int z, int height1, int height2, boolean dowater) {
        if (dowater) {
            // Special case for drowned city
            driver.setBlockRangeSafe(x, height1, z, info.waterLevel, liquidChar);
            driver.setBlockRangeSafe(x, info.waterLevel+1, z, height2, airChar);
        } else {
            driver.setBlockRange(x, height1, z, height2, airChar);
        }
    }

    // Used for space type worlds: fill underside the building/street until a block is encountered
    private void fillToGround(BuildingInfo info, int lowestLevel, Character borderBlock) {
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                int y = lowestLevel - 1;
                driver.current(x, y, z);
                if (isSide(x, z)) {
                    while (y > 1 && driver.getBlock() == airChar) {
                        driver.block(info.getCompiledPalette().get(borderBlock)).decY();
                    }
                } else {
                    while (y > 1 && driver.getBlock() == airChar) {
                        driver.block(baseChar).decY();
                    }
                }
            }
        }
    }

    /**
     * Generate a part. If 'airWaterLevel' is true then 'hard air' blocks are replaced with water below the waterLevel.
     * Otherwise they are replaced with air.
     */
    private int generatePart(BuildingInfo info, IBuildingPart part,
                             Transform transform,
                             int ox, int oy, int oz, boolean airWaterLevel) {
        CompiledPalette compiledPalette = info.getCompiledPalette();
        // Cache the combined palette?
        Palette localPalette = part.getLocalPalette();
        if (localPalette != null) {
            compiledPalette = new CompiledPalette(compiledPalette, localPalette);
        }

        boolean nowater = part.getMetaBoolean("nowater");

        for (int x = 0; x < part.getXSize(); x++) {
            for (int z = 0; z < part.getZSize(); z++) {
                char[] vs = part.getVSlice(x, z);
                if (vs != null) {
                    int rx = ox + transform.rotateX(x, z);
                    int rz = oz + transform.rotateZ(x, z);
                    driver.current(rx, oy, rz);
                    int len = vs.length;
                    for (int y = 0; y < len; y++) {
                        char c = vs[y];
                        Character b = compiledPalette.get(c);
                        if (b == null) {
                            throw new RuntimeException("Could not find entry '" + c + "' in the palette for part '" + part.getName() + "'!");
                        }

                        CompiledPalette.Info inf = compiledPalette.getInfo(c);

                        if (transform != Transform.ROTATE_NONE) {
                            if (getRotatableChars().contains(b)) {
                                IBlockState bs = Block.BLOCK_STATE_IDS.getByValue(b);
                                bs = bs.withRotation(transform.getMcRotation());
                                b = (char) Block.BLOCK_STATE_IDS.get(bs);
                            } else if (getRailChars().contains(b)) {
                                IBlockState bs = Block.BLOCK_STATE_IDS.getByValue(b);
                                PropertyEnum<BlockRailBase.EnumRailDirection> shapeProperty;
                                if (bs.getBlock() == Blocks.RAIL) {
                                    shapeProperty = BlockRail.SHAPE;
                                } else if (bs.getBlock() == Blocks.GOLDEN_RAIL) {
                                    shapeProperty = BlockRailPowered.SHAPE;
                                } else {
                                    throw new RuntimeException("Error with rail!");
                                }
                                BlockRailBase.EnumRailDirection shape = bs.getValue(shapeProperty);
                                bs = bs.withProperty(shapeProperty, transform.transform(shape));
                                b = (char) Block.BLOCK_STATE_IDS.get(bs);
                            }
                        }
                        // We don't replace the world where the part is empty (air)
                        if (b != airChar) {
                            if (b == liquidChar) {
                                if (info.profile.AVOID_WATER) {
                                    b = airChar;
                                }
                            } else if (b == hardAirChar) {
                                if (airWaterLevel && !info.profile.AVOID_WATER && !nowater) {
                                    b = (oy + y) < info.waterLevel ? liquidChar : airChar;
                                } else {
                                    b = airChar;
                                }
                            } else if (inf != null) {
                                Map<String, Integer> orientations = inf.getTorchOrientations();
                                if (orientations != null) {
                                    if (info.profile.GENERATE_LIGHTING) {
                                        info.addTorchTodo(driver.getCurrent(), orientations);
                                    } else {
                                        b = airChar;        // No torches
                                    }
                                } else if (inf.getLoot() != null && !inf.getLoot().isEmpty()) {
                                    if (!info.noLoot) {
                                        info.getTodoChunk(rx, rz).addLootTodo(new BlockPos(info.chunkX * 16 + rx, oy + y, info.chunkZ * 16 + rz),
                                                new BuildingInfo.ConditionTodo(inf.getLoot(), part.getName(), info));
                                    }
                                } else if (inf.getMobId() != null && !inf.getMobId().isEmpty()) {
                                    if (info.profile.GENERATE_SPAWNERS && !info.noLoot) {
                                        String mobid = inf.getMobId();
                                        info.getTodoChunk(rx, rz).addSpawnerTodo(new BlockPos(info.chunkX * 16 + rx, oy + y, info.chunkZ * 16 + rz),
                                                new BuildingInfo.ConditionTodo(mobid, part.getName(), info));
                                    } else {
                                        b = airChar;
                                    }
                                }
                            } else if (getCharactersNeedingLightingUpdate().contains(b)) {
                                info.getTodoChunk(rx, rz).addLightingUpdateTodo(new BlockPos(info.chunkX * 16 + rx, oy + y, info.chunkZ * 16 + rz));
                            } else if (getCharactersNeedingTodo().contains(b)) {
                                IBlockState bs = Block.BLOCK_STATE_IDS.getByValue(b);
                                Block block = bs.getBlock();
                                if (block instanceof BlockSapling || block instanceof BlockFlower) {
                                    if (info.profile.AVOID_FOLIAGE) {
                                        b = airChar;
                                    } else {
                                        info.getTodoChunk(rx, rz).addSaplingTodo(new BlockPos(info.chunkX * 16 + rx, oy + y, info.chunkZ * 16 + rz));
                                    }
                                }
                            }
                            driver.add(b);
                        } else {
                            driver.incY();
                        }
                    }
                }
            }
        }
        return oy + part.getSliceCount();
    }

    private void generateDoors(BuildingInfo info, int height, int f) {

        char filler = info.getCompiledPalette().get(info.getBuilding().getFillerBlock());

        height--;       // Start generating doors one below for the filler

        if (info.hasConnectionAtX(f + info.floorsBelowGround)) {
            int x = 0;
            if (hasConnectionWithBuilding(f, info, info.getXmin())) {
                driver.setBlockRange(x, height, 6, height + 4, filler);
                driver.setBlockRange(x, height, 9, height + 4, filler);

                driver.current(x, height, 7).add(filler).add(airChar).add(airChar).add(filler);
                driver.current(x, height, 8).add(filler).add(airChar).add(airChar).add(filler);

            } else if (hasConnectionToTopOrOutside(f, info, info.getXmin())) {
                driver.setBlockRange(x, height, 6, height + 4, filler);
                driver.setBlockRange(x, height, 9, height + 4, filler);

                driver.current(x, height, 7)
                        .add(filler)
                        .add(getDoor(info.doorBlock, false, true, EnumFacing.EAST))
                        .add(getDoor(info.doorBlock, true, true, EnumFacing.EAST))
                        .add(filler);
                driver.current(x, height, 8)
                        .add(filler)
                        .add(getDoor(info.doorBlock, false, false, EnumFacing.EAST))
                        .add(getDoor(info.doorBlock, true, false, EnumFacing.EAST))
                        .add(filler);
            }
        }
        if (hasConnectionWithBuildingMax(f, info, info.getXmax(), Orientation.X)) {
            int x = 15;
            driver.setBlockRange(x, height, 6, height + 4, filler);
            driver.setBlockRange(x, height, 9, height + 4, filler);
            driver.current(x, height, 7).add(filler).add(airChar).add(airChar).add(filler);
            driver.current(x, height, 8).add(filler).add(airChar).add(airChar).add(filler);
        } else if (hasConnectionToTopOrOutside(f, info, info.getXmax()) && (info.getXmax().hasConnectionAtXFromStreet(f + info.getXmax().floorsBelowGround))) {
            int x = 15;
            driver.setBlockRange(x, height, 6, height + 4, filler);
            driver.setBlockRange(x, height, 9, height + 4, filler);
            driver.current(x, height, 7)
                    .add(filler)
                    .add(getDoor(info.doorBlock, false, false, EnumFacing.WEST))
                    .add(getDoor(info.doorBlock, true, false, EnumFacing.WEST))
                    .add(filler);
            driver.current(x, height, 8)
                    .add(filler)
                    .add(getDoor(info.doorBlock, false, true, EnumFacing.WEST))
                    .add(getDoor(info.doorBlock, true, true, EnumFacing.WEST))
                    .add(filler);
        }
        if (info.hasConnectionAtZ(f + info.floorsBelowGround)) {
            int z = 0;
            if (hasConnectionWithBuilding(f, info, info.getZmin())) {
                driver.setBlockRange(6, height, z, height + 4, filler);
                driver.setBlockRange(9, height, z, height + 4, filler);
                driver.current(7, height, z).add(filler).add(airChar).add(airChar).add(filler);
                driver.current(8, height, z).add(filler).add(airChar).add(airChar).add(filler);
            } else if (hasConnectionToTopOrOutside(f, info, info.getZmin())) {
                driver.setBlockRange(6, height, z, height + 4, filler);
                driver.setBlockRange(9, height, z, height + 4, filler);
                driver.current(7, height, z)
                        .add(filler)
                        .add(getDoor(info.doorBlock, false, true, EnumFacing.NORTH))
                        .add(getDoor(info.doorBlock, true, true, EnumFacing.NORTH))
                        .add(filler);
                driver.current(8, height, z)
                        .add(filler)
                        .add(getDoor(info.doorBlock, false, false, EnumFacing.NORTH))
                        .add(getDoor(info.doorBlock, true, false, EnumFacing.NORTH))
                        .add(filler);
            }
        }
        if (hasConnectionWithBuildingMax(f, info, info.getZmax(), Orientation.Z)) {
            int z = 15;
            driver.setBlockRange(6, height, z, height + 4, filler);
            driver.setBlockRange(9, height, z, height + 4, filler);
            driver.current(7, height, z).add(filler).add(airChar).add(airChar).add(filler);
            driver.current(8, height, z).add(filler).add(airChar).add(airChar).add(filler);
        } else if (hasConnectionToTopOrOutside(f, info, info.getZmax()) && (info.getZmax().hasConnectionAtZFromStreet(f + info.getZmax().floorsBelowGround))) {
            int z = 15;
            driver.setBlockRange(6, height, z, height + 4, filler);
            driver.setBlockRange(9, height, z, height + 4, filler);
            driver.current(7, height, z)
                    .add(filler)
                    .add(getDoor(info.doorBlock, false, false, EnumFacing.SOUTH))
                    .add(getDoor(info.doorBlock, true, false, EnumFacing.SOUTH))
                    .add(filler);
            driver.current(8, height, z)
                    .add(filler)
                    .add(getDoor(info.doorBlock, false, true, EnumFacing.SOUTH))
                    .add(getDoor(info.doorBlock, true, true, EnumFacing.SOUTH))
                    .add(filler);
        }
    }

    private char getDoor(Block door, boolean upper, boolean left, EnumFacing facing) {
        IBlockState bs = door.getDefaultState()
                .withProperty(BlockDoor.HALF, upper ? BlockDoor.EnumDoorHalf.UPPER : BlockDoor.EnumDoorHalf.LOWER)
                .withProperty(BlockDoor.HINGE, left ? BlockDoor.EnumHingePosition.LEFT : BlockDoor.EnumHingePosition.RIGHT)
                .withProperty(BlockDoor.FACING, facing);
        return (char) Block.BLOCK_STATE_IDS.get(bs);
    }

    private void generateCorridorConnections(BuildingInfo info) {
        if (info.getXmin().hasXCorridor()) {
            int x = 0;
            for (int z = 7; z <= 10; z++) {
                driver.setBlockRange(x, info.groundLevel-5, z, info.groundLevel-2, airChar);
            }
        }
        if (info.getXmax().hasXCorridor()) {
            int x = 15;
            for (int z = 7; z <= 10; z++) {
                driver.setBlockRange(x, info.groundLevel-5, z, info.groundLevel-2, airChar);
            }
        }
        if (info.getZmin().hasXCorridor()) {
            int z = 0;
            for (int x = 7; x <= 10; x++) {
                driver.setBlockRange(x, info.groundLevel-5, z, info.groundLevel-2, airChar);
            }
        }
        if (info.getZmax().hasXCorridor()) {
            int z = 15;
            for (int x = 7; x <= 10; x++) {
                driver.setBlockRange(x, info.groundLevel-5, z, info.groundLevel-2, airChar);
            }
        }
    }


    private boolean hasConnectionWithBuildingMax(int localLevel, BuildingInfo info, BuildingInfo info2, Orientation x) {
        if (info.isValidFloor(localLevel) && info.getFloor(localLevel).getMetaBoolean("dontconnect")) {
            return false;
        }
        int globalLevel = info.localToGlobal(localLevel);
        int localAdjacent = info2.globalToLocal(globalLevel);
        if (info2.isValidFloor(localAdjacent) && info2.getFloor(localAdjacent).getMetaBoolean("dontconnect")) {
            return false;
        }
        int level = localAdjacent + info2.floorsBelowGround;
        return info2.hasBuilding && ((localAdjacent >= 0 && localAdjacent < info2.getNumFloors()) || (localAdjacent < 0 && (-localAdjacent) <= info2.floorsBelowGround)) && info2.hasConnectionAt(level, x);
    }

    private boolean hasConnectionToTopOrOutside(int localLevel, BuildingInfo info, BuildingInfo info2) {
        int globalLevel = info.localToGlobal(localLevel);
        int localAdjacent = info2.globalToLocal(globalLevel);
        if (info.getFloor(localLevel).getMetaBoolean("dontconnect")) {
            return false;
        }
        return (info2.isCity && !info2.hasBuilding && localLevel == 0 && localAdjacent == 0) || (info2.hasBuilding && localAdjacent == info2.getNumFloors());
//        return (!info2.hasBuilding && localLevel == localAdjacent) || (info2.hasBuilding && localAdjacent == info2.getNumFloors());
    }

    private boolean hasConnectionWithBuilding(int localLevel, BuildingInfo info, BuildingInfo info2) {
        int globalLevel = info.localToGlobal(localLevel);
        int localAdjacent = info2.globalToLocal(globalLevel);
        return info2.hasBuilding && ((localAdjacent >= 0 && localAdjacent < info2.getNumFloors()) || (localAdjacent < 0 && (-localAdjacent) <= info2.floorsBelowGround));
    }


    private boolean isSide(int x, int z) {
        return x == 0 || x == 15 || z == 0 || z == 15;
    }

    private boolean isCorner(int x, int z) {
        return (x == 0 || x == 15) && (z == 0 || z == 15);
    }
}
