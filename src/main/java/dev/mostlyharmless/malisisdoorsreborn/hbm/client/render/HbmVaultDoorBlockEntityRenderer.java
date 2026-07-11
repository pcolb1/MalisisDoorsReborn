package dev.mostlyharmless.malisisdoorsreborn.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.mostlyharmless.malisisdoorsreborn.client.render.door.CustomDoorBreakingOverlay;
import dev.mostlyharmless.malisisdoorsreborn.core.MdrDefaults;
import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmVaultDoorBlock;
import dev.mostlyharmless.malisisdoorsreborn.hbm.blockentity.HbmVaultDoorBlockEntity;
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

public class HbmVaultDoorBlockEntityRenderer implements BlockEntityRenderer<HbmVaultDoorBlockEntity, HbmVaultDoorBlockEntityRenderer.VaultDoorRenderState> {

    private static final Identifier[] DOOR_TEXTURES = {
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_vault_door_skin_3"),
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_vault_door_skin_3"),
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_vault_door_skin_3"),
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_vault_door_skin_4"),
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_vault_door_skin_4"),
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_vault_door_skin_s"),
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_vault_door_skin_s")
    };
    private static final Identifier[] LABEL_TEXTURES = {
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_vault_door_label_101"),
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_vault_door_label_87"),
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_vault_door_label_106"),
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_vault_door_label_81"),
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_vault_door_label_111"),
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_vault_door_label_2"),
            Identifier.fromNamespaceAndPath(MdrDefaults.MOD_ID, "block/hbm_vault_door_label_99")
    };
    private static final Identifier BLOCK_ATLAS_ID = Identifier.withDefaultNamespace("blocks");
    private static final ObjModel MODEL = ObjModel.load("assets/malisisdoorsreborn/models/pheodoors/vault_door.obj");
    private static final float MAX_SLIDE = 5.0F;
    private static final float MAX_PULL = 1.0F;
    private static final float DIAMETER = 4.25F;
    private static final float ITEM_DOOR_CENTER_X = -0.03125F;
    private static final float ITEM_DOOR_CENTER_Y = 2.5219955F;
    private static final float ITEM_DOOR_CENTER_Z = 0.0F;
    private static final float ITEM_DOOR_HEIGHT = 4.250569F;
    private static final float ITEM_BASE_SCALE = 1.0F / ITEM_DOOR_HEIGHT;

    @SuppressWarnings("unused")
    public HbmVaultDoorBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {}

    @Override
    public int getViewDistance() {
        return MdrDefaults.LARGE_SPECIAL_DOOR_VIEW_DISTANCE;
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(@NotNull final HbmVaultDoorBlockEntity be) {
        final BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 8.0D, pos.getY(), pos.getZ() - 8.0D,
                pos.getX() + 8.0D, pos.getY() + 6.0D, pos.getZ() + 8.0D
        );
    }

    @Override
    public @NotNull VaultDoorRenderState createRenderState() {
        return new VaultDoorRenderState();
    }

    @Override
    public void extractRenderState(@NotNull final HbmVaultDoorBlockEntity be,
                                   @NotNull final VaultDoorRenderState renderState,
                                   final float partialTick,
                                   @NotNull final Vec3 cameraPos,
                                   @Nullable final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(be, renderState, partialTick, cameraPos,
                CustomDoorBreakingOverlay.shouldRenderNormalDuringVanillaBreakingPass(crumblingOverlay, be.getBlockPos())
                        ? null
                        : crumblingOverlay);
        renderState.clear();

        final BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof HbmVaultDoorBlock)) return;
        if (state.getValue(HbmVaultDoorBlock.PART) != HbmVaultDoorBlock.HbmVaultDoorPart.ROOT) return;

        renderState.state = state;
        renderState.facing = state.getValue(HbmVaultDoorBlock.FACING);
        renderState.progress = Math.max(0.0F, Math.min(1.0F, be.getProgress(partialTick)));
        renderState.skinIndex = skinIndexForRender(state, be);
        renderState.packedLight = renderState.lightCoords;
    }

    @Override
    public void submit(@NotNull final VaultDoorRenderState renderState,
                       @NotNull final PoseStack poseStack,
                       @NotNull final SubmitNodeCollector collector,
                       @NotNull final CameraRenderState cameraState) {
        if (renderState.state == null || renderState.facing == null) return;
        submitModel(poseStack, collector, renderState.packedLight, 0, renderState.facing, renderState.progress, renderState.skinIndex);
    }

    private static int skinIndexForRender(@NotNull final BlockState state, @NotNull final HbmVaultDoorBlockEntity be) {
        final int entitySkin = be.getSkinIndex();
        if (!state.hasProperty(HbmVaultDoorBlock.SKIN)) return entitySkin;
        final int stateSkin = state.getValue(HbmVaultDoorBlock.SKIN);
        return stateSkin != 0 || entitySkin == 0 ? stateSkin : entitySkin;
    }

    @SuppressWarnings("resource")
    public static void submitItem(@NotNull final PoseStack poseStack,
                                  @NotNull final SubmitNodeCollector collector,
                                  final int packedLight,
                                  final int packedOverlay,
                                  final int skinIndex) {
        final int normalisedSkin = Math.floorMod(skinIndex, DOOR_TEXTURES.length);
        final TextureAtlasSprite doorSprite = doorSpriteFor(normalisedSkin);
        final TextureAtlasSprite labelSprite = labelSpriteFor(normalisedSkin);

        poseStack.pushPose();
        applyItemBaseTransform(poseStack);
        collector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(), (pose, vertexConsumer) ->
                MODEL.door.render(pose, vertexConsumer, doorSprite, packedLight, packedOverlay));
        collector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(), (pose, vertexConsumer) ->
                MODEL.label.render(pose, vertexConsumer, labelSprite, packedLight, packedOverlay));
        poseStack.popPose();
    }

    private static void applyItemBaseTransform(@NotNull final PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(ITEM_BASE_SCALE, ITEM_BASE_SCALE, ITEM_BASE_SCALE);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.translate(-ITEM_DOOR_CENTER_X, -ITEM_DOOR_CENTER_Y, -ITEM_DOOR_CENTER_Z);
    }

    @SuppressWarnings({"SameParameterValue","resource"})
    private static void submitModel(@NotNull final PoseStack poseStack,
                                    @NotNull final SubmitNodeCollector collector,
                                    final int packedLight,
                                    final int packedOverlay,
                                    @NotNull final Direction facing,
                                    final float progress,
                                    final int skinIndex) {
        final int normalisedSkin = Math.floorMod(skinIndex, DOOR_TEXTURES.length);
        final TextureAtlasSprite doorSprite = doorSpriteFor(normalisedSkin);
        final TextureAtlasSprite labelSprite = labelSpriteFor(normalisedSkin);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawFor(facing)));

        collector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(), (pose, vertexConsumer) ->
                MODEL.frame.render(pose, vertexConsumer, doorSprite, packedLight, packedOverlay));

        final float pull = pullFor(progress) * MAX_PULL;
        final float slide = slideFor(progress) * MAX_SLIDE;
        final float roll = (float) (360.0D * slide / (DIAMETER * Math.PI));

        poseStack.pushPose();
        poseStack.translate(-pull, 0.0F, slide);
        poseStack.translate(0.0F, 2.5F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(roll));
        poseStack.translate(0.0F, -2.5F, 0.0F);
        collector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(), (pose, vertexConsumer) ->
                MODEL.door.render(pose, vertexConsumer, doorSprite, packedLight, packedOverlay));
        collector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(), (pose, vertexConsumer) ->
                MODEL.label.render(pose, vertexConsumer, labelSprite, packedLight, packedOverlay));
        poseStack.popPose();

        poseStack.popPose();
    }

    private static float pullFor(final float progress) {
        final float t = Math.max(0.0F, Math.min(1.0F, progress));
        if (t <= 0.3333F) return sinFull(t / 0.3333F);
        return 1.0F;
    }

    private static float sinFull(final float t) {
        return (float) ((-Math.cos(Math.max(0.0F, Math.min(1.0F, t)) * Math.PI) + 1.0D) / 2.0D);
    }

    private static float slideFor(final float progress) {
        final float t = Math.max(0.0F, Math.min(1.0F, progress));
        if (t <= 0.3333F) return 0.0F;
        return Math.max(0.0F, Math.min(1.0F, (t - 0.3333F) / 0.6667F));
    }

    private static TextureAtlasSprite doorSpriteFor(final int skinIndex) {
        return Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(BLOCK_ATLAS_ID)
                .getSprite(DOOR_TEXTURES[Math.floorMod(skinIndex, DOOR_TEXTURES.length)]);
    }

    private static TextureAtlasSprite labelSpriteFor(final int skinIndex) {
        return Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(BLOCK_ATLAS_ID)
                .getSprite(LABEL_TEXTURES[Math.floorMod(skinIndex, LABEL_TEXTURES.length)]);
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
    private record ObjModel(ObjGroup frame, ObjGroup door, ObjGroup label) {
        private static ObjModel load(final String path) {
            final List<float[]> positions = new ArrayList<>();
            final List<float[]> uvs = new ArrayList<>();
            final List<float[]> normals = new ArrayList<>();
            final List<ObjFace> frame = new ArrayList<>();
            final List<ObjFace> door = new ArrayList<>();
            final List<ObjFace> label = new ArrayList<>();
            List<ObjFace> current = null;

            try (InputStream input = HbmVaultDoorBlockEntityRenderer.class.getClassLoader().getResourceAsStream(path)) {
                if (input == null) return new ObjModel(ObjGroup.empty(), ObjGroup.empty(), ObjGroup.empty());
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        final String[] parts = line.split("\\s+");
                        switch (parts[0]) {
                            case "o", "g" -> current = groupFor(parts.length > 1 ? parts[1] : "", frame, door, label);
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

            return new ObjModel(new ObjGroup(frame), new ObjGroup(door), new ObjGroup(label));
        }

        private static List<ObjFace> groupFor(final String name,
                                              final List<ObjFace> frame,
                                              final List<ObjFace> door,
                                              final List<ObjFace> label) {
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "frame" -> frame;
                case "door" -> door;
                case "label" -> label;
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
                            final int packedOverlay) {
            for (ObjFace face : faces) face.render(pose, vertexConsumer, sprite, packedLight, packedOverlay);
        }
    }

    private record ObjFace(ObjVertex[] vertices) {
        private void render(final PoseStack.Pose pose,
                            final VertexConsumer vertexConsumer,
                            @Nullable final TextureAtlasSprite sprite,
                            final int packedLight,
                            final int packedOverlay) {
            if (vertices.length == 3) {
                emit(vertices[0], pose, vertexConsumer, sprite, packedLight, packedOverlay);
                emit(vertices[1], pose, vertexConsumer, sprite, packedLight, packedOverlay);
                emit(vertices[2], pose, vertexConsumer, sprite, packedLight, packedOverlay);
                emit(vertices[2], pose, vertexConsumer, sprite, packedLight, packedOverlay);
                return;
            }
            for (ObjVertex vertex : vertices) emit(vertex, pose, vertexConsumer, sprite, packedLight, packedOverlay);
        }

        private static void emit(final ObjVertex vertex,
                                 final PoseStack.Pose pose,
                                 final VertexConsumer vertexConsumer,
                                 @Nullable final TextureAtlasSprite sprite,
                                 final int packedLight,
                                 final int packedOverlay) {
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
        return sprite == null ? u : sprite.getU0() + ((sprite.getU1() - sprite.getU0()) * u);
    }

    private static float mappedV(@Nullable final TextureAtlasSprite sprite, final float v) {
        return sprite == null ? v : sprite.getV0() + ((sprite.getV1() - sprite.getV0()) * v);
    }

    public static final class VaultDoorRenderState extends BlockEntityRenderState {
        private BlockState state;
        private Direction facing;
        private float progress;
        private int skinIndex;
        private int packedLight;

        private void clear() {
            state = null;
            facing = null;
            progress = 0.0F;
            skinIndex = 0;
            packedLight = 0;
        }
    }

    private record ObjVertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {}
}
