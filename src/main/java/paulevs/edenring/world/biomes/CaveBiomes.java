package paulevs.edenring.world.biomes;

import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import paulevs.edenring.EdenRing;
import ru.bclib.api.biomes.BCLBiomeBuilder;
import ru.bclib.api.surface.SurfaceRuleBuilder;
import ru.bclib.world.biomes.BCLBiome;

public class CaveBiomes {
	public static BCLBiome makeEmptyCaveBiome() {
		BCLBiomeBuilder builder = BCLBiomeBuilder.start(EdenRing.makeID("empty_cave"));
		BiomesCommonMethods.addDefaultLandFeatures(builder);
		BiomesCommonMethods.setDefaultColors(builder);
		BiomesCommonMethods.addDefaultSounds(builder);
		return builder.surface(Blocks.STONE).plantsColor(0x707c47).build();
	}
	
	public static BCLBiome makeErodedCaveBiome() {
		BCLBiomeBuilder builder = BCLBiomeBuilder.start(EdenRing.makeID("eroded_cave"));
		BiomesCommonMethods.addDefaultLandFeatures(builder);
		BiomesCommonMethods.setDefaultColors(builder);
		BiomesCommonMethods.addDefaultSounds(builder);
		builder.feature(Decoration.UNDERGROUND_DECORATION, CavePlacements.DRIPSTONE_CLUSTER);
		builder.feature(Decoration.UNDERGROUND_DECORATION, CavePlacements.LARGE_DRIPSTONE);
		builder.feature(Decoration.UNDERGROUND_DECORATION, CavePlacements.POINTED_DRIPSTONE);
		return builder
			.plantsColor(0x707c47)
			.surface(SurfaceRuleBuilder
				.start()
				.surface(Blocks.DRIPSTONE_BLOCK.defaultBlockState())
				.subsurface(Blocks.DRIPSTONE_BLOCK.defaultBlockState(), 3)
				.build()
			).build();
	}
}
