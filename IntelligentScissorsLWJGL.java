import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.stb.STBImageWrite.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class IntelligentScissorsLWJGL {
    private long window;
    private int windowWidth = 800;
    private int windowHeight = 600;

    private int textureId = 0;
    private int imageWidth, imageHeight;
    private IntelligentScissorsPart1 processor;

    private final List<Node> seedNodes = new ArrayList<>();
    private final List<List<Node>> paths = new ArrayList<>();
    private boolean isDragging = true;

    private double mouseX, mouseY;

    public void run() {
        init();
        loop();
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void init() {
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        window = glfwCreateWindow(windowWidth, windowHeight, "Intelligent Scissors LWJGL", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Failed to create GLFW window");

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();

        glfwSetDropCallback(window, (win, count, names) -> {
            for (int i = 0; i < count; i++) {
                String path = MemoryUtil.memUTF8(MemoryUtil.memPointerBuffer(names, count).get(i));
                System.out.println("Dropped file: " + path);
                loadTexture(path);
                break; // 只处理第一个文件
            }
        });
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT);

            drawTexture();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void loadTexture(String path) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer image = stbi_load(path, x, y, channels, 4);
            if (image == null) {
                throw new RuntimeException("Failed to load image: " + stbi_failure_reason());
            }

            imageWidth = x.get();
            imageHeight = y.get();

            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, imageWidth, imageHeight, 0,
                    GL_RGBA, GL_UNSIGNED_BYTE, image);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            stbi_image_free(image);
        }
    }


    private void drawTexture() {
        if (textureId == 0) {
            glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);
            return;
        }

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glColor3f(1.0f, 1.0f, 1.0f);
        glBegin(GL_QUADS);
        glTexCoord2f(0, 0); glVertex2f(-1, 1);
        glTexCoord2f(1, 0); glVertex2f(1, 1);
        glTexCoord2f(1, 1); glVertex2f(1, -1);
        glTexCoord2f(0, 1); glVertex2f(-1, -1);
        glEnd();
        glDisable(GL_TEXTURE_2D);
    }

    public static void main(String[] args) {
        new IntelligentScissorsLWJGL().run();
    }
}
