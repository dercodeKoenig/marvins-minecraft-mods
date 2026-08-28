package advRocketry.Registry;

import advRocketry.Main;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class Fluids {
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, Main.MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Main.MODID);


    public static final Supplier<FluidType> ROCKET_FUEL_TYPE = FLUID_TYPES.register("rocket_fuel_type", () -> new FluidType(FluidType.Properties.create()));

    public static final Supplier<FluidType> OXYGEN_TYPE = FLUID_TYPES.register("oxygen_type", () -> new FluidType(FluidType.Properties.create().density(-100)));
    public static final Supplier<FluidType> HYDROGEN_TYPE = FLUID_TYPES.register("hydrogen_type", () -> new FluidType(FluidType.Properties.create().density(-100)));
    public static final Supplier<FluidType> NITROGEN_TYPE = FLUID_TYPES.register("nitrogen_type", () -> new FluidType(FluidType.Properties.create().density(-100)));
    public static final Supplier<FluidType> METHANE_TYPE = FLUID_TYPES.register("methane_type", () -> new FluidType(FluidType.Properties.create().density(-100)));
    public static final Supplier<FluidType> CO2_TYPE = FLUID_TYPES.register("co2_type", () -> new FluidType(FluidType.Properties.create().density(-100)));

    public static final Supplier<FluidType> ENRICHED_LAVA_TYPE = FLUID_TYPES.register("enriched_lava_type", () -> new FluidType(FluidType.Properties.create().density(3000).temperature(1300).viscosity(6000).lightLevel(15).pathType(PathType.LAVA).canSwim(false).canDrown(false)));

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
        event.registerFluidType(
                new IClientFluidTypeExtensions() {
                    public int getTintColor() {
                        return 0xffffffff;
                    }

                    public ResourceLocation getStillTexture() {
                        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "block/fluid/lava_still");
                    }

                    public ResourceLocation getFlowingTexture() {
                        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "block/fluid/lava_flow");
                    }
                }, Fluids.ENRICHED_LAVA_TYPE.get()
        );
    }

    // --- ROCKET FUEL ---
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> ROCKET_FUEL = FLUIDS.register("rocket_fuel",
            () -> new BaseFlowingFluid.Source(Fluids.ROCKET_FUEL_PROPERTIES) {
                protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluidIn, Direction direction) {
                    return false;
                }
            });
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> ROCKET_FUEL_FLOWING = FLUIDS.register("rocket_fuel_flowing",
            () -> new BaseFlowingFluid.Flowing(Fluids.ROCKET_FUEL_PROPERTIES));
    public static final BaseFlowingFluid.Properties ROCKET_FUEL_PROPERTIES = new BaseFlowingFluid.Properties(
            Fluids.ROCKET_FUEL_TYPE,
            ROCKET_FUEL,
            ROCKET_FUEL_FLOWING
    )
            .bucket(Items.ITEM_ROCKET_FUEL_BUCKET)
            .block(Blocks.ROCKET_FUEL_BLOCK);

    // --- METHANE ---
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> METHANE = FLUIDS.register("methane",
            () -> new BaseFlowingFluid.Source(Fluids.METHANE_PROPERTIES) {
                protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluidIn, Direction direction) {
                    return false;
                }
            });
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> METHANE_FLOWING = FLUIDS.register("methane_flowing",
            () -> new BaseFlowingFluid.Flowing(Fluids.METHANE_PROPERTIES));
    public static final BaseFlowingFluid.Properties METHANE_PROPERTIES = new BaseFlowingFluid.Properties(
            Fluids.METHANE_TYPE,
            METHANE,
            METHANE_FLOWING
    )
            .bucket(Items.ITEM_METHANE_BUCKET)
            .block(Blocks.METHANE_BLOCK);

    // --- CO2 ---
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> CO2 = FLUIDS.register("co2",
            () -> new BaseFlowingFluid.Source(Fluids.CO2_PROPERTIES) {
                protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluidIn, Direction direction) {
                    return false;
                }
            });
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> CO2_FLOWING = FLUIDS.register("co2_flowing",
            () -> new BaseFlowingFluid.Flowing(Fluids.CO2_PROPERTIES));
    public static final BaseFlowingFluid.Properties CO2_PROPERTIES = new BaseFlowingFluid.Properties(
            Fluids.CO2_TYPE,
            CO2,
            CO2_FLOWING
    )
            .bucket(Items.ITEM_CO2_BUCKET)
            .block(Blocks.CO2_BLOCK);

    // --- OXYGEN ---
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> OXYGEN = FLUIDS.register("oxygen",
            () -> new BaseFlowingFluid.Source(Fluids.OXYGEN_PROPERTIES) {
                protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluidIn, Direction direction) {
                    return false;
                }
            });
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> OXYGEN_FLOWING = FLUIDS.register("oxygen_flowing",
            () -> new BaseFlowingFluid.Flowing(Fluids.OXYGEN_PROPERTIES));

    public static final BaseFlowingFluid.Properties OXYGEN_PROPERTIES = new BaseFlowingFluid.Properties(
            Fluids.OXYGEN_TYPE, OXYGEN, OXYGEN_FLOWING)
            .bucket(Items.ITEM_OXYGEN_BUCKET)
            .block(Blocks.OXYGEN_BLOCK);

    // --- HYDROGEN ---
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> HYDROGEN = FLUIDS.register("hydrogen",
            () -> new BaseFlowingFluid.Source(Fluids.HYDROGEN_PROPERTIES) {
                protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluidIn, Direction direction) {
                    return false;
                }
            });
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> HYDROGEN_FLOWING = FLUIDS.register("hydrogen_flowing",
            () -> new BaseFlowingFluid.Flowing(Fluids.HYDROGEN_PROPERTIES));

    public static final BaseFlowingFluid.Properties HYDROGEN_PROPERTIES = new BaseFlowingFluid.Properties(
            Fluids.HYDROGEN_TYPE, HYDROGEN, HYDROGEN_FLOWING)
            .bucket(Items.ITEM_HYDROGEN_BUCKET)
            .block(Blocks.HYDROGEN_BLOCK);

    // --- NITROGEN ---
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> NITROGEN = FLUIDS.register("nitrogen",
            () -> new BaseFlowingFluid.Source(Fluids.NITROGEN_PROPERTIES) {
                protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluidIn, Direction direction) {
                    return false;
                }
            });
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> NITROGEN_FLOWING = FLUIDS.register("nitrogen_flowing",
            () -> new BaseFlowingFluid.Flowing(Fluids.NITROGEN_PROPERTIES));

    public static final BaseFlowingFluid.Properties NITROGEN_PROPERTIES = new BaseFlowingFluid.Properties(
            Fluids.NITROGEN_TYPE, NITROGEN, NITROGEN_FLOWING)
            .bucket(Items.ITEM_NITROGEN_BUCKET)
            .block(Blocks.NITROGEN_BLOCK);

    // --- ENRICHED LAVA ---
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> ENRICHED_LAVA = FLUIDS.register("enriched_lava",
            () -> new BaseFlowingFluid.Source(Fluids.ENRICHED_LAVA_PROPERTIES) {
                protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluidIn, Direction direction) {
                    return false;
                }
            });
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> ENRICHED_LAVA_FLOWING = FLUIDS.register("enriched_lava_flowing",
            () -> new BaseFlowingFluid.Flowing(Fluids.ENRICHED_LAVA_PROPERTIES));
    public static final BaseFlowingFluid.Properties ENRICHED_LAVA_PROPERTIES = new BaseFlowingFluid.Properties(
            Fluids.ENRICHED_LAVA_TYPE,
            ENRICHED_LAVA,
            ENRICHED_LAVA_FLOWING
    )
            .bucket(Items.ITEM_ENRICHED_LAVA_BUCKET)
            .block(Blocks.ENRICHED_LAVA_BLOCK)
            .tickRate(30);


}
