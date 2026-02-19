package com.mongodb.mandate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldChange {

    @BsonProperty("fieldName")
    private String fieldName;

    @BsonProperty("oldValue")
    private String oldValue;

    @BsonProperty("newValue")
    private String newValue;
}
