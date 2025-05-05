import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class IntelligentScissorsGUI extends JFrame {
    private IntelligentScissorsPart1 processor;
    private BufferedImage originalImage;
    private BufferedImage gradientImage;
    private JLabel imageLabel;
    private List<Node> seedNodes; // 存储多个种子点
    private List<List<Node>> paths; // 存储路径段
    private boolean showGradient = false;
    private boolean fitWindow = true; // 默认适应窗口
    private double scaleX = 1.0, scaleY = 1.0; // 缩放比例
    private boolean isDragging = true; // 控制鼠标移动事件的标志
    private boolean cursorSnapEnabled = false;  // 控制是否启用 Cursor Snap 功能
    private int displayWidth; // 图像显示宽度
    private int displayHeight; // 图像显示高度


    public IntelligentScissorsGUI() {
        setTitle("Intelligent Scissors");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        seedNodes = new ArrayList<>();
        paths = new ArrayList<>();
        setAlwaysOnTop(true);
//        // 移除 setAlwaysOnTop(true)，改为初始置前
//        toFront();
//        requestFocus();

        // 图像显示区域
        imageLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                // 如果没有图像，绘制提示
                if (originalImage == null) {
                    String text = "You can also drag and load image here";
                    Font font = new Font("微软雅黑", Font.PLAIN, 24);
                    g2d.setFont(font);
                    FontMetrics fm = g2d.getFontMetrics(font);
                    int textWidth = fm.stringWidth(text);
                    int textHeight = fm.getHeight();
                    int x = (getWidth() - textWidth) / 2;
                    int y = (getHeight() + textHeight) / 2;
                    g2d.setColor(Color.GRAY);
                    g2d.drawString(text, x, y);
                    return;
                }

                g2d.setStroke(new BasicStroke(2));
                // 绘制所有路径段
                g2d.setColor(Color.RED);
                for (List<Node> path : paths) {
                    if (path != null && !path.isEmpty()) {
                        for (int i = 1; i < path.size(); i++) {
                            Node n1 = path.get(i - 1);
                            Node n2 = path.get(i);
                            int x1 = (int) (n1.x * scaleX);
                            int y1 = (int) (n1.y * scaleY);
                            int x2 = (int) (n2.x * scaleX);
                            int y2 = (int) (n2.y * scaleY);
                            g2d.drawLine(x1, y1, x2, y2);
                        }
                    }
                }
                // 绘制种子点
                g2d.setColor(Color.GREEN);
                for (Node seed : seedNodes) {
                    int x = (int) (seed.x * scaleX);
                    int y = (int) (seed.y * scaleY);
                    g2d.fillOval(x - 3, y - 3, 6, 6);
                }
            }
        };
        JScrollPane scrollPane = new JScrollPane(imageLabel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        // 添加拖拽支持
        imageLabel.setDropTarget(new DropTarget(imageLabel, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        File file = files.get(0);
                        if (file.getName().matches(".*\\.(png|jpg|jpeg|bmp)")) {
                            loadImage(file);
                        } else {
                            JOptionPane.showMessageDialog(IntelligentScissorsGUI.this, "Please drop an image file (PNG/JPG/BMP)");
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(IntelligentScissorsGUI.this, "Error dropping file: " + ex.getMessage());
                }
            }
        }));

        // 工具栏
        JToolBar toolbar = new JToolBar();
        JButton loadButton = new JButton("Load Image");
        loadButton.addActionListener(e -> loadImage());
        toolbar.add(loadButton);
        JButton gradientButton = new JButton("Show Gradient");
        gradientButton.addActionListener(e -> {
            if (processor != null && gradientImage != null) {
                showGradient = true;
                updateImageDisplay();
            }
        });
        toolbar.add(gradientButton);
        JButton originalButton = new JButton("Show Original");
        originalButton.addActionListener(e -> {
            if (processor != null && originalImage != null) {
                showGradient = false;
                updateImageDisplay();
            }
        });
        toolbar.add(originalButton);
        JButton fitButton = new JButton("Fit Window");
        fitButton.addActionListener(e -> {
            fitWindow = true;
            updateImageDisplay();
        });
        toolbar.add(fitButton);
        JButton originalSizeButton = new JButton("Original Size");
        originalSizeButton.addActionListener(e -> {
            fitWindow = false;
            updateImageDisplay();
        });
        toolbar.add(originalSizeButton);
//        JButton closePathButton = new JButton("Close Path");
//        closePathButton.addActionListener(e -> {
//            if (processor != null && seedNodes.size() >= 2) {
//                // 连接首尾种子点
//                Node first = seedNodes.get(0);
//                Node last = seedNodes.get(seedNodes.size() - 1);
//                List<Node> closingPath = processor.computeShortestPath(last.x, last.y, first.x, first.y);
//                if (!closingPath.isEmpty()) {
//                    paths.add(closingPath);
//                    imageLabel.repaint();
//                }
//            }
//        });
//        toolbar.add(closePathButton);


        JButton saveButton = new JButton("Save Path");
        saveButton.addActionListener(e -> {
            if (!paths.isEmpty()) {
                int width = originalImage.getWidth();
                int height = originalImage.getHeight();
                BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = output.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, width, height);
                GeneralPath combinedPath = new GeneralPath();
                boolean started = false;
                for (List<Node> path : paths) {
                    if (path != null && !path.isEmpty()) {
                        if (!started) {
                            combinedPath.moveTo(path.get(0).x, path.get(0).y);
                            started = true;
                        }
                        for (int i = 1; i < path.size(); i++) {
                            Node node = path.get(i);
                            combinedPath.lineTo(node.x, node.y);
                        }
                    }
                }
                combinedPath.closePath();
                Area clipArea = new Area(combinedPath);
                g2d.setClip(clipArea);
                g2d.drawImage(originalImage, 0, 0, null);
                g2d.setClip(null);
                g2d.dispose();
                File outputFile = new File("output.png");
                if (outputFile.exists()) {
                    outputFile.delete();
                }
                try {
                    System.out.println("Saving output.png...");
                    ImageIO.write(output, "png", outputFile);
                    System.out.println("Output file saved: " + outputFile.getAbsolutePath());
                    JOptionPane.showMessageDialog(this, "Path saved to output.png");
                    if (!outputFile.exists()) {
                        System.err.println("Error: output.png does not exist after saving.");
                        JOptionPane.showMessageDialog(this, "Error: output.png was not found.");
                        return;
                    }
                    System.out.println("Reading output.png...");
                    BufferedImage outputImage = ImageIO.read(outputFile);
                    if (outputImage == null) {
                        System.err.println("Error: Failed to read output.png.");
                        JOptionPane.showMessageDialog(this, "Error: Failed to read output.png.");
                        return;
                    }
                    SwingUtilities.invokeLater(() -> {
                        System.out.println("Creating output window...");
                        JFrame outputFrame = new JFrame("Output Image");
                        outputFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        // 缩放图像以匹配主 GUI 显示尺寸
                        Image scaledOutputImage = outputImage.getScaledInstance(displayWidth, displayHeight, Image.SCALE_SMOOTH);
                        JLabel outputLabel = new JLabel(new ImageIcon(scaledOutputImage));
                        outputFrame.add(new JScrollPane(outputLabel));
                        // 设置窗口尺寸
                        int frameWidth = displayWidth + 20; // 边框偏移
                        int frameHeight = displayHeight + 40; // 包含标题栏
                        outputFrame.setSize(frameWidth, frameHeight);
                        outputFrame.setLocationRelativeTo(null);
                        outputFrame.setAlwaysOnTop(true);
                        outputFrame.setVisible(true);
                        outputFrame.toFront();
                        outputFrame.requestFocus();
                        System.out.println("Output window displayed: " + frameWidth + "x" + frameHeight);
                    });
                } catch (IOException ex) {
                    System.err.println("Error saving or displaying image: " + ex.getMessage());
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error saving or displaying image: " + ex.getMessage());
                }
            } else {
                System.out.println("No paths to save.");
                JOptionPane.showMessageDialog(this, "No paths to save.");
            }
        });
        toolbar.add(saveButton);

        JCheckBox snapCheckbox = new JCheckBox("Cursor Snap");//表示一个开关状态(启用 / 禁用 边缘吸附)
        snapCheckbox.addActionListener(e -> {
            cursorSnapEnabled = snapCheckbox.isSelected();
        });
        toolbar.add(snapCheckbox);
        add(toolbar, BorderLayout.NORTH);



        // 鼠标事件
        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (processor != null && originalImage != null) {
                    if (e.getButton() == MouseEvent.BUTTON3) { // 右键重置
                        seedNodes.clear();
                        paths.clear();
                        imageLabel.repaint();
                    } else if (e.getButton() == MouseEvent.BUTTON1) { // 左键添加种子点
                        // 获取鼠标坐标并转换为原始图像坐标
                        int x = (int) (e.getX() / scaleX);
                        int y = (int) (e.getY() / scaleY);

                        // 边界检查
                        if (x < 0 || x >= originalImage.getWidth() || y < 0 || y >= originalImage.getHeight()) {
                            return;
                        }

                        // 启用 Cursor Snap，调整到最强边缘点
                        if (cursorSnapEnabled) {
                            int[] snapped = processor.findStrongestEdgeInNeighborhood(x, y, 25);
                            x = snapped[0];
                            y = snapped[1];
                        }

                        // 创建新种子点
                        Node newSeed = processor.getGraph()[y][x];
                        seedNodes.add(newSeed);

                        if (e.getClickCount() >= 2) { // 双击闭合路径
                            System.out.println("Double-click: Closing path...");
                            if (seedNodes.size() >= 2) {
                                Node first = seedNodes.get(0);
                                Node last = seedNodes.get(seedNodes.size() - 1);
                                // 使用吸附点计算闭合路径
                                List<Node> closingPath = processor.computeShortestPath(last.x, last.y, first.x, first.y);
                                if (!closingPath.isEmpty()) {
                                    paths.add(closingPath);
                                }
                                imageLabel.repaint();
                                isDragging = false;
                                saveButton.doClick();
                            }
                        } else {
                            // 单击：添加路径到上一个种子点
                            if (seedNodes.size() >= 2) {
                                Node prevSeed = seedNodes.get(seedNodes.size() - 2);
                                List<Node> path = processor.computeShortestPath(prevSeed.x, prevSeed.y, newSeed.x, newSeed.y);
                                if (!path.isEmpty()) {
                                    paths.add(path);
                                }
                            }
                            imageLabel.repaint();
                        }
                        System.out.println("Seed added: (" + x + ", " + y + ")");
                    }
                }
            }
        });
        imageLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (processor != null && originalImage != null && !seedNodes.isEmpty() && isDragging) {
                    // 获取鼠标坐标并转换为原始图像坐标
                    int x = (int) (e.getX() / scaleX);
                    int y = (int) (e.getY() / scaleY);

                    // 边界检查
                    if (x < 0 || x >= originalImage.getWidth() || y < 0 || y >= originalImage.getHeight()) {
                        return;
                    }

                    // 启用 Cursor Snap，调整到最强边缘点
                    if (cursorSnapEnabled) {
                        int[] adjusted = processor.findStrongestEdgeInNeighborhood(x, y, 25);
                        x = adjusted[0];
                        y = adjusted[1];
                    }

                    // 计算从最后一个种子点到吸附点的临时路径
                    Node lastSeed = seedNodes.get(seedNodes.size() - 1);
                    List<Node> tempPath = processor.computeShortestPath(lastSeed.x, lastSeed.y, x, y);

                    // 临时替换最后一个路径（不保存）
                    List<List<Node>> tempPaths = new ArrayList<>(paths);
                    if (!tempPaths.isEmpty()) {
                        tempPaths.remove(tempPaths.size() - 1);
                    }
                    tempPaths.add(tempPath);

                    // 绘制临时路径
                    imageLabel.repaint();

                    // 恢复 paths，避免影响保存
                    paths.clear();
                    paths.addAll(tempPaths.subList(0, tempPaths.size() - 1));
                    if (!tempPath.isEmpty()) {
                        paths.add(tempPath);
                    }
                }
            // 更新标题栏坐标
                int x = (int) (e.getX() / scaleX);
                int y = (int) (e.getY() / scaleY);
                setTitle("Intelligent Scissors - (" + x + ", " + y + ")");
            }
        });

        setSize(800, 600);
        setLocationRelativeTo(null);

        // 添加这个监听器
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateImageDisplay();
            }
        });
    }

    private void loadImage() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadImage(chooser.getSelectedFile());
        }
    }

    private void loadImage(File file) {
        try {
            originalImage = ImageIO.read(file);
            processor = new IntelligentScissorsPart1(file.getAbsolutePath());
            processor.process();
            gradientImage = processor.getGradientImage();
            fitWindow = true;
            updateImageDisplay();
            seedNodes.clear();
            paths.clear();
            setTitle("Intelligent Scissors");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error loading image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateImageDisplay() {
        if (originalImage == null) return;
        BufferedImage displayImage = showGradient ? gradientImage : originalImage;
        if (fitWindow) {
            JViewport viewport = ((JScrollPane) imageLabel.getParent().getParent()).getViewport();
            int maxWidth = viewport.getWidth();
            int maxHeight = viewport.getHeight();
            double scale = Math.min((double) maxWidth / displayImage.getWidth(),
                    (double) maxHeight / displayImage.getHeight());
            int scaledWidth = (int) (displayImage.getWidth() * scale);
            int scaledHeight = (int) (displayImage.getHeight() * scale);
            Image scaled = displayImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaled));
            imageLabel.setPreferredSize(new Dimension(scaledWidth, scaledHeight));
            scaleX = (double) scaledWidth / originalImage.getWidth();
            scaleY = (double) scaledHeight / originalImage.getHeight();
            displayWidth = scaledWidth;
            displayHeight = scaledHeight;
        } else {
            scaleX = 1.0;
            scaleY = 1.0;
            imageLabel.setIcon(new ImageIcon(displayImage));
            imageLabel.setPreferredSize(new Dimension(displayImage.getWidth(), displayImage.getHeight()));
            displayWidth = displayImage.getWidth();
            displayHeight = displayImage.getHeight();
        }
        imageLabel.revalidate();
        imageLabel.repaint();
        System.out.println("Display size updated: " + displayWidth + "x" + displayHeight);
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        updateImageDisplay();
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new IntelligentScissorsGUI().setVisible(true);
        });
    }
}

