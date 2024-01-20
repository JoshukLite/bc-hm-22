package org.simple.manager;

import org.simple.exception.EntityParseException;
import org.simple.exception.EntityQueryException;
import org.simple.meta.Column;
import org.simple.meta.Entity;
import org.simple.meta.Id;
import org.simple.meta.Table;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CustomEntityManager {

    public static final String SELECT_BY_ID_QUERY = "SELECT %s FROM %s WHERE %s = ?";
    private final String url;
    private final String user;
    private final String password;

    public CustomEntityManager(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public <T> T findById(Class<T> cls, Object id) {
        validateIsEntity(cls);

        String tableName = getEntityTableName(cls);
        List<Field> columnFields = getClassColumnFields(cls);
        Field idField = findEntityIdField(columnFields);

        String sql = SELECT_BY_ID_QUERY.formatted(composeSelectBlockFromColumns(columnFields),
                tableName, getIdColumnName(idField));

        return doQuery(sql, id, new EntityMapper<T>(cls, columnFields));
    }

    private <T> T doQuery(String sql, Object id, EntityMapper<T> entityMapper) {
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setObject(1, id);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return entityMapper.mapResult(resultSet);
            }
        } catch (SQLException e) {
            throw new EntityQueryException("Could not read entity data from database for entity [%s] with id [%s]"
                    .formatted(entityMapper.getEntityCls(), id), e);
        }
        return null;
    }

    private <T> void validateIsEntity(Class<T> cls) {
        if (!cls.isAnnotationPresent(Entity.class)) {
            throw new EntityParseException("Clas should be marked with 'Entity' annotation");
        }
    }

    private String getEntityTableName(Class<?> cls) {
        return Optional.ofNullable(cls.getAnnotation(Table.class))
                .map(Table::name)
                .orElseThrow(() -> new EntityParseException("Class should be marked with 'Table' annotation and it should not be empty"));
    }

    private List<Field> getClassColumnFields(Class<?> cls) {
        return Arrays.stream(cls.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(Id.class))
                .toList();
    }

    private Field findEntityIdField(List<Field> fields) {
        List<Field> idFields = fields.stream()
                .filter(field -> field.isAnnotationPresent(Id.class))
                .toList();
        if (idFields.isEmpty()) {
            throw new EntityParseException("Entity should define ID column marked with 'Id' annotation.");
        }
        if (idFields.size() > 1) {
            throw new EntityParseException("Only one field should be marked with 'Id' annotation.");
        }
        return idFields.get(0);
    }

    private String composeSelectBlockFromColumns(List<Field> columnNames) {
        return columnNames.stream()
                .map(this::getFieldColumnName)
                .collect(Collectors.joining(", "));
    }

    private String getFieldColumnName(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            return field.getAnnotation(Column.class).name();
        } else if (field.isAnnotationPresent(Id.class)) {
            return getIdColumnName(field);
        }
        throw new IllegalArgumentException("Field should have annotation of type [Column.class, Id.class]");
    }

    private String getIdColumnName(Field idField) {
        return idField.getAnnotation(Id.class).name();
    }
}
