package com.mongodb.mandate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MandateAudit {

    @BsonId
    private ObjectId id;

    @BsonProperty("mandateId")
    private String mandateId;

    @BsonProperty("changeType")
    private String changeType; // INSERT, UPDATE

    @BsonProperty("changeTimestamp")
    private LocalDateTime changeTimestamp;

    @BsonProperty("sourceFile")
    private String sourceFile;

    @BsonProperty("previousUpdateDate")
    private LocalDateTime previousUpdateDate;

    @BsonProperty("newUpdateDate")
    private LocalDateTime newUpdateDate;

    @BsonProperty("fieldChanges")
    private List<FieldChange> fieldChanges;

    @BsonProperty("processedBy")
    private String processedBy;

    @BsonProperty("batchId")
    private String batchId;
}
