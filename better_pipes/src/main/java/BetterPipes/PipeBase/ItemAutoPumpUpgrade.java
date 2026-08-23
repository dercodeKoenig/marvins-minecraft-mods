package BetterPipes.PipeBase;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ItemAutoPumpUpgrade extends Item {
    public ItemAutoPumpUpgrade() {
        super(new Properties().stacksTo(64));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        Direction face = ctx.getClickedFace();
        Player player = ctx.getPlayer();
        Level level = ctx.getLevel();

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof EntityPipe pipe)) {
            return InteractionResult.PASS;
        }

        // Client simply predicts success; the server owns the state change.
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        pipe.installAutoPumpUpgrade(face);

        if (player != null) {
            ctx.getItemInHand().shrink(1);
            player.displayClientMessage(Component.literal("Installed Automatic Pump on " + face.getName() + "."), true);
        }
        return InteractionResult.SUCCESS;
    }
}
