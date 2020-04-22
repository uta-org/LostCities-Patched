package mcjty.lostcities.cubic.world;

import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.terra121.EarthBiomeProvider;
import io.github.terra121.control.EarthGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.Biomes;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class CubicCityWorldType extends WorldType implements ICubicWorldType {
    public CubicCityWorldType () { super("CityCubic"); }

    public static CubicCityWorldType create() { return new CubicCityWorldType(); }

    public ICubeGenerator createCubeGenerator(World world) {
        return new CubicCityWorldProcessor(world);
    }

    @Override
    public BiomeProvider getBiomeProvider(World world) {
        return getEarthBiomeProvider(Biomes.FOREST, world);
    }

    @Override public IntRange calculateGenerationHeightRange(WorldServer world) {
        return new IntRange(-12000, 9000);
    }

    @Override public boolean hasCubicGeneratorForWorld(World w) {
        return w.provider instanceof WorldProviderSurface; // an even more general way to check if it's overworld (need custom providers)
    }

    public boolean isCustomizable() {
        return true;
    }

    public float getCloudHeight()
    {
        return 5000;
    }

    public double voidFadeMagnitude() {
        return 0;
    }

    @SideOnly(Side.CLIENT)
    public void onCustomizeButton(Minecraft mc, GuiCreateWorld guiCreateWorld) {
        mc.displayGuiScreen(getEarthGui(guiCreateWorld, mc));
    }


    private static GuiScreen getEarthGui(GuiCreateWorld guiCreateWorld, Minecraft minecraft)
    {
        return new EarthGui(guiCreateWorld, minecraft);
    }

    private static BiomeProvider getEarthBiomeProvider(Biome biomeIn, World world)
    {
        return new EarthBiomeProvider(biomeIn, world);
    }
}
