package advRocketry.Blocks;

import advRocketry.Rocket.ICustomWeightBlock;
import net.minecraft.world.level.block.Block;

public class GasIntake extends Block implements ICustomWeightBlock {
    public GasIntake() {
        super(Properties.of().destroyTime(0.5f));
    }

    @Override
    public float getWeightMultiplier() {
        return 3;
    }
}
