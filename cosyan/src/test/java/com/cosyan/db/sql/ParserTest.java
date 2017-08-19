package com.cosyan.db.sql;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.junit.Test;

import com.cosyan.db.sql.Parser.ParserException;
import com.cosyan.db.sql.SyntaxTree.AsteriskExpression;
import com.cosyan.db.sql.SyntaxTree.DoubleLiteral;
import com.cosyan.db.sql.SyntaxTree.Expression;
import com.cosyan.db.sql.SyntaxTree.FuncCallExpression;
import com.cosyan.db.sql.SyntaxTree.Ident;
import com.cosyan.db.sql.SyntaxTree.IdentExpression;
import com.cosyan.db.sql.SyntaxTree.LongLiteral;
import com.cosyan.db.sql.SyntaxTree.Select;
import com.cosyan.db.sql.SyntaxTree.StringLiteral;
import com.cosyan.db.sql.SyntaxTree.TableRef;
import com.cosyan.db.sql.SyntaxTree.UnaryExpression;
import com.cosyan.db.sql.Tokens.Token;
import com.google.common.collect.ImmutableList;

public class ParserTest {

  private Parser parser = new Parser();

  @Test
  public void testSelect() throws ParserException {
    SyntaxTree tree = parser.parse("select * from table;");
    assertEquals(tree, new SyntaxTree(new Select(
        ImmutableList.of(new AsteriskExpression()),
        new TableRef(new Ident("table")),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty())));
  }

  @Test
  public void testSelectColmns() throws ParserException {
    SyntaxTree tree = parser.parse("select a, b from table;");
    assertEquals(tree, new SyntaxTree(new Select(
        ImmutableList.of(new IdentExpression(new Ident("a")), new IdentExpression(new Ident("b"))),
        new TableRef(new Ident("table")),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty())));
  }

  @Test
  public void testSelectAggr() throws ParserException {
    SyntaxTree tree = parser.parse("select sum(a) from table;");
    assertEquals(tree, new SyntaxTree(new Select(
        ImmutableList.of(new FuncCallExpression(
            new Ident("sum"),
            ImmutableList.of(new IdentExpression(new Ident("a"))))),
        new TableRef(new Ident("table")),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty())));
  }

  @Test
  public void testSelectWhere() throws ParserException {
    SyntaxTree tree = parser.parse("select * from table where a = 1;");
    assertEquals(tree, new SyntaxTree(new Select(
        ImmutableList.of(new AsteriskExpression()),
        new TableRef(new Ident("table")),
        Optional.of(new BinaryExpression(
            new Token("="),
            new IdentExpression(new Ident("a")),
            new LongLiteral(1L))),
        Optional.empty(),
        Optional.empty(),
        Optional.empty())));
  }

  @Test
  public void testSelectGroupBy() throws ParserException {
    SyntaxTree tree = parser.parse("select sum(b) from table group by a;");
    assertEquals(tree, new SyntaxTree(new Select(
        ImmutableList.of(new FuncCallExpression(
            new Ident("sum"),
            ImmutableList.of(new IdentExpression(new Ident("b"))))),
        new TableRef(new Ident("table")),
        Optional.empty(),
        Optional.of(ImmutableList.of(new IdentExpression(new Ident("a")))),
        Optional.empty(),
        Optional.empty())));
  }

  @Test
  public void testSelectComplex() throws ParserException {
    SyntaxTree tree = parser.parse("select a, b + 1, c * 2.0 > 3.0 from table;");
    assertEquals(tree, new SyntaxTree(new Select(
        ImmutableList.of(
            new IdentExpression(new Ident("a")),
            new BinaryExpression(
                new Token("+"),
                new IdentExpression(new Ident("b")),
                new LongLiteral(1L)),
            new BinaryExpression(
                new Token(">"),
                new BinaryExpression(
                    new Token("*"),
                    new IdentExpression(new Ident("c")),
                    new DoubleLiteral(2.0)),
                new DoubleLiteral(3.0))),
        new TableRef(new Ident("table")),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty())));
  }

  @Test
  public void testExpr() throws ParserException {
    Expression expr = parser.parseExpression("a = 1;");
    assertEquals(expr, new BinaryExpression(
        new Token("="),
        new IdentExpression(new Ident("a")),
        new LongLiteral(1L)));
  }

  @Test
  public void testExprPrecedence1() throws ParserException {
    Expression expr = parser.parseExpression("a and b or c;");
    assertEquals(expr, new BinaryExpression(
        new Token("or"),
        new BinaryExpression(
            new Token("and"),
            new IdentExpression(new Ident("a")),
            new IdentExpression(new Ident("b"))),
        new IdentExpression(new Ident("c"))));
  }

  @Test
  public void testExprPrecedence2() throws ParserException {
    Expression expr = parser.parseExpression("a or b and c;");
    assertEquals(expr, new BinaryExpression(
        new Token("or"),
        new IdentExpression(new Ident("a")),
        new BinaryExpression(
            new Token("and"),
            new IdentExpression(new Ident("b")),
            new IdentExpression(new Ident("c")))));
  }

  @Test
  public void testExprParentheses1() throws ParserException {
    Expression expr = parser.parseExpression("(a or b) and c;");
    assertEquals(expr, new BinaryExpression(
        new Token("and"),
        new BinaryExpression(
            new Token("or"),
            new IdentExpression(new Ident("a")),
            new IdentExpression(new Ident("b"))),
        new IdentExpression(new Ident("c"))));
  }

  @Test
  public void testExprParentheses2() throws ParserException {
    Expression expr = parser.parseExpression("a and (b or c);");
    assertEquals(expr, new BinaryExpression(
        new Token("and"),
        new IdentExpression(new Ident("a")),
        new BinaryExpression(
            new Token("or"),
            new IdentExpression(new Ident("b")),
            new IdentExpression(new Ident("c")))));
  }

  @Test
  public void testExprLogical() throws ParserException {
    Expression expr = parser.parseExpression("a > 1 or c;");
    assertEquals(expr, new BinaryExpression(
        new Token("or"),
        new BinaryExpression(
            new Token(">"),
            new IdentExpression(new Ident("a")),
            new LongLiteral(1L)),
        new IdentExpression(new Ident("c"))));
  }

  @Test
  public void testExprFuncCall() throws ParserException {
    Expression expr = parser.parseExpression("a and f(b);");
    assertEquals(expr, new BinaryExpression(
        new Token("and"),
        new IdentExpression(new Ident("a")),
        new FuncCallExpression(new Ident("f"), ImmutableList.of(new IdentExpression(new Ident("b"))))));
  }

  @Test
  public void testExprNot() throws ParserException {
    Expression expr = parser.parseExpression("not a;");
    assertEquals(expr, new UnaryExpression(
        new Token(Tokens.NOT),
        new IdentExpression(new Ident("a"))));
  }

  @Test
  public void testExprNotInBinary() throws ParserException {
    Expression expr = parser.parseExpression("not a and not b;");
    assertEquals(expr, new BinaryExpression(
        new Token("and"),
        new UnaryExpression(new Token(Tokens.NOT), new IdentExpression(new Ident("a"))),
        new UnaryExpression(new Token(Tokens.NOT), new IdentExpression(new Ident("b")))));
  }

  @Test
  public void testExprNotWithLogical() throws ParserException {
    Expression expr = parser.parseExpression("not a = 'x';");
    assertEquals(expr, new UnaryExpression(
        new Token(Tokens.NOT),
        new BinaryExpression(
            new Token("="),
            new IdentExpression(new Ident("a")),
            new StringLiteral("x"))));
  }

  @Test(expected = ParserException.class)
  public void testGroupByInconsistentAggr() throws Exception {
    parser.parse("select sum(b) + b from large group by a;");
  }
}
