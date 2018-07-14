package com.cosyan.ui.sql;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import com.cosyan.db.session.Session;
import com.cosyan.ui.SessionHandler;

public class SQLServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private final SessionHandler sessionHandler;

  public SQLServlet(SessionHandler sessionHandler) {
    this.sessionHandler = sessionHandler;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    sessionHandler.execute(req, resp, (Session session) -> {
      String sql = req.getParameter("sql");
      JSONObject result = session.execute(sql).toJSON();
      if (result.has("error")) {
        resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
      } else {
        resp.setStatus(HttpStatus.OK_200);
      }
      resp.getWriter().println(result);
    });
  }
}