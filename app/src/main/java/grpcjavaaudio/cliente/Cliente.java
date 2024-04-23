package grpcjavaaudio.cliente;

// Soporte archivo proto
import com.proto.audio.Audio.DownloadFileRequest;
import com.proto.audio.AudioServiceGrpc;
//Soporte para enviar stream gRPC
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
// Soporte para archivos
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
//Player WAV
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
// import javax.swing.JOptionPane;

// Player MP3
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

public class Cliente {
    public static void main(String[] args) {
        // Establece el servidor gRPC
        String host = "localhost";
        // Establece el puerto gRPC
        int puerto = 8080;
        // Guarda el nombre del archivo a solicitar
        String nombre;

        // Crea el canal de comunicación
        ManagedChannel ch = ManagedChannelBuilder.forAddress(host, puerto).usePlaintext().build();

        // Recibe el archivo WAV y lo reproducce mientras llega
        // nombre = "anyma.wav";
        // streamWav(ch, nombre, 48000F);

        // Descarga el archivo mp3 y lo reproduce después de descargarse
        nombre = "tiesto.mp3";
        ByteArrayInputStream archivoDescargado = downloadFile(ch, nombre);
        playMp3(archivoDescargado, nombre);

        // Terminamos la comunicación
        System.out.println("Apagando...");
        ch.shutdown();
    }

    // Descarga y reproduce al mismo tiempo
    public static void streamWav(ManagedChannel ch, String nombre, float sampleRate) {
        try {
            // AudioFormat (float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian)
            AudioFormat newFormat = new AudioFormat(sampleRate, 16, 2, true, false);
            SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(newFormat);
            sourceDataLine.open(newFormat);
            sourceDataLine.start();

            // Obtenemos la referencia al stub
            AudioServiceGrpc.AudioServiceBlockingStub stub = AudioServiceGrpc.newBlockingStub(ch);

            // Construimos la petición enviando un parámetro
            DownloadFileRequest peticion = DownloadFileRequest.newBuilder().setNombre(nombre).build();

            // Establecemos una longitud de chunk
            int bufferSize = 1024;
            System.out.println("Reproduciendo el archivo: " + nombre);

            // Usando el stub, realizamos la llamada streaming RPC
            stub.downloadAudio(peticion).forEachRemaining(respuesta -> {
                try {
                    sourceDataLine.write(respuesta.getData().toByteArray(), 0, bufferSize);
                    System.out.print(".");
                } catch (Exception e) {
                }
            });

            System.out.println("\nRecepcion de datos correcta.");
            System.out.println("Reproduccion terminada.\n\n");

            // Cerramos la linea
            sourceDataLine.drain();
            sourceDataLine.close();
        } catch (LineUnavailableException e) {
            System.out.print(e.getMessage());
        }
    }

    // Descarga el archivo (stream)
    public static ByteArrayInputStream downloadFile(ManagedChannel ch, String nombre) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        // Obtenemos la referencia al stub
        AudioServiceGrpc.AudioServiceBlockingStub stub = AudioServiceGrpc.newBlockingStub(ch);
        // Construimos la petición enviando un parámetro
        DownloadFileRequest peticion = DownloadFileRequest.newBuilder().setNombre(nombre).build();

        System.out.println("Recibiendo el archivo: " + nombre);
        // Usando el stub, realizamos la llamada streaming RPC
        stub.downloadAudio(peticion).forEachRemaining(respuesta -> {
            // Imprimimos la respuesta de RPC
            try {
                stream.write(respuesta.getData().toByteArray());
                System.out.print(".");
            } catch (IOException e) {
                System.out.println("No se pudo obtener el archivo de audio." + e.getMessage());
            }
        });

        // Se recibieron todos los datos
        System.out.println("\nRecepcion de datos correcta.\n\n");

        // Convierte el output stream a uno de entrada para su reproducción
        return new ByteArrayInputStream(stream.toByteArray());
    }

    // Reproduce un archivo WAV descargado
    public static void playWav(ByteArrayInputStream inStream, String nombre) {
        try {
            AudioInputStream audioInput = AudioSystem.getAudioInputStream(inStream);
            Clip clip = AudioSystem.getClip();
            clip.open(audioInput);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            System.out.println("Reproduciendo el archivo: " + nombre + "...\n\n");
            clip.start();

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {

            }

            clip.stop();
            // JOptionPane.showMessageDialog(null, "Terminar la reproduccion");
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    // Reproduce un archivo MP3 descargado
    public static void playMp3(ByteArrayInputStream inStream, String nombre) {
        try {
            System.out.println("Reproduciendo el archivo: " + nombre + "...\n\n");
            Player player = new Player(inStream);
            player.play();
        } catch (JavaLayerException e) {

        }
    }
}
