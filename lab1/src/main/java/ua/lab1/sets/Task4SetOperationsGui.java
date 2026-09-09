package ua.lab1.sets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.Set;
import java.util.TreeSet;

/**
 * Завдання 4: програма, яка моделює операції A∪B, A∩B, A\B, A⊕B, Ā
 * над множинами у графічному режимі (діаграма Ейлера — Венна).
 */
public final class Task4SetOperationsGui extends JFrame {

    private enum Operation {
        UNION("A ∪ B"),
        INTERSECTION("A ∩ B"),
        DIFFERENCE_AB("A \\ B"),
        SYMMETRIC_DIFFERENCE("A ⊕ B"),
        COMPLEMENT_A("Ā");

        final String label;

        Operation(String label) {
            this.label = label;
        }
    }

    private final JTextField fieldA = new JTextField("1,2,3,4", 14);
    private final JTextField fieldB = new JTextField("3,4,5,6", 14);
    private final JTextField fieldUniverse = new JTextField("1,2,3,4,5,6,7,8", 14);
    private final JTextArea resultArea = new JTextArea(3, 40);
    private final VennPanel vennPanel = new VennPanel();
    private Operation currentOperation = Operation.UNION;

    public Task4SetOperationsGui() {
        super("Лабораторна робота №1 — Завдання 4. Операції над множинами (графічний режим)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        add(buildInputPanel(), BorderLayout.NORTH);
        add(vennPanel, BorderLayout.CENTER);
        add(buildResultPanel(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Множина A:"), gbc);
        gbc.gridx = 1;
        panel.add(fieldA, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Множина B:"), gbc);
        gbc.gridx = 1;
        panel.add(fieldB, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Універсум U (для Ā):"), gbc);
        gbc.gridx = 1;
        panel.add(fieldUniverse, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (Operation op : Operation.values()) {
            JButton button = new JButton(op.label);
            button.addActionListener(e -> {
                currentOperation = op;
                recompute();
            });
            buttons.add(button);
        }
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(buttons, gbc);

        return panel;
    }

    private JPanel buildResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        return panel;
    }

    private static Set<String> parse(JTextField field) {
        Set<String> set = new TreeSet<>();
        for (String token : field.getText().trim().split("[,\\s]+")) {
            if (!token.isEmpty()) {
                set.add(token);
            }
        }
        return set;
    }

    private void recompute() {
        Set<String> a = parse(fieldA);
        Set<String> b = parse(fieldB);
        Set<String> universe = parse(fieldUniverse);

        Set<String> result = switch (currentOperation) {
            case UNION -> SetOperations.union(a, b);
            case INTERSECTION -> SetOperations.intersection(a, b);
            case DIFFERENCE_AB -> SetOperations.difference(a, b);
            case SYMMETRIC_DIFFERENCE -> SetOperations.symmetricDifference(a, b);
            case COMPLEMENT_A -> SetOperations.complement(a, universe);
        };

        resultArea.setText(currentOperation.label + " = " + ConsoleIO.formatSet(result));
        vennPanel.setOperation(currentOperation);
    }

    /** Малює діаграму Ейлера — Венна і зафарбовує область, що відповідає поточній операції. */
    private static final class VennPanel extends JPanel {
        private Operation operation = Operation.UNION;

        VennPanel() {
            setPreferredSize(new Dimension(480, 320));
            setBackground(Color.WHITE);
        }

        void setOperation(Operation operation) {
            this.operation = operation;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            Rectangle2D universe = new Rectangle2D.Double(20, 20, width - 40, height - 40);
            Ellipse2D circleA = new Ellipse2D.Double(width * 0.20, height * 0.25, width * 0.42, height * 0.5);
            Ellipse2D circleB = new Ellipse2D.Double(width * 0.42, height * 0.25, width * 0.42, height * 0.5);

            Area shaded = switch (operation) {
                case UNION -> {
                    Area area = new Area(circleA);
                    area.add(new Area(circleB));
                    yield area;
                }
                case INTERSECTION -> {
                    Area area = new Area(circleA);
                    area.intersect(new Area(circleB));
                    yield area;
                }
                case DIFFERENCE_AB -> {
                    Area area = new Area(circleA);
                    area.subtract(new Area(circleB));
                    yield area;
                }
                case SYMMETRIC_DIFFERENCE -> {
                    Area area = new Area(circleA);
                    area.exclusiveOr(new Area(circleB));
                    yield area;
                }
                case COMPLEMENT_A -> {
                    Area area = new Area(universe);
                    area.subtract(new Area(circleA));
                    yield area;
                }
            };

            g2.setColor(Color.BLACK);
            g2.draw(universe);
            g2.drawString("U", (float) (universe.getMaxX() - 20), (float) (universe.getMinY() + 18));

            g2.setColor(new Color(255, 140, 0, 130));
            g2.fill(shaded);

            g2.setColor(Color.BLACK);
            g2.draw(circleA);
            g2.draw(circleB);
            g2.drawString("A", (float) (circleA.getX() + 15), (float) (circleA.getY() + 30));
            g2.drawString("B", (float) (circleB.getMaxX() - 25), (float) (circleB.getY() + 30));
            g2.drawString(operation.label, 20, height - 8);

            g2.dispose();
        }
    }

    /** Запускає GUI; якщо середовище без дисплея (headless), виводить пояснення замість краху. */
    public static void launch() {
        ConsoleIO.useUtf8Console();
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Графічний режим недоступний: середовище без дисплея (headless). "
                    + "Запустіть застосунок на комп'ютері з графічним інтерфейсом.");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            Task4SetOperationsGui gui = new Task4SetOperationsGui();
            gui.recompute();
            gui.setVisible(true);
        });
    }

    public static void main(String[] args) {
        launch();
    }
}
