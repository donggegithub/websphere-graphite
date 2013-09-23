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

import com.ibm.websphere.pmi.stat.WSRangeStatistic;
import com.ibm.websphere.pmi.stat.WSSessionManagementStats;
import com.ibm.websphere.pmi.stat.WSStats;

import net.wait4it.graphite.wasagent.core.WASClientProxy;

/**
 * Gets the current HTTP sessions count
 * for a web application.
 * 
 * @author Yann Lambret
 *
 */
public class ApplicationTest extends TestUtils implements Test {

    /**
     * WebSphere applications stats.
     * 
     * @param proxy   an applicative proxy for the target WAS instance
     * @param params  a comma separated list of web application names, or
     *                a wildcard character (*) for all web applications
     * @return output a list of strings for collected data
     */
    public List<String> run(WASClientProxy proxy, String params) {
        // HTTP query params
        List<String> applications = Arrays.asList(params.split(","));

        // Graphite data
        List<String> output = new ArrayList<String>();

        // Application name
        String name;

        // PMI stats
        WSStats stats;
        WSRangeStatistic lc;

        // Performance data
        long liveCount;

        try {
            stats = proxy.getStats(WSSessionManagementStats.NAME);
        } catch (Exception ignored) {
            return output;
        }

        WSStats[] stats1 = stats.getSubStats();
        for (WSStats stat1 : stats1) {

            if (stat1.getName().matches("ibmasyncrsp#ibmasyncrsp.war")) {
                continue;
            }

            if (applications.contains("*") || applications.contains(stat1.getName())) {
                lc = (WSRangeStatistic)stat1.getStatistic(WSSessionManagementStats.LiveCount);
                try {
                    liveCount = lc.getCurrent();
                } catch (NullPointerException e) {
                    throw new RuntimeException("invalid 'Servlet Session Manager' PMI settings.");
                }
                name = normalize(stat1.getName());
                output.add("application." + name + ".liveCount " + liveCount);
            }
        }

        Collections.sort(output);
        return output;
    }

}
