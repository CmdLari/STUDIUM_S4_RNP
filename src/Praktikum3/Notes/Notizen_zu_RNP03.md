

# Notizen zu RNP 03


## Client aufruf 

Notwendige Argumente

- argv[0], // ServerArg = localhost
- argv[1], // sourcePath = ./Resources/Testfile_100.csv
- argv[2], // destPath = ./Testfile_100.csv
- argv[3], // windowSize = 2
- argv[4]  // ErrorRate = 1

```shell
cd .\out\production\RN_Praktika\
java Praktikum3.FileCopyClient localhost ./Resources/Testfile_100.csv C:/Users/fabia/Documents/4_Semester/40_Rechnernetze/30_Praktika_Uebung/RNP/out/production/RN_Praktika/Testfile_on_Server_100.csv 2 1
```


## NetCat UDP Server 
Startet einen UDP Listener (Server) auf Port 23000 
```shell
ncat -l -u -p 23000
```

## NetCat UDP Client 
```shell
ncat -u 127.0.0.1 12345
```


# Nachlesen...

- NetCat (Portscans)
	- https://www.digitalocean.com/community/tutorials/how-to-use-netcat-to-establish-and-test-tcp-and-udp-connections
- UDP-Datagramm-Socket 
	- https://openbook.rheinwerk-verlag.de/java8/13_011.html
- Selectiv Repeat
	- https://www.baeldung.com/cs/selective-repeat-protocol
	- https://www.baeldung.com/cs/tcp-flow-control-vs-congestion-control
	
	
