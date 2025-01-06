require 'socket'
require 'google/protobuf'
require 'json'
require_relative 'configuration_pb'
require_relative 'capacity_pb'

# dist_subs.conf dosyasını oku
def read_config_file
  file_path = "dist_subs.conf"
  unless File.exist?(file_path)
    puts "Configuration file not found: #{file_path}"
    exit
  end

  config_line = File.read(file_path).strip
  if config_line =~ /fault_tolerance_level\s*=\s*(\d+)/
    $1.to_i
  else
    puts "Invalid configuration file format."
    exit
  end
end

# Configuration nesnesi oluştur
def create_configuration(fault_tolerance_level)
  Example::Configuration.new(
    fault_tolerance_level: fault_tolerance_level,
    method: "STRT"
  )
end

# Sunuculara Configuration gönder
def send_start_command_to_servers(servers, configuration)
  servers.each do |host, port|
    begin
      socket = TCPSocket.new(host, port)
      puts "Connected to server: #{host}:#{port}"

      serialized_data = Example::Configuration.encode(configuration)
      socket.write([serialized_data.size].pack("N")) # Mesaj boyutunu gönder
      socket.write(serialized_data) # Configuration gönder

      puts "Configuration sent to server: #{host}:#{port}"
      socket.close
    rescue => e
      puts "Error connecting to server #{host}:#{port} - #{e.message}"
    end
  end
end

# Capacity isteği gönder ve yanıt al
def send_capacity_request(server_port, server_id)
  capacity_request = Example::Capacity.new(
    server_id: server_id
  )

  begin
    socket = TCPSocket.new('localhost', server_port)
    socket.write(capacity_request.to_proto)

    response_data = socket.read
    capacity_response = Example::Capacity.decode(response_data)

    # Yanıtı yazdır
    puts "Received response from server #{server_id}:"
    puts "  Server Status (Subscribers): #{capacity_response.server_status}"
    puts "  Timestamp: #{capacity_response.timestamp}"

    # Plotter'a veri gönder
    send_capacity_to_plotter({
                               server_id: capacity_response.server_id,
                               server_status: capacity_response.server_status,
                               timestamp: capacity_response.timestamp
                             })
  rescue => e
    puts "Error communicating with server #{server_id}: #{e.message}"
  ensure
    socket.close if socket
  end
end

# Plotter'a veri gönder
def send_capacity_to_plotter(capacity_data)
  begin
    plotter_socket = TCPSocket.new('localhost', 9000) # Plotter portu
    plotter_socket.puts(capacity_data.to_json) # JSON formatında gönder
    plotter_socket.close
    puts "Capacity data sent to plotter: #{capacity_data}"
  rescue => e
    puts "Error sending data to plotter: #{e.message}"
  end
end

# Tüm sunuculara Capacity istekleri gönder
def query_all_servers
  send_capacity_request(8001, 1) # Server1
  send_capacity_request(8002, 2) # Server2
  send_capacity_request(8003, 3) # Server3
end

# Admin ana döngüsü
def start_admin_loop
  loop do
    query_all_servers
    sleep(5) # 5 saniyede bir sunuculardan veri iste
  end
end

# Main program
if __FILE__ == $0
  fault_tolerance_level = read_config_file
  configuration = create_configuration(fault_tolerance_level)

  # Sunucular
  servers = [
    ["localhost", 7001], # Server1
    ["localhost", 7002], # Server2
    ["localhost", 7003]  # Server3
  ]

  # Configuration gönder
  send_start_command_to_servers(servers, configuration)

  # Admin döngüsünü başlat
  start_admin_loop
end
