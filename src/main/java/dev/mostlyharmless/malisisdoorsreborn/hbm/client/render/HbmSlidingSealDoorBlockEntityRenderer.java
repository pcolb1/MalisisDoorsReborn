package dev.mostlyharmless.malisisdoorsreborn.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.mostlyharmless.malisisdoorsreborn.client.render.door.CustomDoorBreakingOverlay;
import dev.mostlyharmless.malisisdoorsreborn.core.MdrDefaults;
import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmSlidingSealDoorBlock;
import dev.mostlyharmless.malisisdoorsreborn.hbm.blockentity.HbmSlidingSealDoorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
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

public class HbmSlidingSealDoorBlockEntityRenderer implements BlockEntityRenderer<HbmSlidingSealDoorBlockEntity> {

    private static final ResourceLocation[] TEXTURES = {
            ResourceLocation.parse(MdrDefaults.MOD_ID + ":textures/models/pheodoors/seal_door.png"),
            ResourceLocation.parse(MdrDefaults.MOD_ID + ":textures/models/pheodoors/seal_door_trefoil.png")
    };
    private static final ObjModel MODEL = ObjModel.load("assets/malisisdoorsreborn/models/pheodoors/seal_door.obj");
    private static final float MAX_SLIDE = 0.9F;
    private static final float FRAME_CLIP_MAX_Z = 0.5001F;
    private static final float ITEM_BASE_SCALE = 0.6F;
    private static final float ITEM_DOOR_CENTER_X = 0.375F;
    private static final float ITEM_DOOR_CENTER_Y = 1.0F;
    private static final float ITEM_DOOR_CENTER_Z = 0.0F;

    @SuppressWarnings("unused")
    public HbmSlidingSealDoorBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {}

    @Override
    public int getViewDistance() {
        return MdrDefaults.LARGE_SPECIAL_DOOR_VIEW_DISTANCE;
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(@NotNull final HbmSlidingSealDoorBlockEntity be) {
        final BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 2.0D, pos.getY(), pos.getZ() - 2.0D,
                pos.getX() + 3.0D, pos.getY() + 3.0D, pos.getZ() + 3.0D
        );
    }

    @Override
    public void render(@NotNull final HbmSlidingSealDoorBlockEntity be,
                       final float partialTick,
                       @NotNull final PoseStack poseStack,
                       @NotNull final MultiBufferSource buffer,
                       final int packedLight,
                       final int packedOverlay) {
        final BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof HbmSlidingSealDoorBlock)) return;
        if (state.getValue(HbmSlidingSealDoorBlock.PART) != HbmSlidingSealDoorBlock.HbmSlidingSealDoorPart.ROOT) return;

        final Direction facing = state.getValue(HbmSlidingSealDoorBlock.FACING);
        final float progress = Math.max(0.0F, Math.min(1.0F, be.getProgress(partialTick)));
        final float slide = slideFor(state, progress);
        final Minecraft minecraft = Minecraft.getInstance();
        if (CustomDoorBreakingOverlay.shouldRenderNormalDuringVanillaBreakingPass(minecraft, buffer, be.getBlockPos())) {
            renderModel(poseStack, minecraft.renderBuffers().bufferSource(), packedLight, packedOverlay, facing, slide, be.getSkinIndex());
            return;
        }
        renderModel(poseStack, buffer, packedLight, packedOverlay, facing, slide, be.getSkinIndex());
    }

    public static void renderItem(@NotNull final PoseStack poseStack,
                                  @NotNull final MultiBufferSource buffer,
                                  final int packedLight,
                                  final int packedOverlay,
                                  final int skinIndex) {
        poseStack.pushPose();
        applyItemBaseTransform(poseStack);

        final ResourceLocation texture = textureForSkin(skinIndex);
        final VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        MODEL.frame.render(poseStack.last(), vertexConsumer, packedLight, packedOverlay);
        MODEL.door.render(poseStack.last(), vertexConsumer, packedLight, packedOverlay);

        poseStack.popPose();
    }

    private static ResourceLocation textureForSkin(final int skinIndex) {
        return TEXTURES[Math.floorMod(skinIndex, TEXTURES.length)];
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
                                    final float slide,
                                    final int skinIndex) {
        final VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(textureForSkin(skinIndex)));

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawFor(facing)));

        // Literal RenderSealDoor contract after RenderDoorGeneric has translated and rotated the root.
        poseStack.translate(0.5F, 0.0F, 0.0F);
        MODEL.frame.render(poseStack.last(), vertexConsumer, packedLight, packedOverlay);
        if (slide > 0.0F) {
            MODEL.door.renderClippedMaxZ(poseStack.last(), vertexConsumer, packedLight, packedOverlay, slide, FRAME_CLIP_MAX_Z);
        } else if (slide < 0.0F) {
            MODEL.door.renderClippedMinZ(poseStack.last(), vertexConsumer, packedLight, packedOverlay, slide, -FRAME_CLIP_MAX_Z);
        } else {
            MODEL.door.render(poseStack.last(), vertexConsumer, packedLight, packedOverlay);
        }

        poseStack.popPose();
    }

    private static float slideFor(final BlockState state, final float progress) {
        final float slide = smoothstep(progress) * MAX_SLIDE;
        return state.getValue(HbmSlidingSealDoorBlock.HINGE) == DoorHingeSide.LEFT ? -slide : slide;
    }

    private static float smoothstep(final float value) {
        final float x = Mth.clamp(value, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
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
    private record ObjModel(ObjGroup door, ObjGroup frame) {
        private static ObjModel load(final String path) {
            final List<float[]> positions = new ArrayList<>();
            final List<float[]> uvs = new ArrayList<>();
            final List<float[]> normals = new ArrayList<>();
            final List<ObjFace> door = new ArrayList<>();
            final List<ObjFace> frame = new ArrayList<>();
            List<ObjFace> current = null;

            try (InputStream input = HbmSlidingSealDoorBlockEntityRenderer.class.getClassLoader().getResourceAsStream(path)) {
                if (input == null) return new ObjModel(ObjGroup.empty(), ObjGroup.empty());
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
                return new ObjModel(ObjGroup.empty(), ObjGroup.empty());
            }

            return new ObjModel(new ObjGroup(door), new ObjGroup(frame));
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

        private void render(final PoseStack.Pose pose,
                            final VertexConsumer vertexConsumer,
                            final int packedLight,
                            final int packedOverlay) {
            for (ObjFace face : faces) face.render(pose, vertexConsumer, packedLight, packedOverlay);
        }

        @SuppressWarnings("SameParameterValue")
        private void renderClippedMaxZ(final PoseStack.Pose pose,
                                       final VertexConsumer vertexConsumer,
                                       final int packedLight,
                                       final int packedOverlay,
                                       final float offsetZ,
                                       final float maxZ) {
            for (ObjFace face : faces) face.renderClippedMaxZ(pose, vertexConsumer, packedLight, packedOverlay, offsetZ, maxZ);
        }

        @SuppressWarnings("SameParameterValue")
        private void renderClippedMinZ(final PoseStack.Pose pose,
                                       final VertexConsumer vertexConsumer,
                                       final int packedLight,
                                       final int packedOverlay,
                                       final float offsetZ,
                                       final float minZ) {
            for (ObjFace face : faces) face.renderClippedMinZ(pose, vertexConsumer, packedLight, packedOverlay, offsetZ, minZ);
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

        private void renderClippedMaxZ(final PoseStack.Pose pose,
                                       final VertexConsumer vertexConsumer,
                                       final int packedLight,
                                       final int packedOverlay,
                                       final float offsetZ,
                                       final float maxZ) {
            List<ObjVertex> clipped = new ArrayList<>(vertices.length);
            for (ObjVertex vertex : vertices) clipped.add(vertex.offsetZ(offsetZ));
            clipped = clipMaxZ(clipped, maxZ);
            if (clipped.size() < 3) return;

            final ObjVertex first = clipped.getFirst();
            for (int i = 1; i < clipped.size() - 1; i++) {
                final ObjVertex second = clipped.get(i);
                final ObjVertex third = clipped.get(i + 1);
                emit(first, pose, vertexConsumer, packedLight, packedOverlay);
                emit(second, pose, vertexConsumer, packedLight, packedOverlay);
                emit(third, pose, vertexConsumer, packedLight, packedOverlay);
                emit(third, pose, vertexConsumer, packedLight, packedOverlay);
            }
        }

        private void renderClippedMinZ(final PoseStack.Pose pose,
                                       final VertexConsumer vertexConsumer,
                                       final int packedLight,
                                       final int packedOverlay,
                                       final float offsetZ,
                                       final float minZ) {
            List<ObjVertex> clipped = new ArrayList<>(vertices.length);
            for (ObjVertex vertex : vertices) clipped.add(vertex.offsetZ(offsetZ));
            clipped = clipMinZ(clipped, minZ);
            if (clipped.size() < 3) return;

            final ObjVertex first = clipped.getFirst();
            for (int i = 1; i < clipped.size() - 1; i++) {
                final ObjVertex second = clipped.get(i);
                final ObjVertex third = clipped.get(i + 1);
                emit(first, pose, vertexConsumer, packedLight, packedOverlay);
                emit(second, pose, vertexConsumer, packedLight, packedOverlay);
                emit(third, pose, vertexConsumer, packedLight, packedOverlay);
                emit(third, pose, vertexConsumer, packedLight, packedOverlay);
            }
        }

        private static List<ObjVertex> clipMaxZ(final List<ObjVertex> input, final float maxZ) {
            if (input.isEmpty()) return input;

            final List<ObjVertex> output = new ArrayList<>(input.size() + 1);
            ObjVertex previous = input.getLast();
            boolean previousInside = previous.z <= maxZ;

            for (ObjVertex current : input) {
                final boolean currentInside = current.z <= maxZ;
                if (currentInside != previousInside) output.add(intersectZ(previous, current, maxZ));
                if (currentInside) output.add(current);
                previous = current;
                previousInside = currentInside;
            }

            return output;
        }

        private static List<ObjVertex> clipMinZ(final List<ObjVertex> input, final float minZ) {
            if (input.isEmpty()) return input;

            final List<ObjVertex> output = new ArrayList<>(input.size() + 1);
            ObjVertex previous = input.getLast();
            boolean previousInside = previous.z >= minZ;

            for (ObjVertex current : input) {
                final boolean currentInside = current.z >= minZ;
                if (currentInside != previousInside) output.add(intersectZ(previous, current, minZ));
                if (currentInside) output.add(current);
                previous = current;
                previousInside = currentInside;
            }

            return output;
        }

        private static ObjVertex intersectZ(final ObjVertex start, final ObjVertex end, final float z) {
            final float denominator = end.z - start.z;
            final float t = denominator == 0.0F ? 0.0F : (z - start.z) / denominator;
            return start.lerp(end, Math.max(0.0F, Math.min(1.0F, t)));
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

    private record ObjVertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        private ObjVertex offsetZ(final float offset) {
            return new ObjVertex(x, y, z + offset, u, v, nx, ny, nz);
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
