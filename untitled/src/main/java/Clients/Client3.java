package Clients;

import java.io.DataOutputStream;
import java.net.Socket;
import example.SubscriberOuterClass.Subscriber;

public class Client3 {
    public static void main(String[] args) {
        try {
            // Server1'e bağlan ve SUBS isteği gönder
            try (Socket socket = new Socket("localhost", 6001)) { // Server1'e bağlan
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                // Subscriber nesnesi oluştur (SUBS isteği)
                Subscriber subscriber = Subscriber.newBuilder()
                        .setId(3) // Benzersiz ID
                        .setNameSurname("Ozgur Ercan") // Abone adı
                        .setStatus("SUBS") // Abone durumu (SUBS - Subscribe)
                        .setLastAccessed(System.currentTimeMillis()) // Son erişim zamanı
                        .build();

                byte[] data = subscriber.toByteArray();

                // Mesaj boyutunu ve veriyi gönder
                dos.writeInt(data.length);
                dos.write(data);
                dos.flush();

                System.out.println("SUBS request sent to Server1: " + subscriber);
            }

            // 5 saniye bekle
            Thread.sleep(5000);

            // Server2'ye bağlan ve ONLN isteği gönder
            try (Socket socket = new Socket("localhost", 6002)) { // Server2'ye bağlan
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                // Subscriber nesnesi oluştur (ONLN isteği)
                Subscriber subscriber = Subscriber.newBuilder()
                        .setId(3) // Aynı ID (Server1'de kayıtlı olan)
                        .setNameSurname("Ozgur Ercan") // Aynı isim
                        .setStatus("ONLN") // Abone durumu (ONLN - Online)
                        .setLastAccessed(System.currentTimeMillis()) // Son erişim zamanı
                        .build();

                byte[] data = subscriber.toByteArray();

                // Mesaj boyutunu ve veriyi gönder
                dos.writeInt(data.length);
                dos.write(data);
                dos.flush();

                System.out.println("ONLN request sent to Server2: " + subscriber);
            }

            // 5 saniye bekle
            Thread.sleep(5000);

            // Server3'e bağlan ve OFFLN isteği gönder
            try (Socket socket = new Socket("localhost", 6003)) { // Server3'e bağlan
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                // Subscriber nesnesi oluştur (OFFLN isteği)
                Subscriber subscriber = Subscriber.newBuilder()
                        .setId(3) // Güncellenecek abonenin ID'si
                        .setNameSurname("Ozgur Ercan") // Aynı isim
                        .setStatus("OFFLN") // Abone durumu (OFFLN - Offline)
                        .setLastAccessed(System.currentTimeMillis()) // Son erişim zamanı
                        .build();

                byte[] data = subscriber.toByteArray();

                // Mesaj boyutunu ve veriyi gönder
                dos.writeInt(data.length);
                dos.write(data);
                dos.flush();

                System.out.println("OFFLN request sent to Server3: " + subscriber);
            }

        } catch (Exception e) {
            System.err.println("Error in Client3: " + e.getMessage());
        }
    }
}
