package paulevs.edenring.world.biomes;

import paulevs.edenring.EdenRing;
import paulevs.edenring.registries.EdenEntities;
import paulevs.edenring.registries.EdenFeatures;
import paulevs.edenring.registries.EdenParticles;
import ru.bclib.api.biomes.BCLBiomeBuilder;
import ru.bclib.world.biomes.BCLBiome;

public class VoidBiomes {
	public static BCLBiome makeAirOcean() {
		BCLBiomeBuilder builder = BCLBiomeBuilder.start(EdenRing.makeID("air_ocean"));
		BiomesCommonMethods.addDefaultVoidFeatures(builder);
		BiomesCommonMethods.addDefaultSurface(builder);
		BiomesCommonMethods.setDefaultColors(builder);
		BiomesCommonMethods.addDefaultSounds(builder);
		builder.spawn(EdenEntities.DISKWING, 20, 3, 6);
		return builder.build();
	}
	
	public static BCLBiome makeSkyColony() {
		BCLBiomeBuilder builder = BCLBiomeBuilder.start(EdenRing.makeID("sky_colony"));
		BiomesCommonMethods.addDefaultVoidFeatures(builder);
		BiomesCommonMethods.addDefaultSurface(builder);
		BiomesCommonMethods.setDefaultColors(builder);
		BiomesCommonMethods.addDefaultSounds(builder);
		builder.spawn(EdenEntities.DISKWING, 20, 3, 6);
		return builder
			.fogColor(0x84d341)
			.waterColor(0x1e7d56)
			.plantsColor(0x1e7d56)
			.particles(EdenParticles.YOUNG_VOLVOX, 0.0001F)
			.feature(EdenFeatures.VOLVOX)
			.feature(EdenFeatures.PARIGNUM)
			.build();
	}
}
