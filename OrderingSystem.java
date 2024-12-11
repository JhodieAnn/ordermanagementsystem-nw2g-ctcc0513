import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.table.DefaultTableModel;

public class OrderingSystem {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(RegisterFrame::new);
    }
}

// ===================== ORDER CLASS =====================
class Order implements Serializable {  // Ensure Order class is Serializable
    private static final long serialVersionUID = 1L;  // Optional but recommended
    private String lastName;
    private int orderNumber;
    private double totalCost;
    public Order(String lastName, int orderNumber, double totalCost) {
        this.lastName = lastName;
        this.orderNumber = orderNumber;
        this.totalCost = totalCost;
    }

    public String getLastName() {
        return lastName;
    }

    public int getOrderNumber() {
        return orderNumber;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }
}

// ===================== LOGIN FRAME =====================
// ===================== LOGIN FRAME =====================

class RegisterFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    private static final ReentrantLock lock = new ReentrantLock();

    public RegisterFrame() {
        setTitle("LOGIN");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Background Image
        setContentPane(new JLabel(new ImageIcon("C:/Users/JODIE/OneDrive/Desktop/ORDERING PROJECT/OrderingProject/src/registerimage.png")));
        setLayout(new BorderLayout(10, 10));

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        inputPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(usernameLabel, gbc);

        usernameField = new JTextField(30);
        gbc.gridx = 1;
        inputPanel.add(usernameField, gbc);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(passwordLabel, gbc);

        passwordField = new JPasswordField(30);
        gbc.gridx = 1;
        inputPanel.add(passwordField, gbc);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        JButton registerButton = new JButton("Login");
        JButton cancelButton = new JButton("Cancel");

        buttonPanel.add(registerButton);
        buttonPanel.add(cancelButton);

        registerButton.addActionListener(_ -> register());
        cancelButton.addActionListener(_ -> clearFields());

        add(inputPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void register() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        // Check if username starts with "ADMIN" and contains no spaces
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Both username and password must be filled out.", "Input Required", JOptionPane.WARNING_MESSAGE);
        } else if (!username.startsWith("ADMIN") || username.contains(" ")) {
            JOptionPane.showMessageDialog(this, "Invalid username.", "Invalid Username", JOptionPane.ERROR_MESSAGE);
        } else if (!password.equals("easybuy2024")) {
            JOptionPane.showMessageDialog(this, "Invalid password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
        } else {
            if (lock.tryLock()) {
                try {
                    JOptionPane.showMessageDialog(this, "Logging in", "Success", JOptionPane.INFORMATION_MESSAGE);
                    new OrderManagerFrame(lock);
                    dispose();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(this, "System is currently in use. Please try again later.", "Access Denied", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
    }
}

// ===================== ORDER MANAGER FRAME =====================
class OrderManagerFrame extends JFrame {
    private Queue<Order> orders = new LinkedList<>();
    private DefaultTableModel tableModel;
    private JTable orderTable;
    private JTextField nameField, orderNumberField, costField;

    private static final String FILE_NAME = "orders.ser";  // File to store orders

    public OrderManagerFrame(ReentrantLock lock) {
        setTitle("Order Management System");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Table setup
        String[] columns = {"Name", "Order Number", "Total Cost"};
        tableModel = new DefaultTableModel(columns, 0);
        orderTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(orderTable);
        add(scrollPane, BorderLayout.NORTH);

        // Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        inputPanel.add(new JLabel("Customer Name:"));
        nameField = new JTextField(12);
        inputPanel.add(nameField);

        inputPanel.add(new JLabel("Order Number:"));
        orderNumberField = new JTextField(12);
        inputPanel.add(orderNumberField);

        inputPanel.add(new JLabel("Total Cost:"));
        costField = new JTextField(12);
        inputPanel.add(costField);

        // Buttons Panel
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add Order");
        JButton searchButton = new JButton("Search Order");
        JButton editButton = new JButton("Edit Order");
        JButton removeButton = new JButton("Remove Order");
        JButton logoutButton = new JButton("Logout");

        buttonPanel.add(addButton);
        buttonPanel.add(searchButton);
        buttonPanel.add(editButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(logoutButton);

        addButton.addActionListener(_ -> addOrder());
        searchButton.addActionListener(_ -> searchOrder());
        editButton.addActionListener(_ -> editOrder());
        removeButton.addActionListener(_ -> removeOrder());
        logoutButton.addActionListener(_ -> logout(lock));

        add(inputPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Deserialize orders on startup
        loadOrders();

        setVisible(true);
    }

    @SuppressWarnings("unchecked")
    private void loadOrders() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
            Object obj = ois.readObject();
            if (obj instanceof Queue<?>) {
                orders = (Queue<Order>) obj;
                refreshTable();
            } else {
                JOptionPane.showMessageDialog(this, "Error: Invalid data in file.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + FILE_NAME);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading orders.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    

    private void saveOrders() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(orders);  // Serialize the orders list
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving orders: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addOrder() {
        try {
            String lastName = nameField.getText().trim();
            int orderNumber = Integer.parseInt(orderNumberField.getText().trim());
            double totalCost = Double.parseDouble(costField.getText().trim());

            orders.add(new Order(lastName, orderNumber, totalCost));
            tableModel.addRow(new Object[]{lastName, orderNumber, String.format("$%.2f", totalCost)});
            clearFields();

            // Save orders after adding a new one
            saveOrders();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid input.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Order order : orders) {
            tableModel.addRow(new Object[]{order.getLastName(), order.getOrderNumber(), String.format("$%.2f", order.getTotalCost())});
        }
    }

    private void searchOrder() {
        try {
            int orderNumberToSearch = Integer.parseInt(JOptionPane.showInputDialog(this, "Enter order number to search:"));
            boolean found = false;

            for (Order order : orders) {
                if (order.getOrderNumber() == orderNumberToSearch) {
                    JOptionPane.showMessageDialog(this, "Order found: " + order.getLastName() + " - " + order.getTotalCost());
                    found = true;
                    break;
                }
            }

            if (!found) {
                JOptionPane.showMessageDialog(this, "Order not found!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid order number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editOrder() {
        try {
            int orderNumberToEdit = Integer.parseInt(JOptionPane.showInputDialog(this, "Enter order number to edit:"));
            boolean found = false;

            for (Order order : orders) {
                if (order.getOrderNumber() == orderNumberToEdit) {
                    String newName = JOptionPane.showInputDialog(this, "Enter new name:");
                    double newCost = Double.parseDouble(JOptionPane.showInputDialog(this, "Enter new total cost:"));

                    order.setLastName(newName);
                    order.setTotalCost(newCost);
                    found = true;
                    refreshTable();
                    JOptionPane.showMessageDialog(this, "Order edited successfully!");

                    // Save orders after editing
                    saveOrders();
                    break;
                }
            }

            if (!found) {
                JOptionPane.showMessageDialog(this, "Order not found!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid data.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeOrder() {
        try {
            int orderNumberToRemove = Integer.parseInt(JOptionPane.showInputDialog(this, "Enter order number to remove:"));
            boolean found = orders.removeIf(order -> order.getOrderNumber() == orderNumberToRemove);

            if (found) {
                refreshTable();
                JOptionPane.showMessageDialog(this, "Order removed successfully!");

                // Save orders after removal
                saveOrders();
            } else {
                JOptionPane.showMessageDialog(this, "Order not found!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid order number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void logout(ReentrantLock lock) {
        int confirmation = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out?", "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (confirmation == JOptionPane.YES_OPTION) {
            saveOrders();  
            lock.unlock();
            System.exit(0);
        }
    }

    private void clearFields() {
        nameField.setText("");
        orderNumberField.setText("");
        costField.setText("");
    }
}