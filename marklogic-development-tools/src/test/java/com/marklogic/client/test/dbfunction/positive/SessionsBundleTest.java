package com.marklogic.client.test.dbfunction.positive;

import com.marklogic.client.SessionState;
import com.marklogic.client.test.dbfunction.DBFunctionTestUtil;
import org.junit.Test;


import static org.junit.Assert.*;

public class SessionsBundleTest {
   SessionsBundle testObj = SessionsBundle.on(DBFunctionTestUtil.db);

   @Test
   public void fieldSessionTest() {
      final String timestampField = "timestamped";

      SessionState fieldSession = testObj.newSessionState();

      try {
         long timestampValue = testObj.setSessionField(fieldSession, timestampField);

         boolean valueMatched = testObj.getSessionField(fieldSession, timestampField, timestampValue);
         assertTrue("failed to get the "+timestampValue+" value from the "+timestampField+" session field", valueMatched);

         SessionState otherSession = testObj.newSessionState();

         valueMatched = testObj.getSessionField(otherSession, timestampField, timestampValue);
         assertFalse("got the "+timestampValue+" value from the "+timestampField+" field for the wrong session", valueMatched);
      } catch(Exception e) {
         fail(e.getClass().getSimpleName()+": "+e.getMessage());
      }
   }

   @Test
   public void transactionSessionTest() {
      final String docUri = "/test/session/transaction/doc1.txt";

      SessionState transactionSession = testObj.newSessionState();

      boolean hasRolledBack = false;
      try {
         String sessionId = transactionSession.getSessionId();
         String docText = "Transaction for session: "+sessionId;

         boolean docExists = testObj.checkTransaction(null, docUri);
         assertFalse("found "+docUri+" outside unopened transaction in session: "+sessionId, docExists);

         docExists = testObj.checkTransaction(transactionSession, docUri);
         assertFalse("found "+docUri+" before opening transaction in session: "+sessionId, docExists);

         testObj.beginTransaction(transactionSession, docUri, docText);

         docExists = testObj.checkTransaction(transactionSession, docUri);
         assertTrue("failed to find "+docUri+" after opening transaction in session: "+sessionId, docExists);

         docExists = testObj.checkTransaction(null, docUri);
         assertFalse("found "+docUri+" outside open transaction in session: "+sessionId, docExists);

         hasRolledBack = true;
         testObj.rollbackTransaction(transactionSession);

         docExists = testObj.checkTransaction(null, docUri);
         assertFalse("found "+docUri+" outside rolledback transaction in session: "+sessionId, docExists);

         docExists = testObj.checkTransaction(transactionSession, docUri);
         assertFalse("found "+docUri+" after rolling back transaction in session: "+sessionId, docExists);
      } catch(Exception e) {
         if (!hasRolledBack) {
            testObj.rollbackTransaction(transactionSession);
         }
         fail(e.getClass().getSimpleName()+": "+e.getMessage());
      }
   }

   @Test
   public void concurrentSessionTest() {
      final int concurrentMax = 3;
      final int sleeptime = 750;

      Thread[] threads = new Thread[concurrentMax];
      for (int i=0; i < concurrentMax; i++) {
         final int id = i;
         SessionState session = testObj.newSessionState();
         Runnable runner = () ->
            assertTrue("failed to sleep for thread " + id,
               testObj.sleepify(session, sleeptime)
            );
         threads[i] = new Thread(runner);
      }

      for (int i=0; i < concurrentMax; i++) {
         threads[i].start();
      }
   }

   @Test
   public void nullNegativeTest() {
      try {
         testObj.setSessionField(null, "unused");
         fail("null for required session parameter failed to throw an error");
      } catch(Exception e) {
      }
   }

   @Test
   public void transactionNoSessionNegativeTest() {
      try {
         testObj.beginTransactionNoSession("/test/session/transaction/negative1.txt", "should never succeed");
         fail("beginning a multistatement transaction outside a session failed to throw an error");
      } catch(Exception e) {
      }
   }

   @Test
   public void sessionFieldNoSessionNegativeTest() {
      try {
         testObj.setSessionFieldNoSession("shouldNeverSucceed");
         fail("setting a session field outside a session failed to throw an error");
      } catch(Exception e) {
      }
   }
}
