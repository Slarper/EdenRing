package paulevs.edenring.client.environment.weather;

import net.minecraft.core.BlockPos;
import paulevs.edenring.client.environment.animation.SpriteAnimation;
import ru.bclib.util.MHelper;

import java.util.Random;

public class LightningAnimation extends SpriteAnimation {
	private final int start;
	private final float scale;
	private byte frame;
	
	public LightningAnimation(BlockPos origin, Random random) {
		super(origin);
		this.start = random.nextInt(4096);
		this.scale = MHelper.randRange(30.0F, 50.0F, random);
	}
	
	@Override
	public void update(double time) {
		float state = (float) ((time + start) % 1000) / 6.0F;
		if (state > 1) {
			frame = -1;
			return;
		}
		frame = (byte) (state * 9.0F + 0.5F);
	}
	
	@Override
	public float getScale() {
		return scale;
	}
	
	@Override
	public float getAlpha() {
		return 1.0F;
	}
	
	@Override
	public byte getFrame() {
		return frame;
	}
	
	@Override
	public boolean useFogColor() {
		return false;
	}
	
	@Override
	public float getVSize() {
		return 0.1F;
	}
	
	@Override
	public boolean useSqrSqr() {
		return true;
	}
}
