package starvationevasion.client.Logic;

import starvationevasion.client.Networking.Client;
import starvationevasion.common.EnumPolicy;
import starvationevasion.common.EnumRegion;

/**
 * Created by Dayloki on 3/9/2016.
 */
public class ChatManager
{
  private String chat="";
  private  Client client;
  public ChatManager(Client client)
  {
  this.client=client;
  }
  public void sendChatToServer(String message,EnumRegion region,EnumPolicy card)
  {

   // String msg="chat " +region.name()+" {\"card\":null,\"text\":\""+client.getRegion().toString()+": "+message+"\"}";
    //String msg="chat " +region.name()+" {\"card\":null,\"text\":\""+"US_CALIFORNIA"+": "+message+"\"}";
    client.sendChatMessage(client.getRegion().toString()+": "+message,region);
    chat+=message+"\n";
  }
  public void sendChatToClient(String message)
  {
    chat+=message+"\n";

  }
  public String getChat()
  {
    return chat;
  }
}
