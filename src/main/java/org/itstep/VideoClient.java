package org.itstep;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class VideoClient {

    public static final int WIDTH = 1000;
    public static final int HEIGHT = 600;

    private static MulticastSocket ms = null;
    private static int currentSession = 0;

    public static void main(String[] args) throws IOException {

        ms = new MulticastSocket(Settings.PORT);
        InetAddress group = InetAddress.getByName(Settings.ADDRESS);
        ms.joinGroup(group);

//        receive(Settings.ADDRESS, Settings.PORT);

        // Создаем окно нужного размера
        JFrame frame = new JFrame();
        frame.setLayout(new FlowLayout());
        frame.setSize(WIDTH, HEIGHT);

        // Добавляем метку
        JLabel lbl = new JLabel();
        // Считываем изображение из файла
        // TODO: считать из сети используя перегруженный конструктор ImageIcon
//        ImageIcon icon = new ImageIcon("screenshot.jpg");
        while (true){
            ImageIcon icon = new ImageIcon(receiveImage());
            int h = icon.getIconHeight();
            int w = icon.getIconWidth();
            float scale = (float)WIDTH/w; // масштабируем
            icon = new ImageIcon(icon.getImage().getScaledInstance((int) (w*scale), (int) (h*scale), Image.SCALE_SMOOTH));
            lbl.setIcon(icon); // устанавливаем изображение для метки

            // Добавляем метку в окно
            frame.add(lbl);
            frame.setVisible(true);
        }

        // Зкарывать окно при выходе
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private static BufferedImage receiveImage() throws IOException {
        byte[] buffer = new byte[Settings.DATAGRAM_MAX_SIZE];
        int currentSession = 1;
        int slicesStored = 0;
        byte[] imageData = null;
        int[] slicesCol = null;
        boolean sessionAvailable = false;
        while (true) {
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
            ms.receive(dp);
            byte[] data = dp.getData();

            int SESSION_START = 128;
            int SESSION_END = 64;
            int HEADER_SIZE = 8;

            short session = (short)(data[1] & 0xff);
            short slices = (short)(data[2] & 0xff);
            int maxPacketSize = (int)((data[3] & 0xff) << 8 | (data[4] & 0xff)); // mask the sign bit
            short slice = (short)(data[5] & 0xff);
            int size = (int)((data[6] & 0xff) << 8 | (data[7] & 0xff)); // mask the sign bit

            if((data[0] & SESSION_START) == SESSION_START) {
                if(session != currentSession) {
                    currentSession = session;
                    slicesStored = 0;
                    imageData = new byte[slices * maxPacketSize];
                    slicesCol = new int[slices];
                    sessionAvailable = true;
                }
            }

            if(sessionAvailable && (session == currentSession)){
                if(slicesCol != null && slicesCol[slice] == 0) {
                    slicesCol[slice] = 1;
                    System.arraycopy(data, HEADER_SIZE, imageData, slice * maxPacketSize, size - HEADER_SIZE);
                    slicesStored++;
                }
            }

            if(slicesStored == slices) {
                ByteArrayInputStream bis= new ByteArrayInputStream(imageData);
                BufferedImage image = ImageIO.read(bis);
                return image;
            }
        }
    }

    private static void receive(String address, int port) throws IOException {
        byte[] buff = new byte[Settings.DATAGRAM_MAX_SIZE];
        MulticastSocket socket = new MulticastSocket(port);

        socket.joinGroup(InetAddress.getByName(address));
        DatagramPacket packet = new DatagramPacket(buff, buff.length);
        socket.receive(packet);
        System.out.println("Receive: " + new String(packet.getData()));

        socket.leaveGroup(InetAddress.getByName(address));
        socket.close();
    }
}
