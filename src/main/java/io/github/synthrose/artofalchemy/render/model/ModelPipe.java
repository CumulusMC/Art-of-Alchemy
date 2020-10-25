package io.github.synthrose.artofalchemy.render.model;

import com.mojang.datafixers.util.Pair;
import io.github.synthrose.artofalchemy.blockentity.BlockEntityPipe;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.world.BlockRenderView;

public class ModelPipe implements UnbakedModel, BakedModel, FabricBakedModel {
	private static final int DIRECTION_COUNT = Direction.values().length;

	private static final SpriteIdentifier[] SPRITE_IDS = new SpriteIdentifier[] {
		new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("artofalchemy:block/essentia_pipe_core")),
		new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("artofalchemy:block/essentia_pipe_tube")),
		new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("artofalchemy:block/essentia_pipe_blocker")),
		new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("artofalchemy:block/essentia_pipe_sidecap")),
		new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("artofalchemy:block/essentia_pipe_endcap")),
	};

	private Sprite blockBreakSprite;

	private static final class FaceMeshes {
		private Mesh tube;
		private Mesh blocker;
		private Mesh passivePort;
		private Mesh inserterPort;
		private Mesh extractorPort;
	}

	private Mesh coreMesh;

	// Indexed via Direction
	private FaceMeshes[] faceMeshes = new FaceMeshes[DIRECTION_COUNT];

	// UnbakedModel
	@Override
	public Collection<Identifier> getModelDependencies() {
		return Collections.emptyList(); // This model does not depend on other models.
	}

	@Override
	public Collection<SpriteIdentifier> getTextureDependencies(Function<Identifier, UnbakedModel> unbakedModelGetter, Set<Pair<String, String>> unresolvedTextureReferences) {
		return Arrays.asList(SPRITE_IDS); // The textures this model (and all its model dependencies, and their dependencies, etc...!) depends on.
	}

	private static final class TexCoordEmitter {
		private final float minU;
		private final float maxU;
		private final float minV;
		private final float maxV;
		private int currentVertex = 0;

		// 'sx' stands for "sixteenth of a sprite texture area"
		public TexCoordEmitter(final Sprite sprite, final int sxMinU, final int sxMinV, final int sxMaxU, final int sxMaxV) {
			final float spriteMinU = sprite.getMinU();
			final float spriteMaxU = sprite.getMaxU();
			final float spriteMinV = sprite.getMinV();
			final float spriteMaxV = sprite.getMaxV();
			final float pxU = (spriteMaxU - spriteMinU) / 16.0f;
			final float pxV = (spriteMaxV - spriteMinV) / 16.0f;
			minU = spriteMinU + pxU * sxMinU;
			maxU = spriteMinU + pxU * sxMaxU;
			minV = spriteMinV + pxV * sxMinV;
			maxV = spriteMinV + pxV * sxMaxV;
		}

		private int nextVert() {
			int cur = currentVertex;
			currentVertex = (currentVertex+1) % 4;

			return cur;
		}

		public void emitLowerLeft(final QuadEmitter emitter) {
			emitter.sprite(nextVert(), 0, minU, minV);
		}

		public void emitUpperLeft(final QuadEmitter emitter) {
			emitter.sprite(nextVert(), 0, minU, maxV);
		}

		public void emitUpperRight(final QuadEmitter emitter) {
			emitter.sprite(nextVert(), 0, maxU, maxV);
		}

		public void emitLowerRight(final QuadEmitter emitter) {
			emitter.sprite(nextVert(), 0, maxU, minV);
		}

		public void finishEmit(final QuadEmitter emitter) {
			// Passing -1 as sprite color apparently activates texturing
			emitter.spriteColor(0, -1, -1, -1, -1);
		}

		public void emit(final QuadEmitter emitter) {
			// Start at origin (0, 0), and move in clockwise direction
			emitLowerLeft(emitter);
			emitUpperLeft(emitter);
			emitUpperRight(emitter);
			emitLowerRight(emitter);
			finishEmit(emitter);
		}
	}

	private static void squareSx(final QuadEmitter emitter, Direction nominalFace, float left, float bottom, float right, float top, float depth) {
		emitter.square(nominalFace, left / 16.0f, bottom / 16.0f, right / 16.0f, top / 16.0f, depth / 16.0f);
	}

	// Reuse scratch object to avoid allocations
	private static Vector4f scratchVector = new Vector4f();

	private static void emitPosSx(final QuadEmitter emitter, final Matrix4f transformation, final int i, final int x, final int y, final int z) {
		scratchVector.set(x / 16.0f, y / 16.0f, z / 16.0f, 1.0f);
		scratchVector.transform(transformation);
		emitter.pos(i, scratchVector.getX(), scratchVector.getY(), scratchVector.getZ());
	}

	private static final class RingMeshEmitter {
		private final Matrix4f transformation;
		private final int left;
		private final int bottom;
		private final int right;
		private final int top;

		public RingMeshEmitter(final Matrix4f transformation, final int left, final int bottom, final int right, final int top) {
			this.transformation = transformation;
			this.left = left;
			this.bottom = bottom;
			this.right = right;
			this.top = top;
		}

		public void emitFront(final QuadEmitter emitter) {
			emitPosSx(emitter, transformation, 0, left,  bottom, left);
			emitPosSx(emitter, transformation, 1, left,  top,	left);
			emitPosSx(emitter, transformation, 2, right, top,	left);
			emitPosSx(emitter, transformation, 3, right, bottom, left);
		}

		public void emitLeft(final QuadEmitter emitter) {
			emitPosSx(emitter, transformation, 0, left, bottom, right);
			emitPosSx(emitter, transformation, 1, left, top,	right);
			emitPosSx(emitter, transformation, 2, left, top,	 left);
			emitPosSx(emitter, transformation, 3, left, bottom,  left);
		}

		public void emitBack(final QuadEmitter emitter) {
			emitPosSx(emitter, transformation, 0, right, bottom, right);
			emitPosSx(emitter, transformation, 1, right, top,	right);
			emitPosSx(emitter, transformation, 2, left,  top,	right);
			emitPosSx(emitter, transformation, 3, left,  bottom, right);
		}

		public void emitRight(final QuadEmitter emitter) {
			emitPosSx(emitter, transformation, 0, right, bottom,  left);
			emitPosSx(emitter, transformation, 1, right, top,	 left);
			emitPosSx(emitter, transformation, 2, right, top,	right);
			emitPosSx(emitter, transformation, 3, right, bottom, right);
		}

		public void emit(final QuadEmitter emitter, final TexCoordEmitter texEmitter) {
			emitFront(emitter);
			texEmitter.emit(emitter);
			emitter.emit();

			emitLeft(emitter);
			texEmitter.emit(emitter);
			emitter.emit();

			emitBack(emitter);
			texEmitter.emit(emitter);
			emitter.emit();

			emitRight(emitter);
			texEmitter.emit(emitter);
			emitter.emit();
		}
	}

	private static void emitTubeMesh(final QuadEmitter emitter, final TexCoordEmitter texEmitter, final Matrix4f transformation, final int length) {
		final int l = 5 - length;
		(new RingMeshEmitter(transformation, 6, l, 10, 5)).emit(emitter, texEmitter);
	}

	private static void emitEndCapMesh(final QuadEmitter emitter, final TexCoordEmitter texEmitter, final Matrix4f transformation) {
		emitPosSx(emitter, transformation, 0,  4, 4,  4);
		emitPosSx(emitter, transformation, 1,  4, 4, 12);
		emitPosSx(emitter, transformation, 2, 12, 4, 12);
		emitPosSx(emitter, transformation, 3, 12, 4,  4);
		texEmitter.emit(emitter);
		emitter.emit();

		emitPosSx(emitter, transformation, 0,  4, 0,  4);
		emitPosSx(emitter, transformation, 1, 12, 0,  4);
		emitPosSx(emitter, transformation, 2, 12, 0, 12);
		emitPosSx(emitter, transformation, 3,  4, 0, 12);
		texEmitter.emit(emitter);
		emitter.emit();
	}

	private static void emitPortMesh(final QuadEmitter emitter, final TexCoordEmitter shortTubeTexEmitter, final TexCoordEmitter sideTexEmitter, final TexCoordEmitter endTexEmitter, final Matrix4f transformation) {
		// Short tube
		emitTubeMesh(emitter, shortTubeTexEmitter, transformation, 1);
		// Side
		(new RingMeshEmitter(transformation, 4, 0, 12, 4)).emit(emitter, sideTexEmitter);
		// End
		emitEndCapMesh(emitter, endTexEmitter, transformation);
	}

	private static void emitBlockerMesh(final QuadEmitter emitter, final Sprite sprite, final Matrix4f transformation) {
		// We have to build the ring here manually, since the texture for each face differs
		TexCoordEmitter texEmitter;
		final RingMeshEmitter ringMeshEmitter = new RingMeshEmitter(transformation, 5, 4, 11, 5);

		ringMeshEmitter.emitFront(emitter);
		texEmitter = new TexCoordEmitter(sprite, 5, 5, 11, 6);
		texEmitter.emitUpperRight(emitter);
		texEmitter.emitLowerRight(emitter);
		texEmitter.emitLowerLeft(emitter);
		texEmitter.emitUpperLeft(emitter);
		texEmitter.finishEmit(emitter);
		emitter.emit();

		ringMeshEmitter.emitLeft(emitter);
		texEmitter = new TexCoordEmitter(sprite, 10, 5, 11, 11);
		texEmitter.emitUpperLeft(emitter);
		texEmitter.emitUpperRight(emitter);
		texEmitter.emitLowerRight(emitter);
		texEmitter.emitLowerLeft(emitter);
		texEmitter.finishEmit(emitter);
		emitter.emit();

		ringMeshEmitter.emitBack(emitter);
		(new TexCoordEmitter(sprite, 5, 10, 11, 11)).emit(emitter);
		emitter.emit();

		ringMeshEmitter.emitRight(emitter);
		texEmitter = new TexCoordEmitter(sprite, 5, 5, 6, 11);
		texEmitter.emitLowerRight(emitter);
		texEmitter.emitLowerLeft(emitter);
		texEmitter.emitUpperLeft(emitter);
		texEmitter.emitUpperRight(emitter);
		texEmitter.finishEmit(emitter);
		emitter.emit();

		emitPosSx(emitter, transformation, 0, 11, 4,  5);
		emitPosSx(emitter, transformation, 1, 11, 4, 11);
		emitPosSx(emitter, transformation, 2,  5, 4, 11);
		emitPosSx(emitter, transformation, 3,  5, 4,  5);
		(new TexCoordEmitter(sprite, 5, 5, 11, 11)).emit(emitter);
		emitter.emit();
	}

	// Transformations that will rotate our base meshes into all 6 cardinal directions
	private static Matrix4f[] buildCardinalTransformations() {
		final Matrix4f[] matrices = new Matrix4f[DIRECTION_COUNT];

		final Matrix4f transform = new Matrix4f();
		transform.loadIdentity();
		matrices[Direction.DOWN.ordinal()] = new Matrix4f(transform);

		transform.multiply(Matrix4f.translate(0.5f, 0.5f, 0.5f));

		Matrix4f subTransform = new Matrix4f(transform);
		subTransform.multiply(new Quaternion(new Vector3f(0, 0, 1), 90, true));
		subTransform.multiply(Matrix4f.translate(-0.5f, -0.5f, -0.5f));
		matrices[Direction.EAST.ordinal()] = subTransform;

		subTransform = new Matrix4f(transform);
		subTransform.multiply(new Quaternion(new Vector3f(0, 0, 1), 180, true));
		subTransform.multiply(Matrix4f.translate(-0.5f, -0.5f, -0.5f));
		matrices[Direction.UP.ordinal()] = subTransform;

		subTransform = new Matrix4f(transform);
		subTransform.multiply(new Quaternion(new Vector3f(0, 0, 1), 270, true));
		subTransform.multiply(Matrix4f.translate(-0.5f, -0.5f, -0.5f));
		matrices[Direction.WEST.ordinal()] = subTransform;

		subTransform = new Matrix4f(transform);
		subTransform.multiply(new Quaternion(new Vector3f(1, 0, 0), 90, true));
		subTransform.multiply(Matrix4f.translate(-0.5f, -0.5f, -0.5f));
		matrices[Direction.NORTH.ordinal()] = subTransform;

		subTransform = new Matrix4f(transform);
		subTransform.multiply(new Quaternion(new Vector3f(1, 0, 0), 270, true));
		subTransform.multiply(Matrix4f.translate(-0.5f, -0.5f, -0.5f));
		matrices[Direction.SOUTH.ordinal()] = subTransform;

		return matrices;
	}

	@Override
	public BakedModel bake(ModelLoader loader, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId) {
		final Sprite coreSprite	= textureGetter.apply(SPRITE_IDS[0]);
		final Sprite tubeSprite	= textureGetter.apply(SPRITE_IDS[1]);
		final Sprite blockerSprite = textureGetter.apply(SPRITE_IDS[2]);
		final Sprite sideCapSprite = textureGetter.apply(SPRITE_IDS[3]);
		final Sprite endCapSprite  = textureGetter.apply(SPRITE_IDS[4]);

		blockBreakSprite = coreSprite;

		final Renderer renderer = RendererAccess.INSTANCE.getRenderer();
		final MeshBuilder builder = renderer.meshBuilder();
		final QuadEmitter emitter = builder.getEmitter();

		// Build core mesh
		final TexCoordEmitter coreTexEmitter = new TexCoordEmitter(coreSprite, 5, 5, 11, 11);

		for (final Direction dir : Direction.values()) {
			squareSx(emitter, dir, 5, 5, 11, 11, 5);
			coreTexEmitter.emit(emitter);
			emitter.emit();
		}

		coreMesh = builder.build();

		final Matrix4f[] cardTransforms = buildCardinalTransformations();
		final TexCoordEmitter tubeTexEmitter	  = new TexCoordEmitter(tubeSprite, 0, 0, 4, 5);
		final TexCoordEmitter shortTubeTexEmitter = new TexCoordEmitter(tubeSprite, 0, 0, 4, 1);

		final TexCoordEmitter passivePortSideCapTexEmitter   = new TexCoordEmitter(sideCapSprite, 0,  0, 8,  4);
		final TexCoordEmitter inserterPortSideCapTexEmitter  = new TexCoordEmitter(sideCapSprite, 0, 12, 8, 16);
		final TexCoordEmitter extractorPortSideCapTexEmitter = new TexCoordEmitter(sideCapSprite, 0,  8, 8, 12);

		final TexCoordEmitter passivePortEndCapTexEmitter   = new TexCoordEmitter(endCapSprite, 0, 0,  8,  8);
		final TexCoordEmitter inserterPortEndCapTexEmitter  = new TexCoordEmitter(endCapSprite, 8, 8, 16, 16);
		final TexCoordEmitter extractorPortEndCapTexEmitter = new TexCoordEmitter(endCapSprite, 0, 8,  8, 16);

		for (int i = 0; i < DIRECTION_COUNT; ++i) {
			final FaceMeshes meshes = new FaceMeshes();
			final Matrix4f transformation = cardTransforms[i];

			emitTubeMesh(emitter, tubeTexEmitter, transformation, 5);
			meshes.tube = builder.build();

			emitBlockerMesh(emitter, blockerSprite, transformation);
			meshes.blocker = builder.build();

			emitPortMesh(emitter, shortTubeTexEmitter, passivePortSideCapTexEmitter, passivePortEndCapTexEmitter, transformation);
			meshes.passivePort = builder.build();

			emitPortMesh(emitter, shortTubeTexEmitter, inserterPortSideCapTexEmitter, inserterPortEndCapTexEmitter, transformation);
			meshes.inserterPort = builder.build();

			emitPortMesh(emitter, shortTubeTexEmitter, extractorPortSideCapTexEmitter, extractorPortEndCapTexEmitter, transformation);
			meshes.extractorPort = builder.build();

			faceMeshes[i] = meshes;
		}

		return this;
	}

	// BakedModel
	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction face, Random random) {
		// Fixme: Provide a sane fallback eventually, in case a mod oblivious to frapi comes along.
		// Also see: https://github.com/Cumulus-Mods/Art-of-Alchemy/pull/1#pullrequestreview-473530254
		return null;
	}

	@Override
	public boolean useAmbientOcclusion() {
		return false;
	}

	@Override
	public boolean hasDepth() {
		return false;
	}

	@Override
	public boolean isSideLit() {
		return false;
	}

	@Override
	public boolean isBuiltin() {
		return false;
	}

	@Override
	public Sprite getSprite() {
		return blockBreakSprite;
	}

	@Override
	public ModelTransformation getTransformation() {
		return null;
	}

	@Override
	public ModelOverrideList getOverrides() {
		return null;
	}

	// FabricBakedModel
	@Override
	public boolean isVanillaAdapter() {
		return false; // False to trigger FabricBakedModel rendering
	}

	@Override
	public void emitBlockQuads(BlockRenderView blockRenderView, BlockState blockState, BlockPos blockPos, Supplier<Random> supplier, RenderContext renderContext) {
		renderContext.meshConsumer().accept(coreMesh);

		final BlockEntityPipe.IOFace[] faceConfig = BlockEntityPipe.getRenderAttachedFaceConfig(blockRenderView, blockPos);
		for (int i = 0; i < DIRECTION_COUNT; ++i) {
			final FaceMeshes meshes = faceMeshes[i];

			switch (faceConfig[i]) {
				case NONE:
				break;

				case CONNECT:
				renderContext.meshConsumer().accept(meshes.tube);
				break;

				case BLOCK:
				renderContext.meshConsumer().accept(meshes.blocker);
				break;

				case INSERTER:
				renderContext.meshConsumer().accept(meshes.inserterPort);
				break;

				case EXTRACTOR:
				renderContext.meshConsumer().accept(meshes.extractorPort);
				break;

				case PASSIVE:
				renderContext.meshConsumer().accept(meshes.passivePort);
				break;
			}
		}
	}

	@Override
	public void emitItemQuads(ItemStack itemStack, Supplier<Random> supplier, RenderContext renderContext) {

	}
}
