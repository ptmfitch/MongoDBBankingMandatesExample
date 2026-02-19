package com.mongodb.mandate.model;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

public class Creditor {

    @BsonId
    private ObjectId id;

    @BsonProperty("creditorId")
    private String creditorId;

    @BsonProperty("creditorName")
    private String creditorName;

    @BsonProperty("accountNumber")
    private String accountNumber;

    @BsonProperty("sortCode")
    private String sortCode;

    @BsonProperty("iban")
    private String iban;

    @BsonProperty("bic")
    private String bic;

    @BsonProperty("createdAt")
    private LocalDateTime createdAt;

    @BsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    public Creditor() {}

    // Getters
    public ObjectId getId() { return id; }
    public String getCreditorId() { return creditorId; }
    public String getCreditorName() { return creditorName; }
    public String getAccountNumber() { return accountNumber; }
    public String getSortCode() { return sortCode; }
    public String getIban() { return iban; }
    public String getBic() { return bic; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(ObjectId id) { this.id = id; }
    public void setCreditorId(String creditorId) { this.creditorId = creditorId; }
    public void setCreditorName(String creditorName) { this.creditorName = creditorName; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public void setSortCode(String sortCode) { this.sortCode = sortCode; }
    public void setIban(String iban) { this.iban = iban; }
    public void setBic(String bic) { this.bic = bic; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final Creditor c = new Creditor();

        public Builder id(ObjectId v) { c.id = v; return this; }
        public Builder creditorId(String v) { c.creditorId = v; return this; }
        public Builder creditorName(String v) { c.creditorName = v; return this; }
        public Builder accountNumber(String v) { c.accountNumber = v; return this; }
        public Builder sortCode(String v) { c.sortCode = v; return this; }
        public Builder iban(String v) { c.iban = v; return this; }
        public Builder bic(String v) { c.bic = v; return this; }
        public Builder createdAt(LocalDateTime v) { c.createdAt = v; return this; }
        public Builder updatedAt(LocalDateTime v) { c.updatedAt = v; return this; }

        public Creditor build() { return c; }
    }
}
