package AgeOfSteam.Blocks.Mechanics.Axle;

import AgeOfSteam.Main;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class RenderWoodenAxle extends RenderAxle {

    public RenderWoodenAxle(BlockEntityRendererProvider.Context c) {
        super(c, ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/planks.png"));
    }
}