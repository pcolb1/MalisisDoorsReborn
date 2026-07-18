package dev.mostlyharmless.malisisdoorsreborn.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.mostlyharmless.malisisdoorsreborn.client.render.door.CustomDoorBreakingOverlay;
import dev.mostlyharmless.malisisdoorsreborn.core.MdrDefaults;
import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmWaterDoorBlock;
import dev.mostlyharmless.malisisdoorsreborn.hbm.blockentity.HbmWaterDoorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
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

public class HbmWaterDoorBlockEntityRenderer implements BlockEntityRenderer<HbmWaterDoorBlockEntity, HbmWaterDoorBlockEntityRenderer.WaterDoorRenderState> {

    private static final Identifier[] TEXTURES = {
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_water_door_skin_default"),
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_water_door_skin_clean")
    };
    private static final Identifier BLOCK_ATLAS_ID = Identifier.withDefaultNamespace("blocks");
    private static final ObjModel MODEL = ObjModel.load("assets/malisisdoorsreborn/models/pheodoors/water_door.obj");
    private static final float MAX_ROTATION = 120.0F;

    @SuppressWarnings("unused")
    public HbmWaterDoorBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return MdrDefaults.LARGE_SPECIAL_DOOR_VIEW_DISTANCE;
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(@NotNull final HbmWaterDoorBlockEntity be) {
        final BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 4.0D, pos.getY(), pos.getZ() - 4.0D,
                pos.getX() + 4.0D, pos.getY() + 5.0D, pos.getZ() + 4.0D
        );
    }

    @Override
    public @NotNull WaterDoorRenderState createRenderState() {
        return new WaterDoorRenderState();
    }

    @Override
    public void extractRenderState(@NotNull final HbmWaterDoorBlockEntity be,
                                   @NotNull final WaterDoorRenderState renderState,
                                   final float partialTick,
                                   @NotNull final Vec3 cameraPos,
                                   @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(be, renderState, partialTick, cameraPos,
                CustomDoorBreakingOverlay.shouldRenderNormalDuringVanillaBreakingPass(crumblingOverlay, be.getBlockPos())
                        ? null
                        : crumblingOverlay);
        renderState.clear();

        final BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof HbmWaterDoorBlock)) return;
        if (state.getValue(HbmWaterDoorBlock.PART) != HbmWaterDoorBlock.HbmWaterDoorPart.ROOT) return;

        final float progress = Math.max(0.0F, Math.min(1.0F, be.getProgress(partialTick)));
        final boolean opening = be.isOpeningOrOpen();
        renderState.state = state;
        renderState.facing = state.getValue(HbmWaterDoorBlock.FACING);
        renderState.hinge = state.getValue(HbmWaterDoorBlock.HINGE);
        renderState.doorProgress = doorProgress(progress, opening);
        renderState.boltProgress = boltProgress(progress, opening);
        renderState.skinIndex = skinIndexForRender(state, be);
        renderState.packedLight = renderState.lightCoords;
    }

    @Override
    public void submit(@NotNull final WaterDoorRenderState renderState,
                       @NotNull final PoseStack poseStack,
                       @NotNull final SubmitNodeCollector collector,
                       @NotNull final CameraRenderState cameraState) {
        if (renderState.state == null || renderState.facing == null || renderState.hinge == null) return;
        submitModel(poseStack, collector, renderState.packedLight, 0, renderState.facing, renderState.hinge,
                renderState.doorProgress, renderState.boltProgress, renderState.skinIndex);
    }

    private static int skinIndexForRender(@NotNull final BlockState state,
                                          @NotNull final HbmWaterDoorBlockEntity be) {
        final int entitySkin = be.getSkinIndex();
        if (!state.hasProperty(HbmWaterDoorBlock.SKIN)) return entitySkin;
        final int stateSkin = state.getValue(HbmWaterDoorBlock.SKIN);
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
        submitModel(poseStack, collector, packedLight, packedOverlay, Direction.SOUTH, DoorHingeSide.LEFT, 0.0F, 0.0F, skinIndex);
        poseStack.popPose();
    }

    @SuppressWarnings({"resource", "SameParameterValue"})
    private static void submitModel(@NotNull final PoseStack poseStack,
                                    @NotNull final SubmitNodeCollector collector,
                                    final int packedLight,
                                    final int packedOverlay,
                                    @NotNull final Direction facing,
                                    @NotNull final DoorHingeSide hinge,
                                    final float doorProgress,
                                    final float boltProgress,
                                    final int skinIndex) {
        final TextureAtlasSprite sprite = spriteForSkin(skinIndex);
        final float rotation = doorProgress * MAX_ROTATION;
        final boolean mirrored = hinge == DoorHingeSide.RIGHT;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawFor(facing)));
        poseStack.translate(0.375F, 0.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));

        collector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(), (pose, vertexConsumer) ->
                MODEL.frame.render(pose, vertexConsumer, sprite, packedLight, packedOverlay, mirrored));

        poseStack.pushPose();
        if (mirrored) {
            poseStack.translate(1.1875F, 0.0F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
            poseStack.translate(-1.1875F, 0.0F, 0.0F);
        } else {
            poseStack.translate(-1.1875F, 0.0F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-rotation));
            poseStack.translate(1.1875F, 0.0F, 0.0F);
        }

        collector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(), (pose, vertexConsumer) ->
                MODEL.door.render(pose, vertexConsumer, sprite, packedLight, packedOverlay, mirrored));

        poseStack.pushPose();
        poseStack.translate((mirrored ? 0.4F : -0.4F) * boltProgress, 0.0F, 0.0F);
        collector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(), (pose, vertexConsumer) ->
                MODEL.bolts.render(pose, vertexConsumer, sprite, packedLight, packedOverlay, mirrored));
        poseStack.popPose();

        final float wheelPivotX = mirrored ? -0.40625F : 0.40625F;
        final float wheelRotation = (mirrored ? -1.0F : 1.0F) * boltProgress * 360.0F;

        poseStack.pushPose();
        poseStack.translate(wheelPivotX, 2.28125F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(wheelRotation));
        poseStack.translate(-wheelPivotX, -2.28125F, 0.0F);
        collector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(), (pose, vertexConsumer) ->
                MODEL.top.render(pose, vertexConsumer, sprite, packedLight, packedOverlay, mirrored));
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(wheelPivotX, 0.71875F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(wheelRotation));
        poseStack.translate(-wheelPivotX, -0.71875F, 0.0F);
        collector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(), (pose, vertexConsumer) ->
                MODEL.bottom.render(pose, vertexConsumer, sprite, packedLight, packedOverlay, mirrored));
        poseStack.popPose();

        poseStack.popPose();
        poseStack.popPose();
    }

    private static TextureAtlasSprite spriteForSkin(final int skinIndex) {
        return Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(BLOCK_ATLAS_ID)
                .getSprite(TEXTURES[Math.floorMod(skinIndex, TEXTURES.length)]);
    }

    private static float doorProgress(final float progress, final boolean opening) {
        if (opening) {
            return sinFull(clamp01((progress - 0.5F) / 0.5F));
        }

        final float elapsed = 1.0F - progress;
        return 1.0F - sinFull(clamp01(elapsed / 0.5F));
    }

    private static float boltProgress(final float progress, final boolean opening) {
        if (opening) {
            return sinFull(clamp01(progress / 0.5F));
        }

        final float elapsed = 1.0F - progress;
        return 1.0F - sinFull(clamp01((elapsed - 0.5F) / 0.5F));
    }

    private static float sinFull(final float value) {
        return 0.5F - 0.5F * (float) Math.cos(Math.PI * value);
    }

    private static float clamp01(final float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
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
    private record ObjModel(ObjGroup bottom, ObjGroup top, ObjGroup door, ObjGroup bolts, ObjGroup frame) {
        private static ObjModel load(final String path) {
            final List<float[]> positions = new ArrayList<>();
            final List<float[]> uvs = new ArrayList<>();
            final List<float[]> normals = new ArrayList<>();
            final List<ObjFace> bottom = new ArrayList<>();
            final List<ObjFace> top = new ArrayList<>();
            final List<ObjFace> door = new ArrayList<>();
            final List<ObjFace> bolts = new ArrayList<>();
            final List<ObjFace> frame = new ArrayList<>();
            List<ObjFace> current = null;

            try (InputStream input = HbmWaterDoorBlockEntityRenderer.class.getClassLoader().getResourceAsStream(path)) {
                if (input == null) return new ObjModel(ObjGroup.empty(), ObjGroup.empty(), ObjGroup.empty(), ObjGroup.empty(), ObjGroup.empty());
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        final String[] parts = line.split("\\s+");
                        switch (parts[0]) {
                            case "o", "g" -> current = groupFor(parts.length > 1 ? parts[1] : "", bottom, top, door, bolts, frame);
                            case "v" -> positions.add(new float[] {Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])});
                            case "vt" -> uvs.add(new float[] {Float.parseFloat(parts[1]), 1.0F - Float.parseFloat(parts[2])});
                            case "vn" -> normals.add(new float[] {Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])});
                            case "f" -> addFace(parts, positions, uvs, normals, current);
                            default -> { }
                        }
                    }
                }
            } catch (IOException | RuntimeException ignored) {
                return new ObjModel(ObjGroup.empty(), ObjGroup.empty(), ObjGroup.empty(), ObjGroup.empty(), ObjGroup.empty());
            }
            return new ObjModel(new ObjGroup(bottom), new ObjGroup(top), new ObjGroup(door), new ObjGroup(bolts), new ObjGroup(frame));
        }

        private static List<ObjFace> groupFor(final String name,
                                              final List<ObjFace> bottom,
                                              final List<ObjFace> top,
                                              final List<ObjFace> door,
                                              final List<ObjFace> bolts,
                                              final List<ObjFace> frame) {
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "bottom" -> bottom;
                case "top" -> top;
                case "door_cube.003" -> door;
                case "bolts" -> bolts;
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

        private void render(final PoseStack.Pose pose,
                            final VertexConsumer vertexConsumer,
                            @Nullable final TextureAtlasSprite sprite,
                            final int packedLight,
                            final int packedOverlay,
                            final boolean mirrored) {
            for (ObjFace face : faces) face.render(pose, vertexConsumer, sprite, packedLight, packedOverlay, mirrored);
        }
    }

    private record ObjFace(ObjVertex[] vertices) {
        private void render(final PoseStack.Pose pose,
                            final VertexConsumer vertexConsumer,
                            @Nullable final TextureAtlasSprite sprite,
                            final int packedLight,
                            final int packedOverlay,
                            final boolean mirrored) {
            if (vertices.length == 3) {
                if (mirrored) {
                    emit(vertices[2], pose, vertexConsumer, sprite, packedLight, packedOverlay, true);
                    emit(vertices[1], pose, vertexConsumer, sprite, packedLight, packedOverlay, true);
                    emit(vertices[0], pose, vertexConsumer, sprite, packedLight, packedOverlay, true);
                    emit(vertices[0], pose, vertexConsumer, sprite, packedLight, packedOverlay, true);
                } else {
                    emit(vertices[0], pose, vertexConsumer, sprite, packedLight, packedOverlay, false);
                    emit(vertices[1], pose, vertexConsumer, sprite, packedLight, packedOverlay, false);
                    emit(vertices[2], pose, vertexConsumer, sprite, packedLight, packedOverlay, false);
                    emit(vertices[2], pose, vertexConsumer, sprite, packedLight, packedOverlay, false);
                }
                return;
            }
            if (mirrored) {
                for (int i = vertices.length - 1; i >= 0; i--) {
                    emit(vertices[i], pose, vertexConsumer, sprite, packedLight, packedOverlay, true);
                }
                return;
            }
            for (ObjVertex vertex : vertices) {
                emit(vertex, pose, vertexConsumer, sprite, packedLight, packedOverlay, false);
            }
        }

        private static void emit(final ObjVertex vertex,
                                 final PoseStack.Pose pose,
                                 final VertexConsumer vertexConsumer,
                                 @Nullable final TextureAtlasSprite sprite,
                                 final int packedLight,
                                 final int packedOverlay,
                                 final boolean mirrored) {
            final Matrix4f poseMatrix = pose.pose();
            final float x = mirrored ? -vertex.x : vertex.x;
            final float nx = mirrored ? -vertex.nx : vertex.nx;
            vertexConsumer.addVertex(poseMatrix, x, vertex.y, vertex.z)
                    .setColor(255, 255, 255, 255)
                    .setUv(mappedU(sprite, vertex.u), mappedV(sprite, vertex.v))
                    .setOverlay(packedOverlay)
                    .setUv2(packedLight & 0xFFFF, packedLight >> 16)
                    .setNormal(pose, nx, vertex.ny, vertex.nz);
        }
    }

    private static float mappedU(@Nullable final TextureAtlasSprite sprite, final float u) {
        return sprite == null ? u : sprite.getU0() + ((sprite.getU1() - sprite.getU0()) * u);
    }

    private static float mappedV(@Nullable final TextureAtlasSprite sprite, final float v) {
        return sprite == null ? v : sprite.getV0() + ((sprite.getV1() - sprite.getV0()) * v);
    }

    public static final class WaterDoorRenderState extends BlockEntityRenderState {
        private BlockState state;
        private Direction facing;
        private DoorHingeSide hinge;
        private float doorProgress;
        private float boltProgress;
        private int skinIndex;
        private int packedLight;

        private void clear() {
            state = null;
            facing = null;
            hinge = null;
            doorProgress = 0.0F;
            boltProgress = 0.0F;
            skinIndex = 0;
            packedLight = 0;
        }
    }

    private record ObjVertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
    }
}
