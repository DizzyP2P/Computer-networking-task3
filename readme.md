Usage: java ClientStart [options]
Options:
  -h, --help            Show this help message and exit.
  -s, --size <size>     Set the size of data to send from the file. Use a specific byte count.
  -sm                   Use the maximum file size by default.
  -f, --file <path>     Specify the path to the file to be sent.
  -d, --desfile <path>  Specify the destination file path for received data.
  -l, --limit <min> <max> Specify the minimum and maximum packet sizes(1048576) for data chunks.
  -p, --port <port>     Specify the port number to connect to on the server.
  -a, --addr <address>  Specify the IP address or hostname of the server.
  -g show the details.

Examples:
  java ClientStart --file example.txt --size 1024 --desfile output.txt --port 12345 --addr 192.168.1.1
  java ClientStart -f example.txt -s MAX -d result.txt -l 100 2000 -p 12345 -a localhost

Usage: java ServerStart <log_file_path> <tcp_port>
Example: java ServerStart log.txt 12345