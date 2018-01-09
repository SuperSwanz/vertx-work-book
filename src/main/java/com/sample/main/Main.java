package com.sample.main;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;

public class Main {
	private static HttpClient httpClient;

	public static void main(String[] args) {
		System.out.println("starting up...");

		System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
		Vertx vertx = Vertx.vertx();

		HttpServer server = vertx.createHttpServer();
		httpClient = buildHttpClient(vertx);
		server.requestHandler(httpServerRequest -> {
			switch (httpServerRequest.path()) {
			case "/api": {
				execute().setHandler(response -> {
					HttpClientResponse httpClientResponse = response.result();
					if (httpClientResponse.statusCode() == 200) {
						httpClientResponse.bodyHandler(buffer -> {
							HttpServerResponse resp = httpServerRequest.response();
							resp.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
							resp.end(new JsonObject().put("success", buffer.toJsonObject()).encode());
						});
					}
				});
				break;
			}
			case "/error": {
				HttpServerResponse response = httpServerRequest.response();
				response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
				response.setStatusCode(500);
				response.end(new JsonObject().put("error", "Error handler").encode());
				break;
			}
			default: {
				HttpServerResponse response = httpServerRequest.response();
				response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
				response.setStatusCode(404);
				response.end(new JsonObject().put("error", "Unknown path").encode());
				break;
			}
			}
		});

		int port = 9090;
		server.listen(port);
		System.out.println(String.format("Listening on port %d", port));
		// Main exits. Server runs indefinitely.
	}

	public static Future<HttpClientResponse> execute() {
		Future<HttpClientResponse> future = Future.future();
		HttpClientRequest request = httpClient
				.requestAbs(HttpMethod.GET, "http://localhost:8080/api/sample/api", response -> {
					future.complete(response);
				}).exceptionHandler(throwable -> {
					future.fail(throwable);
				});
		request.headers().add("Connection", "close");
		request.end();
		return future;
	}

	public static HttpClient buildHttpClient(Vertx vertx) {
		HttpClientOptions options = new HttpClientOptions();
		options.setTryUseCompression(true);
		options.setKeepAlive(true);
		options.setMaxPoolSize(50);
		return vertx.createHttpClient(options);
	}
}
