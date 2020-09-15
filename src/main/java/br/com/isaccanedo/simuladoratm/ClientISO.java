package br.com.isaccanedo.simuladoratm;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import br.com.isaccanedo.simuladoratm.ISOUtil;
 

public class ClientISO {
 
    private final static Integer PORT_SERVER = 12345;
 
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws UnknownHostException, IOException {
        Socket clientSocket = new Socket("localhost", PORT_SERVER);
        String networkRequest = buildNetworkReqMessage();
 
        PrintWriter outgoing = new PrintWriter(clientSocket.getOutputStream());
        InputStreamReader incoming = new InputStreamReader(clientSocket.getInputStream());
 
        outgoing.print(networkRequest);
        outgoing.flush();
 
        int data;
        StringBuffer sb = new StringBuffer();
        int counter = 0;
        // 4 caracteres adicionais porque o cabeçalho da mensagem tem um comprimento de 4 dígitos
        int lengthOfMsg = 4;
        while((data = incoming.read()) != 0) {
            counter++;
            sb.append((char) data);
            if (counter == 4) lengthOfMsg += Integer.valueOf(sb.toString());
 
            // se o comprimento da mensagem de OFF a END OF MSG for o mesmo valor
            // cabeçalho e prossiga para o método processingMsg();
            if (lengthOfMsg == sb.toString().length()) {
                System.out.println("Rec. Msg ["+sb.toString()+"] len ["+sb.toString().length()+"]");
            }
        }
 
        outgoing.close();
        incoming.close();
        clientSocket.close();
    }
 
    private static String buildNetworkReqMessage() {
        StringBuilder networkReq = new StringBuilder();
 
        // MTI 1800
        networkReq.append("1800");
        // para solicitações, o DE ativo é o DE[3,7,11,12,13,48 dan 70]
        String bitmapReq = ISOUtil.getHexaBitmapFromActiveDE(new int[] {3,7,11,12,13,48,70});
        networkReq.append(bitmapReq);
        // DE 3 código de processamento
        networkReq.append("000001");
        // DE 7 data e hora da transmissão
        networkReq.append(new SimpleDateFormat("yyyyMMdd").format(new Date()));
        // DE 11 número de auditoria de rastreamento do sistema
        networkReq.append("000001");
        // DE 12 transação de hora local
        networkReq.append(new SimpleDateFormat("HHmmss").format(new Date()));
        // DE 13 transação de hora local
        networkReq.append(new SimpleDateFormat("MMdd").format(new Date()));
        // DE 48 Dados privados adicionais
        final String clientID = "CLNT001";
        // comprimento de 48
        String lengthBit48 = "";
        if (clientID.length() < 10) lengthBit48 = "00" + clientID.length();
        if (clientID.length() < 100 && clientID.length() >= 10) lengthBit48 = "0" + clientID.length();
        if (clientID.length() == 100) lengthBit48 = String.valueOf(clientID.length());
        networkReq.append(lengthBit48);
        networkReq.append(clientID);
 
        // DE 70 Código de informações da rede
        networkReq.append("001");
 
        // adicione 4 dígitos de msg como cabeçalho
        String msgHeader = "";
        if (networkReq.toString().length() < 10) msgHeader = "000" + networkReq.toString().length();
        if (networkReq.toString().length() < 100 && networkReq.toString().length() >= 10) msgHeader = "00" + networkReq.toString().length();
        if (networkReq.toString().length() < 1000 && networkReq.toString().length() >= 100) msgHeader = "0" + networkReq.toString().length();
        if (networkReq.toString().length() >= 1000) msgHeader = String.valueOf(networkReq.toString().length());
 
        StringBuilder finalNetworkReqMsg = new StringBuilder();
        finalNetworkReqMsg.append(msgHeader);
        finalNetworkReqMsg.append(networkReq.toString());
 
        return finalNetworkReqMsg.toString();
    }
}