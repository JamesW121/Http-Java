import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

public class HttpServer {
    public static void main(String[] args) throws Exception {
        ServerSocketChannel sschannel = ServerSocketChannel.open();
        sschannel.socket().bind(new InetSocketAddress(8090));
        sschannel.configureBlocking(false);
        Selector selector = Selector.open();
        sschannel.register(selector, SelectionKey.OP_ACCEPT);
        while (true) {
            if (selector.select(3000) == 0) {
                continue;
            }
            Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
            while (keyIter.hasNext()) {
                SelectionKey key = keyIter.next();
                new Thread(new HttpHandler(key)).run();
                keyIter.remove();
            }
        }
    }

    private static class HttpHandler implements Runnable {
        private int bufferSize = 1024;

        private String localCharset = "UTF-8";

        private SelectionKey key;

        public HttpHandler(SelectionKey key) {
            this.key = key;
        }

        public void handleAccept() throws IOException {
            SocketChannel clientChannel = ((ServerSocketChannel) key.channel())
                .accept();
            clientChannel.configureBlocking(false);
            clientChannel.register(key.selector(), SelectionKey.OP_READ,
                ByteBuffer.allocate(bufferSize));
        }

        public void handleRead() throws IOException {
            SocketChannel sc = (SocketChannel) key.channel();
            ByteBuffer buffer = (ByteBuffer) key.attachment();
            buffer.clear();
            if (sc.read(buffer) == -1) {
                sc.close();
            } else {
                buffer.flip();
                String receivedString = Charset.forName(localCharset)
                    .newDecoder().decode(buffer).toString();
                String[] requestMessage = receivedString.split("\r\n");
                for (String s : requestMessage) {
                    System.out.println(s);
                    if (s.isEmpty())
                        break;
                }
                String[] firstLine = requestMessage[0].split(" ");
                System.out.println();

                System.out.println("Method:\t" + firstLine[0]);
                System.out.println("url:\t" + firstLine[1]);
                System.out.println("HTTP Version:\t" + firstLine[2]);
                System.out.println();
                StringBuilder sendString = new StringBuilder();
                sendString.append("HTTP/1.1 200 OK\r\n");

                sendString.append(
                    "Content-Type:text/html;charset=" + localCharset + "\r\n");
                sendString.append("\r\n");
                sendString
                    .append("<html><head><title>Show Request</title></head><body bgcolor='#FFE540'>");
                sendString.append("Received Request£º<br/>");
                for (String s : requestMessage) {
                    sendString.append(s + "<br><br><br>");
                }
                sendString.append(" <center><img src= 'https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1535366318959&di=500f6c6321ce97f19db9146255008f26&imgtype=0&src=http%3A%2F%2F2017.zcool.com.cn%2Fcommunity%2F037b7355775cafb0000018c1b222864.gif'><center></body></html>");
                buffer = ByteBuffer
                    .wrap(sendString.toString().getBytes(localCharset));
                sc.write(buffer);
                sc.close();
            }
        }

        @Override
        public void run() {
            try {
                if (key.isAcceptable()) {
                    handleAccept();
                }
                if (key.isReadable()) {
                    handleRead();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
