package com.mongodb.mandate.model;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DirectDebitMandate {

    @BsonId
    private ObjectId id;

    @BsonProperty("mandateId")
    private String mandateId;

    @BsonProperty("lastUpdateDate")
    private LocalDateTime lastUpdateDate;

    // References to other collections
    @BsonProperty("creditorId")
    private String creditorId;

    @BsonProperty("debtorId")
    private String debtorId;

    // Mandate Details
    @BsonProperty("mandateReference")
    private String mandateReference;

    @BsonProperty("mandateType")
    private String mandateType;

    @BsonProperty("frequency")
    private String frequency;

    @BsonProperty("status")
    private String status;

    @BsonProperty("signatureDate")
    private LocalDate signatureDate;

    @BsonProperty("effectiveDate")
    private LocalDate effectiveDate;

    @BsonProperty("expiryDate")
    private LocalDate expiryDate;

    // Limits
    @BsonProperty("maxAmountPerTransaction")
    private BigDecimal maxAmountPerTransaction;

    @BsonProperty("maxAmountPerMonth")
    private BigDecimal maxAmountPerMonth;

    @BsonProperty("maxTransactionsPerMonth")
    private Integer maxTransactionsPerMonth;

    @BsonProperty("currency")
    private String currency;

    @BsonProperty("description")
    private String description;

    @BsonProperty("schemeType")
    private String schemeType;

    @BsonProperty("createdAt")
    private LocalDateTime createdAt;

    @BsonProperty("version")
    private Integer version;

    public DirectDebitMandate() {}

    // Getters
    public ObjectId getId() { return id; }
    public String getMandateId() { return mandateId; }
    public LocalDateTime getLastUpdateDate() { return lastUpdateDate; }
    public String getCreditorId() { return creditorId; }
    public String getDebtorId() { return debtorId; }
    public String getMandateReference() { return mandateReference; }
    public String getMandateType() { return mandateType; }
    public String getFrequency() { return frequency; }
    public String getStatus() { return status; }
    public LocalDate getSignatureDate() { return signatureDate; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public BigDecimal getMaxAmountPerTransaction() { return maxAmountPerTransaction; }
    public BigDecimal getMaxAmountPerMonth() { return maxAmountPerMonth; }
    public Integer getMaxTransactionsPerMonth() { return maxTransactionsPerMonth; }
    public String getCurrency() { return currency; }
    public String getDescription() { return description; }
    public String getSchemeType() { return schemeType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Integer getVersion() { return version; }

    // Setters
    public void setId(ObjectId id) { this.id = id; }
    public void setMandateId(String mandateId) { this.mandateId = mandateId; }
    public void setLastUpdateDate(LocalDateTime lastUpdateDate) { this.lastUpdateDate = lastUpdateDate; }
    public void setCreditorId(String creditorId) { this.creditorId = creditorId; }
    public void setDebtorId(String debtorId) { this.debtorId = debtorId; }
    public void setMandateReference(String mandateReference) { this.mandateReference = mandateReference; }
    public void setMandateType(String mandateType) { this.mandateType = mandateType; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public void setStatus(String status) { this.status = status; }
    public void setSignatureDate(LocalDate signatureDate) { this.signatureDate = signatureDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public void setMaxAmountPerTransaction(BigDecimal maxAmountPerTransaction) { this.maxAmountPerTransaction = maxAmountPerTransaction; }
    public void setMaxAmountPerMonth(BigDecimal maxAmountPerMonth) { this.maxAmountPerMonth = maxAmountPerMonth; }
    public void setMaxTransactionsPerMonth(Integer maxTransactionsPerMonth) { this.maxTransactionsPerMonth = maxTransactionsPerMonth; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setDescription(String description) { this.description = description; }
    public void setSchemeType(String schemeType) { this.schemeType = schemeType; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setVersion(Integer version) { this.version = version; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final DirectDebitMandate m = new DirectDebitMandate();

        public Builder id(ObjectId v) { m.id = v; return this; }
        public Builder mandateId(String v) { m.mandateId = v; return this; }
        public Builder lastUpdateDate(LocalDateTime v) { m.lastUpdateDate = v; return this; }
        public Builder creditorId(String v) { m.creditorId = v; return this; }
        public Builder debtorId(String v) { m.debtorId = v; return this; }
        public Builder mandateReference(String v) { m.mandateReference = v; return this; }
        public Builder mandateType(String v) { m.mandateType = v; return this; }
        public Builder frequency(String v) { m.frequency = v; return this; }
        public Builder status(String v) { m.status = v; return this; }
        public Builder signatureDate(LocalDate v) { m.signatureDate = v; return this; }
        public Builder effectiveDate(LocalDate v) { m.effectiveDate = v; return this; }
        public Builder expiryDate(LocalDate v) { m.expiryDate = v; return this; }
        public Builder maxAmountPerTransaction(BigDecimal v) { m.maxAmountPerTransaction = v; return this; }
        public Builder maxAmountPerMonth(BigDecimal v) { m.maxAmountPerMonth = v; return this; }
        public Builder maxTransactionsPerMonth(Integer v) { m.maxTransactionsPerMonth = v; return this; }
        public Builder currency(String v) { m.currency = v; return this; }
        public Builder description(String v) { m.description = v; return this; }
        public Builder schemeType(String v) { m.schemeType = v; return this; }
        public Builder createdAt(LocalDateTime v) { m.createdAt = v; return this; }
        public Builder version(Integer v) { m.version = v; return this; }

        public DirectDebitMandate build() { return m; }
    }
}
