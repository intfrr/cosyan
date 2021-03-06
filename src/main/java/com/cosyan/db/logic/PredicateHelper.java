/*
 * Copyright 2018 Gergely Svigruha
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cosyan.db.logic;

import java.util.ArrayList;
import java.util.List;

import com.cosyan.db.lang.expr.BinaryExpression;
import com.cosyan.db.lang.expr.Expression;
import com.cosyan.db.lang.expr.Expression.UnaryExpression;
import com.cosyan.db.lang.expr.FuncCallExpression;
import com.cosyan.db.lang.expr.Literals.Literal;
import com.cosyan.db.lang.expr.Literals.LongLiteral;
import com.cosyan.db.lang.expr.Literals.StringLiteral;
import com.cosyan.db.lang.sql.Tokens;
import com.cosyan.db.meta.MetaRepo.ModelException;
import com.cosyan.db.model.BasicColumn;
import com.cosyan.db.model.Ident;
import com.cosyan.db.model.SeekableTableMeta;
import com.google.common.collect.ImmutableList;

import lombok.Data;

public class PredicateHelper {

  @Data
  public static class VariableEquals {
    private final Ident ident;
    private final Object value;
  }

  public static VariableEquals getBestClause(SeekableTableMeta tableMeta, Expression where) throws ModelException {
    ImmutableList<VariableEquals> clauses = PredicateHelper.extractClauses(where);
    VariableEquals clause = null;
    for (VariableEquals clauseCandidate : clauses) {
      BasicColumn column = tableMeta.tableMeta().column(clauseCandidate.getIdent());
      if ((clause == null && column.isIndexed()) || column.isUnique()) {
        clause = clauseCandidate;
      }
    }
    return clause;
  }
  
  public static ImmutableList<VariableEquals> extractClauses(Expression expression) {
    List<VariableEquals> predicates = new ArrayList<>();
    extractClauses(expression, predicates);
    return ImmutableList.copyOf(predicates);
  }

  private static void extractClauses(Expression node, List<VariableEquals> predicates) {
    if (node instanceof BinaryExpression) {
      BinaryExpression binaryExpression = (BinaryExpression) node;
      if (binaryExpression.getToken().is(Tokens.AND)) {
        extractClauses(binaryExpression.getLeft(), predicates);
        extractClauses(binaryExpression.getRight(), predicates);
      } else if (binaryExpression.getToken().is(Tokens.EQ)) {
        collectClause(binaryExpression.getLeft(), binaryExpression.getRight(), predicates);
        collectClause(binaryExpression.getRight(), binaryExpression.getLeft(), predicates);
      }
    } else if (node instanceof UnaryExpression) {
      // TODO
    }
  }

  private static void collectClause(Expression first, Expression second, List<VariableEquals> lookupsToCollect) {
    if (first instanceof FuncCallExpression && second instanceof Literal) {
      Ident ident = ((FuncCallExpression) first).getIdent();
      // TODO replan this
      if (second instanceof StringLiteral) {
        lookupsToCollect.add(new VariableEquals(ident, ((StringLiteral) second).getValue()));
      } else if (second instanceof LongLiteral) {
        lookupsToCollect.add(new VariableEquals(ident, ((LongLiteral) second).getValue()));
      } 
    }
  }
}
