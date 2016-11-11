javac -cp ./src/jgrapht-core-1.0.0.jar ./src/AntiFraud.java
java  -cp ./src/jgrapht-core-1.0.0.jar:./src/ AntiFraud ./paymo_input/batch_payment.txt ./paymo_input/stream_payment.txt ./paymo_output/output1.txt ./paymo_output/output2.txt ./paymo_output/output3.txt

