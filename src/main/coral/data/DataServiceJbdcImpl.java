/*
 *   Copyright 2009-2015 Markus Schaffner
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package coral.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import coral.model.ExpData;

public class DataServiceJbdcImpl implements DataService {

    Connection conn;

    protected final Log logger = LogFactory.getLog(this.getClass());

    public DataServiceJbdcImpl(String mode, boolean setup) {
        this("db", setup, mode);
    }

    // CONSTRUCTOR
    public DataServiceJbdcImpl(String dbname, boolean setup, String dbmode) {

        // Load the HSQL Database Engine JDBC driver
        // hsqldb.jar should be in the class path or made part of the current
        // jar
        try {
            Class.forName("org.hsqldb.jdbcDriver");

            // connect to the database. This will load the db files and start
            // the
            // database if it is not already running.
            // db_file_name_prefix is used to open or create files that hold the
            // state
            // of the db.
            // It can contain directory names relative to the
            // current working directory
            conn = DriverManager.getConnection("jdbc:hsqldb:" + dbmode + ":"
                    + dbname + "/exp.db", // filenames
                    "sa", // username
                    "");

            if (dbmode.equals("mem") || setup) {
                logger.info("SETUP DATABASE");

                Statement stat = conn.createStatement();
                // stat.execute("delete from datas;");
                // stat.execute("delete from states;");
                stat.execute("drop table if exists datas;");
                stat.execute("CREATE TABLE datas ( id BIGINT, collection VARCHAR(10), name VARCHAR(80), value VARCHAR(1024));");
                stat.execute("CREATE INDEX idatas ON datas ( id, name );");
                stat.execute("drop table if exists states;");
                stat.execute("CREATE TABLE states ( id BIGINT, collection VARCHAR(10), template VARCHAR(80), block INTEGER, round INTEGER, stage INTEGER, msg VARCHAR(255));");
                stat.execute("COMMIT");

                conn.commit();
                stat.close();

            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    static long lastid = -1;

    // FIXME INSERT not sure what to fix
    /*
     * (non-Javadoc)
     * 
     * @see coral.data.DataService#saveOETData(java.lang.String,
     * coral.model.ExpData)
     */
    @Override
    public synchronized void saveOETData(String collection, ExpData stage) {
        long id = System.currentTimeMillis();

        if (id <= lastid) {
            lastid++;
            id = lastid;
        }
        lastid = id;

        PreparedStatement prep;
        try {
            prep = conn
                    .prepareStatement("insert into states values (?, ?, ?, ?, ?, ?, ?);");

            String inmsg = (stage.inmsg.length() < 70) ? stage.inmsg
                    : stage.inmsg.substring(0, 70);

            prep.setString(1, Long.toString(id));
            prep.setString(2, collection);
            prep.setString(3, stage.template);
            prep.setString(4, "1");
            prep.setString(5, Integer.toString(stage._msgCounter));
            prep.setString(6, Integer.toString(stage._stageCounter));
            prep.setString(7, inmsg);
            prep.addBatch();

            conn.setAutoCommit(false);
            prep.executeBatch();
            conn.setAutoCommit(true);

            conn.commit();

            put(id, collection, stage.newMap());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // INSERT
    private long put(long id, String collection, Map<String, String> map)
            throws SQLException {

        if (map.size() > 0) {
            PreparedStatement prep = conn
                    .prepareStatement("insert into datas values (?, ?, ?, ?);");

            for (Map.Entry<String, String> e : map.entrySet()) {
                prep.setString(1, Long.toString(id));
                prep.setString(2, collection);
                prep.setString(3, e.getKey());
                String v = e.getValue();
                prep.setString(4,
                        v.substring(0, (v.length() > 1000) ? 1000 : v.length()));
                prep.addBatch();
            }

            conn.setAutoCommit(false);
            prep.executeBatch();
            conn.setAutoCommit(true);
        }

        conn.commit();

        return id;
    }

    // SELECT
    /*
     * (non-Javadoc)
     * 
     * @see coral.data.DataService#getMap(java.lang.String, long)
     */
    @Override
    public Map<String, String> getMap(String collection, long id)
            throws SQLException {

        Map<String, String> map = new LinkedHashMap<String, String>();

        Statement stat = conn.createStatement();

        ResultSet rs = stat.executeQuery("select * from datas WHERE id = " + id
                + " AND collection = '" + collection + "' ORDER BY name;");
        while (rs.next()) {
            // L.println("name = " + rs.getString("name"));
            map.put(rs.getString(3), rs.getString(4));
        }
        rs.close();

        stat.close();

        return map;
    }

    
    // SELECT
    /*
     * (non-Javadoc)
     * 
     * @see coral.data.DataService#getMap(java.lang.String, long)
     */
    @Override
    public List<String> getAllVariableNames() throws SQLException {

        List<String> result = new ArrayList<String>();

        Statement stat = conn.createStatement();

        ResultSet rs = stat.executeQuery("SELECT DISTINCT name FROM datas ORDER BY name;");
        while (rs.next()) {
            // L.println("name = " + rs.getString("name"));
            result.add(rs.getString(1));
        }
        rs.close();

        stat.close();

        return result;
    }
    // ALL DATA
    /*
     * (non-Javadoc)
     * 
     * @see coral.data.DataService#getAllData()
     */
    @Override
    public List<String[]> getAllData() throws SQLException {

        List<String[]> list = new ArrayList<String[]>();

        Statement stat = conn.createStatement();

        ResultSet rs = stat.executeQuery("select * from datas;");
        while (rs.next()) {
            // L.println("name = " + rs.getString("name"));
            list.add(new String[] { rs.getString(1), rs.getString(2),
                    rs.getString(3), rs.getString(4) });
        }
        rs.close();

        stat.close();

        return list;
    }

    // SELECT
    /*
     * (non-Javadoc)
     * 
     * @see coral.data.DataService#getKeyValue(java.lang.String, long)
     */
    public List<String[]> getKeyValue(String collection, long id)
            throws SQLException {

        List<String[]> list = new ArrayList<String[]>();

        Statement stat = conn.createStatement();

        ResultSet rs = stat.executeQuery("select * from datas WHERE id = " + id
                + " AND collection = '" + collection + "';");
        while (rs.next()) {
            // L.println("name = " + rs.getString("name"));
            list.add(new String[] { rs.getString(3), rs.getString(4) });
        }
        rs.close();

        stat.close();

        return list;
    }

    // SELECT
    /*
     * (non-Javadoc)
     * 
     * @see coral.data.DataService#getMaps(java.lang.String, java.lang.String,
     * java.lang.String)
     */
    public Object[] getMaps(String collection, String name, String value)
            throws SQLException {

        Statement stat = conn.createStatement();
        ResultSet rs0 = stat
                .executeQuery("select distinct id from datas WHERE name = '"
                        + name + "' AND value = '" + value + "';");

        Set<Map<String, String>> maps = new LinkedHashSet<Map<String, String>>();

        while (rs0.next()) {

            Map<String, String> map = new LinkedHashMap<String, String>();

            long id = rs0.getLong("id");
            ResultSet rs = stat.executeQuery("select * from datas WHERE id = "
                    + id + " AND collection = " + collection
                    + " ORDER BY name;");
            while (rs.next()) {
                // L.println("name = " + rs.getString("name"));
                map.put(rs.getString(3), rs.getString(4));
            }
            rs.close();

            maps.add(map);
        }

        rs0.close();
        stat.close();

        return maps.toArray();
    }

    // SELECT
    /*
     * (non-Javadoc)
     * 
     * @see coral.data.DataService#retriveState(java.lang.String, long)
     */
    @Override
    public ExpData retriveState(String collection, long id) {
        ExpData result = new ExpData();

        try {
            Statement stat = conn.createStatement();

            ResultSet rs = stat
                    .executeQuery("select * from states WHERE id <= " + id
                            + " AND collection = '" + collection
                            + " ORDER BY id';");
            if (rs.next()) {
                result._msgCounter = Integer.parseInt(rs.getString("round"));
                result._stageCounter = Integer.parseInt(rs.getString("stage"));
                result.template = rs.getString("template");
                result.inmsg = rs.getString("msg");
            }
            rs.close();

            rs = stat.executeQuery("select * from datas WHERE id <= " + id
                    + " AND collection = '" + collection
                    + "' ORDER BY name,id;");
            while (rs.next()) {
                // L.println("name = " + rs.getString("name"));
                result.put(rs.getString(3), rs.getString(4));
            }
            rs.close();

            stat.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    // SELECT
    /*
     * (non-Javadoc)
     * 
     * @see coral.data.DataService#retriveState(java.lang.String, long)
     */
    @Override
    public boolean retriveData(String collection, ExpData data) {
        ExpData result = data;
        boolean outcome = false;

        try {
            Statement stat = conn.createStatement();

            ResultSet rs = stat
                    .executeQuery("select * from states WHERE collection = '"
                            + collection + "' ORDER BY id DESC LIMIT 1;");
            if (rs.next()) {
                result._msgCounter = Integer.parseInt(rs.getString("round"));
                result._stageCounter = Integer.parseInt(rs.getString("stage"));
                result.template = rs.getString("template");
                result.inmsg = rs.getString("msg");
                outcome = true;
            }
            rs.close();

            rs = stat.executeQuery("select * from datas WHERE collection = '"
                    + collection + "' ORDER BY name,id;");
            while (rs.next()) {
                result.put(rs.getString(3), rs.getString(4));
                outcome = true;
            }
            rs.close();

            stat.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return outcome;
    }

    // SELECT
    /*
     * (non-Javadoc)
     * 
     * @see coral.data.DataService#stageInfos()
     */
    @Override
    public List<String[]> stageInfos() {
        List<String[]> result = new ArrayList<String[]>();

        try {
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery("select * from states;");

            while (rs.next()) {
                String[] s = new String[5];
                int i = 0;
                s[i++] = rs.getString("id");
                s[i++] = rs.getString("collection");
                s[i++] = rs.getString("template");
                s[i++] = rs.getString("stage");
                s[i++] = rs.getString("msg");
                result.add(s);
            }
            rs.close();
            stat.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // SELECT
    private List<String[]> getTable(String by, String who, String... cols) {
        List<String[]> result = new ArrayList<String[]>();

        StringBuilder select = new StringBuilder(
                "SELECT DISTINCT d.collection, d.value");
        StringBuilder from = new StringBuilder(" FROM datas AS d");
        StringBuilder where = new StringBuilder(" WHERE d.name='" + by + "'");
        who = who.replaceAll("\\b" + by + "\\b", "d.value");

        /*
         * for (int i = 0; i< cols.length; i++) { select.append(", d" + i +
         * ".value"); from.append( ", datas AS d"+ i ); where.append(
         * " AND d"+i+".name='" + cols[i] + "' AND d.id = d"+i+".id" ); who =
         * who.replaceAll( "\\b" + cols[i] + "\\b", "d"+i+".value" ); }
         */

        for (int i = 0; i < cols.length; i++) {
            select.append(", d" + i + ".value");
            from.append(" LEFT JOIN datas AS d" + i + " ON  d.id = d" + i
                    + ".id");
            where.append(" AND d" + i + ".name='" + cols[i] + "'");
            who = who.replaceAll("\\b" + cols[i] + "\\b", "d" + i + ".value");
        }

        who = who + " ORDER BY d.collection DESC";

        try {
            // long a = System.currentTimeMillis();
            // System.out.println( System.currentTimeMillis() + ": " +
            // select.toString() + from.toString() + where.toString() + " " +
            // who);

            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(select.toString() + from.toString()
                    + where.toString() + " " + who);
            // ResultSet rs =
            // st.executeQuery("SELECT DISTINCT id, collection, name, value FROM datas");

            // System.out.println( System.currentTimeMillis() + " " + (a -
            // System.currentTimeMillis()) );

            // int c = 0;
            while (rs.next()) {
                String[] r = new String[cols.length + 2];
                for (int i = 1; i < cols.length + 3; i++) {
                    r[i - 1] = rs.getString(i);
                }
                result.add(r);
            }

            rs.close();
            st.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    // SELECT
    /*
     * (non-Javadoc)
     * 
     * @see coral.data.DataService#getTableWithId(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Deprecated
    private List<String[]> getTableWithId(String by, String who, String... cols) {
        List<String[]> result = new ArrayList<String[]>();

        StringBuilder select = new StringBuilder(
                "SELECT DISTINCT d.id, d.collection, d.value");
        StringBuilder from = new StringBuilder(" FROM datas AS d");
        StringBuilder where = new StringBuilder(" WHERE d.name='" + by + "'");
        who = who.replaceAll("\\b" + by + "\\b", "d.value");

        /*
         * for (int i = 0; i< cols.length; i++) { select.append(", d" + i +
         * ".value"); from.append( ", datas AS d"+ i ); where.append(
         * " AND d"+i+".name='" + cols[i] + "' AND d.id = d"+i+".id" ); who =
         * who.replaceAll( "\\b" + cols[i] + "\\b", "d"+i+".value" ); }
         */

        for (int i = 0; i < cols.length; i++) {
            select.append(", d" + i + ".value");
            from.append(" LEFT JOIN datas AS d" + i + " ON  d.id = d" + i
                    + ".id");
            where.append(" AND d" + i + ".name='" + cols[i] + "'");
            who = who.replaceAll("\\b" + cols[i] + "\\b", "d" + i + ".value");
        }

        who = who + " ORDER BY d.collection, d.id DESC";

        try {
            // long a = System.currentTimeMillis();
            // System.out.println( System.currentTimeMillis() + ": " +
            // select.toString() + from.toString() + where.toString() + " " +
            // who);

            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(select.toString() + from.toString()
                    + where.toString() + " " + who);
            // ResultSet rs =
            // st.executeQuery("SELECT DISTINCT id, collection, name, value FROM datas");

            // System.out.println( System.currentTimeMillis() + " " + (a -
            // System.currentTimeMillis()) );

            // int c = 0;
            while (rs.next()) {

                String[] r = new String[cols.length + 4];
                for (int i = 1; i < cols.length + 4; i++) {
                    r[i - 1] = rs.getString(i);
                }
                result.add(r);
            }

            rs.close();
            st.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    // SELECT
    /*
     * (non-Javadoc)
     * 
     * @see coral.data.DataService#getNewId(int)
     */
    @Override
    public int getNewId(int min) {
        int result = 0;
        Statement stat;
        try {
            stat = conn.createStatement();

            ResultSet rs = stat
                    .executeQuery("select max(CAST(collection AS Int)) from datas;");

            while (rs.next()) {
                String s = rs.getString(1);
                logger.debug("max number result " + s);
                int i = (s == null || s.equals("")) ? 0 : Integer.parseInt(s);
                if (i > result) {
                    result = i;
                }
            }

            logger.debug("max id is: " + result);

            rs.close();
            stat.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        result = Math.max(min, result + 1);

        return result;
    }

    // MAIN FIXME TEST
    public static void main(String[] args) throws InterruptedException {

        DataServiceJbdcImpl ds = new DataServiceJbdcImpl("file", true);

        for (String[] s : ds.getTable("number", "")) {
            System.out.println(Arrays.toString(s));
        }

        for (String[] s : ds.getTable("finalpayoff", "")) {
            System.out.println(Arrays.toString(s));
        }

        for (String[] s : ds.getTable("number", "", "payoffselection",
                "racenum", "earnings", "finalpayoff")) {
            System.out.println(Arrays.toString(s));
        }

        ds.close();

        Thread.sleep(1000);

    }

    // UTIL
    /*
     * (non-Javadoc)
     * 
     * @see coral.data.DataService#close()
     */
    @Override
    public void close() {
        try {
            conn.commit();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // UTIL
    /*
     * (non-Javadoc)
     * 
     * @see coral.data.DataService#stageInfo()
     */
    @Override
    public Object[][] stageInfo() {
        return arrayFromList(stageInfos());
    }

    // UTIL
    private static Object[][] arrayFromList(List<String[]> list) {
        Object[][] result = new Object[list.size()][];

        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }

}
