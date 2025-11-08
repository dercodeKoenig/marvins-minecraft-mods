package AgeOfSteam.Blocks.Mechanics.CrankShaft;

import AgeOfSteam.Main;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class RenderBigWoodenCrankShaft extends RenderCrankShaftBase {
    public RenderBigWoodenCrankShaft(BlockEntityRendererProvider.Context c) {
        super(c, ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/planks.png"), ResourceLocation.fromNamespaceAndPath(Main.MODID, "objmodels/big_crankshaft.obj"));
    }
}