import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import edu.princeton.cs.algs4.IndexMinPQ;
import edu.princeton.cs.algs4.Stack;

class Node {
    int x, y;
    double f_G;
    List<Link> neighbors;

    public Node(int x, int y, double f_G) {
        this.x = x;
        this.y = y;
        this.f_G = f_G;
        this.neighbors = new ArrayList<>();
    }
}

class Link {
    Node target;
    double cost;

    public Link(Node target, double cost) {
        this.target = target;
        this.cost = cost;
    }
}

// KD-tree node for high-gradient pixels
class KDNode {
    int x, y;
    double gradient;
    KDNode left, right;

    KDNode(int x, int y, double gradient) {
        this.x = x;
        this.y = y;
        this.gradient = gradient;
    }
}

class KDTree {
    private KDNode root;
    private int size;

    public void insert(int x, int y, double gradient) {
        root = insert(root, x, y, gradient, 0);
        size++;
    }

    private KDNode insert(KDNode node, int x, int y, double gradient, int depth) {
        if (node == null) return new KDNode(x, y, gradient);
        int cd = depth % 2;
        double cmp = cd == 0 ? x - node.x : y - node.y;
        if (cmp < 0) {
            node.left = insert(node.left, x, y, gradient, depth + 1);
        } else {
            node.right = insert(node.right, x, y, gradient, depth + 1);
        }
        return node;
    }

    public KDNode findStrongestInRange(int cx, int cy, int windowSize) {
        double half = windowSize / 2.0;
        PriorityQueue<KDNode> pq = new PriorityQueue<>((a, b) -> Double.compare(b.gradient, a.gradient));
        rangeSearch(root, cx, cy, half, pq, 0);
        return pq.isEmpty() ? null : pq.poll();
    }

    private void rangeSearch(KDNode node, int cx, int cy, double half, PriorityQueue<KDNode> pq, int depth) {
        if (node == null) return;
        double dx = node.x - cx;
        double dy = node.y - cy;
        if (Math.abs(dx) <= half && Math.abs(dy) <= half) {
            pq.offer(node);
        }
        int cd = depth % 2;
        double diff = cd == 0 ? dx : dy;
        if (diff < 0) {
            rangeSearch(node.left, cx, cy, half, pq, depth + 1);
            if (Math.abs(diff) <= half) {
                rangeSearch(node.right, cx, cy, half, pq, depth + 1);
            }
        } else {
            rangeSearch(node.right, cx, cy, half, pq, depth + 1);
            if (Math.abs(diff) <= half) {
                rangeSearch(node.left, cx, cy, half, pq, depth + 1);
            }
        }
    }
}

public class IntelligentScissorsPart1 {
    private static final int[][] SX = {{-3, 0, 3}, {-10, 0, 10}, {-3, 0, 3}};
    private static final int[][] SY = {{-3, -10, -3}, {0, 0, 0}, {3, 10, 3}};
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();

    private BufferedImage image;
    private int width, height;
    private int[][] pixels;
    private float[][] Ix, Iy, G, f_G;
    private Node[][] graph;
    private KDTree kdTree;

    public IntelligentScissorsPart1(String imagePath) throws IOException {
        this.image = ImageIO.read(new File(imagePath));
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.pixels = new int[height][width];
        this.Ix = new float[height][width];
        this.Iy = new float[height][width];
        this.G = new float[height][width];
        this.f_G = new float[height][width];
        this.graph = new Node[height][width];
        this.kdTree = new KDTree();
        loadPixels();
    }

    public IntelligentScissorsPart1(BufferedImage image) {
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.pixels = new int[height][width];
        this.Ix = new float[height][width];
        this.Iy = new float[height][width];
        this.G = new float[height][width];
        this.f_G = new float[height][width];
        this.graph = new Node[height][width];
        this.kdTree = new KDTree();
        loadPixels();
    }

    private void loadPixels() {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        int chunkSize = height / NUM_THREADS;

        for (int t = 0; t < NUM_THREADS; t++) {
            final int startY = t * chunkSize;
            final int endY = (t == NUM_THREADS - 1) ? height : (t + 1) * chunkSize;

            executor.execute(() -> {
                for (int y = startY; y < endY; y++) {
                    for (int x = 0; x < width; x++) {
                        int rgb = image.getRGB(x, y);
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;
                        pixels[y][x] = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                    }
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void applyGaussianBlur() {
        int[][] kernel = {{1, 2, 1}, {2, 4, 2}, {1, 2, 1}};
        float[][] temp = new float[height][width];

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        int chunkSize = (height - 2) / NUM_THREADS;

        for (int t = 0; t < NUM_THREADS; t++) {
            final int startY = 1 + t * chunkSize;
            final int endY = (t == NUM_THREADS - 1) ? height - 1 : 1 + (t + 1) * chunkSize;

            executor.execute(() -> {
                for (int y = startY; y < endY; y++) {
                    for (int x = 1; x < width - 1; x++) {
                        float sum = 0;
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dx = -1; dx <= 1; dx++) {
                                sum += pixels[y + dy][x + dx] * kernel[dy + 1][dx + 1];
                            }
                        }
                        temp[y][x] = sum / 16;
                    }
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Copy temp to pixels with float-to-int conversion
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                pixels[y][x] = Math.round(temp[y][x]);
                // Clamp to 0-255 range
                if (pixels[y][x] < 0) pixels[y][x] = 0;
                if (pixels[y][x] > 255) pixels[y][x] = 255;
            }
        }
    }

    private void computeGradients() {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        int chunkSize = (height - 2) / NUM_THREADS;

        for (int t = 0; t < NUM_THREADS; t++) {
            final int startY = 1 + t * chunkSize;
            final int endY = (t == NUM_THREADS - 1) ? height - 1 : 1 + (t + 1) * chunkSize;

            executor.execute(() -> {
                for (int y = startY; y < endY; y++) {
                    for (int x = 1; x < width - 1; x++) {
                        float sumX = 0;
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dx = -1; dx <= 1; dx++) {
                                sumX += pixels[y + dy][x + dx] * SX[dy + 1][dx + 1];
                            }
                        }
                        Ix[y][x] = sumX;

                        float sumY = 0;
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dx = -1; dx <= 1; dx++) {
                                sumY += pixels[y + dy][x + dx] * SY[dy + 1][dx + 1];
                            }
                        }
                        Iy[y][x] = sumY;
                    }
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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

    private void edgeEnhancement() {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        int chunkSize = (height - 2) / NUM_THREADS;

        for (int t = 0; t < NUM_THREADS; t++) {
            final int startY = 1 + t * chunkSize;
            final int endY = (t == NUM_THREADS - 1) ? height - 1 : 1 + (t + 1) * chunkSize;

            executor.execute(() -> {
                for (int y = startY; y < endY; y++) {
                    for (int x = 1; x < width - 1; x++) {
                        double angle = Math.atan2(Iy[y][x], Ix[y][x]);
                        float q = 255, r = 255;

                        if (angle <= Math.PI/8 || angle > 7*Math.PI/8) {
                            q = G[y][x+1];
                            r = G[y][x-1];
                        } else if (angle > Math.PI/8 && angle <= 3*Math.PI/8) {
                            q = G[y+1][x+1];
                            r = G[y-1][x-1];
                        } else if (angle > 3*Math.PI/8 && angle <= 5*Math.PI/8) {
                            q = G[y+1][x];
                            r = G[y-1][x];
                        } else {
                            q = G[y+1][x-1];
                            r = G[y-1][x+1];
                        }

                        if (G[y][x] >= q && G[y][x] >= r) {
                            G[y][x] = G[y][x];
                        } else {
                            G[y][x] = 0;
                        }
                    }
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void computeGradientMagnitude() {
        double[] localMaxG = new double[NUM_THREADS];
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        int chunkSize = height / NUM_THREADS;

        for (int t = 0; t < NUM_THREADS; t++) {
            final int threadId = t;
            final int startY = t * chunkSize;
            final int endY = (t == NUM_THREADS - 1) ? height : (t + 1) * chunkSize;

            executor.execute(() -> {
                double max = 0;
                for (int y = startY; y < endY; y++) {
                    for (int x = 0; x < width; x++) {
                        G[y][x] = (float) Math.sqrt(Ix[y][x] * Ix[y][x] + Iy[y][x] * Iy[y][x]);
                        if (G[y][x] > max) max = G[y][x];
                    }
                }
                localMaxG[threadId] = max;
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        double G_max = Arrays.stream(localMaxG).filter(max -> max >= 0).max().orElse(0);

        // Build KD-tree for high-gradient pixels
        double threshold = G_max * 0.1; // Top 10% gradients
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (G[y][x] > threshold) {
                    kdTree.insert(x, y, G[y][x]);
                }
            }
        }

        executor = Executors.newFixedThreadPool(NUM_THREADS);
        for (int t = 0; t < NUM_THREADS; t++) {
            final int startY = t * chunkSize;
            final int endY = (t == NUM_THREADS - 1) ? height : (t + 1) * chunkSize;

            executor.execute(() -> {
                for (int y = startY; y < endY; y++) {
                    for (int x = 0; x < width; x++) {
                        f_G[y][x] = G_max == 0 ? 0 : (float) ((G_max - G[y][x]) / G_max);
                    }
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void buildGraph() {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        int chunkSize = height / NUM_THREADS;

        for (int t = 0; t < NUM_THREADS; t++) {
            final int startY = t * chunkSize;
            final int endY = (t == NUM_THREADS - 1) ? height : (t + 1) * chunkSize;

            executor.execute(() -> {
                for (int y = startY; y < endY; y++) {
                    for (int x = 0; x < width; x++) {
                        graph[y][x] = new Node(x, y, f_G[y][x]);
                    }
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor = Executors.newFixedThreadPool(NUM_THREADS);
        int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int t = 0; t < NUM_THREADS; t++) {
            final int startY = t * chunkSize;
            final int endY = (t == NUM_THREADS - 1) ? height : (t + 1) * chunkSize;

            executor.execute(() -> {
                for (int y = startY; y < endY; y++) {
                    for (int x = 0; x < width; x++) {
                        Node node = graph[y][x];
                        for (int i = 0; i < 8; i++) {
                            int nx = x + dx[i];
                            int ny = y + dy[i];
                            if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                                boolean isDiag = dx[i] != 0 && dy[i] != 0;
                                double basecost = 1.0 / (1.0 + G[ny][nx]);
                                double cost = isDiag ? basecost * Math.sqrt(2) : basecost;
                                node.neighbors.add(new Link(graph[ny][nx], cost));
                            }
                        }
                    }
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public List<Node> computeShortestPath(int seedX, int seedY, int targetX, int targetY) {
        if (seedX < 0 || seedX >= width || seedY < 0 || seedY >= height ||
                targetX < 0 || targetX >= width || targetY < 0 || targetY >= height) {
            return new ArrayList<>();
        }

        int V = width * height;
        double[] distTo = new double[V];
        Node[] edgeTo = new Node[V];
        IndexMinPQ<Double> pq = new IndexMinPQ<>(V);

        for (int v = 0; v < V; v++) {
            distTo[v] = Double.POSITIVE_INFINITY;
        }

        int seedIndex = seedY * width + seedX;
        distTo[seedIndex] = 0.0;
        pq.insert(seedIndex, 0.0);

        while (!pq.isEmpty()) {
            int currentIndex = pq.delMin();
            int x = currentIndex % width;
            int y = currentIndex / width;

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

        int targetIndex = targetY * width + targetX;
        if (distTo[targetIndex] == Double.POSITIVE_INFINITY) {
            return new ArrayList<>();
        }

        Stack<Node> path = new Stack<>();
        for (Node current = graph[targetY][targetX];
             current != null && !(current.x == seedX && current.y == seedY);
             current = edgeTo[current.y * width + current.x]) {
            path.push(current);
        }
        path.push(graph[seedY][seedX]);

        List<Node> result = new ArrayList<>();
        while (!path.isEmpty()) {
            result.add(path.pop());
        }

        return result;
    }

    public int[] findStrongestEdgeInNeighborhood(int x, int y, int windowSize) {
        if (G == null) {
            throw new IllegalStateException("Gradient not computed. Call computeGradients() first.");
        }

        // Try KD-tree first
        KDNode strongest = kdTree.findStrongestInRange(x, y, windowSize);
        if (strongest != null) {
            return new int[] { strongest.x, strongest.y };
        }

        // Fallback to brute-force search
        int half = windowSize / 2;
        int bestX = x;
        int bestY = y;
        double maxGrad = -1.0;

        for (int dy = -half; dy <= half; dy++) {
            for (int dx = -half; dx <= half; dx++) {
                int nx = x + dx;
                int ny = y + dy;

                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    if (G[ny][nx] > maxGrad) {
                        maxGrad = G[ny][nx];
                        bestX = nx;
                        bestY = ny;
                    }
                }
            }
        }

        return new int[] { bestX, bestY };
    }

    private void saveToCSV(String outputDir) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String[] fileNames = {"pixels.csv", "Ix.csv", "Iy.csv", "G.csv", "f_G.csv"};
        float[][][] data = {toFloatArray(pixels), Ix, Iy, G, f_G};
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

    private float[][] toFloatArray(int[][] array) {
        float[][] result = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = array[y][x];
            }
        }
        return result;
    }

    public void process() throws IOException {
        applyGaussianBlur();
        computeGradients();
        edgeEnhancement();
        computeGradientMagnitude();
        buildGraph();
        saveToCSV("output");
    }

    public Node[][] getGraph() {
        return graph;
    }

    public float[][] getG() {
        return G;
    }

    public float[][] getFG() {
        return f_G;
    }

    public int[][] getPixels() {
        return pixels;
    }

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
                int rgb = (value << 16) | (value << 8) | value;
                gradientImage.setRGB(x, y, rgb);
            }
        }
        return gradientImage;
    }

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