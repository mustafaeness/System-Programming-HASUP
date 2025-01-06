package example;

import java.util.Arrays;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import example.SubscriberOuterClass.Subscriber;

public class Server1 {

    private static final int serverId = 1;       // Server3 için ID

    public static void main(String[] args) {
        int myClientPort = 6001; // İstemcilerle iletişim için port
        int myServerPort = 5001; // Sunucular arası iletişim için port
        startListeningFromAdmin(7001);
        int[] otherServerPorts = {5002, 5003}; // Diğer sunucuların portları
        int adminPort = 8001; // Admin ile Capacity iletişimi için port


        while (faultToleranceLevel == -1) {
            System.out.println("Waiting for fault tolerance level from admin...");
            try {
                Thread.sleep(1000); // 1 saniye bekle
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Fault Tolerance Level received: " + faultToleranceLevel);


        // Paylaşılan veya yerel listeyi belirle
        List<Subscriber> subscribers = determineList(faultToleranceLevel, serverId);

        // Sunucular arası haberleşme
        connectToOtherServers(otherServerPorts, subscribers);
        startListeningFromOtherServers(myServerPort, subscribers);

        // İstemcilerle haberleşme
        startListeningFromClients(myClientPort, subscribers);

        // Admin'den Capacity isteklerini dinlemeye başla
        handleAdminCapacityRequests(adminPort, serverId, subscribers);
    }

    private static List<Subscriber> determineList(int faultToleranceLevel, int serverId) {
        if (faultToleranceLevel == 1) {
            if (serverId == 3) {
                // Server3 bağımsız bir liste kullanır
                return Collections.synchronizedList(new ArrayList<>());
            } else {
                // Server1 ve Server2 shared list kullanır
                return SharedResources.sharedSubscribers;
            }
        } else if (faultToleranceLevel >= 2) {
            // Tüm serverlar shared list kullanır
            return SharedResources.sharedSubscribers;
        } else {
            // Her server bağımsız liste kullanır
            return Collections.synchronizedList(new ArrayList<>());
        }
    }


    private static void connectToOtherServers(int[] ports, List<Subscriber> subscribers) {
        try {
            System.out.println("Waiting 2 seconds before connecting to other servers...");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting to connect to other servers: " + e.getMessage());
        }
        for (int port : ports) {
            int CURRENT_PORT = 5001; // Mevcut sunucunun portu
            if (port == CURRENT_PORT) continue; // Kendine bağlanmaya çalışma

            new Thread(() -> {
                boolean connected = false;
                while (!connected) {
                    try (Socket socket = new Socket("localhost", port);
                         DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

                        System.out.println("Connected to Server on port: " + port);
                        connected = true;

                        while (faultToleranceLevel == 2) { // Sadece Fault Tolerance Level = 2 olduğunda
                            synchronized (subscribers) {
                                for (Subscriber subscriber : subscribers) {
                                    byte[] data = subscriber.toByteArray();
                                    dos.writeInt(data.length); // Mesaj boyutunu gönder
                                    dos.write(data);           // Mesajı gönder
                                    dos.flush();
                                }
                            }
                            Thread.sleep(5000); // 5 saniyede bir listeyi paylaş
                        }
                    } catch (Exception e) {
                        System.err.println("Error connecting to server on port " + port + ": " + e.getMessage());
                        try {
                            Thread.sleep(3000); // Bağlantı başarısızsa 3 saniye bekle ve tekrar dene
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            }).start();
        }
    }



    private static void startListeningFromClients(int port, List<Subscriber> subscribers) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(6001)) {
                System.out.println("Listening for clients on port: " + port);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> {
                        try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream())) {
                            int length = dis.readInt(); // Mesaj boyutunu oku
                            byte[] buffer = new byte[length];
                            dis.readFully(buffer);      // Mesajın tamamını oku

                            Subscriber subscriber = Subscriber.parseFrom(buffer); // Deserialize et
                            synchronized (subscribers) {
                                boolean updated = false;
                                for (int i = 0; i < subscribers.size(); i++) {
                                    if (subscribers.get(i).getId() == subscriber.getId()) {
                                        subscribers.set(i, subscriber);
                                        updated = true;
                                        break;
                                    }
                                }
                                if (!updated) {
                                    subscribers.add(subscriber);
                                }

                                // Eğer fault_tolerance_level == 2 ise, değişiklikleri diğer sunuculara gönder
                                if (faultToleranceLevel == 2) {
                                    broadcastToOtherServers(subscriber);
                                }

                                logSubscribers(subscribers);
                            }
                        } catch (Exception e) {
                            System.err.println("Error handling client connection: " + e.getMessage());
                        }
                    }).start();
                }
            } catch (Exception e) {
                System.err.println("Error starting client listener: " + e.getMessage());
            }
        }).start();
    }

    private static void broadcastToOtherServers(Subscriber subscriber) {
        int[] otherServerPorts = {5002, 5003}; // Diğer sunucuların portları (Server1 hariç)
        for (int port : otherServerPorts) {
            new Thread(() -> {
                // Fault tolerance kontrolü
                if (faultToleranceLevel == 1 && serverId == 3) {
                    // Fault tolerance level 1 ve Server3 ise, broadcast yapmaz
                    return;
                }

                try (Socket socket = new Socket("localhost", port);
                     DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
                    byte[] data = subscriber.toByteArray();
                    dos.writeInt(data.length); // Mesaj boyutunu gönder
                    dos.write(data);           // Mesajı gönder
                    dos.flush();
                    System.out.println("Broadcasted subscriber update to server on port " + port);
                } catch (Exception e) {
                    System.err.println("Error broadcasting to server on port " + port + ": " + e.getMessage());
                }
            }).start();
        }
    }


    private static void startListeningFromOtherServers(int port, List<Subscriber> subscribers) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Listening for other servers on port: " + port);
                while (true) {
                    Socket serverSocketConnection = serverSocket.accept();
                    new Thread(() -> {
                        try (DataInputStream dis = new DataInputStream(serverSocketConnection.getInputStream())) {
                            while (true) {
                                int length = dis.readInt(); // Mesaj boyutunu oku
                                byte[] buffer = new byte[length];
                                dis.readFully(buffer);      // Mesajın tamamını oku

                                Subscriber subscriber = Subscriber.parseFrom(buffer); // Deserialize et

                                if (faultToleranceLevel == 1 && serverId == 3) {
                                    // Fault Tolerance Level = 1 ve Server3 ise, değişiklikleri görmezden gel
                                    System.out.println("Received update for Server3, ignoring due to fault_tolerance_level = 1.");
                                    return;
                                }

                                synchronized (subscribers) {
                                    boolean updated = false;
                                    for (int i = 0; i < subscribers.size(); i++) {
                                        if (subscribers.get(i).getId() == subscriber.getId()) {
                                            subscribers.set(i, subscriber); // Güncelleme yap
                                            updated = true;
                                            break;
                                        }
                                    }
                                    if (!updated) {
                                        subscribers.add(subscriber); // Yeni bir subscriber ekle
                                    }
                                    logSubscribers(subscribers);
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Error handling server connection: " + e.getMessage());
                        }
                    }).start();
                }
            } catch (Exception e) {
                System.err.println("Error starting server listener: " + e.getMessage());
            }
        }).start();
    }





    private static void logSubscribers(List<Subscriber> subscribers) {
        synchronized (subscribers) {
            System.out.println("Current subscribers:");
            for (Subscriber subscriber : subscribers) {
                System.out.println(subscriber);
            }
        }
    }
    private static int faultToleranceLevel = -1; // Default değer

    private static void startListeningFromAdmin(int port) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Listening for admin on port: " + port);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> {
                        try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream())) {
                            int length = dis.readInt(); // Mesaj boyutunu oku
                            byte[] buffer = new byte[length];
                            dis.readFully(buffer);      // Mesajın tamamını oku

                            // Configuration nesnesini deserialize et
                            ConfigurationOuterClass.Configuration configuration =
                                    ConfigurationOuterClass.Configuration.parseFrom(buffer);

                            // Configuration bilgilerini logla
                            System.out.println("Received configuration from admin:");
                            System.out.println("Fault Tolerance Level: " + configuration.getFaultToleranceLevel());
                            System.out.println("Method: " + configuration.getMethod());

                            // Fault tolerance seviyesini işleme al
                            processConfiguration(configuration);
                        } catch (Exception e) {
                            System.err.println("Error handling admin connection: " + e.getMessage());
                        }
                    }).start();
                }
            } catch (Exception e) {
                System.err.println("Error starting admin listener on port " + port + ": " + e.getMessage());
            }
        }).start();
    }
    private static void processConfiguration(ConfigurationOuterClass.Configuration configuration) {
        faultToleranceLevel = configuration.getFaultToleranceLevel();
        System.out.println("Fault Tolerance Level set to: " + faultToleranceLevel);
    }


    private static int getFaultToleranceLevelFromConfiguration() {
        if (faultToleranceLevel == -1) {
            throw new IllegalStateException("Fault tolerance level has not been set by admin.");
        }
        return faultToleranceLevel;
    }
    private static void handleAdminCapacityRequests(int adminPort, int serverId, List<Subscriber> subscribers) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(adminPort)) {
                System.out.println("Listening for Capacity requests on port: " + adminPort);

                while (true) {
                    try (Socket adminSocket = serverSocket.accept()) {
                        // Gelen Capacity isteğini oku
                        InputStream inputStream = adminSocket.getInputStream();
                        byte[] buffer = new byte[1024];
                        int bytesRead = inputStream.read(buffer);

                        if (bytesRead > 0) {
                            byte[] actualData = Arrays.copyOf(buffer, bytesRead); // Yalnızca okunan baytları alın
                            CapacityOuterClass.Capacity capacityRequest = CapacityOuterClass.Capacity.parseFrom(actualData);
                            System.out.println("Received Capacity request: " + capacityRequest);


                            // Yanıt nesnesini oluştur
                            if (capacityRequest.getServerId() == serverId) {
                                CapacityOuterClass.Capacity response = CapacityOuterClass.Capacity.newBuilder()
                                        .setServerId(serverId)
                                        .setServerStatus(subscribers.size()) // Mevcut abone sayısı
                                        .setTimestamp(System.currentTimeMillis() / 1000) // Epoch time
                                        .build();

                                // Yanıtı admin'e gönder
                                OutputStream outputStream = adminSocket.getOutputStream();
                                outputStream.write(response.toByteArray());
                                outputStream.flush();

                                System.out.println("Sent Capacity response: " + response);
                            } else {
                                System.err.println("Received request for wrong server ID: " + capacityRequest.getServerId());
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing Capacity request: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.err.println("Error starting Capacity listener on port " + adminPort + ": " + e.getMessage());
            }
        }).start();
    }


}
