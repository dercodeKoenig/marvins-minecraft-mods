package advRocketry.Rocket;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import io.netty.buffer.ByteBuf;

public class RenderData {
    VertexBuffer vertexBuffer;
    BufferBuilder bufferBuilder;
    ByteBufferBuilder byteBufferBuilder;
    MeshData mesh;
}
