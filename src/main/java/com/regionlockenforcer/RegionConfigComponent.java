package com.regionlockenforcer;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
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
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;

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
    private final JButton createButton;
    private final JButton importButton;
    private final JButton exportRegionButton;
    private final ImageIcon editIcon;
    private final ImageIcon resetIcon;
    private final ImageIcon deleteIcon;

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
        this.resetIcon = loadIcon("/reset_icon.png", this::createFallbackRefreshIcon);
        this.deleteIcon = loadIcon("/delete_icon.png", this::createFallbackDeleteIcon);

        // Top buttons panel - Create New Region, Import Region, Export Region (stacked vertically)
        JPanel topButtonPanel = new JPanel();
        topButtonPanel.setLayout(new BoxLayout(topButtonPanel, BoxLayout.Y_AXIS));
        topButtonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        topButtonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 15, 0));
        
        // Create buttons with consistent styling
        Dimension buttonSize = new Dimension(180, 30);
        createButton = createStyledButton("Create New Region", buttonSize, e -> createNewRegion());
        importButton = createStyledButton("Import Region", buttonSize, e -> importRegion());
        exportRegionButton = createStyledButton("Export Region", buttonSize, e -> exportCurrentRegion());
        
        // Center buttons horizontally
        JPanel createButtonPanel = new JPanel();
        createButtonPanel.setLayout(new BoxLayout(createButtonPanel, BoxLayout.X_AXIS));
        createButtonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        createButtonPanel.add(Box.createHorizontalGlue());
        createButtonPanel.add(createButton);
        createButtonPanel.add(Box.createHorizontalGlue());
        topButtonPanel.add(createButtonPanel);
        
        topButtonPanel.add(Box.createVerticalStrut(5));
        
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
        JLabel borderSectionLabel = new JLabel("Draw Border");
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
        JLabel teleportsSectionLabel = new JLabel("Teleports Whitelist");
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
            "Are you sure you want to delete the region '" + regionName + "'?\n" +
            "This will delete the region, its border, and all associated settings.",
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
    
    private void resetBorder(Region profile)
    {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to reset the border? This will remove all marked tiles.",
            "Reset Border",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION)
        {
            // Clear all boundary tiles and inner tiles
            profile.getBoundaryTiles().clear();
            profile.getInnerTiles().clear();
            plugin.saveRegions();
            plugin.notifyRegionsChanged();
            // Automatically enable editing mode after reset
            plugin.setEditing(true);
            refreshRegionList();
            refreshBorderList();
        }
    }


    private void finishBorderFromList(Region profile)
    {
        if (profile.getBoundaryTiles().isEmpty())
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
        boolean success = plugin.computeInnerTiles(profile);
        
        if (!success)
        {
            // Shape cannot be processed - show error and keep in edit mode
            JOptionPane.showMessageDialog(
                this,
                "Unable to process this border shape.\n\n" +
                "The marked tiles do not form a valid closed region.\n" +
                "Please adjust your border to create a properly enclosed area.",
                "Invalid Border Shape",
                JOptionPane.ERROR_MESSAGE
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


    private void editBorder(Region profile)
    {
        // Re-enable editing mode: clear inner tiles and enable editor
        plugin.enableEditingMode(profile);
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
     * Fallback refresh icon used if sprite is missing.
     */
    private ImageIcon createFallbackRefreshIcon()
    {
        BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Set color to light gray (same as button foreground)
        g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        // Draw a circular arrow (refresh icon)
        // Center point
        int centerX = 8;
        int centerY = 8;
        int radius = 5;
        
        // Draw circular arc (about 270 degrees, starting from top-right)
        // Using drawArc: x, y, width, height, startAngle, arcAngle
        g.drawArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 45, 270);
        
        // Draw arrow head at the end of the arc (pointing clockwise)
        // The arc ends at approximately 315 degrees (45 + 270)
        // Convert to radians: 315 degrees = 5.5 radians
        double endAngle = Math.toRadians(315);
        double arrowX = centerX + radius * Math.cos(endAngle);
        double arrowY = centerY + radius * Math.sin(endAngle);
        
        // Draw arrow head (small triangle pointing in the direction of rotation)
        java.awt.Polygon arrowHead = new java.awt.Polygon();
        // Arrow point
        arrowHead.addPoint((int)(arrowX), (int)(arrowY));
        // Arrow base point 1 (perpendicular to the arc)
        double perpAngle = endAngle + Math.PI / 2;
        arrowHead.addPoint((int)(arrowX - 2 * Math.cos(perpAngle)), (int)(arrowY - 2 * Math.sin(perpAngle)));
        // Arrow base point 2 (opposite side)
        arrowHead.addPoint((int)(arrowX - 2 * Math.cos(endAngle)), (int)(arrowY - 2 * Math.sin(endAngle)));
        g.fillPolygon(arrowHead);
        
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
                // Show current region's border section
                JPanel borderItemPanel = new JPanel(new BorderLayout());
                borderItemPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
                borderItemPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
                ));

                // Border name label (show region name, not border name)
                JLabel nameLabel = new JLabel(currentProfile.getName());
                nameLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                borderItemPanel.add(nameLabel, BorderLayout.WEST);

                // Button panel for actions
                JPanel buttonPanel = new JPanel();
                buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
                buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
                buttonPanel.setOpaque(false);
                
                boolean hasInnerTiles = !currentProfile.getInnerTiles().isEmpty();
                
                if (hasInnerTiles)
                {
                    // Finished border: show edit and reset buttons
                    // Edit button with pencil icon
                    JButton editButton = new JButton();
                    editButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                    editButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                    editButton.setFocusPainted(false);
                    editButton.setPreferredSize(new Dimension(25, 25));
                    editButton.setMaximumSize(new Dimension(25, 25));
                    editButton.setMinimumSize(new Dimension(25, 25));
                    editButton.setToolTipText("Edit");
                    editButton.setIcon(editIcon);
                    editButton.addActionListener(e -> editBorder(currentProfile));
                    buttonPanel.add(editButton);
                    
                    buttonPanel.add(Box.createHorizontalStrut(5));
                    
                    // Reset button with refresh icon
                    JButton resetButton = new JButton();
                    resetButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                    resetButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                    resetButton.setFocusPainted(false);
                    resetButton.setPreferredSize(new Dimension(25, 25));
                    resetButton.setMaximumSize(new Dimension(25, 25));
                    resetButton.setMinimumSize(new Dimension(25, 25));
                    resetButton.setToolTipText("Reset Border");
                    resetButton.setIcon(resetIcon);
                    resetButton.addActionListener(e -> resetBorder(currentProfile));
                    buttonPanel.add(resetButton);
                }
                else
                {
                    // Unfinished border: show Finish button
                    JButton finishButton = new JButton("Finish");
                    finishButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                    finishButton.setForeground(ColorScheme.BRAND_ORANGE);
                    finishButton.setBorder(BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE, 1));
                    finishButton.setFocusPainted(false);
                    finishButton.setPreferredSize(new Dimension(80, 25));
                    finishButton.setMaximumSize(new Dimension(80, 25));
                    finishButton.setMinimumSize(new Dimension(80, 25));
                    finishButton.setToolTipText("Draw Border Around Tiles");
                    finishButton.addActionListener(e -> finishBorderFromList(currentProfile));
                    buttonPanel.add(finishButton);
                }
                
                borderItemPanel.add(buttonPanel, BorderLayout.EAST);
                borderListPanel.add(borderItemPanel);
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
                    // Panel for each region item
                    JPanel regionItemPanel = new JPanel(new BorderLayout());
                    regionItemPanel.setBackground(
                        profile == currentProfile 
                            ? ColorScheme.DARKER_GRAY_HOVER_COLOR 
                            : ColorScheme.DARK_GRAY_COLOR
                    );
                    regionItemPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
                        BorderFactory.createEmptyBorder(5, 10, 5, 10)
                    ));

                    // Region name label (clickable to select)
                    JLabel nameLabel = new JLabel(profile.getName());
                    nameLabel.setForeground(
                        profile == currentProfile 
                            ? ColorScheme.BRAND_ORANGE 
                            : ColorScheme.LIGHT_GRAY_COLOR
                    );
                    nameLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                    
                    nameLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent e) {
                            plugin.selectRegion(profile.getName());
                            refreshRegionList();
                            refreshBorderList();
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
                    
                    // Delete button
                    JButton deleteButton = new JButton();
                    deleteButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                    deleteButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                    deleteButton.setFocusPainted(false);
                    deleteButton.setPreferredSize(new Dimension(25, 25));
                    deleteButton.setMaximumSize(new Dimension(25, 25));
                    deleteButton.setMinimumSize(new Dimension(25, 25));
                    deleteButton.setToolTipText("Delete Region");
                    deleteButton.setIcon(deleteIcon);
                    deleteButton.addActionListener(e -> deleteRegion(profile.getName()));
                    regionItemPanel.add(deleteButton, BorderLayout.EAST);
                    
                    regionListPanel.add(regionItemPanel);
                }
            }

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
                    List<TeleportDefinition> teleports = teleportRegistry.getTeleportsByCategory(category);
                    for (TeleportDefinition teleport : teleports)
                    {
                        final String teleportId = teleport.getId();
                        
                        // Skip Tele Group spells - they should be hidden and controlled via single teleports
                        if (teleportId.startsWith("spell_lunar_tele_group"))
                        {
                            continue;
                        }
                        
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
                        teleportCheckBox.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                        teleportCheckBox.setSelected(currentProfile.getTeleportWhitelist().contains(teleportId));
                        teleportCheckBox.setFocusPainted(false);
                        
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
}

