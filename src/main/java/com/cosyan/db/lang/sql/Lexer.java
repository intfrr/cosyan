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
package com.cosyan.db.lang.sql;

import java.util.ArrayList;

import com.cosyan.db.lang.sql.Tokens.BooleanToken;
import com.cosyan.db.lang.sql.Tokens.FloatToken;
import com.cosyan.db.lang.sql.Tokens.IdentToken;
import com.cosyan.db.lang.sql.Tokens.IntToken;
import com.cosyan.db.lang.sql.Tokens.Loc;
import com.cosyan.db.lang.sql.Tokens.StringToken;
import com.cosyan.db.lang.sql.Tokens.Token;
import com.cosyan.db.session.ILexer;
import com.cosyan.db.session.IParser.ParserException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

public class Lexer implements ILexer {

  private static final int STATE_DEFAULT = 0;
  private static final int STATE_IN_SINGLE_QUOTE = 1;
  private static final int STATE_IN_DOUBLE_QUOTE = 2;
  private static final int STATE_NUMBER_LITERAL = 3;
  private static final int STATE_FLOAT_LITERAL = 4;
  private static final int STATE_IDENT = 5;

  public PeekingIterator<Token> tokenize(String sql) throws ParserException {
    return Iterators.peekingIterator(tokens(sql, /* fullSQL= */true).iterator());
  }

  public PeekingIterator<Token> tokenizeExpression(String sql) throws ParserException {
    return Iterators.peekingIterator(tokens(sql, /* fullSQL= */false).iterator());
  }

  @VisibleForTesting
  ImmutableList<Token> tokens(String sql) throws ParserException {
    return tokens(sql, /* fullSQL= */true);
  }

  private ImmutableList<Token> tokens(String sql, boolean fullSQL) throws ParserException {
    if (sql == null || sql.isEmpty()) {
      throw new ParserException("Query must not be empty.", new Loc(0, 0));
    }
    String trimmedSQL = sql.trim();
    if (fullSQL && !trimmedSQL.endsWith(";")) {
      throw new ParserException("Query must end with ';'.", new Loc(trimmedSQL.length() - 1, trimmedSQL.length()));
    }
    int state = STATE_DEFAULT;
    ArrayList<Token> builder = new ArrayList<>();
    int literalStartIndex = 0;
    for (int i = 0; i < sql.length(); i++) {
      char c = sql.charAt(i);
      if (state == STATE_IN_SINGLE_QUOTE) {
        if (c == Tokens.SINGLE_QUOTE) {
          builder.add(new StringToken(sql, literalStartIndex + 1, i));
          state = STATE_DEFAULT;
          literalStartIndex = i;
        }
      } else if (state == STATE_IN_DOUBLE_QUOTE) {
        if (c == Tokens.DOUBLE_QUOTE) {
          builder.add(new StringToken(sql, literalStartIndex + 1, i));
          state = STATE_DEFAULT;
          literalStartIndex = i;
        }
      } else if (state == STATE_NUMBER_LITERAL) {
        if (c == Tokens.DOT) {
          state = STATE_FLOAT_LITERAL;
        } else if (Tokens.isDelimiter(c)) {
          builder.add(new IntToken(sql, literalStartIndex, i));
          state = STATE_DEFAULT;
          literalStartIndex = i;
          i--;
        } else {
          if (!Tokens.isDigit(c)) {
            throw new ParserException("Wrong number.", new Loc(literalStartIndex, i));
          }
        }
      } else if (state == STATE_FLOAT_LITERAL) {
        if (Tokens.isDelimiter(c)) {
          builder.add(new FloatToken(sql, literalStartIndex, i));
          state = STATE_DEFAULT;
          literalStartIndex = i;
          i--;
        } else {
          if (!Tokens.isDigit(c)) {
            throw new ParserException("Wrong number.", new Loc(literalStartIndex, i));
          }
        }
      } else if (state == STATE_IDENT) {
        if (Tokens.isDelimiter(c) || c == Tokens.DOT) {
          String str = sql.substring(literalStartIndex, i);
          if (str.equals(Tokens.TRUE) || str.equals(Tokens.FALSE)) {
            builder.add(new BooleanToken(str, new Loc(literalStartIndex, i)));
          } else {
            builder.add(new IdentToken(str, new Loc(literalStartIndex, i)));
          }
          state = STATE_DEFAULT;
          literalStartIndex = i;
          i--;
        } else {
          if (!(Tokens.isDigit(c) || Tokens.isLowerCaseLetter(c) || Tokens.isUpperCaseLetter(c) || c == '_')) {
            throw new ParserException("Wrong ident.", new Loc(literalStartIndex, i));
          }
        }
      } else if (state == STATE_DEFAULT) {
        if (Tokens.isDigit(c) || (c == Tokens.MINUS && i < sql.length() - 1 && Tokens.isDigit(sql.charAt(i + 1)))) {
          state = STATE_NUMBER_LITERAL;
          literalStartIndex = i;
        } else if (Tokens.isDelimiter(c)) {
          if (c == Tokens.LESS && i < sql.length() - 1 && sql.charAt(i + 1) == Tokens.EQ) {
            builder.add(new Token(Tokens.LEQ, new Loc(literalStartIndex, i + 1)));
            i++;
          } else if (c == Tokens.GREATER && i < sql.length() - 1 && sql.charAt(i + 1) == Tokens.EQ) {
            builder.add(new Token(Tokens.GEQ, new Loc(literalStartIndex, i + 1)));
            i++;
          } else if (!Tokens.isWhitespace(c)) {
            builder.add(new Token(String.valueOf(c), new Loc(literalStartIndex, i)));
          }
          literalStartIndex = i;
        } else if (c == Tokens.SINGLE_QUOTE) {
          state = STATE_IN_SINGLE_QUOTE;
          literalStartIndex = i;
        } else if (c == Tokens.DOUBLE_QUOTE) {
          state = STATE_IN_DOUBLE_QUOTE;
          literalStartIndex = i;
        } else if (Tokens.isLowerCaseLetter(c) || c == Tokens.DOT) {
          if (c == Tokens.DOT) {
            builder.add(new Token(String.valueOf(c), new Loc(literalStartIndex, i)));
            i++;
          }
          state = STATE_IDENT;
          literalStartIndex = i;
        } else {
          throw new ParserException("Syntax error.", new Loc(literalStartIndex, i));
        }
      }
    }
    return ImmutableList.copyOf(builder);
  }
}
