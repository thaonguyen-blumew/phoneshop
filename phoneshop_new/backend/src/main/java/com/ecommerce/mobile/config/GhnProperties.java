package com.ecommerce.mobile.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ghn")
public class GhnProperties {

    private String apiBaseUrl;
    private String token;
    private String shopId;
    private String fromName;
    private String fromPhone;
    private String fromAddress;
    private String fromWardName;
    private String fromDistrictName;
    private String fromProvinceName;
    private String returnPhone;
    private String returnAddress;
    private String returnWardName;
    private String returnDistrictName;
    private String returnProvinceName;
    private Integer paymentTypeId = 2;
    private String requiredNote = "KHONGCHOXEMHANG";
    private Integer serviceTypeId = 2;
    private Integer weight = 200;
    private Integer length = 15;
    private Integer width = 15;
    private Integer height = 15;
    private Integer insuranceValue = 0;

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getFromPhone() {
        return fromPhone;
    }

    public void setFromPhone(String fromPhone) {
        this.fromPhone = fromPhone;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getFromWardName() {
        return fromWardName;
    }

    public void setFromWardName(String fromWardName) {
        this.fromWardName = fromWardName;
    }

    public String getFromDistrictName() {
        return fromDistrictName;
    }

    public void setFromDistrictName(String fromDistrictName) {
        this.fromDistrictName = fromDistrictName;
    }

    public String getFromProvinceName() {
        return fromProvinceName;
    }

    public void setFromProvinceName(String fromProvinceName) {
        this.fromProvinceName = fromProvinceName;
    }

    public String getReturnPhone() {
        return returnPhone;
    }

    public void setReturnPhone(String returnPhone) {
        this.returnPhone = returnPhone;
    }

    public String getReturnAddress() {
        return returnAddress;
    }

    public void setReturnAddress(String returnAddress) {
        this.returnAddress = returnAddress;
    }

    public String getReturnWardName() {
        return returnWardName;
    }

    public void setReturnWardName(String returnWardName) {
        this.returnWardName = returnWardName;
    }

    public String getReturnDistrictName() {
        return returnDistrictName;
    }

    public void setReturnDistrictName(String returnDistrictName) {
        this.returnDistrictName = returnDistrictName;
    }

    public String getReturnProvinceName() {
        return returnProvinceName;
    }

    public void setReturnProvinceName(String returnProvinceName) {
        this.returnProvinceName = returnProvinceName;
    }

    public Integer getPaymentTypeId() {
        return paymentTypeId;
    }

    public void setPaymentTypeId(Integer paymentTypeId) {
        this.paymentTypeId = paymentTypeId;
    }

    public String getRequiredNote() {
        return requiredNote;
    }

    public void setRequiredNote(String requiredNote) {
        this.requiredNote = requiredNote;
    }

    public Integer getServiceTypeId() {
        return serviceTypeId;
    }

    public void setServiceTypeId(Integer serviceTypeId) {
        this.serviceTypeId = serviceTypeId;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getInsuranceValue() {
        return insuranceValue;
    }

    public void setInsuranceValue(Integer insuranceValue) {
        this.insuranceValue = insuranceValue;
    }
}
