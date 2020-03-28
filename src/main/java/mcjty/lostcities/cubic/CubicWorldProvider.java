package mcjty.lostcities.cubic;

import com.sun.istack.internal.NotNull;
import mcjty.lostcities.dimensions.world.LostCityChunkGenerator;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.gen.IChunkGenerator;

import javax.annotation.Nonnull;

// TODO: Add provider (Earth w/ Cities)
public class CubicWorldProvider extends WorldProvider {
    @Override
    // @NotNull
    public DimensionType getDimensionType() {
        return null;
    }

    @Override
    @Nonnull
    public IChunkGenerator createChunkGenerator() {
        //if(!CubicWorldPopulator.checkForCubicWorld(world)) return super.createChunkGenerator(); // Don't register any chunk generator, use the default
        //return new LostCityChunkGenerator(world, (world.getSeed() >> 3) ^ 34328884229L);
        return null;
    }
}
