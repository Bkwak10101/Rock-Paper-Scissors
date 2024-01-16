package jadelab2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

class GameGui extends JFrame {
    private GameAgent myAgent;

    private JTextField moveField;
//    private JTextField priceField;

    GameGui(GameAgent a) {
        super(a.getLocalName());

        myAgent = a;

        JPanel p = new JPanel();
        p.setLayout(new GridLayout(2, 2));
        p.add(new JLabel("Title:"));
        moveField = new JTextField(15);
        p.add(moveField);
//        p.add(new JLabel("Price:"));
//        priceField = new JTextField(15);
//        p.add(priceField);
        getContentPane().add(p, BorderLayout.CENTER);

        JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    String title = moveField.getText().trim();
//                    String price = priceField.getText().trim();
//                    myAgent.updateCatalogue(title, Integer.parseInt(price));
                    moveField.setText("");
//                    priceField.setText("");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(GameGui.this,
                            "Invalid values. " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        p = new JPanel();
        p.add(addButton);
        getContentPane().add(p, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                myAgent.doDelete();
            }
        });

        setResizable(false);
    }

    public void display() {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int) screenSize.getWidth() / 2;
        int centerY = (int) screenSize.getHeight() / 2;
        setBounds(
                centerX - getWidth() / 2,
                centerY - getHeight() / 2 - 200,
                400,
                150);
        setVisible(true);
    }
}
