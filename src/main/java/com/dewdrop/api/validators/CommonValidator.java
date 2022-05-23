package com.dewdrop.api.validators;

import java.io.Serializable;
import java.lang.reflect.Field;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.text.WordUtils;
import org.reflections.ReflectionUtils;

@Log4j2
public abstract class CommonValidator<T extends Serializable> extends ValidationResult {
    public static final String IS_REQUIRED = " is required";
    protected T target;

    protected CommonValidator() {}

    protected CommonValidator(T target) {
        this.target = target;
        validateTarget();
    }

    protected ValidationResult title() {
        return notBlank(target, "title");
    }

    protected ValidationResult description() {
        return notBlank(target, "description");
    }

    protected ValidationResult placeholderText() {
        return notBlank(target, "placeholderText");
    }


    protected ValidationResult notBlank(Object item, String field) {
        if (item == null) {return ValidationResult.of(new ValidationError(formatFieldName(field) + IS_REQUIRED));}

        String value = getValue(item, field);

        if (StringUtils.isBlank(value)) {return ValidationResult.of(new ValidationError(formatFieldName(field) + IS_REQUIRED));}

        return this;
    }

    protected ValidationResult notNull(Object item, String field) {
        if (item == null) {return ValidationResult.of(new ValidationError(formatFieldName(field) + IS_REQUIRED));}

        Object value = getValue(item, field);

        if (value == null) {return ValidationResult.of(new ValidationError(formatFieldName(field) + IS_REQUIRED));}

        return this;
    }

    protected abstract ValidationResult rules();

    protected void validateTarget() {
        ValidationResult result = rules();
        this.addAll(result.get());
    }

    protected <S> S getValue(Object item, String fieldName) {

        Field field = FieldUtils.getField(item.getClass(), fieldName);
        if (field == null) {
            add(new ValidationError("Invalid field name '" + fieldName + "' for class " + item.getClass()
                .getSimpleName()));
            return null;
        }
        field.setAccessible(true);
        try {
            return (S) field.get(item);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    protected String formatFieldName(String field) {
        String fieldValue = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(field), ' ');
        fieldValue = WordUtils.capitalizeFully(fieldValue);
        return fieldValue;
    }
}

