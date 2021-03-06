package com.raizlabs.android.dbflow.sql.migration;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.QueryBuilder;
import com.raizlabs.android.dbflow.sql.SQLiteType;
import com.raizlabs.android.dbflow.structure.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Description: Provides a very nice way to alter a single table quickly and easily.
 */
public class AlterTableMigration<ModelClass extends Model> extends BaseMigration {

    /**
     * The table to ALTER
     */
    private final Class<ModelClass> table;

    /**
     * The query we use
     */
    private QueryBuilder query;

    /**
     * The query to rename the table with
     */
    private QueryBuilder renameQuery;

    /**
     * The columns to ALTER within a table
     */
    private ArrayList<QueryBuilder> columnDefinitions;

    /**
     * The old name of the table before renaming it
     */
    private String oldTableName;

    public AlterTableMigration(Class<ModelClass> table) {
        this.table = table;
    }

    @Override
    public final void migrate(SQLiteDatabase database) {
        // "ALTER TABLE "
        String sql = getAlterTableQueryBuilder().getQuery();
        String tableName = FlowManager.getTableName(table);

        // "{oldName}  RENAME TO {newName}"
        // Since the structure has been updated already, the manager knows only the new name.
        if (renameQuery != null) {
            String renameQuery = new QueryBuilder(sql).appendQuotedIfNeeded(oldTableName)
                    .append(this.renameQuery.getQuery())
                    .append(tableName)
                    .toString();
            database.execSQL(renameQuery);
        }

        // We have column definitions to add here
        // ADD COLUMN columnName {type}
        if (columnDefinitions != null) {
            sql = new QueryBuilder(sql).append(tableName).toString();
            for (QueryBuilder columnDefinition : columnDefinitions) {
                database.execSQL(sql + " ADD COLUMN " + columnDefinition.getQuery());
            }
        }
    }

    @CallSuper
    @Override
    public void onPostMigrate() {
        // cleanup and make fields eligible for garbage collection
        query = null;
        renameQuery = null;
        columnDefinitions = null;
    }

    /**
     * Call this to rename a table to a new name, such as changing either the {@link com.raizlabs.android.dbflow.structure.Model} class name
     * or by changing the name through a {@link com.raizlabs.android.dbflow.annotation.Table}
     *
     * @param oldName The new name to call the table.
     * @return This instance
     */
    public AlterTableMigration<ModelClass> renameFrom(@NonNull String oldName) {
        oldTableName = oldName;
        renameQuery = new QueryBuilder().append(" RENAME").appendSpaceSeparated("TO");
        return this;
    }

    /**
     * Add a column to the DB. This does not necessarily need to be reflected in the {@link ModelClass},
     * but it is recommended.
     *
     * @param sqLiteType The type of column represented in the DB.
     * @param columnName The name of the column to add. Use the "_Table" class for the specified table.
     * @return This instance
     */
    public AlterTableMigration<ModelClass> addColumn(@NonNull SQLiteType sqLiteType, @NonNull String columnName) {
        if (columnDefinitions == null) {
            columnDefinitions = new ArrayList<>();
        }

        QueryBuilder queryBuilder = new QueryBuilder()
                .append(QueryBuilder.quoteIfNeeded(columnName)).appendSpace().appendSQLiteType(sqLiteType);
        columnDefinitions.add(queryBuilder);

        return this;
    }

    /**
     * Add a column to the DB. This does not necessarily need to be reflected in the {@link ModelClass},
     * but it is recommended.
     *
     * @param columnType      The type of column that pertains to an {@link SQLiteType}
     * @param columnName      The name of the column to add. Use the "$Table" class for the specified table.
     * @param referenceClause The clause of the references that this foreign key points to.
     * @return This instance
     */
    public AlterTableMigration<ModelClass> addForeignKeyColumn(SQLiteType sqLiteType, String columnName, String referenceClause) {
        if (columnDefinitions == null) {
            columnDefinitions = new ArrayList<>();
        }

        QueryBuilder queryBuilder = new QueryBuilder()
                .append(QueryBuilder.quoteIfNeeded(columnName)).appendSpace().appendSQLiteType(sqLiteType)
                .appendSpace().append("REFERENCES ").append(referenceClause);
        columnDefinitions.add(queryBuilder);

        return this;
    }

    /**
     * @return The query that renames the table.
     */
    public String getRenameQuery() {
        QueryBuilder queryBuilder = new QueryBuilder(getAlterTableQueryBuilder().getQuery()).appendQuotedIfNeeded(oldTableName)
                .append(renameQuery).append(FlowManager.getTableName(table));
        return queryBuilder.getQuery();
    }

    /**
     * @return A List of column definitions that add column to a table in the DB.
     */
    public List<String> getColumnDefinitions() {
        String sql = new QueryBuilder(getAlterTableQueryBuilder()).append(FlowManager.getTableName(table)).toString();
        List<String> columnDefinitions = new ArrayList<>();

        if (this.columnDefinitions != null) {
            for (QueryBuilder columnDefinition : this.columnDefinitions) {
                QueryBuilder queryBuilder = new QueryBuilder(sql).appendSpaceSeparated("ADD COLUMN").append(
                        columnDefinition.getQuery());
                columnDefinitions.add(queryBuilder.getQuery());
            }
        }

        return columnDefinitions;
    }

    public QueryBuilder getAlterTableQueryBuilder() {
        if (query == null) {
            query = new QueryBuilder().append("ALTER").appendSpaceSeparated("TABLE");
        }
        return query;
    }
}
