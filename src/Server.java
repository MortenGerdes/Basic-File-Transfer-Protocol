/**
 * Created by jackzet on 19/05/2018.
 */
import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Server {

    private DatagramSocket serverSocket;
    private ArrayList<Client> clients;
    private byte[] receiveData;
    int B = 4;

    public void start(int port){
        clients = new ArrayList<>();



        try{
            serverSocket = new DatagramSocket(port);
            receiveData = new byte[1024];

            while(true){
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                ByteBuffer bb = ByteBuffer.wrap(receiveData);
            }

        }catch (Exception e){
            e.printStackTrace();
        }


    }

    public void stop(){
        try{
            serverSocket.close();
        }catch(Exception e){
            e.printStackTrace();
        }

    }
}
