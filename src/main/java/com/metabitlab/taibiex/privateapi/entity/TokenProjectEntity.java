package com.metabitlab.taibiex.privateapi.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "token_project")
public class TokenProjectEntity extends BaseEntity{

    private String name;

    private String symbol;

    private String address;

    private String description;

    //private DescriptionTranslations descriptionTranslations;

    private String twitterName;

    private String homepageUrl;

    private String logoUrl;

    private Double logoWidth;

    private Double logoHeight;

    private Boolean isSpam;

    private Integer spamCode;

    @Column(name = "safety_level")
    private String safetyLevel;

    /*@OneToMany
    private List<TokenProjectMarketEntity> markets;*/

    public TokenProjectEntity() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSafetyLevel() {
        return safetyLevel;
    }

    public void setSafetyLevel(String safetyLevel) {
        this.safetyLevel = safetyLevel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTwitterName() {
        return twitterName;
    }

    public void setTwitterName(String twitterName) {
        this.twitterName = twitterName;
    }

    public String getHomepageUrl() {
        return homepageUrl;
    }

    public void setHomepageUrl(String homepageUrl) {
        this.homepageUrl = homepageUrl;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public Double getLogoWidth() {
        return logoWidth;
    }

    public void setLogoWidth(Double logoWidth) {
        this.logoWidth = logoWidth;
    }

    public Double getLogoHeight() {
        return logoHeight;
    }

    public void setLogoHeight(Double logoHeight) {
        this.logoHeight = logoHeight;
    }

    public Boolean getSpam() {
        return isSpam;
    }

    public void setSpam(Boolean spam) {
        isSpam = spam;
    }

    public Integer getSpamCode() {
        return spamCode;
    }

    public void setSpamCode(Integer spamCode) {
        this.spamCode = spamCode;
    }


}
