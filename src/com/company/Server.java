package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public  Server(){
        connections = new ArrayList<>();
        done= false;
    }
    @Override
    public void run(){
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();

            while(!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            e.printStackTrace();
            shutdown();
        }
    }
    public void broadcast(ConnectionHandler sender, String message){
        for(ConnectionHandler ch: connections){
            if(ch!=null && !ch.equals(sender)){
                ch.sendMessage(message);
            }
        }
    }
    public void shutdown(){
        try{
        done = true;
        pool.shutdown();
        if(!server.isClosed()) {
            server.close();
        }
        for(ConnectionHandler ch : connections){
            ch.shutdown();
        }
        } catch (IOException e) {
            System.out.println(e);
        }

    }

    public class ConnectionHandler implements Runnable{

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nume;

        public ConnectionHandler(Socket client){
            this.client= client;

        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in= new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Introdu un nickname: ");
                nume=in.readLine();
                System.out.println(nume+" sa conectat!");
                broadcast(this, nume+" a intrat in chat!");

                String message;

                while((message=in.readLine())!=null){
                    if(message.startsWith("/nume ")){
                        String [] messageSplit = message.split(" ", 2);

                        if (messageSplit.length==2){
                            broadcast(this, nume+"si-a schimbat numele: "+messageSplit[1]);
                            System.out.println(nume+"si-a schimbat numele: "+messageSplit[1]);
                            nume= messageSplit[1];
                            out.println("Nickname a fost schimbat cu succes!");
                        } else{
                            out.println("Nu a fost furnizat nici un nume");

                        }

                    } else if(message.startsWith("/quit")){
                        broadcast(this, nume+" a parasit chat-ul!");
                        shutdown();

                    } else {
                        broadcast(this, nume+": "+ message);
                    }
                }

            } catch (IOException e) {
                System.out.println(e.fillInStackTrace());
                e.printStackTrace();
            }
        }

        public void sendMessage(String message){
            out.println(message);
        }

        public void shutdown(){
            try {

                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e){
                System.out.println(e);
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }

}
