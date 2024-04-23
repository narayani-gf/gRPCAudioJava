package grpcjavaaudio.servidor;

import java.io.IOException;
import java.io.InputStream;
import io.grpc.stub.StreamObserver;
import com.google.protobuf.ByteString;

import com.proto.audio.Audio.DataChunkResponse;
import com.proto.audio.Audio.DownloadFileRequest;
import com.proto.audio.AudioServiceGrpc;

public class ServidorImpl extends AudioServiceGrpc.AudioServiceImplBase {
    @Override
    public void downloadAudio(DownloadFileRequest request, StreamObserver<DataChunkResponse> responseObserver) {
        // Obtenemos el nombre del archivo que quiere el cliente
        String archivoNombre = "/" + request.getNombre();
        System.out.println("\n\nEnviando el archivo: " + request.getNombre());

        // Abrimos el archivo
        InputStream fileStream = ServidorImpl.class.getResourceAsStream(archivoNombre);

        // Establecemos una longitud de chunk
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        // Cuenta los bytes leídos para enviar al cliente
        int length;
        try {
            while ((length = fileStream.read(buffer, 0, bufferSize)) != -1) {
                // Se construye la respuesta a enviarle al cliente
                DataChunkResponse respuesta = DataChunkResponse.newBuilder()
                        .setData(ByteString.copyFrom(buffer, 0, length))
                        .build();

                        System.out.print(".");
                
                // En gRPC se utiliza onNext para enviar la respuesta
                responseObserver.onNext(respuesta);
            }

            // Cierra el stream
            fileStream.close();
        } catch (IOException e) {
            System.out.println("No se pudo obtener el archivo " + archivoNombre);
        }

        // Avisa que se ha terminado
        responseObserver.onCompleted();
    }
}
