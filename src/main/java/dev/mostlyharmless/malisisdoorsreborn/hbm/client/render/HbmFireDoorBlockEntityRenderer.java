package dev.mostlyharmless.malisisdoorsreborn.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmFireDoorBlock;
import net.minecraft.client.Minecraft;
import dev.mostlyharmless.malisisdoorsreborn.hbm.blockentity.HbmFireDoorBlockEntity;
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

public class HbmFireDoorBlockEntityRenderer implements BlockEntityRenderer<HbmFireDoorBlockEntity, HbmFireDoorBlockEntityRenderer.FireDoorRenderState> {

    private static final Identifier[] TEXTURES = {
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_fire_door_skin_default"),
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_fire_door_skin_black"),
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_fire_door_skin_orange"),
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_fire_door_skin_yellow"),
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_fire_door_skin_trefoil")
    };
    private static final Identifier BLOCK_ATLAS_ID = Identifier.withDefaultNamespace("blocks");
    private static final ObjModel MODEL = ObjModel.load("assets/malisisdoorsreborn/models/pheodoors/fire_door.obj");
    private static final float MAX_RAISE = 2.75F;
    private static final float MODEL_TOP_Y = 3.0F;
    private static final float FRAME_CLIP_MAX_Y = 2.99F;
    private static final float MODEL_EPSILON = 0.0001F;

    @SuppressWarnings("unused")
    public HbmFireDoorBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {}

    @Override
    public int getViewDistance() {
        return MdrDefaults.LARGE_SPECIAL_DOOR_VIEW_DISTANCE;
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(@NotNull final HbmFireDoorBlockEntity be) {
        final BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 4.0D, pos.getY(), pos.getZ() - 4.0D,
                pos.getX() + 4.0D, pos.getY() + 5.0D, pos.getZ() + 4.0D
        );
    }

    @Override
    public @NotNull FireDoorRenderState createRenderState() {
        return new FireDoorRenderState();
    }

    @Override
    public void extractRenderState(@NotNull final HbmFireDoorBlockEntity be,
                                   @NotNull final FireDoorRenderState renderState,
                                   final float partialTick,
                                   @NotNull final Vec3 cameraPos,
                                   @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(be, renderState, partialTick, cameraPos,
                CustomDoorBreakingOverlay.shouldRenderNormalDuringVanillaBreakingPass(crumblingOverlay, be.getBlockPos())
                        ? null
                        : crumblingOverlay);
        renderState.clear();

        final BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof HbmFireDoorBlock)) return;
        if (state.getValue(HbmFireDoorBlock.PART) != HbmFireDoorBlock.HbmFireDoorPart.ROOT) return;

        renderState.state = state;
        renderState.facing = state.getValue(HbmFireDoorBlock.FACING);
        renderState.raise = Math.max(0.0F, Math.min(MAX_RAISE, be.getProgress(partialTick) * MAX_RAISE));
        renderState.skinIndex = skinIndexForRender(state, be);
        renderState.packedLight = renderState.lightCoords;
    }

    @Override
    public void submit(@NotNull final FireDoorRenderState renderState,
                       @NotNull final PoseStack poseStack,
                       @NotNull final SubmitNodeCollector collector,
                       @NotNull final CameraRenderState cameraState) {
        if (renderState.state == null || renderState.facing == null) return;
        submitModel(poseStack, collector, renderState.packedLight, 0, renderState.facing, renderState.raise, renderState.skinIndex);
    }

    private static int skinIndexForRender(@NotNull final BlockState state, @NotNull final HbmFireDoorBlockEntity be) {
        final int entitySkin = be.getSkinIndex();
        if (!state.hasProperty(HbmFireDoorBlock.SKIN)) return entitySkin;
        final int stateSkin = state.getValue(HbmFireDoorBlock.SKIN);
        return stateSkin != 0 || entitySkin == 0 ? stateSkin : entitySkin;
    }

    public static void submitItem(@NotNull final PoseStack poseStack,
                                  @NotNull final SubmitNodeCollector collector,
                                  final int packedLight,
                                  final int packedOverlay,
                                  final int skinIndex) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.31F, 0.31F, 0.31F);
        poseStack.translate(-1.5F, 0.0F, -0.5F);
        submitModel(poseStack, collector, packedLight, packedOverlay, Direction.SOUTH, 0.0F, skinIndex);
        poseStack.popPose();
    }

    @SuppressWarnings("resource")
    private static void submitModel(@NotNull final PoseStack poseStack,
                                    @NotNull final SubmitNodeCollector collector,
                                    final int packedLight,
                                    final int packedOverlay,
                                    @NotNull final Direction facing,
                                    final float raise,
                                    final int skinIndex) {
        final TextureAtlasSprite sprite = spriteFor(skinIndex);

        poseStack.pushPose();
        applyHbmTransform(poseStack, facing);

        collector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(), (pose, vertexConsumer) ->
                MODEL.frame.render(pose, vertexConsumer, sprite, packedLight, packedOverlay, VertexTransform.NONE));

        if (MdrDefaults.FIRE_DOOR_CLIP_TO_FRAME && raise > 0.0F) {
            collector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(), (pose, vertexConsumer) ->
                    MODEL.staticTopCap.render(pose, vertexConsumer, sprite, packedLight, packedOverlay, VertexTransform.NONE));
            collector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(), (pose, vertexConsumer) ->
                    MODEL.door.renderClippedMaxY(pose, vertexConsumer, sprite, packedLight, packedOverlay, raise, FRAME_CLIP_MAX_Y));
        } else {
            poseStack.pushPose();
            poseStack.translate(0.0F, raise, 0.0F);
            collector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(), (pose, vertexConsumer) ->
                    MODEL.door.render(pose, vertexConsumer, sprite, packedLight, packedOverlay, VertexTransform.NONE));
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static void applyHbmTransform(@NotNull final PoseStack poseStack, @NotNull final Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawFor(facing)));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(-0.5F, 0.0F, 0.0F);
    }

    private static TextureAtlasSprite spriteFor(final int skinIndex) {
        return Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(BLOCK_ATLAS_ID)
                .getSprite(TEXTURES[Math.floorMod(skinIndex, TEXTURES.length)]);
    }

    private static float yawFor(final Direction facing) {
        // HBM RenderDoorGeneric rotation before RenderFireDoor's own fixed +90° rotation:
        // meta SOUTH -> 270°, EAST -> 0°, NORTH -> 90°, WEST -> 180°.
        return switch (facing) {
            case EAST -> 0.0F;
            case NORTH -> 90.0F;
            case WEST -> 180.0F;
            case SOUTH, UP, DOWN -> -90.0F;
        };
    }

    @SuppressWarnings("SameParameterValue")
    private record ObjModel(ObjGroup door, ObjGroup frame, ObjGroup staticTopCap) {
        private static ObjModel load(final String path) {
            final List<float[]> positions = new ArrayList<>();
            final List<float[]> uvs = new ArrayList<>();
            final List<float[]> normals = new ArrayList<>();
            final List<ObjFace> door = new ArrayList<>();
            final List<ObjFace> frame = new ArrayList<>();
            List<ObjFace> current = null;

            try (InputStream input = HbmFireDoorBlockEntityRenderer.class.getClassLoader().getResourceAsStream(path)) {
                if (input == null) return new ObjModel(ObjGroup.empty(), ObjGroup.empty(), ObjGroup.empty());
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        final String[] parts = line.split("\\s+");
                        switch (parts[0]) {
                            case "o", "g" -> current = groupFor(parts.length > 1 ? parts[1] : "", door, frame);
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

            final List<ObjFace> staticTopCap = new ArrayList<>();
            for (ObjFace face : door) {
                if (face.isHorizontalTopCap(MODEL_TOP_Y)) staticTopCap.add(face);
            }
            return new ObjModel(new ObjGroup(door), new ObjGroup(frame), new ObjGroup(staticTopCap));
        }

        private static List<ObjFace> groupFor(final String name,
                                              final List<ObjFace> door,
                                              final List<ObjFace> frame) {
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "door" -> door;
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

        @SuppressWarnings("SameParameterValue")
        private boolean isHorizontalTopCap(final float y) {
            for (ObjVertex vertex : vertices) {
                if (Math.abs(vertex.y - y) > MODEL_EPSILON) return false;
                if (vertex.ny < 0.9F) return false;
            }
            return true;
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

    public static final class FireDoorRenderState extends BlockEntityRenderState {
        private BlockState state;
        private Direction facing;
        private float raise;
        private int skinIndex;
        private int packedLight;

        private void clear() {
            state = null;
            facing = null;
            raise = 0.0F;
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
