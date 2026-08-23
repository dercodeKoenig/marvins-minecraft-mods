package advRocketry.Registry;

import advRocketry.Main;
import advRocketry.Worldgen.Features.BigCrystalFeature;
import advRocketry.Worldgen.Features.CraterFeature;
import advRocketry.Worldgen.Features.VolcanoFeature;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class Features {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(BuiltInRegistries.FEATURE, Main.MODID);

    public static final Supplier<Feature<?>> BIG_CRYSTAL = FEATURES.register("big_crystal", () -> new BigCrystalFeature());
    public static final Supplier<Feature<?>> CRATER = FEATURES.register("crater", () -> new CraterFeature());
    public static final Supplier<Feature<?>> VOLCANO = FEATURES.register("volcano", () -> new VolcanoFeature());
}
