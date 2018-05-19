/**
 * Created by jackzet on 19/05/2018.
 */
import java.net.*;
import java.io.*;
import java.util.ArrayList;

public class Server {

    private ServerSocket serverSocket;
    private ArrayList<Client> clients;

    public void start(int port){
        clients = new ArrayList<>();

        try{
            serverSocket = new ServerSocket(port);
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
