package advRocketry.Blocks;

import advRocketry.Rocket.ICustomWeightBlock;
import net.minecraft.world.level.block.Block;

public class Drill extends Block implements ICustomWeightBlock {
    public Drill() {
        super(Properties.of()
            .destroyTime(2.0f)
            .requiresCorrectToolForDrops()
        );
    }

    @Override
    public float getWeightMultiplier() {
        return 3;
    }
}
