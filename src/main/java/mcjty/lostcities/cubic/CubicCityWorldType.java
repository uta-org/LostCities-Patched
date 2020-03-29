package mcjty.lostcities.cubic;

import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import mcjty.lostcities.LostCities;
import mcjty.lostcities.LostCitiesDebug;
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

public class CubicCityWorldType extends WorldType implements ICubicWorldType {
    public CubicCityWorldType () { super("CityCubic"); }

    public static CubicCityWorldType create() { return new CubicCityWorldType(); }

    public ICubeGenerator createCubeGenerator(World world) {
        try {
            return new CubicCityWorldProcessor(world);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public BiomeProvider getBiomeProvider(World world) {
        try {
            return getEarthBiomeProvider(Biomes.FOREST, world);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
            return null;
        }
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
        try {
            mc.displayGuiScreen(getEarthGui(guiCreateWorld, mc));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
    }


    private static GuiScreen getEarthGui(GuiCreateWorld guiCreateWorld, Minecraft minecraft)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        if(LostCitiesDebug.debug) System.out.println("getEarthGui");
        Class<?> clazz = Class.forName("io.github.terra121.control.EarthGui");
        Constructor<?> constructor = clazz.getConstructor(GuiCreateWorld.class, Minecraft.class);
        return (GuiScreen) constructor.newInstance(guiCreateWorld, minecraft);
        // return (GuiScreen) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz }, new ClassFactory(instance));
    }

    private static BiomeProvider getEarthBiomeProvider(Biome biomeIn, World world)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        if(LostCitiesDebug.debug) System.out.println("getEarthBiomeProvider");
        Class<?> clazz = Class.forName("io.github.terra121.EarthBiomeProvider");
        Constructor<?> constructor = clazz.getConstructor(Biome.class, World.class);
        return (BiomeProvider) constructor.newInstance(biomeIn, world);
        // return (BiomeProvider) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz }, new ClassFactory(instance));
    }
}
