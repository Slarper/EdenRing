package paulevs.edenring.blocks.complex;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MaterialColor;
import paulevs.edenring.EdenRing;
import ru.bclib.complexmaterials.WoodenComplexMaterial;

public class BrainTreeComplexMaterial extends WoodenComplexMaterial {
	public BrainTreeComplexMaterial(String baseName) {
		super(EdenRing.MOD_ID, baseName, "eden", MaterialColor.COLOR_LIGHT_GRAY, MaterialColor.COLOR_LIGHT_GRAY);
	}
	
	@Override
	protected FabricBlockSettings getBlockSettings() {
		return FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).sounds(SoundType.NETHERITE_BLOCK).mapColor(planksColor);
	}
}
