package com.mongodb.mandate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectDebitMandate {

    @BsonId
    private ObjectId id;

    @BsonProperty("mandateId")
    private String mandateId;

    @BsonProperty("lastUpdateDate")
    private LocalDateTime lastUpdateDate;

    // Creditor Information
    @BsonProperty("creditorId")
    private String creditorId;

    @BsonProperty("creditorName")
    private String creditorName;

    @BsonProperty("creditorAccountNumber")
    private String creditorAccountNumber;

    @BsonProperty("creditorSortCode")
    private String creditorSortCode;

    @BsonProperty("creditorIban")
    private String creditorIban;

    @BsonProperty("creditorBic")
    private String creditorBic;

    // Debtor Information
    @BsonProperty("debtorName")
    private String debtorName;

    @BsonProperty("debtorAccountNumber")
    private String debtorAccountNumber;

    @BsonProperty("debtorSortCode")
    private String debtorSortCode;

    @BsonProperty("debtorIban")
    private String debtorIban;

    @BsonProperty("debtorBic")
    private String debtorBic;

    @BsonProperty("debtorEmail")
    private String debtorEmail;

    @BsonProperty("debtorPhone")
    private String debtorPhone;

    // Mandate Details
    @BsonProperty("mandateReference")
    private String mandateReference;

    @BsonProperty("mandateType")
    private String mandateType; // RECURRING, ONE_OFF

    @BsonProperty("frequency")
    private String frequency; // WEEKLY, MONTHLY, QUARTERLY, ANNUALLY

    @BsonProperty("status")
    private String status; // ACTIVE, SUSPENDED, CANCELLED, PENDING

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

    // Currency
    @BsonProperty("currency")
    private String currency;

    // Additional Fields
    @BsonProperty("description")
    private String description;

    @BsonProperty("schemeType")
    private String schemeType; // BACS, SEPA_CORE, SEPA_B2B

    @BsonProperty("createdAt")
    private LocalDateTime createdAt;

    @BsonProperty("version")
    private Integer version;
}
