package AgeOfSteam.Blocks.Mechanics.CrankShaft;

import AgeOfSteam.Main;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class RenderSmallWoodenCrankShaft extends RenderCrankShaftBase {
    public RenderSmallWoodenCrankShaft(BlockEntityRendererProvider.Context c) {
        super(c, ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/planks.png"), ResourceLocation.fromNamespaceAndPath(Main.MODID, "objmodels/small_crankshaft.obj"));
    }
}