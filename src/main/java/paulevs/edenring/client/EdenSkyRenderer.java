package paulevs.edenring.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry.SkyRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import paulevs.edenring.EdenRing;
import paulevs.edenring.mixin.client.ProgramAccessor;
import paulevs.edenring.world.MoonInfo;
import ru.bclib.util.BackgroundInfo;
import ru.bclib.util.MHelper;

import java.util.Random;

public class EdenSkyRenderer implements SkyRenderer {
	private static final ResourceLocation PLANET_TEXTURE = EdenRing.makeID("textures/environment/planet.png");
	private static final ResourceLocation MOON_TEXTURE = EdenRing.makeID("textures/environment/moon.png");
	private static final ResourceLocation RINGS_SOFT_TEXTURE = EdenRing.makeID("textures/environment/rings_soft.png");
	private static final ResourceLocation RINGS_TEXTURE = EdenRing.makeID("textures/environment/rings.png");
	private static final ResourceLocation HORIZON_BW = EdenRing.makeID("textures/environment/horizon_bw.png");
	private static final ResourceLocation HORIZON = EdenRing.makeID("textures/environment/horizon.png");
	private static final ResourceLocation NEBULA1 = EdenRing.makeID("textures/environment/nebula_1.png");
	private static final ResourceLocation NEBULA2 = EdenRing.makeID("textures/environment/nebula_2.png");
	private static final ResourceLocation STARS = EdenRing.makeID("textures/environment/stars.png");
	private static final ResourceLocation SUN_FADE = EdenRing.makeID("textures/environment/sun_fade.png");
	private static final ResourceLocation SUN = EdenRing.makeID("textures/environment/sun.png");
	private static final MoonInfo[] MOONS = new MoonInfo[8];
	
	private static int dimensionUniform = 0;
	private static int lastProgram = 0;
	
	private static BufferBuilder bufferBuilder;
	private static VertexBuffer[] horizon;
	private static VertexBuffer[] nebula;
	private static VertexBuffer stars;
	private boolean shouldInit = true;
	
	private void init() {
		shouldInit = false;
		bufferBuilder = Tesselator.getInstance().getBuilder();
		
		if (horizon == null) {
			horizon = new VertexBuffer[3];
		}
		
		if (nebula == null) {
			nebula = new VertexBuffer[3];
		}
		
		horizon[0] = buildBufferHorizon(bufferBuilder, horizon[0], 20);
		horizon[1] = buildBufferHorizon(bufferBuilder, horizon[1], 40);
		horizon[2] = buildBufferHorizon(bufferBuilder, horizon[2], 100);
		
		nebula[0] = buildBufferHorizon(bufferBuilder, nebula[0], 30);
		nebula[1] = buildBufferStars(bufferBuilder, nebula[1], 20, 60, 10, 1, 235);
		nebula[2] = buildBufferStars(bufferBuilder, nebula[2], 20, 60, 10, 1, 352);
		
		stars = buildBufferStars(bufferBuilder, stars, 0.1, 0.7, 5000, 4, 41315);
		
		Random random = new Random(0);
		for (int i = 0; i < MOONS.length; i++) {
			MOONS[i] = new MoonInfo(random);
		}
	}
	
	@Override
	public void render(WorldRenderContext context) {
		int programID = GL30.glGetInteger(GL30.GL_CURRENT_PROGRAM);
		if (GL30.glIsProgram(programID) && GL30.glGetProgrami(programID, GL30.GL_LINK_STATUS) == GL30.GL_TRUE) {
			if (lastProgram != programID && dimensionUniform == 0) {
				lastProgram = programID;
				if (programID != 0) {
					dimensionUniform = Uniform.glGetUniformLocation(programID, "moddedDimension");
				}
			}
			else {
				dimensionUniform = 0;
			}
			
			if (dimensionUniform != 0) {
				Uniform.uploadInteger(dimensionUniform, 1);
			}
		}
		
		ClientLevel level = context.world();
		Matrix4f projectionMatrix = context.projectionMatrix();
		
		if (level == null || projectionMatrix == null) {
			return;
		}
		
		Minecraft minecraft = Minecraft.getInstance();
		PoseStack poseStack = context.matrixStack();
		float tickDelta = context.tickDelta();
		
		if (shouldInit) {
			init();
		}
		
		double time = (double) level.getGameTime() + tickDelta;
		double py = minecraft.player.position().y;
		float angle = Mth.clamp((float) (py - 128) * 0.0006F, -0.03F, 0.03F);
		float skyBlend = Mth.clamp((float) Math.abs(py - 128) * 0.006F, 0.0F, 1.0F);
		if (py < 0) {
			py = Mth.clamp(-py * 0.05, 0, 1);
		}
		else if (py < 256) {
			py = 0;
		}
		else {
			py = Mth.clamp((py - 256) * 0.05, 0, 1);
		}
		
		// Get Sky Color //
		
		Vec3 skyColor = level.getSkyColor(minecraft.gameRenderer.getMainCamera().getPosition(), tickDelta);
		float skyR = Mth.lerp(skyBlend, (float) skyColor.x * 0.5F, 0);
		float skyG = Mth.lerp(skyBlend, (float) skyColor.y * 0.5F, 0);
		float skyB = Mth.lerp(skyBlend, (float) skyColor.z * 0.5F, 0);
		
		// Init Setup //
		
		RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
		FogRenderer.setupNoFog();
		RenderSystem.depthMask(false);
		RenderSystem.disableBlend();
		RenderSystem.disableCull();
		RenderSystem.disableTexture();
		RenderSystem.disableDepthTest();
		
		// Render Background //
		
		RenderSystem.setShaderColor(skyR, skyG, skyB, 1.0F);
		RenderSystem.setShader(GameRenderer::getPositionShader);
		bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
		bufferBuilder.vertex(projectionMatrix, -10.0F, -10.0F, 0.0F).endVertex();
		bufferBuilder.vertex(projectionMatrix,  10.0F, -10.0F, 0.0F).endVertex();
		bufferBuilder.vertex(projectionMatrix,  10.0F,  10.0F, 0.0F).endVertex();
		bufferBuilder.vertex(projectionMatrix, -10.0F,  10.0F, 0.0F).endVertex();
		bufferBuilder.end();
		BufferUploader.end(bufferBuilder);
		
		// Render Nebula And Stars //
		
		float dayTime = level.getTimeOfDay(tickDelta);
		
		RenderSystem.enableTexture();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.enableBlend();
		
		poseStack.pushPose();
		poseStack.mulPose(Vector3f.XP.rotation(-0.4F));
		poseStack.mulPose(Vector3f.YP.rotation((float) Math.PI * 0.5F - dayTime * (float) Math.PI * 2.0F));
		
		RenderSystem.setShaderTexture(0, STARS);
		renderBuffer(poseStack, projectionMatrix, stars, DefaultVertexFormat.POSITION_TEX, 1.0F, 1.0F, 1.0F, skyBlend * 0.5F + 0.5F);
		
		float nebulaBlend = skyBlend * 0.75F + 0.25F;
		float nebulaBlend2 = nebulaBlend * 0.15F;
		RenderSystem.setShaderTexture(0, NEBULA1);
		renderBuffer(poseStack, projectionMatrix, nebula[1], DefaultVertexFormat.POSITION_TEX, 1.0F, 1.0F, 1.0F, nebulaBlend2);
		
		RenderSystem.setShaderTexture(0, NEBULA2);
		renderBuffer(poseStack, projectionMatrix, nebula[2], DefaultVertexFormat.POSITION_TEX, 1.0F, 1.0F, 1.0F, nebulaBlend2);
		
		RenderSystem.setShaderTexture(0, HORIZON);
		renderBuffer(poseStack, projectionMatrix, nebula[0], DefaultVertexFormat.POSITION_TEX, 1.0F, 1.0F, 1.0F, nebulaBlend);
		
		// Render Sun //
		
		poseStack.pushPose();
		poseStack.mulPose(Vector3f.ZP.rotation((float) Math.PI * 0.5F));
		Matrix4f matrix = poseStack.last().pose();
		
		RenderSystem.setShaderColor(skyR, skyG, skyB, 1.0F);
		RenderSystem.setShaderTexture(0, SUN_FADE);
		bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferBuilder.vertex(matrix, -80.0F, 100.0F, -80.0F).uv(0.0F, 0.0F).endVertex();
		bufferBuilder.vertex(matrix,  80.0F, 100.0F, -80.0F).uv(1.0F, 0.0F).endVertex();
		bufferBuilder.vertex(matrix,  80.0F, 100.0F,  80.0F).uv(1.0F, 1.0F).endVertex();
		bufferBuilder.vertex(matrix, -80.0F, 100.0F,  80.0F).uv(0.0F, 1.0F).endVertex();
		bufferBuilder.end();
		BufferUploader.end(bufferBuilder);
		
		float color = (float) Math.cos(dayTime * Math.PI * 2) * 1.1F;
		color = Mth.clamp(color, 0.3F, 1.0F);
		RenderSystem.setShaderColor(1.0F, color, color, 1.0F);
		RenderSystem.setShaderTexture(0, SUN);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferBuilder.vertex(matrix, -30.0F, 100.0F, -30.0F).uv(0.0F, 0.0F).endVertex();
		bufferBuilder.vertex(matrix,  30.0F, 100.0F, -30.0F).uv(1.0F, 0.0F).endVertex();
		bufferBuilder.vertex(matrix,  30.0F, 100.0F,  30.0F).uv(1.0F, 1.0F).endVertex();
		bufferBuilder.vertex(matrix, -30.0F, 100.0F,  30.0F).uv(0.0F, 1.0F).endVertex();
		bufferBuilder.end();
		BufferUploader.end(bufferBuilder);
		RenderSystem.defaultBlendFunc();
		
		poseStack.popPose();
		
		poseStack.popPose();
		
		// Setup Perspective //
		
		RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		
		// Render Rings //
		
		poseStack.pushPose();
		poseStack.translate(0, 0, -100);
		poseStack.mulPose(Vector3f.XP.rotation(angle));
		
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, RINGS_SOFT_TEXTURE);
		
		matrix = poseStack.last().pose();
		bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferBuilder.vertex(matrix, -130.0F, 0.0F, -130.0F).uv(0.0F, 0.0F).endVertex();
		bufferBuilder.vertex(matrix,  130.0F, 0.0F, -130.0F).uv(1.0F, 0.0F).endVertex();
		bufferBuilder.vertex(matrix,  130.0F, 0.0F,  130.0F).uv(1.0F, 1.0F).endVertex();
		bufferBuilder.vertex(matrix, -130.0F, 0.0F,  130.0F).uv(0.0F, 1.0F).endVertex();
		bufferBuilder.end();
		BufferUploader.end(bufferBuilder);
		
		if (py > 0) {
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, (float) py);
			RenderSystem.setShaderTexture(0, RINGS_TEXTURE);
			bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
			bufferBuilder.vertex(matrix, -130.0F, 0.01F, -130.0F).uv(0.0F, 0.0F).endVertex();
			bufferBuilder.vertex(matrix,  130.0F, 0.01F, -130.0F).uv(1.0F, 0.0F).endVertex();
			bufferBuilder.vertex(matrix,  130.0F, 0.01F,  130.0F).uv(1.0F, 1.0F).endVertex();
			bufferBuilder.vertex(matrix, -130.0F, 0.01F,  130.0F).uv(0.0F, 1.0F).endVertex();
			bufferBuilder.end();
			BufferUploader.end(bufferBuilder);
		}
		
		poseStack.popPose();
		
		// Render Moons //
		
		int frame = (int) (dayTime * 12 + 0.5F);
		float v0 = frame / 12F;
		float v1 = v0 + 0.083F;
		
		for (int i = 0; i < MOONS.length; i++) {
			MoonInfo moon = MOONS[i];
			double position = (moon.orbitState + dayTime) * moon.speed;
			renderMoon(poseStack, position, moon.orbitRadius, moon.orbitAngle, moon.size, v0, v1, moon.color);
		}
		
		// Render Planet //
		
		poseStack.pushPose();
		poseStack.translate(0, 0, -100);
		
		matrix = poseStack.last().pose();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, PLANET_TEXTURE);
		bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferBuilder.vertex(matrix, -140,  140, 0.0F).uv(0.0F, 0.0F).endVertex();
		bufferBuilder.vertex(matrix,  140,  140, 0.0F).uv(1.0F, 0.0F).endVertex();
		bufferBuilder.vertex(matrix,  140, -140, 0.0F).uv(1.0F, 1.0F).endVertex();
		bufferBuilder.vertex(matrix, -140, -140, 0.0F).uv(0.0F, 1.0F).endVertex();
		bufferBuilder.end();
		BufferUploader.end(bufferBuilder);
		
		poseStack.popPose();
		
		// Render Fog //
		
		if (py < 1) {
			RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
			RenderSystem.depthMask(false);
			RenderSystem.disableDepthTest();
			RenderSystem.setShaderTexture(0, HORIZON_BW);
			
			if (BackgroundInfo.fogDensity > 1) {
				float density = Mth.clamp(BackgroundInfo.fogDensity - 1.5F, 0, 1);
				poseStack.pushPose();
				poseStack.mulPose(Vector3f.YP.rotation((float) (-time * 0.00003)));
				renderBuffer(
					poseStack,
					projectionMatrix,
					horizon[1],
					DefaultVertexFormat.POSITION_TEX,
					BackgroundInfo.fogColorRed * 0.7F,
					BackgroundInfo.fogColorGreen * 0.7F,
					BackgroundInfo.fogColorBlue * 0.7F,
					(1.0F - skyBlend) * density
				);
				poseStack.popPose();
			}
			
			poseStack.pushPose();
			poseStack.mulPose(Vector3f.YP.rotation((float) (time * 0.0002)));
			float density = Mth.clamp(BackgroundInfo.fogDensity, 1, 2);
			renderBuffer(
				poseStack,
				projectionMatrix,
				horizon[0],
				DefaultVertexFormat.POSITION_TEX,
				BackgroundInfo.fogColorRed,
				BackgroundInfo.fogColorGreen,
				BackgroundInfo.fogColorBlue,
				(1.0F - skyBlend) * 0.5F * density
			);
			poseStack.popPose();
			
			poseStack.pushPose();
			poseStack.mulPose(Vector3f.YP.rotation((float) (-time * 0.0001)));
			renderBuffer(
				poseStack,
				projectionMatrix,
				horizon[1],
				DefaultVertexFormat.POSITION_TEX,
				BackgroundInfo.fogColorRed,
				BackgroundInfo.fogColorGreen,
				BackgroundInfo.fogColorBlue,
				(1.0F - skyBlend) * 0.5F
			);
			poseStack.popPose();
		}
		
		// Render Blindness //
		
		if (BackgroundInfo.blindness > 0) {
			RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
			RenderSystem.disableDepthTest();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableTexture();
			
			RenderSystem.setShaderColor(0, 0, 0, BackgroundInfo.blindness);
			RenderSystem.setShader(GameRenderer::getPositionShader);
			bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
			bufferBuilder.vertex(projectionMatrix, -10.0F, -10.0F, 0.0F).endVertex();
			bufferBuilder.vertex(projectionMatrix, 10.0F, -10.0F, 0.0F).endVertex();
			bufferBuilder.vertex(projectionMatrix, 10.0F, 10.0F, 0.0F).endVertex();
			bufferBuilder.vertex(projectionMatrix, -10.0F, 10.0F, 0.0F).endVertex();
			bufferBuilder.end();
			BufferUploader.end(bufferBuilder);
		}
		
		// Finalize //
		
		RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
		RenderSystem.enableTexture();
		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		RenderSystem.enableDepthTest();
		RenderSystem.enableCull();
	}
	
	private VertexBuffer buildBufferHorizon(BufferBuilder bufferBuilder, VertexBuffer buffer, double height) {
		if (buffer != null) {
			buffer.close();
		}
		
		buffer = new VertexBuffer();
		makeCylinder(bufferBuilder, 16, height, 100);
		bufferBuilder.end();
		buffer.upload(bufferBuilder);
		
		return buffer;
	}
	
	private void renderBuffer(PoseStack matrices, Matrix4f matrix4f, VertexBuffer buffer, VertexFormat format, float r, float g, float b, float a) {
		RenderSystem.setShaderColor(r, g, b, a);
		if (format == DefaultVertexFormat.POSITION) {
			buffer.drawWithShader(matrices.last().pose(), matrix4f, GameRenderer.getPositionShader());
		}
		else {
			buffer.drawWithShader(matrices.last().pose(), matrix4f, GameRenderer.getPositionTexShader());
		}
	}
	
	private void makeCylinder(BufferBuilder buffer, int segments, double height, double radius) {
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		for (int i = 0; i < segments; i++) {
			double a1 = (double) i * Math.PI * 2.0 / (double) segments;
			double a2 = (double) (i + 1) * Math.PI * 2.0 / (double) segments;
			double px1 = Math.sin(a1) * radius;
			double pz1 = Math.cos(a1) * radius;
			double px2 = Math.sin(a2) * radius;
			double pz2 = Math.cos(a2) * radius;
			
			float u0 = (float) i / (float) segments;
			float u1 = (float) (i + 1) / (float) segments;
			
			buffer.vertex(px1, -height, pz1).uv(u0, 0).endVertex();
			buffer.vertex(px1, height, pz1).uv(u0, 1).endVertex();
			buffer.vertex(px2, height, pz2).uv(u1, 1).endVertex();
			buffer.vertex(px2, -height, pz2).uv(u1, 0).endVertex();
		}
	}
	
	private VertexBuffer buildBufferStars(BufferBuilder bufferBuilder, VertexBuffer buffer, double minSize, double maxSize, int count, int verticalCount, long seed) {
		if (buffer != null) {
			buffer.close();
		}
		
		buffer = new VertexBuffer();
		makeStars(bufferBuilder, minSize, maxSize, count, verticalCount, seed);
		bufferBuilder.end();
		buffer.upload(bufferBuilder);
		
		return buffer;
	}
	
	private void makeStars(BufferBuilder buffer, double minSize, double maxSize, int count, int verticalCount, long seed) {
		Random random = new Random(seed);
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		
		for (int i = 0; i < count; ++i) {
			double posX = random.nextDouble() * 2.0 - 1.0;
			double posY = random.nextDouble() * 2.0 - 1.0;
			double posZ = random.nextDouble() * 2.0 - 1.0;
			double size = MHelper.randRange(minSize, maxSize, random);
			double length = posX * posX + posY * posY + posZ * posZ;
			if (length < 1.0 && length > 0.001) {
				length = 1.0 / Math.sqrt(length);
				posX *= length;
				posY *= length;
				posZ *= length;
				double j = posX * 100.0;
				double k = posY * 100.0;
				double l = posZ * 100.0;
				double m = Math.atan2(posX, posZ);
				double n = Math.sin(m);
				double o = Math.cos(m);
				double p = Math.atan2(Math.sqrt(posX * posX + posZ * posZ), posY);
				double q = Math.sin(p);
				double r = Math.cos(p);
				double s = random.nextDouble() * Math.PI * 2.0;
				double t = Math.sin(s);
				double u = Math.cos(s);
				
				int pos = 0;
				float minV = verticalCount < 2 ? 0 : (float) random.nextInt(verticalCount) / verticalCount;
				for (int v = 0; v < 4; ++v) {
					double x = (double) ((v & 2) - 1) * size;
					double y = (double) ((v + 1 & 2) - 1) * size;
					double aa = x * u - y * t;
					double ab = y * u + x * t;
					double ad = aa * q + 0.0 * r;
					double ae = 0.0 * q - aa * r;
					double af = ae * n - ab * o;
					double ah = ab * n + ae * o;
					float texU = (pos >> 1) & 1;
					float texV = (float) (((pos + 1) >> 1) & 1) / verticalCount + minV;
					pos++;
					buffer.vertex(j + af, k + ad, l + ah).uv(texU, texV).endVertex();
				}
			}
		}
	}
	
	private void renderMoon(PoseStack matrices, double orbitPosition, float orbitRadius, float orbitAngle, float size, float v0, float v1, Vector3f color) {
		float offset1 = (float) Math.sin(orbitPosition);
		float offset2 = (float) Math.cos(orbitPosition);
		
		matrices.pushPose();
		matrices.translate(offset1 * (70 + orbitRadius), offset1 * orbitAngle, offset2 * orbitRadius - 100);
		
		Matrix4f matrix = matrices.last().pose();
		RenderSystem.setShaderColor(color.x(), color.y(), color.z(), 1F);
		RenderSystem.setShaderTexture(0, MOON_TEXTURE);
		bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferBuilder.vertex(matrix, -size,  size, 0.0F).uv(0.0F, v0).endVertex();
		bufferBuilder.vertex(matrix,  size,  size, 0.0F).uv(1.0F, v0).endVertex();
		bufferBuilder.vertex(matrix,  size, -size, 0.0F).uv(1.0F, v1).endVertex();
		bufferBuilder.vertex(matrix, -size, -size, 0.0F).uv(0.0F, v1).endVertex();
		bufferBuilder.end();
		BufferUploader.end(bufferBuilder);
		
		matrices.popPose();
	}
}
