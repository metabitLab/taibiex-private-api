package com.metabitlab.taibiex.privateapi.entity;

import com.metabitlab.taibiex.privateapi.graphqlapi.codegen.types.*;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "token")
public class TokenEntity extends BaseEntity{

    private String chain;

    private String address;

    private TokenStandard standard;

    private Integer decimals;

    private String name;

    private String symbol;

    /*@ManyToOne
    private TokenProjectEntity project;*/

    /*@OneToOne
    private TokenMarketEntity market;*/

    private String sellFeeBps;

    private String buyFeeBps;

    //private List<PoolTransaction> v3Transactions;

    //private List<PoolTransaction> v2Transactions;


    public String getChain() {
        return chain;
    }

    public void setChain(String chain) {
        this.chain = chain;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public TokenStandard getStandard() {
        return standard;
    }

    public void setStandard(TokenStandard standard) {
        this.standard = standard;
    }

    public Integer getDecimals() {
        return decimals;
    }

    public void setDecimals(Integer decimals) {
        this.decimals = decimals;
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

    public String getSellFeeBps() {
        return sellFeeBps;
    }

    public void setSellFeeBps(String sellFeeBps) {
        this.sellFeeBps = sellFeeBps;
    }

    public String getBuyFeeBps() {
        return buyFeeBps;
    }

    public void setBuyFeeBps(String buyFeeBps) {
        this.buyFeeBps = buyFeeBps;
    }
}
