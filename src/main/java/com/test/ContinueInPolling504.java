package com.test;

import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpPipelineCallContext;
import com.azure.core.http.HttpPipelineNextPolicy;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.util.FluxUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ContinueInPolling504 implements HttpPipelinePolicy {
    @Override
    public Mono<HttpResponse> process(HttpPipelineCallContext context, HttpPipelineNextPolicy next) {
        return next.process().map(httpResponse -> {
            if (httpResponse.getStatusCode() == 504) {
                URL url = httpResponse.getRequest().getUrl();
                if (url.toString().matches(".*/operations/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
                    String body = "{\"status\": \"InProgress\"}";
                    HttpHeaders headers = new HttpHeaders();
                    headers.put("Content-Type", "application/json; charset=utf-8");
                    headers.put("Content-Length", String.valueOf(body.length()));
                    return new SimpleHttpResponse(202, headers, Flux.just(ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8))), httpResponse.getRequest());
                }
            }
            return httpResponse;
        });
    }

    private static class SimpleHttpResponse extends HttpResponse {
        private int statusCode;
        private HttpHeaders headers;
        private Flux<ByteBuffer> body;

        SimpleHttpResponse(int statusCode, HttpHeaders headers, Flux<ByteBuffer> body, HttpRequest request) {
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
        public String getHeaderValue(String s) {
            return headers.getValue(s);
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
            return getBodyAsString(StandardCharsets.UTF_8);
        }

        @Override
        public Mono<String> getBodyAsString(Charset charset) {
            return getBodyAsByteArray().map(bytes -> new String(bytes, charset));
        }
    }
}
