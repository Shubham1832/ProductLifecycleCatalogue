
package edu.niet.plcm;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;

public class Main extends JFrame {

    private final JTabbedPane tabs = new JTabbedPane();

    public Main() {
        super("Product Lifecycle and Catalogue Management System");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 650);
        setLocationRelativeTo(null);

        tabs.addTab("Products", new ProductsPanel());
        tabs.addTab("Categories",
                new SimpleLookupPanel("categories", "category_id",
                        "name", "description", "Categories"));
        tabs.addTab("Sales Channels",
                new SimpleLookupPanel("sales_channels", "channel_id",
                        "name", "description", "Sales Channels"));
        tabs.addTab("Lifecycle", new LifecyclePanel());
        tabs.addTab("Inventory", new InventoryPanel());

        add(tabs);
    }

    private static void showError(Component parent, Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(
                parent,
                ex.getMessage(),
                "Operation failed",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(
                parent, message, "Information",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static JTextField field(int columns) {
        return new JTextField(columns);
    }

    // ==================== PRODUCTS ====================

    private class ProductsPanel extends JPanel {

        private final DefaultTableModel tm = model(
                "ID", "SKU", "Name", "Description",
                "Price", "Category ID", "Status"
        );

        private final JTable table = new JTable(tm);

        private final JTextField sku = field(8);
        private final JTextField name = field(12);
        private final JTextField description = field(12);
        private final JTextField price = field(7);
        private final JTextField categoryId = field(4);

        ProductsPanel() {
            setLayout(new BorderLayout(8, 8));

            JPanel form = new JPanel(new GridLayout(2, 5, 6, 5));
            form.setBorder(BorderFactory.createTitledBorder(
                    "Product details (category ID is optional)"
            ));

            form.add(new JLabel("SKU *"));
            form.add(new JLabel("Name *"));
            form.add(new JLabel("Description"));
            form.add(new JLabel("Price *"));
            form.add(new JLabel("Category ID"));

            form.add(sku);
            form.add(name);
            form.add(description);
            form.add(price);
            form.add(categoryId);

            JButton add = new JButton("Add");
            JButton update = new JButton("Update selected");
            JButton delete = new JButton("Delete selected");
            JButton refresh = new JButton("Refresh");

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
            buttons.add(add);
            buttons.add(update);
            buttons.add(delete);
            buttons.add(refresh);

            JPanel south = new JPanel(new BorderLayout());
            south.add(form, BorderLayout.CENTER);
            south.add(buttons, BorderLayout.SOUTH);

            add(new JScrollPane(table), BorderLayout.CENTER);
            add(south, BorderLayout.SOUTH);

            add.addActionListener(e -> insertProduct());
            update.addActionListener(e -> updateProduct());
            delete.addActionListener(e -> deleteProduct());
            refresh.addActionListener(e -> load());

            table.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()
                        && table.getSelectedRow() >= 0) {

                    int r = table.convertRowIndexToModel(
                            table.getSelectedRow());

                    sku.setText(String.valueOf(tm.getValueAt(r, 1)));
                    name.setText(String.valueOf(tm.getValueAt(r, 2)));
                    description.setText(String.valueOf(tm.getValueAt(r, 3)));
                    price.setText(String.valueOf(tm.getValueAt(r, 4)));

                    Object cat = tm.getValueAt(r, 5);
                    categoryId.setText(cat == null ? "" : String.valueOf(cat));
                }
            });

            load();
        }

        private void validateProduct() {
            if (sku.getText().trim().isEmpty()
                    || name.getText().trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "SKU and Name are required.");
            }

            try {
                BigDecimal p = new BigDecimal(price.getText().trim());

                if (p.signum() < 0) {
                    throw new IllegalArgumentException(
                            "Price cannot be negative.");
                }
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(
                        "Price must be a valid number, e.g. 199.99.");
            }

            if (!categoryId.getText().trim().isEmpty()) {
                try {
                    if (Integer.parseInt(categoryId.getText().trim()) <= 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(
                            "Category ID must be a positive integer.");
                }
            }
        }

        private void insertProduct() {
            try {
                validateProduct();

                String sql = "INSERT INTO products " +
                        "(sku,name,description,price,category_id) " +
                        "VALUES(?,?,?,?,?)";

                try (Connection c = Database.getConnection();
                     PreparedStatement ps = c.prepareStatement(sql)) {

                    ps.setString(1, sku.getText().trim());
                    ps.setString(2, name.getText().trim());
                    ps.setString(3, description.getText().trim());
                    ps.setBigDecimal(4,
                            new BigDecimal(price.getText().trim()));

                    setOptionalCategory(ps, 5);
                    ps.executeUpdate();
                }

                clearProductFields();
                load();
                info(this, "Product added.");

            } catch (Exception ex) {
                showError(this, ex);
            }
        }

        private void updateProduct() {
            int id = selectedId();

            if (id < 0) {
                info(this, "Select a product first.");
                return;
            }

            try {
                validateProduct();

                String sql = "UPDATE products SET " +
                        "sku=?, name=?, description=?, price=?, " +
                        "category_id=?, updated_at=CURRENT_TIMESTAMP " +
                        "WHERE product_id=?";

                try (Connection c = Database.getConnection();
                     PreparedStatement ps = c.prepareStatement(sql)) {

                    ps.setString(1, sku.getText().trim());
                    ps.setString(2, name.getText().trim());
                    ps.setString(3, description.getText().trim());
                    ps.setBigDecimal(4,
                            new BigDecimal(price.getText().trim()));

                    setOptionalCategory(ps, 5);
                    ps.setInt(6, id);
                    ps.executeUpdate();
                }

                load();
                info(this, "Product updated.");

            } catch (Exception ex) {
                showError(this, ex);
            }
        }

        private void setOptionalCategory(
                PreparedStatement ps, int index) throws SQLException {

            if (categoryId.getText().trim().isEmpty()) {
                ps.setNull(index, Types.INTEGER);
            } else {
                ps.setInt(index,
                        Integer.parseInt(categoryId.getText().trim()));
            }
        }

        private int selectedId() {
            int row = table.getSelectedRow();

            if (row < 0) {
                return -1;
            }

            return Integer.parseInt(String.valueOf(
                    tm.getValueAt(
                            table.convertRowIndexToModel(row), 0)));
        }

        private void deleteProduct() {
            int id = selectedId();

            if (id < 0) {
                info(this, "Select a product first.");
                return;
            }

            int answer = JOptionPane.showConfirmDialog(
                    this,
                    "Delete product " + id
                            + " and its related records?",
                    "Confirm",
                    JOptionPane.YES_NO_OPTION
            );

            if (answer != JOptionPane.YES_OPTION) {
                return;
            }

            try (Connection c = Database.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "DELETE FROM products WHERE product_id=?")) {

                ps.setInt(1, id);
                ps.executeUpdate();

                load();
                clearProductFields();

            } catch (Exception ex) {
                showError(this, ex);
            }
        }

        private void clearProductFields() {
            sku.setText("");
            name.setText("");
            description.setText("");
            price.setText("");
            categoryId.setText("");
        }

        private void load() {
            tm.setRowCount(0);

            String sql = "SELECT product_id,sku,name," +
                    "COALESCE(description,''),price,category_id," +
                    "lifecycle_status FROM products ORDER BY product_id";

            try (Connection c = Database.getConnection();
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {

                while (rs.next()) {
                    tm.addRow(new Object[]{
                            rs.getInt(1),
                            rs.getString(2),
                            rs.getString(3),
                            rs.getString(4),
                            rs.getBigDecimal(5),
                            rs.getObject(6),
                            rs.getString(7)
                    });
                }

            } catch (Exception ex) {
                showError(this, ex);
            }
        }
    }

    // ==================== CATEGORIES AND SALES CHANNELS ====================

    private class SimpleLookupPanel extends JPanel {

        private final String tableName;
        private final String idColumn;
        private final String nameColumn;
        private final String descriptionColumn;

        private final DefaultTableModel tm =
                model("ID", "Name", "Description");

        private final JTable table = new JTable(tm);
        private final JTextField name = field(18);
        private final JTextField description = field(24);

        SimpleLookupPanel(
                String tableName,
                String idColumn,
                String nameColumn,
                String descriptionColumn,
                String title) {

            this.tableName = tableName;
            this.idColumn = idColumn;
            this.nameColumn = nameColumn;
            this.descriptionColumn = descriptionColumn;

            setLayout(new BorderLayout(8, 8));

            JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
            form.setBorder(BorderFactory.createTitledBorder(title));

            form.add(new JLabel("Name *"));
            form.add(name);
            form.add(new JLabel("Description"));
            form.add(description);

            JButton add = new JButton("Add");
            JButton update = new JButton("Update selected");
            JButton delete = new JButton("Delete selected");
            JButton refresh = new JButton("Refresh");

            form.add(add);
            form.add(update);
            form.add(delete);
            form.add(refresh);

            add(new JScrollPane(table), BorderLayout.CENTER);
            add(form, BorderLayout.SOUTH);

            add.addActionListener(e -> save(false));
            update.addActionListener(e -> save(true));
            delete.addActionListener(e -> delete());
            refresh.addActionListener(e -> load());

            table.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()
                        && table.getSelectedRow() >= 0) {

                    int r = table.convertRowIndexToModel(
                            table.getSelectedRow());

                    name.setText(String.valueOf(tm.getValueAt(r, 1)));
                    description.setText(String.valueOf(tm.getValueAt(r, 2)));
                }
            });

            load();
        }

        private int selectedId() {
            int r = table.getSelectedRow();

            if (r < 0) {
                return -1;
            }

            return Integer.parseInt(String.valueOf(
                    tm.getValueAt(
                            table.convertRowIndexToModel(r), 0)));
        }

        private void save(boolean update) {
            if (name.getText().trim().isEmpty()) {
                info(this, "Name is required.");
                return;
            }

            int id = selectedId();

            if (update && id < 0) {
                info(this, "Select a row first.");
                return;
            }

            String sql = update
                    ? "UPDATE " + tableName + " SET " +
                      nameColumn + "=?, " + descriptionColumn +
                      "=? WHERE " + idColumn + "=?"
                    : "INSERT INTO " + tableName + "(" +
                      nameColumn + "," + descriptionColumn +
                      ") VALUES(?,?)";

            try (Connection c = Database.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setString(1, name.getText().trim());
                ps.setString(2, description.getText().trim());

                if (update) {
                    ps.setInt(3, id);
                }

                ps.executeUpdate();

                name.setText("");
                description.setText("");
                load();

            } catch (Exception ex) {
                showError(this, ex);
            }
        }

        private void delete() {
            int id = selectedId();

            if (id < 0) {
                info(this, "Select a row first.");
                return;
            }

            try (Connection c = Database.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "DELETE FROM " + tableName +
                         " WHERE " + idColumn + "=?")) {

                ps.setInt(1, id);
                ps.executeUpdate();
                load();

            } catch (Exception ex) {
                showError(this, ex);
            }
        }

        private void load() {
            tm.setRowCount(0);

            String sql = "SELECT " + idColumn + "," +
                    nameColumn + ",COALESCE(" +
                    descriptionColumn + ",'') FROM " +
                    tableName + " ORDER BY " + idColumn;

            try (Connection c = Database.getConnection();
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {

                while (rs.next()) {
                    tm.addRow(new Object[]{
                            rs.getInt(1),
                            rs.getString(2),
                            rs.getString(3)
                    });
                }

            } catch (Exception ex) {
                showError(this, ex);
            }
        }
    }

    // ==================== PRODUCT LIFECYCLE ====================

    private class LifecyclePanel extends JPanel {

        private final DefaultTableModel tm =
                model("Product ID", "SKU", "Name", "Current Status");

        private final JTable table = new JTable(tm);

        private final JComboBox<String> status = new JComboBox<>(
                new String[]{
                        "DRAFT", "REVIEW", "PUBLISHED",
                        "REJECTED", "DISCONTINUED"
                });

        private final JTextField note = field(24);

        LifecyclePanel() {
            setLayout(new BorderLayout(8, 8));

            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
            controls.setBorder(BorderFactory.createTitledBorder(
                    "Change selected product status"));

            controls.add(new JLabel("New status"));
            controls.add(status);
            controls.add(new JLabel("Note"));
            controls.add(note);

            JButton apply = new JButton("Apply status");
            JButton refresh = new JButton("Refresh");

            controls.add(apply);
            controls.add(refresh);

            add(new JScrollPane(table), BorderLayout.CENTER);
            add(controls, BorderLayout.SOUTH);

            apply.addActionListener(e -> changeStatus());
            refresh.addActionListener(e -> load());

            load();
        }

        private void load() {
            tm.setRowCount(0);

            try (Connection c = Database.getConnection();
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT product_id,sku,name,lifecycle_status " +
                         "FROM products ORDER BY product_id")) {

                while (rs.next()) {
                    tm.addRow(new Object[]{
                            rs.getInt(1),
                            rs.getString(2),
                            rs.getString(3),
                            rs.getString(4)
                    });
                }

            } catch (Exception ex) {
                showError(this, ex);
            }
        }

        private void changeStatus() {
            int row = table.getSelectedRow();

            if (row < 0) {
                info(this, "Select a product first.");
                return;
            }

            int id = Integer.parseInt(String.valueOf(
                    tm.getValueAt(
                            table.convertRowIndexToModel(row), 0)));

            String next = String.valueOf(status.getSelectedItem());

            try (Connection c = Database.getConnection()) {
                c.setAutoCommit(false);

                try {
                    String old;

                    try (PreparedStatement ps = c.prepareStatement(
                            "SELECT lifecycle_status FROM products " +
                            "WHERE product_id=? FOR UPDATE")) {

                        ps.setInt(1, id);

                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                throw new SQLException(
                                        "Product no longer exists.");
                            }

                            old = rs.getString(1);
                        }
                    }

                    if (!allowed(old, next)) {
                        throw new SQLException(
                                "Transition not allowed: " +
                                old + " -> " + next);
                    }

                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE products SET lifecycle_status=?," +
                            "updated_at=CURRENT_TIMESTAMP " +
                            "WHERE product_id=?")) {

                        ps.setString(1, next);
                        ps.setInt(2, id);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO lifecycle_history " +
                            "(product_id,old_status,new_status,note) " +
                            "VALUES(?,?,?,?)")) {

                        ps.setInt(1, id);
                        ps.setString(2, old);
                        ps.setString(3, next);
                        ps.setString(4, note.getText().trim());
                        ps.executeUpdate();
                    }

                    c.commit();
                    note.setText("");
                    load();
                    info(this, "Lifecycle status updated.");

                } catch (Exception ex) {
                    c.rollback();
                    throw ex;

                } finally {
                    c.setAutoCommit(true);
                }

            } catch (Exception ex) {
                showError(this, ex);
            }
        }

        private boolean allowed(String old, String next) {
            if (old.equals(next)) {
                return false;
            }

            return switch (old) {
                case "DRAFT" ->
                        next.equals("REVIEW")
                                || next.equals("DISCONTINUED");

                case "REVIEW" ->
                        next.equals("PUBLISHED")
                                || next.equals("REJECTED")
                                || next.equals("DRAFT");

                case "REJECTED" ->
                        next.equals("DRAFT");

                case "PUBLISHED" ->
                        next.equals("DISCONTINUED")
                                || next.equals("DRAFT");

                case "DISCONTINUED" ->
                        next.equals("DRAFT");

                default -> false;
            };
        }
    }

    // ==================== INVENTORY ====================

    private class InventoryPanel extends JPanel {

        private final DefaultTableModel tm = model(
                "Product ID", "SKU", "Product",
                "Quantity", "Reorder Level"
        );

        private final JTable table = new JTable(tm);

        private final JComboBox<ProductChoice> productBox =
                new JComboBox<>();

        private final JTextField quantity = field(6);
        private final JTextField reorder = field(6);

        private class ProductChoice {
            final int id;
            final String sku;
            final String name;

            ProductChoice(int id, String sku, String name) {
                this.id = id;
                this.sku = sku;
                this.name = name;
            }

            @Override
            public String toString() {
                return sku + " - " + name;
            }
        }

        InventoryPanel() {
            setLayout(new BorderLayout(8, 8));

            JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
            form.setBorder(BorderFactory.createTitledBorder(
                    "Inventory Management"));

            form.add(new JLabel("Select Product"));

            productBox.setPreferredSize(new Dimension(220, 28));
            form.add(productBox);

            form.add(new JLabel("Quantity"));
            form.add(quantity);

            form.add(new JLabel("Reorder level"));
            form.add(reorder);

            JButton save = new JButton("Save / Update");
            JButton refresh = new JButton("Refresh");

            form.add(save);
            form.add(refresh);

            add(new JScrollPane(table), BorderLayout.CENTER);
            add(form, BorderLayout.SOUTH);

            save.addActionListener(e -> save());
            refresh.addActionListener(e -> refreshAll());

            table.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()
                        && table.getSelectedRow() >= 0) {

                    int r = table.convertRowIndexToModel(
                            table.getSelectedRow());

                    int id = Integer.parseInt(
                            String.valueOf(tm.getValueAt(r, 0)));

                    for (int i = 0; i < productBox.getItemCount(); i++) {
                        ProductChoice p = productBox.getItemAt(i);

                        if (p.id == id) {
                            productBox.setSelectedIndex(i);
                            break;
                        }
                    }

                    quantity.setText(
                            String.valueOf(tm.getValueAt(r, 3)));

                    reorder.setText(
                            String.valueOf(tm.getValueAt(r, 4)));
                }
            });

            refreshAll();
        }

        private void refreshAll() {
            loadProducts();
            load();
        }

        private void loadProducts() {
            productBox.removeAllItems();

            String sql =
                    "SELECT product_id,sku,name FROM products " +
                    "ORDER BY product_id";

            try (Connection c = Database.getConnection();
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {

                while (rs.next()) {
                    productBox.addItem(new ProductChoice(
                            rs.getInt("product_id"),
                            rs.getString("sku"),
                            rs.getString("name")
                    ));
                }

            } catch (Exception ex) {
                showError(this, ex);
            }
        }

        private void save() {
            ProductChoice selected =
                    (ProductChoice) productBox.getSelectedItem();

            if (selected == null) {
                info(this,
                        "No products found. Add a product first, " +
                        "then click Refresh.");
                return;
            }

            try {
                int qty = Integer.parseInt(quantity.getText().trim());
                int level = Integer.parseInt(reorder.getText().trim());

                if (qty < 0 || level < 0) {
                    info(this,
                            "Quantity and reorder level cannot be negative.");
                    return;
                }

                String sql =
                        "INSERT INTO inventory " +
                        "(product_id,quantity,reorder_level) " +
                        "VALUES(?,?,?) " +
                        "ON CONFLICT(product_id) DO UPDATE SET " +
                        "quantity=EXCLUDED.quantity, " +
                        "reorder_level=EXCLUDED.reorder_level, " +
                        "updated_at=CURRENT_TIMESTAMP";

                try (Connection c = Database.getConnection();
                     PreparedStatement ps = c.prepareStatement(sql)) {

                    ps.setInt(1, selected.id);
                    ps.setInt(2, qty);
                    ps.setInt(3, level);
                    ps.executeUpdate();
                }

                load();
                info(this, "Inventory saved successfully.");

            } catch (NumberFormatException ex) {
                info(this,
                        "Quantity and reorder level must be whole numbers.");

            } catch (Exception ex) {
                showError(this, ex);
            }
        }

        private void load() {
            tm.setRowCount(0);

            String sql =
                    "SELECT i.product_id,p.sku,p.name," +
                    "i.quantity,i.reorder_level " +
                    "FROM inventory i " +
                    "JOIN products p ON p.product_id=i.product_id " +
                    "ORDER BY i.product_id";

            try (Connection c = Database.getConnection();
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {

                while (rs.next()) {
                    tm.addRow(new Object[]{
                            rs.getInt("product_id"),
                            rs.getString("sku"),
                            rs.getString("name"),
                            rs.getInt("quantity"),
                            rs.getInt("reorder_level")
                    });
                }

            } catch (Exception ex) {
                showError(this, ex);
            }
        }
    }

    // ==================== MAIN METHOD ====================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new Main().setVisible(true));
    }
}
