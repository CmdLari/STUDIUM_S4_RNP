# Wir benutzen Oracle OpenJDK 17
FROM openjdk:17

# Lege im Container einen Ordner für unser Programm an
# Konvention ist es diesen app zu nennen.
RUN mkdir /app

# Kopiere die Benötigten datein in den contain
# COPY <From|Files|Folders> <Destination>
COPY out/production/RN_Praktika/ /app

# Legt fest, dass alle nachfolgenden Befehle in diesem Verzeichnis ausgeführt werden
# ähnlich wie cd in Linux
WORKDIR /app

# Führe einen Befehl im Container aus.
# Wir starten den Server, auf Port 80
CMD java Server 80