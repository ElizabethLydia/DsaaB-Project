import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IntelligentScissorsGUI2 extends JFrame {
    private IntelligentScissorsPart1 processor;
    private BufferedImage originalImage;
    private BufferedImage gradientImage;
    private JLabel imageLabel;
    private List<Node> seedNodes;
    private List<List<Node>> paths;
    private boolean showGradient = false;
    private boolean fitWindow = true;
    private double scaleX = 1.0, scaleY = 1.0;

    public IntelligentScissorsGUI2() {
        setTitle("Intelligent Scissors");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        seedNodes = new ArrayList<>();
        paths = new ArrayList<>();

        imageLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setStroke(new BasicStroke(2));
                g2d.setColor(Color.RED);
                for (List<Node> path : paths) {
                    for (int i = 1; i < path.size(); i++) {
                        Node n1 = path.get(i - 1);
                        Node n2 = path.get(i);
//                        int x1 = (int) (n1.x * scaleX);
//                        int y1 = (int) (n1.y * scaleY);
//                        int x2 = (int) (n2.x * scaleX);
//                        int y2 = (int) (n2.y * scaleY);
                        int x1 = n1.x;
                        int y1 = n1.y;
                        int x2 = n2.x;
                        int y2 = n2.y;
                        g2d.drawLine(x1, y1, x2, y2);
                    }
                }
                g2d.setColor(Color.GREEN);
                for (Node seed : seedNodes) {
//                    int x = (int) (seed.x * scaleX);
//                    int y = (int) (seed.y * scaleY);
                    int x = seed.x;
                    int y = seed.y;
                    g2d.fillOval(x - 3, y - 3, 6, 6);
                }
            }
        };

        //将 imageLabel 包装进 JScrollPane 中，让图像支持滚动查看，并放在中间区域。
        JScrollPane scrollPane = new JScrollPane(imageLabel);
        add(scrollPane, BorderLayout.CENTER);

        //让用户可以把图片文件拖入窗口以加载图像。
        imageLabel.setDropTarget(new DropTarget(imageLabel, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        File file = files.get(0);
                        loadImage(file);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(IntelligentScissorsGUI2.this, "Error dropping file: " + ex.getMessage());
                }
            }
        }));

        JToolBar toolbar = new JToolBar();
        JButton loadButton = new JButton("Load Image");
        loadButton.addActionListener(e -> loadImage());
        toolbar.add(loadButton);
        JButton gradientButton = new JButton("Show Gradient");
        gradientButton.addActionListener(e -> {
            showGradient = true;
            updateImageDisplay();
        });
        toolbar.add(gradientButton);
        JButton originalButton = new JButton("Show Original");
        originalButton.addActionListener(e -> {
            showGradient = false;
            //updateImageDisplay();
            refreshProcessor();
        });
        toolbar.add(originalButton);
        JButton fitButton = new JButton("Fit Window");
        fitButton.addActionListener(e -> {
            fitWindow = true;
            //updateImageDisplay();
            refreshProcessor();
        });
        toolbar.add(fitButton);
        JButton originalSizeButton = new JButton("Original Size");
        originalSizeButton.addActionListener(e -> {
            fitWindow = false;
            //updateImageDisplay();
            refreshProcessor();
        });
        toolbar.add(originalSizeButton);
        add(toolbar, BorderLayout.NORTH);

//        imageLabel.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                if (processor != null) {
//                    if (e.getButton() == MouseEvent.BUTTON3) {
//                        seedNodes.clear();
//                        paths.clear();
//                        imageLabel.repaint();
//                    } else if (e.getButton() == MouseEvent.BUTTON1) {
////                        int rawX = e.getX();
////                        int rawY = e.getY();
////
////                        int x = fitWindow ? rawX : (int) (rawX / scaleX);
////                        int y = fitWindow ? rawY : (int) (rawY / scaleY);
//                        int x = e.getX();
//                        int y = e.getY();
//                        if (x >= 0 && x < processor.getGraph()[0].length && y >= 0 && y < processor.getGraph().length) {
//                            Node newSeed = processor.getGraph()[y][x];
//                            seedNodes.add(newSeed);
//                            if (seedNodes.size() >= 2) {
//                                Node prev = seedNodes.get(seedNodes.size() - 2);
//                                List<Node> path = processor.computeShortestPath(prev.x, prev.y, newSeed.x, newSeed.y);
//                                paths.add(path);
//                            }
//                            imageLabel.repaint();
//                        }
//                    }
//                }
//            }
//        });

        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (processor != null) {
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        seedNodes.clear();
                        paths.clear();
                        imageLabel.repaint();
                    } else if (e.getButton() == MouseEvent.BUTTON1) {
                        int x = e.getX();  // 直接使用点击位置
                        int y = e.getY();
                        BufferedImage currentImage = fitWindow ? getScaledImage(originalImage) : originalImage;
                        if (x >= 0 && x < currentImage.getWidth() && y >= 0 && y < currentImage.getHeight()) {
                            Node newSeed = processor.getGraph()[y][x];
                            seedNodes.add(newSeed);
                            if (seedNodes.size() >= 2) {
                                Node prev = seedNodes.get(seedNodes.size() - 2);
                                List<Node> path = processor.computeShortestPath(prev.x, prev.y, newSeed.x, newSeed.y);
                                paths.add(path);
                            }
                            imageLabel.repaint();
                        }
                    }
                }
            }
        });

        setSize(800, 600);
        setLocationRelativeTo(null);
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
            BufferedImage imageToProcess = fitWindow ? getScaledImage(originalImage) : originalImage;
            processor = new IntelligentScissorsPart1(imageToProcess);
            processor.process();
            gradientImage = processor.getGradientImage();
            updateImageDisplay();
            seedNodes.clear();
            paths.clear();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error loading image: " + ex.getMessage());
        }
    }
    private void refreshProcessor() {
        try {
            if (originalImage == null) return;
            BufferedImage imageToProcess = fitWindow ? getScaledImage(originalImage) : originalImage;
            processor = new IntelligentScissorsPart1(imageToProcess);
            processor.process();
            gradientImage = processor.getGradientImage();
            seedNodes.clear();
            paths.clear();
            updateImageDisplay();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error refreshing image: " + ex.getMessage());
        }
    }

    private BufferedImage getScaledImage(BufferedImage img) {
        int maxWidth = getWidth() - 50;
        int maxHeight = getHeight() - 100;
        double scale = Math.min((double) maxWidth / img.getWidth(), (double) maxHeight / img.getHeight());
        int newW = (int) (img.getWidth() * scale);
        int newH = (int) (img.getHeight() * scale);
        Image scaled = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage buffered = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = buffered.createGraphics();
        g2d.drawImage(scaled, 0, 0, null);
        g2d.dispose();
        return buffered;
    }

    private void updateImageDisplay() {
        if (originalImage == null) return;
        BufferedImage displayImage = showGradient ? gradientImage : originalImage;
        if (fitWindow) {
//            int maxWidth = getWidth() - 50;
//            int maxHeight = getHeight() - 100;
//            double scale = Math.min((double) maxWidth / displayImage.getWidth(),
//                    (double) maxHeight / displayImage.getHeight());
//            int scaledWidth = (int) (displayImage.getWidth() * scale);
//            int scaledHeight = (int) (displayImage.getHeight() * scale);
//            Image scaled = displayImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
//            imageLabel.setIcon(new ImageIcon(scaled));
//            imageLabel.setPreferredSize(new Dimension(scaledWidth, scaledHeight));
//            scaleX = (double) scaledWidth / originalImage.getWidth();
//            scaleY = (double) scaledHeight / originalImage.getHeight();
            BufferedImage scaledBuffered = getScaledImage(displayImage);
            imageLabel.setIcon(new ImageIcon(scaledBuffered));
            imageLabel.setPreferredSize(new Dimension(scaledBuffered.getWidth(), scaledBuffered.getHeight()));
            scaleX = (double) scaledBuffered.getWidth() / originalImage.getWidth();
            scaleY = (double) scaledBuffered.getHeight() / originalImage.getHeight();
        } else {
            imageLabel.setIcon(new ImageIcon(displayImage));
            imageLabel.setPreferredSize(new Dimension(displayImage.getWidth(), displayImage.getHeight()));
            scaleX = 1.0;
            scaleY = 1.0;
        }
        imageLabel.revalidate();
        imageLabel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new IntelligentScissorsGUI().setVisible(true));
    }
}
