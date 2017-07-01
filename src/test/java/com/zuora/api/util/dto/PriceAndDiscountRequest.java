package com.zuora.api.util.dto;


public class PriceAndDiscountRequest {
    private String currency;
    private String[] ratePlans;

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String[] getRatePlans() {
        return ratePlans;
    }

    public void setRatePlans(String[] ratePlans) {
        this.ratePlans = ratePlans;
    }

    public static final class Builder {
        private String currency;
        private String[] ratePlans;

        private Builder() {
        }

        public static Builder aPriceAndDiscountRequest() {
            return new Builder();
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder ratePlans(String[] ratePlans) {
            this.ratePlans = ratePlans;
            return this;
        }

        public PriceAndDiscountRequest build() {
            PriceAndDiscountRequest priceAndDiscountRequest = new PriceAndDiscountRequest();
            priceAndDiscountRequest.setCurrency(currency);
            priceAndDiscountRequest.setRatePlans(ratePlans);
            return priceAndDiscountRequest;
        }
    }
}
