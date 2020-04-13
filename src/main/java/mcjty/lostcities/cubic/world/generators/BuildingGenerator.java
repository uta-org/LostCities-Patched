package mcjty.lostcities.cubic.world.generators;

import mcjty.lostcities.config.LostCityConfiguration;
import mcjty.lostcities.cubic.world.CubicCityWorldPopulator;
import mcjty.lostcities.cubic.world.ICommonHeightmap;
import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import mcjty.lostcities.dimensions.world.lost.Transform;
import mcjty.lostcities.dimensions.world.lost.cityassets.BuildingPart;
import mcjty.lostcities.dimensions.world.lost.cityassets.CompiledPalette;
import mcjty.lostcities.varia.ChunkCoord;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockSapling;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

import static mcjty.lostcities.cubic.world.CubicCityUtils.airChar;
import static mcjty.lostcities.cubic.world.CubicCityUtils.baseChar;
import static mcjty.lostcities.cubic.world.CubicCityWorldProcessor.driver;
import static mcjty.lostcities.cubic.world.generators.PartGenerator.generatePart;
import static mcjty.lostcities.cubic.world.generators.States.addStates;
import static mcjty.lostcities.cubic.world.generators.Utils.*;

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

    public void generate(BuildingInfo info, ICommonHeightmap heightmap) {
        int heightLevel = info.getCityGroundLevel(false);
        int lowestLevel = heightLevel - info.floorsBelowGround * 6;
        int highestLevel = heightLevel + info.getNumFloors() * 6;

        Character borderBlock = info.getCityStyle().getBorderBlock();
        CompiledPalette palette = info.getCompiledPalette();
        char fillerBlock = info.getBuilding().getFillerBlock();

        if (info.profile.isFloating()) {
            // For floating worldgen we try to fit the underside of the building better with the island
            // We also remove all blocks from the inside because we generate buildings on top of
            // generated chunks as opposed to blank chunks with non-floating worlds
            for (int x = 0; x < 16; ++x) {
                for (int z = 0; z < 16; ++z) {
                    // int index = (x << 12) | (z << 8);
                    int height = heightmap.getHeight(x, z);
                    if (height > 1 && height < lowestLevel - 1) {
                        driver.setBlockRange(x, height+1, z, lowestLevel, baseChar);
                    }
                    // Also clear the inside of buildings to avoid geometry that doesn't really belong there
                    clearRange(info, x, z, lowestLevel, heightLevel + info.getNumFloors() * 6, info.waterLevel > info.groundLevel);
                }
            }
        } else if (info.profile.isSpace()) {
            fillToGround(info, lowestLevel, borderBlock);

            // Also clear the inside of buildings to avoid geometry that doesn't really belong there
            for (int x = 0; x < 16; ++x) {
                for (int z = 0; z < 16; ++z) {
                    clearRange(info, x, z, lowestLevel, heightLevel + info.getNumFloors() * 6, false);     // Never water in bubbles?
                }
            }
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
                        // TODO
                        // driver.setBlockRange(x, lowestLevel - 10, z, highestLevel, baseChar);
                    }
                    // TODO
                    if (driver.getBlock(x, lowestLevel, z) == airChar) {
                        char filler = palette.get(fillerBlock);
                        driver.current(x, lowestLevel, z).block(filler); // There is nothing below so we fill this with the filler
                    }


                    if (info.profile.isCavern()) {
                        // Also clear the inside of buildings to avoid geometry that doesn't really belong there
//                        clearRange(primer, index, lowestLevel, info.getCityGroundLevel() + info.getNumFloors() * 6, waterLevel > mainGroundLevel);
                        clearRange(info, x, z, lowestLevel, heightLevel+ info.getNumFloors() * 6, info.waterLevel > info.groundLevel);
                    }
                }
            }
        }

        info.setCurrentFiller(fillerBlock);

        int height = lowestLevel;
        for (int f = -info.floorsBelowGround; f <= info.getNumFloors(); f++)
        {
            info.setCurrentFloor(f);

            BuildingPart part = info.getFloor(f);
            generatePart(info, part, Transform.ROTATE_NONE, 0, height, 0, false);

            part = info.getFloorPart2(f);
            if (part != null) {
                generatePart(info, part, Transform.ROTATE_NONE, 0, height, 0, false);
            }

            // Check for doors
            boolean isTop = f == info.getNumFloors();
            if(!isTop)
                info.addDoorTodo(
                        new DoorModel(
                                new ChunkCoord(
                                        CubicCityWorldPopulator.provider.getDimensionId(),
                                        driver.getLocalX() / 16,
                                        driver.getLocalZ() / 16), height, f));

            height += 6;    // We currently only support 6 here
        }

        info.disableCurrentFloor();
    }

    public class DoorModel {
        private ChunkCoord coord;
        private int height;
        private int floor;

        private DoorModel() {}

        public DoorModel(ChunkCoord coord, int height, int floor) {
            this.coord = coord;
            this.height = height;
            this.floor = floor;
        }

        public ChunkCoord getCoord() {
            return coord;
        }

        public int getHeight() {
            return height;
        }

        public int getFloor() {
            return floor;
        }
    }
}
