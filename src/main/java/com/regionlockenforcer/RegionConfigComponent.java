package com.regionlockenforcer;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import java.awt.image.BufferedImage;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;
import javax.swing.JToggleButton;
import com.regionlockenforcer.Border.RenderMode;
import com.regionlockenforcer.RegionLockEnforcerConfig.PropStyle;

/**
 * Custom config component for managing regions.
 * Shows sections for Border and Teleports, with create/import buttons.
 */
@Slf4j
public class RegionConfigComponent extends JPanel
{
    private final RegionLockEnforcerPlugin plugin;
    private final TeleportRegistry teleportRegistry;
    private final JPanel borderListPanel;
    private final JPanel teleportsPanel;
    private final JPanel regionListPanel;
    private final JButton importButton;
    private final JButton exportRegionButton;
    private final ImageIcon editIcon;
    private final ImageIcon deleteIcon;
    private final ImageIcon addIcon;
    private final ImageIcon propsIcon;
    private Border styleDialogBorderHighlight = null;

    public RegionConfigComponent(RegionLockEnforcerPlugin plugin, @SuppressWarnings("unused") ConfigManager configManager, TeleportRegistry teleportRegistry)
    {
        super();
        this.plugin = plugin;
        this.teleportRegistry = teleportRegistry;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        setOpaque(true);
        this.editIcon = loadIcon("/edit_icon.png", this::createFallbackEditIcon);
        this.deleteIcon = loadIcon("/delete_icon.png", this::createFallbackDeleteIcon);
        this.addIcon = loadIcon("/add_icon.png", this::createFallbackAddIcon);
        this.propsIcon = loadIcon("/theme_icon.png", this::createFallbackPropsIcon);

        // Top buttons panel - Import Region, Export Region (stacked vertically)
        JPanel topButtonPanel = new JPanel();
        topButtonPanel.setLayout(new BoxLayout(topButtonPanel, BoxLayout.Y_AXIS));
        topButtonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        topButtonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 15, 0));
        
        // Create buttons with consistent styling
        Dimension buttonSize = new Dimension(180, 30);
        importButton = createStyledButton("Import Region", buttonSize, e -> importRegion());
        exportRegionButton = createStyledButton("Export Region", buttonSize, e -> exportCurrentRegion());
        
        // Center buttons horizontally
        JPanel importButtonPanel = new JPanel();
        importButtonPanel.setLayout(new BoxLayout(importButtonPanel, BoxLayout.X_AXIS));
        importButtonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        importButtonPanel.add(Box.createHorizontalGlue());
        importButtonPanel.add(importButton);
        importButtonPanel.add(Box.createHorizontalGlue());
        topButtonPanel.add(importButtonPanel);
        
        topButtonPanel.add(Box.createVerticalStrut(5));
        
        JPanel exportButtonPanel = new JPanel();
        exportButtonPanel.setLayout(new BoxLayout(exportButtonPanel, BoxLayout.X_AXIS));
        exportButtonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        exportButtonPanel.add(Box.createHorizontalGlue());
        exportButtonPanel.add(exportRegionButton);
        exportButtonPanel.add(Box.createHorizontalGlue());
        topButtonPanel.add(exportButtonPanel);
        
        add(topButtonPanel);
        
        // Region selection list
        JPanel regionLabelPanel = new JPanel();
        regionLabelPanel.setLayout(new BoxLayout(regionLabelPanel, BoxLayout.X_AXIS));
        regionLabelPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        regionLabelPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        JLabel regionListLabel = new JLabel("Select Region");
        regionListLabel.setForeground(ColorScheme.BRAND_ORANGE);
        regionListLabel.setFont(regionListLabel.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        regionListLabel.setAlignmentX(CENTER_ALIGNMENT);
        regionLabelPanel.add(Box.createHorizontalGlue());
        regionLabelPanel.add(regionListLabel);
        regionLabelPanel.add(Box.createHorizontalGlue());
        add(regionLabelPanel);
        
        regionListPanel = new JPanel();
        regionListPanel.setLayout(new BoxLayout(regionListPanel, BoxLayout.Y_AXIS));
        regionListPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        regionListPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        add(regionListPanel);

        // Configure Region section header
        JPanel configureRegionLabelPanel = new JPanel();
        configureRegionLabelPanel.setLayout(new BoxLayout(configureRegionLabelPanel, BoxLayout.X_AXIS));
        configureRegionLabelPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        configureRegionLabelPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        JLabel configureRegionLabel = new JLabel("Configure Region");
        configureRegionLabel.setForeground(ColorScheme.BRAND_ORANGE);
        configureRegionLabel.setFont(configureRegionLabel.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        configureRegionLabel.setAlignmentX(CENTER_ALIGNMENT);
        configureRegionLabelPanel.add(Box.createHorizontalGlue());
        configureRegionLabelPanel.add(configureRegionLabel);
        configureRegionLabelPanel.add(Box.createHorizontalGlue());
        add(configureRegionLabelPanel);

        // Border Section
        JPanel borderLabelPanel = new JPanel();
        borderLabelPanel.setLayout(new BoxLayout(borderLabelPanel, BoxLayout.X_AXIS));
        borderLabelPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        borderLabelPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));
        JLabel borderSectionLabel = new JLabel("Borders");
        borderSectionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        borderSectionLabel.setFont(borderSectionLabel.getFont().deriveFont(java.awt.Font.BOLD, 13f));
        borderSectionLabel.setAlignmentX(CENTER_ALIGNMENT);
        borderLabelPanel.add(Box.createHorizontalGlue());
        borderLabelPanel.add(borderSectionLabel);
        borderLabelPanel.add(Box.createHorizontalGlue());
        add(borderLabelPanel);
        
        // Dividing line below Draw Border heading
        JPanel borderDividerPanel = new JPanel();
        borderDividerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        borderDividerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR));
        borderDividerPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 1));
        borderDividerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        add(borderDividerPanel);
        
        borderListPanel = new JPanel();
        borderListPanel.setLayout(new BoxLayout(borderListPanel, BoxLayout.Y_AXIS));
        borderListPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        borderListPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        add(borderListPanel);

        // Teleports Section
        JPanel teleportsLabelPanel = new JPanel();
        teleportsLabelPanel.setLayout(new BoxLayout(teleportsLabelPanel, BoxLayout.X_AXIS));
        teleportsLabelPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        teleportsLabelPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
        JLabel teleportsSectionLabel = new JLabel("Teleport Allow List");
        teleportsSectionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        teleportsSectionLabel.setFont(teleportsSectionLabel.getFont().deriveFont(java.awt.Font.BOLD, 13f));
        teleportsSectionLabel.setAlignmentX(CENTER_ALIGNMENT);
        teleportsLabelPanel.add(Box.createHorizontalGlue());
        teleportsLabelPanel.add(teleportsSectionLabel);
        teleportsLabelPanel.add(Box.createHorizontalGlue());
        add(teleportsLabelPanel);
        
        teleportsPanel = new JPanel();
        teleportsPanel.setLayout(new BoxLayout(teleportsPanel, BoxLayout.Y_AXIS));
        teleportsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        teleportsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        add(teleportsPanel);

        refreshRegionList();
        refreshBorderList();
    }

    /**
     * Create a styled button with consistent appearance.
     */
    private JButton createStyledButton(String text, Dimension size, java.awt.event.ActionListener action)
    {
        JButton button = new JButton(text);
        button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        button.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        button.setFocusPainted(false);
        button.setPreferredSize(size);
        button.setMaximumSize(size);
        button.setMinimumSize(size);
        button.addActionListener(action);
        return button;
    }

    /**
     * Create a RuneLite-style toggle switch.
     */
    private JToggleButton createSwitchToggle(boolean selected, Runnable onEnable, Runnable onDisable)
    {
        JToggleButton toggle = new PaintedSwitchButton();
        toggle.setSelected(selected);
        toggle.addActionListener(e -> {
            boolean on = toggle.isSelected();
            if (on)
            {
                if (onEnable != null) onEnable.run();
            }
            else
            {
                if (onDisable != null) onDisable.run();
            }
        });
        return toggle;
    }

    /**
     * Toggle button that applies RuneLite's native toggle UI.
     */
    private static final class PaintedSwitchButton extends JToggleButton
    {
        // Sized and colored to closely match RuneLite toggles
        private static final Dimension SIZE = new Dimension(22, 13);
        private static final int TRACK_HEIGHT = 8;
        private static final int KNOB_SIZE = 10;
        private static final Color ACTIVE_TRACK = new Color(0x6C480B);
        private static final Color ACTIVE_KNOB = new Color(0xE98600);
        private static final Color INACTIVE_TRACK = new Color(0x3A3A3A);
        private static final Color INACTIVE_KNOB = new Color(0x5C5C5C);
        private static final Color ACTIVE_TRACK_HOVER = new Color(0x583F17);
        private static final Color ACTIVE_KNOB_HOVER = new Color(0xB06A00);
        private static final Color INACTIVE_TRACK_HOVER = new Color(0x353535);
        private static final Color INACTIVE_KNOB_HOVER = new Color(0x4D4D4D);

        private boolean hovered = false;

        private PaintedSwitchButton()
        {
            setPreferredSize(SIZE);
            setMinimumSize(SIZE);
            setMaximumSize(SIZE);
            setBorder(BorderFactory.createEmptyBorder());
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            updateTooltip();
            addMouseListener(new java.awt.event.MouseAdapter()
            {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e)
                {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e)
                {
                    hovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(java.awt.Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            boolean on = isSelected();
            Color track = on
                ? (hovered ? ACTIVE_TRACK_HOVER : ACTIVE_TRACK)
                : (hovered ? INACTIVE_TRACK_HOVER : INACTIVE_TRACK);
            Color knob = on
                ? (hovered ? ACTIVE_KNOB_HOVER : ACTIVE_KNOB)
                : (hovered ? INACTIVE_KNOB_HOVER : INACTIVE_KNOB);

            int trackX = 1;
            int trackY = (h - TRACK_HEIGHT) / 2;
            int trackWidth = w - 2;
            g2.setColor(track);
            g2.fillRoundRect(trackX, trackY, trackWidth, TRACK_HEIGHT, TRACK_HEIGHT, TRACK_HEIGHT);

            int knobX = on ? (trackX + trackWidth - KNOB_SIZE) : (trackX);
            int knobY = (h - KNOB_SIZE) / 2;

            g2.setColor(knob);
            g2.fillOval(knobX, knobY, KNOB_SIZE, KNOB_SIZE);

            g2.dispose();
        }

        @Override
        public void setSelected(boolean b)
        {
            super.setSelected(b);
            updateTooltip();
        }

        private void updateTooltip()
        {
            setToolTipText(isSelected() ? "Disable Region" : "Enable Region");
        }
    }

    private void openThemeDialog(Border border)
    {
        if (border == null)
        {
            return;
        }

        styleDialogBorderHighlight = border;
        refreshBorderList();

        JDialog dialog = new JDialog();
        dialog.setTitle("Border Style - " + border.getName());
        dialog.setModal(true);
        dialog.setAlwaysOnTop(true);
        dialog.setResizable(false);
        dialog.setSize(700, 420);
        dialog.setLocationRelativeTo(this);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Render mode radios
        JPanel modePanel = new JPanel();
        modePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        modePanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        modePanel.setLayout(new BoxLayout(modePanel, BoxLayout.Y_AXIS));
        modePanel.setAlignmentX(CENTER_ALIGNMENT);

        JRadioButton linesRadio = new JRadioButton("Lines");
        JRadioButton propsRadio = new JRadioButton("Props");
        ButtonGroup group = new ButtonGroup();
        group.add(linesRadio);
        group.add(propsRadio);

        linesRadio.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        propsRadio.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        linesRadio.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        propsRadio.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

        Border.RenderMode currentMode = border.getRenderMode();
        switch (currentMode)
        {
            case LINES:
                linesRadio.setSelected(true);
                break;
            default:
                propsRadio.setSelected(true);
                break;
        }

        linesRadio.setAlignmentX(CENTER_ALIGNMENT);
        propsRadio.setAlignmentX(CENTER_ALIGNMENT);
        modePanel.add(linesRadio);
        modePanel.add(propsRadio);

        // Color chooser for lines (embedded)
        JPanel colorPanel = new JPanel();
        colorPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        colorPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        colorPanel.setLayout(new BorderLayout());
        Color initial = border.getLineColor() != null ? border.getLineColor() : new Color(255, 255, 0, 220);

        javax.swing.JColorChooser chooser = new javax.swing.JColorChooser(initial);
        chooser.setPreviewPanel(new JPanel()); // minimal preview
        chooser.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        // Keep only swatches panel
        javax.swing.colorchooser.AbstractColorChooserPanel swatchPanel = null;
        for (var panel : chooser.getChooserPanels())
        {
            if (panel != null && "Swatches".equalsIgnoreCase(panel.getDisplayName()))
            {
                swatchPanel = panel;
                break;
            }
        }
        if (swatchPanel != null)
        {
            chooser.setChooserPanels(new javax.swing.colorchooser.AbstractColorChooserPanel[]{swatchPanel});
        }

        JPanel alphaPanel = new JPanel();
        alphaPanel.setLayout(new BoxLayout(alphaPanel, BoxLayout.X_AXIS));
        alphaPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JLabel alphaLabel = new JLabel("Opacity");
        alphaLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        JSlider alphaSlider = new JSlider(0, 255, initial.getAlpha());
        alphaSlider.setBackground(ColorScheme.DARK_GRAY_COLOR);
        alphaSlider.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        alphaSlider.setMajorTickSpacing(85);
        alphaSlider.setMinorTickSpacing(17);
        alphaSlider.setPaintTicks(true);

        JLabel alphaValue = new JLabel(String.valueOf(initial.getAlpha()));
        alphaValue.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        alphaPanel.add(alphaLabel);
        alphaPanel.add(Box.createHorizontalStrut(8));
        alphaPanel.add(alphaSlider);
        alphaPanel.add(Box.createHorizontalStrut(8));
        alphaPanel.add(alphaValue);

        alphaSlider.addChangeListener(e -> alphaValue.setText(String.valueOf(alphaSlider.getValue())));

        JPanel chooserContainer = new JPanel(new BorderLayout());
        chooserContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        chooserContainer.add(chooser, BorderLayout.CENTER);
        chooserContainer.add(alphaPanel, BorderLayout.SOUTH);

        colorPanel.add(chooserContainer, BorderLayout.CENTER);

        // Prop grid
        JPanel propPanel = new JPanel(new BorderLayout());
        propPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        propPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel grid = new JPanel(new GridLayout(0, 4, 4, 4));
        grid.setBackground(ColorScheme.DARK_GRAY_COLOR);

        PropStyle currentStyle = border.getPropStyle() != null ? border.getPropStyle() : PropStyle.SEA_ROCK;
        for (PropStyle style : PropStyle.values())
        {
            JButton b = new JButton();
            b.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            b.setFocusPainted(false);
            b.setToolTipText(getDisplayName(style));
            BufferedImage img = loadPropThumbnail(style);
            if (img != null)
            {
                java.awt.Image scaled = img.getScaledInstance(96, 96, java.awt.Image.SCALE_SMOOTH);
                b.setIcon(new javax.swing.ImageIcon(scaled));
            }
            else
            {
                b.setText(getDisplayName(style));
            }
            if (style == currentStyle)
            {
                b.setBorder(BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE, 2));
            }
            b.addActionListener(e -> {
                clearBorders(grid);
                b.setBorder(BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE, 2));
                border.setPropStyle(style);
            });
            grid.add(b);
        }

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        propPanel.add(scroll, BorderLayout.CENTER);

        // Buttons
        JPanel actions = new JPanel();
        actions.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JButton ok = new JButton("OK");
        ok.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        ok.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        ok.addActionListener(e -> {
            if (linesRadio.isSelected())
            {
                border.setRenderMode(RenderMode.LINES);
                Color chosen = chooser.getColor();
                int alpha = alphaSlider.getValue();
                if (chosen != null)
                {
                    border.setLineColor(new Color(chosen.getRed(), chosen.getGreen(), chosen.getBlue(), alpha));
                }
            }
            else
            {
                border.setRenderMode(RenderMode.PROPS);
            }
            plugin.saveRegions();
            plugin.notifyRegionsChanged();
            refreshBorderList();
            dialog.dispose();
        });

        JButton cancel = new JButton("Cancel");
        cancel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        cancel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        cancel.addActionListener(e -> dialog.dispose());

        actions.add(ok);
        actions.add(cancel);

        root.add(modePanel);
        root.add(colorPanel);
        root.add(propPanel);
        root.add(actions);

        dialog.setContentPane(root);
        // Toggle panels based on selection
        Runnable toggle = () -> {
            boolean lines = linesRadio.isSelected();
            colorPanel.setVisible(lines);
            propPanel.setVisible(!lines);
            dialog.repaint();
        };
        linesRadio.addActionListener(e -> toggle.run());
        propsRadio.addActionListener(e -> toggle.run());
        toggle.run();
        dialog.setVisible(true);
        styleDialogBorderHighlight = null;
        refreshBorderList();
    }


    private void clearBorders(JPanel grid)
    {
        for (java.awt.Component c : grid.getComponents())
        {
            if (c instanceof JButton)
            {
                ((JButton)c).setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
            }
        }
    }

    private BufferedImage loadPropThumbnail(PropStyle style)
    {
        String id = String.valueOf(getModelId(style));
        try
        {
            return ImageUtil.loadImageResource(RegionConfigComponent.class, "/" + id + ".png");
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private String getDisplayName(PropStyle style)
    {
        switch (style)
        {
            case SEA_ROCK:
                return "Sea Rock";
            case ROCK_WALL:
                return "Rock Wall";
            case IRON_FENCE:
                return "Iron Fence";
            case LOG_FENCE:
                return "Log Fence";
            default:
                return style.name();
        }
    }

    private int getModelId(PropStyle style)
    {
        switch (style)
        {
            case SEA_ROCK:
                return 58598;
            case ROCK_WALL:
                return 17319;
            case IRON_FENCE:
                return 6745;
            case LOG_FENCE:
                return 42889;
            default:
                return 58598;
        }
    }

    private JButton createPropsButton(Border border)
    {
        JButton propsButton = new JButton();
        propsButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        propsButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        propsButton.setFocusPainted(false);
        propsButton.setPreferredSize(new Dimension(24, 24));
        propsButton.setMaximumSize(new Dimension(24, 24));
        propsButton.setMinimumSize(new Dimension(24, 24));
        propsButton.setToolTipText("Border Style");
        if (propsIcon != null && propsIcon.getIconWidth() > 0)
        {
            java.awt.Image scaled = propsIcon.getImage().getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH);
            propsButton.setIcon(new javax.swing.ImageIcon(scaled));
        }
        propsButton.addActionListener(e -> {
            plugin.selectBorder(border);
            openThemeDialog(border);
        });
        return propsButton;
    }

    private String ellipsize(String text, int maxLength)
    {
        if (text == null)
        {
            return "";
        }
        if (maxLength < 4 || text.length() <= maxLength)
        {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private void createNewRegion()
    {
        String name = JOptionPane.showInputDialog(
            this,
            "Enter a name for the new region:",
            "Create New Region",
            JOptionPane.PLAIN_MESSAGE
        );

        if (name != null && !name.trim().isEmpty())
        {
            String trimmedName = name.trim();
            // Check if profile with this name already exists
            boolean exists = plugin.getRegions().stream()
                    .anyMatch(p -> p.getName().equals(trimmedName));
            
            if (!exists)
            {
                Region newProfile = plugin.createRegion(trimmedName);
                // Automatically enable editing mode for the new profile
                if (newProfile != null)
                {
                    plugin.setEditing(true);
                }
                refreshRegionList();
                refreshBorderList();
            }
            else
            {
                JOptionPane.showMessageDialog(
                    this,
                    "A region with that name already exists.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void importRegion()
    {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import Region");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files (*.json)", "json"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION)
        {
            java.io.File file = fileChooser.getSelectedFile();
            plugin.importRegionFromFile(file.getAbsolutePath());
            refreshRegionList();
            refreshBorderList();
            refreshTeleportsList();
        }
    }
    
    private void exportCurrentRegion()
    {
        Region currentProfile = plugin.getCurrentRegion();
        if (currentProfile == null)
        {
            JOptionPane.showMessageDialog(
                this,
                "No region selected. Please select a region to export.",
                "No Region Selected",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        exportRegion(currentProfile);
    }
    
    private void exportRegion(Region profile)
    {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Region");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files (*.json)", "json"));
        fileChooser.setSelectedFile(new java.io.File(profile.getName() + ".json"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION)
        {
            java.io.File file = fileChooser.getSelectedFile();
            String filePath = file.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".json"))
            {
                filePath += ".json";
            }
            
            if (plugin.exportRegion(profile, filePath))
            {
                JOptionPane.showMessageDialog(
                    this,
                    "Region exported successfully to:\n" + filePath,
                    "Export Successful",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
            else
            {
                JOptionPane.showMessageDialog(
                    this,
                    "Failed to export region.",
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }


    private void deleteRegion(String regionName)
    {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Delete the region '" + regionName + "'?\n" +
            "This will only remove it from RuneLite. The JSON file remains untouched.",
            "Delete Region",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION)
        {
            plugin.deleteRegion(regionName);
            refreshRegionList();
            refreshBorderList();
        }
    }
    
    private void finishBorderFromList(Border border)
    {
        if (border.getBoundaryTiles().isEmpty())
        {
            JOptionPane.showMessageDialog(
                this,
                "No tiles marked. Please mark some tiles to define the border.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        // Compute inner tiles
        boolean success = plugin.computeInnerTiles(border);
        
        if (!success)
        {
            // Shape cannot be processed - show error and keep in edit mode
            JOptionPane.showMessageDialog(
                this,
                "Border must be fully enclosed before it can be finished.\n\n" +
                "Close every gap so the outline surrounds a complete area,\n" +
                "then click Finish again.",
                "Border Is Not Fully Bounded",
                JOptionPane.WARNING_MESSAGE
            );
            // Keep editing mode enabled (inner tiles are already cleared by computeInnerTiles)
            plugin.saveRegions();
            plugin.notifyRegionsChanged();
            refreshBorderList();
            return;
        }
        
        // Success - turn off editing toggle when finishing
        plugin.setEditing(false);
        plugin.saveRegions();
        plugin.notifyRegionsChanged();
        refreshRegionList();
        refreshBorderList();
    }


    private void editBorder(Border border)
    {
        // Re-enable editing mode: clear inner tiles and enable editor
        plugin.enableEditingMode(border);
        refreshRegionList();
        refreshBorderList();
    }

    private ImageIcon loadIcon(String resourcePath, Supplier<ImageIcon> fallback)
    {
        try
        {
            BufferedImage image = ImageUtil.loadImageResource(RegionConfigComponent.class, resourcePath);
            if (image != null)
            {
                return new ImageIcon(image);
            }
        }
        catch (Exception ex)
        {
            log.warn("Failed to load icon resource {}", resourcePath, ex);
        }
        return fallback.get();
    }

    /**
     * Fallback edit/pencil icon used if sprite is missing.
     */
    private ImageIcon createFallbackEditIcon()
    {
        BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Set color to light gray (same as button foreground)
        g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        // Draw a clean pencil icon: simple angled pencil
        // Main pencil body (diagonal line)
        g.drawLine(4, 4, 12, 12);
        
        // Pencil tip (pointed triangle at bottom-right)
        java.awt.Polygon tip = new java.awt.Polygon();
        tip.addPoint(12, 12);
        tip.addPoint(10, 14);
        tip.addPoint(12, 14);
        g.fillPolygon(tip);
        
        // Pencil eraser (small filled square at top-left)
        g.fillRect(3, 3, 2, 2);
        
        g.dispose();
        return new ImageIcon(icon);
    }
    
    /**
     * Fallback delete icon (simple X) used if sprite missing.
     */
    private ImageIcon createFallbackDeleteIcon()
    {
        BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(4, 4, 12, 12);
        g.drawLine(12, 4, 4, 12);
        g.dispose();
        return new ImageIcon(icon);
    }

    /**
     * Fallback add icon (simple +) used if sprite missing.
     */
    private ImageIcon createFallbackAddIcon()
    {
        BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(8, 3, 8, 13);
        g.drawLine(3, 8, 13, 8);
        g.dispose();
        return new ImageIcon(icon);
    }

    /**
     * Fallback props icon (simple themed square) if sprite missing.
     */
    private ImageIcon createFallbackPropsIcon()
    {
        BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(ColorScheme.BRAND_ORANGE);
        g.fillRoundRect(3, 3, 10, 10, 4, 4);
        g.setColor(ColorScheme.DARKER_GRAY_COLOR);
        g.drawRoundRect(3, 3, 10, 10, 4, 4);
        g.dispose();
        return new ImageIcon(icon);
    }

    public void refreshBorderList()
    {
        SwingUtilities.invokeLater(() -> {
            borderListPanel.removeAll();

            Region currentProfile = plugin.getCurrentRegion();
            
            if (currentProfile == null)
            {
                JPanel emptyPanel = new JPanel(new BorderLayout());
                emptyPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
                JLabel emptyLabel = new JLabel("No region selected");
                emptyLabel.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
                emptyLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                emptyPanel.add(emptyLabel, BorderLayout.WEST);
                borderListPanel.add(emptyPanel);
            }
            else
            {
                Border activeBorder = plugin.getActiveBorder();
                for (Border border : currentProfile.getBorders())
                {
                    boolean highlight = (plugin.isEditing() && border == activeBorder) || border == styleDialogBorderHighlight;

                    JPanel borderItemPanel = new JPanel(new BorderLayout());
                    borderItemPanel.setBackground(highlight ? ColorScheme.DARKER_GRAY_HOVER_COLOR : ColorScheme.DARK_GRAY_COLOR);
                    borderItemPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
                        BorderFactory.createEmptyBorder(5, 10, 5, 10)
                    ));

                    boolean hasInnerTiles = !border.getInnerTiles().isEmpty();
                    int maxChars = hasInnerTiles ? 26 : 20;
                    JLabel nameLabel = new JLabel(ellipsize(border.getName(), maxChars));
                    nameLabel.setToolTipText(border.getName());
                    nameLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                    nameLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                    nameLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent e) {
                            plugin.selectBorder(border);
                            String newName = JOptionPane.showInputDialog(
                                RegionConfigComponent.this,
                                "Rename border:",
                                border.getName()
                            );
                            if (newName != null && !newName.trim().isEmpty())
                            {
                                plugin.renameBorder(border, newName.trim());
                            }
                        }
                        @Override
                        public void mouseEntered(java.awt.event.MouseEvent e) {
                            borderItemPanel.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
                        }
                        @Override
                        public void mouseExited(java.awt.event.MouseEvent e) {
                            borderItemPanel.setBackground(
                                ((plugin.isEditing() && border == plugin.getActiveBorder()) || border == styleDialogBorderHighlight)
                                    ? ColorScheme.DARKER_GRAY_HOVER_COLOR
                                    : ColorScheme.DARK_GRAY_COLOR
                            );
                        }
                    });
                    borderItemPanel.add(nameLabel, BorderLayout.WEST);

                    JPanel buttonPanel = new JPanel();
                    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
                    buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
                    buttonPanel.setOpaque(false);

                    boolean showStyleButton = !(plugin.isEditing() && border == plugin.getActiveBorder());
                    if (showStyleButton)
                    {
                        JButton propsButton = createPropsButton(border);
                        buttonPanel.add(propsButton);
                        buttonPanel.add(Box.createHorizontalStrut(5));
                    }

                    if (hasInnerTiles)
                    {
                        JButton editButton = new JButton();
                        editButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                        editButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                        editButton.setFocusPainted(false);
                        editButton.setPreferredSize(new Dimension(25, 25));
                        editButton.setMaximumSize(new Dimension(25, 25));
                        editButton.setMinimumSize(new Dimension(25, 25));
                        editButton.setToolTipText("Edit Border");
                        editButton.setIcon(editIcon);
                        editButton.addActionListener(e -> {
                            plugin.selectBorder(border);
                            editBorder(border);
                        });
                        buttonPanel.add(editButton);
                    }
                    else
                    {
                        JButton finishButton = new JButton("Finish");
                        finishButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                        finishButton.setForeground(ColorScheme.BRAND_ORANGE);
                        finishButton.setBorder(BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE, 1));
                        finishButton.setFocusPainted(false);
                        finishButton.setPreferredSize(new Dimension(80, 25));
                        finishButton.setMaximumSize(new Dimension(80, 25));
                        finishButton.setMinimumSize(new Dimension(80, 25));
                        finishButton.setToolTipText("Draw Border Around Tiles");
                        finishButton.addActionListener(e -> {
                            plugin.selectBorder(border);
                            finishBorderFromList(border);
                        });
                        buttonPanel.add(finishButton);
                    }

                    buttonPanel.add(Box.createHorizontalStrut(5));

                    JButton deleteButton = new JButton();
                    deleteButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                    deleteButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                    deleteButton.setFocusPainted(false);
                    deleteButton.setPreferredSize(new Dimension(24, 24));
                    deleteButton.setMaximumSize(new Dimension(24, 24));
                    deleteButton.setMinimumSize(new Dimension(24, 24));
                    deleteButton.setToolTipText("Delete Border");
                    if (deleteIcon != null && deleteIcon.getIconWidth() > 0)
                    {
                        java.awt.Image scaled = deleteIcon.getImage().getScaledInstance(18, 18, java.awt.Image.SCALE_SMOOTH);
                        deleteButton.setIcon(new javax.swing.ImageIcon(scaled));
                    }
                    else
                    {
                        deleteButton.setIcon(deleteIcon);
                    }
                    deleteButton.addActionListener(e -> {
                        int result = JOptionPane.showConfirmDialog(
                            RegionConfigComponent.this,
                            "Delete border '" + border.getName() + "'?",
                            "Delete Border",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                        );
                        if (result == JOptionPane.YES_OPTION)
                        {
                            plugin.deleteBorder(currentProfile, border);
                        }
                    });
                    buttonPanel.add(deleteButton);

                    borderItemPanel.add(buttonPanel, BorderLayout.EAST);
                    borderListPanel.add(borderItemPanel);
                }

                JPanel addButtonPanel = new JPanel(new BorderLayout());
                addButtonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
                addButtonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

                JButton addBorderButton = new JButton();
                addBorderButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                addBorderButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                addBorderButton.setFocusPainted(false);
                addBorderButton.setPreferredSize(new Dimension(30, 30));
                addBorderButton.setMaximumSize(new Dimension(30, 30));
                addBorderButton.setMinimumSize(new Dimension(30, 30));
                addBorderButton.setToolTipText("Add Border");
                addBorderButton.setIcon(addIcon);
                addBorderButton.addActionListener(e -> {
                    Border newBorder = plugin.createBorder(currentProfile, null);
                    plugin.setEditing(true);
                    plugin.selectBorder(newBorder);
                });

                addButtonPanel.add(addBorderButton, BorderLayout.CENTER);
                borderListPanel.add(addButtonPanel);
            }

            borderListPanel.revalidate();
            borderListPanel.repaint();
        });
    }
    
    public void refreshRegionList()
    {
        SwingUtilities.invokeLater(() -> {
            if (regionListPanel == null) return;
            
            regionListPanel.removeAll();
            
            java.util.List<Region> profiles = plugin.getRegions();
            Region currentProfile = plugin.getCurrentRegion();
            
            if (profiles.isEmpty())
            {
                JPanel emptyPanel = new JPanel(new BorderLayout());
                emptyPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
                JLabel emptyLabel = new JLabel("No regions created yet");
                emptyLabel.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
                emptyLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                emptyPanel.add(emptyLabel, BorderLayout.WEST);
                regionListPanel.add(emptyPanel);
            }
            else
            {
                for (Region profile : profiles)
                {
                    boolean isActive = profile == currentProfile;

                    // Panel for each region item
                    JPanel regionItemPanel = new JPanel(new BorderLayout());
                    regionItemPanel.setBackground(
                        isActive 
                            ? ColorScheme.DARKER_GRAY_HOVER_COLOR 
                            : ColorScheme.DARK_GRAY_COLOR
                    );
                    regionItemPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
                        BorderFactory.createEmptyBorder(5, 10, 5, 10)
                    ));

                    // Region name label (clickable to select)
                    String regionName = profile.getName();
                JLabel nameLabel = new JLabel(ellipsize(regionName, 32));
                    nameLabel.setToolTipText(regionName);
                    nameLabel.setForeground(isActive ? ColorScheme.BRAND_ORANGE : ColorScheme.LIGHT_GRAY_COLOR);
                    nameLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                    
                    nameLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent e) {
                            String newName = JOptionPane.showInputDialog(
                                RegionConfigComponent.this,
                                "Rename region:",
                                profile.getName()
                            );
                            if (newName != null && !newName.trim().isEmpty())
                            {
                                plugin.renameRegion(profile, newName.trim());
                            }
                        }
                        @Override
                        public void mouseEntered(java.awt.event.MouseEvent e) {
                            regionItemPanel.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
                        }
                        @Override
                        public void mouseExited(java.awt.event.MouseEvent e) {
                            regionItemPanel.setBackground(
                                profile == plugin.getCurrentRegion()
                                    ? ColorScheme.DARKER_GRAY_HOVER_COLOR
                                    : ColorScheme.DARK_GRAY_COLOR
                            );
                        }
                    });
                    regionItemPanel.add(nameLabel, BorderLayout.WEST);
                    
                    // Toggle + Delete buttons on the right
                    JPanel rightPanel = new JPanel();
                    rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
                    rightPanel.setBackground(regionItemPanel.getBackground());
                    rightPanel.setOpaque(false);

                    JToggleButton toggleButton = createSwitchToggle(isActive, () -> plugin.setActiveRegion(profile), plugin::clearActiveRegion);
                    rightPanel.add(toggleButton);
                    rightPanel.add(Box.createHorizontalStrut(8));

                    JButton deleteButton = new JButton();
                    deleteButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                    deleteButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                    deleteButton.setFocusPainted(false);
                    deleteButton.setPreferredSize(new Dimension(24, 24));
                    deleteButton.setMaximumSize(new Dimension(24, 24));
                    deleteButton.setMinimumSize(new Dimension(24, 24));
                    deleteButton.setToolTipText("Delete Region");
                    if (deleteIcon != null && deleteIcon.getIconWidth() > 0)
                    {
                        java.awt.Image scaled = deleteIcon.getImage().getScaledInstance(18, 18, java.awt.Image.SCALE_SMOOTH);
                        deleteButton.setIcon(new javax.swing.ImageIcon(scaled));
                    }
                    else
                    {
                        deleteButton.setIcon(deleteIcon);
                    }
                    deleteButton.addActionListener(e -> deleteRegion(profile.getName()));
                    rightPanel.add(deleteButton);

                    regionItemPanel.add(rightPanel, BorderLayout.EAST);
                    
                    regionListPanel.add(regionItemPanel);
                }
            }

            // Add-region button matching the border add button style
            JPanel addRegionPanel = new JPanel(new BorderLayout());
            addRegionPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
            addRegionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

            JButton addRegionButton = new JButton();
            addRegionButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            addRegionButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            addRegionButton.setFocusPainted(false);
            addRegionButton.setPreferredSize(new Dimension(30, 30));
            addRegionButton.setMaximumSize(new Dimension(30, 30));
            addRegionButton.setMinimumSize(new Dimension(30, 30));
            addRegionButton.setToolTipText("Create New Region");
            if (addIcon != null)
            {
                addRegionButton.setIcon(addIcon);
            }
            addRegionButton.addActionListener(e -> createNewRegion());

            addRegionPanel.add(addRegionButton, BorderLayout.CENTER);
            regionListPanel.add(addRegionPanel);

            regionListPanel.revalidate();
            regionListPanel.repaint();
        });
    }
    
    public void refreshTeleportsList()
    {
        SwingUtilities.invokeLater(() -> {
            if (teleportsPanel == null || teleportRegistry == null) return;
            
            teleportsPanel.removeAll();
            
            Region currentProfile = plugin.getCurrentRegion();
            if (currentProfile == null)
            {
                JPanel emptyPanel = new JPanel(new BorderLayout());
                emptyPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
                JLabel emptyLabel = new JLabel("No region selected");
                emptyLabel.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
                emptyLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                emptyPanel.add(emptyLabel, BorderLayout.WEST);
                teleportsPanel.add(emptyPanel);
            }
            else
            {
                // Ensure whitelist exists
                if (currentProfile.getTeleportWhitelist() == null)
                {
                    currentProfile.setTeleportWhitelist(new java.util.HashSet<>());
                }
                
                // Get categories grouped by type
                java.util.Map<String, List<String>> categoriesByGroup = teleportRegistry.getCategoriesByGroup();
                
                // Display groups in order: Spellbooks, Jewellery, Miscellaneous
                String[] groupOrder = {"Spellbooks", "Jewellery", "Miscellaneous"};
                
                for (String groupName : groupOrder)
                {
                    List<String> categories = categoriesByGroup.get(groupName);
                    if (categories == null || categories.isEmpty())
                    {
                        continue;
                    }
                    
                    // Group header panel - make it stand out more
                    JPanel groupHeaderPanel = new JPanel(new BorderLayout());
                    groupHeaderPanel.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
                    groupHeaderPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(2, 0, 2, 0, ColorScheme.MEDIUM_GRAY_COLOR),
                        BorderFactory.createEmptyBorder(6, 15, 6, 15)
                    ));
                    
                    JLabel groupLabel = new JLabel(groupName);
                    groupLabel.setForeground(ColorScheme.BRAND_ORANGE);
                    groupLabel.setFont(groupLabel.getFont().deriveFont(java.awt.Font.BOLD, 15f));
                    groupHeaderPanel.add(groupLabel, BorderLayout.WEST);
                    teleportsPanel.add(groupHeaderPanel);
                    
                    // Sort categories alphabetically within this group
                    java.util.Collections.sort(categories);
                    
                    // Categories in this group
                    for (String category : categories)
                {
                    // Container panel for header + content - no padding on outer panel so header spans full width
                    JPanel categoryPanel = new JPanel(new BorderLayout());
                    categoryPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
                    categoryPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR));
                    
                    // Category header (clickable) - no padding, darkened background fills full width
                    JPanel categoryHeader = new JPanel(new BorderLayout());
                    categoryHeader.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                    categoryHeader.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Padding for label text only
                    categoryHeader.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                    
                    JLabel categoryLabel = new JLabel(category);
                    categoryLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                    categoryLabel.setFont(categoryLabel.getFont().deriveFont(java.awt.Font.BOLD));
                    categoryHeader.add(categoryLabel, BorderLayout.WEST);
                    
                    // Teleports in this category (filtered)
                    List<TeleportDefinition> teleports = teleportRegistry.getTeleportsByCategory(category);
                    final List<TeleportDefinition> visibleTeleports = new java.util.ArrayList<>(teleports);
                    visibleTeleports.removeIf(t -> t.getId().startsWith("spell_lunar_tele_group"));
                    
                    JLabel categoryCountLabel = new JLabel(formatTeleportCount(currentProfile, visibleTeleports));
                    categoryCountLabel.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
                    categoryCountLabel.setFont(categoryCountLabel.getFont().deriveFont(java.awt.Font.BOLD));
                    categoryHeader.add(categoryCountLabel, BorderLayout.EAST);
                    // Ensure imported profiles reflect current counts/colors immediately
                    updateTeleportCountLabel(currentProfile, visibleTeleports, categoryCountLabel);
                    
                    // Panel to hold teleports in this category (collapsible body)
                    JPanel categoryContentPanel = new JPanel();
                    categoryContentPanel.setLayout(new BoxLayout(categoryContentPanel, BoxLayout.Y_AXIS));
                    categoryContentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
                    categoryContentPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10)); // Left/right padding for content alignment
                    categoryContentPanel.setVisible(false); // collapsed by default
                    
                    // Add header and content to categoryPanel - use NORTH/CENTER for vertical stacking
                    // Structure: header at top, content below (when visible)
                    categoryPanel.add(categoryHeader, BorderLayout.NORTH);
                    categoryPanel.add(categoryContentPanel, BorderLayout.CENTER);
                    
                    // Toggle handler
                    MouseAdapter headerClickListener = new MouseAdapter() {
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent e) {
                            if (e.getSource() != categoryHeader && e.getSource() != categoryLabel)
                            {
                                return;
                            }
                            
                            boolean newState = !categoryContentPanel.isVisible();
                            categoryContentPanel.setVisible(newState);
                            
                            categoryContentPanel.revalidate();
                            categoryPanel.revalidate();
                            teleportsPanel.revalidate();
                            
                            categoryContentPanel.repaint();
                            categoryPanel.repaint();
                            teleportsPanel.repaint();
                        }
                    };
                    categoryHeader.addMouseListener(headerClickListener);
                    categoryLabel.addMouseListener(headerClickListener);
                    
                    // Add category panel - BoxLayout with Y_AXIS will left-align by default when alignmentX is 0.0f
                    // Remove any explicit alignment settings and let it match regionItemPanel behavior
                    teleportsPanel.add(categoryPanel);
                    
                    // Teleports in this category
                    for (TeleportDefinition teleport : visibleTeleports)
                    {
                        final String teleportId = teleport.getId();
                        
                        JPanel teleportItemPanel = new JPanel(new BorderLayout());
                        teleportItemPanel.setAlignmentX(0.0f);
                        teleportItemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
                        teleportItemPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
                        // Left padding matches categoryPanel (10px), plus extra for checkbox (10px more)
                        teleportItemPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
                            BorderFactory.createEmptyBorder(3, 10, 3, 10) // Same left padding as categoryPanel
                        ));
                        JCheckBox teleportCheckBox = new JCheckBox(teleport.getName());
                        teleportCheckBox.setBackground(ColorScheme.DARK_GRAY_COLOR);
                        teleportCheckBox.setSelected(currentProfile.getTeleportWhitelist().contains(teleportId));
                        teleportCheckBox.setFocusPainted(false);
                        applyTeleportSelectionStyles(teleportCheckBox);
                        
                        teleportCheckBox.addActionListener(e -> {
                            boolean isSelected = teleportCheckBox.isSelected();
                            
                            if (isSelected)
                            {
                                currentProfile.getTeleportWhitelist().add(teleportId);
                            }
                            else
                            {
                                currentProfile.getTeleportWhitelist().remove(teleportId);
                            }
                            
                            // Auto-toggle corresponding Tele Group
                            String teleGroupId = teleportRegistry.getTeleGroupIdForSingleTeleport(teleportId);
                            if (teleGroupId != null)
                            {
                                // This is a single teleport, toggle its Tele Group
                                if (isSelected)
                                {
                                    currentProfile.getTeleportWhitelist().add(teleGroupId);
                                }
                                else
                                {
                                    currentProfile.getTeleportWhitelist().remove(teleGroupId);
                                }
                            }
                            
                            applyTeleportSelectionStyles(teleportCheckBox);
                            updateTeleportCountLabel(currentProfile, visibleTeleports, categoryCountLabel);
                            plugin.saveRegions();
                            // Redraw spellbook to apply changes immediately
                            plugin.redrawSpellbook();
                        });
                        
                        teleportItemPanel.add(teleportCheckBox, BorderLayout.WEST);
                        categoryContentPanel.add(teleportItemPanel);
                    }
                }
                } // End of group loop
            }
            
            teleportsPanel.revalidate();
            teleportsPanel.repaint();
        });
    }

    private void applyTeleportSelectionStyles(JCheckBox checkbox)
    {
        boolean selected = checkbox.isSelected();
        checkbox.setForeground(selected ? ColorScheme.LIGHT_GRAY_COLOR : ColorScheme.MEDIUM_GRAY_COLOR);
        checkbox.setFont(checkbox.getFont().deriveFont(java.awt.Font.PLAIN));
    }

    private String formatTeleportCount(Region profile, List<TeleportDefinition> teleports)
    {
        int selected = 0;
        if (profile != null && profile.getTeleportWhitelist() != null)
        {
            for (TeleportDefinition teleport : teleports)
            {
                if (profile.getTeleportWhitelist().contains(teleport.getId()))
                {
                    selected++;
                }
            }
        }
        return selected + "/" + teleports.size();
    }

    private void updateTeleportCountLabel(Region profile, List<TeleportDefinition> teleports, JLabel label)
    {
        if (label == null)
        {
            return;
        }

        label.setText(formatTeleportCount(profile, teleports));
        boolean anySelected = profile != null && profile.getTeleportWhitelist() != null
            && teleports.stream().anyMatch(t -> profile.getTeleportWhitelist().contains(t.getId()));
        label.setForeground(anySelected ? ColorScheme.LIGHT_GRAY_COLOR : ColorScheme.MEDIUM_GRAY_COLOR);
    }
}

