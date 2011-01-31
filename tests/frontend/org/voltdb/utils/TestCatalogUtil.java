/* This file is part of VoltDB.
 * Copyright (C) 2008-2011 VoltDB Inc.
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

package org.voltdb.utils;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;

import org.voltdb.benchmark.tpcc.TPCCProjectBuilder;
import org.voltdb.catalog.Catalog;
import org.voltdb.catalog.Column;
import org.voltdb.catalog.ColumnRef;
import org.voltdb.catalog.Constraint;
import org.voltdb.catalog.Database;
import org.voltdb.catalog.Index;
import org.voltdb.catalog.Table;
import org.voltdb.compiler.VoltProjectBuilder;
import org.voltdb.types.ConstraintType;

public class TestCatalogUtil extends TestCase {

    protected Catalog catalog;
    protected Database catalog_db;

    @Override
    protected void setUp() throws Exception {
        catalog = TPCCProjectBuilder.getTPCCSchemaCatalog();
        assertNotNull(catalog);
        catalog_db = catalog.getClusters().get("cluster").getDatabases().get("database");
        assertNotNull(catalog_db);
    }

    /**
     *
     */
    public void testGetSortedCatalogItems() {
        for (Table catalog_tbl : catalog_db.getTables()) {
            int last_idx = -1;
            List<Column> columns = CatalogUtil.getSortedCatalogItems(catalog_tbl.getColumns(), "index");
            assertFalse(columns.isEmpty());
            assertEquals(catalog_tbl.getColumns().size(), columns.size());
            for (Column catalog_col : columns) {
                assertTrue(catalog_col.getIndex() > last_idx);
                last_idx = catalog_col.getIndex();
            }
        }
    }

    /**
     *
     */
    public void testToSchema() {
        String search_str = "";

        // Simple check to make sure things look ok...
        for (Table catalog_tbl : catalog_db.getTables()) {
            String sql = CatalogUtil.toSchema(catalog_tbl);
            assertTrue(sql.startsWith("CREATE TABLE " + catalog_tbl.getTypeName()));

            // Columns
            for (Column catalog_col : catalog_tbl.getColumns()) {
                assertTrue(sql.indexOf(catalog_col.getTypeName()) != -1);
            }

            // Constraints
            for (Constraint catalog_const : catalog_tbl.getConstraints()) {
                ConstraintType const_type = ConstraintType.get(catalog_const.getType());
                Index catalog_idx = catalog_const.getIndex();
                List<ColumnRef> columns = CatalogUtil.getSortedCatalogItems(catalog_idx.getColumns(), "index");

                if (!columns.isEmpty()) {
                    search_str = "";
                    String add = "";
                    for (ColumnRef catalog_colref : columns) {
                        search_str += add + catalog_colref.getColumn().getTypeName();
                        add = ", ";
                    }
                    assertTrue(sql.indexOf(search_str) != -1);
                }

                switch (const_type) {
                    case PRIMARY_KEY:
                        assertTrue(sql.indexOf("PRIMARY KEY") != -1);
                        break;
                    case FOREIGN_KEY:
                        search_str = "REFERENCES " + catalog_const.getForeignkeytable().getTypeName();
                        assertTrue(sql.indexOf(search_str) != -1);
                        break;
                }
            }
        }
    }

    public void testDeploymentCRCs() {
        final String dep1 = "<?xml version='1.0' encoding='UTF-8' standalone='no'?>" +
                            "<deployment>" +
                            "<cluster hostcount='3' kfactor='1' leader='localhost' sitesperhost='2'/>" +
                            "<paths><voltroot path=\"/tmp\" /></paths>" +
                            "<httpd port='0'>" +
                            "<jsonapi enabled='true'/>" +
                            "</httpd>" +
                            "</deployment>";

        // differs in a meaningful way from dep1
        final String dep2 = "<?xml version='1.0' encoding='UTF-8' standalone='no'?>" +
                            "<deployment>" +
                            "<cluster hostcount='4' kfactor='1' leader='localhost' sitesperhost='2'/>" +
                            "<paths><voltroot path=\"/tmp\" /></paths>" +
                            "<httpd port='0'>" +
                            "<jsonapi enabled='true'/>" +
                            "</httpd>" +
                            "</deployment>";

        // differs in whitespace and attribute order from dep1
        final String dep3 = "<?xml version='1.0' encoding='UTF-8' standalone='no'?>" +
                            "<deployment>" +
                            "   <cluster hostcount='3' kfactor='1' leader='localhost' sitesperhost='2' />" +
                            "   <paths><voltroot path=\"/tmp\" /></paths>" +
                            "   <httpd port='0' >" +
                            "       <jsonapi enabled='true'/>" +
                            "   </httpd>" +
                            "</deployment>";

        // admin-mode section actually impacts CRC, dupe above and add it
        final String dep4 = "<?xml version='1.0' encoding='UTF-8' standalone='no'?>" +
                            "<deployment>" +
                            "   <cluster hostcount='3' kfactor='1' leader='localhost' sitesperhost='2'>" +
                            "      <admin-mode port='32323' adminstartup='true'/>" +
                            "   </cluster>" +
                            "   <paths><voltroot path=\"/tmp\" /></paths>" +
                            "   <httpd port='0' >" +
                            "       <jsonapi enabled='true'/>" +
                            "   </httpd>" +
                            "</deployment>";

        final File tmpDep1 = VoltProjectBuilder.writeStringToTempFile(dep1);
        final File tmpDep2 = VoltProjectBuilder.writeStringToTempFile(dep2);
        final File tmpDep3 = VoltProjectBuilder.writeStringToTempFile(dep3);
        final File tmpDep4 = VoltProjectBuilder.writeStringToTempFile(dep4);

        final long crcDep1 = CatalogUtil.getDeploymentCRC(tmpDep1.getPath());
        final long crcDep2 = CatalogUtil.getDeploymentCRC(tmpDep2.getPath());
        final long crcDep3 = CatalogUtil.getDeploymentCRC(tmpDep3.getPath());
        final long crcDep4 = CatalogUtil.getDeploymentCRC(tmpDep4.getPath());

        assertTrue(crcDep1 > 0);
        assertTrue(crcDep2 > 0);
        assertTrue(crcDep3 > 0);
        assertTrue(crcDep4 > 0);

        assertTrue(crcDep1 != crcDep2);
        assertTrue(crcDep1 == crcDep3);
        assertTrue(crcDep3 != crcDep4);
    }
}
