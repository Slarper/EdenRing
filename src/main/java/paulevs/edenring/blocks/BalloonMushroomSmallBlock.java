package paulevs.edenring.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import paulevs.edenring.registries.EdenBlocks;
import paulevs.edenring.registries.EdenFeatures;
import ru.bclib.blocks.FeatureSaplingBlock;

import java.util.Optional;
import java.util.Random;

public class BalloonMushroomSmallBlock extends FeatureSaplingBlock {
	private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 8, 12);
	
	public BalloonMushroomSmallBlock() {
		super((state) -> EdenFeatures.BALLOON_MUSHROOM_TREE.getFeature());
	}
	
	@Override
	public void randomTick(BlockState state, ServerLevel world, BlockPos pos, Random random) {}
	
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext ePos) {
		Vec3 vec3d = state.getOffset(view, pos);
		return SHAPE.move(vec3d.x, vec3d.y, vec3d.z);
	}
	
	@Override
	public OffsetType getOffsetType() {
		return OffsetType.XZ;
	}
	
	@Override
	protected boolean mayPlaceOn(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
		return blockState.is(EdenBlocks.EDEN_MYCELIUM);
	}
	
	@Override
	public void advanceTree(ServerLevel level, BlockPos pos, BlockState blockState, Random random) {
		BlockPos start = getStart(level, pos);
		if (start == null) super.advanceTree(level, pos, blockState, random);
		FeaturePlaceContext context = new FeaturePlaceContext(Optional.empty(), level, level.getChunkSource().getGenerator(), random, start, null);
		EdenFeatures.OLD_BALLOON_MUSHROOM_TREE.getFeature().place(context);
	}
	
	private BlockPos getStart(ServerLevel level, BlockPos pos) {
		MutableBlockPos p = pos.mutable();
		if (hasSquare(level, p)) return p;
		if (hasSquare(level, p.setX(pos.getX() - 1))) return p;
		if (hasSquare(level, p.setZ(pos.getZ() - 1))) return p;
		if (hasSquare(level, p.setX(pos.getX()))) return p;
		return null;
	}
	
	private boolean hasSquare(ServerLevel level, BlockPos pos) {
		MutableBlockPos p = pos.mutable();
		if (!level.getBlockState(p).is(this)) return false;
		if (!level.getBlockState(p.setX(pos.getX() + 1)).is(this)) return false;
		if (!level.getBlockState(p.setZ(pos.getZ() + 1)).is(this)) return false;
		if (!level.getBlockState(p.setX(pos.getX())).is(this)) return false;
		return true;
	}
}
