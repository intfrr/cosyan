package com.cosyan.db.tools;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.cosyan.db.DBApi;
import com.cosyan.db.UnitTestBase;
import com.cosyan.db.lang.transaction.Result.ErrorResult;
import com.cosyan.db.logging.MetaJournal.DBException;
import com.cosyan.db.meta.MetaRepo.ModelException;

public class BackupManagerTest extends UnitTestBase {

  @Test
  public void testCreateBackup() throws IOException, DBException {
    BackupManager backupManager = new BackupManager(config, metaRepo);
    execute("create table t1 (a varchar, b integer);");
    execute("insert into t1 values ('abc', 123), ('abc', 123), ('abc', 123);");

    backupManager.backup("my_backup.zip");
    File file = new File("/tmp/data/backup/my_backup.zip");
    assertTrue(file.exists());

    execute("create table t2 (a varchar, b float);");
    execute("insert into t2 values ('xyz', 10.0);");
    query("select * from t1;");
    query("select * from t2;");

    dbApi.shutdown();

    dbApi = new DBApi(config);
    backupManager.restore("my_backup.zip");

    query("select * from t1;");
    ErrorResult e = error("select * from t2;");
    assertError(ModelException.class, "[14, 16]: Table 't2' does not exist.", e);
  }
}
