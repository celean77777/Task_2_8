import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class MyClient extends JFrame {
    private final String SERVER_ADDR = "127.0.0.1";
    private final int SERVER_PORT = 8189;
    private JTextField msgInputField;
    private JTextArea chatArea;
    private JTextField loginField;
    private JTextField passField;
    private JButton btnSendAuth;
    private JComboBox nameList;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Boolean authorized;
    private String myNick;
    private Thread t;


    public MyClient() {
        prepareGUI();
        try {
            openConnection();
        } catch (IOException | InterruptedException e) {
            chatArea.append("Сервер не активен. Попробуйте подключиться позже");
            e.printStackTrace();
        }

    }

    private void setAuthorized(Boolean bol){
        authorized = bol;
    }
    public void openConnection() throws IOException, InterruptedException {
        socket = new Socket(SERVER_ADDR, SERVER_PORT);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        setAuthorized(false);
        t = new Thread(() -> {
            try {
                while (true) {
                    String strFromServer = in.readUTF();
                    if(strFromServer.startsWith("/authok")) {
                        myNick = strFromServer.split("\\s")[1];
                        setTitle(myNick);
                        setAuthorized(true);
                        btnSendAuth.setBackground(Color.GREEN);
                        btnSendAuth.setText("Выйти");
                        break;
                    }
                    chatArea.append(strFromServer + "\n");
                }
                while (true) {

                        String strFromServer = in.readUTF();
                        if (strFromServer.equalsIgnoreCase("/end")) {
                            closeConnection();
                            break;
                        }
                        if(strFromServer.startsWith("/clients")){
                            String[] clientsFromServer = strFromServer.split("\\s");
                            nameList.removeAllItems();
                            nameList.addItem("Send to all");
                            for (int i = 1; i<clientsFromServer.length; i++){
                                if(!clientsFromServer[i].equals(myNick)) {
                                    nameList.addItem(clientsFromServer[i]);
                                }
                            }

                        }
                        chatArea.append(strFromServer);
                        chatArea.append("\n");

                }
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                try {
                    setAuthorized(false);
                    socket.close();
                    myNick = "";
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });
        t.setDaemon(true);
        t.start();
    }

    public void onAuthClick() throws IOException, InterruptedException {
        if (socket.isClosed()) {
            chatArea.append("Сервер закрыл Ваше соединение\n");
        }else {

            try {
                if (!authorized) {
                    out.writeUTF("/auth" + " " + loginField.getText() + " " + passField.getText());
                    loginField.setText("");
                    passField.setText("");
                }else {
                    out.writeUTF("/end");
                    loginField.setText("");
                    passField.setText("");
                    btnSendAuth.setBackground(Color.GRAY);
                    btnSendAuth.setEnabled(false);
                    closeConnection();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void closeConnection() {

        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void sendMessage() {
        if (!msgInputField.getText().trim().isEmpty()&&!socket.isClosed()) {
            try {
                if(nameList.getSelectedItem().equals("Send to all")) {
                    out.writeUTF(msgInputField.getText());
                    msgInputField.setText("");
                    msgInputField.grabFocus();
                } else {
                    out.writeUTF("/w " + nameList.getSelectedItem() + " " + msgInputField.getText());
                    msgInputField.setText("");
                    msgInputField.grabFocus();
                }
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Ошибка отправки сообщения");
            }
        }
    }
    public void prepareGUI() {
// Параметры окна
        setBounds(600, 300, 500, 500);
        setTitle("Клиент");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
// Текстовое поле для вывода сообщений
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);
// Нижняя панель с полем для ввода сообщений и кнопкой отправки сообщений

        JPanel bottomPanel = new JPanel();
        JPanel upPanel = new JPanel();
        upPanel.setLayout(new BoxLayout(upPanel, BoxLayout.X_AXIS));
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));

        JButton btnSendMsg = new JButton("Отправить");
        btnSendAuth = new JButton("Авторизоваться");
        btnSendAuth.setBackground(Color.GRAY);



        msgInputField = new JTextField();
        loginField = new JTextField();
        passField = new JTextField();

        nameList = new JComboBox();



        loginField.setText("Login");
        passField.setText("Password");

        upPanel.add(loginField);
        upPanel.add(passField);
        upPanel.add(btnSendAuth);

        bottomPanel.add(msgInputField);
        bottomPanel.add(btnSendMsg);
        bottomPanel.add(nameList);

        add(bottomPanel, BorderLayout.SOUTH);
        add(upPanel, BorderLayout.NORTH);

        btnSendMsg.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        btnSendAuth.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    onAuthClick();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });
        msgInputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        loginField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
               loginField.setText("");
            }
        });

        passField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                passField.setText("");
            }
        });
// Настраиваем действие на закрытие окна
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                        out.writeUTF("/end");
                        closeConnection();

                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        });
        setVisible(true);
    }

}