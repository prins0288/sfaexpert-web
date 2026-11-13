package in.opt.sfa.tenant.hr;

import in.opt.sfa.ai.AiScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiScopeTest {
    @Test void explicitWriteSqlIsRefused() {
        for (String q : new String[]{"INSERT INTO emp_detail (emp_id) VALUES ('x')", "update emp_detail set emp_name='a' where emp_id=1", "delete from client_master", "DROP TABLE x", "truncate table dcr_parent_summary", "alter table emp_detail add column z int", "create table t (id int)", "grant all on *.* to x"})
            assertTrue(AiScope.asksToChangeData(q), q);
    }
    @Test void askingForSuchCodeIsRefused() {
        for (String q : new String[]{"give me an update query for salary", "write a delete sql for all leaves", "script to truncate the dcr table", "sql to insert a new employee", "delete query likh do", "curl command to delete a holiday"})
            assertTrue(AiScope.asksToChangeData(q), q);
    }
    @Test void howToQuestionsAreNotBlocked() {
        for (String q : new String[]{"salary kaise update karun?", "how do I delete a payslip", "leave approve kahan hoga", "tour plan ka update kaise hota hai", "Where is the DCR report?", "how to insert a new holiday", "kitne employees active hain", "who has not submitted DCR today", "update karne ka option kahan hai"})
            assertFalse(AiScope.asksToChangeData(q), q);
    }
    @Test void answersWithSqlAreReplaced() {
        assertEquals(AiScope.REFUSE_WRITE, AiScope.screen("<p>Run <code>DELETE FROM emp_detail WHERE 1=1</code></p>"));
        assertEquals(AiScope.REFUSE_WRITE, AiScope.screen("<pre>UPDATE emp_detail SET emp_name = 'x'</pre>"));
        assertTrue(AiScope.screen("<p>SELECT emp_name FROM emp_detail</p>").contains("can’t share the underlying database structure"));
    }
    @Test void normalAnswersPass() {
        String ok = "<ol><li>Open <b>Masters &gt; Master Entry &gt; Payroll</b></li><li>Tap Generate</li></ol>";
        assertEquals(ok, AiScope.screen(ok));
        String sel = "<p>Please select the employee from the list and update the remarks.</p>";
        assertEquals(sel, AiScope.screen(sel));
        assertEquals(AiScope.OFF_TOPIC, AiScope.screen(null));
    }
}
