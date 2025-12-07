package io.github.jihyundev.spring_batch_etl.api;

import io.github.jihyundev.spring_batch_etl.api.dto.member.ApiMallMemberDto;
import io.github.jihyundev.spring_batch_etl.api.dto.member.ApiMallMembersResponse;
import io.github.jihyundev.spring_batch_etl.api.dto.member.ApiMallMemberCountResponse;
import io.github.jihyundev.spring_batch_etl.api.dto.sales.ApiSalesDto;
import io.github.jihyundev.spring_batch_etl.api.dto.sales.ApiSalesResponse;
import io.github.jihyundev.spring_batch_etl.domain.mall.MallConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@Component
@Profile("live") //실제 운영에서 사용
@RequiredArgsConstructor
public class MallApiClientImpl implements MallApiClient {
    @Qualifier("externalApiWebClient")
    private final WebClient externalApiWebClient;

    @Value("${external-api.path.member-list}")
    private String memberListPath;

    @Value("${external-api.path.member-count}")
    private String memberCountPath;

    @Value("${external-api.path.sales-list}")
    private String salesListPath;


    /**
     * 회원 수 api
     * @param mallConfig
     * @param startDate
     * @param endDate
     * @return
     */
    @Override
    public int getMemberCount(MallConfig mallConfig, LocalDateTime startDate, LocalDateTime endDate) {
        String memberCountApiUrl = mallConfig.getApiUrl() + memberCountPath;
        URI url = URI.create(memberCountApiUrl);

        log.debug("[MallApiClientImpl#getMemberCount] request members mall_id={}, startDate={}, endDate={}",
                mallConfig.getMallId(), startDate, endDate);

        try {
            return externalApiWebClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.scheme(url.getScheme())
                                .host(url.getHost())
                                .port(url.getPort())
                                .path(url.getPath())
                                .queryParam("mallId", mallConfig.getMallId())
                                .queryParam("startDate", formatDate(startDate, "yyyy-MM-dd HH:mm:ss"))
                                .queryParam("endDate", formatDate(endDate, "yyyy-MM-dd HH:mm:ss"));
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        log.error("[MallApiClientImpl#getMemberCount] 4xx error status={} url{}",
                                clientResponse.statusCode(), memberCountPath);
                        return clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new RuntimeException("[MallApiClientImpl#getMemberCount] 4xx error : " + clientResponse.statusCode() + " body=" + body)));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
                        log.error("[MallApiClientImpl#getMemberCount] 5xx error status={} url{}",
                                clientResponse.statusCode(), memberCountPath);
                        return clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new RuntimeException("[MallApiClientImpl#getMemberCount] 5xx error : " + clientResponse.statusCode() + " body=" + body)));
                    })
                    .bodyToMono(ApiMallMemberCountResponse.class)
                    .timeout(Duration.ofSeconds(5)) //개별 호출 타임아웃
                    // .retryWhen(Retry.backoff(3, Duration.ofMillis(200)))
                    .blockOptional()
                    .orElseGet(() -> {
                        log.warn("[MallApiClientImpl#getMemberCount] empty response for startDate = {}, endDate = {}", startDate, endDate);
                        return new ApiMallMemberCountResponse();
                    })
                    .getCount();
        } catch (WebClientResponseException e) {
            log.error("[MallApiClientImpl#getMemberCount] status={} body{}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw e; // 배치에서 잡아서 중단/재시도 정책 정하면 됨.
        } catch (Exception e) {
            log.error("[MallApiClientImpl#getMemberCount] unexpected error while calling member API", e);
            throw new RuntimeException("Failed to call member API", e);
        }
    }

    /**
     * 회원 list api
     * @param mallConfig
     * @param offset
     * @param limit
     * @param startDate
     * @param endDate
     * @return
     */
    @Override
    public List<ApiMallMemberDto> getMembers(MallConfig mallConfig, int offset, int limit, LocalDateTime startDate, LocalDateTime endDate) {
        String memberListApiUrl = mallConfig.getApiUrl() + memberListPath;
        URI url = URI.create(memberListApiUrl);

        log.debug("[MallApiClientImpl#getMembers] request members mall_id={}, offset={}, limit={}, startDate={}, endDate={}",
                mallConfig.getMallId(), offset, limit, startDate, endDate);

        try{
            return externalApiWebClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.scheme(url.getScheme())
                                .host(url.getHost())
                                .port(url.getPort())
                                .path(url.getPath())
                                .queryParam("mallId", mallConfig.getMallId())
                                .queryParam("offset", offset)
                                .queryParam("limit", limit)
                                .queryParam("startDate", formatDate(startDate, "yyyy-MM-dd HH:mm:ss"))
                                .queryParam("endDate", formatDate(endDate, "yyyy-MM-dd HH:mm:ss"));
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        log.error("[MallApiClientImpl#getMembers] 4xx error status={} url{}",
                                clientResponse.statusCode(), memberCountPath);
                        return clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new RuntimeException("[MallApiClientImpl#getMembers] 4xx error : " + clientResponse.statusCode() + " body=" + body)));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
                        log.error("[MallApiClientImpl#getMembers] 5xx error status={} url{}",
                                clientResponse.statusCode(), memberCountPath);
                        return clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new RuntimeException("[MallApiClientImpl#getMembers] 5xx error : " + clientResponse.statusCode() + " body=" + body)));
                    })
                    .bodyToMono(ApiMallMembersResponse.class)
                    .blockOptional()
                    .orElseGet(() -> {
                        log.warn("[MallApiClientImpl#getMembers] empty response for offset={}, limit={}, startDate = {}, endDate = {}", offset, limit, startDate, endDate);
                        return null;
                    })
                    .getMembers();
        } catch (WebClientResponseException e) {
            log.error("[MallApiClientImpl#getMembers] status={} body{}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw e; // 배치에서 잡아서 중단/재시도 정책 정하면 됨.
        } catch (Exception e) {
            log.error("[MallApiClientImpl#getMembers] unexpected error while calling member API", e);
            throw new RuntimeException("Failed to call member API", e);
        }
    }

    @Override
    public List<ApiSalesDto> getSales(MallConfig mallConfig, int offset, int limit, LocalDateTime startDate, LocalDateTime endDate) {
        String salesApiUrl = mallConfig.getApiUrl() + salesListPath;
        URI url = URI.create(salesApiUrl);

        log.debug("[MallApiClientImpl#getSales] request members mall_id={}, offset={}, limit={}, startDate={}, endDate={}",
                mallConfig.getMallId(), offset, limit, startDate, endDate);

        try{
            return externalApiWebClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.scheme(url.getScheme())
                                .host(url.getHost())
                                .port(url.getPort())
                                .path(url.getPath())
                                .queryParam("offset", offset)
                                .queryParam("limit", limit)
                                .queryParam("startDate", formatDate(startDate, "yyyy-MM-dd HH:mm:ss"))
                                .queryParam("endDate", formatDate(endDate, "yyyy-MM-dd HH:mm:ss"));
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        log.error("[MallApiClientImpl#getSales] 4xx error status={} url{}", clientResponse.statusCode(), salesListPath);
                        return clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new RuntimeException("[MallApiClientImpl#getSales] 4xx error : " + clientResponse.statusCode() + " body=" + body)));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
                        log.error("[MallApiClientImpl#getSales] 5xx error status={} url{}", clientResponse.statusCode(), salesListPath);
                        return clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new RuntimeException("[MallApiClientImpl#getSales] 5xx error : " + clientResponse.statusCode() + " body=" + body)));
                    })
                    .bodyToMono(ApiSalesResponse.class)
                    .blockOptional()
                    .orElseGet(() -> {
                        log.warn("[MallApiClientImpl#getSales] empty response for offset={}, limit={}, startDate = {}, endDate = {}", offset, limit, startDate, endDate);
                        return null;
                    })
                    .getSalesList();
        }catch (WebClientResponseException e) {
            log.error("[MallApiClientImpl#getSales] status={} body{}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw e; // 배치에서 잡아서 중단/재시도 정책 정하면 됨.
        } catch (Exception e) {
            log.error("[MallApiClientImpl#getSales] unexpected error while calling sales API", e);
            throw new RuntimeException("Failed to call sales API", e);
        }

    }

    /**
     * 날짜 변환
     * @param date
     * @param pattern
     * @return
     */
    private String formatDate(LocalDateTime date, String pattern) {
        String v = null;
        if (date != null) {
            try {
                v = date.format(DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException e) {
                log.error(e.getMessage(), e);
            }
        }
        return v;
    }
}
