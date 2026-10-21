package com.loanproject.loan_app.security;

import java.lang.reflect.Field;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/** Preserves the application's Long identifiers while using MongoDB. */
@Component
public class MongoLongIdGenerator implements BeforeConvertCallback<Object> {
    private final ObjectProvider<MongoTemplate> mongoTemplateProvider;

    public MongoLongIdGenerator(ObjectProvider<MongoTemplate> mongoTemplateProvider) {
        this.mongoTemplateProvider = mongoTemplateProvider;
    }

    @Override
    public Object onBeforeConvert(Object entity, String collection) {
        Field idField = findLongIdField(entity.getClass());
        if (idField == null) return entity;
        try {
            idField.setAccessible(true);
            if (idField.get(entity) == null) {
                String sequenceId = entity.getClass().getName();
                Sequence sequence = mongoTemplateProvider.getObject().findAndModify(
                        Query.query(Criteria.where("_id").is(sequenceId)),
                        new Update().inc("value", 1),
                        FindAndModifyOptions.options().upsert(true).returnNew(true),
                        Sequence.class);
                idField.set(entity, sequence.getValue());
            }
            return entity;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not assign MongoDB ID for " + entity.getClass().getSimpleName(), e);
        }
    }

    private Field findLongIdField(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType() == Long.class && field.isAnnotationPresent(Id.class)) return field;
            }
        }
        return null;
    }

    @org.springframework.data.mongodb.core.mapping.Document(collection = "_sequences")
    public static class Sequence {
        @Id private String id;
        private Long value;
        public Sequence() {}
        public Long getValue() { return value == null ? 1L : value; }
        public void setValue(Long value) { this.value = value; }
    }
}
