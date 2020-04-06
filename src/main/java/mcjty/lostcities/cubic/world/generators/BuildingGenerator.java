package mcjty.lostcities.cubic.world.generators;

import mcjty.lostcities.config.LostCityConfiguration;
import mcjty.lostcities.cubic.world.CubicCityWorldPopulator;
import mcjty.lostcities.cubic.world.ICommonHeightmap;
import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import mcjty.lostcities.dimensions.world.lost.Orientation;
import mcjty.lostcities.dimensions.world.lost.Transform;
import mcjty.lostcities.dimensions.world.lost.cityassets.BuildingPart;
import mcjty.lostcities.dimensions.world.lost.cityassets.CompiledPalette;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

import static mcjty.lostcities.cubic.world.generators.PartGenerator.generatePart;
import static mcjty.lostcities.cubic.world.generators.States.addStates;
import static mcjty.lostcities.cubic.world.generators.Utils.clearRange;
import static mcjty.lostcities.cubic.world.generators.Utils.isSide;

import static mcjty.lostcities.cubic.world.CubicCityWorldProcessor.driver;

import static mcjty.lostcities.cubic.world.CubeCityUtils.*;

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

    public void generate(BuildingInfo info, ICommonHeightmap heightmap) {
        int lowestLevel = info.getCityGroundLevel() - info.floorsBelowGround * 6;
        int highestLevel = info.getCityGroundLevel() + info.getNumFloors() * 6;

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
            // TODO
            /*fillToGround(info, lowestLevel, borderBlock);

            // Also clear the inside of buildings to avoid geometry that doesn't really belong there
            for (int x = 0; x < 16; ++x) {
                for (int z = 0; z < 16; ++z) {
                    clearRange(info, x, z, lowestLevel, info.getCityGroundLevel() + info.getNumFloors() * 6, false);     // Never water in bubbles?
                }
            }
            */
        } else {
            // For normal worldgen (non floating) or cavern we have a thin layer of 'border' blocks because that looks nicer
            for (int x = 0; x < 16; ++x) {
                for (int z = 0; z < 16; ++z) {
                    if (isSide(x, z)) {
                        driver.setBlockRange(x, lowestLevel - 10, z, highestLevel, palette.get(borderBlock));
                        /* // TODO
                        int y = lowestLevel - 10;
                        driver.current(x, y, z);
                        while (y < lowestLevel) {
                            driver.add(palette.get(borderBlock));
                            y++;
                        }
                        */
                    } else if (info.profile.isDefault()) {
                        driver.setBlockRange(x, info.profile.BEDROCK_LAYER, z, lowestLevel, baseChar);
                    }
                    // TODO
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
}
