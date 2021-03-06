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
package com.cosyan.db.index;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import com.cosyan.db.index.ByteTrie.LongIndex;

public class ByteTrieLargeTest {

  @Test
  @Ignore
  public void testLongByteTrie() throws Exception {
    Files.deleteIfExists(Paths.get("/tmp/longindex"));
    LongIndex index = new LongIndex("/tmp/longindex");
    Random random = new Random();
    Set<Long> ll = new HashSet<>();
    for (int i = 0; i < 1000000; i++) {
      ll.add(random.nextLong());
    }
    long t = System.currentTimeMillis();
    int j = 0;
    for (long i: ll) {
      if (j++ % 10000 == 0) {
        index.commit();
      }
      index.put(i, random.nextLong());
    }
    System.out.println(System.currentTimeMillis() - t);
    //index.cleanUp();
    t = System.currentTimeMillis();
    for (long i: ll) {
      index.get(i);
    }
    System.out.println(System.currentTimeMillis() - t);
  }
}
