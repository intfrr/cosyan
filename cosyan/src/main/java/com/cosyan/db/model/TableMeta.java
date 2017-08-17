package com.cosyan.db.model;

import java.io.DataInputStream;

import com.cosyan.db.io.TableReader;
import com.cosyan.db.io.TableReader.AggrTableReader;
import com.cosyan.db.io.TableReader.DerivedTableReader;
import com.cosyan.db.io.TableReader.ExposedTableReader;
import com.cosyan.db.io.TableReader.FilteredTableReader;
import com.cosyan.db.io.TableReader.HashJoinTableReader;
import com.cosyan.db.io.TableReader.MaterializedTableReader;
import com.cosyan.db.io.TableReader.SortedTableReader;
import com.cosyan.db.model.ColumnMeta.AggrColumn;
import com.cosyan.db.model.ColumnMeta.BasicColumn;
import com.cosyan.db.model.ColumnMeta.OrderColumn;
import com.cosyan.db.model.MetaRepo.ModelException;
import com.cosyan.db.sql.SyntaxTree.Ident;
import com.cosyan.db.sql.Tokens;
import com.cosyan.db.sql.Tokens.Token;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import lombok.Data;
import lombok.EqualsAndHashCode;

public abstract class TableMeta {

  public static final ImmutableMap<String, ColumnMeta> wholeTableKeys = ImmutableMap.of("",
      ColumnMeta.TRUE_COLUMN);

  public abstract ImmutableMap<String, ? extends ColumnMeta> columns();

  protected abstract TableReader reader() throws ModelException;

  public abstract int indexOf(Ident ident) throws ModelException;

  protected int indexOf(ImmutableSet<String> keys, Ident ident) throws ModelException {
    int index = keys.asList().indexOf(ident.getString());
    if (index < 0) {
      throw new ModelException("Invalid identifier '" + ident.getString() + "'.");
    }
    return index;
  }

  public static abstract class ExposedTableMeta extends TableMeta {
    @Override
    public abstract ExposedTableReader reader() throws ModelException;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class MaterializedTableMeta extends ExposedTableMeta {
    private final String tableName;
    private final ImmutableMap<String, BasicColumn> columns;
    private final MetaRepo metaRepo;

    @Override
    public ImmutableMap<String, ? extends ColumnMeta> columns() {
      return columns;
    }

    @Override
    public ExposedTableReader reader() throws ModelException {
      return new MaterializedTableReader(
          new DataInputStream(metaRepo.open(this)),
          columns());
    }

    @Override
    public int indexOf(Ident ident) throws ModelException {
      return indexOf(columns().keySet(), ident);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class DerivedTableMeta extends ExposedTableMeta {
    private final TableMeta sourceTable;
    private final ImmutableMap<String, ColumnMeta> columns;

    @Override
    public ImmutableMap<String, ? extends ColumnMeta> columns() {
      return columns;
    }

    @Override
    public ExposedTableReader reader() throws ModelException {
      return new DerivedTableReader(sourceTable.reader(), columns());
    }

    @Override
    public int indexOf(Ident ident) throws ModelException {
      return indexOf(columns().keySet(), ident);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class FilteredTableMeta extends ExposedTableMeta {
    private final ExposedTableMeta sourceTable;
    private final ColumnMeta whereColumn;

    @Override
    public ImmutableMap<String, ? extends ColumnMeta> columns() {
      return sourceTable.columns();
    }

    @Override
    public ExposedTableReader reader() throws ModelException {
      return new FilteredTableReader(sourceTable.reader(), whereColumn);
    }

    @Override
    public int indexOf(Ident ident) throws ModelException {
      return sourceTable.indexOf(ident);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class KeyValueTableMeta extends TableMeta {
    private final TableMeta sourceTable;
    private final ImmutableMap<String, ColumnMeta> keyColumns;
    private final ImmutableMap<String, ? extends ColumnMeta> valueColumns;

    @Override
    public ImmutableMap<String, ? extends ColumnMeta> columns() {
      return valueColumns;
    }

    @Override
    public TableReader reader() throws ModelException {
      return sourceTable.reader();
    }

    @Override
    public int indexOf(Ident ident) throws ModelException {
      return indexOf(keyColumns.keySet(), ident);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class AggrTableMeta extends TableMeta {
    private final KeyValueTableMeta sourceTable;
    private final ImmutableList<AggrColumn> aggrColumns;
    private final ColumnMeta havingColumn;

    @Override
    public ImmutableMap<String, ? extends ColumnMeta> columns() {
      throw new UnsupportedOperationException();
    }

    @Override
    public TableReader reader() throws ModelException {
      return new AggrTableReader(sourceTable.reader(), sourceTable.keyColumns, aggrColumns, havingColumn);
    }

    @Override
    public int indexOf(Ident ident) throws ModelException {
      return sourceTable.keyColumns.size() + sourceTable.indexOf(ident);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class SortedTableMeta extends ExposedTableMeta {
    private final ExposedTableMeta sourceTable;
    private final ImmutableList<OrderColumn> orderColumns;

    @Override
    public ImmutableMap<String, ? extends ColumnMeta> columns() {
      return sourceTable.columns();
    }

    @Override
    public ExposedTableReader reader() throws ModelException {
      return new SortedTableReader(sourceTable.reader(), orderColumns);
    }

    @Override
    public int indexOf(Ident ident) throws ModelException {
      return sourceTable.indexOf(ident);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class JoinTableMeta extends ExposedTableMeta {
    private final Token joinType;
    private final TableMeta leftTable;
    private final TableMeta rightTable;
    private final ImmutableList<ColumnMeta> leftTableJoinColumns;
    private final ImmutableList<ColumnMeta> rightTableJoinColumns;

    @Override
    public ImmutableMap<String, ? extends ColumnMeta> columns() {
      return ImmutableMap.<String, ColumnMeta>builder()
          .putAll(leftTable.columns())
          .putAll(rightTable.columns())
          .build();
    }

    @Override
    public ExposedTableReader reader() throws ModelException {
      if (joinType.is(Tokens.INNER)) {
        return new HashJoinTableReader(leftTable.reader(), rightTable.reader(), leftTableJoinColumns,
            rightTableJoinColumns, columns(), true);
      } else {
        throw new ModelException("Unknown join type '" + joinType.getString() + "'.");
      }
    }

    @Override
    public int indexOf(Ident ident) throws ModelException {
      if (leftTable.columns().containsKey(ident.getString())) {
        return leftTable.indexOf(ident);
      } else {
        return leftTable.columns().size() + rightTable.indexOf(ident);
      }
    }
  }
}
