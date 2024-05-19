import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Estacionamiento extends JFrame {
    private JPanel[] espacios;

    public Estacionamiento(EstacionamientoGUI parent) {
        setTitle("Estacionamiento");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridLayout(5, 6, 10, 10)); // 5 filas y 6 columnas
        setLocationRelativeTo(null); // Centra la ventana en la pantalla

        espacios = new JPanel[26];

        for (int i = 0; i < 26; i++) {
            espacios[i] = new JPanel();
            espacios[i].setBackground(Color.GREEN);
            espacios[i].setBorder(BorderFactory.createLineBorder(Color.BLACK));
            add(espacios[i]);
        }

        JButton actualizarButton = new JButton("Actualizar");
        actualizarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actualizarEstacionamiento();
            }
        });

        JButton regresarButton = new JButton("Regresar");
        regresarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parent.setVisible(true);
                dispose();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(actualizarButton);
        buttonPanel.add(regresarButton);
        add(buttonPanel);

        actualizarEstacionamiento(); // Actualiza al iniciar
    }

    private void actualizarEstacionamiento() {
        try (Connection connection = DatabaseConnection.getConnection()) {
            String query = "SELECT * FROM espacio_estacionamiento";
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
    
            int index = 0;
            while (rs.next()) {
                String matricula = rs.getString("placa");
                if (matricula == null || matricula.isEmpty()) {
                    espacios[index].setBackground(Color.GREEN);
                    espacios[index].removeAll();
                } else {
                    espacios[index].setBackground(Color.RED);
                    espacios[index].removeAll();
                    JLabel label = new JLabel(matricula);
                    label.setForeground(Color.WHITE);
                    espacios[index].add(label);
                }
                espacios[index].revalidate();
                espacios[index].repaint();
                index++;
            }
    
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al actualizar el estacionamiento: " + ex.getMessage());
        }
    }
    

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EstacionamientoGUI parentGUI = new EstacionamientoGUI();
            Estacionamiento estacionamiento = new Estacionamiento(parentGUI);
            estacionamiento.setVisible(true);
        });
    }
}
