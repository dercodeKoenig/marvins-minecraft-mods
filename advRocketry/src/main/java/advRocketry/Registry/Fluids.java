package advRocketry.Registry;

import advRocketry.Fluid.*;
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

    public static final Supplier<Fluid> HYDROGEN = FLUIDS.register("hydrogen", () -> new Hydrogen());
    public static final Supplier<FluidType> HYDROGEN_TYPE = FLUID_TYPES.register("hydrogen_type", () -> new FluidType(FluidType.Properties.create()));

    public static final Supplier<Fluid> NITROGEN = FLUIDS.register("nitrogen", () -> new Nitrogen());
    public static final Supplier<FluidType> NITROGEN_TYPE = FLUID_TYPES.register("nitrogen_type", () -> new FluidType(FluidType.Properties.create()));

    public static final Supplier<Fluid> METHANE = FLUIDS.register("methane", () -> new Methane());
    public static final Supplier<FluidType> METHANE_TYPE = FLUID_TYPES.register("methane_type", () -> new FluidType(FluidType.Properties.create()));

    public static final Supplier<Fluid> CO2 = FLUIDS.register("co2", () -> new Co2());
    public static final Supplier<FluidType> CO2_TYPE = FLUID_TYPES.register("co2_type", () -> new FluidType(FluidType.Properties.create()));

    public static void registerFluidTypes(RegisterClientExtensionsEvent event) {
        event.registerFluidType(
                new IClientFluidTypeExtensions() {
                    public int getTintColor() {
                        return 0xffffffff;
                    }

                    public ResourceLocation getStillTexture() {
                        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "block/fluid/fuel_still");
                    }

                    public ResourceLocation getFlowingTexture() {
                        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "block/fluid/fuel_flow");
                    }
                }, Fluids.ROCKET_FUEL_TYPE.get()
        );
        event.registerFluidType(
                new IClientFluidTypeExtensions() {
                    public int getTintColor() {
                        return 0xffffffff;
                    }

                    public ResourceLocation getStillTexture() {
                        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "block/fluid/oxygen_still");
                    }

                    public ResourceLocation getFlowingTexture() {
                        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "block/fluid/oxygen_flow");
                    }
                }, Fluids.OXYGEN_TYPE.get()
        );
        event.registerFluidType(
                new IClientFluidTypeExtensions() {
                    public int getTintColor() {
                        return 0xffffffff;
                    }

                    public ResourceLocation getStillTexture() {
                        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "block/fluid/hydrogen_still");
                    }

                    public ResourceLocation getFlowingTexture() {
                        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "block/fluid/hydrogen_flow");
                    }
                }, Fluids.HYDROGEN_TYPE.get()
        );
        event.registerFluidType(
                new IClientFluidTypeExtensions() {
                    public int getTintColor() {
                        return 0xffffffff;
                    }

                    public ResourceLocation getStillTexture() {
                        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "block/fluid/nitrogen_still");
                    }

                    public ResourceLocation getFlowingTexture() {
                        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "block/fluid/nitrogen_flow");
                    }
                }, Fluids.NITROGEN_TYPE.get()
        );

        event.registerFluidType(
                new IClientFluidTypeExtensions() {
                    public int getTintColor() {
                        return 0xffffffff;
                    }

                    public ResourceLocation getStillTexture() {
                        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "block/fluid/methane_still");
                    }

                    public ResourceLocation getFlowingTexture() {
                        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "block/fluid/methane_flow");
                    }
                }, Fluids.METHANE_TYPE.get()
        );
        event.registerFluidType(
                new IClientFluidTypeExtensions() {
                    public int getTintColor() {
                        return 0xffffffff;
                    }

                    public ResourceLocation getStillTexture() {
                        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "block/fluid/co2_still");
                    }

                    public ResourceLocation getFlowingTexture() {
                        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "block/fluid/co2_flow");
                    }
                }, Fluids.CO2_TYPE.get()
        );
    }
}
