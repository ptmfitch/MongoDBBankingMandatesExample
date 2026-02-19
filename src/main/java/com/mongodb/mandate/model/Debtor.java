package com.mongodb.mandate.model;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

public class Debtor {

    @BsonId
    private ObjectId id;

    @BsonProperty("debtorId")
    private String debtorId;

    @BsonProperty("name")
    private String name;

    @BsonProperty("accountNumber")
    private String accountNumber;

    @BsonProperty("sortCode")
    private String sortCode;

    @BsonProperty("iban")
    private String iban;

    @BsonProperty("bic")
    private String bic;

    @BsonProperty("email")
    private String email;

    @BsonProperty("phone")
    private String phone;

    @BsonProperty("createdAt")
    private LocalDateTime createdAt;

    @BsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    public Debtor() {}

    // Getters
    public ObjectId getId() { return id; }
    public String getDebtorId() { return debtorId; }
    public String getName() { return name; }
    public String getAccountNumber() { return accountNumber; }
    public String getSortCode() { return sortCode; }
    public String getIban() { return iban; }
    public String getBic() { return bic; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(ObjectId id) { this.id = id; }
    public void setDebtorId(String debtorId) { this.debtorId = debtorId; }
    public void setName(String name) { this.name = name; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public void setSortCode(String sortCode) { this.sortCode = sortCode; }
    public void setIban(String iban) { this.iban = iban; }
    public void setBic(String bic) { this.bic = bic; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final Debtor d = new Debtor();

        public Builder id(ObjectId v) { d.id = v; return this; }
        public Builder debtorId(String v) { d.debtorId = v; return this; }
        public Builder name(String v) { d.name = v; return this; }
        public Builder accountNumber(String v) { d.accountNumber = v; return this; }
        public Builder sortCode(String v) { d.sortCode = v; return this; }
        public Builder iban(String v) { d.iban = v; return this; }
        public Builder bic(String v) { d.bic = v; return this; }
        public Builder email(String v) { d.email = v; return this; }
        public Builder phone(String v) { d.phone = v; return this; }
        public Builder createdAt(LocalDateTime v) { d.createdAt = v; return this; }
        public Builder updatedAt(LocalDateTime v) { d.updatedAt = v; return this; }

        public Debtor build() { return d; }
    }
}
