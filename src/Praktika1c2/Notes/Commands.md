
# Teste Der Anwendung - Server mittels WSL 

WINDOWS (Powershell oder CMD) geht nicht, weil windows immer ein `\r` anhängt und damit fallen immer alle Anweisungen durch...

# WSL

Ins Entwickler Verzichnis wechseln und befehle Ausführen: 
```shell
# 
cd /mnt/c/Users/fabia/Documents/4_Semester/40_Rechnernetze/30_Praktika_Uebung/RNP

# Von hier den ServerStarten 
cd /mnt/c/Users/fabia/Documents/4_Semester/40_Rechnernetze/30_Praktika_Uebung/RNP/out/production/RN_Praktika
java Praktika1c2.Server 12345 

# Von hier die Eingaben Benutzen 
cd /mnt/c/Users/fabia/Documents/4_Semester/40_Rechnernetze/30_Praktika_Uebung/RNP/src/Praktika1c2/Notes
netcat localhost 12345 < ./test_Input_success.txt # Erfolgreiche Testeingaben

# Testeingaben die Fehlschlagen weil \r\n enthalten ist
netcat localhost 12345 < ./test_Input_fail.txt

# Test mit genau 256 (incl. \n)
netcat localhost 12345 < ./test_Input_256_success.txt

# Test mit mit mehr als 256 
netcat localhost 12345 < ./test_Input_too_long.txt


```


# Böse eingabe die Trozdem funktioniert:
`netcat localhost 12345 < ./BadTest.txt`

# Perl Test-Script von Kossakowski
```shell
perl simple-test_v02.pl localhost 12345
```