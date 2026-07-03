package dev.mostlyharmless.malisisdoorsreborn.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmQeContainmentDoorBlock;
import dev.mostlyharmless.malisisdoorsreborn.hbm.blockentity.HbmQeContainmentDoorBlockEntity;
import dev.mostlyharmless.malisisdoorsreborn.client.render.door.CustomDoorBreakingOverlay;
import dev.mostlyharmless.malisisdoorsreborn.core.MdrDefaults;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HbmQeContainmentDoorBlockEntityRenderer implements BlockEntityRenderer<HbmQeContainmentDoorBlockEntity> {

    private static final ResourceLocation[] TEXTURES = {
            ResourceLocation.parse(MdrDefaults.MOD_ID + ":textures/models/pheodoors/containment_door.png"),
            ResourceLocation.parse(MdrDefaults.MOD_ID + ":textures/models/pheodoors/containment_door_trefoil.png"),
            ResourceLocation.parse(MdrDefaults.MOD_ID + ":textures/models/pheodoors/containment_door_trefoil_yellow.png")
    };
    private static final ObjModel MODEL = ObjModel.load("assets/malisisdoorsreborn/models/pheodoors/containment_door.obj");
    private static final float MAX_RAISE = 2.25F;
    private static final float MODEL_TOP_Y = 3.0F;
    private static final float FRAME_CLIP_MAX_Y = 2.99F;
    private static final float MODEL_EPSILON = 0.0001F;

    @SuppressWarnings("unused")
    public HbmQeContainmentDoorBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {}

    @Override
    public int getViewDistance() {
        return MdrDefaults.LARGE_SPECIAL_DOOR_VIEW_DISTANCE;
    }

    @Override
    public void render(@NotNull final HbmQeContainmentDoorBlockEntity be,
                       final float partialTick,
                       @NotNull final PoseStack poseStack,
                       @NotNull final MultiBufferSource buffer,
                       final int packedLight,
                       final int packedOverlay) {
        final BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof HbmQeContainmentDoorBlock)) return;
        if (state.getValue(HbmQeContainmentDoorBlock.PART) != HbmQeContainmentDoorBlock.HbmQeContainmentDoorPart.ROOT) return;

        final Direction facing = state.getValue(HbmQeContainmentDoorBlock.FACING);
        final float raise = Math.max(0.0F, Math.min(MAX_RAISE, be.getProgress(partialTick) * MAX_RAISE));
        final Minecraft minecraft = Minecraft.getInstance();
        if (CustomDoorBreakingOverlay.shouldRenderNormalDuringVanillaBreakingPass(minecraft, buffer, be.getBlockPos())) {
            renderModel(poseStack, minecraft.renderBuffers().bufferSource(), packedLight, packedOverlay, facing, raise, skinIndexForRender(state, be));
            return;
        }
        renderModel(poseStack, buffer, packedLight, packedOverlay, facing, raise, skinIndexForRender(state, be));
    }


    private static int skinIndexForRender(@NotNull final BlockState state, @NotNull final HbmQeContainmentDoorBlockEntity be) {
        final int entitySkin = be.getSkinIndex();
        if (!state.hasProperty(HbmQeContainmentDoorBlock.SKIN)) return entitySkin;
        final int stateSkin = state.getValue(HbmQeContainmentDoorBlock.SKIN);
        return stateSkin != 0 || entitySkin == 0 ? stateSkin : entitySkin;
    }

    public static void renderItem(@NotNull final PoseStack poseStack,
                                  @NotNull final MultiBufferSource buffer,
                                  final int packedLight,
                                  final int packedOverlay,
                                  final int skinIndex) {
        poseStack.pushPose();
        poseStack.translate(0.5F, -0.05F, 0.5F);
        poseStack.scale(0.26F, 0.26F, 0.26F);
        renderModel(poseStack, buffer, packedLight, packedOverlay, Direction.SOUTH, 0.0F, skinIndex);
        poseStack.popPose();
    }

    private static void renderModel(@NotNull final PoseStack poseStack,
                                    @NotNull final MultiBufferSource buffer,
                                    final int packedLight,
                                    final int packedOverlay,
                                    @NotNull final Direction facing,
                                    final float raise,
                                    final int skinIndex) {
        final ResourceLocation texture = TEXTURES[Math.floorMod(skinIndex, TEXTURES.length)];
        final VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawFor(facing)));

        // Literal RenderContainmentDoor contract after RenderDoorGeneric has translated and rotated the root.
        poseStack.translate(0.25F, 0.0F, 0.0F);

        MODEL.frame.render(poseStack.last(), vertexConsumer, packedLight, packedOverlay);

        if (MdrDefaults.FIRE_DOOR_CLIP_TO_FRAME && raise > 0.0F) {
            MODEL.door.renderClippedMaxY(poseStack.last(), vertexConsumer, packedLight, packedOverlay, raise, FRAME_CLIP_MAX_Y);
        } else {
            poseStack.pushPose();
            poseStack.translate(0.0F, raise, 0.0F);
            MODEL.door.render(poseStack.last(), vertexConsumer, packedLight, packedOverlay);
            poseStack.popPose();
        }

        poseStack.popPose();
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

            try (InputStream input = HbmQeContainmentDoorBlockEntityRenderer.class.getClassLoader().getResourceAsStream(path)) {
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

        private void render(final PoseStack.Pose pose,
                            final VertexConsumer vertexConsumer,
                            final int packedLight,
                            final int packedOverlay) {
            for (ObjFace face : faces) face.render(pose, vertexConsumer, packedLight, packedOverlay);
        }

        @SuppressWarnings("SameParameterValue")
        private void renderClippedMaxY(final PoseStack.Pose pose,
                                       final VertexConsumer vertexConsumer,
                                       final int packedLight,
                                       final int packedOverlay,
                                       final float offsetY,
                                       final float maxY) {
            for (ObjFace face : faces) face.renderClippedMaxY(pose, vertexConsumer, packedLight, packedOverlay, offsetY, maxY);
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
                                       final int packedLight,
                                       final int packedOverlay,
                                       final float offsetY,
                                       final float maxY) {
            List<ObjVertex> clipped = new ArrayList<>(vertices.length);
            for (ObjVertex vertex : vertices) clipped.add(vertex.offsetY(offsetY));
            clipped = clipMaxY(clipped, maxY);
            if (clipped.size() < 3) return;

            final ObjVertex first = clipped.get(0);
            for (int i = 1; i < clipped.size() - 1; i++) {
                final ObjVertex second = clipped.get(i);
                final ObjVertex third = clipped.get(i + 1);
                emit(first, pose, vertexConsumer, packedLight, packedOverlay);
                emit(second, pose, vertexConsumer, packedLight, packedOverlay);
                emit(third, pose, vertexConsumer, packedLight, packedOverlay);
                emit(third, pose, vertexConsumer, packedLight, packedOverlay);
            }
        }

        private static List<ObjVertex> clipMaxY(final List<ObjVertex> input, final float maxY) {
            if (input.isEmpty()) return input;

            final List<ObjVertex> output = new ArrayList<>(input.size() + 1);
            ObjVertex previous = input.get(input.size() - 1);
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

        private static void emit(final ObjVertex vertex,
                                 final PoseStack.Pose pose,
                                 final VertexConsumer vertexConsumer,
                                 final int packedLight,
                                 final int packedOverlay) {
            final Matrix4f poseMatrix = pose.pose();
            final Matrix3f normalMatrix = pose.normal();
            vertexConsumer.vertex(poseMatrix, vertex.x, vertex.y, vertex.z)
                    .color(255, 255, 255, 255)
                    .uv(vertex.u, vertex.v)
                    .overlayCoords(packedOverlay)
                    .uv2(packedLight)
                    .normal(normalMatrix, vertex.nx, vertex.ny, vertex.nz)
                    .endVertex();
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
