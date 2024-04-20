package masterMind;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MastermindCliente {
    private static volatile boolean juegoTerminado = false;

    public static void main(String[] args) {
        final boolean[] idRecibido = {false}; // Nuevo mecanismo de señalización para el ID

        try (BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.print("Ingresa la dirección IP del servidor: ");
            String direccionServidor = teclado.readLine();
            System.out.print("Ingresa el puerto de conexión: ");
            int puerto = Integer.parseInt(teclado.readLine());

            try (Socket socket = new Socket(direccionServidor, puerto);
                 BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter output = new PrintWriter(socket.getOutputStream(), true)) {

                // Hilo para escuchar mensajes del servidor
                Thread escucharServidor = new Thread(() -> {
                    try {
                        String mensaje;
                        while ((mensaje = input.readLine()) != null) {
                            if (mensaje.startsWith("Tu ID es: ")) {
                                idRecibido[0] = true; // Señaliza que el ID ha sido recibido
                            }
                            System.out.println("\nRespuesta: " + mensaje);
                            if (mensaje.contains("Fin del juego") || mensaje.contains("¡Correcto! La combinación es")) {
                                juegoTerminado = true;
                                break;
                            }
                        }
                    } catch (IOException e) {
                        if (!juegoTerminado) {
                            System.out.println("Error al leer del servidor: " + e.getMessage());
                        }
                    }
                    System.out.println("Cerrando cliente.");
                    System.exit(0); // Cierra la aplicación cliente
                });
                escucharServidor.start();

                // Espera activa hasta que el ID ha sido recibido
                while (!idRecibido[0]) {
                    try {
                        Thread.sleep(100); // Espera un breve momento para evitar una espera activa intensa
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // Bucle para enviar intentos al servidor
                while (!juegoTerminado) {
                    System.out.print("Ingresa tu intento: ");
                    String intento = teclado.readLine();
                    output.println(intento);
                }
            } catch (IOException e) {
                System.out.println("Error al conectar con el servidor: " + e.getMessage());
            }
        } catch (IOException e) {
            System.out.println("Error al leer desde el teclado: " + e.getMessage());
        }
    }
}
