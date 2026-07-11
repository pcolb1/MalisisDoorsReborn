package dev.mostlyharmless.malisisdoorsreborn.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmVaultDoorBlock;
import dev.mostlyharmless.malisisdoorsreborn.hbm.blockentity.HbmVaultDoorBlockEntity;
import dev.mostlyharmless.malisisdoorsreborn.client.render.door.CustomDoorBreakingOverlay;
import dev.mostlyharmless.malisisdoorsreborn.core.MdrDefaults;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HbmVaultDoorBlockEntityRenderer implements BlockEntityRenderer<HbmVaultDoorBlockEntity> {

    private static final ResourceLocation[] DOOR_TEXTURES = {
            ResourceLocation.fromNamespaceAndPath(MdrDefaults.MOD_ID, "textures/models/pheodoors/vault/vault_door_3.png"),
            ResourceLocation.fromNamespaceAndPath(MdrDefaults.MOD_ID, "textures/models/pheodoors/vault/vault_door_3.png"),
            ResourceLocation.fromNamespaceAndPath(MdrDefaults.MOD_ID, "textures/models/pheodoors/vault/vault_door_3.png"),
            ResourceLocation.fromNamespaceAndPath(MdrDefaults.MOD_ID, "textures/models/pheodoors/vault/vault_door_4.png"),
            ResourceLocation.fromNamespaceAndPath(MdrDefaults.MOD_ID, "textures/models/pheodoors/vault/vault_door_4.png"),
            ResourceLocation.fromNamespaceAndPath(MdrDefaults.MOD_ID, "textures/models/pheodoors/vault/vault_door_s.png"),
            ResourceLocation.fromNamespaceAndPath(MdrDefaults.MOD_ID, "textures/models/pheodoors/vault/vault_door_s.png")
    };
    private static final ResourceLocation[] LABEL_TEXTURES = {
            ResourceLocation.fromNamespaceAndPath(MdrDefaults.MOD_ID, "textures/models/pheodoors/vault/label_101.png"),
            ResourceLocation.fromNamespaceAndPath(MdrDefaults.MOD_ID, "textures/models/pheodoors/vault/label_87.png"),
            ResourceLocation.fromNamespaceAndPath(MdrDefaults.MOD_ID, "textures/models/pheodoors/vault/label_106.png"),
            ResourceLocation.fromNamespaceAndPath(MdrDefaults.MOD_ID, "textures/models/pheodoors/vault/label_81.png"),
            ResourceLocation.fromNamespaceAndPath(MdrDefaults.MOD_ID, "textures/models/pheodoors/vault/label_111.png"),
            ResourceLocation.fromNamespaceAndPath(MdrDefaults.MOD_ID, "textures/models/pheodoors/vault/label_2.png"),
            ResourceLocation.fromNamespaceAndPath(MdrDefaults.MOD_ID, "textures/models/pheodoors/vault/label_99.png")
    };
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
    public void render(@NotNull final HbmVaultDoorBlockEntity be,
                       final float partialTick,
                       @NotNull final PoseStack poseStack,
                       @NotNull final MultiBufferSource buffer,
                       final int packedLight,
                       final int packedOverlay) {
        final BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof HbmVaultDoorBlock)) return;
        if (state.getValue(HbmVaultDoorBlock.PART) != HbmVaultDoorBlock.HbmVaultDoorPart.ROOT) return;

        final Direction facing = state.getValue(HbmVaultDoorBlock.FACING);
        final float progress = Math.max(0.0F, Math.min(1.0F, be.getProgress(partialTick)));
        final Minecraft minecraft = Minecraft.getInstance();
        if (CustomDoorBreakingOverlay.shouldRenderNormalDuringVanillaBreakingPass(minecraft, buffer, be.getBlockPos())) {
            renderModel(poseStack, minecraft.renderBuffers().bufferSource(), packedLight, packedOverlay, facing, progress, skinIndexForRender(state, be));
            return;
        }
        renderModel(poseStack, buffer, packedLight, packedOverlay, facing, progress, skinIndexForRender(state, be));
    }


    private static int skinIndexForRender(@NotNull final BlockState state, @NotNull final HbmVaultDoorBlockEntity be) {
        final int entitySkin = be.getSkinIndex();
        if (!state.hasProperty(HbmVaultDoorBlock.SKIN)) return entitySkin;
        final int stateSkin = state.getValue(HbmVaultDoorBlock.SKIN);
        return stateSkin != 0 || entitySkin == 0 ? stateSkin : entitySkin;
    }

    public static void renderItem(@NotNull final PoseStack poseStack,
                                  @NotNull final MultiBufferSource buffer,
                                  final int packedLight,
                                  final int packedOverlay,
                                  final int skinIndex) {
        final int normalisedSkin = Math.floorMod(skinIndex, DOOR_TEXTURES.length);

        poseStack.pushPose();
        applyItemBaseTransform(poseStack);

        final VertexConsumer doorConsumer = buffer.getBuffer(RenderType.entityCutout(DOOR_TEXTURES[normalisedSkin]));
        MODEL.door.render(poseStack.last(), doorConsumer, packedLight, packedOverlay);

        final VertexConsumer labelConsumer = buffer.getBuffer(RenderType.entityCutout(LABEL_TEXTURES[normalisedSkin]));
        MODEL.label.render(poseStack.last(), labelConsumer, packedLight, packedOverlay);
        poseStack.popPose();
    }


    private static void applyItemBaseTransform(@NotNull final PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(ITEM_BASE_SCALE, ITEM_BASE_SCALE, ITEM_BASE_SCALE);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.translate(-ITEM_DOOR_CENTER_X, -ITEM_DOOR_CENTER_Y, -ITEM_DOOR_CENTER_Z);
    }

    private static void renderModel(@NotNull final PoseStack poseStack,
                                    @NotNull final MultiBufferSource buffer,
                                    final int packedLight,
                                    final int packedOverlay,
                                    @NotNull final Direction facing,
                                    final float progress,
                                    final int skinIndex) {
        final int normalisedSkin = Math.floorMod(skinIndex, DOOR_TEXTURES.length);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawFor(facing)));

        final VertexConsumer doorConsumer = buffer.getBuffer(RenderType.entityCutout(DOOR_TEXTURES[normalisedSkin]));
        MODEL.frame.render(poseStack.last(), doorConsumer, packedLight, packedOverlay);

        final float pull = pullFor(progress) * MAX_PULL;
        final float slide = slideFor(progress) * MAX_SLIDE;
        final float roll = (float) (360.0D * slide / (DIAMETER * Math.PI));

        poseStack.pushPose();
        poseStack.translate(-pull, 0.0F, slide);
        poseStack.translate(0.0F, 2.5F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(roll));
        poseStack.translate(0.0F, -2.5F, 0.0F);
        MODEL.door.render(poseStack.last(), doorConsumer, packedLight, packedOverlay);

        final VertexConsumer labelConsumer = buffer.getBuffer(RenderType.entityCutout(LABEL_TEXTURES[normalisedSkin]));
        MODEL.label.render(poseStack.last(), labelConsumer, packedLight, packedOverlay);
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

    private static float yawFor(final Direction facing) {
        // HBM RenderDoorGeneric orientation: meta SOUTH -> 270°, EAST -> 0°, NORTH -> 90°, WEST -> 180°.
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
                            final int packedLight,
                            final int packedOverlay) {
            for (ObjFace face : faces) face.render(pose, vertexConsumer, packedLight, packedOverlay);
        }
    }

    private record ObjFace(ObjVertex[] vertices) {
        private void render(final PoseStack.Pose pose,
                            final VertexConsumer vertexConsumer,
                            final int packedLight,
                            final int packedOverlay) {
            if (vertices.length == 3) {
                emit(vertices[0], pose, vertexConsumer, packedLight, packedOverlay);
                emit(vertices[1], pose, vertexConsumer, packedLight, packedOverlay);
                emit(vertices[2], pose, vertexConsumer, packedLight, packedOverlay);
                emit(vertices[2], pose, vertexConsumer, packedLight, packedOverlay);
                return;
            }
            for (ObjVertex vertex : vertices) emit(vertex, pose, vertexConsumer, packedLight, packedOverlay);
        }

        private static void emit(final ObjVertex vertex,
                                 final PoseStack.Pose pose,
                                 final VertexConsumer vertexConsumer,
                                 final int packedLight,
                                 final int packedOverlay) {
            final Matrix4f poseMatrix = pose.pose();
            vertexConsumer.addVertex(poseMatrix, vertex.x, vertex.y, vertex.z)
                    .setColor(255, 255, 255, 255)
                    .setUv(vertex.u, vertex.v)
                    .setOverlay(packedOverlay)
                    .setUv2(packedLight & 0xFFFF, packedLight >> 16)
                    .setNormal(pose, vertex.nx, vertex.ny, vertex.nz);
        }
    }

    private record ObjVertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {}
}
