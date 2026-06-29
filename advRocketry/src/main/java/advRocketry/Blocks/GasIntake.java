package advRocketry.Blocks;

import advRocketry.Rocket.ICustomWeightBlock;
import net.minecraft.world.level.block.Block;

public class GasIntake extends Block implements ICustomWeightBlock {
    public GasIntake() {
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
