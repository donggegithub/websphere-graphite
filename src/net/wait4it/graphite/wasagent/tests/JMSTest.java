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
import com.ibm.websphere.pmi.stat.WSJCAConnectionPoolStats;
import com.ibm.websphere.pmi.stat.WSRangeStatistic;
import com.ibm.websphere.pmi.stat.WSStats;

import net.wait4it.graphite.wasagent.core.WASClientProxy;

/**
 * Gets statistics for JMS connection factories.
 * 
 * The following metrics are available:
 * 
 *   - The JMS connection factory current pool size
 *   - The JMS connection factory maximum pool size
 *   - The active connection count
 *   - The number of threads waiting for an available
 *     connection
 * 
 * @author Yann Lambret
 *
 */
public class JMSTest extends TestUtils implements Test {

    /**
     * WebSphere JMS connection factories stats.
     * 
     * @param proxy   an applicative proxy for the target WAS instance
     * @param params  a comma separated list of factory names, or
     *                a wildcard character (*) for all factories
     * @return output a list of strings for collected data
     */
    public List<String> run(WASClientProxy proxy, String params) {
        // HTTP query params
        List<String> factories = Arrays.asList(params.split(","));

        // Graphite data
        List<String> output = new ArrayList<String>();

        // JMS factory name
        String name;

        // PMI stats
        WSStats stats;
        WSBoundedRangeStatistic ps;
        WSBoundedRangeStatistic fps;
        WSRangeStatistic wtc;

        // Performance data
        long maximumPoolSize, currentPoolSize, freePoolSize, waitingThreadCount, activeCount;

        try {
            stats = proxy.getStats(WSJCAConnectionPoolStats.NAME);
        } catch (Exception ignored) {
            return output;
        }    

        WSStats[] stats1 = stats.getSubStats(); // JMS provider level
        for (WSStats stat1 : stats1) {
            if (stat1.getName().equals("SIB JMS Resource Adapter") || stat1.getName().equals("WebSphere MQ JMS Provider")) {
                WSStats[] stats2 = stat1.getSubStats(); // JCA factory level
                for (WSStats stat2 : stats2) {

                    if (factories.contains("*") || factories.contains(stat2.getName())) {
                        ps = (WSBoundedRangeStatistic)stat2.getStatistic(WSJCAConnectionPoolStats.PoolSize);
                        fps = (WSBoundedRangeStatistic)stat2.getStatistic(WSJCAConnectionPoolStats.FreePoolSize);
                        wtc = (WSRangeStatistic)stat2.getStatistic(WSJCAConnectionPoolStats.WaitingThreadCount);
                        try {
                            currentPoolSize = ps.getCurrent();
                            maximumPoolSize = ps.getUpperBound();
                            freePoolSize = fps.getCurrent();
                            waitingThreadCount = wtc.getCurrent();
                            activeCount = currentPoolSize - freePoolSize;
                        } catch (NullPointerException e) {
                            throw new RuntimeException("invalid 'JCA Connection Pools' PMI settings.");
                        }

                        name = normalize(stat2.getName());
                        output.add("jms." + name + ".activeCount " + activeCount);
                        output.add("jms." + name + ".currentPoolSize " + currentPoolSize);
                        output.add("jms." + name + ".maximumPoolSize " + maximumPoolSize);
                        output.add("jms." + name + ".waitingThreadCount " + waitingThreadCount);
                    }
                }
            }
        }

        Collections.sort(output);
        return output;
    }

}
