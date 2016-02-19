package starvationevasion.server;


/**
 * @author Javier Chavez
 */

import starvationevasion.server.model.Response;
import starvationevasion.server.model.User;
import starvationevasion.sim.Simulator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 */
public class Server
{
  private ServerSocket serverSocket;
  private LinkedList<Worker> allConnections = new LinkedList<>();
  private long startNanoSec = 0l;
  private final Timer timer = new Timer();
  private Simulator simulator;
  private HashMap<String, User> users = new HashMap<>();

  public Server (int portNumber)
  {
    startNanoSec = System.nanoTime();
    //    simulator = new Simulator(Constant.FIRST_YEAR);
    ////    for (EnumRegion region : EnumRegion.US_REGIONS)
    ////    {
    ////      playerHands.put(region, new ArrayList<>(Arrays.asList(simulator.drawCards(region))));
    ////    }
    //
    //    WorldData currentWorldData = simulator.init();

    try
    {
      serverSocket = new ServerSocket(portNumber);
    }
    catch(IOException e)
    {
      System.err.println("Server error: Opening socket failed.");
      e.printStackTrace();
      System.exit(-1);
    }

    // Mimic a chron-job that every half sec. it deletes stale workers.
    timer.schedule(new TimerTask()
    {
      @Override
      public void run ()
      {
        cleanConnectionList();
      }
    }, 500, 500);

    waitForConnection(portNumber);


  }


  /**
   * Get the time of server spawn time to a given time.
   *
   * @param curr is the current time
   *
   * @return difference time in seconds
   */
  public double getTimeDiff (long curr)
  {
    long nanoSecDiff = curr - startNanoSec;
    return nanoSecDiff / 1000000000.0;
  }


  /**
   * Wait for a connection.
   *
   * @param port port to listen on.
   */
  public void waitForConnection (int port)
  {

    String host = "";
    try
    {
      host = InetAddress.getLocalHost().getHostName();
    }
    catch(UnknownHostException e)
    {
      e.printStackTrace();
    }
    while(true)
    {
      System.out.println("Server(" + host + "): waiting for Connection on port: " + port);
      try
      {
        Socket client = serverSocket.accept();
        System.out.println("Server: *********** new Connection");
        Worker worker = new Worker(client, this);
        worker.setServerStartTime(startNanoSec);

        worker.start();
        worker.setName("worker" + timeDiff());

        allConnections.add(worker);

      }
      catch(IOException e)
      {
        System.err.println("Server error: Failed to connect to client.");
        e.printStackTrace();
      }
    }
  }


  /**
   * Send a response of a transaction to all listening workers.
   *
   * @param response a response to be send globally.
   */
  public void broadcastTransaction (Response response)
  {
    for (Worker workers : allConnections)
    {
      workers.send(response.toString());
    }
  }

  /**
   * Send a message to all workers.
   *
   * @param s string containing a message.
   */
  public void broadcast (String s)
  {
    for (Worker workers : allConnections)
    {
      workers.send(s);
    }
  }

  public static void main (String args[])
  {
    //Valid port numbers are Port numbers are 1024 through 65535.
    //  ports under 1024 are reserved for system services http, ftp, etc.
    int port = 5555; //default
    if (args.length > 0)
    {
      try
      {
        port = Integer.parseInt(args[0]);
        if (port < 1)
        {
          throw new Exception();
        }
      }
      catch(Exception e)
      {
        System.out.println("Usage: Server portNumber");
        System.exit(0);
      }
    }

    new Server(port);
  }


  public String timeDiff ()
  {
    long nanoSecDiff = System.nanoTime() - startNanoSec;
    double secDiff = nanoSecDiff / 1000000000.0;
    return String.format("%.3f", secDiff);
  }

  public double uptime ()
  {
    long nanoSecDiff = System.nanoTime() - startNanoSec;
    double secDiff = nanoSecDiff / 1000000000.0;
    return secDiff;
  }

  public Simulator getSimulator ()
  {
    return simulator;
  }

  public User getUser (String worker)
  {
    return users.get(worker);
  }

  public boolean addUser (User u, Worker client)
  {
    users.put(client.getName(), u);
    // get available regions
    // check if region is available
    // if username and region available
    // return true
    // else return region taken
    // else return username taken

    return true;
  }

  /**
   * Handle a handshake with web client
   * @param x
   * @return
   */
  private static String handshake (String x)
  {

    MessageDigest digest = null;
    byte[] one = x.getBytes();
    byte[] two = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11".getBytes();
    byte[] combined = new byte[one.length + two.length];

    for (int i = 0; i < combined.length; ++i)
    {
      combined[i] = i < one.length ? one[i] : two[i - one.length];
    }

    try
    {
      digest = MessageDigest.getInstance("SHA-1");
    }
    catch(NoSuchAlgorithmException e)
    {
      e.printStackTrace();
      return "";
    }

    digest.reset();
    digest.update(combined);

    return new String(Base64.getEncoder().encode(digest.digest()));

  }

  private void websocketConnect(Worker worker) throws IOException
  {
    // Handling websocket
    StringBuilder reading = new StringBuilder();
    String line = "";

    while((line = worker.getClientReader().readLine()) != null)
    {
      if (line.equals("") || line.isEmpty())
      {
        return;
      }
      reading.append(line);
      if (line.contains("Sec-WebSocket-Key:"))
      {
        String key = line.replace("Sec-WebSocket-Key: ", "");
        String socketKey = Server.handshake(key);
        worker.send("HTTP/1.1 101 Switching Protocols\n" +
                            "Upgrade: websocket\n" +
                            "Connection: Upgrade\n" +
                            "Sec-WebSocket-Accept: " + socketKey + "\n");
      }
    }
  }

  private void cleanConnectionList ()
  {
    int con = 0;
    for (int i = 0; i < allConnections.size(); i++)
    {
      if (!allConnections.get(i).isRunning())
      {
        // the worker is not running. remove it.
        allConnections.remove(i);
        con++;
      }
    }
    // check if any removed. Show removed count
    if (con > 0)
    {
      System.out.println("Removed " + con + " connection workers.");
    }
  }

}