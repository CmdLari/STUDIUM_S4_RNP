
echo "     ================ All test for RN TCP STRING SERVER ===================="

echo "================ Change directory ================"
cd /mnt/c/Users/fabia/Documents/4_Semester/40_Rechnernetze/30_Praktika_Uebung/RNP/src/Praktika1c2/Notes

echo "Run Positiv Tests"
netcat localhost 12345 < ./test_Input_success.txt # Erfolgreiche Testeingaben



echo "================ Run Negativ Tests ================"
# Testeingaben die Fehlschlagen weil \r\n enthalten ist
netcat localhost 12345 < ./test_Input_fail.txt

# Test mit mit mehr als 256
netcat localhost 12345 < ./test_Input_too_long.txt


echo "================ Run Edgecase Test with 256 Chars (incl. newlin) ================"
# Test mit genau 256 (incl. \n)
netcat localhost 12345 < ./test_Input_256_success.txt

echo "================ Run Shutdown Test with wrong Password ================"
# Test mit genau 256 (incl. \n)
netcat localhost 12345 < ./test_Input_shutdown_fail.txt

echo "================ Run Shutdown Test with correct Password ================"
# Test mit genau 256 (incl. \n)
netcat localhost 12345 < ./test_Input_shutdown_success.txt

