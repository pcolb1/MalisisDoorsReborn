package dev.mostlyharmless.malisisdoorsreborn.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmCargoDoorBlock;
import net.minecraft.client.Minecraft;
import dev.mostlyharmless.malisisdoorsreborn.hbm.blockentity.HbmCargoDoorBlockEntity;
import dev.mostlyharmless.malisisdoorsreborn.client.render.door.CustomDoorBreakingOverlay;
import dev.mostlyharmless.malisisdoorsreborn.core.MdrDefaults;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.state.level.CameraRenderState;
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

public class HbmCargoDoorBlockEntityRenderer implements BlockEntityRenderer<HbmCargoDoorBlockEntity, HbmCargoDoorBlockEntityRenderer.CargoDoorRenderState> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_cargo_door_skin_default");
    private static final Identifier BLOCK_ATLAS_ID = Identifier.withDefaultNamespace("blocks");
    private static final ObjModel MODEL = ObjModel.load("assets/malisisdoorsreborn/models/pheodoors/cargo_door.obj");
    private static final float FRAME_CLIP_MAX_Y = 2.9374F;

    @SuppressWarnings("unused")
    public HbmCargoDoorBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {}

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return MdrDefaults.LARGE_SPECIAL_DOOR_VIEW_DISTANCE;
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(@NotNull final HbmCargoDoorBlockEntity be) {
        final BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 4.0D, pos.getY(), pos.getZ() - 4.0D,
                pos.getX() + 4.0D, pos.getY() + 5.0D, pos.getZ() + 4.0D
        );
    }

    @Override
    public @NotNull CargoDoorRenderState createRenderState() {
        return new CargoDoorRenderState();
    }

    @Override
    public void extractRenderState(@NotNull final HbmCargoDoorBlockEntity be,
                                   @NotNull final CargoDoorRenderState renderState,
                                   final float partialTick,
                                   @NotNull final Vec3 cameraPos,
                                   @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(be, renderState, partialTick, cameraPos,
                CustomDoorBreakingOverlay.shouldRenderNormalDuringVanillaBreakingPass(crumblingOverlay, be.getBlockPos())
                        ? null
                        : crumblingOverlay);
        renderState.clear();

        final BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof HbmCargoDoorBlock)) return;
        if (state.getValue(HbmCargoDoorBlock.PART) != HbmCargoDoorBlock.HbmCargoDoorPart.ROOT) return;

        renderState.state = state;
        renderState.facing = state.getValue(HbmCargoDoorBlock.FACING);
        renderState.progress = Math.max(0.0F, Math.min(1.0F, be.getProgress(partialTick)));
        renderState.opening = be.isOpeningOrOpened();
        renderState.skinIndex = skinIndexForRender(state, be);
        renderState.packedLight = renderState.lightCoords;
    }

    @Override
    public void submit(@NotNull final CargoDoorRenderState renderState,
                       @NotNull final PoseStack poseStack,
                       @NotNull final SubmitNodeCollector collector,
                       @NotNull final CameraRenderState cameraState) {
        if (renderState.state == null || renderState.facing == null) return;
        submitModel(poseStack, collector, renderState.packedLight, 0, renderState.facing, renderState.progress, renderState.opening, renderState.skinIndex);
    }

    private static int skinIndexForRender(@NotNull final BlockState state, @NotNull final HbmCargoDoorBlockEntity be) {
        final int entitySkin = be.getSkinIndex();
        if (!state.hasProperty(HbmCargoDoorBlock.SKIN)) return entitySkin;
        final int stateSkin = state.getValue(HbmCargoDoorBlock.SKIN);
        return stateSkin != 0 || entitySkin == 0 ? stateSkin : entitySkin;
    }

    public static void submitItem(@NotNull final PoseStack poseStack,
                                  @NotNull final SubmitNodeCollector collector,
                                  final int packedLight,
                                  final int packedOverlay,
                                  final int skinIndex) {
        poseStack.pushPose();
        poseStack.translate(0.5F, -0.05F, 0.5F);
        poseStack.scale(0.26F, 0.26F, 0.26F);
        submitModel(poseStack, collector, packedLight, packedOverlay, Direction.SOUTH, 0.0F, false, skinIndex);
        poseStack.popPose();
    }

    @SuppressWarnings("resource")
    private static void submitModel(@NotNull final PoseStack poseStack,
                                    @NotNull final SubmitNodeCollector collector,
                                    final int packedLight,
                                    final int packedOverlay,
                                    @NotNull final Direction facing,
                                    final float progress,
                                    final boolean opening,
                                    @SuppressWarnings("unused") final int skinIndex) {
        final TextureAtlasSprite sprite = spriteFor();
        final float bottomRaise = progress * 2.0F;
        final float topRaise = opening
                ? Math.max(0.0F, progress * 2.0F - 1.0F)
                : Math.min(1.0F, progress * 2.0F);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.rotateDegrees(Axis.YP, yawFor(facing));

        collector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(), (pose, vertexConsumer) ->
                MODEL.frame.render(pose, vertexConsumer, sprite, packedLight, packedOverlay, VertexTransform.NONE));
        collector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(), (pose, vertexConsumer) ->
                MODEL.top.renderClippedMaxY(pose, vertexConsumer, sprite, packedLight, packedOverlay, topRaise, FRAME_CLIP_MAX_Y));
        collector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(), (pose, vertexConsumer) ->
                MODEL.bottom.renderClippedMaxY(pose, vertexConsumer, sprite, packedLight, packedOverlay, bottomRaise, FRAME_CLIP_MAX_Y));

        poseStack.popPose();
    }

    private static TextureAtlasSprite spriteFor() {
        return Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(BLOCK_ATLAS_ID)
                .getSprite(TEXTURE);
    }

    private static float yawFor(final Direction facing) {
        // HBM RenderDoorGeneric rotation before RenderContainmentDoor:
        // meta SOUTH -> 270°, EAST -> 0°, NORTH -> 90°, WEST -> 180°.
        return switch (facing) {
            case EAST -> 0.0F;
            case NORTH -> 90.0F;
            case WEST -> 180.0F;
            case SOUTH, UP, DOWN -> -90.0F;
        };
    }

    @SuppressWarnings("SameParameterValue")
    private record ObjModel(ObjGroup bottom, ObjGroup top, ObjGroup frame) {
        private static ObjModel load(final String path) {
            final List<float[]> positions = new ArrayList<>();
            final List<float[]> uvs = new ArrayList<>();
            final List<float[]> normals = new ArrayList<>();
            final List<ObjFace> bottom = new ArrayList<>();
            final List<ObjFace> top = new ArrayList<>();
            final List<ObjFace> frame = new ArrayList<>();
            List<ObjFace> current = null;

            try (InputStream input = HbmCargoDoorBlockEntityRenderer.class.getClassLoader().getResourceAsStream(path)) {
                if (input == null) return new ObjModel(ObjGroup.empty(), ObjGroup.empty(), ObjGroup.empty());
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        final String[] parts = line.split("\\s+");
                        switch (parts[0]) {
                            case "o", "g" -> current = groupFor(parts.length > 1 ? parts[1] : "", bottom, top, frame);
                            case "v" -> positions.add(new float[] {
                                    Float.parseFloat(parts[1]),
                                    Float.parseFloat(parts[2]),
                                    Float.parseFloat(parts[3])
                            });
                            case "vt" -> uvs.add(new float[] {
                                    Float.parseFloat(parts[1]),
                                    1.0F - Float.parseFloat(parts[2])
                            });
                            case "vn" -> normals.add(new float[] {
                                    Float.parseFloat(parts[1]),
                                    Float.parseFloat(parts[2]),
                                    Float.parseFloat(parts[3])
                            });
                            case "f" -> addFace(parts, positions, uvs, normals, current);
                            default -> {
                            }
                        }
                    }
                }
            } catch (IOException | RuntimeException ignored) {
                return new ObjModel(ObjGroup.empty(), ObjGroup.empty(), ObjGroup.empty());
            }

            return new ObjModel(new ObjGroup(bottom), new ObjGroup(top), new ObjGroup(frame));
        }

        private static List<ObjFace> groupFor(final String name,
                                              final List<ObjFace> bottom,
                                              final List<ObjFace> top,
                                              final List<ObjFace> frame) {
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "doorbot" -> bottom;
                case "doortop" -> top;
                case "frame" -> frame;
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
            for (int i = 1; i < parts.length; i++) {
                vertices[i - 1] = vertex(parts[i], positions, uvs, normals);
            }
            if (vertices.length <= 4) {
                current.add(new ObjFace(vertices));
                return;
            }
            for (int i = 1; i < vertices.length - 1; i++) {
                current.add(new ObjFace(new ObjVertex[] {vertices[0], vertices[i], vertices[i + 1]}));
            }
        }

        private static ObjVertex vertex(final String token,
                                        final List<float[]> positions,
                                        final List<float[]> uvs,
                                        final List<float[]> normals) {
            final String[] indices = token.split("/");
            final float[] position = positions.get(parseObjIndex(indices[0], positions.size()));
            float u = 0.0F;
            float v = 0.0F;
            if (indices.length > 1 && !indices[1].isEmpty()) {
                final float[] uv = uvs.get(parseObjIndex(indices[1], uvs.size()));
                u = uv[0];
                v = uv[1];
            }
            float nx = 0.0F;
            float ny = 1.0F;
            float nz = 0.0F;
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

        private ObjGroup(final List<ObjFace> faces) {
            this.faces = List.copyOf(faces);
        }

        @SuppressWarnings("SameParameterValue")
        private void render(final PoseStack.Pose pose,
                            final VertexConsumer vertexConsumer,
                            @Nullable final TextureAtlasSprite sprite,
                            final int packedLight,
                            final int packedOverlay,
                            final VertexTransform transform) {
            for (ObjFace face : faces) face.render(pose, vertexConsumer, sprite, packedLight, packedOverlay, transform);
        }

        @SuppressWarnings("SameParameterValue")
        private void renderClippedMaxY(final PoseStack.Pose pose,
                                       final VertexConsumer vertexConsumer,
                                       @Nullable final TextureAtlasSprite sprite,
                                       final int packedLight,
                                       final int packedOverlay,
                                       final float offsetY,
                                       final float maxY) {
            for (ObjFace face : faces) face.renderClippedMaxY(pose, vertexConsumer, sprite, packedLight, packedOverlay, offsetY, maxY);
        }
    }

    private record ObjFace(ObjVertex[] vertices) {
        private void render(final PoseStack.Pose pose,
                            final VertexConsumer vertexConsumer,
                            @Nullable final TextureAtlasSprite sprite,
                            final int packedLight,
                            final int packedOverlay,
                            final VertexTransform transform) {
            if (vertices.length == 3) {
                emit(vertices[0], pose, vertexConsumer, sprite, packedLight, packedOverlay, transform);
                emit(vertices[1], pose, vertexConsumer, sprite, packedLight, packedOverlay, transform);
                emit(vertices[2], pose, vertexConsumer, sprite, packedLight, packedOverlay, transform);
                emit(vertices[2], pose, vertexConsumer, sprite, packedLight, packedOverlay, transform);
                return;
            }
            for (ObjVertex vertex : vertices) emit(vertex, pose, vertexConsumer, sprite, packedLight, packedOverlay, transform);
        }

        private void renderClippedMaxY(final PoseStack.Pose pose,
                                       final VertexConsumer vertexConsumer,
                                       @Nullable final TextureAtlasSprite sprite,
                                       final int packedLight,
                                       final int packedOverlay,
                                       final float offsetY,
                                       final float maxY) {
            List<ObjVertex> clipped = new ArrayList<>(vertices.length);
            for (ObjVertex vertex : vertices) clipped.add(vertex.offsetY(offsetY));
            clipped = clipMaxY(clipped, maxY);
            if (clipped.size() < 3) return;

            final ObjVertex first = clipped.getFirst();
            for (int i = 1; i < clipped.size() - 1; i++) {
                final ObjVertex second = clipped.get(i);
                final ObjVertex third = clipped.get(i + 1);
                emit(first, pose, vertexConsumer, sprite, packedLight, packedOverlay, VertexTransform.NONE);
                emit(second, pose, vertexConsumer, sprite, packedLight, packedOverlay, VertexTransform.NONE);
                emit(third, pose, vertexConsumer, sprite, packedLight, packedOverlay, VertexTransform.NONE);
                emit(third, pose, vertexConsumer, sprite, packedLight, packedOverlay, VertexTransform.NONE);
            }
        }

        private static List<ObjVertex> clipMaxY(final List<ObjVertex> input, final float maxY) {
            if (input.isEmpty()) return input;

            final List<ObjVertex> output = new ArrayList<>(input.size() + 1);
            ObjVertex previous = input.getLast();
            boolean previousInside = previous.y <= maxY;

            for (ObjVertex current : input) {
                final boolean currentInside = current.y <= maxY;
                if (currentInside != previousInside) output.add(intersectY(previous, current, maxY));
                if (currentInside) output.add(current);
                previous = current;
                previousInside = currentInside;
            }

            return output;
        }

        private static ObjVertex intersectY(final ObjVertex start, final ObjVertex end, final float y) {
            final float denominator = end.y - start.y;
            final float t = denominator == 0.0F ? 0.0F : (y - start.y) / denominator;
            return start.lerp(end, Math.max(0.0F, Math.min(1.0F, t)));
        }

        private static void emit(final ObjVertex source,
                                 final PoseStack.Pose pose,
                                 final VertexConsumer vertexConsumer,
                                 @Nullable final TextureAtlasSprite sprite,
                                 final int packedLight,
                                 final int packedOverlay,
                                 final VertexTransform transform) {
            final MutableVertex vertex = new MutableVertex(source);
            transform.apply(vertex);
            final Matrix4f poseMatrix = pose.pose();
            vertexConsumer.addVertex(poseMatrix, vertex.x, vertex.y, vertex.z)
                    .setColor(255, 255, 255, 255)
                    .setUv(mappedU(sprite, vertex.u), mappedV(sprite, vertex.v))
                    .setOverlay(packedOverlay)
                    .setUv2(packedLight & 0xFFFF, packedLight >> 16)
                    .setNormal(pose, vertex.nx, vertex.ny, vertex.nz);
        }
    }

    private static float mappedU(@Nullable final TextureAtlasSprite sprite, final float u) {
        return sprite == null ? u : atlasU(sprite, u);
    }

    private static float mappedV(@Nullable final TextureAtlasSprite sprite, final float v) {
        return sprite == null ? v : atlasV(sprite, v);
    }

    private static float atlasU(final TextureAtlasSprite sprite, final float u) {
        return sprite.getU0() + ((sprite.getU1() - sprite.getU0()) * u);
    }

    private static float atlasV(final TextureAtlasSprite sprite, final float v) {
        return sprite.getV0() + ((sprite.getV1() - sprite.getV0()) * v);
    }

    @FunctionalInterface
    private interface VertexTransform {
        VertexTransform NONE = ignored -> {};

        void apply(MutableVertex vertex);
    }

    private static final class MutableVertex {
        private final float x;
        private final float y;
        private final float z;
        private final float u;
        private final float v;
        private final float nx;
        private final float ny;
        private final float nz;

        private MutableVertex(final ObjVertex source) {
            this.x = source.x;
            this.y = source.y;
            this.z = source.z;
            this.u = source.u;
            this.v = source.v;
            this.nx = source.nx;
            this.ny = source.ny;
            this.nz = source.nz;
        }
    }

    public static final class CargoDoorRenderState extends BlockEntityRenderState {
        private BlockState state;
        private Direction facing;
        private float progress;
        private boolean opening;
        private int skinIndex;
        private int packedLight;

        private void clear() {
            state = null;
            facing = null;
            progress = 0.0F;
            opening = false;
            skinIndex = 0;
            packedLight = 0;
        }
    }

    private record ObjVertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        private ObjVertex offsetY(final float offset) {
            return new ObjVertex(x, y + offset, z, u, v, nx, ny, nz);
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
