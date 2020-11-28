package org.itstep;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.*;

public class VideoStreamer {

    private static DatagramSocket socket;
    private static int sessionNumber = 0;

    public static void main(String[] args) throws IOException {

//        transmit("1234567890".getBytes(), Settings.ADDRESS, Settings.PORT);

        while (true){
            try {
                Robot robot = new Robot();
                String format = "jpg";
//                String fileName = "screenshot." + format;

                // получить скриншот
                Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                BufferedImage screenFullImage = robot.createScreenCapture(screenRect);

                // сохраняем в файл (для примера)
//            ImageIO.write(screenFullImage, format, new File(fileName));
//            System.out.println("Сохранен!");

                // получаем мессив байт для отравки по сети
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(screenFullImage, format, out);

                byte[] bytes = out.toByteArray();
                System.out.println("Image size: " + bytes.length);
                out.close();

                // TODO: здесь код для отправки изображения по сети
                transmitImage(bytes);

            } catch (AWTException | IOException ex) {
                System.err.println(ex);
            }
        }

    }

    private static void transmitImage(byte[] imgData) throws IOException {
        byte[] imageByteArray = imgData;
        int packets = (int) Math.ceil(imageByteArray.length / (float)Settings.DATAGRAM_MAX_SIZE);


        int HEADER_SIZE = 8;
        int MAX_PACKETS = 255;
        int SESSION_START = 128;
        int SESSION_END = 64;

        if(packets > MAX_PACKETS) {
            System.out.println("Image is too large to be transmitted!");
            return;
        }

        for(int i = 0; i <= packets; i++) {
            int flags = 0;
            flags = i == 0 ? flags | SESSION_START: flags;
            flags = (i + 1) * Settings.DATAGRAM_MAX_SIZE > imageByteArray.length ? flags | SESSION_END : flags;

            int size = (flags & SESSION_END) != SESSION_END ? Settings.DATAGRAM_MAX_SIZE : imageByteArray.length - i * Settings.DATAGRAM_MAX_SIZE;

            byte[] data = new byte[HEADER_SIZE + size];
            data[0] = (byte)flags;
            data[1] = (byte)sessionNumber;
            data[2] = (byte)packets;
            data[3] = (byte)(Settings.DATAGRAM_MAX_SIZE >> 8);
            data[4] = (byte)Settings.DATAGRAM_MAX_SIZE;
            data[5] = (byte)i;
            data[6] = (byte)(size >> 8);
            data[7] = (byte)size;

            System.arraycopy(imageByteArray, i * Settings.DATAGRAM_MAX_SIZE, data, HEADER_SIZE, size);
            transmit(data, Settings.ADDRESS, Settings.PORT);


            if((flags & SESSION_END) == SESSION_END) break;
        }

        sessionNumber = sessionNumber < Settings.MAX_SESSION_NUMBER ? ++sessionNumber : 0;
    }

    private static void transmit(byte[] data, String address, int port) throws IOException {
        socket = new MulticastSocket();

        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(address), port);
        socket.send(packet);
        socket.close();
    }

}
