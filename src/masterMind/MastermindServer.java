package masterMind;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MastermindServer {
    private static final int PUERTO = 12345;
    private static String combinacionGanadora;
    private static Map<Integer, Socket> clientes = Collections.synchronizedMap(new HashMap<>());
    private static ExecutorService pool = Executors.newCachedThreadPool();
    private static AtomicBoolean partidaTerminada = new AtomicBoolean(false);
    private static ServerSocket serverSocket;
    private static int idCliente = 1;

    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(PUERTO);
            System.out.println("Servidor iniciado...");
            generarCombinacionGanadora();
            System.out.println("Combinación ganadora: " + combinacionGanadora);

            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    synchronized (partidaTerminada) {
                        if (partidaTerminada.get()) {
                            System.out.println("Juego ha terminado, rechazando nuevas conexiones.");
                            break;
                        }
                        clientes.put(idCliente, socket);
                        System.out.println("Cliente conectado: " + socket.getInetAddress() + ", ID: " + idCliente);
                        pool.execute(new ManejadorCliente(socket, idCliente++));
                    }
                } catch (IOException e) {
                    if (partidaTerminada.get()) {
                        System.out.println("Servidor cerrando después del juego.");
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error al iniciar el servidor: " + e.getMessage());
        } finally {
            cerrarServidor();
            pool.shutdown();
        }
    }

    private static void generarCombinacionGanadora() {
        List<Integer> numeros = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        Collections.shuffle(numeros);
        combinacionGanadora = numeros.subList(0, 4).stream()
                .map(String::valueOf)
                .reduce("", (a, b) -> a + b);
    }

    private static void avisarGanadorYCerrar(int idGanador) {
        partidaTerminada.set(true);
        String mensajeFin = "El ganador es el cliente " + idGanador + ". Fin del juego.";
        clientes.forEach((id, socket) -> {
            try {
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                output.println(mensajeFin);
                Thread.sleep(100); // Da tiempo para que el mensaje sea enviado
            } catch (IOException | InterruptedException e) {
                System.out.println("Error al enviar el mensaje de fin del juego: " + e.getMessage());
            }
        });
        cerrarServidor();
    }

    private static void cerrarServidor() {
        try {
            clientes.values().forEach(socket -> {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error al cerrar socket del cliente: " + e.getMessage());
                }
            });
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            System.out.println("Servidor cerrado.");
        } catch (IOException e) {
            System.out.println("Error al cerrar el servidor: " + e.getMessage());
        }
    }

    private static class ManejadorCliente implements Runnable {
        private Socket socket;
        private int idCliente;

        public ManejadorCliente(Socket socket, int idCliente) {
            this.socket = socket;
            this.idCliente = idCliente;
        }

        @Override
        public void run() {
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true);

                output.println("Tu ID es: " + idCliente);

                String intento;
                while ((intento = input.readLine()) != null) {
                    if (intento.equals(combinacionGanadora)) {
                        output.println("¡Correcto! La combinación es " + combinacionGanadora);
                        avisarGanadorYCerrar(idCliente);
                        break;
                    } else {
                        String pista = generarPista(intento);
                        output.println(pista);
                        System.out.println("Cliente " + idCliente + " intento: " + intento + " pista: " + pista);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error con el cliente " + idCliente + ": " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error al cerrar la conexión con el cliente: " + e.getMessage());
                }
            }
        }

        private String generarPista(String intento) {
            StringBuilder pista = new StringBuilder();
            // Contadores para los dígitos correctos en posición correcta e incorrecta.
            int posicionesCorrectas = 0, coincidencias = 0;

            // Marcar los caracteres ya evaluados para evitar contarlos dos veces.
            boolean[] combinacionMarcada = new boolean[4];
            boolean[] intentoMarcado = new boolean[4];

            // Primero, identificar los dígitos correctos en la posición correcta.
            for (int i = 0; i < 4; i++) {
                if (intento.charAt(i) == combinacionGanadora.charAt(i)) {
                    posicionesCorrectas++;
                    combinacionMarcada[i] = true;
                    intentoMarcado[i] = true;
                }
            }

            // Luego, identificar los dígitos correctos en posición incorrecta.
            for (int i = 0; i < 4; i++) {
                if (!intentoMarcado[i]) {
                    for (int j = 0; j < 4; j++) {
                        if (!combinacionMarcada[j] && intento.charAt(i) == combinacionGanadora.charAt(j)) {
                            coincidencias++;
                            combinacionMarcada[j] = true; // Marcar para evitar contar este dígito nuevamente.
                            break;
                        }
                    }
                }
            }

            // Construir la pista con los caracteres correspondientes.
            for (int i = 0; i < posicionesCorrectas; i++) {
                pista.append("█");
            }
            for (int i = 0; i < coincidencias; i++) {
                pista.append("▒");
            }

            return pista.toString();
        }

    }
}
