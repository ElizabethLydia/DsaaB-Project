import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class IntelligentScissorsGUI extends JFrame {
    private IntelligentScissorsPart1 processor;
    private BufferedImage originalImage;
    private BufferedImage gradientImage;
    private JLabel imageLabel;
    private Node seedNode;
    private List<Node> path;
    private boolean showGradient = false;

    public IntelligentScissorsGUI() {
        setTitle("Intelligent Scissors");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 图像显示区域
        imageLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setStroke(new BasicStroke(2)); // 加粗路径线条
                if (path != null && !path.isEmpty()) {
                    g2d.setColor(Color.RED);
                    for (int i = 1; i < path.size(); i++) {
                        Node n1 = path.get(i - 1);
                        Node n2 = path.get(i);
                        g2d.drawLine(n1.x, n1.y, n2.x, n2.y);
                    }
                }
                if (seedNode != null) {
                    g2d.setColor(Color.GREEN);
                    g2d.fillOval(seedNode.x - 3, seedNode.y - 3, 6, 6);
                }
            }
        };
        add(new JScrollPane(imageLabel), BorderLayout.CENTER);

        // 工具栏
        JToolBar toolbar = new JToolBar();
        JButton loadButton = new JButton("Load Image");
        loadButton.addActionListener(e -> loadImage());
        toolbar.add(loadButton);
        JButton gradientButton = new JButton("Show Gradient");
        gradientButton.addActionListener(e -> {
            if (processor != null) {
                showGradient = true;
                imageLabel.setIcon(new ImageIcon(gradientImage));
                imageLabel.repaint();
            }
        });
        toolbar.add(gradientButton);
        JButton originalButton = new JButton("Show Original");
        originalButton.addActionListener(e -> {
            if (processor != null) {
                showGradient = false;
                imageLabel.setIcon(new ImageIcon(originalImage));
                imageLabel.repaint();
            }
        });
        toolbar.add(originalButton);
        add(toolbar, BorderLayout.NORTH);

        JButton saveButton = new JButton("Save Path");
        saveButton.addActionListener(e -> {
            if (path != null && !path.isEmpty()) {
                BufferedImage output = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = output.createGraphics();
                g2d.drawImage(originalImage, 0, 0, null);
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(2));
                for (int i = 1; i < path.size(); i++) {
                    Node n1 = path.get(i - 1);
                    Node n2 = path.get(i);
                    g2d.drawLine(n1.x, n1.y, n2.x, n2.y);
                }
                g2d.dispose();
                try {
                    ImageIO.write(output, "png", new File("path_output.png"));
                    JOptionPane.showMessageDialog(this, "Path saved to path_output.png");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving image: " + ex.getMessage());
                }
            }
        });
        toolbar.add(saveButton);

        // 鼠标事件
        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (processor != null && originalImage != null) {
                    if (e.getButton() == MouseEvent.BUTTON3) { // 右键重置
                        seedNode = null;
                        path = null;
                        imageLabel.repaint();
                    } else if (e.getButton() == MouseEvent.BUTTON1) { // 左键选种子点
                        int x = e.getX();
                        int y = e.getY();
                        if (x >= 0 && x < originalImage.getWidth() && y >= 0 && y < originalImage.getHeight()) {
                            seedNode = processor.getGraph()[y][x];
                            path = null;
                            imageLabel.repaint();
                        }
                    }
                }
            }
        });
        imageLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (processor != null && originalImage != null && seedNode != null) {
                    int x = e.getX();
                    int y = e.getY();
                    if (x >= 0 && x < originalImage.getWidth() && y >= 0 && y < originalImage.getHeight()) {
                        path = processor.computeShortestPath(seedNode.x, seedNode.y, x, y);
                        imageLabel.repaint();
                    }
                }
                // 显示鼠标坐标
                setTitle("Intelligent Scissors - (" + e.getX() + ", " + e.getY() + ")");
            }
        });

        setSize(800, 600);
        setLocationRelativeTo(null);
    }

    private void loadImage() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                originalImage = ImageIO.read(file);
                processor = new IntelligentScissorsPart1(file.getAbsolutePath());
                processor.process();
                gradientImage = processor.getGradientImage();
                imageLabel.setIcon(new ImageIcon(originalImage));
                showGradient = false;
                seedNode = null;
                path = null;
                imageLabel.repaint();
                setTitle("Intelligent Scissors");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error loading image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new IntelligentScissorsGUI().setVisible(true);
        });
    }
}

//import javax.imageio.ImageIO;
//import javax.swing.*;
//        import java.awt.*;
//        import java.awt.event.*;
//        import java.awt.image.BufferedImage;
//import java.io.File;
//import java.io.IOException;
//import java.util.List;
//
//public class IntelligentScissorsGUI extends JFrame {
//    private IntelligentScissorsPart1 processor;
//    private BufferedImage originalImage;
//    private BufferedImage gradientImage;
//    private JLabel imageLabel;
//    private Node seedNode;
//    private List<Node> path;
//    private boolean showGradient = false;
//    private boolean fitWindow = true; // 默认适应窗口
//    private double scaleX = 1.0, scaleY = 1.0; // 缩放比例
//
//    public IntelligentScissorsGUI() {
//        setTitle("Intelligent Scissors");
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        setLayout(new BorderLayout());
//
//        // 图像显示区域
//        imageLabel = new JLabel() {
//            @Override
//            protected void paintComponent(Graphics g) {
//                super.paintComponent(g);
//                Graphics2D g2d = (Graphics2D) g;
//                g2d.setStroke(new BasicStroke(2));
//                if (path != null && !path.isEmpty()) {
//                    g2d.setColor(Color.RED);
//                    for (int i = 1; i < path.size(); i++) {
//                        Node n1 = path.get(i - 1);
//                        Node n2 = path.get(i);
//                        // 按缩放比例绘制路径
//                        int x1 = (int) (n1.x * scaleX);
//                        int y1 = (int) (n1.y * scaleY);
//                        int x2 = (int) (n2.x * scaleX);
//                        int y2 = (int) (n2.y * scaleY);
//                        g2d.drawLine(x1, y1, x2, y2);
//                    }
//                }
//                if (seedNode != null) {
//                    g2d.setColor(Color.GREEN);
//                    int x = (int) (seedNode.x * scaleX);
//                    int y = (int) (seedNode.y * scaleY);
//                    g2d.fillOval(x - 3, y - 3, 6, 6);
//                }
//            }
//        };
//        JScrollPane scrollPane = new JScrollPane(imageLabel);
//        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
//        add(scrollPane, BorderLayout.CENTER);
//
//        // 工具栏
//        JToolBar toolbar = new JToolBar();
//        JButton loadButton = new JButton("Load Image");
//        loadButton.addActionListener(e -> loadImage());
//        toolbar.add(loadButton);
//        JButton gradientButton = new JButton("Show Gradient");
//        gradientButton.addActionListener(e -> {
//            if (processor != null && gradientImage != null) {
//                showGradient = true;
//                updateImageDisplay();
//            }
//        });
//        toolbar.add(gradientButton);
//        JButton originalButton = new JButton("Show Original");
//        originalButton.addActionListener(e -> {
//            if (processor != null && originalImage != null) {
//                showGradient = false;
//                updateImageDisplay();
//            }
//        });
//        toolbar.add(originalButton);
//        JButton fitButton = new JButton("Fit Window");
//        fitButton.addActionListener(e -> {
//            fitWindow = true;
//            updateImageDisplay();
//        });
//        toolbar.add(fitButton);
//        JButton originalSizeButton = new JButton("Original Size");
//        originalSizeButton.addActionListener(e -> {
//            fitWindow = false;
//            updateImageDisplay();
//        });
//        toolbar.add(originalSizeButton);
//        add(toolbar, BorderLayout.NORTH);
//
//        // 鼠标事件
//        imageLabel.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                if (processor != null && originalImage != null) {
//                    if (e.getButton() == MouseEvent.BUTTON3) { // 右键重置
//                        seedNode = null;
//                        path = null;
//                        imageLabel.repaint();
//                    } else if (e.getButton() == MouseEvent.BUTTON1) { // 左键选种子点
//                        // 转换为原始坐标
//                        int x = (int) (e.getX() / scaleX);
//                        int y = (int) (e.getY() / scaleY);
//                        if (x >= 0 && x < originalImage.getWidth() && y >= 0 && y < originalImage.getHeight()) {
//                            seedNode = processor.getGraph()[y][x];
//                            path = null;
//                            imageLabel.repaint();
//                        }
//                    }
//                }
//            }
//        });
//        imageLabel.addMouseMotionListener(new MouseMotionAdapter() {
//            @Override
//            public void mouseMoved(MouseEvent e) {
//                if (processor != null && originalImage != null && seedNode != null) {
//                    // 转换为原始坐标
//                    int x = (int) (e.getX() / scaleX);
//                    int y = (int) (e.getY() / scaleY);
//                    if (x >= 0 && x < originalImage.getWidth() && y >= 0 && y < originalImage.getHeight()) {
//                        path = processor.computeShortestPath(seedNode.x, seedNode.y, x, y);
//                        imageLabel.repaint();
//                    }
//                }
//                // 显示原始坐标
//                int x = (int) (e.getX() / scaleX);
//                int y = (int) (e.getY() / scaleY);
//                setTitle("Intelligent Scissors - (" + x + ", " + y + ")");
//            }
//        });
//
//        setSize(800, 600);
//        setLocationRelativeTo(null);
//    }
//
//    private void loadImage() {
//        JFileChooser chooser = new JFileChooser();
//        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
//            try {
//                File file = chooser.getSelectedFile();
//                originalImage = ImageIO.read(file);
//                processor = new IntelligentScissorsPart1(file.getAbsolutePath());
//                processor.process();
//                gradientImage = processor.getGradientImage();
//                fitWindow = true; // 默认适应窗口
//                updateImageDisplay();
//                seedNode = null;
//                path = null;
//                setTitle("Intelligent Scissors");
//            } catch (IOException ex) {
//                JOptionPane.showMessageDialog(this, "Error loading image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
//            }
//        }
//    }
//
//    private void updateImageDisplay() {
//        if (originalImage == null) return;
//        BufferedImage displayImage = showGradient ? gradientImage : originalImage;
//        if (fitWindow) {
//            // 计算缩放比例
//            int maxWidth = getWidth() - 50; // 留边距
//            int maxHeight = getHeight() - 100; // 留工具栏和边距
//            double scale = Math.min((double) maxWidth / displayImage.getWidth(),
//                    (double) maxHeight / displayImage.getHeight());
//            scaleX = scale;
//            scaleY = scale;
//            int scaledWidth = (int) (displayImage.getWidth() * scale);
//            int scaledHeight = (int) (displayImage.getHeight() * scale);
//            Image scaled = displayImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
//            imageLabel.setIcon(new ImageIcon(scaled));
//            imageLabel.setPreferredSize(new Dimension(scaledWidth, scaledHeight));
//        } else {
//            // 原始尺寸
//            scaleX = 1.0;
//            scaleY = 1.0;
//            imageLabel.setIcon(new ImageIcon(displayImage));
//            imageLabel.setPreferredSize(new Dimension(displayImage.getWidth(), displayImage.getHeight()));
//        }
//        imageLabel.revalidate();
//        imageLabel.repaint();
//    }
//
//    @Override
//    public void setSize(int width, int height) {
//        super.setSize(width, height);
//        updateImageDisplay(); // 窗口大小变化时更新图像
//    }
//
//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> {
//            new IntelligentScissorsGUI().setVisible(true);
//        });
//    }
//}