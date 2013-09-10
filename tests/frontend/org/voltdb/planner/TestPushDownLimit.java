/* This file is part of VoltDB.
 * Copyright (C) 2008-2013 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.voltdb.planner;

import java.util.List;

import org.voltdb.plannodes.AbstractPlanNode;
import org.voltdb.plannodes.AbstractJoinPlanNode;
import org.voltdb.plannodes.AbstractScanPlanNode;
import org.voltdb.plannodes.LimitPlanNode;
import org.voltdb.types.JoinType;
import org.voltdb.types.PlanNodeType;

public class TestPushDownLimit extends PlannerTestCase {
    @Override
    protected void setUp() throws Exception {
        setupSchema(getClass().getResource("testplans-groupby-ddl.sql"),
                    "testpushdownlimit", false);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testPushDownIntoJoin() {
        List<AbstractPlanNode> pn = compileToFragments("SELECT * FROM D1, D2 WHERE D1.D1_PKEY = D2.D2_PKEY LIMIT 2");
        checkPushedDownLimit(pn, false, false, true, false);
    }

    public void testPushDownIntoJoinMultiPart() {
        List<AbstractPlanNode> pn = compileToFragments("SELECT * FROM T1, T2 WHERE T1.PKEY = T2.PKEY LIMIT 2");
        checkPushedDownLimit(pn, true, false, true, false);
    }

    public void testPushDownIntoLeftJoin() {
        List<AbstractPlanNode> pn = compileToFragments("SELECT * FROM D1 LEFT JOIN D2 ON D1.D1_PKEY = D2.D2_PKEY LIMIT 2");
        checkPushedDownLimit(pn, false, false, true, true);
    }

    public void testPushDownIntoLeftJoinMultiPart() {
        List<AbstractPlanNode> pn = compileToFragments("SELECT * FROM T1 LEFT JOIN T2 ON T1.PKEY = T2.PKEY LIMIT 2");
        checkPushedDownLimit(pn, true, false, true, true);
    }



    /**
     * Check if the limit node is pushed-down in the given plan.
     *
     * @param np
     *            The generated plan
     * @param isMultiPart
     *            Whether or not the plan is distributed
     * @param downIntoScan
     *            limit node is pushed down into the scan node
     * @param downIntoJoin
     *            limit node is pushed down into the join node
     * @param isLeftJoin
     *            Whether or not the join node type is left outer join, TRUE when it's left outer join and downIntoJoin is TRUE
     */
    private void checkPushedDownLimit(List<AbstractPlanNode> pn, boolean isMultiPart, boolean downIntoScan, boolean downIntoJoin, boolean isLeftJoin) {
        assertTrue(pn.size() > 0);

        for ( AbstractPlanNode nd : pn) {
            System.out.println("PlanNode Explain string:\n" + nd.toExplainPlanString());
        }

        if (isMultiPart) {
            assertTrue(pn.size() == 2);
            AbstractPlanNode p = pn.get(0).getChild(0);
            assertTrue(p instanceof LimitPlanNode);
            assertTrue(p.toJSONString().contains("\"LIMIT\""));
            checkPushedDownLimit(pn.get(1).getChild(0), downIntoScan, downIntoJoin, isLeftJoin);
        } else {
            checkPushedDownLimit(pn.get(0).getChild(0).getChild(0), downIntoScan, downIntoJoin, isLeftJoin);
        }
    }

    private void checkPushedDownLimit(AbstractPlanNode p, boolean downIntoScan, boolean downIntoJoin, boolean isLeftJoin) {

        if (downIntoScan) {
            assertTrue(p instanceof AbstractScanPlanNode);
            assertTrue(p.getInlinePlanNode(PlanNodeType.LIMIT) != null);
        }

        if (downIntoJoin) {
            assertTrue(p instanceof AbstractJoinPlanNode);
            assertTrue(p.getInlinePlanNode(PlanNodeType.LIMIT) != null);
        }

        if (isLeftJoin) {
            assertTrue(p instanceof AbstractJoinPlanNode);
            assertTrue(((AbstractJoinPlanNode)p).getJoinType() == JoinType.LEFT);
            assertTrue(p.getInlinePlanNode(PlanNodeType.LIMIT) != null);
            if (p.getChild(0) instanceof AbstractScanPlanNode || p.getChild(0) instanceof AbstractJoinPlanNode) {
                assertTrue(p.getChild(0).getInlinePlanNode(PlanNodeType.LIMIT) != null);
            } else {
                assertTrue(p.getChild(0).getPlanNodeType() == PlanNodeType.LIMIT);
            }
        }
    }

}
