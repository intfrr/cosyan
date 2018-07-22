package com.cosyan.ui.sql;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.cosyan.db.session.Session;
import com.cosyan.ui.SessionHandler;

public class CancelServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private final SessionHandler sessionHandler;

  public CancelServlet(SessionHandler sessionHandler) {
    this.sessionHandler = sessionHandler;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    sessionHandler.execute(req, resp, (Session session) -> {
      session.cancel();
      return new JSONObject();
    });
  }
}