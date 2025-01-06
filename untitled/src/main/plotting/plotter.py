import socket
import json
import matplotlib.pyplot as plt
import threading
import time

server_data = {1: [], 2: [], 3: []}  # Server doluluk verileri
timestamps = {1: [], 2: [], 3: []}  # Her sunucu için ayrı zaman damgaları

def update_plot():
    plt.ion()  # Interaktif mod
    fig, ax = plt.subplots()
    while True:
        ax.clear()  # Grafiği temizle
        for server_id, data in server_data.items():
            if len(data) > 0 and len(timestamps[server_id]) > 0:  # Veri var mı kontrol et
                ax.plot(timestamps[server_id], data, label=f"Server {server_id}")
        ax.legend()
        ax.set_xlabel("Time")
        ax.set_ylabel("Capacity")
        ax.set_title("Server Capacities Over Time")
        plt.pause(0.1)  # 100ms'de bir güncelle

def start_plotter_server():
    host = 'localhost'
    port = 9000
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind((host, port))
    server_socket.listen(5)
    print(f"Plotter listening on {host}:{port}")

    while True:
        conn, addr = server_socket.accept()
        print(f"Connection from {addr}")
        data = conn.recv(1024).decode('utf-8')
        if data:
            print(f"Received data: {data}")
            try:
                capacity = json.loads(data)
                server_id = capacity["server_id"]
                server_status = capacity["server_status"]
                timestamp = capacity["timestamp"]

                # Verileri ekle
                if server_id in server_data:
                    server_data[server_id].append(server_status)
                    timestamps[server_id].append(timestamp)

                    # Sadece en son 10 veriyi tutmak için
                    if len(server_data[server_id]) > 10:
                        server_data[server_id].pop(0)
                        timestamps[server_id].pop(0)

                    print(f"Updated data for Server {server_id}: {server_data[server_id]}")
                else:
                    print(f"Unknown server_id: {server_id}")

            except json.JSONDecodeError as e:
                print(f"Error decoding JSON: {e}")
        conn.close()

# Plot ve sunucu başlatma
threading.Thread(target=start_plotter_server, daemon=True).start()
update_plot()
