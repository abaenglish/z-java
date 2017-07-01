package com.zuora.api.util.dto;

public enum ChargeModelType {

    PER_UNIT_PRICING("Per Unit Pricing"),
    DISCOUNT_PERCENTAGE("Discount-Percentage");

    private String chargeModel;

    ChargeModelType(String chargeModel) {
        this.chargeModel = chargeModel;
    }

    public String getChargeModel() {
        return chargeModel;
    }

}
