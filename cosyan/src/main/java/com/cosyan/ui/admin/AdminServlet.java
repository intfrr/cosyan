package com.cosyan.ui.admin;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import com.cosyan.db.DBApi;

public class AdminServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private final MetaRepoConnector metaRepoConnector;

  public AdminServlet(DBApi dbApi) {
    this.metaRepoConnector = new MetaRepoConnector(dbApi);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    try {
      JSONObject obj = new JSONObject();
      obj.put("tables", metaRepoConnector.tables());
      resp.setStatus(HttpStatus.OK_200);
      resp.getWriter().println(obj);
    } catch (Exception e) {
      e.printStackTrace();
      JSONObject error = new JSONObject();
      error.put("error", e.getMessage());
      resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
      resp.getWriter().println(error);
    }
  }
}
