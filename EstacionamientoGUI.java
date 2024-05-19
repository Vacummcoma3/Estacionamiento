import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.time.LocalDateTime;

public class EstacionamientoGUI extends JFrame {
    private JTextField placaField;
    private JTextField propietarioField;
    private JComboBox<String> tipoBox;
    private JRadioButton entradaButton;
    private JRadioButton salidaButton;
    private JButton registrarButton;
    private JButton visualizarButton;
    private JComboBox<String> vehiculosEnEstacionamientoBox;
    private ButtonGroup entradaSalidaGroup;

    public EstacionamientoGUI() {
        setTitle("Registro de Entrada y Salida de Vehículo");
        setSize(400, 350);
        setLocationRelativeTo(null); // Centra la ventana en la pantalla
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);

        JLabel placaLabel = new JLabel("Placa:");
        placaLabel.setBounds(20, 20, 100, 25);
        add(placaLabel);

        placaField = new JTextField();
        placaField.setBounds(120, 20, 200, 25);
        add(placaField);

        JLabel propietarioLabel = new JLabel("Propietario:");
        propietarioLabel.setBounds(20, 60, 100, 25);
        add(propietarioLabel);

        propietarioField = new JTextField();
        propietarioField.setBounds(120, 60, 200, 25);
        add(propietarioField);

        JLabel tipoLabel = new JLabel("Tipo:");
        tipoLabel.setBounds(20, 100, 100, 25);
        add(tipoLabel);

        tipoBox = new JComboBox<>(new String[]{"auto", "moto", "camioneta"});
        tipoBox.setBounds(120, 100, 200, 25);
        add(tipoBox);

        entradaButton = new JRadioButton("Entrada");
        entradaButton.setBounds(120, 140, 100, 25);
        add(entradaButton);

        salidaButton = new JRadioButton("Salida");
        salidaButton.setBounds(220, 140, 100, 25);
        add(salidaButton);

        entradaSalidaGroup = new ButtonGroup();
        entradaSalidaGroup.add(entradaButton);
        entradaSalidaGroup.add(salidaButton);

        vehiculosEnEstacionamientoBox = new JComboBox<>();
        vehiculosEnEstacionamientoBox.setBounds(120, 180, 200, 25);
        vehiculosEnEstacionamientoBox.setVisible(false);
        add(vehiculosEnEstacionamientoBox);

        registrarButton = new JButton("Registrar");
        registrarButton.setBounds(120, 220, 200, 25);
        add(registrarButton);

        visualizarButton = new JButton("Visualizar Estacionamiento");
        visualizarButton.setBounds(120, 260, 200, 25);
        add(visualizarButton);

        registrarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (entradaButton.isSelected()) {
                    registrarEntrada();
                } else if (salidaButton.isSelected()) {
                    registrarSalida();
                } else {
                    JOptionPane.showMessageDialog(null, "Seleccione si es una entrada o salida.");
                }
            }
        });

        visualizarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Estacionamiento estacionamiento = new Estacionamiento(EstacionamientoGUI.this);
                estacionamiento.setVisible(true);
                setVisible(false);
            }
        });

        entradaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                vehiculosEnEstacionamientoBox.setVisible(false);
                placaField.setEditable(true);
                propietarioField.setEditable(true);
                tipoBox.setEnabled(true);
                placaField.setText("");
                propietarioField.setText("");
                tipoBox.setSelectedIndex(0);
            }
        });

        salidaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                vehiculosEnEstacionamientoBox.setVisible(true);
                placaField.setEditable(false);
                propietarioField.setEditable(false);
                tipoBox.setEnabled(false);
                cargarVehiculosEnEstacionamiento();
            }
        });

        vehiculosEnEstacionamientoBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedPlaca = (String) vehiculosEnEstacionamientoBox.getSelectedItem();
                if (selectedPlaca != null) {
                    cargarDatosVehiculo(selectedPlaca);
                }
            }
        });
    }

    private void registrarEntrada() {
        String placa = placaField.getText();
        String propietario = propietarioField.getText();
        String tipo = (String) tipoBox.getSelectedItem();
    
        try (Connection connection = DatabaseConnection.getConnection()) {
            // Verificar si la placa ya está registrada sin haber registrado una salida
            String checkPlacaQuery = "SELECT r.id FROM registro r JOIN vehiculo v ON r.vehiculo_id = v.id WHERE v.placa = ? AND r.salida IS NULL";
            PreparedStatement checkPlacaStmt = connection.prepareStatement(checkPlacaQuery);
            checkPlacaStmt.setString(1, placa);
            ResultSet placaRs = checkPlacaStmt.executeQuery();
    
            if (placaRs.next()) {
                JOptionPane.showMessageDialog(this, "El vehículo con esta placa ya está registrado en el estacionamiento.");
                return;
            }
    
            // Verificar si hay espacio disponible
            String selectEspacioQuery = "SELECT id FROM espacio_estacionamiento WHERE ocupado = 0 LIMIT 1";
            PreparedStatement selectEspacioStmt = connection.prepareStatement(selectEspacioQuery);
            ResultSet rs = selectEspacioStmt.executeQuery();
    
            if (rs.next()) {
                int espacioId = rs.getInt("id");
    
                // Registrar vehículo (si no existe)
                String vehiculoQuery = "INSERT INTO vehiculo (placa, tipo, propietario) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE tipo = VALUES(tipo), propietario = VALUES(propietario)";
                PreparedStatement vehiculoStmt = connection.prepareStatement(vehiculoQuery, PreparedStatement.RETURN_GENERATED_KEYS);
                vehiculoStmt.setString(1, placa);
                vehiculoStmt.setString(2, tipo);
                vehiculoStmt.setString(3, propietario);
                vehiculoStmt.executeUpdate();
    
                // Obtener el ID del vehículo
                int vehiculoId;
                ResultSet generatedKeys = vehiculoStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    vehiculoId = generatedKeys.getInt(1);
                } else {
                    // Si el vehículo ya existía, necesitamos obtener su ID
                    String getVehiculoIdQuery = "SELECT id FROM vehiculo WHERE placa = ?";
                    PreparedStatement getVehiculoIdStmt = connection.prepareStatement(getVehiculoIdQuery);
                    getVehiculoIdStmt.setString(1, placa);
                    ResultSet vehiculoIdRs = getVehiculoIdStmt.executeQuery();
                    if (vehiculoIdRs.next()) {
                        vehiculoId = vehiculoIdRs.getInt("id");
                    } else {
                        throw new SQLException("No se pudo obtener el ID del vehículo.");
                    }
                }
    
                // Registrar entrada en la tabla 'registro'
                String registroQuery = "INSERT INTO registro (vehiculo_id, entrada) VALUES (?, ?)";
                PreparedStatement registroStmt = connection.prepareStatement(registroQuery);
                registroStmt.setInt(1, vehiculoId);
                registroStmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                registroStmt.executeUpdate();
    
                // Actualizar estado del espacio
                String updateEspacioQuery = "UPDATE espacio_estacionamiento SET placa = ?, ocupado = 1 WHERE id = ?";
                PreparedStatement updateEspacioStmt = connection.prepareStatement(updateEspacioQuery);
                updateEspacioStmt.setString(1, placa);
                updateEspacioStmt.setInt(2, espacioId);
                updateEspacioStmt.executeUpdate();
    
                JOptionPane.showMessageDialog(this, "Entrada de vehículo registrada exitosamente.");
            } else {
                JOptionPane.showMessageDialog(this, "No hay espacios disponibles.");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al registrar la entrada del vehículo: " + ex.getMessage());
        }
    }
    
    
    private void registrarSalida() {
        String placa = (String) vehiculosEnEstacionamientoBox.getSelectedItem();
    
        try (Connection connection = DatabaseConnection.getConnection()) {
            // Verificar si el vehículo tiene un registro de entrada sin salida
            String selectQuery = "SELECT r.id, r.entrada, v.tipo FROM registro r JOIN vehiculo v ON r.vehiculo_id = v.id WHERE v.placa = ? AND r.salida IS NULL";
            PreparedStatement selectStmt = connection.prepareStatement(selectQuery);
            selectStmt.setString(1, placa);
            ResultSet rs = selectStmt.executeQuery();
    
            if (rs.next()) {
                int registroId = rs.getInt("id");
                Timestamp entrada = rs.getTimestamp("entrada");
                String tipo = rs.getString("tipo");
    
                // Calcular tarifa
                long minutos = (System.currentTimeMillis() - entrada.getTime()) / (1000 * 60);
                double tarifa = calcularTarifa(tipo, minutos);
    
                // Actualizar el registro con la salida y tarifa
                String updateQuery = "UPDATE registro SET salida = CURRENT_TIMESTAMP, tarifa = ? WHERE id = ?";
                PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
                updateStmt.setDouble(1, tarifa);
                updateStmt.setInt(2, registroId);
                updateStmt.executeUpdate();
    
                // Liberar espacio de estacionamiento
                String updateEspacioQuery = "UPDATE espacio_estacionamiento SET placa = NULL, ocupado = 0 WHERE placa = ?";
                PreparedStatement updateEspacioStmt = connection.prepareStatement(updateEspacioQuery);
                updateEspacioStmt.setString(1, placa);
                updateEspacioStmt.executeUpdate();
    
                JOptionPane.showMessageDialog(this, "Salida de vehículo registrada. Tarifa: " + tarifa);
            } else {
                JOptionPane.showMessageDialog(this, "No se encontró un registro de entrada para la placa proporcionada.");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al registrar la salida del vehículo: " + ex.getMessage());
        }
    }
    

    private double calcularTarifa(String tipo, long minutos) {
        double tarifaPorMinuto = 0.0;
        switch (tipo) {
            case "auto":
                tarifaPorMinuto = 0.06;
                break;
            case "moto":
                tarifaPorMinuto = 0.03;
                break;
            case "camioneta":
                tarifaPorMinuto = 0.09;
                break;
        }
        return tarifaPorMinuto * minutos;
        // Redondear el resultado a 5 números decimales
        
        }

    private void cargarVehiculosEnEstacionamiento() {
        try (Connection connection = DatabaseConnection.getConnection()) {
            String query = "SELECT v.placa FROM vehiculo v JOIN registro r ON v.id = r.vehiculo_id WHERE r.salida IS NULL";
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
    
            vehiculosEnEstacionamientoBox.removeAllItems();
            while (rs.next()) {
                vehiculosEnEstacionamientoBox.addItem(rs.getString("placa"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar vehículos en el estacionamiento: " + ex.getMessage());
        }
    }
    

    private void cargarDatosVehiculo(String placa) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            String query = "SELECT v.propietario, v.tipo FROM vehiculo v WHERE v.placa = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, placa);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                propietarioField.setText(rs.getString("propietario"));
                tipoBox.setSelectedItem(rs.getString("tipo"));
                placaField.setText(placa);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar datos del vehículo: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EstacionamientoGUI gui = new EstacionamientoGUI();
            gui.setVisible(true);
        });
    }
}

class DatabaseConnection {
    public static Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/estacionamiento";
        String user = "root";
        String password = "password";
        return DriverManager.getConnection(url, user, password);
    }
}
