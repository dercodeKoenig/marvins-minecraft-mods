package ARLib.utils;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;

import java.lang.ref.Cleaner;

///  use to auto-close a VertexBuffer when the parent object is deleted
public class VertexBufferCleaner implements Runnable {

    public static java.lang.ref.Cleaner cleaner = Cleaner.create();

    public static void register(Object object, VertexBuffer buffer) {
        register(object, buffer, null);
    }

    public static void register(Object object, VertexBuffer buffer, String comment) {
        cleaner.register(object, new VertexBufferCleaner(buffer, comment));
    }

    VertexBuffer buffer;
    String comment;

    VertexBufferCleaner(VertexBuffer buffer, String comment) {
        this.buffer = buffer;
        this.comment = comment;
    }

    @Override
    public void run() {
        RenderSystem.recordRenderCall(() -> {
            buffer.close();
            if (comment != null)
                System.out.println(comment);
        });
    }
}
