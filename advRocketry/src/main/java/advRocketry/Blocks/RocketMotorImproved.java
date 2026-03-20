package advRocketry.Blocks;

import advRocketry.Rocket.ICustomWeightBlock;
import net.minecraft.world.level.block.Block;

public class RocketMotorImproved extends RocketMotor implements ICustomWeightBlock {
    public RocketMotorImproved() {
        super();
    }

    public float getThrust() {
        return 15f;
    }

    public float getFuelRateMax() {
        return 150f;
    }

    @Override
    public float getWeightMultiplier() {
        return 5;
    }
}
