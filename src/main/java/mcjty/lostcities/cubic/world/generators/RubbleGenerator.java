package mcjty.lostcities.cubic.world.generators;

import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import net.minecraft.world.gen.NoiseGeneratorPerlin;

import static mcjty.lostcities.cubic.world.CubicCityWorldPopulator.*;

import static mcjty.lostcities.cubic.world.generators.Utils.*;

import static mcjty.lostcities.cubic.world.CubicCityWorldProcessor.driver;

public class RubbleGenerator {
    private double[] rubbleBuffer = new double[256];
    private double[] leavesBuffer = new double[256];

    private NoiseGeneratorPerlin rubbleNoise;
    private NoiseGeneratorPerlin leavesNoise;
    private NoiseGeneratorPerlin ruinNoise;


    public RubbleGenerator() {
        this.rubbleNoise = new NoiseGeneratorPerlin(provider.getRandom(), 4);
        this.leavesNoise = new NoiseGeneratorPerlin(provider.getRandom(), 4);
        this.ruinNoise = new NoiseGeneratorPerlin(provider.getRandom(), 4);
    }

    public void generateRubble(int chunkX, int chunkZ, BuildingInfo info) {
        this.rubbleBuffer = this.rubbleNoise.getRegion(this.rubbleBuffer, (chunkX * 16), (chunkZ * 16), 16, 16, 1.0 / 16.0, 1.0 / 16.0, 1.0D);
        this.leavesBuffer = this.leavesNoise.getRegion(this.leavesBuffer, (chunkX * 64), (chunkZ * 64), 16, 16, 1.0 / 64.0, 1.0 / 64.0, 4.0D);

        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                double vr = info.profile.RUBBLE_DIRT_SCALE < 0.01f ? 0 : rubbleBuffer[x + z * 16] / info.profile.RUBBLE_DIRT_SCALE;
                double vl = info.profile.RUBBLE_LEAVE_SCALE < 0.01f ? 0 : leavesBuffer[x + z * 16] / info.profile.RUBBLE_LEAVE_SCALE;
                if (vr > .5 || vl > .5) {
                    int height = getInterpolatedHeight(info, x, z);
                    driver.current(x, height, z);
                    if (height == 0) {
                        // whoops, it's air all the way down. No rubble here
                        continue;
                    }
                    char c = driver.getBlockDown();
                    if (c != airChar && c != liquidChar) {
                        for (int i = 0; i < vr; i++) {
                            if (driver.getBlock() == airChar || driver.getBlock() == liquidChar) {
                                driver.add(baseChar);
                            } else {
                                driver.incY();
                            }
                        }
                    }
                    if (driver.getBlockDown() == baseChar) {
                        for (int i = 0; i < vl; i++) {
                            if (driver.getBlock() == airChar || driver.getBlock() == liquidChar) {
                                driver.add(getRandomLeaf());
                            } else {
                                driver.incY();
                            }
                        }
                    }
                }
            }
        }
    }

}
