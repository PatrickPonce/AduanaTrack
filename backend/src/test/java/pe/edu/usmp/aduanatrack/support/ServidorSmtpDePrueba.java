package pe.edu.usmp.aduanatrack.support;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/**
 * Servidor SMTP mínimo que corre dentro de la prueba y guarda los correos que recibe, para comprobar que un
 * correo sale de verdad por SMTP sin depender de Mailpit ni de Docker. Escucha solo en la interfaz local, en un
 * puerto libre que elige el sistema, y responde lo justo para que JavaMail entregue el mensaje (sin TLS ni
 * autenticación).
 */
public final class ServidorSmtpDePrueba implements AutoCloseable {

    private final ServerSocket socket;
    private final List<MimeMessage> recibidos = new CopyOnWriteArrayList<>();
    private final Thread hilo;

    public ServidorSmtpDePrueba() {
        try {
            socket = new ServerSocket(0, 5, InetAddress.getLoopbackAddress());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo abrir el puerto del SMTP de prueba", e);
        }
        hilo = new Thread(this::atender, "smtp-de-prueba");
        hilo.setDaemon(true);
        hilo.start();
    }

    public String host() {
        return socket.getInetAddress().getHostAddress();
    }

    public int puerto() {
        return socket.getLocalPort();
    }

    /** Correos entregados, en el orden en que llegaron. */
    public List<MimeMessage> recibidos() {
        return List.copyOf(recibidos);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    private void atender() {
        while (!socket.isClosed()) {
            try (Socket cliente = socket.accept()) {
                cliente.setSoTimeout(5_000);
                conversar(cliente);
            } catch (SocketException cerrado) {
                return;
            } catch (IOException | MessagingException e) {
                // Una sesión con error no detiene al servidor; la prueba lo notará porque falta el correo.
            }
        }
    }

    private void conversar(Socket cliente) throws IOException, MessagingException {
        BufferedReader entrada = new BufferedReader(
                new InputStreamReader(cliente.getInputStream(), StandardCharsets.UTF_8));
        Writer salida = new OutputStreamWriter(cliente.getOutputStream(), StandardCharsets.UTF_8);
        responder(salida, "220 localhost SMTP de prueba");
        String linea;
        while ((linea = entrada.readLine()) != null) {
            String comando = linea.length() >= 4 ? linea.substring(0, 4).toUpperCase() : linea.toUpperCase();
            switch (comando) {
                case "DATA" -> {
                    responder(salida, "354 Termine con <CRLF>.<CRLF>");
                    recibidos.add(leerMensaje(entrada));
                    responder(salida, "250 Mensaje recibido");
                }
                case "QUIT" -> {
                    responder(salida, "221 Hasta luego");
                    return;
                }
                default -> responder(salida, "250 OK");
            }
        }
    }

    private static MimeMessage leerMensaje(BufferedReader entrada) throws IOException, MessagingException {
        StringBuilder crudo = new StringBuilder();
        String linea;
        while ((linea = entrada.readLine()) != null && !".".equals(linea)) {
            // RFC 5321 §4.5.2: el cliente duplica el punto inicial de cada línea.
            crudo.append(linea.startsWith("..") ? linea.substring(1) : linea).append("\r\n");
        }
        return new MimeMessage(Session.getInstance(new Properties()),
                new ByteArrayInputStream(crudo.toString().getBytes(StandardCharsets.UTF_8)));
    }

    private static void responder(Writer salida, String respuesta) throws IOException {
        salida.write(respuesta + "\r\n");
        salida.flush();
    }
}
