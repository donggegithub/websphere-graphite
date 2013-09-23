/**
 * This file is part of Wasagent.
 *
 * Wasagent is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wasagent is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Wasagent. If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package net.wait4it.graphite.wasagent.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.ibm.websphere.pmi.stat.WSBoundedRangeStatistic;
import com.ibm.websphere.pmi.stat.WSJDBCConnectionPoolStats;
import com.ibm.websphere.pmi.stat.WSRangeStatistic;
import com.ibm.websphere.pmi.stat.WSStats;

import net.wait4it.graphite.wasagent.core.WASClientProxy;

/**
 * Gets statistics for JDBC datasources.
 * 
 * The following metrics are available:
 * 
 *   - The datasource current pool size
 *   - The datasource maximum pool size
 *   - The active connection count
 *   - The number of threads waiting for
 *     a connection from the pool
 * 
 * @author Yann Lambret
 *
 */
public class JDBCTest extends TestUtils implements Test {

    /**
     * WebSphere JDBC datasources stats.
     * 
     * @param proxy   an applicative proxy for the target WAS instance
     * @param params  a comma separated list of datasource names, or
     *                a wildcard character (*) for all datasources
     * @return output a list of strings for collected data
     */
    public List<String> run(WASClientProxy proxy, String params) {
        // HTTP query params
        List<String> datasources = Arrays.asList(params.split(","));

        // Graphite data
        List<String> output = new ArrayList<String>();

        // Datasource name
        String name;

        // PMI stats
        WSStats stats;
        WSBoundedRangeStatistic ps;
        WSBoundedRangeStatistic fps;
        WSRangeStatistic wtc;

        // Performance data
        long currentPoolSize, maximumPoolSize, freePoolSize, waitingThreadCount, activeCount;

        try {
            stats = proxy.getStats(WSJDBCConnectionPoolStats.NAME);
        } catch (Exception ignored) {
            return output;
        }

        WSStats[] stats1 = stats.getSubStats(); // JDBC Provider level
        for (WSStats stat1 : stats1) {
            WSStats[] stats2 = stat1.getSubStats(); // DataSource level
            for (WSStats stat2 : stats2) {

                // No statistics for WAS internal datasources
                if (stat2.getName().matches("jdbc/DefaultEJBTimerDataSource")) {
                    continue;
                }

                if (datasources.contains("*") || datasources.contains(stat2.getName())) {
                    ps = (WSBoundedRangeStatistic)stat2.getStatistic(WSJDBCConnectionPoolStats.PoolSize);
                    fps = (WSBoundedRangeStatistic)stat2.getStatistic(WSJDBCConnectionPoolStats.FreePoolSize);
                    wtc = (WSRangeStatistic)stat2.getStatistic(WSJDBCConnectionPoolStats.WaitingThreadCount);
                    try {
                        currentPoolSize = ps.getCurrent();
                        maximumPoolSize = ps.getUpperBound();
                        freePoolSize = fps.getCurrent();
                        waitingThreadCount = wtc.getCurrent();
                        activeCount = currentPoolSize - freePoolSize;
                    } catch (NullPointerException e) {
                        throw new RuntimeException("invalid 'JDBC Connection Pools' PMI settings.");
                    }

                    name = normalize(stat2.getName());
                    output.add("jdbc." + name + ".activeCount " + activeCount);
                    output.add("jdbc." + name + ".currentPoolSize " + currentPoolSize);
                    output.add("jdbc." + name + ".maximumPoolSize " + maximumPoolSize);
                    output.add("jdbc." + name + ".waitingThreadCount " + waitingThreadCount);
                }
            }
        }

        Collections.sort(output);
        return output;
    }

}
