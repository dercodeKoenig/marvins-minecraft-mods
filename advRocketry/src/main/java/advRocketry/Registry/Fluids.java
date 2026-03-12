package advRocketry.Registry;

import advRocketry.Fluid.Oxygen;
import advRocketry.Fluid.RocketFuel;
import advRocketry.Main;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class Fluids {
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, Main.MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Main.MODID);


    public static final Supplier<Fluid> ROCKET_FUEL = FLUIDS.register("rocket_fuel", () -> new RocketFuel());
    public static final Supplier<FluidType> ROCKET_FUEL_TYPE = FLUID_TYPES.register("rocket_fuel_type", () -> new FluidType(FluidType.Properties.create()));

    public static final Supplier<Fluid> OXYGEN = FLUIDS.register("oxygen", () -> new Oxygen());
    public static final Supplier<FluidType> OXYGEN_TYPE = FLUID_TYPES.register("oxygen_type", () -> new FluidType(FluidType.Properties.create()));

    public static void registerFluidTypes(RegisterClientExtensionsEvent event) {
        event.registerFluidType(
                new IClientFluidTypeExtensions() {
                    @Override
                    public int getTintColor() {
                        return 0xffffffff;
                    }

                    public ResourceLocation getStillTexture() {
                        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "block/fuel_still");
                    }

                    public ResourceLocation getFlowingTexture() {
                        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "block/fuel_flow");
                    }
                }, Fluids.ROCKET_FUEL_TYPE.get()
        );
        event.registerFluidType(
                new IClientFluidTypeExtensions() {
                    @Override
                    public int getTintColor() {
                        return 0xffffffff;
                    }

                    public ResourceLocation getStillTexture() {
                        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "block/oxygen_still");
                    }

                    public ResourceLocation getFlowingTexture() {
                        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "block/oxygen_flow");
                    }
                }, Fluids.OXYGEN_TYPE.get()
        );
    }
}
