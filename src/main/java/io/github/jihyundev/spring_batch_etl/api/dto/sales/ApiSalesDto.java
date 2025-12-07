package io.github.jihyundev.spring_batch_etl.api.dto.sales;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ApiSalesDto {

    @JsonProperty("collection_date")
    private String collectionDate;

    @JsonProperty("collection_hour")
    private String collectionHour;

    @JsonProperty("order_count")
    private int orderCount;

    @JsonProperty("item_count")
    private int itemCount;

    @JsonProperty("order_price_amount")
    private String orderPriceAmount;     // API 값이 문자열 → String으로 받기

    @JsonProperty("shipping_fee")
    private String shippingFee;

    @JsonProperty("order_sale_price")
    private String orderSalePrice;

    @JsonProperty("coupon_discount_price")
    private String couponDiscountPrice;

    @JsonProperty("actual_order_amount")
    private String actualOrderAmount;

    @JsonProperty("refund_amount")
    private String refundAmount;
    private String sales;

    @JsonProperty("used_points")
    private String usedPoints;

    @JsonProperty("used_credits")
    private String usedCredits;

    @JsonProperty("used_naver_points")
    private String usedNaverPoints;

    @JsonProperty("used_naver_cash")
    private String usedNaverCash;

    @JsonProperty("refund_points")
    private String refundPoints;

    @JsonProperty("refund_credits")
    private String refundCredits;

    @JsonProperty("refund_naver_points")
    private String refundNaverPoints;

    @JsonProperty("refund_naver_cash")
    private String refundNaverCash;
}
