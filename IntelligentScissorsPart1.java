import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

// 节点类，表示图像中的一个像素
class Node {
    int x, y; // 像素坐标
    double f_G; // 归一化梯度值
    List<Link> neighbors; // 8个邻居的链接

    public Node(int x, int y, double f_G) {
        this.x = x;
        this.y = y;
        this.f_G = f_G;
        this.neighbors = new ArrayList<>();
    }
}

// 链接类，表示两个节点之间的连接
class Link {
    Node target; // 目标节点
    double cost; // 链接成本 C(x, y)

    public Link(Node target, double cost) {
        this.target = target;
        this.cost = cost;
    }
}

public class IntelligentScissorsPart1{
    // Sobel算子
    private static final int[][] SX = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
    private static final int[][] SY = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};

    private BufferedImage image;
    private int width, height;
    private int[][] pixels; // 像素强度值
    private double[][] Ix, Iy, G, f_G; // 梯度值
    private Node[][] graph; // 图结构

    // 构造函数：读取图像
    public IntelligentScissorsPart1(String imagePath) throws IOException {
        this.image = ImageIO.read(new File(imagePath));
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.pixels = new int[height][width];
        this.Ix = new double[height][width];
        this.Iy = new double[height][width];
        this.G = new double[height][width];
        this.f_G = new double[height][width];
        this.graph = new Node[height][width];
        loadPixels();
    }

    // 步骤1：读取图像像素值（灰度）
    private void loadPixels() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                // 转换为灰度值：0.299R + 0.587G + 0.114B
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                pixels[y][x] = (int) (0.299 * r + 0.587 * g + 0.114 * b);
            }
        }
    }

    // 步骤2：计算梯度 Ix 和 Iy
    private void computeGradients() {
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                // 应用 Sx 核（水平梯度）
                double sumX = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        sumX += pixels[y + dy][x + dx] * SX[dy + 1][dx + 1];
                    }
                }
                Ix[y][x] = sumX;

                // 应用 Sy 核（垂直梯度）
                double sumY = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        sumY += pixels[y + dy][x + dx] * SY[dy + 1][dx + 1];
                    }
                }
                Iy[y][x] = sumY;
            }
        }
        // 边界像素梯度设为0（简化处理）
        for (int x = 0; x < width; x++) {
            Ix[0][x] = Ix[height - 1][x] = 0;
            Iy[0][x] = Iy[height - 1][x] = 0;
        }
        for (int y = 0; y < height; y++) {
            Ix[y][0] = Ix[y][width - 1] = 0;
            Iy[y][0] = Iy[y][width - 1] = 0;
        }
    }

    // 步骤3：计算梯度幅度 G 和归一化 f_G
    private void computeGradientMagnitude() {
        double G_max = 0;
        // 计算 G = sqrt(Ix^2 + Iy^2)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                G[y][x] = Math.sqrt(Ix[y][x] * Ix[y][x] + Iy[y][x] * Iy[y][x]);
                if (G[y][x] > G_max) {
                    G_max = G[y][x];
                }
            }
        }
        // 归一化 f_G = (G_max - G) / G_max
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                f_G[y][x] = G_max == 0 ? 0 : (G_max - G[y][x]) / G_max;
            }
        }
    }

    // 步骤4：构建图结构，计算链接成本
    private void buildGraph() {
        // 初始化节点
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                graph[y][x] = new Node(x, y, f_G[y][x]);
            }
        }
        // 添加邻居链接（8个方向）
        int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1}; // 8个方向的x偏移
        int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1}; // 8个方向的y偏移
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Node node = graph[y][x];
                for (int i = 0; i < 8; i++) {
                    int nx = x + dx[i];
                    int ny = y + dy[i];
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                        // 计算链接成本 C(x, y) = 1 / (1 + G)
                        double avg_G = (G[y][x] + G[ny][nx]) / 2;
                        double cost = 1.0 / (1.0 + avg_G);
                        node.neighbors.add(new Link(graph[ny][nx], cost));
                    }
                }
            }
        }
    }

    // 保存数据到CSV文件
    private void saveToCSV(String outputDir) throws IOException {
        // 创建输出目录
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 定义文件名和数据
        String[] fileNames = {"pixels.csv", "Ix.csv", "Iy.csv", "G.csv", "f_G.csv"};
        double[][][] data = {toDoubleArray(pixels), Ix, Iy, G, f_G};

        // 保存每个数据集到CSV
        for (int s = 0; s < fileNames.length; s++) {
            try (PrintWriter writer = new PrintWriter(new File(outputDir + "/" + fileNames[s]))) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        writer.print(String.format("%.6f", data[s][y][x]));
                        if (x < width - 1) {
                            writer.print(",");
                        }
                    }
                    writer.println();
                }
            }
        }
    }

    // 将int[][]转换为double[][]以统一处理
    private double[][] toDoubleArray(int[][] array) {
        double[][] result = new double[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = array[y][x];
            }
        }
        return result;
    }

    // 执行所有步骤
    public void process() throws IOException {
        computeGradients();
        computeGradientMagnitude();
        buildGraph();
        saveToCSV("output");
    }

    // 获取图结构（供后续使用）
    public Node[][] getGraph() {
        return graph;
    }

    // 主函数：测试代码
    public static void main(String[] args) {
        try {
            // 替换为你的图像路径
            IntelligentScissorsPart1 processor = new IntelligentScissorsPart1("sample.png");
            processor.process();
            System.out.println("Data saved to output directory (pixels.csv, Ix.csv, Iy.csv, G.csv, f_G.csv)");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}