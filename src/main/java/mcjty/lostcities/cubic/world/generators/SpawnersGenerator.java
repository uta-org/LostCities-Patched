package mcjty.lostcities.cubic.world.generators;

import mcjty.lostcities.LostCities;
import mcjty.lostcities.config.LostCityConfiguration;
import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.cubic.world.ICommonGeneratorProvider;
import mcjty.lostcities.dimensions.world.LostCityChunkGenerator;
import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import mcjty.lostcities.dimensions.world.lost.CitySphere;
import mcjty.lostcities.dimensions.world.lost.cityassets.AssetRegistries;
import mcjty.lostcities.dimensions.world.lost.cityassets.Condition;
import mcjty.lostcities.dimensions.world.lost.cityassets.ConditionContext;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Random;

import static mcjty.lostcities.dimensions.world.LostCityChunkGenerator.fixEntityId;

public class SpawnersGenerator {
    private LostCityProfile profile;
    private ICommonGeneratorProvider provider;

    private SpawnersGenerator() {}

    public SpawnersGenerator(LostCityProfile profile, ICommonGeneratorProvider provider) {
        this.profile = profile;
        this.provider = provider;
    }

    public void populate(Random rand, int chunkX, int chunkZ, World world) {
        generateLootSpawners(rand, chunkX, chunkZ, world, provider);
    }

    private void generateLootSpawners(Random random, int chunkX, int chunkZ, World world, ICommonGeneratorProvider chunkGenerator) {
        BuildingInfo info = BuildingInfo.getBuildingInfo(chunkX, chunkZ, chunkGenerator);

        for (Pair<BlockPos, BuildingInfo.ConditionTodo> pair : info.getMobSpawnerTodo()) {
            BlockPos pos = pair.getKey();
            // Double check that it is still a spawner (could be destroyed by explosion)
            if (world.getBlockState(pos).getBlock() == Blocks.MOB_SPAWNER) {
                TileEntity tileentity = world.getTileEntity(pos);
                if (tileentity instanceof TileEntityMobSpawner) {
                    TileEntityMobSpawner spawner = (TileEntityMobSpawner) tileentity;
                    BuildingInfo.ConditionTodo todo = pair.getValue();
                    String condition = todo.getCondition();
                    Condition cnd = AssetRegistries.CONDITIONS.get(condition);
                    if (cnd == null) {
                        throw new RuntimeException("Cannot find condition '" + condition + "'!");
                    }
                    int level = (pos.getY() - profile.GROUNDLEVEL) / 6;
                    int floor = (pos.getY() - info.getCityGroundLevel()) / 6;
                    ConditionContext conditionContext = new ConditionContext(level, floor, info.floorsBelowGround, info.getNumFloors(),
                            todo.getPart(), todo.getBuilding(), info.chunkX, info.chunkZ) {
                        @Override
                        public boolean isSphere() {
                            return false;
                        }

                        @Override
                        public String getBiome() {
                            return world.getBiome(pos).getBiomeName();
                        }
                    };
                    String randomValue = cnd.getRandomValue(random, conditionContext);
                    if (randomValue == null) {
                        throw new RuntimeException("Condition '" + cnd.getName() + "' did not return a valid mob!");
                    }
                    String fixedId = fixEntityId(randomValue);
                    MobSpawnerBaseLogic mobspawnerbaselogic = spawner.getSpawnerBaseLogic();
                    mobspawnerbaselogic.setEntityId(new ResourceLocation(fixedId));
                    spawner.markDirty();
                    if (LostCityConfiguration.DEBUG) {
                        LostCities.setup.getLogger().debug("generateLootSpawners: mob=" + randomValue + " pos=" + pos.toString());
                    }
                } else if(tileentity != null) {
                    LostCities.setup.getLogger().error("The mob spawner at (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ") has a TileEntity of incorrect type " + tileentity.getClass().getName() + "!");
                } else {
                    LostCities.setup.getLogger().error("The mob spawner at (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ") is missing its TileEntity!");
                }
            }
        }
        info.clearMobSpawnerTodo();


        for (Pair<BlockPos, BuildingInfo.ConditionTodo> pair : info.getLootTodo()) {
            BlockPos pos = pair.getKey();
            // Double check that it is still something that can hold loot (could be destroyed by explosion)
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityLockableLoot) {
                // TODO
                //if (chunkGenerator.profile.GENERATE_LOOT) {
                    createLoot(info, random, world, pos, pair.getRight());
                //}
            } else if (te == null) {
                IBlockState state = world.getBlockState(pos);
                Block block = state.getBlock();
                if (block.hasTileEntity(state)) {
                    LostCities.setup.getLogger().error("The block " + block.getRegistryName() + " (" + block.getClass().getName() + ") at (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ") is missing its TileEntity!");
                }
            }
        }
        info.clearLootTodo();


        for (BlockPos pos : info.getLightingUpdateTodo()) {
            IBlockState state = world.getBlockState(pos);
            world.setBlockState(pos, state, 3);
        }
        info.clearLightingUpdateTodo();
    }


    private void createLoot(BuildingInfo info, Random random, World world, BlockPos pos, BuildingInfo.ConditionTodo todo) {
        if (random.nextFloat() < profile.CHEST_WITHOUT_LOOT_CHANCE) {
            return;
        }
        TileEntity tileentity = world.getTileEntity(pos);
        if (tileentity instanceof TileEntityLockableLoot) {
            if (todo != null) {
                String lootTable = todo.getCondition();
                int level = (pos.getY() - profile.GROUNDLEVEL) / 6;
                int floor = (pos.getY() - info.getCityGroundLevel()) / 6;
                ConditionContext conditionContext = new ConditionContext(level, floor, info.floorsBelowGround, info.getNumFloors(),
                        todo.getPart(), todo.getBuilding(), info.chunkX, info.chunkZ) {
                    @Override
                    public boolean isSphere() {
                        return false;
                    }

                    @Override
                    public String getBiome() {
                        return world.getBiome(pos).getBiomeName();
                    }
                };
                String randomValue = AssetRegistries.CONDITIONS.get(lootTable).getRandomValue(random, conditionContext);
                if (randomValue == null) {
                    throw new RuntimeException("Condition '" + lootTable + "' did not return a table under certain conditions!");
                }
                ((TileEntityLockableLoot) tileentity).setLootTable(new ResourceLocation(randomValue), random.nextLong());
                tileentity.markDirty();
                if (LostCityConfiguration.DEBUG) {
                    LostCities.setup.getLogger().debug("createLootChest: loot=" + randomValue + " pos=" + pos.toString());
                }
            }
        }
    }
}
