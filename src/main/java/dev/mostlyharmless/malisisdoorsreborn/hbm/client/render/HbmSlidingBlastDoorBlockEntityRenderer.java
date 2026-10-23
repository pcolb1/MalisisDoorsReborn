package dev.mostlyharmless.malisisdoorsreborn.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.mostlyharmless.malisisdoorsreborn.client.render.door.CustomDoorBreakingOverlay;
import dev.mostlyharmless.malisisdoorsreborn.core.MdrDefaults;
import dev.mostlyharmless.malisisdoorsreborn.hbm.block.HbmSlidingBlastDoorBlock;
import dev.mostlyharmless.malisisdoorsreborn.hbm.blockentity.HbmSlidingBlastDoorBlockEntity;
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

public class HbmSlidingBlastDoorBlockEntityRenderer implements BlockEntityRenderer<HbmSlidingBlastDoorBlockEntity> {

    private static final ResourceLocation[] TEXTURES = {
            ResourceLocation.parse(MdrDefaults.MOD_ID + ":textures/models/pheodoors/blast_door.png"),
            ResourceLocation.parse(MdrDefaults.MOD_ID + ":textures/models/pheodoors/blast_door_industrial.png")
    };
    private static final ObjModel MODEL = ObjModel.load("assets/malisisdoorsreborn/models/pheodoors/blast_door.obj");
    private static final float MAX_OPEN = 2.125F;
    private static final float CLIP_MIN_Z = -2.5F;
    private static final float CLIP_MAX_Z = 2.5F;
    private static final float LOCK_PIVOT_Y = 1.8125F;

    @SuppressWarnings("unused")
    public HbmSlidingBlastDoorBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return MdrDefaults.LARGE_SPECIAL_DOOR_VIEW_DISTANCE;
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull final HbmSlidingBlastDoorBlockEntity be) {
        return true;
    }

    @Override
    public void render(@NotNull final HbmSlidingBlastDoorBlockEntity blockEntity,
                       final float partialTick,
                       @NotNull final PoseStack poseStack,
                       @NotNull final MultiBufferSource buffer,
                       final int packedLight,
                       final int packedOverlay) {
        final BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof HbmSlidingBlastDoorBlock)) return;
        if (state.getValue(HbmSlidingBlastDoorBlock.PART) != HbmSlidingBlastDoorBlock.HbmSlidingBlastDoorPart.ROOT) return;

        final Direction facing = state.getValue(HbmSlidingBlastDoorBlock.FACING);
        final float progress = blockEntity.getProgress(partialTick);
        final boolean opening = state.getValue(HbmSlidingBlastDoorBlock.OPEN);
        final float elapsedMs = (opening ? progress : 1.0F - progress) * 1200.0F;
        final float open = Math.max(0.0F, Math.min(MAX_OPEN, doorProgress(elapsedMs, opening) * MAX_OPEN));
        final float lock = 90.0F * lockProgress(elapsedMs, opening);
        final Minecraft minecraft = Minecraft.getInstance();

        if (CustomDoorBreakingOverlay.shouldRenderNormalDuringVanillaBreakingPass(minecraft, buffer, blockEntity.getBlockPos())) {
            renderModel(poseStack, minecraft.renderBuffers().bufferSource(), packedLight, packedOverlay, facing, open, lock, skinIndexForRender(state, blockEntity));
            return;
        }

        renderModel(poseStack, buffer, packedLight, packedOverlay, facing, open, lock, skinIndexForRender(state, blockEntity));
    }

    public static void renderItem(@NotNull final PoseStack poseStack,
                                  @NotNull final MultiBufferSource buffer,
                                  final int packedLight,
                                  final int packedOverlay,
                                  final int skinIndex) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.31F, 0.31F, 0.31F);
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        renderModel(poseStack, buffer, packedLight, packedOverlay, Direction.SOUTH, 0.0F, 0.0F, skinIndex);
        poseStack.popPose();
    }

    private static int skinIndexForRender(@NotNull final BlockState state,
                                          @NotNull final HbmSlidingBlastDoorBlockEntity blockEntity) {
        final int entitySkin = blockEntity.getSkinIndex();
        if (!state.hasProperty(HbmSlidingBlastDoorBlock.SKIN)) return entitySkin;
        final int stateSkin = state.getValue(HbmSlidingBlastDoorBlock.SKIN);
        return stateSkin != 0 || entitySkin == 0 ? stateSkin : entitySkin;
    }

    private static void renderModel(@NotNull final PoseStack poseStack,
                                    @NotNull final MultiBufferSource buffer,
                                    final int packedLight,
                                    final int packedOverlay,
                                    @NotNull final Direction facing,
                                    final float open,
                                    final float lock,
                                    final int skinIndex) {
        final ResourceLocation texture = TEXTURES[Math.floorMod(skinIndex, TEXTURES.length)];
        final VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawFor(facing)));

        final PoseStack.Pose pose = poseStack.last();
        MODEL.frame.render(pose, vertexConsumer, packedLight, packedOverlay);
        MODEL.leftDoor.renderOffsetClippedZ(pose, vertexConsumer, packedLight, packedOverlay, open, CLIP_MIN_Z, CLIP_MAX_Z);
        MODEL.rightLock.renderLockClippedZ(pose, vertexConsumer, packedLight, packedOverlay, open, 90.0F + lock, LOCK_PIVOT_Y, CLIP_MIN_Z, CLIP_MAX_Z);
        MODEL.rightDoor.renderOffsetClippedZ(pose, vertexConsumer, packedLight, packedOverlay, -open, CLIP_MIN_Z, CLIP_MAX_Z);
        MODEL.leftLock.renderLockClippedZ(pose, vertexConsumer, packedLight, packedOverlay, -open, 90.0F + lock, LOCK_PIVOT_Y, CLIP_MIN_Z, CLIP_MAX_Z);

        poseStack.popPose();
    }

    private static float doorProgress(final float elapsedMs, final boolean opening) {
        if (opening) {
            if (elapsedMs <= 350.0F) return 0.0F;
            if (elapsedMs <= 550.0F) return 0.05F * ((elapsedMs - 350.0F) / 200.0F);
            return 0.05F + 0.95F * sinUp((elapsedMs - 550.0F) / 650.0F);
        }

        if (elapsedMs <= 650.0F) return 1.0F + (0.05F - 1.0F) * sinUp(elapsedMs / 650.0F);
        if (elapsedMs <= 850.0F) return 0.05F * (1.0F - (elapsedMs - 650.0F) / 200.0F);
        return 0.0F;
    }

    private static float lockProgress(final float elapsedMs, final boolean opening) {
        if (opening) return Math.max(0.0F, Math.min(1.0F, elapsedMs / 200.0F));
        if (elapsedMs <= 1000.0F) return 1.0F;
        return Math.max(0.0F, 1.0F - (elapsedMs - 1000.0F) / 200.0F);
    }

    private static float sinUp(final float value) {
        final float clamped = Math.max(0.0F, Math.min(1.0F, value));
        return 1.0F - (float) Math.cos(clamped * Math.PI * 0.5D);
    }

    private static float yawFor(final Direction facing) {
        return switch (facing) {
            case EAST -> 0.0F;
            case NORTH -> 90.0F;
            case WEST -> 180.0F;
            case SOUTH, UP, DOWN -> -90.0F;
        };
    }

    private record ObjModel(ObjGroup frame,
                            ObjGroup leftDoor,
                            ObjGroup rightDoor,
                            ObjGroup leftLock,
                            ObjGroup rightLock) {

        @SuppressWarnings("SameParameterValue")
        private static ObjModel load(final String path) {
            final List<float[]> positions = new ArrayList<>();
            final List<float[]> uvs = new ArrayList<>();
            final List<float[]> normals = new ArrayList<>();
            final List<ObjFace> frame = new ArrayList<>();
            final List<ObjFace> leftDoor = new ArrayList<>();
            final List<ObjFace> rightDoor = new ArrayList<>();
            final List<ObjFace> leftLock = new ArrayList<>();
            final List<ObjFace> rightLock = new ArrayList<>();
            List<ObjFace> current = null;

            try (InputStream input = HbmSlidingBlastDoorBlockEntityRenderer.class.getClassLoader().getResourceAsStream(path)) {
                if (input == null) return empty();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        final String[] parts = line.split("\\s+");
                        switch (parts[0]) {
                            case "o", "g" -> current = groupFor(parts.length > 1 ? parts[1] : "", frame, leftDoor, rightDoor, leftLock, rightLock);
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
                return empty();
            }

            return new ObjModel(
                    new ObjGroup(frame),
                    new ObjGroup(leftDoor),
                    new ObjGroup(rightDoor),
                    new ObjGroup(leftLock),
                    new ObjGroup(rightLock)
            );
        }

        private static ObjModel empty() {
            return new ObjModel(ObjGroup.empty(), ObjGroup.empty(), ObjGroup.empty(), ObjGroup.empty(), ObjGroup.empty());
        }

        private static List<ObjFace> groupFor(final String name,
                                              final List<ObjFace> frame,
                                              final List<ObjFace> leftDoor,
                                              final List<ObjFace> rightDoor,
                                              final List<ObjFace> leftLock,
                                              final List<ObjFace> rightLock) {
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "frame" -> frame;
                case "leftdoor" -> leftDoor;
                case "rightdoor" -> rightDoor;
                case "leftlock" -> leftLock;
                case "rightlock" -> rightLock;
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

        private ObjGroup(final List<ObjFace> faces) {
            this.faces = List.copyOf(faces);
        }

        private static ObjGroup empty() {
            return new ObjGroup(List.of());
        }

        private void render(final PoseStack.Pose pose,
                            final VertexConsumer vertexConsumer,
                            final int packedLight,
                            final int packedOverlay) {
            for (ObjFace face : faces) {
                face.render(pose, vertexConsumer, packedLight, packedOverlay);
            }
        }

        @SuppressWarnings("SameParameterValue")
        private void renderOffsetClippedZ(final PoseStack.Pose pose,
                                          final VertexConsumer vertexConsumer,
                                          final int packedLight,
                                          final int packedOverlay,
                                          final float offsetZ,
                                          final float minZ,
                                          final float maxZ) {
            for (ObjFace face : faces) {
                face.renderOffsetClippedZ(pose, vertexConsumer, packedLight, packedOverlay, offsetZ, minZ, maxZ);
            }
        }

        @SuppressWarnings("SameParameterValue")
        private void renderLockClippedZ(final PoseStack.Pose pose,
                                        final VertexConsumer vertexConsumer,
                                        final int packedLight,
                                        final int packedOverlay,
                                        final float offsetZ,
                                        final float angleDegrees,
                                        final float pivotY,
                                        final float minZ,
                                        final float maxZ) {
            for (ObjFace face : faces) {
                face.renderLockClippedZ(pose, vertexConsumer, packedLight, packedOverlay, offsetZ, angleDegrees, pivotY, minZ, maxZ);
            }
        }
    }

    private record ObjFace(ObjVertex[] vertices) {

        private void render(final PoseStack.Pose pose,
                            final VertexConsumer vertexConsumer,
                            final int packedLight,
                            final int packedOverlay) {
            emitPolygon(List.of(vertices), pose, vertexConsumer, packedLight, packedOverlay);
        }

        private void renderOffsetClippedZ(final PoseStack.Pose pose,
                                          final VertexConsumer vertexConsumer,
                                          final int packedLight,
                                          final int packedOverlay,
                                          final float offsetZ,
                                          final float minZ,
                                          final float maxZ) {
            final List<ObjVertex> transformed = new ArrayList<>(vertices.length);
            for (ObjVertex vertex : vertices) transformed.add(vertex.offsetZ(offsetZ));
            emitClipped(transformed, pose, vertexConsumer, packedLight, packedOverlay, minZ, maxZ);
        }

        private void renderLockClippedZ(final PoseStack.Pose pose,
                                        final VertexConsumer vertexConsumer,
                                        final int packedLight,
                                        final int packedOverlay,
                                        final float offsetZ,
                                        final float angleDegrees,
                                        final float pivotY,
                                        final float minZ,
                                        final float maxZ) {
            final float angle = angleDegrees * ((float) Math.PI / 180.0F);
            final float sin = (float) Math.sin(angle);
            final float cos = (float) Math.cos(angle);
            final List<ObjVertex> transformed = new ArrayList<>(vertices.length);
            for (ObjVertex vertex : vertices) {
                transformed.add(vertex.rotateXAndOffsetZ(sin, cos, pivotY, offsetZ));
            }
            emitClipped(transformed, pose, vertexConsumer, packedLight, packedOverlay, minZ, maxZ);
        }

        private static void emitClipped(List<ObjVertex> vertices,
                                        final PoseStack.Pose pose,
                                        final VertexConsumer vertexConsumer,
                                        final int packedLight,
                                        final int packedOverlay,
                                        final float minZ,
                                        final float maxZ) {
            vertices = clipZ(vertices, minZ, true);
            vertices = clipZ(vertices, maxZ, false);
            emitPolygon(vertices, pose, vertexConsumer, packedLight, packedOverlay);
        }

        private static List<ObjVertex> clipZ(final List<ObjVertex> input,
                                             final float bound,
                                             final boolean keepGreater) {
            if (input.isEmpty()) return input;

            final List<ObjVertex> output = new ArrayList<>(input.size() + 1);
            ObjVertex previous = input.get(input.size() - 1);
            boolean previousInside = keepGreater ? previous.z >= bound : previous.z <= bound;

            for (ObjVertex current : input) {
                final boolean currentInside = keepGreater ? current.z >= bound : current.z <= bound;
                if (currentInside != previousInside) {
                    final float denominator = current.z - previous.z;
                    final float amount = denominator == 0.0F ? 0.0F : (bound - previous.z) / denominator;
                    output.add(previous.lerp(current, Math.max(0.0F, Math.min(1.0F, amount))));
                }
                if (currentInside) output.add(current);
                previous = current;
                previousInside = currentInside;
            }

            return output;
        }

        private static void emitPolygon(final List<ObjVertex> vertices,
                                        final PoseStack.Pose pose,
                                        final VertexConsumer vertexConsumer,
                                        final int packedLight,
                                        final int packedOverlay) {
            if (vertices.size() < 3) return;

            if (vertices.size() == 4) {
                for (ObjVertex vertex : vertices) {
                    emit(vertex, pose, vertexConsumer, packedLight, packedOverlay);
                }
                return;
            }

            final ObjVertex first = vertices.get(0);
            for (int i = 1; i < vertices.size() - 1; i++) {
                final ObjVertex second = vertices.get(i);
                final ObjVertex third = vertices.get(i + 1);
                emit(first, pose, vertexConsumer, packedLight, packedOverlay);
                emit(second, pose, vertexConsumer, packedLight, packedOverlay);
                emit(third, pose, vertexConsumer, packedLight, packedOverlay);
                emit(third, pose, vertexConsumer, packedLight, packedOverlay);
            }
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

    private record ObjVertex(float x,
                             float y,
                             float z,
                             float u,
                             float v,
                             float nx,
                             float ny,
                             float nz) {

        private ObjVertex offsetZ(final float offset) {
            return new ObjVertex(x, y, z + offset, u, v, nx, ny, nz);
        }

        private ObjVertex rotateXAndOffsetZ(final float sin,
                                            final float cos,
                                            final float pivotY,
                                            final float offsetZ) {
            final float localY = y - pivotY;
            final float transformedY = pivotY + localY * cos - z * sin;
            final float transformedZ = localY * sin + z * cos + offsetZ;
            final float transformedNy = ny * cos - nz * sin;
            final float transformedNz = ny * sin + nz * cos;
            return new ObjVertex(x, transformedY, transformedZ, u, v, nx, transformedNy, transformedNz);
        }

        private ObjVertex lerp(final ObjVertex other, final float amount) {
            return new ObjVertex(
                    x + (other.x - x) * amount,
                    y + (other.y - y) * amount,
                    z + (other.z - z) * amount,
                    u + (other.u - u) * amount,
                    v + (other.v - v) * amount,
                    nx + (other.nx - nx) * amount,
                    ny + (other.ny - ny) * amount,
                    nz + (other.nz - nz) * amount
            );
        }
    }
}
