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
import static org.lwjgl.opengl.GL12.*;
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

        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            mouseX = xpos;
            mouseY = ypos;
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                onMouseClick((int) mouseX, (int) mouseY);
            }
        });

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
            drawPaths();

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
            if (image == null) throw new RuntimeException("Failed to load image: " + stbi_failure_reason());

            imageWidth = x.get(0);
            imageHeight = y.get(0);

            if (textureId != 0) {
                glDeleteTextures(textureId);
            }

            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, imageWidth, imageHeight, 0, GL_BGRA, GL_UNSIGNED_BYTE, image);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            stbi_image_free(image);

            processor = new IntelligentScissorsPart1(new File(path).getAbsolutePath());
            processor.process();

            seedNodes.clear();
            paths.clear();
        } catch (Exception e) {
            e.printStackTrace();
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
        glBegin(GL_QUADS);
        glTexCoord2f(0, 0); glVertex2f(-1, 1);
        glTexCoord2f(1, 0); glVertex2f(1, 1);
        glTexCoord2f(1, 1); glVertex2f(1, -1);
        glTexCoord2f(0, 1); glVertex2f(-1, -1);
        glEnd();
        glDisable(GL_TEXTURE_2D);
    }

    private void drawPaths() {
        if (textureId == 0) return;

        glColor3f(1, 0, 0);
        glLineWidth(2);
        glBegin(GL_LINES);
        for (List<Node> path : paths) {
            for (int i = 1; i < path.size(); i++) {
                Node n1 = path.get(i - 1);
                Node n2 = path.get(i);
                glVertex2f(mapX(n1.x), mapY(n1.y));
                glVertex2f(mapX(n2.x), mapY(n2.y));
            }
        }
        glEnd();
    }

    private void onMouseClick(int screenX, int screenY) {
        if (processor == null) return;

        int imgX = (int) (screenX * (double) imageWidth / windowWidth);
        int imgY = (int) (screenY * (double) imageHeight / windowHeight);
        imgY = imageHeight - imgY;

        Node seed = processor.getGraph()[imgY][imgX];
        seedNodes.add(seed);

        if (seedNodes.size() >= 2) {
            Node prev = seedNodes.get(seedNodes.size() - 2);
            List<Node> path = processor.computeShortestPath(prev.x, prev.y, seed.x, seed.y);
            if (!path.isEmpty()) {
                paths.add(path);
            }
        }

        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS) {
            saveScreenshot("output.png");
        }
    }

    private void saveScreenshot(String filename) {
        int width = windowWidth;
        int height = windowHeight;

        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        stbi_flip_vertically_on_write(true);
        stbi_write_png(filename, width, height, 4, buffer, width * 4);

        System.out.println("Saved screenshot to " + filename);
    }

    private float mapX(int x) {
        return (float) (x * 2.0 / imageWidth - 1);
    }

    private float mapY(int y) {
        return (float) (1 - y * 2.0 / imageHeight);
    }

    public static void main(String[] args) {
        new IntelligentScissorsLWJGL().run();
    }
}
