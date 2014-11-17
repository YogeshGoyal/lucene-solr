package org.apache.lucene.index;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.document.Document2;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldTypes;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.TestUtil;

/**
 * Tests {@link Terms#getSumDocFreq()}
 * @lucene.experimental
 */
public class TestSumDocFreq extends LuceneTestCase {
  
  public void testSumDocFreq() throws Exception {
    final int numDocs = atLeast(500);
    
    Directory dir = newDirectory();
    RandomIndexWriter writer = new RandomIndexWriter(random(), dir);
    FieldTypes fieldTypes = writer.getFieldTypes();
    
    for (int i = 0; i < numDocs; i++) {
      Document2 doc = writer.newDocument();
      doc.addUniqueInt("id", i);
      char ch1 = (char) TestUtil.nextInt(random(), 'a', 'z');
      char ch2 = (char) TestUtil.nextInt(random(), 'a', 'z');
      doc.addLargeText("foo", "" + ch1 + " " + ch2);
      ch1 = (char) TestUtil.nextInt(random(), 'a', 'z');
      ch2 = (char) TestUtil.nextInt(random(), 'a', 'z');
      doc.addLargeText("bar", "" + ch1 + " " + ch2);
      writer.addDocument(doc);
    }
    
    IndexReader ir = writer.getReader();
    
    assertSumDocFreq(ir);    
    ir.close();
    
    int numDeletions = atLeast(20);
    for (int i = 0; i < numDeletions; i++) {
      writer.deleteDocuments(fieldTypes.newIntTerm("id", random().nextInt(numDocs)));
    }
    writer.forceMerge(1);
    writer.close();
    
    ir = DirectoryReader.open(dir);
    assertSumDocFreq(ir);
    ir.close();
    dir.close();
  }
  
  private void assertSumDocFreq(IndexReader ir) throws Exception {
    // compute sumDocFreq across all fields
    Fields fields = MultiFields.getFields(ir);

    for (String f : fields) {
      Terms terms = fields.terms(f);
      long sumDocFreq = terms.getSumDocFreq();
      if (sumDocFreq == -1) {
        if (VERBOSE) {
          System.out.println("skipping field: " + f + ", codec does not support sumDocFreq");
        }
        continue;
      }
      
      long computedSumDocFreq = 0;
      TermsEnum termsEnum = terms.iterator(null);
      while (termsEnum.next() != null) {
        computedSumDocFreq += termsEnum.docFreq();
      }
      assertEquals(computedSumDocFreq, sumDocFreq);
    }
  }
}
