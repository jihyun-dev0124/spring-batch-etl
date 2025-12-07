package io.github.jihyundev.spring_batch_etl.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;


import java.time.Duration;

@Slf4j
@Configuration
public class ApiWebClientConfig {

    @Value("${external-api.base-url}")
    private String externalApiBaseUrl;

    @Value("${external-api.connect-timeout}")
    private int connectTimeout;

    @Value("${external-api.read-timeout}")
    private int readTimeout;



    @Bean
    public WebClient externalApiWebClient() {

        // Connection Pool 설정
        ConnectionProvider connectionProvider = ConnectionProvider.builder("api-connection-pool")
                .maxConnections(50)
                .pendingAcquireMaxCount(100)
                .pendingAcquireTimeout(Duration.ofSeconds(5))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .compress(true)
                .responseTimeout(Duration.ofMillis(readTimeout))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(readTimeout / 1000))
                        .addHandlerLast(new WriteTimeoutHandler(readTimeout / 1000))
                );

        //JSON Body 최대 크기 상향 (리스트 API 받을 경우 필수)
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) //10MB, 설정 안하면 DataBufferLimitException 위험
                .build();


        return WebClient.builder()
                //.baseUrl(externalApiBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies)
                .defaultHeaders(headers -> {
                    headers.add("Accept", "application/json");
                    headers.add("Content-Type", "application/json");
                })
                .filter((request, next) ->
                        next.exchange(request)
                                .doOnNext(response ->
                                        log.debug("[API RESPONSE] status={}, url={}",
                                                response.statusCode(),
                                                request.url()))
                )
                .build();
    }
}
