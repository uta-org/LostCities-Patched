package mcjty.lostcities.cubic.world.generators;

import mcjty.lostcities.config.LostCityConfiguration;
import mcjty.lostcities.dimensions.world.LostCityChunkGenerator;
import mcjty.lostcities.dimensions.world.driver.IPrimerDriver;
import mcjty.lostcities.dimensions.world.driver.OptimizedDriver;
import mcjty.lostcities.dimensions.world.driver.SafeDriver;
import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import mcjty.lostcities.dimensions.world.terraingen.CavernTerrainGenerator;
import mcjty.lostcities.dimensions.world.terraingen.IslandTerrainGenerator;
import mcjty.lostcities.dimensions.world.terraingen.SpaceTerrainGenerator;
import net.minecraft.world.gen.NoiseGeneratorPerlin;

import static mcjty.lostcities.cubic.world.CubicCityUtils.*;
import static mcjty.lostcities.cubic.world.CubicCityWorldProcessor.*;
import static mcjty.lostcities.cubic.world.CubicCityWorldPopulator.*;
import static mcjty.lostcities.cubic.world.generators.Utils.getRandomLeaf;

public class RuinsGenerator {
    private double[] ruinBuffer = new double[256];

    private NoiseGeneratorPerlin leavesNoise;
    private NoiseGeneratorPerlin ruinNoise;


    private double[] rubbleBuffer = new double[256];
    private double[] leavesBuffer = new double[256];

    private IPrimerDriver driver;
    private IslandTerrainGenerator islandTerrainGenerator = new IslandTerrainGenerator(IslandTerrainGenerator.ISLANDS);
    private CavernTerrainGenerator cavernTerrainGenerator = new CavernTerrainGenerator();
    private SpaceTerrainGenerator spaceTerrainGenerator = new SpaceTerrainGenerator();


    public RuinsGenerator() {
        this.leavesNoise = new NoiseGeneratorPerlin(provider.getRandom(), 4);
        this.ruinNoise = new NoiseGeneratorPerlin(provider.getRandom(), 4);
    }

    public void generate(BuildingInfo info) {
        if (info.ruinHeight < 0) {
            return;
        }

        int chunkX = info.chunkX;
        int chunkZ = info.chunkZ;
        double d0 = 0.03125D;
        this.ruinBuffer = this.ruinNoise.getRegion(this.ruinBuffer, (chunkX * 16), (chunkZ * 16), 16, 16, d0 * 2.0D, d0 * 2.0D, 1.0D);
        boolean doLeaves = info.profile.RUBBLELAYER;
        if (doLeaves) {
            this.leavesBuffer = this.leavesNoise.getRegion(this.leavesBuffer, (chunkX * 64), (chunkZ * 64), 16, 16, 1.0 / 64.0, 1.0 / 64.0, 4.0D);
        }

        int baseheight = (int) (info.getCityGroundLevel() + 1 + (info.ruinHeight * info.getNumFloors() * 6.0f));

        for (int x = 0; x < 16; ++x) {
            zLoop: for (int z = 0; z < 16; ++z) {
                double v = ruinBuffer[x + z * 16];
                int height = baseheight + (int) v;
                if (height == 0) continue; // Ruins need to sit on something, and the void isn't something
                driver.current(x, height, z);
                height = info.getMaxHeight() + 10 - height;
                int vl = 0;
                if (doLeaves) {
                    vl = (int) (info.profile.RUBBLE_LEAVE_SCALE < 0.01f ? 0 : leavesBuffer[x + z * 16] / info.profile.RUBBLE_LEAVE_SCALE);
                }
                while (height > 0) {
                    Character damage = info.getCompiledPalette().canBeDamagedToIronBars(driver.getBlock());
                    char c = driver.getBlockDown();
                    if ((damage != null || c == ironbarsChar) && c != airChar && c != liquidChar && provider.getRandom().nextFloat() < .2f) {
                        driver.add(ironbarsChar);
                    } else {
                        if (vl > 0) {
                            c = driver.getBlockDown();
                            while (c == airChar || c == liquidChar) {
                                driver.decY();
                                if (driver.getY() == 0) {
                                    // whoops, it's air all the way down. No ruins here
                                    continue zLoop;
                                }
                                height++;   // Make sure we keep on filling with air a bit longer because we are lowering here
                                c = driver.getBlockDown();
                            }
                            driver.add(getRandomLeaf());
                            vl--;
                        } else {
                            driver.add(airChar);
                        }
                    }
                    height--;
                }
            }
        }
    }

}
