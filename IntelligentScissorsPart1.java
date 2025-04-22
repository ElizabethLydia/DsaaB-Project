import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import edu.princeton.cs.algs4.IndexMinPQ;
import edu.princeton.cs.algs4.Stack;

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

class Link {
    Node target; // 目标节点
    double cost; // 链接成本 C(x, y)

    public Link(Node target, double cost) {
        this.target = target;
        this.cost = cost;
    }
}

public class IntelligentScissorsPart1 {
    private static final int[][] SX = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
    private static final int[][] SY = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};

    private BufferedImage image;
    private int width, height;
    private int[][] pixels; // 像素强度值
    private double[][] Ix, Iy, G, f_G; // 梯度值
    private Node[][] graph; // 图结构

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

    public IntelligentScissorsPart1(BufferedImage image) { //从 GUI 中传入已缩放或原始图像
        this.image = image;
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

    private void loadPixels() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                pixels[y][x] = (int) (0.299 * r + 0.587 * g + 0.114 * b);
            }
        }
    }

    private void computeGradients() {
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                double sumX = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        sumX += pixels[y + dy][x + dx] * SX[dy + 1][dx + 1];
                    }
                }
                Ix[y][x] = sumX;

                double sumY = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        sumY += pixels[y + dy][x + dx] * SY[dy + 1][dx + 1];
                    }
                }
                Iy[y][x] = sumY;
            }
        }
        for (int x = 0; x < width; x++) {
            Ix[0][x] = Ix[height - 1][x] = 0;
            Iy[0][x] = Iy[height - 1][x] = 0;
        }
        for (int y = 0; y < height; y++) {
            Ix[y][0] = Ix[y][width - 1] = 0;
            Iy[y][0] = Iy[y][width - 1] = 0;
        }
    }

    private void computeGradientMagnitude() {
        double G_max = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                G[y][x] = Math.sqrt(Ix[y][x] * Ix[y][x] + Iy[y][x] * Iy[y][x]);
                if (G[y][x] > G_max) {
                    G_max = G[y][x];
                }
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                f_G[y][x] = G_max == 0 ? 0 : (G_max - G[y][x]) / G_max;
            }
        }
    }

    private void buildGraph() {
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
                        boolean isDiag = dx[i] != 0 && dy[i] != 0;
                        double basecost = 1.0 / (1.0 + G[ny][nx]);
                        double cost = isDiag ? basecost * Math.sqrt(2) : basecost; //对角线乘根号2
//                        double avg_G = (G[y][x] + G[ny][nx]) / 2;
//                        double cost = 1.0 / (1.0 + avg_G);
                        node.neighbors.add(new Link(graph[ny][nx], cost));
                    }
                }
            }
        }
    }

    public List<Node> computeShortestPath(int seedX, int seedY, int targetX, int targetY) {
        // Boundary check
        if (seedX < 0 || seedX >= width || seedY < 0 || seedY >= height ||
                targetX < 0 || targetX >= width || targetY < 0 || targetY >= height) {
            return new ArrayList<>();
        }

        // Convert 2D coordinates to 1D index for algs4 data structures
        int V = width * height;
        double[] distTo = new double[V];
        Node[] edgeTo = new Node[V];
        IndexMinPQ<Double> pq = new IndexMinPQ<>(V);

        // Initialize distances to infinity
        for (int v = 0; v < V; v++) {
            distTo[v] = Double.POSITIVE_INFINITY;
        }

        // Convert seed coordinates to 1D index
        int seedIndex = seedY * width + seedX;
        distTo[seedIndex] = 0.0;
        pq.insert(seedIndex, 0.0);

        while (!pq.isEmpty()) {
            int currentIndex = pq.delMin();
            int x = currentIndex % width;
            int y = currentIndex / width;

            // Early termination if target is reached
            if (x == targetX && y == targetY) break;

            for (Link link : graph[y][x].neighbors) {
                Node neighbor = link.target;
                int neighborIndex = neighbor.y * width + neighbor.x;

                double newDist = distTo[currentIndex] + link.cost;
                if (newDist < distTo[neighborIndex]) {
                    distTo[neighborIndex] = newDist;
                    edgeTo[neighborIndex] = graph[y][x];

                    if (pq.contains(neighborIndex)) {
                        pq.decreaseKey(neighborIndex, distTo[neighborIndex]);
                    } else {
                        pq.insert(neighborIndex, distTo[neighborIndex]);
                    }
                }
            }
        }

        // Reconstruct path
        int targetIndex = targetY * width + targetX;
        if (distTo[targetIndex] == Double.POSITIVE_INFINITY) {
            return new ArrayList<>(); // No path exists
        }

        Stack<Node> path = new Stack<>();
        for (Node current = graph[targetY][targetX];
             current != null && !(current.x == seedX && current.y == seedY);
             current = edgeTo[current.y * width + current.x]) {
            path.push(current);
        }
        path.push(graph[seedY][seedX]);

        // Convert stack to list
        List<Node> result = new ArrayList<>();
        while (!path.isEmpty()) {
            result.add(path.pop());
        }

        return result;
    }

    private void saveToCSV(String outputDir) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String[] fileNames = {"pixels.csv", "Ix.csv", "Iy.csv", "G.csv", "f_G.csv"};
        double[][][] data = {toDoubleArray(pixels), Ix, Iy, G, f_G};
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

    private double[][] toDoubleArray(int[][] array) {
        double[][] result = new double[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = array[y][x];
            }
        }
        return result;
    }

    public void process() throws IOException {
        computeGradients();
        computeGradientMagnitude();
        buildGraph();
        saveToCSV("output");
    }

    public Node[][] getGraph() {
        return graph;
    }

    // 新增：获取梯度幅度G
    public double[][] getG() {
        return G;
    }

    // 新增：获取归一化梯度f_G
    public double[][] getFG() {
        return f_G;
    }

    // 新增：获取灰度像素值
    public int[][] getPixels() {
        return pixels;
    }

    // 新增：生成梯度图像（热力图）
    public BufferedImage getGradientImage() {
        BufferedImage gradientImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        double maxG = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (G[y][x] > maxG) {
                    maxG = G[y][x];
                }
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = maxG == 0 ? 0 : (int) (G[y][x] * 255 / maxG);
                int rgb = (value << 16) | (value << 8) | value; // 灰度
                gradientImage.setRGB(x, y, rgb);
            }
        }
        return gradientImage;
    }

    // 新增：生成灰度图像
    public BufferedImage getGrayImage() {
        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = pixels[y][x];
                int rgb = (value << 16) | (value << 8) | value;
                grayImage.setRGB(x, y, rgb);
            }
        }
        return grayImage;
    }

    public static void main(String[] args) {
        try {
            IntelligentScissorsPart1 processor = new IntelligentScissorsPart1("sample.png");
            processor.process();
            int seedX = 10, seedY = 10;
            int targetX = 20, targetY = 20;
            List<Node> path = processor.computeShortestPath(seedX, seedY, targetX, targetY);
            System.out.println("Shortest Path from (" + seedX + ", " + seedY + ") to (" + targetX + ", " + targetY + "):");
            if (path.isEmpty()) {
                System.out.println("No path found!");
            } else {
                for (Node node : path) {
                    System.out.println("(" + node.x + ", " + node.y + ")");
                }
                System.out.println("Path length: " + path.size() + " nodes");
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}

//    // 主函数：测试代码
//    public static void main(String[] args) {
//        try {
//            // 替换为你的图像路径
//            IntelligentScissorsPart1 processor = new IntelligentScissorsPart1("sample.png");
//            processor.process();
//            System.out.println("Data saved to output directory (pixels.csv, Ix.csv, Iy.csv, G.csv, f_G.csv)");
//        } catch (IOException e) {
//            System.err.println("Error: " + e.getMessage());
//        }
//    }
//}

//    public static void main(String[] args) {
//        try {
//            // 替换为你的图像路径
//            IntelligentScissorsPart1 processor = new IntelligentScissorsPart1("sample.png");
//            processor.process();
//
//            // 测试Dijkstra算法
//            int seedX = 10, seedY = 10; // 种子点坐标
//            int targetX = 20, targetY = 20; // 目标点坐标
//            List<Node> path = processor.computeShortestPath(seedX, seedY, targetX, targetY);
//
//            // 打印路径
//            System.out.println("Shortest Path from (" + seedX + ", " + seedY + ") to (" + targetX + ", " + targetY + "):");
//            if (path.isEmpty()) {
//                System.out.println("No path found!");
//            } else {
//                for (Node node : path) {
//                    System.out.println("(" + node.x + ", " + node.y + ")");
//                }
//                System.out.println("Path length: " + path.size() + " nodes");
//            }
//        } catch (IOException e) {
//            System.err.println("Error: " + e.getMessage());
//        }
//    }
