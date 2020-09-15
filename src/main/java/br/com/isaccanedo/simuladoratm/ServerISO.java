package br.com.isaccanedo.simuladoratm;


import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import br.com.isaccanedo.simuladoratm.ISOUtil;
 

public class ServerISO {
 
    private static final Integer PORT = 12345;
    private static final Map<String, Integer> mappingDENetworkMsg = new HashMap<String, Integer>();
 
    /* Este método serve para inicializar o elemento de dados e o comprimento de cada elemento de dados ativo
    */
    private static void initMappingDENetworkRequest() {
        /* [data-element] [panjang data element] */
        mappingDENetworkMsg.put("3", 6);
        mappingDENetworkMsg.put("7", 8);
        mappingDENetworkMsg.put("11", 6);
        mappingDENetworkMsg.put("12", 6);
        mappingDENetworkMsg.put("13", 4);
        mappingDENetworkMsg.put("39", 3);
        mappingDENetworkMsg.put("48", 999);
        mappingDENetworkMsg.put("70", 3);
    }
 
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        initMappingDENetworkRequest();
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Servidor está escutando na porta ["+PORT+"]");
        Socket socket = serverSocket.accept();
        InputStreamReader inStreamReader = new InputStreamReader(socket.getInputStream());
        PrintWriter sendMsg = new PrintWriter(socket.getOutputStream());
 
        int data;
        StringBuffer sb = new StringBuffer();
        int counter = 0;
 
        // 4 caracteres adicionais porque o cabeçalho da mensagem tem um comprimento de 4 dígitos
        int lengthOfMsg = 4;
        while((data = inStreamReader.read()) != 0) {
            counter++;
            sb.append((char) data);
            if (counter == 4) lengthOfMsg += Integer.valueOf(sb.toString());
 
         // Se o comprimento da mensagem de MTI a END OF MSG for igual ao valor
        // cabeçalho então prossiga para o método processingMsg();
            if (lengthOfMsg == sb.toString().length()) {
                System.out.println("Rec. Msg ["+sb.toString()+"] len ["+sb.toString().length()+"]");
                processingMsg(sb.toString(), sendMsg);
            }
        }
    }
 
    /** Processa a mensagem enviada pelo cliente com base no valor MTI.
    * @param msg request data que contém [header 4byte] [MTI] [BITMAP] [DATA ELEMENT]
    * @param sendMsg printWriter objeto para escrever msg para fluxo de rede
    */
    private static void processingMsg(String data, PrintWriter sendMsg) {
        // msg.asli tanpa 4 digit msg.header
        String origMsgWithoutMsgHeader = data.substring(4, data.length());
 
        // cek nilai MTI
        if (ISOUtil.findMTI(origMsgWithoutMsgHeader).equalsIgnoreCase("1800")) {
            handleNetworkMsg(origMsgWithoutMsgHeader, sendMsg);
        }
    }
 
    /** Este método processará solicitações de gerenciamento de rede e adicionará
    * 1 elemento de dados, ou seja, elemento de dados 39 (código de resposta) 000 para o cliente / remetente
    * @param networkMsg request msg que contém [header 4byte] [MTI] [BITMAP] [DATA ELEMENT]
    * @param sendMsg printWriter objeto para escrever msg para fluxo de rede
    */    
    private static void handleNetworkMsg(String networkMsg, PrintWriter sendMsg) {
        int panjangBitmap = ISOUtil.findLengthOfBitmap(networkMsg);
        String hexaBitmap = networkMsg.substring(4, 4+panjangBitmap);
 
        // contar bitmaps
        String binaryBitmap = ISOUtil.findBinaryBitmapFromHexa(hexaBitmap);
        String[] activeDE = ISOUtil.findActiveDE(binaryBitmap).split(";");
 
        StringBuilder networkResp = new StringBuilder();
 
        // configurando MTI para responder a solicitação de rede
        networkResp.append("1810");
 
        // para resposta, o DE ativo é DE[3,7,11,12,13,39,48 dan 70]
        String bitmapReply = ISOUtil.getHexaBitmapFromActiveDE(new int[] {3,7,11,12,13,39,48, 70});
        networkResp.append(bitmapReply);
 
        // índice msg começa dr (4 digit MTI+panjang bitmap = index DE ke 3)
        int startIndexMsg = 4+ISOUtil.findLengthOfBitmap(networkMsg);
        int nextIndex = startIndexMsg;
        String sisaDefaultDE = "";
 
        // pegue o mesmo valor DE primeiro
        for (int i=0;i<activeDE.length;i++) {
            // demora um pouco ke 3
            if (activeDE[i].equalsIgnoreCase("3")) {
                nextIndex += mappingDENetworkMsg.get(activeDE[i]);
                networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
                debugMessage(3, networkMsg.substring(startIndexMsg, nextIndex));
            } else if(activeDE[i].equalsIgnoreCase("7")) {
                startIndexMsg = nextIndex;
                nextIndex += mappingDENetworkMsg.get(activeDE[i]);
                networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
                debugMessage(7, networkMsg.substring(startIndexMsg, nextIndex));
            } else if(activeDE[i].equalsIgnoreCase("11")) {
                startIndexMsg = nextIndex;
                nextIndex += mappingDENetworkMsg.get(activeDE[i]);
                networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
                debugMessage(11, networkMsg.substring(startIndexMsg, nextIndex));
            } else if(activeDE[i].equalsIgnoreCase("12")) {
                startIndexMsg = nextIndex;
                nextIndex += mappingDENetworkMsg.get(activeDE[i]);
                networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
                debugMessage(12, networkMsg.substring(startIndexMsg, nextIndex));
            } else if(activeDE[i].equalsIgnoreCase("13")) {
                startIndexMsg = nextIndex;
                nextIndex += mappingDENetworkMsg.get(activeDE[i]);
                networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
                debugMessage(13, networkMsg.substring(startIndexMsg, nextIndex));
            } else if(activeDE[i].equalsIgnoreCase("48")) {
                startIndexMsg = nextIndex;
                // pegue primeiro var.len utk DE 48
                int varLen = Integer.valueOf(networkMsg.substring(startIndexMsg, (startIndexMsg+3)));
                // 3 digit utk variabel len
                varLen += 3;
                nextIndex += varLen;
                sisaDefaultDE += networkMsg.substring(startIndexMsg, nextIndex);
                debugMessage(48, networkMsg.substring(startIndexMsg, nextIndex));
            } else if(activeDE[i].equalsIgnoreCase("70")) {
                startIndexMsg = nextIndex;
                nextIndex += mappingDENetworkMsg.get(activeDE[i]);
                sisaDefaultDE += networkMsg.substring(startIndexMsg, nextIndex);
                debugMessage(70, networkMsg.substring(startIndexMsg, nextIndex));
            }
        }
 
        // código de resposta 39 sucesso
        networkResp.append("000");
        // tambahkan sisa default DE
        networkResp.append(sisaDefaultDE);
 
        // adicionar comprimento 4 digit utk msg.header
        String msgHeader = "";
        if (networkResp.length() < 10) msgHeader = "000" + networkResp.length();
        if (networkResp.length() < 100 && networkResp.length() >= 10) msgHeader = "00" + networkResp.length();
        if (networkResp.length() < 1000 && networkResp.length() >= 100) msgHeader = "0" + networkResp.length();
        if (networkResp.length() >= 1000) msgHeader = String.valueOf(networkResp.length());
 
        String finalMsg = msgHeader + networkResp.toString();
 
        // enviar para o cliente
        sendMsg.print(finalMsg);
        sendMsg.flush();
    }
 
    private static void debugMessage(Integer fieldNo, String msg) {
        System.out.println("["+fieldNo+"] ["+msg+"]");
    }
}