package dev.mostlyharmless.malisisdoorsreborn.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.mostlyharmless.malisisdoorsreborn.client.render.door.CustomDoorBreakingOverlay;
import dev.mostlyharmless.malisisdoorsreborn.core.MdrDefaults;
import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmRoundAirlockDoorBlock;
import dev.mostlyharmless.malisisdoorsreborn.hbm.blockentity.HbmRoundAirlockDoorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HbmRoundAirlockDoorBlockEntityRenderer implements BlockEntityRenderer<HbmRoundAirlockDoorBlockEntity, HbmRoundAirlockDoorBlockEntityRenderer.RoundAirlockDoorRenderState> {

    private static final Identifier[] TEXTURES = {
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_round_airlock_door_skin_default"),
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_round_airlock_door_skin_clean"),
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_round_airlock_door_skin_green")
    };
    private static final Identifier BLOCK_ATLAS_ID = Identifier.withDefaultNamespace("blocks");
    private static final ObjModel MODEL = ObjModel.load("assets/malisisdoorsreborn/models/pheodoors/airlock_door.obj");
    private static final float MAX_SLIDE = 1.5F;
    private static final float CLIP_MIN_Z = -2.0F;
    private static final float CLIP_MAX_Z = 2.0F;

    @SuppressWarnings("unused")
    public HbmRoundAirlockDoorBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {}

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return MdrDefaults.LARGE_SPECIAL_DOOR_VIEW_DISTANCE;
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(@NotNull final HbmRoundAirlockDoorBlockEntity be) {
        final BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 5.0D, pos.getY(), pos.getZ() - 5.0D,
                pos.getX() + 5.0D, pos.getY() + 9.0D, pos.getZ() + 5.0D
        );
    }

    @Override
    public @NotNull RoundAirlockDoorRenderState createRenderState() {
        return new RoundAirlockDoorRenderState();
    }

    @Override
    public void extractRenderState(@NotNull final HbmRoundAirlockDoorBlockEntity be,
                                   @NotNull final RoundAirlockDoorRenderState renderState,
                                   final float partialTick,
                                   @NotNull final Vec3 cameraPos,
                                   @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(be, renderState, partialTick, cameraPos,
                CustomDoorBreakingOverlay.shouldRenderNormalDuringVanillaBreakingPass(crumblingOverlay, be.getBlockPos())
                        ? null
                        : crumblingOverlay);
        renderState.clear();

        final BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof HbmRoundAirlockDoorBlock)) return;
        if (state.getValue(HbmRoundAirlockDoorBlock.PART) != HbmRoundAirlockDoorBlock.HbmRoundAirlockDoorPart.ROOT) return;

        renderState.state = state;
        renderState.facing = state.getValue(HbmRoundAirlockDoorBlock.FACING);
        renderState.slide = Math.max(0.0F, Math.min(MAX_SLIDE, be.getProgress(partialTick) * MAX_SLIDE));
        renderState.skinIndex = skinIndexForRender(state, be);
        renderState.packedLight = renderState.lightCoords;
    }

    @Override
    public void submit(@NotNull final RoundAirlockDoorRenderState renderState,
                       @NotNull final PoseStack poseStack,
                       @NotNull final SubmitNodeCollector collector,
                       @NotNull final CameraRenderState cameraState) {
        if (renderState.state == null || renderState.facing == null) return;
        submitModel(poseStack, collector, renderState.packedLight, 0, renderState.facing, renderState.slide, renderState.skinIndex, 0.5F);
    }

    private static int skinIndexForRender(@NotNull final BlockState state, @NotNull final HbmRoundAirlockDoorBlockEntity be) {
        final int entitySkin = be.getSkinIndex();
        if (!state.hasProperty(HbmRoundAirlockDoorBlock.SKIN)) return entitySkin;
        final int stateSkin = state.getValue(HbmRoundAirlockDoorBlock.SKIN);
        return stateSkin != 0 || entitySkin == 0 ? stateSkin : entitySkin;
    }

    public static void submitItem(@NotNull final PoseStack poseStack,
                                  @NotNull final SubmitNodeCollector collector,
                                  final int packedLight,
                                  final int packedOverlay,
                                  final int skinIndex) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.21F, 0.21F, 0.21F);
        submitModel(poseStack, collector, packedLight, packedOverlay, Direction.SOUTH, 0.0F, skinIndex, 0.0F);
        poseStack.popPose();
    }

    @SuppressWarnings("resource")
    private static void submitModel(@NotNull final PoseStack poseStack,
                                    @NotNull final SubmitNodeCollector collector,
                                    final int packedLight,
                                    final int packedOverlay,
                                    @NotNull final Direction facing,
                                    final float slide,
                                    final int skinIndex,
                                    final float footprintOffset) {
        final TextureAtlasSprite sprite = spriteFor(skinIndex);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawFor(facing)));
        poseStack.translate(0.0F, 0.0F, -footprintOffset);

        collector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(), (pose, vertexConsumer) ->
                MODEL.frame.render(pose, vertexConsumer, sprite, packedLight, packedOverlay));
        collector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(), (pose, vertexConsumer) ->
                MODEL.left.renderClippedZ(pose, vertexConsumer, sprite, packedLight, packedOverlay, slide, CLIP_MIN_Z, CLIP_MAX_Z));
        collector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(), (pose, vertexConsumer) ->
                MODEL.right.renderClippedZ(pose, vertexConsumer, sprite, packedLight, packedOverlay, -slide, CLIP_MIN_Z, CLIP_MAX_Z));

        poseStack.popPose();
    }

    private static TextureAtlasSprite spriteFor(final int skinIndex) {
        return Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(BLOCK_ATLAS_ID)
                .getSprite(TEXTURES[Math.floorMod(skinIndex, TEXTURES.length)]);
    }

    private static float yawFor(final Direction facing) {
        return switch (facing) {
            case EAST -> 0.0F;
            case NORTH -> 90.0F;
            case WEST -> 180.0F;
            case SOUTH, UP, DOWN -> -90.0F;
        };
    }

    @SuppressWarnings("SameParameterValue")
    private record ObjModel(ObjGroup frame, ObjGroup left, ObjGroup right) {
        private static ObjModel load(final String path) {
            final List<float[]> positions = new ArrayList<>();
            final List<float[]> uvs = new ArrayList<>();
            final List<float[]> normals = new ArrayList<>();
            final List<ObjFace> frame = new ArrayList<>();
            final List<ObjFace> left = new ArrayList<>();
            final List<ObjFace> right = new ArrayList<>();
            List<ObjFace> current = null;

            try (InputStream input = HbmRoundAirlockDoorBlockEntityRenderer.class.getClassLoader().getResourceAsStream(path)) {
                if (input == null) return new ObjModel(ObjGroup.empty(), ObjGroup.empty(), ObjGroup.empty());
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        final String[] parts = line.split("\\s+");
                        switch (parts[0]) {
                            case "o", "g" -> current = groupFor(parts.length > 1 ? parts[1] : "", frame, left, right);
                            case "v" -> positions.add(new float[] {Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])});
                            case "vt" -> uvs.add(new float[] {Float.parseFloat(parts[1]), 1.0F - Float.parseFloat(parts[2])});
                            case "vn" -> normals.add(new float[] {Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])});
                            case "f" -> addFace(parts, positions, uvs, normals, current);
                            default -> {}
                        }
                    }
                }
            } catch (IOException | RuntimeException ignored) {
                return new ObjModel(ObjGroup.empty(), ObjGroup.empty(), ObjGroup.empty());
            }

            return new ObjModel(new ObjGroup(frame), new ObjGroup(left), new ObjGroup(right));
        }

        private static List<ObjFace> groupFor(final String name,
                                              final List<ObjFace> frame,
                                              final List<ObjFace> left,
                                              final List<ObjFace> right) {
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "frame" -> frame;
                case "left" -> left;
                case "right" -> right;
                default -> null;
            };
        }

        private static void addFace(final String[] parts,
                                    final List<float[]> positions,
                                    final List<float[]> uvs,
                                    final List<float[]> normals,
                                    final List<ObjFace> current) {
            if (current == null || parts.length < 4) return;
            final ObjVertex[] vertices = new ObjVertex[parts.length - 1];
            for (int i = 1; i < parts.length; i++) vertices[i - 1] = vertex(parts[i], positions, uvs, normals);
            if (vertices.length <= 4) {
                current.add(new ObjFace(vertices));
                return;
            }
            for (int i = 1; i < vertices.length - 1; i++) current.add(new ObjFace(new ObjVertex[] {vertices[0], vertices[i], vertices[i + 1]}));
        }

        private static ObjVertex vertex(final String token,
                                        final List<float[]> positions,
                                        final List<float[]> uvs,
                                        final List<float[]> normals) {
            final String[] indices = token.split("/");
            final float[] position = positions.get(parseObjIndex(indices[0], positions.size()));
            float u = 0.0F;
            float v = 0.0F;
            float nx = 0.0F;
            float ny = 1.0F;
            float nz = 0.0F;
            if (indices.length > 1 && !indices[1].isEmpty()) {
                final float[] uv = uvs.get(parseObjIndex(indices[1], uvs.size()));
                u = uv[0];
                v = uv[1];
            }
            if (indices.length > 2 && !indices[2].isEmpty()) {
                final float[] normal = normals.get(parseObjIndex(indices[2], normals.size()));
                nx = normal[0];
                ny = normal[1];
                nz = normal[2];
            }
            return new ObjVertex(position[0], position[1], position[2], u, v, nx, ny, nz);
        }

        private static int parseObjIndex(final String value, final int size) {
            final int parsed = Integer.parseInt(value);
            return parsed < 0 ? size + parsed : parsed - 1;
        }
    }

    private record ObjGroup(List<ObjFace> faces) {
        private static ObjGroup empty() {
            return new ObjGroup(List.of());
        }

        private ObjGroup {
            faces = List.copyOf(faces);
        }

        private void render(final PoseStack.Pose pose,
                            final VertexConsumer vertexConsumer,
                            @Nullable final TextureAtlasSprite sprite,
                            final int packedLight,
                            final int packedOverlay) {
            for (ObjFace face : faces) face.render(pose, vertexConsumer, sprite, packedLight, packedOverlay);
        }

        @SuppressWarnings("SameParameterValue")
        private void renderClippedZ(final PoseStack.Pose pose,
                                   final VertexConsumer vertexConsumer,
                                   @Nullable final TextureAtlasSprite sprite,
                                   final int packedLight,
                                   final int packedOverlay,
                                   final float offsetZ,
                                   final float minZ,
                                   final float maxZ) {
            for (ObjFace face : faces) face.renderClippedZ(pose, vertexConsumer, sprite, packedLight, packedOverlay, offsetZ, minZ, maxZ);
        }
    }

    private record ObjFace(ObjVertex[] vertices) {
        private void render(final PoseStack.Pose pose,
                            final VertexConsumer vertexConsumer,
                            @Nullable final TextureAtlasSprite sprite,
                            final int packedLight,
                            final int packedOverlay) {
            if (vertices.length == 3) {
                emit(vertices[0], pose, vertexConsumer, sprite, packedLight, packedOverlay, false);
                emit(vertices[1], pose, vertexConsumer, sprite, packedLight, packedOverlay, false);
                emit(vertices[2], pose, vertexConsumer, sprite, packedLight, packedOverlay, false);
                emit(vertices[2], pose, vertexConsumer, sprite, packedLight, packedOverlay, false);

                emit(vertices[2], pose, vertexConsumer, sprite, packedLight, packedOverlay, true);
                emit(vertices[1], pose, vertexConsumer, sprite, packedLight, packedOverlay, true);
                emit(vertices[0], pose, vertexConsumer, sprite, packedLight, packedOverlay, true);
                emit(vertices[0], pose, vertexConsumer, sprite, packedLight, packedOverlay, true);
                return;
            }

            for (ObjVertex vertex : vertices) emit(vertex, pose, vertexConsumer, sprite, packedLight, packedOverlay, false);
            for (int i = vertices.length - 1; i >= 0; i--) {
                emit(vertices[i], pose, vertexConsumer, sprite, packedLight, packedOverlay, true);
            }
        }

        private void renderClippedZ(final PoseStack.Pose pose,
                                   final VertexConsumer vertexConsumer,
                                   @Nullable final TextureAtlasSprite sprite,
                                   final int packedLight,
                                   final int packedOverlay,
                                   final float offsetZ,
                                   final float minZ,
                                   final float maxZ) {
            List<ObjVertex> clipped = new ArrayList<>(vertices.length);
            for (ObjVertex vertex : vertices) clipped.add(vertex.offsetZ(offsetZ));
            clipped = clipMinZ(clipped, minZ);
            clipped = clipMaxZ(clipped, maxZ);
            if (clipped.size() < 3) return;
            final ObjVertex first = clipped.getFirst();
            for (int i = 1; i < clipped.size() - 1; i++) {
                final ObjVertex second = clipped.get(i);
                final ObjVertex third = clipped.get(i + 1);
                emit(first, pose, vertexConsumer, sprite, packedLight, packedOverlay, false);
                emit(second, pose, vertexConsumer, sprite, packedLight, packedOverlay, false);
                emit(third, pose, vertexConsumer, sprite, packedLight, packedOverlay, false);
                emit(third, pose, vertexConsumer, sprite, packedLight, packedOverlay, false);

                emit(third, pose, vertexConsumer, sprite, packedLight, packedOverlay, true);
                emit(second, pose, vertexConsumer, sprite, packedLight, packedOverlay, true);
                emit(first, pose, vertexConsumer, sprite, packedLight, packedOverlay, true);
                emit(first, pose, vertexConsumer, sprite, packedLight, packedOverlay, true);
            }
        }

        private static List<ObjVertex> clipMinZ(final List<ObjVertex> input, final float minZ) {
            final List<ObjVertex> output = new ArrayList<>();
            for (int i = 0; i < input.size(); i++) {
                final ObjVertex current = input.get(i);
                final ObjVertex previous = input.get((i + input.size() - 1) % input.size());
                final boolean currentInside = current.z >= minZ;
                final boolean previousInside = previous.z >= minZ;
                if (currentInside != previousInside) output.add(intersectZ(previous, current, minZ));
                if (currentInside) output.add(current);
            }
            return output;
        }

        private static List<ObjVertex> clipMaxZ(final List<ObjVertex> input, final float maxZ) {
            final List<ObjVertex> output = new ArrayList<>();
            for (int i = 0; i < input.size(); i++) {
                final ObjVertex current = input.get(i);
                final ObjVertex previous = input.get((i + input.size() - 1) % input.size());
                final boolean currentInside = current.z <= maxZ;
                final boolean previousInside = previous.z <= maxZ;
                if (currentInside != previousInside) output.add(intersectZ(previous, current, maxZ));
                if (currentInside) output.add(current);
            }
            return output;
        }

        private static ObjVertex intersectZ(final ObjVertex a, final ObjVertex b, final float z) {
            final float denominator = b.z - a.z;
            final float t = denominator == 0.0F ? 0.0F : (z - a.z) / denominator;
            return a.lerp(b, t).withZ(z);
        }

        private static void emit(final ObjVertex vertex,
                                 final PoseStack.Pose pose,
                                 final VertexConsumer vertexConsumer,
                                 @Nullable final TextureAtlasSprite sprite,
                                 final int packedLight,
                                 final int packedOverlay,
                                 final boolean reverseNormal) {
            final Matrix4f poseMatrix = pose.pose();
            final float normalScale = reverseNormal ? -1.0F : 1.0F;
            vertexConsumer.addVertex(poseMatrix, vertex.x, vertex.y, vertex.z)
                    .setColor(255, 255, 255, 255)
                    .setUv(mappedU(sprite, vertex.u), mappedV(sprite, vertex.v))
                    .setOverlay(packedOverlay)
                    .setUv2(packedLight & 0xFFFF, packedLight >> 16)
                    .setNormal(pose, vertex.nx * normalScale, vertex.ny * normalScale, vertex.nz * normalScale);
        }
    }

    private static float mappedU(@Nullable final TextureAtlasSprite sprite, final float u) {
        return sprite == null ? u : sprite.getU0() + ((sprite.getU1() - sprite.getU0()) * u);
    }

    private static float mappedV(@Nullable final TextureAtlasSprite sprite, final float v) {
        return sprite == null ? v : sprite.getV0() + ((sprite.getV1() - sprite.getV0()) * v);
    }

    public static final class RoundAirlockDoorRenderState extends BlockEntityRenderState {
        private BlockState state;
        private Direction facing;
        private float slide;
        private int skinIndex;
        private int packedLight;

        private void clear() {
            state = null;
            facing = null;
            slide = 0.0F;
            skinIndex = 0;
            packedLight = 0;
        }
    }

    private record ObjVertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        private ObjVertex offsetZ(final float offset) {
            return new ObjVertex(x, y, z + offset, u, v, nx, ny, nz);
        }

        private ObjVertex withZ(final float newZ) {
            return new ObjVertex(x, y, newZ, u, v, nx, ny, nz);
        }

        private ObjVertex lerp(final ObjVertex other, final float t) {
            return new ObjVertex(
                    x + (other.x - x) * t,
                    y + (other.y - y) * t,
                    z + (other.z - z) * t,
                    u + (other.u - u) * t,
                    v + (other.v - v) * t,
                    nx + (other.nx - nx) * t,
                    ny + (other.ny - ny) * t,
                    nz + (other.nz - nz) * t
            );
        }
    }
}
