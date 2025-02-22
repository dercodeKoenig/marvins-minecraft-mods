package NPCs.Blocks.Armory;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public class RenderArmory  implements BlockEntityRenderer<EntityArmory> {
    public RenderArmory(BlockEntityRendererProvider.Context c) {

    }

    public void render(EntityArmory tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
            BlockState state = tile.getBlockState();
            if (state.getBlock() instanceof BlockArmory) {
                Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                stack.translate(0.5F, 0.5F, 0.5F);

                if (facing == Direction.WEST) {
                    stack.mulPose((new Quaternionf()).fromAxisAngleDeg(0.0F, 1.0F, 0.0F, 270));
                }

                if (facing == Direction.EAST) {
                    stack.mulPose((new Quaternionf()).fromAxisAngleDeg(0.0F, 1.0F, 0.0F, 90.0F));
                }

                if (facing == Direction.SOUTH) {
                    stack.mulPose((new Quaternionf()).fromAxisAngleDeg(0.0F, 1.0F, 0.0F, 0.0F));
                }

                if (facing == Direction.NORTH) {
                    stack.mulPose((new Quaternionf()).fromAxisAngleDeg(0.0F, 1.0F, 0.0F, 180.0F));
                }

                List<List<Float>> translations = new ArrayList<>();
                translations.add(List.of(-0.2f,0.02f,0f, -80f, 0f));
                translations.add(List.of(0f,0.03f,0f, 0f, 0f));
                translations.add(List.of(0.2f,0.02f,0.1f, 0f, 0f));
                translations.add(List.of(0.2f,0.05f,-0.1f, 0f, 0f));
                translations.add(List.of(0.1f,0.06f,-0.15f, -20f, 0f));

                translations.add(List.of(-0.2f,0.0f-0.40f,0f, -60f, 0f));
                translations.add(List.of(0f,0.01f-0.40f,0f, -20f, 0f));
                translations.add(List.of(0.2f,0.00f-0.40f,0.1f, -50f, 0f));
                translations.add(List.of(0.2f,0.03f-0.40f,-0.2f, 0f, 0f));
                translations.add(List.of(0.1f,0.04f-0.40f,-0.25f, -20f, 0f));

                translations.add(List.of(-0.2f,0.0f+0.51f,0f, -60f, 0f));
                translations.add(List.of(0.3f,0.0f+0.52f,-0.16f, -20f, 0f));
                translations.add(List.of(0.15f,0.0f+0.53f,0.1f, 70f, 0f));
                translations.add(List.of(0.05f,0.0f+0.54f,-0.1f, -10f, 0f));;

                translations.add(List.of(-0.2f,0.3f+1.0f, 0.43f, 0f, -90f));
                translations.add(List.of(-0.15f,-0.1f+1.0f, 0.44f, -60f, -90f));
                translations.add(List.of(0.2f,0.3f+1.0f, 0.43f, 0f, -90f));
                translations.add(List.of(0.15f,-0.1f+1.0f, 0.44f, -60f, -90f));


                //long t0 = System.nanoTime();
                for(int i = 0; i < tile.inventory.getSlots(); ++i) {
                    if (i < translations.size()) {
                        ItemStack s = tile.inventory.getStackInSlot(i);
                        if(!s.isEmpty()) {
                            stack.pushPose();
                            stack.translate(translations.get(i).get(0), translations.get(i).get(1), translations.get(i).get(2));
                            stack.mulPose((new Quaternionf()).fromAxisAngleDeg(1.0F, 0.0F, 0.0F, translations.get(i).get(4) + 90f));
                            stack.mulPose((new Quaternionf()).fromAxisAngleDeg(0.0F, 0.0F, 1.0F, translations.get(i).get(3)));
                            float scale = 0.4F;
                            stack.scale(scale, scale, scale);
                            Minecraft.getInstance().getItemRenderer().renderStatic(s, ItemDisplayContext.FIXED, packedLight, packedOverlay, stack, bufferSource, (Level) null, 0);
                            stack.popPose();
                        }
                    }
                }
                //long t1 = System.nanoTime();
                //System.out.println((float)(t1-t0) / 1000 / 1000);
            }

        }
    }
