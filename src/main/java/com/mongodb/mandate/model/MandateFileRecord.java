package com.mongodb.mandate.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class MandateFileRecord {

    // Mandate fields
    private String mandateId;
    private LocalDateTime lastUpdateDate;
    private String mandateReference;
    private String mandateType;
    private String frequency;
    private String status;
    private LocalDate signatureDate;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private BigDecimal maxAmountPerTransaction;
    private BigDecimal maxAmountPerMonth;
    private Integer maxTransactionsPerMonth;
    private String currency;
    private String description;
    private String schemeType;

    // Creditor fields (from file)
    private String creditorId;
    private String creditorName;
    private String creditorAccountNumber;
    private String creditorSortCode;
    private String creditorIban;
    private String creditorBic;

    // Debtor fields (from file)
    private String debtorName;
    private String debtorAccountNumber;
    private String debtorSortCode;
    private String debtorIban;
    private String debtorBic;
    private String debtorEmail;
    private String debtorPhone;

    public MandateFileRecord() {}

    // Getters
    public String getMandateId() { return mandateId; }
    public LocalDateTime getLastUpdateDate() { return lastUpdateDate; }
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
    public String getCreditorId() { return creditorId; }
    public String getCreditorName() { return creditorName; }
    public String getCreditorAccountNumber() { return creditorAccountNumber; }
    public String getCreditorSortCode() { return creditorSortCode; }
    public String getCreditorIban() { return creditorIban; }
    public String getCreditorBic() { return creditorBic; }
    public String getDebtorName() { return debtorName; }
    public String getDebtorAccountNumber() { return debtorAccountNumber; }
    public String getDebtorSortCode() { return debtorSortCode; }
    public String getDebtorIban() { return debtorIban; }
    public String getDebtorBic() { return debtorBic; }
    public String getDebtorEmail() { return debtorEmail; }
    public String getDebtorPhone() { return debtorPhone; }

    // Setters
    public void setMandateId(String mandateId) { this.mandateId = mandateId; }
    public void setLastUpdateDate(LocalDateTime lastUpdateDate) { this.lastUpdateDate = lastUpdateDate; }
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
    public void setCreditorId(String creditorId) { this.creditorId = creditorId; }
    public void setCreditorName(String creditorName) { this.creditorName = creditorName; }
    public void setCreditorAccountNumber(String creditorAccountNumber) { this.creditorAccountNumber = creditorAccountNumber; }
    public void setCreditorSortCode(String creditorSortCode) { this.creditorSortCode = creditorSortCode; }
    public void setCreditorIban(String creditorIban) { this.creditorIban = creditorIban; }
    public void setCreditorBic(String creditorBic) { this.creditorBic = creditorBic; }
    public void setDebtorName(String debtorName) { this.debtorName = debtorName; }
    public void setDebtorAccountNumber(String debtorAccountNumber) { this.debtorAccountNumber = debtorAccountNumber; }
    public void setDebtorSortCode(String debtorSortCode) { this.debtorSortCode = debtorSortCode; }
    public void setDebtorIban(String debtorIban) { this.debtorIban = debtorIban; }
    public void setDebtorBic(String debtorBic) { this.debtorBic = debtorBic; }
    public void setDebtorEmail(String debtorEmail) { this.debtorEmail = debtorEmail; }
    public void setDebtorPhone(String debtorPhone) { this.debtorPhone = debtorPhone; }

    // Generate unique debtor ID from account details
    public String generateDebtorId() {
        return "DBT-" + debtorSortCode.replace("-", "") + "-" + debtorAccountNumber;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final MandateFileRecord r = new MandateFileRecord();

        public Builder mandateId(String v) { r.mandateId = v; return this; }
        public Builder lastUpdateDate(LocalDateTime v) { r.lastUpdateDate = v; return this; }
        public Builder mandateReference(String v) { r.mandateReference = v; return this; }
        public Builder mandateType(String v) { r.mandateType = v; return this; }
        public Builder frequency(String v) { r.frequency = v; return this; }
        public Builder status(String v) { r.status = v; return this; }
        public Builder signatureDate(LocalDate v) { r.signatureDate = v; return this; }
        public Builder effectiveDate(LocalDate v) { r.effectiveDate = v; return this; }
        public Builder expiryDate(LocalDate v) { r.expiryDate = v; return this; }
        public Builder maxAmountPerTransaction(BigDecimal v) { r.maxAmountPerTransaction = v; return this; }
        public Builder maxAmountPerMonth(BigDecimal v) { r.maxAmountPerMonth = v; return this; }
        public Builder maxTransactionsPerMonth(Integer v) { r.maxTransactionsPerMonth = v; return this; }
        public Builder currency(String v) { r.currency = v; return this; }
        public Builder description(String v) { r.description = v; return this; }
        public Builder schemeType(String v) { r.schemeType = v; return this; }
        public Builder creditorId(String v) { r.creditorId = v; return this; }
        public Builder creditorName(String v) { r.creditorName = v; return this; }
        public Builder creditorAccountNumber(String v) { r.creditorAccountNumber = v; return this; }
        public Builder creditorSortCode(String v) { r.creditorSortCode = v; return this; }
        public Builder creditorIban(String v) { r.creditorIban = v; return this; }
        public Builder creditorBic(String v) { r.creditorBic = v; return this; }
        public Builder debtorName(String v) { r.debtorName = v; return this; }
        public Builder debtorAccountNumber(String v) { r.debtorAccountNumber = v; return this; }
        public Builder debtorSortCode(String v) { r.debtorSortCode = v; return this; }
        public Builder debtorIban(String v) { r.debtorIban = v; return this; }
        public Builder debtorBic(String v) { r.debtorBic = v; return this; }
        public Builder debtorEmail(String v) { r.debtorEmail = v; return this; }
        public Builder debtorPhone(String v) { r.debtorPhone = v; return this; }

        public MandateFileRecord build() { return r; }
    }
}
