package com.test;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.util.FluxUtil;
import com.google.common.primitives.Bytes;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class SimpleHttpClientWithLogging implements HttpClient {
    List<Byte> byteBuffer = new ArrayList<>();

    private String readLine(InputStream inputStream) throws Exception {
        int i = 0;
        while (true) {
            for (; i + 1 < byteBuffer.size(); ++i) {
                if (byteBuffer.get(i) == '\r' && byteBuffer.get(i + 1) == '\n') {
                    String ret = new String(Bytes.toArray(byteBuffer.subList(0, i + 2)));
                    byteBuffer = byteBuffer.subList(i + 2, byteBuffer.size());
                    return ret;
                }
            }
            byte[] buffer = new byte[4096];
            int size = inputStream.read(buffer);
            if (size <= 0) {
                throw new IOException("Socket read failed");
            }
            for (int j = 0; j < size; ++j) {
                byteBuffer.add(buffer[j]);
            }
        }
    }

    @Override
    public Mono<HttpResponse> send(HttpRequest request) {
        Socket socket;
        OutputStream outputStream;
        PrintWriter writer;
        try {
            int port = request.getUrl().getPort();
            if (port < 0 || 65536 <= port) {
                port = request.getUrl().getDefaultPort();
            }
            if (request.getUrl().getProtocol().equalsIgnoreCase("http")) {
                socket = new Socket(request.getUrl().getHost(), port);
            } else {
                SocketFactory factory = SSLSocketFactory.getDefault();
                socket = factory.createSocket(request.getUrl().getHost(), port);
            }

            outputStream = socket.getOutputStream();
            writer = new PrintWriter(outputStream, true);
        } catch (Exception e) {
            return Mono.error(e);
        }
        return Flux.defer(() -> {
            try {
                String msg = String.format("%s %s HTTP/1.1\r\n", request.getHttpMethod(), request.getUrl().getFile());
                writer.print(msg);
                System.out.printf("< %s", msg);

                request.getHeaders().put("Host", request.getUrl().getHost());

                Mono<byte[]> body = Mono.empty();
                if (request.getBody() != null) {
                    body = FluxUtil.collectBytesInByteBufferStream(request.getBody());
                }

                return body.switchIfEmpty(Mono.defer(() -> {
                    request.getHeaders().forEach(httpHeader -> {
                        String msg1 = String.format("%s: %s\r\n", httpHeader.getName(), httpHeader.getValue());
                        writer.printf(msg1);
                        System.out.printf("< %s", msg1);
                    });
                    writer.write("\r\n");
                    writer.flush();
                    System.out.print("< \r\n");
                    return Mono.empty();
                })).flatMap(bytes -> {
                    try {
                        request.getHeaders().put("Content-Length", String.valueOf(bytes.length));

                        request.getHeaders().forEach(httpHeader -> {
                            String msg1 = String.format("%s: %s\r\n", httpHeader.getName(), httpHeader.getValue());
                            writer.printf(msg1);
                            System.out.printf("< %s", msg1);
                        });
                        writer.write("\r\n");
                        writer.flush();
                        outputStream.write(bytes);
                        outputStream.flush();

                        System.out.print("< \r\n");
                        System.out.printf("< %s\r\n", new String(bytes));

                        return Mono.empty();
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                });
            } catch (Exception e) {
                return Flux.error(e);
            }
        }).then(Mono.defer(() -> {
            try {
                InputStream inputStream = socket.getInputStream();
                String firstLine = readLine(inputStream);
                System.out.printf("> %s", firstLine);

                int statusCode = Integer.parseInt(firstLine.split(" ")[1]);
                HttpHeaders headers = new HttpHeaders();

                for (String line = readLine(inputStream); !line.trim().isEmpty(); line = readLine(inputStream)) {
                    System.out.printf("> %s", line);
                    String[] values = line.split(":");
                    headers.put(values[0].trim(), values[1].trim());
                }

                System.out.print("> \r\n");

                int length = Integer.parseInt(headers.getValue("Content-Length"));
                while (byteBuffer.size() < length) {
                    byte[] buffer = new byte[length - byteBuffer.size()];
                    int size = inputStream.read(buffer);
                    for (int i = 0; i < size; ++i) {
                        byteBuffer.add(buffer[i]);
                    }
                }

                byte[] body = Bytes.toArray(byteBuffer);
                System.out.printf("> %s\r\n", new String(body));
                socket.close();

                return Mono.just(new SimpleHttpResponse(request, statusCode, headers, Flux.just(ByteBuffer.wrap(body))));
            } catch (Exception e) {
                return Mono.error(e);
            }
        }));
    }

    private static class SimpleHttpResponse extends HttpResponse {
        private final int statusCode;
        private final HttpHeaders headers;
        private final Flux<ByteBuffer> body;

        SimpleHttpResponse(HttpRequest request, int statusCode, HttpHeaders headers, Flux<ByteBuffer> body) {
            super(request);
            this.statusCode = statusCode;
            this.headers = headers;
            this.body = body;
        }

        @Override
        public int getStatusCode() {
            return statusCode;
        }

        @Override
        public String getHeaderValue(String name) {
            return headers.getValue(name);
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        @Override
        public Flux<ByteBuffer> getBody() {
            return body;
        }

        @Override
        public Mono<byte[]> getBodyAsByteArray() {
            return FluxUtil.collectBytesInByteBufferStream(body);
        }

        @Override
        public Mono<String> getBodyAsString() {
            return getBodyAsByteArray().map(String::new);
        }

        @Override
        public Mono<String> getBodyAsString(Charset charset) {
            return getBodyAsByteArray().map(bytes -> new String(bytes, charset));
        }
    }
}
