package com.mongodb.mandate.service;

import com.mongodb.mandate.model.DirectDebitMandate;
import com.mongodb.mandate.model.FieldChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MandateDiffService {

    private static final Logger logger = LoggerFactory.getLogger(MandateDiffService.class);

    // Fields to exclude from diff comparison
    private static final Set<String> EXCLUDED_FIELDS = Set.of(
            "id", "createdAt", "version"
    );

    /**
     * Compare two mandates and return a list of field changes
     */
    public List<FieldChange> diff(DirectDebitMandate existing, DirectDebitMandate updated) {
        List<FieldChange> changes = new ArrayList<>();

        Field[] fields = DirectDebitMandate.class.getDeclaredFields();

        for (Field field : fields) {
            if (EXCLUDED_FIELDS.contains(field.getName())) {
                continue;
            }

            field.setAccessible(true);

            try {
                Object existingValue = field.get(existing);
                Object updatedValue = field.get(updated);

                if (!areEqual(existingValue, updatedValue)) {
                    changes.add(FieldChange.builder()
                            .fieldName(field.getName())
                            .oldValue(formatValue(existingValue))
                            .newValue(formatValue(updatedValue))
                            .build());
                }
            } catch (IllegalAccessException e) {
                logger.error("Error accessing field {}: {}", field.getName(), e.getMessage());
            }
        }

        return changes;
    }

    /**
     * Apply changes from updated mandate to existing mandate while preserving
     * system fields like id, createdAt
     */
    public DirectDebitMandate applyChanges(DirectDebitMandate existing, DirectDebitMandate updated) {
        // Preserve system fields
        updated.setId(existing.getId());
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setVersion(existing.getVersion());

        return updated;
    }

    private boolean areEqual(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }

        // Special handling for BigDecimal comparison
        if (a instanceof BigDecimal && b instanceof BigDecimal) {
            return ((BigDecimal) a).compareTo((BigDecimal) b) == 0;
        }

        return Objects.equals(a, b);
    }

    private String formatValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).toString();
        }

        if (value instanceof LocalDate) {
            return ((LocalDate) value).toString();
        }

        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).toPlainString();
        }

        return value.toString();
    }
}
